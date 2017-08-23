/*
 * Copyright 2017 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.jbbp.plugin.mvn;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.plugin.common.converters.JBBPScriptTranslator;
import com.igormaznitsa.jbbp.plugin.common.converters.ParserFlags;
import com.igormaznitsa.jbbp.plugin.common.converters.Target;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.igormaznitsa.jbbp.plugin.common.utils.CommonUtils.ensureEncodingName;

/**
 * The Mojo looks for all JBBP scripts in source and generate sources.
 *
 * @author Igor Maznitsa
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class JBBPGenerateMojo extends AbstractJBBPMojo {

    /**
     * Interfaces for section 'implements' in generated classes.
     */
    @Parameter(alias = "interfaces")
    private final Set<String> interfaces = new HashSet<String>();
    /**
     * List of parser flags.
     * <ul>
     * <li>SKIP_REMAINING_FIELDS_IF_EOF</li>
     * </ul>
     */
    @Parameter(alias = "parserFlags")
    private final Set<ParserFlags> parserFlags = new HashSet<ParserFlags>();
    /**
     * List of names of allowed custom value types.
     */
    @Parameter(alias = "customTypes")
    private final Set<String> customTypes = new HashSet<String>();
    /**
     * Interfaces for structures, structure will be implementing the interface and getter will return interface instead of structure type.
     */
    @Parameter(alias = "mapStructToInterfaces")
    private final Map<String, String> mapStructToInterfaces = new HashMap<String, String>();
    /**
     * Specify output file encoding; defaults to source encoding.
     */
    @Parameter(alias = "outputEncoding", defaultValue = "${project.build.sourceEncoding}")
    private String outputEncoding;
    /**
     * Specify grammar file encoding; defaults to source encoding.
     */
    @Parameter(alias = "inputEncoding", defaultValue = "${project.build.sourceEncoding}")
    private String inputEncoding;
    /**
     * File contains text of cap comment for each generated class file. The Cap
     * text will be placed before package name and usually it can be used to
     * provide license information.
     */
    @Parameter(alias = "headCommentFile")
    private File headCommentFile;
    /**
     * Plain text of cap comment for each generated class file. The Cap text will
     * be placed before package name and usually it can be used to provide license
     * information.
     */
    @Parameter(alias = "headComment")
    private String headComment;
    /**
     * File contains text of custom section to be added into class body.
     */
    @Parameter(alias = "customTextFile")
    private File customTextFile;
    /**
     * Plain text of custom section to be added into class body.
     */
    @Parameter(alias = "customText")
    private String customText;
    /**
     * Generate getters and setters for class fields (class fields will be private
     * ones).
     */
    @Parameter(alias = "addGettersSetters")
    private boolean addGettersSetters;
    /**
     * Super class for generated classes.
     */
    @Parameter(alias = "superClass")
    private String superClass;
    /**
     * Force abstract modifier for generated classes even if they don't have abstract methods.
     */
    @Parameter(alias = "doAbstract")
    private boolean doAbstract;

    @Nullable
    public String getSuperClass() {
        return this.superClass;
    }

    public boolean getDoAbstract() {
        return this.doAbstract;
    }

    @Nonnull
    public Map<String, String> getMapStructToInterfaces() {
        return this.mapStructToInterfaces;
    }

    @Nonnull
    @MustNotContainNull
    public Set<String> getInterfaces() {
        return this.interfaces;
    }

    @Nonnull
    @MustNotContainNull
    public Set<String> getCustomTypes() {
        return this.customTypes;
    }

    public boolean getAddGettersSetters() {
        return this.addGettersSetters;
    }

    @Nullable
    public File getCustomTextFile() {
        return this.customTextFile;
    }

    @Nullable
    public String getCustomText() {
        return this.customText;
    }

    @Nullable
    public File getHeadCommentFile() {
        return this.headCommentFile;
    }

    @Nullable
    public String getHeadComment() {
        return this.headComment;
    }

    @Nullable
    public String getInputEncoding() {
        return this.inputEncoding;
    }

    @Nullable
    public String getOutputEncoding() {
        return this.outputEncoding;
    }

    @MustNotContainNull
    @Nonnull
    public Set<ParserFlags> getParserFlags() {
        return this.parserFlags;
    }

    @Nullable
    private String makeCapText(@Nonnull final String inEncoding) throws IOException {
        String result = null;
        if (this.headComment != null) {
            result = this.headComment;
        } else if (this.headCommentFile != null) {
            getLog().debug("Provided CAP comment file: " + this.headCommentFile.getPath());
            result = FileUtils.readFileToString(this.headCommentFile, inEncoding);
        }
        return result;
    }

    @Nullable
    private String makeCustomText(@Nonnull final String inEncoding) throws IOException {
        String result = null;
        if (this.customText != null) {
            result = this.customText;
        } else if (this.customTextFile != null) {
            getLog().debug("Provided custom text file: " + this.customTextFile.getPath());
            result = FileUtils.readFileToString(this.customTextFile, inEncoding);
        }
        return result;
    }

    @Override
    protected void executeMojo() throws MojoExecutionException, MojoFailureException {
        final String inEncoding = ensureEncodingName(this.inputEncoding);
        final String outEncoding = ensureEncodingName(this.outputEncoding);

        getLog().debug("Encoding In: " + inEncoding);
        getLog().debug("Encoding Out: " + outEncoding);

        final String capText;
        try {
            capText = makeCapText(inEncoding);
        } catch (IOException ex) {
            throw new MojoExecutionException("Can't read cap comment file", ex);
        }

        final String customTextForClass;
        try {
            customTextForClass = makeCustomText(inEncoding);
        } catch (IOException ex) {
            throw new MojoExecutionException("Can't read custom text file", ex);
        }

        if (this.includes.isEmpty()) {
            this.includes.add("**/*.jbbp");
        }

        final Set<String> normalizedCustomTypeNames = new HashSet<String>();
        for (final String s : this.customTypes) {
            final String trimmed = s.trim();
            final String normalized = trimmed.toLowerCase(Locale.ENGLISH);
            if (!normalized.equals(trimmed)) {
                getLog().warn(String.format("Custom type name '%s' in JBBP normal form is '%s' ", trimmed, normalized));
            }
            normalizedCustomTypeNames.add(normalized);
        }
        getLog().debug("Defined normalized custom types : " + normalizedCustomTypeNames);

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
                    getLog().warn("Detected not allowed custom type name : " + fieldType.getTypeName());
                }
                return result;
            }

            @Override
            @Nonnull
            public JBBPAbstractField readCustomFieldType(@Nonnull final JBBPBitInputStream in, @Nonnull final JBBPBitOrder bitOrder, final int parserFlags, @Nonnull final JBBPFieldTypeParameterContainer customTypeFieldInfo, @Nullable final JBBPNamedFieldInfo fieldName, final int extraData, final boolean readWholeStream, final int arrayLength) throws IOException {
                throw new Error("Must not be called");
            }
        };

        final Set<File> foundJBBPScripts = findSources(this.source);

        if (checkSetNonEmptyWithLogging(foundJBBPScripts)) {
            final Target theTarget = findTarget();
            final JBBPScriptTranslator.Parameters parameters = new JBBPScriptTranslator.Parameters();
            parameters
                    .setPackageName(this.packageName)
                    .setParserFlags(ParserFlags.makeFromSet(this.parserFlags))
                    .setOutputDir(this.output)
                    .setHeadComment(capText)
                    .setCustomText(customTextForClass)
                    .setEncodingIn(inEncoding)
                    .setEncodingOut(outEncoding)
                    .setCustomFieldTypeProcessor(customFieldProcessor)
                    .setSuperClass(this.superClass)
                    .setClassImplements(this.interfaces)
                    .setSubClassInterfaces(this.mapStructToInterfaces)
                    .setAddGettersSetters(this.addGettersSetters)
                    .setDoAbstract(this.doAbstract);

            for (final File aScript : foundJBBPScripts) {
                parameters.setScriptFile(aScript).assertAllOk();
                getLog().debug("Processing JBBP script file : " + aScript);
                try {
                    final Set<File> files = theTarget.getTranslator().translate(parameters, false);
                    getLog().debug("Converted " + aScript + " into " + files);
                    for (final File f : files) {
                        logInfo(String.format("JBBP script '%s' has been converted into '%s'", aScript.getName(), f.getName()), false);
                    }
                } catch (IOException ex) {
                    throw new MojoExecutionException("Error during JBBP script translation : " + aScript.getAbsolutePath(), ex);
                }
            }
        }

        registerSourceRoot(this.output);
    }

}
