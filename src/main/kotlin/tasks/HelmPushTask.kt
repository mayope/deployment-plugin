package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class HelmPushTask @Inject constructor(@Input val serviceName: String) :
    DefaultTask() {

    @Input
    var url: String = ""

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

    init {
        outputs.file(project.pushedChartVersion(serviceName))
    }

    @Suppress("SpreadOperator")
    private fun Project.pushChart() {
        val args = if (chartVersion != null) {
            arrayOf(
                "helmpush", ".", url, "-u", userName, "-p",
                password, "-v", chartVersion, "-f"
            )
        } else {
            arrayOf(
                "helmpush", ".", url, "-u", userName, "-p",
                password, "-f",
            )
        }
        logger.info("Executing helmpush with: ${args.joinToString(" ")}")

        providers.exec {
            it.workingDir(helmDir)
            it.commandLine(*args)
        }.result.get()
        file(project.pushedChartVersion(serviceName)).parentFile.mkdirs()
    }

    @TaskAction
    fun deploy() {
        project.pushChart()
    }
}
