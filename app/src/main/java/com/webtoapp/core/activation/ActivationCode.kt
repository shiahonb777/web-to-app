package com.webtoapp.core.activation

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 激活码类型
 */
enum class ActivationCodeType(val displayName: String, val description: String) {
    PERMANENT("永久激活", "激活后永久有效，无任何限制"),
    TIME_LIMITED("时间限制", "激活后在指定时间内有效"),
    USAGE_LIMITED("次数限制", "激活后可使用指定次数"),
    DEVICE_BOUND("设备绑定", "激活后绑定到当前设备"),
    COMBINED("组合限制", "同时支持时间和次数限制")
}

/**
 * 激活码数据类
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
         * 从 JSON 字符串解析激活码
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
         * 从旧格式字符串创建永久激活码
         */
        fun fromLegacyString(code: String): ActivationCode {
            return ActivationCode(
                code = code,
                type = ActivationCodeType.PERMANENT
            )
        }
    }
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return gson.toJson(this)
    }
}
