package com.webtoapp.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * ZIP 项目导入工具
 * 
 * 功能：
 * 1. 解压 ZIP 文件到临时目录（自动处理嵌套根目录）
 * 2. 扫描并分类所有文件（HTML/CSS/JS/图片/字体/音频/视频/其他）
 * 3. 自动识别入口文件（index.html 优先）
 * 4. 生成项目分析报告（统计信息、问题检测）
 */
object ZipProjectImporter {

    private const val TAG = "ZipProjectImporter"
    private const val MAX_ZIP_SIZE = 500L * 1024 * 1024 // 500MB
    private const val MAX_ENTRY_COUNT = 10_000

    // ==================== 文件分类扩展名 ====================

    private val HTML_EXTENSIONS = setOf("html", "htm", "xhtml")
    private val CSS_EXTENSIONS = setOf("css")
    private val JS_EXTENSIONS = setOf("js", "mjs", "jsx", "ts", "tsx")
    private val IMAGE_EXTENSIONS = setOf(
        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp", "avif", "tiff", "tif"
    )
    private val FONT_EXTENSIONS = setOf("ttf", "otf", "woff", "woff2", "eot")
    private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "aac", "flac", "m4a", "wma")
    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "avi", "mov", "flv", "m4v")
    private val DATA_EXTENSIONS = setOf("json", "xml", "csv", "txt", "md", "yaml", "yml", "toml")

    /** 应当跳过的文件/目录（macOS 元数据、版本控制等） */
    private val SKIP_PATTERNS = setOf(
        "__MACOSX", ".DS_Store", "Thumbs.db", ".git", ".svn", ".hg",
        "node_modules", ".idea", ".vscode"
    )

    // ==================== 数据模型 ====================

    /** 文件资源类型（比 HtmlFileType 更细粒度） */
    enum class ResourceType(val icon: String, val label: String) {
        HTML("globe", "HTML"),
        CSS("palette", "CSS"),
        JS("bolt", "JavaScript"),
        IMAGE("image", "图片"),
        FONT("font_download", "字体"),
        AUDIO("music_note", "音频"),
        VIDEO("movie", "视频"),
        DATA("file", "数据"),
        OTHER("attachment", "其他")
    }

    /** 单个文件信息 */
    data class ProjectFile(
        val relativePath: String,   // 相对路径（如 "css/style.css"）
        val absolutePath: String,   // 绝对路径
        val size: Long,             // 文件大小（字节）
        val resourceType: ResourceType
    ) {
        val fileName: String get() = relativePath.substringAfterLast('/')
        val extension: String get() = fileName.substringAfterLast('.', "").lowercase()
        
        /** 转换为 HtmlFile */
        fun toHtmlFile(): HtmlFile = HtmlFile(
            name = relativePath,
            path = absolutePath,
            type = when (resourceType) {
                ResourceType.HTML -> HtmlFileType.HTML
                ResourceType.CSS -> HtmlFileType.CSS
                ResourceType.JS -> HtmlFileType.JS
                ResourceType.IMAGE -> HtmlFileType.IMAGE
                ResourceType.FONT -> HtmlFileType.FONT
                else -> HtmlFileType.OTHER
            }
        )
    }

    /** 资源统计信息 */
    data class ResourceStats(
        val type: ResourceType,
        val count: Int,
        val totalSize: Long
    ) {
        val formattedSize: String get() = totalSize.toFileSizeString()
    }

    /** ZIP 项目分析结果 */
    data class ZipProjectAnalysis(
        val extractDir: String,                     // 解压目录路径
        val allFiles: List<ProjectFile>,            // 所有文件列表
        val entryFile: String,                      // 自动识别的入口文件（相对路径）
        val htmlFiles: List<ProjectFile>,            // HTML 文件列表
        val stats: List<ResourceStats>,             // 各类型资源统计
        val totalFileCount: Int,                    // 文件总数
        val totalSize: Long,                        // 总大小
        val warnings: List<String>,                 // 警告信息
        val zipFileName: String                     // 原始 ZIP 文件名
    ) {
        val suggestedAppName: String
            get() = zipFileName
                .removeSuffix(".zip")
                .removeSuffix(".ZIP")
                .replace(Regex("[_\\-]+"), " ")
                .trim()
                .ifBlank { "HTML App" }

        val formattedTotalSize: String get() = totalSize.toFileSizeString()
    }

    // ==================== 核心方法 ====================

    /**
     * 从 URI 解压 ZIP 并分析项目
     * @return 分析结果，失败时抛异常
     */
    suspend fun importZip(context: Context, zipUri: Uri): ZipProjectAnalysis {
        val zipFileName = getZipFileName(context, zipUri) ?: "project.zip"
        AppLogger.i(TAG, "开始导入 ZIP: $zipFileName")

        // 1. 解压
        val extractDir = extractZip(context, zipUri)
        AppLogger.i(TAG, "解压完成: ${extractDir.absolutePath}")

        // 2. 处理嵌套根目录（如 ZIP 内只有一个文件夹包裹了所有内容）
        val projectRoot = unwrapSingleRootDir(extractDir)

        // 3. 分析
        val analysis = analyzeProject(projectRoot, zipFileName)
        AppLogger.i(TAG, "分析完成: ${analysis.totalFileCount} 个文件, ${analysis.formattedTotalSize}")

        return analysis
    }

    /**
     * 解压 ZIP 到临时目录
     */
    private fun extractZip(context: Context, zipUri: Uri): File {
        val tempDir = File(context.cacheDir, "zip_import_${System.currentTimeMillis()}").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        var entryCount = 0
        var totalBytes = 0L

        try {
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        // 安全检查
                        entryCount++
                        if (entryCount > MAX_ENTRY_COUNT) {
                            throw ZipImportException("ZIP 文件包含过多条目（超过 $MAX_ENTRY_COUNT）")
                        }

                        val entryName = entry.name

                        // 跳过不需要的文件/目录
                        if (shouldSkipEntry(entryName)) {
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }

                        // Zip Slip 防护
                        val targetFile = File(tempDir, entryName).canonicalFile
                        if (!targetFile.path.startsWith(tempDir.canonicalPath)) {
                            AppLogger.w(TAG, "跳过不安全的 ZIP 条目: $entryName")
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }

                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { fos ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (zis.read(buffer).also { bytesRead = it } != -1) {
                                    totalBytes += bytesRead
                                    if (totalBytes > MAX_ZIP_SIZE) {
                                        throw ZipImportException("解压后文件大小超过限制（${MAX_ZIP_SIZE / 1024 / 1024}MB）")
                                    }
                                    fos.write(buffer, 0, bytesRead)
                                }
                            }
                        }

                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw ZipImportException("无法打开 ZIP 文件")

        } catch (e: ZipImportException) {
            tempDir.deleteRecursively()
            throw e
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            AppLogger.e(TAG, "解压 ZIP 失败", e)
            throw ZipImportException("解压失败: ${e.message}", e)
        }

        if (entryCount == 0) {
            tempDir.deleteRecursively()
            throw ZipImportException("ZIP 文件为空")
        }

        return tempDir
    }

    /**
     * 如果解压目录中只有一个子目录（常见的 ZIP 打包方式），展开它
     */
    private fun unwrapSingleRootDir(dir: File): File {
        val children = dir.listFiles() ?: return dir
        return if (children.size == 1 && children[0].isDirectory) {
            children[0]
        } else {
            dir
        }
    }

    /**
     * 分析项目目录
     */
    private fun analyzeProject(projectDir: File, zipFileName: String): ZipProjectAnalysis {
        val allFiles = mutableListOf<ProjectFile>()
        val warnings = mutableListOf<String>()

        // 遍历所有文件
        projectDir.walkTopDown()
            .filter { it.isFile }
            .filter { !shouldSkipEntry(it.name) }
            .forEach { file ->
                val relativePath = file.relativeTo(projectDir).path
                val resourceType = classifyFile(file.name)
                allFiles.add(
                    ProjectFile(
                        relativePath = relativePath,
                        absolutePath = file.absolutePath,
                        size = file.length(),
                        resourceType = resourceType
                    )
                )
            }

        // 分类
        val htmlFiles = allFiles.filter { it.resourceType == ResourceType.HTML }

        // 自动识别入口文件
        val entryFile = detectEntryFile(htmlFiles)
        if (entryFile == null && htmlFiles.isEmpty()) {
            warnings.add("未找到 HTML 文件，请确认 ZIP 内容正确")
        }

        // 统计
        val stats = ResourceType.entries
            .map { type ->
                val files = allFiles.filter { it.resourceType == type }
                ResourceStats(
                    type = type,
                    count = files.size,
                    totalSize = files.sumOf { it.size }
                )
            }
            .filter { it.count > 0 }

        // 额外检查
        if (allFiles.any { it.size > 50 * 1024 * 1024 }) {
            warnings.add("存在超过 50MB 的大文件，打包 APK 时可能影响安装包大小")
        }

        return ZipProjectAnalysis(
            extractDir = projectDir.absolutePath,
            allFiles = allFiles,
            entryFile = entryFile ?: htmlFiles.firstOrNull()?.relativePath ?: "index.html",
            htmlFiles = htmlFiles,
            stats = stats,
            totalFileCount = allFiles.size,
            totalSize = allFiles.sumOf { it.size },
            warnings = warnings,
            zipFileName = zipFileName
        )
    }

    /**
     * 自动识别入口文件
     * 优先级: index.html > index.htm > 根目录下的 HTML > 子目录的 HTML
     */
    private fun detectEntryFile(htmlFiles: List<ProjectFile>): String? {
        if (htmlFiles.isEmpty()) return null

        // 1. 精确匹配 index.html（根目录）
        htmlFiles.find { it.relativePath.equals("index.html", ignoreCase = true) }
            ?.let { return it.relativePath }

        // 2. 精确匹配 index.htm
        htmlFiles.find { it.relativePath.equals("index.htm", ignoreCase = true) }
            ?.let { return it.relativePath }

        // 3. 任意目录下的 index.html
        htmlFiles.find { it.fileName.equals("index.html", ignoreCase = true) }
            ?.let { return it.relativePath }

        // 4. 根目录下的任意 HTML 文件
        htmlFiles.find { !it.relativePath.contains('/') }
            ?.let { return it.relativePath }

        // 5. 第一个 HTML 文件
        return htmlFiles.firstOrNull()?.relativePath
    }

    // ==================== 辅助方法 ====================

    /** 根据文件名分类资源类型 */
    private fun classifyFile(fileName: String): ResourceType {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            in HTML_EXTENSIONS -> ResourceType.HTML
            in CSS_EXTENSIONS -> ResourceType.CSS
            in JS_EXTENSIONS -> ResourceType.JS
            in IMAGE_EXTENSIONS -> ResourceType.IMAGE
            in FONT_EXTENSIONS -> ResourceType.FONT
            in AUDIO_EXTENSIONS -> ResourceType.AUDIO
            in VIDEO_EXTENSIONS -> ResourceType.VIDEO
            in DATA_EXTENSIONS -> ResourceType.DATA
            else -> ResourceType.OTHER
        }
    }

    /** 是否应跳过此条目 */
    private fun shouldSkipEntry(name: String): Boolean {
        val parts = name.split('/', '\\')
        return parts.any { part -> SKIP_PATTERNS.any { pattern -> part.equals(pattern, ignoreCase = true) } }
    }

    /** 获取 ZIP 文件名 */
    private fun getZipFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }

    /**
     * 清理导入临时文件
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("zip_import_")) {
                    file.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "清理 ZIP 临时文件失败", e)
        }
    }

    /** ZIP 导入异常 */
    class ZipImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
