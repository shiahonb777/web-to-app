package com.webtoapp.core.extension

import android.content.Context
import android.webkit.WebResourceResponse
import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayInputStream

/**
 * Chrome Extension 资源拦截器
 * 
 * 拦截 chrome-extension://{extId}/{path} URL 请求，
 * 从 Android assets/extensions/{extId}/ 目录读取并返回真实文件。
 * 
 * 支持的资源类型：图片、CSS、JS、JSON、SVG、字体等。
 */
object ExtensionResourceInterceptor {

    private const val TAG = "ExtResInterceptor"
    private const val SCHEME = "chrome-extension"
    private const val ASSETS_BASE = "extensions"

    // MIME type 映射表
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

    // 文本类型 MIME（用于设定 encoding）
    private val TEXT_MIME_TYPES = setOf(
        "text/html", "text/css", "text/plain",
        "application/javascript", "application/json",
        "application/xml", "image/svg+xml"
    )

    /**
     * 判断 URL 是否为 chrome-extension:// 协议
     */
    fun isChromeExtensionUrl(url: String): Boolean {
        return url.startsWith("$SCHEME://")
    }

    /**
     * 处理 chrome-extension:// URL 请求
     * 
     * @param context Android Context
     * @param url 完整的 chrome-extension:// URL
     * @return WebResourceResponse 或 null（如果资源不存在）
     */
    fun intercept(context: Context, url: String): WebResourceResponse? {
        if (!isChromeExtensionUrl(url)) return null

        try {
            // 解析 URL: chrome-extension://{extId}/{path}
            val withoutScheme = url.removePrefix("$SCHEME://")
            val slashIndex = withoutScheme.indexOf('/')
            if (slashIndex == -1) {
                AppLogger.w(TAG, "Invalid chrome-extension URL (no path): $url")
                return createEmptyResponse()
            }

            val extId = withoutScheme.substring(0, slashIndex)
            var resourcePath = withoutScheme.substring(slashIndex + 1)

            // 移除 query string 和 fragment
            resourcePath = resourcePath.split("?")[0].split("#")[0]
            // 移除前导斜杠
            resourcePath = resourcePath.trimStart('/')

            if (extId.isBlank() || resourcePath.isBlank()) {
                AppLogger.w(TAG, "Invalid chrome-extension URL (empty extId or path): $url")
                return createEmptyResponse()
            }

            // 安全检查：防止路径遍历
            if (resourcePath.contains("..")) {
                AppLogger.w(TAG, "Path traversal attempt blocked: $url")
                return createEmptyResponse()
            }

            // 构建 asset 路径: extensions/{extId}/{resourcePath}
            val assetPath = "$ASSETS_BASE/$extId/$resourcePath"

            // 优先从 assets/ 加载（内置扩展）
            val assetResponse = loadAssetResource(context, assetPath, url)
            if (assetResponse != null) return assetResponse
            
            // Fallback: 从 filesDir/extensions/ 加载（用户导入的扩展）
            return loadFileResource(context, extId, resourcePath, url)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error intercepting chrome-extension URL: $url", e)
            return createEmptyResponse()
        }
    }

    /**
     * 从 assets 加载资源并创建 WebResourceResponse
     */
    private fun loadAssetResource(context: Context, assetPath: String, originalUrl: String): WebResourceResponse? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val mimeType = getMimeType(assetPath)
            val encoding = if (mimeType in TEXT_MIME_TYPES) "UTF-8" else null

            // 构建响应头
            val headers = mutableMapOf<String, String>()
            headers["Access-Control-Allow-Origin"] = "*"
            headers["Cache-Control"] = "max-age=86400" // 缓存 1 天

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
     * 从 filesDir/extensions/ 加载资源（用户导入的 Chrome 扩展）
     * 
     * 用户导入的扩展解压到 filesDir/extensions/{extId}/
     * 如果扩展 ZIP 有嵌套目录，manifest.json 可能在子目录中，
     * 因此需要同时检查直接路径和一级子目录路径。
     */
    private fun loadFileResource(context: Context, extId: String, resourcePath: String, originalUrl: String): WebResourceResponse? {
        val extensionsDir = java.io.File(context.filesDir, ASSETS_BASE)
        
        // 1. 直接路径: filesDir/extensions/{extId}/{resourcePath}
        val directFile = java.io.File(extensionsDir, "$extId/$resourcePath")
        if (directFile.exists() && directFile.isFile) {
            return createFileResponse(directFile, originalUrl)
        }
        
        // 2. 检查 extId 目录下的一级子目录（处理嵌套 ZIP 结构）
        val extDir = java.io.File(extensionsDir, extId)
        if (extDir.exists() && extDir.isDirectory) {
            extDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                val nestedFile = java.io.File(subDir, resourcePath)
                if (nestedFile.exists() && nestedFile.isFile) {
                    return createFileResponse(nestedFile, originalUrl)
                }
            }
        }
        
        // 3. 扫描所有扩展目录，查找包含此 extId 的子目录
        // 适用于 chromeExtId 为子目录名称的情况
        if (extensionsDir.exists()) {
            extensionsDir.listFiles()?.filter { it.isDirectory }?.forEach { parentDir ->
                val subDir = java.io.File(parentDir, extId)
                if (subDir.exists() && subDir.isDirectory) {
                    val file = java.io.File(subDir, resourcePath)
                    if (file.exists() && file.isFile) {
                        return createFileResponse(file, originalUrl)
                    }
                }
                // 也检查 parentDir 直接就是 extId 匹配的情况（已在步骤1处理）
            }
        }
        
        AppLogger.w(TAG, "Extension resource not found in assets or filesDir: $extId/$resourcePath (URL: $originalUrl)")
        return createEmptyResponse()
    }
    
    /**
     * 从本地文件创建 WebResourceResponse
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
     * 根据文件扩展名获取 MIME type
     */
    private fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return MIME_TYPES[extension] ?: "application/octet-stream"
    }

    /**
     * 创建空的 WebResourceResponse（用于 404 等情况）
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
