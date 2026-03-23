package com.webtoapp.core.linux

import android.content.Context
import android.util.Log
import com.webtoapp.core.frontend.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 前端项目构建器
 * 
 * 使用 PureBuildEngine 构建前端项目
 * 不依赖 Node.js 运行时
 */
class NodeProjectBuilder(private val context: Context) {
    
    companion object {
        private const val TAG = "NodeProjectBuilder"
    }
    
    private val buildEngine = PureBuildEngine(context)
    
    // Build状态
    private val _buildState = MutableStateFlow<NodeBuildState>(NodeBuildState.Idle)
    val buildState: StateFlow<NodeBuildState> = _buildState
    
    // Build日志
    private val _buildLogs = MutableStateFlow<List<BuildLogEntry>>(emptyList())
    val buildLogs: StateFlow<List<BuildLogEntry>> = _buildLogs
    
    /**
     * 构建项目
     */
    suspend fun buildProject(
        projectPath: String,
        config: NodeBuildConfig = NodeBuildConfig()
    ): Result<NodeBuildResult> = withContext(Dispatchers.IO) {
        _buildLogs.value = emptyList()
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                throw Exception("项目目录不存在: $projectPath")
            }
            
            // 检测项目
            _buildState.value = NodeBuildState.Analyzing
            addLog(LogLevel.INFO, "分析项目...")
            
            val detection = ProjectDetector.detectProject(projectPath)
            addLog(LogLevel.INFO, "框架: ${getFrameworkName(detection.framework)}")
            addLog(LogLevel.INFO, "包管理器: ${detection.packageManager}")
            
            // 准备输出目录
            val outputDir = File(context.filesDir, "frontend_builds/${System.currentTimeMillis()}")
            outputDir.mkdirs()
            
            // 使用 PureBuildEngine 构建
            _buildState.value = NodeBuildState.Building(0f, "构建中...")
            addLog(LogLevel.INFO, "开始构建...")
            
            val buildResult = buildEngine.build(
                projectPath = projectPath,
                outputPath = outputDir.absolutePath
            ) { step, progress ->
                addLog(LogLevel.DEBUG, step)
                _buildState.value = NodeBuildState.Building(progress, step)
            }
            
            if (buildResult.isFailure) {
                throw buildResult.exceptionOrNull() ?: Exception("Build failed")
            }
            
            val result = buildResult.getOrThrow()
            
            addLog(LogLevel.INFO, "构建完成: ${result.fileCount} 个文件, ${formatSize(result.totalSize)}")
            addLog(LogLevel.INFO, "构建方法: ${result.method.name}")
            
            _buildState.value = NodeBuildState.Success(result.outputPath, 0)
            
            Result.success(NodeBuildResult(
                outputPath = result.outputPath,
                framework = detection.framework,
                fileCount = result.fileCount,
                totalSize = result.totalSize,
                duration = 0
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Build failed", e)
            addLog(LogLevel.ERROR, "构建失败: ${e.message}")
            _buildState.value = NodeBuildState.Error(e.message ?: "未知错误", _buildLogs.value)
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
        _buildState.value = NodeBuildState.Idle
        _buildLogs.value = emptyList()
        buildEngine.reset()
    }
    
    /**
     * 获取框架名称
     */
    private fun getFrameworkName(framework: FrontendFramework): String {
        return when (framework) {
            FrontendFramework.VUE -> "Vue.js"
            FrontendFramework.REACT -> "React"
            FrontendFramework.NEXT -> "Next.js"
            FrontendFramework.NUXT -> "Nuxt.js"
            FrontendFramework.ANGULAR -> "Angular"
            FrontendFramework.SVELTE -> "Svelte"
            FrontendFramework.VITE -> "Vite"
            FrontendFramework.UNKNOWN -> "Unknown"
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}

/**
 * Node.js 构建状态
 */
sealed class NodeBuildState {
    object Idle : NodeBuildState()
    object Analyzing : NodeBuildState()
    data class CopyingFiles(val progress: Float) : NodeBuildState()
    data class InstallingDeps(val progress: Float, val currentPackage: String) : NodeBuildState()
    data class Building(val progress: Float, val stage: String) : NodeBuildState()
    object Processing : NodeBuildState()
    data class Success(val outputPath: String, val duration: Long) : NodeBuildState()
    data class Error(val message: String, val logs: List<BuildLogEntry>) : NodeBuildState()
}

/**
 * 构建配置
 */
data class NodeBuildConfig(
    val buildCommand: String? = null,
    val cleanInstall: Boolean = false,
    val envVars: Map<String, String> = emptyMap(),
    val installTimeout: Long = 600_000,
    val buildTimeout: Long = 600_000
)

/**
 * 构建结果
 */
data class NodeBuildResult(
    val outputPath: String,
    val framework: FrontendFramework,
    val fileCount: Int,
    val totalSize: Long,
    val duration: Long
)
