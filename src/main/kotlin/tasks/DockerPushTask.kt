package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class DockerPushTask @Inject constructor(@Input val serviceName: String) : DefaultTask() {

    @Input
    var registry: String? = null

    init {
        inputs.file(project.dockerTagFile())
        inputs.file(project.dockerNameFile())
        outputs.file(project.dockerPushedTagFile())
        outputs.file(project.dockerPushedRepoFile())
    }

    private fun Project.pushDocker(serviceName: String) {
        val dockerRegistry = registry ?: ""

        exec {
            it.commandLine(
                "docker", "tag", file(dockerTagFile()).readText(),
                tagLatest(dockerRegistry, serviceName)
            )
        }
        val buildTag = file(dockerTagFile()).readText()
        val buildName = file(dockerNameFile()).readText()
        val pushedDockerTag = "$dockerRegistry/$buildTag"
        val pushedDockerRepo = "$dockerRegistry/$buildName"
        exec {
            it.commandLine(
                "docker", "tag", buildTag, pushedDockerTag
            )
        }
        exec {
            it.commandLine("docker", "push", pushedDockerTag)
        }
        exec {
            it.commandLine("docker", "push", tagLatest(dockerRegistry, serviceName))
        }
        file(dockerPushedTagFile()).writeText(pushedDockerTag)
        file(dockerPushedRepoFile()).writeText(pushedDockerRepo)
    }

    @TaskAction
    fun push() {
        project.pushDocker(serviceName)
    }
}
