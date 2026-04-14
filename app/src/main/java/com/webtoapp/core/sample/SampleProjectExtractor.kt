package com.webtoapp.core.sample

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings
import java.io.File

/**
 * Note: brief English comment.
 */
data class TypedSampleProject(
    val id: String,
    val name: String,
    val description: String,
    val frameworkName: String,
    val icon: String,
    val tags: List<String>,
    val brandColor: Long // 0xFFxxxxxx
)

/**
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 */
object SampleProjectExtractor {
    
    private const val TAG = "SampleProjectExtractor"
    private const val SAMPLES_DIR = "sample_projects"
    
    /**
     * Note: brief English comment.
     */
    fun getLanguageSuffix(): String {
        return when (Strings.currentLanguage.value) {
            AppLanguage.CHINESE -> ""
            AppLanguage.ENGLISH -> "-en"
            AppLanguage.ARABIC -> "-ar"
        }
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     */
    suspend fun extractSampleProject(
        context: Context,
        projectId: String,
        forceRefresh: Boolean = false
    ): Result<String> {
        return try {
            val outputDir = File(context.filesDir, "sample_projects/$projectId")
            
            val versionFile = File(outputDir, ".version")
            val currentVersion = getAppVersionCode(context)
            val cachedVersion = if (versionFile.exists()) versionFile.readText().trim().toLongOrNull() else null
            
            if (!forceRefresh && outputDir.exists() && cachedVersion == currentVersion) {
                // Note: brief English comment.
                if (outputDir.listFiles()?.any { it.name != ".version" } == true) {
                    AppLogger.d(TAG, "示例项目已存在且版本匹配: ${outputDir.absolutePath}")
                    return Result.success(outputDir.absolutePath)
                }
            }
            
            AppLogger.i(TAG, "解压示例项目: $projectId (版本: $cachedVersion -> $currentVersion)")
            
            outputDir.deleteRecursively()
            outputDir.mkdirs()
            
            val assetPath = "$SAMPLES_DIR/$projectId"
            copyAssetFolder(context, assetPath, outputDir)
            
            versionFile.writeText(currentVersion.toString())
            
            AppLogger.i(TAG, "示例项目已解压: ${outputDir.absolutePath}")
            Result.success(outputDir.absolutePath)
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压示例项目失败: $projectId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun getAppVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            // Note: brief English comment.
            versionCode xor (packageInfo.lastUpdateTime / 1000)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Note: brief English comment.
     */
    private fun copyAssetFolder(context: Context, assetPath: String, targetDir: File) {
        val assetManager = context.assets
        
        try {
            val files = assetManager.list(assetPath) ?: return
            
            if (files.isEmpty()) {
                assetManager.open(assetPath).use { input ->
                    File(targetDir.parent, targetDir.name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                targetDir.mkdirs()
                for (file in files) {
                    copyAssetFolder(context, "$assetPath/$file", File(targetDir, file))
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "复制文件失败: $assetPath", e)
        }
    }
    
    /**
     * Note: brief English comment.
     */
    fun clearExtractedProjects(context: Context) {
        val samplesDir = File(context.filesDir, "sample_projects")
        if (samplesDir.exists()) {
            samplesDir.deleteRecursively()
        }
    }
}
