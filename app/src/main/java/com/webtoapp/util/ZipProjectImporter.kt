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
 * ZIP project importer
 * 
 * Features:
 * 1. Extract ZIP to temp directory.
 * 2. Scan and classify project files.
 * 3. Detect entry file (prefer index.html).
 * 4. Build analysis summary and warnings.
 */
object ZipProjectImporter {

    private const val TAG = "ZipProjectImporter"
    private const val MAX_ZIP_SIZE = 500L * 1024 * 1024 // 500MB
    private const val MAX_ENTRY_COUNT = 10_000

    // ==================== File type extensions ====================

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

    /** Files/directories to skip (metadata and VCS folders). */
    private val SKIP_PATTERNS = setOf(
        "__MACOSX", ".DS_Store", "Thumbs.db", ".git", ".svn", ".hg",
        "node_modules", ".idea", ".vscode"
    )

    // ==================== Data models ====================

    /** Resource type (more detailed than HtmlFileType). */
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

    /** Note. */
    data class ProjectFile(
        val relativePath: String,   // （ "css/style.css"）
        val absolutePath: String,   // Note.
        val size: Long,             // （）
        val resourceType: ResourceType
    ) {
        val fileName: String get() = relativePath.substringAfterLast('/')
        val extension: String get() = fileName.substringAfterLast('.', "").lowercase()
        
        /** HtmlFile */
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

    /** Resource stats */
    data class ResourceStats(
        val type: ResourceType,
        val count: Int,
        val totalSize: Long
    ) {
        val formattedSize: String get() = totalSize.toFileSizeString()
    }

    /** ZIP */
    data class ZipProjectAnalysis(
        val extractDir: String,                     // Note.
        val allFiles: List<ProjectFile>,            // Note.
        val entryFile: String,                      // （）
        val htmlFiles: List<ProjectFile>,            // HTML
        val stats: List<ResourceStats>,             // Note.
        val totalFileCount: Int,                    // Note.
        val totalSize: Long,                        // Note.
        val warnings: List<String>,                 // Note.
        val zipFileName: String                     // ZIP
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

    // ==================== ====================

    /**
     * URI ZIP
     * @return result
     */
    suspend fun importZip(context: Context, zipUri: Uri): ZipProjectAnalysis {
        val zipFileName = getZipFileName(context, zipUri) ?: "project.zip"
        AppLogger.i(TAG, "开始导入 ZIP: $zipFileName")

        // 1.
        val extractDir = extractZip(context, zipUri)
        AppLogger.i(TAG, "解压完成: ${extractDir.absolutePath}")

        // 2. （ ZIP ）
        val projectRoot = unwrapSingleRootDir(extractDir)

        // 3.
        val analysis = analyzeProject(projectRoot, zipFileName)
        AppLogger.i(TAG, "分析完成: ${analysis.totalFileCount} 个文件, ${analysis.formattedTotalSize}")

        return analysis
    }

    /**
     * ZIP
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
                        // Note.
                        entryCount++
                        if (entryCount > MAX_ENTRY_COUNT) {
                            throw ZipImportException("ZIP 文件包含过多条目（超过 $MAX_ENTRY_COUNT）")
                        }

                        val entryName = entry.name

                        // /
                        if (shouldSkipEntry(entryName)) {
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }

                        // Zip Slip
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
     * （ ZIP ），
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
     * Note.
     */
    private fun analyzeProject(projectDir: File, zipFileName: String): ZipProjectAnalysis {
        val allFiles = mutableListOf<ProjectFile>()
        val warnings = mutableListOf<String>()

        // Note.
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

        // Note.
        val htmlFiles = allFiles.filter { it.resourceType == ResourceType.HTML }

        // Note.
        val entryFile = detectEntryFile(htmlFiles)
        if (entryFile == null && htmlFiles.isEmpty()) {
            warnings.add("未找到 HTML 文件，请确认 ZIP 内容正确")
        }

        // Note.
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

        // Note.
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
     * Note.
     * : index.html > index.htm > HTML > HTML
     */
    private fun detectEntryFile(htmlFiles: List<ProjectFile>): String? {
        if (htmlFiles.isEmpty()) return null

        // 1. index.html（）
        htmlFiles.find { it.relativePath.equals("index.html", ignoreCase = true) }
            ?.let { return it.relativePath }

        // 2. index.htm
        htmlFiles.find { it.relativePath.equals("index.htm", ignoreCase = true) }
            ?.let { return it.relativePath }

        // 3. index.html
        htmlFiles.find { it.fileName.equals("index.html", ignoreCase = true) }
            ?.let { return it.relativePath }

        // 4. HTML
        htmlFiles.find { !it.relativePath.contains('/') }
            ?.let { return it.relativePath }

        // 5. HTML
        return htmlFiles.firstOrNull()?.relativePath
    }

    // ==================== ====================

    /** Note. */
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

    /** Note. */
    private fun shouldSkipEntry(name: String): Boolean {
        val parts = name.split('/', '\\')
        return parts.any { part -> SKIP_PATTERNS.any { pattern -> part.equals(pattern, ignoreCase = true) } }
    }

    /** ZIP */
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
     * Note.
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

    /** ZIP */
    class ZipImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
