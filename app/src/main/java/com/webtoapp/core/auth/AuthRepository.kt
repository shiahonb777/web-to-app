package com.webtoapp.core.auth

import com.google.gson.Gson
import com.webtoapp.core.logging.AppLogger

/**
 * 认证仓库 — 封装 AuthApiClient 与 TokenManager 的交互
 * 
 * 负责登录/注册后自动保存 Token，并提供统一的认证状态查询
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
     * 登录（支持用户名或邮箱）
     */
    suspend fun login(account: String, password: String): AuthResult<UserProfile> {
        return when (val result = apiClient.login(account, password)) {
            is AuthResult.Success -> {
                val loginResponse = result.data
                // 保存 Token
                tokenManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                // 缓存用户信息
                tokenManager.saveUserJson(gson.toJson(loginResponse.user))
                AppLogger.i(TAG, "Login successful: ${loginResponse.user.username}")
                AuthResult.Success(loginResponse.user)
            }
            is AuthResult.Error -> result
        }
    }

    /**
     * 发送邮箱验证码
     */
    suspend fun sendVerificationCode(email: String): AuthResult<String> {
        return apiClient.sendVerificationCode(email)
    }

    /**
     * 注册
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
     * 退出登录（通知服务器使 Token 失效）
     */
    suspend fun logout() {
        try {
            val deviceId = tokenManager.getDeviceId()
            apiClient.logout(deviceId)
        } catch (_: Exception) {
            // 即使服务器调用失败，也清理本地
            tokenManager.clearTokens()
        }
        AppLogger.i(TAG, "User logged out")
    }

    /**
     * 从所有设备登出
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
     * 获取本地缓存的用户信息
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
     * 从服务端刷新用户信息
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
     * 获取 Pro 状态
     */
    suspend fun getProStatus(): AuthResult<ProStatus> {
        return apiClient.getProStatus()
    }

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    // ─── Heartbeat ───

    /**
     * 心跳 — 用于统计在线时长
     */
    suspend fun heartbeat() {
        apiClient.heartbeat()
    }

    // ─── Password Management ───

    /**
     * 修改密码
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<String> {
        return apiClient.changePassword(currentPassword, newPassword)
    }

    /**
     * 忘记密码
     */
    suspend fun forgotPassword(email: String): AuthResult<String> {
        return apiClient.forgotPassword(email)
    }

    /**
     * 重置密码（验证码方式）→ 自动登录
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
     * 永久删除账号
     */
    suspend fun deleteAccount(password: String, reason: String = ""): AuthResult<String> {
        return apiClient.deleteAccount(password, reason)
    }

    // ─── Google OAuth ───

    /**
     * Google 登录
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
     * 上传头像
     */
    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String = "image/jpeg"): AuthResult<AvatarUploadResult> {
        return apiClient.uploadAvatar(imageBytes, mimeType)
    }

    // ─── Update Profile ───

    /**
     * 更新用户资料
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
     * 绑定邮箱
     */
    suspend fun bindEmail(email: String, verificationCode: String): AuthResult<String> {
        return apiClient.bindEmail(email, verificationCode)
    }
}

