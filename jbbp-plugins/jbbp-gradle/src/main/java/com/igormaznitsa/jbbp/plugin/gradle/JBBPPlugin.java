package com.igormaznitsa.jbbp.plugin.gradle;

import javax.annotation.Nonnull;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JBBPPlugin implements Plugin<Project> {

  @Override
  public void apply(@Nonnull final Project project) {
    project.getExtensions().create(JBBPExtension.EXT_NAME, JBBPExtension.class, project);
    project.getTasks().create("jbbpGenerate", JBBPGenerateTask.class);
    project.getTasks().create("jbbpClean", JBBPCleanTask.class);
  }
}