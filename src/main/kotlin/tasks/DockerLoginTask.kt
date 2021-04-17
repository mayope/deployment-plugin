package net.mayope.deployplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class DockerLoginMethod {
    CLASSIC, AWS;

    fun login(project: Project, host: String, username: String, password: String): String {
        return when (this) {
            CLASSIC -> project.classicLogin(host, username, password)
            AWS -> project.awsLogin(host)
        }
    }

    fun Project.classicLogin(host: String, username: String, password: String): String {
        if (username.isBlank() || password.isBlank() || host.isBlank()) {
            error("You have to configure host, username and password for classical docker login")
        }
        exec {
            it.commandLine(listOf("docker", "login", host, "--username", username, "-p", password))
        }
        return "logged in"

    }

    fun Project.awsLogin(host: String): String {
        if (host.isBlank()) {
            error("You have to configure host for aws docker login")
        }
        ByteArrayOutputStream().use { os ->
            exec {
                it.commandLine("aws", "ecr", "get-login-password")
                it.standardOutput = os
            }
            val loginToken = os.toString(Charsets.UTF_8).trim()
            exec {
                it.commandLine(
                    listOf(
                        "docker", "login", host, "--username",
                        "AWS", "-p", loginToken
                    )
                )
            }
            return loginToken
        }
    }
}

@Suppress("MagicNumber")
open class DockerLoginTask : DefaultTask() {

    @Input
    var loginMethod: DockerLoginMethod = DockerLoginMethod.CLASSIC

    @Input
    var host: String = ""

    @Input
    @Optional
    var username: String = ""

    @Input
    @Optional
    var password: String = ""
    private val loginTokenFile = "${project.rootProject.buildDir}/login_token"

    init {
        // Enable gradle build cache
        outputs.file(loginTokenFile)
        // Lifetime of aws token is 12 Hours
        outputs.upToDateWhen {
            Instant.ofEpochMilli(project.file(loginTokenFile).lastModified())
                .plus(11, ChronoUnit.HOURS)
                .isAfter(Instant.now())
        }
    }

    @TaskAction
    fun login() {
        loginMethod.login(project, host, username, password)
    }
}
