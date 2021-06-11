package net.mayope.deployplugin

internal interface WithProfile {
    val profiles: MutableList<Profile>
    fun deploymentProfiles() = profiles.toList()

    fun profile(name: String, call: Profile.() -> Unit) {
        if (profiles.any { it.name == name }) {
            error("DeployProfile with name: $name already exists please mention each profile only once")
        }
        Profile(name).apply {
            call()
        }.also {
            profiles.add(it)
        }
    }

    fun default(call: Profile.() -> Unit) {
        if (profiles.any { it.name == "default" }) {
            error("DeployProfile with name: default already exists please mention each profile only once")
        }
        Profile("default").apply {
            call()
        }.also {
            profiles.add(it)
        }
    }
}

open class DefaultDeployExtension : WithProfile {
    override val profiles = mutableListOf<Profile>()
}
