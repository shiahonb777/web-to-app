package com.webtoapp.core.golang

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.shell.ShellLogger
import com.webtoapp.util.destroyGracefullyCompat
import com.webtoapp.util.destroyForciblyCompat
import com.webtoapp.util.isAliveCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL







class GoRuntime(private val context: Context) {

    companion object {
        private const val TAG = "GoRuntime"
        private const val MAX_HEALTH_CHECK_RETRIES = 30
        private const val HEALTH_CHECK_INTERVAL_MS = 500L
    }

    sealed class ServerState {
        object Stopped : ServerState()
        object Starting : ServerState()
        data class Running(val port: Int, val pid: Long) : ServerState()
        data class Error(val message: String) : ServerState()
    }

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState

    private var goProcess: Process? = null
    private var currentPort: Int = 0
    private val goOutputBuffer = StringBuffer()
    private val goStderrBuffer = StringBuffer()



    fun getProjectsDir(): File = File(context.filesDir, "go_projects").also { it.mkdirs() }

    fun getProjectDir(projectId: String): File = File(getProjectsDir(), projectId)

    suspend fun ensureBinaryReady(
        projectDir: File,
        binaryName: String
    ): String? = withContext(Dispatchers.IO) {
        val explicitName = binaryName.trim()
        if (explicitName.isNotEmpty()) {
            GoDependencyManager.findBinaryPath(projectDir, explicitName)?.let { return@withContext it }
        }

        GoDependencyManager.detectAnyCompatibleBinary(projectDir)?.let { return@withContext it.absolutePath }
        null
    }


