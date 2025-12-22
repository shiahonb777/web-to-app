package com.webtoapp.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 应用更新检查器
 * 基于 Gitee releases 下载地址格式动态识别版本
 * 
 * 下载地址格式: https://gitee.com/ashiahonb777/web-to-app/releases/download/v{version}/web-to-app-{version}.apk
 */
object AppUpdateChecker {
    
    // Gitee releases 页面 URL
    private const val RELEASES_PAGE_URL = "https://gitee.com/ashiahonb777/web-to-app/releases"
    
    // 下载地址模板（{VERSION} 会被替换为实际版本号）
    private const val DOWNLOAD_URL_TEMPLATE = "https://gitee.com/ashiahonb777/web-to-app/releases/download/v{VERSION}/web-to-app-{VERSION}.APK"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    /**
     * 版本更新信息
     */
    data class UpdateInfo(
        val versionName: String,      // 版本名称，如 "v1.5.0"
        val downloadUrl: String,      // APK 下载链接
        val hasUpdate: Boolean,       // 是否有更新
        val releaseNotes: String = "" // 更新说明
    )
    
    /**
     * 检查更新
     * @param currentVersionName 当前应用的版本名称（如 "1.5.0"）
     * @return 更新信息，失败返回 Result.failure
     */
    suspend fun checkUpdate(currentVersionName: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_PAGE_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("网络请求失败: ${response.code}"))
            }
            
            val html = response.body?.string() ?: ""
            
            // 从页面中提取最新版本号
            val latestVersion = extractLatestVersion(html)
            if (latestVersion.isEmpty()) {
                return@withContext Result.failure(Exception("未找到版本信息"))
            }
            
            // 比较版本
            val hasUpdate = compareVersions(latestVersion, currentVersionName) > 0
            
            // 动态构建下载地址
            val downloadUrl = buildDownloadUrl(latestVersion)
            
            Result.success(UpdateInfo(
                versionName = "v$latestVersion",
                downloadUrl = downloadUrl,
                hasUpdate = hasUpdate
            ))
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateChecker", "检查更新失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从 releases 页面提取最新版本号
     * 匹配格式: releases/tag/v1.5.0 或 releases/download/v1.5.0
     */
    private fun extractLatestVersion(html: String): String {
        // 匹配 releases/tag/vX.X.X 或 releases/download/vX.X.X
        val pattern = Pattern.compile("""releases/(?:tag|download)/v?(\d+\.\d+\.\d+)""")
        val matcher = pattern.matcher(html)
        
        var latestVersion = ""
        var latestVersionCode = 0
        
        // 找出所有版本中最大的
        while (matcher.find()) {
            val version = matcher.group(1) ?: continue
            val versionCode = parseVersionToCode(version)
            if (versionCode > latestVersionCode) {
                latestVersionCode = versionCode
                latestVersion = version
            }
        }
        
        android.util.Log.d("AppUpdateChecker", "检测到最新版本: $latestVersion")
        return latestVersion
    }
    
    /**
     * 动态构建下载地址
     * @param version 版本号（不带v前缀），如 "1.5.0"
     */
    private fun buildDownloadUrl(version: String): String {
        return DOWNLOAD_URL_TEMPLATE.replace("{VERSION}", version)
    }
    
    /**
     * 比较两个版本号
     * @return 正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等
     */
    fun compareVersions(v1: String, v2: String): Int {
        val code1 = parseVersionToCode(v1)
        val code2 = parseVersionToCode(v2)
        return code1 - code2
    }
    
    /**
     * 将版本号转换为数字便于比较
     * 支持格式: "1.5.0", "v1.5.0", "1.5", "v1.5"
     * 
     * 转换规则: major * 10000 + minor * 100 + patch
     * 例如: 1.5.0 -> 10500, 1.6.0 -> 10600, 2.0.0 -> 20000
     */
    private fun parseVersionToCode(version: String): Int {
        return try {
            val cleanVersion = version.removePrefix("v").removePrefix("V")
            val parts = cleanVersion.split(".").map { it.toIntOrNull() ?: 0 }
            
            val major = parts.getOrElse(0) { 0 }
            val minor = parts.getOrElse(1) { 0 }
            val patch = parts.getOrElse(2) { 0 }
            
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            0
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
