package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DeployTask
import net.mayope.deployplugin.tasks.DockerBuildTask
import net.mayope.deployplugin.tasks.DockerLoginTask
import net.mayope.deployplugin.tasks.DockerPushTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

/**
 * Handle docker build and deployment of services to kubernetes using docker and helm
 * configuration:
 *
 * ```
 * deploy {
 *   serviceName = "service"
 *   dockerRegistry = "047797762047.dkr.ecr.eu-central-1.amazonaws.com"
 * }
 * ```
 *
 * will build an docker image consisting of the Dockerfile in src/docker and all other files there plus
 * the output of the bootJar task.
 * If you don't have an bootJar task present you can specify your own prepareTask and copy all stuff needed
 * for the docker build to
 * /build/buildDocker. In this case you have to copy the contents of src/docker by hand.
 *
 * ```
 * deploy {
 *   serviceName = "service"
 *   prepareTask = "nonSpringDockerPrepare"
 *   dockerRegistry = "047797762047.dkr.ecr.eu-central-1.amazonaws.com"
 * }
 * ```
 *
 * This docker image will then be pushed to the docker registry:
 * - 047797762047.dkr.ecr.eu-central-1.amazonaws.com/$serviceName:latest
 * - 047797762047.dkr.ecr.eu-central-1.amazonaws.com/$serviceName:$version
 *
 * The deploy$ServiceName task will first check if the deployed version through helm has identical
 * layers as the locale build version.
 * If they are identical the old version is redeployed and therefore the service is not restarted.
 * if not the new version is deployed.
 *
 * The deployment is execute through a helm upgrade command in the src/helm directory submitting
 * the versionToDeploy as the variable image.version, which you can use in the helm templates.
 *
 */
class DeployPlugin : Plugin<Project> {

    private val dockerLogin = "dockerLogin"
    private val stageBuildDocker = "buildDocker"
    private val stagePushBuildDocker = "pushBuildDocker"

    override fun apply(project: Project) {

        project.extensions.create("deploy", DeployExtension::class.java)
        project.extensions.create("deployDefault", DefaultDeployExtension::class.java)

        project.afterEvaluate {
            val deployExtension = extension(project)
            if (deployExtension == null || deployExtension.serviceName.isBlank()) {
                project.logger.info(
                    "Found no DeployExtension / deploy{}-block, disabling deploy tasks for this project"
                )
            } else {
                registerTasks(project, deployExtension)
            }
        }
    }

    private fun registerTasks(project: Project, deployExtension: DeployExtension) {
        val defaultDeployExtension = defaultExtension(project)

        registerLoginTask(project, deployExtension, defaultDeployExtension)

        val serviceName = deployExtension.serviceName

        val registry =
            deployExtension.dockerRegistryRoot ?: defaultDeployExtension.defaultDockerRegistryRoot ?: error(
                "You have to specify at least one DockerRegistryRoot," +
                        " either in the deploy{} or in the deployDefault{} extension"
            )
        val targetNamespaces =
            deployExtension.targetNamespaces ?: defaultDeployExtension.defaultTargetNamespaces
        val prepareTask = deployExtension.prepareTask ?: defaultDeployExtension.defaultPrepareTask

        val stagePrepareBuildDocker = prepareTask ?: "prepareBuildDocker"

        if (prepareTask == null) {
            project.tasks.register(stagePrepareBuildDocker, Copy::class.java) {
                val workingDirectory = "${project.buildDir}/buildDocker"
                it.dependsOn("bootJar")
                it.from("${project.buildDir}/libs") {
                    it.include("*.*")
                }
                it.from("src/docker") {
                    it.include("*")
                }
                it.into(workingDirectory)
            }
        }

        registerBuildDockerTask(project, serviceName, stagePrepareBuildDocker, registry)

        registerPushDockerTask(project, serviceName, registry)

        val attributes = deployExtension.attributes.plus(defaultDeployExtension.defaultAttributes)
        registerDeployTask(project, serviceName, targetNamespaces, attributes, registry)
    }

    private fun extension(project: Project) = project.extensions.findByType(DeployExtension::class.java)

    private fun defaultExtension(project: Project) =
        project.rootProject.extensions.findByType(DefaultDeployExtension::class.java) ?: DefaultDeployExtension()

    private fun registerDeployTask(
        project: Project,
        serviceName: String,
        targetNamespaces: List<String>,
        attributes: Map<String, String>,
        registry: String
    ) {
        project.tasks.register("deploy${serviceName.capitalize()}")

        targetNamespaces.forEach { namespace ->

            project.tasks.register(
                "deploy${serviceName.capitalize()}${namespace.capitalize()}",
                DeployTask::class.java
            ) {
                it.description = "Builds and deploys the $serviceName service."
                it.group = "deploy"
                it.dependsOn(stagePushBuildDocker)
                it.serviceName = serviceName
                it.attributes = attributes
                it.targetNamespaces = listOf(namespace)
                it.registry = registry
            }
            project.tasks.named("deploy${serviceName.capitalize()}") {
                it.dependsOn("deploy${serviceName.capitalize()}${namespace.capitalize()}")
            }
        }
    }

    private fun registerBuildDockerTask(
        project: Project,
        serviceName: String,
        stagePrepareBuildDocker: String,
        registry: String
    ) {
        project.tasks.register(stageBuildDocker, DockerBuildTask::class.java) {
            it.registry = registry
            it.serviceName = serviceName
            it.description = "Builds the dockerImage of $serviceName service."
            it.group = "build"
            it.dependsOn(stagePrepareBuildDocker)
        }

        project.tasks.named("build") {
            it.dependsOn(stageBuildDocker)
        }
    }

    private fun registerPushDockerTask(
        project: Project,
        serviceName: String,
        registry: String
    ) {
        project.tasks.register(stagePushBuildDocker, DockerPushTask::class.java) {
            it.dependsOn(stageBuildDocker, dockerLogin)
            it.description = "Pushes the dockerImage of $serviceName service."
            it.group = "deploy"
            it.serviceName = serviceName
            it.registry = registry
        }
    }

    private fun registerLoginTask(
        project: Project,
        deployExtension: DeployExtension,
        defaultDeployExtension: DefaultDeployExtension
    ) {
        project.tasks.register(dockerLogin, DockerLoginTask::class.java) {
            it.host = deployExtension.dockerRegistryRoot ?: defaultDeployExtension.defaultDockerRegistryRoot ?: error(
                "you have to define a dockerRegistryRoot either in the deploy{} or defaultDeploy{} block"
            )
            it.loginMethod = deployExtension.dockerLoginMethod ?: defaultDeployExtension.defaultDockerLoginMethod
            it.username = deployExtension.dockerLoginUsername ?: defaultDeployExtension.defaultDockerLoginUsername
            it.password = deployExtension.dockerLoginPassword ?: defaultDeployExtension.defaultDockerLoginPassword
        }
    }
}
