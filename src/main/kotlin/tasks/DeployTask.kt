package net.mayope.deployplugin.tasks

import net.mayope.deployplugin.ValidatedDeployProfile
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

open class DeployTask : DefaultTask() {

    @Input
    var validatedDeployProfile: ValidatedDeployProfile? = null

    @Input
    var targetNamespace: String = ""

    @Input
    var serviceName: String = ""

    @Input
    @Optional
    var ownDockerImage: Boolean = true

    @InputDirectory
    var helmDir: String = "src/helm"

    @Suppress("TooGenericExceptionCaught")
    private fun Project.queryRemoteVersion(profile: ValidatedDeployProfile,release: String, environment: String): String? {
        return try {
            command(
                listOf("helm", "get", "-n", environment, "values", release),
                environment = kubeconfigEnv(profile)
            )
                .lines()
                .map { it.trim() }
                .firstOrNull { it.startsWith("version:") }
                ?.substring("version: ".length)
        } catch (e: Throwable) {
            println(e)
            null
        }
    }

    private fun kubeconfigEnv(profile: ValidatedDeployProfile) = profile.kubeConfig?.let {
        mapOf("KUBECONFIG" to it)
    }?: emptyMap()

    @SuppressWarnings("SpreadOperator")
    private fun Project.deployForEnvironment(
        profile: ValidatedDeployProfile,
        serviceName: String,
        namespace: String,
    ) {
        if (ownDockerImage) {
            val appVersion = file(dockerVersionFile()).readText()

            val registry = profile.dockerRegistryRoot
            val tag = "$registry/$serviceName:$appVersion"
            val remoteVersion = queryRemoteVersion(profile,serviceName, namespace)
            val remoteTag = "$registry/$serviceName:$remoteVersion"

            println("Deploying chart: $serviceName, currentVersion: $remoteVersion in environment: $namespace")

            val attributesWithImageVersion = findVersionToDeploy(tag, remoteTag, remoteVersion, appVersion).let {
                println("Deploying version: $it of image: $serviceName")
                profile.attributes.plus(mapOf("image.version" to it))
            }
            upgradeChart(profile, attributesWithImageVersion, serviceName, namespace)

        } else {
            upgradeChart(profile, profile.attributes, serviceName, namespace)
        }
    }

    private fun Project.upgradeChart(
        profile: ValidatedDeployProfile,
        attributes: Map<String, String>,
        chartName: String,
        namespace: String) {
        val helmAttributes = attributes.entries.map { "${it.key}=${it.value}" }.joinToString(",")
        val args = arrayOf(
            "helm", "upgrade", "--install", chartName, ".", "--set",
            helmAttributes, "-n", namespace
        )
        logger.info("Executing helm with: ${args.joinToString(" ")}")

        exec {
            it.environment(kubeconfigEnv(profile))
            it.workingDir(helmDir)
            it.commandLine(*args)
        }
        file(deployedChartFile(profile.name, namespace, chartName)).parentFile.mkdirs()
        file(deployedChartFile(profile.name, namespace, chartName)).writeText(helmAttributes)
    }


    @TaskAction
    fun deploy() {
        project.deployForEnvironment(
            validatedDeployProfile ?: error("DeployProfile must be set nonnull"), serviceName, targetNamespace
        )
    }
}
