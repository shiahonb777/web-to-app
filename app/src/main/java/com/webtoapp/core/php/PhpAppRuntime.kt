package com.webtoapp.core.php

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.wordpress.WordPressDependencyManager
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

/**
 * 通用 PHP 应用运行时管理器
 * 
 * 复用 WordPress DependencyManager 的 PHP 二进制，管理通用 PHP 项目：
 * - Laravel / ThinkPHP / CodeIgniter / Slim / 原生 PHP
 * - 启动 PHP 内置 Web 服务器（指定 document root 和 entry file）
 * - 项目文件管理（导入/复制）
 * - 框架检测和 document root 自动推断
 */
class PhpAppRuntime(private val context: Context) {
    
    companion object {
        private const val TAG = "PhpAppRuntime"
        
        /** 最大健康检查重试次数 */
        private const val MAX_HEALTH_CHECK_RETRIES = 30
        
        /** 健康检查间隔（毫秒） */
        private const val HEALTH_CHECK_INTERVAL_MS = 500L
    }
    
    // ==================== 状态 ====================
    
    sealed class ServerState {
        object Stopped : ServerState()
        object Starting : ServerState()
        data class Running(val port: Int, val pid: Long) : ServerState()
        data class Error(val message: String) : ServerState()
    }
    
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState
    
    /** 当前 PHP 进程 */
    private var phpProcess: Process? = null
    
    /** 当前服务器端口 */
    private var currentPort: Int = 0
    
    /** PHP 进程输出缓冲区（用于崩溃诊断） */
    private val phpOutputBuffer = StringBuffer()
    private val phpStderrBuffer = StringBuffer()
    
    /** 路由服务器脚本路径 */
    private var routerScriptPath: String? = null
    
    // ==================== 公开 API ====================
    
    /**
     * 获取 PHP 二进制执行路径
     * 优先使用 nativeLibraryDir（SELinux 允许 execute_no_trans），回退到下载目录
     */
    fun getPhpBinaryPath(): String {
        return WordPressDependencyManager.getPhpExecutablePath(context)
    }
    
    /**
     * 检查 PHP 是否可用
     */
    fun isPhpAvailable(): Boolean {
        return WordPressDependencyManager.isPhpReady(context)
    }
    
    /**
     * 获取 PHP 项目存储根目录
     */
    fun getPhpProjectsDir(): File {
        return File(context.filesDir, "php_projects").also { it.mkdirs() }
    }
    
    /**
     * 获取指定项目目录
     */
    fun getProjectDir(projectId: String): File {
        return File(getPhpProjectsDir(), projectId)
    }
    
