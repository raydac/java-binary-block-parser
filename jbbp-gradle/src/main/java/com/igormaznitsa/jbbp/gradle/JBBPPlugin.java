package com.igormaznitsa.jbbp.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JBBPPlugin implements Plugin<Project> {
    
    @Override
    public void apply(Project project) {
        project.getTasks().create("jbbpGenerate", JBBPGenerateTask.class);
        project.getTasks().create("jbbpClean", JBBPCleanTask.class);
    }
}