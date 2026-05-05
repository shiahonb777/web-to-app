package com.webtoapp.core.php

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.wordpress.WordPressDependencyManager
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










class PhpAppRuntime(private val context: Context) {

    companion object {
        private const val TAG = "PhpAppRuntime"


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


    private var phpProcess: Process? = null


    private var currentPort: Int = 0


    private val phpOutputBuffer = StringBuffer()
    private val phpStderrBuffer = StringBuffer()


    private var routerScriptPath: String? = null







    fun getPhpBinaryPath(): String {
        return WordPressDependencyManager.getPhpExecutablePath(context)
    }




    fun isPhpAvailable(): Boolean {
        return WordPressDependencyManager.isPhpReady(context)
    }




    fun getPhpProjectsDir(): File {
        return File(context.filesDir, "php_projects").also { it.mkdirs() }
    }




    fun getProjectDir(projectId: String): File {
        return File(getPhpProjectsDir(), projectId)
    }











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


            stopServer()

            _serverState.value = ServerState.Starting


            val actualDocRoot = if (documentRoot.isNotBlank()) {
                File(projectDir, documentRoot).absolutePath
            } else {
                projectDir
            }


            if (!File(actualDocRoot).isDirectory) {
                _serverState.value = ServerState.Error("Document root 不存在: $actualDocRoot")
                return@withContext -1
            }


            val entryFilePath = File(actualDocRoot, entryFile)
            if (!entryFilePath.exists()) {
                _serverState.value = ServerState.Error("入口文件不存在: $entryFile")
                return@withContext -1
            }


            val projectId = File(projectDir).name
            val serverPort = PortManager.allocateForPhp(projectId, port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort


            val phpBinary = getPhpBinaryPath()


            val routerScript = extractRouterScript()


            val command = buildPhpCommand(phpBinary, serverPort, actualDocRoot, routerScript, entryFile)


            AppLogger.i(TAG, "PHP 二进制: $phpBinary")
            AppLogger.i(TAG, "执行命令: ${command.take(3).joinToString(" ")} ... (共 ${command.size} 个参数)")
            AppLogger.i(TAG, "项目目录: $projectDir")
            AppLogger.i(TAG, "Document root: $actualDocRoot")

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(File(projectDir))




            val env = processBuilder.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["PHP_INI_SCAN_DIR"] = ""
            env["APP_ENV"] = "production"
            env["APP_DEBUG"] = "false"


            envVars.forEach { (k, v) -> env[k] = v }

            phpOutputBuffer.setLength(0)
            phpStderrBuffer.setLength(0)
            phpProcess = processBuilder.start()


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


            val ready = waitForServerReady(serverPort)
            if (ready) {
                val pid = getProcessPid(phpProcess)

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




    fun stopServer() {
        try {
            phpProcess?.let { process ->
                try {
                    process.destroyGracefullyCompat(timeoutMs = 200L)
                    if (process.isAliveCompat()) {
                        process.destroyForciblyCompat()
                    }
                } catch (e: Exception) { AppLogger.d(TAG, "Force kill PHP process failed", e) }
                AppLogger.i(TAG, "PHP 服务器已停止")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "停止 PHP 服务器异常: ${e.message}")
        } finally {

            if (currentPort > 0) {
                PortManager.release(currentPort)
            }
            phpProcess = null
            currentPort = 0
            _serverState.value = ServerState.Stopped
        }
    }




    fun isServerRunning(): Boolean {
        val process = phpProcess ?: return false
        return try {
            process.isAliveCompat()
        } catch (_: Exception) {
            false
        }
    }




    fun getCurrentPort(): Int = currentPort




    fun getServerUrl(): String? {
        return if (isServerRunning() && currentPort > 0) {
            "http://127.0.0.1:$currentPort"
        } else null
    }




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






    fun detectFramework(projectDir: File): String {

        if (File(projectDir, "artisan").exists()) {
            return "laravel"
        }

        if (File(projectDir, "think").exists()) {
            return "thinkphp"
        }

        if (File(projectDir, "spark").exists() || File(projectDir, "system/CodeIgniter.php").exists()) {
            return "codeigniter"
        }

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

        if (File(projectDir, "routes").isDirectory && File(projectDir, "public/index.php").exists()) {
            return "laravel"
        }
        return "raw"
    }




    fun detectDocumentRoot(projectDir: File, framework: String): String {
        return when (framework) {
            "laravel", "thinkphp", "codeigniter", "slim" -> {
                if (File(projectDir, "public/index.php").exists()) "public" else ""
            }
            else -> {

                if (!File(projectDir, "index.php").exists() && File(projectDir, "public/index.php").exists()) {
                    "public"
                } else {
                    ""
                }
            }
        }
    }




    fun detectEntryFile(projectDir: File, documentRoot: String): String {
        val docRoot = if (documentRoot.isNotBlank()) File(projectDir, documentRoot) else projectDir
        val candidates = listOf("index.php", "app.php", "main.php", "server.php")
        for (candidate in candidates) {
            if (File(docRoot, candidate).exists()) return candidate
        }
        return "index.php"
    }




    fun hasComposerJson(projectDir: File): Boolean {
        return File(projectDir, "composer.json").exists()
    }






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








    private fun buildPhpCommand(phpBinary: String, serverPort: Int, documentRoot: String, routerScript: String, entryFile: String): List<String> {
        val tmpDir = context.cacheDir.absolutePath
        val sessionDir = File(context.filesDir, "php_app_deps/sessions")
        sessionDir.mkdirs()


        val phpArgs = mutableListOf(
            "-n"
        )


        val iniSettings = linkedMapOf(
            "error_reporting" to "22527",
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


            "disable_functions" to "header,headers_list,headers_sent,header_remove,setcookie,setrawcookie"
        )

        iniSettings.forEach { (key, value) ->
            phpArgs.add("-d")
            phpArgs.add("$key=$value")
        }


        phpArgs.add(routerScript)
        phpArgs.add(serverPort.toString())
        phpArgs.add(documentRoot)
        phpArgs.add(entryFile.ifBlank { "index.php" })

        return mutableListOf(phpBinary).apply { addAll(phpArgs) }
    }




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
