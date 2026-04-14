package com.webtoapp.core.auth

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cloud auth API client
 * 
 * API paths: /api/v1/auth/ and /api/v1/user/
 */
class AuthApiClient(private val tokenManager: TokenManager) {

    companion object {
        // API URL
        const val BASE_URL = "https://api.shiaho.sbs"
        private const val TAG = "AuthApiClient"
    }

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // ─── Auth API ───

    /**
     * Send email verification code
     */
    suspend fun sendVerificationCode(email: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("email", email)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/send-code")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                AuthResult.Success(json.get("message")?.asString ?: "验证码已发送")
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Send verification code failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    /**
     * User registration
     */
    suspend fun register(
        email: String,
        username: String,
        password: String,
        verificationCode: String
    ): AuthResult<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("email", email)
                addProperty("username", username)
                addProperty("password", password)
                addProperty("verification_code", verificationCode)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/register")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val user = parseUserProfile(data.getAsJsonObject("user"))
                val tokens = data.getAsJsonObject("tokens")
                val loginResponse = LoginResponse(
                    user = user,
                    accessToken = tokens.get("access_token").asString,
                    refreshToken = tokens.get("refresh_token").asString,
                    expiresIn = tokens.get("expires_in").asInt
                )
                AuthResult.Success(loginResponse)
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Register failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    /**
     * User login (username or email)
     */
    suspend fun login(
        account: String,
        password: String
    ): AuthResult<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val deviceId = tokenManager.getDeviceId()
            val body = JsonObject().apply {
                addProperty("account", account)
                addProperty("password", password)
                addProperty("device_id", deviceId)
                addProperty("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
                addProperty("device_os", "Android ${Build.VERSION.RELEASE}")
                addProperty("app_version", getAppVersion())
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/login")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val user = parseUserProfile(data.getAsJsonObject("user"))
                val tokens = data.getAsJsonObject("tokens")
                val loginResponse = LoginResponse(
                    user = user,
                    accessToken = tokens.get("access_token").asString,
                    refreshToken = tokens.get("refresh_token").asString,
                    expiresIn = tokens.get("expires_in").asInt
                )
                AuthResult.Success(loginResponse)
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Login failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    /**
     * Refresh token
     */
    suspend fun refreshToken(refreshToken: String): AuthResult<TokenPair> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("refresh_token", refreshToken)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/refresh")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val tokenPair = TokenPair(
                    accessToken = data.get("access_token").asString,
                    refreshToken = data.get("refresh_token").asString,
                    expiresIn = data.get("expires_in").asInt
                )
                AuthResult.Success(tokenPair)
            } else {
                AuthResult.Error("Token 刷新失败")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Refresh token failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    // ─── User API ───

    /**
     * Get user profile (token required)
     */
    suspend fun getProfile(): AuthResult<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext AuthResult.Error("未登录")

            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/profile")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val profile = parseUserProfile(data)
                AuthResult.Success(profile)
            } else if (response.code == 401) {
                // Token ，
                val refreshResult = tryRefreshAndRetry { getProfile() }
                refreshResult
            } else {
                AuthResult.Error("获取用户资料失败")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Get profile failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(
        username: String? = null,
        avatarUrl: String? = null
    ): AuthResult<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext AuthResult.Error("未登录")

            val body = JsonObject().apply {
                username?.let { addProperty("username", it) }
                avatarUrl?.let { addProperty("avatar_url", it) }
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/profile")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val profile = parseUserProfile(data)
                AuthResult.Success(profile)
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Update profile failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    /**
     * Get Pro status
     */
    suspend fun getProStatus(): AuthResult<ProStatus> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext AuthResult.Error("未登录")

            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/pro-status")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val proStatus = ProStatus(
                    isPro = data.get("is_pro")?.asBoolean ?: false,
                    proPlan = data.get("pro_plan")?.asString ?: "free",
                    isActive = data.get("is_active")?.asBoolean ?: false,
                    daysRemaining = data.get("days_remaining")?.asInt
                )
                AuthResult.Success(proStatus)
            } else {
                AuthResult.Error("获取会员状态失败")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Get pro status failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    // ─── Logout (Server-side) ───

    /**
     * Logout (invalidate token on server)
     */
    suspend fun logout(deviceId: String? = null): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
            val body = JsonObject().apply {
                deviceId?.let { addProperty("device_id", it) }
            }
            val reqBuilder = Request.Builder()
                .url("$BASE_URL/api/v1/auth/logout")
                .post(body.toString().toRequestBody(jsonMediaType))
            token?.let { reqBuilder.header("Authorization", "Bearer $it") }

            val response = client.newCall(reqBuilder.build()).execute()
            // Always clear local tokens regardless of server response
            tokenManager.clearTokens()
            AuthResult.Success("已退出登录")
        } catch (e: Exception) {
            tokenManager.clearTokens()
            AuthResult.Success("已退出登录")
        }
    }

