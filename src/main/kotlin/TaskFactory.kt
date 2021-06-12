package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DeployTask
import net.mayope.deployplugin.tasks.DockerBuildTask
import net.mayope.deployplugin.tasks.DockerLoginTask
import net.mayope.deployplugin.tasks.DockerPushTask
import net.mayope.deployplugin.tasks.HelmPushTask
import net.mayope.deployplugin.tasks.dockerPushedTagFile
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy

internal fun Project.registerTasksForProfile(profile: ValidatedProfile, serviceName: String): List<String> {
    profile.dockerLogin?.let {
        registerLoginTask(it, profile.taskSuffix())
    }
    val buildDockerTask = profile.dockerBuild?.let {
        registerDockerBuildTask(it, serviceName, profile.taskSuffix())
    }
    val pushDockerTask = profile.dockerPush?.let {
        if (buildDockerTask == null) {
            error("Docker Push needs a dockerBuild first")
        }
        registerDockerPushTask(it, serviceName, profile.taskSuffix(), buildDockerTask)
    }
    val deployTasks = profile.deploy?.let {
        registerDeployTasks(it, serviceName, profile.taskSuffix(), pushDockerTask)
    } ?: emptyList()

    val helmPushTasks = profile.helmPush?.let {
        registerHelmPushTask(it, serviceName, profile.taskSuffix())
    }

    return deployTasks.plus(listOfNotNull(pushDockerTask, helmPushTasks))
}

private fun Project.registerLoginTask(
    profile: ValidatedDockerLoginProfile,
    taskSuffix: String
): String {
    val loginTask = "dockerLogin$taskSuffix"
    tasks.register(loginTask, DockerLoginTask::class.java) {
        it.host = profile.registryRoot
        it.loginMethod = profile.loginMethod
        it.username = profile.loginUsername
        it.password = profile.loginPassword
        it.description =
            "Logs into the dockerRegistry: ${profile.registryRoot} using method:" +
                    " ${profile.loginMethod}"
        it.awsProfile = profile.awsProfile
    }
    return loginTask
}

private fun Project.registerDockerBuildTask(
    profile: ValidatedDockerBuildProfile,
    serviceName: String,
    taskSuffix: String,
): String {
    val prepareTask = registerPrepareBuildDockerTask(profile, taskSuffix)
    val dockerBuildTask = "dockerBuild$taskSuffix${serviceName.capitalize()}"
    tasks.register(dockerBuildTask, DockerBuildTask::class.java) {
        it.serviceName = serviceName
        it.description = "Builds the dockerImage of $serviceName service."
        it.group = "build"
        if (profile.dockerDir != null) {
            it.buildDockerDir = profile.dockerDir
        }
        it.versionOverride = profile.version
        it.dependsOn(prepareTask)
    }

    tasks.named("build") {
        it.dependsOn(dockerBuildTask)
    }
    return dockerBuildTask
}

private fun Project.registerHelmPushTask(
    profile: ValidatedHelmPushProfile,
    serviceName: String,
    taskSuffix: String,
): String {
    val task = "pushChart$taskSuffix${serviceName.capitalize()}"
    tasks.register(task, HelmPushTask::class.java, serviceName).configure {
        it.description = "Pushes the helmChart of $serviceName service."
        it.group = "deploy"
        it.helmDir = profile.helmDir
        it.url = profile.repositoryUrl
        it.password = profile.repositoryPassword
        it.userName = profile.repositoryUserName
        it.chartVersion = profile.version
    }

    return task
}

private fun Project.registerPrepareBuildDockerTask(profile: ValidatedDockerBuildProfile, taskSuffix: String): String {

    val prepareTask = profile.prepareTask

    prepareTask?.let {
        return it
    }
    val prepareBuildDockerTask = "prepareBuildDocker$taskSuffix"
    tasks.register(prepareBuildDockerTask, Copy::class.java) {
        val workingDirectory = "$buildDir/buildDocker"
        it.dependsOn("bootJar")
        it.from("$buildDir/libs") {
            it.include("*.*")
        }
        it.from("src/docker") {
            it.include("*")
        }
        it.into(workingDirectory)
    }
    return prepareBuildDockerTask
}

private fun Project.registerDockerPushTask(
    profile: ValidatedDockerPushProfile,
    serviceName: String,
    taskSuffix: String,
    buildDockerTask: String,
): String {
    val loginTask = "dockerLogin$taskSuffix"
    if (tasks.findByName(loginTask) == null) {
        tasks.register(loginTask, DockerLoginTask::class.java) {
            it.host = profile.registryRoot
            it.loginMethod = profile.loginMethod
            it.username = profile.loginUsername
            it.password = profile.loginPassword
            it.description =
                "Logs into the dockerRegistry: ${profile.registryRoot} using method:" +
                        " ${profile.loginMethod}"
            it.awsProfile = profile.awsProfile
        }
    }
    val pushDockerTask = "pushDocker$taskSuffix${serviceName.capitalize()}"
    tasks.register(pushDockerTask, DockerPushTask::class.java, serviceName).configure {
        it.dependsOn(buildDockerTask, loginTask)
        it.description = "Pushes the dockerImage of $serviceName service."
        it.group = "deploy"
        it.registry = profile.registryRoot
    }
    return pushDockerTask
}

private fun Project.registerDeployTasks(
    profile: ValidatedDeployProfile,
    serviceName: String,
    taskSuffix: String,
    pushDockerTask: String?,
): List<String> {

    return profile.targetNamespaces.map {
        registerDeployInNamespaceTask(profile, serviceName, it, pushDockerTask, taskSuffix)
    }.also {
        registerProfileDeployTask(serviceName, taskSuffix, it)
    }
}

private fun Project.registerProfileDeployTask(
    serviceName: String,
    taskSuffix: String,
    deployTasks: List<String>
) {
    tasks.register("deploy${serviceName.capitalize()}$taskSuffix") {
        it.dependsOn(deployTasks)
    }
}

private fun Project.registerDeployInNamespaceTask(
    profile: ValidatedDeployProfile,
    serviceName: String,
    namespace: String,
    pushDockerTask: String?,
    taskSuffix: String
): String {
    val namespaceDeployTask = "deploy${serviceName.capitalize()}${taskSuffix}${namespace.capitalize()}"
    val pushedTagFile = if (pushDockerTask != null) {
        project.dockerPushedTagFile()
    } else {
        null
    }
    tasks.register(
        namespaceDeployTask,
        DeployTask::class.java, serviceName, pushedTagFile
    ).configure {
        it.description =
            "Builds and deploys the $serviceName service in namespace: $namespace ."
        it.group = "deploy"
        if (pushDockerTask != null) {
            it.dependsOn(pushDockerTask)
        }
        it.attributes = profile.attributes
        it.helmDir = profile.helmDir
        it.kubeConfig = profile.kubeConfig
        it.targetNamespace = namespace
        it.skipLayerCheck = profile.skipLayerCheck
    }
    namespaceDeploymentTaskInRootProject(namespace).dependsOn(tasks.findByPath(namespaceDeployTask)!!.path)
    return namespaceDeployTask
}

private fun Project.namespaceDeploymentTaskInRootProject(namespace: String): Task {
    return rootProject.tasks.findByPath("deploy${namespace.capitalize()}") ?: error(
        "task should aready be created"
    )
}
