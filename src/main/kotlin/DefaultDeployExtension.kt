package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DockerLoginMethod

open class DefaultDeployExtension {
    var defaultDockerRegistryRoot: String? = null

    var defaultAttributes: Map<String, String> = emptyMap()
    var defaultPrepareTask: String? = null
    var defaultTargetNamespaces: List<String> = listOf("default")

    var defaultDockerLoginMethod: DockerLoginMethod = DockerLoginMethod.CLASSIC
    var defaultDockerLoginUsername: String = ""
    var defaultDockerLoginPassword: String = ""
}
