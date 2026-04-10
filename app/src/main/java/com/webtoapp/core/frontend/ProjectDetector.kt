package com.webtoapp.core.frontend

import com.webtoapp.core.logging.AppLogger
import com.google.gson.JsonObject
import java.io.File

/**
 * 前端项目检测器
 * 
 * 自动检测项目类型、依赖等配置
 * 支持导入已构建好的 dist 目录
 */
object ProjectDetector {
    
    private const val TAG = "ProjectDetector"
    private val gson = com.webtoapp.util.GsonProvider.gson
    
    // 框架检测规则
    private val frameworkDetectors = mapOf(
        "vue" to FrontendFramework.VUE,
        "@vue/cli" to FrontendFramework.VUE,
        "nuxt" to FrontendFramework.NUXT,
        "react" to FrontendFramework.REACT,
        "react-dom" to FrontendFramework.REACT,
        "next" to FrontendFramework.NEXT,
        "@angular/core" to FrontendFramework.ANGULAR,
        "svelte" to FrontendFramework.SVELTE
    )
    
    // Database检测规则
    private val databaseDetectors = mapOf(
        "sqlite3" to DatabaseType.SQLITE,
        "better-sqlite3" to DatabaseType.SQLITE,
        "mysql" to DatabaseType.MYSQL,
        "mysql2" to DatabaseType.MYSQL,
        "pg" to DatabaseType.POSTGRESQL,
        "postgres" to DatabaseType.POSTGRESQL,
        "mongodb" to DatabaseType.MONGODB,
        "mongoose" to DatabaseType.MONGODB,
        "redis" to DatabaseType.REDIS,
        "ioredis" to DatabaseType.REDIS
    )

    
    // 依赖分类规则
    private val categoryDetectors = mapOf(
        // UI 库
        "element-ui" to DependencyCategory.UI_LIBRARY,
        "element-plus" to DependencyCategory.UI_LIBRARY,
        "ant-design-vue" to DependencyCategory.UI_LIBRARY,
        "antd" to DependencyCategory.UI_LIBRARY,
        "vuetify" to DependencyCategory.UI_LIBRARY,
        "@mui/material" to DependencyCategory.UI_LIBRARY,
        "tailwindcss" to DependencyCategory.UI_LIBRARY,
        "bootstrap" to DependencyCategory.UI_LIBRARY,
        
        // 状态管理
        "vuex" to DependencyCategory.STATE_MANAGEMENT,
        "pinia" to DependencyCategory.STATE_MANAGEMENT,
        "redux" to DependencyCategory.STATE_MANAGEMENT,
        "@reduxjs/toolkit" to DependencyCategory.STATE_MANAGEMENT,
        "mobx" to DependencyCategory.STATE_MANAGEMENT,
        "zustand" to DependencyCategory.STATE_MANAGEMENT,
        
        // 路由
        "vue-router" to DependencyCategory.ROUTER,
        "react-router" to DependencyCategory.ROUTER,
        "react-router-dom" to DependencyCategory.ROUTER,
        
        // HTTP 客户端
        "axios" to DependencyCategory.HTTP_CLIENT,
        "ky" to DependencyCategory.HTTP_CLIENT,
        
        // Build工具
        "vite" to DependencyCategory.BUILD_TOOL,
        "webpack" to DependencyCategory.BUILD_TOOL,
        "rollup" to DependencyCategory.BUILD_TOOL,
        "esbuild" to DependencyCategory.BUILD_TOOL,
        
        // 测试
        "jest" to DependencyCategory.TESTING,
        "vitest" to DependencyCategory.TESTING,
        "mocha" to DependencyCategory.TESTING,
        "cypress" to DependencyCategory.TESTING
    )
    
