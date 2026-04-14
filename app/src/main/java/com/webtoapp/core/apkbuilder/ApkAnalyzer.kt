package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 */
object ApkAnalyzer {
    
    private const val TAG = "ApkAnalyzer"
    
    /**
     * Note: brief English comment.
     */
    enum class FileCategory(val displayName: String, val color: String) {
        NATIVE_LIBS("Native Libraries", "#FF6B6B"),
        DEX("DEX (Code)", "#4ECDC4"),
        ASSETS("Assets", "#45B7D1"),
        RESOURCES("Resources", "#96CEB4"),
        META_INF("Signatures", "#FFEAA7"),
        KOTLIN("Kotlin Metadata", "#DDA0DD"),
        OTHER("Other", "#95A5A6")
    }
    
    /**
     * Note: brief English comment.
     */
    data class FileEntry(
        val path: String,
        val compressedSize: Long,
        val uncompressedSize: Long,
        val category: FileCategory,
        val isCompressed: Boolean
    ) {
        val compressionRatio: Float
            get() = if (uncompressedSize > 0) {
                1f - (compressedSize.toFloat() / uncompressedSize.toFloat())
            } else 0f
    }
    
    /**
     * Note: brief English comment.
     */
    data class CategorySummary(
        val category: FileCategory,
        val totalCompressedSize: Long,
        val totalUncompressedSize: Long,
        val fileCount: Int,
        val percentage: Float,
        val largestFiles: List<FileEntry>
    )
    
    /**
     * Note: brief English comment.
     */
    data class OptimizationHint(
        val title: String,
        val description: String,
        val potentialSavingBytes: Long,
        val priority: Priority
    ) {
        enum class Priority { HIGH, MEDIUM, LOW }
    }
    
    /**
     * Note: brief English comment.
     */
    data class AnalysisReport(
        val apkFile: File,
        val totalSize: Long,
        val totalUncompressedSize: Long,
        val entryCount: Int,
        val categories: List<CategorySummary>,
        val topLargestFiles: List<FileEntry>,
        val optimizationHints: List<OptimizationHint>,
        val architectures: Set<String>,
        val analysisTimeMs: Long
    ) {
        val totalSizeFormatted: String get() = formatSize(totalSize)
        val compressionRatio: Float
            get() = if (totalUncompressedSize > 0) {
                1f - (totalSize.toFloat() / totalUncompressedSize.toFloat())
            } else 0f
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    suspend fun analyze(apkFile: File, topN: Int = 15): AnalysisReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        AppLogger.i(TAG, "Analyzing APK: ${apkFile.name} (${formatSize(apkFile.length())})")
        
        val entries = mutableListOf<FileEntry>()
        val architectures = mutableSetOf<String>()
        
        ZipFile(apkFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val category = categorize(entry.name)
                entries.add(
                    FileEntry(
                        path = entry.name,
                        compressedSize = entry.compressedSize,
                        uncompressedSize = entry.size,
                        category = category,
                        isCompressed = entry.method == java.util.zip.ZipEntry.DEFLATED
                    )
                )
                
                // Track architectures
                if (entry.name.startsWith("lib/")) {
                    val abi = entry.name.removePrefix("lib/").substringBefore("/")
                    if (abi.isNotEmpty() && !abi.contains(".")) {
                        architectures.add(abi)
                    }
                }
            }
        }
        
        val totalCompressed = entries.sumOf { it.compressedSize }
        val totalUncompressed = entries.sumOf { it.uncompressedSize }
        
        // Group by category
        val categoryGroups = entries.groupBy { it.category }
        val categories = FileCategory.entries.mapNotNull { cat ->
            val group = categoryGroups[cat] ?: return@mapNotNull null
            val catCompressed = group.sumOf { it.compressedSize }
            CategorySummary(
                category = cat,
                totalCompressedSize = catCompressed,
                totalUncompressedSize = group.sumOf { it.uncompressedSize },
                fileCount = group.size,
                percentage = if (totalCompressed > 0) catCompressed.toFloat() / totalCompressed * 100f else 0f,
                largestFiles = group.sortedByDescending { it.compressedSize }.take(5)
            )
        }.sortedByDescending { it.totalCompressedSize }
        
        // Top largest files
        val topLargest = entries.sortedByDescending { it.compressedSize }.take(topN)
        
        // Generate optimization hints
        val hints = generateHints(entries, categories, architectures, apkFile.length())
        
