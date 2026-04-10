package com.webtoapp.core.wordpress

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
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
 * PHP 运行时管理器
 * 
 * 管理 PHP 内置 Web 服务器的生命周期：
 * - 检查/下载 PHP 二进制
 * - 生成 php.ini 配置
 * - 启动/停止 PHP 内置 Web 服务器进程
 * - 健康监控和自动重启
 */
class WordPressPhpRuntime(private val context: Context) {
    
    companion object {
        private const val TAG = "WordPressPhpRuntime"
        
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
    
    /** 路由服务器脚本路径缓存 */
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
     * 启动 PHP 内置 Web 服务器
     * 
     * @param documentRoot WordPress 项目根目录
     * @param port 监听端口（0=自动分配）
     * @return 实际使用的端口号，失败返回 -1
     */
    suspend fun startServer(documentRoot: String, port: Int = 0): Int = withContext(Dispatchers.IO) {
        try {
            // 检查 PHP 是否就绪
            if (!isPhpAvailable()) {
                _serverState.value = ServerState.Error("PHP 二进制未就绪，请先下载依赖")
                return@withContext -1
            }
            
            // 停止已有进程
            stopServer()
            
            _serverState.value = ServerState.Starting
            
            // 分配端口（使用 PortManager）
            val projectId = File(documentRoot).name
            val serverPort = PortManager.allocateForPhp("wp:$projectId", port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort
            
            // 构建命令
            // 使用自定义路由服务器（CLI SAPI），绕过 pmmp PHP 对 cli-server SAPI 的限制
            val phpBinary = getPhpBinaryPath()
            val routerScript = extractRouterScript()
            val command = buildPhpCommand(phpBinary, serverPort, documentRoot, routerScript)
            
            AppLogger.i(TAG, "启动 PHP 服务器: ${command.joinToString(" ")}")
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(File(documentRoot))
            processBuilder.redirectErrorStream(false)
            
            // 设置环境变量
            val env = processBuilder.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["PHP_INI_SCAN_DIR"] = ""  // 禁止扫描额外的 ini 目录
            
            phpOutputBuffer.setLength(0)  // 清空上次输出
            phpProcess = processBuilder.start()
            
            // 读取 stdout
            phpProcess?.inputStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.d(TAG, "[PHP-OUT] $line")
                            if (phpOutputBuffer.length < 4096) {
                                phpOutputBuffer.appendLine(line)
                            }
                        }
                    } catch (e: Exception) { AppLogger.d(TAG, "PHP stdout reader ended", e) }
                }.apply { isDaemon = true; start() }
            }
            
            // 读取 stderr（路由服务器日志输出到 stderr）
            phpProcess?.errorStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.e(TAG, "[PHP-ERR] $line")
                            if (phpOutputBuffer.length < 4096) {
                                phpOutputBuffer.appendLine(line)
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
                    // 给进程 2 秒优雅关闭的时间
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
    
    // ==================== 内部方法 ====================
    
    /**
     * 提取路由服务器脚本到缓存目录
     * 每次都从 assets 重新提取，确保使用最新版本
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
     * 使用自定义 PHP 路由服务器（CLI SAPI），绕过 pmmp PHP 对 cli-server SAPI 的限制。
     * PHP 二进制来自 pmmp，静态编译了 pmmpthread 扩展，该扩展禁止 php -S（cli-server SAPI）。
     * 路由服务器在 CLI SAPI 下用 stream_socket_server + pcntl_fork 提供 HTTP 服务。
     */
    private fun buildPhpCommand(phpBinary: String, serverPort: Int, documentRoot: String, routerScript: String): List<String> {
        val tmpDir = context.cacheDir.absolutePath
        val sessionDir = File(context.filesDir, "wordpress_deps/php/sessions")
        sessionDir.mkdirs()
        
        val phpArgs = mutableListOf(
            phpBinary,
            "-n"  // 不加载 php.ini（避免 SELinux ioctl 拒绝）
        )
        
        // 通过 -d 传递所有配置
        val iniSettings = linkedMapOf(
            "error_reporting" to "22527",  // E_ALL & ~E_DEPRECATED & ~E_STRICT
            "display_errors" to "Off",
            "log_errors" to "On",
            "error_log" to "/dev/stderr",
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
        
        return phpArgs
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
            
            // 检查进程是否已退出
            phpProcess?.let { process ->
                if (!process.isAliveCompat()) {
                    val exitCode = try { process.exitValue() } catch (_: Exception) { -1 }
                    Thread.sleep(50)  // 给日志线程时间读取剩余输出
                    val output = phpOutputBuffer.toString().trim().ifEmpty { "(no output captured)" }
                    AppLogger.e(TAG, "PHP 进程意外退出, exitCode=$exitCode\n$output")
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
            // Reflection: works on Android's UNIXProcess
            val pidField = process.javaClass.getDeclaredField("pid")
            pidField.isAccessible = true
            pidField.getInt(process).toLong()
        } catch (_: Exception) {
            -1
        }
    }
}
