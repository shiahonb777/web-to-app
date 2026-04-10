package com.webtoapp.core.python

import android.content.Context
import android.os.Build
import com.webtoapp.core.download.DependencyDownloadEngine
import com.webtoapp.core.download.DependencyDownloadNotification
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Python 运行时依赖管理器
 * 
 * 负责按需下载 Python 预编译二进制（python-build-standalone 项目提供 Android 原生构建）。
 * 下载后通过 ProcessBuilder 执行 Python 脚本，类似 PHP 的执行方式。
 * 
 * 加载策略：
 * - 主应用预览：从下载缓存 (python_deps/{abi}/) 执行
 * - 导出 APK (Shell 模式)：打包为 lib/{abi}/libpython*.so，安装后位于 nativeLibraryDir
 * 
 * 根据设备语言自动选择国内镜像或国际源。
 */
object PythonDependencyManager {
    
    private const val TAG = "PythonDependencyManager"
    
    /**
     * Python 版本
     * 来源: github.com/astral-sh/python-build-standalone
     * 格式: tar.gz (install_only_stripped) 包含 Python 解释器 + 标准库
     * 使用 musl 动态链接构建，需配合 musl 动态链接器在 Android 上执行
     * 
     * musl 动态链接器来源: Alpine Linux 官方包 (dl-cdn.alpinelinux.org)
     * 执行方式: ld-musl-{arch}.so.1 --library-path <lib_dir> python3.12 script.py
     */
    const val PYTHON_VERSION = "3.12"
    const val PYTHON_FULL_VERSION = "3.12.12"
    private const val PYTHON_BUILD_TAG = "20260211"
    
    // musl 动态链接器版本（来自 Alpine Linux）
    private const val MUSL_VERSION = "1.2.5-r9"
    private const val MUSL_ALPINE_BRANCH = "v3.21"
    
    // ==================== 镜像源配置 ====================
    
    enum class MirrorRegion { CN, GLOBAL }
    
