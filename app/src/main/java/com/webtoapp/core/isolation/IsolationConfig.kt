package com.webtoapp.core.isolation

import com.google.gson.Gson
import java.util.UUID

/**
 * 应用隔离/多开配置
 * 
 * 提供独立浏览器环境，包括：
 * - 随机指纹生成
 * - 伪造 HTTP Headers
 * - 随机 IP 伪装
 * - 独立存储环境
 */
data class IsolationConfig(
    val enabled: Boolean = false,
    
    // 指纹伪造
    val fingerprintConfig: FingerprintConfig = FingerprintConfig(),
    
    // Header 伪造
    val headerConfig: HeaderConfig = HeaderConfig(),
    
    // IP 伪装
    val ipSpoofConfig: IpSpoofConfig = IpSpoofConfig(),
    
    // Storage隔离
    val storageIsolation: Boolean = true,
    
    // WebRTC 防泄漏
    val blockWebRTC: Boolean = true,
    
    // Canvas 指纹防护
    val protectCanvas: Boolean = true,
    
    // AudioContext 指纹防护
    val protectAudio: Boolean = true,
    
    // WebGL 指纹防护
    val protectWebGL: Boolean = true,
    
    // 字体指纹防护
    val protectFonts: Boolean = true,
    
    // 时区伪装
    val spoofTimezone: Boolean = false,
    val customTimezone: String? = null,
    
    // 语言伪装
    val spoofLanguage: Boolean = false,
    val customLanguage: String? = null,
    
    // 屏幕分辨率伪装
    val spoofScreen: Boolean = false,
    val customScreenWidth: Int? = null,
    val customScreenHeight: Int? = null
) {
    companion object {
        val DISABLED = IsolationConfig(enabled = false)
        
        /** 基础隔离 - 仅存储隔离和基本指纹保护 */
        val BASIC = IsolationConfig(
            enabled = true,
            storageIsolation = true,
            blockWebRTC = true,
            protectCanvas = true,
            protectAudio = false,
            protectWebGL = false,
            protectFonts = false
        )
        
        /** 标准隔离 - 推荐配置 */
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
        
        /** 最高隔离 - 全部防护 */
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
                Gson().fromJson(json, IsolationConfig::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun toJson(): String = Gson().toJson(this)
}

/**
 * 浏览器指纹配置
 */
data class FingerprintConfig(
    val randomize: Boolean = true,
    val regenerateOnLaunch: Boolean = false,
    
    // Custom User-Agent
    val customUserAgent: String? = null,
    val randomUserAgent: Boolean = true,
    
    // 平台信息
    val platform: String? = null,
    val vendor: String? = null,
    
    // 硬件信息
    val hardwareConcurrency: Int? = null,
    val deviceMemory: Int? = null,
    
    // 唯一指纹ID（用于保持一致性）
    val fingerprintId: String = UUID.randomUUID().toString()
)

/**
 * HTTP Header 伪造配置
 */
data class HeaderConfig(
    val enabled: Boolean = false,
    val randomizeOnRequest: Boolean = false,
    
    // Custom Headers
    val customHeaders: Map<String, String> = emptyMap(),
    
    // Accept-Language
    val acceptLanguage: String? = null,
    
    // Accept-Encoding
    val acceptEncoding: String? = null,
    
    // DNT (Do Not Track)
    val dnt: Boolean = true,
    
    // Sec-CH-UA (Client Hints)
    val spoofClientHints: Boolean = true,
    
    // Referer 策略
    val refererPolicy: RefererPolicy = RefererPolicy.STRICT_ORIGIN
)

/**
 * Referer 策略
 */
enum class RefererPolicy(val value: String, val displayName: String) {
    NO_REFERRER("no-referrer", "不发送"),
    ORIGIN("origin", "仅域名"),
    STRICT_ORIGIN("strict-origin", "严格域名"),
    SAME_ORIGIN("same-origin", "同源"),
    STRICT_ORIGIN_WHEN_CROSS_ORIGIN("strict-origin-when-cross-origin", "跨域时严格")
}

/**
 * IP 伪装配置
 */
data class IpSpoofConfig(
    val enabled: Boolean = false,
    
    // 伪装方式
    val spoofMethod: IpSpoofMethod = IpSpoofMethod.HEADER,
    
    // Custom IP（仅 Header 伪装有效）
    val customIp: String? = null,
    
    // IP 选择模式
    val randomIpRange: IpRange = IpRange.USA,
    
    // Search关键词（用于搜索模式）
    val searchKeyword: String? = null,
    
    // X-Forwarded-For Header
    val xForwardedFor: Boolean = true,
    
    // X-Real-IP Header
    val xRealIp: Boolean = true,
    
    // Client-IP Header
    val clientIp: Boolean = true
)

/**
 * IP 伪装方式
 */
enum class IpSpoofMethod(val displayName: String) {
    HEADER("Header 伪造"),
    // PROXY("代理服务器")  // 未来可扩展
}

/**
 * IP 选择模式
 */
enum class IpRange(val displayName: String) {
    USA("美国"),
    SEARCH("搜索"),
    GLOBAL("全球随机")
}
