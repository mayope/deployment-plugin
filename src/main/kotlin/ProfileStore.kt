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

    private fun fillMissingProperties(defaultProfile: Profile) {
        profileMap[defaultProfile.name]?.let {
            configureProfile(it, defaultProfile)
        }
    }

    private fun configureProfile(existing: Profile, defaultProfile: Profile) {
        existing.deploy?.configureDeploy(defaultProfile)
        existing.dockerBuild?.configureDockerBuild(defaultProfile)
        existing.dockerScan?.configureDockerScan(defaultProfile)
        existing.helmPush?.configureHelmPush(defaultProfile)
        existing.helmOCIPush?.configureHelmOCIPush(defaultProfile)
        existing.dockerLogin?.configureDockerLogin(defaultProfile)
        if (existing.dockerLogin == null && defaultProfile.dockerLogin != null) {
            DockerLoginProfile().configureDockerLogin(defaultProfile)
        }
        existing.dockerPush?.configureDockerPush(defaultProfile)
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
        kubeContext = kubeContext ?: profile.deploy?.kubeContext
        targetNamespaces = targetNamespaces ?: profile.deploy?.targetNamespaces
        valuesFiles = valuesFiles ?: profile.deploy?.valuesFiles
    }

    private fun HelmPushProfile.configureHelmPush(profile: Profile) {
        helmDir = helmDir ?: profile.helmPush?.helmDir
        repositoryUrl = repositoryUrl ?: profile.helmPush?.repositoryUrl
        repositoryUsername = repositoryUsername ?: profile.helmPush?.repositoryUsername
        repositoryPassword = repositoryPassword ?: profile.helmPush?.repositoryPassword
    }

    private fun HelmOCIPushProfile.configureHelmOCIPush(profile: Profile) {
        helmDir = helmDir ?: profile.helmOCIPush?.helmDir
        registryRoot = registryRoot ?: profile.helmOCIPush?.registryRoot
        loginUsername = loginUsername ?: profile.helmOCIPush?.loginUsername
        loginPassword = loginPassword ?: profile.helmOCIPush?.loginPassword
    }

    private fun DockerSecurityScanProfile.configureDockerScan(profile: Profile) {
        failOnThreshold = failOnThreshold ?: profile.dockerScan?.failOnThreshold
        ignoreFilePath = ignoreFilePath ?: profile.dockerScan?.ignoreFilePath
    }
}

private fun Project.extension() = extensions.findByType(DeployExtension::class.java)

private fun Project.defaultExtension() =
    rootProject.extensions.findByType(DefaultDeployExtension::class.java) ?: DefaultDeployExtension()
