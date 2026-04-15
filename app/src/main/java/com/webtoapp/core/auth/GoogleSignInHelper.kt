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
 * Google Sign-In
 * 
 * Login flow:
 * 1. Try Credential Manager first.
 * 2. Fall back to OAuth 2.0 Web flow when needed.
 */
object GoogleSignInHelper {

    private const val TAG = "GoogleSignInHelper"

    /**
     * Web Client ID — id_token
     * 
     * Use the Web Client ID here, not the Android Client ID.
     * The Web Client ID is required for server-side id_token validation.
     */
    const val WEB_CLIENT_ID = "112374364944-34pvgaljamv9imq321bthgccqggf54a6.apps.googleusercontent.com"

    /**
     * OAuth 2.0 URL
     */
    private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"

    /**
     * OAuth 2.0 Token URL —
     * 
     * Token exchange is performed on the client in this flow.
     */
    private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"

    /**
     * OAuth redirect URI —
     * 
     * Redirects through server callback, then back to app scheme.
     */
    private val REDIRECT_URI = "${AuthApiClient.BASE_URL}/api/v1/auth/google/callback"

    /**
     * Web OAuth
     */
    private var pendingOAuthCallback: ((GoogleSignInResult) -> Unit)? = null
    private var pendingOAuthState: String? = null
    private var pendingOAuthCodeVerifier: String? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Google login —
     * 
     * Use Credential Manager first, then fall back to Web OAuth.
     * 
     * @return result
     */
    suspend fun getGoogleIdToken(context: Context): GoogleSignInResult {
        // Step 1: Credential Manager
        val credentialResult = tryCredentialManager(context)
        
        if (credentialResult is GoogleSignInResult.Success) {
            return credentialResult
        }
        
        if (credentialResult is GoogleSignInResult.Cancelled) {
            return credentialResult
        }
        
        // Step 2: Credential Manager （），fallback Web OAuth
        AppLogger.i(TAG, "Credential Manager unavailable, falling back to Web OAuth flow")
        return startWebOAuthFlow(context)
    }

    /**
     * Credential Manager Google ID Token
     */
    private suspend fun tryCredentialManager(context: Context): GoogleSignInResult {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false) // Google
                .setAutoSelectEnabled(true) // ，
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = request
            )

            // Google ID Token
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
     * Web OAuth 2.0
     * 
     * Open Google sign-in in Chrome Custom Tabs and handle redirect callback.
     */
    private suspend fun startWebOAuthFlow(context: Context): GoogleSignInResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                // PKCE code_verifier code_challenge
                val codeVerifier = generateCodeVerifier()
                val codeChallenge = generateCodeChallenge(codeVerifier)
                val state = generateRandomState()

                pendingOAuthState = state
                pendingOAuthCodeVerifier = codeVerifier

                // Google OAuth URL
                val authUri = Uri.parse(GOOGLE_AUTH_URL).buildUpon()
                    .appendQueryParameter("client_id", WEB_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", REDIRECT_URI)
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("scope", "openid email profile")
                    .appendQueryParameter("state", state)
                    .appendQueryParameter("code_challenge", codeChallenge)
                    .appendQueryParameter("code_challenge_method", "S256")
                    .appendQueryParameter("access_type", "offline")
                    .appendQueryParameter("prompt", "select_account") // Note.
                    .build()

                // Note.
                pendingOAuthCallback = { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                // Chrome Custom Tab ，
                try {
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    customTabsIntent.launchUrl(context, authUri)
                } catch (e: Exception) {
                    // Custom Tab ，
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
     * OAuth URI
     * 
     * Call from Activity onCreate/onNewIntent to process OAuth callback URI.
     * 
     * @return result
     */
    suspend fun handleOAuthCallback(uri: Uri): Boolean {
        val scheme = uri.scheme ?: return false
        val host = uri.host ?: return false

        // OAuth
        if (scheme != "com.webtoapp" || host != "oauth2callback") {
            return false
        }

        AppLogger.i(TAG, "Received OAuth callback")

        // Note.
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

        // state （ CSRF ）
        val state = uri.getQueryParameter("state")
        if (state != pendingOAuthState) {
            AppLogger.e(TAG, "OAuth state mismatch!")
            pendingOAuthCallback?.invoke(GoogleSignInResult.Error("安全验证失败，请重试"))
            cleanup()
            return true
        }

        // authorization code
        val code = uri.getQueryParameter("code")
        if (code == null) {
            pendingOAuthCallback?.invoke(GoogleSignInResult.Error("Google 登录失败: 未收到授权码"))
            cleanup()
            return true
        }

        // authorization code id_token
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
     * Google id_token
     * 
     * Exchange authorization code for id_token using PKCE.
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
     * Intent OAuth
     */
    fun isOAuthCallback(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        return uri.scheme == "com.webtoapp" && uri.host == "oauth2callback"
    }

    // ─── PKCE Helpers ───

    /**
     * code_verifier (43-128 )
     */
    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, 
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    /**
     * code_challenge = BASE64URL(SHA-256(code_verifier))
     */
    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray())
        return android.util.Base64.encodeToString(bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    /**
     * state （ CSRF）
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
 * Google login
 */
sealed class GoogleSignInResult {
    /** Google id_token（ Credential Manager Web OAuth） */
    data class Success(val idToken: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
