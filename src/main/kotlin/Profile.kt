package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DockerLoginMethod
import net.mayope.deployplugin.tasks.VulnerabilitySeverity

open class DeployProfile {
    var helmDir: String? = null
    var kubeConfig: String? = null
    var targetNamespaces: List<String>? = null
    var attributes: Map<String, String>? = null
    var skipLayerCheck: Boolean? = null
}

open class DockerBuildProfile {
    var prepareTask: String? = null
    var buildOutputTask: String? = null
    var version: String? = null
    var dockerDir: String? = null
}

open class DockerSecurityScanProfile {
    var dockerDir: String? = null
    var failOnThreshold: VulnerabilitySeverity? = null
    var ignoreFilePath: String? = null
}

open class DockerPushProfile {
    var awsProfile: String? = null
    var registryRoot: String? = null
    var loginMethod: DockerLoginMethod? = null
    var loginUsername: String? = null
    var loginPassword: String? = null
}

open class HelmPushProfile {
    var helmDir: String? = null
    var repositoryUrl: String? = null
    var repositoryUsername: String? = null
    var repositoryPassword: String? = null
    var version: String? = null
}

open class DockerLoginProfile {
    var awsProfile: String? = null
    var registryRoot: String? = null
    var loginMethod: DockerLoginMethod? = null
    var loginUsername: String? = null
    var loginPassword: String? = null
}

open class Profile(val name: String) {
    internal var deploy: DeployProfile? = null
    internal var dockerBuild: DockerBuildProfile? = null
    internal var dockerScan: DockerSecurityScanProfile? = null
    internal var dockerPush: DockerPushProfile? = null
    internal var helmPush: HelmPushProfile? = null
    internal var dockerLogin: DockerLoginProfile? = null

    fun deploy(receiver: DeployProfile.() -> Unit = {}) {
        deploy = DeployProfile().apply {
            receiver()
        }
    }

    fun dockerBuild(receiver: DockerBuildProfile.() -> Unit = {}) {
        dockerBuild = DockerBuildProfile().apply {
            receiver()
        }
    }

    fun dockerScan(receiver: DockerSecurityScanProfile.() -> Unit = {}) {
        dockerScan = DockerSecurityScanProfile().apply{
            receiver()
        }
    }

    fun dockerLogin(receiver: DockerLoginProfile.() -> Unit = {}) {
        dockerLogin = DockerLoginProfile().apply { receiver() }
    }

    fun dockerPush(receiver: DockerPushProfile.() -> Unit = {}) {
        dockerPush = DockerPushProfile().apply { receiver() }
    }

    fun helmPush(receiver: HelmPushProfile.() -> Unit = {}) {
        helmPush = HelmPushProfile().apply {
            receiver()
        }
    }
}
