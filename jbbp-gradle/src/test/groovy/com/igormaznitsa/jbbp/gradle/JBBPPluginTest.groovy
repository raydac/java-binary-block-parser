package com.igormaznitsa.jbbp.gradle;

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

public class JBBPPluginTest {

    @Test
    public void demo_plugin_should_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()
        project.getPlugins().apply 'com.igormaznitsa.gradle.jbbp'

        assertTrue(project.tasks.jbbpGenerate instanceof JBBPGenerateTask)
        assertTrue(project.tasks.jbbpClean instanceof JBBPCleanTask)
    }

}