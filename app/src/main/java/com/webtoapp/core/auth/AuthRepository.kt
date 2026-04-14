package com.webtoapp.core.auth

import com.google.gson.Gson
import com.webtoapp.core.logging.AppLogger

/**
 * — AuthApiClient TokenManager
 * 
 * Token handling wrapper.
 */
class AuthRepository(
    private val apiClient: AuthApiClient,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    private val gson = Gson()

    /**
     * （）
     */
    suspend fun login(account: String, password: String): AuthResult<UserProfile> {
        return when (val result = apiClient.login(account, password)) {
            is AuthResult.Success -> {
                val loginResponse = result.data
                // Token
                tokenManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                // Note.
                tokenManager.saveUserJson(gson.toJson(loginResponse.user))
                AppLogger.i(TAG, "Login successful: ${loginResponse.user.username}")
                AuthResult.Success(loginResponse.user)
            }
            is AuthResult.Error -> result
        }
    }

    /**
     * Send email verification code
     */
    suspend fun sendVerificationCode(email: String): AuthResult<String> {
        return apiClient.sendVerificationCode(email)
    }

    /**
     * Note.
     */
    suspend fun register(email: String, username: String, password: String, verificationCode: String): AuthResult<UserProfile> {
        return when (val result = apiClient.register(email, username, password, verificationCode)) {
            is AuthResult.Success -> {
                val loginResponse = result.data
                // Save tokens directly — no separate login call needed
                tokenManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                tokenManager.saveUserJson(gson.toJson(loginResponse.user))
                AppLogger.i(TAG, "Registration + auto-login successful: ${loginResponse.user.username}")
                AuthResult.Success(loginResponse.user)
            }
            is AuthResult.Error -> result
        }
    }

    /**
     * （ Token ）
     */
    suspend fun logout() {
        try {
            val deviceId = tokenManager.getDeviceId()
            apiClient.logout(deviceId)
        } catch (_: Exception) {
            // ，
            tokenManager.clearTokens()
        }
        AppLogger.i(TAG, "User logged out")
    }

    /**
     * Note.
     */
    suspend fun logoutAll(): AuthResult<String> {
        return when (val result = apiClient.logoutAll()) {
            is AuthResult.Success -> {
                AppLogger.i(TAG, "Logged out from all devices")
                result
            }
            is AuthResult.Error -> {
                tokenManager.clearTokens()
                result
            }
        }
    }

    /**
     * Note.
     */
    fun getCachedUser(): UserProfile? {
        val json = tokenManager.getUserJson() ?: return null
        return try {
            gson.fromJson(json, UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Note.
     */
    suspend fun refreshProfile(): AuthResult<UserProfile> {
        return when (val result = apiClient.getProfile()) {
            is AuthResult.Success -> {
                tokenManager.saveUserJson(gson.toJson(result.data))
                result
            }
            is AuthResult.Error -> result
        }
    }

    /**
     * Pro
     */
    suspend fun getProStatus(): AuthResult<ProStatus> {
        return apiClient.getProStatus()
    }

    /**
     * Note.
     */
    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    // ─── Heartbeat ───

    /**
     * —
     */
    suspend fun heartbeat() {
        apiClient.heartbeat()
    }

    // ─── Password Management ───

    /**
     * Change password
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<String> {
        return apiClient.changePassword(currentPassword, newPassword)
    }

    /**
     * Note.
     */
    suspend fun forgotPassword(email: String): AuthResult<String> {
        return apiClient.forgotPassword(email)
    }

    /**
     * （）→
     */
    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult<UserProfile> {
        return when (val result = apiClient.resetPassword(email, code, newPassword)) {
            is AuthResult.Success -> {
                val loginResponse = result.data
                tokenManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                tokenManager.saveUserJson(gson.toJson(loginResponse.user))
                AppLogger.i(TAG, "Password reset + auto-login: ${loginResponse.user.username}")
                AuthResult.Success(loginResponse.user)
            }
            is AuthResult.Error -> result
        }
    }

    // ─── Account Deletion ───

    /**
     * Permanently delete account
     */
    suspend fun deleteAccount(password: String, reason: String = ""): AuthResult<String> {
        return apiClient.deleteAccount(password, reason)
    }

    // ─── Google OAuth ───

    /**
     * Google login
     */
    suspend fun googleLogin(idToken: String): AuthResult<UserProfile> {
        val deviceId = tokenManager.getDeviceId()
        return when (val result = apiClient.googleLogin(
            idToken = idToken,
            deviceId = deviceId,
            deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            deviceOs = "Android ${android.os.Build.VERSION.RELEASE}",
            appVersion = "1.9.5"
        )) {
            is AuthResult.Success -> {
                val loginResponse = result.data
                tokenManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                tokenManager.saveUserJson(gson.toJson(loginResponse.user))
                AppLogger.i(TAG, "Google login successful: ${loginResponse.user.username}")
                AuthResult.Success(loginResponse.user)
            }
            is AuthResult.Error -> result
        }
    }

    // ─── Avatar Upload ───

    /**
     * Upload avatar
     */
    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String = "image/jpeg"): AuthResult<AvatarUploadResult> {
        return apiClient.uploadAvatar(imageBytes, mimeType)
    }

    // ─── Update Profile ───

    /**
     * Update user profile
     */
    suspend fun updateProfile(username: String? = null): AuthResult<UserProfile> {
        return when (val result = apiClient.updateProfile(username = username)) {
            is AuthResult.Success -> {
                tokenManager.saveUserJson(gson.toJson(result.data))
                result
            }
            is AuthResult.Error -> result
        }
    }

    // ─── Bind Email ───

    /**
     * Note.
     */
    suspend fun bindEmail(email: String, verificationCode: String): AuthResult<String> {
        return apiClient.bindEmail(email, verificationCode)
    }
}
