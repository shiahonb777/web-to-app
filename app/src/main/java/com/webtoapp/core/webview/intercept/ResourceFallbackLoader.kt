package com.webtoapp.core.webview.intercept

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.webtoapp.core.crypto.SecureAssetLoader
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.webview.WebViewManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

internal class ResourceFallbackLoader(
    private val context: Context,
    private val cleartextProxyClient: OkHttpClient = WebViewManager.cleartextProxyClient
) {
    fun fetchCleartextResource(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val method = request.method?.uppercase() ?: "GET"
            val body = if (method == "POST" || method == "PUT" || method == "PATCH") {
                ByteArray(0).toRequestBody(null)
            } else {
                null
            }

            val okRequestBuilder = Request.Builder()
                .url(url)
                .method(method, body)

            request.requestHeaders.forEach { (key, value) ->
                if (key.lowercase() !in WebViewManager.SKIP_HEADERS) {
                    okRequestBuilder.addHeader(key, value)
                }
            }

            val okResponse: Response = cleartextProxyClient.newCall(okRequestBuilder.build()).execute()
            val responseCode = okResponse.code
            val responseMessage = okResponse.message.ifBlank { "OK" }
            val contentType = okResponse.header("Content-Type") ?: "application/octet-stream"
            val mimeType = contentType.substringBefore(';').ifBlank { "application/octet-stream" }
            val charset = contentType.substringAfter("charset=", "").ifBlank { "UTF-8" }
            val responseHeaders = mutableMapOf<String, String>()
            okResponse.headers.forEach { (name, value) ->
                responseHeaders[name] = value
            }
            val inputStream = okResponse.body?.byteStream() ?: ByteArrayInputStream(ByteArray(0))

            AppLogger.d("WebViewManager", "CleartextProxy fetched: $url -> $responseCode")
            WebResourceResponse(
                mimeType,
                charset,
                responseCode,
                responseMessage,
                responseHeaders,
                inputStream
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "CleartextProxy failed: ${request.url}", e)
            null
        }
    }

    fun fetchWithCrossOriginHeaders(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val connection = URL(url).openConnection() as HttpURLConnection

            request.requestHeaders.forEach { (key, value) ->
                if (key.lowercase() !in WebViewManager.SKIP_HEADERS) {
                    connection.setRequestProperty(key, value)
                }
            }

            connection.requestMethod = request.method ?: "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            val mimeType = connection.contentType?.substringBefore(';') ?: "application/octet-stream"
            val encoding = connection.contentEncoding ?: "UTF-8"
            val responseHeaders = mutableMapOf<String, String>()
            connection.headerFields.forEach { (key, values) ->
                if (key != null && !values.isNullOrEmpty()) {
                    responseHeaders[key] = values.first()
                }
            }
            responseHeaders["Cross-Origin-Opener-Policy"] = "same-origin"
            responseHeaders["Cross-Origin-Embedder-Policy"] = "require-corp"
            responseHeaders["Cross-Origin-Resource-Policy"] = "cross-origin"

            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: ByteArrayInputStream(ByteArray(0))
            }

            AppLogger.d("WebViewManager", "CrossOriginIsolation fetch: $url -> $responseCode")
            WebResourceResponse(
                mimeType,
                encoding,
                responseCode,
                connection.responseMessage ?: "OK",
                responseHeaders,
                inputStream
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "CrossOriginIsolation fetch failed: ${request.url}", e)
            null
        }
    }

    fun loadEncryptedAsset(assetPath: String): WebResourceResponse? {
        return try {
            val secureLoader = SecureAssetLoader.getInstance(context)
            if (!secureLoader.assetExists(assetPath)) {
                AppLogger.d("WebViewManager", "Resource not found: $assetPath")
                return null
            }

            val data = secureLoader.loadAsset(assetPath)
            val mimeType = getMimeType(assetPath)
            val encoding = if (isTextMimeType(mimeType)) "UTF-8" else null

            AppLogger.d("WebViewManager", "Load resource: $assetPath (${data.size} bytes, $mimeType)")
            WebResourceResponse(
                mimeType,
                encoding,
                ByteArrayInputStream(data)
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to load resource: $assetPath", e)
            null
        }
    }

    fun loadLocalResource(localPath: String): WebResourceResponse? {
        return try {
            val file = File(localPath)
            if (!file.exists() || !file.isFile) {
                AppLogger.w("WebViewManager", "Local file not found: $localPath")
                return null
            }

            WebResourceResponse(
                getMimeType(localPath),
                "UTF-8",
                FileInputStream(file)
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Error loading local resource: $localPath", e)
            null
        }
    }

    fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return WebViewManager.MIME_TYPE_MAP[extension] ?: "application/octet-stream"
    }

    fun isTextMimeType(mimeType: String): Boolean {
        return mimeType in WebViewManager.TEXT_MIME_TYPES
    }
}
