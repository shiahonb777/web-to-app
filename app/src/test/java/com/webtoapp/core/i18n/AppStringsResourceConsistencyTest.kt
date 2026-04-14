package com.webtoapp.core.i18n

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppStringsResourceConsistencyTest {

    @Test
    fun `grouped app strings stay aligned across locales`() {
        val resDir = resolveExistingDir("app/src/main/res", "src/main/res")
        val defaultDir = File(resDir, "values")
        val localeDirs = listOf("values-zh", "values-en", "values-ar")
        val defaultFiles = defaultDir.listFiles { file ->
            file.isFile && file.name.startsWith("app_strings_") && file.name.endsWith(".xml")
        }?.sortedBy { it.name }.orEmpty()

        assertThat(defaultFiles).isNotEmpty()
        val expectedFileNames = defaultFiles.map { it.name }

        localeDirs.forEach { localeDir ->
            val actualNames = File(resDir, localeDir)
                .listFiles { file ->
                    file.isFile && file.name.startsWith("app_strings_") && file.name.endsWith(".xml")
                }
                ?.map { it.name }
                ?.sorted()
                .orEmpty()
            assertWithMessage("Locale file set mismatch for $localeDir")
                .that(actualNames)
                .containsExactlyElementsIn(expectedFileNames)
        }

        defaultFiles.forEach { defaultFile ->
            val expectedKeys = readStringKeys(defaultFile)
            localeDirs.forEach { localeDir ->
                val localeFile = File(resDir, "$localeDir/${defaultFile.name}")
                val actualKeys = readStringKeys(localeFile)
                assertWithMessage("Key mismatch for ${defaultFile.name} in $localeDir")
                    .that(actualKeys)
                    .containsExactlyElementsIn(expectedKeys)
            }
        }
    }

    private fun readStringKeys(file: File): List<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildList {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index)
                val name = element.attributes?.getNamedItem("name")?.nodeValue
                if (!name.isNullOrBlank()) {
                    add(name)
                }
            }
        }
    }

    private fun resolveExistingDir(vararg candidates: String): File {
        return candidates
            .asSequence()
            .map(::File)
            .firstOrNull(File::exists)
            ?: error("Cannot locate resource directory from: ${candidates.joinToString()}")
    }
}
