package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class DockerPushTask : DefaultTask() {
    @Input
    var serviceName: String = ""
    @Input
    var registry: String = ""

    init {
        inputs.file(project.dockerTagFile())
        outputs.file(project.dockerPushedTagFile())
    }

    private fun Project.pushDocker(serviceName: String) {

        exec {
            it.commandLine("docker", "push", file(dockerTagFile()).readText())
        }
        exec {
            it.commandLine("docker", "push", tagLatest(registry, serviceName))
        }
        file(dockerPushedTagFile()).writeText(file(dockerTagFile()).readText())
    }

    @TaskAction
    fun push() {
        project.pushDocker(serviceName)
    }
}
