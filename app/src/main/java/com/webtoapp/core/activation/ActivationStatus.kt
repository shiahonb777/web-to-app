package com.webtoapp.core.activation




data class ActivationStatus(
    val isActivated: Boolean = false,
    val activatedTime: Long? = null,
    val expireTime: Long? = null,
    val usageCount: Int = 0,
    val usageLimit: Int? = null,
    val deviceId: String? = null,
    val codeType: ActivationCodeType? = null
) {



    val isValid: Boolean
        get() {
            if (!isActivated) return false
            if (isExpired) return false
            if (isUsageExceeded) return false
            return true
        }




    val isExpired: Boolean
        get() {
            expireTime?.let {
                return System.currentTimeMillis() > it
            }
            return false
        }




    val isUsageExceeded: Boolean
        get() {
            usageLimit?.let {
                return usageCount >= it
            }
            return false
        }




    val remainingTimeMs: Long?
        get() {
            expireTime?.let {
                val remaining = it - System.currentTimeMillis()
                return if (remaining > 0) remaining else 0
            }
            return null
        }




    val remainingUsage: Int?
        get() {
            usageLimit?.let {
                val remaining = it - usageCount
                return if (remaining > 0) remaining else 0
            }
            return null
        }
}
