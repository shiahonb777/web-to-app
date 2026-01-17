package com.webtoapp.core.activation

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * 设备ID生成器
 */
object DeviceIdGenerator {
    
    private const val PREFS_NAME = "device_id_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    
    /**
     * 获取设备唯一ID
     * 优先使用 Android ID，如果不可用则生成 UUID 并持久化
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 先检查是否已有保存的设备ID
        val savedId = prefs.getString(KEY_DEVICE_ID, null)
        if (!savedId.isNullOrBlank()) {
            return savedId
        }
        
        // 尝试获取 Android ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            // 使用 Android ID 的哈希值
            hashString(androidId)
        } else {
            // 生成 UUID
            UUID.randomUUID().toString().replace("-", "")
        }
        
        // 保存设备ID
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        
        return deviceId
    }
    
    /**
     * 计算字符串的 SHA-256 哈希值
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }
}
