package net.mayope.deployplugin

import org.gradle.api.Project

@Suppress("TooManyFunctions")
internal class ProfileStore {
    fun init(project: Project) {
        addProfiles(
            project.defaultExtension().deploymentProfiles(), project.extension()?.profiles ?: emptyList()
        )
    }

    private val profileMap = mutableMapOf<String, Profile>()

    private fun addProfiles(defaultProfiles: List<Profile>, profiles: List<Profile>) {
        profiles.forEach {
            profileMap[it.name] = it
        }
        defaultProfiles.forEach {
            fillMissingProperties(it)
        }
    }

    fun profiles() = profileMap.values.map { ValidatedProfile(it) }

    private fun fillMissingProperties(profile: Profile) {
        if (profile.name !in profileMap) {
            return
        }
        profileMap[profile.name]!!.let {
            configureProfile(it, profile)
        }
    }

    private fun configureProfile(existing: Profile, profile: Profile) {
        existing.deploy?.configureDeploy(profile)
        existing.dockerBuild?.configureDockerBuild(profile)
        existing.dockerScan?.configureDockerScan(profile)
        existing.helmPush?.configureHelmPush(profile)
        existing.dockerLogin?.configureDockerLogin(profile)
        if (existing.dockerLogin == null && profile.dockerLogin != null) {
            DockerLoginProfile().configureDockerLogin(profile)
        }
        existing.dockerPush?.configureDockerPush(profile)
    }

    private fun DockerBuildProfile.configureDockerBuild(profile: Profile) {
        prepareTask = profile.dockerBuild?.prepareTask ?: prepareTask
        dockerDir = dockerDir ?: profile.dockerBuild?.dockerDir
        version = version ?: profile.dockerBuild?.version
        buildOutputTask = buildOutputTask ?: profile.dockerBuild?.buildOutputTask
    }

    private fun DockerPushProfile.configureDockerPush(profile: Profile) {
        awsProfile = awsProfile ?: profile.dockerPush?.awsProfile
        loginMethod = loginMethod ?: profile.dockerPush?.loginMethod
        registryRoot = registryRoot ?: profile.dockerPush?.registryRoot
        loginUsername = loginUsername ?: profile.dockerPush?.loginUsername
        loginPassword = loginPassword ?: profile.dockerPush?.loginPassword
    }

    private fun DockerLoginProfile.configureDockerLogin(profile: Profile) {
        awsProfile = awsProfile ?: profile.dockerLogin?.awsProfile
        loginMethod = loginMethod ?: profile.dockerLogin?.loginMethod
        registryRoot = registryRoot ?: profile.dockerLogin?.registryRoot
        loginUsername = loginUsername ?: profile.dockerLogin?.loginUsername
        loginPassword = loginPassword ?: profile.dockerLogin?.loginPassword
    }

    private fun DeployProfile.configureDeploy(profile: Profile) {
        attributes = attributes ?: profile.deploy?.attributes
        helmDir = helmDir ?: profile.deploy?.helmDir
        kubeConfig = kubeConfig ?: profile.deploy?.kubeConfig
        targetNamespaces = targetNamespaces ?: profile.deploy?.targetNamespaces
    }

    private fun HelmPushProfile.configureHelmPush(profile: Profile) {
        helmDir = helmDir ?: profile.helmPush?.helmDir
        repositoryUrl = repositoryUrl ?: profile.helmPush?.repositoryUrl
        repositoryUsername = repositoryUsername ?: profile.helmPush?.repositoryUsername
        repositoryPassword = repositoryPassword ?: profile.helmPush?.repositoryPassword
    }

    private fun DockerSecurityScanProfile.configureDockerScan(profile: Profile) {
        failOnThreshold = failOnThreshold ?: profile.dockerScan?.failOnThreshold
        ignoreFilePath = ignoreFilePath ?: profile.dockerScan?.ignoreFilePath
    }
}

private fun Project.extension() = extensions.findByType(DeployExtension::class.java)

private fun Project.defaultExtension() =
    rootProject.extensions.findByType(DefaultDeployExtension::class.java) ?: DefaultDeployExtension()
