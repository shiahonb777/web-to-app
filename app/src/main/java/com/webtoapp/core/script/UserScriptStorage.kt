package com.webtoapp.core.script

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.UserScript
import java.io.File










object UserScriptStorage {

    private const val TAG = "UserScriptStorage"


    const val EXTERNAL_STORAGE_THRESHOLD = 2048


    const val FILE_REF_PREFIX = "FILE_REF:"


    private const val SCRIPTS_DIR = "user_scripts"




    private fun getScriptsDir(context: Context): File {
        val dir = File(context.filesDir, SCRIPTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }





    private fun generateFileName(appId: Long, scriptName: String): String {
        val key = "${appId}_${scriptName}"
        val hash = key.hashCode().toUInt().toString(16)
        return "script_${appId}_${hash}.js"
    }




    fun isFileReference(code: String): Boolean {
        return code.startsWith(FILE_REF_PREFIX)
    }




    private fun extractFileName(code: String): String? {
        return if (isFileReference(code)) {
            code.removePrefix(FILE_REF_PREFIX).trim()
        } else null
    }





    fun saveScriptToFile(context: Context, appId: Long, scriptName: String, code: String): String {
        return try {
            val fileName = generateFileName(appId, scriptName)
            val file = File(getScriptsDir(context), fileName)
            file.writeText(code)
            AppLogger.d(TAG, "Saved script to file: $fileName (${code.length} chars)")
            "$FILE_REF_PREFIX$fileName"
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save script to file", e)

            code
        }
    }






    fun loadScriptCode(context: Context, code: String): String {
        if (!isFileReference(code)) return code

        return try {
            val fileName = extractFileName(code) ?: return ""
            val file = File(getScriptsDir(context), fileName)
            if (file.exists()) {
                val content = file.readText()
                AppLogger.d(TAG, "Loaded script from file: $fileName (${content.length} chars)")
                content
            } else {
                AppLogger.w(TAG, "Script file not found: $fileName")
                ""
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load script from file", e)
            ""
        }
    }





    fun ensureCodeLoaded(context: Context, script: UserScript): UserScript {
        if (!isFileReference(script.code)) return script
        val actualCode = loadScriptCode(context, script.code)
        return script.copy(code = actualCode)
    }





    fun externalizeScripts(context: Context, appId: Long, scripts: List<UserScript>): List<UserScript> {
        return scripts.map { script ->
            if (!isFileReference(script.code) && script.code.length > EXTERNAL_STORAGE_THRESHOLD) {

                val ref = saveScriptToFile(context, appId, script.name, script.code)
                script.copy(code = ref)
            } else {
                script
            }
        }
    }





    fun internalizeScripts(context: Context, scripts: List<UserScript>): List<UserScript> {
        return scripts.map { ensureCodeLoaded(context, it) }
    }





    fun deleteScriptsForApp(context: Context, appId: Long) {
        try {
            val dir = getScriptsDir(context)
            val prefix = "script_${appId}_"
            dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { file ->
                file.delete()
                AppLogger.d(TAG, "Deleted script file: ${file.name}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete scripts for app $appId", e)
        }
    }




    fun cleanupOrphanedFiles(context: Context, activeAppIds: Set<Long>) {
        try {
            val dir = getScriptsDir(context)
            dir.listFiles()?.forEach { file ->
                val match = Regex("script_(\\d+)_").find(file.name)
                val fileAppId = match?.groupValues?.get(1)?.toLongOrNull()
                if (fileAppId != null && fileAppId !in activeAppIds) {
                    file.delete()
                    AppLogger.d(TAG, "Cleaned up orphaned script file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cleanup orphaned script files", e)
        }
    }
}
