package com.webtoapp.core.sample

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings
import java.io.File

/**
 * 通用示例项目数据类
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
 * 通用示例项目提取器
 * 
 * 提供从 assets 解压示例项目到 app 目录的公共逻辑，
 * 被各类型的 SampleManager 共享使用。
 */
object SampleProjectExtractor {
    
    private const val TAG = "SampleProjectExtractor"
    private const val SAMPLES_DIR = "sample_projects"
    
    /**
     * 根据当前语言获取项目 ID 后缀
     */
    fun getLanguageSuffix(): String {
        return when (Strings.currentLanguage.value) {
            AppLanguage.CHINESE -> ""
            AppLanguage.ENGLISH -> "-en"
            AppLanguage.ARABIC -> "-ar"
        }
    }
    
    /**
     * 解压示例项目到应用目录
     * 
     * @return 解压后的项目路径
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
                // 检查是否有实质文件（至少存在 1 个子文件）
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
     * 获取 App 版本标识（versionCode + lastUpdateTime）
     * 使用 lastUpdateTime 确保即使 versionCode 不变（debug 构建），
     * 重新安装 APK 后也会刷新示例项目缓存
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
            // 混合 versionCode 和 lastUpdateTime，确保 APK 更新后缓存失效
            versionCode xor (packageInfo.lastUpdateTime / 1000)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 递归复制 assets 目录
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
     * 清理所有已解压的示例项目
     */
    fun clearExtractedProjects(context: Context) {
        val samplesDir = File(context.filesDir, "sample_projects")
        if (samplesDir.exists()) {
            samplesDir.deleteRecursively()
        }
    }
}
