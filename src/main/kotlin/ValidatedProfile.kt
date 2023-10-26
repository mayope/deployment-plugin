package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DockerLoginMethod
import net.mayope.deployplugin.tasks.VulnerabilitySeverity

open class ValidatedDockerBuildProfile(profile: DockerBuildProfile) {
    val prepareTask: String? = profile.prepareTask
    val version: String? = profile.version
    val dockerDir: String? = profile.dockerDir
    val architecture: String? = profile.architecture
}

open class ValidatedDockerScanProfile(profile: DockerSecurityScanProfile) {
    val dockerDir: String? = profile.dockerDir
    val failOnThreshold: VulnerabilitySeverity = profile.failOnThreshold ?: VulnerabilitySeverity.HIGH
    val ignoreFilePath: String? = profile.ignoreFilePath
}

open class ValidatedDockerPushProfile(profile: DockerPushProfile) {
    val awsProfile: String? = profile.awsProfile
    val registryRoot: String =
        profile.registryRoot ?: error("Docker registry root not set for profile")
    val loginMethod: DockerLoginMethod = profile.loginMethod ?: DockerLoginMethod.CLASSIC
    val loginUsername: String = profile.loginUsername ?: ""
    val loginPassword: String = profile.loginPassword ?: ""
}

open class ValidatedDockerLoginProfile(profile: DockerLoginProfile) {
    val awsProfile: String? = profile.awsProfile
    val registryRoot: String =
        profile.registryRoot ?: error("Docker registry root has to be set for dockerLogin")
    val loginMethod: DockerLoginMethod = profile.loginMethod ?: DockerLoginMethod.CLASSIC
    val loginUsername: String = profile.loginUsername ?: ""
    val loginPassword: String = profile.loginPassword ?: ""
}

open class ValidatedDeployProfile(profile: DeployProfile) {
    val kubeConfig: String? = profile.kubeConfig
    val kubeContext: String? = profile.kubeContext
    val targetNamespaces: List<String> = profile.targetNamespaces ?: emptyList()
    val attributes: Map<String, String> = profile.attributes ?: emptyMap()
    val helmDir: String = profile.helmDir ?: "src/helm"
    val skipLayerCheck: Boolean? = profile.skipLayerCheck ?: false
}

open class ValidatedHelmPushProfile(profile: HelmPushProfile) {
    val helmDir: String = profile.helmDir ?: "src/helm"
    val repositoryUrl: String =
        profile.repositoryUrl ?: error("HelmRepository url has to be set for helmPush")
    val repositoryUserName: String = profile.repositoryUsername ?: ""
    val repositoryPassword: String = profile.repositoryPassword ?: ""
    val version: String? = null
}

open class ValidatedProfile(profile: Profile) {
    val name = profile.name
    val dockerBuild = profile.dockerBuild?.let {
        ValidatedDockerBuildProfile(it)
    }
    val dockerScan = profile.dockerScan?.let {
        ValidatedDockerScanProfile(it)
    }
    val deploy = profile.deploy?.let {
        ValidatedDeployProfile(it)
    }
    val dockerPush = profile.dockerPush?.let {
        ValidatedDockerPushProfile(it)
    }
    val helmPush = profile.helmPush?.let {
        ValidatedHelmPushProfile(it)
    }
    val dockerLogin = profile.dockerLogin?.let {
        ValidatedDockerLoginProfile(it)
    }

    fun taskSuffix() = if (name == "default") {
        ""
    } else {
        name.capitalize()
    }
}
