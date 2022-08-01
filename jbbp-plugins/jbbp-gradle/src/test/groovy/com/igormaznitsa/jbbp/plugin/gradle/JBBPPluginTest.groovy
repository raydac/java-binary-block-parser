import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertNotNull

class JBBPPluginTest {

    @Test
    void demo_plugin_should_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()
        project.tasks.create("jbbpGenerate")
        project.tasks.create("jbbpClean")

        assertNotNull(project.tasks.getByName("jbbpGenerate"))
        assertNotNull(project.tasks.getByName("jbbpClean"))
    }

}