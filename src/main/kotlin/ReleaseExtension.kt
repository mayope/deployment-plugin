package net.mayope.deployplugin

open class ReleaseExtension : WithProfile {
    var serviceName: String = ""

    override val profiles = mutableListOf<Profile>()
}
