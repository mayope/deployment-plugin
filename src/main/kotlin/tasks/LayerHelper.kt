package net.mayope.deployplugin.tasks

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.gradle.api.Project

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

private fun Project.getLayers(tag: String): JsonArray<String>? =
    command(listOf("docker", "image", "inspect", tag)).let {
        parseLayers(it)
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

fun Project.findVersionToDeploy(
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
