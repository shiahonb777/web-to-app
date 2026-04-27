package com.webtoapp.ui.screens.create.runtime

import com.webtoapp.ui.screens.create.common.ProjectImportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PhpProjectImportAnalyzerTest {

    @Test
    fun `analyze laravel project reports dependencies and databases`() {
        val projectDir = Files.createTempDirectory("php-project").toFile()
        try {
            File(projectDir, "composer.json").writeText(
                """
                {
                  "name": "example/laravel-app",
                  "require": {
                    "laravel/framework": "^10.0",
                    "intervention/image": "^2.7",
                    "guzzlehttp/guzzle": "^7.0"
                  },
                  "require-dev": {
                    "phpunit/phpunit": "^10.0"
                  }
                }
                """.trimIndent()
            )
            File(projectDir, "artisan").writeText("<?php // stub ?>")
            File(projectDir, "routes").apply {
                mkdirs()
                File(this, "web.php").writeText("<?php // routes ?>")
            }
            File(projectDir, "public").apply {
                mkdirs()
                File(this, "index.php").writeText("<?php echo 'hello'; ?>")
            }
            File(projectDir, "data").apply {
                mkdirs()
                File(this, "app.db").writeText("sqlite data")
            }
            File(projectDir, ".env").writeText("APP_ENV=production")

            val analysis = PhpProjectImportAnalyzer().analyze(projectDir)

            assertEquals("laravel", analysis.framework)
            assertEquals("public", analysis.documentRoot)
            assertEquals("index.php", analysis.entryFile)
            assertTrue(analysis.composerDependencies.containsKey("laravel/framework"))
            assertTrue(analysis.composerDevDependencies.containsKey("phpunit/phpunit"))
            assertTrue(analysis.detectedDatabaseFiles.any { it.endsWith("data${File.separator}app.db") })
            assertTrue(analysis.detectedWebDirs.contains("public"))
            assertTrue(analysis.phpExtensions["gd"] == true)
            assertTrue(analysis.phpExtensions["curl"] == true)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test(expected = ProjectImportException::class)
    fun `analyze missing php project throws`() {
        val projectDir = Files.createTempDirectory("php-project-missing").toFile()
        projectDir.deleteRecursively()
        PhpProjectImportAnalyzer().analyze(projectDir)
    }
}
