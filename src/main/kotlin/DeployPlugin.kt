package net.mayope.deployplugin

import org.gradle.api.Plugin
import org.gradle.api.Project

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

    private val profileStore = ProfileStore()

    override fun apply(project: Project) {

        project.extensions.create("deploy", DeployExtension::class.java)
        project.extensions.create("deployDefault", DefaultDeployExtension::class.java)


        project.afterEvaluate {
            val defaultDeployExtension = defaultExtension(project)
            val deployExtension = extension(project)
            profileStore.addProfiles(
                defaultDeployExtension.deploymentProfiles(), deployExtension?.profiles ?: emptyList()
            )


            createRootNamespaceTasks(project)

            val serviceName = deployExtension?.serviceName ?: ""

            createProfileTasks(project, serviceName)
        }
    }

    private fun createProfileTasks(project: Project, serviceName: String) {
        profileStore.profiles().flatMap {
            project.registerTasksForProfile(it, serviceName)
        }.let { tasks ->
            if (tasks.isNotEmpty()) {
                project.task("deploy${serviceName.capitalize()}AllProfiles").let {
                    it.dependsOn(tasks)
                }
            }
        }
    }

    private fun createRootNamespaceTasks(project: Project) {
        val usedNamespaces = profileStore.profiles().flatMap { it.targetNamespaces }.distinct()
        project.ensureGlobalNamespaceTasksExists(usedNamespaces)
    }

    private fun Project.ensureGlobalNamespaceTasksExists(namespaces: List<String>) {
        namespaces.filter {
            isMissingInRootProject(it)
        }.forEach {
            registerRootNamespaceTask(it)
        }
    }

    private fun Project.registerRootNamespaceTask(namespace: String) {
        project.rootProject.tasks.register(deployNamespaceName(namespace)) {
            it.group = "deploy"
            it.description = "Deploys all services for the namespace: $namespace"
        }
    }

    private fun Project.isMissingInRootProject(namespace: String) =
        project.rootProject.tasks.none { it.name == deployNamespaceName(namespace) }

    private fun deployNamespaceName(namespace: String) = "deploy${namespace.capitalize()}"

    private fun extension(project: Project) = project.extensions.findByType(DeployExtension::class.java)

    private fun defaultExtension(project: Project) =
        project.rootProject.extensions.findByType(DefaultDeployExtension::class.java) ?: DefaultDeployExtension()
}

