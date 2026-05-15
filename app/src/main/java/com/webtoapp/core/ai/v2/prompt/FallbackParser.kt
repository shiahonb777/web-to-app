package com.webtoapp.core.ai.v2.prompt

import com.webtoapp.core.ai.coding.AiCodingType

object FallbackParser {
    data class ExtractedFile(val path: String, val content: String)

    fun parse(text: String, codingType: AiCodingType): List<ExtractedFile> {
        val byMarker = parseMarkers(text); if(byMarker.isNotEmpty()) return byMarker
        val byFence = parseFences(text, codingType); if(byFence.isNotEmpty()) return byFence
        val byBare = parseBareHtml(text, codingType); if(byBare.isNotEmpty()) return byBare
        return emptyList()
    }

    private fun parseMarkers(text: String): List<ExtractedFile> {
        val re = Regex("^[ \\t]*===[ \\t]*file[ \\t]*:[ \\t]*(\\S[^=]*?)[ \\t]*===[ \\t]*$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
        val matches = re.findAll(text).toList(); if(matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { i, m ->
            val path = m.groupValues[1].trim()
            val bodyStart = m.range.last + 1
            val bodyEnd = matches.getOrNull(i+1)?.range?.first ?: text.length
            val body = stripFences(text.substring(bodyStart, bodyEnd).trim())
            if(body.isNotEmpty() && path.isNotEmpty()) ExtractedFile(path, body) else null
        }.distinctBy { it.path }
    }

    private fun parseFences(text: String, codingType: AiCodingType): List<ExtractedFile> {
        val re = Regex("```([a-zA-Z0-9_+\\-]*)\\s*\\r?\\n([\\s\\S]*?)```")
        val matches = re.findAll(text).toList(); if(matches.isEmpty()) return emptyList()
        if(matches.size == 1) return listOf(ExtractedFile(codingType.defaultEntryFile, matches[0].groupValues[2].trim()))
        return matches.mapNotNull { m ->
            val body = m.groupValues[2].trim()
            val path = sniffPath(body) ?: return@mapNotNull null
            ExtractedFile(path, body.replaceFirst(Regex("^[ \\t]*(?://|#|<!--)[ \\t]*file[ \\t]*:[^\\n]*\\r?\\n", RegexOption.IGNORE_CASE), "").trim())
        }.distinctBy { it.path }
    }

    private fun parseBareHtml(text: String, codingType: AiCodingType): List<ExtractedFile> {
        if(codingType != AiCodingType.HTML && codingType != AiCodingType.FRONTEND) return emptyList()
        val re = Regex("(<!DOCTYPE[\\s\\S]*?</html>|<html[\\s\\S]*?</html>)", RegexOption.IGNORE_CASE)
        val m = re.find(text) ?: return emptyList()
        return listOf(ExtractedFile("index.html", m.value.trim()))
    }

    private fun sniffPath(body: String): String? {
        val first = body.lineSequence().firstOrNull()?.trim().orEmpty()
        listOf(Regex("<!--\\s*file\\s*:\\s*(\\S+)\\s*-->",RegexOption.IGNORE_CASE), Regex("//\\s*file\\s*:\\s*(\\S+)",RegexOption.IGNORE_CASE), Regex("#\\s*file\\s*:\\s*(\\S+)",RegexOption.IGNORE_CASE))
            .forEach { re -> re.find(first)?.groupValues?.get(1)?.let { return it } }
        return null
    }

    private fun stripFences(body: String): String {
        val trimmed = body.trim()
        val re = Regex("^```[a-zA-Z0-9_+\\-]*\\s*\\r?\\n([\\s\\S]*?)\\r?\\n```\\s*$")
        return re.matchEntire(trimmed)?.groupValues?.get(1) ?: trimmed
    }
}