    /**
     * 启动 PHP 内置 Web 服务器
     * 
     * @param projectDir 项目根目录
     * @param documentRoot 相对于项目根的 Web 根目录（如 "public"）；空字符串表示项目根
     * @param entryFile 入口文件（如 index.php）
     * @param port 监听端口（0=自动分配）
     * @param envVars 额外环境变量
     * @return 实际使用的端口号，失败返回 -1
     */
    suspend fun startServer(
        projectDir: String,
        documentRoot: String = "",
        entryFile: String = "index.php",
        port: Int = 0,
        envVars: Map<String, String> = emptyMap()
    ): Int = withContext(Dispatchers.IO) {
        try {
            if (!isPhpAvailable()) {
                _serverState.value = ServerState.Error("PHP 二进制未就绪，请先下载依赖")
                return@withContext -1
            }
            
            // 停止已有进程
            stopServer()
            
            _serverState.value = ServerState.Starting
            
            // 确定实际 document root
            val actualDocRoot = if (documentRoot.isNotBlank()) {
                File(projectDir, documentRoot).absolutePath
            } else {
                projectDir
            }
            
            // 检查 document root 是否存在
            if (!File(actualDocRoot).isDirectory) {
                _serverState.value = ServerState.Error("Document root 不存在: $actualDocRoot")
                return@withContext -1
            }
            
            // 检查入口文件是否存在
            val entryFilePath = File(actualDocRoot, entryFile)
            if (!entryFilePath.exists()) {
                _serverState.value = ServerState.Error("入口文件不存在: $entryFile")
                return@withContext -1
            }
            
            // 分配端口（使用 PortManager）
            val projectId = File(projectDir).name
            val serverPort = PortManager.allocateForPhp(projectId, port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort
            
            // 获取 PHP 二进制路径
            val phpBinary = getPhpBinaryPath()
            
            // 提取路由服务器脚本
            val routerScript = extractRouterScript()
            
            // 构建启动命令 (使用 CLI SAPI 路由服务器，绕过 pmmpthread 对 cli-server SAPI 的限制)
            val command = buildPhpCommand(phpBinary, serverPort, actualDocRoot, routerScript)
            
            // 日志输出
            AppLogger.i(TAG, "PHP 二进制: $phpBinary")
            AppLogger.i(TAG, "执行命令: ${command.take(3).joinToString(" ")} ... (共 ${command.size} 个参数)")
            AppLogger.i(TAG, "项目目录: $projectDir")
            AppLogger.i(TAG, "Document root: $actualDocRoot")
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(File(projectDir))
            // 不合并 stderr — 分开捕获以便诊断 PHP 启动错误
            // processBuilder.redirectErrorStream(true)
            
            // 设置环境变量
            val env = processBuilder.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["PHP_INI_SCAN_DIR"] = ""
            env["APP_ENV"] = "production"
            env["APP_DEBUG"] = "false"
            
            // 用户自定义环境变量
            envVars.forEach { (k, v) -> env[k] = v }
            
            phpOutputBuffer.setLength(0)  // 清空上次输出
            phpStderrBuffer.setLength(0)
            phpProcess = processBuilder.start()
            
            // 启动 stdout 日志读取线程
            phpProcess?.inputStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.d(TAG, "[PHP] $line")
                            if (phpOutputBuffer.length < 4096) {
                                phpOutputBuffer.appendLine(line)
                            }
                        }
                    } catch (e: Exception) { AppLogger.d(TAG, "PHP stdout reader ended", e) }
                }.apply { isDaemon = true; start() }
            }
            
            // 启动 stderr 日志读取线程（捕获 PHP 启动错误）
            phpProcess?.errorStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.e(TAG, "[PHP-ERR] $line")
                            if (phpStderrBuffer.length < 4096) {
                                phpStderrBuffer.appendLine(line)
                            }
                        }
                    } catch (e: Exception) { AppLogger.d(TAG, "PHP stderr reader ended", e) }
                }.apply { isDaemon = true; start() }
            }
            
            // 等待服务器就绪
            val ready = waitForServerReady(serverPort)
            if (ready) {
                val pid = getProcessPid(phpProcess)
                // 注册进程到 PortManager
                phpProcess?.let { PortManager.registerProcess(serverPort, it, pid) }
                _serverState.value = ServerState.Running(serverPort, pid)
                AppLogger.i(TAG, "PHP 服务器已启动: 127.0.0.1:$serverPort (PID: $pid)")
                serverPort
            } else {
                stopServer()
                _serverState.value = ServerState.Error("PHP 服务器启动超时")
                -1
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "启动 PHP 服务器失败", e)
            _serverState.value = ServerState.Error("启动失败: ${e.message}")
            -1
        }
    }
    
    /**
     * 停止 PHP 服务器
     */
    fun stopServer() {
        try {
            phpProcess?.let { process ->
                process.destroy()
                try {
                    Thread.sleep(200)
                    if (process.isAliveCompat()) {
                        process.destroyForciblyCompat()
                    }
                } catch (e: Exception) { AppLogger.d(TAG, "Force kill PHP process failed", e) }
                AppLogger.i(TAG, "PHP 服务器已停止")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "停止 PHP 服务器异常: ${e.message}")
        } finally {
            // 释放端口
            if (currentPort > 0) {
                PortManager.release(currentPort)
            }
            phpProcess = null
            currentPort = 0
            _serverState.value = ServerState.Stopped
        }
    }
    
    /**
     * 检查服务器是否正在运行
     */
    fun isServerRunning(): Boolean {
        val process = phpProcess ?: return false
        return try {
            process.isAliveCompat()
        } catch (_: Exception) {
            false
        }
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
     * 导入 PHP 项目（复制文件到内部存储）
     */
    suspend fun createProject(
        projectId: String,
        sourceDir: File
    ): File = withContext(Dispatchers.IO) {
        val projectDir = File(getPhpProjectsDir(), projectId)
        projectDir.mkdirs()
        
        val excludeDirs = setOf("vendor", ".git", "node_modules", ".idea", "__MACOSX", "storage/logs", "storage/framework/cache")
        
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
        
        AppLogger.i(TAG, "PHP 项目文件已复制到: ${projectDir.absolutePath}")
        projectDir
    }
    
    /**
     * 检测 PHP 框架类型
     * 
     * @return 框架标识（laravel, thinkphp, codeigniter, slim, raw）
     */
    fun detectFramework(projectDir: File): String {
        // Laravel: artisan 文件或 composer.json 含 laravel/framework
        if (File(projectDir, "artisan").exists()) {
            return "laravel"
        }
        // ThinkPHP: think 命令文件
        if (File(projectDir, "think").exists()) {
            return "thinkphp"
        }
        // CodeIgniter: spark 命令文件或 system/CodeIgniter.php
        if (File(projectDir, "spark").exists() || File(projectDir, "system/CodeIgniter.php").exists()) {
            return "codeigniter"
        }
        // 通过 composer.json 检测框架
        val composerJson = File(projectDir, "composer.json")
        if (composerJson.exists()) {
            try {
                val content = composerJson.readText()
                if (content.contains("laravel/framework") || content.contains("laravel/laravel")) return "laravel"
                if (content.contains("topthink/framework") || content.contains("topthink/think")) return "thinkphp"
                if (content.contains("codeigniter4/framework") || content.contains("codeigniter/framework")) return "codeigniter"
                if (content.contains("slim/slim")) return "slim"
            } catch (e: Exception) { AppLogger.w(TAG, "Failed to read composer.json", e) }
        }
        // Laravel 特征目录检测（无 artisan 但有 Laravel 目录结构）
        if (File(projectDir, "routes").isDirectory && File(projectDir, "public/index.php").exists()) {
            return "laravel"
        }
        return "raw"
    }
    
    /**
     * 推断框架的默认 document root
     */
    fun detectDocumentRoot(projectDir: File, framework: String): String {
        return when (framework) {
            "laravel", "thinkphp", "codeigniter", "slim" -> {
                if (File(projectDir, "public/index.php").exists()) "public" else ""
            }
            else -> {
                // raw 框架：如果根目录没有 index.php 但 public/ 有，自动使用 public/
                if (!File(projectDir, "index.php").exists() && File(projectDir, "public/index.php").exists()) {
                    "public"
                } else {
                    ""
                }
            }
        }
    }
    
    /**
     * 检测入口文件
     */
    fun detectEntryFile(projectDir: File, documentRoot: String): String {
        val docRoot = if (documentRoot.isNotBlank()) File(projectDir, documentRoot) else projectDir
        val candidates = listOf("index.php", "app.php", "main.php", "server.php")
        for (candidate in candidates) {
            if (File(docRoot, candidate).exists()) return candidate
        }
        return "index.php"
    }
    
    /**
     * 检查项目是否有 composer.json
     */
    fun hasComposerJson(projectDir: File): Boolean {
        return File(projectDir, "composer.json").exists()
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 提取路由服务器脚本到缓存目录
     */
    private fun extractRouterScript(): String {
        routerScriptPath?.let { if (File(it).exists()) return it }
        
        val scriptFile = File(context.cacheDir, "php_router_server.php")
        try {
            context.assets.open("php_router_server.php").use { input ->
                scriptFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            routerScriptPath = scriptFile.absolutePath
            AppLogger.d(TAG, "路由服务器脚本已提取: ${scriptFile.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "提取路由服务器脚本失败", e)
        }
        return scriptFile.absolutePath
    }
    
    /**
     * 构建 PHP 启动命令
     * 
     * 使用自定义 PHP 路由服务器（CLI SAPI），绕过 pmmpthread 对 cli-server SAPI 的限制。
     * PHP 二进制来自 pmmp，静态编译了 pmmpthread 扩展，该扩展禁止 php -S（cli-server SAPI）。
     * 路由服务器在 CLI SAPI 下用 stream_socket_server + pcntl_fork 提供 HTTP 服务。
     */
    private fun buildPhpCommand(phpBinary: String, serverPort: Int, documentRoot: String, routerScript: String): List<String> {
        val tmpDir = context.cacheDir.absolutePath
        val sessionDir = File(context.filesDir, "php_app_deps/sessions")
        sessionDir.mkdirs()
        
        // 构建 PHP 参数: php -n -d ... router_server.php PORT DOCROOT ENTRYFILE
        val phpArgs = mutableListOf(
            "-n"  // 不加载 php.ini
        )
        
        // 通过 -d 传递配置
        val iniSettings = linkedMapOf(
            "error_reporting" to "22527",  // E_ALL & ~E_DEPRECATED & ~E_STRICT
            "display_errors" to "Off",
            "log_errors" to "On",
            "error_log" to "$tmpDir/php_app_errors.log",
            "memory_limit" to "256M",
            "max_execution_time" to "120",
            "max_input_time" to "60",
            "file_uploads" to "On",
            "upload_max_filesize" to "64M",
            "post_max_size" to "64M",
            "upload_tmp_dir" to tmpDir,
            "session.save_handler" to "files",
            "session.save_path" to sessionDir.absolutePath,
            "sys_temp_dir" to tmpDir,
            "date.timezone" to "UTC",
            "sqlite3.defensive" to "On",
            "allow_url_fopen" to "On",
            "allow_url_include" to "Off",
            "opcache.enable" to "1",
            "opcache.enable_cli" to "1",
            "opcache.memory_consumption" to "64",
            "opcache.interned_strings_buffer" to "8",
            "opcache.max_accelerated_files" to "4000",
            "opcache.validate_timestamps" to "0",
            // 禁用内置 header 相关函数，由路由服务器提供自定义实现
            // pmmp PHP CLI SAPI 的 headers_list() 不会跟踪 header() 设置的头，导致 302 重定向等丢失 Location 头
            "disable_functions" to "header,headers_list,headers_sent,header_remove,setcookie,setrawcookie"
        )
        
        iniSettings.forEach { (key, value) ->
            phpArgs.add("-d")
            phpArgs.add("$key=$value")
        }
        
        // 路由服务器脚本 + 参数
        phpArgs.add(routerScript)
        phpArgs.add(serverPort.toString())
        phpArgs.add(documentRoot)
        phpArgs.add("index.php")
        
        return mutableListOf(phpBinary).apply { addAll(phpArgs) }
    }
    
    /**
     * 等待 PHP 服务器就绪
     */
    private suspend fun waitForServerReady(port: Int): Boolean {
        repeat(MAX_HEALTH_CHECK_RETRIES) { attempt ->
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://127.0.0.1:$port/__health")
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1000
                conn.readTimeout = 2000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                
                if (code in 200..499) {
                    AppLogger.i(TAG, "PHP 服务器就绪 (尝试 ${attempt + 1}, code=$code)")
                    return true
                }
            } catch (e: Exception) {
                if (attempt % 5 == 0) {
                    AppLogger.d(TAG, "健康检查 #${attempt + 1}: ${e.javaClass.simpleName}: ${e.message}")
                }
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
            
            phpProcess?.let { process ->
                if (!process.isAliveCompat()) {
                    val exitCode = try { process.exitValue() } catch (_: Exception) { -1 }
                    Thread.sleep(200)  // 给日志线程更多时间读取剩余输出
                    val stdout = phpOutputBuffer.toString().trim().ifEmpty { "(no stdout)" }
                    val stderr = phpStderrBuffer.toString().trim().ifEmpty { "(no stderr)" }
                    AppLogger.e(TAG, "PHP 进程意外退出, exitCode=$exitCode\nstdout: $stdout\nstderr: $stderr")
                    return false
                }
            }
            
            delay(HEALTH_CHECK_INTERVAL_MS)
        }
        
        AppLogger.e(TAG, "PHP 服务器启动超时 (${MAX_HEALTH_CHECK_RETRIES * HEALTH_CHECK_INTERVAL_MS}ms)")
        return false
    }
    
    /**
     * 获取进程 PID
     */
    private fun getProcessPid(process: Process?): Long {
        if (process == null) return -1
        return try {
            val pidField = process.javaClass.getDeclaredField("pid")
            pidField.isAccessible = true
            pidField.getInt(process).toLong()
        } catch (_: Exception) {
            -1
        }
    }
}
