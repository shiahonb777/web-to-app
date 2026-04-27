package com.webtoapp.ui.screens.create.runtime

import com.webtoapp.ui.screens.create.common.ProjectImportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PythonProjectImportAnalyzerTest {

    @Test
    fun `analyze django project fills metadata and requirements`() {
        val projectDir = Files.createTempDirectory("python-project").toFile()
        try {
            File(projectDir, "manage.py").writeText(
                """
                import os
                os.environ.setdefault("DJANGO_SETTINGS_MODULE", "demo.settings")
                """.trimIndent()
            )
            File(projectDir, "pyproject.toml").writeText(
                """
                [tool.poetry]
                name = "demo-django"
                version = "0.1.0"

                [tool.poetry.dependencies]
                python = "^3.11"
                """.trimIndent()
            )
            File(projectDir, "requirements.txt").writeText("Django==4.2\n")
            File(projectDir, ".env").writeText("DJANGO_DEBUG=1")
            File(projectDir, ".venv").apply {
                mkdirs()
                File(this, "bin").apply {
                    mkdirs()
                    File(this, "python").writeText("echo python")
                }
            }

            val analysis = PythonProjectImportAnalyzer().analyze(projectDir)

            assertEquals("django", analysis.framework)
            assertEquals("manage.py", analysis.entryFile)
            assertEquals("gunicorn", analysis.serverType)
            assertTrue(analysis.envVars.containsKey("DJANGO_DEBUG"))
            assertEquals(".venv", analysis.venvPath)
            assertTrue(analysis.venvDetected)
            assertEquals("demo.settings", analysis.djangoSettingsModule)
            assertEquals("demo.settings.wsgi:application", analysis.entryModule)
            assertEquals("requirements.txt", analysis.requirementsSource)
            assertEquals("Django", analysis.requirements.first().first)
            assertEquals("==4.2", analysis.requirements.first().second)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test(expected = ProjectImportException::class)
    fun `analyze missing python directory throws`() {
        val projectDir = Files.createTempDirectory("python-project-missing").toFile()
        projectDir.deleteRecursively()
        PythonProjectImportAnalyzer().analyze(projectDir)
    }
}
