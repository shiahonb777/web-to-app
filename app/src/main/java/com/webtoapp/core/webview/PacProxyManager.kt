package com.webtoapp.core.webview

import android.content.Context
import android.webkit.WebView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * PAC (Proxy Auto-Configuration) 代理管理器
 *
 * 支持三种代理模式：
 * 1. NONE — 不使用代理（默认）
 * 2. STATIC — 固定代理服务器（HTTP/HTTPS/SOCKS5）
 * 3. PAC — PAC 脚本自动配置（支持远程 .pac URL）
 *
 * PAC 模式工作原理：
 * - 下载并解析 PAC 脚本（JavaScript），提取 PROXY/SOCKS 服务器列表
 * - 将提取到的代理规则通过 AndroidX ProxyController 应用到 WebView
 * - 支持 DIRECT、PROXY host:port、SOCKS host:port
 *
 * 使用 AndroidX WebKit 的 ProxyController API（需要 WebView Feature PROXY_OVERRIDE）
 */
class PacProxyManager(private val context: Context) {

    companion object {
        private const val TAG = "PacProxyManager"
        
        /**
         * 检查当前 WebView 是否支持代理覆盖功能
         */
        fun isProxyOverrideSupported(): Boolean {
            return try {
                WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to check PROXY_OVERRIDE support: ${e.message}")
                false
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 缓存的 PAC 脚本内容
    @Volatile
    private var cachedPacScript: String? = null
    @Volatile
    private var cachedPacUrl: String? = null

    /**
     * 应用代理配置到 WebView
     *
     * @param proxyMode 代理模式: NONE, STATIC, PAC
     * @param staticProxyHost 固定代理主机（STATIC 模式）
     * @param staticProxyPort 固定代理端口（STATIC 模式）
     * @param staticProxyType 固定代理类型: HTTP, HTTPS, SOCKS5（STATIC 模式）
     * @param pacUrl PAC 脚本 URL（PAC 模式）
     * @param bypassRules 代理绕过规则列表
     */
    suspend fun applyProxy(
        proxyMode: String,
        staticProxyHost: String = "",
        staticProxyPort: Int = 0,
        staticProxyType: String = "HTTP",
        pacUrl: String = "",
        bypassRules: List<String> = emptyList()
    ) {
        if (!isProxyOverrideSupported()) {
            AppLogger.w(TAG, "PROXY_OVERRIDE not supported on this WebView version")
            return
        }

        when (proxyMode.uppercase()) {
            "NONE" -> clearProxy()
            "STATIC" -> applyStaticProxy(staticProxyHost, staticProxyPort, staticProxyType, bypassRules)
            "PAC" -> applyPacProxy(pacUrl, bypassRules)
            else -> {
                AppLogger.w(TAG, "Unknown proxy mode: $proxyMode, clearing proxy")
                clearProxy()
            }
        }
    }

    /**
     * 清除代理配置，恢复系统默认
     */
    fun clearProxy() {
        if (!isProxyOverrideSupported()) return
        
        try {
            ProxyController.getInstance().clearProxyOverride(
                { command -> command.run() },
                { AppLogger.d(TAG, "Proxy cleared successfully") }
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear proxy", e)
        }
    }

    /**
     * 应用固定代理服务器
     */
    private fun applyStaticProxy(
        host: String,
        port: Int,
        type: String,
        bypassRules: List<String>
    ) {
        if (host.isBlank() || port <= 0) {
            AppLogger.w(TAG, "Invalid static proxy config: host=$host, port=$port")
            return
        }

        try {
            val proxyRule = when (type.uppercase()) {
                "SOCKS5", "SOCKS" -> "socks5://$host:$port"
                "HTTPS" -> "https://$host:$port"
                else -> "$host:$port"  // HTTP default
            }

            val builder = ProxyConfig.Builder()
                .addProxyRule(proxyRule)

            // 添加默认绕过规则
            builder.addBypassRule("localhost")
            builder.addBypassRule("127.0.0.1")
            builder.addBypassRule("[::1]")
            builder.addBypassRule("10.0.2.2") // Android emulator host

            // 添加自定义绕过规则
            bypassRules.filter { it.isNotBlank() }.forEach { rule ->
                builder.addBypassRule(rule)
            }

            ProxyController.getInstance().setProxyOverride(
                builder.build(),
                { command -> command.run() },
                { AppLogger.d(TAG, "Static proxy applied: $proxyRule") }
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to apply static proxy", e)
        }
    }

    /**
     * 应用 PAC 代理脚本
     *
     * PAC 脚本格式示例：
     * ```js
     * function FindProxyForURL(url, host) {
     *     if (host == "example.com") return "SOCKS5 proxy1:1080";
     *     if (host == "blocked.com") return "PROXY proxy2:8080";
     *     return "DIRECT";
     * }
     * ```
     *
     * 由于 AndroidX ProxyController 不直接支持 PAC 脚本，
     * 我们从 PAC 中解析出代理服务器列表，作为 fallback 链应用。
     */
    private suspend fun applyPacProxy(pacUrl: String, bypassRules: List<String>) {
        if (pacUrl.isBlank()) {
            AppLogger.w(TAG, "Empty PAC URL")
            return
        }

        try {
            // 下载 PAC 脚本
            val pacScript = downloadPacScript(pacUrl)
            if (pacScript.isNullOrBlank()) {
                AppLogger.e(TAG, "Failed to download PAC script from: $pacUrl")
                return
            }

            // 缓存
            cachedPacScript = pacScript
            cachedPacUrl = pacUrl

            // 解析 PAC 脚本中的代理服务器
            val proxyEntries = parsePacScript(pacScript)

            if (proxyEntries.isEmpty()) {
                AppLogger.w(TAG, "No proxy entries found in PAC script, using DIRECT")
                clearProxy()
                return
            }

            // 构建 ProxyConfig —— 按 PAC 中出现的顺序添加代理服务器作为 fallback 链
            val builder = ProxyConfig.Builder()

            proxyEntries.forEach { entry ->
                when {
                    entry.startsWith("DIRECT", ignoreCase = true) -> {
                        builder.addDirect()
                    }
                    entry.startsWith("SOCKS5 ", ignoreCase = true) ||
                    entry.startsWith("SOCKS ", ignoreCase = true) -> {
                        val server = entry.substringAfter(" ").trim()
                        if (server.isNotBlank()) {
                            builder.addProxyRule("socks5://$server")
                        }
                    }
                    entry.startsWith("PROXY ", ignoreCase = true) -> {
                        val server = entry.substringAfter(" ").trim()
                        if (server.isNotBlank()) {
                            builder.addProxyRule(server)
                        }
                    }
                    entry.startsWith("HTTPS ", ignoreCase = true) -> {
                        val server = entry.substringAfter(" ").trim()
                        if (server.isNotBlank()) {
                            builder.addProxyRule("https://$server")
                        }
                    }
                    else -> {
                        // 未知类型，尝试作为普通代理规则添加
                        if (entry.isNotBlank()) {
                            builder.addProxyRule(entry)
                        }
                    }
                }
            }

            // 添加默认绕过规则
            builder.addBypassRule("localhost")
            builder.addBypassRule("127.0.0.1")
            builder.addBypassRule("[::1]")
            builder.addBypassRule("10.0.2.2")

            // 添加自定义绕过规则
            bypassRules.filter { it.isNotBlank() }.forEach { rule ->
                builder.addBypassRule(rule)
            }

            ProxyController.getInstance().setProxyOverride(
                builder.build(),
                { command -> command.run() },
                {
                    AppLogger.d(TAG, "PAC proxy applied from $pacUrl, " +
                            "${proxyEntries.size} proxy entries extracted")
                }
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to apply PAC proxy", e)
        }
    }

    /**
     * 下载 PAC 脚本
     */
    private suspend fun downloadPacScript(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/x-ns-proxy-autoconfig, application/javascript, text/javascript, */*")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    AppLogger.d(TAG, "PAC script downloaded: ${body?.length ?: 0} chars")
                    body
                } else {
                    AppLogger.e(TAG, "PAC download failed: ${response.code} ${response.message}")
                    null
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "PAC download error: ${e.message}")
                null
            }
        }
    }

    /**
     * 解析 PAC 脚本，提取代理规则
     *
     * 提取策略：
     * 1. 使用正则匹配 return "PROXY host:port" / "SOCKS5 host:port" / "DIRECT" 等返回值
     * 2. 去重，保持顺序
     * 3. 所有唯一的代理服务器组成 fallback 链
     *
     * 注意：这种静态解析不能完全替代 PAC 的动态 JS 求值，
     * 但能覆盖绝大多数使用场景（固定规则、域名分流等）
     */
    private fun parsePacScript(script: String): List<String> {
        val entries = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        // 匹配 return "..." 或 return '...' 中的代理配置
        // 支持多种格式：
        //   return "PROXY 1.2.3.4:8080";
        //   return "SOCKS5 1.2.3.4:1080; DIRECT";
        //   return "PROXY proxy1:8080; PROXY proxy2:8080; DIRECT"
        val returnPattern = Regex(
            """(?:return\s+['"])([^'"]+)['"]""",
            RegexOption.IGNORE_CASE
        )

        returnPattern.findAll(script).forEach { match ->
            val returnValue = match.groupValues[1].trim()
            
            // PAC return 值可以包含多个用分号分隔的条目
            returnValue.split(";").forEach { entry ->
                val trimmed = entry.trim()
                if (trimmed.isNotBlank() && trimmed !in seen) {
                    seen.add(trimmed)
                    entries.add(trimmed)
                }
            }
        }

        // 如果正则没匹配到，尝试更宽泛的匹配
        if (entries.isEmpty()) {
            // 匹配 PROXY host:port 或 SOCKS host:port 模式（可能不在 return 语句中）
            val proxyPattern = Regex(
                """(PROXY|SOCKS5?|HTTPS|DIRECT)\s+[\w.\-]+:\d+""",
                RegexOption.IGNORE_CASE
            )
            proxyPattern.findAll(script).forEach { match ->
                val trimmed = match.value.trim()
                if (trimmed !in seen) {
                    seen.add(trimmed)
                    entries.add(trimmed)
                }
            }
            // 检查是否有 DIRECT
            if (script.contains("DIRECT", ignoreCase = true) && "DIRECT" !in seen) {
                entries.add("DIRECT")
            }
        }

        AppLogger.d(TAG, "Parsed ${entries.size} proxy entries from PAC: $entries")
        return entries
    }

    /**
     * 获取缓存的 PAC URL
     */
    fun getCachedPacUrl(): String? = cachedPacUrl

    /**
     * 刷新 PAC 脚本（重新下载并应用）
     */
    suspend fun refreshPac(bypassRules: List<String> = emptyList()) {
        val url = cachedPacUrl ?: return
        applyPacProxy(url, bypassRules)
    }
}
