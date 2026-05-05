package com.webtoapp.core.frontend

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings
import java.io.File






object SampleProjectManager {

    private const val TAG = "SampleProjectManager"
    private const val SAMPLES_DIR = "sample_projects"




    private fun getLanguageSuffix(): String {
        return when (Strings.currentLanguage.value) {
            AppLanguage.CHINESE -> ""
            AppLanguage.ENGLISH -> "-en"
            AppLanguage.ARABIC -> "-ar"
        }
    }




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


            val cachedEntry = File(outputDir, "dist/index.html")
            if (!forceRefresh &&
                outputDir.exists() &&
                cachedEntry.exists() &&
                cachedEntry.length() > 0L &&
                cachedVersion == currentVersion) {
                AppLogger.d(TAG, "示例项目已存在且版本匹配: ${outputDir.absolutePath}")
                return Result.success(outputDir.absolutePath)
            }

            AppLogger.i(TAG, "重新解压示例项目 (版本: $cachedVersion -> $currentVersion)")


            outputDir.deleteRecursively()
            outputDir.mkdirs()


            val assetPath = "$SAMPLES_DIR/$projectId"
            copyAssetFolder(context, assetPath, outputDir)


            versionFile.writeText(currentVersion.toString())

            AppLogger.i(TAG, "示例项目已解压: ${outputDir.absolutePath}")
            Result.success(outputDir.absolutePath)

        } catch (e: Exception) {
            AppLogger.e(TAG, "解压示例项目失败", e)
            Result.failure(e)
        }
    }




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




    suspend fun getSampleDistPath(
        context: Context,
        projectId: String
    ): Result<String> {
        val extractResult = extractSampleProject(context, projectId)
        return extractResult.map { projectPath ->
            "$projectPath/dist"
        }
    }




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




    fun clearExtractedProjects(context: Context) {
        val samplesDir = File(context.filesDir, "sample_projects")
        if (samplesDir.exists()) {
            samplesDir.deleteRecursively()
        }
    }
}




data class SampleProject(
    val id: String,
    val name: String,
    val description: String,
    val framework: FrontendFramework,
    val icon: String,
    val tags: List<String>
)
