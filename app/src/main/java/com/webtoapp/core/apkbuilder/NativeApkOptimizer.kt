package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.io.File

/**
 * Native APK 体积优化器
 * 
 * 使用 C 级别的底层 ZIP 操作实现极致 APK 压缩：
 * 
 * 1. **条目级裁剪**: 移除 Shell 模式不需要的资源、Kotlin 元数据、
 *    编辑器专用 assets 等
 * 2. **DEFLATE 超压缩**: 使用 zlib 最高压缩级别 (9) 重压缩所有 DEFLATED 条目
 * 3. **CRC32 去重**: 检测并合并内容完全相同的条目
 * 4. **resources.arsc 对齐**: 确保满足 Android R+ 的 4 字节对齐要求
 * 
 * 所有操作在 C 原生层完成，不占用 JVM 堆内存。
 */
object NativeApkOptimizer {
    
    private const val TAG = "NativeApkOptimizer"
    
    /**
     * 优化结果数据类
     */
    data class OptimizeResult(
        val success: Boolean,
        val originalSize: Long,
        val optimizedSize: Long,
        val entriesTotal: Int,
        val entriesStripped: Int,
        val entriesRecompressed: Int,
        val entriesDeduplicated: Int,
        val dexFilesStripped: Int,
        val nativeLibSavings: Long,
        val dexSavings: Long,
        val resourceSavings: Long,
        val recompressionSavings: Long,
        val dedupSavings: Long,
        val unusedResSavings: Long
    ) {
        /** 节省的总字节数 */
        val totalSavings: Long get() = originalSize - optimizedSize
        
        /** 压缩比 (百分比) */
        val savingsPercent: Float 
            get() = if (originalSize > 0) totalSavings * 100f / originalSize else 0f
        
        /** 格式化的报告 */
        fun formatReport(): String = buildString {
            appendLine("═══ APK 优化报告 ═══")
            appendLine("原始大小: ${formatSize(originalSize)}")
            appendLine("优化后: ${formatSize(optimizedSize)}")
            appendLine("节省: ${formatSize(totalSavings)} (${String.format("%.1f", savingsPercent)}%)")
            appendLine()
            appendLine("── 优化明细 ──")
            appendLine("条目总数: $entriesTotal")
            appendLine("移除条目: $entriesStripped")
            appendLine("重压缩: $entriesRecompressed 个条目, 节省 ${formatSize(recompressionSavings)}")
            appendLine("CRC去重: $entriesDeduplicated 个条目, 节省 ${formatSize(dedupSavings)}")
            appendLine("资源裁剪: ${formatSize(resourceSavings)}")
            appendLine("未使用资源移除: ${formatSize(unusedResSavings)}")
            if (dexFilesStripped > 0) {
                appendLine("DEX 裁剪: $dexFilesStripped 个文件, 节省 ${formatSize(dexSavings)}")
            }
        }
        
        private fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
    
    /**
     * 体积分析结果
     */
    data class SizeBreakdown(
        val nativeLibs: Long,
        val dex: Long,
        val assets: Long,
        val resources: Long,
        val metaInf: Long,
        val kotlinMetadata: Long,
        val other: Long,
        val strippableTotal: Long
    ) {
        val total: Long get() = nativeLibs + dex + assets + resources + metaInf + kotlinMetadata + other
        
        fun formatReport(): String = buildString {
            val t = total.toFloat()
            appendLine("═══ APK 体积分析 ═══")
            appendLine(String.format("Native Libraries: %8s (%5.1f%%)", formatSize(nativeLibs), nativeLibs * 100f / t))
            appendLine(String.format("DEX (Code):       %8s (%5.1f%%)", formatSize(dex), dex * 100f / t))
            appendLine(String.format("Assets:           %8s (%5.1f%%)", formatSize(assets), assets * 100f / t))
            appendLine(String.format("Resources:        %8s (%5.1f%%)", formatSize(resources), resources * 100f / t))
            appendLine(String.format("META-INF:         %8s (%5.1f%%)", formatSize(metaInf), metaInf * 100f / t))
            appendLine(String.format("Kotlin Metadata:  %8s (%5.1f%%)", formatSize(kotlinMetadata), kotlinMetadata * 100f / t))
            appendLine(String.format("Other:            %8s (%5.1f%%)", formatSize(other), other * 100f / t))
            appendLine("───────────────────────────")
            appendLine(String.format("Total:            %8s", formatSize(total)))
            appendLine(String.format("可优化:           %8s", formatSize(strippableTotal)))
        }
        
        private fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
    
    private var isLoaded = false
    
    init {
        try {
            System.loadLibrary("apk_optimizer")
            isLoaded = true
            AppLogger.i(TAG, "Native APK optimizer loaded")
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.e(TAG, "Failed to load native APK optimizer", e)
        }
    }
    
    /**
     * 优化 APK 文件大小
     * 
     * 在签名之前对未签名的 APK 进行深度优化。
     * 
     * @param inputApk  输入 APK 文件（未签名）
     * @param outputApk 输出优化后的 APK 文件
     * @return 优化结果，null 表示 native 库未加载
     */
    fun optimizeApk(inputApk: File, outputApk: File): OptimizeResult? {
        if (!isLoaded) {
            AppLogger.w(TAG, "Native optimizer not loaded, skipping")
            return null
        }
        
        if (!inputApk.exists()) {
            AppLogger.e(TAG, "Input APK not found: ${inputApk.absolutePath}")
            return null
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            val result = nativeOptimizeApk(inputApk.absolutePath, outputApk.absolutePath)
            val elapsed = System.currentTimeMillis() - startTime
            
            if (result != null) {
                AppLogger.i(TAG, "APK optimized in ${elapsed}ms: " +
                    "${result.originalSize / 1024}KB -> ${result.optimizedSize / 1024}KB " +
                    "(saved ${result.totalSavings / 1024}KB, ${String.format("%.1f", result.savingsPercent)}%)")
            }
            
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "APK optimization failed", e)
            null
        }
    }
    
    /**
     * 快速分析 APK 体积构成
     * 
     * @param apkFile APK 文件
     * @return 体积分解，null 表示失败
     */
    fun analyzeSize(apkFile: File): SizeBreakdown? {
        if (!isLoaded) return null
        if (!apkFile.exists()) return null
        
        return try {
            val sizes = nativeAnalyzeApkSize(apkFile.absolutePath) ?: return null
            if (sizes.size < 8) return null
            
            SizeBreakdown(
                nativeLibs = sizes[0],
                dex = sizes[1],
                assets = sizes[2],
                resources = sizes[3],
                metaInf = sizes[4],
                kotlinMetadata = sizes[5],
                other = sizes[6],
                strippableTotal = sizes[7]
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "APK size analysis failed", e)
            null
        }
    }
    
    /**
     * 检查 native 优化器是否可用
     */
    fun isAvailable(): Boolean = isLoaded
    
    // JNI 原生方法
    private external fun nativeOptimizeApk(inputPath: String, outputPath: String): OptimizeResult?
    private external fun nativeAnalyzeApkSize(apkPath: String): LongArray?
}
