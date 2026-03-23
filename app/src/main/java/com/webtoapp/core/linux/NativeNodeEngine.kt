package com.webtoapp.core.linux

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Native Node.js 引擎
 * 
 * 第一性原理设计：
 * - 不需要 PRoot
 * - 不需要 Linux rootfs
 * - 直接使用为 Android 编译的 Node.js 二进制
 * - Node.js 作为 native library 打包或运行时下载
 * 
 * 核心原理：
 * Node.js 可以被编译为 Android 可执行文件
 * 我们使用 nodejs-mobile 项目提供的预编译二进制
 * 或者使用 esbuild 等纯 JavaScript 工具链
 */
object NativeNodeEngine {
    
    private const val TAG = "NativeNodeEngine"
    
    // Node.js 版本
    private const val NODE_VERSION = "18.19.0"
    
    // nodejs-mobile 预编译二进制下载源
    // 这是专门为 Android 编译的 Node.js
    private val NODE_DOWNLOAD_URLS = mapOf(
        "arm64-v8a" to "https://github.com/nicolo-ribaudo/pnpm-prebuilt-android/releases/download/v8.15.4/pnpm-android-arm64",
        "armeabi-v7a" to "https://github.com/nicolo-ribaudo/pnpm-prebuilt-android/releases/download/v8.15.4/pnpm-android-arm"
    )
    
    // 备用：使用 esbuild 作为构建工具（纯 Go 编译，支持 Android）
    private val ESBUILD_DOWNLOAD_URLS = mapOf(
        "arm64-v8a" to "https://registry.npmmirror.com/@esbuild/android-arm64/-/android-arm64-0.20.0.tgz",
        "armeabi-v7a" to "https://registry.npmmirror.com/@esbuild/android-arm/-/android-arm-0.20.0.tgz",
        "x86_64" to "https://registry.npmmirror.com/@esbuild/android-x64/-/android-x64-0.20.0.tgz"
    )
    
    // 状态
    private val _state = MutableStateFlow<NodeEngineState>(NodeEngineState.NotInitialized)
    val state: StateFlow<NodeEngineState> = _state
    
    /**
     * 获取当前架构
     */
    fun getArchitecture(): String {
        return when {
            Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "arm64-v8a"
            Build.SUPPORTED_ABIS.contains("armeabi-v7a") -> "armeabi-v7a"
            Build.SUPPORTED_ABIS.contains("x86_64") -> "x86_64"
            Build.SUPPORTED_ABIS.contains("x86") -> "x86"
            else -> "arm64-v8a"
        }
    }
    
    /**
     * 获取引擎目录
     */
    private fun getEngineDir(context: Context): File {
        return File(context.filesDir, "node_engine")
    }
    
    /**
     * 获取 esbuild 可执行文件路径
     */
    fun getEsbuildPath(context: Context): File {
        return File(getEngineDir(context), "esbuild")
    }
    
    /**
     * 检查引擎是否可用
     */
    fun isAvailable(context: Context): Boolean {
        val esbuild = getEsbuildPath(context)
        return esbuild.exists() && esbuild.canExecute()
    }
    
