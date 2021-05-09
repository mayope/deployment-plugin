package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DeployTask
import net.mayope.deployplugin.tasks.DockerBuildTask
import net.mayope.deployplugin.tasks.DockerLoginTask
import net.mayope.deployplugin.tasks.DockerPushTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy

internal fun Project.registerTasksForProfile(profile: ValidatedDeployProfile, serviceName: String): List<String> {
    val loginTask = registerLoginTask(profile)
    if (serviceName.isBlank()) {
        logger.info(
            "No serviceName given for profile: ${profile.name} in project: ${name}. Only registered login task."
        )
        return emptyList()
    }
    val prepareTask = registerPrepareBuildDockerTask(profile)
    val buildTask = registerBuildDockerTask(profile, prepareTask, serviceName)
    val pushDockerTask = registerPushDockerTask(profile, serviceName, buildTask, loginTask)
    return registerDeployTasks(profile, serviceName, pushDockerTask)
}

private fun Project.registerLoginTask(
    profile: ValidatedDeployProfile
): String {
    val loginTask = "dockerLogin${profile.taskSuffix()}"
    tasks.register(loginTask, DockerLoginTask::class.java) {
        it.host = profile.dockerRegistryRoot
        it.loginMethod = profile.dockerLoginMethod
        it.username = profile.dockerLoginUsername
        it.password = profile.dockerLoginPassword
        it.description =
            "Logs into the dockerRegistry: ${profile.dockerRegistryRoot} using method:" +
                    " ${profile.dockerLoginMethod} on profile ${profile.name}."
        it.awsProfile = profile.awsProfile
    }
    return loginTask
}

private fun Project.registerBuildDockerTask(
    profile: ValidatedDeployProfile,
    prepareDockerTask: String,
    serviceName: String
): String {
    val dockerBuildTask = "dockerBuild${profile.taskSuffix()}"
    tasks.register(dockerBuildTask, DockerBuildTask::class.java) {
        it.registry = profile.dockerRegistryRoot
        it.serviceName = serviceName
        it.description = "Builds the dockerImage of $serviceName service on profile ${profile.name}."
        it.group = "build"
        it.dependsOn(prepareDockerTask)
    }

    tasks.named("build") {
        it.dependsOn(dockerBuildTask)
    }
    return dockerBuildTask
}

private fun Project.registerPrepareBuildDockerTask(profile: ValidatedDeployProfile): String {

    val prepareTask = profile.prepareTask

    if (prepareTask.isNotBlank()) {
        return prepareTask
    }
    val prepareBuildDockerTask = "prepareBuildDocker${profile.taskSuffix()}"
    tasks.register(prepareBuildDockerTask, Copy::class.java) {
        val workingDirectory = "${buildDir}/buildDocker"
        it.dependsOn("bootJar")
        it.from("${buildDir}/libs") {
            it.include("*.*")
        }
        it.from("src/docker") {
            it.include("*")
        }
        it.into(workingDirectory)
    }
    return prepareBuildDockerTask
}

private fun Project.registerPushDockerTask(
    profile: ValidatedDeployProfile,
    serviceName: String,
    dockerLoginTask: String,
    buildDockerTask: String
): String {
    val pushDockerTask = "pushDocker${profile.taskSuffix()}"
    tasks.register(pushDockerTask, DockerPushTask::class.java) {
        it.dependsOn(buildDockerTask, dockerLoginTask)
        it.description = "Pushes the dockerImage of $serviceName service with profile ${profile.name}."
        it.group = "deploy"
        it.serviceName = serviceName
        it.registry = profile.dockerRegistryRoot
    }
    return pushDockerTask
}

private fun Project.registerDeployTasks(
    profile: ValidatedDeployProfile,
    serviceName: String,
    pushDockerTask: String,
): List<String> {

    return profile.targetNamespaces.map {
        registerDeployInNamespaceTask(serviceName, profile, it, pushDockerTask)
    }.also {
        registerProfileDeployTask(serviceName, profile, it)
    }
}

private fun Project.registerProfileDeployTask(serviceName: String,
    profile: ValidatedDeployProfile,
    deployTasks: List<String>) {
    tasks.register("deploy${serviceName.capitalize()}${profile.taskSuffix()}") {
        it.dependsOn(deployTasks)
    }
}

private fun Project.registerDeployInNamespaceTask(serviceName: String,
    profile: ValidatedDeployProfile,
    namespace: String,
    pushDockerTask: String): String {
    val namespaceDeployTask = "deploy${serviceName.capitalize()}${profile.taskSuffix()}${namespace.capitalize()}"
    tasks.register(
        namespaceDeployTask,
        DeployTask::class.java
    ) {
        it.description =
            "Builds and deploys the $serviceName service in namespace: $namespace on profile: ${profile.name}."
        it.group = "deploy"
        it.dependsOn(pushDockerTask)
        it.serviceName = serviceName
        it.validatedDeployProfile = profile
        it.targetNamespace = namespace
    }
    namespaceDeploymentTaskInRootProject(namespace).dependsOn(tasks.findByPath(namespaceDeployTask)!!.path)
    return namespaceDeployTask
}

private fun Project.namespaceDeploymentTaskInRootProject(namespace: String): Task {
    return rootProject.tasks.findByPath("deploy${namespace.capitalize()}") ?: error(
        "task should aready be created"
    )
}
