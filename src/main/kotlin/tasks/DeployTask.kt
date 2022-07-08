package net.mayope.deployplugin.tasks

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayInputStream
import javax.inject.Inject

abstract class DeployTask @Inject constructor(
    @Input val serviceName: String,
) :
    DefaultTask() {

    @Input
    var targetNamespace: String = ""

    @Input
    @Optional
    var pushedTagFile: String? = null

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
    private fun Project.queryRemoteTag(release: String, environment: String): Pair<String, String>? {
        return try {
            val values = command(
                listOf("helm", "get", "-n", environment, "values", release, "-a", "-o", "json"),
                environment = kubeconfigEnv()
            ).let {
                Parser.default().parse(ByteArrayInputStream(it.toByteArray())) as JsonObject
            }.obj("image") ?: error("deployed image not found in json")
            val version = values.string("version") ?: error("image version not found in json")
            val repository = values.string("repository") ?: error("image repository not found in json")
            repository to version
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
        updateHelmDependencies()
        if (pushedTagFile != null) {
            val appVersion = file(dockerVersionFile()).readText()
            val appRepo = file(dockerPushedRepoFile()).readText()

            val tag = file(pushedTagFile).readText(Charsets.UTF_8)
            val (remoteRepository, remoteVersion) = queryRemoteTag(serviceName, namespace) ?: "" to ""

            println("Deploying chart: $serviceName, currentVersion: $remoteVersion in environment: $namespace")

            val attributesWithImageVersion =
                addImageVersion(
                    tag, "$remoteRepository:$remoteVersion", remoteVersion, appVersion, serviceName, appRepo
                )
            upgradeChart(attributesWithImageVersion, serviceName, namespace)
        } else {
            upgradeChart(attributes, serviceName, namespace)
        }
    }
    @Suppress("LongParameterList")
    private fun Project.addImageVersion(
        tag: String,
        remoteTag: String,
        remoteVersion: String,
        appVersion: String,
        serviceName: String,
        appRepo: String
    ): Map<String, String> {
        if (skipLayerCheck == true) {
            return attributes.plus(mapOf("image.version" to appVersion, "image.repository" to appRepo))
        }
        return findVersionToDeploy(tag, remoteTag, remoteVersion, appVersion).let {
            println("Deploying version: $it of image: $serviceName")
            attributes.plus(mapOf("image.version" to it, "image.repository" to appRepo))
        }
    }

    private fun Project.updateHelmDependencies() {

        logger.info("Updating helm dependencies")

        exec {
            it.environment(kubeconfigEnv())
            it.workingDir(helmDir)
            it.commandLine("helm", "dependency", "update")
        }
    }

    @Suppress("SpreadOperator")
    private fun Project.upgradeChart(
        attributes: Map<String, String>,
        chartName: String,
        namespace: String
    ) {
        val helmAttributes = attributes.entries.map { "${it.key}=${it.value}" }.joinToString(",")
        val args = if (helmAttributes.isNotEmpty()) {
            arrayOf(
                "helm", "upgrade", "--install", chartName, ".", "--set",
                helmAttributes, "-n", namespace
            )
        } else {
            arrayOf(
                "helm", "upgrade", "--install", chartName, ".", "-n", namespace
            )
        }
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
