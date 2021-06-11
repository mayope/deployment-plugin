package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class HelmPushTask @Inject constructor(@Input val serviceName: String) :
    DefaultTask() {

    @Input
    var url: String = ""

    @Input
    var userName: String? = null

    @Input
    var password: String? = null

    @Input
    var chartVersion: String = project.version.toString()

    @InputDirectory
    var helmDir: String = "src/helm"

    init {
        outputs.file(project.pushedChartVersion(serviceName))
    }

    @Suppress("SpreadOperator")
    private fun Project.pushChart() {
        val args = arrayOf(
            "helmpush", ".", url, "-u", userName, "-p",
            password, "-v", chartVersion
        )
        logger.info("Executing helmpush with: ${args.joinToString(" ")}")

        exec {
            it.workingDir(helmDir)
            it.commandLine(*args)
        }
        file(project.pushedChartVersion(serviceName)).parentFile.mkdirs()
        file(project.pushedChartVersion(serviceName)).writeText(chartVersion)
    }

    @TaskAction
    fun deploy() {
        project.pushChart()
    }
}
