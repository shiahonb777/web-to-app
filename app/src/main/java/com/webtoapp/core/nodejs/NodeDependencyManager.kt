package com.webtoapp.core.nodejs

import android.content.Context
import android.os.Build
import com.webtoapp.core.download.DependencyDownloadEngine
import com.webtoapp.core.download.DependencyDownloadNotification
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * Node.js 依赖管理器
 * 
 * 负责按需下载 Node.js 预编译二进制。
 * 使用 nodejs-mobile 项目提供的 Android 原生 Node.js 构建（libnode.so）。
 * libnode.so 是共享库（shared library），通过 JNI dlopen + node::Start() 在进程内启动。
 * 
 * 加载策略：
 * - 主应用预览：从下载缓存 (nodejs_deps/node/{abi}/libnode.so) 通过 dlopen 加载
 * - 导出 APK (Shell 模式)：打包为 lib/{abi}/libnode.so，安装后位于 nativeLibraryDir
 *   通过 dlopen 加载（Android 10+ W^X / Android 15+ SELinux 安全）
 * 
 * 根据设备语言自动选择国内镜像或国际源。
 */
object NodeDependencyManager {
    
    private const val TAG = "NodeDependencyManager"
    
    /**
     * Node.js 版本 (nodejs-mobile 提供 Android 原生构建)
     * 来源: github.com/nodejs-mobile/nodejs-mobile
     * 格式: libnode.so（共享库，通过 dlopen + node::Start() 调用）
     */
    const val NODE_VERSION = "18.20.4"
    
    // ==================== 镜像源配置 ====================
    
    enum class MirrorRegion { CN, GLOBAL }
    
