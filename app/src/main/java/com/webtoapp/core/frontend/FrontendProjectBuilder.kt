package com.webtoapp.core.frontend

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 前端项目构建器
 * 
 * 负责导入已构建的前端项目：
 * 1. 检测项目类型和依赖
 * 2. 查找构建输出目录
 * 3. 复制静态文件
 * 
 * 注意：不支持在设备上构建，需要用户先在电脑上构建好项目
 */
class FrontendProjectBuilder(private val context: Context) {
    
    companion object {
        private const val TAG = "FrontendProjectBuilder"
    }
    
    // Build状态
    private val _buildState = MutableStateFlow<BuildState>(BuildState.Idle)
    val buildState: StateFlow<BuildState> = _buildState
    
    // Build日志
    private val _buildLogs = MutableStateFlow<List<BuildLogEntry>>(emptyList())
    val buildLogs: StateFlow<List<BuildLogEntry>> = _buildLogs
    
    /**
     * 导入前端项目
     * 
     * @param projectPath 项目路径（可以是项目根目录或 dist 目录）
     * @return 导入结果
     */
    suspend fun importProject(projectPath: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        _buildLogs.value = emptyList()
        
        try {
            _buildState.value = BuildState.Scanning
            addLog(LogLevel.INFO, "开始扫描项目...")
            
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                throw Exception("项目目录不存在: $projectPath")
            }
            
            // 检测项目
            addLog(LogLevel.INFO, "检测项目类型...")
            val detection = ProjectDetector.detectProject(projectPath)
            
            addLog(LogLevel.INFO, "框架: ${getFrameworkDisplayName(detection.framework)}")
            if (detection.frameworkVersion != null) {
                addLog(LogLevel.INFO, "版本: ${detection.frameworkVersion}")
            }

            
            // Check问题
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
            
            // 确定输出目录
            val outputDir = File(detection.outputDir)
            if (!outputDir.exists() || !outputDir.isDirectory) {
                throw Exception("未找到构建输出目录，请先在电脑上运行 npm run build")
            }
            
            // Check是否有 index.html
            val indexHtml = File(outputDir, "index.html")
            if (!indexHtml.exists()) {
                throw Exception("输出目录中未找到 index.html")
            }
            
            addLog(LogLevel.INFO, "找到输出目录: ${outputDir.name}")
            
            // 统计文件
            val files = outputDir.walkTopDown().filter { it.isFile }.toList()
            addLog(LogLevel.INFO, "共 ${files.size} 个文件")
            
            _buildState.value = BuildState.Importing(0f, "准备导入...")
            
            // Return result（实际复制在 ViewModel 中完成）
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
            Log.e(TAG, "导入失败", e)
            addLog(LogLevel.ERROR, "导入失败: ${e.message}")
            _buildState.value = BuildState.Error(e.message ?: "未知错误")
            Result.failure(e)
        }
    }
    
    /**
     * 添加日志
     */
    private fun addLog(level: LogLevel, message: String) {
        val entry = BuildLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message
        )
        _buildLogs.value = _buildLogs.value + entry
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        _buildState.value = BuildState.Idle
        _buildLogs.value = emptyList()
    }
    
    /**
     * 获取框架显示名称
     */
    private fun getFrameworkDisplayName(framework: FrontendFramework): String {
        return when (framework) {
            FrontendFramework.VUE -> "Vue.js"
            FrontendFramework.REACT -> "React"
            FrontendFramework.NEXT -> "Next.js"
            FrontendFramework.NUXT -> "Nuxt.js"
            FrontendFramework.ANGULAR -> "Angular"
            FrontendFramework.SVELTE -> "Svelte"
            FrontendFramework.VITE -> "Vite"
            FrontendFramework.UNKNOWN -> "静态网站"
        }
    }
}

/**
 * 导入结果
 */
data class ImportResult(
    val outputPath: String,
    val framework: FrontendFramework,
    val fileCount: Int,
    val hasTypeScript: Boolean,
    val dependencies: Int
)