    /** 国内 GitHub 代理加速列表 */
    private val GITHUB_CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "https://ghproxy.cc/"
    )

    /**
     * 下载地址映射（ABI -> GitHub Release URL）
     * astral-sh/python-build-standalone 提供 musl 静态链接 CPython 构建
     * musl 构建无 glibc 依赖，可在 Android 的 Linux 内核上直接执行
     * 
     * 已验证 URL (HTTP 200):
     * - aarch64-unknown-linux-musl (arm64-v8a)
     * - x86_64-unknown-linux-musl  (x86_64)
     * armv7 仅有 gnueabihf 构建（动态链接），可能在部分设备上不可用
     */
    private fun getPythonUrl(abi: String): String {
        // ABI -> (arch-triple, variant)
        // 优先 musl 静态链接；armv7 回退到 gnueabihf
        val tripleMap = mapOf(
            "arm64-v8a"   to "aarch64-unknown-linux-musl",
            "x86_64"      to "x86_64-unknown-linux-musl",
            "armeabi-v7a" to "armv7-unknown-linux-gnueabihf",
            "x86"         to "x86_64-unknown-linux-musl"  // x86 极罕见，回退 x86_64
        )
        val triple = tripleMap[abi] ?: "aarch64-unknown-linux-musl"
        // URL 中 + 号需要保持原样，GitHub 会正确解析
        return "https://github.com/astral-sh/python-build-standalone/releases/download/$PYTHON_BUILD_TAG/cpython-${PYTHON_FULL_VERSION}+${PYTHON_BUILD_TAG}-${triple}-install_only_stripped.tar.gz"
    }
    
    /**
     * musl 动态链接器下载地址（来自 Alpine Linux 官方包）
     * 仅 aarch64 和 x86_64 支持（armv7 Python 使用 gnueabihf 构建，不兼容 musl）
     */
    private fun getMuslLinkerUrl(abi: String): String? {
        val archMap = mapOf(
            "arm64-v8a"   to "aarch64",
            "x86_64"      to "x86_64",
            "x86"         to "x86_64"
        )
        val arch = archMap[abi] ?: return null
        return "https://dl-cdn.alpinelinux.org/alpine/$MUSL_ALPINE_BRANCH/main/$arch/musl-$MUSL_VERSION.apk"
    }
    
    /**
     * 获取 musl 动态链接器文件名
     */
    fun getMuslLinkerName(abi: String): String {
        val archMap = mapOf(
            "arm64-v8a"   to "aarch64",
            "x86_64"      to "x86_64",
            "x86"         to "x86_64",
            "armeabi-v7a" to "armhf"
        )
        val arch = archMap[abi] ?: "aarch64"
        return "ld-musl-$arch.so.1"
    }
    
    data class MirrorConfig(
        val pythonUrls: List<String>,
        val muslLinkerUrl: String? = null
    )
    
    private fun getCnMirror(abi: String): MirrorConfig {
        val baseUrl = getPythonUrl(abi)
        return MirrorConfig(
            pythonUrls = GITHUB_CN_PROXIES.map { proxy -> "${proxy}${baseUrl}" } + baseUrl,
            muslLinkerUrl = getMuslLinkerUrl(abi)
        )
    }
    
    private fun getGlobalMirror(abi: String): MirrorConfig {
        return MirrorConfig(
            pythonUrls = listOf(getPythonUrl(abi)),
            muslLinkerUrl = getMuslLinkerUrl(abi)
        )
    }
    
    /** 每个 URL 最大重试次数 */
    private const val MAX_RETRY_PER_URL = 2
    private const val RETRY_DELAY_MS = 2000L
    
    // ==================== 下载状态 ====================
    
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Extracting(val fileName: String) : DownloadState()
        object Complete : DownloadState()
        data class Error(val message: String, val retryable: Boolean = true) : DownloadState()
        data class Paused(val progress: Float, val currentFile: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    }
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    private var _userMirrorRegion: MirrorRegion? = null
    
    // ==================== 公开 API ====================
    
    fun setMirrorRegion(region: MirrorRegion?) {
        _userMirrorRegion = region
    }
    
    fun getMirrorRegion(): MirrorRegion {
        _userMirrorRegion?.let { return it }
        val lang = Locale.getDefault().language
        return if (lang == "zh") MirrorRegion.CN else MirrorRegion.GLOBAL
    }
    
    fun getDepsDir(context: Context): File {
        return File(context.filesDir, "python_deps").also { it.mkdirs() }
    }
    
    fun getPythonDir(context: Context): File {
        return File(getDepsDir(context), "python").also { it.mkdirs() }
    }
    
    fun getProjectsDir(context: Context): File {
        return File(context.filesDir, "python_projects").also { it.mkdirs() }
    }
    
    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }
    
    /**
     * 检查 Python 运行时是否已下载就绪
     * 检查 python3.12（真实二进制）而非 python3（可能是符号链接）
     * 文件大小 > 1MB 确认不是空文件或损坏的符号链接
     */
    fun isPythonReady(context: Context): Boolean {
        val pythonBin312 = File(getPythonDir(context), "bin/python3.12")
        if (pythonBin312.exists() && pythonBin312.length() > 1024 * 1024) return true
        val pythonBin = File(getPythonDir(context), "bin/python3")
        if (pythonBin.exists() && pythonBin.length() > 1024 * 1024) return true
        // 也检查 nativeLibraryDir（导出 APK 场景）
        val nativePython = File(context.applicationInfo.nativeLibraryDir, "libpython3.so")
        return nativePython.exists() && nativePython.length() > 1024 * 1024
    }
    
    /**
     * 获取 Python 可执行文件路径
     * 优先使用 python3.12（真实二进制）而非 python3（可能是符号链接）
     */
    fun getPythonExecutablePath(context: Context): String {
        // 优先 nativeLibraryDir（SELinux 安全，导出 APK 场景）
        val nativePython = File(context.applicationInfo.nativeLibraryDir, "libpython3.so")
        if (nativePython.exists() && nativePython.length() > 1024 * 1024) {
            AppLogger.d(TAG, "使用 nativeLibraryDir Python: ${nativePython.absolutePath} (${nativePython.length() / 1024} KB)")
            return nativePython.absolutePath
        }
        // 回退到下载目录 - 优先 python3.12（真实二进制），避免 python3（符号链接在解压时可能损坏）
        val downloaded312 = File(getPythonDir(context), "bin/python3.12")
        if (downloaded312.exists() && downloaded312.length() > 1024 * 1024) {
            AppLogger.d(TAG, "使用下载目录 Python: ${downloaded312.absolutePath} (${downloaded312.length() / 1024} KB)")
            return downloaded312.absolutePath
        }
        val downloaded = File(getPythonDir(context), "bin/python3")
        AppLogger.d(TAG, "使用下载目录 Python (fallback): ${downloaded.absolutePath}")
        return downloaded.absolutePath
    }
    
    /**
     * 获取 Python 标准库路径（PYTHONHOME）
     */
    fun getPythonHome(context: Context): String {
        return getPythonDir(context).absolutePath
    }
    
    /**
     * 获取 musl 动态链接器路径
     * 在 Android 上执行 musl 链接的 Python 二进制需要通过 musl linker 间接执行:
     *   ld-musl-aarch64.so.1 --library-path <lib_dir> python3.12 script.py
     * @return linker 路径，如果不存在则返回 null
     */
    fun getMuslLinkerPath(context: Context): String? {
        // 优先 nativeLibraryDir（导出 APK 场景，SELinux 安全可执行）
        val nativeLinker = File(context.applicationInfo.nativeLibraryDir, "libmusl-linker.so")
        if (nativeLinker.exists()) {
            return nativeLinker.absolutePath
        }
        // 下载目录
        val abi = getDeviceAbi()
        val linkerName = getMuslLinkerName(abi)
        val downloadedLinker = File(getPythonDir(context), "lib/$linkerName")
        if (downloadedLinker.exists()) {
            return downloadedLinker.absolutePath
        }
        return null
    }
    
    /**
     * 获取 pip 可执行文件路径
     */
    fun getPipPath(context: Context): String {
        return File(getPythonDir(context), "bin/pip3").absolutePath
    }
    
    /**
     * 下载 Python 运行时
     */
    suspend fun downloadPythonRuntime(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Idle
            DependencyDownloadNotification.getInstance(context)
            DependencyDownloadEngine.reset()
            
            if (isPythonReady(context)) {
                _downloadState.value = DownloadState.Complete
                return@withContext true
            }
            
            val abi = getDeviceAbi()
            val mirror = when (getMirrorRegion()) {
                MirrorRegion.CN -> getCnMirror(abi)
                MirrorRegion.GLOBAL -> getGlobalMirror(abi)
            }
            
            val success = downloadPython(context, mirror, abi)
            if (!success) return@withContext false
            
            _downloadState.value = DownloadState.Complete
            AppLogger.i(TAG, "Python 运行时下载完成")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 Python 运行时失败", e)
            _downloadState.value = DownloadState.Error(e.message ?: "未知错误")
            false
        }
    }
    
    /**
     * 安装 pip 包到项目虚拟环境
     */
    suspend fun installRequirements(
        context: Context,
        projectDir: File,
        onOutput: ((String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val reqFile = File(projectDir, "requirements.txt")
        if (!reqFile.exists()) {
            AppLogger.i(TAG, "无 requirements.txt，跳过依赖安装")
            return@withContext true
        }
        
        // 如果 .pypackages 已存在且非空，跳过安装（依赖已预装或已打包进 APK）
        val sitePackages = File(projectDir, ".pypackages")
        val existingPackages = sitePackages.listFiles()
        if (sitePackages.exists() && existingPackages != null && existingPackages.isNotEmpty()) {
            AppLogger.i(TAG, ".pypackages 已存在 (${existingPackages.size} items)，跳过 pip install")
            onOutput?.invoke("依赖已就绪，跳过安装")
            return@withContext true
        }
        
        val pythonBin = getPythonExecutablePath(context)
        val pythonHome = getPythonHome(context)
        val muslLinker = getMuslLinkerPath(context)
        
        try {
            sitePackages.mkdirs()
            onOutput?.invoke("正在安装 Python 依赖...")
            
            // ★ 核心方案：创建 shell wrapper + Python bootstrap 脚本
            // 在 Android 上，pip 内部通过 subprocess 调用 sys.executable 时会失败，
            // 因为 Python 二进制的 ELF 解释器 /lib/ld-musl-aarch64.so.1 不存在。
            // 
            // 解法: 创建一个 shell wrapper 脚本，内容为:
            //   #!/system/bin/sh
            //   exec /path/to/musl-linker --library-path /path/to/lib /path/to/python "$@"
            // 
            // 然后设置 sys.executable = wrapper_path，这样 pip 内部所有子进程调用
            // 都会通过 wrapper → musl linker → python 的方式正确执行。
            
            val result = if (muslLinker != null) {
                runPipWithWrapper(context, projectDir, pythonBin, pythonHome, muslLinker,
                    sitePackages, reqFile, onOutput)
            } else {
                // 无 musl linker，直接调用 pip（简单场景）
                runPipDirect(context, projectDir, pythonBin, pythonHome,
                    sitePackages, reqFile, onOutput)
            }
            
            if (result) {
                AppLogger.i(TAG, "Python 依赖安装成功")
                onOutput?.invoke("依赖安装完成")
            } else {
                AppLogger.e(TAG, "Python 依赖安装失败")
                onOutput?.invoke("依赖安装失败")
            }
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "安装 Python 依赖异常", e)
            onOutput?.invoke("安装异常: ${e.message}")
            false
        }
    }
    
    /**
     * 通过 shell wrapper + Python bootstrap 脚本执行 pip install
     * 
     * 这是 Android 上唯一可靠的 pip install 方式：
     * 1. 创建 shell wrapper 脚本 → 封装 musl-linker + python 调用
     * 2. 设置 sys.executable = wrapper → pip 子进程通过 wrapper 执行
     * 3. 使用 pip._internal.cli.main 直接调用 → 避免 -m pip 的额外子进程
     */
    private fun runPipWithWrapper(
        context: Context,
        projectDir: File,
        pythonBin: String,
        pythonHome: String,
        muslLinker: String,
        sitePackages: File,
        reqFile: File,
        onOutput: ((String) -> Unit)?
    ): Boolean {
        val cacheDir = context.cacheDir
        
        // 1. 创建 shell wrapper 脚本
        val wrapperScript = File(cacheDir, "python_wrapper.sh")
        wrapperScript.writeText("""#!/system/bin/sh
exec "$muslLinker" --library-path "$pythonHome/lib" "$pythonBin" "${'$'}@"
""")
        wrapperScript.setExecutable(true, false)
        AppLogger.d(TAG, "创建 Python wrapper: ${wrapperScript.absolutePath}")
        
        // 2. 创建 Python bootstrap 脚本
        // 关键: 在 import pip 之前先修改 sys.executable，
        // 这样 pip 内部所有 subprocess.Popen([sys.executable, ...]) 调用
        // 都会使用 wrapper 脚本，通过 musl linker 正确执行 Python。
        val bootstrapScript = File(cacheDir, "pip_bootstrap.py")
        bootstrapScript.writeText("""
import sys
import os

# ★ 核心: 将 sys.executable 设为 shell wrapper
# pip 在内部调用子进程时使用 sys.executable，
# 原始值指向 libpython3.so (musl ELF)，直接 exec 会失败
# wrapper 脚本封装了 musl-linker 调用，确保子进程正确执行
wrapper_path = os.environ.get('_PYTHON_WRAPPER')
if wrapper_path and os.path.exists(wrapper_path):
    sys.executable = wrapper_path
    # 同时修改 _base_executable 确保 venv/pip 子组件也使用 wrapper
    if hasattr(sys, '_base_executable'):
        sys._base_executable = wrapper_path

# 额外安全: monkey-patch subprocess.Popen 和 os.execv
# 以防某些 pip 插件直接使用旧的 sys.executable 值
import subprocess
_OrigPopen = subprocess.Popen
_MUSL = os.environ.get('_WTA_MUSL_LINKER', '')
_LIB = os.environ.get('_WTA_MUSL_LIB_PATH', '')
_PYBIN = os.environ.get('_WTA_PYTHON_BIN', '')
_PYNAMES = ('python3', 'python3.12', 'libpython3.so')

def _is_py(p):
    return p == _PYBIN or os.path.basename(str(p)) in _PYNAMES

class _MPopen(_OrigPopen):
    def __init__(self, args, *a, **kw):
        if isinstance(args, (list, tuple)) and len(args) > 0 and _MUSL and _LIB:
            if _is_py(str(args[0])):
                args = [_MUSL, '--library-path', _LIB] + list(args)
        super().__init__(args, *a, **kw)

subprocess.Popen = _MPopen

_oexecv = os.execv
def _mexecv(p, a):
    if _MUSL and _LIB and _is_py(p):
        a = [_MUSL, '--library-path', _LIB] + list(a)
        p = _MUSL
    return _oexecv(p, a)
os.execv = _mexecv

if hasattr(os, 'execve'):
    _oexecve = os.execve
    def _mexecve(p, a, e):
        if _MUSL and _LIB and _is_py(p):
            a = [_MUSL, '--library-path', _LIB] + list(a)
            p = _MUSL
        return _oexecve(p, a, e)
    os.execve = _mexecve

# 运行 pip
from pip._internal.cli.main import main
sys.exit(main())
""".trimIndent())

        // 3. 构建 pip 参数 (--timeout 30 避免网络卡住, --retries 2 快速失败)
        val pipArgs = listOf(
            "install",
            "--target", sitePackages.absolutePath,
            "--no-cache-dir",
            "--disable-pip-version-check",
            "--no-compile",
            "--timeout", "30",
            "--retries", "2",
            "--only-binary", ":all:",
            "-r", reqFile.absolutePath
        )
        
        // 4. 通过 musl linker 执行 bootstrap 脚本
        val command = listOf(
            muslLinker, "--library-path", "$pythonHome/lib",
            pythonBin, bootstrapScript.absolutePath
        ) + pipArgs
        
        AppLogger.i(TAG, "安装 Python 依赖 (wrapper模式): ${command.joinToString(" ")}")
        
        val exitCode = executeCommand(command, projectDir, pythonBin, pythonHome, muslLinker,
            wrapperScript.absolutePath, context, onOutput)
        
        if (exitCode == 0) return true
        
        // 第一次失败 (--only-binary)，第二次不限制二进制
        AppLogger.w(TAG, "pip --only-binary 失败 (exitCode=$exitCode)，重试不限制...")
        onOutput?.invoke("正在重试依赖安装...")
        sitePackages.deleteRecursively()
        sitePackages.mkdirs()
        
        val retryArgs = listOf(
            "install",
            "--target", sitePackages.absolutePath,
            "--no-cache-dir",
            "--disable-pip-version-check",
            "--no-compile",
            "--timeout", "30",
            "--retries", "2",
            "-r", reqFile.absolutePath
        )
        
        val retryCommand = listOf(
            muslLinker, "--library-path", "$pythonHome/lib",
            pythonBin, bootstrapScript.absolutePath
        ) + retryArgs
        
        AppLogger.i(TAG, "安装 Python 依赖 (wrapper模式-重试): ${retryCommand.joinToString(" ")}")
        
        return executeCommand(retryCommand, projectDir, pythonBin, pythonHome, muslLinker,
            wrapperScript.absolutePath, context, onOutput) == 0
    }
    
    /**
     * 直接执行 pip install（无需 musl linker 的场景）
     */
    private fun runPipDirect(
        context: Context,
        projectDir: File,
        pythonBin: String,
        pythonHome: String,
        sitePackages: File,
        reqFile: File,
        onOutput: ((String) -> Unit)?
    ): Boolean {
        val command = listOf(
            pythonBin, "-m", "pip", "install",
            "--target", sitePackages.absolutePath,
            "--no-cache-dir",
            "--disable-pip-version-check",
            "--no-compile",
            "-r", reqFile.absolutePath
        )
        AppLogger.i(TAG, "安装 Python 依赖 (直接模式): ${command.joinToString(" ")}")
        return executeCommand(command, projectDir, pythonBin, pythonHome, null, null,
            context, onOutput) == 0
    }
    
    /**
     * 执行命令并捕获输出
     */
    private fun executeCommand(
        command: List<String>,
        workDir: File,
        pythonBin: String,
        pythonHome: String,
        muslLinker: String?,
        wrapperPath: String?,
        context: Context,
        onOutput: ((String) -> Unit)?
    ): Int {
        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(workDir)
        processBuilder.redirectErrorStream(true)
        
        val env = processBuilder.environment()
        env["PYTHONHOME"] = pythonHome
        env["PYTHONPATH"] = "$pythonHome/lib/python$PYTHON_VERSION"
        env["LD_LIBRARY_PATH"] = "$pythonHome/lib"
        env["HOME"] = context.filesDir.absolutePath
        env["TMPDIR"] = context.cacheDir.absolutePath
        env["PATH"] = "${File(pythonHome, "bin").absolutePath}:${env["PATH"] ?: "/usr/bin"}"
        env["PYTHONDONTWRITEBYTECODE"] = "1"
        if (muslLinker != null) {
            env["_WTA_MUSL_LINKER"] = muslLinker
            env["_WTA_MUSL_LIB_PATH"] = "$pythonHome/lib"
        }
        env["_WTA_PYTHON_BIN"] = pythonBin
        if (wrapperPath != null) {
            env["_PYTHON_WRAPPER"] = wrapperPath
        }
        
        AppLogger.i(TAG, "executeCommand: 启动进程...")
        val process = processBuilder.start()
        
        // 在后台线程读取输出（避免主线程阻塞）
        val outputLines = mutableListOf<String>()
        val readerThread = Thread {
            try {
                process.inputStream.bufferedReader().forEachLine { line ->
                    AppLogger.d(TAG, "[pip] $line")
                    onOutput?.invoke(line)
                    synchronized(outputLines) {
                        if (outputLines.size < 50) outputLines.add(line)
                    }
                }
            } catch (e: Exception) {
                AppLogger.d(TAG, "pip 输出流读取结束: ${e.message}")
            }
        }.apply { isDaemon = true; start() }
        
        // 等待进程完成，最多 120 秒
        val PIP_TIMEOUT_SECONDS = 120L
        val completed = process.waitFor(PIP_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        
        if (!completed) {
            // 超时：强制杀死进程
            AppLogger.e(TAG, "pip install 超时 (${PIP_TIMEOUT_SECONDS}秒)，强制终止进程")
            onOutput?.invoke("依赖安装超时 (${PIP_TIMEOUT_SECONDS}秒)")
            process.destroyForcibly()
            readerThread.interrupt()
            return -1
        }
        
        // 等待读取线程完成
        readerThread.join(3000)
        
        val exitCode = process.exitValue()
        if (exitCode != 0) {
            val lastOutput = synchronized(outputLines) { outputLines.takeLast(5).joinToString("\n") }
            AppLogger.e(TAG, "pip install 失败, exitCode=$exitCode, output=$lastOutput")
            onOutput?.invoke("依赖安装失败 (exitCode=$exitCode)")
        }
        return exitCode
    }
    
    fun clearCache(context: Context) {
        getDepsDir(context).deleteRecursively()
        AppLogger.i(TAG, "Python 依赖缓存已清理")
    }
    
    fun getCacheSize(context: Context): Long {
        return getDepsDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    // ==================== 内部方法 ====================
    
    private suspend fun downloadWithRetry(
        urls: List<String>,
        destFile: File,
        displayName: String,
        context: Context?
    ): Boolean {
        for ((urlIndex, url) in urls.withIndex()) {
            val sourceName = if (urls.size > 1) "$displayName [源${urlIndex + 1}/${urls.size}]" else displayName
            AppLogger.i(TAG, "尝试下载 $sourceName: $url")
            
            for (attempt in 1..MAX_RETRY_PER_URL) {
                val success = DependencyDownloadEngine.downloadFile(url, destFile, sourceName, context)
                if (success) return true
                
                if (attempt < MAX_RETRY_PER_URL) {
                    AppLogger.i(TAG, "$sourceName 下载失败, ${RETRY_DELAY_MS / 1000}s 后重试 ($attempt/$MAX_RETRY_PER_URL)")
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                    DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Idle
                }
            }
            
            if (urlIndex < urls.lastIndex) {
                val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
                tmpFile.delete()
                AppLogger.i(TAG, "$sourceName 失败，切换下一个源...")
                DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Idle
            }
        }
        return false
    }
    
    private suspend fun downloadPython(context: Context, mirror: MirrorConfig, abi: String): Boolean {
        val pythonUrls = mirror.pythonUrls
        val fileName = pythonUrls.first().substringAfterLast("/")
        val destDir = getPythonDir(context)
        val archiveFile = File(getDepsDir(context), fileName)
        
        AppLogger.i(TAG, "下载 Python 运行时 (共 ${pythonUrls.size} 个源)")
        
        val downloaded = downloadWithRetry(pythonUrls, archiveFile, "Python $PYTHON_FULL_VERSION ($abi)", context)
        syncEngineState()
        if (!downloaded) return false
        
        // 解压 tar.gz
        _downloadState.value = DownloadState.Extracting("Python")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("Python")
        try {
            // python-build-standalone 的 install_only tar.gz 内部有 python/ 前缀
            // 例如 python/bin/python3.12, python/lib/python3.12/ 等
            // 注意: python3 是指向 python3.12 的符号链接，真实二进制是 python3.12
            extractTarGz(archiveFile, destDir, stripPrefix = "python/")
            
            // 检查真实二进制 python3.12（不是 python3 符号链接）
            val pythonBin312 = File(destDir, "bin/python3.12")
            val pythonBin = File(destDir, "bin/python3")
            if (pythonBin312.exists() && pythonBin312.length() > 1024 * 1024) {
                pythonBin312.setExecutable(true, false)
                pythonBin312.setReadable(true, true)
                AppLogger.i(TAG, "Python 运行时已就绪: ${pythonBin312.absolutePath} (${pythonBin312.length() / 1024} KB)")
                // 如果 python3 是空文件/损坏的符号链接，用真实二进制覆盖它
                if (!pythonBin.exists() || pythonBin.length() < 1024 * 1024) {
                    pythonBin312.copyTo(pythonBin, overwrite = true)
                    pythonBin.setExecutable(true, false)
                    AppLogger.i(TAG, "复制 python3.12 -> python3 (替代损坏的符号链接)")
                }
            } else if (pythonBin.exists() && pythonBin.length() > 1024 * 1024) {
                pythonBin.setExecutable(true, false)
                pythonBin.setReadable(true, true)
                AppLogger.i(TAG, "Python 运行时已就绪: ${pythonBin.absolutePath}")
            } else {
                // 回退：搜索任何大于 1MB 的 Python 二进制
                AppLogger.w(TAG, "未找到有效的 python3.12 或 python3，搜索其他二进制")
                val found = destDir.walkTopDown()
                    .filter { it.name.startsWith("python3") && it.isFile && it.length() > 1024 * 1024 }
                    .firstOrNull()
                
                if (found != null) {
                    val binDir = File(destDir, "bin")
                    binDir.mkdirs()
                    val target = File(binDir, "python3.12")
                    found.copyTo(target, overwrite = true)
                    target.setExecutable(true, false)
                    target.copyTo(File(binDir, "python3"), overwrite = true)
                    File(binDir, "python3").setExecutable(true, false)
                    AppLogger.i(TAG, "Python 二进制已移动到: ${target.absolutePath}")
                } else {
                    AppLogger.e(TAG, "解压后未找到有效的 Python 二进制 (>1MB)")
                    _downloadState.value = DownloadState.Error("解压后未找到 Python 二进制")
                    return false
                }
            }
            
            // 设置 bin 目录下所有文件的执行权限
            File(destDir, "bin").listFiles()?.forEach { it.setExecutable(true, false) }
            
            // 设置 lib 目录下 .so 文件的执行权限
            File(destDir, "lib").listFiles()?.filter { it.name.endsWith(".so") || it.name.contains(".so.") }?.forEach {
                it.setExecutable(true, false)
            }
            
            // 清理压缩包
            archiveFile.delete()
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 Python 失败", e)
            _downloadState.value = DownloadState.Error("解压 Python 失败: ${e.message}")
            return false
        }
        
        // 下载 musl 动态链接器（Android 上执行 musl 链接的 Python 必需）
        if (mirror.muslLinkerUrl != null) {
            val muslSuccess = downloadMuslLinker(context, mirror.muslLinkerUrl, abi)
            if (!muslSuccess) {
                AppLogger.w(TAG, "musl 动态链接器下载失败，Python 可能无法在 Android 上执行")
            }
        } else {
            AppLogger.w(TAG, "当前 ABI ($abi) 无可用的 musl linker")
        }
        
        return true
    }
    
    /**
     * 下载 musl 动态链接器（从 Alpine Linux 官方包）
     * Alpine APK 是 tar.gz 格式，提取 lib/ld-musl-{arch}.so.1
     */
    private suspend fun downloadMuslLinker(context: Context, url: String, abi: String): Boolean {
        val linkerName = getMuslLinkerName(abi)
        val destDir = getPythonDir(context)
        val linkerFile = File(destDir, "lib/$linkerName")
        
        if (linkerFile.exists() && linkerFile.length() > 100 * 1024) {
            AppLogger.i(TAG, "musl linker 已存在: ${linkerFile.absolutePath} (${linkerFile.length() / 1024} KB)")
            return true
        }
        
        try {
            AppLogger.i(TAG, "下载 musl linker: $url")
            val apkFile = File(getDepsDir(context), "musl-${abi}.apk")
            val downloaded = downloadWithRetry(listOf(url), apkFile, "musl linker ($abi)", context)
            if (!downloaded) return false
            
            // Alpine APK 是 tar.gz，提取 lib/ld-musl-*.so.1
            val gzipStream = java.util.zip.GZIPInputStream(apkFile.inputStream().buffered())
            val tarStream = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzipStream)
            var found = false
            tarStream.use { tar ->
                var entry = tar.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && !entry.isSymbolicLink && entry.name.endsWith(linkerName)) {
                        linkerFile.parentFile?.mkdirs()
                        FileOutputStream(linkerFile).use { fos ->
                            tar.copyTo(fos)
                        }
                        linkerFile.setExecutable(true, false)
                        found = true
                        AppLogger.i(TAG, "musl linker 已提取: ${linkerFile.absolutePath} (${linkerFile.length() / 1024} KB)")
                        break
                    }
                    entry = tar.nextEntry
                }
            }
            apkFile.delete()
            
            if (!found) {
                AppLogger.e(TAG, "Alpine APK 中未找到 $linkerName")
            }
            return found
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 musl linker 失败", e)
            return false
        }
    }
    
    /**
     * 解压 tar.gz 文件
     * @param stripPrefix 如果非空，从每个条目路径中剥离此前缀
     *                    例如 stripPrefix="python/" 会将 "python/bin/python3" 解压为 "bin/python3"
     * 符号链接处理: tar 中的符号链接延迟处理，先提取所有真实文件，
     *                  然后通过复制目标文件内容来"解析"符号链接
     */
    private fun extractTarGz(archiveFile: File, destDir: File, stripPrefix: String? = null) {
        AppLogger.i(TAG, "解压 ${archiveFile.name} 到 ${destDir.absolutePath}" +
            (if (stripPrefix != null) " (剥离前缀: $stripPrefix)" else ""))
        
        val gzipStream = java.util.zip.GZIPInputStream(archiveFile.inputStream().buffered())
        val tarStream = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzipStream)
        
        // 收集符号链接，延迟到所有真实文件提取后再处理
        val deferredSymlinks = mutableListOf<Pair<String, String>>() // (entryName, linkTarget)
        var fileCount = 0
        
        tarStream.use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                var entryName = entry.name
                
                // 剥离前缀
                if (stripPrefix != null && entryName.startsWith(stripPrefix)) {
                    entryName = entryName.removePrefix(stripPrefix)
                    if (entryName.isEmpty()) {
                        entry = tar.nextEntry
                        continue
                    }
                }
                
                val outFile = File(destDir, entryName)
                
                // 安全检查：防止路径遍历
                if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                    AppLogger.w(TAG, "跳过可疑路径: ${entry.name}")
                    entry = tar.nextEntry
                    continue
                }
                
                if (entry.isSymbolicLink) {
                    // 符号链接延迟处理
                    var linkTarget = entry.linkName
                    if (stripPrefix != null && linkTarget.startsWith(stripPrefix)) {
                        linkTarget = linkTarget.removePrefix(stripPrefix)
                    }
                    deferredSymlinks.add(entryName to linkTarget)
                } else if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        tar.copyTo(fos)
                    }
                    // 保留执行权限
                    if (entry.mode and 0b001_000_000 != 0) {
                        outFile.setExecutable(true, false)
                    }
                    fileCount++
                }
                entry = tar.nextEntry
            }
        }
        
        // 处理符号链接：通过复制目标文件内容来"解析"符号链接
        var resolvedLinks = 0
        for ((linkName, targetName) in deferredSymlinks) {
            val linkFile = File(destDir, linkName)
            val linkParent = linkFile.parentFile ?: destDir
            val targetFile = File(linkParent, targetName)
            
            if (targetFile.exists() && targetFile.isFile) {
                try {
                    linkFile.parentFile?.mkdirs()
                    targetFile.copyTo(linkFile, overwrite = true)
                    if (targetFile.canExecute()) {
                        linkFile.setExecutable(true, false)
                    }
                    resolvedLinks++
                } catch (e: Exception) {
                    AppLogger.w(TAG, "解析符号链接失败: $linkName -> $targetName: ${e.message}")
                }
            } else {
                AppLogger.d(TAG, "跳过符号链接（目标不存在）: $linkName -> $targetName")
            }
        }
        
        AppLogger.i(TAG, "解压完成: $fileCount 个文件, $resolvedLinks/${deferredSymlinks.size} 个符号链接已解析")
    }
    
    private fun syncEngineState() {
        when (val es = DependencyDownloadEngine.state.value) {
            is DependencyDownloadEngine.State.Downloading -> {
                _downloadState.value = DownloadState.Downloading(
                    progress = es.progress,
                    currentFile = es.displayName,
                    bytesDownloaded = es.bytesDownloaded,
                    totalBytes = es.totalBytes
                )
            }
            is DependencyDownloadEngine.State.Paused -> {
                _downloadState.value = DownloadState.Paused(
                    progress = es.progress,
                    currentFile = es.displayName,
                    bytesDownloaded = es.bytesDownloaded,
                    totalBytes = es.totalBytes
                )
            }
            is DependencyDownloadEngine.State.Error -> {
                _downloadState.value = DownloadState.Error(es.message)
            }
            else -> {}
        }
    }
}
