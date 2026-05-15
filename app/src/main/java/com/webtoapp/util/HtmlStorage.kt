package com.webtoapp.util

import android.content.Context
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.util.UUID





object HtmlStorage {

    private const val TAG = "HtmlStorage"
    private const val HTML_DIR = "html_projects"
    // 镜像到 App 外部目录，让用户在 MT 管理器 / 系统文件管理器中能直接看到、修改源文件。
    // 路径：/sdcard/Android/data/<package>/files/WebToApp/html_projects/<projectId>/
    // 选 getExternalFilesDir 而不是公共 Downloads / DocumentFile，是因为：
    //   - 不需要运行时权限，Android 6+ 直接可写
    //   - App 卸载会自动清理，不污染用户存储
    //   - 文件管理器默认就能进入 Android/data 浏览
    private const val MIRROR_PARENT = "WebToApp"









    fun saveHtmlFile(
        context: Context,
        uri: Uri,
        fileName: String,
        projectId: String
    ): String? {
        return try {
            val projectDir = getProjectDir(context, projectId)
            val targetFile = File(projectDir, fileName)


            targetFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 镜像到外部可见目录，让第三方文件管理器/编辑器能看到
            mirrorToExternal(context, projectId, fileName, targetFile)

            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }









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


            targetFile.parentFile?.mkdirs()

            tempFile.copyTo(targetFile, overwrite = true)

            mirrorToExternal(context, projectId, fileName, targetFile)

            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }









    fun saveProcessedHtml(
        context: Context,
        htmlContent: String,
        fileName: String,
        projectId: String
    ): String? {
        return try {
            val projectDir = getProjectDir(context, projectId)
            val targetFile = File(projectDir, fileName)


            targetFile.parentFile?.mkdirs()


            targetFile.writeText(htmlContent, Charsets.UTF_8)

            mirrorToExternal(context, projectId, fileName, targetFile)

            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }




    fun deleteProject(context: Context, projectId: String) {
        try {
            val projectDir = getProjectDir(context, projectId)
            projectDir.deleteRecursively()
            // 同步清理外部镜像，避免遗留垃圾文件
            externalMirrorDir(context, projectId)?.deleteRecursively()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
        }
    }




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




    fun generateProjectId(): String {
        return UUID.randomUUID().toString().take(8)
    }




    private fun getProjectDir(context: Context, projectId: String): File {
        val htmlDir = File(context.filesDir, HTML_DIR)
        val projectDir = File(htmlDir, projectId)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        return projectDir
    }

    /**
     * 计算外部镜像目录：/sdcard/Android/data/<package>/files/WebToApp/html_projects/<projectId>/
     * 部分定制 ROM 在外部存储不可用时返回 null（如 OOBE 阶段、SD 卡未挂载），调用方按 null 跳过即可。
     */
    private fun externalMirrorDir(context: Context, projectId: String): File? {
        val externalRoot = context.getExternalFilesDir(MIRROR_PARENT) ?: return null
        return File(File(externalRoot, HTML_DIR), projectId)
    }

    /**
     * 把权威私有目录里的文件镜像一份到外部可见目录。
     * 失败不向上抛：用户的核心保存路径仍在 filesDir，运行时/打包不会受影响。
     */
    private fun mirrorToExternal(
        context: Context,
        projectId: String,
        relativeName: String,
        sourceFile: File
    ) {
        try {
            val mirrorDir = externalMirrorDir(context, projectId) ?: return
            val target = File(mirrorDir, relativeName)
            target.parentFile?.mkdirs()
            sourceFile.copyTo(target, overwrite = true)
        } catch (e: Exception) {
            // 镜像是辅助功能，失败不阻断主流程
            AppLogger.w(TAG, "External mirror failed for $relativeName: ${e.message}")
        }
    }
}
