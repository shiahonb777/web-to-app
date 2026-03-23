package com.webtoapp.core.activation

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Device ID Generator
 */
object DeviceIdGenerator {
    
    private const val PREFS_NAME = "device_id_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    
    /**
     * Get unique device ID
     * Prefer Android ID, generate and persist UUID if unavailable
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // First check if device ID is already saved
        val savedId = prefs.getString(KEY_DEVICE_ID, null)
        if (!savedId.isNullOrBlank()) {
            return savedId
        }
        
        // Try to get Android ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            // Use hash of Android ID
            hashString(androidId)
        } else {
            // Generate UUID
            UUID.randomUUID().toString().replace("-", "")
        }
        
        // Save device ID
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        
        return deviceId
    }
    
    /**
     * Calculate SHA-256 hash of string
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }
}
