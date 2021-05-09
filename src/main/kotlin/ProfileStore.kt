package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DockerLoginMethod

internal class ProfileStore {
    private val profileMap = mutableMapOf<String, DeployProfile>()

    fun addProfiles(defaultProfiles: List<DeployProfile>, profiles: List<DeployProfile>) {

        defaultProfiles.forEach {
            addProfile(it)
        }
        profiles.forEach {
            addProfile(it)
        }
    }

    fun profiles() = profileMap.values.map { ValidatedDeployProfile(it) }

    private fun addProfile(profile: DeployProfile) {
        if (profile.name !in profileMap) {
            profileMap[profile.name] = DeployProfile(profile.name)
        }
        profileMap[profile.name]!!.let {
            it.awsProfile = profile.awsProfile ?: it.awsProfile
            it.kubeConfig = profile.kubeConfig ?: it.kubeConfig
            it.dockerRegistryRoot = profile.dockerRegistryRoot ?: it.dockerRegistryRoot
            it.dockerLoginMethod =
                profile.dockerLoginMethod ?: it.dockerLoginMethod ?: DockerLoginMethod.CLASSIC
            it.dockerLoginUsername = profile.dockerLoginUsername ?: it.dockerLoginUsername
            it.dockerLoginPassword = profile.dockerLoginPassword ?: it.dockerLoginPassword
            it.targetNamespaces = profile.targetNamespaces ?: it.targetNamespaces
            it.prepareTask = profile.prepareTask ?: it.prepareTask
            it.attributes = it.attributes.plus(profile.attributes)
        }

    }
}
