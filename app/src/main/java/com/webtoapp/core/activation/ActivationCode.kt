package com.webtoapp.core.activation

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.i18n.Strings

/**
 * 激活码类型
 */
enum class ActivationCodeType {
    PERMANENT,
    TIME_LIMITED,
    USAGE_LIMITED,
    DEVICE_BOUND,
    COMBINED;

    val displayName: String
        get() = when (this) {
            PERMANENT -> Strings.activationPermanent
            TIME_LIMITED -> Strings.activationTimeLimited
            USAGE_LIMITED -> Strings.activationUsageLimited
            DEVICE_BOUND -> Strings.activationDeviceBound
            COMBINED -> Strings.activationCombined
        }

    val description: String
        get() = when (this) {
            PERMANENT -> Strings.activationPermanentDesc
            TIME_LIMITED -> Strings.activationTimeLimitedDesc
            USAGE_LIMITED -> Strings.activationUsageLimitedDesc
            DEVICE_BOUND -> Strings.activationDeviceBoundDesc
            COMBINED -> Strings.activationCombinedDesc
        }
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
