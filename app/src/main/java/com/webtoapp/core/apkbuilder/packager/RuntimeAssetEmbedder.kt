package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.TextFileClassifier
import java.io.File
import java.util.zip.ZipOutputStream

/**
 * Runtime asset embedder — replaces the repetitive addNodeJsFilesToAssets,
 * addPhpAppFilesToAssets, addPythonAppFilesToAssets, addGoAppFilesToAssets,
 * addFrontendFilesToAssets methods in ApkBuilder.
 *
 * Each runtime shares the same directory-traversal + text-classification +
 * ZIP-write pattern; only the asset prefix, exclude dirs, runtime type for
 * text classification, and optional per-file hook differ.
 */
object RuntimeAssetEmbedder {

    /**
     * Configuration for embedding a runtime project's files.
     */
    data class EmbedConfig(
        val runtimeName: String,
        val assetPrefix: String,
        val excludeDirs: Set<String>,
        val runtimeType: String? = null,
        /**
         * Optional per-file hook that can override the default text/binary
         * write strategy. Return true if the hook handled the file, false
         * to fall back to the default behaviour.
         */
        val fileHook: ((zipOut: ZipOutputStream, assetPath: String, file: File) -> Boolean)? = null
    )

    /**
     * Embed project files into the APK ZIP output.
     *
     * @return Pair(fileCount, totalSizeBytes)
     */
    fun embedProjectFiles(
        zipOut: ZipOutputStream,
        projectDir: File,
        config: EmbedConfig,
        logger: BuildLogger
    ): Pair<Int, Long> {
        AppLogger.d("RuntimeAssetEmbedder", "Embedding ${config.runtimeName} files from: ${projectDir.absolutePath}")

        var fileCount = 0
        var totalSize = 0L

        fun addDirRecursive(dir: File, basePath: String) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name in config.excludeDirs) {
                    return@forEach
                }
                val relativePath = "$basePath/${file.name}"
                if (file.isDirectory) {
                    addDirRecursive(file, relativePath)
                } else {
                    try {
                        val assetPath = "${config.assetPrefix}$relativePath"

                        // Let the hook handle the file first
                        val handled = config.fileHook?.invoke(zipOut, assetPath, file) ?: false
                        if (!handled) {
                            // Default: text → DEFLATED, binary → STORED
                            if (TextFileClassifier.isTextFile(file.name, config.runtimeType)) {
                                ZipUtils.writeEntryDeflated(zipOut, assetPath, file.readBytes())
                            } else {
                                ZipUtils.writeEntryStoredSimple(zipOut, assetPath, file.readBytes())
                            }
                        }
                        fileCount++
                        totalSize += file.length()
                    } catch (e: Exception) {
                        AppLogger.w("RuntimeAssetEmbedder",
                            "Failed to embed ${config.runtimeName} file: ${file.absolutePath}", e)
                    }
                }
            }
        }

        addDirRecursive(projectDir, "")
        logger.logKeyValue("${config.runtimeName}FilesEmbedded", fileCount)
        logger.logKeyValue("${config.runtimeName}TotalSize", "${totalSize / 1024} KB")

        return fileCount to totalSize
    }

    // ---- Pre-defined configs ----

    fun nodeJsConfig(): EmbedConfig = EmbedConfig(
        runtimeName = "nodejs",
        assetPrefix = "assets/nodejs_app",
        excludeDirs = setOf(".git", ".cache", ".next", ".nuxt", "__pycache__"),
        runtimeType = "nodejs"
    )

    fun phpConfig(): EmbedConfig = EmbedConfig(
        runtimeName = "phpApp",
        assetPrefix = "assets/php_app",
        excludeDirs = setOf("vendor", ".git", "node_modules", ".idea", "__pycache__"),
        runtimeType = "php"
    )

    fun pythonConfig(): EmbedConfig = EmbedConfig(
        runtimeName = "pythonApp",
        assetPrefix = "assets/python_app",
        excludeDirs = setOf("venv", ".venv", "__pycache__", ".git", "node_modules", ".mypy_cache", ".pytest_cache"),
        runtimeType = "python"
    )

    fun goConfig(): EmbedConfig = EmbedConfig(
        runtimeName = "goApp",
        assetPrefix = "assets/go_app",
        excludeDirs = setOf(".git", "vendor", "node_modules"),
        runtimeType = "go",
        fileHook = { zipOut, assetPath, file ->
            // Go binary is large; use streaming for executables
            if (file.canExecute() && file.length() > 10 * 1024 * 1024) {
                ZipUtils.writeEntryStoredStreaming(zipOut, assetPath, file)
                true
            } else {
                false // fall back to default text/binary logic
            }
        }
    )

    fun frontendConfig(): EmbedConfig = EmbedConfig(
        runtimeName = "frontend",
        assetPrefix = "assets/frontend_app",
        excludeDirs = setOf("node_modules", ".git", ".cache", "__pycache__", ".next", ".nuxt"),
        runtimeType = "nodejs"
    )

    /**
     * Embed Python standard library into assets (required for Python to function).
     * Separate from project files because it uses a different exclude list
     * and always compresses all files.
     */
    fun embedPythonStdlib(
        zipOut: ZipOutputStream,
        pythonLibDir: File,
        logger: BuildLogger
    ): Pair<Int, Long> {
        if (!pythonLibDir.exists() || !pythonLibDir.isDirectory) {
            logger.warn("Python standard library not found: ${pythonLibDir.absolutePath}")
            return 0 to 0L
        }

        logger.section("Embed Python Standard Library")
        var fileCount = 0
        var totalSize = 0L
        val excludeDirs = setOf("__pycache__", "test", "tests", "idle_test", "idlelib", "tkinter", "turtledemo", "turtle")

        fun addDir(dir: File, assetBasePath: String) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name in excludeDirs) return@forEach
                val entryPath = "$assetBasePath/${file.name}"
                if (file.isDirectory) {
                    addDir(file, entryPath)
                } else {
                    try {
                        ZipUtils.writeEntryDeflated(zipOut, entryPath, file.readBytes())
                        fileCount++
                        totalSize += file.length()
                    } catch (e: Exception) {
                        AppLogger.w("RuntimeAssetEmbedder", "Failed to embed Python lib file: ${file.name}", e)
                    }
                }
            }
        }

        addDir(pythonLibDir, "assets/python_runtime/lib")
        logger.logKeyValue("pythonRuntimeFiles", fileCount)
        logger.logKeyValue("pythonRuntimeSize", "${totalSize / 1024} KB")
        return fileCount to totalSize
    }
}
