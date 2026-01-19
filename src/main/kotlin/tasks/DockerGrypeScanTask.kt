package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

enum class VulnerabilitySeverity(val value: String) {
    LOW("low"), MEDIUM("medium"), HIGH("high"), CRITICAL("critical")
}

open class DockerGrypeScanTask : DefaultTask() {

    @Input
    var serviceName: String = ""

    @Input
    var failOnThreshold: VulnerabilitySeverity = VulnerabilitySeverity.MEDIUM

    @Input
    @Optional
    var ignoreFilePath: String? = "${project.path}/grype.yaml"

    init {
        outputs.files(project.securityScanFile())
    }

    @InputDirectory
    var buildDockerDir: String = "${project.buildDir}/buildDocker"

    private fun Project.securityScan() {
        ByteArrayOutputStream().use { os ->
            providers.exec {
                it.workingDir(buildDockerDir)
                it.commandLine(
                    "docker", "run", "-t", "-v", "$buildDockerDir/$serviceName:/var/$serviceName",
                    "-v", "$ignoreFilePath/grype.yaml:/.grype.yaml", "anchore/grype:latest", "dir:/var/$serviceName",
                    "--fail-on", failOnThreshold.value
                )
                it.standardOutput = os
            }.result.get()
            file(securityScanFile()).writeText(os.toString(Charsets.UTF_8).trim())
        }
    }

    @TaskAction
    fun securityScan() {
        project.securityScan()
    }
}
