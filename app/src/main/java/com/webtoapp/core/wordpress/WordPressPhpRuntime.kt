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
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 */
class WordPressPhpRuntime(private val context: Context) {
    
    companion object {
        private const val TAG = "WordPressPhpRuntime"
        
        /** Note: brief English comment. */
        private const val MAX_HEALTH_CHECK_RETRIES = 30
        
        /** Note: brief English comment. */
        private const val HEALTH_CHECK_INTERVAL_MS = 500L
    }
    
    // Note: brief English comment.
    
    sealed class ServerState {
        object Stopped : ServerState()
        object Starting : ServerState()
        data class Running(val port: Int, val pid: Long) : ServerState()
        data class Error(val message: String) : ServerState()
    }
    
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState
    
    /** Note: brief English comment. */
    private var phpProcess: Process? = null
    
    /** Note: brief English comment. */
    private var currentPort: Int = 0
    
    /** Note: brief English comment. */
    private val phpOutputBuffer = StringBuffer()
    
    /** Note: brief English comment. */
    private var routerScriptPath: String? = null
    
    // Note: brief English comment.
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun getPhpBinaryPath(): String {
        return WordPressDependencyManager.getPhpExecutablePath(context)
    }
    
    /**
     * Note: brief English comment.
     */
    fun isPhpAvailable(): Boolean {
        return WordPressDependencyManager.isPhpReady(context)
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    suspend fun startServer(documentRoot: String, port: Int = 0): Int = withContext(Dispatchers.IO) {
        try {
            // Note: brief English comment.
            if (!isPhpAvailable()) {
                _serverState.value = ServerState.Error("PHP 二进制未就绪，请先下载依赖")
                return@withContext -1
            }
            
            // Note: brief English comment.
            stopServer()
            
            _serverState.value = ServerState.Starting
            
            // Note: brief English comment.
            val projectId = File(documentRoot).name
            val serverPort = PortManager.allocateForPhp("wp:$projectId", port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort
            
            // Note: brief English comment.
            // Note: brief English comment.
            val phpBinary = getPhpBinaryPath()
            val routerScript = extractRouterScript()
            val command = buildPhpCommand(phpBinary, serverPort, documentRoot, routerScript)
            
            AppLogger.i(TAG, "启动 PHP 服务器: ${command.joinToString(" ")}")
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(File(documentRoot))
            processBuilder.redirectErrorStream(false)
            
            // Note: brief English comment.
            val env = processBuilder.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["PHP_INI_SCAN_DIR"] = ""  // 禁止扫描额外的 ini 目录
            
            phpOutputBuffer.setLength(0)  // 清空上次输出
            phpProcess = processBuilder.start()
            
            // Note: brief English comment.
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
            
            // Note: brief English comment.
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
            
            // Note: brief English comment.
            val ready = waitForServerReady(serverPort)
            if (ready) {
                val pid = getProcessPid(phpProcess)
                // Note: brief English comment.
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
     * Note: brief English comment.
     */
    fun stopServer() {
        try {
            phpProcess?.let { process ->
                process.destroy()
                try {
                    // Note: brief English comment.
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
            // Note: brief English comment.
            if (currentPort > 0) {
                PortManager.release(currentPort)
            }
            phpProcess = null
            currentPort = 0
            _serverState.value = ServerState.Stopped
        }
    }
    
    /**
     * Note: brief English comment.
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
     * Note: brief English comment.
     */
    fun getCurrentPort(): Int = currentPort
    
    /**
     * Note: brief English comment.
     */
    fun getServerUrl(): String? {
        return if (isServerRunning() && currentPort > 0) {
            "http://127.0.0.1:$currentPort"
        } else null
    }
    
    // Note: brief English comment.
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
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
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun buildPhpCommand(phpBinary: String, serverPort: Int, documentRoot: String, routerScript: String): List<String> {
        val tmpDir = context.cacheDir.absolutePath
        val sessionDir = File(context.filesDir, "wordpress_deps/php/sessions")
        sessionDir.mkdirs()
        
        val phpArgs = mutableListOf(
            phpBinary,
            "-n"  // 不加载 php.ini（避免 SELinux ioctl 拒绝）
        )
        
        // Note: brief English comment.
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
            // Note: brief English comment.
            // Note: brief English comment.
            "disable_functions" to "header,headers_list,headers_sent,header_remove,setcookie,setrawcookie"
        )
        
        iniSettings.forEach { (key, value) ->
            phpArgs.add("-d")
            phpArgs.add("$key=$value")
        }
        
        // Note: brief English comment.
        phpArgs.add(routerScript)
        phpArgs.add(serverPort.toString())
        phpArgs.add(documentRoot)
        phpArgs.add("index.php")
        
        return phpArgs
    }
    
    /**
     * Note: brief English comment.
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
            
            // Note: brief English comment.
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
     * Note: brief English comment.
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
