package com.webtoapp.core.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Google Sign-In 辅助工具
 * 
 * 完整的 Google 登录流程：
 * 1. 首先尝试 Credential Manager API（设备上已有 Google 账号时，体验最佳）
 * 2. 如果 Credential Manager 不可用（NoCredentialException），自动 fallback 到
 *    OAuth 2.0 Web 授权流程（通过 Chrome Custom Tab 打开 Google 登录页面）
 * 
 * 这样即使设备没有登录 Google 账号，用户也能通过浏览器正常登录/注册。
 */
object GoogleSignInHelper {

    private const val TAG = "GoogleSignInHelper"

    /**
     * 服务器端 Web Client ID — 用于请求 id_token
     * 
     * 注意：这里用的是 **Web** Client ID，不是 Android Client ID。
     * Android Client ID 用于 Google Cloud Console 识别应用（通过包名+SHA1），
     * 而 Web Client ID 用于服务器端验证 id_token。
     */
    const val WEB_CLIENT_ID = "112374364944-34pvgaljamv9imq321bthgccqggf54a6.apps.googleusercontent.com"

    /**
     * OAuth 2.0 授权 URL
     */
    private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"

    /**
     * OAuth 2.0 Token 交换 URL — 客户端直接调用
     * 
     * 手机能连 Google，所以 code exchange 在客户端做，
     * 服务器在中国大陆不一定能连 Google。
     */
    private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"

    /**
     * OAuth 回调 redirect URI — 指向服务器中转端点
     * 
     * Google Cloud Console 只接受 https:// 的 redirect URI，
     * 所以先重定向到服务器，服务器再 302 跳转到 App 的自定义 scheme。
     * 
     * 流程：Google → 服务器 /auth/google/callback → 302 → com.webtoapp:/oauth2callback → App
     */
    private val REDIRECT_URI = "${AuthApiClient.BASE_URL}/api/v1/auth/google/callback"

    /**
     * Web OAuth 流程状态
     */
    private var pendingOAuthCallback: ((GoogleSignInResult) -> Unit)? = null
    private var pendingOAuthState: String? = null
    private var pendingOAuthCodeVerifier: String? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 唤起 Google 登录 — 自动选择最佳方式
     * 
     * 优先使用 Credential Manager（体验更好），
     * 失败时自动 fallback 到 OAuth Web 流程。
     * 
     * @return id_token 字符串，或错误信息
     */
    suspend fun getGoogleIdToken(context: Context): GoogleSignInResult {
        // Step 1: 尝试 Credential Manager
        val credentialResult = tryCredentialManager(context)
        
        if (credentialResult is GoogleSignInResult.Success) {
            return credentialResult
        }
        
        if (credentialResult is GoogleSignInResult.Cancelled) {
            return credentialResult
        }
        
        // Step 2: Credential Manager 失败（没有可用账号等），fallback 到 Web OAuth
        AppLogger.i(TAG, "Credential Manager unavailable, falling back to Web OAuth flow")
        return startWebOAuthFlow(context)
    }

