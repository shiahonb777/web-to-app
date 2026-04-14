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
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 */
object WordPressDependencyManager {
    
    private const val TAG = "DependencyManager"
    
    /** Note: brief English comment. */
    const val PHP_VERSION = "8.4"
    
    /** Note: brief English comment. */
    const val WORDPRESS_VERSION = "6.9.1"
    
    /** Note: brief English comment. */
    const val SQLITE_PLUGIN_VERSION = "2.2.17"
    
    // Note: brief English comment.
    
    enum class MirrorRegion { CN, GLOBAL }
    
    /** Note: brief English comment. */
    private val GITHUB_CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "https://ghproxy.cc/"
    )
    
    /** Note: brief English comment. */
    private val PHP_GITHUB_URL = "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-${PHP_VERSION}-latest/PHP-${PHP_VERSION}-Android-arm64-PM5.tar.gz"
    
    data class MirrorConfig(
        /** Note: brief English comment. */
        val phpUrls: List<String>,
        /** Note: brief English comment. */
        val wordpressUrls: List<String>,
        val sqlitePluginUrl: String
    )
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
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
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private val GLOBAL_MIRROR = MirrorConfig(
        phpUrls = listOf(PHP_GITHUB_URL),
        wordpressUrls = listOf(
            "https://wordpress.org/wordpress-${WORDPRESS_VERSION}.tar.gz",
            "https://wordpress.org/latest.tar.gz"
        ),
        sqlitePluginUrl = "https://downloads.wordpress.org/plugin/"
    )
    
    // Note: brief English comment.
    
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Verifying(val fileName: String) : DownloadState()
        data class Extracting(val fileName: String) : DownloadState()
        object Complete : DownloadState()
        data class Error(val message: String, val retryable: Boolean = true) : DownloadState()
        /** Note: brief English comment. */
        data class Paused(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    }
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    /** Note: brief English comment. */
    private var _userMirrorRegion: MirrorRegion? = null
    
    // Note: brief English comment.
    
    /**
     * Note: brief English comment.
     */
    fun setMirrorRegion(region: MirrorRegion?) {
        _userMirrorRegion = region
    }
    
    /**
     * Note: brief English comment.
     */
    fun getMirrorRegion(): MirrorRegion {
        _userMirrorRegion?.let { return it }
        val lang = Locale.getDefault().language
        return if (lang == "zh") MirrorRegion.CN else MirrorRegion.GLOBAL
    }
    
    /**
     * Note: brief English comment.
     */
    fun getMirrorConfig(): MirrorConfig {
        return when (getMirrorRegion()) {
            MirrorRegion.CN -> CN_MIRROR
            MirrorRegion.GLOBAL -> GLOBAL_MIRROR
        }
    }
    
    /**
     * Note: brief English comment.
     */
    fun getDepsDir(context: Context): File {
        return File(context.filesDir, "wordpress_deps").also { it.mkdirs() }
    }
    
    /**
     * Note: brief English comment.
     */
    fun getPhpDir(context: Context): File {
        val abi = getDeviceAbi()
        return File(getDepsDir(context), "php/$abi").also { it.mkdirs() }
    }
    
    /**
     * Note: brief English comment.
     */
    fun getWordPressProjectsDir(context: Context): File {
        return File(context.filesDir, "wordpress_projects").also { it.mkdirs() }
    }
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun isPhpReady(context: Context): Boolean {
        // Note: brief English comment.
        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        if (nativePhp.exists()) return true
        // Note: brief English comment.
        if (Build.VERSION.SDK_INT >= 35) return false
        // Note: brief English comment.
        val phpBinary = File(getPhpDir(context), "php")
        return phpBinary.exists() && phpBinary.canExecute()
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     *
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun getPhpExecutablePath(context: Context): String {
        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        if (nativePhp.exists()) {
            AppLogger.d(TAG, "使用 nativeLibraryDir PHP: ${nativePhp.absolutePath}")
            return nativePhp.absolutePath
        }
        // Note: brief English comment.
        val downloaded = File(getPhpDir(context), "php")
        AppLogger.d(TAG, "使用下载目录 PHP: ${downloaded.absolutePath}")
        return downloaded.absolutePath
    }
    
    /**
     * Note: brief English comment.
     */
    fun isWordPressReady(context: Context): Boolean {
        val wpDir = File(getDepsDir(context), "wordpress")
        return wpDir.exists() && File(wpDir, "wp-includes/version.php").exists()
    }
    
    /**
     * Note: brief English comment.
     */
    fun isSqlitePluginReady(context: Context): Boolean {
        val pluginDir = File(getDepsDir(context), "sqlite-database-integration")
        return pluginDir.exists() && File(pluginDir, "load.php").exists()
    }
    
    /**
     * Note: brief English comment.
     */
    fun isAllReady(context: Context): Boolean {
        return isPhpReady(context) && isWordPressReady(context) && isSqlitePluginReady(context)
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     */
    suspend fun downloadAllDependencies(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Idle
            // Note: brief English comment.
            DependencyDownloadNotification.getInstance(context)
            DependencyDownloadEngine.reset()
            val mirror = getMirrorConfig()
            
            // Note: brief English comment.
            if (!isPhpReady(context)) {
                val success = downloadPhp(context, mirror)
                if (!success) return@withContext false
            }
            
            // Note: brief English comment.
            if (!isWordPressReady(context)) {
                val success = downloadWordPress(context, mirror)
                if (!success) return@withContext false
            }
            
            // Note: brief English comment.
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
     * Note: brief English comment.
     *
     * Note: brief English comment.
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
     * Note: brief English comment.
     */
    fun clearCache(context: Context) {
        getDepsDir(context).deleteRecursively()
        AppLogger.i(TAG, "依赖缓存已清理")
    }
    
    /**
     * Note: brief English comment.
     */
    fun getCacheSize(context: Context): Long {
        return getDepsDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    // Note: brief English comment.
    
    /**
     * Note: brief English comment.
     */
    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }
    
    /** Note: brief English comment. */
    private const val MAX_RETRY_PER_URL = 2
    
    /** Note: brief English comment. */
    private const val RETRY_DELAY_MS = 2000L
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
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
            
            // Note: brief English comment.
            if (urlIndex < urls.lastIndex) {
                val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
                tmpFile.delete()
                AppLogger.i(TAG, "$sourceName 失败，切换下一个源...")
                DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Idle
            }
        }
        return false
    }
    
    /** Note: brief English comment. */
    private suspend fun downloadWithRetry(
        url: String,
        destFile: File,
        displayName: String,
        context: Context?
    ): Boolean = downloadWithRetry(listOf(url), destFile, displayName, context)
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
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
        
        // Note: brief English comment.
        val downloaded = downloadWithRetry(phpUrls, archiveFile, "PHP $PHP_VERSION ($abi)", context)
        syncEngineState()
        if (!downloaded) return false
        
        // Note: brief English comment.
        _downloadState.value = DownloadState.Extracting("PHP")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("PHP")
        try {
            extractTarGz(archiveFile, destDir)
            
            // Note: brief English comment.
            var phpBinary = File(destDir, "php")
            if (!phpBinary.exists()) {
                phpBinary = File(destDir, "bin/php")
            }
            // Note: brief English comment.
            if (!phpBinary.exists()) {
                phpBinary = destDir.walkTopDown().firstOrNull { it.name == "php" && it.isFile } ?: phpBinary
            }
            
            if (phpBinary.exists()) {
                phpBinary.setExecutable(true, false)
                // Note: brief English comment.
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
            
            // Note: brief English comment.
            archiveFile.delete()
            return true
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 PHP 失败", e)
            _downloadState.value = DownloadState.Error("解压 PHP 失败: ${e.message}")
            return false
        }
    }
    
    /**
     * Note: brief English comment.
     */
    private suspend fun downloadWordPress(context: Context, mirror: MirrorConfig): Boolean {
        val wpUrls = mirror.wordpressUrls
        val destDir = getDepsDir(context)
        // Note: brief English comment.
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
            
            // Note: brief English comment.
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
     * Note: brief English comment.
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
     * Note: brief English comment.
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
     * Note: brief English comment.
     */
    private fun extractTarGz(archiveFile: File, destDir: File) {
        val processBuilder = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", destDir.absolutePath)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            // Note: brief English comment.
            extractTarGzWithCommons(archiveFile, destDir)
        }
    }
    
    /**
     * Note: brief English comment.
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
                // Note: brief English comment.
                if (entry.mode and 0b001_000_000 != 0) {
                    outFile.setExecutable(true, false)
                }
            }
            entry = tarIn.nextEntry
        }
        tarIn.close()
    }
    
    /**
     * Note: brief English comment.
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
     * Note: brief English comment.
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
