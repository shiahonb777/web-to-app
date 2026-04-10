package com.webtoapp.core.engine.shields

import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTPS Everywhere — 自动将 HTTP 请求升级为 HTTPS
 *
 * 功能：
 * - 拦截 http:// URL，自动重写为 https://
 * - 白名单机制（localhost、内网 IP、已知不支持 HTTPS 的域名）
 * - 升级失败回退：SSL 错误后自动标记该域名为不升级
 *
 * 优化点:
 * 1. 使用 ConcurrentHashMap.newKeySet() 替换 mutableSetOf，解决多线程竞争问题
 * 2. IP 正则预编译并缓存为 companion object 常量
 * 3. 内网 IP 检查优化：按首字节快速跳过
 */
class HttpsUpgrader {

    companion object {
        private const val TAG = "HttpsUpgrader"

        // 不升级的域名/IP 模式
        private val SKIP_HOSTS = setOf(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1"
        )

        // 内网 IP 前缀
        private val PRIVATE_IP_PREFIXES = listOf(
            "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
            "172.25.", "172.26.", "172.27.", "172.28.", "172.29.",
            "172.30.", "172.31.", "192.168."
        )

        /** ★ 预编译的 IP 地址正则，避免每次调用都重新编译 */
        private val IP_REGEX = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
    }

    // ★ 使用线程安全集合，避免 WebView 回调线程与主线程同时读写导致 ConcurrentModificationException
    private val failedDomains: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val pendingUpgrades: MutableSet<String> = ConcurrentHashMap.newKeySet()
    // ★ 记录已尝试过 HTTP 回退的域名，避免无限循环
    private val httpFallbackAttempted: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * 尝试将 URL 升级为 HTTPS
     * 
     * @param url 原始 URL
     * @return 升级后的 URL，如果不需要升级则返回 null
     */
    fun tryUpgrade(url: String): String? {
        if (!url.startsWith("http://")) return null

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return null
        }

        val host = uri.host?.lowercase() ?: return null

        // 白名单检查
        if (shouldSkip(host)) return null

        // 已知升级失败的域名
        if (failedDomains.contains(host)) return null

        // 重写为 HTTPS
        val httpsUrl = url.replaceFirst("http://", "https://")
        pendingUpgrades.add(host)
        AppLogger.d(TAG, "HTTPS upgrade: $host")
        return httpsUrl
    }

    /**
     * SSL 错误时调用 — 如果该域名正在等待升级，则标记为失败并回退
     *
     * @param url 发生 SSL 错误的 URL
     * @return 回退的 HTTP URL，如果不是升级导致的则返回 null
     */
    fun onSslError(url: String?): String? {
        if (url == null) return null
        val host = try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return null

        if (pendingUpgrades.remove(host)) {
            failedDomains.add(host)
            val httpUrl = url.replaceFirst("https://", "http://")
            AppLogger.w(TAG, "HTTPS upgrade failed for $host, falling back to HTTP")
            return httpUrl
        }
        return null
    }

    /**
     * 通用 HTTP 回退 — 任何 HTTPS URL 遇到 SSL 错误时，尝试返回 HTTP 版本
     * 防止同一域名重复回退
     *
     * @param url 发生 SSL 错误的 HTTPS URL
     * @return HTTP URL（如果已尝试过则返回 null）
     */
    fun tryHttpFallback(url: String?): String? {
        if (url == null || !url.startsWith("https://")) return null
        val host = try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return null

        if (httpFallbackAttempted.contains(host)) {
            AppLogger.w(TAG, "HTTP fallback already attempted for $host, skipping to avoid loop")
            return null
        }

        httpFallbackAttempted.add(host)
        val httpUrl = url.replaceFirst("https://", "http://")
        AppLogger.w(TAG, "SSL error on $host, attempting HTTP fallback: $httpUrl")
        return httpUrl
    }

    /** 页面开始加载时清除 pending 状态 */
    fun onPageStarted() {
        pendingUpgrades.clear()
    }

    /** 检查域名是否在白名单中 */
    private fun shouldSkip(host: String): Boolean {
        // 精确匹配
        if (host in SKIP_HOSTS) return true

        // ★ 快速路径：首字符不是 '1' 或 '2' 开头就不可能是内网 IP，跳过遍历
        val firstChar = host.firstOrNull()
        if (firstChar == '1' || firstChar == '2') {
            if (PRIVATE_IP_PREFIXES.any { host.startsWith(it) }) return true
        }

        // .local 域名
        if (host.endsWith(".local") || host.endsWith(".localhost")) return true

        // ★ 使用预编译正则
        if (IP_REGEX.matches(host)) return true

        return false
    }

    /** 清除失败记录 */
    fun clearFailedDomains() {
        failedDomains.clear()
        AppLogger.d(TAG, "Failed domains list cleared")
    }

    /** 获取失败域名数量 */
    fun getFailedDomainCount(): Int = failedDomains.size
}