    /**
     * 初始化引擎
     * 
     * 策略：
     * 1. 首先检查是否已有可用的构建工具
     * 2. 如果没有，下载 esbuild（纯 Go 编译，无依赖）
     * 3. esbuild 可以直接构建 Vue/React/Vite 项目
     */
    suspend fun initialize(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = NodeEngineState.Initializing("检查环境", 0f)
            
            val engineDir = getEngineDir(context)
            engineDir.mkdirs()
            
            // Check是否已安装
            if (isAvailable(context)) {
                _state.value = NodeEngineState.Ready
                return@withContext Result.success(Unit)
            }
            
            // Download esbuild
            _state.value = NodeEngineState.Initializing("下载构建工具", 0.1f)
            onProgress("下载 esbuild...", 0.1f)
            
            downloadEsbuild(context) { progress ->
                _state.value = NodeEngineState.Initializing("下载构建工具", 0.1f + progress * 0.8f)
                onProgress("下载 esbuild...", 0.1f + progress * 0.8f)
            }
            
            // Verify
            _state.value = NodeEngineState.Initializing("验证", 0.95f)
            onProgress("验证安装...", 0.95f)
            
            if (!isAvailable(context)) {
                throw Exception("esbuild 安装失败")
            }
            
            _state.value = NodeEngineState.Ready
            onProgress("Done", 1f)
            
            Log.i(TAG, "Node 引擎初始化完成")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            _state.value = NodeEngineState.Error(e.message ?: "未知错误")
            Result.failure(e)
        }
    }
    
    /**
     * 下载 esbuild
     */
    private suspend fun downloadEsbuild(
        context: Context,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val arch = getArchitecture()
        val url = ESBUILD_DOWNLOAD_URLS[arch] 
            ?: throw Exception("不支持的架构: $arch")
        
        val engineDir = getEngineDir(context)
        val tempFile = File(engineDir, "esbuild.tgz")
        val esbuildFile = getEsbuildPath(context)
        
        try {
            // Download
            Log.i(TAG, "下载 esbuild: $url")
            downloadFile(url, tempFile, onProgress)
            
            // Decompression tgz
            extractEsbuildFromTgz(tempFile, esbuildFile)
            
            // Set执行权限
            esbuildFile.setExecutable(true, false)
            
            Log.i(TAG, "esbuild 安装完成: ${esbuildFile.absolutePath}")
            
        } finally {
            tempFile.delete()
        }
    }
    
    /**
     * 从 tgz 中提取 esbuild 二进制
     */
    private fun extractEsbuildFromTgz(tgzFile: File, outputFile: File) {
        // npm 包结构: package/bin/esbuild
        java.util.zip.GZIPInputStream(FileInputStream(tgzFile)).use { gzip ->
            org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzip).use { tar ->
                var entry = tar.nextTarEntry
                while (entry != null) {
                    if (entry.name.endsWith("/esbuild") || entry.name == "package/bin/esbuild") {
                        FileOutputStream(outputFile).use { out ->
                            tar.copyTo(out)
                        }
                        return
                    }
                    entry = tar.nextTarEntry
                }
            }
        }
        throw Exception("在包中找不到 esbuild 二进制文件")
    }
    
    /**
     * 执行 esbuild 命令
     */
    suspend fun executeEsbuild(
        context: Context,
        args: List<String>,
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        timeout: Long = 300_000,
        onOutput: (String) -> Unit = {}
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        val esbuild = getEsbuildPath(context)
        if (!esbuild.exists()) {
            return@withContext ExecutionResult(
                exitCode = -1,
                stdout = "",
                stderr = "esbuild 未安装",
                duration = 0
            )
        }
        
        val cmdList = mutableListOf(esbuild.absolutePath)
        cmdList.addAll(args)
        
        Log.d(TAG, "执行: ${cmdList.joinToString(" ")}")
        
        val processBuilder = ProcessBuilder(cmdList)
        processBuilder.directory(workingDir)
        processBuilder.redirectErrorStream(false)
        
        // 环境变量
        val processEnv = processBuilder.environment()
        processEnv["HOME"] = context.filesDir.absolutePath
        processEnv["TMPDIR"] = context.cacheDir.absolutePath
        env.forEach { (k, v) -> processEnv[k] = v }
        
        val process = processBuilder.start()
        
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        
        val stdoutReader = Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                stdout.appendLine(line)
                onOutput(line)
            }
        }
        
        val stderrReader = Thread {
            process.errorStream.bufferedReader().forEachLine { line ->
                stderr.appendLine(line)
            }
        }
        
        stdoutReader.start()
        stderrReader.start()
        
        val completed = process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
        
        stdoutReader.join(1000)
        stderrReader.join(1000)
        
        val exitCode = if (completed) process.exitValue() else {
            process.destroyForcibly()
            -1
        }
        
        ExecutionResult(
            exitCode = exitCode,
            stdout = stdout.toString(),
            stderr = stderr.toString(),
            duration = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * 下载文件
     */
    private suspend fun downloadFile(
        url: String,
        target: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = true
        
        try {
            conn.connect()
            
            // Handle重定向
            var finalConn = conn
            var responseCode = conn.responseCode
            var redirectCount = 0
            
            while (responseCode in 300..399 && redirectCount < 5) {
                val newUrl = conn.getHeaderField("Location")
                finalConn.disconnect()
                finalConn = URL(newUrl).openConnection() as HttpURLConnection
                finalConn.connectTimeout = 30000
                finalConn.readTimeout = 60000
                finalConn.connect()
                responseCode = finalConn.responseCode
                redirectCount++
            }
            
            if (responseCode != 200) {
                throw Exception("HTTP $responseCode")
            }
            
            val total = finalConn.contentLength.toLong()
            var downloaded = 0L
            
            target.parentFile?.mkdirs()
            
            finalConn.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
    
    /**
     * 重置引擎
     */
    suspend fun reset(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = NodeEngineState.NotInitialized
            getEngineDir(context).deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 引擎状态
 */
sealed class NodeEngineState {
    object NotInitialized : NodeEngineState()
    data class Initializing(val step: String, val progress: Float) : NodeEngineState()
    object Ready : NodeEngineState()
    data class Error(val message: String) : NodeEngineState()
}

/**
 * 执行结果
 */
data class ExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val duration: Long
) {
    val isSuccess: Boolean get() = exitCode == 0
}
