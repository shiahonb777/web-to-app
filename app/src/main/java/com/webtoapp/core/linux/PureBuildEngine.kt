package com.webtoapp.core.linux

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.*

/**
 * 纯净构建引擎
 * 
 * 第一性原理：
 * 前端项目构建的本质是什么？
 * 1. 解析入口文件
 * 2. 递归解析依赖
 * 3. 转换代码（TypeScript -> JavaScript, JSX -> JavaScript）
 * 4. 打包成 bundle
 * 5. 生成 HTML
 * 
 * 我们不需要完整的 Node.js 环境
 * 我们只需要一个能做这些事的工具
 * 
 * esbuild 就是这样的工具：
 * - 用 Go 编写，编译为原生二进制
 * - 支持 Android
 * - 可以直接构建 Vue/React/Vite 项目
 * - 速度极快
 * 
 * 但如果连 esbuild 都无法下载怎么办？
 * 
 * 终极方案：纯 Kotlin 实现的 JavaScript 打包器
 * - 不依赖任何外部二进制
 * - 完全在 JVM 上运行
 * - 支持基本的模块解析和打包
 */
class PureBuildEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "PureBuildEngine"
    }
    
    private val _state = MutableStateFlow<PureBuildState>(PureBuildState.Idle)
    val state: StateFlow<PureBuildState> = _state
    
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs
    
    /**
     * 构建项目
     * 
     * 策略：
     * 1. 如果项目已有 dist 目录，直接使用
     * 2. 如果有 esbuild，使用 esbuild 构建
     * 3. 否则使用纯 Kotlin 打包器
     */
    suspend fun build(
        projectPath: String,
        outputPath: String,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Result<BuildResult> = withContext(Dispatchers.IO) {
        try {
            _state.value = PureBuildState.Analyzing
            _logs.value = emptyList()
            
            log("分析项目: $projectPath")
            
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                throw Exception("项目目录不存在")
            }
            
            // Check是否已有构建输出
            val existingDist = findExistingDist(projectDir)
            if (existingDist != null) {
                log("发现已构建的输出: ${existingDist.name}")
                _state.value = PureBuildState.Copying(0f)
                
                val outputDir = File(outputPath)
                copyDirectory(existingDist, outputDir) { progress ->
                    _state.value = PureBuildState.Copying(progress)
                    onProgress("复制文件", progress)
                }
                
                _state.value = PureBuildState.Success(outputPath)
                return@withContext Result.success(BuildResult(
                    outputPath = outputPath,
                    method = BuildMethod.EXISTING_DIST,
                    fileCount = countFiles(outputDir),
                    totalSize = calculateSize(outputDir)
                ))
            }
            
            // Check esbuild 是否可用
            if (NativeNodeEngine.isAvailable(context)) {
                log("使用 esbuild 构建")
                return@withContext buildWithEsbuild(projectDir, File(outputPath), onProgress)
            }
            
            // 尝试初始化 esbuild
            log("初始化构建工具...")
            val initResult = NativeNodeEngine.initialize(context) { step, progress ->
                log(step)
                onProgress(step, progress * 0.3f)
            }
            
            if (initResult.isSuccess && NativeNodeEngine.isAvailable(context)) {
                log("使用 esbuild 构建")
                return@withContext buildWithEsbuild(projectDir, File(outputPath), onProgress)
            }
            
            // 最后手段：纯 Kotlin 打包
            log("使用内置打包器")
            return@withContext buildWithPureKotlin(projectDir, File(outputPath), onProgress)
            
        } catch (e: Exception) {
            Log.e(TAG, "Build failed", e)
            log("错误: ${e.message}")
            _state.value = PureBuildState.Error(e.message ?: "未知错误")
            Result.failure(e)
        }
    }
    
    /**
     * 查找已有的构建输出
     */
    private fun findExistingDist(projectDir: File): File? {
        val candidates = listOf("dist", "build", "out", ".output", "public")
        
        for (name in candidates) {
            val dir = File(projectDir, name)
            if (dir.exists() && dir.isDirectory) {
                // Check是否包含 index.html
                if (File(dir, "index.html").exists()) {
                    return dir
                }
                // Check子目录
                dir.listFiles()?.forEach { subDir ->
                    if (subDir.isDirectory && File(subDir, "index.html").exists()) {
                        return subDir
                    }
                }
            }
        }
        
        // Check项目根目录是否就是构建输出
        if (File(projectDir, "index.html").exists()) {
            return projectDir
        }
        
        return null
    }
    
    /**
     * 使用 esbuild 构建
     */
    private suspend fun buildWithEsbuild(
        projectDir: File,
        outputDir: File,
        onProgress: (String, Float) -> Unit
    ): Result<BuildResult> = withContext(Dispatchers.IO) {
        _state.value = PureBuildState.Building("esbuild", 0f)
        
        // 检测入口文件
        val entryFile = detectEntryFile(projectDir)
            ?: throw Exception("找不到入口文件")
        
        log("入口文件: ${entryFile.name}")
        
        outputDir.mkdirs()
        
        // Build参数
        val args = mutableListOf(
            entryFile.absolutePath,
            "--bundle",
            "--outdir=${outputDir.absolutePath}",
            "--format=esm",
            "--platform=browser",
            "--target=es2020",
            "--minify",
            "--sourcemap"
        )
        
        // If it is JSX/TSX，添加相应 loader
        if (entryFile.extension in listOf("jsx", "tsx")) {
            args.add("--loader:.jsx=jsx")
            args.add("--loader:.tsx=tsx")
        }
        
        log("执行 esbuild...")
        onProgress("构建中", 0.5f)
        
        val result = NativeNodeEngine.executeEsbuild(
            context = context,
            args = args,
            workingDir = projectDir,
            onOutput = { log(it) }
        )
        
        if (result.exitCode != 0) {
            throw Exception("esbuild 构建失败: ${result.stderr}")
        }
        
        // Generate index.html
        generateIndexHtml(outputDir, entryFile.nameWithoutExtension)
        
        // Copy静态资源
        copyStaticAssets(projectDir, outputDir)
        
        _state.value = PureBuildState.Success(outputDir.absolutePath)
        onProgress("Done", 1f)
        
        Result.success(BuildResult(
            outputPath = outputDir.absolutePath,
            method = BuildMethod.ESBUILD,
            fileCount = countFiles(outputDir),
            totalSize = calculateSize(outputDir)
        ))
    }
    
    /**
     * 使用纯 Kotlin 打包
     * 
     * 这是最后的手段，功能有限但不依赖任何外部工具
     */
    private suspend fun buildWithPureKotlin(
        projectDir: File,
        outputDir: File,
        onProgress: (String, Float) -> Unit
    ): Result<BuildResult> = withContext(Dispatchers.IO) {
        _state.value = PureBuildState.Building("内置打包器", 0f)
        log("使用内置打包器（功能有限）")
        
        outputDir.mkdirs()
        
        // 检测入口文件
        val entryFile = detectEntryFile(projectDir)
        
        if (entryFile != null) {
            // 简单打包：收集所有 JS/TS 文件
            log("收集源文件...")
            onProgress("收集文件", 0.3f)
            
            val sourceFiles = collectSourceFiles(projectDir)
            log("找到 ${sourceFiles.size} 个源文件")
            
            // 简单合并（不做真正的模块解析）
            log("打包中...")
            onProgress("打包", 0.6f)
            
            val bundleContent = buildSimpleBundle(sourceFiles, entryFile)
            
            // 写入输出
            File(outputDir, "bundle.js").writeText(bundleContent)
            
            // Generate index.html
            generateIndexHtml(outputDir, "bundle", isModule = false)
        }
        
        // Copy静态资源
        log("复制静态资源...")
        onProgress("复制资源", 0.8f)
        copyStaticAssets(projectDir, outputDir)
        
        // 如果有 index.html，复制它
        val indexHtml = File(projectDir, "index.html")
        if (indexHtml.exists()) {
            indexHtml.copyTo(File(outputDir, "index.html"), overwrite = true)
        }
        
        _state.value = PureBuildState.Success(outputDir.absolutePath)
        onProgress("Done", 1f)
        
        Result.success(BuildResult(
            outputPath = outputDir.absolutePath,
            method = BuildMethod.PURE_KOTLIN,
            fileCount = countFiles(outputDir),
            totalSize = calculateSize(outputDir)
        ))
    }
    
    /**
     * 检测入口文件
     */
    private fun detectEntryFile(projectDir: File): File? {
        val candidates = listOf(
            "src/main.ts", "src/main.tsx", "src/main.js", "src/main.jsx",
            "src/index.ts", "src/index.tsx", "src/index.js", "src/index.jsx",
            "src/App.tsx", "src/App.jsx", "src/App.ts", "src/App.js",
            "main.ts", "main.tsx", "main.js", "main.jsx",
            "index.ts", "index.tsx", "index.js", "index.jsx"
        )
        
        for (candidate in candidates) {
            val file = File(projectDir, candidate)
            if (file.exists()) return file
        }
        
        // 从 package.json 读取
        val packageJson = File(projectDir, "package.json")
        if (packageJson.exists()) {
            try {
                val content = packageJson.readText()
                // 简单解析 main 字段
                val mainMatch = Regex(""""main"\s*:\s*"([^"]+)"""").find(content)
                mainMatch?.groupValues?.get(1)?.let { main ->
                    val mainFile = File(projectDir, main)
                    if (mainFile.exists()) return mainFile
                }
            } catch (e: Exception) {
                Log.w(TAG, "解析 package.json 失败", e)
            }
        }
        
        return null
    }
    
    /**
     * 收集源文件
     */
    private fun collectSourceFiles(projectDir: File): List<File> {
        val extensions = setOf("js", "jsx", "ts", "tsx", "mjs")
        val excludeDirs = setOf("node_modules", "dist", "build", ".git", ".cache")
        
        return projectDir.walkTopDown()
            .filter { it.isFile }
            .filter { it.extension in extensions }
            .filter { file -> 
                !excludeDirs.any { excluded -> 
                    file.absolutePath.contains("/$excluded/") 
                }
            }
            .toList()
    }
    
    /**
     * 简单打包
     * 
     * 注意：这是一个非常简化的实现
     * 不支持真正的模块解析，只是简单合并
     */
    private fun buildSimpleBundle(files: List<File>, entryFile: File): String {
        val sb = StringBuilder()
        
        sb.appendLine("// Auto-generated bundle")
        sb.appendLine("(function() {")
        sb.appendLine("'use strict';")
        sb.appendLine()
        
        // 按依赖顺序排序（简单实现：入口文件放最后）
        val sortedFiles = files.sortedBy { 
            if (it == entryFile) 1 else 0 
        }
        
        for (file in sortedFiles) {
            sb.appendLine("// === ${file.name} ===")
            
            var content = file.readText()
            
            // 移除 import/export 语句（简化处理）
            content = content
                .replace(Regex("""import\s+.*?from\s+['"][^'"]+['"];?\s*"""), "")
                .replace(Regex("""import\s+['"][^'"]+['"];?\s*"""), "")
                .replace(Regex("""export\s+default\s+"""), "")
                .replace(Regex("""export\s+\{[^}]*\};?\s*"""), "")
                .replace(Regex("""export\s+(const|let|var|function|class)\s+"""), "$1 ")
            
            // 简单的 TypeScript 转换（移除类型注解）
            if (file.extension in listOf("ts", "tsx")) {
                content = stripTypeAnnotations(content)
            }
            
            // 简单的 JSX 转换
            if (file.extension in listOf("jsx", "tsx")) {
                content = transformJsx(content)
            }
            
            sb.appendLine(content)
            sb.appendLine()
        }
        
        sb.appendLine("})();")
        
        return sb.toString()
    }
    
    /**
     * 移除 TypeScript 类型注解（简化实现）
     */
    private fun stripTypeAnnotations(code: String): String {
        var result = code
        
        // 移除类型导入
        result = result.replace(Regex("""import\s+type\s+.*?;"""), "")
        
        // 移除变量类型注解 (: Type)
        result = result.replace(Regex("""\s*:\s*[A-Z][a-zA-Z0-9<>,\s\[\]|&]*(?=\s*[=;,)\]])"""), "")
        
        // 移除函数返回类型
        result = result.replace(Regex("""\)\s*:\s*[A-Z][a-zA-Z0-9<>,\s\[\]|&]*\s*(?=\{)"""), ") ")
        
        // 移除泛型参数
        result = result.replace(Regex("""<[A-Z][a-zA-Z0-9<>,\s\[\]|&]*>"""), "")
        
        // 移除 interface 和 type 声明
        result = result.replace(Regex("""(interface|type)\s+\w+\s*[^{]*\{[^}]*\}"""), "")
        
        return result
    }
    
    /**
     * 简单的 JSX 转换
     */
    private fun transformJsx(code: String): String {
        var result = code
        
        // 自闭合标签: <Component /> -> React.createElement(Component)
        result = result.replace(
            Regex("""<(\w+)\s*/>""")
        ) { match ->
            val tag = match.groupValues[1]
            if (tag[0].isUpperCase()) {
                "React.createElement($tag)"
            } else {
                "React.createElement('$tag')"
            }
        }
        
        // 简单标签: <div>content</div> -> React.createElement('div', null, 'content')
        result = result.replace(
            Regex("""<(\w+)>([^<]*)</\1>""")
        ) { match ->
            val tag = match.groupValues[1]
            val content = match.groupValues[2].trim()
            val tagArg = if (tag[0].isUpperCase()) tag else "'$tag'"
            if (content.isEmpty()) {
                "React.createElement($tagArg)"
            } else {
                "React.createElement($tagArg, null, '$content')"
            }
        }
        
        return result
    }
    
    /**
     * 生成 index.html
     */
    private fun generateIndexHtml(outputDir: File, bundleName: String, isModule: Boolean = true) {
        val moduleAttr = if (isModule) """ type="module"""" else ""
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>App</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div id="root"></div>
    <div id="app"></div>
    <script$moduleAttr src="$bundleName.js"></script>
</body>
</html>
        """.trimIndent()
        
        val indexFile = File(outputDir, "index.html")
        if (!indexFile.exists()) {
            indexFile.writeText(html)
        }
    }
    
    /**
     * 复制静态资源
     */
    private fun copyStaticAssets(projectDir: File, outputDir: File) {
        val assetDirs = listOf("public", "static", "assets")
        val assetExtensions = setOf("css", "png", "jpg", "jpeg", "gif", "svg", "ico", "woff", "woff2", "ttf", "eot")
        
        // Copy资源目录
        for (dirName in assetDirs) {
            val dir = File(projectDir, dirName)
            if (dir.exists() && dir.isDirectory) {
                copyDirectory(dir, File(outputDir, dirName)) { }
            }
        }
        
        // Copy根目录的静态文件
        projectDir.listFiles()?.filter { 
            it.isFile && it.extension in assetExtensions 
        }?.forEach { file ->
            file.copyTo(File(outputDir, file.name), overwrite = true)
        }
        
        // Copy src 目录下的 CSS
        val srcDir = File(projectDir, "src")
        if (srcDir.exists()) {
            srcDir.walkTopDown()
                .filter { it.isFile && it.extension == "css" }
                .forEach { cssFile ->
                    cssFile.copyTo(File(outputDir, cssFile.name), overwrite = true)
                }
        }
    }
    
    /**
     * 复制目录
     */
    private fun copyDirectory(src: File, dest: File, onProgress: (Float) -> Unit) {
        if (!src.exists()) return
        
        dest.mkdirs()
        
        val files = src.walkTopDown().filter { it.isFile }.toList()
        val total = files.size
        
        files.forEachIndexed { index, file ->
            val relativePath = file.relativeTo(src).path
            val destFile = File(dest, relativePath)
            destFile.parentFile?.mkdirs()
            file.copyTo(destFile, overwrite = true)
            onProgress((index + 1).toFloat() / total)
        }
    }
    
    private fun countFiles(dir: File): Int {
        return dir.walkTopDown().filter { it.isFile }.count()
    }
    
    private fun calculateSize(dir: File): Long {
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    private fun log(message: String) {
        Log.d(TAG, message)
        _logs.value = _logs.value + message
    }
    
    fun reset() {
        _state.value = PureBuildState.Idle
        _logs.value = emptyList()
    }
}

/**
 * 构建状态
 */
sealed class PureBuildState {
    object Idle : PureBuildState()
    object Analyzing : PureBuildState()
    data class Copying(val progress: Float) : PureBuildState()
    data class Building(val tool: String, val progress: Float) : PureBuildState()
    data class Success(val outputPath: String) : PureBuildState()
    data class Error(val message: String) : PureBuildState()
}

/**
 * 构建结果
 */
data class BuildResult(
    val outputPath: String,
    val method: BuildMethod,
    val fileCount: Int,
    val totalSize: Long
)

/**
 * 构建方法
 */
enum class BuildMethod {
    EXISTING_DIST,  // 使用已有的构建输出
    ESBUILD,        // 使用 esbuild
    PURE_KOTLIN     // 使用纯 Kotlin 打包器
}
