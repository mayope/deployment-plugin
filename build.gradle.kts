plugins {
    kotlin("jvm") version "1.4.21"

    id("com.gradle.plugin-publish") version "0.11.0"

    id("java-gradle-plugin")
    // Security check for dependencies by task
    id("org.owasp.dependencycheck") version "6.1.5"
    // static code analysis
    id("io.gitlab.arturbosch.detekt") version "1.16.0"
}

dependencies {
    implementation("com.beust:klaxon:5.2")

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    implementation(gradleApi())
    implementation(localGroovy())
}


repositories {
    jcenter()
}

tasks {
    withType<io.gitlab.arturbosch.detekt.Detekt> {
        // Target version of the generated JVM bytecode. It is used for type resolution.
        this.jvmTarget = "1.8"
    }
}

pluginBundle {
    website = "https://mayope.net"
    vcsUrl = "https://github.com/mayope/deployment-plugin"
    tags = listOf("helm","kubernetes","deployment","docker")

    plugins {
        create("deployment-plugin") {
            id = "net.mayope.deployplugin"
            displayName = "Mayope Deployment Plugin"
            description = "Opinionated tool to deploy docker container to kubernetes using helm"
        }
    }
}
dependencyCheck {
    failOnError = true
    // https://www.first.org/cvss/specification-document#Qualitative-Severity-Rating-Scale
    failBuildOnCVSS = 0.0f
    analyzers.assemblyEnabled = false
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
