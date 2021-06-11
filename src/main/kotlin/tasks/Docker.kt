package net.mayope.deployplugin.tasks

import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File

// docker tag constants
internal fun tagLatest(registry: String, serviceName: String) = "$registry/$serviceName:latest"

internal fun tag(registry: String, serviceName: String, version: String) = if (registry.isBlank()) {
    "$serviceName:$version"
} else {
    "$registry/$serviceName:$version"
}

// docker file outputs:
internal fun Project.dockerVersionFile() = "$buildDir/deploy/docker_version"
internal fun Project.dockerTagFile() = "$buildDir/deploy/build_docker_tag"
internal fun Project.dockerNameFile() = "$buildDir/deploy/build_docker_name"
internal fun Project.dockerPushedTagFile() = "$buildDir/deploy/pushed_docker_tag"
internal fun Project.dockerPushedRepoFile() = "$buildDir/deploy/pushed_docker_repo"
internal fun Project.pushedChartVersion(chartName: String) =
    "$buildDir/deploy/${chartName}_pushed_chart_version"

internal fun Project.deployedChartFile(serviceName: String, namespace: String, chartName: String) =
    "$buildDir/deploy/${chartName}_${serviceName}_${namespace}_deployed_chart_values"

internal fun Project.command(
    cmd: List<String>,
    workingDirectory: String = ".",
    environment: Map<String, String> = emptyMap()
) =
    ByteArrayOutputStream().also { stream ->
        logger.info("Running command $cmd")
        exec {
            it.environment.putAll(environment)
            it.commandLine = cmd
            it.standardOutput = stream
            it.workingDir = File(workingDirectory)
        }
    }.run {
        toString().trim()
    }
