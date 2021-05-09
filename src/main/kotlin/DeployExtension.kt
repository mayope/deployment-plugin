package net.mayope.deployplugin


open class DeployExtension : WithProfile {
    var serviceName: String = ""


    override val profiles = mutableListOf<DeployProfile>()
}
