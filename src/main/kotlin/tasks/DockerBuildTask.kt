package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.time.Instant

open class DockerBuildTask : DefaultTask() {
    @Input
    var serviceName: String = ""

    @Input
    @Optional
    var versionOverride: String? = null

    @Input
    @Optional
    var architecture: String? = null

    @InputDirectory
    var buildDockerDir: String = "${project.buildDir}/buildDocker"

    init {
        outputs.files(project.dockerTagFile(), project.dockerVersionFile(), project.dockerNameFile())
    }

    private fun Project.buildDocker() {
        val appVersion = determineVersion()
        val tag = tag("", serviceName, appVersion)
        exec {
            it.workingDir(buildDockerDir)
            it.commandLine("docker", "build", ".", "-t", tag, getPlatform())
        }
        file(dockerTagFile()).writeText(tag)
        file(dockerVersionFile()).writeText(appVersion)
        file(dockerNameFile()).writeText(serviceName)
    }

    private fun Project.determineVersion(): String {
        versionOverride?.let {
            return it
        }
        val timestamp = Instant.now().toEpochMilli()
        return "$version-$timestamp"
    }

    private fun Project.getPlatform(): String {
        val platform = ""
        if(architecture) {
            platform = "--platform " + architecture
        }
        return platform
    }

    @TaskAction
    fun build() {
        project.buildDocker()
    }
}
