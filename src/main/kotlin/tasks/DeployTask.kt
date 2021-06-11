package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class DeployTask @Inject constructor(
    @Input val serviceName: String,
    @Input @Optional val pushedTagFile: String? = null,
) :
    DefaultTask() {

    @Input
    var targetNamespace: String = ""

    @Input
    @Optional
    var kubeConfig: String? = null

    @Input
    @Optional
    var skipLayerCheck: Boolean? = null

    @Input
    @Optional
    var attributes: Map<String, String> = emptyMap()

    @InputDirectory
    var helmDir: String = "src/helm"

    init {
        if (pushedTagFile != null) {
            inputs.file(project.dockerVersionFile())
            inputs.file(project.dockerPushedTagFile())
            inputs.file(project.dockerPushedRepoFile())
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun Project.queryRemoteTag(release: String, environment: String): String? {
        return try {
            val values = command(
                listOf("helm", "get", "-n", environment, "values", release, "-a"),
                environment = kubeconfigEnv()
            ).lines()
                .map { it.trim() }
            val tag = values.firstOrNull { it.startsWith("version:") }
                ?.substring("version: ".length)
            val repository = values.firstOrNull { it.startsWith("repository:") }
                ?.substring("repository: ".length)
            "$repository:$tag"
        } catch (e: Throwable) {
            println(e)
            null
        }
    }

    private fun kubeconfigEnv() = kubeConfig?.let {
        mapOf("KUBECONFIG" to it)
    } ?: emptyMap()

    @SuppressWarnings("SpreadOperator")
    private fun Project.deployForEnvironment(
        serviceName: String,
        namespace: String,
    ) {
        if (pushedTagFile != null) {
            val appVersion = file(dockerVersionFile()).readText()
            val appRepo = file(dockerPushedRepoFile()).readText()

            val tag = file(pushedTagFile).readText(Charsets.UTF_8)
            val remoteTag = queryRemoteTag(serviceName, namespace) ?: ""

            println("Deploying chart: $serviceName, currentVersion: $remoteTag in environment: $namespace")

            val attributesWithImageVersion = addImageVersion(tag, remoteTag, appVersion, serviceName, appRepo)
            upgradeChart(attributesWithImageVersion, serviceName, namespace)
        } else {
            upgradeChart(attributes, serviceName, namespace)
        }
    }

    private fun Project.addImageVersion(
        tag: String,
        remoteTag: String,
        appVersion: String,
        serviceName: String,
        appRepo: String
    ): Map<String, String> {
        if (skipLayerCheck == true) {
            return attributes.plus(mapOf("image.version" to appVersion, "image.repository" to appRepo))
        }
        return findVersionToDeploy(tag, remoteTag, remoteTag, appVersion).let {
            println("Deploying version: $it of image: $serviceName")
            attributes.plus(mapOf("image.version" to it, "image.repository" to appRepo))
        }
    }

    @Suppress("SpreadOperator")
    private fun Project.upgradeChart(
        attributes: Map<String, String>,
        chartName: String,
        namespace: String
    ) {
        val helmAttributes = attributes.entries.map { "${it.key}=${it.value}" }.joinToString(",")
        val args = arrayOf(
            "helm", "upgrade", "--install", chartName, ".", "--set",
            helmAttributes, "-n", namespace
        )
        logger.info("Executing helm with: ${args.joinToString(" ")}")

        exec {
            it.environment(kubeconfigEnv())
            it.workingDir(helmDir)
            it.commandLine(*args)
        }
        file(deployedChartFile(serviceName, namespace, chartName)).parentFile.mkdirs()
        file(deployedChartFile(serviceName, namespace, chartName)).writeText(helmAttributes)
    }

    @TaskAction
    fun deploy() {
        project.deployForEnvironment(serviceName, targetNamespace)
    }
}
