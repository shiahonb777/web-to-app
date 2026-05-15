package com.webtoapp.core.extension

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.File

internal object ChromeExtensionResourceLoader {

    private const val TAG = "ChromeExtResLoader"
    private const val EXTENSIONS_DIR = "extensions"

    internal fun loadResourceBundle(
        context: Context,
        extensionId: String,
        paths: List<String>,
        isCss: Boolean
    ): String {
        if (paths.isEmpty()) return ""
        val parts = mutableListOf<String>()
        paths.forEach { rawPath ->
            val path = normalizeResourcePath(rawPath) ?: return@forEach
            val text = loadTextResource(context, extensionId, path)
            if (text == null) {
                AppLogger.w(TAG, "Extension resource not found: $extensionId/$path")
                return@forEach
            }
            val banner = if (isCss) {
                "/* === $path === */"
            } else {
                "// === $path ==="
            }
            parts += "$banner\n$text"
        }
        return parts.joinToString("\n\n")
    }

    internal fun loadTextResource(
        context: Context,
        extensionId: String,
        resourcePath: String
    ): String? {
        val normalizedPath = normalizeResourcePath(resourcePath) ?: return null
        loadAssetText(context, extensionId, normalizedPath)?.let { return it }

        val file = findExtensionResourceFile(context, extensionId, normalizedPath) ?: return null
        return try {
            file.readText()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to read extension file: $extensionId/$normalizedPath", e)
            null
        }
    }

    internal fun findExtensionResourceFile(
        context: Context,
        extensionId: String,
        resourcePath: String
    ): File? {
        val normalizedPath = normalizeResourcePath(resourcePath) ?: return null
        val extensionsDir = File(context.filesDir, EXTENSIONS_DIR)
        val directFile = File(extensionsDir, "$extensionId/$normalizedPath")
        if (directFile.exists() && directFile.isFile) return directFile

        val extDir = File(extensionsDir, extensionId)
        if (extDir.exists() && extDir.isDirectory) {
            extDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                val nestedFile = File(subDir, normalizedPath)
                if (nestedFile.exists() && nestedFile.isFile) return nestedFile
            }
        }

        if (extensionsDir.exists()) {
            extensionsDir.listFiles()?.filter { it.isDirectory }?.forEach { parentDir ->
                val nestedRoot = File(parentDir, extensionId)
                if (!nestedRoot.exists() || !nestedRoot.isDirectory) return@forEach
                val nestedFile = File(nestedRoot, normalizedPath)
                if (nestedFile.exists() && nestedFile.isFile) return nestedFile
            }
        }

        return null
    }

    internal fun normalizeResourcePath(rawPath: String): String? {
        val path = rawPath
            .substringBefore('?')
            .substringBefore('#')
            .trim()
            .trimStart('/')
        return path.takeIf { it.isNotEmpty() && !it.contains("..") }
    }

    private fun loadAssetText(context: Context, extensionId: String, resourcePath: String): String? {
        return try {
            context.assets.open("$EXTENSIONS_DIR/$extensionId/$resourcePath")
                .bufferedReader()
                .use { it.readText() }
        } catch (_: java.io.FileNotFoundException) {
            null
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to read extension asset: $extensionId/$resourcePath", e)
            null
        }
    }
}