    /**
     * 检测项目
     */
    fun detectProject(projectPath: String): ProjectDetectionResult {
        val projectDir = File(projectPath)
        val issues = mutableListOf<ProjectIssue>()
        val suggestions = mutableListOf<String>()
        
        // 首先检查是否有 dist/build 目录（已构建的项目）
        val distDir = findDistDirectory(projectDir)
        if (distDir != null) {
            suggestions.add("检测到已构建的输出目录: ${distDir.name}，可直接导入")
        }
        
        // Check package.json
        val packageJsonFile = File(projectDir, "package.json")
        if (!packageJsonFile.exists()) {
            // 没有 package.json，检查是否是纯静态目录
            if (distDir != null || hasIndexHtml(projectDir)) {
                return createStaticResult(projectDir, distDir, issues, suggestions)
            }
            return createErrorResult("未找到 package.json 文件，也没有检测到静态文件", issues)
        }
        
        val packageJson = try {
            gson.fromJson(packageJsonFile.readText(), JsonObject::class.java)
        } catch (e: Exception) {
            AppLogger.d(TAG, "解析 package.json 失败", e)
            return createErrorResult("package.json 格式错误: ${e.message}", issues)
        }
        
        // Parse依赖
        val dependencies = parseDependencies(packageJson, "dependencies", false)
        val devDependencies = parseDependencies(packageJson, "devDependencies", true)
        val allDeps = dependencies + devDependencies
        
        // 检测框架
        val framework = detectFramework(allDeps, projectDir)
        val frameworkVersion = getFrameworkVersion(framework, allDeps)
        
        // 检测包管理器
        val packageManager = detectPackageManager(projectDir)
        
        // 检测 TypeScript
        val hasTypeScript = allDeps.any { it.name == "typescript" } ||
                           File(projectDir, "tsconfig.json").exists()
        
        // 检测数据库
        val databases = detectDatabases(allDeps)
        
        // Parse脚本
        val scripts = parseScripts(packageJson)
        
        // 确定构建命令和输出目录
        val (buildCommand, outputDir) = determineBuildConfig(framework, scripts, projectDir)
        
        // 确定开发命令
        val devCommand = determineDevCommand(scripts)
        
        // Check是否有已构建的输出
        val hasDistFolder = distDir != null
        if (!hasDistFolder) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.WARNING,
                type = IssueType.NO_DIST_FOLDER,
                message = "未检测到构建输出目录（dist/build）",
                suggestion = "请先在电脑上运行 npm run build 构建项目，然后导入构建后的文件夹"
            ))
        }
        
        // 检测运行时需求（后端、SSR、环境变量）
        val runtimeRequirement = detectRuntimeRequirement(projectDir, framework, allDeps, scripts)
        
        // Generate建议
        generateSuggestions(framework, allDeps, databases, hasDistFolder, runtimeRequirement, suggestions)
        
        return ProjectDetectionResult(
            framework = framework,
            frameworkVersion = frameworkVersion,
            packageManager = packageManager,
            hasTypeScript = hasTypeScript,
            databases = databases,
            dependencies = dependencies,
            devDependencies = devDependencies,
            scripts = scripts,
            buildCommand = buildCommand,
            devCommand = devCommand,
            outputDir = distDir?.absolutePath ?: outputDir,
            issues = issues,
            suggestions = suggestions,
            runtimeRequirement = runtimeRequirement
        )
    }

    
    /**
     * 查找 dist 目录
     */
    private fun findDistDirectory(projectDir: File): File? {
        val possibleDirs = listOf("dist", "build", "out", ".output/public", "public")
        for (dirName in possibleDirs) {
            val dir = File(projectDir, dirName)
            if (dir.exists() && dir.isDirectory && hasIndexHtml(dir)) {
                return dir
            }
        }
        return null
    }
    
    /**
     * 检查目录是否包含 index.html
     */
    private fun hasIndexHtml(dir: File): Boolean {
        return File(dir, "index.html").exists()
    }
    
    /**
     * 创建静态项目结果
     */
    private fun createStaticResult(
        projectDir: File,
        distDir: File?,
        issues: MutableList<ProjectIssue>,
        suggestions: MutableList<String>
    ): ProjectDetectionResult {
        val outputDir = distDir ?: projectDir
        
        if (!hasIndexHtml(outputDir)) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.ERROR,
                type = IssueType.MISSING_CONFIG,
                message = "未找到 index.html 文件"
            ))
        }
        
        suggestions.add("检测到静态网站项目，将直接导入所有文件")
        
        return ProjectDetectionResult(
            framework = FrontendFramework.UNKNOWN,
            frameworkVersion = null,
            packageManager = PackageManager.NPM,
            hasTypeScript = false,
            databases = emptyList(),
            dependencies = emptyList(),
            devDependencies = emptyList(),
            scripts = emptyMap(),
            buildCommand = null,
            devCommand = null,
            outputDir = outputDir.absolutePath,
            issues = issues,
            suggestions = suggestions
        )
    }
    
    /**
     * 解析依赖
     */
    private fun parseDependencies(
        packageJson: JsonObject,
        key: String,
        isDevDependency: Boolean
    ): List<DependencyInfo> {
        val deps = packageJson.getAsJsonObject(key) ?: return emptyList()
        
        return deps.entrySet().map { (name, version) ->
            val category = categoryDetectors[name] 
                ?: frameworkDetectors[name]?.let { DependencyCategory.FRAMEWORK }
                ?: databaseDetectors[name]?.let { DependencyCategory.DATABASE }
                ?: DependencyCategory.OTHER
            
            DependencyInfo(
                name = name,
                version = version.asString,
                isDevDependency = isDevDependency,
                category = category
            )
        }
    }
    
    /**
     * 检测框架
     */
    private fun detectFramework(deps: List<DependencyInfo>, projectDir: File): FrontendFramework {
        val depNames = deps.map { it.name }.toSet()
        
        return when {
            "nuxt" in depNames || "nuxt3" in depNames -> FrontendFramework.NUXT
            "next" in depNames -> FrontendFramework.NEXT
            "@angular/core" in depNames -> FrontendFramework.ANGULAR
            "svelte" in depNames -> FrontendFramework.SVELTE
            "vue" in depNames -> {
                if ("vite" in depNames) FrontendFramework.VITE else FrontendFramework.VUE
            }
            "react" in depNames || "react-dom" in depNames -> {
                if ("vite" in depNames) FrontendFramework.VITE else FrontendFramework.REACT
            }
            "vite" in depNames -> FrontendFramework.VITE
            else -> {
                when {
                    File(projectDir, "vue.config.js").exists() -> FrontendFramework.VUE
                    File(projectDir, "vite.config.js").exists() ||
                    File(projectDir, "vite.config.ts").exists() -> FrontendFramework.VITE
                    File(projectDir, "next.config.js").exists() -> FrontendFramework.NEXT
                    File(projectDir, "nuxt.config.js").exists() ||
                    File(projectDir, "nuxt.config.ts").exists() -> FrontendFramework.NUXT
                    File(projectDir, "angular.json").exists() -> FrontendFramework.ANGULAR
                    else -> FrontendFramework.UNKNOWN
                }
            }
        }
    }
    
    /**
     * 获取框架版本
     */
    private fun getFrameworkVersion(framework: FrontendFramework, deps: List<DependencyInfo>): String? {
        val frameworkPackage = when (framework) {
            FrontendFramework.VUE -> "vue"
            FrontendFramework.REACT -> "react"
            FrontendFramework.NEXT -> "next"
            FrontendFramework.NUXT -> "nuxt"
            FrontendFramework.ANGULAR -> "@angular/core"
            FrontendFramework.SVELTE -> "svelte"
            FrontendFramework.VITE -> "vite"
            else -> null
        }
        
        return frameworkPackage?.let { pkg ->
            deps.find { it.name == pkg }?.version?.removePrefix("^")?.removePrefix("~")
        }
    }
    
    /**
     * 检测包管理器
     */
    private fun detectPackageManager(projectDir: File): PackageManager {
        return when {
            File(projectDir, "pnpm-lock.yaml").exists() -> PackageManager.PNPM
            File(projectDir, "yarn.lock").exists() -> PackageManager.YARN
            File(projectDir, "bun.lockb").exists() -> PackageManager.BUN
            else -> PackageManager.NPM
        }
    }
    
    /**
     * 检测数据库
     */
    private fun detectDatabases(deps: List<DependencyInfo>): List<DatabaseType> {
        return deps.mapNotNull { dep ->
            databaseDetectors[dep.name]
        }.distinct()
    }
    
    /**
     * 解析脚本
     */
    private fun parseScripts(packageJson: JsonObject): Map<String, String> {
        val scripts = packageJson.getAsJsonObject("scripts") ?: return emptyMap()
        return scripts.entrySet().associate { it.key to it.value.asString }
    }
    
    /**
     * 确定构建配置
     */
    private fun determineBuildConfig(
        framework: FrontendFramework,
        scripts: Map<String, String>,
        projectDir: File
    ): Pair<String?, String> {
        val buildCommand = when {
            "build" in scripts -> "build"
            "build:prod" in scripts -> "build:prod"
            "generate" in scripts -> "generate"
            else -> "build"
        }
        
        val outputDir = when (framework) {
            FrontendFramework.VUE -> "dist"
            FrontendFramework.REACT -> "build"
            FrontendFramework.NEXT -> "out"
            FrontendFramework.NUXT -> ".output/public"
            FrontendFramework.ANGULAR -> "dist"
            FrontendFramework.VITE -> "dist"
            else -> "dist"
        }
        
        return buildCommand to outputDir
    }
    
    /**
     * 确定开发命令
     */
    private fun determineDevCommand(scripts: Map<String, String>): String? {
        return when {
            "dev" in scripts -> "dev"
            "serve" in scripts -> "serve"
            "start" in scripts -> "start"
            else -> null
        }
    }
    
    /**
     * 检测运行时需求（后端、SSR、环境变量）
     */
    private fun detectRuntimeRequirement(
        projectDir: File,
        framework: FrontendFramework,
        deps: List<DependencyInfo>,
        scripts: Map<String, String>
    ): ProjectRuntimeRequirement {
        val depNames = deps.map { it.name }.toSet()
        
        // 1. 检测后端框架
        val backendFramework = when {
            "express" in depNames -> BackendFramework.EXPRESS
            "fastify" in depNames -> BackendFramework.FASTIFY
            "koa" in depNames -> BackendFramework.KOA
            "@nestjs/core" in depNames || "@nestjs/common" in depNames -> BackendFramework.NESTJS
            "@hapi/hapi" in depNames -> BackendFramework.HAPI
            else -> BackendFramework.NONE
        }
        
        // 2. 检测是否有后端入口文件
        val serverDirs = listOf("server", "api", "backend", "src/server", "src/api")
        val hasServerDir = serverDirs.any { File(projectDir, it).exists() }
        
        // 检测 scripts 中是否有后端启动命令
        val hasServerScript = scripts.keys.any { key ->
            key.contains("server", ignoreCase = true) || 
            key.contains("api", ignoreCase = true) ||
            key.contains("backend", ignoreCase = true)
        }
        
        val needsBackend = backendFramework != BackendFramework.NONE || hasServerDir || hasServerScript
        
        // 3. 检测 SSR 模式
        val isSSR = when (framework) {
            FrontendFramework.NEXT -> {
                // 检查是否配置了静态导出
                val nextConfig = File(projectDir, "next.config.js")
                val canStaticExport = if (nextConfig.exists()) {
                    val content = nextConfig.readText()
                    content.contains("output: 'export'") || content.contains("output:'export'")
                } else false
                !canStaticExport
            }
            FrontendFramework.NUXT -> {
                // 检查是否配置了静态生成
                val nuxtConfig = File(projectDir, "nuxt.config.js")
                val canStaticExport = if (nuxtConfig.exists()) {
                    val content = nuxtConfig.readText()
                    content.contains("ssr: false") || scripts.containsKey("generate")
                } else false
                !canStaticExport
            }
            else -> false
        }
        
        // 4. 解析环境变量需求
        val envVarHints = mutableMapOf<String, String>()
        val envExample = File(projectDir, ".env.example")
        if (envExample.exists()) {
            envExample.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size >= 1) {
                        val key = parts[0].trim()
                        // 尝试从注释中提取说明
                        envVarHints[key] = "请配置环境变量 $key"
                    }
                }
            }
        }
        
        // 5. 检测后端入口文件
        val backendEntryFile = serverDirs.firstNotNullOfOrNull { dir ->
            val serverIndex = File(projectDir, "$dir/index.js")
            if (serverIndex.exists()) "$dir/index.js" else null
        } ?: run {
            val serverJs = File(projectDir, "server.js")
            if (serverJs.exists()) "server.js" else null
        }
        
        return ProjectRuntimeRequirement(
            needsNodeRuntime = needsBackend || isSSR,
            backendFramework = backendFramework,
            backendEntryFile = backendEntryFile,
            isSSR = isSSR,
            canStaticExport = !isSSR && !needsBackend,
            envVarHints = envVarHints
        )
    }
    
    /**
     * 生成建议
     */
    private fun generateSuggestions(
        framework: FrontendFramework,
        deps: List<DependencyInfo>,
        databases: List<DatabaseType>,
        hasDistFolder: Boolean,
        runtimeRequirement: ProjectRuntimeRequirement,
        suggestions: MutableList<String>
    ) {
        if (runtimeRequirement.needsNodeRuntime) {
            // 需要后端运行时
            if (runtimeRequirement.backendFramework != BackendFramework.NONE) {
                val fwName = runtimeRequirement.backendFramework.name.lowercase()
                    .replaceFirstChar { it.uppercase() }
                suggestions.add("🚀 检测到 $fwName 后端框架，建议使用 Node.js 应用模式打包")
            }
            if (runtimeRequirement.isSSR) {
                suggestions.add("⚠️ 检测到 SSR 框架，需要 Node.js 运行时或切换到静态导出模式")
            }
            if (runtimeRequirement.backendEntryFile != null) {
                suggestions.add("📄 后端入口文件: ${runtimeRequirement.backendEntryFile}")
            }
        } else if (!hasDistFolder) {
            suggestions.add("💡 请先在电脑上构建项目：npm run build")
            suggestions.add("💡 然后选择构建输出目录（通常是 dist 或 build）导入")
        }
        
        if (runtimeRequirement.envVarHints.isNotEmpty()) {
            suggestions.add("🔑 检测到 ${runtimeRequirement.envVarHints.size} 个环境变量需要配置")
        }
        
        if (databases.isNotEmpty()) {
            if (runtimeRequirement.needsNodeRuntime) {
                suggestions.add("🗃️ 检测到数据库依赖，Node.js 模式下可使用 SQLite")
            } else {
                suggestions.add("⚠️ 检测到数据库依赖，纯静态模式无法使用后端数据库")
            }
        }
    }
    
    /**
     * 创建错误结果
     */
    private fun createErrorResult(message: String, issues: MutableList<ProjectIssue>): ProjectDetectionResult {
        issues.add(ProjectIssue(
            severity = IssueSeverity.ERROR,
            type = IssueType.MISSING_CONFIG,
            message = message
        ))
        
        return ProjectDetectionResult(
            framework = FrontendFramework.UNKNOWN,
            frameworkVersion = null,
            packageManager = PackageManager.NPM,
            hasTypeScript = false,
            databases = emptyList(),
            dependencies = emptyList(),
            devDependencies = emptyList(),
            scripts = emptyMap(),
            buildCommand = null,
            devCommand = null,
            outputDir = "dist",
            issues = issues,
            suggestions = emptyList()
        )
    }
}
