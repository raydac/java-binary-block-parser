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
package com.igormaznitsa.jbbp.mvn.plugin;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * The Mojo looks for all JBBP scripts in source and generate Java class sources
 * (1.6+) for these scripts.
 *
 * @author Igor Maznitsa
 */
@Mojo(name = "jbbp", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateSourcesMojo extends AbstractMojo {

    /**
     * Provides an explicit list of all the JBBP scripts that should be included
     * in the generate phase of the plug-in.
     * <p>
     * A set of Ant-like inclusion patterns used to select files from the source
     * directory for processing. By default, the pattern
     * <code>**&#47;*.jbbp</code> is used to select JBBP script files.
     * </p>
     */
    @Parameter(alias = "includes")
    private final Set<String> includes = new HashSet<String>();
    /**
     * A set of Ant-like exclusion patterns used to prevent certain files from
     * being processed. By default, this set is empty such that no files are
     * excluded.
     */
    @Parameter(alias = "excludes")
    private final Set<String> excludes = new HashSet<String>();
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
    private final List<ParserFlags> parserFlags = new ArrayList<ParserFlags>();
    /**
     * List of names of allowed custom types.
     */
    @Parameter(alias = "customTypes")
    private final Set<String> customTypes = new HashSet<String>();
    /**
     * Interfaces for structures, structure will be implementing the interface and getter will return interface instead of structure type.
     */
    @Parameter(alias = "mapStructToInterfaces")
    private final Map<String, String> mapStructToInterfaces = new HashMap<String, String>();
    /**
     * Target for source generation.
     */
    @Parameter(alias = "target", defaultValue = "JAVA_1_6")
    private String target;
    /**
     * Flag to make plugin verbose.
     */
    @Parameter(alias = "verbose", defaultValue = "false")
    private boolean verbose;
    /**
     * Package to override package name extracted from script file name, it will be
     * common for all processed classes.
     */
    @Parameter(alias = "commonPackage")
    private String commonPackage;
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
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;
    /**
     * Specifies whether sources are added to the {@code compile} or {@code test}
     * scope.
     */
    @Parameter(alias = "generateTestSources", defaultValue = "false")
    private boolean generateTestSources;
    /**
     * Flag to skip processing of the plug-in, it also can be defined through the
     * "jbbp.skip" property
     */
    @Parameter(alias = "skip", defaultValue = "false")
    private boolean skip;
    /**
     * File contains text of cap comment for each generated class file. The Cap
     * text will be placed before package name and usually it can be used to
     * provide license information.
     */
    @Parameter(alias = "capCommentFile")
    private File capCommentFile;
    /**
     * Plain text of cap comment for each generated class file. The Cap text will
     * be placed before package name and usually it can be used to provide license
     * information.
     */
    @Parameter(alias = "capCommentText")
    private String capCommentText;
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
    @Parameter(alias = "doGettersSetters")
    private boolean doGettersSetters;
    /**
     * Super class for generated classes.
     */
    @Parameter(alias = "superClass")
    private String superClass;
    /**
     * Force abstract modifier for generated classes even if they don't have abstract methods.
     */
    @Parameter(alias = "forceAbstract")
    private boolean forceAbstract;
    /**
     * Source directory for JBBP scripts, the default value is
     * "${project.basedir}/src/jbbp"
     */
    @Parameter(alias = "sourceDirectory", defaultValue = "${project.basedir}/src/jbbp")
    private File sourceDirectory;

    /**
     * Destination directory for generated Java classes, the default value is
     * "${project.build.directory}/generated-sources/jbbp"
     */
    @Parameter(alias = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/jbbp")
    private File outputDirectory;

    @Nonnull
    private static String getEncoding(@Nullable final String encoding) {
        return (encoding == null) ? Charset.defaultCharset().name() : Charset.forName(encoding.trim()).name();
    }

    public boolean getVerbose() {
        return this.verbose;
    }

    @Nullable
    public String getTarget() {
        return this.target;
    }

    public void setTarget(@Nullable final String value) {
        this.target = value;
    }

    @Nullable
    public String getSuperClass() {
        return this.superClass;
    }

    public boolean getForceAbstract() {
        return this.forceAbstract;
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

    public boolean getDoGettersSetters() {
        return this.doGettersSetters;
    }

    public boolean getSkip() {
        return this.skip || Boolean.getBoolean("jbbp.skip");
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
    public File getCapCommentFile() {
        return this.capCommentFile;
    }

    @Nullable
    public String getCapCommentText() {
        return this.capCommentText;
    }

    @Nullable
    public String getInputEncoding() {
        return this.inputEncoding;
    }

    @Nullable
    public String getOutputEncoding() {
        return this.outputEncoding;
    }

    @Nullable
    public String getCommonPackage() {
        return this.commonPackage;
    }

    public boolean getGenerateTestSources() {
        return this.generateTestSources || Boolean.getBoolean("jbbp.generateTestSources");
    }

    @MustNotContainNull
    @Nonnull
    public Set<String> getExcludes() {
        return this.excludes;
    }

    @MustNotContainNull
    @Nonnull
    public Set<String> getIncludes() {
        return this.includes;
    }

    @MustNotContainNull
    @Nonnull
    public List<ParserFlags> getParserFlags() {
        return this.parserFlags;
    }

    @Nonnull
    public File getSourceDirectory() {
        return this.sourceDirectory;
    }

    @Nonnull
    public File getOutputDirectory() {
        return this.outputDirectory;
    }

    private void registerSourceRoot(@Nonnull final File outputDir) {
        if (this.generateTestSources) {
            getLog().debug("Registering TEST source root : " + outputDir.getPath());
            this.project.addTestCompileSourceRoot(outputDir.getPath());
        } else {
            getLog().debug("Registering source root : " + outputDir.getPath());
            this.project.addCompileSourceRoot(outputDir.getPath());
        }
    }

    public void logInfo(@Nonnull final String text, final boolean onlyIfVerbose) {
        if (this.verbose) {
            getLog().info(text);
        } else {
            if (!onlyIfVerbose) {
                getLog().info(text);
            }
        }
    }

    @Nullable
    private String makeCapText(@Nonnull final String inEncoding) throws IOException {
        String result = null;
        if (this.capCommentText != null) {
            result = this.capCommentText;
        } else if (this.capCommentFile != null) {
            getLog().debug("Provided CAP comment file: " + this.capCommentFile.getPath());
            result = FileUtils.readFileToString(this.capCommentFile, inEncoding);
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

    public int makeParserFlags() {
        int result = 0;
        for (final ParserFlags s : this.parserFlags) {
            result |= s.getFlag();
        }
        return result;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip) {
            logInfo("Skip processing", false);
            return;
        }

        final Set<File> foundJBBPScripts;

        final String inEncoding = getEncoding(this.inputEncoding);
        final String outEncoding = getEncoding(this.outputEncoding);

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

        try {
            final SourceInclusionScanner scanner = new SimpleSourceInclusionScanner(this.includes, this.excludes);
            scanner.addSourceMapping(new SuffixMapping("JBBP", "jbbp"));
            foundJBBPScripts = scanner.getIncludedSources(this.sourceDirectory, this.outputDirectory);
        } catch (InclusionScanException ex) {
            throw new MojoExecutionException("Error during sources scanning", ex);
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

        if (foundJBBPScripts.isEmpty()) {
            getLog().warn("There is not any detected JBBP script");
        } else {
            final Target theTarget;
            try{
                theTarget = this.target == null ? Target.JAVA_1_6 : Target.valueOf(this.target);
            }catch(IllegalArgumentException ex){
                throw new MojoExecutionException("Wrong target : "+this.target,ex);
            }

            for (final File f : foundJBBPScripts) {
                getLog().debug("Processing found JBBP script file : " + f);
                try {
                    theTarget.getScriptProcessor().processScript(this,
                            f,
                            capText,
                            customTextForClass,
                            inEncoding,
                            outEncoding,
                            customFieldProcessor);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Error during JBBP script translation : " + f.getAbsolutePath(), ex);
                }
            }
        }

        registerSourceRoot(this.outputDirectory);
    }

}
