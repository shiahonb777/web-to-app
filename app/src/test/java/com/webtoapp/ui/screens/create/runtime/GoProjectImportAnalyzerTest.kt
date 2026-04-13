package com.webtoapp.ui.screens.create.runtime

import com.webtoapp.ui.screens.create.common.ProjectImportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GoProjectImportAnalyzerTest {

    @Test
    fun `analyze go project parses mod and static dir`() {
        val projectDir = Files.createTempDirectory("go-project").toFile()
        try {
            File(projectDir, "go.mod").writeText(
                """
                module github.com/example/demo

                go 1.21

                require (
                        github.com/gin-gonic/gin v1.8.0
                        github.com/gorilla/mux v1.8.0
                )
                """.trimIndent()
            )
            File(projectDir, "main.go").writeText("package main\n\nimport \"github.com/gin-gonic/gin\"")
            File(projectDir, ".env").writeText("PORT=8090")
            File(projectDir, "static").apply {
                mkdirs()
                File(this, "index.html").writeText("<html></html>")
            }

            val analysis = GoProjectImportAnalyzer().analyze(projectDir)

            assertEquals("gin", analysis.framework)
            assertEquals("main.go", analysis.entryFile)
            assertEquals("github.com/example/demo", analysis.modulePath)
            assertEquals("1.21", analysis.goVersion)
            assertEquals("static", analysis.staticDir)
            assertEquals("", analysis.binaryName)
            assertFalse(analysis.binaryDetected)
            assertEquals("/health", analysis.healthCheckEndpoint)
            assertEquals("demo", analysis.suggestedAppName)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test(expected = ProjectImportException::class)
    fun `analyze missing go project throws`() {
        val projectDir = Files.createTempDirectory("go-project-missing").toFile()
        projectDir.deleteRecursively()
        GoProjectImportAnalyzer().analyze(projectDir)
    }
}
