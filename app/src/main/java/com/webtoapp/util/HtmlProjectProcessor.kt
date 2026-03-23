package com.webtoapp.util

import android.util.Log
import android.util.LruCache
import java.io.File
import java.nio.charset.Charset

/**
 * HTML 项目处理器
 * 
 * 功能：
 * 1. 检测和修复 HTML 中的资源路径引用
 * 2. 将外部 CSS/JS 内联到 HTML 中
 * 3. 检测文件编码并正确读取
 * 4. 分析项目结构并提供问题诊断
 */
object HtmlProjectProcessor {
    
    private const val TAG = "HtmlProjectProcessor"
    
    // 编码检测缓存
    private val encodingCache = LruCache<String, String>(50)
    
    // 预编译的正则表达式
    private val cssLinkRegex by lazy { 
        Regex("""<link[^>]*href=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE) 
    }
    private val jsScriptRegex by lazy { 
        Regex("""<script[^>]*src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE) 
    }
    private val imgSrcRegex by lazy { 
        Regex("""<img[^>]*src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE) 
    }
    private val absolutePathRegex by lazy { 
        Regex("""(href|src)=["'](/[^"']+)["']""") 
    }
    private val parentPathRegex by lazy { 
        Regex("""(href|src)=["'](\.\.+/[^"']+)["']""") 
    }
    private val localCssRegex by lazy { 
        Regex("""<link[^>]*href=["'](?!https?://)[^"']*\.css["'][^>]*>""", RegexOption.IGNORE_CASE) 
    }
    private val localJsRegex by lazy { 
        Regex("""<script[^>]*src=["'](?!https?://)[^"']*\.js["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE) 
    }
    private val charsetRegex by lazy { 
        Regex("""charset=["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE) 
    }
    
    /**
     * 项目分析结果
     */
    data class ProjectAnalysis(
        val htmlFiles: List<FileInfo>,
        val cssFiles: List<FileInfo>,
        val jsFiles: List<FileInfo>,
        val otherFiles: List<FileInfo>,
        val issues: List<ProjectIssue>,
        val suggestions: List<String>
    )
    
    /**
     * 文件信息
     */
    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val encoding: String?,
        val references: List<ResourceReference> = emptyList()
    )
    
    /**
     * 资源引用
     */
    data class ResourceReference(
        val type: ReferenceType,
        val originalPath: String,
        val resolvedPath: String?,
        val isValid: Boolean,
        val issue: String? = null
    )
    
    enum class ReferenceType {
        CSS_LINK,       // <link href="...">
        JS_SCRIPT,      // <script src="...">
        IMAGE,          // <img src="...">
        CSS_IMPORT,     // @import url(...)
        CSS_URL,        // url(...) in CSS
        OTHER
    }
    
    /**
     * 项目问题
     */
    data class ProjectIssue(
        val severity: IssueSeverity,
        val type: IssueType,
        val message: String,
        val file: String? = null,
        val suggestion: String? = null
    )
    
    enum class IssueSeverity {
        ERROR,      // 会导致功能失效
        WARNING,    // 可能导致问题
        INFO        // 提示信息
    }
    
    enum class IssueType {
        MISSING_FILE,           // Reference的文件不存在
        ABSOLUTE_PATH,          // 使用了绝对路径
        ENCODING_ISSUE,         // 编码问题
        STRUCTURE_ISSUE,        // Directory结构问题
        SYNTAX_ERROR,           // 语法错误
        EXTERNAL_RESOURCE       // Reference了外部资源
    }
    
    /**
     * 分析 HTML 项目
     */
    fun analyzeProject(
        htmlFilePath: String?,
        cssFilePath: String?,
        jsFilePath: String?,
        additionalFiles: List<String> = emptyList()
    ): ProjectAnalysis {
        val issues = mutableListOf<ProjectIssue>()
        val suggestions = mutableListOf<String>()
        
        val htmlFiles = mutableListOf<FileInfo>()
        val cssFiles = mutableListOf<FileInfo>()
        val jsFiles = mutableListOf<FileInfo>()
        val otherFiles = mutableListOf<FileInfo>()
        
        // 分析 HTML 文件
        htmlFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val encoding = detectEncoding(file)
                val content = readFileWithEncoding(file, encoding)
                val references = analyzeHtmlReferences(content, file.parentFile)
                
                htmlFiles.add(FileInfo(
                    name = file.name,
                    path = path,
                    size = file.length(),
                    encoding = encoding,
                    references = references
                ))
                
                // Check引用问题
                references.forEach { ref ->
                    if (!ref.isValid) {
                        issues.add(ProjectIssue(
                            severity = IssueSeverity.WARNING,
                            type = if (ref.originalPath.startsWith("/")) IssueType.ABSOLUTE_PATH else IssueType.MISSING_FILE,
                            message = ref.issue ?: "资源引用问题: ${ref.originalPath}",
                            file = file.name,
                            suggestion = when {
                                ref.originalPath.startsWith("/") -> "建议使用相对路径，如 './style.css' 而非 '/style.css'"
                                else -> "请确保引用的文件 '${ref.originalPath}' 已导入"
                            }
                        ))
                    }
                }
            } else {
                issues.add(ProjectIssue(
                    severity = IssueSeverity.ERROR,
                    type = IssueType.MISSING_FILE,
                    message = "HTML 文件不存在: $path"
                ))
            }
        }
        
        // 分析 CSS 文件
        cssFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val encoding = detectEncoding(file)
                cssFiles.add(FileInfo(
                    name = file.name,
                    path = path,
                    size = file.length(),
                    encoding = encoding
                ))
                
                // Check编码
                if (encoding != "UTF-8" && encoding != null) {
                    issues.add(ProjectIssue(
                        severity = IssueSeverity.WARNING,
                        type = IssueType.ENCODING_ISSUE,
                        message = "CSS 文件编码为 $encoding，建议使用 UTF-8",
                        file = file.name,
                        suggestion = "将文件另存为 UTF-8 编码可避免中文乱码问题"
                    ))
                }
            }
        }
        
        // 分析 JS 文件
        jsFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val encoding = detectEncoding(file)
                val content = readFileWithEncoding(file, encoding)
                
                jsFiles.add(FileInfo(
                    name = file.name,
                    path = path,
                    size = file.length(),
                    encoding = encoding
                ))
                
                // Check常见 JS 问题
                checkJsIssues(content, file.name, issues)
            }
        }
        
        // Generate建议
        if (htmlFiles.isNotEmpty() && cssFiles.isEmpty() && jsFiles.isEmpty()) {
            val htmlContent = htmlFilePath?.let { File(it).takeIf { f -> f.exists() }?.readText() } ?: ""
            if (htmlContent.contains("<link", ignoreCase = true) || htmlContent.contains("<script", ignoreCase = true)) {
                suggestions.add("检测到 HTML 中引用了外部 CSS/JS 文件，请确保已导入对应文件")
            }
        }
        
        if (issues.any { it.type == IssueType.ABSOLUTE_PATH }) {
            suggestions.add("建议将所有资源路径改为相对路径，以确保在应用中正常加载")
        }
        
        return ProjectAnalysis(
            htmlFiles = htmlFiles,
            cssFiles = cssFiles,
            jsFiles = jsFiles,
            otherFiles = otherFiles,
            issues = issues,
            suggestions = suggestions
        )
    }
    
    /**
     * 处理 HTML 内容，内联 CSS 和 JS
     */
    fun processHtmlContent(
        htmlContent: String,
        cssContent: String?,
        jsContent: String?,
        fixPaths: Boolean = true
    ): String {
        var result = htmlContent
        
        // 1. 修复路径引用
        if (fixPaths) {
            result = fixResourcePaths(result)
        }
        
        // 2. 移除本地 CSS/JS 引用（因为会内联）
        result = removeLocalResourceReferences(result)
        
        // 3. 内联 CSS
        if (!cssContent.isNullOrBlank()) {
            result = inlineCss(result, cssContent)
        }
        
        // 4. 内联 JS
        if (!jsContent.isNullOrBlank()) {
            result = inlineJs(result, jsContent)
        }
        
        // 5. 添加 viewport meta（如果没有）
        if (!result.contains("viewport", ignoreCase = true)) {
            result = addViewportMeta(result)
        }
        
        return result
    }
    
    /**
     * 修复资源路径引用
     */
    private fun fixResourcePaths(html: String): String {
        var result = html
        
        // 修复绝对路径为相对路径
        // /css/style.css -> ./css/style.css
        result = absolutePathRegex.replace(result) { match ->
            val attr = match.groupValues[1]
            val path = match.groupValues[2]
            """$attr=".${path}""""
        }
        
        // 修复 ../ 开头的路径（可能导致访问应用外部）
        result = parentPathRegex.replace(result) { match ->
            val attr = match.groupValues[1]
            val path = match.groupValues[2]
            // 将 ../ 替换为 ./
            val fixedPath = path.replace(Regex("""^\.\.+/"""), "./")
            """$attr="$fixedPath""""
        }
        
        return result
    }
    
    /**
     * 移除本地资源引用
     */
    private fun removeLocalResourceReferences(html: String): String {
        var result = html
        
        // 移除本地 CSS link 标签（保留外部 CDN 链接）
        result = localCssRegex.replace(result, "<!-- CSS inlined -->")
        
        // 移除本地 JS script 标签（保留外部 CDN 链接）
        result = localJsRegex.replace(result, "<!-- JS inlined -->")
        
        return result
    }
    
    /**
     * 内联 CSS
     */
    private fun inlineCss(html: String, css: String): String {
        val styleTag = "<style>\n/* Inlined CSS */\n$css\n</style>"
        val escapedStyleTag = Regex.escapeReplacement(styleTag)
        
        return when {
            html.contains("</head>", ignoreCase = true) -> {
                html.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$escapedStyleTag\n</head>")
            }
            html.contains("<body", ignoreCase = true) -> {
                html.replaceFirst(Regex("<body", RegexOption.IGNORE_CASE), "$escapedStyleTag\n<body")
            }
            html.contains("<html", ignoreCase = true) -> {
                val regex = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)
                val match = regex.find(html)
                if (match != null) {
                    html.substring(0, match.range.last + 1) + 
                        "\n<head>\n$styleTag\n</head>" + 
                        html.substring(match.range.last + 1)
                } else {
                    "$styleTag\n$html"
                }
            }
            else -> "$styleTag\n$html"
        }
    }
    
    /**
     * 内联 JS
     */
    private fun inlineJs(html: String, js: String): String {
        val wrappedJs = wrapJsForSafeExecution(js)
        val scriptTag = "<script>\n/* Inlined JS */\n$wrappedJs\n</script>"
        val escapedScriptTag = Regex.escapeReplacement(scriptTag)
        
        return when {
            html.contains("</body>", ignoreCase = true) -> {
                html.replaceFirst(Regex("</body>", RegexOption.IGNORE_CASE), "$escapedScriptTag\n</body>")
            }
            html.contains("</html>", ignoreCase = true) -> {
                html.replaceFirst(Regex("</html>", RegexOption.IGNORE_CASE), "$escapedScriptTag\n</html>")
            }
            else -> "$html\n$scriptTag"
        }
    }
    
    /**
     * 包装 JS 确保安全执行
     */
    private fun wrapJsForSafeExecution(js: String): String {
        val trimmed = js.trim()
        if (trimmed.isEmpty()) return ""
        
        // Check是否已有 DOM 加载包装
        val hasWrapper = trimmed.contains("DOMContentLoaded", ignoreCase = true) ||
                        trimmed.contains("window.onload", ignoreCase = true) ||
                        trimmed.contains("addEventListener('load'", ignoreCase = true) ||
                        trimmed.contains("addEventListener(\"load\"", ignoreCase = true) ||
                        trimmed.contains("\$(document).ready", ignoreCase = true) ||
                        trimmed.contains("\$(function()", ignoreCase = true)
        
        return if (hasWrapper) {
            trimmed
        } else {
            """
(function() {
    'use strict';
    
    function initApp() {
        try {
$trimmed
        } catch (e) {
            console.error('[WebToApp] JS execution error:', e);
        }
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initApp);
    } else {
        initApp();
    }
})();
            """.trimIndent()
        }
    }
    
    /**
     * 添加 viewport meta
     */
    private fun addViewportMeta(html: String): String {
        val viewportMeta = """<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">"""
        
        return when {
            html.contains("<head>", ignoreCase = true) -> {
                html.replaceFirst(Regex("<head>", RegexOption.IGNORE_CASE), "<head>\n$viewportMeta")
            }
            html.contains("<html", ignoreCase = true) -> {
                val regex = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)
                val match = regex.find(html)
                if (match != null) {
                    html.substring(0, match.range.last + 1) + 
                        "\n<head>\n$viewportMeta\n</head>" + 
                        html.substring(match.range.last + 1)
                } else {
                    "$viewportMeta\n$html"
                }
            }
            else -> "$viewportMeta\n$html"
        }
    }
    
    /**
     * 分析 HTML 中的资源引用
     */
    private fun analyzeHtmlReferences(html: String, baseDir: File?): List<ResourceReference> {
        val references = mutableListOf<ResourceReference>()
        
        // CSS link 标签
        cssLinkRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            if (!path.startsWith("http://") && !path.startsWith("https://") && path.endsWith(".css", ignoreCase = true)) {
                references.add(analyzeReference(path, ReferenceType.CSS_LINK, baseDir))
            }
        }
        
        // JS script 标签
        jsScriptRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            if (!path.startsWith("http://") && !path.startsWith("https://")) {
                references.add(analyzeReference(path, ReferenceType.JS_SCRIPT, baseDir))
            }
        }
        
