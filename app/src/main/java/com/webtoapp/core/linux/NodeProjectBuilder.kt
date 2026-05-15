package com.webtoapp.core.linux

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.frontend.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max







class NodeProjectBuilder(private val context: Context) {

    companion object {
        private const val TAG = "NodeProjectBuilder"
    }

    private val buildEngine = PureBuildEngine(context)


    private val _buildState = MutableStateFlow<NodeBuildState>(NodeBuildState.Idle)
    val buildState: StateFlow<NodeBuildState> = _buildState


    private val _buildLogs = MutableStateFlow<List<BuildLogEntry>>(emptyList())
    val buildLogs: StateFlow<List<BuildLogEntry>> = _buildLogs




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


            _buildState.value = NodeBuildState.Analyzing
            addLog(LogLevel.INFO, "分析项目...")

            val detection = ProjectDetector.detectProject(projectPath)
            addLog(LogLevel.INFO, "框架: ${getFrameworkName(detection.framework)}")
            addLog(LogLevel.INFO, "包管理器: ${detection.packageManager}")

            _buildState.value = NodeBuildState.InstallingDeps(0f, "准备构建环境")
            addLog(LogLevel.INFO, "准备本地 Node.js 构建环境...")
            LocalBuildEnvironment.ensureInstalled(context) { step, progress ->
                _buildState.value = NodeBuildState.InstallingDeps(progress * 0.2f, step)
                addLog(LogLevel.DEBUG, step)
            }

            val workDir = prepareWorkingDirectory(projectDir)
            val envVars = buildEnvironmentVariables(workDir, config)
            val buildScript = config.buildCommand ?: detection.buildCommand
            if (buildScript.isNullOrBlank()) {
                throw IllegalStateException("未检测到可执行的构建脚本")
            }

            addLog(LogLevel.INFO, "安装项目依赖...")
            _buildState.value = NodeBuildState.InstallingDeps(0.25f, detection.packageManager.name.lowercase())
            val installResult = LocalBuildEnvironment.installDependencies(
                context = context,
                projectDir = workDir,
                packageManager = detection.packageManager,
                cleanInstall = config.cleanInstall,
                timeout = config.installTimeout,
                env = envVars
            ) { line -> addLog(LogLevel.DEBUG, line) }
            if (installResult.exitCode != 0) {
                throw IllegalStateException(buildFailure("依赖安装失败", installResult))
            }

            val outputDir = File(LocalBuildEnvironment.getProjectsRoot(context), System.currentTimeMillis().toString())
            outputDir.mkdirs()

            _buildState.value = NodeBuildState.Building(0.55f, "执行 $buildScript")
            addLog(LogLevel.INFO, "执行构建脚本: $buildScript")
            val scriptResult = LocalBuildEnvironment.runPackageScript(
                context = context,
                projectDir = workDir,
                packageManager = detection.packageManager,
                scriptName = buildScript,
                timeout = config.buildTimeout,
                env = envVars
            ) { line -> addLog(LogLevel.DEBUG, line) }
            if (scriptResult.exitCode != 0) {
                throw IllegalStateException(buildFailure("构建脚本执行失败", scriptResult))
            }

            _buildState.value = NodeBuildState.Processing
            val producedOutput = locateOutputDirectory(workDir, detection)
                ?: throw IllegalStateException("构建完成，但未找到输出目录: ${detection.outputDir}")
            copyDirectory(producedOutput, outputDir)

            val fileCount = outputDir.walkTopDown().count { it.isFile }
            val totalSize = outputDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

            addLog(LogLevel.INFO, "构建完成: $fileCount 个文件, ${formatSize(totalSize)}")
            addLog(LogLevel.INFO, "输出目录: ${producedOutput.absolutePath}")

            _buildState.value = NodeBuildState.Success(outputDir.absolutePath, scriptResult.duration)

