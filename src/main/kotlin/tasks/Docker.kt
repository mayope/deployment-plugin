package net.mayope.deployplugin.tasks

import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File

// docker tag constants
internal fun tagLatest(registry: String, serviceName: String) = "$registry/$serviceName:latest"

internal fun tag(registry: String, serviceName: String, version: String) = "$registry/$serviceName:$version"

// docker file outputs:
internal fun Project.dockerTagFile() = "$buildDir/current_docker_tag"
internal fun Project.dockerPushedTagFile() = "$buildDir/current_pushed_docker_tag"
internal fun Project.dockerVersionFile() = "$buildDir/current_docker_version"
internal fun Project.deployedDockerVersionFile() = "$buildDir/deployed_docker_version"
internal fun Project.deployedIngressesDir() = "$buildDir/ingress/"

internal fun Project.command(cmd: List<String>, workingDirectory: String = ".") =
    ByteArrayOutputStream().also { stream ->
        logger.info("Running command $cmd")
        exec {
            it.commandLine = cmd
            it.standardOutput = stream
            it.workingDir = File(workingDirectory)
        }
    }.run {
        toString().trim()
    }
