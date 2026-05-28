package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOutput
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

        providers.exec {
            it.commandLine(
                "docker", "tag", file(dockerTagFile()).readText(),
                tagLatest(dockerRegistry, serviceName)
            )
        }.print()
        val buildTag = file(dockerTagFile()).readText()
        val buildName = file(dockerNameFile()).readText()
        val pushedDockerTag = "$dockerRegistry/$buildTag"
        val pushedDockerRepo = "$dockerRegistry/$buildName"
        providers.exec {
            it.commandLine(
                "docker", "tag", buildTag, pushedDockerTag
            )
        }.print()
        providers.exec {
            it.commandLine("docker", "push", pushedDockerTag)
        }.print()
        providers.exec {
            it.commandLine("docker", "push", tagLatest(dockerRegistry, serviceName))
        }.print()
        file(dockerPushedTagFile()).writeText(pushedDockerTag)
        file(dockerPushedRepoFile()).writeText(pushedDockerRepo)
    }

    @TaskAction
    fun push() {
        project.pushDocker(serviceName)
    }
}

fun ExecOutput.print(){
    println(standardError.asText.get())
    println(standardOutput.asText.get())
}