package com.webtoapp.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * HTML 文件存储管理
 * 将 HTML 项目文件持久化保存到应用私有目录
 */
object HtmlStorage {
    
    private const val HTML_DIR = "html_projects"
    
    /**
     * 保存 HTML 文件从 Uri
     * @param context 上下文
     * @param uri 源文件 Uri
     * @param fileName 文件名（保持原始文件名）
     * @param projectId 项目ID（用于隔离不同项目的文件）
     * @return 保存后的文件路径，失败返回 null
     */
    fun saveHtmlFile(
        context: Context,
        uri: Uri,
        fileName: String,
        projectId: String
    ): String? {
        return try {
            val projectDir = getProjectDir(context, projectId)
            val targetFile = File(projectDir, fileName)
            
            // 确保父目录存在（支持嵌套路径如 css/style.css）
            targetFile.parentFile?.mkdirs()
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从临时文件保存到持久化目录
     * @param context 上下文
     * @param tempPath 临时文件路径
     * @param fileName 文件名
     * @param projectId 项目ID
     * @return 保存后的文件路径，失败返回 null
     */
    fun saveFromTempFile(
        context: Context,
        tempPath: String,
        fileName: String,
        projectId: String
    ): String? {
        return try {
            val tempFile = File(tempPath)
            if (!tempFile.exists()) return null
            
            val projectDir = getProjectDir(context, projectId)
            val targetFile = File(projectDir, fileName)
            
            // 确保父目录存在
            targetFile.parentFile?.mkdirs()
            
            tempFile.copyTo(targetFile, overwrite = true)
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 保存处理后的 HTML 内容到持久化目录
     * @param context 上下文
     * @param htmlContent 处理后的 HTML 内容（已内联 CSS/JS）
     * @param fileName 文件名
     * @param projectId 项目ID
     * @return 保存后的文件路径，失败返回 null
     */
    fun saveProcessedHtml(
        context: Context,
        htmlContent: String,
        fileName: String,
        projectId: String
    ): String? {
        return try {
            val projectDir = getProjectDir(context, projectId)
            val targetFile = File(projectDir, fileName)
            
            // 确保父目录存在
            targetFile.parentFile?.mkdirs()
            
            // 写入处理后的 HTML 内容
            targetFile.writeText(htmlContent, Charsets.UTF_8)
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除项目所有文件
     */
    fun deleteProject(context: Context, projectId: String) {
        try {
            val projectDir = getProjectDir(context, projectId)
            projectDir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 清理所有临时 HTML 文件
     */
    fun clearTempFiles(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "html_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 生成新的项目 ID
     */
    fun generateProjectId(): String {
        return UUID.randomUUID().toString().take(8)
    }
    
    /**
     * Get project directory
     */
    private fun getProjectDir(context: Context, projectId: String): File {
        val htmlDir = File(context.filesDir, HTML_DIR)
        val projectDir = File(htmlDir, projectId)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        return projectDir
    }
}
