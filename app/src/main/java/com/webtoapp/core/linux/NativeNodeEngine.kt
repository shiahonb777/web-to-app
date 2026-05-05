package com.webtoapp.core.linux

import android.content.Context
import android.os.Build
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.destroyForciblyCompat
import com.webtoapp.util.waitForCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL















object NativeNodeEngine {

    private const val TAG = "NativeNodeEngine"


    private const val NODE_VERSION = "18.19.0"



    private val NODE_DOWNLOAD_URLS = mapOf(
        "arm64-v8a" to "https://github.com/nicolo-ribaudo/pnpm-prebuilt-android/releases/download/v8.15.4/pnpm-android-arm64",
        "armeabi-v7a" to "https://github.com/nicolo-ribaudo/pnpm-prebuilt-android/releases/download/v8.15.4/pnpm-android-arm"
    )


    private val ESBUILD_DOWNLOAD_URLS = mapOf(
        "arm64-v8a" to "https://registry.npmmirror.com/@esbuild/android-arm64/-/android-arm64-0.20.0.tgz",
        "armeabi-v7a" to "https://registry.npmmirror.com/@esbuild/android-arm/-/android-arm-0.20.0.tgz",
        "x86_64" to "https://registry.npmmirror.com/@esbuild/android-x64/-/android-x64-0.20.0.tgz"
    )


    private val _state = MutableStateFlow<NodeEngineState>(NodeEngineState.NotInitialized)
    val state: StateFlow<NodeEngineState> = _state




    fun getArchitecture(): String {
        return when {
            Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "arm64-v8a"
            Build.SUPPORTED_ABIS.contains("armeabi-v7a") -> "armeabi-v7a"
            Build.SUPPORTED_ABIS.contains("x86_64") -> "x86_64"
            Build.SUPPORTED_ABIS.contains("x86") -> "x86"
            else -> "arm64-v8a"
        }
    }




    private fun getEngineDir(context: Context): File {
        return File(context.filesDir, "node_engine")
    }




    fun getEsbuildPath(context: Context): File {
        return File(getEngineDir(context), "esbuild")
    }




    fun isAvailable(context: Context): Boolean {
        val esbuild = getEsbuildPath(context)
        return esbuild.exists() && esbuild.canExecute()
    }









    suspend fun initialize(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = NodeEngineState.Initializing("检查环境", 0f)

            val engineDir = getEngineDir(context)
            engineDir.mkdirs()


            if (isAvailable(context)) {
                _state.value = NodeEngineState.Ready
                return@withContext Result.success(Unit)
            }


            _state.value = NodeEngineState.Initializing(Strings.nodeDownloadEsbuild, 0.1f)
            onProgress(Strings.nodeDownloadEsbuild, 0.1f)

            downloadEsbuild(context) { progress ->
                _state.value = NodeEngineState.Initializing(Strings.nodeDownloadEsbuild, 0.1f + progress * 0.8f)
                onProgress(Strings.nodeDownloadEsbuild, 0.1f + progress * 0.8f)
            }


            _state.value = NodeEngineState.Initializing(Strings.nodeVerifyInstall, 0.95f)
            onProgress(Strings.nodeVerifyInstall, 0.95f)

            if (!isAvailable(context)) {
                throw Exception(Strings.nodeEsbuildInstallFailed)
            }

            _state.value = NodeEngineState.Ready
            onProgress("Done", 1f)

            AppLogger.w(TAG, "Node 引擎初始化完成")
            Result.success(Unit)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Initialization failed", e)
            _state.value = NodeEngineState.Error(e.message ?: "未知错误")
            Result.failure(e)
        }
    }




    private suspend fun downloadEsbuild(
        context: Context,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val arch = getArchitecture()
        val url = ESBUILD_DOWNLOAD_URLS[arch]
            ?: throw Exception(Strings.nodeUnsupportedArch.format(arch))

        val engineDir = getEngineDir(context)
        val tempFile = File(engineDir, "esbuild.tgz")
        val esbuildFile = getEsbuildPath(context)

        try {

            AppLogger.w(TAG, "下载 esbuild: $url")
            downloadFile(url, tempFile, onProgress)


            extractEsbuildFromTgz(tempFile, esbuildFile)


            esbuildFile.setExecutable(true, false)

            AppLogger.w(TAG, "esbuild 安装完成: ${esbuildFile.absolutePath}")

        } finally {
            tempFile.delete()
        }
    }




    private fun extractEsbuildFromTgz(tgzFile: File, outputFile: File) {

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
        throw Exception(Strings.nodeEsbuildBinaryNotFound)
    }




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
                stderr = Strings.nodeEsbuildNotInstalled,
                duration = 0
            )
        }

        val cmdList = mutableListOf(esbuild.absolutePath)
        cmdList.addAll(args)

        AppLogger.d(TAG, "执行: ${cmdList.joinToString(" ")}")

        val processBuilder = ProcessBuilder(cmdList)
        processBuilder.directory(workingDir)
        processBuilder.redirectErrorStream(false)


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

        val completed = process.waitForCompat(timeout)

        stdoutReader.join(1000)
        stderrReader.join(1000)

        val exitCode = if (completed) process.exitValue() else {
            process.destroyForciblyCompat()
            -1
        }

        ExecutionResult(
            exitCode = exitCode,
            stdout = stdout.toString(),
            stderr = stderr.toString(),
            duration = System.currentTimeMillis() - startTime
        )
    }




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




sealed class NodeEngineState {
    object NotInitialized : NodeEngineState()
    data class Initializing(val step: String, val progress: Float) : NodeEngineState()
    object Ready : NodeEngineState()
    data class Error(val message: String) : NodeEngineState()
}




data class ExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val duration: Long
) {
    val isSuccess: Boolean get() = exitCode == 0
}
