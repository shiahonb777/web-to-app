package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger






object UserScriptParser {

    private const val TAG = "UserScriptParser"

    private val METADATA_BLOCK_REGEX = Regex(
        """//\s*==UserScript==\s*\n(.*?)//\s*==/UserScript==""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val METADATA_LINE_REGEX = Regex(
        """//\s*@(\S+)\s+(.+)"""
    )




    data class ParseResult(
        val module: ExtensionModule,
        val isValid: Boolean,
        val warnings: List<String> = emptyList()
    )







    fun parse(scriptContent: String, fileName: String = ""): ParseResult {
        val metadataMatch = METADATA_BLOCK_REGEX.find(scriptContent)

        if (metadataMatch == null) {

            val fallbackName = fileName.removeSuffix(".user.js").removeSuffix(".js").ifBlank { "Unnamed Script" }
            return ParseResult(
                module = ExtensionModule(
                    name = fallbackName,
                    code = scriptContent,
                    sourceType = ModuleSourceType.USERSCRIPT,
                    runAt = ModuleRunTime.DOCUMENT_END,
                    category = ModuleCategory.FUNCTION_ENHANCE,
                    runMode = ModuleRunMode.AUTO
                ),
                isValid = false,
                warnings = listOf("No ==UserScript== metadata block found, imported as plain JS")
            )
        }

        val metadataBlock = metadataMatch.groupValues[1]
        val warnings = mutableListOf<String>()


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


        val urlMatchRules = mutableListOf<UrlMatchRule>()


        matches.forEach { pattern ->
            urlMatchRules.add(UrlMatchRule(
                pattern = convertMatchPattern(pattern),
                isRegex = false,
                exclude = false
            ))
        }


        includes.forEach { pattern ->
            val (converted, isRegex) = convertIncludePattern(pattern)
            urlMatchRules.add(UrlMatchRule(
                pattern = converted,
                isRegex = isRegex,
                exclude = false
            ))
        }


        excludes.forEach { pattern ->
            val (converted, isRegex) = convertIncludePattern(pattern)
            urlMatchRules.add(UrlMatchRule(
                pattern = converted,
                isRegex = isRegex,
                exclude = true
            ))
        }


        val moduleRunAt = when (runAt) {
            "document-start" -> ModuleRunTime.DOCUMENT_START
            "document-end" -> ModuleRunTime.DOCUMENT_END
            "document-idle" -> ModuleRunTime.DOCUMENT_IDLE
            "document-body" -> ModuleRunTime.DOCUMENT_END
            else -> ModuleRunTime.DOCUMENT_IDLE
        }


        val effectiveGrants = grants.filter { it != "none" && it.isNotBlank() }


        val module = ExtensionModule(
            name = name,
            description = description,
            icon = if (icon.isNotBlank()) "🔧" else "🐵",
            category = ModuleCategory.FUNCTION_ENHANCE,
            version = ModuleVersion(name = version),
            author = if (author.isNotBlank()) ModuleAuthor(
                name = author,
                url = homepage.ifBlank { null }
            ) else null,
            code = scriptContent,
            runAt = moduleRunAt,
            urlMatches = urlMatchRules,
            enabled = true,
            sourceType = ModuleSourceType.USERSCRIPT,
            runMode = ModuleRunMode.AUTO,
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




    fun isUserScript(content: String): Boolean {
        return METADATA_BLOCK_REGEX.containsMatchIn(content)
    }




    private fun convertMatchPattern(pattern: String): String {
        if (pattern == "<all_urls>") return "*"

        return pattern
    }




    private fun convertIncludePattern(pattern: String): Pair<String, Boolean> {

        if (pattern.startsWith("/") && pattern.length > 2) {
            val lastSlash = pattern.lastIndexOf('/')
            if (lastSlash > 0) {
                val regexBody = pattern.substring(1, lastSlash)
                return Pair(regexBody, true)
            }
        }

        return Pair(pattern, false)
    }




    fun extractCodeBody(scriptContent: String): String {
        return METADATA_BLOCK_REGEX.replace(scriptContent, "").trim()
    }
}
