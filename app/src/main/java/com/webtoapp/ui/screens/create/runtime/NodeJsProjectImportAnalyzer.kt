package com.webtoapp.ui.screens.create.runtime

import com.google.gson.JsonObject
import com.webtoapp.data.model.NodeJsBuildMode
import com.webtoapp.ui.screens.create.common.ProjectImportAnalysis
import com.webtoapp.ui.screens.create.common.ProjectImportException
import com.webtoapp.ui.screens.create.common.detectPortFromText
import com.webtoapp.ui.screens.create.common.formatProjectName
import com.webtoapp.ui.screens.create.common.parseEnvFile
import com.webtoapp.util.GsonProvider
import java.io.File

data class NodeJsProjectImportAnalysis(
    override val projectDir: File,
    override val suggestedAppName: String?,
    override val framework: String?,
    override val entryFile: String?,
    override val envVars: Map<String, String>,
    override val detectedPort: Int?,
    val buildMode: NodeJsBuildMode,
    val packageManager: String,
    val packageName: String?,
    val packageVersion: String?,
    val packageDescription: String?,
    val scripts: Map<String, String>,
    val selectedStartScript: String?,
    val dependencies: Map<String, String>,
    val devDependencies: Map<String, String>,
    val hasTypeScript: Boolean,
    val nodeVersion: String?,
) : ProjectImportAnalysis

class NodeJsProjectImportAnalyzer {
    fun analyze(projectDir: File): NodeJsProjectImportAnalysis {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            throw ProjectImportException("Node.js 项目目录不存在")
        }

        val packageJsonFile = File(projectDir, "package.json")
        if (!packageJsonFile.exists()) {
            throw ProjectImportException("未找到 package.json")
        }

        val packageJson = GsonProvider.gson.fromJson(
            packageJsonFile.readText(),
            JsonObject::class.java
        )

        val scripts = packageJson.getAsJsonObject("scripts")
            ?.entrySet()
            ?.associate { it.key to it.value.asString }
            .orEmpty()
        val dependencies = packageJson.getAsJsonObject("dependencies")
            ?.entrySet()
            ?.associate { it.key to it.value.asString }
            .orEmpty()
        val devDependencies = packageJson.getAsJsonObject("devDependencies")
            ?.entrySet()
            ?.associate { it.key to it.value.asString }
            .orEmpty()
        val allDependencyKeys = dependencies.keys + devDependencies.keys

        val framework = when {
            "express" in allDependencyKeys -> "Express"
            "fastify" in allDependencyKeys -> "Fastify"
            "koa" in allDependencyKeys -> "Koa"
            "@nestjs/core" in allDependencyKeys -> "NestJS"
            "@hapi/hapi" in allDependencyKeys -> "Hapi"
            "next" in allDependencyKeys -> "Next.js"
            "nuxt" in allDependencyKeys -> "Nuxt.js"
            else -> null
        }

        val buildMode = when {
            "next" in allDependencyKeys || "nuxt" in allDependencyKeys -> NodeJsBuildMode.FULLSTACK
            "express" in allDependencyKeys ||
                "fastify" in allDependencyKeys ||
                "koa" in allDependencyKeys ||
                "@nestjs/core" in allDependencyKeys ||
                "@hapi/hapi" in allDependencyKeys -> NodeJsBuildMode.API_BACKEND
            else -> NodeJsBuildMode.STATIC
        }

        val selectedStartScript = when {
            "start" in scripts -> "start"
            "dev" in scripts -> "dev"
            "serve" in scripts -> "serve"
            else -> scripts.keys.firstOrNull()
        }
        val entryFile = detectEntryFile(projectDir, packageJson) ?: "index.js"
        val envVars = parseEnvFile(File(projectDir, ".env.example")).ifEmpty {
            parseEnvFile(File(projectDir, ".env"))
        }
        val detectedPort = detectPortFromText(selectedStartScript?.let(scripts::get))
            ?: parseEnvFile(File(projectDir, ".env"))["PORT"]?.toIntOrNull()
            ?: detectPortFromText(File(projectDir, entryFile).takeIf(File::exists)?.readText())

        return NodeJsProjectImportAnalysis(
            projectDir = projectDir,
            suggestedAppName = formatProjectName(projectDir, packageJson.get("name")?.asString),
            framework = framework,
            entryFile = entryFile,
            envVars = envVars,
            detectedPort = detectedPort,
            buildMode = buildMode,
            packageManager = detectPackageManager(projectDir),
            packageName = packageJson.get("name")?.asString,
            packageVersion = packageJson.get("version")?.asString,
            packageDescription = packageJson.get("description")?.asString,
            scripts = scripts,
            selectedStartScript = selectedStartScript,
            dependencies = dependencies,
            devDependencies = devDependencies,
            hasTypeScript = File(projectDir, "tsconfig.json").exists() ||
                "typescript" in allDependencyKeys ||
                "ts-node" in allDependencyKeys,
            nodeVersion = packageJson.getAsJsonObject("engines")?.get("node")?.asString,
        )
    }

    private fun detectEntryFile(projectDir: File, packageJson: JsonObject): String? {
        packageJson.get("main")?.asString?.let { main ->
            if (File(projectDir, main).exists()) return main
        }

        packageJson.getAsJsonObject("scripts")?.get("start")?.asString?.let { startCommand ->
            Regex("""node\s+(\S+\.(?:js|mjs|cjs))""")
                .find(startCommand)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { entry ->
                    if (File(projectDir, entry).exists()) return entry
                }
        }

        return listOf(
            "server.js",
            "server/index.js",
            "src/server.js",
            "app.js",
            "src/app.js",
            "index.js",
            "src/index.js",
            "main.js",
            "src/main.js",
            "server.mjs",
            "index.mjs",
        ).firstOrNull { File(projectDir, it).exists() }
    }

    private fun detectPackageManager(projectDir: File): String {
        return when {
            File(projectDir, "pnpm-lock.yaml").exists() -> "pnpm"
            File(projectDir, "yarn.lock").exists() -> "yarn"
            File(projectDir, "bun.lockb").exists() -> "bun"
            else -> "npm"
        }
    }
}
