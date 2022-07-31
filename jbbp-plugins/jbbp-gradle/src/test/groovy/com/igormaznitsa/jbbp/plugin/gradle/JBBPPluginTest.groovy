import com.igormaznitsa.jbbp.plugin.gradle.JBBPCleanTask
import com.igormaznitsa.jbbp.plugin.gradle.JBBPGenerateTask
import com.igormaznitsa.jbbp.plugin.gradle.JBBPPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue

class JBBPPluginTest {

    @Test
    void demo_plugin_should_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()
        project.getPlugins().apply(JBBPPlugin)

        assertTrue(project.tasks.jbbpGenerate instanceof JBBPGenerateTask)
        assertTrue(project.tasks.jbbpClean instanceof JBBPCleanTask)
    }

}