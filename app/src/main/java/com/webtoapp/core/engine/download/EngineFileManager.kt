package com.webtoapp.core.engine.download

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.webtoapp.core.engine.EngineType
import java.io.File





class EngineFileManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "engine_manager"
        private const val KEY_VERSION_PREFIX = "engine_version_"
        private const val KEY_DOWNLOAD_TIME_PREFIX = "engine_download_time_"


        private const val GECKO_ENGINE_DIR = "gecko_engine"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }




    fun getEngineDir(type: EngineType): File {
        val dirName = when (type) {
            EngineType.GECKOVIEW -> GECKO_ENGINE_DIR
            else -> return File(context.filesDir, "engines")
        }
        return File(context.filesDir, dirName).apply { mkdirs() }
    }





    fun getAbiDir(type: EngineType, abi: String): File {
        return File(getEngineDir(type), "lib/$abi").apply { mkdirs() }
    }




    fun isEngineDownloaded(type: EngineType): Boolean {
        if (!type.requiresDownload) return true
        val engineDir = getEngineDir(type)
        val libDir = File(engineDir, "lib")
        if (!libDir.exists()) return false


        return libDir.listFiles()?.any { abiDir ->
            abiDir.isDirectory && abiDir.listFiles()?.any { it.extension == "so" } == true
        } == true
    }




    fun isAbiDownloaded(type: EngineType, abi: String): Boolean {
        val abiDir = getAbiDir(type, abi)
        return abiDir.exists() && abiDir.listFiles()?.any { it.extension == "so" } == true
    }




    fun getDevicePrimaryAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }




    fun getDownloadedVersion(type: EngineType): String? {
        return prefs.getString("${KEY_VERSION_PREFIX}${type.name}", null)
    }




    fun setDownloadedVersion(type: EngineType, version: String) {
        prefs.edit()
            .putString("${KEY_VERSION_PREFIX}${type.name}", version)
            .putLong("${KEY_DOWNLOAD_TIME_PREFIX}${type.name}", System.currentTimeMillis())
            .apply()
    }




    fun getEngineSize(type: EngineType): Long {
        val dir = getEngineDir(type)
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }




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
