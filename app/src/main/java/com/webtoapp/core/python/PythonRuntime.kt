package com.webtoapp.core.python

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.shell.ShellLogger
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
 * Python Web 应用运行时管理器
 * 
 * 通过下载的 CPython 二进制执行 Python Web 项目。
 * 执行方式类似 PHP：ProcessBuilder 启动独立进程。
 * 
 * 支持框架：Flask / Django / FastAPI / Tornado / 原生 Python
 * 
 * 执行流程：
 * 1. 检测框架类型和入口文件
 * 2. 安装 requirements.txt 依赖（pip install --target）
 * 3. 构建启动命令（根据框架选择不同的启动方式）
 * 4. 通过 ProcessBuilder 启动 Python 进程
 * 5. 健康检查确认服务就绪
 */
class PythonRuntime(private val context: Context) {
    
    companion object {
        private const val TAG = "PythonRuntime"
        private const val MAX_HEALTH_CHECK_RETRIES = 60
        private const val HEALTH_CHECK_INTERVAL_MS = 500L
    }
    
    // ==================== 状态 ====================
    
    sealed class ServerState {
        object Stopped : ServerState()
        object Starting : ServerState()
        object InstallingDeps : ServerState()
        data class Running(val port: Int, val pid: Long) : ServerState()
        data class Error(val message: String) : ServerState()
    }
    
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState

    private var pythonProcess: Process? = null
    private var currentPort: Int = 0
    private val pythonOutputBuffer = StringBuffer()
    private val pythonStderrBuffer = StringBuffer()
    
    // ==================== 公开 API ====================
    
    fun getProjectsDir(): File {
        return File(context.filesDir, "python_projects").also { it.mkdirs() }
    }
    
    fun getProjectDir(projectId: String): File {
        return File(getProjectsDir(), projectId)
    }
    
    /**
     * 检查 Python 是否可用
     */
    fun isPythonAvailable(): Boolean {
        return PythonDependencyManager.isPythonReady(context)
    }
    
