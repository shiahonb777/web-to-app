package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger

/**
 * 油猴脚本 (.user.js) 解析器
 * 
 * 解析 ==UserScript== 元数据头，将油猴脚本转换为 ExtensionModule
 */
object UserScriptParser {
    
    private const val TAG = "UserScriptParser"
    
    private val METADATA_BLOCK_REGEX = Regex(
        """//\s*==UserScript==\s*\n(.*?)//\s*==/UserScript==""",
        RegexOption.DOT_MATCHES_ALL
    )
    
    private val METADATA_LINE_REGEX = Regex(
        """//\s*@(\S+)\s+(.+)"""
    )
    
    /**
     * 解析结果
     */
    data class ParseResult(
        val module: ExtensionModule,
        val isValid: Boolean,
        val warnings: List<String> = emptyList()
    )
    
    /**
     * 解析油猴脚本文本
     * @param scriptContent 脚本全文内容
     * @param fileName 文件名（作为 fallback name）
     * @return 解析结果，如果不是有效的油猴脚本则 isValid=false
     */
    fun parse(scriptContent: String, fileName: String = ""): ParseResult {
        val metadataMatch = METADATA_BLOCK_REGEX.find(scriptContent)
        
        if (metadataMatch == null) {
            // 不是油猴脚本格式，但仍可作为普通 JS 导入
            val fallbackName = fileName.removeSuffix(".user.js").removeSuffix(".js").ifBlank { "Unnamed Script" }
            return ParseResult(
                module = ExtensionModule(
                    name = fallbackName,
                    code = scriptContent,
                    sourceType = ModuleSourceType.USERSCRIPT,
                    runAt = ModuleRunTime.DOCUMENT_END,
                    category = ModuleCategory.FUNCTION_ENHANCE
                ),
                isValid = false,
                warnings = listOf("No ==UserScript== metadata block found, imported as plain JS")
            )
        }
        
        val metadataBlock = metadataMatch.groupValues[1]
        val warnings = mutableListOf<String>()
        
        // 解析元数据字段
        var name = ""
        var description = ""
        var version = "1.0.0"
        var author = ""
        var namespace = ""
        val matches = mutableListOf<String>()
        val includes = mutableListOf<String>()
        val excludes = mutableListOf<String>()
        var runAt = "document-idle"
        val grants = mutableListOf<String>()
        val requires = mutableListOf<String>()
        val resources = mutableMapOf<String, String>()
        var noframes = false
        var icon = ""
        var homepage = ""
        
        metadataBlock.lines().forEach { line ->
            val match = METADATA_LINE_REGEX.find(line) ?: return@forEach
            val key = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            
            when (key) {
                "name" -> name = value
                "description", "desc" -> description = value
                "version" -> version = value
                "author" -> author = value
                "namespace" -> namespace = value
                "match" -> matches.add(value)
                "include" -> includes.add(value)
                "exclude", "exclude-match" -> excludes.add(value)
                "run-at" -> runAt = value
                "grant" -> grants.add(value)
                "require" -> requires.add(value)
                "resource" -> {
                    // @resource name url
                    val parts = value.split(Regex("\\s+"), limit = 2)
                    if (parts.size == 2) {
                        resources[parts[0]] = parts[1]
                    }
                }
                "noframes" -> noframes = true
                "icon", "iconURL", "icon64", "icon64URL" -> icon = value
                "homepage", "homepageURL", "website" -> homepage = value
            }
        }
        
        if (name.isBlank()) {
            name = fileName.removeSuffix(".user.js").removeSuffix(".js").ifBlank { "Unnamed Script" }
        }
        
        // 转换 URL 匹配规则
        val urlMatchRules = mutableListOf<UrlMatchRule>()
        
        // @match 使用 Chrome match pattern 格式
        matches.forEach { pattern ->
            urlMatchRules.add(UrlMatchRule(
                pattern = convertMatchPattern(pattern),
                isRegex = false,
                exclude = false
            ))
        }
        
        // @include 可能是通配符或正则
        includes.forEach { pattern ->
            val (converted, isRegex) = convertIncludePattern(pattern)
            urlMatchRules.add(UrlMatchRule(
                pattern = converted,
                isRegex = isRegex,
                exclude = false
            ))
        }
        
        // @exclude
        excludes.forEach { pattern ->
            val (converted, isRegex) = convertIncludePattern(pattern)
            urlMatchRules.add(UrlMatchRule(
                pattern = converted,
                isRegex = isRegex,
                exclude = true
            ))
        }
        
        // 转换 runAt
        val moduleRunAt = when (runAt) {
            "document-start" -> ModuleRunTime.DOCUMENT_START
            "document-end" -> ModuleRunTime.DOCUMENT_END
            "document-idle" -> ModuleRunTime.DOCUMENT_IDLE
            "document-body" -> ModuleRunTime.DOCUMENT_END // fallback
            else -> ModuleRunTime.DOCUMENT_IDLE
        }
        
        // 过滤掉 "none" grant
        val effectiveGrants = grants.filter { it != "none" && it.isNotBlank() }
        
        // 构建 ExtensionModule
        val module = ExtensionModule(
            name = name,
            description = description,
            icon = if (icon.isNotBlank()) "🔧" else "🐵", // 油猴图标
            category = ModuleCategory.FUNCTION_ENHANCE,
            version = ModuleVersion(name = version),
            author = if (author.isNotBlank()) ModuleAuthor(
                name = author,
                url = homepage.ifBlank { null }
            ) else null,
            code = scriptContent, // 保留完整脚本（包含元数据头）
            runAt = moduleRunAt,
            urlMatches = urlMatchRules,
            enabled = true,
            sourceType = ModuleSourceType.USERSCRIPT,
            gmGrants = effectiveGrants,
            requireUrls = requires,
            resources = resources,
            noframes = noframes
        )
        
        AppLogger.i(TAG, "Parsed userscript: name='$name', " +
            "matches=${urlMatchRules.size}, grants=${effectiveGrants.size}, " +
            "requires=${requires.size}, resources=${resources.size}")
        
        return ParseResult(
            module = module,
            isValid = true,
            warnings = warnings
        )
    }
    
    /**
     * 检测文本是否包含油猴脚本元数据头
     */
    fun isUserScript(content: String): Boolean {
        return METADATA_BLOCK_REGEX.containsMatchIn(content)
    }
    
    // 转换 Chrome match pattern 为通配符模式
    // 格式: scheme://host/path
    // 特殊: <all_urls> -> *
    private fun convertMatchPattern(pattern: String): String {
        if (pattern == "<all_urls>") return "*"
        // Chrome match pattern 本身就是通配符格式，可直接使用
        return pattern
    }
    
    // 转换 @include 模式
    // 可能是通配符、正则 (/regex/) 或简单 URL
    // @return Pair(pattern, isRegex)
    private fun convertIncludePattern(pattern: String): Pair<String, Boolean> {
        // 正则模式: /pattern/ 或 /pattern/flags
        if (pattern.startsWith("/") && pattern.length > 2) {
            val lastSlash = pattern.lastIndexOf('/')
            if (lastSlash > 0) {
                val regexBody = pattern.substring(1, lastSlash)
                return Pair(regexBody, true)
            }
        }
        // 通配符模式
        return Pair(pattern, false)
    }
    
    /**
     * 从脚本内容中提取纯代码（去掉元数据头）
     */
    fun extractCodeBody(scriptContent: String): String {
        return METADATA_BLOCK_REGEX.replace(scriptContent, "").trim()
    }
}
