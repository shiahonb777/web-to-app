package com.webtoapp.core.frontend

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File











class FrontendProjectBuilder(private val context: Context) {

    companion object {
        private const val TAG = "FrontendProjectBuilder"
    }


    private val _buildState = MutableStateFlow<BuildState>(BuildState.Idle)
    val buildState: StateFlow<BuildState> = _buildState


    private val _buildLogs = MutableStateFlow<List<BuildLogEntry>>(emptyList())
    val buildLogs: StateFlow<List<BuildLogEntry>> = _buildLogs







    suspend fun importProject(projectPath: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        _buildLogs.value = emptyList()

        try {
            _buildState.value = BuildState.Scanning
            addLog(LogLevel.INFO, "开始扫描项目...")

            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                throw Exception(Strings.frontendProjectDirNotFound.format(projectPath))
            }


            addLog(LogLevel.INFO, "检测项目类型...")
            val detection = ProjectDetector.detectProject(projectPath)

            addLog(LogLevel.INFO, "框架: ${getFrameworkDisplayName(detection.framework)}")
            if (detection.frameworkVersion != null) {
                addLog(LogLevel.INFO, "版本: ${detection.frameworkVersion}")
            }



            detection.issues.forEach { issue ->
                when (issue.severity) {
                    IssueSeverity.ERROR -> {
                        addLog(LogLevel.ERROR, issue.message)
                        throw Exception(issue.message)
                    }
                    IssueSeverity.WARNING -> addLog(LogLevel.WARNING, issue.message)
                    IssueSeverity.INFO -> addLog(LogLevel.INFO, issue.message)
                }
            }


            val outputDir = File(detection.outputDir)
            if (!outputDir.exists() || !outputDir.isDirectory) {
                throw Exception(Strings.frontendBuildOutputNotFound)
            }


            val indexHtml = File(outputDir, "index.html")
            if (!indexHtml.exists()) {
                throw Exception(Strings.frontendIndexHtmlNotFound)
            }

            addLog(LogLevel.INFO, "找到输出目录: ${outputDir.name}")


            val files = outputDir.walkTopDown().filter { it.isFile }.toList()
            addLog(LogLevel.INFO, "共 ${files.size} 个文件")

            _buildState.value = BuildState.Importing(0f, "准备导入...")


            _buildState.value = BuildState.Success(outputDir.absolutePath, files.size)
            addLog(LogLevel.INFO, "扫描完成，准备导入")

            Result.success(ImportResult(
                outputPath = outputDir.absolutePath,
                framework = detection.framework,
                fileCount = files.size,
                hasTypeScript = detection.hasTypeScript,
                dependencies = detection.dependencies.size + detection.devDependencies.size
            ))

        } catch (e: Exception) {
            AppLogger.d(TAG, "导入失败", e)
            addLog(LogLevel.ERROR, "导入失败: ${e.message}")
            _buildState.value = BuildState.Error(e.message ?: "未知错误")
            Result.failure(e)
        }
    }




    private fun addLog(level: LogLevel, message: String) {
        val entry = BuildLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message
        )
        _buildLogs.value = _buildLogs.value + entry
    }




    fun reset() {
        _buildState.value = BuildState.Idle
        _buildLogs.value = emptyList()
    }




    private fun getFrameworkDisplayName(framework: FrontendFramework): String {
        return when (framework) {
            FrontendFramework.VUE -> "Vue.js"
            FrontendFramework.REACT -> "React"
            FrontendFramework.NEXT -> "Next.js"
            FrontendFramework.NUXT -> "Nuxt.js"
            FrontendFramework.ANGULAR -> "Angular"
            FrontendFramework.SVELTE -> "Svelte"
            FrontendFramework.VITE -> "Vite"
            FrontendFramework.UNKNOWN -> Strings.frontendStaticSite
        }
    }
}




data class ImportResult(
    val outputPath: String,
    val framework: FrontendFramework,
    val fileCount: Int,
    val hasTypeScript: Boolean,
    val dependencies: Int
)
