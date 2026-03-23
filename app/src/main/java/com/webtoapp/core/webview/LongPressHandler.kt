package com.webtoapp.core.webview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.DownloadHelper
import com.webtoapp.util.MediaSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * WebView 长按处理器
 * 处理长按图片、视频、链接等元素的交互
 */
class LongPressHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    /**
     * 长按结果类型
     */
    sealed class LongPressResult {
        data class Image(val url: String) : LongPressResult()
        data class Video(val url: String) : LongPressResult()
        data class Link(val url: String, val title: String? = null) : LongPressResult()
        data class ImageLink(val imageUrl: String, val linkUrl: String) : LongPressResult()
        object Text : LongPressResult()
        object None : LongPressResult()
    }
    
    /**
     * 注入JS代码绕过网站的长按事件阻止
     * 适用于小红书等阻止长按保存图片的网站
     */
    fun injectLongPressEnhancer(webView: WebView) {
        val js = """
            (function() {
                if (window.__wtaLongPressEnhanced) return;
                window.__wtaLongPressEnhanced = true;
                
                console.log('[WebToApp] 注入长按增强脚本');
                
                // 只阻止 contextmenu 事件，不干扰 touch 事件（以免影响网页的点击事件处理）
                // 这样可以允许长按保存图片，同时不影响点击功能（如小说阅读器的中央点击设置）
                document.addEventListener('contextmenu', function(e) {
                    // Check是否是图片相关元素
                    var target = e.target;
                    var isImageRelated = false;
                    var current = target;
                    var depth = 0;
                    
                    while (current && depth < 10) {
                        var tagName = current.tagName ? current.tagName.toUpperCase() : '';
                        if (tagName === 'IMG' || tagName === 'CANVAS') {
                            isImageRelated = true;
                            break;
                        }
                        // Check背景图片
                        var style = window.getComputedStyle(current);
                        if (style.backgroundImage && style.backgroundImage !== 'none' && 
                            style.backgroundImage.includes('url(')) {
                            isImageRelated = true;
                            break;
                        }
                        // Check小红书特殊容器
                        if (current.className && (
                            current.className.includes('note-image') ||
                            current.className.includes('swiper') ||
                            current.className.includes('carousel') ||
                            current.className.includes('image')
                        )) {
                            isImageRelated = true;
                            break;
                        }
                        current = current.parentElement;
                        depth++;
                    }
                    
                    if (isImageRelated) {
                        e.stopPropagation();
                        e.preventDefault();
                    }
                }, true);
                
                // 移除元素上的长按阻止属性（仅针对图片元素）
                // 注意：不移除 touch 事件处理器，以免影响网页的点击功能
                function removeEventBlockers() {
                    var elements = document.querySelectorAll('img, canvas');
                    elements.forEach(function(el) {
                        // 只允许图片元素的长按操作
                        el.style.webkitTouchCallout = 'default';
                        el.removeAttribute('oncontextmenu');
                        // 不移除 touch 事件，以免影响网页的点击事件处理
                    });
                }
                
                removeEventBlockers();
                
                // ListenDOM变化持续移除
                var observer = new MutationObserver(function() {
                    removeEventBlockers();
                });
                observer.observe(document.body, { childList: true, subtree: true });
                
                console.log('[WebToApp] 长按增强脚本注入完成');
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js, null)
    }
    
    /**
     * 解析 WebView 长按结果
     */
    fun parseLongPressResult(webView: WebView): LongPressResult {
        val hitResult = webView.hitTestResult
        val type = hitResult.type
        val extra = hitResult.extra ?: return LongPressResult.None
        
        return when (type) {
            WebView.HitTestResult.IMAGE_TYPE -> {
                LongPressResult.Image(extra)
            }
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                // Image链接：需要通过 JS 获取链接地址
                LongPressResult.ImageLink(imageUrl = extra, linkUrl = "")
            }
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                // 可能是视频或普通链接
                if (isVideoUrl(extra)) {
                    LongPressResult.Video(extra)
                } else {
                    LongPressResult.Link(extra)
                }
            }
            WebView.HitTestResult.ANCHOR_TYPE -> {
                LongPressResult.Link(extra)
            }
            WebView.HitTestResult.EDIT_TEXT_TYPE -> {
                LongPressResult.Text
            }
            else -> LongPressResult.None
        }
    }
    
    /**
     * 通过 JS 获取更详细的长按信息
     * 
     * 注意：x, y 是相对于 WebView 视图的坐标（来自 MotionEvent）
     * 需要转换为页面视口坐标才能正确使用 document.elementFromPoint()
     * 
     * 增强功能：
     * - 支持检测 background-image 渲染的图片
     * - 支持检测 canvas 渲染的图片（转为 dataURL）
     * - 支持小红书等网站的特殊图片容器
     */
    fun getLongPressDetails(webView: WebView, x: Float, y: Float, callback: (LongPressResult) -> Unit) {
        // Get WebView 的缩放比例
        val scale = webView.scale
        
        // 将视图坐标转换为页面坐标
        // MotionEvent 的坐标是相对于 WebView 视图的像素坐标
        // document.elementFromPoint() 需要的是 CSS 像素坐标（考虑缩放）
        val pageX = x / scale
        val pageY = y / scale
        
        val js = """
            (function() {
                try {
                    // 使用传入的坐标（已经是 CSS 像素）
                    var x = $pageX;
                    var y = $pageY;
                    
                    var elem = document.elementFromPoint(x, y);
                    if (!elem) {
                        console.log('WebToApp: No element at (' + x + ', ' + y + ')');
                        return JSON.stringify({type: 'none'});
                    }
                    
                    console.log('WebToApp: Element at (' + x + ', ' + y + '): ' + elem.tagName + ', class=' + elem.className);
                    
                    var result = {type: 'none'};
                    
                    // 辅助函数：提取背景图片URL
                    function extractBgImageUrl(element) {
                        var style = window.getComputedStyle(element);
                        var bgImage = style.backgroundImage;
                        if (bgImage && bgImage !== 'none') {
                            // Support多个 url() 的情况
                            var matches = bgImage.match(/url\(['"]?([^'")\s]+)['"]?\)/g);
                            if (matches && matches.length > 0) {
                                var url = matches[0].replace(/url\(['"]?/, '').replace(/['"]?\)/, '');
                                if (url && !url.startsWith('data:image/svg') && !url.includes('gradient')) {
                                    return url;
                                }
                            }
                        }
                        return null;
                    }
                    
                    // 辅助函数：检测小红书等网站的特殊图片容器
                    function findXhsImage(element) {
                        // 小红书图片通常在特定的容器中
                        var selectors = [
                            'img[src*="xhscdn"]',
                            'img[src*="xiaohongshu"]',
                            '[class*="note-image"] img',
                            '[class*="swiper-slide"] img',
                            '[class*="carousel"] img',
                            '[class*="image-container"] img',
                            '[class*="media"] img',
                            '[data-v-] img'  // Vue 组件中的图片
                        ];
                        
                        // 先检查当前元素及其子元素
                        for (var i = 0; i < selectors.length; i++) {
                            var img = element.querySelector(selectors[i]);
                            if (img && img.src) return img.src;
                        }
                        
                        // 向上查找父容器中的图片
                        var parent = element;
                        var depth = 0;
                        while (parent && depth < 5) {
                            for (var i = 0; i < selectors.length; i++) {
                                var img = parent.querySelector(selectors[i]);
                                if (img && img.src) return img.src;
                            }
                            parent = parent.parentElement;
                            depth++;
                        }
                        
                        return null;
                    }
                    
                    // 辅助函数：从 canvas 提取图片
                    function extractCanvasImage(canvas) {
                        try {
                            if (canvas.width > 10 && canvas.height > 10) {
                                return canvas.toDataURL('image/png');
                            }
                        } catch (e) {
                            console.log('WebToApp: Canvas extraction failed (CORS)', e);
                        }
                        return null;
                    }
                    
                    // 向上遍历查找可交互元素（最多 15 层，增加深度以适应复杂DOM）
                    var current = elem;
                    var depth = 0;
                    var foundBgImage = null;
                    
                    while (current && depth < 15) {
                        var tagName = current.tagName ? current.tagName.toUpperCase() : '';
                        
                        // Check是否是图片
                        if (tagName === 'IMG' && current.src) {
                            result = {
                                type: 'image',
                                url: current.src,
                                alt: current.alt || ''
                            };
                            // Resume向上查找是否在链接内
                            var parent = current.parentElement;
                            while (parent) {
                                if (parent.tagName && parent.tagName.toUpperCase() === 'A' && parent.href) {
                                    result.type = 'imageLink';
                                    result.linkUrl = parent.href;
                                    break;
                                }
                                parent = parent.parentElement;
                            }
                            break;
                        }
                        
                        // Check canvas 元素
                        if (tagName === 'CANVAS') {
                            var canvasUrl = extractCanvasImage(current);
                            if (canvasUrl) {
                                result = {type: 'image', url: canvasUrl, alt: 'canvas'};
                                break;
                            }
                        }
                        
                        // Check是否是视频
                        if (tagName === 'VIDEO') {
                            var videoUrl = current.src || current.currentSrc || '';
                            if (!videoUrl) {
                                var source = current.querySelector('source');
                                if (source) videoUrl = source.src;
                            }
                            if (videoUrl) {
                                result = {type: 'video', url: videoUrl};
                                break;
                            }
                        }
                        
                        // Check是否是链接
                        if (tagName === 'A' && current.href) {
                            var href = current.href;
                            if (href.match(/\.(mp4|webm|ogg|mov|avi|mkv|m3u8)(\?|#|$)/i)) {
                                result = {type: 'video', url: href};
                            } else {
                                result = {
                                    type: 'link', 
                                    url: href, 
                                    title: (current.textContent || '').trim().substring(0, 100)
                                };
                            }
                            break;
                        }
                        
                        // Check背景图片（保存第一个找到的，但继续向上查找更好的结果）
                        if (!foundBgImage) {
                            var bgUrl = extractBgImageUrl(current);
                            if (bgUrl) {
                                foundBgImage = bgUrl;
                            }
                        }
                        
                        // Check小红书等特殊图片容器
                        if (result.type === 'none') {
                            var xhsImg = findXhsImage(current);
                            if (xhsImg) {
                                result = {type: 'image', url: xhsImg, alt: 'xhs'};
                                break;
                            }
                        }
                        
                        current = current.parentElement;
                        depth++;
                    }
                    
                    // 如果没找到标准图片，使用背景图片
                    if (result.type === 'none' && foundBgImage) {
                        result = {type: 'image', url: foundBgImage, alt: 'background'};
                    }
                    
                    console.log('WebToApp: Result type=' + result.type);
                    return JSON.stringify(result);
                } catch (e) {
                    console.error('WebToApp: Error in getLongPressDetails', e);
                    return JSON.stringify({type: 'none', error: e.message});
                }
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { jsonResult ->
            try {
                val json = jsonResult?.trim()?.removeSurrounding("\"")
                    ?.replace("\\\"", "\"")
                    ?.replace("\\\\", "\\")
                if (json.isNullOrBlank() || json == "null") {
                    callback(LongPressResult.None)
                    return@evaluateJavascript
                }
                
                val result = org.json.JSONObject(json)
                val type = result.optString("type", "none")
                
                when (type) {
                    "image" -> callback(LongPressResult.Image(result.getString("url")))
                    "video" -> callback(LongPressResult.Video(result.getString("url")))
                    "link" -> callback(LongPressResult.Link(
                        result.getString("url"),
                        result.optString("title")?.takeIf { it.isNotEmpty() }
                    ))
                    "imageLink" -> callback(LongPressResult.ImageLink(
                        result.getString("url"),
                        result.optString("linkUrl", "")
                    ))
                    else -> callback(LongPressResult.None)
                }
            } catch (e: Exception) {
                android.util.Log.e("LongPressHandler", "解析长按结果失败", e)
                callback(LongPressResult.None)
            }
        }
    }
    
    /**
     * 复制文本到剪贴板
     */
    fun copyToClipboard(text: String, label: String = "链接") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, Strings.copiedToClipboard, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 保存图片
     */
    fun saveImage(imageUrl: String, onResult: (Boolean, String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val result = when {
                    imageUrl.startsWith("data:") -> saveBase64Image(imageUrl)
                    imageUrl.startsWith("blob:") -> {
                        withContext(Dispatchers.Main) {
                            onResult(false, "Blob 图片需要通过页面下载")
                        }
                        return@launch
                    }
                    else -> saveUrlImage(imageUrl)
                }
                
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        onResult(true, "Image saved")
                    } else {
                        onResult(false, "Save failed")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LongPressHandler", "保存图片失败", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "保存失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 保存 Base64 图片
     */
    private fun saveBase64Image(dataUrl: String): Uri? {
        val parts = dataUrl.split(",")
        if (parts.size != 2) return null
        
        val meta = parts[0]
        val base64Data = parts[1]
        
        // Parse MIME 类型
        val mimeType = Regex("data:([^;]+)").find(meta)?.groupValues?.get(1) ?: "image/png"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "png"
        
        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return null
        
        return saveBitmapToGallery(bitmap, "IMG_${System.currentTimeMillis()}.$extension", mimeType)
    }
    
    /**
     * 保存 URL 图片
     */
    private fun saveUrlImage(imageUrl: String): Uri? {
        val url = URL(imageUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        val inputStream = connection.getInputStream()
        val imageBytes = inputStream.readBytes()
        inputStream.close()
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return null
        
        val fileName = DownloadHelper.parseFileName(imageUrl, null, connection.contentType)
            .ifBlank { "IMG_${System.currentTimeMillis()}.jpg" }
        val mimeType = connection.contentType ?: "image/jpeg"
        
        return saveBitmapToGallery(bitmap, fileName, mimeType)
    }
    
    /**
     * 保存 Bitmap 到相册
     */
    private fun saveBitmapToGallery(bitmap: Bitmap, fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WebToApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return null
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val format = if (mimeType.contains("png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, 95, outputStream)
            }
            
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            
            uri
        } else {
            // Android 9 及以下
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "WebToApp")
            if (!appDir.exists()) appDir.mkdirs()
            
            val file = File(appDir, fileName)
            FileOutputStream(file).use { outputStream ->
                val format = if (mimeType.contains("png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, 95, outputStream)
            }
            
            // 通知媒体库
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }
    
    /**
     * 下载视频并保存到相册
     */
    fun downloadVideo(videoUrl: String, onResult: (Boolean, String) -> Unit) {
        if (videoUrl.startsWith("blob:")) {
            onResult(false, "Blob 视频需要通过页面下载")
            return
        }
        
        scope.launch(Dispatchers.Main) {
            // 先显示提示
            Toast.makeText(context, Strings.downloadingVideo, Toast.LENGTH_SHORT).show()
            
            val fileName = DownloadHelper.parseFileName(videoUrl, null, "video/mp4")
                .ifBlank { "VIDEO_${System.currentTimeMillis()}.mp4" }
            
            val result = MediaSaver.saveFromUrl(
                context = context,
                url = videoUrl,
                fileName = fileName,
                mimeType = "video/mp4"
            )
            
            when (result) {
                is MediaSaver.SaveResult.Success -> {
                    onResult(true, "视频已保存到相册")
                }
                is MediaSaver.SaveResult.Error -> {
                    android.util.Log.e("LongPressHandler", "下载视频失败: ${result.message}")
                    onResult(false, result.message)
                }
            }
        }
    }
    
    /**
     * 判断是否为视频 URL
     */
    private fun isVideoUrl(url: String): Boolean {
        val videoExtensions = listOf(".mp4", ".webm", ".ogg", ".mov", ".avi", ".mkv", ".m3u8")
        val lowerUrl = url.lowercase()
        return videoExtensions.any { lowerUrl.contains(it) }
    }
}