    /**
     * 尝试使用 Credential Manager 获取 Google ID Token
     */
    private suspend fun tryCredentialManager(context: Context): GoogleSignInResult {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false) // 显示所有 Google 账号
                .setAutoSelectEnabled(true) // 如果只有一个账号，自动选择
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = request
            )

            // 提取 Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken

            AppLogger.i(TAG, "Google Sign-In via Credential Manager successful")
            GoogleSignInResult.Success(idToken)

        } catch (e: GetCredentialCancellationException) {
            AppLogger.i(TAG, "Google Sign-In cancelled by user")
            GoogleSignInResult.Cancelled

        } catch (e: NoCredentialException) {
            AppLogger.w(TAG, "No credential available, will fallback to web OAuth")
            GoogleSignInResult.Error("fallback_to_web")

        } catch (e: GetCredentialException) {
            AppLogger.w(TAG, "Credential Manager failed: ${e.type} - ${e.message}, will fallback")
            GoogleSignInResult.Error("fallback_to_web")

        } catch (e: Exception) {
            AppLogger.e(TAG, "Credential Manager unexpected error, will fallback", e)
            GoogleSignInResult.Error("fallback_to_web")
        }
    }

    /**
     * 启动 Web OAuth 2.0 流程
     * 
     * 使用 Chrome Custom Tab 打开 Google 登录页面，
     * 用户登录后通过 redirect URI 回调。
     */
    private suspend fun startWebOAuthFlow(context: Context): GoogleSignInResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                // 生成 PKCE code_verifier 和 code_challenge
                val codeVerifier = generateCodeVerifier()
                val codeChallenge = generateCodeChallenge(codeVerifier)
                val state = generateRandomState()

                pendingOAuthState = state
                pendingOAuthCodeVerifier = codeVerifier

                // 构建 Google OAuth 授权 URL
                val authUri = Uri.parse(GOOGLE_AUTH_URL).buildUpon()
                    .appendQueryParameter("client_id", WEB_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", REDIRECT_URI)
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("scope", "openid email profile")
                    .appendQueryParameter("state", state)
                    .appendQueryParameter("code_challenge", codeChallenge)
                    .appendQueryParameter("code_challenge_method", "S256")
                    .appendQueryParameter("access_type", "offline")
                    .appendQueryParameter("prompt", "select_account") // 总是让用户选择账号
                    .build()

                // 设置回调
                pendingOAuthCallback = { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                // 使用 Chrome Custom Tab 打开，体验最好
                try {
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    customTabsIntent.launchUrl(context, authUri)
                } catch (e: Exception) {
                    // 如果 Custom Tab 不可用，用普通浏览器
                    AppLogger.w(TAG, "Custom Tab unavailable, using default browser")
                    val browserIntent = Intent(Intent.ACTION_VIEW, authUri)
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(browserIntent)
                }

                AppLogger.i(TAG, "Web OAuth flow started")

            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start Web OAuth flow", e)
                if (continuation.isActive) {
                    continuation.resume(
                        GoogleSignInResult.Error("无法启动 Google 登录页面: ${e.message}")
                    )
                }
            }

            continuation.invokeOnCancellation {
                pendingOAuthCallback = null
                pendingOAuthState = null
                pendingOAuthCodeVerifier = null
            }
        }
    }

    /**
     * 处理 OAuth 回调 URI
     * 
     * 在 Activity 的 onNewIntent / onCreate 中调用此方法，
     * 当用户从 Google 登录页面返回时会带上 authorization code。
     * 
     * @return true 如果成功处理了 OAuth 回调
     */
    suspend fun handleOAuthCallback(uri: Uri): Boolean {
        val scheme = uri.scheme ?: return false
        val host = uri.host ?: return false

        // 检查是否是我们的 OAuth 回调
        if (scheme != "com.webtoapp" || host != "oauth2callback") {
            return false
        }

        AppLogger.i(TAG, "Received OAuth callback")

        // 检查错误
        val error = uri.getQueryParameter("error")
        if (error != null) {
            AppLogger.w(TAG, "OAuth error: $error")
            val result = if (error == "access_denied") {
                GoogleSignInResult.Cancelled
            } else {
                GoogleSignInResult.Error("Google 登录失败: $error")
            }
            pendingOAuthCallback?.invoke(result)
            cleanup()
            return true
        }

        // 验证 state 参数（防 CSRF 攻击）
        val state = uri.getQueryParameter("state")
        if (state != pendingOAuthState) {
            AppLogger.e(TAG, "OAuth state mismatch!")
            pendingOAuthCallback?.invoke(GoogleSignInResult.Error("安全验证失败，请重试"))
            cleanup()
            return true
        }

        // 获取 authorization code
        val code = uri.getQueryParameter("code")
        if (code == null) {
            pendingOAuthCallback?.invoke(GoogleSignInResult.Error("Google 登录失败: 未收到授权码"))
            cleanup()
            return true
        }

        // 用 authorization code 换 id_token
        val codeVerifier = pendingOAuthCodeVerifier
        val callback = pendingOAuthCallback
        cleanup()

        if (codeVerifier == null || callback == null) {
            return true
        }

        val tokenResult = exchangeCodeForToken(code, codeVerifier)
        callback.invoke(tokenResult)
        return true
    }

    /**
     * 用授权码直接向 Google 交换 id_token
     * 
     * 手机能连 Google，所以 code exchange 在客户端做。
     * 服务器在中国大陆连不上 Google，所以不经过服务器。
     * 
     * 用 PKCE 流程，不需要 client_secret。
     */
    private suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String
    ): GoogleSignInResult = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("code", code)
                .add("client_id", WEB_CLIENT_ID)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code")
                .add("code_verifier", codeVerifier)
                .build()

            val request = Request.Builder()
                .url(GOOGLE_TOKEN_URL)
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                AppLogger.e(TAG, "Google token exchange failed: ${response.code} - $body")
                return@withContext GoogleSignInResult.Error("Google 登录失败: 令牌交换失败")
            }

            val json = JSONObject(body)
            val idToken = json.optString("id_token")

            if (idToken.isBlank()) {
                AppLogger.e(TAG, "No id_token in Google response")
                return@withContext GoogleSignInResult.Error("Google 登录失败: 未获取到身份令牌")
            }

            AppLogger.i(TAG, "Web OAuth token exchange successful")
            GoogleSignInResult.Success(idToken)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Google token exchange error", e)
            GoogleSignInResult.Error("Google 登录失败: ${e.message}")
        }
    }

    /**
     * 检查给定的 Intent 是否包含 OAuth 回调
     */
    fun isOAuthCallback(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        return uri.scheme == "com.webtoapp" && uri.host == "oauth2callback"
    }

    // ─── PKCE Helpers ───

    /**
     * 生成随机 code_verifier (43-128 字符)
     */
    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, 
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    /**
     * 生成 code_challenge = BASE64URL(SHA-256(code_verifier))
     */
    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray())
        return android.util.Base64.encodeToString(bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    /**
     * 生成随机 state 参数（防 CSRF）
     */
    private fun generateRandomState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    private fun cleanup() {
        pendingOAuthCallback = null
        pendingOAuthState = null
        pendingOAuthCodeVerifier = null
    }
}

/**
 * Google 登录结果
 */
sealed class GoogleSignInResult {
    /** 成功获取到 Google id_token（无论来自 Credential Manager 还是 Web OAuth） */
    data class Success(val idToken: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
