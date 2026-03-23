package com.webtoapp.core.linux

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 构建环境管理器
 * 
 * 第一性原理重构：
 * 
 * 问题本质：我们需要在 Android 上构建前端项目
 * 
 * 传统方案的问题：
 * - PRoot + Alpine + Node.js = 三层依赖，复杂且脆弱
 * - 依赖外部下载，网络问题导致失败
 * - SELinux 限制执行权限
 * 
 * 新方案：
 * 1. 优先使用已构建的项目（dist/build 目录）
 * 2. 如果需要构建，使用 esbuild（为 Android 编译的原生二进制）
 * 3. 最后手段：纯 Kotlin 实现的简单打包器
 * 
 * 这个方案：
 * - 不依赖 PRoot
 * - 不依赖 Linux rootfs
 * - 不依赖 Node.js
 * - 渐进式降级，总能工作
 */
class LinuxEnvironmentManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "LinuxEnvManager"
        
        @Volatile
        private var instance: LinuxEnvironmentManager? = null
        
        fun getInstance(context: Context): LinuxEnvironmentManager {
            return instance ?: synchronized(this) {
                instance ?: LinuxEnvironmentManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    // 状态
    private val _state = MutableStateFlow<EnvironmentState>(EnvironmentState.NotInstalled)
    val state: StateFlow<EnvironmentState> = _state
    
    private val _progress = MutableStateFlow(InstallProgress())
    val installProgress: StateFlow<InstallProgress> = _progress
    
    // Build引擎
    private val buildEngine by lazy { PureBuildEngine(context) }
    
    // 引擎目录
    private val engineDir: File by lazy { File(context.filesDir, "build_engine") }
    
    /**
     * 检查是否已安装（esbuild 可用）
     */
    fun isInstalled(): Boolean = NativeNodeEngine.isAvailable(context)
    
    /**
     * 检查环境状态
     */
    suspend fun checkEnvironment() = withContext(Dispatchers.IO) {
        _state.value = when {
            NativeNodeEngine.isAvailable(context) -> EnvironmentState.Ready
            else -> EnvironmentState.NotInstalled
        }
    }
    
    /**
     * 初始化环境
     * 
     * 下载并安装 esbuild
     */
    suspend fun initialize(
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始初始化构建环境")
            
            _state.value = EnvironmentState.Downloading("esbuild", 0f)
            onProgress("初始化构建工具...", 0.1f)
            
            val result = NativeNodeEngine.initialize(context) { step, progress ->
                _state.value = EnvironmentState.Installing(step, progress)
                _progress.value = InstallProgress(step, progress)
                onProgress(step, progress)
            }
            
            if (result.isFailure) {
                // esbuild 安装失败，但我们仍然可以工作
                // 使用纯 Kotlin 打包器作为后备
                Log.w(TAG, "esbuild 安装失败，将使用内置打包器")
                _state.value = EnvironmentState.Ready
                onProgress("使用内置打包器", 1f)
                return@withContext Result.success(Unit)
            }
            
            _state.value = EnvironmentState.Ready
            onProgress("Done", 1f)
            
            Log.i(TAG, "构建环境初始化完成")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            // 即使失败，我们仍然标记为 Ready
            // 因为纯 Kotlin 打包器不需要任何外部依赖
            _state.value = EnvironmentState.Ready
            Result.success(Unit)
        }
    }
    
    /**
     * 构建项目
     */
    suspend fun buildProject(
        projectPath: String,
        outputPath: String,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<BuildResult> = withContext(Dispatchers.IO) {
        buildEngine.build(projectPath, outputPath, onProgress)
    }
    
    /**
     * 获取环境信息
     */
    suspend fun getEnvironmentInfo(): EnvironmentInfo = withContext(Dispatchers.IO) {
        val esbuildAvailable = NativeNodeEngine.isAvailable(context)
        
        EnvironmentInfo(
            isInstalled = true, // 总是可用（至少有纯 Kotlin 打包器）
            nodeVersion = null,
            npmVersion = null,
            yarnVersion = null,
            pnpmVersion = null,
            esbuildAvailable = esbuildAvailable,
            storageUsed = calculateSize(engineDir),
            cacheSize = calculateSize(File(context.cacheDir, "build_cache"))
        )
    }
    
    /**
     * 清理缓存
     */
    suspend fun clearCache(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freed = 0L
            val cacheDir = File(context.cacheDir, "build_cache")
            if (cacheDir.exists()) {
                freed = calculateSize(cacheDir)
                cacheDir.deleteRecursively()
            }
            Result.success(freed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 重置环境
     */
    suspend fun reset(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            NativeNodeEngine.reset(context)
            engineDir.deleteRecursively()
            _state.value = EnvironmentState.NotInstalled
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 执行命令（兼容旧接口）
     */
    suspend fun executeCommand(
        command: String,
        args: List<String> = emptyList(),
        workingDir: String = "",
        env: Map<String, String> = emptyMap(),
        timeout: Long = 300_000,
        onOutput: (String) -> Unit = {}
    ): CommandResult = withContext(Dispatchers.IO) {
        // If it is esbuild 命令
        if (command == "esbuild" && NativeNodeEngine.isAvailable(context)) {
            val result = NativeNodeEngine.executeEsbuild(
                context = context,
                args = args,
                workingDir = if (workingDir.isNotEmpty()) File(workingDir) else context.filesDir,
                env = env,
                timeout = timeout,
                onOutput = onOutput
            )
            return@withContext CommandResult(
                exitCode = result.exitCode,
                stdout = result.stdout,
                stderr = result.stderr,
                duration = result.duration
            )
        }
        
        // 其他命令不支持
        CommandResult(
            exitCode = -1,
            stdout = "",
            stderr = "不支持的命令: $command",
            duration = 0
        )
    }
    
    private fun calculateSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}

// ========== 数据类 ==========

sealed class EnvironmentState {
    object NotInstalled : EnvironmentState()
    object NodeNotInstalled : EnvironmentState()
    data class Downloading(val component: String, val progress: Float) : EnvironmentState()
    data class Installing(val step: String, val progress: Float) : EnvironmentState()
    object Ready : EnvironmentState()
    data class Error(val message: String, val recoverable: Boolean) : EnvironmentState()
}

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val duration: Long
)

data class EnvironmentInfo(
    val isInstalled: Boolean,
    val nodeVersion: String?,
    val npmVersion: String?,
    val yarnVersion: String?,
    val pnpmVersion: String?,
    val esbuildAvailable: Boolean = false,
    val storageUsed: Long,
    val cacheSize: Long
)

data class InstallProgress(
    val currentStep: String = "",
    val progress: Float = 0f
)
