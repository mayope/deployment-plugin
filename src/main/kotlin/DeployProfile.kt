package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DockerLoginMethod

open class DeployProfile(val name: String) {
    var awsProfile: String? = null
    var kubeConfig: String? = null
    var dockerRegistryRoot: String? = null
    var dockerLoginMethod: DockerLoginMethod? = null
    var dockerLoginUsername: String? = null
    var dockerLoginPassword: String? = null
    var targetNamespaces: List<String>? = null
    var prepareTask: String? = null
    var attributes: Map<String, String> = emptyMap()
}

open class ValidatedDeployProfile(profile: DeployProfile) {
    val name = profile.name
    val awsProfile: String? = profile.awsProfile
    val kubeConfig: String? = profile.kubeConfig
    val dockerRegistryRoot: String
    val dockerLoginMethod: DockerLoginMethod
    val dockerLoginUsername: String
    val dockerLoginPassword: String
    val targetNamespaces: List<String>
    val prepareTask: String
    val attributes: Map<String, String>

    init {
        dockerRegistryRoot =
            profile.dockerRegistryRoot ?: error("Docker registry root not set for profile: ${profile.name}")
        dockerLoginMethod = profile.dockerLoginMethod ?: DockerLoginMethod.CLASSIC
        dockerLoginUsername = profile.dockerLoginUsername ?: ""
        dockerLoginPassword = profile.dockerLoginPassword ?: ""
        targetNamespaces = profile.targetNamespaces ?: emptyList()
        prepareTask = profile.prepareTask ?: ""
        attributes = profile.attributes
    }

    fun taskSuffix() = if (name == "default") {
        ""
    } else {
        name.capitalize()
    }

}
