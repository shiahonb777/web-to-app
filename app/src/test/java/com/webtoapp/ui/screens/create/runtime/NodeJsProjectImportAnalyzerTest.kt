package com.webtoapp.ui.screens.create.runtime

import com.webtoapp.ui.screens.create.common.ProjectImportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class NodeJsProjectImportAnalyzerTest {

    @Test
    fun `analyze express project returns expected metadata`() {
        val projectDir = Files.createTempDirectory("node-project").toFile()
        try {
            File(projectDir, "package.json").writeText(
                """
                {
                  "name": "demo-node-app",
                  "version": "1.2.3",
                  "description": "sample node server",
                  "main": "server.js",
                  "scripts": {
                    "start": "node server.js --port 5555",
                    "dev": "node server-dev.js"
                  },
                  "dependencies": {
                    "express": "^4.18.2"
                  },
                  "devDependencies": {
                    "typescript": "^5.0.4"
                  }
                }
                """.trimIndent()
            )
            File(projectDir, "server.js").writeText("console.log('hello');")
            File(projectDir, ".env").writeText("PORT=6000")

            val analysis = NodeJsProjectImportAnalyzer().analyze(projectDir)

            assertEquals("Express", analysis.framework)
            assertEquals("server.js", analysis.entryFile)
            assertEquals(5555, analysis.detectedPort)
            assertEquals("npm", analysis.packageManager)
            assertTrue(analysis.dependencies.containsKey("express"))
            assertTrue(analysis.devDependencies.containsKey("typescript"))
            assertEquals("start", analysis.selectedStartScript)
            assertEquals("demo-node-app", analysis.packageName)
            assertEquals("1.2.3", analysis.packageVersion)
            assertEquals("sample node server", analysis.packageDescription)
            assertTrue(analysis.hasTypeScript)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test(expected = ProjectImportException::class)
    fun `analyze without package json throws`() {
        val projectDir = Files.createTempDirectory("node-project-missing-package").toFile()
        try {
            NodeJsProjectImportAnalyzer().analyze(projectDir)
        } finally {
            projectDir.deleteRecursively()
        }
    }
}
