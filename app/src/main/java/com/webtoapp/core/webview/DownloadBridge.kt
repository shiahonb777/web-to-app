package com.webtoapp.core.webview

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.DownloadNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * JavaScript 下载桥接
 * 用于处理 WebView 中的 Blob 下载和文件保存
 * 
 * 解决问题：
 * 1. WebView 无法处理 blob: URL 下载
 * 2. HTML 应用中的数据导出功能
 * 3. 前端生成的文件下载（如 JSON、CSV 等）
 */
class DownloadBridge(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val notificationManager = DownloadNotificationManager.getInstance(context)
    companion object {
        const val JS_INTERFACE_NAME = "AndroidDownload"
        
        /**
         * 生成注入到 WebView 的 JavaScript 代码
         * 拦截 Blob 下载并转发给原生代码处理
         */
        fun getInjectionScript(): String {
            // Get翻译字符串
            val msgPreparingDownload = Strings.preparingDownload
            val msgCannotGetFileData = Strings.cannotGetFileData
            val msgDownloadUnavailable = Strings.downloadUnavailable
            val msgProcessFileFailed = Strings.processFileFailed
            val msgReadFileFailed = Strings.readFileFailed
            val msgDownloadFailed = Strings.downloadFailedPrefix
            
            return """
            (function() {
                // 避免重复注入
                if (window._downloadBridgeInjected) return;
                window._downloadBridgeInjected = true;
                
                console.log('[DownloadBridge] Starting injection...');
                
                // Save原始方法
                const originalCreateElement = document.createElement.bind(document);
                const originalCreateObjectURL = URL.createObjectURL.bind(URL);
                const originalRevokeObjectURL = URL.revokeObjectURL.bind(URL);
                
                // Blob URL 映射表
                const blobUrlMap = new Map();
                
                // 拦截 URL.createObjectURL
                URL.createObjectURL = function(blob) {
                    const url = originalCreateObjectURL(blob);
                    if (blob instanceof Blob) {
                        blobUrlMap.set(url, blob);
                        console.log('[DownloadBridge] Blob URL created:', url, 'type:', blob.type, 'size:', blob.size);
                    }
                    return url;
                };
                
                // 拦截 URL.revokeObjectURL
                URL.revokeObjectURL = function(url) {
                    // 延迟删除，给下载处理留时间
                    setTimeout(() => {
                        blobUrlMap.delete(url);
                    }, 5000);
                    return originalRevokeObjectURL(url);
                };
                
                // 拦截 document.createElement，监控 <a> 标签的创建和点击
                document.createElement = function(tagName) {
                    const element = originalCreateElement(tagName);
                    
                    if (tagName.toLowerCase() === 'a') {
                        // 拦截 click 方法
                        const originalClick = element.click.bind(element);
                        element.click = function() {
                            const href = element.href || '';
                            const download = element.getAttribute('download');
                            
                            console.log('[DownloadBridge] <a>.click() intercepted:', href.substring(0, 100), 'download:', download);
                            
                            // Handle blob: URL
                            if (href.startsWith('blob:') && download) {
                                console.log('[DownloadBridge] Handling blob download programmatically');
                                handleBlobDownload(href, download);
                                return; // 阻止默认行为
                            }
                            
                            // Handle data: URL
                            if (href.startsWith('data:') && download) {
                                console.log('[DownloadBridge] Handling data URL download programmatically');
                                handleDataUrlDownload(href, download);
                                return; // 阻止默认行为
                            }
                            
                            // 其他情况执行原始 click
                            return originalClick();
                        };
                    }
                    
                    return element;
                };
                
                // 拦截 a 标签的点击事件（处理已存在的 a 标签）
                document.addEventListener('click', function(e) {
                    let target = e.target;
                    // 向上查找 a 标签
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    
                    if (target && target.tagName === 'A') {
                        const href = target.href || '';
                        const download = target.getAttribute('download');
                        
                        // Handle blob: URL
                        if (href.startsWith('blob:') && download) {
                            e.preventDefault();
                            e.stopPropagation();
                            console.log('[DownloadBridge] Handling blob download from click event');
                            handleBlobDownload(href, download);
                            return false;
                        }
                        
                        // Handle data: URL
                        if (href.startsWith('data:') && download) {
                            e.preventDefault();
                            e.stopPropagation();
                            console.log('[DownloadBridge] Handling data URL download from click event');
                            handleDataUrlDownload(href, download);
                            return false;
                        }
                    }
                }, true);
                
                // Handle Blob URL 下载
                async function handleBlobDownload(blobUrl, filename) {
                    try {
                        console.log('[DownloadBridge] handleBlobDownload:', blobUrl, filename);
                        
                        // Show下载中提示
                        if (window.AndroidDownload && window.AndroidDownload.showToast) {
                            window.AndroidDownload.showToast('$msgPreparingDownload' + filename);
                        }
                        
                        // 优先从缓存获取 Blob
                        let blob = blobUrlMap.get(blobUrl);
                        
                        if (!blob) {
                            // 尝试 fetch
                            console.log('[DownloadBridge] Blob not in cache, trying fetch...');
                            try {
                                const response = await fetch(blobUrl);
                                blob = await response.blob();
                            } catch (fetchError) {
                                console.error('[DownloadBridge] Fetch failed:', fetchError);
                                alert('$msgCannotGetFileData');
                                return;
                            }
                        }
                        
                        console.log('[DownloadBridge] Blob obtained, type:', blob.type, 'size:', blob.size);
                        
                        const reader = new FileReader();
                        
                        reader.onloadend = function() {
                            try {
                                const base64Data = reader.result.split(',')[1];
                                const mimeType = blob.type || getMimeTypeFromFilename(filename) || 'application/octet-stream';
                                
                                console.log('[DownloadBridge] Sending to native, base64 length:', base64Data ? base64Data.length : 0);
                                
                                if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                    window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                                } else {
                                    console.error('[DownloadBridge] AndroidDownload bridge not available');
                                    alert('$msgDownloadUnavailable');
                                }
                            } catch (e) {
                                console.error('[DownloadBridge] Error processing blob:', e);
                                alert('$msgProcessFileFailed' + e.message);
                            }
                        };
                        
                        reader.onerror = function() {
                            console.error('[DownloadBridge] FileReader error');
                            alert('$msgReadFileFailed');
                        };
                        
                        reader.readAsDataURL(blob);
                    } catch (error) {
                        console.error('[DownloadBridge] Blob download error:', error);
                        alert('$msgDownloadFailed' + error.message);
                    }
                }
                
                // Handle Data URL 下载
                function handleDataUrlDownload(dataUrl, filename) {
                    try {
                        console.log('[DownloadBridge] handleDataUrlDownload:', filename);
                        
                        const parts = dataUrl.split(',');
                        const meta = parts[0];
                        const base64Data = parts[1];
                        
                        // 提取 MIME 类型
                        const mimeMatch = meta.match(/data:([^;]+)/);
                        const mimeType = mimeMatch ? mimeMatch[1] : getMimeTypeFromFilename(filename) || 'application/octet-stream';
                        
                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                        } else {
                            console.error('[DownloadBridge] AndroidDownload bridge not available');
                            alert('$msgDownloadUnavailable');
                        }
                    } catch (error) {
                        console.error('[DownloadBridge] Data URL download error:', error);
                        alert('$msgDownloadFailed' + error.message);
                    }
                }
                
                // 根据文件名猜测 MIME 类型
                function getMimeTypeFromFilename(filename) {
                    const ext = filename.split('.').pop().toLowerCase();
                    const mimeTypes = {
                        'json': 'application/json',
                        'txt': 'text/plain',
                        'html': 'text/html',
                        'htm': 'text/html',
                        'css': 'text/css',
                        'js': 'application/javascript',
                        'xml': 'application/xml',
                        'csv': 'text/csv',
                        'pdf': 'application/pdf',
                        'zip': 'application/zip',
                        'png': 'image/png',
                        'jpg': 'image/jpeg',
                        'jpeg': 'image/jpeg',
                        'gif': 'image/gif',
                        'webp': 'image/webp',
                        'svg': 'image/svg+xml',
                        'mp3': 'audio/mpeg',
                        'mp4': 'video/mp4',
                        'webm': 'video/webm'
                    };
                    return mimeTypes[ext] || null;
                }
                
                // 提供全局下载方法供 HTML 应用调用
                window.nativeDownload = function(data, filename, mimeType) {
                    mimeType = mimeType || getMimeTypeFromFilename(filename) || 'application/octet-stream';
                    
                    console.log('[DownloadBridge] nativeDownload called:', filename, mimeType);
                    
                    if (typeof data === 'string') {
                        // 字符串数据，转为 Base64
                        const base64 = btoa(unescape(encodeURIComponent(data)));
                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64, filename, mimeType);
                        }
                    } else if (data instanceof Blob) {
                        // Blob 数据
                        const reader = new FileReader();
                        reader.onloadend = function() {
                            const base64Data = reader.result.split(',')[1];
                            if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                            }
                        };
                        reader.readAsDataURL(data);
                    } else if (data instanceof ArrayBuffer) {
                        // ArrayBuffer 数据
                        const bytes = new Uint8Array(data);
                        let binary = '';
                        for (let i = 0; i < bytes.byteLength; i++) {
                            binary += String.fromCharCode(bytes[i]);
                        }
                        const base64 = btoa(binary);
                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64, filename, mimeType);
                        }
                    }
                };
                
                // 提供 JSON 导出快捷方法
                window.nativeDownloadJSON = function(obj, filename) {
                    const json = JSON.stringify(obj, null, 2);
                    window.nativeDownload(json, filename || 'data.json', 'application/json');
                };
                
                // 提供文本导出快捷方法
                window.nativeDownloadText = function(text, filename) {
                    window.nativeDownload(text, filename || 'text.txt', 'text/plain');
                };
                
                // Check桥接是否可用
                window.isNativeDownloadAvailable = function() {
                    return !!(window.AndroidDownload && window.AndroidDownload.saveBase64File);
                };
                
                console.log('[DownloadBridge] Injection complete, bridge available:', window.isNativeDownloadAvailable());
            })();
        """.trimIndent()
        }
    }
    
    /**
     * 显示 Toast 提示
     */
    @JavascriptInterface
    fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 保存 Base64 编码的文件
     * 媒体文件（图片/视频）会保存到相册，其他文件保存到下载目录
     * @param base64Data Base64 编码的文件数据
     * @param filename 文件名
     * @param mimeType MIME 类型
     */
    @JavascriptInterface
    fun saveBase64File(base64Data: String, filename: String, mimeType: String) {
        scope.launch(Dispatchers.IO) {
            // Show进度通知
            val progressNotificationId = notificationManager.showIndeterminateProgress(filename)
            
            try {
                // 解码 Base64 数据
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                
                // 确保文件名安全
                val safeFilename = sanitizeFilename(filename)
                
                // Check是否为媒体文件
                if (com.webtoapp.util.MediaSaver.isMediaFile(mimeType, safeFilename)) {
                    // Media文件保存到相册
                    val result = com.webtoapp.util.MediaSaver.saveFromBytes(
                        context, decodedBytes, safeFilename, mimeType
                    )
                    
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is com.webtoapp.util.MediaSaver.SaveResult.Success -> {
                                val isImage = mimeType.startsWith("image/")
                                val typeText = if (isImage) Strings.image else Strings.video
                                Toast.makeText(context, Strings.savedToGallery.replace("%s", typeText), Toast.LENGTH_SHORT).show()
                                
                                // Show完成通知
                                notificationManager.showMediaSaveComplete(
                                    fileName = safeFilename,
                                    uri = result.uri,
                                    mimeType = mimeType,
                                    isImage = isImage,
                                    progressNotificationId = progressNotificationId
                                )
                            }
                            is com.webtoapp.util.MediaSaver.SaveResult.Error -> {
                                Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                                notificationManager.showSaveFailed(safeFilename, result.message, progressNotificationId)
                            }
                        }
                    }
                } else {
                    // 非媒体文件保存到下载目录
                    val savedFile = saveToDownloadsInternal(decodedBytes, safeFilename)
                    
                    withContext(Dispatchers.Main) {
                        if (savedFile != null) {
                            Toast.makeText(context, Strings.savedTo.replace("%s", savedFile.name), Toast.LENGTH_LONG).show()
                            
                            // Show完成通知
                            notificationManager.showSaveComplete(
                                fileName = savedFile.name,
                                filePath = savedFile.absolutePath,
                                mimeType = mimeType,
                                progressNotificationId = progressNotificationId
                            )
                        } else {
                            Toast.makeText(context, Strings.saveFailed, Toast.LENGTH_SHORT).show()
                            notificationManager.showSaveFailed(safeFilename, "无法写入文件", progressNotificationId)
                        }
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("DownloadBridge", "保存文件失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", e.message ?: ""), Toast.LENGTH_SHORT).show()
                    notificationManager.showSaveFailed(filename, e.message ?: "未知错误", progressNotificationId)
                }
            }
        }
    }
    
    /**
     * 保存文件到下载目录（内部方法，返回保存的文件）
     * Android 10+ 使用 MediaStore API 保存到公共下载目录
     * Android 9 及以下使用传统文件 API
     */
    private fun saveToDownloadsInternal(bytes: ByteArray, filename: String): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 MediaStore API 保存到公共下载目录
            saveToDownloadsMediaStore(bytes, filename)
        } else {
            // Android 9 及以下使用传统方式
            saveToDownloadsLegacy(bytes, filename)
        }
    }
    
    /**
     * Android 10+ 使用 MediaStore API 保存到公共下载目录
     */
    private fun saveToDownloadsMediaStore(bytes: ByteArray, filename: String): File? {
        try {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, getMimeType(filename))
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WebToApp")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return null
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            }
            
            // 标记文件写入完成
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            
            // 返回一个虚拟 File 对象用于显示路径
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadDir, "WebToApp")
            android.util.Log.d("DownloadBridge", "文件已保存到公共下载目录: ${appDir.absolutePath}/$filename")
            return File(appDir, filename)
            
        } catch (e: Exception) {
            android.util.Log.e("DownloadBridge", "MediaStore 保存失败，尝试备用方案", e)
            // 降级到应用私有目录
            return saveToAppPrivateDir(bytes, filename)
        }
    }
    
    /**
     * Android 9 及以下使用传统文件 API
     */
    private fun saveToDownloadsLegacy(bytes: ByteArray, filename: String): File? {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(downloadDir, "WebToApp")
        
        // 确保目录存在
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        // Handle文件名冲突
        var targetFile = File(appDir, filename)
        var counter = 1
        val nameWithoutExt = filename.substringBeforeLast(".")
        val ext = if (filename.contains(".")) ".${filename.substringAfterLast(".")}" else ""
        
        while (targetFile.exists()) {
            targetFile = File(appDir, "${nameWithoutExt}_$counter$ext")
            counter++
        }
        
        // 写入文件
        return try {
            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
            }
            android.util.Log.d("DownloadBridge", "文件已保存: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            android.util.Log.e("DownloadBridge", "Failed to write file", e)
            null
        }
    }
    
    /**
     * 保存到应用私有目录（备用方案）
     */
    private fun saveToAppPrivateDir(bytes: ByteArray, filename: String): File? {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        
        // 确保目录存在
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        // Handle文件名冲突
        var targetFile = File(downloadDir, filename)
        var counter = 1
        val nameWithoutExt = filename.substringBeforeLast(".")
        val ext = if (filename.contains(".")) ".${filename.substringAfterLast(".")}" else ""
        
        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${nameWithoutExt}_$counter$ext")
            counter++
        }
        
        // 写入文件
        return try {
            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
            }
            android.util.Log.d("DownloadBridge", "文件已保存到应用目录: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            android.util.Log.e("DownloadBridge", "Failed to write file", e)
            null
        }
    }
    
    /**
     * 根据文件名获取 MIME 类型
     */
    private fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "json" -> "application/json"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "xml" -> "application/xml"
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * 直接保存文本内容到文件
     * @param content 文本内容
     * @param filename 文件名
     */
    @JavascriptInterface
    fun saveTextFile(content: String, filename: String) {
        val base64Data = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        saveBase64File(base64Data, filename, "text/plain")
    }
    
    /**
     * 直接保存 JSON 内容到文件
     * @param jsonContent JSON 字符串
     * @param filename 文件名
     */
    @JavascriptInterface
    fun saveJsonFile(jsonContent: String, filename: String) {
        val base64Data = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val safeFilename = if (filename.endsWith(".json")) filename else "$filename.json"
        saveBase64File(base64Data, safeFilename, "application/json")
    }
    
    /**
     * 检查下载桥接是否可用
     */
    @JavascriptInterface
    fun isAvailable(): Boolean = true
    
    /**
     * 获取下载目录路径（供 JS 显示给用户）
     */
    @JavascriptInterface
    fun getDownloadPath(): String {
        val downloadDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        return downloadDir.absolutePath
    }
    
    /**
     * 清理文件名，移除不安全字符
     */
    private fun sanitizeFilename(filename: String): String {
        // 移除路径分隔符和其他不安全字符
        var safe = filename.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        
        // 限制长度
        if (safe.length > 200) {
            val ext = safe.substringAfterLast(".", "")
            val name = safe.substringBeforeLast(".")
            safe = if (ext.isNotEmpty()) {
                "${name.take(190)}.$ext"
            } else {
                name.take(200)
            }
        }
        
        // 确保不为空
        if (safe.isBlank()) {
            safe = "download_${System.currentTimeMillis()}"
        }
        
        return safe
    }
}
