package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * MV3 declarativeNetRequest rules.
 *
 * extension declarativeNetRequest JSON rules in .
 * Supports action types:
 * - block: request.
 * - allow: request.
 * - redirect: to URL.
 * - modifyHeaders: request/.
 *
 * rules: https://developer.chrome.com/docs/extensions/reference/api/declarativeNetRequest.
 */
object DeclarativeNetRequestEngine {

    private const val TAG = "DNREngine"

    /**
     * rules action.
     */
    enum class ActionType {
        BLOCK, ALLOW, REDIRECT, MODIFY_HEADERS, UPGRADE_SCHEME, ALLOW_ALL_REQUESTS
    }

    /**
     * resource type.
     */
    enum class ResourceType {
        MAIN_FRAME, SUB_FRAME, STYLESHEET, SCRIPT, IMAGE, FONT,
        OBJECT, XMLHTTPREQUEST, PING, CSP_REPORT, MEDIA, WEBSOCKET, OTHER;

        companion object {
            fun fromString(s: String): ResourceType? = try {
                valueOf(s.uppercase().replace("-", "_"))
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * single DNR rules.
     */
    data class DnrRule(
        val id: Int,
        val priority: Int,
        val action: ActionType,
        val redirectUrl: String?,               // REDIRECT.
        val urlFilter: Pattern?,                // URL.
        val regexFilter: Pattern?,              // urlFilter.
        val resourceTypes: Set<ResourceType>,   // Note.
        val excludedResourceTypes: Set<ResourceType>,
        val domains: Set<String>,               // initiator domains.
        val excludedDomains: Set<String>,
        val requestMethods: Set<String>,        // Note.
        val excludedRequestMethods: Set<String>
    )

    // extension rules: extId -> (static rules + dynamic rules)
    private val staticRules = ConcurrentHashMap<String, List<DnrRule>>()
    private val dynamicRules = ConcurrentHashMap<String, MutableList<DnrRule>>()
    private val sessionRules = ConcurrentHashMap<String, MutableList<DnrRule>>()

    @Volatile
    var matchedCount: Long = 0
        private set

    /**
     * extension rules .
     *
     * @param extensionId extension ID.
     * @param rulesJson JSON.
     */
    fun loadStaticRules(extensionId: String, rulesJson: String) {
        try {
            val rules = parseRules(rulesJson)
            staticRules[extensionId] = rules
            AppLogger.i(TAG, "Loaded ${rules.size} static DNR rules for $extensionId")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load static DNR rules for $extensionId", e)
        }
    }

    /**
     * extension rules.
     *
     * @param extensionId extension ID.
     * @param addRulesJson rules JSON.
     * @param removeRuleIdsJson rules ID JSON.
     */
    fun updateDynamicRules(
        extensionId: String,
        addRulesJson: String,
        removeRuleIdsJson: String
    ) {
        try {
            val removeIds = parseIntArray(removeRuleIdsJson).toSet()
            val addRules = parseRules(addRulesJson)

            val existing = dynamicRules.getOrPut(extensionId) { mutableListOf() }
            if (removeIds.isNotEmpty()) {
                existing.removeAll { it.id in removeIds }
            }
            existing.addAll(addRules)
            AppLogger.d(TAG, "Updated dynamic DNR rules for $extensionId: removed=${removeIds.size}, added=${addRules.size}, total=${existing.size}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update dynamic DNR rules for $extensionId", e)
        }
    }

    /**
     * extension rules.
     */
    fun updateSessionRules(
        extensionId: String,
        addRulesJson: String,
        removeRuleIdsJson: String
    ) {
        try {
            val removeIds = parseIntArray(removeRuleIdsJson).toSet()
            val addRules = parseRules(addRulesJson)

            val existing = sessionRules.getOrPut(extensionId) { mutableListOf() }
            if (removeIds.isNotEmpty()) {
                existing.removeAll { it.id in removeIds }
            }
            existing.addAll(addRules)
            AppLogger.d(TAG, "Updated session DNR rules for $extensionId: removed=${removeIds.size}, added=${addRules.size}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update session DNR rules for $extensionId", e)
        }
    }

    /**
     * Getextension rules JSON.
     */
    fun getDynamicRulesJson(extensionId: String): String {
        val rules = dynamicRules[extensionId] ?: return "[]"
        return JSONArray().apply {
            rules.forEach { rule -> put(ruleToJson(rule)) }
        }.toString()
    }

    /**
     * request is intercept.
     * in shouldInterceptRequest after use thread-safe.
     *
     * @param url request URL.
     * @param resourceType resource type.
     * @param initiatorDomain request.
     * @param method HTTP.
     * @return: null= not, BLOCK=, REDIRECT= (url in redirectUrl), ALLOW=.
     */
    fun evaluate(
        url: String,
        resourceType: String = "",
        initiatorDomain: String = "",
        method: String = "GET"
    ): EvalResult? {
        val allRuleSets = mutableListOf<List<DnrRule>>()

        // Collect all rules from all extensions (session > dynamic > static priority)
        for ((_, rules) in sessionRules) allRuleSets.add(rules)
        for ((_, rules) in dynamicRules) allRuleSets.add(rules)
        for ((_, rules) in staticRules) allRuleSets.add(rules)

        if (allRuleSets.all { it.isEmpty() }) return null

        val resType = ResourceType.fromString(resourceType)
        var bestMatch: Pair<DnrRule, Int>? = null // rule + effective priority

        for (ruleSet in allRuleSets) {
            for (rule in ruleSet) {
                if (!matchesRule(rule, url, resType, initiatorDomain, method)) continue

                val effectivePriority = rule.priority
                if (bestMatch == null || effectivePriority > bestMatch.second) {
                    bestMatch = rule to effectivePriority
                }
            }
        }

        val matchedRule = bestMatch?.first ?: return null
        matchedCount++

        return when (matchedRule.action) {
            ActionType.BLOCK -> EvalResult(ActionType.BLOCK, null)
            ActionType.ALLOW, ActionType.ALLOW_ALL_REQUESTS -> EvalResult(ActionType.ALLOW, null)
            ActionType.REDIRECT -> EvalResult(ActionType.REDIRECT, matchedRule.redirectUrl)
            ActionType.UPGRADE_SCHEME -> {
                val upgraded = url.replaceFirst("http://", "https://")
                EvalResult(ActionType.REDIRECT, upgraded)
            }
            ActionType.MODIFY_HEADERS -> null // Header modification not supported in shouldInterceptRequest
        }
    }

    data class EvalResult(
        val action: ActionType,
        val redirectUrl: String?
    )

    /**
     * extension rules.
     */
    fun clearExtension(extensionId: String) {
        staticRules.remove(extensionId)
        dynamicRules.remove(extensionId)
        sessionRules.remove(extensionId)
    }

    /**
     * rules.
     */
    fun clear() {
        staticRules.clear()
        dynamicRules.clear()
        sessionRules.clear()
        matchedCount = 0
    }

    // ===== =====.

    private fun matchesRule(
        rule: DnrRule,
        url: String,
        resType: ResourceType?,
        initiatorDomain: String,
        method: String
    ): Boolean {
        // URL filter
        val urlMatched = when {
            rule.urlFilter != null -> rule.urlFilter.matcher(url).find()
            rule.regexFilter != null -> rule.regexFilter.matcher(url).find()
            else -> true // No filter = match all
        }
        if (!urlMatched) return false

        // Resource type
        if (resType != null) {
            if (rule.resourceTypes.isNotEmpty() && resType !in rule.resourceTypes) return false
            if (resType in rule.excludedResourceTypes) return false
        }

        // Domain
        if (initiatorDomain.isNotEmpty()) {
            if (rule.domains.isNotEmpty() && !matchesDomain(initiatorDomain, rule.domains)) return false
            if (matchesDomain(initiatorDomain, rule.excludedDomains)) return false
        }

        // Method
        val upperMethod = method.uppercase()
        if (rule.requestMethods.isNotEmpty() && upperMethod !in rule.requestMethods) return false
        if (upperMethod in rule.excludedRequestMethods) return false

        return true
    }

    private fun matchesDomain(domain: String, domainSet: Set<String>): Boolean {
        if (domainSet.isEmpty()) return false
        return domainSet.any { d ->
            domain == d || domain.endsWith(".$d")
        }
    }

    /**
     * DNR rules JSON .
     */
    private fun parseRules(json: String): List<DnrRule> {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()

        val arr = JSONArray(trimmed)
        val rules = mutableListOf<DnrRule>()
        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
                parseRule(obj)?.let { rules.add(it) }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to parse DNR rule at index $i", e)
            }
        }
        return rules
    }

