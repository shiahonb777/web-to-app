package com.webtoapp.util

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HTML project file processing helper.
 *
 * Extracted from MainViewModel — contains pure IO/processing logic
 * for categorising, inlining, and persisting HTML project files.
 */
object HtmlProjectHelper {

    private const val TAG = "HtmlProjectHelper"

    /**
     * Detect HtmlFileType from file name extension.
     */
    fun detectFileType(fileName: String): HtmlFileType = when {
        fileName.endsWith(".html", ignoreCase = true) ||
        fileName.endsWith(".htm", ignoreCase = true) -> HtmlFileType.HTML
        fileName.endsWith(".css", ignoreCase = true) -> HtmlFileType.CSS
        fileName.endsWith(".js", ignoreCase = true) -> HtmlFileType.JS
        else -> HtmlFileType.OTHER
    }

    /**
     * Process and save HTML project files.
     *
     * Categorises files, inlines CSS/JS into HTML, and saves to project directory.
     * If any CSS/JS file exceeds MAX_INLINE_FILE_SIZE, all files are saved separately
     * (no inlining) to prevent OOM crashes on large bundled files.
     *
     * @return list of saved HtmlFile entries
     */
    suspend fun processAndSaveFiles(
        context: Context,
        files: List<HtmlFile>,
        projectId: String
    ): List<HtmlFile> = withContext(Dispatchers.IO) {
        // Categorize files
        val htmlFiles = files.filter {
            it.type == HtmlFileType.HTML ||
            it.name.endsWith(".html", ignoreCase = true) ||
            it.name.endsWith(".htm", ignoreCase = true)
        }
        val cssFiles = files.filter {
            it.type == HtmlFileType.CSS ||
            it.name.endsWith(".css", ignoreCase = true)
        }
        val jsFiles = files.filter {
            it.type == HtmlFileType.JS ||
            it.name.endsWith(".js", ignoreCase = true)
        }
        val otherFiles = files.filter { file ->
            file !in htmlFiles && file !in cssFiles && file !in jsFiles
        }

        AppLogger.d(TAG, "HTML file classification: HTML=${htmlFiles.size}, CSS=${cssFiles.size}, JS=${jsFiles.size}, Other=${otherFiles.size}")

        // Check if any file is too large to inline safely (prevent OOM)
        val maxInlineSize = 2L * 1024 * 1024 // 2MB
        val totalContentSize = (cssFiles + jsFiles + htmlFiles).sumOf { java.io.File(it.path).let { f -> if (f.exists()) f.length() else 0L } }
        val hasLargeFile = (cssFiles + jsFiles + htmlFiles).any {
            val f = java.io.File(it.path)
            f.exists() && f.length() > maxInlineSize
        }

        if (hasLargeFile || totalContentSize > maxInlineSize * 3) {
            // Large file mode: save all files as-is without inlining (like ZIP mode)
            AppLogger.i(TAG, "Large file detected (total=${totalContentSize / 1024}KB), skipping CSS/JS inlining to prevent OOM")
            return@withContext saveAllWithoutInlining(files, context, projectId)
        }

        // Normal mode: inline CSS/JS into HTML
        // Read CSS content
        val cssContent = cssFiles.mapNotNull { cssFile ->
            try {
                val file = java.io.File(cssFile.path)
                if (file.exists() && file.canRead()) {
                    HtmlProjectProcessor.readFileWithEncoding(file, null)
                } else null
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to read CSS file: ${cssFile.path}", e)
                null
            }
        }.joinToString("\n\n")

        // Read JS content
        val jsContent = jsFiles.mapNotNull { jsFile ->
            try {
                val file = java.io.File(jsFile.path)
                if (file.exists() && file.canRead()) {
                    HtmlProjectProcessor.readFileWithEncoding(file, null)
                } else null
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to read JS file: ${jsFile.path}", e)
                null
            }
        }.joinToString("\n\n")

        // Handle HTML files, inline CSS and JS
        val processedHtmlFiles = htmlFiles.mapNotNull { htmlFile ->
            try {
                val sourceFile = java.io.File(htmlFile.path)
                if (!sourceFile.exists() || !sourceFile.canRead()) {
                    AppLogger.e(TAG, "HTML file does not exist or cannot be read: ${htmlFile.path}")
                    return@mapNotNull null
                }

                var htmlContent = HtmlProjectProcessor.readFileWithEncoding(sourceFile, null)
                htmlContent = HtmlProjectProcessor.processHtmlContent(
                    htmlContent = htmlContent,
                    cssContent = cssContent.takeIf { it.isNotBlank() },
                    jsContent = jsContent.takeIf { it.isNotBlank() },
                    fixPaths = true
                )

                val savedPath = HtmlStorage.saveProcessedHtml(context, htmlContent, htmlFile.name, projectId)
                if (savedPath != null) {
                    AppLogger.d(TAG, "HTML file saved (inlined CSS/JS): ${htmlFile.name}")
                    htmlFile.copy(path = savedPath)
                } else {
                    AppLogger.e(TAG, "Cannot save processed HTML file: ${htmlFile.name}")
                    null
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to process HTML file: ${htmlFile.path}", e)
                null
            }
        }

        // Save other files (images, fonts, etc.)
        val savedOtherFiles = otherFiles.mapNotNull { file ->
            val savedPath = HtmlStorage.saveFromTempFile(context, file.path, file.name, projectId)
            if (savedPath != null) file.copy(path = savedPath) else null
        }

        processedHtmlFiles + savedOtherFiles
    }

    /**
     * Save all files without inlining CSS/JS into HTML.
     * Used when files are too large to safely load into memory.
     */
    fun saveAllWithoutInlining(
        files: List<HtmlFile>,
        context: Context,
        projectId: String
    ): List<HtmlFile> {
        return files.mapNotNull { file ->
            try {
                val savedPath = HtmlStorage.saveFromTempFile(context, file.path, file.name, projectId)
                if (savedPath != null) {
                    AppLogger.d(TAG, "File saved (no inlining): ${file.name}")
                    file.copy(path = savedPath)
                } else {
                    AppLogger.e(TAG, "Cannot save file: ${file.name}")
                    null
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to save file: ${file.path}", e)
                null
            }
        }
    }

    /**
     * Copy build output directory to app storage as HtmlFile list.
     */
    suspend fun copyBuildOutputToStorage(
        context: Context,
        outputPath: String,
        projectId: String
    ): List<HtmlFile> = withContext(Dispatchers.IO) {
        val outputDir = java.io.File(outputPath)
        if (!outputDir.exists() || !outputDir.isDirectory) {
            throw Exception("Build output directory not found: $outputPath")
        }

        val files = mutableListOf<HtmlFile>()
        outputDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = file.relativeTo(outputDir).path
                val savedPath = HtmlStorage.saveFromTempFile(context, file.absolutePath, relativePath, projectId)
                if (savedPath != null) {
                    files.add(HtmlFile(name = relativePath, path = savedPath, type = detectFileType(file.name)))
                }
            }
        }
        files
    }
}
