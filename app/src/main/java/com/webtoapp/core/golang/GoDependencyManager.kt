package com.webtoapp.core.golang

import android.content.Context
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import java.io.File

/**
 * Go 运行时依赖管理器
 * 
 * Go 的特殊性：Go 编译为静态链接的原生 ELF 二进制，不需要运行时解释器。
 * 用户需要提供预编译的 ARM/ARM64 二进制文件。
 * 
 * 本管理器负责：
 * - 验证 Go 二进制的 ABI 兼容性（ELF header 检查）
 * - 将二进制放置到可执行位置（nativeLibraryDir 或 filesDir）
 * - 设置执行权限
 * - 管理 Go 项目目录
 */
object GoDependencyManager {
    
    private const val TAG = "GoDependencyManager"
    
    // ==================== 目录管理 ====================
    
    /**
     * 获取 Go 项目存储根目录
     */
    fun getProjectsDir(context: Context): File {
        return File(context.filesDir, "go_projects").also { it.mkdirs() }
    }
    
    /**
     * 获取 Go 二进制缓存目录
     */
    fun getBinDir(context: Context): File {
        return File(context.filesDir, "go_bins").also { it.mkdirs() }
    }
    
    /**
     * 获取设备主 ABI
     */
    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }
    
    // ==================== ELF 验证 ====================
    
    /** ELF 架构常量 */
    private const val ELF_CLASS_32 = 1
    private const val ELF_CLASS_64 = 2
    private const val ELF_MACHINE_ARM = 0x28     // ARM (armeabi-v7a)
    private const val ELF_MACHINE_AARCH64 = 0xB7 // ARM64 (arm64-v8a)
    private const val ELF_MACHINE_386 = 0x03     // x86
    private const val ELF_MACHINE_X86_64 = 0x3E  // x86_64
    
    data class ElfInfo(
        val isValid: Boolean,
        val is64Bit: Boolean = false,
        val machine: Int = 0,
        val abiName: String = "unknown"
    )
    
    /**
     * 解析 ELF 文件头，验证架构兼容性
     */
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
            
            // ELF magic: 0x7f 'E' 'L' 'F'
            if (header[0] != 0x7f.toByte() || header[1] != 'E'.code.toByte() ||
                header[2] != 'L'.code.toByte() || header[3] != 'F'.code.toByte()) {
                return ElfInfo(isValid = false)
            }
            
            val elfClass = header[4].toInt() and 0xFF
            val is64Bit = elfClass == ELF_CLASS_64
            
            // e_machine 在 offset 18 (little-endian)
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
    
    /**
     * 检查 ELF 二进制是否与当前设备 ABI 兼容
     */
    fun isCompatible(elfInfo: ElfInfo): Boolean {
        if (!elfInfo.isValid) return false
        val deviceAbi = getDeviceAbi()
        // 精确匹配
        if (elfInfo.abiName == deviceAbi) return true
        // arm64 设备可以运行 arm32 二进制
        if (deviceAbi == "arm64-v8a" && elfInfo.abiName == "armeabi-v7a") return true
        // x86_64 设备可以运行 x86 二进制
        if (deviceAbi == "x86_64" && elfInfo.abiName == "x86") return true
        return false
    }
    
    /**
     * 准备 Go 二进制到可执行位置
     * 
     * @param projectDir 项目目录
     * @param binaryName 二进制文件名
     * @return 可执行的二进制路径，失败返回 null
     */
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
        
        // 复制到 bin 目录并设置执行权限
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
    
    /**
     * 在项目目录中查找二进制文件
     */
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
    
    /**
     * 清理 Go 二进制缓存
     */
    fun clearCache(context: Context) {
        getBinDir(context).deleteRecursively()
        AppLogger.i(TAG, "Go 二进制缓存已清理")
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(context: Context): Long {
        return getBinDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
