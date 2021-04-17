package net.mayope.deployplugin

open class DeployExtension {
    var serviceName: String = ""
    var attributes: Map<String, String> = emptyMap()
    var prepareTask: String? = null
    var targetNamespaces: List<String>? = null
    var dockerRegistryRoot: String? = null
}
