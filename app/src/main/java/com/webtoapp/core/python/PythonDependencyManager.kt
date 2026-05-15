package com.webtoapp.core.python

import android.content.Context
import android.os.Build
import com.webtoapp.core.download.DependencyDownloadEngine
import com.webtoapp.core.download.DependencyDownloadNotification
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.destroyForciblyCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale













object PythonDependencyManager {

    private const val TAG = "PythonDependencyManager"
    private val ANDROID_UNSUPPORTED_REQUIREMENTS = setOf(
        "uvloop",
        "httptools",
        "watchfiles"
    )
    private val UVICORN_EXTRAS_REGEX = Regex(
        """^(\s*)uvicorn\s*\[[^]]+](\s*.*)$""",
        RegexOption.IGNORE_CASE
    )










    const val PYTHON_VERSION = "3.12"
    const val PYTHON_FULL_VERSION = "3.12.12"
    private const val PYTHON_BUILD_TAG = "20260211"


    private const val MUSL_VERSION = "1.2.5-r11"
    private const val MUSL_ALPINE_BRANCH = "v3.21"



    enum class MirrorRegion { CN, GLOBAL }


    // CN mirror proxies verified reachable on 2026-05-05. ghproxy.cc was removed
    // because its DNS no longer resolves. If you add more proxies, preserve the
    // trailing slash: `${proxy}${original_github_url}`.
    private val GITHUB_CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/"
    )











    private fun getPythonUrl(abi: String): String {


        val tripleMap = mapOf(
            "arm64-v8a"   to "aarch64-unknown-linux-musl",
            "x86_64"      to "x86_64-unknown-linux-musl",
            "armeabi-v7a" to "armv7-unknown-linux-gnueabihf",
            "x86"         to "x86_64-unknown-linux-musl"
        )
        val triple = tripleMap[abi] ?: "aarch64-unknown-linux-musl"

        return "https://github.com/astral-sh/python-build-standalone/releases/download/$PYTHON_BUILD_TAG/cpython-${PYTHON_FULL_VERSION}+${PYTHON_BUILD_TAG}-${triple}-install_only_stripped.tar.gz"
    }





    private fun getMuslLinkerUrl(abi: String): String? {
        val archMap = mapOf(
            "arm64-v8a"   to "aarch64",
            "x86_64"      to "x86_64",
            "x86"         to "x86_64"
        )
        val arch = archMap[abi] ?: return null
        return "https://dl-cdn.alpinelinux.org/alpine/$MUSL_ALPINE_BRANCH/main/$arch/musl-$MUSL_VERSION.apk"
    }




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
        val orderedProxies = com.webtoapp.core.network.CnMirrorProbe.getOrderedProxies(GITHUB_CN_PROXIES)
        return MirrorConfig(
            pythonUrls = orderedProxies.map { proxy -> "${proxy}${baseUrl}" } + baseUrl,
            muslLinkerUrl = getMuslLinkerUrl(abi)
        )
    }

    private fun getGlobalMirror(abi: String): MirrorConfig {
        return MirrorConfig(
            pythonUrls = listOf(getPythonUrl(abi)),
            muslLinkerUrl = getMuslLinkerUrl(abi)
        )
    }


    private const val MAX_RETRY_PER_URL = 2
    private const val RETRY_DELAY_MS = 2000L



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

    private fun resolvePythonBinary(context: Context): File? {
        val nativePython = File(context.applicationInfo.nativeLibraryDir, "libpython3.so")
        if (nativePython.exists() && nativePython.length() > 1024 * 1024) {
            return nativePython
        }

        val downloaded312 = File(getPythonDir(context), "bin/python3.12")
        if (downloaded312.exists() && downloaded312.length() > 1024 * 1024) {
            return downloaded312
        }

        val downloaded = File(getPythonDir(context), "bin/python3")
        if (downloaded.exists() && downloaded.length() > 1024 * 1024) {
            return downloaded
        }

        return null
    }

    private fun resolveMuslLinker(context: Context): File? {
        val nativeLinker = File(context.applicationInfo.nativeLibraryDir, "libmusl-linker.so")
        if (nativeLinker.exists() && nativeLinker.length() > 1024) {
            return nativeLinker
        }

        val abi = getDeviceAbi()
        val linkerName = getMuslLinkerName(abi)
        val downloadedLinker = File(getPythonDir(context), "lib/$linkerName")
        if (downloadedLinker.exists() && downloadedLinker.length() > 1024) {
            return downloadedLinker
        }

        return null
    }

    private fun resolveBuilderMuslLinker(context: Context): File? {
        val nativeLinker = File(context.applicationInfo.nativeLibraryDir, "libmusl-linker.so")
        if (nativeLinker.exists() && nativeLinker.length() > 1024 && nativeLinker.canExecute()) {
            return nativeLinker
        }
        return null
    }






    fun isPythonReady(context: Context): Boolean {
        return resolvePythonBinary(context) != null && resolveMuslLinker(context) != null
    }





    fun getPythonExecutablePath(context: Context): String {
        resolvePythonBinary(context)?.let { pythonBinary ->
            AppLogger.d(TAG, "使用 Python: ${pythonBinary.absolutePath} (${pythonBinary.length() / 1024} KB)")
            return pythonBinary.absolutePath
        }

        val fallback = File(getPythonDir(context), "bin/python3")
        AppLogger.d(TAG, "使用下载目录 Python (fallback): ${fallback.absolutePath}")
        return fallback.absolutePath
    }




    fun getPythonHome(context: Context): String {
        return getPythonDir(context).absolutePath
    }







    fun getMuslLinkerPath(context: Context): String? {
        return resolveMuslLinker(context)?.absolutePath
    }

    fun getBuilderMuslLinkerPath(context: Context): String? {
        return resolveBuilderMuslLinker(context)?.absolutePath
    }




    fun getPipPath(context: Context): String {
        return File(getPythonDir(context), "bin/pip3").absolutePath
    }

    fun hasInstalledPackages(sitePackagesDir: File): Boolean {
        if (!sitePackagesDir.exists() || !sitePackagesDir.isDirectory) return false
        return sitePackagesDir.walkTopDown()
            .drop(1)
            .any { it.isFile }
    }




    suspend fun downloadPythonRuntime(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Idle
            DependencyDownloadNotification.getInstance(context)
            DependencyDownloadEngine.reset()

            val pythonReady = resolvePythonBinary(context) != null
            val muslReady = resolveMuslLinker(context) != null
            if (pythonReady && muslReady) {
                markComplete()
                return@withContext true
            }

            val abi = getDeviceAbi()
            val mirror = when (getMirrorRegion()) {
                MirrorRegion.CN -> getCnMirror(abi)
                MirrorRegion.GLOBAL -> getGlobalMirror(abi)
            }

            val success = when {
                !pythonReady -> downloadPython(context, mirror, abi)
                !muslReady -> {
                    val muslUrl = mirror.muslLinkerUrl
                    if (muslUrl == null) {
                        markError("当前 ABI ($abi) 暂无可用的 musl linker")
                        false
                    } else {
                        _downloadState.value = DownloadState.Extracting("musl linker")
                        downloadMuslLinker(context, muslUrl, abi)
                    }
                }
                else -> true
            }
            if (!success) return@withContext false

            if (!isPythonReady(context)) {
                markError("Python 运行时下载不完整：缺少 Python 二进制或 musl linker")
                return@withContext false
            }

            markComplete()
            AppLogger.i(TAG, "Python 运行时下载完成")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 Python 运行时失败", e)
            markError(e.message ?: "未知错误")
            false
        }
    }




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


        val sitePackages = File(projectDir, ".pypackages")
        if (hasInstalledPackages(sitePackages)) {
            val existingPackages = sitePackages.listFiles()?.size ?: 0
            AppLogger.i(TAG, ".pypackages 已存在 (${existingPackages} items)，跳过 pip install")
            onOutput?.invoke("依赖已就绪，跳过安装")
            return@withContext true
        }

        val pythonBin = getPythonExecutablePath(context)
        val pythonHome = getPythonHome(context)
        val muslLinker = getBuilderMuslLinkerPath(context)
        val installReqFile = prepareRequirementsFileForInstall(context, projectDir, reqFile, onOutput)

        try {
            sitePackages.mkdirs()
            onOutput?.invoke("正在安装 Python 依赖...")

            if (muslLinker == null) {
                AppLogger.w(
                    TAG,
                    "构建阶段缺少可执行 musl linker，无法预安装 Python 依赖: nativeLibraryDir=${context.applicationInfo.nativeLibraryDir}"
                )
                onOutput?.invoke("本机构建器缺少可执行 musl linker，无法预安装依赖")
                return@withContext false
            }












            val result = runPipWithWrapper(context, projectDir, pythonBin, pythonHome, muslLinker,
                sitePackages, installReqFile, onOutput)

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
        } finally {
            if (installReqFile != reqFile) {
                installReqFile.delete()
            }
        }
    }

    private fun prepareRequirementsFileForInstall(
        context: Context,
        projectDir: File,
        reqFile: File,
        onOutput: ((String) -> Unit)?
    ): File {
        val original = reqFile.readText()
        val sanitized = sanitizeRequirementsForAndroid(original)
        if (sanitized == original) {
            return reqFile
        }

        val tempFile = File.createTempFile("w2a_requirements_android_", ".txt", context.cacheDir)
        tempFile.writeText(sanitized)
        AppLogger.w(
            TAG,
            "检测到 Android 不兼容的 Python 依赖，已改用清洗后的 requirements: ${tempFile.absolutePath} (project=${projectDir.absolutePath})"
        )
        onOutput?.invoke("已自动调整 Android 不兼容依赖")
        return tempFile
    }

    internal fun sanitizeRequirementsForAndroid(requirementsText: String): String {
        val sanitized = requirementsText
            .lineSequence()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
                    return@mapNotNull line
                }

                val packageName = trimmed.takeWhile { char ->
                    char.isLetterOrDigit() || char == '-' || char == '_' || char == '.'
                }
                if (packageName.lowercase(Locale.US) in ANDROID_UNSUPPORTED_REQUIREMENTS) {
                    return@mapNotNull null
                }
                if (packageName.equals("uvicorn", ignoreCase = true) && trimmed.getOrNull(packageName.length) == '[') {
                    return@mapNotNull line.replaceFirst(UVICORN_EXTRAS_REGEX, "$1uvicorn$2")
                }
                line
            }
            .joinToString("\n")

        return if (requirementsText.endsWith("\n") && !sanitized.endsWith("\n")) {
            "$sanitized\n"
        } else {
            sanitized
        }
    }









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


        val wrapperScript = File(cacheDir, "python_wrapper.sh")
        wrapperScript.writeText("""#!/system/bin/sh
exec "$muslLinker" --library-path "$pythonHome/lib" "$pythonBin" "${'$'}@"
""")
        wrapperScript.setExecutable(true, false)
        AppLogger.d(TAG, "创建 Python wrapper: ${wrapperScript.absolutePath}")





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


        val command = listOf(
            muslLinker, "--library-path", "$pythonHome/lib",
            pythonBin, bootstrapScript.absolutePath
        ) + pipArgs

        AppLogger.i(TAG, "安装 Python 依赖 (wrapper模式): ${command.joinToString(" ")}")

        val exitCode = executeCommand(command, projectDir, pythonBin, pythonHome, muslLinker,
            wrapperScript.absolutePath, context, onOutput)

        if (exitCode == 0) return true


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


        val PIP_TIMEOUT_SECONDS = 120L
        val deadline = System.currentTimeMillis() + PIP_TIMEOUT_SECONDS * 1000L
        var completed = false
        while (System.currentTimeMillis() < deadline && !completed) {
            completed = try {
                process.exitValue()
                true
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(200)
                false
            }
        }

        if (!completed) {

            AppLogger.e(TAG, "pip install 超时 (${PIP_TIMEOUT_SECONDS}秒)，强制终止进程")
            onOutput?.invoke("依赖安装超时 (${PIP_TIMEOUT_SECONDS}秒)")
            process.destroyForciblyCompat()
            readerThread.interrupt()
            return -1
        }


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


        _downloadState.value = DownloadState.Extracting("Python")
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Extracting("Python")
        try {



            extractTarGz(archiveFile, destDir, stripPrefix = "python/")


            val pythonBin312 = File(destDir, "bin/python3.12")
            val pythonBin = File(destDir, "bin/python3")
            if (pythonBin312.exists() && pythonBin312.length() > 1024 * 1024) {
                pythonBin312.setExecutable(true, false)
                pythonBin312.setReadable(true, true)
                AppLogger.i(TAG, "Python 运行时已就绪: ${pythonBin312.absolutePath} (${pythonBin312.length() / 1024} KB)")

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
                    markError("解压后未找到 Python 二进制")
                    return false
                }
            }


            File(destDir, "bin").listFiles()?.forEach { it.setExecutable(true, false) }


            File(destDir, "lib").listFiles()?.filter { it.name.endsWith(".so") || it.name.contains(".so.") }?.forEach {
                it.setExecutable(true, false)
            }


            archiveFile.delete()
        } catch (e: Exception) {
            AppLogger.e(TAG, "解压 Python 失败", e)
            markError("解压 Python 失败: ${e.message}")
            return false
        }


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
                markError("Alpine APK 中未找到 $linkerName")
            }
            return found
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 musl linker 失败", e)
            markError("下载 musl linker 失败: ${e.message}")
            return false
        }
    }








    private fun extractTarGz(archiveFile: File, destDir: File, stripPrefix: String? = null) {
        AppLogger.i(TAG, "解压 ${archiveFile.name} 到 ${destDir.absolutePath}" +
            (if (stripPrefix != null) " (剥离前缀: $stripPrefix)" else ""))

        val gzipStream = java.util.zip.GZIPInputStream(archiveFile.inputStream().buffered())
        val tarStream = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzipStream)


        val deferredSymlinks = mutableListOf<Pair<String, String>>()
        var fileCount = 0

        tarStream.use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                var entryName = entry.name


                if (stripPrefix != null && entryName.startsWith(stripPrefix)) {
                    entryName = entryName.removePrefix(stripPrefix)
                    if (entryName.isEmpty()) {
                        entry = tar.nextEntry
                        continue
                    }
                }

                val outFile = File(destDir, entryName)


                if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                    AppLogger.w(TAG, "跳过可疑路径: ${entry.name}")
                    entry = tar.nextEntry
                    continue
                }

                if (entry.isSymbolicLink) {

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

                    if (entry.mode and 0b001_000_000 != 0) {
                        outFile.setExecutable(true, false)
                    }
                    fileCount++
                }
                entry = tar.nextEntry
            }
        }


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

    private fun markComplete() {
        _downloadState.value = DownloadState.Complete
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Complete
    }

    private fun markError(message: String) {
        _downloadState.value = DownloadState.Error(message)
        DependencyDownloadEngine._state.value = DependencyDownloadEngine.State.Error(message)
    }
}
