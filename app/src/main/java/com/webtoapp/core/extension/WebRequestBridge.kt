package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Chrome webRequest API.
 *
 * shouldInterceptRequest in after JS.
 * extension URL filter as .
 * in shouldInterceptRequest in requestintercept.
 *
 * - chrome.webRequest.onBeforeRequest filter.urls.
 * - requestintercept (cancel=true)
 * - by resourceType.
 * - asynchronously JS.
 */
object WebRequestBridge {

    private const val TAG = "WebRequestBridge"

    /**
     * single webRequest rules.
     */
    data class WebRequestRule(
        val extensionId: String,
        val urlPatterns: List<Pattern>,      // URL.
        val resourceTypes: Set<String>,       // script image stylesheet etc..
        val blocking: Boolean                 // Note.
    )

    // extension webRequest rules (extId -> rules)
    private val rules = ConcurrentHashMap<String, MutableList<WebRequestRule>>()

    // interceptrequest.
    @Volatile
    var blockedCount: Long = 0
        private set

    /**
     * webRequest filter rules.
     * JS chrome.webRequest.onBeforeRequest.addListener() native bridge use .
     *
     * @param extensionId extension ID.
     * @param urlPatternsJson JSON URL pattern.
     * @param resourceTypesJson JSON resource type.
     * @param blocking is as blocking.
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
     * extension webRequest rules.
     */
    fun unregisterAll(extensionId: String) {
        rules.remove(extensionId)
        AppLogger.d(TAG, "Unregistered all webRequest filters for $extensionId")
    }

    /**
     * Check URL is intercept.
     * in shouldInterceptRequest after in use thread-safe.
     *
     * @param url request URL.
     * @param resourceType resource type (script/image/stylesheet/xmlhttprequest/other)
     * @return true request.
     */
    fun shouldBlock(url: String, resourceType: String = ""): Boolean {
        if (rules.isEmpty()) return false

        for ((_, ruleList) in rules) {
            for (rule in ruleList) {
                if (!rule.blocking) continue

                // Check resourceType.
                if (rule.resourceTypes.isNotEmpty() && resourceType.isNotEmpty()) {
                    if (resourceType !in rule.resourceTypes) continue
                }

                // Check URL.
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
     * Get rules .
     */
    fun getRuleCount(): Int {
        return rules.values.sumOf { it.size }
    }

    /**
     * rules .
     */
    fun clear() {
        rules.clear()
        blockedCount = 0
    }

    // Chrome extension URL match pattern as Java .
    // Supports: <all_urls>, scheme wildcard, domain wildcard, path wildcard.
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
     * JSON .
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