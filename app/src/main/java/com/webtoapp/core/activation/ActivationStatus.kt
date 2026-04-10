package com.webtoapp.core.activation

/**
 * Activation status data class
 */
data class ActivationStatus(
    val isActivated: Boolean = false,
    val activatedTime: Long? = null,
    val expireTime: Long? = null,
    val usageCount: Int = 0,
    val usageLimit: Int? = null,
    val deviceId: String? = null,
    val codeType: ActivationCodeType? = null
) {
    /**
     * Check if activation is still valid
     */
    val isValid: Boolean
        get() {
            if (!isActivated) return false
            if (isExpired) return false
            if (isUsageExceeded) return false
            return true
        }
    
    /**
     * Check if expired
     */
    val isExpired: Boolean
        get() {
            expireTime?.let {
                return System.currentTimeMillis() > it
            }
            return false
        }
    
    /**
     * Check if usage exceeded
     */
    val isUsageExceeded: Boolean
        get() {
            usageLimit?.let {
                return usageCount >= it
            }
            return false
        }
    
    /**
     * Get remaining time (milliseconds)
     */
    val remainingTimeMs: Long?
        get() {
            expireTime?.let {
                val remaining = it - System.currentTimeMillis()
                return if (remaining > 0) remaining else 0
            }
            return null
        }
    
    /**
     * Get remaining usage count
     */
    val remainingUsage: Int?
        get() {
            usageLimit?.let {
                val remaining = it - usageCount
                return if (remaining > 0) remaining else 0
            }
            return null
        }
}
