package com.webtoapp.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class ApkConfigWiringGuardTest {

    private val apkBuilderFile: File by lazy {
        val candidates = listOf(
            "src/main/java/com/webtoapp/core/apkbuilder/ApkBuilder.kt",
            "app/src/main/java/com/webtoapp/core/apkbuilder/ApkBuilder.kt"
        )
        candidates.map(::File).firstOrNull { it.exists() }
            ?: error("ApkBuilder.kt not found at any of: $candidates (cwd=${File(".").absolutePath})")
    }

    @Test
    fun `apk builder file is reachable from test working directory`() {
        assertThat(apkBuilderFile.exists()).isTrue()
        assertThat(apkBuilderFile.isFile).isTrue()
    }

    @Test
    fun `buildXxxBlock functions do not hardcode user-facing field literals`() {
        val text = apkBuilderFile.readText()
        val blocks = extractBuildBlocks(text)
        assertThat(blocks).isNotEmpty()

        val violations = mutableListOf<String>()
        for (block in blocks) {
            for (hit in scanForLiterals(block.body)) {
                if (hit.value in WHITELISTED_INTERNALS) continue
                violations.add("${block.name} (L${block.startLine}): `${hit.field} = ${hit.value}`")
            }
        }

        assertThat(violations).isEmpty()
    }

    private data class BuildBlock(
        val name: String,
        val startLine: Int,
        val body: String
    )

    private data class LiteralHit(
        val field: String,
        val value: String
    )

    private fun extractBuildBlocks(text: String): List<BuildBlock> {
        val lines = text.lines()
        val starts = mutableListOf<Pair<Int, String>>()
        for ((idx, line) in lines.withIndex()) {
            val m = BUILD_BLOCK_HEADER.find(line) ?: continue
            starts.add(idx to m.groupValues[1])
        }
        if (starts.isEmpty()) return emptyList()
        return starts.mapIndexed { i, (startIdx, name) ->
            val endIdx = if (i + 1 < starts.size) starts[i + 1].first else lines.size
            val body = lines.subList(startIdx + 1, endIdx).joinToString("\n")
            BuildBlock(name, startIdx + 2, body)
        }
    }

    private fun scanForLiterals(body: String): List<LiteralHit> {
        val hits = mutableListOf<LiteralHit>()
        for (raw in body.lines()) {
            val line = raw.trim()
            if (line.isBlank()) continue
            if (line.startsWith("val ")) continue
            if (line.startsWith("private ") || line.startsWith("internal ")) continue
            if (line.startsWith("//")) continue
            if (line.startsWith("return ")) continue
            if (line.startsWith("add(") || line.startsWith("addAll(")) continue
            if (line.startsWith("}") || line.startsWith(")")) continue
            val m = ASSIGN.find(line) ?: continue
            val field = m.groupValues[1]
            val rhs = m.groupValues[2].trimEnd(',').trim()
            if (line.contains("?:")) continue
            if (rhs.contains("(") && rhs.contains(")")) continue
            if (rhs == "true" || rhs == "false" || rhs == "null") continue
            if (rhs.startsWith("listOf") || rhs.startsWith("emptyList")) continue
            if (rhs.startsWith("it.") || rhs.startsWith("this.")) continue
            val isStringLiteral = rhs.startsWith("\"") && rhs.endsWith("\"")
            val hasInterpolation = rhs.contains("$")
            if (isStringLiteral && !hasInterpolation) {
                hits.add(LiteralHit(field, rhs))
            } else if (NUMERIC_LITERAL.matches(rhs)) {
                hits.add(LiteralHit(field, rhs))
            }
        }
        return hits
    }

    companion object {
        private val BUILD_BLOCK_HEADER =
            Regex("^private fun WebApp\\.(build\\w+Block)\\(")
        private val ASSIGN = Regex("^(\\w+)\\s*=\\s*(.+)$")
        private val NUMERIC_LITERAL = Regex("^-?\\d+\\.?\\d*[fFlL]?$")
        private val WHITELISTED_INTERNALS = setOf(
            "\"__kernel__\"",
            "\"__perf_start__\"",
            "\"__perf_end__\""
        )
    }
}
