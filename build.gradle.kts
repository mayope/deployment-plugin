plugins {
    kotlin("jvm") version "1.8.22"

    id("com.gradle.plugin-publish") version "0.14.0"

    id("java-gradle-plugin")
    // Security check for dependencies by task
    id("org.owasp.dependencycheck") version "6.1.5"
    id("com.diffplug.spotless") version "5.6.1"
    id("signing")
    id("maven-publish")
}
val ktLintVersion = "0.41.0"
spotless {
    kotlin {
        ktlint(ktLintVersion)
    }
}

project.group = "net.mayope"

dependencies {
    implementation("com.beust:klaxon:5.6")

    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    implementation(gradleApi())
    implementation(localGroovy())
}


repositories {
    mavenCentral()
}

pluginBundle {
    website = "https://github.com/mayope/deployment-plugin"
    vcsUrl = "https://github.com/mayope/deployment-plugin"
    tags = listOf("helm", "kubernetes", "deployment", "docker")

}
gradlePlugin {
    plugins {
        create("deployplugin") {
            group = project.group
            id = "net.mayope.deployplugin"
            displayName = "Mayope Deployment Plugin"
            description = "Opinionated tool to deploy docker container to kubernetes using helm"
            implementationClass="net.mayope.deployplugin.DeployPlugin"
        }
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}

val publications = project.publishing.publications.withType(MavenPublication::class.java).map {
    with(it.pom) {
        withXml {
            val root = asNode()
            root.appendNode("name", "Mayope Deployment Plugin")
            root.appendNode("description", "Opinionated tool to deploy docker container to kubernetes using helm")
            root.appendNode("url", "https://github.com/mayope/deployment-plugin")
        }
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/mayope/deployment-plugin")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("klg71")
                name.set("Lukas Meisegeier")
                email.set("MeisegeierLukas@gmx.de")
            }
        }
        scm {
            url.set("https://github.com/mayope/deployment-plugin")
            connection.set("scm:git:git://github.com/mayope/deployment-plugin.git")
            developerConnection.set("scm:git:ssh://git@github.com/mayope/deployment-plugin.git")
        }
    }
}

signing{
    sign(publishing.publications["mavenJava"])
}
dependencyCheck {
    failOnError = true
    // https://www.first.org/cvss/specification-document#Qualitative-Severity-Rating-Scale
    failBuildOnCVSS = 0.0f
    analyzers.assemblyEnabled = false
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "17"
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

