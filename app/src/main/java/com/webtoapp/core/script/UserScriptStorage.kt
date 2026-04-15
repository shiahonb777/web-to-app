package com.webtoapp.core.script

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.UserScript
import java.io.File

/**
 * 用户注入脚本的独立文件存储管理器
 * 
 * 当用户脚本代码超过阈值时，将代码存储到独立文件中，
 * UserScript.code 仅保存一个文件引用标记（FILE_REF:xxx），
 * 避免大代码嵌入 Room JSON 导致序列化/反序列化卡顿。
 * 
 * 文件存储路径：{appInternalStorage}/user_scripts/script_{hash}.js
 */
object UserScriptStorage {
    
    private const val TAG = "UserScriptStorage"
    
    /** 超过此字节数的代码将存储到独立文件 */
    const val EXTERNAL_STORAGE_THRESHOLD = 2048  // 2KB
    
    /** 文件引用标记前缀 */
    const val FILE_REF_PREFIX = "FILE_REF:"
    
    /** 存储目录名 */
    private const val SCRIPTS_DIR = "user_scripts"
    
    /**
     * 获取脚本存储目录
     */
    private fun getScriptsDir(context: Context): File {
        val dir = File(context.filesDir, SCRIPTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /**
     * 为脚本生成唯一的文件名
     * 使用 appId + scriptName 的 hash 来确保唯一性
     */
    private fun generateFileName(appId: Long, scriptName: String): String {
        val key = "${appId}_${scriptName}"
        val hash = key.hashCode().toUInt().toString(16)
        return "script_${appId}_${hash}.js"
    }
    
    /**
     * 判断代码是否为文件引用
     */
    fun isFileReference(code: String): Boolean {
        return code.startsWith(FILE_REF_PREFIX)
    }
    
    /**
     * 从文件引用中提取文件名
     */
    private fun extractFileName(code: String): String? {
        return if (isFileReference(code)) {
            code.removePrefix(FILE_REF_PREFIX).trim()
        } else null
    }
    
    /**
     * 保存脚本代码到独立文件
     * @return 文件引用标记字符串（FILE_REF:filename）
     */
    fun saveScriptToFile(context: Context, appId: Long, scriptName: String, code: String): String {
        return try {
            val fileName = generateFileName(appId, scriptName)
            val file = File(getScriptsDir(context), fileName)
            file.writeText(code)
            AppLogger.d(TAG, "Saved script to file: $fileName (${code.length} chars)")
            "$FILE_REF_PREFIX$fileName"
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save script to file", e)
            // 降级：返回原始代码
            code
        }
    }
    
    /**
     * 从文件中加载脚本代码
     * @param code UserScript.code —— 可能是文件引用或内联代码
     * @return 实际的脚本代码
     */
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
    
    /**
     * 确保 UserScript 的代码已加载（懒加载）
     * 如果是文件引用则从文件读取，否则原样返回
     */
    fun ensureCodeLoaded(context: Context, script: UserScript): UserScript {
        if (!isFileReference(script.code)) return script
        val actualCode = loadScriptCode(context, script.code)
        return script.copy(code = actualCode)
    }
    
    /**
     * 处理脚本列表，将大代码存储到文件，返回处理后的列表
     * 用于保存到 Room 前调用
     */
    fun externalizeScripts(context: Context, appId: Long, scripts: List<UserScript>): List<UserScript> {
        return scripts.map { script ->
            if (!isFileReference(script.code) && script.code.length > EXTERNAL_STORAGE_THRESHOLD) {
                // 大代码：存储到文件
                val ref = saveScriptToFile(context, appId, script.name, script.code)
                script.copy(code = ref)
            } else {
                script
            }
        }
    }
    
    /**
     * 处理脚本列表，加载所有文件引用的代码
     * 用于注入/编辑/导出前调用
     */
    fun internalizeScripts(context: Context, scripts: List<UserScript>): List<UserScript> {
        return scripts.map { ensureCodeLoaded(context, it) }
    }
    
    /**
     * 删除指定应用的所有脚本文件
     * 在删除 WebApp 时调用
     */
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
    
    /**
     * 清理孤立的脚本文件（没有关联到任何 WebApp 的文件）
     */
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
