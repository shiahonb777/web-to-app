package com.webtoapp.core.wordpress

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
 * WordPress 依赖管理器
 * 
 * 负责按需下载 PHP 二进制、WordPress 核心、SQLite 插件等大体积依赖。
 * 根据设备语言自动选择国内镜像或国际源。
 */
object WordPressDependencyManager {
    
    private const val TAG = "DependencyManager"
    
    /** PHP 版本 (pmmp/PHP-Binaries 提供 Android ARM64 预编译) */
    const val PHP_VERSION = "8.4"
    
    /** WordPress 版本 (2026-02-03 维护更新) */
    const val WORDPRESS_VERSION = "6.9.1"
    
    /** SQLite 插件版本 (2026-02 验证) */
    const val SQLITE_PLUGIN_VERSION = "2.2.17"
    
    // ==================== 镜像源配置 ====================
    
    enum class MirrorRegion { CN, GLOBAL }
    
    /** 国内 GitHub 代理加速列表（按优先级排序，失败自动 fallback） */
    private val GITHUB_CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "https://ghproxy.cc/"
    )
    
    /** GitHub 原始 PHP 下载地址 */
    private val PHP_GITHUB_URL = "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-${PHP_VERSION}-latest/PHP-${PHP_VERSION}-Android-arm64-PM5.tar.gz"
    
    data class MirrorConfig(
        /** PHP 二进制下载 URL 列表（按优先级排序，支持多源 fallback） */
        val phpUrls: List<String>,
        /** WordPress 核心下载 URL 列表（按优先级排序，支持多源 fallback） */
        val wordpressUrls: List<String>,
        val sqlitePluginUrl: String
    )
    
    /**
     * 国内镜像源
     * - PHP: GitHub 代理加速（多个代理源 + 原始 GitHub 地址 fallback）
     * - WordPress: cn.wordpress.org 中文版 → cn.wordpress.org latest → wordpress.org 国际版
     * - SQLite 插件: WordPress 官方插件仓库
     */
    private val CN_MIRROR = MirrorConfig(
        phpUrls = GITHUB_CN_PROXIES.map { proxy -> "${proxy}${PHP_GITHUB_URL}" } + PHP_GITHUB_URL,
        wordpressUrls = listOf(
            "https://cn.wordpress.org/wordpress-${WORDPRESS_VERSION}-zh_CN.tar.gz",
            "https://cn.wordpress.org/latest-zh_CN.tar.gz",
            "https://wordpress.org/wordpress-${WORDPRESS_VERSION}.tar.gz",
            "https://wordpress.org/latest.tar.gz"
        ),
        sqlitePluginUrl = "https://downloads.wordpress.org/plugin/"
    )
    
    /**
     * 国际源
     * - PHP: GitHub pmmp/PHP-Binaries 直连
     * - WordPress: wordpress.org 指定版本 → latest
     * - SQLite 插件: downloads.wordpress.org
     */
    private val GLOBAL_MIRROR = MirrorConfig(
        phpUrls = listOf(PHP_GITHUB_URL),
        wordpressUrls = listOf(
            "https://wordpress.org/wordpress-${WORDPRESS_VERSION}.tar.gz",
            "https://wordpress.org/latest.tar.gz"
        ),
        sqlitePluginUrl = "https://downloads.wordpress.org/plugin/"
    )
    
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
        return File(context.filesDir, "wordpress_deps").also { it.mkdirs() }
    }
    
    /**
     * 获取 PHP 二进制存储目录
     */
    fun getPhpDir(context: Context): File {
        val abi = getDeviceAbi()
        return File(getDepsDir(context), "php/$abi").also { it.mkdirs() }
    }
    
    /**
     * 获取 WordPress 项目根目录
     */
    fun getWordPressProjectsDir(context: Context): File {
        return File(context.filesDir, "wordpress_projects").also { it.mkdirs() }
    }
    
    /**
     * 检查 PHP 二进制是否可用
     * 优先检查 nativeLibraryDir（有 SELinux execute_no_trans 权限），再检查下载目录。
     * Android 15+ (API 35) 强制要求 execute_no_trans 权限，只有 nativeLibraryDir 中的
     * 二进制才有 apk_data_file SELinux 标签可以执行；下载目录 (app_data_file) 无法执行。
     */
    fun isPhpReady(context: Context): Boolean {
        // 检查 nativeLibraryDir (bundled via jniLibs, SELinux apk_data_file label)
        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        if (nativePhp.exists()) return true
        // Android 15+ (API 35): 下载目录的 PHP 二进制无法通过 SELinux execute_no_trans 检查
        if (Build.VERSION.SDK_INT >= 35) return false
        // Android 14 及以下: 下载目录仍可执行
        val phpBinary = File(getPhpDir(context), "php")
        return phpBinary.exists() && phpBinary.canExecute()
    }
    
    /**
     * 获取 PHP 二进制执行路径
     * 
     * 优先使用 nativeLibraryDir 中的 libphp.so（有 SELinux execute_no_trans 权限），
     * 回退到 wordpress_deps 下载目录中的 php 二进制（Android 14 及以下可用）。
     *
     * 在 Android 15+ 上，app_data_file 目录下的二进制被 SELinux 禁止 execute_no_trans，
     * 而 nativeLibraryDir 下的文件具有 apk_data_file 标签，允许 execute_no_trans。
     */
    fun getPhpExecutablePath(context: Context): String {
        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        if (nativePhp.exists()) {
            AppLogger.d(TAG, "使用 nativeLibraryDir PHP: ${nativePhp.absolutePath}")
            return nativePhp.absolutePath
        }
        // 回退到下载目录
        val downloaded = File(getPhpDir(context), "php")
        AppLogger.d(TAG, "使用下载目录 PHP: ${downloaded.absolutePath}")
        return downloaded.absolutePath
    }
    
    /**
     * 检查 WordPress 核心是否已下载
     */
    fun isWordPressReady(context: Context): Boolean {
        val wpDir = File(getDepsDir(context), "wordpress")
        return wpDir.exists() && File(wpDir, "wp-includes/version.php").exists()
    }
    
    /**
     * 检查 SQLite 插件是否已下载
     */
    fun isSqlitePluginReady(context: Context): Boolean {
        val pluginDir = File(getDepsDir(context), "sqlite-database-integration")
        return pluginDir.exists() && File(pluginDir, "load.php").exists()
    }
    
    /**
     * 检查所有依赖是否就绪
     */
    fun isAllReady(context: Context): Boolean {
        return isPhpReady(context) && isWordPressReady(context) && isSqlitePluginReady(context)
    }
    
    /**
     * 下载所有缺失的依赖
     * 
     * @return true 如果所有依赖就绪
     */
    suspend fun downloadAllDependencies(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Idle
            // 初始化通知管理器
            DependencyDownloadNotification.getInstance(context)
            DependencyDownloadEngine.reset()
            val mirror = getMirrorConfig()
            
            // 1. 下载 PHP 二进制
            if (!isPhpReady(context)) {
                val success = downloadPhp(context, mirror)
                if (!success) return@withContext false
            }
            
            // 2. 下载 WordPress 核心
            if (!isWordPressReady(context)) {
                val success = downloadWordPress(context, mirror)
                if (!success) return@withContext false
            }
            
            // 3. 下载 SQLite 插件
            if (!isSqlitePluginReady(context)) {
                val success = downloadSqlitePlugin(context, mirror)
                if (!success) return@withContext false
            }
            
            _downloadState.value = DownloadState.Complete
            AppLogger.i(TAG, "所有 WordPress 依赖下载完成")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载依赖失败", e)
            _downloadState.value = DownloadState.Error(e.message ?: "未知错误")
            false
        }
    }
    
    /**
     * 仅下载 PHP 二进制依赖（供 PHP_APP 预览使用，不下载 WordPress/SQLite）
     *
     * @return true 如果 PHP 二进制已就绪
     */
    suspend fun downloadPhpDependency(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isPhpReady(context)) return@withContext true
            
            _downloadState.value = DownloadState.Idle
            DependencyDownloadNotification.getInstance(context)
            DependencyDownloadEngine.reset()
            val mirror = getMirrorConfig()
            
            val success = downloadPhp(context, mirror)
            if (success) {
                _downloadState.value = DownloadState.Complete
            }
            success
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 PHP 依赖失败", e)
            _downloadState.value = DownloadState.Error(e.message ?: "未知错误")
            false
        }
    }
    
    /**
     * 清理所有依赖缓存
     */
    fun clearCache(context: Context) {
        getDepsDir(context).deleteRecursively()
        AppLogger.i(TAG, "依赖缓存已清理")
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
    
    /** 每个 URL 最大重试次数 */
    private const val MAX_RETRY_PER_URL = 2
    
    /** 重试延迟（毫秒） */
    private const val RETRY_DELAY_MS = 2000L
    
    /**
     * 带多源 fallback + 重试的下载封装
     * 
     * 对每个 URL 重试 [MAX_RETRY_PER_URL] 次，失败则切换到下一个 URL。
     * 断点续传 .tmp 文件在切换源时清除（不同源的 Content-Length 可能不同）。
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
            
            // 切换到下一个源前清除 tmp 文件（不同源断点不兼容）
            if (urlIndex < urls.lastIndex) {
                val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
                tmpFile.delete()
                AppLogger.i(TAG, "$sourceName 失败，切换下一个源...")
                DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Idle
            }
        }
        return false
    }
    
    /** 单源重试封装（用于 WordPress / SQLite 等非 GitHub 下载） */
    private suspend fun downloadWithRetry(
        url: String,
        destFile: File,
        displayName: String,
        context: Context?
    ): Boolean = downloadWithRetry(listOf(url), destFile, displayName, context)
    
    /**
     * 下载 PHP 二进制
     * 来源: github.com/pmmp/PHP-Binaries (Android ARM64 预编译)
     */
    private suspend fun downloadPhp(context: Context, mirror: MirrorConfig): Boolean {
        val abi = getDeviceAbi()
        if (abi != "arm64-v8a") {
            AppLogger.e(TAG, "PHP 二进制仅支持 arm64-v8a, 当前设备: $abi")
            _downloadState.value = DownloadState.Error("PHP 二进制仅支持 arm64 设备")
            return false
        }
        
        val phpUrls = mirror.phpUrls
        val fileName = phpUrls.first().substringAfterLast("/")
        val destDir = getPhpDir(context)
        val archiveFile = File(getDepsDir(context), fileName)
        
        AppLogger.i(TAG, "下载 PHP 二进制 (共 ${phpUrls.size} 个源)")
        
        // 下载压缩包（多源 fallback + 重试，断点续传自动恢复）
        val downloaded = downloadWithRetry(phpUrls, archiveFile, "PHP $PHP_VERSION ($abi)", context)
        syncEngineState()
        if (!downloaded) return false
        
        // 解压
        _downloadState.value = DownloadState.Extracting("PHP")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("PHP")
        try {
            extractTarGz(archiveFile, destDir)
            
            // pmmp 的 tar.gz 内部结构: bin/php — 尝试多种路径查找
            var phpBinary = File(destDir, "php")
            if (!phpBinary.exists()) {
                phpBinary = File(destDir, "bin/php")
            }
            // 递归查找 php 可执行文件
            if (!phpBinary.exists()) {
                phpBinary = destDir.walkTopDown().firstOrNull { it.name == "php" && it.isFile } ?: phpBinary
            }
            
            if (phpBinary.exists()) {
                phpBinary.setExecutable(true, false)
                // 如果不在 destDir 根目录，复制到根目录以保持兼容
                val targetBinary = File(destDir, "php")
                if (phpBinary.absolutePath != targetBinary.absolutePath) {
                    phpBinary.copyTo(targetBinary, overwrite = true)
                    targetBinary.setExecutable(true, false)
                }
                AppLogger.i(TAG, "PHP 二进制已就绪: ${targetBinary.absolutePath}")
            } else {
                AppLogger.e(TAG, "解压后未找到 PHP 二进制")
                _downloadState.value = DownloadState.Error("解压后未找到 PHP 二进制")
                return false
            }
            
            // 清理压缩包
            archiveFile.delete()
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 PHP 失败", e)
            _downloadState.value = DownloadState.Error("解压 PHP 失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 下载 WordPress 核心
     */
    private suspend fun downloadWordPress(context: Context, mirror: MirrorConfig): Boolean {
        val wpUrls = mirror.wordpressUrls
        val destDir = getDepsDir(context)
        // 统一缓存文件名（不同 URL 可能产生不同文件名，使用固定名称避免混乱）
        val archiveFile = File(destDir, "wordpress-core.tar.gz")
        
        AppLogger.i(TAG, "下载 WordPress 核心 (共 ${wpUrls.size} 个源)")
        
        val downloaded = downloadWithRetry(wpUrls, archiveFile, "WordPress $WORDPRESS_VERSION", context)
        syncEngineState()
        if (!downloaded) return false
        
        _downloadState.value = DownloadState.Extracting("WordPress")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("WordPress")
        try {
            extractTarGz(archiveFile, destDir)
            archiveFile.delete()
            
            // 验证解压结果
            val wpDir = File(destDir, "wordpress")
            if (!wpDir.exists() || !File(wpDir, "wp-includes/version.php").exists()) {
                _downloadState.value = DownloadState.Error("WordPress 解压不完整")
                return false
            }
            
            AppLogger.i(TAG, "WordPress 核心已就绪")
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 WordPress 失败", e)
            _downloadState.value = DownloadState.Error("解压 WordPress 失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 下载 SQLite Database Integration 插件
     */
    private suspend fun downloadSqlitePlugin(context: Context, mirror: MirrorConfig): Boolean {
        val fileName = "sqlite-database-integration.${SQLITE_PLUGIN_VERSION}.zip"
        val url = "${mirror.sqlitePluginUrl}sqlite-database-integration.${SQLITE_PLUGIN_VERSION}.zip"
        val destDir = getDepsDir(context)
        val archiveFile = File(destDir, fileName)
        
        AppLogger.i(TAG, "下载 SQLite 插件: $url")
        
        val downloaded = downloadWithRetry(url, archiveFile, "SQLite Plugin $SQLITE_PLUGIN_VERSION", context)
        syncEngineState()
        if (!downloaded) return false
        
        _downloadState.value = DownloadState.Extracting("SQLite Plugin")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("SQLite Plugin")
        try {
            extractZip(archiveFile, destDir)
            archiveFile.delete()
            
            val pluginDir = File(destDir, "sqlite-database-integration")
            if (!pluginDir.exists()) {
                _downloadState.value = DownloadState.Error("SQLite 插件解压不完整")
                return false
            }
            
            AppLogger.i(TAG, "SQLite 插件已就绪")
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 SQLite 插件失败", e)
            _downloadState.value = DownloadState.Error("解压 SQLite 插件失败: ${e.message}")
            return false
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
     * 解压 tar.gz 文件
     */
    private fun extractTarGz(archiveFile: File, destDir: File) {
        val processBuilder = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", destDir.absolutePath)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            // Fallback：使用 Apache Commons Compress
            extractTarGzWithCommons(archiveFile, destDir)
        }
    }
    
    /**
     * 使用 Apache Commons Compress 解压 tar.gz（fallback）
     */
    private fun extractTarGzWithCommons(archiveFile: File, destDir: File) {
        val gzIn = java.util.zip.GZIPInputStream(archiveFile.inputStream().buffered())
        val tarIn = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzIn)
        
        var entry = tarIn.nextEntry
        while (entry != null) {
            val outFile = File(destDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos ->
                    tarIn.copyTo(fos)
                }
                // 保留可执行权限
                if (entry.mode and 0b001_000_000 != 0) {
                    outFile.setExecutable(true, false)
                }
            }
            entry = tarIn.nextEntry
        }
        tarIn.close()
    }
    
    /**
     * 解压 zip 文件
     */
    private fun extractZip(zipFile: File, destDir: File) {
        val zipInputStream = java.util.zip.ZipInputStream(zipFile.inputStream().buffered())
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            val outFile = File(destDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos ->
                    zipInputStream.copyTo(fos)
                }
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
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
