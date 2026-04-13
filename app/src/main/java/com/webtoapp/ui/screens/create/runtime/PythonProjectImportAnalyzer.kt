package com.webtoapp.ui.screens.create.runtime

import com.webtoapp.ui.screens.create.common.ProjectImportAnalysis
import com.webtoapp.ui.screens.create.common.ProjectImportException
import com.webtoapp.ui.screens.create.common.formatProjectName
import com.webtoapp.ui.screens.create.common.parseEnvFile
import com.webtoapp.ui.screens.create.common.parseVersionedList
import java.io.File

data class PythonProjectImportAnalysis(
    override val projectDir: File,
    override val suggestedAppName: String?,
    override val framework: String?,
    override val entryFile: String?,
    override val envVars: Map<String, String>,
    override val detectedPort: Int? = null,
    val serverType: String,
    val entryModule: String,
    val requirements: List<Pair<String, String>>,
    val requirementsSource: String,
    val venvDetected: Boolean,
    val venvPath: String?,
    val pythonVersion: String?,
    val djangoSettingsModule: String,
    val djangoStaticDir: String,
    val fastapiDocsEnabled: Boolean,
) : ProjectImportAnalysis

class PythonProjectImportAnalyzer {
    fun analyze(projectDir: File): PythonProjectImportAnalysis {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            throw ProjectImportException("Python 项目目录不存在")
        }

        val framework = detectFramework(projectDir)
        val entryFile = detectEntryFile(projectDir, framework)
        val (venvDetected, venvPath) = detectVirtualEnv(projectDir)
        val pyProjectName = parsePyprojectName(projectDir)
        val pyProjectVersion = parsePyprojectPythonVersion(projectDir)
        val setupName = parseSetupName(projectDir)
        val requirementsInfo = parseRequirements(projectDir)
        val djangoSettingsModule = if (framework == "django") detectDjangoSettingsModule(projectDir) else ""
        val entryModule = when (framework) {
            "django" -> detectDjangoEntryModule(projectDir, djangoSettingsModule)
            "fastapi" -> detectFastApiEntryModule(projectDir, entryFile)
            else -> ""
        }

