package net.mayope.deployplugin

internal interface WithProfile {
    val profiles: MutableList<DeployProfile>
    fun deploymentProfiles() = profiles.toList()

    fun profile(name: String, call: DeployProfile.() -> Unit) {
        if(profiles.any{  it.name==name  }){
            error("DeployProfile with name: $name already exists please mention each profile only once")
        }
        DeployProfile(name).apply {
            call()
        }.also {
            profiles.add(it)
        }
    }

    fun default(call: DeployProfile.() -> Unit) {
        if(profiles.any{  it.name=="default"  }){
            error("DeployProfile with name: default already exists please mention each profile only once")
        }
        DeployProfile("default").apply {
            call()
        }.also {
            profiles.add(it)
        }

    }
}

open class DefaultDeployExtension : WithProfile {
    override val profiles = mutableListOf<DeployProfile>()
}
