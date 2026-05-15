package com.webtoapp.core.linux

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.frontend.PackageManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
























@SuppressLint("StaticFieldLeak")
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

        internal fun resetForTests() {
            instance = null
        }
    }


    private val _state = MutableStateFlow<EnvironmentState>(EnvironmentState.NotInstalled)
    val state: StateFlow<EnvironmentState> = _state

    private val _progress = MutableStateFlow(InstallProgress())
    val installProgress: StateFlow<InstallProgress> = _progress
    private val initializeMutex = Mutex()


    private val buildEngine by lazy { NodeProjectBuilder(context) }




    fun isInstalled(): Boolean = LocalBuildEnvironment.isNodeReady(context) && LocalBuildEnvironment.isNpmReady(context)




    suspend fun checkEnvironment() = withContext(Dispatchers.IO) {
        _state.value = resolveEnvironmentState()
    }






    suspend fun initialize(
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        initializeMutex.withLock {
            try {
                if (_state.value is EnvironmentState.Installing || _state.value is EnvironmentState.Downloading) {
                    return@withLock Result.success(Unit)
                }
                AppLogger.d(TAG, "开始初始化构建环境")

                _state.value = EnvironmentState.Installing("准备本地构建环境", 0f)
                _progress.value = InstallProgress("准备本地构建环境", 0f)
                onProgress("准备本地构建环境", 0f)
                LocalBuildEnvironment.ensureInstalled(context) { step, progress ->
                    _state.value = EnvironmentState.Installing(step, progress)
                    _progress.value = InstallProgress(step, progress)
                    onProgress(step, progress)
                }

                if (!NativeNodeEngine.isAvailable(context)) {
                    try {
                        _state.value = EnvironmentState.Installing(Strings.nodeDownloadEsbuild, 0.1f)
                        NativeNodeEngine.initialize(context) { step, progress ->
                            _state.value = EnvironmentState.Installing(step, progress)
                            _progress.value = InstallProgress(step, progress)
                            onProgress(step, progress)
                        }
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "esbuild optional install failed: ${e.message}")
                    }
                }

                val info = getEnvironmentInfo()
                return@withLock if (info.nodeReady && info.npmReady) {
                    _state.value = EnvironmentState.Ready
                    _progress.value = InstallProgress("Done", 1f)
                    onProgress("Done", 1f)
                    AppLogger.d(TAG, "构建环境初始化完成")
                    Result.success(Unit)
                } else {
                    val message = buildString {
                        append("构建环境未完全就绪")
                        if (!info.nodeReady) append("，Node 启动器不可用")
                        if (!info.npmReady) append("，npm 不可用")
                    }
                    _state.value = EnvironmentState.Error(message, recoverable = true)
                    Result.failure(IllegalStateException(message))
                }

            } catch (e: Exception) {
                AppLogger.e(TAG, "Initialization failed", e)
                _state.value = EnvironmentState.Error(e.message ?: "未知错误", recoverable = true)
                Result.failure(e)
            }
        }
    }




    suspend fun buildProject(
        projectPath: String,
        outputPath: String,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<BuildResult> = withContext(Dispatchers.IO) {
        if (!isInstalled()) {
            return@withContext Result.failure(IllegalStateException("本地构建环境未就绪"))
        }
        val result = buildEngine.buildProject(
            projectPath = projectPath,
            config = NodeBuildConfig(allowBuiltinPackagerFallback = false)
        )
        result.map { nodeResult ->
            val builtOutput = File(nodeResult.outputPath)
            val finalOutput = File(outputPath)
            if (builtOutput.absolutePath != finalOutput.absolutePath) {
                finalOutput.deleteRecursively()
                finalOutput.mkdirs()
                builtOutput.copyRecursively(finalOutput, overwrite = true)
            }

            BuildResult(
                outputPath = finalOutput.absolutePath,
                method = BuildMethod.NODE_PACKAGE_SCRIPT,
                fileCount = nodeResult.fileCount,
                totalSize = nodeResult.totalSize
            )
        }
    }




    suspend fun getEnvironmentInfo(): EnvironmentInfo = withContext(Dispatchers.IO) {
        // 整体降级保护：任何运行时探测失败都不应让 Compose 主协程崩溃。
        // detectToolVersion 内部已经 runCatching 兜底，这里再外包一层是为了防御
        // 未来加新工具时漏写 try-catch、或 isXReady 调用本身抛 IO 异常的极端情况。
        runCatching { computeEnvironmentInfo() }.getOrElse { e ->
            AppLogger.e(TAG, "getEnvironmentInfo failed, returning safe defaults", e)
            // 返回一个全 false 的 EnvironmentInfo，让 UI 看到"什么都没装"的安全状态
            EnvironmentInfo(
                isInstalled = false,
                nodeReady = false,
                npmReady = false,
                pnpmReady = false,
                yarnReady = false,
                nodeVersion = null,
                npmVersion = null,
                yarnVersion = null,
                pnpmVersion = null,
                esbuildAvailable = false,
                phpReady = false,
                composerReady = false,
                pythonReady = false,
                pipReady = false,
                phpVersion = null,
                composerVersion = null,
                pythonVersion = null,
                storageUsed = 0,
                cacheSize = 0,
            )
        }
    }

    private suspend fun computeEnvironmentInfo(): EnvironmentInfo = withContext(Dispatchers.IO) {
        val nodeReady = LocalBuildEnvironment.isNodeReady(context)
        val npmReady = LocalBuildEnvironment.isNpmReady(context)
        val pnpmReady = LocalBuildEnvironment.isPnpmReady(context)
        val yarnReady = LocalBuildEnvironment.isYarnReady(context)
        val esbuildAvailable = NativeNodeEngine.isAvailable(context)
        val nodeVersion = LocalBuildEnvironment.detectToolVersion(context, BuildTool.NODE)
        val npmVersion = LocalBuildEnvironment.detectToolVersion(context, BuildTool.NPM)
        val pnpmVersion = if (pnpmReady) LocalBuildEnvironment.detectToolVersion(context, BuildTool.PNPM) else null
        val yarnVersion = if (yarnReady) LocalBuildEnvironment.detectToolVersion(context, BuildTool.YARN) else null

        val phpReady = LocalBuildEnvironment.isPhpReady(context)
        val composerReady = LocalBuildEnvironment.isComposerReady(context)
        val pythonReady = LocalBuildEnvironment.isPythonReady(context)
        val phpVersion = if (phpReady) LocalBuildEnvironment.detectToolVersion(context, BuildTool.PHP) else null
        val composerVersion = if (composerReady) LocalBuildEnvironment.detectToolVersion(context, BuildTool.COMPOSER) else null
        val pythonVersion = if (pythonReady) LocalBuildEnvironment.detectToolVersion(context, BuildTool.PYTHON) else null

        val storageDirs = listOf(
            LocalBuildEnvironment.getRootDir(context),
            File(context.filesDir, "node_engine"),
            com.webtoapp.core.nodejs.NodeDependencyManager.getDepsDir(context)
        )

        EnvironmentInfo(
            isInstalled = nodeReady && npmReady,
            nodeReady = nodeReady,
            npmReady = npmReady,
            pnpmReady = pnpmReady,
            yarnReady = yarnReady,
            nodeVersion = nodeVersion,
            npmVersion = npmVersion,
            yarnVersion = yarnVersion,
            pnpmVersion = pnpmVersion,
            esbuildAvailable = esbuildAvailable,
            phpReady = phpReady,
            composerReady = composerReady,
            pythonReady = pythonReady,
            pipReady = pythonReady, // pip 与 python 同生命周期
            phpVersion = phpVersion,
            composerVersion = composerVersion,
            pythonVersion = pythonVersion,
            storageUsed = storageDirs.sumOf { calculateSize(it) },
            cacheSize = calculateSize(LocalBuildEnvironment.getNpmCacheDir(context)) +
                calculateSize(File(context.cacheDir, "build_cache")) +
                calculateSize(LocalBuildEnvironment.getWorkRoot(context))
        )
    }

    /**
     * 安装 PHP 运行时（由 WordPressDependencyManager 实际下载二进制）
     */
    suspend fun installPhpRuntime(
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            LocalBuildEnvironment.ensurePhpRuntime(context, onProgress)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "PHP 运行时安装失败", e)
            Result.failure(e)
        }
    }

    /**
     * 下载 composer.phar（依赖 PHP 已就绪）
     */
    suspend fun installComposer(
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            LocalBuildEnvironment.ensureComposer(context, onProgress)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Composer 安装失败", e)
            Result.failure(e)
        }
    }

    suspend fun installPythonRuntime(
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            LocalBuildEnvironment.ensurePythonRuntime(context, onProgress)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Python 运行时安装失败", e)
            Result.failure(e)
        }
    }

    /**
     * 在指定项目目录里跑 composer install（PHP 项目依赖安装）
     */
    suspend fun installPhpProjectDependencies(
        projectDir: File,
        onOutput: (String) -> Unit = {},
    ): Result<ExecutionResult> = withContext(Dispatchers.IO) {
        try {
            val result = LocalBuildEnvironment.installPhpDependencies(context, projectDir, onOutput = onOutput)
            if (result.exitCode == 0) Result.success(result) else Result.failure(IllegalStateException(result.stderr))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 在指定项目目录里跑 pip install -r requirements.txt
     */
    suspend fun installPythonProjectDependencies(
        projectDir: File,
        onOutput: (String) -> Unit = {},
    ): Result<ExecutionResult> = withContext(Dispatchers.IO) {
        try {
            val result = LocalBuildEnvironment.installPythonDependencies(context, projectDir, onOutput)
            if (result.exitCode == 0) Result.success(result) else Result.failure(IllegalStateException(result.stderr))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 在指定项目目录里跑 npm/pnpm/yarn install。
     * 复用 LocalBuildEnvironment.installDependencies——这条链路已经被前端构建（NodeProjectBuilder）验证。
     * 这里和它的区别仅在于：装完就停，不接着跑 build 脚本。
     *
     * 包管理器自动嗅探：根据项目里的锁文件决定（pnpm-lock.yaml / yarn.lock / bun.lockb / 默认 npm），
     * 与 ProjectDetector.detectPackageManager 的逻辑完全一致。
     */
    suspend fun installNodeProjectDependencies(
        projectDir: File,
        cleanInstall: Boolean = false,
        onOutput: (String) -> Unit = {},
    ): Result<ExecutionResult> = withContext(Dispatchers.IO) {
        try {
            if (!File(projectDir, "package.json").exists()) {
                return@withContext Result.failure(IllegalStateException("package.json 不存在"))
            }
            if (!LocalBuildEnvironment.isNodeReady(context)) {
                return@withContext Result.failure(IllegalStateException("Node.js 运行时未就绪"))
            }
            val pm = com.webtoapp.core.frontend.ProjectDetector.detectPackageManager(projectDir)
            // bun 我们当前没装，回退到 npm（行为与 NodeProjectBuilder 一致）
            val effectivePm = if (pm == com.webtoapp.core.frontend.PackageManager.BUN) {
                com.webtoapp.core.frontend.PackageManager.NPM
            } else pm
            val result = LocalBuildEnvironment.installDependencies(
                context = context,
                projectDir = projectDir,
                packageManager = effectivePm,
                cleanInstall = cleanInstall,
                timeout = java.util.concurrent.TimeUnit.MINUTES.toMillis(20),
                env = emptyMap(),
                onOutput = onOutput,
            )
            if (result.exitCode == 0) Result.success(result) else Result.failure(IllegalStateException(result.stderr.ifBlank { "${effectivePm.name.lowercase()} install failed" }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    suspend fun clearCache(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freed = LocalBuildEnvironment.clearCache(context)
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




    suspend fun reset(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            NativeNodeEngine.reset(context)
            LocalBuildEnvironment.reset(context)
            _state.value = EnvironmentState.NotInstalled
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    suspend fun executeCommand(
        command: String,
        args: List<String> = emptyList(),
        workingDir: String = "",
        env: Map<String, String> = emptyMap(),
        timeout: Long = 300_000,
        onOutput: (String) -> Unit = {}
    ): CommandResult = withContext(Dispatchers.IO) {
        val result = LocalBuildEnvironment.executeCommand(
            context = context,
            command = command,
            args = args,
            workingDir = if (workingDir.isNotEmpty()) File(workingDir) else context.filesDir,
            env = env,
            timeout = timeout,
            onOutput = onOutput
        )
        CommandResult(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr,
            duration = result.duration
        )
    }

    private fun calculateSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun resolveEnvironmentState(): EnvironmentState {
        val nodeReady = LocalBuildEnvironment.isNodeReady(context)
        val npmReady = LocalBuildEnvironment.isNpmReady(context)
        return when {
            nodeReady && npmReady -> EnvironmentState.Ready
            nodeReady -> EnvironmentState.NodeInstalledNpmMissing
            else -> EnvironmentState.NotInstalled
        }
    }
}



sealed class EnvironmentState {
    object NotInstalled : EnvironmentState()
    object NodeNotInstalled : EnvironmentState()
    object NodeInstalledNpmMissing : EnvironmentState()
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
    val nodeReady: Boolean,
    val npmReady: Boolean,
    val pnpmReady: Boolean,
    val yarnReady: Boolean,
    val nodeVersion: String?,
    val npmVersion: String?,
    val yarnVersion: String?,
    val pnpmVersion: String?,
    val esbuildAvailable: Boolean = false,
    // PHP / Python 运行时（来自各自的 DependencyManager，由 LocalBuildEnvironment 桥接）
    val phpReady: Boolean = false,
    val composerReady: Boolean = false,
    val pythonReady: Boolean = false,
    val pipReady: Boolean = false,
    val phpVersion: String? = null,
    val composerVersion: String? = null,
    val pythonVersion: String? = null,
    val storageUsed: Long,
    val cacheSize: Long
)

data class InstallProgress(
    val currentStep: String = "",
    val progress: Float = 0f
)
