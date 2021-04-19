package net.mayope.deployplugin

import net.mayope.deployplugin.tasks.DockerLoginMethod

open class DeployExtension {
    var serviceName: String = ""
    var attributes: Map<String, String> = emptyMap()
    var prepareTask: String? = null
    var targetNamespaces: List<String>? = null
    var dockerRegistryRoot: String? = null

    var dockerLoginMethod: DockerLoginMethod? = null
    var dockerLoginUsername: String? = null
    var dockerLoginPassword: String? = null
}
