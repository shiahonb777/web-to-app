package com.webtoapp.core.linux

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * HTML 项目优化器
 * 
 * 利用 Linux 构建环境（esbuild）对 HTML 项目进行优化：
 * - JS/CSS 文件压缩（minify）
 * - TypeScript 编译为 JavaScript
 * - 多 JS 文件打包为单文件 bundle
 * 
 * 如果 esbuild 不可用，使用纯 Kotlin 实现的基础压缩作为 fallback。
 */
object HtmlProjectOptimizer {
    
    private const val TAG = "HtmlProjectOptimizer"
    
    /**
     * 优化结果
     */
    data class OptimizeResult(
        val success: Boolean,
        val jsFilesOptimized: Int = 0,
        val cssFilesOptimized: Int = 0,
        val tsFilesCompiled: Int = 0,
        val savedBytes: Long = 0,
        val error: String? = null
    )
    
    /**
     * 优化单个文件（手动模式：HTML+CSS+JS 独立文件）
     * 
     * @param context Android Context
     * @param jsFilePath JS 文件路径（可为 null）
     * @param cssFilePath CSS 文件路径（可为 null）
     * @param onProgress 进度回调
     * @return 优化结果
     */
    suspend fun optimizeFiles(
        context: Context,
        jsFilePath: String?,
        cssFilePath: String?,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): OptimizeResult = withContext(Dispatchers.IO) {
        var jsOptimized = 0
        var cssOptimized = 0
        var savedBytes = 0L
        
        try {
            val esbuildAvailable = NativeNodeEngine.isAvailable(context)
            
            // 优化 JS 文件
            if (jsFilePath != null) {
                val jsFile = File(jsFilePath)
                if (jsFile.exists() && jsFile.length() > 0) {
                    onProgress("优化 JavaScript...", 0.2f)
                    val originalSize = jsFile.length()
                    
                    if (esbuildAvailable) {
                        val result = minifyWithEsbuild(context, jsFile)
                        if (result) {
                            savedBytes += originalSize - jsFile.length()
                            jsOptimized++
                        }
                    } else {
                        val result = minifyJsPure(jsFile)
                        if (result) {
                            savedBytes += originalSize - jsFile.length()
                            jsOptimized++
                        }
                    }
                }
            }
            
            // 优化 CSS 文件
            if (cssFilePath != null) {
                val cssFile = File(cssFilePath)
                if (cssFile.exists() && cssFile.length() > 0) {
                    onProgress("优化 CSS...", 0.6f)
                    val originalSize = cssFile.length()
                    
                    if (esbuildAvailable) {
                        val result = minifyWithEsbuild(context, cssFile)
                        if (result) {
                            savedBytes += originalSize - cssFile.length()
                            cssOptimized++
                        }
                    } else {
                        val result = minifyCssPure(cssFile)
                        if (result) {
                            savedBytes += originalSize - cssFile.length()
                            cssOptimized++
                        }
                    }
                }
            }
            
            onProgress("完成", 1f)
            
            OptimizeResult(
                success = true,
                jsFilesOptimized = jsOptimized,
                cssFilesOptimized = cssOptimized,
                savedBytes = savedBytes
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "优化失败", e)
            OptimizeResult(success = false, error = e.message)
        }
    }
    
