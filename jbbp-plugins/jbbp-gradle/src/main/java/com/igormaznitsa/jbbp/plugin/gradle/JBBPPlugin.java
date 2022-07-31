package com.igormaznitsa.jbbp.plugin.gradle;

import javax.annotation.Nonnull;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JBBPPlugin implements Plugin<Project> {

  @Override
  public void apply(@Nonnull final Project project) {
    project.getExtensions().create(JBBPExtension.EXT_NAME, JBBPExtension.class, project);

    JBBPExtension extension = project.getExtensions().create(JBBPExtension.class, JBBPExtension.EXT_NAME, JBBPExtension.class, project);
    project.getTasks().register("jbbpGenerate", JBBPGenerateTask.class, task -> {
      task.setDescription("Generate JBBP stuff.");
    });
    project.getTasks().register("jbbpClean", JBBPCleanTask.class, task -> {
      task.setDescription("Clean all JBBP stuff.");
    });
  }
}