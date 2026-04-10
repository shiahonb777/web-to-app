package com.webtoapp.core.linux

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*

/**
 * 全面性能优化器
 * 
 * 利用 Linux 构建环境（esbuild）+ 纯 Kotlin 实现，对 WebToApp 应用进行全方位性能优化：
 * 
 * 一、资源优化（减小 APK 体积）
 *   - 图片压缩：PNG/JPEG 质量优化、WebP 转换
 *   - JS/CSS 压缩：esbuild minify 或纯 Kotlin fallback
 *   - 移除未使用资源：清理模板 APK 中不需要的文件
 *   - SVG 精简：移除注释和多余空白
 * 
 * 二、构建速度优化（加速 APK 打包）
 *   - 并行文件处理：利用协程并发处理资源
 *   - 流式 ZIP 处理：避免大文件全量加载到内存
 *   - 智能缓存：缓存已优化的资源，避免重复处理
 *   - 增量处理：只处理变更的文件
 * 
 * 三、加载速度优化（提升应用启动和页面加载）
 *   - 关键 CSS 内联：提取关键渲染路径 CSS
 *   - 资源预加载提示：注入 preload/prefetch/preconnect
 *   - 图片懒加载：注入 Intersection Observer 懒加载
 *   - Script 优化：自动添加 defer/async 属性
 *   - DNS 预解析：注入 dns-prefetch 提示
 * 
 * 四、运行时性能优化
 *   - WebView 渲染优化脚本注入
 *   - 内存管理优化建议
 *   - 滚动性能优化（passive listeners）
 */
object PerformanceOptimizer {
    
    private const val TAG = "PerformanceOptimizer"
    
    // 优化缓存目录
    private const val CACHE_DIR = "perf_optimize_cache"
    
    // 图片压缩阈值（超过此大小才压缩）
    private const val IMAGE_COMPRESS_THRESHOLD = 10 * 1024L // 10KB
    
    // JS/CSS 压缩阈值
    private const val CODE_MINIFY_THRESHOLD = 512L // 512 bytes
    
    /**
     * 性能优化配置
     */
    data class OptimizeConfig(
        // 资源优化
        val compressImages: Boolean = true,
        val imageQuality: Int = 80,           // JPEG 质量 0-100
        val convertToWebP: Boolean = true,    // PNG/JPEG → WebP
        val minifyCode: Boolean = true,       // JS/CSS 压缩
        val minifySvg: Boolean = true,        // SVG 精简
        val removeUnusedResources: Boolean = true,
        
        // 构建优化
        val parallelProcessing: Boolean = true,
        val enableCache: Boolean = true,
        
        // 加载优化
        val injectPreloadHints: Boolean = true,
        val injectLazyLoading: Boolean = true,
        val optimizeScripts: Boolean = true,
        val injectDnsPrefetch: Boolean = true,
        
        // 运行时优化
        val injectPerformanceScript: Boolean = true
    )
    
    /**
     * 优化结果统计
     */
    data class OptimizeStats(
        val originalSize: Long = 0,
        val optimizedSize: Long = 0,
        val imagesCompressed: Int = 0,
        val codeFilesMinified: Int = 0,
        val resourcesRemoved: Int = 0,
        val preloadHintsInjected: Int = 0,
        val lazyImagesInjected: Int = 0,
        val optimizationTimeMs: Long = 0,
        val errors: List<String> = emptyList()
    ) {
        val savedBytes: Long get() = originalSize - optimizedSize
        val savedPercent: Float get() = if (originalSize > 0) (savedBytes * 100f / originalSize) else 0f
    }
    
    // ==================== 一、资源优化 ====================
    