    /**
     * 优化整个项目目录（ZIP 导入模式）
     * 
     * @param context Android Context
     * @param projectDir 项目根目录
     * @param onProgress 进度回调
     * @return 优化结果
     */
    suspend fun optimizeDirectory(
        context: Context,
        projectDir: String,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): OptimizeResult = withContext(Dispatchers.IO) {
        var jsOptimized = 0
        var cssOptimized = 0
        var tsCompiled = 0
        var savedBytes = 0L
        
        try {
            val dir = File(projectDir)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext OptimizeResult(success = false, error = "目录不存在")
            }
            
            val esbuildAvailable = NativeNodeEngine.isAvailable(context)
            
            // 收集所有需要优化的文件
            val jsFiles = mutableListOf<File>()
            val cssFiles = mutableListOf<File>()
            val tsFiles = mutableListOf<File>()
            
            dir.walkTopDown()
                .filter { it.isFile }
                .filter { !it.absolutePath.contains("/node_modules/") }
                .filter { !it.absolutePath.contains("/.git/") }
                .forEach { file ->
                    when (file.extension.lowercase()) {
                        "js", "mjs" -> jsFiles.add(file)
                        "css" -> cssFiles.add(file)
                        "ts" -> tsFiles.add(file)
                        "tsx", "jsx" -> tsFiles.add(file)
                    }
                }
            
            val totalFiles = jsFiles.size + cssFiles.size + tsFiles.size
            if (totalFiles == 0) {
                return@withContext OptimizeResult(success = true)
            }
            
            var processedCount = 0
            
            // 1. 编译 TypeScript 文件
            if (tsFiles.isNotEmpty() && esbuildAvailable) {
                onProgress("编译 TypeScript (${tsFiles.size} 个文件)...", 0.1f)
                
                for (tsFile in tsFiles) {
                    val originalSize = tsFile.length()
                    val outputFile = File(tsFile.parentFile, tsFile.nameWithoutExtension + ".js")
                    
                    val result = compileTypeScriptWithEsbuild(context, tsFile, outputFile)
                    if (result) {
                        tsCompiled++
                        // 更新 HTML 中的引用：.ts → .js
                        updateHtmlReferences(dir, tsFile.name, outputFile.name)
                    }
                    processedCount++
                    onProgress(
                        "编译 TypeScript: ${tsFile.name}",
                        processedCount.toFloat() / totalFiles
                    )
                }
            }
            
            // 2. 压缩 JS 文件
            if (jsFiles.isNotEmpty()) {
                onProgress("压缩 JavaScript (${jsFiles.size} 个文件)...", 
                    processedCount.toFloat() / totalFiles)
                
                for (jsFile in jsFiles) {
                    val originalSize = jsFile.length()
                    
                    val result = if (esbuildAvailable) {
                        minifyWithEsbuild(context, jsFile)
                    } else {
                        minifyJsPure(jsFile)
                    }
                    
                    if (result) {
                        savedBytes += originalSize - jsFile.length()
                        jsOptimized++
                    }
                    processedCount++
                    onProgress(
                        "压缩 JS: ${jsFile.name}",
                        processedCount.toFloat() / totalFiles
                    )
                }
            }
            
            // 3. 压缩 CSS 文件
            if (cssFiles.isNotEmpty()) {
                onProgress("压缩 CSS (${cssFiles.size} 个文件)...",
                    processedCount.toFloat() / totalFiles)
                
                for (cssFile in cssFiles) {
                    val originalSize = cssFile.length()
                    
                    val result = if (esbuildAvailable) {
                        minifyWithEsbuild(context, cssFile)
                    } else {
                        minifyCssPure(cssFile)
                    }
                    
                    if (result) {
                        savedBytes += originalSize - cssFile.length()
                        cssOptimized++
                    }
                    processedCount++
                    onProgress(
                        "压缩 CSS: ${cssFile.name}",
                        processedCount.toFloat() / totalFiles
                    )
                }
            }
            
            onProgress("优化完成", 1f)
            
            OptimizeResult(
                success = true,
                jsFilesOptimized = jsOptimized,
                cssFilesOptimized = cssOptimized,
                tsFilesCompiled = tsCompiled,
                savedBytes = savedBytes
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "目录优化失败", e)
            OptimizeResult(success = false, error = e.message)
        }
    }
    
    // ==================== esbuild 优化 ====================
    
    /**
     * 使用 esbuild 压缩单个文件（就地压缩）
     */
    private suspend fun minifyWithEsbuild(
        context: Context,
        file: File
    ): Boolean {
        val tempOutput = File(file.parentFile, "${file.nameWithoutExtension}.min.${file.extension}")
        
        try {
            val args = listOf(
                file.absolutePath,
                "--outfile=${tempOutput.absolutePath}",
                "--minify",
                "--allow-overwrite"
            )
            
            val result = NativeNodeEngine.executeEsbuild(
                context = context,
                args = args,
                workingDir = file.parentFile ?: file,
                timeout = 30_000
            )
            
            if (result.exitCode == 0 && tempOutput.exists() && tempOutput.length() > 0) {
                // 用压缩后的文件替换原文件
                tempOutput.copyTo(file, overwrite = true)
                tempOutput.delete()
                return true
            } else {
                tempOutput.delete()
                AppLogger.w(TAG, "esbuild minify 失败: ${result.stderr}")
                return false
            }
        } catch (e: Exception) {
            tempOutput.delete()
            AppLogger.w(TAG, "esbuild minify 异常: ${e.message}")
            return false
        }
    }
    
    /**
     * 使用 esbuild 编译 TypeScript 为 JavaScript
     */
    private suspend fun compileTypeScriptWithEsbuild(
        context: Context,
        tsFile: File,
        outputFile: File
    ): Boolean {
        try {
            val loader = when (tsFile.extension.lowercase()) {
                "tsx" -> "tsx"
                "jsx" -> "jsx"
                else -> "ts"
            }
            
            val args = listOf(
                tsFile.absolutePath,
                "--outfile=${outputFile.absolutePath}",
                "--loader:.${tsFile.extension}=$loader",
                "--minify",
                "--platform=browser",
                "--target=es2020"
            )
            
            val result = NativeNodeEngine.executeEsbuild(
                context = context,
                args = args,
                workingDir = tsFile.parentFile ?: tsFile,
                timeout = 30_000
            )
            
            return result.exitCode == 0 && outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            AppLogger.w(TAG, "TypeScript 编译失败: ${e.message}")
            return false
        }
    }
    
    // ==================== 纯 Kotlin Fallback ====================
    
    /**
     * 纯 Kotlin JS 压缩（基础实现）
     * 移除注释、多余空白、换行
     */
    private fun minifyJsPure(file: File): Boolean {
        try {
            val content = file.readText()
            if (content.length < 100) return false // 太短不压缩
            
            val minified = StringBuilder()
            var i = 0
            var inString = false
            var stringChar = ' '
            var inSingleLineComment = false
            var inMultiLineComment = false
            var lastChar = ' '
            
            while (i < content.length) {
                val c = content[i]
                val next = if (i + 1 < content.length) content[i + 1] else ' '
                
                when {
                    // 字符串内容原样保留
                    inString -> {
                        minified.append(c)
                        if (c == stringChar && lastChar != '\\') {
                            inString = false
                        }
                    }
                    // 单行注释
                    inSingleLineComment -> {
                        if (c == '\n') {
                            inSingleLineComment = false
                            minified.append('\n')
                        }
                    }
                    // 多行注释
                    inMultiLineComment -> {
                        if (c == '*' && next == '/') {
                            inMultiLineComment = false
                            i++ // 跳过 '/'
                        }
                    }
                    // 检测注释开始
                    c == '/' && next == '/' -> {
                        inSingleLineComment = true
                        i++
                    }
                    c == '/' && next == '*' -> {
                        inMultiLineComment = true
                        i++
                    }
                    // 检测字符串开始
                    c == '"' || c == '\'' || c == '`' -> {
                        inString = true
                        stringChar = c
                        minified.append(c)
                    }
                    // 压缩连续空白
                    c.isWhitespace() -> {
                        if (minified.isNotEmpty() && !minified.last().isWhitespace()) {
                            // 保留必要的空格（关键字之间等）
                            if (minified.last().isLetterOrDigit() || minified.last() == '_' || minified.last() == '$') {
                                minified.append(' ')
                            }
                        }
                    }
                    else -> {
                        minified.append(c)
                    }
                }
                
                lastChar = c
                i++
            }
            
            val result = minified.toString().trim()
            if (result.length < content.length) {
                file.writeText(result)
                return true
            }
            return false
        } catch (e: Exception) {
            AppLogger.w(TAG, "纯 Kotlin JS 压缩失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 纯 Kotlin CSS 压缩（基础实现）
     * 移除注释、多余空白、换行
     */
    private fun minifyCssPure(file: File): Boolean {
        try {
            val content = file.readText()
            if (content.length < 100) return false
            
            var result = content
            // 移除 CSS 注释
            result = result.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
            // 压缩空白
            result = result.replace(Regex("\\s+"), " ")
            // 移除选择器和属性周围的空白
            result = result.replace(Regex("\\s*([{}:;,>~+])\\s*"), "$1")
            // 移除最后的分号
            result = result.replace(Regex(";\\}"), "}")
            // 移除头尾空白
            result = result.trim()
            
            if (result.length < content.length) {
                file.writeText(result)
                return true
            }
            return false
        } catch (e: Exception) {
            AppLogger.w(TAG, "纯 Kotlin CSS 压缩失败: ${e.message}")
            return false
        }
    }
    
    // ==================== 辅助函数 ====================
    
    /**
     * 更新 HTML 文件中的脚本引用（.ts → .js）
     */
    private fun updateHtmlReferences(projectDir: File, oldName: String, newName: String) {
        projectDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in listOf("html", "htm") }
            .forEach { htmlFile ->
                try {
                    var content = htmlFile.readText()
                    if (content.contains(oldName)) {
                        content = content.replace(oldName, newName)
                        htmlFile.writeText(content)
                        AppLogger.d(TAG, "更新 HTML 引用: ${htmlFile.name} ($oldName → $newName)")
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "更新 HTML 引用失败: ${htmlFile.name}", e)
                }
            }
    }
}
