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
import android.webkit.WebView
import android.widget.Toast
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.logging.AppLogger
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
 * WebView by.
 * by etc.
 */
class LongPressHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "LongPressHandler"
        private val DATA_URI_MIME_REGEX = Regex("data:([^;]+)")
        private val VIDEO_EXTENSIONS = setOf(".mp4", ".webm", ".ogg", ".mov", ".avi", ".mkv", ".m3u8")
    }
    
    /**
     * by.
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
     * JS by.
     * use small etc by Save.
     */
    fun injectLongPressEnhancer(webView: WebView) {
        val js = """
            (function() {
                if (window.__wtaLongPressEnhanced) return;
                window.__wtaLongPressEnhanced = true;
                
                console.log('[WebToApp] 注入长按增强脚本');
                
                // contextmenu not touch.
                // can by Save when not.
                document.addEventListener('contextmenu', function(e) {
                    // Check is is.
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
                        // Check.
                        var style = window.getComputedStyle(current);
                        if (style.backgroundImage && style.backgroundImage !== 'none' && 
                            style.backgroundImage.includes('url(')) {
                            isImageRelated = true;
                            break;
                        }
                        // Check small.
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
                
                // by.
                // not touch .
                function removeEventBlockers() {
                    var elements = document.querySelectorAll('img, canvas');
                    elements.forEach(function(el) {
                        // by.
                        el.style.webkitTouchCallout = 'default';
                        el.removeAttribute('oncontextmenu');
                        // not touch .
                    });
                }
                
                removeEventBlockers();
                
                // ListenDOM.
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
     * WebView by.
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
                // Image JS Get.
                LongPressResult.ImageLink(imageUrl = extra, linkUrl = "")
            }
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                // can is or.
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
     * JS Get by.
     *
     * x, y is WebView.
     * as use document.elementFromPoint()
     *
     * .
     * - Supports background-image.
     * - Supports canvas.
     * - Supports small etc.
     */
    fun getLongPressDetails(webView: WebView, x: Float, y: Float, callback: (LongPressResult) -> Unit) {
        // Get WebView.
        val scale = webView.scale
        
        // as.
        // MotionEvent is WebView.
        // document.elementFromPoint() is CSS.
        val pageX = x / scale
        val pageY = y / scale
        
        val js = """
            (function() {
                try {
                    // use.
                    var x = $pageX;
                    var y = $pageY;
                    
                    var elem = document.elementFromPoint(x, y);
                    if (!elem) {
                        console.log('WebToApp: No element at (' + x + ', ' + y + ')');
                        return JSON.stringify({type: 'none'});
                    }
                    
                    console.log('WebToApp: Element at (' + x + ', ' + y + '): ' + elem.tagName + ', class=' + elem.className);
                    
                    var result = {type: 'none'};
                    
                    // Extract URL.
                    function extractBgImageUrl(element) {
                        var style = window.getComputedStyle(element);
                        var bgImage = style.backgroundImage;
                        if (bgImage && bgImage !== 'none') {
                            // Support multiple url()
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
                    
                    // small etc.
                    function findXhsImage(element) {
                        // small in in.
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
                        
                        // Check before and.
                        for (var i = 0; i < selectors.length; i++) {
                            var img = element.querySelector(selectors[i]);
                            if (img && img.src) return img.src;
                        }
                        
                        // in.
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
                    
                    // from canvas Extract.
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
                    
                    // can.
                    var current = elem;
                    var depth = 0;
                    var foundBgImage = null;
                    
                    while (current && depth < 15) {
                        var tagName = current.tagName ? current.tagName.toUpperCase() : '';
                        
                        // Check is is.
                        if (tagName === 'IMG' && current.src) {
                            result = {
                                type: 'image',
                                url: current.src,
                                alt: current.alt || ''
                            };
                            // Resume is in.
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
                        
                        // Check canvas.
                        if (tagName === 'CANVAS') {
                            var canvasUrl = extractCanvasImage(current);
                            if (canvasUrl) {
                                result = {type: 'image', url: canvasUrl, alt: 'canvas'};
                                break;
                            }
                        }
                        
                        // Check is is.
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
                        
                        // Check is is.
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
                        
                        // Check.
                        if (!foundBgImage) {
                            var bgUrl = extractBgImageUrl(current);
                            if (bgUrl) {
                                foundBgImage = bgUrl;
                            }
                        }
                        
                        // Check small etc.
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
                    
                    // to use.
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
                AppLogger.e(TAG, "解析长按结果失败", e)
                callback(LongPressResult.None)
            }
        }
    }
    
    /**
     * to.
     */
    fun copyToClipboard(text: String, label: String = "链接") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, AppStringsProvider.current().copiedToClipboard, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Save.
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
                AppLogger.e(TAG, "保存图片失败", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "保存失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Save Base64.
     */
    private fun saveBase64Image(dataUrl: String): Uri? {
        val parts = dataUrl.split(",")
        if (parts.size != 2) return null
        
        val meta = parts[0]
        val base64Data = parts[1]
        
        // Parse MIME.
        val mimeType = DATA_URI_MIME_REGEX.find(meta)?.groupValues?.get(1) ?: "image/png"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "png"
        
        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return null
        
        return saveBitmapToGallery(bitmap, "IMG_${System.currentTimeMillis()}.$extension", mimeType)
    }
    
    /**
     * Save URL.
     */
    private fun saveUrlImage(imageUrl: String): Uri? {
        val url = URL(imageUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        val imageBytes = connection.getInputStream().use { it.readBytes() }
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return null
        
        val fileName = DownloadHelper.parseFileName(imageUrl, null, connection.contentType)
            .ifBlank { "IMG_${System.currentTimeMillis()}.jpg" }
        val mimeType = connection.contentType ?: "image/jpeg"
        
        return saveBitmapToGallery(bitmap, fileName, mimeType)
    }
    
    /**
     * Save Bitmap to.
     */
    private fun saveBitmapToGallery(bitmap: Bitmap, fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ use MediaStore.
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
            // Android 9 and.
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "WebToApp")
            if (!appDir.exists()) appDir.mkdirs()
            
            val file = File(appDir, fileName)
            FileOutputStream(file).use { outputStream ->
                val format = if (mimeType.contains("png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, 95, outputStream)
            }
            
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }
    
    /**
     * and Save to.
     */
    fun downloadVideo(videoUrl: String, onResult: (Boolean, String) -> Unit) {
        if (videoUrl.startsWith("blob:")) {
            onResult(false, "Blob 视频需要通过页面下载")
            return
        }
        
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, AppStringsProvider.current().downloadingVideo, Toast.LENGTH_SHORT).show()
            
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
                    AppLogger.e(TAG, "下载视频失败: ${result.message}")
                    onResult(false, result.message)
                }
            }
        }
    }
    
    /**
     * is as URL.
     */
    private fun isVideoUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return VIDEO_EXTENSIONS.any { lowerUrl.contains(it) }
    }
}
