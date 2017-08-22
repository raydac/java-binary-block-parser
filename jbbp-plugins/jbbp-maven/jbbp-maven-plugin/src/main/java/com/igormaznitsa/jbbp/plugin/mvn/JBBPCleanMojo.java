package com.igormaznitsa.jbbp.plugin.mvn;

import com.igormaznitsa.jbbp.plugin.common.converters.Target;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Set;

/**
 * The Mojo looks for all java files generated for JBBP scripts and delete them.
 *
 * @author Igor Maznitsa
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true)
public class JBBPCleanMojo extends AbstractJBBPMojo {

    @Override
    public void executeMojo() throws MojoExecutionException, MojoFailureException {
        final Set<File> scripts = findSources(this.outputDirectory);
        if (checkSetNonEmptyWithLogging(scripts)) {
            int counter = 0;
            final Target target = findTarget();
            for (final File aScript : scripts) {
                getLog().debug("Processing JBBP script : " + aScript);
                final Set<File> files = target.getScriptProcessor().makeTargetFiles(this.getOutputDirectory(), this.getCommonPackage(), aScript);
                for (final File f : files) {
                    if (f.isFile()) {
                        if (f.delete()) {
                            counter++;
                            logInfo("Deleted file '" + f.getAbsolutePath() + "' for script '" + aScript + "'", true);
                        } else {
                            getLog().error("Can't delete file '" + f.getAbsolutePath() + "' for script '" + aScript + "'");
                        }
                    } else {
                        getLog().debug("File '" + f.getAbsolutePath() + "' generated for script '" + aScript + "' is not found");
                    }
                }
            }
            getLog().info(String.format("Deleted %d file(s)", counter));
        }
    }
}
