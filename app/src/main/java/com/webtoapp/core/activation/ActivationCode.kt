package com.webtoapp.core.activation

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Activation code type
 */
enum class ActivationCodeType(val displayName: String, val description: String) {
    PERMANENT("Permanent", "Valid permanently after activation, no restrictions"),
    TIME_LIMITED("Time Limited", "Valid within specified time after activation"),
    USAGE_LIMITED("Usage Limited", "Can be used specified number of times after activation"),
    DEVICE_BOUND("Device Bound", "Bound to current device after activation"),
    COMBINED("Combined", "Supports both time and usage limits")
}

/**
 * Activation code data class
 */
data class ActivationCode(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("type")
    val type: ActivationCodeType = ActivationCodeType.PERMANENT,
    
    @SerializedName("timeLimitMs")
    val timeLimitMs: Long? = null,
    
    @SerializedName("usageLimit")
    val usageLimit: Int? = null,
    
    @SerializedName("allowDeviceBinding")
    val allowDeviceBinding: Boolean = false,
    
    @SerializedName("note")
    val note: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson = Gson()
        
        /**
         * Parse activation code from JSON string
         */
        fun fromJson(json: String): ActivationCode? {
            return try {
                if (json.trimStart().startsWith("{")) {
                    gson.fromJson(json, ActivationCode::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Create permanent activation code from legacy string
         */
        fun fromLegacyString(code: String): ActivationCode {
            return ActivationCode(
                code = code,
                type = ActivationCodeType.PERMANENT
            )
        }
    }
    
    /**
     * Convert to JSON string
     */
    fun toJson(): String {
        return gson.toJson(this)
    }
}
