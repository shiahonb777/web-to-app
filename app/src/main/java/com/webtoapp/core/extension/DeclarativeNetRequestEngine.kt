package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * MV3 declarativeNetRequest 规则引擎
 *
 * 解析扩展的 declarativeNetRequest JSON 规则文件，在原生层高效匹配。
 * 支持的 action types:
 * - block: 阻断请求
 * - allow: 允许请求（优先级高于 block）
 * - redirect: 重定向到指定 URL
 * - modifyHeaders: 修改请求/响应头
 *
 * 规则格式参考: https://developer.chrome.com/docs/extensions/reference/api/declarativeNetRequest
 */
object DeclarativeNetRequestEngine {

    private const val TAG = "DNREngine"

    /**
     * 规则 action 类型
     */
    enum class ActionType {
        BLOCK, ALLOW, REDIRECT, MODIFY_HEADERS, UPGRADE_SCHEME, ALLOW_ALL_REQUESTS
    }

    /**
     * 资源类型
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
     * 单条 DNR 规则
     */
    data class DnrRule(
        val id: Int,
        val priority: Int,
        val action: ActionType,
        val redirectUrl: String?,               // 仅 REDIRECT 使用
        val urlFilter: Pattern?,                // 编译后的 URL 过滤正则
        val regexFilter: Pattern?,              // 正则过滤 (与 urlFilter 互斥)
        val resourceTypes: Set<ResourceType>,   // 空=匹配全部
        val excludedResourceTypes: Set<ResourceType>,
        val domains: Set<String>,               // initiator domains (空=全部)
        val excludedDomains: Set<String>,
        val requestMethods: Set<String>,        // 空=全部
        val excludedRequestMethods: Set<String>
    )

    // 每个扩展的规则: extId -> (static rules + dynamic rules)
    private val staticRules = ConcurrentHashMap<String, List<DnrRule>>()
    private val dynamicRules = ConcurrentHashMap<String, MutableList<DnrRule>>()
    private val sessionRules = ConcurrentHashMap<String, MutableList<DnrRule>>()

    // 匹配统计
    @Volatile
    var matchedCount: Long = 0
        private set

    /**
     * 加载扩展的静态规则文件。
     *
     * @param extensionId 扩展 ID
     * @param rulesJson JSON 数组字符串（从 rule_resources 文件读取）
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
     * 更新扩展的动态规则。
     *
     * @param extensionId 扩展 ID
     * @param addRulesJson 要添加的规则 JSON 数组
     * @param removeRuleIdsJson 要移除的规则 ID JSON 数组
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
     * 更新扩展的会话规则。
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
     * 获取扩展的动态规则 JSON。
     */
    fun getDynamicRulesJson(extensionId: String): String {
        val rules = dynamicRules[extensionId] ?: return "[]"
        return JSONArray().apply {
            rules.forEach { rule -> put(ruleToJson(rule)) }
        }.toString()
    }

    /**
     * 评估请求是否应被拦截。
     * 在 shouldInterceptRequest 后台线程调用，必须线程安全。
     *
     * @param url 请求 URL
     * @param resourceType 资源类型字符串
     * @param initiatorDomain 发起请求的域名
     * @param method HTTP 方法
     * @return 匹配结果: null=不匹配, BLOCK=阻断, REDIRECT=重定向(url in redirectUrl), ALLOW=明确允许
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
     * 清除指定扩展的所有规则。
     */
    fun clearExtension(extensionId: String) {
        staticRules.remove(extensionId)
        dynamicRules.remove(extensionId)
        sessionRules.remove(extensionId)
    }

    /**
     * 清除所有规则。
     */
    fun clear() {
        staticRules.clear()
        dynamicRules.clear()
        sessionRules.clear()
        matchedCount = 0
    }

    // ===== 内部方法 =====

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
     * 解析 DNR 规则 JSON 数组。
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
     * 编译 DNR urlFilter 为正则。
     * DNR urlFilter 语法:
     * - || 锚定到域名开始
     * - | 锚定到 URL 开始或结尾
     * - * 通配符
     * - ^ 分隔符 (非字母数字和 -._~)
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
