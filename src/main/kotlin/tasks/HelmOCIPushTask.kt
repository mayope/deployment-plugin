package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

abstract class HelmOCIPushTask @Inject constructor(@Input val serviceName: String) :
    DefaultTask() {

    @Input
    var registry: String = ""

    @Optional
    @Input
    var userName: String? = null

    @Optional
    @Input
    var password: String? = null

    @Input
    @Optional
    var chartVersion: String? = null

    @InputDirectory
    var helmDir: String = "src/helm"

    @OutputDirectory
    var buildHelmDir: String = "${project.buildDir}/buildHelm"

    init {
        outputs.file(project.pushedChartVersion(serviceName))
    }

    private fun Project.packageChart(): String {
        File(buildHelmDir).mkdirs()

        val filesBefore = File(buildHelmDir).listFiles()?.toSet() ?: error("Could not list files of: $buildHelmDir")
        val args = if (chartVersion != null) {
            arrayOf(
                "helm", "package", helmDir, "-v", chartVersion, "-d", buildHelmDir
            )
        } else {
            arrayOf(
                "helm", "package", helmDir, "-d", buildHelmDir
            )
        }
        logger.info("Executing helm package with: ${args.joinToString(" ")}")

        providers.exec {
            it.workingDir(project.projectDir)
            it.commandLine(*args)
        }.result.get()
        return File(buildHelmDir).listFiles()?.toSet()?.subtract(filesBefore)?.firstOrNull()?.absolutePath
            ?: error("Could not find new file in $buildHelmDir")
    }

    private fun Project.pushChart(packagedChart: String) {
        val args = arrayOf(
            "helm", "push", packagedChart, registry,
        )
        logger.info("Executing helm push with: ${args.joinToString(" ")}")

        providers.exec {
            it.commandLine(*args)
        }.result.get()
        file(project.pushedChartVersion(serviceName)).parentFile.mkdirs()
    }

    @TaskAction
    fun deploy() {
        project.packageChart().let {
            project.pushChart(it)
        }
    }
}