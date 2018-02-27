package com.igormaznitsa.jbbp.plugin.gradle;

import com.igormaznitsa.meta.common.utils.GetUtils;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractJBBPTask extends DefaultTask {

  public AbstractJBBPTask() {
    super();
  }

  @Nullable
  public static String getTextOrFileContent(@Nonnull final JBBPExtension extension, @Nullable final String text, @Nullable final File file) {
    String result = null;
    if (text != null) {
      result = text;
    } else if (file != null) {
      try {
        result = FileUtils.readFileToString(file, GetUtils.ensureNonNull(extension.inEncoding, "UTF-8"));
      } catch (IOException ex) {
        throw new GradleException("Can't read file " + file, ex);
      }
    }
    return result;
  }

  @Nonnull
  protected static Set<File> findScripts(@Nonnull final JBBPExtension ext) {
    final Set<File> result = new HashSet<File>();

    ext.source.visit(new FileVisitor() {
      @Override
      public void visitDir(final FileVisitDetails fileVisitDetails) {

      }

      @Override
      public void visitFile(final FileVisitDetails fileVisitDetails) {
        result.add(fileVisitDetails.getFile());
      }
    });
    return result;
  }

  @TaskAction
  public final void doAction() {
    JBBPExtension ext = getProject().getExtensions().findByType(JBBPExtension.class);
    if (ext == null) {
      ext = new JBBPExtension(getProject());
    }
    ext.prepare(getProject());
    doTaskAction(ext);
  }

  protected abstract void doTaskAction(@Nonnull JBBPExtension extension);

}
