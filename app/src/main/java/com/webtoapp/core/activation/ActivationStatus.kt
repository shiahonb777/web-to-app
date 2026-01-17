package com.webtoapp.core.activation

/**
 * 激活状态数据类
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
     * 检查激活是否仍然有效
     */
    val isValid: Boolean
        get() {
            if (!isActivated) return false
            if (isExpired) return false
            if (isUsageExceeded) return false
            return true
        }
    
    /**
     * 检查是否已过期
     */
    val isExpired: Boolean
        get() {
            expireTime?.let {
                return System.currentTimeMillis() > it
            }
            return false
        }
    
    /**
     * 检查是否超出使用次数
     */
    val isUsageExceeded: Boolean
        get() {
            usageLimit?.let {
                return usageCount >= it
            }
            return false
        }
    
    /**
     * 获取剩余时间（毫秒）
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
     * 获取剩余使用次数
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
