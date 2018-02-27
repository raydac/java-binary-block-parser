package com.igormaznitsa.jbbp.plugin.gradle;

import com.igormaznitsa.jbbp.plugin.common.converters.JBBPScriptTranslator;
import org.gradle.api.GradleException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * Task allows to delete generated files.
 *
 * @since 1.3.0
 */
public class JBBPCleanTask extends AbstractJBBPTask {

  @Override
  protected void doTaskAction(@Nonnull final JBBPExtension ext) {
    final JBBPScriptTranslator.Parameters parameters = new JBBPScriptTranslator.Parameters();

    parameters
        .setPackageName(ext.packageName)
        .setOutputDir(ext.output);

    for (final File aScript : findScripts(ext)) {
      getLogger().debug("Script file : " + aScript);
      parameters.setScriptFile(aScript);
      try {
        for (final File f : ext.target.getTranslator().translate(parameters, true)) {
          if (f.isFile()) {
            if (f.delete()) {
              getLogger().info("File " + f + " has been deleted");
            } else {
              getLogger().error("Can't delete file " + f);
            }
          }
        }
      } catch (IOException ex) {
        throw new GradleException("Error during processing JBBP script " + aScript, ex);
      }
    }

  }
}
