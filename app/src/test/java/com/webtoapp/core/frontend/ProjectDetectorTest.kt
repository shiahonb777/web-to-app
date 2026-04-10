package com.webtoapp.core.frontend

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.Test

class ProjectDetectorTest {

    @Test
    fun `detectProject treats plain folder with index as static project`() {
        withTempProject { projectDir ->
            writeFile(projectDir, "index.html", "<html><body>Hello</body></html>")

            val result = ProjectDetector.detectProject(projectDir.absolutePath)

            assertThat(result.framework).isEqualTo(FrontendFramework.UNKNOWN)
            assertThat(result.outputDir).isEqualTo(projectDir.absolutePath)
            assertThat(result.issues).isEmpty()
            assertThat(result.suggestions.any { it.contains("静态网站") }).isTrue()
        }
    }

    @Test
    fun `detectProject returns error when project has no package json and no static entry`() {
        withTempProject { projectDir ->
            val result = ProjectDetector.detectProject(projectDir.absolutePath)

            assertThat(result.framework).isEqualTo(FrontendFramework.UNKNOWN)
            assertThat(result.issues).isNotEmpty()
            assertThat(result.issues.first().message).contains("未找到 package.json")
        }
    }

    @Test
    fun `detectProject identifies vite react backend and sqlite requirements`() {
        withTempProject { projectDir ->
            writeFile(
                projectDir,
                "package.json",
                """
                {
                  "dependencies": {
                    "react": "^18.3.0",
                    "react-dom": "^18.3.0",
                    "vite": "^5.2.0",
                    "express": "^4.19.0",
                    "sqlite3": "^5.1.0"
                  },
                  "devDependencies": {
                    "typescript": "^5.5.0"
                  },
                  "scripts": {
                    "dev": "vite",
                    "build": "vite build",
                    "server:start": "node server/index.js"
                  }
                }
                """.trimIndent()
            )
            writeFile(projectDir, "tsconfig.json", "{}")
            writeFile(projectDir, "pnpm-lock.yaml", "lockfileVersion: '9.0'")
            writeFile(projectDir, "server/index.js", "console.log('server');")

            val result = ProjectDetector.detectProject(projectDir.absolutePath)

            assertThat(result.framework).isEqualTo(FrontendFramework.VITE)
            assertThat(result.frameworkVersion).isEqualTo("5.2.0")
            assertThat(result.packageManager).isEqualTo(PackageManager.PNPM)
            assertThat(result.hasTypeScript).isTrue()
            assertThat(result.databases).contains(DatabaseType.SQLITE)
            assertThat(result.buildCommand).isEqualTo("build")
            assertThat(result.devCommand).isEqualTo("dev")
            assertThat(result.issues.map { it.type }).contains(IssueType.NO_DIST_FOLDER)
            assertThat(result.runtimeRequirement.needsNodeRuntime).isTrue()
            assertThat(result.runtimeRequirement.backendFramework).isEqualTo(BackendFramework.EXPRESS)
            assertThat(result.runtimeRequirement.backendEntryFile).isEqualTo("server/index.js")
            assertThat(result.runtimeRequirement.canStaticExport).isFalse()
        }
    }

    @Test
    fun `detectProject prefers built dist folder when present`() {
        withTempProject { projectDir ->
            writeFile(
                projectDir,
                "package.json",
                """
                {
                  "dependencies": {
                    "vue": "^3.4.0",
                    "vite": "^5.2.0"
                  },
                  "scripts": {
                    "build": "vite build"
                  }
                }
                """.trimIndent()
            )
            writeFile(projectDir, "dist/index.html", "<!doctype html>")

            val result = ProjectDetector.detectProject(projectDir.absolutePath)

            assertThat(result.outputDir).isEqualTo(File(projectDir, "dist").absolutePath)
            assertThat(result.issues.map { it.type }).doesNotContain(IssueType.NO_DIST_FOLDER)
        }
    }

    @Test
    fun `detectProject marks next export config as non ssr`() {
        withTempProject { projectDir ->
            writeFile(
                projectDir,
                "package.json",
                """
                {
                  "dependencies": {
                    "next": "^15.0.0",
                    "react": "^18.3.0"
                  },
                  "scripts": {
                    "build": "next build"
                  }
                }
                """.trimIndent()
            )
            writeFile(projectDir, "next.config.js", "module.exports = { output: 'export' }")

            val result = ProjectDetector.detectProject(projectDir.absolutePath)

            assertThat(result.framework).isEqualTo(FrontendFramework.NEXT)
            assertThat(result.runtimeRequirement.isSSR).isFalse()
            assertThat(result.runtimeRequirement.needsNodeRuntime).isFalse()
            assertThat(result.runtimeRequirement.canStaticExport).isTrue()
        }
    }

    private fun withTempProject(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("project-detector-test-").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun writeFile(projectDir: File, relativePath: String, content: String) {
        val file = File(projectDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }
}
