package com.webtoapp.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * 下载处理工具类
 */
object DownloadHelper {
    
    // Download重试配置
    private const val MAX_RETRY_COUNT = 3
    private const val RETRY_DELAY_MS = 1000L
    
    // 记录下载重试次数
    private val retryCountMap = mutableMapOf<String, Int>()
    
    /**
     * 从 Content-Disposition 和 URL 中智能解析文件名
     * Priority:Content-Disposition > URL路径 > 系统猜测
     */
    fun parseFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        var fileName: String? = null
        
        // 1. 优先从 Content-Disposition 解析
        if (!contentDisposition.isNullOrBlank()) {
            fileName = parseContentDisposition(contentDisposition)
        }
        
        // 2. 从 URL 路径解析
        if (fileName.isNullOrBlank()) {
            fileName = parseFileNameFromUrl(url)
        }
        
        // 3. 降级到系统方法
        if (fileName.isNullOrBlank()) {
            fileName = URLUtil.guessFileName(url, contentDisposition, mimeType) ?: "download"
        }
        
        // 4. 确保有正确的扩展名
        fileName = ensureExtension(fileName, mimeType)
        
        return fileName
    }
    
    /**
     * 解析 Content-Disposition 头获取文件名
     * 支持多种格式：
     * - filename="xxx.pdf"
     * - filename=xxx.pdf
     * - filename*=UTF-8''xxx.pdf (RFC 5987)
     * - filename*=utf-8'zh-CN'xxx.pdf
     */
    private fun parseContentDisposition(contentDisposition: String): String? {
        // 尝试 RFC 5987 格式 (filename*=)
        val rfc5987Pattern = Pattern.compile(
            "filename\\*\\s*=\\s*([^']*)'([^']*)'(.+)",
            Pattern.CASE_INSENSITIVE
        )
        val rfc5987Matcher = rfc5987Pattern.matcher(contentDisposition)
        if (rfc5987Matcher.find()) {
            val encoding = rfc5987Matcher.group(1)?.ifBlank { "UTF-8" } ?: "UTF-8"
            val encodedName = rfc5987Matcher.group(3)
            if (!encodedName.isNullOrBlank()) {
                try {
                    return URLDecoder.decode(encodedName, encoding)
                } catch (e: UnsupportedEncodingException) {
                    // 忽略，继续尝试其他方式
                }
            }
        }
        
        // 尝试带引号的 filename="xxx"
        val quotedPattern = Pattern.compile(
            "filename\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
        )
        val quotedMatcher = quotedPattern.matcher(contentDisposition)
        if (quotedMatcher.find()) {
            val name = quotedMatcher.group(1)
            if (!name.isNullOrBlank()) {
                return decodeFileName(name)
            }
        }
        
        // 尝试不带引号的 filename=xxx
        val unquotedPattern = Pattern.compile(
            "filename\\s*=\\s*([^;\\s]+)",
            Pattern.CASE_INSENSITIVE
        )
        val unquotedMatcher = unquotedPattern.matcher(contentDisposition)
        if (unquotedMatcher.find()) {
            val name = unquotedMatcher.group(1)
            if (!name.isNullOrBlank()) {
                return decodeFileName(name)
            }
        }
        
        return null
    }
    
    /**
     * 解码文件名（处理 URL 编码和特殊字符）
     */
    private fun decodeFileName(name: String): String {
        var decoded = name
        try {
            // 尝试 URL 解码
            if (name.contains("%")) {
                decoded = URLDecoder.decode(name, "UTF-8")
            }
        } catch (e: Exception) {
            // 忽略解码错误
        }
        // 移除可能的引号
        return decoded.trim().removeSurrounding("\"").removeSurrounding("'")
    }
    
    /**
     * 从 URL 路径中解析文件名
     */
    private fun parseFileNameFromUrl(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val path = uri.path ?: return null
            
            // Get路径最后一段
            val lastSegment = path.substringAfterLast('/')
            if (lastSegment.isBlank()) return null
            
            // Check是否像一个有效的文件名（包含扩展名）
            if (lastSegment.contains('.') && !lastSegment.startsWith('.')) {
                val decoded = try {
                    URLDecoder.decode(lastSegment, "UTF-8")
                } catch (e: Exception) {
                    lastSegment
                }
                // Filter掉明显不是文件名的情况
                if (decoded.length <= 255 && !decoded.contains('?') && !decoded.contains('&')) {
                    return decoded
                }
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }
        return null
    }
    
    /**
     * 确保文件名有正确的扩展名
     */
    private fun ensureExtension(fileName: String, mimeType: String?): String {
        // 如果已经有扩展名，直接返回
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot > 0 && lastDot < fileName.length - 1) {
            val ext = fileName.substring(lastDot + 1).lowercase()
            // Check是否是有效扩展名（不是 bin 这种通用的）
            if (ext.length in 2..5 && ext != "bin") {
                return fileName
            }
        }
        
        // 尝试从 MIME 类型获取扩展名
        if (!mimeType.isNullOrBlank()) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (!ext.isNullOrBlank()) {
                val baseName = if (lastDot > 0) fileName.substring(0, lastDot) else fileName
                return "$baseName.$ext"
            }
        }
        
        return fileName
    }
    
    /**
     * 下载方式
     */
    enum class DownloadMethod {
        DOWNLOAD_MANAGER,  // 使用系统下载管理器
        BROWSER           // 跳转浏览器下载
    }
    
    /**
     * 处理下载请求
     * @param context 上下文
     * @param url 下载链接
     * @param userAgent User-Agent
     * @param contentDisposition Content-Disposition 头
     * @param mimeType MIME类型
     * @param contentLength 文件大小
     * @param method 下载方式
     * @param showEnhancedNotification 是否显示增强通知（带进度和打开按钮）
     * @param saveToGallery 是否将媒体文件保存到相册（默认 true）
     * @param scope 协程作用域（保存到相册时需要）
     * @param onBlobDownload Blob URL 下载回调，用于通过 WebView 执行 JS 处理
     */
    fun handleDownload(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        method: DownloadMethod = DownloadMethod.DOWNLOAD_MANAGER,
        showEnhancedNotification: Boolean = true,
        saveToGallery: Boolean = true,
        scope: CoroutineScope? = null,
        onBlobDownload: ((blobUrl: String, filename: String) -> Unit)? = null
    ) {
        // Check是否为 Blob URL（blob: 协议无法通过 DownloadManager 下载）
        if (url.startsWith("blob:")) {
            val filename = parseFileName(url, contentDisposition, mimeType)
            if (onBlobDownload != null) {
                Toast.makeText(context, Strings.blobDownloadProcessing, Toast.LENGTH_SHORT).show()
                onBlobDownload(url, filename)
            } else {
                // 没有回调，显示提示
                Toast.makeText(context, Strings.blobDownloadFailed, Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        // Check是否为 Data URL
        if (url.startsWith("data:")) {
            val filename = parseFileName(url, contentDisposition, mimeType)
            if (onBlobDownload != null) {
                Toast.makeText(context, Strings.blobDownloadProcessing, Toast.LENGTH_SHORT).show()
                onBlobDownload(url, filename)
            } else {
                Toast.makeText(context, Strings.blobDownloadFailed, Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val fileName = parseFileName(url, contentDisposition, mimeType)
        
        // Check是否为媒体文件，如果是且启用了保存到相册，则使用 MediaSaver
        if (saveToGallery && MediaSaver.isMediaFile(mimeType, fileName) && scope != null) {
            saveMediaToGallery(context, url, fileName, mimeType, scope)
            return
        }
        
        when (method) {
            DownloadMethod.DOWNLOAD_MANAGER -> {
                downloadWithManager(context, url, userAgent, contentDisposition, mimeType, showEnhancedNotification)
            }
            DownloadMethod.BROWSER -> {
                openInBrowser(context, url)
            }
        }
    }
    
    /**
     * 保存媒体文件到相册
     */
    private fun saveMediaToGallery(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String,
        scope: CoroutineScope
    ) {
        val mediaType = MediaSaver.getMediaType(mimeType) ?: MediaSaver.getMediaTypeByExtension(fileName)
        val typeText = when (mediaType) {
            MediaSaver.MediaType.IMAGE -> if (Strings.currentLanguage.value == com.webtoapp.core.i18n.AppLanguage.CHINESE) "图片" else "image"
            MediaSaver.MediaType.VIDEO -> if (Strings.currentLanguage.value == com.webtoapp.core.i18n.AppLanguage.CHINESE) "视频" else "video"
            else -> if (Strings.currentLanguage.value == com.webtoapp.core.i18n.AppLanguage.CHINESE) "文件" else "file"
        }
        
        val notificationManager = DownloadNotificationManager.getInstance(context)
        val progressNotificationId = notificationManager.showIndeterminateProgress(fileName)
        
        val savingMsg = when (mediaType) {
            MediaSaver.MediaType.IMAGE -> Strings.savingImageToGallery
            MediaSaver.MediaType.VIDEO -> Strings.savingVideoToGallery
            else -> Strings.savingToGallery
        }
        Toast.makeText(context, savingMsg, Toast.LENGTH_SHORT).show()
        
        scope.launch(Dispatchers.Main) {
            val result = MediaSaver.saveFromUrl(context, url, fileName, mimeType)
            
            when (result) {
                is MediaSaver.SaveResult.Success -> {
                    val savedMsg = when (mediaType) {
                        MediaSaver.MediaType.IMAGE -> Strings.imageSavedToGallery
                        MediaSaver.MediaType.VIDEO -> Strings.videoSavedToGallery
                        else -> Strings.imageSavedToGallery
                    }
                    Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                    
                    // Show完成通知
                    notificationManager.showMediaSaveComplete(
                        fileName = fileName,
                        uri = result.uri,
                        mimeType = mimeType,
                        isImage = mediaType == MediaSaver.MediaType.IMAGE,
                        progressNotificationId = progressNotificationId
                    )
                }
                is MediaSaver.SaveResult.Error -> {
                    Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                    notificationManager.showSaveFailed(fileName, result.message, progressNotificationId)
                }
            }
        }
    }
    
    /**
     * 使用系统下载管理器下载（带重试机制）
     */
    fun downloadWithManager(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        showEnhancedNotification: Boolean = true,
        retryOnFailure: Boolean = true
    ) {
        try {
            // 使用智能文件名解析
            val fileName = parseFileName(url, contentDisposition, mimeType)
            
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                // SetUser-Agent
                addRequestHeader("User-Agent", userAgent)
                
                // Set通知栏显示
                if (showEnhancedNotification) {
                    // 使用增强通知时，隐藏系统默认通知，由我们自己管理
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                } else {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                }
                setTitle(fileName)
                setDescription("正在下载...")
                
                // Set保存位置到Download文件夹
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                
                // Allow在移动网络和WiFi下下载
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or 
                    DownloadManager.Request.NETWORK_MOBILE
                )
                
                // SetMIME类型
                if (mimeType.isNotBlank()) {
                    setMimeType(mimeType)
                }
            }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            
            // 使用增强通知管理器跟踪下载
            if (showEnhancedNotification) {
                val notificationManager = DownloadNotificationManager.getInstance(context)
                notificationManager.trackDownload(downloadId, fileName, mimeType)
            }
            
            // 清除重试计数
            retryCountMap.remove(url)
            
            Toast.makeText(context, Strings.startDownload.replace("%s", fileName), Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            
            // 重试逻辑
            if (retryOnFailure) {
                val currentRetry = retryCountMap.getOrDefault(url, 0)
                if (currentRetry < MAX_RETRY_COUNT) {
                    retryCountMap[url] = currentRetry + 1
                    Toast.makeText(
                        context, 
                        "${Strings.downloadFailed}, ${Strings.retry} (${currentRetry + 1}/$MAX_RETRY_COUNT)...", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 延迟重试
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        downloadWithManager(context, url, userAgent, contentDisposition, mimeType, showEnhancedNotification, true)
                    }, RETRY_DELAY_MS * (currentRetry + 1))
                    return
                }
            }
            
            // 重试次数用尽，清除计数并降级到浏览器
            retryCountMap.remove(url)
            Toast.makeText(context, Strings.downloadFailedTryBrowser, Toast.LENGTH_SHORT).show()
            openInBrowser(context, url)
        }
    }
    
    /**
     * 跳转到浏览器下载
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, Strings.cannotOpenBrowser, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 从URL猜测文件扩展名
     */
    fun guessExtension(url: String, mimeType: String?): String {
        // 尝试从MIME类型获取
        mimeType?.let {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
            if (!ext.isNullOrBlank()) return ".$ext"
        }
        
        // 尝试从URL获取
        val path = Uri.parse(url).path ?: return ""
        val lastDot = path.lastIndexOf('.')
        if (lastDot >= 0 && lastDot < path.length - 1) {
            val ext = path.substring(lastDot)
            if (ext.length <= 5) return ext
        }
        
        return ""
    }
}
