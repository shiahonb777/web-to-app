package com.webtoapp.core.extension

import android.content.Context
import android.webkit.WebResourceResponse
import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayInputStream

/**
 * Chrome Extension intercept.
 *
 * intercept chrome-extension://{extId}/{path} URL request.
 * from Android assets/extensions/{extId}/ and .
 *
 * Supports resource type CSSJSJSONSVG etc .
 */
object ExtensionResourceInterceptor {

    private const val TAG = "ExtResInterceptor"
    private const val SCHEME = "chrome-extension"
    private const val ASSETS_BASE = "extensions"

    // MIME type.
    private val MIME_TYPES = mapOf(
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "mjs" to "application/javascript",
        "json" to "application/json",
        "xml" to "application/xml",
        "txt" to "text/plain",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "woff" to "font/woff",
        "woff2" to "font/woff2",
        "ttf" to "font/ttf",
        "otf" to "font/otf",
        "eot" to "application/vnd.ms-fontobject",
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "mp4" to "video/mp4",
        "webm" to "video/webm"
    )

    // MIME.
    private val TEXT_MIME_TYPES = setOf(
        "text/html", "text/css", "text/plain",
        "application/javascript", "application/json",
        "application/xml", "image/svg+xml"
    )

    /**
     * URL is as chrome-extension://.
     */
    fun isChromeExtensionUrl(url: String): Boolean {
        return url.startsWith("$SCHEME://")
    }

    /**
     * chrome-extension:// URL request.
     *
     * @param context Android Context
     * @param url chrome-extension:// URL.
     * @return WebResourceResponse or null.
     */
    fun intercept(context: Context, url: String): WebResourceResponse? {
        if (!isChromeExtensionUrl(url)) return null

        try {
            // URL: chrome-extension://{extId}/{path}.
            val withoutScheme = url.removePrefix("$SCHEME://")
            val slashIndex = withoutScheme.indexOf('/')
            if (slashIndex == -1) {
                AppLogger.w(TAG, "Invalid chrome-extension URL (no path): $url")
                return createEmptyResponse()
            }

            val extId = withoutScheme.substring(0, slashIndex)
            var resourcePath = withoutScheme.substring(slashIndex + 1)

            // query string fragment.
            resourcePath = resourcePath.split("?")[0].split("#")[0]
            // before.
            resourcePath = resourcePath.trimStart('/')

            if (extId.isBlank() || resourcePath.isBlank()) {
                AppLogger.w(TAG, "Invalid chrome-extension URL (empty extId or path): $url")
                return createEmptyResponse()
            }

            // Check.
            if (resourcePath.contains("..")) {
                AppLogger.w(TAG, "Path traversal attempt blocked: $url")
                return createEmptyResponse()
            }

            // asset: extensions/{extId}/{resourcePath}.
            val assetPath = "$ASSETS_BASE/$extId/$resourcePath"

            // from assets/.
            val assetResponse = loadAssetResource(context, assetPath, url)
            if (assetResponse != null) return assetResponse
            
            // Fallback: from filesDir/extensions/.
            return loadFileResource(context, extId, resourcePath, url)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error intercepting chrome-extension URL: $url", e)
            return createEmptyResponse()
        }
    }

    /**
     * from assets and WebResourceResponse.
     */
    private fun loadAssetResource(context: Context, assetPath: String, originalUrl: String): WebResourceResponse? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val mimeType = getMimeType(assetPath)
            val encoding = if (mimeType in TEXT_MIME_TYPES) "UTF-8" else null

            val headers = mutableMapOf<String, String>()
            headers["Access-Control-Allow-Origin"] = "*"
            headers["Cache-Control"] = "max-age=86400" // Note.

            AppLogger.d(TAG, "Serving extension resource (assets): $assetPath (MIME: $mimeType)")

            WebResourceResponse(
                mimeType,
                encoding,
                200,
                "OK",
                headers,
                inputStream
            )
        } catch (e: java.io.FileNotFoundException) {
            // Return null to allow fallback to filesDir
            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error loading extension resource: $assetPath", e)
            null
        }
    }
    
    /**
     * from filesDir/extensions/.
     *
     * use extension to filesDir/extensions/{extId}/.
     * extension ZIP manifest.json can in in .
     * when Check level .
     */
    private fun loadFileResource(context: Context, extId: String, resourcePath: String, originalUrl: String): WebResourceResponse? {
        val extensionsDir = java.io.File(context.filesDir, ASSETS_BASE)
        
        // 1.: filesDir/extensions/{extId}/{resourcePath}.
        val directFile = java.io.File(extensionsDir, "$extId/$resourcePath")
        if (directFile.exists() && directFile.isFile) {
            return createFileResponse(directFile, originalUrl)
        }
        
        // 2. Check extId level.
        val extDir = java.io.File(extensionsDir, extId)
        if (extDir.exists() && extDir.isDirectory) {
            extDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                val nestedFile = java.io.File(subDir, resourcePath)
                if (nestedFile.exists() && nestedFile.isFile) {
                    return createFileResponse(nestedFile, originalUrl)
                }
            }
        }
        
        // 3. extension extId.
        // use chromeExtId as.
        if (extensionsDir.exists()) {
            extensionsDir.listFiles()?.filter { it.isDirectory }?.forEach { parentDir ->
                val subDir = java.io.File(parentDir, extId)
                if (subDir.exists() && subDir.isDirectory) {
                    val file = java.io.File(subDir, resourcePath)
                    if (file.exists() && file.isFile) {
                        return createFileResponse(file, originalUrl)
                    }
                }
                // Check parentDir is extId.
            }
        }
        
        AppLogger.w(TAG, "Extension resource not found in assets or filesDir: $extId/$resourcePath (URL: $originalUrl)")
        return createEmptyResponse()
    }
    
    /**
     * from WebResourceResponse.
     */
    private fun createFileResponse(file: java.io.File, originalUrl: String): WebResourceResponse {
        val mimeType = getMimeType(file.name)
        val encoding = if (mimeType in TEXT_MIME_TYPES) "UTF-8" else null
        val headers = mutableMapOf<String, String>()
        headers["Access-Control-Allow-Origin"] = "*"
        headers["Cache-Control"] = "max-age=86400"
        
        AppLogger.d(TAG, "Serving extension resource (filesDir): ${file.absolutePath} (MIME: $mimeType)")
        
        return WebResourceResponse(
            mimeType,
            encoding,
            200,
            "OK",
            headers,
            java.io.FileInputStream(file)
        )
    }

    /**
     * extension Get MIME type.
     */
    private fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return MIME_TYPES[extension] ?: "application/octet-stream"
    }

    /**
     * WebResourceResponse.
     */
    private fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "No Content",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}