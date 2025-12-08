package com.webtoapp.core.adblock

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * 广告拦截器 - 基于URL规则拦截网页广告
 */
class AdBlocker {

    companion object {
        // 默认广告拦截规则（常见广告域名）
        private val DEFAULT_AD_HOSTS = listOf(
            // Google Ads
            "googleadservices.com",
            "googlesyndication.com",
            "doubleclick.net",
            "google-analytics.com",
            "googletagmanager.com",
            "googletagservices.com",

            // Facebook Ads
            "facebook.net",
            "fbcdn.net",

            // 其他常见广告网络
            "adservice",
            "adsserver",
            "adnxs.com",
            "advertising.com",
            "taboola.com",
            "outbrain.com",
            "criteo.com",
            "pubmatic.com",
            "rubiconproject.com",
            "moatads.com",

            // 中国常见广告
            "cpro.baidu.com",
            "pos.baidu.com",
            "cbjs.baidu.com",
            "eclick.baidu.com",
            "hm.baidu.com",
            "tanx.com",
            "alimama.com",
            "mmstat.com",
            "cnzz.com",
            "51.la",
            "union.sogou.com",
            "js.sogou.com"
        )

        private val DEFAULT_AD_PATTERNS = listOf(
            "*/ads/*",
            "*/ad/*",
            "*/advert/*",
            "*/banner/*",
            "*_ad.*",
            "*_ads.*",
            "*-ad.*",
            "*-ads.*",
            "*.ad.*",
            "*admob*",
            "*adsense*",
            "*adserver*",
            "*tracking*",
            "*analytics*"
        )
    }

    private val blockedHosts = mutableSetOf<String>()
    private val blockedPatterns = mutableSetOf<String>()
    private var enabled = false

    /**
     * 初始化拦截器
     */
    fun initialize(customRules: List<String> = emptyList(), useDefaultRules: Boolean = true) {
        blockedHosts.clear()
        blockedPatterns.clear()

        if (useDefaultRules) {
            blockedHosts.addAll(DEFAULT_AD_HOSTS)
            blockedPatterns.addAll(DEFAULT_AD_PATTERNS)
        }

        // 解析自定义规则
        customRules.forEach { rule ->
            when {
                rule.startsWith("||") -> {
                    // 域名规则 ||example.com
                    blockedHosts.add(rule.removePrefix("||").removeSuffix("^"))
                }
                rule.contains("*") -> {
                    // 通配符规则
                    blockedPatterns.add(rule)
                }
                else -> {
                    // 普通域名
                    blockedHosts.add(rule)
                }
            }
        }
    }

    fun setEnabled(enable: Boolean) {
        enabled = enable
    }

    fun isEnabled(): Boolean = enabled

    /**
     * 检查URL是否应该被拦截
     */
    fun shouldBlock(url: String): Boolean {
        if (!enabled) return false

        val lowerUrl = url.lowercase()

        // 检查域名
        blockedHosts.forEach { host ->
            if (lowerUrl.contains(host)) {
                return true
            }
        }

        // 检查URL模式
        blockedPatterns.forEach { pattern ->
            if (matchPattern(lowerUrl, pattern)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查WebResourceRequest是否应该被拦截
     */
    fun shouldBlock(request: WebResourceRequest): Boolean {
        return shouldBlock(request.url.toString())
    }

    /**
     * 返回空响应（用于拦截广告请求）
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * 简单的通配符模式匹配
     */
    private fun matchPattern(url: String, pattern: String): Boolean {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".?")

        return try {
            Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(url)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取当前规则数量
     */
    fun getRuleCount(): Int = blockedHosts.size + blockedPatterns.size

    /**
     * 添加自定义规则
     */
    fun addRule(rule: String) {
        when {
            rule.startsWith("||") -> blockedHosts.add(rule.removePrefix("||").removeSuffix("^"))
            rule.contains("*") -> blockedPatterns.add(rule)
            else -> blockedHosts.add(rule)
        }
    }

    /**
     * 移除规则
     */
    fun removeRule(rule: String) {
        blockedHosts.remove(rule)
        blockedPatterns.remove(rule)
    }

    /**
     * 清空所有规则
     */
    fun clearRules() {
        blockedHosts.clear()
        blockedPatterns.clear()
    }
}
