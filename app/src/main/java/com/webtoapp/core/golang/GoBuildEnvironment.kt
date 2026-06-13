package com.webtoapp.core.golang

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.destroyForciblyCompat
import com.webtoapp.util.waitForCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class GoExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
)

object GoBuildEnvironment {

    private const val TAG = "GoBuildEnvironment"

    fun isGoReady(context: Context): Boolean = GoToolchainManager.isGoReady(context)

    suspend fun executeGo(
        context: Context,
        arguments: List<String>,
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        timeout: Long = TimeUnit.MINUTES.toMillis(30),
        onOutput: (String) -> Unit = {},
    ): GoExecutionResult = withContext(Dispatchers.IO) {
        if (!isGoReady(context)) {
            return@withContext GoExecutionResult(
                exitCode = -1,
                stdout = "",
                stderr = Strings.goBuildStreamToolchainNotReady,
                durationMs = 0,
            )
        }

        GoToolchainManager.ensureDnsPatched(context)
        if (!workingDir.exists() || !workingDir.isDirectory) {

            return@withContext GoExecutionResult(
                exitCode = -1,
                stdout = "",
                stderr = Strings.workingDirNotFound.format(workingDir.absolutePath),
                durationMs = 0,
            )
        }

        val start = System.currentTimeMillis()
        val goBin = GoToolchainManager.getGoBinary(context)
        val command = buildList {
            add(goBin.absolutePath)
            addAll(arguments)
        }
        AppLogger.d(TAG, "exec: ${command.joinToString(" ")} (cwd=${workingDir.absolutePath})")

        val pb = ProcessBuilder(command)
        pb.directory(workingDir)
        pb.redirectErrorStream(false)

        val processEnv = pb.environment()
        configureEnvironment(context, processEnv)
        env.forEach { (k, v) -> processEnv[k] = v }

        val proc = pb.start()
        val stdoutBuf = StringBuilder()
        val stderrBuf = StringBuilder()
        val tOut = Thread {
            proc.inputStream.bufferedReader().forEachLine { line ->
                stdoutBuf.appendLine(line)
                onOutput(line)
            }
        }
        val tErr = Thread {
            proc.errorStream.bufferedReader().forEachLine { line ->
                stderrBuf.appendLine(line)
                onOutput(line)
            }
        }
        tOut.start(); tErr.start()
        val finished = proc.waitForCompat(timeout)
        tOut.join(2_000); tErr.join(2_000)
        val exit = if (finished) proc.exitValue() else {
            proc.destroyForciblyCompat()
            -1
        }
        GoExecutionResult(
            exitCode = exit,
            stdout = stdoutBuf.toString(),
            stderr = stderrBuf.toString(),
            durationMs = System.currentTimeMillis() - start,
        )
    }

    fun configureEnvironment(context: Context, processEnv: MutableMap<String, String>) {
        val goRoot = GoToolchainManager.getGoRoot(context)
        val goPath = GoToolchainManager.getGoPath(context)
        val buildCache = GoToolchainManager.getBuildCacheDir(context)

        processEnv["GOROOT"] = goRoot.absolutePath
        processEnv["GOPATH"] = goPath.absolutePath
        processEnv["GOCACHE"] = buildCache.absolutePath
        processEnv["GOMODCACHE"] = GoToolchainManager.getModCacheDir(context).absolutePath
        processEnv["HOME"] = context.filesDir.absolutePath
        processEnv["TMPDIR"] = context.cacheDir.absolutePath

        processEnv["GOOS"] = processEnv["GOOS"] ?: "android"
        processEnv["GOARCH"] = processEnv["GOARCH"] ?: "arm64"

        processEnv["CGO_ENABLED"] = processEnv["CGO_ENABLED"] ?: "0"

        processEnv["GO111MODULE"] = processEnv["GO111MODULE"] ?: "on"

        processEnv["GOPROXY"] = processEnv["GOPROXY"] ?: "https://goproxy.cn,direct"
        processEnv["GOSUMDB"] = processEnv["GOSUMDB"] ?: "sum.golang.google.cn"

        processEnv["GOBIN"] = File(goPath, "bin").absolutePath

        val originalPath = processEnv["PATH"].orEmpty()
        processEnv["PATH"] = listOf(
            File(goRoot, "bin").absolutePath,
            File(goPath, "bin").absolutePath,
            originalPath,
        ).filter { it.isNotBlank() }.joinToString(File.pathSeparator)
    }

