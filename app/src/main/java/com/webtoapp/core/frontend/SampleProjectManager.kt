package com.webtoapp.core.frontend

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings
import java.io.File

/**
 * 示例项目管理器
 * 
 * 提供内置的示例前端项目供用户体验和测试
 */
object SampleProjectManager {
    
    private const val TAG = "SampleProjectManager"
    private const val SAMPLES_DIR = "sample_projects"
    
    /**
     * 根据当前语言获取项目 ID 后缀
     */
    private fun getLanguageSuffix(): String {
        return when (Strings.currentLanguage.value) {
            AppLanguage.CHINESE -> "" // 中文使用原始项目（无后缀）
            AppLanguage.ENGLISH -> "-en"
            AppLanguage.ARABIC -> "-ar"
        }
    }
    
    /**
     * 获取所有示例项目（根据当前语言动态选择）
     */
    fun getSampleProjects(): List<SampleProject> {
        val suffix = getLanguageSuffix()
        return listOf(
            SampleProject(
                id = "vue-demo$suffix",
                name = Strings.sampleVueCounterName,
                description = Strings.sampleVueCounterDesc,
                framework = FrontendFramework.VUE,
                icon = "🟢",
                tags = listOf("Vue 3", Strings.sampleVueCounterTagReactive)
            ),
            SampleProject(
                id = "react-demo$suffix",
                name = Strings.sampleReactTodoName,
                description = Strings.sampleReactTodoDesc,
                framework = FrontendFramework.REACT,
                icon = "⚛️",
                tags = listOf("React 18", "Hooks")
            ),
            SampleProject(
                id = "vite-vanilla$suffix",
                name = Strings.sampleWeatherAppName,
                description = Strings.sampleWeatherAppDesc,
                framework = FrontendFramework.VITE,
                icon = "🌤️",
                tags = listOf("Vite", "Vanilla JS")
            )
        )
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
            
            // Check版本标记文件，确保 assets 更新后能重新解压
            val versionFile = File(outputDir, ".version")
            val currentVersion = getAppVersionCode(context)
            val cachedVersion = if (versionFile.exists()) versionFile.readText().trim().toLongOrNull() else null
            
            // 如果已存在且版本匹配且不强制刷新，直接返回
            if (!forceRefresh && 
                outputDir.exists() && 
                File(outputDir, "dist/index.html").exists() &&
                cachedVersion == currentVersion) {
                AppLogger.d(TAG, "示例项目已存在且版本匹配: ${outputDir.absolutePath}")
                return Result.success(outputDir.absolutePath)
            }
            
            AppLogger.i(TAG, "重新解压示例项目 (版本: $cachedVersion -> $currentVersion)")
            
            // Cleanup并创建目录
            outputDir.deleteRecursively()
            outputDir.mkdirs()
            
            // 从 assets 复制文件
            val assetPath = "$SAMPLES_DIR/$projectId"
            copyAssetFolder(context, assetPath, outputDir)
            
            // 写入版本标记
            versionFile.writeText(currentVersion.toString())
            
            AppLogger.i(TAG, "示例项目已解压: ${outputDir.absolutePath}")
            Result.success(outputDir.absolutePath)
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压示例项目失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get app版本号
     */
    private fun getAppVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取示例项目的 dist 目录路径
     */
    suspend fun getSampleDistPath(
        context: Context,
        projectId: String
    ): Result<String> {
        val extractResult = extractSampleProject(context, projectId)
        return extractResult.map { projectPath ->
            "$projectPath/dist"
        }
    }
    
    /**
     * 复制 assets 文件夹
     */
    private fun copyAssetFolder(context: Context, assetPath: String, targetDir: File) {
        val assetManager = context.assets
        
        try {
            val files = assetManager.list(assetPath) ?: return
            
            if (files.isEmpty()) {
                // 这是一个文件，直接复制
                assetManager.open(assetPath).use { input ->
                    File(targetDir.parent, targetDir.name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                // 这是一个目录，递归复制
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

/**
 * 示例项目信息
 */
data class SampleProject(
    val id: String,
    val name: String,
    val description: String,
    val framework: FrontendFramework,
    val icon: String,
    val tags: List<String>
)
