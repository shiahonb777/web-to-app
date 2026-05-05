package com.webtoapp.core.golang

import android.content.Context
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import java.io.File













object GoDependencyManager {

    private const val TAG = "GoDependencyManager"






    fun getProjectsDir(context: Context): File {
        return File(context.filesDir, "go_projects").also { it.mkdirs() }
    }




    fun getBinDir(context: Context): File {
        return File(context.filesDir, "go_bins").also { it.mkdirs() }
    }




    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }




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




    fun parseElf(file: File): ElfInfo {
        if (!file.exists() || file.length() < 20) {
            return ElfInfo(isValid = false)
        }

        try {
            val header = file.inputStream().use { stream ->
                val bytes = ByteArray(20)
                val read = stream.read(bytes)
                if (read < 20) return ElfInfo(isValid = false)
                bytes
            }


            if (header[0] != 0x7f.toByte() || header[1] != 'E'.code.toByte() ||
                header[2] != 'L'.code.toByte() || header[3] != 'F'.code.toByte()) {
                return ElfInfo(isValid = false)
            }

            val elfClass = header[4].toInt() and 0xFF
            val is64Bit = elfClass == ELF_CLASS_64


            val machine = (header[18].toInt() and 0xFF) or ((header[19].toInt() and 0xFF) shl 8)

            val abiName = when (machine) {
                ELF_MACHINE_ARM -> "armeabi-v7a"
                ELF_MACHINE_AARCH64 -> "arm64-v8a"
                ELF_MACHINE_386 -> "x86"
                ELF_MACHINE_X86_64 -> "x86_64"
                else -> "unknown($machine)"
            }

            return ElfInfo(isValid = true, is64Bit = is64Bit, machine = machine, abiName = abiName)
        } catch (e: Exception) {
            AppLogger.w(TAG, "ELF 解析失败: ${e.message}")
            return ElfInfo(isValid = false)
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

        try {
            sourceBinary.copyTo(destBinary, overwrite = true)
            destBinary.setExecutable(true, false)
            destBinary.setReadable(true, true)
            AppLogger.i(TAG, "Go 二进制已准备: ${destBinary.absolutePath} (${elfInfo.abiName}, ${destBinary.length()} bytes)")
            return destBinary.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "准备 Go 二进制失败", e)
            return null
        }
    }




    private fun findBinaryInProject(projectDir: File, binaryName: String): File? {
        val searchDirs = listOf(
            projectDir,
            File(projectDir, "bin"),
            File(projectDir, "build")
        )

        for (dir in searchDirs) {
            val file = File(dir, binaryName)
            if (file.exists() && file.isFile) return file
        }
        return null
    }




    fun clearCache(context: Context) {
        getBinDir(context).deleteRecursively()
        AppLogger.i(TAG, "Go 二进制缓存已清理")
    }




    fun getCacheSize(context: Context): Long {
        return getBinDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
