package com.igormaznitsa.jbbp.plugin.mvn;

import com.igormaznitsa.jbbp.plugin.common.converters.Target;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractJBBPMojo extends AbstractMojo {
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
    protected final Set<String> includes = new HashSet<String>();
    /**
     * A set of Ant-like exclusion patterns used to prevent certain files from
     * being processed. By default, this set is empty such that no files are
     * excluded.
     */
    @Parameter(alias = "excludes")
    protected final Set<String> excludes = new HashSet<String>();
    /**
     * Current maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;
    /**
     * Specifies whether sources are added to the {@code compile} or {@code test}
     * scope.
     */
    @Parameter(alias = "generateTestSources", defaultValue = "false")
    protected boolean generateTestSources;
    /**
     * Package to override package name extracted from script file name, it will be
     * common for all processed classes.
     */
    @Parameter(alias = "packageName")
    protected String packageName;
    /**
     * Target for translation.
     * @see Target
     */
    @Parameter(alias = "target", defaultValue = "JAVA_1_6")
    protected String target;
    /**
     * Flag to skip processing of the plug-in.
     */
    @Parameter(alias = "skip", defaultValue = "false")
    protected boolean skip;
    /**
     * Destination directory for generated Java classes, the default value is
     * "${project.build.directory}/generated-sources/jbbp"
     */
    @Parameter(alias = "output", defaultValue = "${project.build.directory}/generated-sources/jbbp")
    protected File output;
    /**
     * Flag to make plugin verbose.
     */
    @Parameter(alias = "verbose", defaultValue = "false")
    protected boolean verbose;
    /**
     * Source directory for JBBP scripts, the default value is
     * "${project.basedir}/src/jbbp"
     */
    @Parameter(alias = "source", defaultValue = "${project.basedir}/src/jbbp")
    protected File source;

    @Nullable
    public String getTarget() {
        return this.target;
    }

    public void setTarget(@Nullable final String value) {
        this.target = value;
    }

    @MustNotContainNull
    @Nonnull
    public Set<String> getExcludes() {
        return this.excludes;
    }

    @Nullable
    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(@Nullable final String text) {
        this.packageName = text;
    }

    @MustNotContainNull
    @Nonnull
    public Set<String> getIncludes() {
        return this.includes;
    }

    @Nonnull
    public File getSource() {
        return this.source;
    }

    public void setSource(@Nonnull final File file) {
        if (file == null) throw new NullPointerException("File must not be null");
        this.source = file;
    }

    public boolean getSkip() {
        return this.skip;
    }

    public void setSkip(final boolean value) {
        this.skip = value;
    }

    public void setOutput(@Nonnull final File file) {
        if (file == null) throw new NullPointerException("File must not be null");
        this.output = file;
    }

    @Nonnull
    public File getOutput() {
        return this.output;
    }

    public boolean getVerbose() {
        return this.verbose;
    }

    public void setVerbose(final boolean flag) {
        this.verbose = flag;
    }

    public boolean getGenerateTestSources() {
        return this.generateTestSources;
    }

    public void setGenerateTestSources(final boolean flag) {
        this.generateTestSources = flag;
    }

    protected void registerSourceRoot(@Nonnull final File outputDir) {
        if (this.generateTestSources) {
            getLog().debug("Registering TEST source root : " + outputDir.getPath());
            this.project.addTestCompileSourceRoot(outputDir.getPath());
        } else {
            getLog().debug("Registering source root : " + outputDir.getPath());
            this.project.addCompileSourceRoot(outputDir.getPath());
        }
    }

    @Nonnull
    public Set<File> findSources(@Nonnull final File targetDirectory) throws MojoExecutionException {
        try {
            final SourceInclusionScanner scanner = new SimpleSourceInclusionScanner(this.includes, this.excludes);
            scanner.addSourceMapping(new SuffixMapping("JBBP", "jbbp"));
            return scanner.getIncludedSources(this.source, targetDirectory);
        } catch (InclusionScanException ex) {
            throw new MojoExecutionException("Error during sources scanning", ex);
        }
    }

    public void logInfo(@Nullable final String text, final boolean onlyIfVerbose) {
        if (text != null) {
            if (this.verbose) {
                getLog().info(text);
            } else {
                if (!onlyIfVerbose) {
                    getLog().info(text);
                }
            }
        }
    }

    public boolean checkSetNonEmptyWithLogging(@Nonnull final Set<File> files) {
        boolean result = true;
        if (files.isEmpty()) {
            getLog().warn("There is not any detected JBBP script");
            result = false;
        }
        return result;
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip) {
            logInfo("Skip processing", false);
            return;
        }

        if (this.includes.isEmpty()) {
            this.includes.add("**/*.jbbp");
        }

        executeMojo();
    }

    @Nonnull
    public Target findTarget() throws MojoExecutionException {
        final Target theTarget;
        try {
            return Target.valueOf(this.target);
        } catch (IllegalArgumentException ex) {
            throw new MojoExecutionException("Unsupported target : " + this.target, ex);
        }
    }

    protected abstract void executeMojo() throws MojoExecutionException, MojoFailureException;
}
