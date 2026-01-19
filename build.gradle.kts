import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"

    id("java-gradle-plugin")
    id("signing")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "2.0.0"
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
gradle.taskGraph.whenReady {
    if (allTasks.any { it is Sign }) {
        allprojects {
            extra["signing.keyId"] = "5357AC31"
            extra["signing.secretKeyRingFile"] = project.findProperty("signing_key_ring_file")
            extra["signing.password"] = project.findProperty("signing_key_ring_file_password")
        }
    }
}

gradlePlugin {
    website = "https://github.com/mayope/deployment-plugin"
    vcsUrl = "https://github.com/mayope/deployment-plugin"


    plugins {
        create("deployplugin") {
            group = project.group
            id = "net.mayope.deployplugin"
            displayName = "Mayope Deployment Plugin"
            description = "Opinionated tool to deploy docker container to kubernetes using helm"
            implementationClass="net.mayope.deployplugin.DeployPlugin"
            tags = listOf("helm", "kubernetes", "deployment", "docker")
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

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_23)
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}
