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

/**
 * Node.js 运行时管理器
 * 
 * 通过 JNI 桥接层（NodeBridge）在进程内启动 nodejs-mobile。
 * libnode.so 是共享库，通过 dlopen + node::Start() 调用，不是独立可执行文件。
 *
 * 重要限制：
 * - node::Start() 每个进程只能调用一次（nodejs-mobile 限制）
 * - Node.js 在调用线程中阻塞运行，直到事件循环退出
 * - 退出后无法在同一进程中重启
 */
class NodeRuntime(private val context: Context) {
    
    companion object {
        private const val TAG = "NodeRuntime"
        private const val MAX_HEALTH_CHECK_RETRIES = 60
        private const val HEALTH_CHECK_INTERVAL_MS = 500L
    }
    
    // ==================== 状态 ====================
    
    sealed class ServerState {
        object Stopped : ServerState()
        object Starting : ServerState()
        data class Running(val port: Int) : ServerState()
        data class Error(val message: String) : ServerState()
    }
    
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState
    
    /** Node.js 运行线程 */
    private var nodeThread: Thread? = null
    
    /** 当前服务器端口 */
    private var currentPort: Int = 0
    
    /** Node.js 是否正在运行 */
    @Volatile
    private var isRunning = false
    
    // ==================== 公开 API ====================
    
    /**
     * 检查 Node.js 是否可用
     */
    fun isNodeAvailable(): Boolean {
        return NodeDependencyManager.isNodeReady(context)
    }
    
    /**
     * 启动 Node.js 服务器
     * 
     * @param projectDir Node.js 项目根目录
     * @param entryFile 入口文件（如 server.js, index.js, app.js）
     * @param port 监听端口（0=自动分配）
     * @param envVars 环境变量
     * @return 实际使用的端口号，失败返回 -1
     */
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
            
            // 分配端口
            val projectId = File(projectDir).name
            val serverPort = PortManager.allocateForNodeJs(projectId, port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort
            
            // 检查入口文件
            val entryFilePath = File(projectDir, entryFile).absolutePath
            val entryFileObj = File(entryFilePath)
            if (!entryFileObj.exists()) {
                _serverState.value = ServerState.Error("入口文件不存在: $entryFile")
                cleanupFailedStart()
                return@withContext -1
            }
            
            // 加载 libnode.so
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
            
            // 设置环境变量（在启动 Node.js 之前设置，因为 node::Start 在同一进程中）
            setEnvironmentVars(projectDir, serverPort, envVars)
            
            // 构建 Node.js 参数
            val args = arrayOf("node", entryFilePath)
            
            AppLogger.i(TAG, "启动 Node.js 服务器 (JNI): ${args.joinToString(" ")}")
            AppLogger.i(TAG, "工作目录: $projectDir, 端口: $serverPort")
            ShellLogger.i(TAG, "启动 Node.js 服务器 (JNI): ${args.joinToString(" ")}")
            ShellLogger.i(TAG, "工作目录: $projectDir, 端口: $serverPort")
            
            // 在后台线程中启动 Node.js（node::Start 是阻塞调用）
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
            
            // 等待服务器就绪
            // 短暂等待让 Node.js 初始化
            delay(300)
            
            // 检查线程是否还活着（如果立即崩溃，线程会很快结束）
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
    
    /**
     * 设置 Node.js 需要的环境变量
     * 因为 Node.js 在同一进程中运行，直接设置系统属性
     */
    private fun setEnvironmentVars(projectDir: String, port: Int, envVars: Map<String, String>) {
        // Node.js 通过 process.env 读取环境变量
        // 在同一进程中，我们需要在 node::Start 之前设置
        // 使用 System properties 或直接设置 env（通过 JNI 不太方便）
        // 更好的方式：通过 Node.js 脚本包装器注入环境变量
        // 但最简单的方式是设置系统环境变量
        
        // 注意：Java 不支持直接修改 System.getenv()
        // 我们通过在入口脚本前注入一个 wrapper 来设置环境变量
        // 这在 startServer 的 args 构建中处理
        
        // 实际上 Node.js 的 process.env 会继承当前进程的环境变量
        // 所以我们需要在 native 层设置 — 但 setenv 在 Android 上是进程级的
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
            
            // 通过反射调用 Os.setenv（Android API）
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
    
    /**
     * 停止 Node.js 服务器
     * 注意：由于 node::Start 是阻塞调用，无法优雅停止。
     * 只能中断线程（Node.js 会在下一个事件循环检查点退出）。
     */
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
    
    /**
     * 检查服务器是否正在运行
     */
    fun isServerRunning(): Boolean {
        return isRunning && nodeThread?.isAlive == true
    }
    
    /**
     * 获取当前服务器端口
     */
    fun getCurrentPort(): Int = currentPort
    
    /**
     * 获取服务器 URL
     */
    fun getServerUrl(): String? {
        return if (isServerRunning() && currentPort > 0) {
            "http://127.0.0.1:$currentPort"
        } else null
    }
    
    /**
     * 为没有静态 HTML 的 Node.js 后端项目生成预览页面
     */
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

    /**
     * 创建 Node.js 项目目录
     */
    suspend fun createProject(
        projectId: String,
        sourceDir: File
    ): File = withContext(Dispatchers.IO) {
        val projectDir = File(NodeDependencyManager.getNodeProjectsDir(context), projectId)
        projectDir.mkdirs()
        
        val excludeDirs = setOf(".git", "dist", "build", ".cache", ".next", ".nuxt")
        
        sourceDir.walkTopDown()
            .filter { file ->
                !excludeDirs.any { excluded -> file.absolutePath.contains("/$excluded/") }
            }
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(sourceDir).path
                val destFile = File(projectDir, relativePath)
                destFile.parentFile?.mkdirs()
                file.copyTo(destFile, overwrite = true)
            }
        
        AppLogger.i(TAG, "项目文件已复制到: ${projectDir.absolutePath}")
        projectDir
    }
    
    /**
     * 获取项目目录
     */
    fun getProjectDir(projectId: String): File {
        return File(NodeDependencyManager.getNodeProjectsDir(context), projectId)
    }
    
    /**
     * 检测 Node.js 项目的入口文件
     */
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
    
    // ==================== 内部方法 ====================
    
    /**
     * 等待 Node.js 服务器就绪
     */
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
                // 服务器尚未就绪
                AppLogger.d(TAG, "Health check attempt failed: ${e.message}")
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
            
            // 检查 Node.js 线程是否还活着
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