    private fun parseRule(obj: JSONObject): DnrRule? {
        val id = obj.optInt("id", -1)
        if (id < 0) return null

        val priority = obj.optInt("priority", 1)

        // Action
        val actionObj = obj.optJSONObject("action") ?: return null
        val actionType = when (actionObj.optString("type", "")) {
            "block" -> ActionType.BLOCK
            "allow" -> ActionType.ALLOW
            "redirect" -> ActionType.REDIRECT
            "modifyHeaders" -> ActionType.MODIFY_HEADERS
            "upgradeScheme" -> ActionType.UPGRADE_SCHEME
            "allowAllRequests" -> ActionType.ALLOW_ALL_REQUESTS
            else -> return null
        }

        val redirectUrl = actionObj.optJSONObject("redirect")?.optString("url")
            ?: actionObj.optJSONObject("redirect")?.optString("extensionPath")

        // Condition
        val condition = obj.optJSONObject("condition")
        val urlFilter = condition?.optString("urlFilter", "")?.takeIf { it.isNotEmpty() }?.let {
            compileUrlFilter(it)
        }
        val regexFilter = condition?.optString("regexFilter", "")?.takeIf { it.isNotEmpty() }?.let {
            try { Pattern.compile(it, Pattern.CASE_INSENSITIVE) } catch (_: Exception) { null }
        }

        val resourceTypes = parseResourceTypes(condition?.optJSONArray("resourceTypes"))
        val excludedResourceTypes = parseResourceTypes(condition?.optJSONArray("excludedResourceTypes"))
        val domains = parseStringSet(condition?.optJSONArray("initiatorDomains")
            ?: condition?.optJSONArray("domains"))
        val excludedDomains = parseStringSet(condition?.optJSONArray("excludedInitiatorDomains")
            ?: condition?.optJSONArray("excludedDomains"))
        val requestMethods = parseStringSet(condition?.optJSONArray("requestMethods")).map { it.uppercase() }.toSet()
        val excludedRequestMethods = parseStringSet(condition?.optJSONArray("excludedRequestMethods")).map { it.uppercase() }.toSet()

        return DnrRule(
            id = id,
            priority = priority,
            action = actionType,
            redirectUrl = redirectUrl,
            urlFilter = urlFilter,
            regexFilter = regexFilter,
            resourceTypes = resourceTypes,
            excludedResourceTypes = excludedResourceTypes,
            domains = domains,
            excludedDomains = excludedDomains,
            requestMethods = requestMethods,
            excludedRequestMethods = excludedRequestMethods
        )
    }