    /**
     * 启动 Python 服务器
     * 
     * @param projectDir Python 项目根目录
     * @param entryFile 入口文件
     * @param framework 框架类型
     * @param port 监听端口（0=自动分配）
     * @param envVars 额外环境变量
     * @param installDeps 是否先安装依赖
     * @return 实际使用的端口号，失败返回 -1
     */
    suspend fun startServer(
        projectDir: String,
        entryFile: String = "app.py",
        framework: String = "raw",
        port: Int = 0,
        envVars: Map<String, String> = emptyMap(),
        installDeps: Boolean = true
    ): Int = withContext(Dispatchers.IO) {
        try {
            if (!isPythonAvailable()) {
                _serverState.value = ServerState.Error("Python 运行时未就绪，请先下载依赖")
                return@withContext -1
            }
            
            stopServer()
            _serverState.value = ServerState.Starting
            
            val projDir = File(projectDir)
            val pythonBin = PythonDependencyManager.getPythonExecutablePath(context)
            val pythonHome = PythonDependencyManager.getPythonHome(context)
            val muslLinker = PythonDependencyManager.getMuslLinkerPath(context)
            
            AppLogger.i(TAG, "musl linker: ${muslLinker ?: "(不可用 - Python 可能无法执行)"}")
            ShellLogger.i(TAG, "musl linker: ${muslLinker ?: "(不可用)"}")
            
            // === 预飞检查：验证 Python 二进制有效性 ===
            val binFile = File(pythonBin)
            AppLogger.i(TAG, "Python 二进制: ${binFile.absolutePath} (${binFile.length() / 1024} KB, executable=${binFile.canExecute()})")
            ShellLogger.i(TAG, "Python 二进制: ${binFile.absolutePath} (${binFile.length() / 1024} KB)")
            
            // 检查文件大小（有效的 Python 二进制应 > 1MB）
            if (binFile.length() < 1024 * 1024) {
                val errMsg = "Python 二进制无效: 文件过小 (${binFile.length()} bytes)，请重新下载 Python 运行时或重新构建 APK"
                AppLogger.e(TAG, errMsg)
                ShellLogger.e(TAG, errMsg)
                _serverState.value = ServerState.Error(errMsg)
                return@withContext -1
            }
            
            // 验证 PYTHONHOME 包含标准库
            val stdlibDir = File(pythonHome, "lib/python${PythonDependencyManager.PYTHON_VERSION}")
            if (!stdlibDir.exists()) {
                AppLogger.w(TAG, "PYTHONHOME 标准库目录不存在: ${stdlibDir.absolutePath}")
                ShellLogger.w(TAG, "PYTHONHOME 标准库目录不存在: ${stdlibDir.absolutePath}")
            } else {
                val fileCount = stdlibDir.walkTopDown().filter { it.isFile }.count()
                AppLogger.i(TAG, "PYTHONHOME 标准库: ${stdlibDir.absolutePath} ($fileCount 文件)")
            }
            
            // 运行 --version 验证二进制是否真正可执行
            val versionCheck = verifyPythonBinary(pythonBin, pythonHome, muslLinker)
            if (versionCheck == null) {
                val errMsg = "Python 二进制无法执行: 运行 --version 失败。" +
                    (if (muslLinker == null) "缺少 musl 动态链接器，请重新下载 Python 运行时" else "二进制可能损坏或不兼容当前设备") +
                    "，请重新下载 Python 运行时或重新构建 APK"
                AppLogger.e(TAG, errMsg)
                ShellLogger.e(TAG, errMsg)
                _serverState.value = ServerState.Error(errMsg)
                return@withContext -1
            }
            AppLogger.i(TAG, "Python 版本验证通过: $versionCheck")
            ShellLogger.i(TAG, "Python 版本验证通过: $versionCheck")
            
            // 安装依赖
            if (installDeps && File(projDir, "requirements.txt").exists()) {
                _serverState.value = ServerState.InstallingDeps
                AppLogger.i(TAG, "安装 Python 依赖...")
                ShellLogger.i(TAG, "安装 Python 依赖...")
                val depsInstalled = PythonDependencyManager.installRequirements(context, projDir) { line ->
                    ShellLogger.d(TAG, "[pip] $line")
                }
                if (!depsInstalled) {
                    // pip install 失败，但如果 .pypackages 已从 APK assets 中提取（构建时预装），仍然可以继续
                    val sitePackages = File(projDir, ".pypackages")
                    val existingPackages = sitePackages.listFiles()
                    if (sitePackages.exists() && existingPackages != null && existingPackages.isNotEmpty()) {
                        AppLogger.w(TAG, "pip install 失败，但 .pypackages 已携带 ${existingPackages.size} 个包，继续启动")
                        ShellLogger.w(TAG, "依赖安装失败，使用预打包依赖继续")
                    } else {
                        val errMsg = "Python 依赖安装失败。可能原因：1) 无网络连接 2) requirements.txt 中包含不兼容的包 3) Android 环境不支持该包的安装方式"
                        AppLogger.e(TAG, errMsg)
                        ShellLogger.e(TAG, errMsg)
                        _serverState.value = ServerState.Error(errMsg)
                        return@withContext -1
                    }
                }
            }
            
            // 分配端口
            val projectId = projDir.name
            val serverPort = PortManager.allocateForPython(projectId, port)
            if (serverPort < 0) {
                _serverState.value = ServerState.Error("无法分配端口")
                return@withContext -1
            }
            currentPort = serverPort
            
            // 检查入口文件
            val entryFilePath = File(projDir, entryFile)
            if (!entryFilePath.exists()) {
                _serverState.value = ServerState.Error("入口文件不存在: $entryFile")
                PortManager.release(serverPort)
                return@withContext -1
            }
            
            // 创建引导脚本（修复 importlib.metadata 和端口问题）
            createBootstrapScript(projDir, serverPort)
            
            // 构建启动命令
            val command = buildPythonCommand(pythonBin, framework, entryFile, serverPort, muslLinker, pythonHome)
            
            AppLogger.i(TAG, "启动 Python 服务器: ${command.joinToString(" ")}")
            AppLogger.i(TAG, "工作目录: $projectDir, 端口: $serverPort, 框架: $framework")
            ShellLogger.i(TAG, "启动 Python 服务器: ${command.joinToString(" ")}")
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(projDir)
            
            // 设置环境变量
            val env = processBuilder.environment()
            env["PYTHONHOME"] = pythonHome
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["PORT"] = serverPort.toString()
            env["HOST"] = "127.0.0.1"
            env["FLASK_ENV"] = "production"
            env["DJANGO_SETTINGS_MODULE"] = detectDjangoSettings(projDir)
            
            // PYTHONPATH: 标准库 + 项目本地包 + 项目目录
            val sitePackages = File(projDir, ".pypackages")
            val pythonPath = buildList {
                add("${pythonHome}/lib/python${PythonDependencyManager.PYTHON_VERSION}")
                add("${pythonHome}/lib/python${PythonDependencyManager.PYTHON_VERSION}/lib-dynload")
                if (sitePackages.exists()) add(sitePackages.absolutePath)
                add(projDir.absolutePath)
            }.joinToString(":")
            env["PYTHONPATH"] = pythonPath
            env["LD_LIBRARY_PATH"] = "${pythonHome}/lib"
            
            // ★ 详细诊断日志
            AppLogger.i(TAG, "=== Python 服务器启动诊断 ===")
            AppLogger.i(TAG, "PYTHONHOME=$pythonHome")
            AppLogger.i(TAG, "PYTHONPATH=$pythonPath")
            AppLogger.i(TAG, "LD_LIBRARY_PATH=${pythonHome}/lib")
            AppLogger.i(TAG, "musl linker=$muslLinker")
            AppLogger.i(TAG, ".pypackages 存在=${sitePackages.exists()}, 数量=${sitePackages.listFiles()?.size ?: 0}")
            AppLogger.i(TAG, "stdlib 存在=${File(pythonHome, "lib/python${PythonDependencyManager.PYTHON_VERSION}").exists()}, 文件数=${File(pythonHome, "lib/python${PythonDependencyManager.PYTHON_VERSION}").walkTopDown().filter { it.isFile }.count()}")
            AppLogger.i(TAG, "libpython so 存在=${File(pythonHome, "lib/libpython3.12.so.1.0").exists()}, 大小=${File(pythonHome, "lib/libpython3.12.so.1.0").let { if (it.exists()) "${it.length()/1024}KB" else "N/A" }}")
            AppLogger.i(TAG, "=========================")
            
            env["PATH"] = "${File(pythonHome, "bin").absolutePath}:${env["PATH"] ?: "/usr/bin"}"
            
            // 用户自定义环境变量
            envVars.forEach { (k, v) -> env[k] = v }
            
            pythonOutputBuffer.setLength(0)
            pythonStderrBuffer.setLength(0)
            pythonProcess = processBuilder.start()
            
            // stdout 日志线程
            pythonProcess?.inputStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.d(TAG, "[Python] $line")
                            ShellLogger.d(TAG, "[Python] $line")
                            if (pythonOutputBuffer.length < 4096) pythonOutputBuffer.appendLine(line)
                        }
                    } catch (e: Exception) { AppLogger.d(TAG, "Python stdout reader ended", e) }
                }.apply { isDaemon = true; start() }
            }
            
            // stderr 日志线程
            pythonProcess?.errorStream?.let { stream ->
                Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            AppLogger.w(TAG, "[Python-ERR] $line")
                            ShellLogger.w(TAG, "[Python-ERR] $line")
                            if (pythonStderrBuffer.length < 4096) pythonStderrBuffer.appendLine(line)
                        }
                    } catch (e: Exception) { AppLogger.d(TAG, "Python stderr reader ended", e) }
                }.apply { isDaemon = true; start() }
            }
            
            // 等待服务器就绪
            val ready = waitForServerReady(serverPort)
            if (ready) {
                val pid = getProcessPid(pythonProcess)
                pythonProcess?.let { PortManager.registerProcess(serverPort, it, pid) }
                _serverState.value = ServerState.Running(serverPort, pid)
                AppLogger.i(TAG, "Python 服务器已启动: 127.0.0.1:$serverPort (PID: $pid)")
                ShellLogger.i(TAG, "Python 服务器已启动: 127.0.0.1:$serverPort")
                serverPort
            } else {
                val stdout = pythonOutputBuffer.toString().trim().take(500)
                val stderr = pythonStderrBuffer.toString().trim().take(500)
                val processAlive = pythonProcess?.isAliveCompat() == true
                val exitCode = if (!processAlive) try { pythonProcess?.exitValue() } catch (_: Exception) { null } else null
                val detail = "processAlive=$processAlive, exitCode=$exitCode\nstdout: ${stdout.ifEmpty { "(无)" }}\nstderr: ${stderr.ifEmpty { "(无)" }}"
                AppLogger.e(TAG, "Python 服务器启动超时, $detail")
                val errorMsg = if (stderr.isNotEmpty()) {
                    "Python 服务器启动超时\n$stderr"
                } else if (stdout.isNotEmpty()) {
                    "Python 服务器启动超时\n$stdout"
                } else {
                    "Python 服务器启动超时（无输出）"
                }
                stopServer()
                _serverState.value = ServerState.Error(errorMsg)
                -1
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "启动 Python 服务器失败", e)
            ShellLogger.e(TAG, "启动 Python 服务器失败: ${e.message}")
            _serverState.value = ServerState.Error("启动失败: ${e.message}")
            -1
        }
    }
    
    fun stopServer() {
        try {
            pythonProcess?.let { process ->
                process.destroy()
                try {
                    Thread.sleep(200)
                    if (process.isAliveCompat()) process.destroyForciblyCompat()
                } catch (e: Exception) { AppLogger.d(TAG, "Force kill Python process failed", e) }
                AppLogger.i(TAG, "Python 服务器已停止")
                ShellLogger.i(TAG, "Python 服务器已停止")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "停止 Python 服务器异常: ${e.message}")
        } finally {
            if (currentPort > 0) PortManager.release(currentPort)
            pythonProcess = null
            currentPort = 0
            _serverState.value = ServerState.Stopped
        }
    }
    
    fun isServerRunning(): Boolean {
        return try { pythonProcess?.isAliveCompat() == true } catch (_: Exception) { false }
    }
    
    fun getCurrentPort(): Int = currentPort
    
    fun getServerUrl(): String? {
        return if (isServerRunning() && currentPort > 0) "http://127.0.0.1:$currentPort" else null
    }

    // ==================== 项目检测 ====================
    
    fun detectFramework(projectDir: File): String {
        val candidates = listOf("app.py", "main.py", "wsgi.py", "application.py", "run.py")
        for (candidate in candidates) {
            val file = File(projectDir, candidate)
            if (file.exists()) {
                try {
                    val content = file.readText()
                    if (content.contains("from flask") || content.contains("import flask")) return "flask"
                    if (content.contains("from fastapi") || content.contains("import fastapi")) return "fastapi"
                    if (content.contains("from tornado") || content.contains("import tornado")) return "tornado"
                } catch (e: Exception) { AppLogger.w(TAG, "Failed to read $candidate for framework detection", e) }
            }
        }
        if (File(projectDir, "manage.py").exists()) return "django"
        val requirements = File(projectDir, "requirements.txt")
        if (requirements.exists()) {
            try {
                val content = requirements.readText().lowercase()
                if (content.contains("flask")) return "flask"
                if (content.contains("django")) return "django"
                if (content.contains("fastapi")) return "fastapi"
                if (content.contains("tornado")) return "tornado"
            } catch (e: Exception) { AppLogger.w(TAG, "Failed to read requirements.txt", e) }
        }
        val pyproject = File(projectDir, "pyproject.toml")
        if (pyproject.exists()) {
            try {
                val content = pyproject.readText().lowercase()
                if (content.contains("flask")) return "flask"
                if (content.contains("django")) return "django"
                if (content.contains("fastapi")) return "fastapi"
                if (content.contains("tornado")) return "tornado"
            } catch (e: Exception) { AppLogger.w(TAG, "Failed to read pyproject.toml", e) }
        }
        return "raw"
    }
    
    fun detectEntryFile(projectDir: File, framework: String): String {
        return when (framework) {
            "django" -> if (File(projectDir, "manage.py").exists()) "manage.py" else "app.py"
            "flask" -> listOf("app.py", "application.py", "wsgi.py", "run.py", "main.py")
                .firstOrNull { File(projectDir, it).exists() } ?: "app.py"
            "fastapi" -> listOf("main.py", "app.py", "api.py", "server.py")
                .firstOrNull { File(projectDir, it).exists() } ?: "main.py"
            "tornado" -> listOf("app.py", "main.py", "server.py")
                .firstOrNull { File(projectDir, it).exists() } ?: "app.py"
            else -> listOf("app.py", "main.py", "server.py", "index.py", "run.py")
                .firstOrNull { File(projectDir, it).exists() } ?: "app.py"
        }
    }
    
    fun createProject(projectId: String, sourceDir: File): File {
        val projectDir = File(getProjectsDir(), projectId)
        projectDir.mkdirs()
        val excludeDirs = setOf("venv", ".venv", "__pycache__", ".git", "node_modules", ".idea", ".mypy_cache", ".pytest_cache", "env")
        sourceDir.walkTopDown()
            .filter { file -> !excludeDirs.any { excluded -> file.absolutePath.contains("/$excluded/") || file.absolutePath.endsWith("/$excluded") } }
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(sourceDir).path
                val destFile = File(projectDir, relativePath)
                destFile.parentFile?.mkdirs()
                file.copyTo(destFile, overwrite = true)
            }
        AppLogger.i(TAG, "Python 项目文件已复制到: ${projectDir.absolutePath}")
        return projectDir
    }
    
    fun generatePreviewHtml(projectDir: File, framework: String, entryFile: String): String {
        val entryFileObj = File(projectDir, entryFile)
        val sourceCode = if (entryFileObj.exists()) {
            try { entryFileObj.readText().take(8000) } catch (_: Exception) { "# 无法读取文件内容" }
        } else "# 文件不存在: $entryFile"
        val reqFile = File(projectDir, "requirements.txt")
        val requirements = if (reqFile.exists()) { try { reqFile.readText().trim() } catch (_: Exception) { "" } } else ""
        val fileList = projectDir.walkTopDown().filter { it.isFile }.take(30).map { it.relativeTo(projectDir).path }.toList()
        val frameworkLabel = framework.replaceFirstChar { it.uppercase() }
        val escapedSource = sourceCode.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        val escapedReqs = requirements.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val filesHtml = fileList.joinToString("\n") { "  <li>$it</li>" }
        val pythonReady = isPythonAvailable()
        val statusBadge = if (pythonReady) "<span class=\"badge ready\">运行时就绪</span>"
        else "<span class=\"badge warn\">需要下载 Python 运行时</span>"
        return """<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>$frameworkLabel - 项目预览</title>
<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;background:#0d1117;color:#c9d1d9;padding:16px;line-height:1.6}.header{text-align:center;padding:24px 0;border-bottom:1px solid #30363d;margin-bottom:20px}.header h1{font-size:22px;color:#3776AB;margin-bottom:8px}.badge{display:inline-block;background:#3776AB;color:#fff;padding:4px 12px;border-radius:12px;font-size:13px;margin:4px}.badge.warn{background:#d29922}.badge.ready{background:#2ea043}.section{background:#161b22;border:1px solid #30363d;border-radius:8px;margin-bottom:16px;overflow:hidden}.section-title{padding:12px 16px;background:#21262d;font-weight:600;font-size:14px;color:#8b949e;border-bottom:1px solid #30363d}pre{padding:16px;overflow-x:auto;font-size:13px;font-family:'SF Mono',Consolas,monospace;white-space:pre-wrap;word-break:break-all;color:#c9d1d9;max-height:400px;overflow-y:auto}ul{padding:12px 16px 12px 32px;font-size:13px}li{padding:2px 0;color:#8b949e}.tip{background:#1a2332;border:1px solid #3776AB;border-radius:8px;padding:16px;margin-top:16px;font-size:13px;color:#3776AB}</style></head><body>
<div class="header"><h1>🐍 $frameworkLabel 项目</h1><span class="badge">$frameworkLabel</span>$statusBadge</div>
<div class="section"><div class="section-title">📄 $entryFile</div><pre>$escapedSource</pre></div>
${if (escapedReqs.isNotBlank()) """<div class="section"><div class="section-title">📦 requirements.txt</div><pre>$escapedReqs</pre></div>""" else ""}
<div class="section"><div class="section-title">📁 项目文件 (${fileList.size}${if (fileList.size >= 30) "+" else ""})</div><ul>$filesHtml</ul></div>
<div class="tip">💡 此项目是 $frameworkLabel 后端应用。${if (pythonReady) "Python 运行时已就绪，可直接启动服务器。" else "需要先下载 Python 运行时才能运行。"}</div>
</body></html>"""
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 验证 Python 二进制是否可执行
     * 通过 musl linker 运行 python --version，返回版本字符串，失败返回 null
     */
    private fun verifyPythonBinary(pythonBin: String, pythonHome: String, muslLinker: String? = null): String? {
        return try {
            // 通过 musl linker 间接执行（Android 上无 /lib/ld-musl-aarch64.so.1）
            val cmd = if (muslLinker != null) {
                listOf(muslLinker, "--library-path", "${pythonHome}/lib", pythonBin, "--version")
            } else {
                listOf(pythonBin, "--version")
            }
            AppLogger.d(TAG, "verifyPythonBinary: ${cmd.joinToString(" ")}")
            val pb = ProcessBuilder(cmd)
            val env = pb.environment()
            env["PYTHONHOME"] = pythonHome
            env["PYTHONPATH"] = "${pythonHome}/lib/python${PythonDependencyManager.PYTHON_VERSION}"
            env["LD_LIBRARY_PATH"] = "${pythonHome}/lib"
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            pb.redirectErrorStream(true)
            val process = pb.start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            AppLogger.d(TAG, "Python --version: exitCode=$exitCode, output='$output'")
            if (exitCode == 0 && output.contains("Python", ignoreCase = true)) {
                output
            } else if (output.isNotEmpty()) {
                AppLogger.e(TAG, "Python --version 输出异常: exitCode=$exitCode, output='$output'")
                null
            } else {
                AppLogger.e(TAG, "Python --version 无输出, exitCode=$exitCode" +
                    (if (muslLinker == null) " (无 musl linker，二进制可能无法直接执行)" else ""))
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "验证 Python 二进制失败", e)
            null
        }
    }
    
    /**
     * 根据框架类型构建 Python 启动命令
     * 在 Android 上通过 musl linker 间接执行 Python（因为 Python 二进制依赖 musl 动态链接器）
     */
    private fun buildPythonCommand(
        pythonBin: String, framework: String, entryFile: String, port: Int,
        muslLinker: String? = null, pythonHome: String? = null
    ): List<String> {
        // musl linker 前缀: ld-musl-aarch64.so.1 --library-path <lib> python3.12 ...
        val linkerPrefix = if (muslLinker != null && pythonHome != null) {
            listOf(muslLinker, "--library-path", "${pythonHome}/lib")
        } else emptyList()
        
        val pythonArgs = when (framework) {
            "flask" -> {
                // Flask：使用引导脚本修复 importlib.metadata 和端口问题
                listOf(pythonBin, "_w2a_bootstrap.py", entryFile, port.toString())
            }
            "django" -> {
                listOf(pythonBin, "manage.py", "runserver", "127.0.0.1:$port", "--noreload")
            }
            "fastapi" -> {
                val appModule = entryFile.removeSuffix(".py")
                listOf(pythonBin, "-m", "uvicorn", "$appModule:app", "--host", "127.0.0.1", "--port", port.toString())
            }
            "tornado" -> {
                listOf(pythonBin, "_w2a_bootstrap.py", entryFile, port.toString())
            }
            else -> {
                listOf(pythonBin, "_w2a_bootstrap.py", entryFile, port.toString())
            }
        }
        
        return linkerPrefix + pythonArgs
    }
    
    /**
     * 创建 Python 引导脚本
     * 修复 Android 运行时的两个关键问题:
     * 1. importlib.metadata.PackageNotFoundError - 通过 --target 安装的包缺少 .dist-info 元数据
     * 2. 端口硬编码 - Flask app.run() 中硬编码的端口与 WebToApp 分配的端口不一致
     */
    private fun createBootstrapScript(projectDir: File, port: Int) {
        val bootstrapFile = File(projectDir, "_w2a_bootstrap.py")
        bootstrapFile.writeText("""#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# WebToApp Python Bootstrap - fixes Android runtime issues
import os, sys

# === 1. Patch importlib.metadata for --target installed packages ===
# When packages are installed via 'pip install --target .pypackages',
# the .dist-info directories may be missing, causing PackageNotFoundError.
try:
    import importlib.metadata
    _orig_version = importlib.metadata.version
    _orig_distribution = importlib.metadata.distribution
    
    def _patched_version(name):
        try:
            return _orig_version(name)
        except importlib.metadata.PackageNotFoundError:
            # Try to find __version__ from the package module directly
            try:
                mod = __import__(name.replace('-', '_'))
                if hasattr(mod, '__version__'):
                    return mod.__version__
            except (ImportError, Exception):
                pass
            return "0.0.0"
    
    importlib.metadata.version = _patched_version
except Exception:
    pass

# === 2. Fix port for Flask/Werkzeug ===
# WebToApp allocates a dynamic port, but app.py may hardcode a different one.
_w2a_port = int(sys.argv[2]) if len(sys.argv) > 2 else int(os.environ.get('PORT', '5000'))
os.environ['PORT'] = str(_w2a_port)

try:
    import flask
    _orig_run = flask.Flask.run
    def _patched_run(self, host=None, port=None, **kwargs):
        _orig_run(self, host='127.0.0.1', port=_w2a_port, **kwargs)
    flask.Flask.run = _patched_run
except ImportError:
    pass

# === 3. Execute the actual entry file ===
entry_file = sys.argv[1] if len(sys.argv) > 1 else 'app.py'
sys.argv = [entry_file]  # Reset argv so the app sees clean arguments

if os.path.exists(entry_file):
    with open(entry_file) as f:
        code = compile(f.read(), entry_file, 'exec')
        exec(code, {'__name__': '__main__', '__file__': entry_file})
else:
    print(f"Error: Entry file not found: {entry_file}", file=sys.stderr)
    sys.exit(1)
""")
        AppLogger.d(TAG, "Created bootstrap script: ${bootstrapFile.absolutePath}")
    }
    
    /**
     * 检测 Django settings 模块
     */
    private fun detectDjangoSettings(projectDir: File): String {
        // 查找包含 settings.py 的子目录
        projectDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && File(dir, "settings.py").exists()) {
                return "${dir.name}.settings"
            }
        }
        return "config.settings"
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
                    AppLogger.i(TAG, "Python 服务器就绪 (尝试 ${attempt + 1})")
                    return true
                }
            } catch (e: Exception) {
                AppLogger.d(TAG, "Health check attempt failed: ${e.message}")
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
            
            pythonProcess?.let { process ->
                if (!process.isAliveCompat()) {
                    val exitCode = try { process.exitValue() } catch (_: Exception) { -1 }
                    Thread.sleep(200)
                    val stdout = pythonOutputBuffer.toString().trim().ifEmpty { "(no stdout)" }
                    val stderr = pythonStderrBuffer.toString().trim().ifEmpty { "(no stderr)" }
                    AppLogger.e(TAG, "Python 进程意外退出, exitCode=$exitCode\nstdout: $stdout\nstderr: $stderr")
                    return false
                }
            }
            delay(HEALTH_CHECK_INTERVAL_MS)
        }
        AppLogger.e(TAG, "Python 服务器启动超时 (${MAX_HEALTH_CHECK_RETRIES * HEALTH_CHECK_INTERVAL_MS}ms)")
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