    /**
     * Logout all devices
     */
    suspend fun logoutAll(): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
            val reqBuilder = Request.Builder()
                .url("$BASE_URL/api/v1/auth/logout-all")
                .post("".toRequestBody(jsonMediaType))
            token?.let { reqBuilder.header("Authorization", "Bearer $it") }

            client.newCall(reqBuilder.build()).execute()
            tokenManager.clearTokens()
            AuthResult.Success("已在所有设备登出")
        } catch (e: Exception) {
            tokenManager.clearTokens()
            AuthResult.Success("已在所有设备登出")
        }
    }

    // ─── Password Management ───

    /**
     * Change password
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext AuthResult.Error("未登录")

            val body = JsonObject().apply {
                addProperty("current_password", currentPassword)
                addProperty("new_password", newPassword)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/password")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Password changed → tokens invalidated, need re-login
                tokenManager.clearTokens()
                AuthResult.Success("密码修改成功，请重新登录")
            } else {
                AuthResult.Error(parseErrorMessage(responseBody))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Change password failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    /**
     * Forgot password - request reset code
     */
    suspend fun forgotPassword(email: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("email", email)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/forgot-password")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                AuthResult.Success(json.get("message")?.asString ?: "验证码已发送")
            } else {
                AuthResult.Error(parseErrorMessage(responseBody))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Forgot password failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    /**
     * Reset password with code
     */
    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): AuthResult<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("email", email)
                addProperty("code", code)
                addProperty("new_password", newPassword)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/reset-password")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val user = parseUserProfile(data.getAsJsonObject("user"))
                val tokens = data.getAsJsonObject("tokens")
                val loginResponse = LoginResponse(
                    user = user,
                    accessToken = tokens.get("access_token").asString,
                    refreshToken = tokens.get("refresh_token").asString,
                    expiresIn = tokens.get("expires_in").asInt
                )
                AuthResult.Success(loginResponse)
            } else {
                AuthResult.Error(parseErrorMessage(responseBody))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Reset password failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    // ─── Account Deletion (Google Play Compliance) ───

    /**
     * Permanently delete account
     */
    suspend fun deleteAccount(
        password: String,
        reason: String = ""
    ): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext AuthResult.Error("未登录")

            val body = JsonObject().apply {
                addProperty("password", password)
                addProperty("reason", reason)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/account")
                .header("Authorization", "Bearer $token")
                .delete(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                tokenManager.clearTokens()
                val json = JsonParser.parseString(responseBody).asJsonObject
                AuthResult.Success(json.get("message")?.asString ?: "账号已永久删除")
            } else {
                AuthResult.Error(parseErrorMessage(responseBody))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Delete account failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    // ─── Google OAuth ───

    /**
     * Google login
     */
    suspend fun googleLogin(
        idToken: String,
        deviceId: String? = null,
        deviceName: String? = null,
        deviceOs: String? = null,
        appVersion: String? = null
    ): AuthResult<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("id_token", idToken)
                deviceId?.let { addProperty("device_id", it) }
                deviceName?.let { addProperty("device_name", it) }
                deviceOs?.let { addProperty("device_os", it) }
                appVersion?.let { addProperty("app_version", it) }
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/google")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val user = parseUserProfile(data.getAsJsonObject("user"))
                val tokens = data.getAsJsonObject("tokens")
                val loginResponse = LoginResponse(
                    user = user,
                    accessToken = tokens.get("access_token").asString,
                    refreshToken = tokens.get("refresh_token").asString,
                    expiresIn = tokens.get("expires_in").asInt
                )
                AuthResult.Success(loginResponse)
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Google login failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    // ─── Avatar Upload ───

    /**
     * Upload avatar
     */
    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String = "image/jpeg"): AuthResult<AvatarUploadResult> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext AuthResult.Error("未登录")

            val ext = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "jpg"
            }
            val requestBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart(
                    "file", "avatar.$ext",
                    imageBytes.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/avatar")
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                AuthResult.Success(AvatarUploadResult(
                    avatarUrl = data?.get("avatar_url")?.asString ?: "",
                    avatarUrlGithub = data?.get("avatar_url_github")?.asString,
                    avatarUrlGitee = data?.get("avatar_url_gitee")?.asString
                ))
            } else {
                AuthResult.Error(parseErrorMessage(responseBody))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Upload avatar failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }

    // ─── Helper ───

    private suspend fun <T> tryRefreshAndRetry(retry: suspend () -> AuthResult<T>): AuthResult<T> {
        val refresh = tokenManager.getRefreshToken() ?: return AuthResult.Error("登录已过期，请重新登录")
        return when (val result = refreshToken(refresh)) {
            is AuthResult.Success -> {
                tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                retry()
            }
            is AuthResult.Error -> {
                tokenManager.clearTokens()
                AuthResult.Error("登录已过期，请重新登录")
            }
        }
    }

    // ── Safe JSON accessors (JsonNull is NOT Kotlin null, so ?. doesn't help) ──

    private fun JsonObject.safeString(key: String): String? {
        val el = get(key) ?: return null
        return if (el.isJsonPrimitive) el.asString else null
    }

    private fun JsonObject.safeLong(key: String, default: Long = 0): Long {
        val el = get(key) ?: return default
        return if (el.isJsonPrimitive) el.asLong else default
    }

    private fun JsonObject.safeInt(key: String, default: Int = 0): Int {
        val el = get(key) ?: return default
        return if (el.isJsonPrimitive) el.asInt else default
    }

    private fun JsonObject.safeBoolean(key: String, default: Boolean = false): Boolean {
        val el = get(key) ?: return default
        return if (el.isJsonPrimitive) el.asBoolean else default
    }

    private fun parseUserProfile(json: JsonObject): UserProfile {
        return UserProfile(
            id = json.safeLong("id"),
            email = json.safeString("email") ?: "",
            username = json.safeString("username") ?: "",
            avatarUrl = json.safeString("avatar_url"),
            isPro = json.safeBoolean("is_pro"),
            proPlan = json.safeString("pro_plan") ?: "free",
            maxDevices = json.safeInt("max_devices", 2),
            appsCreated = json.safeInt("apps_created"),
            apksBuilt = json.safeInt("apks_built")
        )
    }

    private fun parseErrorMessage(responseBody: String): String {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            // Check detail first, then message — must verify isJsonPrimitive
            // because json.get() can return JsonNull which is non-null but not a string
            val detail = json.get("detail")
            if (detail != null && detail.isJsonPrimitive) return detail.asString
            val message = json.get("message")
            if (message != null && message.isJsonPrimitive) return message.asString
            "操作失败"
        } catch (e: Exception) {
            "操作失败"
        }
    }

    private fun getAppVersion(): String {
        return "1.9.5"
    }

    /**
     * Heartbeat called every 60s for online duration
     */
    suspend fun heartbeat(): Unit = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/heartbeat")
                .post("{}".toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer ${tokenManager.getAccessToken()}")
                .build()
            client.newCall(request).execute().close()
        } catch (_: Exception) {
            // Heartbeat failures are non-critical
        }
    }

    /**
     * （Google login）
     */
    suspend fun bindEmail(email: String, verificationCode: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext AuthResult.Error("未登录")

            val body = JsonObject().apply {
                addProperty("email", email)
                addProperty("verification_code", verificationCode)
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/user/bind-email")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                AuthResult.Success(json.get("message")?.asString ?: "邮箱绑定成功")
            } else {
                AuthResult.Error(parseErrorMessage(responseBody))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Bind email failed", e)
            AuthResult.Error("网络连接失败，请检查网络后重试")
        }
    }
}

// ─── Data Classes ───

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

data class UserProfile(
    val id: Long,
    val email: String,
    val username: String,
    val avatarUrl: String? = null,
    val isPro: Boolean = false,
    val proPlan: String = "free",
    val maxDevices: Int = 2,
    val appsCreated: Int = 0,
    val apksBuilt: Int = 0
)

data class LoginResponse(
    val user: UserProfile,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

data class ProStatus(
    val isPro: Boolean,
    val proPlan: String,
    val isActive: Boolean,
    val daysRemaining: Int?
)

data class AvatarUploadResult(
    val avatarUrl: String,
    val avatarUrlGithub: String?,
    val avatarUrlGitee: String?
)