    /**
     * DNR urlFilter as .
     * DNR urlFilter:
     * - || to.
     * - | to URL or.
     * - *.
     * - ^.
     */
    private fun compileUrlFilter(filter: String): Pattern? {
        return try {
            val sb = StringBuilder()
            var i = 0
            val len = filter.length

            // Start anchor
            if (filter.startsWith("||")) {
                sb.append("^https?://([^/]*\\.)?")
                i = 2
            } else if (filter.startsWith("|")) {
                sb.append("^")
                i = 1
            }

            while (i < len) {
                val c = filter[i]
                when {
                    c == '*' -> sb.append(".*")
                    c == '^' -> sb.append("[^\\w\\-.~%]|$")
                    c == '|' && i == len - 1 -> sb.append("$")
                    c in ".+?{}()[]\\$" -> sb.append("\\").append(c)
                    else -> sb.append(c)
                }
                i++
            }

            Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to compile DNR urlFilter: $filter", e)
            null
        }
    }

    private fun parseResourceTypes(arr: JSONArray?): Set<ResourceType> {
        if (arr == null || arr.length() == 0) return emptySet()
        return (0 until arr.length()).mapNotNull { i ->
            ResourceType.fromString(arr.optString(i, ""))
        }.toSet()
    }

    private fun parseStringSet(arr: JSONArray?): Set<String> {
        if (arr == null || arr.length() == 0) return emptySet()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optString(i, "").takeIf { it.isNotEmpty() }
        }.toSet()
    }

    private fun parseIntArray(json: String): List<Int> {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        return try {
            val arr = JSONArray(trimmed)
            (0 until arr.length()).map { arr.getInt(it) }
        } catch (_: Exception) { emptyList() }
    }

    private fun ruleToJson(rule: DnrRule): JSONObject {
        return JSONObject().apply {
            put("id", rule.id)
            put("priority", rule.priority)
            put("action", JSONObject().apply {
                put("type", rule.action.name.lowercase())
                if (rule.redirectUrl != null) {
                    put("redirect", JSONObject().put("url", rule.redirectUrl))
                }
            })
        }
    }
}