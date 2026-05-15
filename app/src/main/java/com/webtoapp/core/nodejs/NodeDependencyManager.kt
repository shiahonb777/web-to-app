package com.webtoapp.core.nodejs

import android.content.Context
import android.os.Build
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.download.DependencyDownloadEngine
import com.webtoapp.core.download.DependencyDownloadNotification
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale















object NodeDependencyManager {

    private const val TAG = "NodeDependencyManager"






    const val NODE_VERSION = "18.20.4"



    enum class MirrorRegion { CN, GLOBAL }


    // CN mirror proxies verified reachable on 2026-05-05. ghproxy.cc was removed
    // because its DNS no longer resolves.
    private val GITHUB_CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/"
    )


    private val NODE_GITHUB_URL = "https://github.com/nodejs-mobile/nodejs-mobile/releases/download/v${NODE_VERSION}/nodejs-mobile-v${NODE_VERSION}-android.zip"

    data class MirrorConfig(

        val nodeUrls: List<String>
    )





    private fun buildCnMirror(): MirrorConfig {
        val orderedProxies = com.webtoapp.core.network.CnMirrorProbe.getOrderedProxies(GITHUB_CN_PROXIES)
        return MirrorConfig(
            nodeUrls = orderedProxies.map { proxy -> "${proxy}${NODE_GITHUB_URL}" } + NODE_GITHUB_URL
        )
    }





    private val GLOBAL_MIRROR = MirrorConfig(
        nodeUrls = listOf(NODE_GITHUB_URL)
    )


    private const val MAX_RETRY_PER_URL = 2


    private const val RETRY_DELAY_MS = 2000L



    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Verifying(val fileName: String) : DownloadState()
        data class Extracting(val fileName: String) : DownloadState()
        object Complete : DownloadState()
        data class Error(val message: String, val retryable: Boolean = true) : DownloadState()

        data class Paused(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    private val runtimeDownloadMutex = Mutex()


    private var _userMirrorRegion: MirrorRegion? = null






    fun setMirrorRegion(region: MirrorRegion?) {
        _userMirrorRegion = region
    }




    fun getMirrorRegion(): MirrorRegion {
        _userMirrorRegion?.let { return it }
        val lang = Locale.getDefault().language
        return if (lang == "zh") MirrorRegion.CN else MirrorRegion.GLOBAL
    }




    fun getMirrorConfig(): MirrorConfig {
        return when (getMirrorRegion()) {
            MirrorRegion.CN -> buildCnMirror()
            MirrorRegion.GLOBAL -> GLOBAL_MIRROR
        }
    }




    fun getDepsDir(context: Context): File {
        return File(context.filesDir, "nodejs_deps").also { it.mkdirs() }
    }




    fun getNodeDir(context: Context): File {
        val abi = getDeviceAbi()
        return File(getDepsDir(context), "node/$abi").also { it.mkdirs() }
    }




    fun getNodeProjectsDir(context: Context): File {
        return File(context.filesDir, "nodejs_projects").also { it.mkdirs() }
    }




    const val NODE_BINARY_NAME = "libnode.so"





    fun isNodeReady(context: Context): Boolean {

        val nativeNode = File(context.applicationInfo.nativeLibraryDir, NODE_BINARY_NAME)
        if (nativeNode.exists()) return true

        val cachedNode = File(getNodeDir(context), NODE_BINARY_NAME)
        return cachedNode.exists()
    }





    fun getNodeExecutablePath(context: Context): String {
        val nativeNode = File(context.applicationInfo.nativeLibraryDir, NODE_BINARY_NAME)
        if (nativeNode.exists()) {
            AppLogger.d(TAG, "使用 nativeLibraryDir Node: ${nativeNode.absolutePath}")
            return nativeNode.absolutePath
        }
        val cachedNode = File(getNodeDir(context), NODE_BINARY_NAME)
        AppLogger.d(TAG, "使用下载缓存 Node: ${cachedNode.absolutePath}")
        return cachedNode.absolutePath
    }






    fun getNodeLibraryPath(context: Context): String? {

        val nativeNode = File(context.applicationInfo.nativeLibraryDir, NODE_BINARY_NAME)
        if (nativeNode.exists()) {
            AppLogger.d(TAG, "libnode.so 路径 (nativeLibraryDir): ${nativeNode.absolutePath}")
            return nativeNode.absolutePath
        }

        val cachedNode = File(getNodeDir(context), NODE_BINARY_NAME)
        if (cachedNode.exists()) {
            AppLogger.d(TAG, "libnode.so 路径 (下载缓存): ${cachedNode.absolutePath}")
            return cachedNode.absolutePath
        }
        AppLogger.w(TAG, "libnode.so 未找到")
        return null
    }






    suspend fun downloadNodeRuntime(context: Context): Boolean = withContext(Dispatchers.IO) {
        runtimeDownloadMutex.withLock {
            DependencyDownloadNotification.getInstance(context)
            if (isNodeReady(context)) {
                markComplete()
                return@withLock true
            }
            try {
                _downloadState.value = DownloadState.Idle
                DependencyDownloadEngine.reset()
                val mirror = getMirrorConfig()

                if (!isNodeReady(context)) {
                    val success = downloadNode(context, mirror)
                    if (!success) return@withLock false
                }

                markComplete()
                AppLogger.i(TAG, "Node.js 运行时下载完成")
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "下载 Node.js 运行时失败", e)
                markError(e.message ?: "未知错误")
                false
            }
        }
    }




    fun clearCache(context: Context) {
        getDepsDir(context).deleteRecursively()
        AppLogger.i(TAG, "Node.js 依赖缓存已清理")
    }




    fun getCacheSize(context: Context): Long {
        return getDepsDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }






    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }









    private suspend fun downloadWithRetry(
        urls: List<String>,
        destFile: File,
        displayName: String,
        context: Context?
    ): Boolean {
        for ((urlIndex, url) in urls.withIndex()) {
            val sourceName = if (urls.size > 1) "$displayName [源${urlIndex + 1}/${urls.size}]" else displayName
            AppLogger.i(TAG, "尝试下载 $sourceName: $url")

            for (attempt in 1..MAX_RETRY_PER_URL) {
                val success = DependencyDownloadEngine.downloadFile(url, destFile, sourceName, context)
                if (success) return true

                if (attempt < MAX_RETRY_PER_URL) {
                    AppLogger.i(TAG, "$sourceName 下载失败, ${RETRY_DELAY_MS / 1000}s 后重试 ($attempt/$MAX_RETRY_PER_URL)")
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                    DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Idle
                }
            }

            if (urlIndex < urls.lastIndex) {
                val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
                tmpFile.delete()
                AppLogger.i(TAG, "$sourceName 失败，切换下一个源...")
                DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Idle
            }
        }
        return false
    }

    private suspend fun downloadNode(context: Context, mirror: MirrorConfig): Boolean {
        val abi = getDeviceAbi()
        val nodeUrls = mirror.nodeUrls
        val fileName = nodeUrls.first().substringAfterLast("/")
        val destDir = getNodeDir(context)
        val archiveFile = File(getDepsDir(context), fileName)

        AppLogger.i(TAG, "下载 Node.js 运行时 (共 ${nodeUrls.size} 个源)")


        val downloaded = downloadWithRetry(nodeUrls, archiveFile, "Node.js $NODE_VERSION ($abi)", context)
        syncEngineState()
        if (!downloaded) return false


        _downloadState.value = DownloadState.Extracting("Node.js")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("Node.js")
        try {
            extractNodeZip(archiveFile, destDir, abi)


            val nodeLib = File(destDir, NODE_BINARY_NAME)
            if (nodeLib.exists()) {
                nodeLib.setExecutable(true, false)
                AppLogger.i(TAG, "Node.js 运行时已就绪: ${nodeLib.absolutePath} (${nodeLib.length()} bytes)")
            } else {
                AppLogger.e(TAG, "解压后未找到 $NODE_BINARY_NAME (ABI: $abi)")
                markError("解压后未找到 Node.js 运行时 (ABI: $abi)")
                return false
            }


            archiveFile.delete()
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 Node.js 失败", e)
            markError("解压 Node.js 失败: ${e.message}")
            return false
        }
    }






    private fun extractNodeZip(zipFile: File, destDir: File, abi: String) {
        val zipInput = java.util.zip.ZipInputStream(zipFile.inputStream().buffered())
        var foundLib = false

        zipInput.use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {

                if (!entry.isDirectory && entry.name.contains(abi) && entry.name.endsWith("libnode.so")) {
                    val outFile = File(destDir, NODE_BINARY_NAME)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    outFile.setExecutable(true, false)
                    foundLib = true
                    AppLogger.i(TAG, "提取 ${entry.name} -> ${outFile.absolutePath}")
                } else if (!entry.isDirectory && entry.name.endsWith(".so") && entry.name.contains(abi)) {

                    val soName = entry.name.substringAfterLast("/")
                    val outFile = File(destDir, soName)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }

        if (!foundLib) {
            throw IllegalStateException(Strings.nodeLibNotFoundInZip.format(abi))
        }
    }




    private fun syncEngineState() {
        when (val es = DependencyDownloadEngine.state.value) {
            is DependencyDownloadEngine.State.Downloading -> {
                _downloadState.value = DownloadState.Downloading(
                    progress = es.progress,
                    currentFile = es.displayName,
                    bytesDownloaded = es.bytesDownloaded,
                    totalBytes = es.totalBytes
                )
            }
            is DependencyDownloadEngine.State.Paused -> {
                _downloadState.value = DownloadState.Paused(
                    progress = es.progress,
                    currentFile = es.displayName,
                    bytesDownloaded = es.bytesDownloaded,
                    totalBytes = es.totalBytes
                )
            }
            is DependencyDownloadEngine.State.Error -> {
                _downloadState.value = DownloadState.Error(es.message)
            }
            else -> {}
        }
    }

    private fun markComplete() {
        _downloadState.value = DownloadState.Complete
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Complete
    }

    private fun markError(message: String) {
        _downloadState.value = DownloadState.Error(message)
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Error(message)
    }




    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
