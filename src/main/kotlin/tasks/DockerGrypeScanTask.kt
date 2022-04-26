package net.mayope.deployplugin.tasks


import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

enum class VulnerabilitySeverity(val value: String){
    LOW("low"), MEDIUM("medium"), HIGH("high"), CRITICAL("critical")
}

open class DockerGrypeScanTask: DefaultTask(){

    @Input
    var serviceName: String = ""

    @Input
    var failOnThreshold: VulnerabilitySeverity = VulnerabilitySeverity.MEDIUM

    @Input
    var ignoreFilePath: String? = project.path

    @InputDirectory
    var buildDockerDir: String = "${project.buildDir}/buildDocker"

    private fun Project.securityScan() {
        exec{
            it.workingDir(buildDockerDir)
            it.commandLine("docker", "run", "-it", "-v", "$buildDockerDir/$serviceName:/var/$serviceName",
                "-v", "$ignoreFilePath/grype.yaml:/.grype.yaml", "anchore/grype:latest", "dir:/var/$serviceName",
                "--fail-on", failOnThreshold.value)
        }
    }

    @TaskAction
    fun securityScan() {
        project.securityScan()
    }


}