        // Image
        imgSrcRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            if (!path.startsWith("http://") && !path.startsWith("https://") && !path.startsWith("data:")) {
                references.add(analyzeReference(path, ReferenceType.IMAGE, baseDir))
            }
        }
        
        return references
    }
    
    /**
     * 分析单个资源引用
     */
    private fun analyzeReference(path: String, type: ReferenceType, baseDir: File?): ResourceReference {
        val isAbsolute = path.startsWith("/")
        val resolvedPath = if (baseDir != null && !isAbsolute) {
            File(baseDir, path.removePrefix("./")).absolutePath
        } else null
        
        val exists = resolvedPath?.let { File(it).exists() } ?: false
        
        val issue = when {
            isAbsolute -> "使用了绝对路径，可能导致资源无法加载"
            !exists && resolvedPath != null -> "引用的文件不存在"
            else -> null
        }
        
        return ResourceReference(
            type = type,
            originalPath = path,
            resolvedPath = resolvedPath,
            isValid = !isAbsolute && (exists || resolvedPath == null),
            issue = issue
        )
    }
    
    /**
     * 检测文件编码（带缓存）
     */
    private fun detectEncoding(file: File): String? {
        val cacheKey = file.absolutePath + "_" + file.lastModified()
        
        // Check缓存
        encodingCache.get(cacheKey)?.let { return it }
        
        return try {
            val bytes = file.readBytes().take(1000).toByteArray()
            
            val encoding = when {
                // Check BOM
                bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> "UTF-8"
                bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> "UTF-16BE"
                bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> "UTF-16LE"
                else -> {
                    // 尝试检测 charset 声明
                    val content = String(bytes, Charsets.ISO_8859_1)
                    charsetRegex.find(content)?.groupValues?.get(1)?.uppercase() ?: "UTF-8"
                }
            }
            
            // Cache结果
            encodingCache.put(cacheKey, encoding)
            encoding
        } catch (e: Exception) {
            Log.e(TAG, "检测编码失败: ${file.path}", e)
            "UTF-8"
        }
    }
    
    /**
     * 清除编码缓存
     */
    fun clearEncodingCache() {
        encodingCache.evictAll()
    }
    
    /**
     * 使用正确编码读取文件
     */
    fun readFileWithEncoding(file: File, encoding: String?): String {
        return try {
            val charset = when (encoding?.uppercase()) {
                "UTF-8", "UTF8" -> Charsets.UTF_8
                "GBK", "GB2312", "GB18030" -> Charset.forName("GBK")
                "UTF-16", "UTF-16BE" -> Charsets.UTF_16BE
                "UTF-16LE" -> Charsets.UTF_16LE
                "ISO-8859-1", "LATIN1" -> Charsets.ISO_8859_1
                else -> Charsets.UTF_8
            }
            file.readText(charset)
        } catch (e: Exception) {
            Log.e(TAG, "读取文件失败: ${file.path}", e)
            // 降级尝试
            try {
                file.readText(Charsets.UTF_8)
            } catch (e2: Exception) {
                file.readText(Charsets.ISO_8859_1)
            }
        }
    }
    
    /**
     * 检查 JS 常见问题
     */
    private fun checkJsIssues(content: String, fileName: String, issues: MutableList<ProjectIssue>) {
        // Check是否使用了 document.write（在 WebView 中可能有问题）
        if (content.contains("document.write", ignoreCase = true)) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.WARNING,
                type = IssueType.SYNTAX_ERROR,
                message = "使用了 document.write()，可能导致页面加载问题",
                file = fileName,
                suggestion = "建议使用 DOM 操作方法替代 document.write()"
            ))
        }
        
        // Check是否有语法错误的常见模式
        val unclosedBraces = content.count { it == '{' } - content.count { it == '}' }
        if (unclosedBraces != 0) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.WARNING,
                type = IssueType.SYNTAX_ERROR,
                message = "可能存在未闭合的大括号",
                file = fileName,
                suggestion = "请检查 JS 代码中的大括号是否正确配对"
            ))
        }
    }
}
