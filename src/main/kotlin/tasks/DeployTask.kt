package net.mayope.deployplugin.tasks

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

open class DeployTask : DefaultTask() {

    @Input
    var registry: String = ""

    @Input
    @Optional
    var targetNamespaces: List<String> = emptyList()

    @Input
    var serviceName: String = ""

    @Input
    @Optional
    var attributes: Map<String, String> = emptyMap()

    @InputDirectory
    var helmDir: String = "src/helm"

    @Suppress("TooGenericExceptionCaught")
    private fun Project.queryRemoteVersion(release: String, environment: String): String? {
        return try {
            command(listOf("helm", "get", "-n", environment, "values", release))
                .lines()
                .map { it.trim() }
                .firstOrNull { it.startsWith("version:") }
                ?.substring("version: ".length)
        } catch (e: Throwable) {
            println(e)
            null
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    private fun Project.tagsHaveEqualLayers(localTag: String, remoteTag: String): Boolean {
        try {
            command(listOf("docker", "image", "pull", remoteTag))
        } catch (e: Throwable) {
            return false
        }

        val remoteLayers = getLayers(remoteTag)
        val localLayers = getLayers(localTag)
        if (remoteLayers == null || localLayers == null) {
            return false
        }

        return remoteLayers == localLayers
    }

    private fun getLayers(tag: String): JsonArray<String>? {
        val content = project.command(listOf("docker", "image", "inspect", tag))
        return parseLayers(content)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLayers(content: String): JsonArray<String>? {
        return (Parser.default().parse(content.byteInputStream()) as JsonArray<JsonObject>).run {
            firstOrNull()
        }?.run {
            obj("RootFS")
        }?.run {
            array("Layers")
        }
    }

    private fun Project.deploy(serviceName: String) {
        for (environment in targetNamespaces) {
            deployForEnvironment(serviceName, environment, attributes)
        }
    }

    @SuppressWarnings("SpreadOperator")
    private fun Project.deployForEnvironment(
        serviceName: String,
        namespace: String,
        attributes: Map<String, String>
    ) {
        val appVersion = file(dockerVersionFile()).readText()

        val tag = "$registry/$serviceName:$appVersion"
        val remoteVersion = queryRemoteVersion(serviceName, namespace)
        val remoteTag = "$registry/$serviceName:$remoteVersion"

        println("Deploying service: $serviceName, currentVersion: $remoteVersion in environment: $namespace")

        val versionToDeploy = findVersionToDeploy(tag, remoteTag, remoteVersion, appVersion)

        val helmAttributes = attributes.entries.map { "${it.key}=${it.value}" }.joinToString(",")
        val args = arrayOf(
            "helm", "upgrade", "--install", serviceName, ".", "--set",
            "image.version=$versionToDeploy,$helmAttributes", "-n", namespace
        )
        logger.info("Executing helm with: ${args.joinToString(" ")}")

        exec {
            it.workingDir(helmDir)
            it.commandLine(*args)
        }

        println("Deployed version: $versionToDeploy of service: $serviceName")
        file(deployedDockerVersionFile()).writeText(versionToDeploy)
    }


    private fun Project.findVersionToDeploy(
        tag: String,
        remoteTag: String,
        remoteVersion: String?,
        appVersion: String
    ): String {
        return if (tagsHaveEqualLayers(tag, remoteTag) && remoteVersion != null) {
            logger.info("Local version has same layers, deploying existing version.")
            println("Deploying existing version: $appVersion")
            remoteVersion
        } else {
            logger.info("Local version has different layers, deploying new version.")
            println("Deploying new version: $appVersion")
            appVersion
        }
    }

    @TaskAction
    fun deploy() {
        project.deploy(serviceName)
    }
}
