package com.webtoapp.core.isolation

import java.util.UUID










data class IsolationConfig(
    val enabled: Boolean = false,


    val fingerprintConfig: FingerprintConfig = FingerprintConfig(),


    val headerConfig: HeaderConfig = HeaderConfig(),


    val ipSpoofConfig: IpSpoofConfig = IpSpoofConfig(),


    val storageIsolation: Boolean = true,


    val blockWebRTC: Boolean = true,


    val protectCanvas: Boolean = true,


    val protectAudio: Boolean = true,


    val protectWebGL: Boolean = true,


    val protectFonts: Boolean = true,


    val spoofTimezone: Boolean = false,
    val customTimezone: String? = null,


    val spoofLanguage: Boolean = false,
    val customLanguage: String? = null,


    val spoofScreen: Boolean = false,
    val customScreenWidth: Int? = null,
    val customScreenHeight: Int? = null
) {
    companion object {
        private val gson = com.webtoapp.util.GsonProvider.gson
        val DISABLED = IsolationConfig(enabled = false)


        val BASIC = IsolationConfig(
            enabled = true,
            storageIsolation = true,
            blockWebRTC = true,
            protectCanvas = true,
            protectAudio = false,
            protectWebGL = false,
            protectFonts = false
        )


        val STANDARD = IsolationConfig(
            enabled = true,
            fingerprintConfig = FingerprintConfig(randomize = true),
            headerConfig = HeaderConfig(enabled = true),
            storageIsolation = true,
            blockWebRTC = true,
            protectCanvas = true,
            protectAudio = true,
            protectWebGL = true,
            protectFonts = false
        )


        val MAXIMUM = IsolationConfig(
            enabled = true,
            fingerprintConfig = FingerprintConfig(randomize = true, regenerateOnLaunch = true),
            headerConfig = HeaderConfig(enabled = true, randomizeOnRequest = true),
            ipSpoofConfig = IpSpoofConfig(enabled = true),
            storageIsolation = true,
            blockWebRTC = true,
            protectCanvas = true,
            protectAudio = true,
            protectWebGL = true,
            protectFonts = true,
            spoofTimezone = true,
            spoofLanguage = true,
            spoofScreen = true
        )

        fun fromJson(json: String): IsolationConfig? {
            return try {
                gson.fromJson(json, IsolationConfig::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String = gson.toJson(this)
}




data class FingerprintConfig(
    val randomize: Boolean = true,
    val regenerateOnLaunch: Boolean = false,


    val customUserAgent: String? = null,
    val randomUserAgent: Boolean = true,


    val platform: String? = null,
    val vendor: String? = null,


    val hardwareConcurrency: Int? = null,
    val deviceMemory: Int? = null,


    val fingerprintId: String = UUID.randomUUID().toString()
)




data class HeaderConfig(
    val enabled: Boolean = false,
    val randomizeOnRequest: Boolean = false,


    val customHeaders: Map<String, String> = emptyMap(),


    val acceptLanguage: String? = null,


    val acceptEncoding: String? = null,


    val dnt: Boolean = true,


    val spoofClientHints: Boolean = true,


    val refererPolicy: RefererPolicy = RefererPolicy.STRICT_ORIGIN
)




enum class RefererPolicy(val value: String, val displayName: String) {
    NO_REFERRER("no-referrer", "不发送"),
    ORIGIN("origin", "仅域名"),
    STRICT_ORIGIN("strict-origin", "严格域名"),
    SAME_ORIGIN("same-origin", "同源"),
    STRICT_ORIGIN_WHEN_CROSS_ORIGIN("strict-origin-when-cross-origin", "跨域时严格")
}




data class IpSpoofConfig(
    val enabled: Boolean = false,


    val spoofMethod: IpSpoofMethod = IpSpoofMethod.HEADER,


    val customIp: String? = null,


    val randomIpRange: IpRange = IpRange.USA,


    val searchKeyword: String? = null,


    val xForwardedFor: Boolean = true,


    val xRealIp: Boolean = true,


    val clientIp: Boolean = true
)




enum class IpSpoofMethod(val displayName: String) {
    HEADER("Header 伪造"),

}




enum class IpRange(val displayName: String) {
    USA("美国"),
    SEARCH("搜索"),
    GLOBAL("全球随机")
}
