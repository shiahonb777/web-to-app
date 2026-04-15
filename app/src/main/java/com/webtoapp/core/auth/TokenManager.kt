package com.webtoapp.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.webtoapp.core.logging.AppLogger
import java.util.UUID

/**
 * 认证令牌存储。
 *
 * 优先使用 EncryptedSharedPreferences。
 * 如果安全存储不可用，则敏感登录数据只保留在内存中，不再静默回退到明文磁盘。
 */
class TokenManager(context: Context) {

    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "webtoapp_auth"
        private const val META_PREFS_NAME = "${PREFS_NAME}_meta"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_JSON = "user_json"
        private const val KEY_DEVICE_ID = "device_id"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val encryptedPrefs: SharedPreferences? = try {
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
        AppLogger.e(TAG, "EncryptedSharedPreferences 不可用，敏感登录数据将不再落明文磁盘", e)
        null
    }

    private val metaPrefs = context.getSharedPreferences(META_PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPlaintextPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var volatileAccessToken: String? = null
    private var volatileRefreshToken: String? = null
    private var volatileUserJson: String? = null
    private var warnedInMemoryFallback = false

    init {
        synchronized(Companion) {
            if (instance == null) {
                instance = this
            }
        }
        migrateDeviceIdIfNeeded()
        if (encryptedPrefs == null) {
            purgeLegacyPlaintextSensitiveData()
        } else {
            migrateLegacyPlaintextSensitiveDataIfNeeded()
        }
    }

    private fun warnInMemoryFallbackOnce() {
        if (!warnedInMemoryFallback) {
            warnedInMemoryFallback = true
            AppLogger.w(TAG, "安全存储不可用，登录态和用户信息只在当前进程内保留")
        }
    }

    private fun purgeLegacyPlaintextSensitiveData() {
        legacyPlaintextPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_JSON)
            .apply()
    }

    private fun migrateLegacyPlaintextSensitiveDataIfNeeded() {
        val prefs = encryptedPrefs ?: return
        val editor = prefs.edit()
        var changed = false

        listOf(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_USER_JSON).forEach { key ->
            val legacyValue = legacyPlaintextPrefs.getString(key, null)
            if (legacyValue != null && !prefs.contains(key)) {
                editor.putString(key, legacyValue)
                changed = true
            }
        }

        if (changed) {
            editor.apply()
        }
        purgeLegacyPlaintextSensitiveData()
    }

    private fun migrateDeviceIdIfNeeded() {
        if (metaPrefs.contains(KEY_DEVICE_ID)) {
            return
        }

        val existingDeviceId = encryptedPrefs?.getString(KEY_DEVICE_ID, null)
            ?: legacyPlaintextPrefs.getString(KEY_DEVICE_ID, null)

        if (existingDeviceId != null) {
            metaPrefs.edit().putString(KEY_DEVICE_ID, existingDeviceId).apply()
        }
    }

    private fun putSensitiveString(key: String, value: String?) {
        val prefs = encryptedPrefs
        if (prefs != null) {
            prefs.edit().putString(key, value).apply()
            return
        }

        warnInMemoryFallbackOnce()
        when (key) {
            KEY_ACCESS_TOKEN -> volatileAccessToken = value
            KEY_REFRESH_TOKEN -> volatileRefreshToken = value
            KEY_USER_JSON -> volatileUserJson = value
        }
    }

    private fun getSensitiveString(key: String): String? {
        encryptedPrefs?.let { return it.getString(key, null) }
        return when (key) {
            KEY_ACCESS_TOKEN -> volatileAccessToken
            KEY_REFRESH_TOKEN -> volatileRefreshToken
            KEY_USER_JSON -> volatileUserJson
            else -> null
        }
    }

    private fun clearSensitiveStrings(vararg keys: String) {
        val prefs = encryptedPrefs
        if (prefs != null) {
            prefs.edit().apply {
                keys.forEach(::remove)
            }.apply()
            return
        }

        keys.forEach { key ->
            when (key) {
                KEY_ACCESS_TOKEN -> volatileAccessToken = null
                KEY_REFRESH_TOKEN -> volatileRefreshToken = null
                KEY_USER_JSON -> volatileUserJson = null
            }
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        putSensitiveString(KEY_ACCESS_TOKEN, accessToken)
        putSensitiveString(KEY_REFRESH_TOKEN, refreshToken)
    }

    fun getAccessToken(): String? = getSensitiveString(KEY_ACCESS_TOKEN)

    fun getRefreshToken(): String? = getSensitiveString(KEY_REFRESH_TOKEN)

    fun clearTokens() {
        clearSensitiveStrings(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_USER_JSON)
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null && getRefreshToken() != null

    fun saveUserJson(json: String) {
        putSensitiveString(KEY_USER_JSON, json)
    }

    fun getUserJson(): String? = getSensitiveString(KEY_USER_JSON)

    fun getDeviceId(): String {
        var deviceId = metaPrefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            metaPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
}
