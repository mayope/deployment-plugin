package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.time.Instant


internal fun Project.dockerVersionFile() = "$buildDir/current_docker_version"

open class DockerBuildTask : DefaultTask() {
    @Input
    var registry: String = ""

    @Input
    var serviceName: String = ""

    @Input
    @Optional
    var versionOverride: String? = null

    @InputDirectory
    var buildDockerDir: String = "${project.buildDir}/buildDocker"

    init {
        outputs.files(project.dockerTagFile(), project.dockerVersionFile())
    }

    private fun Project.buildDocker(serviceName: String) {
        val appVersion = determineVersion()
        val tag = tag(registry, serviceName, appVersion)
        exec {
            it.workingDir(buildDockerDir)
            it.commandLine("docker", "build", ".", "-t", tag)
        }
        file(dockerTagFile()).writeText(tag)
        exec {
            it.commandLine(
                "docker", "tag", file(dockerTagFile()).readText(),
                tagLatest(registry, serviceName)
            )
        }
        file(dockerVersionFile()).writeText(appVersion)
    }

    private fun Project.determineVersion(): String {
        versionOverride?.let {
            return it
        }
        val timestamp = Instant.now().toEpochMilli()
        return "$version-$timestamp"
    }

    @TaskAction
    fun build() {
        project.buildDocker(serviceName)
    }
}