            Result.success(NodeBuildResult(
                outputPath = outputDir.absolutePath,
                framework = detection.framework,
                fileCount = fileCount,
                totalSize = totalSize,
                duration = installResult.duration + scriptResult.duration
            ))

        } catch (e: Exception) {
            AppLogger.d(TAG, "Build failed", e)
            addLog(LogLevel.ERROR, "构建失败: ${e.message}")
            _buildState.value = NodeBuildState.Error(e.message ?: "未知错误", _buildLogs.value)
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
        _buildState.value = NodeBuildState.Idle
        _buildLogs.value = emptyList()
        buildEngine.reset()
    }

    private fun prepareWorkingDirectory(projectDir: File): File {
        val workDir = File(LocalBuildEnvironment.getWorkRoot(context), "${projectDir.name}-${System.currentTimeMillis()}")
        _buildState.value = NodeBuildState.CopyingFiles(0f)
        addLog(LogLevel.INFO, "复制项目到本地构建工作区...")
        copyDirectory(projectDir, workDir) { progress ->
            _buildState.value = NodeBuildState.CopyingFiles(progress * 0.2f)
        }
        return workDir
    }

    private fun buildEnvironmentVariables(projectDir: File, config: NodeBuildConfig): Map<String, String> {
        val env = mutableMapOf<String, String>()
        env.putAll(config.envVars)
        env.putIfAbsent("NODE_ENV", "production")
        env.putIfAbsent("CI", "1")
        env.putIfAbsent("npm_config_legacy_peer_deps", "true")
        env.putIfAbsent("npm_config_audit", "false")
        env.putIfAbsent("npm_config_fund", "false")
        env.putIfAbsent("npm_config_update_notifier", "false")
        env.putIfAbsent("npm_config_yes", "true")
        env.putIfAbsent("npm_config_loglevel", "warn")
        env.putIfAbsent("COREPACK_ENABLE_DOWNLOAD_PROMPT", "0")
        env.putIfAbsent("PWD", projectDir.absolutePath)
        return env
    }

    private fun locateOutputDirectory(projectDir: File, detection: ProjectDetectionResult): File? {
        val candidates = linkedSetOf<String>()
        candidates += detection.outputDir
        candidates += File(detection.outputDir).name
        when (detection.framework) {
            FrontendFramework.REACT -> candidates += "build"
            FrontendFramework.NEXT -> {
                candidates += "out"
                candidates += ".next"
            }
            FrontendFramework.NUXT -> {
                candidates += ".output/public"
                candidates += "dist"
            }
            else -> candidates += "dist"
        }

        return candidates
            .map { candidate ->
                val file = if (File(candidate).isAbsolute) File(candidate) else File(projectDir, candidate)
                if (file.isDirectory && file.exists()) file else null
            }
            .firstOrNull { it?.containsFrontEndOutput() == true }
    }

    private fun File.containsFrontEndOutput(): Boolean {
        if (!exists() || !isDirectory) return false
        return File(this, "index.html").exists() || walkTopDown().any { it.isFile && it.name.endsWith(".html") }
    }

    private fun buildFailure(prefix: String, result: ExecutionResult): String {
        val detail = buildString {
            if (result.stderr.isNotBlank()) append(result.stderr.trim())
            if (result.stdout.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(result.stdout.trim())
            }
        }.ifBlank { "exitCode=${result.exitCode}" }
        return "$prefix\n$detail"
    }

    private fun copyDirectory(src: File, dest: File, onProgress: (Float) -> Unit = {}) {
        dest.mkdirs()
        val excludes = setOf(".git", ".idea", ".gradle", "dist", "build", "out", ".next", ".nuxt", ".output", "node_modules")
        val files = src.walkTopDown()
            .filter { file ->
                val rel = file.relativeTo(src).invariantSeparatorsPath
                rel.isNotEmpty() && excludes.none { rel == it || rel.startsWith("$it/") }
            }
            .toList()
        val total = max(1, files.size)
        files.forEachIndexed { index, file ->
            val target = File(dest, file.relativeTo(src).path)
            if (file.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                file.copyTo(target, overwrite = true)
            }
            onProgress((index + 1).toFloat() / total)
        }
    }




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




    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}




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




data class NodeBuildConfig(
    val buildCommand: String? = null,
    val cleanInstall: Boolean = false,
    val envVars: Map<String, String> = emptyMap(),
    val installTimeout: Long = 600_000,
    val buildTimeout: Long = 600_000,
    val allowBuiltinPackagerFallback: Boolean = false
)




data class NodeBuildResult(
    val outputPath: String,
    val framework: FrontendFramework,
    val fileCount: Int,
    val totalSize: Long,
    val duration: Long
)
