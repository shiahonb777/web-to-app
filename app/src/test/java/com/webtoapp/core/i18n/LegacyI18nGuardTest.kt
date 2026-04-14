package com.webtoapp.core.i18n

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

class LegacyI18nGuardTest {

    private val inlineLanguageBranchPattern = Regex(
        "AppLanguage\\.(CHINESE|ENGLISH|ARABIC)\\s*->\\s*\""
    )

    private val inlineCurrentLanguagePattern = Regex(
        "AppStringsProvider\\.currentLanguage\\s*==\\s*(?:AppLanguage|com\\.webtoapp\\.core\\.i18n\\.AppLanguage)\\.(CHINESE|ENGLISH|ARABIC)\\s*\\)\\s*\""
    )

    private val legacyFacadeUsagePattern = Regex("\\bStrings\\.")

    private val allowedInlineBranchFiles = setOf(
        "com/webtoapp/core/ai/coding/SimplePrompts.kt",
        "com/webtoapp/core/extension/agent/EnhancedAgentEngine.kt",
        "com/webtoapp/core/frontend/SampleProjectManager.kt",
        "com/webtoapp/core/i18n/AiPromptManager.kt",
        "com/webtoapp/core/sample/SampleProjectExtractor.kt",
    )

    @Test
    fun `legacy strings facade is removed`() {
        val mainJavaDir = resolveExistingDir("app/src/main/java", "src/main/java")
        assertThat(File(mainJavaDir, "com/webtoapp/core/i18n/Strings.kt").exists()).isFalse()

        val legacyDir = File(mainJavaDir, "com/webtoapp/core/i18n/strings")
        val legacyFiles = if (legacyDir.exists()) {
            legacyDir.walkTopDown().filter { it.isFile }.toList()
        } else {
            emptyList()
        }
        assertThat(legacyFiles).isEmpty()
    }

    @Test
    fun `ui and business code no longer define multilingual literals inline`() {
        val mainJavaDir = resolveExistingDir("app/src/main/java", "src/main/java")
        val scanRoots = listOf(
            File(mainJavaDir, "com/webtoapp/ui"),
            File(mainJavaDir, "com/webtoapp/core"),
            File(mainJavaDir, "com/webtoapp/util"),
            File(mainJavaDir, "com/webtoapp/data"),
        )

        val offenders = scanRoots
            .filter(File::exists)
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .mapNotNull { file ->
                        val relativePath = file.invariantSeparatorsPath
                            .substringAfter("com/webtoapp/", missingDelimiterValue = file.invariantSeparatorsPath)
                            .let { "com/webtoapp/$it" }
                        if (relativePath in allowedInlineBranchFiles) {
                            return@mapNotNull null
                        }

                        val text = file.readText(Charsets.UTF_8)
                        when {
                            legacyFacadeUsagePattern.containsMatchIn(text) -> "$relativePath contains legacy Strings usage"
                            inlineLanguageBranchPattern.containsMatchIn(text) -> relativePath
                            inlineCurrentLanguagePattern.containsMatchIn(text) -> relativePath
                            else -> null
                        }
                    }
                    .toList()
            }

        assertWithMessage(
            "Found legacy inline i18n branches:\n${offenders.joinToString(separator = "\n")}"
        ).that(offenders).isEmpty()
    }

    private fun resolveExistingDir(vararg candidates: String): File {
        return candidates
            .asSequence()
            .map(::File)
            .firstOrNull(File::exists)
            ?: error("Cannot locate source directory from: ${candidates.joinToString()}")
    }
}
