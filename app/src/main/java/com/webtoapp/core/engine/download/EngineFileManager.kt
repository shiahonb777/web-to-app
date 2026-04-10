package com.webtoapp.core.engine.download

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.webtoapp.core.engine.EngineType
import java.io.File

/**
 * 引擎文件管理器
 * 管理引擎原生库的缓存、版本追踪、磁盘空间
 */
class EngineFileManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "engine_manager"
        private const val KEY_VERSION_PREFIX = "engine_version_"
        private const val KEY_DOWNLOAD_TIME_PREFIX = "engine_download_time_"

        /** GeckoView 引擎文件存储目录名 */
        private const val GECKO_ENGINE_DIR = "gecko_engine"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取引擎文件存储根目录
     */
    fun getEngineDir(type: EngineType): File {
        val dirName = when (type) {
            EngineType.GECKOVIEW -> GECKO_ENGINE_DIR
            else -> return File(context.filesDir, "engines") // fallback
        }
        return File(context.filesDir, dirName).apply { mkdirs() }
    }

    /**
     * 获取指定 ABI 的 .so 文件目录
     * 例如：{filesDir}/gecko_engine/lib/arm64-v8a/
     */
    fun getAbiDir(type: EngineType, abi: String): File {
        return File(getEngineDir(type), "lib/$abi").apply { mkdirs() }
    }

    /**
     * 检查引擎是否已下载（至少有一个 ABI 的 .so 文件）
     */
    fun isEngineDownloaded(type: EngineType): Boolean {
        if (!type.requiresDownload) return true
        val engineDir = getEngineDir(type)
        val libDir = File(engineDir, "lib")
        if (!libDir.exists()) return false

        // 检查是否有任意 ABI 目录包含 .so 文件
        return libDir.listFiles()?.any { abiDir ->
            abiDir.isDirectory && abiDir.listFiles()?.any { it.extension == "so" } == true
        } == true
    }

    /**
     * 检查指定 ABI 是否已下载
     */
    fun isAbiDownloaded(type: EngineType, abi: String): Boolean {
        val abiDir = getAbiDir(type, abi)
        return abiDir.exists() && abiDir.listFiles()?.any { it.extension == "so" } == true
    }

    /**
     * 获取设备的主 ABI
     */
    fun getDevicePrimaryAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    /**
     * 获取已下载的版本号
     */
    fun getDownloadedVersion(type: EngineType): String? {
        return prefs.getString("${KEY_VERSION_PREFIX}${type.name}", null)
    }

    /**
     * 保存已下载的版本号
     */
    fun setDownloadedVersion(type: EngineType, version: String) {
        prefs.edit()
            .putString("${KEY_VERSION_PREFIX}${type.name}", version)
            .putLong("${KEY_DOWNLOAD_TIME_PREFIX}${type.name}", System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取引擎文件占用的磁盘空间 (bytes)
     */
    fun getEngineSize(type: EngineType): Long {
        val dir = getEngineDir(type)
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * 删除引擎文件
     */
    fun deleteEngineFiles(type: EngineType): Boolean {
        val dir = getEngineDir(type)
        val deleted = dir.deleteRecursively()
        if (deleted) {
            prefs.edit()
                .remove("${KEY_VERSION_PREFIX}${type.name}")
                .remove("${KEY_DOWNLOAD_TIME_PREFIX}${type.name}")
                .apply()
        }
        return deleted
    }

    /**
     * 列出所有已下载的 .so 文件路径（用于 APK 构建时注入）
     * @return Map<abi, List<File>>  例如 {"arm64-v8a": [libxul.so, libmozglue.so, ...]}
     */
    fun listEngineNativeLibs(type: EngineType): Map<String, List<File>> {
        val result = mutableMapOf<String, List<File>>()
        val libDir = File(getEngineDir(type), "lib")
        if (!libDir.exists()) return result

        libDir.listFiles()?.filter { it.isDirectory }?.forEach { abiDir ->
            val soFiles = abiDir.listFiles()?.filter { it.extension == "so" } ?: emptyList()
            if (soFiles.isNotEmpty()) {
                result[abiDir.name] = soFiles
            }
        }
        return result
    }
}
