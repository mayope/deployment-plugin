package net.mayope.deployplugin.tasks

import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOutput
import java.io.BufferedReader
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi


open class DockerPushTask @Inject constructor(@Input val serviceName: String) : DefaultTask() {

    @Input
    var registry: String? = null

    init {
        inputs.file(project.dockerTagFile())
        inputs.file(project.dockerNameFile())
        outputs.file(project.dockerPushedTagFile())
        outputs.file(project.dockerPushedRepoFile())
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun Project.pushDocker(serviceName: String) {
        val dockerRegistry = registry ?: ""

        executeCommand(
            "docker", "tag", file(dockerTagFile()).readText(),
            tagLatest(dockerRegistry, serviceName),
            isIgnoreExitValue = true
        )
        val buildTag = file(dockerTagFile()).readText()
        val buildName = file(dockerNameFile()).readText()
        val pushedDockerTag = "$dockerRegistry/$buildTag"
        val pushedDockerRepo = "$dockerRegistry/$buildName"
        executeCommand("docker", "tag", buildTag, pushedDockerTag, isIgnoreExitValue = true)

        ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build().let{


            }
        executeCommand("docker", "push", pushedDockerTag)

        executeCommand(
            "docker", "push", tagLatest(dockerRegistry, serviceName),
            isIgnoreExitValue = true
        )
        file(dockerPushedTagFile()).writeText(pushedDockerTag)
        file(dockerPushedRepoFile()).writeText(pushedDockerRepo)
    }


    @TaskAction
    fun push() {
        project.pushDocker(serviceName)
    }
}

fun ExecOutput.printAndFailIfExitValueUnzero() {
    println(standardError.asText.get())
    println(standardOutput.asText.get())
    if (this.result.get().exitValue != 0) {
        error("Process failed with exit value ${this.result.get().exitValue}")
    }
}

@OptIn(ExperimentalAtomicApi::class)
fun Project.executeCommand(vararg args: String, isIgnoreExitValue: Boolean = false, workingDir: String? = null) {
    val finished = AtomicBoolean(false)
    val scope = CoroutineScope(Dispatchers.Default)
    val process = PtyProcessBuilder(args).also {
        if (!workingDir.isNullOrBlank()) {
            it.setDirectory(workingDir)

        }
        val env: MutableMap<String, String> = HashMap<String, String>(System.getenv())
        if (!env.containsKey("TERM")) env["TERM"] = "xterm"
        it.setEnvironment(env)
        it.setConsole(true)
    }.start()
    scope.launch {
        watchOutput(finished, process.inputReader())
    }
    process.waitFor().let {
        if (it != 0) {
            error("Process failed with exit value ${it}")
        }
    }
    finished.store(true)
}


@OptIn(ExperimentalAtomicApi::class)
private suspend fun watchOutput(
    finished: AtomicBoolean,
    output: BufferedReader
) {
    while (!finished.load()) {
        while (!finished.load()) {
            output.read().let {
                if (it != -1) {
                    it.toChar()
                } else {
                    break
                }
            }.let {
                print(it)
            }

        }
        delay(100)
    }
}
