package com.webtoapp.core.nodejs

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.shell.ShellLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL












class NodeRuntime(private val context: Context) {

    companion object {
        private const val TAG = "NodeRuntime"
        private const val MAX_HEALTH_CHECK_RETRIES = 60
        private const val HEALTH_CHECK_INTERVAL_MS = 500L
        private val PROJECT_EXCLUDE_DIRS = setOf(".git", ".cache")
    }



    sealed class ServerState {
        object Stopped : ServerState()
        object Starting : ServerState()
        data class Running(val port: Int) : ServerState()
        data class Error(val message: String) : ServerState()
    }

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState


    private var nodeThread: Thread? = null


    private var currentPort: Int = 0


    @Volatile
    private var isRunning = false






    fun isNodeAvailable(): Boolean {
        return NodeDependencyManager.isNodeReady(context)
    }










    suspend fun startServer(
        projectDir: String,
        entryFile: String = "index.js",
        port: Int = 0,
        envVars: Map<String, String> = emptyMap()
    ): Int = withContext(Dispatchers.IO) {
        try {
            if (!isNodeAvailable()) {
                _serverState.value = ServerState.Error("Node.js 运行时未就绪，请先下载依赖")
                return@withContext -1
            }

            stopServer()
            _serverState.value = ServerState.Starting


            val projectId = File(projectDir).name
            val serverPort = PortManager.allocateForNodeJs(projectId, port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort


            val entryFilePath = File(projectDir, entryFile).absolutePath
            val entryFileObj = File(entryFilePath)
            if (!entryFileObj.exists()) {
                _serverState.value = ServerState.Error("入口文件不存在: $entryFile")
                cleanupFailedStart()
                return@withContext -1
            }


            AppLogger.i(TAG, "加载 libnode.so...")
            ShellLogger.i(TAG, "加载 libnode.so...")
            if (!NodeBridge.loadNode(context)) {
                val msg = "libnode.so 加载失败"
                AppLogger.e(TAG, msg)
                ShellLogger.e(TAG, msg)
                _serverState.value = ServerState.Error(msg)
                cleanupFailedStart()
                return@withContext -1
            }

            if (NodeBridge.isStarted()) {
                val msg = "Node.js 已启动过（每个进程只能启动一次），请重启应用"
                AppLogger.w(TAG, msg)
                ShellLogger.w(TAG, msg)
                _serverState.value = ServerState.Error(msg)
                cleanupFailedStart()
                return@withContext -1
            }


            setEnvironmentVars(projectDir, serverPort, envVars)


            val args = arrayOf("node", entryFilePath)

            AppLogger.i(TAG, "启动 Node.js 服务器 (JNI): ${args.joinToString(" ")}")
            AppLogger.i(TAG, "工作目录: $projectDir, 端口: $serverPort")
            ShellLogger.i(TAG, "启动 Node.js 服务器 (JNI): ${args.joinToString(" ")}")
            ShellLogger.i(TAG, "工作目录: $projectDir, 端口: $serverPort")


            isRunning = true
            val outputCallback = object : NodeBridge.OutputCallback {
                override fun onOutput(line: String, isError: Boolean) {
                    if (isError) {
                        AppLogger.w(TAG, "[Node stderr] $line")
                        ShellLogger.w(TAG, "[Node stderr] $line")
                    } else {
                        AppLogger.d(TAG, "[Node] $line")
                        ShellLogger.d(TAG, "[Node] $line")
                    }
                }
            }

            nodeThread = Thread({
                try {
                    val exitCode = NodeBridge.startNode(args, outputCallback)
                    if (exitCode == -99) {
                        AppLogger.e(TAG, "Node.js 异常终止 (SIGABRT 已捕获，应用未崩溃)")
                        ShellLogger.e(TAG, "Node.js 异常终止: 脚本执行失败，请检查依赖和语法")
                        _serverState.value = ServerState.Error("Node.js 异常终止，请检查项目配置")
                    } else {
                        AppLogger.i(TAG, "Node.js 退出, exitCode=$exitCode")
                        ShellLogger.i(TAG, "Node.js 退出, exitCode=$exitCode")
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Node.js 线程异常", e)
                    ShellLogger.e(TAG, "Node.js 线程异常: ${e.message}")
                } finally {
                    isRunning = false
                    if (currentPort > 0) {
                        PortManager.release(currentPort)
                        currentPort = 0
                    }
                    _serverState.value = ServerState.Stopped
                }
            }, "NodeJS-Runtime").apply {
                isDaemon = true
                start()
            }



            delay(300)


            if (nodeThread?.isAlive != true) {
                val msg = "Node.js 启动后立即退出"
                AppLogger.e(TAG, msg)
                ShellLogger.e(TAG, msg)
                _serverState.value = ServerState.Error(msg)
                cleanupFailedStart()
                return@withContext -1
            }

            val ready = waitForServerReady(serverPort)
            if (ready) {
                _serverState.value = ServerState.Running(serverPort)
                AppLogger.i(TAG, "Node.js 服务器已启动: 127.0.0.1:$serverPort")
                ShellLogger.i(TAG, "Node.js 服务器已启动: 127.0.0.1:$serverPort")
                serverPort
            } else {
                stopServer()
                _serverState.value = ServerState.Error("Node.js 服务器启动超时")
                -1
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "启动 Node.js 服务器失败", e)
            ShellLogger.e(TAG, "启动 Node.js 服务器失败: ${e.message}")
            cleanupFailedStart()
            _serverState.value = ServerState.Error("启动失败: ${e.message}")
            -1
        }
    }

    private fun cleanupFailedStart() {
        nodeThread = null
        isRunning = false
        if (currentPort > 0) {
            PortManager.release(currentPort)
            currentPort = 0
        }
    }





    private fun setEnvironmentVars(projectDir: String, port: Int, envVars: Map<String, String>) {












        try {
            val envMap = mutableMapOf(
                "HOME" to context.filesDir.absolutePath,
                "TMPDIR" to context.cacheDir.absolutePath,
                "NODE_ENV" to "production",
                "PORT" to port.toString(),
                "HOST" to "127.0.0.1",
                "NODE_PATH" to File(projectDir, "node_modules").absolutePath
            )
            envMap.putAll(envVars)


            for ((key, value) in envMap) {
                try {
                    android.system.Os.setenv(key, value, true)
                } catch (e: Exception) {
                    AppLogger.w(TAG, "设置环境变量失败: $key=${value}, ${e.message}")
                }
            }
            AppLogger.d(TAG, "环境变量已设置: PORT=$port, HOME=${context.filesDir.absolutePath}")
        } catch (e: Exception) {
            AppLogger.w(TAG, "设置环境变量异常", e)
        }
    }






    fun stopServer() {
        try {
            nodeThread?.let { thread ->
                if (thread.isAlive) {
                    thread.interrupt()
                    thread.join(2000)
                    AppLogger.i(TAG, "Node.js 服务器已停止")
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "停止 Node.js 服务器异常: ${e.message}")
        } finally {
            if (currentPort > 0) {
                PortManager.release(currentPort)
            }
            nodeThread = null
            currentPort = 0
            isRunning = false
            _serverState.value = ServerState.Stopped
        }
    }




    fun isServerRunning(): Boolean {
        return isRunning && nodeThread?.isAlive == true
    }




    fun getCurrentPort(): Int = currentPort




    fun getServerUrl(): String? {
        return if (isServerRunning() && currentPort > 0) {
            "http://127.0.0.1:$currentPort"
        } else null
    }




    fun generatePreviewHtml(projectDir: File, framework: String, entryFile: String): String {
        val entryFileObj = File(projectDir, entryFile)
        val sourceCode = if (entryFileObj.exists()) {
            try { entryFileObj.readText().take(8000) } catch (_: Exception) { "// 无法读取文件内容" }
        } else { "// 文件不存在: $entryFile" }

        val pkgFile = File(projectDir, "package.json")
        val packageJson = if (pkgFile.exists()) {
            try { pkgFile.readText().trim() } catch (_: Exception) { "" }
        } else ""

        val fileList = projectDir.walkTopDown()
            .filter { it.isFile }.take(30)
            .map { it.relativeTo(projectDir).path }.toList()

        val frameworkLabel = framework.replaceFirstChar { it.uppercase() }
        val escapedSource = sourceCode.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        val escapedPkg = packageJson.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val filesHtml = fileList.joinToString("\n") { "  <li>$it</li>" }

        return """<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>$frameworkLabel - 项目预览</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;background:#0d1117;color:#c9d1d9;padding:16px;line-height:1.6}
.header{text-align:center;padding:24px 0;border-bottom:1px solid #30363d;margin-bottom:20px}.header h1{font-size:22px;color:#58a6ff;margin-bottom:8px}
.badge{display:inline-block;background:#1f6feb;color:#fff;padding:4px 12px;border-radius:12px;font-size:13px;margin:4px}.badge.warn{background:#d29922}
.section{background:#161b22;border:1px solid #30363d;border-radius:8px;margin-bottom:16px;overflow:hidden}
.section-title{padding:12px 16px;background:#21262d;font-weight:600;font-size:14px;color:#8b949e;border-bottom:1px solid #30363d}
pre{padding:16px;overflow-x:auto;font-size:13px;font-family:'SF Mono',Consolas,monospace;white-space:pre-wrap;word-break:break-all;color:#c9d1d9;max-height:400px;overflow-y:auto}
ul{padding:12px 16px 12px 32px;font-size:13px}li{padding:2px 0;color:#8b949e}
.tip{background:#1a2332;border:1px solid #1f6feb;border-radius:8px;padding:16px;margin-top:16px;font-size:13px;color:#58a6ff}
</style></head><body>
<div class="header"><h1>$frameworkLabel Project</h1><span class="badge">$frameworkLabel</span><span class="badge warn">Requires Node.js Runtime</span></div>
<div class="section"><div class="section-title">$entryFile</div><pre>$escapedSource</pre></div>
${if (escapedPkg.isNotBlank()) """<div class="section"><div class="section-title">package.json</div><pre>$escapedPkg</pre></div>""" else ""}
<div class="section"><div class="section-title">Project Files (${fileList.size}${if (fileList.size >= 30) "+" else ""})</div><ul>
$filesHtml
</ul></div>
<div class="tip">This is a $frameworkLabel backend app. In preview mode, source code is displayed. After building APK with Node.js runtime, it will run normally.</div>
</body></html>"""
    }




    suspend fun createProject(
        projectId: String,
        sourceDir: File,
        cleanTarget: Boolean = true
    ): File = withContext(Dispatchers.IO) {
        require(sourceDir.exists() && sourceDir.isDirectory) { "源项目目录不存在: ${sourceDir.absolutePath}" }

        val projectDir = File(NodeDependencyManager.getNodeProjectsDir(context), projectId)
        if (cleanTarget && projectDir.exists()) {
            projectDir.deleteRecursively()
        }
        projectDir.mkdirs()

        sourceDir.walkTopDown()
            .onEnter { dir -> dir == sourceDir || dir.name !in PROJECT_EXCLUDE_DIRS }
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(sourceDir).path
                val destFile = File(projectDir, relativePath)
                destFile.parentFile?.mkdirs()
                file.copyTo(destFile, overwrite = true)
            }

        AppLogger.i(TAG, "项目文件已同步到: ${projectDir.absolutePath}")
        projectDir
    }

    suspend fun syncProjectFromSource(
        projectId: String,
        sourceDir: File
    ): File = createProject(projectId = projectId, sourceDir = sourceDir, cleanTarget = true)

    fun resolveSourceProjectDir(sourceProjectPath: String?): File? {
        val path = sourceProjectPath?.trim().orEmpty()
        if (path.isEmpty()) return null
        val file = File(path)
        return file.takeIf { it.exists() && it.isDirectory }
    }




    fun getProjectDir(projectId: String): File {
        return File(NodeDependencyManager.getNodeProjectsDir(context), projectId)
    }




    fun detectEntryFile(projectDir: File): String? {
        val packageJson = File(projectDir, "package.json")
        if (packageJson.exists()) {
            try {
                val content = packageJson.readText()
                val gson = com.webtoapp.util.GsonProvider.gson
                val json = gson.fromJson(content, com.google.gson.JsonObject::class.java)

                json.get("main")?.asString?.let { main ->
                    if (File(projectDir, main).exists()) return main
                }

                json.getAsJsonObject("scripts")?.get("start")?.asString?.let { startCmd ->
                    val nodeFileRegex = Regex("""node\s+(\S+\.(?:js|mjs|cjs))""")
                    nodeFileRegex.find(startCmd)?.groupValues?.get(1)?.let { entryFile ->
                        if (File(projectDir, entryFile).exists()) return entryFile
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "解析 package.json 失败", e)
            }
        }

        val candidates = listOf(
            "server.js", "server/index.js", "src/server.js",
            "app.js", "src/app.js",
            "index.js", "src/index.js",
            "main.js", "src/main.js",
            "server.mjs", "index.mjs"
        )

        for (candidate in candidates) {
            if (File(projectDir, candidate).exists()) return candidate
        }

        return null
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
                    AppLogger.i(TAG, "Node.js 服务器就绪 (尝试 ${attempt + 1})")
                    return true
                }
            } catch (e: Exception) {

                AppLogger.d(TAG, "Health check attempt failed: ${e.message}")
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }


            if (nodeThread?.isAlive != true) {
                AppLogger.e(TAG, "Node.js 线程已退出")
                ShellLogger.e(TAG, "Node.js 线程已退出")
                return false
            }

            delay(HEALTH_CHECK_INTERVAL_MS)
        }

        AppLogger.e(TAG, "Node.js 服务器启动超时 (${MAX_HEALTH_CHECK_RETRIES * HEALTH_CHECK_INTERVAL_MS}ms)")
        return false
    }
}
