package com.igormaznitsa.jbbp.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.Set;

public class JBBPPlugin implements Plugin<Project> {
    
    @Override
    public void apply(Project project) {
        project.getTasks().create("jbbp", JBBPTask.class);
    }
}