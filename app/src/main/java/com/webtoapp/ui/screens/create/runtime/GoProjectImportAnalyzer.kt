package com.webtoapp.ui.screens.create.runtime

import com.webtoapp.core.golang.GoDependencyManager
import com.webtoapp.ui.screens.create.common.ProjectImportAnalysis
import com.webtoapp.ui.screens.create.common.ProjectImportException
import com.webtoapp.ui.screens.create.common.formatProjectName
import com.webtoapp.ui.screens.create.common.parseEnvFile
import java.io.File

data class GoProjectImportAnalysis(
    override val projectDir: File,
    override val suggestedAppName: String?,
    override val framework: String?,
    override val entryFile: String?,
    override val envVars: Map<String, String>,
    override val detectedPort: Int? = null,
    val binaryName: String,
    val binaryDetected: Boolean,
    val binarySize: Long?,
    val staticDir: String,
    val modulePath: String?,
    val goVersion: String?,
    val dependencies: List<Pair<String, String>>,
    val targetArch: String,
    val healthCheckEndpoint: String,
) : ProjectImportAnalysis

class GoProjectImportAnalyzer {
    fun analyze(projectDir: File): GoProjectImportAnalysis {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            throw ProjectImportException("Go 项目目录不存在")
        }

        val goModInfo = parseGoMod(File(projectDir, "go.mod"))
        val framework = detectFramework(projectDir, goModInfo.dependencies)
        val binaryName = detectBinary(projectDir)
        val binaryFile = binaryName?.let { findBinary(projectDir, it) }

        return GoProjectImportAnalysis(
            projectDir = projectDir,
            suggestedAppName = formatProjectName(projectDir, goModInfo.modulePath?.substringAfterLast("/")),
            framework = framework,
            entryFile = "main.go",
            envVars = parseEnvFile(File(projectDir, ".env")),
            binaryName = binaryName.orEmpty(),
            binaryDetected = binaryName != null,
            binarySize = binaryFile?.length(),
            staticDir = detectStaticDir(projectDir),
            modulePath = goModInfo.modulePath,
            goVersion = goModInfo.goVersion,
            dependencies = goModInfo.dependencies,
            targetArch = "arm64",
            healthCheckEndpoint = "/health",
        )
    }

    private fun parseGoMod(goMod: File): GoModInfo {
        if (!goMod.exists()) return GoModInfo()
        val lines = goMod.readLines()
        val modulePath = lines.firstOrNull { it.startsWith("module ") }
            ?.substringAfter("module ")
            ?.trim()
        val goVersion = lines.firstOrNull { it.startsWith("go ") }
            ?.substringAfter("go ")
            ?.trim()

        var inRequireBlock = false
        val dependencies = mutableListOf<Pair<String, String>>()
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed == "require (" -> inRequireBlock = true
                trimmed == ")" -> inRequireBlock = false
                inRequireBlock && trimmed.isNotEmpty() && !trimmed.startsWith("//") && !trimmed.contains("// indirect") -> {
                    val parts = trimmed.split(" ", limit = 2)
                    if (parts.size == 2) dependencies += parts[0].trim() to parts[1].trim()
                }
                trimmed.startsWith("require ") && !trimmed.contains("(") -> {
                    val parts = trimmed.removePrefix("require ").trim().split(" ", limit = 2)
                    if (parts.size == 2) dependencies += parts[0].trim() to parts[1].trim()
                }
            }
        }
        return GoModInfo(modulePath, goVersion, dependencies)
    }

    private fun detectFramework(projectDir: File, dependencies: List<Pair<String, String>>): String {
        val dependencyNames = dependencies.map { it.first.lowercase() }
        return when {
            dependencyNames.any { it.contains("github.com/gin-gonic/gin") } -> "gin"
            dependencyNames.any { it.contains("github.com/gofiber/fiber") } -> "fiber"
            dependencyNames.any { it.contains("github.com/labstack/echo") } -> "echo"
            dependencyNames.any { it.contains("github.com/go-chi/chi") } -> "chi"
            else -> detectFrameworkFromSource(projectDir)
        }
    }

    private fun detectFrameworkFromSource(projectDir: File): String {
        listOf("main.go", "server.go", "app.go").forEach { name ->
            val file = File(projectDir, name)
            if (!file.exists()) return@forEach
            val content = file.readText()
            when {
                content.contains("gin.") -> return "gin"
                content.contains("fiber.") -> return "fiber"
                content.contains("echo.") -> return "echo"
                content.contains("chi.") -> return "chi"
                content.contains("net/http") -> return "net_http"
            }
        }
        return "raw"
    }

    private fun detectStaticDir(projectDir: File): String {
        return listOf("static", "public", "web", "dist", "assets", "www")
            .firstOrNull { dir ->
                val candidate = File(projectDir, dir)
                candidate.isDirectory && candidate.listFiles()?.isNotEmpty() == true
            }
            .orEmpty()
    }

    private fun detectBinary(projectDir: File): String? {
        listOf(projectDir, File(projectDir, "bin"), File(projectDir, "build"))
            .filter(File::isDirectory)
            .forEach { dir ->
                dir.listFiles().orEmpty().forEach { file ->
                    if (!file.isFile || file.length() <= 1000) return@forEach
                    val elfInfo = GoDependencyManager.parseElf(file)
                    if (GoDependencyManager.isCompatible(elfInfo)) {
                        return file.name
                    }
                }
            }
        return null
    }

    private fun findBinary(projectDir: File, binaryName: String): File? {
        return listOf(projectDir, File(projectDir, "bin"), File(projectDir, "build"))
            .map { File(it, binaryName) }
            .firstOrNull(File::exists)
    }
}

private data class GoModInfo(
    val modulePath: String? = null,
    val goVersion: String? = null,
    val dependencies: List<Pair<String, String>> = emptyList(),
)