    suspend fun buildProject(
        context: Context,
        projectDir: File,
        binaryName: String,
        env: Map<String, String> = emptyMap(),
        onOutput: (String) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        val name = binaryName.ifBlank { projectDir.name }

        val envProbe = executeGo(
            context = context,
            arguments = listOf("env", "GOPROXY", "GOSUMDB", "GO111MODULE", "GOFLAGS"),
            workingDir = projectDir,
            env = env,
        )
        envProbe.stdout.lineSequence()
            .filter { it.isNotBlank() }
            .forEachIndexed { idx, line ->
                val key = listOf("GOPROXY", "GOSUMDB", "GO111MODULE", "GOFLAGS").getOrNull(idx) ?: "ENV"
                onOutput("[env] $key=$line")
            }

        val goMod = File(projectDir, "go.mod")

        val vendorDir = File(projectDir, "vendor")
        val hasVendor = vendorDir.isDirectory && vendorDir.list().isNullOrEmpty().not()
        if (hasVendor) {
            onOutput("[go] vendor/ found, building offline (-mod=vendor)")
            AppLogger.i(TAG, "Go 项目带 vendor/，走离线 build 路径")
        } else if (goMod.exists()) {

            onOutput("[go] 1/3 解析依赖图 (go mod tidy)")
            val tidyResult = executeGo(
                context = context,
                arguments = listOf("mod", "tidy"),
                workingDir = projectDir,
                env = env,
                onOutput = onOutput,
            )
            if (tidyResult.exitCode != 0) {

                AppLogger.e(TAG, "go mod tidy failed exit=${tidyResult.exitCode}\n${tidyResult.stderr}")
                onOutput("[go] ${Strings.goBuildStreamGoModTidyFailed.format(tidyResult.exitCode)}")
                tidyResult.stderr.lineSequence()
                    .filter { it.isNotBlank() }
                    .forEach { onOutput("[stderr] $it") }

                onOutput("[hint] 网络受限时可在电脑上跑 `go mod vendor`，把生成的 vendor/ 一并导入项目即可离线构建")
                return@withContext null
            }
            onOutput("[go] 2/3 预下载依赖 (go mod download)")
            val downloadResult = executeGo(
                context = context,
                arguments = listOf("mod", "download"),
                workingDir = projectDir,
                env = env,
                onOutput = onOutput,
            )
            if (downloadResult.exitCode != 0) {
                AppLogger.w(
                    TAG,
                    "go mod download 退出码 ${downloadResult.exitCode},将交给后续 go build 重试:\n${downloadResult.stderr}"
                )
                onOutput("[go] (mod download 非阻塞失败,继续 build 阶段)")
            }
        } else {
            onOutput("[go] ${Strings.goBuildStreamNoGoMod}")
        }

        val binDir = File(projectDir, "bin").also { it.mkdirs() }
        val output = File(binDir, name)

        val buildArgs = if (hasVendor) {
            listOf("build", "-mod=vendor", "-o", output.absolutePath, "./...")
        } else {
            listOf("build", "-o", output.absolutePath, "./...")
        }
        onOutput("[go] 3/3 编译 (go build ${buildArgs.joinToString(" ")})")
        val buildResult = executeGo(
            context = context,
            arguments = buildArgs,
            workingDir = projectDir,
            env = env,
            onOutput = onOutput,
        )
        if (buildResult.exitCode != 0 || !output.exists()) {
            AppLogger.e(TAG, "go build failed: exit=${buildResult.exitCode}\n${buildResult.stderr}")

            onOutput("[go] ${Strings.goBuildStreamFailedExit.format(buildResult.exitCode)}")
            buildResult.stderr.lineSequence()
                .filter { it.isNotBlank() }
                .forEach { onOutput("[stderr] $it") }
            return@withContext null
        }
        output.setExecutable(true, false)
        AppLogger.i(TAG, "Go 项目构建成功: ${output.absolutePath} (${output.length() / 1024} KB)")
        output
    }
}
