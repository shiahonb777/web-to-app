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
                // 图片链接：需要通过 JS 获取链接地址
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
     */
    fun getLongPressDetails(webView: WebView, x: Float, y: Float, callback: (LongPressResult) -> Unit) {
        val js = """
            (function() {
                var elem = document.elementFromPoint($x, $y);
                if (!elem) return JSON.stringify({type: 'none'});
                
                var result = {type: 'none'};
                
                // 检查是否是图片
                if (elem.tagName === 'IMG') {
                    result = {
                        type: 'image',
                        url: elem.src,
                        alt: elem.alt || ''
                    };
                    // 检查图片是否在链接内
                    var parent = elem.parentElement;
                    while (parent) {
                        if (parent.tagName === 'A' && parent.href) {
                            result.type = 'imageLink';
                            result.linkUrl = parent.href;
                            break;
                        }
                        parent = parent.parentElement;
                    }
                }
                // 检查是否是视频
                else if (elem.tagName === 'VIDEO') {
                    result = {
                        type: 'video',
                        url: elem.src || elem.currentSrc || ''
                    };
                    // 检查 source 标签
                    if (!result.url) {
                        var source = elem.querySelector('source');
                        if (source) result.url = source.src;
                    }
                }
                // 检查是否是链接
                else if (elem.tagName === 'A') {
                    var href = elem.href || '';
                    if (href.match(/\.(mp4|webm|ogg|mov|avi|mkv)(\?|$)/i)) {
                        result = {type: 'video', url: href};
                    } else {
                        result = {type: 'link', url: href, title: elem.textContent || ''};
                    }
                }
                // 向上查找链接
                else {
                    var parent = elem.parentElement;
                    while (parent) {
                        if (parent.tagName === 'A' && parent.href) {
                            var href = parent.href;
                            if (href.match(/\.(mp4|webm|ogg|mov|avi|mkv)(\?|$)/i)) {
                                result = {type: 'video', url: href};
                            } else {
                                result = {type: 'link', url: href, title: parent.textContent || ''};
                            }
                            break;
                        }
                        parent = parent.parentElement;
                    }
                }
                
                return JSON.stringify(result);
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
                        result.optString("title", null)
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
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
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
                        onResult(true, "图片已保存")
                    } else {
                        onResult(false, "保存失败")
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
        
        // 解析 MIME 类型
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
        
        val fileName = URLUtil.guessFileName(imageUrl, null, null)
            ?: "IMG_${System.currentTimeMillis()}.jpg"
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
            Toast.makeText(context, "正在下载视频...", Toast.LENGTH_SHORT).show()
            
            val fileName = URLUtil.guessFileName(videoUrl, null, "video/mp4")
                ?: "VIDEO_${System.currentTimeMillis()}.mp4"
            
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
