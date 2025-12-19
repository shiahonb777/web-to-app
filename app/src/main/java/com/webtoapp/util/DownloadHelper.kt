package com.webtoapp.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast

/**
 * 下载处理工具类
 */
object DownloadHelper {
    
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
     */
    fun handleDownload(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        method: DownloadMethod = DownloadMethod.DOWNLOAD_MANAGER
    ) {
        when (method) {
            DownloadMethod.DOWNLOAD_MANAGER -> {
                downloadWithManager(context, url, userAgent, contentDisposition, mimeType)
            }
            DownloadMethod.BROWSER -> {
                openInBrowser(context, url)
            }
        }
    }
    
    /**
     * 使用系统下载管理器下载
     */
    fun downloadWithManager(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ) {
        try {
            // 从URL或Content-Disposition中获取文件名
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                // 设置User-Agent
                addRequestHeader("User-Agent", userAgent)
                
                // 设置通知栏显示
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setTitle(fileName)
                setDescription("正在下载...")
                
                // 设置保存位置到Download文件夹
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                
                // 允许在移动网络和WiFi下下载
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or 
                    DownloadManager.Request.NETWORK_MOBILE
                )
                
                // 设置MIME类型
                if (mimeType.isNotBlank()) {
                    setMimeType(mimeType)
                }
            }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            
            Toast.makeText(context, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "下载失败，尝试使用浏览器下载", Toast.LENGTH_SHORT).show()
            // 降级到浏览器下载
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
            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
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
