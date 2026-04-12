package com.webtoapp.core.cloud

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.auth.TokenManager
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

private const val CLOUD_API_TAG = "CloudApiClient"
private const val HTTP_CACHE_SIZE = 10L * 1024 * 1024

internal class CloudApiSupport(
    val tokenManager: TokenManager,
    context: Context? = null,
) {
    val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaType()

    private val httpCache = context?.let {
        try {
            okhttp3.Cache(File(it.cacheDir, "http_cache"), HTTP_CACHE_SIZE)
        } catch (_: Exception) {
            null
        }
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .apply { httpCache?.let { cache(it) } }
        .addInterceptor { chain ->
            val request = chain.request()
            val isIdempotent = request.method in listOf("GET", "HEAD")
            var lastException: java.io.IOException? = null
            val maxRetries = if (isIdempotent) 2 else 0
            for (attempt in 0..maxRetries) {
                try {
                    return@addInterceptor chain.proceed(request)
                } catch (e: java.io.IOException) {
                    lastException = e
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(500L * (attempt + 1))
                        } catch (_: InterruptedException) {
                        }
                    }
                }
            }
            throw lastException!!
        }
        .build()

    private val errorParser = CloudErrorParser()
    private val requestExecutor = CloudRequestExecutor(tokenManager, client, jsonMediaType, errorParser)

    suspend fun <T> authRequest(block: suspend (token: String) -> AuthResult<T>): AuthResult<T> =
        requestExecutor.authRequest(block)

    fun parseError(body: String, statusCode: Int = 0): String = errorParser.parse(body, statusCode)
}

internal class CloudRequestExecutor(
    private val tokenManager: TokenManager,
    private val client: OkHttpClient,
    private val jsonMediaType: MediaType,
    private val errorParser: CloudErrorParser,
) {
    private val refreshMutex = Mutex()

    suspend fun <T> authRequest(block: suspend (token: String) -> AuthResult<T>): AuthResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getAccessToken()
                    ?: return@withContext AuthResult.Error("未登录，请先登录")
                val result = block(token)
                if (result is AuthResult.Error && result.message.contains("HTTP 401")) {
                    refreshMutex.withLock {
                        val currentToken = tokenManager.getAccessToken()
                        if (currentToken != null && currentToken != token) {
                            return@withContext block(currentToken)
                        }
                        val refresh = tokenManager.getRefreshToken()
                            ?: return@withContext AuthResult.Error("登录已过期，请重新登录")
                        val refreshBody = JsonObject().apply { addProperty("refresh_token", refresh) }
                        val refreshReq = Request.Builder()
                            .url("${CloudApiClient.BASE_URL}/api/v1/auth/refresh")
                            .post(refreshBody.toString().toRequestBody(jsonMediaType))
                            .build()
                        val refreshResp = client.newCall(refreshReq).execute()
                        if (refreshResp.isSuccessful) {
                            val responseBody = refreshResp.body?.string() ?: ""
                            val refreshJson = JsonParser.parseString(responseBody).asJsonObject
                            val refreshData = refreshJson.getAsJsonObject("data")
                            val newToken = refreshData.get("access_token").asString
                            tokenManager.saveTokens(
                                newToken,
                                refreshData.get("refresh_token").asString,
                            )
                            block(newToken)
                        } else {
                            tokenManager.clearTokens()
                            AuthResult.Error("登录已过期，请重新登录")
                        }
                    }
                } else {
                    result
                }
            } catch (e: Exception) {
                AppLogger.e(CLOUD_API_TAG, "API request failed", e)
                AuthResult.Error("网络连接失败: ${e.message}")
            }
        }
}

internal class CloudErrorParser {
    fun parse(body: String, statusCode: Int = 0): String = try {
        val json = JsonParser.parseString(body).asJsonObject
        val msg = json.get("detail")?.asString ?: json.get("message")?.asString ?: "操作失败"
        if (statusCode == 401) "HTTP 401: $msg" else msg
    } catch (_: Exception) {
        if (statusCode > 0) "HTTP $statusCode" else "操作失败"
    }
}
