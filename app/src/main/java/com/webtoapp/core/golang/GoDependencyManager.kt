package com.webtoapp.core.golang

import android.content.Context
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import java.io.File

object GoDependencyManager {

    private const val TAG = "GoDependencyManager"

    private const val ELF_CLASS_32 = 1
    private const val ELF_CLASS_64 = 2
    private const val ELF_MACHINE_ARM = 0x28
    private const val ELF_MACHINE_AARCH64 = 0xB7
    private const val ELF_MACHINE_386 = 0x03
    private const val ELF_MACHINE_X86_64 = 0x3E

    data class ElfInfo(
        val isValid: Boolean,
        val is64Bit: Boolean = false,
        val machine: Int = 0,
        val abiName: String = "unknown"
    )

    fun getBinDir(context: Context): File {
        return File(context.filesDir, "go_bins").also { it.mkdirs() }
    }

    private fun getLegacyDepsDir(context: Context): File {
        return File(context.filesDir, "go_deps")
    }

    private fun getGoExecLoader(context: Context): File {
        return File(context.applicationInfo.nativeLibraryDir, "libgo_exec_loader.so")
    }

    fun getGoExecLoaderPath(context: Context): String {
        return getGoExecLoader(context).absolutePath
    }

    fun isGoExecLoaderReady(context: Context): Boolean {
        val loader = getGoExecLoader(context)
        return loader.exists() && loader.canExecute()
    }

    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS?.firstOrNull() ?: "arm64-v8a"
    }

    fun parseElf(file: File): ElfInfo {
        if (!file.exists() || file.length() < 20) {
            return ElfInfo(isValid = false)
        }

        return try {
            val header = file.inputStream().use { stream ->
                val bytes = ByteArray(20)
                val read = stream.read(bytes)
                if (read < 20) return ElfInfo(isValid = false)
                bytes
            }

            if (header[0] != 0x7f.toByte() ||
                header[1] != 'E'.code.toByte() ||
                header[2] != 'L'.code.toByte() ||
                header[3] != 'F'.code.toByte()
            ) {
                return ElfInfo(isValid = false)
            }

            val elfClass = header[4].toInt() and 0xFF
            val machine = (header[18].toInt() and 0xFF) or ((header[19].toInt() and 0xFF) shl 8)
            val abiName = when (machine) {
                ELF_MACHINE_ARM -> "armeabi-v7a"
                ELF_MACHINE_AARCH64 -> "arm64-v8a"
                ELF_MACHINE_386 -> "x86"
                ELF_MACHINE_X86_64 -> "x86_64"
                else -> "unknown($machine)"
            }

            ElfInfo(
                isValid = true,
                is64Bit = elfClass == ELF_CLASS_64,
                machine = machine,
                abiName = abiName
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "ELF 解析失败: ${e.message}")
            ElfInfo(isValid = false)
        }
    }

    fun isCompatible(elfInfo: ElfInfo): Boolean {
        if (!elfInfo.isValid) return false
        val deviceAbi = getDeviceAbi()

        if (elfInfo.abiName == deviceAbi) return true
        if (deviceAbi == "arm64-v8a" && elfInfo.abiName == "armeabi-v7a") return true
        if (deviceAbi == "x86_64" && elfInfo.abiName == "x86") return true
        return false
    }

    internal fun buildBinaryCommand(
        context: Context,
        executablePath: String,
        arguments: List<String>
    ): List<String> {
        return buildList {
            add(getGoExecLoader(context).absolutePath)
            add(executablePath)
            addAll(arguments)
        }
    }

    internal fun configureGoBinaryEnvironment(
        context: Context,
        processEnv: MutableMap<String, String>,
        additionalPathEntries: List<String> = emptyList(),
        libraryPath: String? = null
    ) {
        processEnv["HOME"] = context.filesDir.absolutePath
        processEnv["TMPDIR"] = context.cacheDir.absolutePath

        if (libraryPath.isNullOrBlank()) {
            processEnv.remove("LD_LIBRARY_PATH")
        } else {
            processEnv["LD_LIBRARY_PATH"] = buildList {
                add(libraryPath)
                add(processEnv["LD_LIBRARY_PATH"].orEmpty())
            }.filter { it.isNotBlank() }.joinToString(File.pathSeparator)
        }

        processEnv["PATH"] = buildList {
            addAll(additionalPathEntries)
            add(processEnv["PATH"].orEmpty())
        }.filter { it.isNotBlank() }.joinToString(File.pathSeparator)
    }

    fun prepareBinary(context: Context, projectDir: File, binaryName: String): String? {
        val sourceBinary = findBinaryInProject(projectDir, binaryName) ?: return null

        val elfInfo = parseElf(sourceBinary)
        if (!elfInfo.isValid) {
            AppLogger.e(TAG, "$binaryName 不是有效的 ELF 二进制")
            return null
        }
        if (!isCompatible(elfInfo)) {
            AppLogger.e(TAG, "$binaryName ABI 不兼容: ${elfInfo.abiName}, 设备: ${getDeviceAbi()}")
            return null
        }

        val binDir = getBinDir(context)
        val destBinary = File(binDir, binaryName)

        return try {
            sourceBinary.copyTo(destBinary, overwrite = true)
            destBinary.setExecutable(true, false)
            destBinary.setReadable(true, true)
            AppLogger.i(TAG, "Go 二进制已准备: ${destBinary.absolutePath} (${elfInfo.abiName}, ${destBinary.length()} bytes)")
            destBinary.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "准备 Go 二进制失败", e)
            null
        }
    }

    fun findBinaryPath(projectDir: File, binaryName: String): String? {
        return findBinaryInProject(projectDir, binaryName)?.absolutePath
    }

    fun detectAnyCompatibleBinary(projectDir: File): File? {
        val preferredRoots = listOf(
            projectDir,
            File(projectDir, "bin"),
            File(projectDir, "build"),
            File(projectDir, "build/${normalizeTargetAbi(getDeviceAbi())}")
        )

        preferredRoots.asSequence()
            .filter { it.exists() }
            .flatMap { root ->
                if (root.isDirectory) root.walkTopDown().asSequence() else sequenceOf(root)
            }
            .filter { it.isFile && it.length() > 1000 }
            .forEach { file ->
                if (isCompatible(parseElf(file))) {
                    return file
                }
            }
        return null
    }

    fun clearCache(context: Context) {
        getBinDir(context).deleteRecursively()
        getLegacyDepsDir(context).deleteRecursively()
        AppLogger.i(TAG, "Go 二进制缓存和历史工具链缓存已清理")
    }

    fun getCacheSize(context: Context): Long {
        return sequenceOf(getBinDir(context), getLegacyDepsDir(context))
            .filter { it.exists() }
            .sumOf { dir -> dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
    }

    private fun findBinaryInProject(projectDir: File, binaryName: String): File? {
        val normalizedDeviceAbi = normalizeTargetAbi(getDeviceAbi())
        val directCandidates = listOf(
            File(projectDir, binaryName),
            File(projectDir, "bin/$binaryName"),
            File(projectDir, "build/$binaryName"),
            File(projectDir, "build/$normalizedDeviceAbi/$binaryName")
        )
        directCandidates.firstOrNull { it.exists() && it.isFile }?.let { return it }

        return projectDir.walkTopDown()
            .filter { it.isFile && it.name == binaryName }
            .firstOrNull()
    }

    private fun normalizeTargetAbi(targetAbi: String): String {
        return when (targetAbi) {
            "arm64", "arm64-v8a", "aarch64" -> "arm64-v8a"
            "arm", "armeabi-v7a", "armv7a" -> "armeabi-v7a"
            "x86_64", "amd64" -> "x86_64"
            "x86", "386" -> "x86"
            else -> targetAbi
        }
    }
}