        val elapsed = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "Analysis complete in ${elapsed}ms: ${entries.size} entries, ${architectures.size} ABIs")
        
        AnalysisReport(
            apkFile = apkFile,
            totalSize = apkFile.length(),
            totalUncompressedSize = totalUncompressed,
            entryCount = entries.size,
            categories = categories,
            topLargestFiles = topLargest,
            optimizationHints = hints,
            architectures = architectures,
            analysisTimeMs = elapsed
        )
    }
    
    /**
     * Note: brief English comment.
     */
    fun formatReport(report: AnalysisReport): String = buildString {
        appendLine("═══════════════════════════════════════")
        appendLine("  APK Analysis Report")
        appendLine("═══════════════════════════════════════")
        appendLine()
        appendLine("File: ${report.apkFile.name}")
        appendLine("Size: ${report.totalSizeFormatted}")
        appendLine("Entries: ${report.entryCount}")
        appendLine("Architectures: ${report.architectures.joinToString(", ").ifEmpty { "none" }}")
        appendLine("Compression: ${String.format("%.1f%%", report.compressionRatio * 100)}")
        appendLine()
        
        appendLine("─── Size Breakdown ───")
        report.categories.forEach { cat ->
            val bar = "█".repeat((cat.percentage / 5).toInt().coerceIn(0, 20))
            appendLine(String.format(
                "  %-18s %8s  %5.1f%%  %s",
                cat.category.displayName,
                formatSize(cat.totalCompressedSize),
                cat.percentage,
                bar
            ))
        }
        appendLine()
        
        appendLine("─── Top ${report.topLargestFiles.size} Largest Files ───")
        report.topLargestFiles.forEachIndexed { i, entry ->
            appendLine(String.format(
                "  %2d. %-40s %8s",
                i + 1,
                entry.path.takeLast(40),
                formatSize(entry.compressedSize)
            ))
        }
        appendLine()
        
        if (report.optimizationHints.isNotEmpty()) {
            appendLine("─── Optimization Hints ───")
            report.optimizationHints.forEach { hint ->
                val priority = when (hint.priority) {
                    OptimizationHint.Priority.HIGH -> "[HIGH]"
                    OptimizationHint.Priority.MEDIUM -> "[MED]"
                    OptimizationHint.Priority.LOW -> "[LOW]"
                }
                appendLine("  $priority ${hint.title}")
                appendLine("     ${hint.description}")
                if (hint.potentialSavingBytes > 0) {
                    appendLine("     Potential saving: ~${formatSize(hint.potentialSavingBytes)}")
                }
                appendLine()
            }
        }
        
        appendLine("═══════════════════════════════════════")
        appendLine("Analysis completed in ${report.analysisTimeMs}ms")
    }
    
    /**
     * Note: brief English comment.
     */
    private fun categorize(path: String): FileCategory = when {
        path.startsWith("lib/") -> FileCategory.NATIVE_LIBS
        path.endsWith(".dex") -> FileCategory.DEX
        path.startsWith("assets/") -> FileCategory.ASSETS
        path == "resources.arsc" || path.startsWith("res/") -> FileCategory.RESOURCES
        path.startsWith("META-INF/") -> FileCategory.META_INF
        path.startsWith("kotlin/") || path == "DebugProbesKt.bin" -> FileCategory.KOTLIN
        else -> FileCategory.OTHER
    }
    
    /**
     * Note: brief English comment.
     */
    private fun generateHints(
        entries: List<FileEntry>,
        categories: List<CategorySummary>,
        architectures: Set<String>,
        totalSize: Long
    ): List<OptimizationHint> {
        val hints = mutableListOf<OptimizationHint>()
        
        // 1. Multi-architecture check
        if (architectures.size > 1) {
            val nativeLibs = categories.find { it.category == FileCategory.NATIVE_LIBS }
            val singleAbiSize = nativeLibs?.totalCompressedSize?.div(architectures.size) ?: 0L
            val savingPerAbi = nativeLibs?.totalCompressedSize?.minus(singleAbiSize) ?: 0L
            
            hints.add(OptimizationHint(
                title = "Multiple architectures detected: ${architectures.joinToString(", ")}",
                description = "Building separate APKs per ABI (e.g., arm64-v8a only) can significantly reduce size. " +
                    "Most modern devices only need arm64-v8a.",
                potentialSavingBytes = savingPerAbi,
                priority = if (savingPerAbi > 10 * 1024 * 1024) OptimizationHint.Priority.HIGH 
                    else OptimizationHint.Priority.MEDIUM
            ))
        }
        
        // 2. Large native libraries
        val largeNativeLibs = entries.filter { 
            it.category == FileCategory.NATIVE_LIBS && it.compressedSize > 5 * 1024 * 1024 
        }
        largeNativeLibs.forEach { lib ->
            hints.add(OptimizationHint(
                title = "Large native library: ${lib.path.substringAfterLast("/")}",
                description = "This library is ${formatSize(lib.compressedSize)}. " +
                    "Check if it's required for your app type.",
                potentialSavingBytes = lib.compressedSize,
                priority = if (lib.compressedSize > 20 * 1024 * 1024) OptimizationHint.Priority.HIGH 
                    else OptimizationHint.Priority.MEDIUM
            ))
        }
        
        // 3. Uncompressed large assets
        val uncompressedAssets = entries.filter { 
            it.category == FileCategory.ASSETS && 
            !it.isCompressed && 
            it.uncompressedSize > 1 * 1024 * 1024 &&
            !it.path.endsWith(".mp4") && !it.path.endsWith(".mp3") // media files are intentionally stored
        }
        if (uncompressedAssets.isNotEmpty()) {
            val totalUncompressed = uncompressedAssets.sumOf { it.uncompressedSize }
            hints.add(OptimizationHint(
                title = "${uncompressedAssets.size} large uncompressed assets",
                description = "Some assets are stored without compression. " +
                    "Compressing them could save space (unless they need random access).",
                potentialSavingBytes = totalUncompressed / 4, // estimate ~25% savings
                priority = OptimizationHint.Priority.LOW
            ))
        }
        
        // 4. Kotlin metadata
        val kotlinMeta = categories.find { it.category == FileCategory.KOTLIN }
        if (kotlinMeta != null && kotlinMeta.totalCompressedSize > 10 * 1024) {
            hints.add(OptimizationHint(
                title = "Kotlin metadata present",
                description = "Kotlin reflection metadata files are included but usually not needed at runtime. " +
                    "These are already stripped during APK build.",
                potentialSavingBytes = kotlinMeta.totalCompressedSize,
                priority = OptimizationHint.Priority.LOW
            ))
        }
        
        // 5. Large resources.arsc
        val arsc = entries.find { it.path == "resources.arsc" }
        if (arsc != null && arsc.uncompressedSize > 2 * 1024 * 1024) {
            hints.add(OptimizationHint(
                title = "Large resources.arsc (${formatSize(arsc.uncompressedSize)})",
                description = "Resource table is large. Consider removing unused string translations or resources.",
                potentialSavingBytes = arsc.uncompressedSize / 3,
                priority = OptimizationHint.Priority.MEDIUM
            ))
        }
        
        // 6. Duplicate file detection (by size — not perfect but catches most cases)
        val sizeGroups = entries
            .filter { it.uncompressedSize > 10 * 1024 } // only check files > 10KB
            .groupBy { it.uncompressedSize }
            .filter { it.value.size > 1 }
        
        if (sizeGroups.isNotEmpty()) {
            val duplicateCount = sizeGroups.values.sumOf { it.size - 1 }
            val duplicateSize = sizeGroups.values.sumOf { group -> 
                group.drop(1).sumOf { it.compressedSize } 
            }
            if (duplicateSize > 50 * 1024) {
                hints.add(OptimizationHint(
                    title = "$duplicateCount potentially duplicate files",
                    description = "Found files with identical sizes that may be duplicates. " +
                        "Review and remove redundant copies.",
                    potentialSavingBytes = duplicateSize,
                    priority = if (duplicateSize > 1 * 1024 * 1024) OptimizationHint.Priority.MEDIUM 
                        else OptimizationHint.Priority.LOW
                ))
            }
        }
        
        // 7. Overall size warning
        if (totalSize > 100 * 1024 * 1024) {
            hints.add(0, OptimizationHint(
                title = "APK exceeds 100MB",
                description = "Large APKs take longer to download and install. " +
                    "Consider using single-ABI builds and removing unused features.",
                potentialSavingBytes = 0,
                priority = OptimizationHint.Priority.HIGH
            ))
        }
        
        return hints.sortedBy { it.priority.ordinal }
    }
    
    /**
     * Note: brief English comment.
     */
    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
