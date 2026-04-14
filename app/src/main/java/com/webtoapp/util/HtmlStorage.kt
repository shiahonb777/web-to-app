package com.webtoapp.util

import android.content.Context
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.util.UUID

/**
 * HTML
 * HTML
 */
object HtmlStorage {
    
    private const val TAG = "HtmlStorage"
    private const val HTML_DIR = "html_projects"
    
    /**
     * HTML Uri
     * @param context parameter
     * @param uri parameter
     * @param fileName parameter
     * @param projectId parameter
     * @return result
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
            
            // （ css/style.css）
            targetFile.parentFile?.mkdirs()
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * Note.
     * @param context parameter
     * @param tempPath parameter
     * @param fileName parameter
     * @param projectId parameter
     * @return result
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
            
            // Note.
            targetFile.parentFile?.mkdirs()
            
            tempFile.copyTo(targetFile, overwrite = true)
            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * HTML
     * @param context parameter
     * @param htmlContent parameter
     * @param fileName parameter
     * @param projectId parameter
     * @return result
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
            
            // Note.
            targetFile.parentFile?.mkdirs()
            
            // HTML
            targetFile.writeText(htmlContent, Charsets.UTF_8)
            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * Note.
     */
    fun deleteProject(context: Context, projectId: String) {
        try {
            val projectDir = getProjectDir(context, projectId)
            projectDir.deleteRecursively()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
        }
    }
    
    /**
     * HTML
     */
    fun clearTempFiles(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "html_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
        }
    }
    
    /**
     * ID
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
