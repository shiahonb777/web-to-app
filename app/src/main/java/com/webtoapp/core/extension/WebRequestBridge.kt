package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Chrome webRequest API 原生桥接
 *
 * 由于 shouldInterceptRequest 在后台线程运行，无法同步回调 JS，
 * 本类将扩展注册的 URL filter 预解析为原生数据结构，
 * 在 shouldInterceptRequest 中直接匹配，实现请求拦截。
 *
 * 支持:
 * - chrome.webRequest.onBeforeRequest 的 filter.urls 注册
 * - 请求拦截 (cancel=true)
 * - 按 resourceType 过滤
 * - 异步通知 JS 侧事件
 */
object WebRequestBridge {

    private const val TAG = "WebRequestBridge"

    /**
     * 单条 webRequest 规则
     */
    data class WebRequestRule(
        val extensionId: String,
        val urlPatterns: List<Pattern>,      // 编译后的 URL 正则
        val resourceTypes: Set<String>,       // "script", "image", "stylesheet", etc. 空=全部
        val blocking: Boolean                 // 是否阻断请求
    )

    // 每个扩展的 webRequest 规则列表 (extId -> rules)
    private val rules = ConcurrentHashMap<String, MutableList<WebRequestRule>>()

    // 已拦截请求计数
    @Volatile
    var blockedCount: Long = 0
        private set

    /**
     * 注册 webRequest filter 规则。
     * 由 JS 侧 chrome.webRequest.onBeforeRequest.addListener() 通过 native bridge 调用。
     *
     * @param extensionId 扩展 ID
     * @param urlPatternsJson JSON URL pattern 数组字符串
     * @param resourceTypesJson JSON 资源类型数组字符串
     * @param blocking 是否为 blocking 模式
     */
    fun registerFilter(
        extensionId: String,
        urlPatternsJson: String,
        resourceTypesJson: String,
        blocking: Boolean
    ) {
        try {
            val urlPatterns = parseJsonArray(urlPatternsJson)
            val resourceTypes = parseJsonArray(resourceTypesJson).toSet()

            if (urlPatterns.isEmpty()) {
                AppLogger.w(TAG, "Empty URL patterns for $extensionId, ignoring")
                return
            }

            val compiledPatterns = urlPatterns.mapNotNull { pattern ->
                compileUrlPattern(pattern)
            }

            if (compiledPatterns.isEmpty()) {
                AppLogger.w(TAG, "No valid URL patterns compiled for $extensionId")
                return
            }

            val rule = WebRequestRule(
                extensionId = extensionId,
                urlPatterns = compiledPatterns,
                resourceTypes = resourceTypes,
                blocking = blocking
            )

            rules.getOrPut(extensionId) { mutableListOf() }.add(rule)
            AppLogger.i(TAG, "Registered webRequest filter for $extensionId: ${urlPatterns.size} patterns, blocking=$blocking")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to register webRequest filter for $extensionId", e)
        }
    }

    /**
     * 移除指定扩展的所有 webRequest 规则。
     */
    fun unregisterAll(extensionId: String) {
        rules.remove(extensionId)
        AppLogger.d(TAG, "Unregistered all webRequest filters for $extensionId")
    }

    /**
     * 检查 URL 是否应被拦截。
     * 在 shouldInterceptRequest 后台线程中调用，必须线程安全。
     *
     * @param url 请求 URL
     * @param resourceType 资源类型 (script/image/stylesheet/xmlhttprequest/other)
     * @return true 如果应阻断请求
     */
    fun shouldBlock(url: String, resourceType: String = ""): Boolean {
        if (rules.isEmpty()) return false

        for ((_, ruleList) in rules) {
            for (rule in ruleList) {
                if (!rule.blocking) continue

                // 检查 resourceType 过滤
                if (rule.resourceTypes.isNotEmpty() && resourceType.isNotEmpty()) {
                    if (resourceType !in rule.resourceTypes) continue
                }

                // 检查 URL 匹配
                for (pattern in rule.urlPatterns) {
                    if (pattern.matcher(url).matches()) {
                        blockedCount++
                        AppLogger.d(TAG, "Blocked by ${rule.extensionId}: $url")
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 获取所有已注册规则数量。
     */
    fun getRuleCount(): Int {
        return rules.values.sumOf { it.size }
    }

    /**
     * 清除所有规则和计数器。
     */
    fun clear() {
        rules.clear()
        blockedCount = 0
    }

    // 将 Chrome 扩展 URL match pattern 编译为 Java 正则。
    // 支持: <all_urls>, scheme wildcard, domain wildcard, path wildcard
    private fun compileUrlPattern(pattern: String): Pattern? {
        return try {
            if (pattern == "<all_urls>") {
                return Pattern.compile("^https?://.*$")
            }

            // Convert Chrome match pattern to regex
            val regex = pattern
                .replace(".", "\\.")           // Escape dots
                .replace("*://", "(https?|wss?)://")  // Scheme wildcard
                .replace("\\.*\\.", "([^/]*\\.)?")     // *. domain wildcard -> optional subdomain
                .let { p ->
                    // Handle remaining * as .*
                    val sb = StringBuilder()
                    var i = 0
                    while (i < p.length) {
                        if (p[i] == '*') {
                            sb.append(".*")
                        } else {
                            sb.append(p[i])
                        }
                        i++
                    }
                    sb.toString()
                }

            Pattern.compile("^$regex$", Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to compile URL pattern: $pattern", e)
            null
        }
    }

    /**
     * 简易 JSON 数组解析。
     */
    private fun parseJsonArray(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[]" || trimmed == "null") return emptyList()

        return try {
            val arr = org.json.JSONArray(trimmed)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to parse JSON array: $json", e)
            emptyList()
        }
    }
}