        return PythonProjectImportAnalysis(
            projectDir = projectDir,
            suggestedAppName = formatProjectName(projectDir, pyProjectName ?: setupName),
            framework = framework,
            entryFile = entryFile,
            envVars = parseEnvFile(File(projectDir, ".env")),
            serverType = when (framework) {
                "fastapi" -> "uvicorn"
                "django" -> "gunicorn"
                else -> "builtin"
            },
            entryModule = entryModule,
            requirements = requirementsInfo.requirements,
            requirementsSource = requirementsInfo.source,
            venvDetected = venvDetected,
            venvPath = venvPath,
            pythonVersion = pyProjectVersion,
            djangoSettingsModule = djangoSettingsModule,
            djangoStaticDir = "static",
            fastapiDocsEnabled = true,
        )
    }

    private fun detectFramework(projectDir: File): String {
        listOf("app.py", "main.py", "wsgi.py", "application.py", "run.py").forEach { name ->
            val file = File(projectDir, name)
            if (!file.exists()) return@forEach
            val content = file.readText()
            when {
                content.contains("from flask") || content.contains("import flask") -> return "flask"
                content.contains("from fastapi") || content.contains("import fastapi") -> return "fastapi"
                content.contains("from tornado") || content.contains("import tornado") -> return "tornado"
            }
        }
        if (File(projectDir, "manage.py").exists()) return "django"
        val requirementsContent = File(projectDir, "requirements.txt").takeIf(File::exists)?.readText()?.lowercase()
        val pyprojectContent = File(projectDir, "pyproject.toml").takeIf(File::exists)?.readText()?.lowercase()
        return when {
            requirementsContent?.contains("django") == true || pyprojectContent?.contains("django") == true -> "django"
            requirementsContent?.contains("fastapi") == true || pyprojectContent?.contains("fastapi") == true -> "fastapi"
            requirementsContent?.contains("flask") == true || pyprojectContent?.contains("flask") == true -> "flask"
            requirementsContent?.contains("tornado") == true || pyprojectContent?.contains("tornado") == true -> "tornado"
            else -> "raw"
        }
    }

    private fun detectEntryFile(projectDir: File, framework: String): String {
        val candidates = when (framework) {
            "django" -> listOf(
                if (File(projectDir, "manage.py").exists()) "manage.py" else "app.py"
            )
            "flask" -> listOf("app.py", "application.py", "wsgi.py", "run.py", "main.py")
            "fastapi" -> listOf("main.py", "app.py", "api.py", "server.py")
            "tornado" -> listOf("app.py", "main.py", "server.py")
            else -> listOf("app.py", "main.py", "server.py", "index.py", "run.py")
        }
        return candidates.firstOrNull { File(projectDir, it).exists() } ?: "app.py"
    }

    private fun detectVirtualEnv(projectDir: File): Pair<Boolean, String?> {
        listOf("venv", ".venv", "env", ".env").forEach { dirName ->
            val venvDir = File(projectDir, dirName)
            if (File(venvDir, "bin/python").exists() || File(venvDir, "Scripts/python.exe").exists()) {
                return true to dirName
            }
        }
        return false to null
    }

    private fun parsePyprojectName(projectDir: File): String? {
        val pyproject = File(projectDir, "pyproject.toml")
        if (!pyproject.exists()) return null
        return Regex("""name\s*=\s*"([^"]+)"""")
            .find(pyproject.readText())
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun parsePyprojectPythonVersion(projectDir: File): String? {
        val pyproject = File(projectDir, "pyproject.toml")
        if (!pyproject.exists()) return null
        return Regex("""requires-python\s*=\s*"([^"]+)"""")
            .find(pyproject.readText())
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun parseSetupName(projectDir: File): String? {
        val setupPy = File(projectDir, "setup.py")
        if (!setupPy.exists()) return null
        return Regex("""name\s*=\s*['"]([^'"]+)['"]""")
            .find(setupPy.readText())
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun detectDjangoSettingsModule(projectDir: File): String {
        val managePy = File(projectDir, "manage.py")
        if (managePy.exists()) {
            val content = managePy.readText()
            Regex("""setdefault\(\s*['"]DJANGO_SETTINGS_MODULE['"]\s*,\s*['"]([^'"]+)['"]\s*\)""")
                .find(content)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { return it }
            Regex("""DJANGO_SETTINGS_MODULE\s*=\s*['"]([^'"]+)['"]""")
                .find(content)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { return it }
        }
        return ""
    }

    private fun detectDjangoEntryModule(projectDir: File, settingsModule: String): String {
        if (settingsModule.isNotBlank()) return "$settingsModule.wsgi:application"
        projectDir.walkTopDown()
            .maxDepth(2)
            .firstOrNull { it.name == "wsgi.py" }
            ?.relativeTo(projectDir)
            ?.path
            ?.removeSuffix(".py")
            ?.replace(File.separator, ".")
            ?.let { return "$it:application" }
        return ""
    }

    private fun detectFastApiEntryModule(projectDir: File, entryFile: String): String {
        val entry = File(projectDir, entryFile)
        if (!entry.exists()) return "main:app"
        val appVar = Regex("""(\w+)\s*=\s*FastAPI\(""")
            .find(entry.readText())
            ?.groupValues
            ?.getOrNull(1)
            ?: "app"
        return "${entryFile.removeSuffix(".py")}:$appVar"
    }

    private fun parseRequirements(projectDir: File): PythonRequirementsInfo {
        val requirementsFile = File(projectDir, "requirements.txt")
        if (requirementsFile.exists()) {
            return PythonRequirementsInfo(
                requirements = parseVersionedList(requirementsFile.readLines().asSequence()),
                source = "requirements.txt",
            )
        }

        val pipfile = File(projectDir, "Pipfile")
        if (pipfile.exists()) {
            val packagesBlock = pipfile.readText()
                .substringAfter("[packages]", "")
                .substringBefore("[", "")
            return PythonRequirementsInfo(
                requirements = packagesBlock.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.contains("=") }
                    .map { line ->
                        val key = line.substringBefore("=").trim()
                        val value = line.substringAfter("=").trim().removeSurrounding("\"")
                        key to value
                    },
                source = "Pipfile",
            )
        }

        val pyproject = File(projectDir, "pyproject.toml")
        if (pyproject.exists()) {
            val depsBlock = pyproject.readText()
                .substringAfter("dependencies", "")
                .substringAfter("[", "")
                .substringBefore("]", "")
            val requirements = depsBlock.lines()
                .map { it.trim().removeSuffix(",").removeSurrounding("\"").trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    val parts = line.split(Regex("[>=<~!]+"), limit = 2)
                    val name = parts.first().trim()
                    val version = if (parts.size > 1) line.substring(name.length).trim() else ""
                    name to version
                }
            return PythonRequirementsInfo(
                requirements = requirements,
                source = "pyproject.toml",
            )
        }

        return PythonRequirementsInfo(emptyList(), "")
    }
}

private data class PythonRequirementsInfo(
    val requirements: List<Pair<String, String>>,
    val source: String,
)
