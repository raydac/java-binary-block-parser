package com.igormaznitsa.jbbp.plugin.gradle;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.plugin.common.converters.ParserFlags;
import com.igormaznitsa.jbbp.plugin.common.converters.ScriptProcessor;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class JBBPGenerateTask extends AbstractJBBPTask {

    @Input
    @Optional
    protected final Set<String> interfaces = new HashSet<String>();

    @Input
    @Optional
    private final List<ParserFlags> parserFlags = new ArrayList<ParserFlags>();

    @Input
    @Optional
    private final Set<String> customTypes = new HashSet<String>();

    @Input
    @Optional
    private final Map<String, String> mapStructToInterfaces = new HashMap<String, String>();

    @Input
    @Optional
    protected String outEncoding = "UTF-8";

    @Input
    @Optional
    protected String inEncoding = "UTF-8";

    @Input
    @Optional
    protected String classCapText;

    @Input
    @Optional
    protected String customText;

    @Input
    @Optional
    protected boolean doGettersSetters;

    @Input
    @Optional
    protected String superClass;

    @Input
    @Optional
    protected boolean forceAbstract;

    @Input
    @Optional
    protected boolean disableRegisterSrc;

    public void setCustomTypes(final Set<String> value) {
        this.customTypes.clear();
        if (value != null) {
            this.customTypes.addAll(value);
        }
    }

    public void setParserFlags(final List<ParserFlags> value) {
        this.parserFlags.clear();
        if (value != null) {
            this.parserFlags.addAll(value);
        }
    }

    public void setInterfaces(final Set<String> value) {
        this.interfaces.clear();
        if (value != null) {
            this.interfaces.addAll(value);
        }
    }

    public void setMapStructToInterfaces(final Map<String, String> value) {
        this.mapStructToInterfaces.clear();
        if (value != null) {
            this.mapStructToInterfaces.putAll(value);
        }
    }

    @Override
    protected void doTaskAction() {
        final Set<File> scripts = findScripts();

        final Set<String> normalizedCustomTypeNames = new HashSet<String>();
        for (final String s : this.customTypes) {
            final String trimmed = s.trim();
            final String normalized = trimmed.toLowerCase(Locale.ENGLISH);
            if (!normalized.equals(trimmed)) {
                getLogger().warn(String.format("Custom type name '%s' in JBBP normal form is '%s' ", trimmed, normalized));
            }
            normalizedCustomTypeNames.add(normalized);
        }
        getLogger().debug("Defined normalized custom types : " + normalizedCustomTypeNames);
        final String[] customTypesArray = normalizedCustomTypeNames.toArray(new String[normalizedCustomTypeNames.size()]);

        final JBBPCustomFieldTypeProcessor customFieldProcessor = new JBBPCustomFieldTypeProcessor() {
            @Override
            @Nonnull
            @MustNotContainNull
            public String[] getCustomFieldTypes() {
                return customTypesArray;
            }

            @Override
            public boolean isAllowed(@Nonnull final JBBPFieldTypeParameterContainer fieldType, @Nullable final String fieldName, final int extraData, final boolean isArray) {
                final boolean result = normalizedCustomTypeNames.contains(fieldType.getTypeName());
                if (!result) {
                    getLogger().warn("Detected not allowed custom type name : " + fieldType.getTypeName());
                }
                return result;
            }

            @Override
            @Nonnull
            public JBBPAbstractField readCustomFieldType(@Nonnull final JBBPBitInputStream in, @Nonnull final JBBPBitOrder bitOrder, final int parserFlags, @Nonnull final JBBPFieldTypeParameterContainer customTypeFieldInfo, @Nullable final JBBPNamedFieldInfo fieldName, final int extraData, final boolean readWholeStream, final int arrayLength) throws IOException {
                throw new Error("Must not be called");
            }
        };


        final ScriptProcessor.Parameters parameters = new ScriptProcessor.Parameters();
        parameters
                .setOutputDir(this.getOutput())
                .setClassCapComment(this.classCapText)
                .setCustomText(this.customText)
                .setEncodingIn(this.inEncoding)
                .setEncodingOut(this.outEncoding)
                .setCustomFieldTypeProcessor(customFieldProcessor)
                .setSuperClass(this.superClass)
                .setClassInterfaces(this.interfaces)
                .setMapClassInterfaces(this.mapStructToInterfaces)
                .setDoGettersSetters(this.doGettersSetters)
                .setForceAbstract(this.forceAbstract);


        for (final File aScript : findScripts()) {
            parameters.setScriptFile(aScript).assertAllOk();
            getLogger().info("Detected JBBP script file : " + aScript);
            try {
                final Set<File> files = this.target.getScriptProcessor().processScript(parameters);
                getLogger().debug("Converted " + aScript + " into " + files);
                for (final File f : files) {
                    getLogger().info(String.format("JBBP script '%s' has been converted into '%s'", aScript.getName(), f.getName()));
                }
            } catch (IOException ex) {
                throw new GradleException("Error during JBBP script translation : " + aScript.getAbsolutePath(), ex);
            }
        }


        if (!this.disableRegisterSrc) {
            getLogger().debug("Registering path to java sources : " + this.output);
            if (getProject().getPlugins().hasPlugin(JavaPlugin.class)) {
                final JavaPluginConvention javaPluginConvention = getProject().getConvention().getPlugin(JavaPluginConvention.class);
                final SourceSet main = javaPluginConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                main.getJava().srcDir(this.output);
                getLogger().info("Source folder has been added into Java  task : " + this.output);
            } else {
                getLogger().info("Java plugin not found");
            }
        } else {
            getLogger().info("Source folder registration has been disabled");
        }
    }
}
