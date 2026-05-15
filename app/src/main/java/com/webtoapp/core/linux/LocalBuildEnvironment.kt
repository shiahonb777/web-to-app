package com.webtoapp.core.linux

import android.content.Context
import com.google.gson.JsonObject
import com.webtoapp.core.frontend.PackageManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.nodejs.NodeDependencyManager
import com.webtoapp.util.GsonProvider
import com.webtoapp.util.destroyForciblyCompat
import com.webtoapp.util.waitForCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object LocalBuildEnvironment {

    private const val TAG = "LocalBuildEnv"
    private const val NPM_VERSION = "10.9.0"
    private const val PNPM_VERSION = "9.15.9"
    private const val YARN_VERSION = "1.22.22"
    private const val PACKAGED_LAUNCHER_NAME = "libnode_launcher.so"
    private const val LEGACY_LAUNCHER_NAME = "node"
    private const val HTTP_CONNECT_TIMEOUT_MS = 30_000
    private const val HTTP_READ_TIMEOUT_MS = 120_000
    private val installMutex = Mutex()

    private val gson = GsonProvider.gson

    fun getRootDir(context: Context): File = File(context.filesDir, "local_build_env").also { it.mkdirs() }

    fun getBinDir(context: Context): File = File(getRootDir(context), "bin").also { it.mkdirs() }

    fun getToolDir(context: Context): File = File(getRootDir(context), "tools").also { it.mkdirs() }

    fun getNpmCacheDir(context: Context): File = File(getRootDir(context), "cache/npm").also { it.mkdirs() }

    fun getNpmPrefixDir(context: Context): File = File(getRootDir(context), "prefix").also { it.mkdirs() }

    fun getProjectsRoot(context: Context): File = File(context.filesDir, "frontend_builds").also { it.mkdirs() }

    fun getWorkRoot(context: Context): File = File(context.cacheDir, "frontend_build_work").also { it.mkdirs() }

    private fun getPackagedLauncherPath(context: Context): File =
        File(context.applicationInfo.nativeLibraryDir, PACKAGED_LAUNCHER_NAME)

    private fun getLegacyLauncherPath(context: Context): File =
        File(getBinDir(context), LEGACY_LAUNCHER_NAME)

    fun getLauncherPath(context: Context): File {
        val packagedLauncher = getPackagedLauncherPath(context)
        return if (packagedLauncher.exists()) packagedLauncher else getLegacyLauncherPath(context)
    }

    fun getNpmCliPath(context: Context): File =
        File(getToolDir(context), "npm/package/bin/npm-cli.js")

    fun getPnpmCliPath(context: Context): File =
        File(getToolDir(context), "pnpm/package/bin/pnpm.cjs")

    fun getYarnCliPath(context: Context): File =
        File(getToolDir(context), "yarn/package/bin/yarn.js")

    fun getNodeLibPath(context: Context): String? = NodeDependencyManager.getNodeLibraryPath(context)

    fun hasNodeLauncher(context: Context): Boolean = getLauncherPath(context).exists()

    fun isNodeReady(context: Context): Boolean = hasNodeLauncher(context) && getNodeLibPath(context) != null

    fun isNpmReady(context: Context): Boolean = getNpmCliPath(context).exists() && isNodeReady(context)

    fun isPnpmReady(context: Context): Boolean = getPnpmCliPath(context).exists() && isNodeReady(context)

    fun isYarnReady(context: Context): Boolean = getYarnCliPath(context).exists() && isNodeReady(context)

    // ========================================================================
    // PHP / Python 运行时桥接：直接转发到各自现有的 manager，不重复造轮子
    // 这些 manager 的下载/初始化逻辑已经被 WordPress / PHP App / Python App 验证过，
    // LocalBuildEnvironment 把它们包装成统一的"语言运行时"概念给 UI 使用
    // ========================================================================

    /**
     * Android 14+ 默认禁止 untrusted_app 执行 app_data 目录下的 ELF（EACCES）。
     * 唯一可靠的 PHP 二进制位置是 nativeLibraryDir（APK 解压时由系统 +x 标记），
     * 也就是 build.gradle.kts 里 `downloadPhpBinary` 任务打进 jniLibs/<abi>/libphp.so 的那个。
     *
     * 之前 WordPressDependencyManager.isPhpReady 会同时承认下载到 wordpress_deps 目录的
     * PHP 二进制，但那条路径在新版本 Android 上无法 ProcessBuilder.start。
     * 因此本门面只承认 nativeLibraryDir 的 libphp.so——下载目录的视为"装了但用不了"。
     */
    fun isPhpReady(context: Context): Boolean {
        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        return nativePhp.exists() && nativePhp.canExecute()
    }

    fun getPhpExecutablePath(context: Context): String? {
        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        return if (nativePhp.exists() && nativePhp.canExecute()) nativePhp.absolutePath else null
    }

    fun isPythonReady(context: Context): Boolean =
        com.webtoapp.core.python.PythonDependencyManager.isPythonReady(context)

    fun getPythonExecutablePath(context: Context): String? {
        if (!isPythonReady(context)) return null
        return com.webtoapp.core.python.PythonDependencyManager.getPythonExecutablePath(context).takeIf { it.isNotBlank() }
    }

    /**
     * Composer 是 PHP 项目的依赖管理器（类似 npm）。
     * 我们以单文件 phar 形式分发：php composer.phar install
     *
     * 注意：composer.phar 比较大（约 3.5 MB），按需下载——只在用户点击"安装 composer"时拉
     */
    private const val COMPOSER_PHAR_URL = "https://getcomposer.org/download/latest-stable/composer.phar"

    fun getComposerPharPath(context: Context): File =
        File(getToolDir(context), "composer/composer.phar")

    fun isComposerReady(context: Context): Boolean =
        getComposerPharPath(context).exists() && isPhpReady(context)

    /**
     * 下载 composer.phar 到本地（不下载 PHP 自身——PHP 由 WordPressDependencyManager 管）
     */
    suspend fun ensureComposer(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (!isPhpReady(context)) {
            throw IOException("PHP 运行时未就绪，请先安装 PHP 后再装 composer")
        }
        val target = getComposerPharPath(context)
        if (target.exists()) return@withContext
        onProgress("下载 composer.phar", 0.5f)
        target.parentFile?.mkdirs()
        downloadFile(COMPOSER_PHAR_URL, target)
    }

    suspend fun ensurePhpRuntime(
        context: Context,
        @Suppress("UNUSED_PARAMETER") onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (isPhpReady(context)) return@withContext
        // Android 14+ 不允许 untrusted_app 执行 app_data 目录下的 ELF——
        // 这意味着即使下载 PHP 到 wordpress_deps 也无法 ProcessBuilder.start。
        // PHP 二进制必须在打 APK 时就放进 jniLibs/<abi>/libphp.so，由系统解压时自动赋 +x。
        // 当前 APK 内不含 libphp.so（可能是 build.gradle.kts:downloadPhpBinary 没跑），
        // 用户只能等带有 PHP 的版本——这里直接抛错，UI 会展示给用户看
        throw IOException("此 APK 不包含 PHP 运行时。需要重装包含 PHP 的完整版 WebToApp（构建时执行 ./gradlew downloadPhpBinary）")
    }

    suspend fun ensurePythonRuntime(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (isPythonReady(context)) return@withContext
        onProgress("下载 Python 运行时", 0.05f)
        val ok = com.webtoapp.core.python.PythonDependencyManager.downloadPythonRuntime(context)
        if (!ok) throw IOException("Python 运行时下载失败")
    }

    /**
     * 在指定项目目录里执行 `php composer.phar install`。
     * 调用方负责传入项目目录（包含 composer.json）；如果没有 composer.json 直接返回成功。
     */
    suspend fun installPhpDependencies(
        context: Context,
        projectDir: File,
        timeout: Long = TimeUnit.MINUTES.toMillis(15),
        onOutput: (String) -> Unit = {}
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val composerJson = File(projectDir, "composer.json")
        if (!composerJson.exists()) {
            onOutput("composer.json 不存在，跳过 composer 依赖安装")
            return@withContext ExecutionResult(0, "no composer.json", "", 0)
        }
        val phpBin = getPhpExecutablePath(context)
            ?: return@withContext ExecutionResult(-1, "", "PHP 运行时未就绪", 0)
        val composer = getComposerPharPath(context)
        if (!composer.exists()) {
            return@withContext ExecutionResult(-1, "", "composer.phar 未安装，请先在「本地构建环境」里安装 Composer", 0)
        }
        onOutput("php composer.phar install --no-interaction --no-progress")
        executePhp(
            context = context,
            arguments = listOf(composer.absolutePath, "install", "--no-interaction", "--no-progress"),
            workingDir = projectDir,
            timeout = timeout,
            onOutput = onOutput,
        )
    }

    /**
     * 在指定项目目录里执行 pip install -r requirements.txt。
     * 直接复用 PythonDependencyManager 已经验证过的 pip 调用链——它处理了 musl 链接器、
     * pip 缓存、site-packages 路径等所有 Android 上的特殊情况。
     */
    suspend fun installPythonDependencies(
        context: Context,
        projectDir: File,
        onOutput: (String) -> Unit = {}
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val reqFile = File(projectDir, "requirements.txt")
        if (!reqFile.exists()) {
            onOutput("requirements.txt 不存在，跳过 pip 依赖安装")
            return@withContext ExecutionResult(0, "no requirements.txt", "", 0)
        }
        if (!isPythonReady(context)) {
            return@withContext ExecutionResult(-1, "", "Python 运行时未就绪", 0)
        }
        val start = System.currentTimeMillis()
        val ok = com.webtoapp.core.python.PythonDependencyManager.installRequirements(
            context = context,
            projectDir = projectDir,
            onOutput = onOutput,
        )
        val duration = System.currentTimeMillis() - start
        ExecutionResult(
            exitCode = if (ok) 0 else -1,
            stdout = "",
            stderr = if (ok) "" else "pip install 失败",
            duration = duration,
        )
    }

    /**
     * 通用 PHP 进程执行器——与 executeNode 对齐，调用方传入 args（含 composer.phar 路径或脚本路径），
     * 我们组装好 PHP 二进制并启动子进程。
     */
    suspend fun executePhp(
        context: Context,
        arguments: List<String>,
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        timeout: Long = TimeUnit.MINUTES.toMillis(10),
        onOutput: (String) -> Unit = {},
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val phpBin = getPhpExecutablePath(context)
            ?: return@withContext ExecutionResult(-1, "", "PHP 运行时未就绪", 0)
        val start = System.currentTimeMillis()
        val command = mutableListOf(phpBin)
        command.addAll(arguments)

        val pb = ProcessBuilder(command)
        pb.directory(workingDir)
        pb.redirectErrorStream(false)
        val processEnv = pb.environment()
        processEnv["HOME"] = getRootDir(context).absolutePath
        processEnv["TMPDIR"] = context.cacheDir.absolutePath
        processEnv["COMPOSER_HOME"] = File(getRootDir(context), "composer").absolutePath
        processEnv["COMPOSER_CACHE_DIR"] = File(getRootDir(context), "cache/composer").absolutePath
        processEnv["COMPOSER_NO_INTERACTION"] = "1"
        env.forEach { (k, v) -> processEnv[k] = v }

        val process = pb.start()
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val tOut = Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                stdout.appendLine(line)
                onOutput(line)
            }
        }
        val tErr = Thread {
            process.errorStream.bufferedReader().forEachLine { line ->
                stderr.appendLine(line)
                onOutput(line)
            }
        }
        tOut.start(); tErr.start()
        val completed = process.waitForCompat(timeout)
        tOut.join(2_000); tErr.join(2_000)
        val exit = if (completed) process.exitValue() else { process.destroyForciblyCompat(); -1 }
        ExecutionResult(exit, stdout.toString(), stderr.toString(), System.currentTimeMillis() - start)
    }

    suspend fun ensureInstalled(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        installMutex.withLock {
            ensureNodeLauncher(context, onProgress)
            ensureNpm(context, onProgress)
            ensurePnpm(context, onProgress)
            ensureYarn(context, onProgress)
        }
    }

    suspend fun ensureNodeLauncher(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (!NodeDependencyManager.isNodeReady(context)) {
            onProgress("下载 Node.js 运行时", 0.05f)
            val success = NodeDependencyManager.downloadNodeRuntime(context)
            if (!success) {
                throw IOException("Node.js 运行时下载失败")
            }
        }

        val packagedLauncher = getPackagedLauncherPath(context)
        if (packagedLauncher.exists()) {
            return@withContext
        }

        val legacyLauncher = getLegacyLauncherPath(context)
        if (legacyLauncher.exists() && legacyLauncher.canExecute()) {
            AppLogger.w(TAG, "使用旧版 node 启动器路径: ${legacyLauncher.absolutePath}")
            return@withContext
        }

        onProgress("准备 node 启动器", 0.12f)
        throw IOException("node 启动器未随 APK 打包: ${packagedLauncher.absolutePath}")
    }

    suspend fun ensureNpm(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (getNpmCliPath(context).exists()) return@withContext
        onProgress("安装 npm", 0.35f)
        installTarballPackage(
            context = context,
            tarballUrl = "https://registry.npmjs.org/npm/-/npm-$NPM_VERSION.tgz",
            targetDir = File(getToolDir(context), "npm")
        )
    }

    suspend fun ensurePnpm(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (getPnpmCliPath(context).exists()) return@withContext
        onProgress("安装 pnpm", 0.6f)
        installTarballPackage(
            context = context,
            tarballUrl = "https://registry.npmjs.org/pnpm/-/pnpm-$PNPM_VERSION.tgz",
            targetDir = File(getToolDir(context), "pnpm")
        )
    }

    suspend fun ensureYarn(
        context: Context,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (getYarnCliPath(context).exists()) return@withContext
        onProgress("安装 yarn", 0.8f)
        installTarballPackage(
            context = context,
            tarballUrl = "https://registry.npmjs.org/yarn/-/yarn-$YARN_VERSION.tgz",
            targetDir = File(getToolDir(context), "yarn")
        )
    }

    suspend fun detectToolVersion(context: Context, tool: BuildTool): String? = withContext(Dispatchers.IO) {
        // 任何工具版本探测都要包 try-catch：
        // 这个方法在 LinuxEnvironmentManager.getEnvironmentInfo 里被同步调用，
        // 而 getEnvironmentInfo 在 Compose LaunchedEffect (AndroidUiDispatcher) 里跑——
        // 一旦 ProcessBuilder.start 抛 EACCES（如 Android 14+ 禁止执行 app_data 下二进制），
        // 异常会冒泡到主协程，撑爆 UI 进程导致 crash。
        runCatching {
            when (tool) {
                BuildTool.NODE, BuildTool.NPM, BuildTool.PNPM, BuildTool.YARN -> {
                    if (!isNodeReady(context)) return@runCatching null
                    val args = when (tool) {
                        BuildTool.NODE -> listOf("--version")
                        BuildTool.NPM -> listOf(getNpmCliPath(context).absolutePath, "--version")
                        BuildTool.PNPM -> listOf(getPnpmCliPath(context).absolutePath, "--version")
                        BuildTool.YARN -> listOf(getYarnCliPath(context).absolutePath, "--version")
                        else -> return@runCatching null
                    }
                    val result = executeNode(context, args, context.filesDir)
                    if (result.exitCode == 0) result.stdout.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { null } else null
                }
                BuildTool.PHP -> {
                    if (!isPhpReady(context)) return@runCatching null
                    val result = executePhp(context, listOf("-v"), context.filesDir)
                    if (result.exitCode == 0) {
                        Regex("""PHP\s+(\S+)""").find(result.stdout)?.groupValues?.get(1)
                    } else null
                }
                BuildTool.COMPOSER -> {
                    if (!isComposerReady(context)) return@runCatching null
                    val composer = getComposerPharPath(context)
                    val result = executePhp(context, listOf(composer.absolutePath, "--version", "--no-ansi"), context.filesDir)
                    if (result.exitCode == 0) {
                        Regex("""Composer\s+version\s+(\S+)""").find(result.stdout)?.groupValues?.get(1)
                    } else null
                }
                BuildTool.PYTHON -> {
                    if (!isPythonReady(context)) return@runCatching null
                    val bin = getPythonExecutablePath(context) ?: return@runCatching null
                    File(bin).takeIf { it.exists() }?.let { "Python 3.x" }
                }
                BuildTool.PIP -> {
                    if (isPythonReady(context)) "pip (bundled)" else null
                }
            }
        }.getOrElse { e ->
            AppLogger.w(TAG, "detectToolVersion failed for $tool: ${e.message}")
            null
        }
    }

    suspend fun installDependencies(
        context: Context,
        projectDir: File,
        packageManager: PackageManager,
        cleanInstall: Boolean,
        timeout: Long,
        env: Map<String, String>,
        onOutput: (String) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val packageLock = File(projectDir, "package-lock.json")
        val installArgs = when (packageManager) {
            PackageManager.PNPM -> listOf(getPnpmCliPath(context).absolutePath, "install", "--prod=false")
            PackageManager.YARN -> listOf(getYarnCliPath(context).absolutePath, "install")
            else -> {
                if (cleanInstall && packageLock.exists()) {
                    listOf(getNpmCliPath(context).absolutePath, "ci")
                } else {
                    listOf(getNpmCliPath(context).absolutePath, "install")
                }
            }
        }
        executeNode(
            context = context,
            arguments = installArgs,
            workingDir = projectDir,
            env = env,
            timeout = timeout,
            onOutput = onOutput
        )
    }

    suspend fun runPackageScript(
        context: Context,
        projectDir: File,
        packageManager: PackageManager,
        scriptName: String,
        timeout: Long,
        env: Map<String, String>,
        onOutput: (String) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val packageJson = File(projectDir, "package.json")
        if (!packageJson.exists()) {
            return@withContext ExecutionResult(-1, "", "package.json 不存在", 0)
        }
        val script = parsePackageScript(packageJson, scriptName)
            ?: return@withContext ExecutionResult(-1, "", "未找到脚本: $scriptName", 0)
        AppLogger.d(TAG, "run script $scriptName: $script")

        val args = when (packageManager) {
            PackageManager.PNPM -> listOf(getPnpmCliPath(context).absolutePath, "run", scriptName)
            PackageManager.YARN -> listOf(getYarnCliPath(context).absolutePath, "run", scriptName)
            else -> listOf(getNpmCliPath(context).absolutePath, "run", scriptName)
        }
        executeNode(
            context = context,
            arguments = args,
            workingDir = projectDir,
            env = env,
            timeout = timeout,
            onOutput = onOutput
        )
    }

    suspend fun executeCommand(
        context: Context,
        command: String,
        args: List<String>,
        workingDir: File,
        env: Map<String, String>,
        timeout: Long,
        onOutput: (String) -> Unit = {}
    ): ExecutionResult = withContext(Dispatchers.IO) {
        when (command) {
            "node" -> executeNode(context, args, workingDir, env, timeout, onOutput)
            "npm" -> executeNode(context, listOf(getNpmCliPath(context).absolutePath) + args, workingDir, env, timeout, onOutput)
            "pnpm" -> executeNode(context, listOf(getPnpmCliPath(context).absolutePath) + args, workingDir, env, timeout, onOutput)
            "yarn" -> executeNode(context, listOf(getYarnCliPath(context).absolutePath) + args, workingDir, env, timeout, onOutput)
            "esbuild" -> NativeNodeEngine.executeEsbuild(context, args, workingDir, env, timeout, onOutput)
            "php" -> executePhp(context, args, workingDir, env, timeout, onOutput)
            "composer" -> executePhp(
                context,
                listOf(getComposerPharPath(context).absolutePath) + args,
                workingDir, env, timeout, onOutput,
            )
            else -> ExecutionResult(-1, "", "不支持的命令: $command", 0)
        }
    }

    suspend fun executeNode(
        context: Context,
        arguments: List<String>,
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        timeout: Long = TimeUnit.MINUTES.toMillis(10),
        onOutput: (String) -> Unit = {}
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val launcher = getLauncherPath(context)
        val nodeLib = getNodeLibPath(context)
            ?: return@withContext ExecutionResult(-1, "", "Node.js 运行时未就绪", 0)
        if (!launcher.exists()) {
            return@withContext ExecutionResult(-1, "", "node 启动器未安装", 0)
        }

        val start = System.currentTimeMillis()
        val command = mutableListOf(launcher.absolutePath)
        command.addAll(arguments)

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(workingDir)
        processBuilder.redirectErrorStream(false)

        val processEnv = processBuilder.environment()
        val rootDir = getRootDir(context)
        val originalPath = processEnv["PATH"].orEmpty()
        processEnv["HOME"] = rootDir.absolutePath
        processEnv["TMPDIR"] = context.cacheDir.absolutePath
        processEnv["WTA_NODE_LIB"] = nodeLib
        // libnode.so 依赖 libc++_shared.so（C++ 标准库），后者在 APK 的 nativeLibraryDir 里。
        // 当 libnode.so 是运行时下载到 files/nodejs_deps/ 的（而非 APK 内置），
        // dlopen 的 linker 搜索路径不包含 nativeLibraryDir，必须显式设 LD_LIBRARY_PATH。
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val existingLdPath = processEnv["LD_LIBRARY_PATH"].orEmpty()
        processEnv["LD_LIBRARY_PATH"] = if (existingLdPath.isBlank()) {
            nativeLibDir
        } else {
            "$nativeLibDir${File.pathSeparator}$existingLdPath"
        }
        processEnv["NODE_PATH"] = buildNodePath(context, workingDir)
        processEnv["PATH"] = listOf(
            getBinDir(context).absolutePath,
            File(workingDir, "node_modules/.bin").absolutePath,
            File(getNpmPrefixDir(context), "bin").absolutePath,
            originalPath
        ).filter { it.isNotBlank() }.joinToString(File.pathSeparator)
        processEnv["npm_config_cache"] = getNpmCacheDir(context).absolutePath
        processEnv["npm_config_prefix"] = getNpmPrefixDir(context).absolutePath
        processEnv["npm_config_userconfig"] = File(rootDir, "npmrc").absolutePath
        processEnv["COREPACK_ENABLE_AUTO_PIN"] = "0"
        processEnv["CI"] = "1"
        env.forEach { (key, value) -> processEnv[key] = value }

        AppLogger.d(TAG, "exec: ${command.joinToString(" ")}")
        val process = processBuilder.start()

        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val stdoutThread = Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                stdout.appendLine(line)
                onOutput(line)
            }
        }
        val stderrThread = Thread {
            process.errorStream.bufferedReader().forEachLine { line ->
                stderr.appendLine(line)
                onOutput(line)
            }
        }
        stdoutThread.start()
        stderrThread.start()

        val completed = process.waitForCompat(timeout)
        stdoutThread.join(2_000)
        stderrThread.join(2_000)

        val exitCode = if (completed) process.exitValue() else {
            process.destroyForciblyCompat()
            -1
        }
        ExecutionResult(
            exitCode = exitCode,
            stdout = stdout.toString(),
            stderr = stderr.toString(),
            duration = System.currentTimeMillis() - start
        )
    }

    suspend fun reset(context: Context) = withContext(Dispatchers.IO) {
        getRootDir(context).deleteRecursively()
        getWorkRoot(context).deleteRecursively()
    }

    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        val dirs = listOf(getNpmCacheDir(context), getWorkRoot(context))
        var total = 0L
        dirs.forEach { dir ->
            total += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            dir.deleteRecursively()
        }
        total
    }

    private fun buildNodePath(context: Context, workingDir: File): String {
        val paths = linkedSetOf<String>()
        paths += File(workingDir, "node_modules").absolutePath
        paths += File(getNpmPrefixDir(context), "lib/node_modules").absolutePath
        listOf("npm", "pnpm", "yarn").forEach { tool ->
            paths += File(getToolDir(context), "$tool/package/node_modules").absolutePath
        }
        return paths.joinToString(File.pathSeparator)
    }

    private fun parsePackageScript(packageJson: File, scriptName: String): String? {
        val json = gson.fromJson(packageJson.readText(), JsonObject::class.java)
        val scripts = json.getAsJsonObject("scripts") ?: return null
        return scripts.get(scriptName)?.asString
    }

    private suspend fun installTarballPackage(
        context: Context,
        tarballUrl: String,
        targetDir: File
    ) = withContext(Dispatchers.IO) {
        targetDir.parentFile?.mkdirs()
        if (targetDir.exists()) targetDir.deleteRecursively()
        val tempFile = File.createTempFile("wta-tool", ".tgz", context.cacheDir)
        try {
            downloadFile(tarballUrl, tempFile)
            extractTarGz(tempFile, targetDir)
        } finally {
            tempFile.delete()
        }
    }

    private fun downloadFile(url: String, targetFile: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = HTTP_CONNECT_TIMEOUT_MS
        connection.readTimeout = HTTP_READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractTarGz(archive: File, destinationDir: File) {
        destinationDir.mkdirs()
        GZIPInputStream(FileInputStream(archive)).use { gzip ->
            TarArchiveInputStream(gzip).use { tar ->
                var entry = tar.nextTarEntry
                while (entry != null) {
                    val dest = File(destinationDir, entry.name.removePrefix("./"))
                    if (entry.isDirectory) {
                        dest.mkdirs()
                    } else {
                        dest.parentFile?.mkdirs()
                        FileOutputStream(dest).use { output -> tar.copyTo(output) }
                        if ((entry.mode and 0b001_001_001) != 0) {
                            dest.setExecutable(true, false)
                        }
                    }
                    entry = tar.nextTarEntry
                }
            }
        }
    }

}

enum class BuildTool {
    NODE,
    NPM,
    PNPM,
    YARN,
    PHP,
    COMPOSER,
    PYTHON,
    PIP
}
