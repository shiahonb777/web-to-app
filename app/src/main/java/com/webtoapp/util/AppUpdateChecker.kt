package com.webtoapp.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 应用更新检查器
 * 从 Gitee releases 页面获取最新版本信息
 */
object AppUpdateChecker {
    
    // Gitee releases 页面 URL
    private const val RELEASES_URL = "https://gitee.com/ashiahonb777/web-to-app/releases"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 版本更新信息
     */
    data class UpdateInfo(
        val versionName: String,      // 版本名称，如 "v1.5.0"
        val versionCode: Int,         // 版本号
        val downloadUrl: String,      // APK 下载链接
        val releaseNotes: String,     // 更新说明
        val hasUpdate: Boolean        // 是否有更新
    )
    
    /**
     * 检查更新
     * @param currentVersionCode 当前应用的版本号
     * @return 更新信息，失败返回 null
     */
    suspend fun checkUpdate(currentVersionCode: Int): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            // 获取 releases 页面 HTML
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("网络请求失败: ${response.code}"))
            }
            
            val html = response.body?.string() ?: ""
            
            // 解析最新版本信息
            val updateInfo = parseReleasesPage(html, currentVersionCode)
            
            Result.success(updateInfo)
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateChecker", "检查更新失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 解析 Gitee releases 页面
     */
    private fun parseReleasesPage(html: String, currentVersionCode: Int): UpdateInfo {
        android.util.Log.d("AppUpdateChecker", "开始解析 releases 页面")
        
        // 匹配版本标签，格式如 v1.5.0 或 vx.x.x
        // Gitee 格式: releases/tag/v1.5.0 或 /releases/tag/v1.5.0
        val versionPattern = Pattern.compile("""releases/tag/(v?[\d.]+)""")
        val versionMatcher = versionPattern.matcher(html)
        
        var latestVersion = ""
        if (versionMatcher.find()) {
            latestVersion = versionMatcher.group(1) ?: ""
            // 确保以 v 开头
            if (!latestVersion.startsWith("v")) {
                latestVersion = "v$latestVersion"
            }
            android.util.Log.d("AppUpdateChecker", "找到最新版本: $latestVersion")
        }
        
        // 提取版本号数字（如 v1.5.0 -> 150）
        val latestVersionCode = parseVersionCode(latestVersion)
        android.util.Log.d("AppUpdateChecker", "版本代码: $latestVersionCode, 当前: $currentVersionCode")
        
        // 查找 APK 下载链接
        // Gitee 附件下载格式有多种：
        // 1. /releases/download/tag/file.apk
        // 2. /attach_files/xxxxx/download/file.apk  
        var downloadUrl = ""
        
        // 尝试匹配 releases/download 格式
        val downloadPattern1 = Pattern.compile("""href=["']([^"']*?/releases/download/[^"']*?\.apk)["']""", Pattern.CASE_INSENSITIVE)
        val downloadMatcher1 = downloadPattern1.matcher(html)
        if (downloadMatcher1.find()) {
            val path = downloadMatcher1.group(1) ?: ""
            downloadUrl = if (path.startsWith("http")) path else "https://gitee.com$path"
        }
        
        // 尝试匹配 attach_files 格式（Gitee 上传附件）
        if (downloadUrl.isEmpty()) {
            val downloadPattern2 = Pattern.compile("""href=["']([^"']*?/attach_files/[^"']*?download[^"']*?)["'][^>]*>[^<]*\.apk""", Pattern.CASE_INSENSITIVE)
            val downloadMatcher2 = downloadPattern2.matcher(html)
            if (downloadMatcher2.find()) {
                val path = downloadMatcher2.group(1) ?: ""
                downloadUrl = if (path.startsWith("http")) path else "https://gitee.com$path"
            }
        }
        
        // 尝试直接匹配 .apk 链接
        if (downloadUrl.isEmpty()) {
            val downloadPattern3 = Pattern.compile("""href=["']([^"']*?\.apk)["']""", Pattern.CASE_INSENSITIVE)
            val downloadMatcher3 = downloadPattern3.matcher(html)
            if (downloadMatcher3.find()) {
                val path = downloadMatcher3.group(1) ?: ""
                downloadUrl = if (path.startsWith("http")) path else "https://gitee.com$path"
            }
        }
        
        android.util.Log.d("AppUpdateChecker", "下载链接: $downloadUrl")
        
        // 提取更新说明（release notes）
        val releaseNotes = extractReleaseNotes(html)
        
        val hasUpdate = latestVersionCode > currentVersionCode
        
        return UpdateInfo(
            versionName = latestVersion,
            versionCode = latestVersionCode,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes,
            hasUpdate = hasUpdate
        )
    }
    
    /**
     * 解析版本号
     * v1.5.0 -> 150, v1.3.0 -> 130
     */
    private fun parseVersionCode(versionName: String): Int {
        return try {
            val numbers = versionName.replace("v", "")
                .split(".")
                .take(3)
                .map { it.toIntOrNull() ?: 0 }
            
            when (numbers.size) {
                1 -> numbers[0] * 100
                2 -> numbers[0] * 100 + numbers[1] * 10
                else -> numbers[0] * 100 + numbers[1] * 10 + numbers.getOrElse(2) { 0 }
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 提取更新说明
     */
    private fun extractReleaseNotes(html: String): String {
        // 尝试从 release notes 区域提取文本
        val notesPattern = Pattern.compile("""<div class="release-body"[^>]*>(.*?)</div>""", Pattern.DOTALL)
        val notesMatcher = notesPattern.matcher(html)
        
        return if (notesMatcher.find()) {
            val rawNotes = notesMatcher.group(1) ?: ""
            // 清理 HTML 标签
            rawNotes.replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .trim()
                .take(500) // 限制长度
        } else {
            ""
        }
    }
    
    /**
     * 使用系统 DownloadManager 下载 APK
     * @return 下载ID，失败返回 -1
     */
    fun downloadApk(
        context: Context,
        downloadUrl: String,
        versionName: String
    ): Long {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            val fileName = "WebToApp_${versionName}.apk"
            
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("WebToApp 更新")
                .setDescription("正在下载 $versionName ...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateChecker", "下载失败", e)
            -1
        }
    }
    
    /**
     * 安装 APK
     */
    fun installApk(context: Context, downloadId: Long) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = downloadManager.getUriForDownloadedFile(downloadId)
            
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateChecker", "安装失败", e)
        }
    }
    
    /**
     * 获取当前应用版本信息
     */
    fun getCurrentVersionInfo(context: Context): Pair<String, Int> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Pair("1.0.0", 1)
        }
    }
}
