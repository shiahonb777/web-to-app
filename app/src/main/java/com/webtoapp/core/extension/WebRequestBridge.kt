package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern














object WebRequestBridge {

    private const val TAG = "WebRequestBridge"




    data class WebRequestRule(
        val extensionId: String,
        val urlPatterns: List<Pattern>,
        val resourceTypes: Set<String>,
        val blocking: Boolean
    )


    private val rules = ConcurrentHashMap<String, MutableList<WebRequestRule>>()


    @Volatile
    var blockedCount: Long = 0
        private set










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




    fun unregisterAll(extensionId: String) {
        rules.remove(extensionId)
        AppLogger.d(TAG, "Unregistered all webRequest filters for $extensionId")
    }









    fun shouldBlock(url: String, resourceType: String = ""): Boolean {
        if (rules.isEmpty()) return false

        for ((_, ruleList) in rules) {
            for (rule in ruleList) {
                if (!rule.blocking) continue


                if (rule.resourceTypes.isNotEmpty() && resourceType.isNotEmpty()) {
                    if (resourceType !in rule.resourceTypes) continue
                }


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




    fun getRuleCount(): Int {
        return rules.values.sumOf { it.size }
    }




    fun clear() {
        rules.clear()
        blockedCount = 0
    }



    private fun compileUrlPattern(pattern: String): Pattern? {
        return try {
            if (pattern == "<all_urls>") {
                return Pattern.compile("^https?://.*$")
            }


            val regex = pattern
                .replace(".", "\\.")
                .replace("*://", "(https?|wss?)://")
                .replace("\\.*\\.", "([^/]*\\.)?")
                .let { p ->

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