    /**
     * 优化目录中的所有资源文件
     * 适用于 HTML/前端/WordPress 等项目的资源文件夹
     */
    suspend fun optimizeResources(
        context: Context,
        projectDir: File,
        config: OptimizeConfig = OptimizeConfig(),
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): OptimizeStats = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var originalSize = 0L
        var optimizedSize = 0L
        var imagesCompressed = 0
        var codeMinified = 0
        var resourcesRemoved = 0
        val errors = mutableListOf<String>()
        
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return@withContext OptimizeStats(errors = listOf("Project directory not found"))
        }
        
        val allFiles = projectDir.walkTopDown()
            .filter { it.isFile }
            .toList()
        
        val totalFiles = allFiles.size
        var processedCount = 0
        
        onProgress("扫描文件...", 0f)
        
        val esbuildAvailable = NativeNodeEngine.isAvailable(context)
        
        // 按类型分组
        val imageFiles = mutableListOf<File>()
        val jsFiles = mutableListOf<File>()
        val cssFiles = mutableListOf<File>()
        val svgFiles = mutableListOf<File>()
        val htmlFiles = mutableListOf<File>()
        
        for (file in allFiles) {
            originalSize += file.length()
            when (file.extension.lowercase()) {
                "png", "jpg", "jpeg" -> imageFiles.add(file)
                "js" -> if (!file.name.endsWith(".min.js")) jsFiles.add(file)
                "css" -> if (!file.name.endsWith(".min.css")) cssFiles.add(file)
                "svg" -> svgFiles.add(file)
                "html", "htm" -> htmlFiles.add(file)
            }
        }
        
        // 1. 图片压缩
        if (config.compressImages && imageFiles.isNotEmpty()) {
            onProgress("压缩图片 (${imageFiles.size})...", 0.1f)
            
            if (config.parallelProcessing) {
                coroutineScope {
                    imageFiles.chunked(4).forEach { chunk ->
                        val jobs = chunk.map { file ->
                            async(Dispatchers.IO) {
                                try {
                                    if (file.length() > IMAGE_COMPRESS_THRESHOLD) {
                                        val saved = compressImage(file, config)
                                        if (saved > 0) synchronized(errors) { imagesCompressed++ }
                                    }
                                    Unit
                                } catch (e: Exception) {
                                    synchronized(errors) { errors.add("图片压缩失败: ${file.name}: ${e.message}") }
                                }
                            }
                        }
                        jobs.awaitAll()
                    }
                }
            } else {
                for (file in imageFiles) {
                    try {
                        if (file.length() > IMAGE_COMPRESS_THRESHOLD) {
                            val saved = compressImage(file, config)
                            if (saved > 0) imagesCompressed++
                        }
                    } catch (e: Exception) {
                        errors.add("图片压缩失败: ${file.name}: ${e.message}")
                    }
                    processedCount++
                    onProgress("压缩图片: ${file.name}", processedCount.toFloat() / totalFiles * 0.4f)
                }
            }
        }
        
        // 2. JS/CSS 压缩
        if (config.minifyCode) {
            onProgress("压缩代码...", 0.4f)
            
            for (jsFile in jsFiles) {
                if (jsFile.length() > CODE_MINIFY_THRESHOLD) {
                    try {
                        val result = if (esbuildAvailable) {
                            minifyWithEsbuild(context, jsFile)
                        } else {
                            minifyJsFallback(jsFile)
                        }
                        if (result) codeMinified++
                    } catch (e: Exception) {
                        errors.add("JS 压缩失败: ${jsFile.name}: ${e.message}")
                    }
                }
                processedCount++
                onProgress("压缩 JS: ${jsFile.name}", 0.4f + processedCount.toFloat() / totalFiles * 0.2f)
            }
            
            for (cssFile in cssFiles) {
                if (cssFile.length() > CODE_MINIFY_THRESHOLD) {
                    try {
                        val result = if (esbuildAvailable) {
                            minifyWithEsbuild(context, cssFile)
                        } else {
                            minifyCssFallback(cssFile)
                        }
                        if (result) codeMinified++
                    } catch (e: Exception) {
                        errors.add("CSS 压缩失败: ${cssFile.name}: ${e.message}")
                    }
                }
                processedCount++
                onProgress("压缩 CSS: ${cssFile.name}", 0.6f + processedCount.toFloat() / totalFiles * 0.1f)
            }
        }
        
        // 3. SVG 精简
        if (config.minifySvg && svgFiles.isNotEmpty()) {
            onProgress("优化 SVG...", 0.7f)
            for (svgFile in svgFiles) {
                try {
                    if (minifySvg(svgFile)) codeMinified++
                } catch (e: Exception) {
                    errors.add("SVG 优化失败: ${svgFile.name}: ${e.message}")
                }
            }
        }
        
        // 4. HTML 优化（注入性能提示）
        if (htmlFiles.isNotEmpty()) {
            onProgress("优化 HTML...", 0.8f)
            for (htmlFile in htmlFiles) {
                try {
                    optimizeHtml(htmlFile, config)
                } catch (e: Exception) {
                    errors.add("HTML 优化失败: ${htmlFile.name}: ${e.message}")
                }
            }
        }
        
        // 统计最终大小
        optimizedSize = allFiles.filter { it.exists() }.sumOf { it.length() }
        
        onProgress("优化完成", 1f)
        
        OptimizeStats(
            originalSize = originalSize,
            optimizedSize = optimizedSize,
            imagesCompressed = imagesCompressed,
            codeFilesMinified = codeMinified,
            resourcesRemoved = resourcesRemoved,
            optimizationTimeMs = System.currentTimeMillis() - startTime,
            errors = errors
        )
    }
    
    /**
     * 在 APK 打包前优化嵌入的资源文件
     * 调用点：ApkBuilder.modifyApk → 写入 assets 前
     */
    suspend fun optimizeFileForApk(
        context: Context,
        file: File,
        config: OptimizeConfig = OptimizeConfig()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (file.extension.lowercase()) {
                "png", "jpg", "jpeg" -> {
                    if (config.compressImages && file.length() > IMAGE_COMPRESS_THRESHOLD) {
                        compressImage(file, config) > 0
                    } else false
                }
                "js" -> {
                    if (config.minifyCode && file.length() > CODE_MINIFY_THRESHOLD && !file.name.endsWith(".min.js")) {
                        val esbuildAvailable = NativeNodeEngine.isAvailable(context)
                        if (esbuildAvailable) minifyWithEsbuild(context, file)
                        else minifyJsFallback(file)
                    } else false
                }
                "css" -> {
                    if (config.minifyCode && file.length() > CODE_MINIFY_THRESHOLD && !file.name.endsWith(".min.css")) {
                        val esbuildAvailable = NativeNodeEngine.isAvailable(context)
                        if (esbuildAvailable) minifyWithEsbuild(context, file)
                        else minifyCssFallback(file)
                    } else false
                }
                "svg" -> {
                    if (config.minifySvg) minifySvg(file) else false
                }
                else -> false
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "单文件优化失败: ${file.name}", e)
            false
        }
    }
    
    /**
     * 优化 APK 打包时的 ZIP 流数据
     * 返回优化后的字节数组（用于直接写入 ZipOutputStream）
     */
    suspend fun optimizeBytesForApk(
        context: Context,
        fileName: String,
        data: ByteArray,
        config: OptimizeConfig = OptimizeConfig()
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            when (ext) {
                "png", "jpg", "jpeg" -> {
                    if (config.compressImages && data.size > IMAGE_COMPRESS_THRESHOLD) {
                        compressImageBytes(data, ext, config) ?: data
                    } else data
                }
                "js" -> {
                    if (config.minifyCode && data.size > CODE_MINIFY_THRESHOLD && !fileName.endsWith(".min.js")) {
                        minifyCodeBytes(context, data, "js", config) ?: data
                    } else data
                }
                "css" -> {
                    if (config.minifyCode && data.size > CODE_MINIFY_THRESHOLD && !fileName.endsWith(".min.css")) {
                        minifyCodeBytes(context, data, "css", config) ?: data
                    } else data
                }
                "svg" -> {
                    if (config.minifySvg) {
                        minifySvgBytes(data) ?: data
                    } else data
                }
                else -> data
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "字节流优化失败: $fileName", e)
            data
        }
    }
    
    // ==================== 二、构建速度优化 ====================
    
    /**
     * 获取优化缓存目录
     */
    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
    }
    
    /**
     * 检查文件是否已被缓存优化
     */
    fun isCached(context: Context, file: File): Boolean {
        val cacheFile = getCachedFile(context, file)
        return cacheFile.exists() && cacheFile.lastModified() >= file.lastModified()
    }
    
    /**
     * 获取缓存文件路径
     */
    private fun getCachedFile(context: Context, file: File): File {
        val hash = "${file.absolutePath.hashCode()}_${file.length()}_${file.lastModified()}"
        return File(getCacheDir(context), hash)
    }
    
    /**
     * 清理优化缓存
     */
    fun clearCache(context: Context): Long {
        val cacheDir = getCacheDir(context)
        val size = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        return size
    }
    
    /**
     * 判断模板 APK 中哪些文件可以安全移除以减小体积
     */
    fun getRemovableEntries(entryName: String, appType: String): Boolean {
        // 不需要的语言资源（只保留基础语言）
        if (entryName.startsWith("res/") && entryName.contains("-")) {
            val dirName = entryName.substringAfter("res/").substringBefore("/")
            // 保留默认和常见的资源目录
            if (dirName.startsWith("values-") && !dirName.startsWith("values-zh") && 
                !dirName.startsWith("values-en") && !dirName.startsWith("values-ar")) {
                return true
            }
        }
        
        // 不使用的 assets 模板
        if (entryName.startsWith("assets/template/") && appType != "FRONTEND") {
            return true
        }
        
        // 不使用的 BGM 默认文件
        if (entryName.startsWith("assets/bgm/default") && appType != "WEB") {
            return true
        }
        
        return false
    }
    
    // ==================== 三、加载速度优化 ====================
    
    /**
     * 生成 WebView 性能优化注入脚本
     * 在 WebView 加载页面时注入，提升页面加载和渲染性能
     */
    fun generatePerformanceScript(): String {
        return """
(function() {
    'use strict';
    if (window.__wta_perf_injected) return;
    window.__wta_perf_injected = true;
    
    // 1. 图片懒加载 (Intersection Observer)
    if ('IntersectionObserver' in window) {
        var lazyObserver = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    var img = entry.target;
                    if (img.dataset.src) {
                        img.src = img.dataset.src;
                        img.removeAttribute('data-src');
                    }
                    if (img.dataset.srcset) {
                        img.srcset = img.dataset.srcset;
                        img.removeAttribute('data-srcset');
                    }
                    img.classList.remove('wta-lazy');
                    lazyObserver.unobserve(img);
                }
            });
        }, { rootMargin: '200px 0px' });
        
        document.querySelectorAll('img[data-src], img[loading="lazy"]').forEach(function(img) {
            lazyObserver.observe(img);
        });
    }
    
    // 2. 被动事件监听器（滚动性能优化）
    var origAdd = EventTarget.prototype.addEventListener;
    var passiveEvents = { touchstart: 1, touchmove: 1, wheel: 1, mousewheel: 1, scroll: 1 };
    EventTarget.prototype.addEventListener = function(type, fn, opts) {
        if (passiveEvents[type] && opts !== false) {
            var newOpts = typeof opts === 'object' ? opts : {};
            if (!('passive' in newOpts)) newOpts.passive = true;
            return origAdd.call(this, type, fn, newOpts);
        }
        return origAdd.call(this, type, fn, opts);
    };
    
    // 3. 预连接常见 CDN
    var cdnDomains = [];
    document.querySelectorAll('script[src], link[href]').forEach(function(el) {
        try {
            var url = new URL(el.src || el.href, location.href);
            if (url.origin !== location.origin && cdnDomains.indexOf(url.origin) === -1) {
                cdnDomains.push(url.origin);
            }
        } catch(e) { /* URL parse failed */ }
    });
    cdnDomains.slice(0, 5).forEach(function(origin) {
        if (!document.querySelector('link[rel="preconnect"][href="' + origin + '"]')) {
            var link = document.createElement('link');
            link.rel = 'preconnect';
            link.href = origin;
            link.crossOrigin = '';
            document.head.appendChild(link);
        }
    });
    
    // 4. 长任务监控 + 自动让步
    if ('PerformanceObserver' in window) {
        try {
            var longTaskObserver = new PerformanceObserver(function(list) {
                list.getEntries().forEach(function(entry) {
                    if (entry.duration > 100) {
                        console.warn('[WTA Perf] Long task detected:', entry.duration.toFixed(0) + 'ms');
                    }
                });
            });
            longTaskObserver.observe({ entryTypes: ['longtask'] });
        } catch(e) { /* PerformanceObserver not supported */ }
    }
    
    // 5. 资源加载优先级优化
    document.querySelectorAll('img').forEach(function(img) {
        if (img.getBoundingClientRect().top > window.innerHeight * 1.5) {
            img.loading = 'lazy';
            img.decoding = 'async';
        } else {
            img.decoding = 'async';
        }
    });
    
    // 6. CSS 动画性能优化（will-change 提示）
    var style = document.createElement('style');
    style.textContent = '.wta-animate{will-change:transform,opacity}.wta-gpu{transform:translateZ(0)}';
    document.head.appendChild(style);
    
    // 7. 内存优化：页面隐藏时释放非关键资源
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            // 暂停视频
            document.querySelectorAll('video').forEach(function(v) { 
                if (!v.paused) { v.dataset.wtaWasPlaying = '1'; v.pause(); }
            });
        } else {
            // 恢复视频
            document.querySelectorAll('video[data-wta-was-playing]').forEach(function(v) {
                v.play().catch(function(){ /* autoplay blocked */ });
                delete v.dataset.wtaWasPlaying;
            });
        }
    });
    
    // 8. 首次内容绘制加速：关键 CSS 阻塞检测
    if (window.performance && performance.getEntriesByType) {
        var cssEntries = performance.getEntriesByType('resource').filter(function(r) {
            return r.initiatorType === 'link' && r.name.endsWith('.css');
        });
        if (cssEntries.length > 3) {
            console.info('[WTA Perf] ' + cssEntries.length + ' CSS files detected. Consider inlining critical CSS.');
        }
    }
})();
""".trimIndent()
    }
    
    /**
     * 生成 DNS 预解析和资源预加载的 HTML 头部注入
     */
    fun generatePreloadHead(htmlContent: String): String {
        val hints = StringBuilder()
        
        // 提取外部域名
        val urlRegex = Regex("""(https?://[^/"'\s>]+)""")
        val domains = urlRegex.findAll(htmlContent)
            .map { it.groupValues[1] }
            .distinct()
            .take(8)
        
        for (domain in domains) {
            hints.appendLine("""<link rel="dns-prefetch" href="$domain">""")
            hints.appendLine("""<link rel="preconnect" href="$domain" crossorigin>""")
        }
        
        // 预加载关键字体
        val fontRegex = Regex("""url\(['"]?([^'")\s]+\.woff2?)['"]?\)""")
        val fonts = fontRegex.findAll(htmlContent)
            .map { it.groupValues[1] }
            .distinct()
            .take(3)
        
        for (font in fonts) {
            hints.appendLine("""<link rel="preload" href="$font" as="font" type="font/woff2" crossorigin>""")
        }
        
        return hints.toString()
    }
    
    /**
     * 优化 HTML 文件：注入性能优化标签
     */
    private fun optimizeHtml(htmlFile: File, config: OptimizeConfig): Boolean {
        val content = htmlFile.readText()
        var modified = content
        
        // 1. 注入 DNS 预解析
        if (config.injectDnsPrefetch) {
            val preloadHead = generatePreloadHead(content)
            if (preloadHead.isNotBlank()) {
                val headIdx = modified.indexOf("<head>", ignoreCase = true)
                if (headIdx >= 0) {
                    modified = modified.substring(0, headIdx + 6) + "\n" + preloadHead + modified.substring(headIdx + 6)
                }
            }
        }
        
        // 2. 图片懒加载属性
        if (config.injectLazyLoading) {
            modified = injectLazyLoading(modified)
        }
        
        // 3. Script defer/async
        if (config.optimizeScripts) {
            modified = optimizeScriptTags(modified)
        }
        
        // 4. 注入 viewport 优化
        if (!modified.contains("viewport", ignoreCase = true)) {
            val headIdx = modified.indexOf("<head>", ignoreCase = true)
            if (headIdx >= 0) {
                modified = modified.substring(0, headIdx + 6) + 
                    "\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                    modified.substring(headIdx + 6)
            }
        }
        
        if (modified != content) {
            htmlFile.writeText(modified)
            return true
        }
        return false
    }
    
    /**
     * 注入图片懒加载
     */
    private fun injectLazyLoading(html: String): String {
        // 给非首屏图片添加 loading="lazy"
        val imgRegex = Regex("""<img([^>]*?)(/?>)""", RegexOption.IGNORE_CASE)
        var count = 0
        return imgRegex.replace(html) { match ->
            count++
            val attrs = match.groupValues[1]
            val close = match.groupValues[2]
            // 前 2 张图片不懒加载（可能在首屏）
            if (count > 2 && !attrs.contains("loading=", ignoreCase = true)) {
                """<img$attrs loading="lazy" decoding="async"$close"""
            } else if (!attrs.contains("decoding=", ignoreCase = true)) {
                """<img$attrs decoding="async"$close"""
            } else {
                match.value
            }
        }
    }
    
    /**
     * 优化 script 标签：添加 defer 属性
     */
    private fun optimizeScriptTags(html: String): String {
        val scriptRegex = Regex("""<script([^>]*)\ssrc=["']([^"']+)["']([^>]*)>""", RegexOption.IGNORE_CASE)
        return scriptRegex.replace(html) { match ->
            val before = match.groupValues[1]
            val src = match.groupValues[2]
            val after = match.groupValues[3]
            val fullAttrs = before + after
            // 不修改已有 defer/async/type=module 的 script
            if (fullAttrs.contains("defer", ignoreCase = true) ||
                fullAttrs.contains("async", ignoreCase = true) ||
                fullAttrs.contains("type=\"module\"", ignoreCase = true) ||
                fullAttrs.contains("type='module'", ignoreCase = true)) {
                match.value
            } else {
                """<script${before} src="$src"$after defer>"""
            }
        }
    }
    
    // ==================== 私有优化方法 ====================
    
    /**
     * 压缩图片文件（就地替换）
     * @return 节省的字节数
     */
    private fun compressImage(file: File, config: OptimizeConfig): Long {
        val originalSize = file.length()
        
        // 解码图片
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            // 大图片降采样
            if (originalSize > 1024 * 1024) inSampleSize = 2
            if (originalSize > 5 * 1024 * 1024) inSampleSize = 4
        }
        
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: return 0
        
        try {
            val tempFile = File(file.parent, "${file.nameWithoutExtension}_opt.${file.extension}")
            
            // 选择输出格式
            var format: Bitmap.CompressFormat
            var quality: Int
            
            if (config.convertToWebP && file.extension.lowercase() in listOf("png", "jpg", "jpeg")) {
                format = Bitmap.CompressFormat.WEBP
                quality = config.imageQuality
                // WebP 输出文件
                val webpFile = File(file.parent, "${file.nameWithoutExtension}.webp")
                FileOutputStream(webpFile).use { out ->
                    bitmap.compress(format, quality, out)
                }
                // 如果 WebP 更小，替换原文件
                if (webpFile.length() < originalSize) {
                    file.delete()
                    // 保留 WebP 文件即可，HTML 引用更新由 HtmlProjectOptimizer 处理
                    return originalSize - webpFile.length()
                } else {
                    webpFile.delete()
                }
            }
            
            // 常规压缩（不转 WebP 或 WebP 不够小时的 fallback）
            format = when (file.extension.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG
            }
            quality = config.imageQuality
            
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(format, quality, out)
            }
            
            if (tempFile.length() < originalSize) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
                return originalSize - file.length()
            } else {
                tempFile.delete()
                return 0
            }
        } finally {
            bitmap.recycle()
        }
    }
    
    /**
     * 压缩图片字节数据
     */
    private fun compressImageBytes(data: ByteArray, ext: String, config: OptimizeConfig): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return null
        try {
            val format = when {
                config.convertToWebP -> Bitmap.CompressFormat.WEBP
                ext == "png" -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG
            }
            val baos = ByteArrayOutputStream()
            bitmap.compress(format, config.imageQuality, baos)
            val result = baos.toByteArray()
            return if (result.size < data.size) result else null
        } finally {
            bitmap.recycle()
        }
    }
    
    /**
     * 使用 esbuild 压缩代码文件
     */
    private suspend fun minifyWithEsbuild(context: Context, file: File): Boolean {
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
            if (result.exitCode == 0 && tempOutput.exists() && tempOutput.length() > 0 && tempOutput.length() < file.length()) {
                tempOutput.copyTo(file, overwrite = true)
                tempOutput.delete()
                return true
            } else {
                tempOutput.delete()
                return false
            }
        } catch (e: Exception) {
            tempOutput.delete()
            return false
        }
    }
    
    /**
     * 压缩代码字节数据（通过临时文件调用 esbuild）
     */
    private suspend fun minifyCodeBytes(
        context: Context,
        data: ByteArray,
        type: String,
        config: OptimizeConfig
    ): ByteArray? {
        val esbuildAvailable = NativeNodeEngine.isAvailable(context)
        val content = String(data, Charsets.UTF_8)
        
        if (esbuildAvailable) {
            val tempDir = getCacheDir(context)
            val tempFile = File(tempDir, "temp_minify_${System.nanoTime()}.$type")
            try {
                tempFile.writeText(content)
                if (minifyWithEsbuild(context, tempFile)) {
                    val result = tempFile.readBytes()
                    tempFile.delete()
                    return if (result.size < data.size) result else null
                }
                tempFile.delete()
            } catch (e: Exception) {
                tempFile.delete()
            }
        }
        
        // Kotlin fallback
        val minified = when (type) {
            "js" -> minifyJsContent(content)
            "css" -> minifyCssContent(content)
            else -> null
        }
        return minified?.let { 
            val bytes = it.toByteArray(Charsets.UTF_8)
            if (bytes.size < data.size) bytes else null
        }
    }
    
    /**
     * JS 压缩 fallback（纯 Kotlin）
     */
    private fun minifyJsFallback(file: File): Boolean {
        val content = file.readText()
        val minified = minifyJsContent(content) ?: return false
        if (minified.length < content.length) {
            file.writeText(minified)
            return true
        }
        return false
    }
    
    /**
     * CSS 压缩 fallback（纯 Kotlin）
     */
    private fun minifyCssFallback(file: File): Boolean {
        val content = file.readText()
        val minified = minifyCssContent(content) ?: return false
        if (minified.length < content.length) {
            file.writeText(minified)
            return true
        }
        return false
    }
    
    /**
     * JS 内容压缩（移除注释、多余空白）
     */
    private fun minifyJsContent(content: String): String? {
        if (content.length < 100) return null
        var result = content
        // 移除单行注释（注意不要移除 URL 中的 //）
        result = result.replace(Regex("""(?<![:"'])//[^\n]*"""), "")
        // 移除多行注释
        result = result.replace(Regex("""/\*[\s\S]*?\*/"""), "")
        // 压缩空白
        result = result.replace(Regex("""\s+"""), " ")
        // 移除操作符周围的空格
        result = result.replace(Regex("""\s*([{}();,:])\s*"""), "$1")
        return result.trim()
    }
    
    /**
     * CSS 内容压缩
     */
    private fun minifyCssContent(content: String): String? {
        if (content.length < 100) return null
        var result = content
        // 移除注释
        result = result.replace(Regex("""/\*[\s\S]*?\*/"""), "")
        // 压缩空白
        result = result.replace(Regex("""\s+"""), " ")
        // 移除操作符周围的空格
        result = result.replace(Regex("""\s*([{}:;,])\s*"""), "$1")
        // 移除最后一个分号
        result = result.replace(Regex(""";}"""), "}")
        return result.trim()
    }
    
    /**
     * SVG 精简
     */
    private fun minifySvg(file: File): Boolean {
        val content = file.readText()
        val minified = minifySvgContent(content) ?: return false
        if (minified.length < content.length) {
            file.writeText(minified)
            return true
        }
        return false
    }
    
    private fun minifySvgBytes(data: ByteArray): ByteArray? {
        val content = String(data, Charsets.UTF_8)
        val minified = minifySvgContent(content) ?: return null
        val result = minified.toByteArray(Charsets.UTF_8)
        return if (result.size < data.size) result else null
    }
    
    private fun minifySvgContent(content: String): String? {
        if (content.length < 100) return null
        var result = content
        // 移除 XML 注释
        result = result.replace(Regex("""<!--[\s\S]*?-->"""), "")
        // 移除多余空白
        result = result.replace(Regex("""\s+"""), " ")
        // 移除标签间空白
        result = result.replace(Regex(""">\s+<"""), "><")
        // 移除 metadata
        result = result.replace(Regex("""<metadata[\s\S]*?</metadata>""", RegexOption.IGNORE_CASE), "")
        return result.trim()
    }
}