    suspend fun startServer(
        projectDir: String,
        binaryName: String,
        port: Int = 0,
        envVars: Map<String, String> = emptyMap()
    ): Int = withContext(Dispatchers.IO) {
        try {
            stopServer()
            _serverState.value = ServerState.Starting

            val projDir = File(projectDir)

            val readyBinaryPath = ensureBinaryReady(
                projectDir = projDir,
                binaryName = binaryName
            )
            val runnableBinaryName = readyBinaryPath?.let { File(it).name } ?: binaryName
            val binaryPath = GoDependencyManager.prepareBinary(context, projDir, runnableBinaryName)
            if (binaryPath == null) {
                _serverState.value = ServerState.Error("Go 二进制无效或 ABI 不兼容")
                return@withContext -1
            }


            val projectId = projDir.name
            val serverPort = PortManager.allocateForGo(projectId, port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort

            if (!GoDependencyManager.isGoExecLoaderReady(context)) {
                _serverState.value = ServerState.Error("Go executable loader 未就绪")
                return@withContext -1
            }

            val command = GoDependencyManager.buildBinaryCommand(context, binaryPath, emptyList())

            AppLogger.i(TAG, "启动 Go 服务器: $binaryPath")
            AppLogger.i(TAG, "工作目录: $projectDir, 端口: $serverPort")
            ShellLogger.i(TAG, "启动 Go 服务器: ${command.joinToString(" ")}, 端口: $serverPort")

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(projDir)


            val env = processBuilder.environment()
            GoDependencyManager.configureGoBinaryEnvironment(
                context = context,
                processEnv = env,
                additionalPathEntries = listOf(File(context.filesDir, "go_bins").absolutePath)
            )
            env["PORT"] = serverPort.toString()
            env["HOST"] = "127.0.0.1"
            env["ADDR"] = "127.0.0.1:$serverPort"
            env["GIN_MODE"] = "release"


            envVars.forEach { (k, v) -> env[k] = v }

            goOutputBuffer.setLength(0)
            goStderrBuffer.setLength(0)
            goProcess = processBuilder.start()


            goProcess?.inputStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.d(TAG, "[Go] $line")
                            ShellLogger.d(TAG, "[Go] $line")
                            if (goOutputBuffer.length < 4096) goOutputBuffer.appendLine(line)
                        }
                    } catch (e: Exception) { AppLogger.d(TAG, "Go stdout reader ended", e) }
                }.apply { isDaemon = true; start() }
            }


            goProcess?.errorStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.w(TAG, "[Go-ERR] $line")
                            ShellLogger.w(TAG, "[Go-ERR] $line")
                            if (goStderrBuffer.length < 4096) goStderrBuffer.appendLine(line)
                        }
                    } catch (e: Exception) { AppLogger.d(TAG, "Go stderr reader ended", e) }
                }.apply { isDaemon = true; start() }
            }


            val ready = waitForServerReady(serverPort)
            if (ready) {
                val pid = getProcessPid(goProcess)
                goProcess?.let { PortManager.registerProcess(serverPort, it, pid) }
                _serverState.value = ServerState.Running(serverPort, pid)
                AppLogger.i(TAG, "Go 服务器已启动: 127.0.0.1:$serverPort (PID: $pid)")
                ShellLogger.i(TAG, "Go 服务器已启动: 127.0.0.1:$serverPort")
                serverPort
            } else {
                stopServer()
                _serverState.value = ServerState.Error("Go 服务器启动超时")
                -1
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "启动 Go 服务器失败", e)
            ShellLogger.e(TAG, "启动 Go 服务器失败: ${e.message}")
            _serverState.value = ServerState.Error("启动失败: ${e.message}")
            -1
        }
    }

    fun stopServer() {
        try {
            goProcess?.let { process ->
                try {
                    process.destroyGracefullyCompat(timeoutMs = 200L)
                    if (process.isAliveCompat()) process.destroyForciblyCompat()
                } catch (e: Exception) { AppLogger.d(TAG, "Force kill Go process failed", e) }
                AppLogger.i(TAG, "Go 服务器已停止")
                ShellLogger.i(TAG, "Go 服务器已停止")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "停止 Go 服务器异常: ${e.message}")
        } finally {
            if (currentPort > 0) PortManager.release(currentPort)
            goProcess = null
            currentPort = 0
            _serverState.value = ServerState.Stopped
        }
    }

    fun isServerRunning(): Boolean {
        return try { goProcess?.isAliveCompat() == true } catch (_: Exception) { false }
    }

    fun getCurrentPort(): Int = currentPort

    fun getServerUrl(): String? {
        return if (isServerRunning() && currentPort > 0) "http://127.0.0.1:$currentPort" else null
    }







    fun detectBinary(projectDir: File): String? {
        val binary = GoDependencyManager.detectAnyCompatibleBinary(projectDir) ?: return null
        val elfInfo = GoDependencyManager.parseElf(binary)
        AppLogger.i(TAG, "检测到 Go 二进制: ${binary.name} (${elfInfo.abiName})")
        return binary.name
    }




    fun detectFramework(projectDir: File): String {
        val goMod = File(projectDir, "go.mod")
        if (goMod.exists()) {
            try {
                val content = goMod.readText().lowercase()
                if (content.contains("github.com/gin-gonic/gin")) return "gin"
                if (content.contains("github.com/gofiber/fiber")) return "fiber"
                if (content.contains("github.com/labstack/echo")) return "echo"
                if (content.contains("github.com/go-chi/chi")) return "chi"
                if (content.contains("github.com/gorilla/mux")) return "gorilla"
                if (content.contains("github.com/beego/beego")) return "beego"
            } catch (e: Exception) { AppLogger.w(TAG, "Failed to read go.mod", e) }
        }
        val sourceFiles = listOf("main.go", "server.go", "app.go")
        for (name in sourceFiles) {
            val file = File(projectDir, name)
            if (file.exists()) {
                try {
                    val content = file.readText()
                    if (content.contains("gin.")) return "gin"
                    if (content.contains("fiber.")) return "fiber"
                    if (content.contains("echo.")) return "echo"
                    if (content.contains("chi.")) return "chi"
                    if (content.contains("net/http")) return "net_http"
                } catch (e: Exception) { AppLogger.w(TAG, "Failed to read $name for framework detection", e) }
            }
        }
        return "raw"
    }




    fun detectStaticDir(projectDir: File): String {
        val candidates = listOf("static", "public", "web", "dist", "assets", "www")
        for (dir in candidates) {
            val candidate = File(projectDir, dir)
            if (candidate.isDirectory && (candidate.listFiles()?.isNotEmpty() == true)) {
                return dir
            }
        }
        return ""
    }




    fun createProject(projectId: String, sourceDir: File): File {
        val projectDir = File(getProjectsDir(), projectId)
        projectDir.mkdirs()
        val excludeDirs = setOf(".git", "node_modules", ".idea", "vendor", "tmp", ".cache")
        sourceDir.walkTopDown()
            .filter { file ->
                !excludeDirs.any { excluded ->
                    file.absolutePath.contains("/$excluded/") || file.absolutePath.endsWith("/$excluded")
                }
            }
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(sourceDir).path
                val destFile = File(projectDir, relativePath)
                destFile.parentFile?.mkdirs()
                file.copyTo(destFile, overwrite = true)
            }
        AppLogger.i(TAG, "Go 项目文件已复制到: ${projectDir.absolutePath}")
        return projectDir
    }




    fun generatePreviewHtml(projectDir: File, framework: String, binaryName: String): String {
        val goMod = File(projectDir, "go.mod")
        val goModContent = if (goMod.exists()) {
            try { goMod.readText().take(4000) } catch (_: Exception) { "# 无法读取" }
        } else "# 无 go.mod"

        val mainGo = listOf("main.go", "server.go", "app.go")
            .map { File(projectDir, it) }
            .firstOrNull { it.exists() }
        val sourceCode = if (mainGo != null) {
            try { mainGo.readText().take(8000) } catch (_: Exception) { "// 无法读取" }
        } else "// 未找到入口文件"

        val fileList = projectDir.walkTopDown()
            .filter { it.isFile }
            .take(30)
            .map { it.relativeTo(projectDir).path }
            .toList()

        val binaryDetected = detectBinary(projectDir)
        val frameworkLabel = framework.replaceFirstChar { it.uppercase() }
        val escapedSource = sourceCode.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val escapedGoMod = goModContent.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val filesHtml = fileList.joinToString("\n") { "  <li>$it</li>" }

        val statusBadge = if (binaryDetected != null) {
            """<span class="badge ready">可运行二进制已就绪: $binaryDetected</span>"""
        } else {
            """<span class="badge warn">缺少预编译二进制</span>"""
        }

        val entryFileName = mainGo?.name ?: "main.go"
        val fileCount = "${fileList.size}${if (fileList.size >= 30) "+" else ""}"
        val tipText = if (binaryDetected != null) {
            "已检测到可运行二进制 ($binaryDetected)，可直接启动服务器。"
        } else {
            "当前 GO_APP 仅支持运行预编译二进制。请先为目标 ABI 构建可执行文件。"
        }

        return """<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>$frameworkLabel - Go 项目预览</title>
<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;background:#0d1117;color:#c9d1d9;padding:16px;line-height:1.6}.header{text-align:center;padding:24px 0;border-bottom:1px solid #30363d;margin-bottom:20px}.header h1{font-size:22px;color:#00ADD8;margin-bottom:8px}.badge{display:inline-block;background:#00ADD8;color:#fff;padding:4px 12px;border-radius:12px;font-size:13px;margin:4px}.badge.warn{background:#d29922}.badge.ready{background:#2ea043}.section{background:#161b22;border:1px solid #30363d;border-radius:8px;margin-bottom:16px;overflow:hidden}.section-title{padding:12px 16px;background:#21262d;font-weight:600;font-size:14px;color:#8b949e;border-bottom:1px solid #30363d}pre{padding:16px;overflow-x:auto;font-size:13px;font-family:'SF Mono',Consolas,monospace;white-space:pre-wrap;word-break:break-all;color:#c9d1d9;max-height:400px;overflow-y:auto}ul{padding:12px 16px 12px 32px;font-size:13px}li{padding:2px 0;color:#8b949e}.tip{background:#1a2332;border:1px solid #00ADD8;border-radius:8px;padding:16px;margin-top:16px;font-size:13px;color:#00ADD8}</style></head><body>
<div class="header"><h1>🔵 $frameworkLabel Go 项目</h1><span class="badge">$frameworkLabel</span>$statusBadge</div>
<div class="section"><div class="section-title">📄 $entryFileName</div><pre>$escapedSource</pre></div>
<div class="section"><div class="section-title">📦 go.mod</div><pre>$escapedGoMod</pre></div>
<div class="section"><div class="section-title">📁 项目文件 ($fileCount)</div><ul>$filesHtml</ul></div>
<div class="tip">💡 此项目是 $frameworkLabel Go 后端应用。$tipText</div>
</body></html>"""
    }



    private suspend fun waitForServerReady(port: Int): Boolean {
        repeat(MAX_HEALTH_CHECK_RETRIES) { attempt ->
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://127.0.0.1:$port/")
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 500
                conn.readTimeout = 500
                conn.requestMethod = "GET"
                val code = conn.responseCode
                if (code in 200..499) {
                    AppLogger.i(TAG, "Go 服务器就绪 (尝试 ${attempt + 1})")
                    return true
                }
            } catch (e: Exception) {
                AppLogger.d(TAG, "Health check attempt failed: ${e.message}")
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }

            goProcess?.let { process ->
                if (!process.isAliveCompat()) {
                    val exitCode = try { process.exitValue() } catch (_: Exception) { -1 }
                    val stdout = goOutputBuffer.toString().trim().ifEmpty { "(no stdout)" }
                    val stderr = goStderrBuffer.toString().trim().ifEmpty { "(no stderr)" }
                    AppLogger.e(TAG, "Go 进程意外退出, exitCode=$exitCode\nstdout: $stdout\nstderr: $stderr")
                    return false
                }
            }
            delay(HEALTH_CHECK_INTERVAL_MS)
        }
        AppLogger.e(TAG, "Go 服务器启动超时 (${MAX_HEALTH_CHECK_RETRIES * HEALTH_CHECK_INTERVAL_MS}ms)")
        return false
    }

    private fun getProcessPid(process: Process?): Long {
        if (process == null) return -1
        return try {
            val pidField = process.javaClass.getDeclaredField("pid")
            pidField.isAccessible = true
            pidField.getInt(process).toLong()
        } catch (_: Exception) { -1 }
    }
}
