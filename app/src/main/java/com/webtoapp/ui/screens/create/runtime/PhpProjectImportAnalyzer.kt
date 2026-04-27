package com.webtoapp.ui.screens.create.runtime

import com.google.gson.JsonObject
import com.webtoapp.ui.screens.create.common.ProjectImportAnalysis
import com.webtoapp.ui.screens.create.common.ProjectImportException
import com.webtoapp.ui.screens.create.common.formatProjectName
import com.webtoapp.ui.screens.create.common.parseEnvFile
import com.webtoapp.util.GsonProvider
import java.io.File

data class PhpProjectImportAnalysis(
    override val projectDir: File,
    override val suggestedAppName: String?,
    override val framework: String?,
    override val entryFile: String?,
    override val envVars: Map<String, String>,
    override val detectedPort: Int? = null,
    val documentRoot: String,
    val composerDependencies: Map<String, String>,
    val composerDevDependencies: Map<String, String>,
    val detectedWebDirs: List<String>,
    val phpExtensions: Map<String, Boolean>,
    val detectedDatabaseFiles: List<String>,
    val sqlitePath: String,
    val frameworkVersion: String?,
) : ProjectImportAnalysis

class PhpProjectImportAnalyzer {
    fun analyze(inputDir: File): PhpProjectImportAnalysis {
        val projectDir = resolveProjectRoot(inputDir)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            throw ProjectImportException("PHP 项目目录不存在")
        }

        val framework = detectFramework(projectDir)
        val documentRoot = detectDocumentRoot(projectDir, framework)
        val entryFile = detectEntryFile(projectDir, documentRoot)
        val composerJson = parseComposerJson(File(projectDir, "composer.json"))
        val baseExtensions = mutableMapOf(
            "pdo_sqlite" to true,
            "json" to true,
            "mbstring" to true,
            "openssl" to true,
            "curl" to false,
            "gd" to false,
            "zip" to false,
            "xml" to false,
        )
        val dependencyKeys = composerJson.dependencies.keys + composerJson.devDependencies.keys
        if (dependencyKeys.any { it.contains("gd") || it.contains("image") || it.contains("intervention") }) {
            baseExtensions["gd"] = true
        }
        if (dependencyKeys.any { it.contains("zip") || it.contains("archive") }) {
            baseExtensions["zip"] = true
        }
        if (dependencyKeys.any { it.contains("xml") || it.contains("soap") }) {
            baseExtensions["xml"] = true
        }
        if (dependencyKeys.any { it.contains("curl") || it.contains("guzzle") || it.contains("http") }) {
            baseExtensions["curl"] = true
        }

        val detectedDatabaseFiles = projectDir.walkTopDown()
            .maxDepth(3)
            .filter { file -> listOf(".db", ".sqlite", ".sqlite3").any(file.name::endsWith) }
            .map { it.relativeTo(projectDir).path }
            .toList()

        return PhpProjectImportAnalysis(
            projectDir = projectDir,
            suggestedAppName = formatProjectName(projectDir, composerJson.projectName?.substringAfterLast("/")),
            framework = framework,
            entryFile = entryFile,
            envVars = parseEnvFile(File(projectDir, ".env")),
            documentRoot = documentRoot,
            composerDependencies = composerJson.dependencies,
            composerDevDependencies = composerJson.devDependencies,
            detectedWebDirs = listOf("public", "www", "htdocs", "web", "webroot", "html")
                .filter { File(projectDir, it).isDirectory },
            phpExtensions = baseExtensions,
            detectedDatabaseFiles = detectedDatabaseFiles,
            sqlitePath = detectedDatabaseFiles.firstOrNull().orEmpty(),
            frameworkVersion = composerJson.phpVersion,
        )
    }

    fun resolveProjectRoot(dir: File): File {
        if (isPhpProjectRoot(dir)) return dir
        return dir.walkTopDown()
            .maxDepth(2)
            .firstOrNull { candidate ->
                candidate.isDirectory && isPhpProjectRoot(candidate)
            } ?: dir
    }

    private fun isPhpProjectRoot(dir: File): Boolean {
        return File(dir, "composer.json").exists() ||
            File(dir, "index.php").exists() ||
            File(dir, "public/index.php").exists()
    }

    private fun detectFramework(projectDir: File): String {
        if (File(projectDir, "artisan").exists()) return "laravel"
        if (File(projectDir, "think").exists()) return "thinkphp"
        if (File(projectDir, "spark").exists() || File(projectDir, "system/CodeIgniter.php").exists()) return "codeigniter"

        val composerContent = File(projectDir, "composer.json").takeIf(File::exists)?.readText().orEmpty()
        return when {
            composerContent.contains("laravel/framework") || composerContent.contains("laravel/laravel") -> "laravel"
            composerContent.contains("topthink/framework") || composerContent.contains("topthink/think") -> "thinkphp"
            composerContent.contains("codeigniter4/framework") || composerContent.contains("codeigniter/framework") -> "codeigniter"
            composerContent.contains("slim/slim") -> "slim"
            File(projectDir, "routes").isDirectory && File(projectDir, "public/index.php").exists() -> "laravel"
            else -> "raw"
        }
    }

    private fun detectDocumentRoot(projectDir: File, framework: String): String {
        return when (framework) {
            "laravel", "thinkphp", "codeigniter", "slim" -> {
                if (File(projectDir, "public/index.php").exists()) "public" else ""
            }

            else -> {
                if (!File(projectDir, "index.php").exists() && File(projectDir, "public/index.php").exists()) {
                    "public"
                } else {
                    ""
                }
            }
        }
    }

    private fun detectEntryFile(projectDir: File, documentRoot: String): String {
        val docRoot = if (documentRoot.isNotBlank()) File(projectDir, documentRoot) else projectDir
        return listOf("index.php", "app.php", "main.php", "server.php")
            .firstOrNull { File(docRoot, it).exists() }
            ?: "index.php"
    }

    private fun parseComposerJson(file: File): ComposerInfo {
        if (!file.exists()) return ComposerInfo()
        val json = GsonProvider.gson.fromJson(file.readText(), JsonObject::class.java)
        return ComposerInfo(
            projectName = json.get("name")?.asString,
            dependencies = json.getAsJsonObject("require")
                ?.entrySet()
                ?.filter { it.key != "php" }
                ?.associate { it.key to it.value.asString }
                .orEmpty(),
            devDependencies = json.getAsJsonObject("require-dev")
                ?.entrySet()
                ?.associate { it.key to it.value.asString }
                .orEmpty(),
            phpVersion = json.getAsJsonObject("require")?.get("php")?.asString,
        )
    }
}

private data class ComposerInfo(
    val projectName: String? = null,
    val dependencies: Map<String, String> = emptyMap(),
    val devDependencies: Map<String, String> = emptyMap(),
    val phpVersion: String? = null,
)