    /** 国内 GitHub 代理加速列表（按优先级排序） */
    private val GITHUB_CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "https://ghproxy.cc/"
    )
    
    /** GitHub 原始 Node.js 下载地址 */
    private val NODE_GITHUB_URL = "https://github.com/nodejs-mobile/nodejs-mobile/releases/download/v${NODE_VERSION}/nodejs-mobile-v${NODE_VERSION}-android.zip"
    
    data class MirrorConfig(
        /** nodejs-mobile 下载 URL 列表（按优先级排序，支持多源 fallback） */
        val nodeUrls: List<String>
    )
    
    /**
     * 国内镜像源
     * nodejs-mobile GitHub 代理加速 + 原始地址 fallback
     */
    private val CN_MIRROR = MirrorConfig(
        nodeUrls = GITHUB_CN_PROXIES.map { proxy -> "${proxy}${NODE_GITHUB_URL}" } + NODE_GITHUB_URL
    )
    
    /**
     * 国际源
     * nodejs-mobile GitHub Releases 直连
     */
    private val GLOBAL_MIRROR = MirrorConfig(
        nodeUrls = listOf(NODE_GITHUB_URL)
    )
    
    /** 每个 URL 最大重试次数 */
    private const val MAX_RETRY_PER_URL = 2
    
    /** 重试延迟（毫秒） */
    private const val RETRY_DELAY_MS = 2000L
    
    // ==================== 下载状态 ====================
    
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Verifying(val fileName: String) : DownloadState()
        data class Extracting(val fileName: String) : DownloadState()
        object Complete : DownloadState()
        data class Error(val message: String, val retryable: Boolean = true) : DownloadState()
        /** 暂停状态 */
        data class Paused(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    }
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    /** 用户手动设置的镜像区域（null = 自动检测） */
    private var _userMirrorRegion: MirrorRegion? = null
    
    // ==================== 公开 API ====================
    
    /**
     * 手动设置镜像区域
     */
    fun setMirrorRegion(region: MirrorRegion?) {
        _userMirrorRegion = region
    }
    
    /**
     * 获取当前镜像区域（自动检测或用户设置）
     */
    fun getMirrorRegion(): MirrorRegion {
        _userMirrorRegion?.let { return it }
        val lang = Locale.getDefault().language
        return if (lang == "zh") MirrorRegion.CN else MirrorRegion.GLOBAL
    }
    
    /**
     * 获取当前镜像配置
     */
    fun getMirrorConfig(): MirrorConfig {
        return when (getMirrorRegion()) {
            MirrorRegion.CN -> CN_MIRROR
            MirrorRegion.GLOBAL -> GLOBAL_MIRROR
        }
    }
    
    /**
     * 获取依赖缓存目录
     */
    fun getDepsDir(context: Context): File {
        return File(context.filesDir, "nodejs_deps").also { it.mkdirs() }
    }
    
    /**
     * 获取 Node.js 二进制存储目录
     */
    fun getNodeDir(context: Context): File {
        val abi = getDeviceAbi()
        return File(getDepsDir(context), "node/$abi").also { it.mkdirs() }
    }
    
    /**
     * 获取 Node.js 项目根目录
     */
    fun getNodeProjectsDir(context: Context): File {
        return File(context.filesDir, "nodejs_projects").also { it.mkdirs() }
    }
    
    /**
     * Node.js 二进制文件名（在下载缓存和 nativeLibraryDir 中的名称）
     */
    const val NODE_BINARY_NAME = "libnode.so"
    
    /**
     * 检查 Node.js 运行时是否已下载就绪
     * 优先检查 nativeLibraryDir（有 SELinux 安全标签），再检查下载目录。
     */
    fun isNodeReady(context: Context): Boolean {
        // 1. 检查 nativeLibraryDir (bundled via jniLibs, SELinux apk_data_file label)
        val nativeNode = File(context.applicationInfo.nativeLibraryDir, NODE_BINARY_NAME)
        if (nativeNode.exists()) return true
        // 2. 检查下载缓存
        val cachedNode = File(getNodeDir(context), NODE_BINARY_NAME)
        return cachedNode.exists()
    }
    
    /**
     * 获取 Node.js 二进制执行路径
     * 优先使用 nativeLibraryDir（SELinux 允许 execute_no_trans），回退到下载目录
     */
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
    
    /**
     * 获取 libnode.so 共享库路径（用于 dlopen 加载）
     * 优先使用 nativeLibraryDir，回退到下载缓存目录
     * @return 完整路径，如果文件不存在返回 null
     */
    fun getNodeLibraryPath(context: Context): String? {
        // 1. nativeLibraryDir（导出 APK 或主应用内置）
        val nativeNode = File(context.applicationInfo.nativeLibraryDir, NODE_BINARY_NAME)
        if (nativeNode.exists()) {
            AppLogger.d(TAG, "libnode.so 路径 (nativeLibraryDir): ${nativeNode.absolutePath}")
            return nativeNode.absolutePath
        }
        // 2. 下载缓存
        val cachedNode = File(getNodeDir(context), NODE_BINARY_NAME)
        if (cachedNode.exists()) {
            AppLogger.d(TAG, "libnode.so 路径 (下载缓存): ${cachedNode.absolutePath}")
            return cachedNode.absolutePath
        }
        AppLogger.w(TAG, "libnode.so 未找到")
        return null
    }
    
    /**
     * 下载 Node.js 运行时
     * 
     * @return true 如果运行时就绪
     */
    suspend fun downloadNodeRuntime(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Idle
            DependencyDownloadNotification.getInstance(context)
            DependencyDownloadEngine.reset()
            val mirror = getMirrorConfig()
            
            if (!isNodeReady(context)) {
                val success = downloadNode(context, mirror)
                if (!success) return@withContext false
            }
            
            _downloadState.value = DownloadState.Complete
            AppLogger.i(TAG, "Node.js 运行时下载完成")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 Node.js 运行时失败", e)
            _downloadState.value = DownloadState.Error(e.message ?: "未知错误")
            false
        }
    }
    
    /**
     * 清理所有依赖缓存
     */
    fun clearCache(context: Context) {
        getDepsDir(context).deleteRecursively()
        AppLogger.i(TAG, "Node.js 依赖缓存已清理")
    }
    
    /**
     * 获取缓存占用大小（字节）
     */
    fun getCacheSize(context: Context): Long {
        return getDepsDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 获取设备主 ABI
     */
    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }
    
    /**
     * 下载 Node.js 运行时
     * 来源: github.com/nodejs-mobile/nodejs-mobile
     * 格式: .zip 包含各 ABI 目录下的 libnode.so
     */
    /**
     * 带多源 fallback + 重试的下载封装
     */
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
        
        // 下载 zip 包（多源 fallback + 重试）
        val downloaded = downloadWithRetry(nodeUrls, archiveFile, "Node.js $NODE_VERSION ($abi)", context)
        syncEngineState()
        if (!downloaded) return false
        
        // 解压 zip
        _downloadState.value = DownloadState.Extracting("Node.js")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("Node.js")
        try {
            extractNodeZip(archiveFile, destDir, abi)
            
            // 检查 libnode.so 是否就绪
            val nodeLib = File(destDir, NODE_BINARY_NAME)
            if (nodeLib.exists()) {
                nodeLib.setExecutable(true, false)
                AppLogger.i(TAG, "Node.js 运行时已就绪: ${nodeLib.absolutePath} (${nodeLib.length()} bytes)")
            } else {
                AppLogger.e(TAG, "解压后未找到 $NODE_BINARY_NAME (ABI: $abi)")
                _downloadState.value = DownloadState.Error("解压后未找到 Node.js 运行时 (ABI: $abi)")
                return false
            }
            
            // 清理压缩包
            archiveFile.delete()
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 Node.js 失败", e)
            _downloadState.value = DownloadState.Error("解压 Node.js 失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 解压 nodejs-mobile zip 并提取当前 ABI 对应的 libnode.so
     * zip 内部结构: bin/<abi>/libnode.so
     * libnode.so 是共享库，通过 JNI dlopen + node::Start() 调用
     */
    private fun extractNodeZip(zipFile: File, destDir: File, abi: String) {
        val zipInput = java.util.zip.ZipInputStream(zipFile.inputStream().buffered())
        var foundLib = false
        
        zipInput.use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // 查找匹配当前 ABI 的 libnode.so
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
                    // 提取其他与当前 ABI 匹配的 .so 库
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
            throw IllegalStateException("zip 中未找到 $abi/libnode.so")
        }
    }
    
    /**
     * 同步引擎状态到本地 DownloadState
     */
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
    
    /**
     * 计算文件 SHA-256 哈希
     */
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
