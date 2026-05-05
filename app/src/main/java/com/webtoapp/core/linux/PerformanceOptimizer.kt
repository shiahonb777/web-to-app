package com.webtoapp.core.linux

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*






























object PerformanceOptimizer {

    private const val TAG = "PerformanceOptimizer"


    private const val CACHE_DIR = "perf_optimize_cache"


    private const val IMAGE_COMPRESS_THRESHOLD = 10 * 1024L


    private const val CODE_MINIFY_THRESHOLD = 512L




    data class OptimizeConfig(

        val compressImages: Boolean = true,
        val imageQuality: Int = 80,
        val convertToWebP: Boolean = true,
        val minifyCode: Boolean = true,
        val minifySvg: Boolean = true,
        val removeUnusedResources: Boolean = true,


        val parallelProcessing: Boolean = true,
        val enableCache: Boolean = true,


        val injectPreloadHints: Boolean = true,
        val injectLazyLoading: Boolean = true,
        val optimizeScripts: Boolean = true,
        val injectDnsPrefetch: Boolean = true,


        val injectPerformanceScript: Boolean = true
    )




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

        onProgress(Strings.perfScanFiles, 0f)

        val esbuildAvailable = NativeNodeEngine.isAvailable(context)


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


        if (config.compressImages && imageFiles.isNotEmpty()) {
            onProgress("${Strings.perfCompressImages} (${imageFiles.size})...", 0.1f)

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
                    onProgress(Strings.perfCompressImage.format(file.name), processedCount.toFloat() / totalFiles * 0.4f)
                }
            }
        }


        if (config.minifyCode) {
            onProgress(Strings.perfCompressCode, 0.4f)

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
                onProgress(Strings.perfCompressJs.format(jsFile.name), 0.4f + processedCount.toFloat() / totalFiles * 0.2f)
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
                onProgress(Strings.perfCompressCss.format(cssFile.name), 0.6f + processedCount.toFloat() / totalFiles * 0.1f)
            }
        }


        if (config.minifySvg && svgFiles.isNotEmpty()) {
            onProgress(Strings.perfOptimizeSvg, 0.7f)
            for (svgFile in svgFiles) {
                try {
                    if (minifySvg(svgFile)) codeMinified++
                } catch (e: Exception) {
                    errors.add("SVG 优化失败: ${svgFile.name}: ${e.message}")
                }
            }
        }


        if (htmlFiles.isNotEmpty()) {
            onProgress(Strings.perfOptimizeHtml, 0.8f)
            for (htmlFile in htmlFiles) {
                try {
                    optimizeHtml(htmlFile, config)
                } catch (e: Exception) {
                    errors.add("HTML 优化失败: ${htmlFile.name}: ${e.message}")
                }
            }
        }


        optimizedSize = allFiles.filter { it.exists() }.sumOf { it.length() }

        onProgress(Strings.perfOptimizeComplete, 1f)

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






    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
    }




    fun isCached(context: Context, file: File): Boolean {
        val cacheFile = getCachedFile(context, file)
        return cacheFile.exists() && cacheFile.lastModified() >= file.lastModified()
    }




    private fun getCachedFile(context: Context, file: File): File {
        val hash = "${file.absolutePath.hashCode()}_${file.length()}_${file.lastModified()}"
        return File(getCacheDir(context), hash)
    }




    fun clearCache(context: Context): Long {
        val cacheDir = getCacheDir(context)
        val size = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        return size
    }




    fun getRemovableEntries(entryName: String, appType: String): Boolean {

        if (entryName.startsWith("res/") && entryName.contains("-")) {
            val dirName = entryName.substringAfter("res/").substringBefore("/")

            if (dirName.startsWith("values-") && !dirName.startsWith("values-zh") &&
                !dirName.startsWith("values-en") && !dirName.startsWith("values-ar")) {
                return true
            }
        }


        if (entryName.startsWith("assets/template/") && appType != "FRONTEND") {
            return true
        }


        if (entryName.startsWith("assets/bgm/default") && appType != "WEB") {
            return true
        }

        return false
    }







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




    fun generatePreloadHead(htmlContent: String): String {
        val hints = StringBuilder()


        val urlRegex = Regex("""(https?://[^/"'\s>]+)""")
        val domains = urlRegex.findAll(htmlContent)
            .map { it.groupValues[1] }
            .distinct()
            .take(8)

        for (domain in domains) {
            hints.appendLine("""<link rel="dns-prefetch" href="$domain">""")
            hints.appendLine("""<link rel="preconnect" href="$domain" crossorigin>""")
        }


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




    private fun optimizeHtml(htmlFile: File, config: OptimizeConfig): Boolean {
        val content = htmlFile.readText()
        var modified = content


        if (config.injectDnsPrefetch) {
            val preloadHead = generatePreloadHead(content)
            if (preloadHead.isNotBlank()) {
                val headIdx = modified.indexOf("<head>", ignoreCase = true)
                if (headIdx >= 0) {
                    modified = modified.substring(0, headIdx + 6) + "\n" + preloadHead + modified.substring(headIdx + 6)
                }
            }
        }


        if (config.injectLazyLoading) {
            modified = injectLazyLoading(modified)
        }


        if (config.optimizeScripts) {
            modified = optimizeScriptTags(modified)
        }


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




    private fun injectLazyLoading(html: String): String {

        val imgRegex = Regex("""<img([^>]*?)(/?>)""", RegexOption.IGNORE_CASE)
        var count = 0
        return imgRegex.replace(html) { match ->
            count++
            val attrs = match.groupValues[1]
            val close = match.groupValues[2]

            if (count > 2 && !attrs.contains("loading=", ignoreCase = true)) {
                """<img$attrs loading="lazy" decoding="async"$close"""
            } else if (!attrs.contains("decoding=", ignoreCase = true)) {
                """<img$attrs decoding="async"$close"""
            } else {
                match.value
            }
        }
    }




    private fun optimizeScriptTags(html: String): String {
        val scriptRegex = Regex("""<script([^>]*)\ssrc=["']([^"']+)["']([^>]*)>""", RegexOption.IGNORE_CASE)
        return scriptRegex.replace(html) { match ->
            val before = match.groupValues[1]
            val src = match.groupValues[2]
            val after = match.groupValues[3]
            val fullAttrs = before + after

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







    private fun compressImage(file: File, config: OptimizeConfig): Long {
        val originalSize = file.length()


        val options = BitmapFactory.Options().apply {
            inSampleSize = 1

            if (originalSize > 1024 * 1024) inSampleSize = 2
            if (originalSize > 5 * 1024 * 1024) inSampleSize = 4
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: return 0

        try {
            val tempFile = File(file.parent, "${file.nameWithoutExtension}_opt.${file.extension}")


            var format: Bitmap.CompressFormat
            var quality: Int

            if (config.convertToWebP && file.extension.lowercase() in listOf("png", "jpg", "jpeg")) {
                format = Bitmap.CompressFormat.WEBP
                quality = config.imageQuality

                val webpFile = File(file.parent, "${file.nameWithoutExtension}.webp")
                FileOutputStream(webpFile).use { out ->
                    bitmap.compress(format, quality, out)
                }

                if (webpFile.length() < originalSize) {
                    file.delete()

                    return originalSize - webpFile.length()
                } else {
                    webpFile.delete()
                }
            }


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




    private fun minifyJsFallback(file: File): Boolean {
        val content = file.readText()
        val minified = minifyJsContent(content) ?: return false
        if (minified.length < content.length) {
            file.writeText(minified)
            return true
        }
        return false
    }




    private fun minifyCssFallback(file: File): Boolean {
        val content = file.readText()
        val minified = minifyCssContent(content) ?: return false
        if (minified.length < content.length) {
            file.writeText(minified)
            return true
        }
        return false
    }




    private fun minifyJsContent(content: String): String? {
        if (content.length < 100) return null
        var result = content

        result = result.replace(Regex("""(?<![:"'])//[^\n]*"""), "")

        result = result.replace(Regex("""/\*[\s\S]*?\*/"""), "")

        result = result.replace(Regex("""\s+"""), " ")

        result = result.replace(Regex("""\s*([{}();,:])\s*"""), "$1")
        return result.trim()
    }




    private fun minifyCssContent(content: String): String? {
        if (content.length < 100) return null
        var result = content

        result = result.replace(Regex("""/\*[\s\S]*?\*/"""), "")

        result = result.replace(Regex("""\s+"""), " ")

        result = result.replace(Regex("""\s*([{}:;,])\s*"""), "$1")

        result = result.replace(Regex(""";}"""), "}")
        return result.trim()
    }




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

        result = result.replace(Regex("""<!--[\s\S]*?-->"""), "")

        result = result.replace(Regex("""\s+"""), " ")

        result = result.replace(Regex(""">\s+<"""), "><")

        result = result.replace(Regex("""<metadata[\s\S]*?</metadata>""", RegexOption.IGNORE_CASE), "")
        return result.trim()
    }
}
