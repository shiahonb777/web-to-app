package com.webtoapp.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.webtoapp.core.logging.AppLogger
import java.util.UUID

/**
 * JWT Token
 * 
 * EncryptedSharedPreferences Token
 * SharedPreferences
 */
class TokenManager(context: Context) {

    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "webtoapp_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_JSON = "user_json"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        AppLogger.w(TAG, "EncryptedSharedPreferences failed, falling back to plain prefs", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ─── Token ───

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_JSON)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    // ─── ───

    fun saveUserJson(json: String) {
        prefs.edit().putString(KEY_USER_JSON, json).apply()
    }

    fun getUserJson(): String? = prefs.getString(KEY_USER_JSON, null)

    // ─── ID ───

    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
}
