package com.webtoapp.util

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.network.NetworkModule
import com.google.gson.JsonParser
import okhttp3.Request

/**
 * 应用更新检查器
 * 
 * 架构设计（最大化节省服务器流量）：
 * - 版本检查：通过自有服务器 API（仅几 KB 的 JSON 响应）
 * - APK 下载：直接走 GitHub Releases（免费无限流量，不消耗服务器带宽）
 * - 备用方案：如果服务器 API 不可用，fallback 到 GitHub releases 页面抓取
 * 
 * 流量消耗对比：
 *   服务器 API：~1 KB/次检查
 *   GitHub 下载：0 服务器流量（GitHub 承担）
 */
object AppUpdateChecker {
    
    private const val TAG = "AppUpdateChecker"
    
    // 自有服务器 API（版本检查用，仅传输 JSON，几乎不消耗流量）
    private const val API_BASE_URL = "https://api.shiaho.sbs"
    private const val CHECK_UPDATE_URL = "$API_BASE_URL/api/v1/app-version/check"
    
    // APK 下载地址模板（通过 gh-proxy 加速，全球可用，不消耗服务器流量）
    private const val DOWNLOAD_URL_TEMPLATE = "https://gh-proxy.org/https://github.com/shiahonb777/web-to-app/releases/download/v{VERSION}/web-to-app-{VERSION}.APK"
    
    // GitHub releases 页面（备用版本检测）
    private const val GITHUB_RELEASES_URL = "https://github.com/shiahonb777/web-to-app/releases"
    
    // 重试配置
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L
    
    // Cache配置
    private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30分钟
    
    // 自动检查更新 偏好设置
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
    private const val KEY_LAST_AUTO_CHECK_TIME = "last_auto_check_time"
    private const val AUTO_CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6小时冷却
    
    // Pre-compiled regex for fallback version extraction
    private val VERSION_REGEX = Regex("""releases/(?:tag|download)/v?(\d+\.\d+\.\d+)""")
    
    @Volatile
    private var cachedUpdateInfo: UpdateInfo? = null
    private var cacheTimestamp: Long = 0
    
    private val client get() = NetworkModule.defaultClient
    
    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 获取自动检查更新开关状态（默认开启）
     */
    fun isAutoCheckEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_CHECK_UPDATE, true)
    
    /**
     * 设置自动检查更新开关
     */
    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_CHECK_UPDATE, enabled).apply()
    }
    
    /**
     * 检查是否需要自动检查更新（冷却时间内不重复检查）
     */
    fun shouldAutoCheck(context: Context): Boolean {
        if (!isAutoCheckEnabled(context)) return false
        val lastCheck = getPrefs(context).getLong(KEY_LAST_AUTO_CHECK_TIME, 0L)
        return System.currentTimeMillis() - lastCheck > AUTO_CHECK_COOLDOWN_MS
    }
    
    /**
     * 记录自动检查时间
     */
    fun recordAutoCheck(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_AUTO_CHECK_TIME, System.currentTimeMillis()).apply()
    }
    
    /**
     * 版本更新信息
     */
    data class UpdateInfo(
        val versionName: String,      // Version名称，如 "v1.9.5"
        val versionCode: Int = 0,     // 版本号
        val downloadUrl: String,      // APK 下载链接（GitHub）
        val hasUpdate: Boolean,       // 是否有更新
        val isForceUpdate: Boolean = false, // 是否强制更新
        val title: String = "",       // 更新标题
        val releaseNotes: String = "", // 更新说明
        val fileSize: Long = 0       // 文件大小
    )
    
    /**
     * 检查更新（带缓存）
     * @param currentVersionName 当前应用的版本名称（如 "1.9.5"）
     * @param currentVersionCode 当前应用的版本号（如 10905）
     * @param forceRefresh 是否强制刷新（忽略缓存）
     * @return 更新信息，失败返回 Result.failure
     */
    suspend fun checkUpdate(
        currentVersionName: String,
        currentVersionCode: Int = 0,
        forceRefresh: Boolean = false
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        // Check缓存
        if (!forceRefresh && isCacheValid()) {
            cachedUpdateInfo?.let { cached ->
                // 重新计算 hasUpdate
                val hasUpdate = if (currentVersionCode > 0 && cached.versionCode > 0) {
                    cached.versionCode > currentVersionCode
                } else {
                    compareVersions(cached.versionName.removePrefix("v"), currentVersionName) > 0
                }
                return@withContext Result.success(cached.copy(hasUpdate = hasUpdate))
            }
        }
        
        // 带重试的请求：优先用服务器 API，失败后 fallback 到 GitHub
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                // 首选：自有服务器 API（几乎不消耗流量）
                val result = fetchFromServerApi(currentVersionName, currentVersionCode)
                if (result.isSuccess) {
                    result.getOrNull()?.let { info ->
                        cachedUpdateInfo = info
                        cacheTimestamp = System.currentTimeMillis()
                    }
                    return@withContext result
                }
                lastException = result.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
                AppLogger.w(TAG, "服务器 API 检查更新失败 (尝试 ${attempt + 1}/$MAX_RETRIES)", e)
            }
            
            if (attempt < MAX_RETRIES - 1) {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        
        // Fallback：GitHub releases 页面抓取
        AppLogger.i(TAG, "服务器 API 不可用，尝试 GitHub fallback")
        try {
            val fallbackResult = fetchFromGitHub(currentVersionName)
            if (fallbackResult.isSuccess) {
                fallbackResult.getOrNull()?.let { info ->
                    cachedUpdateInfo = info
                    cacheTimestamp = System.currentTimeMillis()
                }
                return@withContext fallbackResult
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "GitHub fallback 也失败了", e)
        }
        
        Result.failure(lastException ?: Exception("检查更新失败"))
    }
    
    /**
     * 通过自有服务器 API 检查更新
     * 流量消耗：约 1KB（JSON 响应）
     */
    private fun fetchFromServerApi(currentVersionName: String, currentVersionCode: Int): Result<UpdateInfo> {
        return try {
            val vCode = if (currentVersionCode > 0) currentVersionCode else parseVersionToCode(currentVersionName)
            
            val request = Request.Builder()
                .url("$CHECK_UPDATE_URL?current_version_code=$vCode")
                .header("User-Agent", "WebToApp-Android")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("服务器返回: ${response.code}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            
            if (data == null || !data.has("has_update") || !data.get("has_update").asBoolean) {
                // 没有更新
                return Result.success(UpdateInfo(
                    versionName = "v$currentVersionName",
                    versionCode = vCode,
                    downloadUrl = "",
                    hasUpdate = false
                ))
            }
            
            // 有更新
            val latest = data.getAsJsonObject("latest_version") ?: data
            val serverVersionName = latest.get("version_name")?.asString ?: ""
            val serverDownloadUrl = latest.get("download_url")?.asString ?: ""
            
            // 如果服务器返回的下载链接是 GitHub 链接，直接用；
            // 否则构建 GitHub 下载链接以节省服务器流量
            val downloadUrl = if (serverDownloadUrl.contains("github.com")) {
                serverDownloadUrl
            } else {
                buildGitHubDownloadUrl(serverVersionName.removePrefix("v"))
            }
            
            Result.success(UpdateInfo(
                versionName = if (serverVersionName.startsWith("v")) serverVersionName else "v$serverVersionName",
                versionCode = latest.get("version_code")?.asInt ?: parseVersionToCode(serverVersionName),
                downloadUrl = downloadUrl,
                hasUpdate = true,
                isForceUpdate = data.get("is_force")?.asBoolean 
                    ?: latest.get("is_force_update")?.asBoolean ?: false,
                title = latest.get("title")?.asString ?: "",
                releaseNotes = latest.get("changelog")?.asString ?: "",
                fileSize = latest.get("file_size")?.asLong ?: 0
            ))
        } catch (e: Exception) {
            AppLogger.e(TAG, "服务器 API 检查失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fallback：通过 GitHub releases 页面抓取版本（备用方案）
     */
    private fun fetchFromGitHub(currentVersionName: String): Result<UpdateInfo> {
        return try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("GitHub 请求失败: ${response.code}"))
            }
            
            val html = response.body?.string() ?: ""
            val latestVersion = extractLatestVersion(html)
            if (latestVersion.isEmpty()) {
                return Result.failure(Exception("未找到版本信息"))
            }
            
            val hasUpdate = compareVersions(latestVersion, currentVersionName) > 0
            val downloadUrl = buildGitHubDownloadUrl(latestVersion)
            
            Result.success(UpdateInfo(
                versionName = "v$latestVersion",
                versionCode = parseVersionToCode(latestVersion),
                downloadUrl = downloadUrl,
                hasUpdate = hasUpdate
            ))
        } catch (e: Exception) {
            AppLogger.e(TAG, "GitHub fallback 检查失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        return cachedUpdateInfo != null && 
               System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cachedUpdateInfo = null
        cacheTimestamp = 0
    }
    
    /**
     * 从 releases 页面提取最新版本号（fallback 用）
     */
    private fun extractLatestVersion(html: String): String {
        var latestVersion = ""
        var latestVersionCode = 0
        
        VERSION_REGEX.findAll(html).forEach { match ->
            val version = match.groupValues[1]
            val versionCode = parseVersionToCode(version)
            if (versionCode > latestVersionCode) {
                latestVersionCode = versionCode
                latestVersion = version
            }
        }
        
        AppLogger.d(TAG, "检测到最新版本: $latestVersion")
        return latestVersion
    }
    
    /**
     * 构建 GitHub 下载地址（不消耗服务器流量）
     */
    private fun buildGitHubDownloadUrl(version: String): String {
        return DOWNLOAD_URL_TEMPLATE.replace("{VERSION}", version)
    }
    
    /**
     * 比较两个版本号
     */
    fun compareVersions(v1: String, v2: String): Int {
        val code1 = parseVersionToCode(v1)
        val code2 = parseVersionToCode(v2)
        return code1 - code2
    }
    
    /**
     * 将版本号转换为数字便于比较
     * 支持格式: "1.5.0", "v1.5.0", "1.5", "v1.5"
     * 转换规则: major * 10000 + minor * 100 + patch
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
            val safeUrl = normalizeExternalIntentUrl(downloadUrl)
            if (safeUrl.isEmpty() || !isAllowedUrlScheme(safeUrl, setOf("http", "https"))) {
                AppLogger.w(TAG, "Blocked non-http(s) update download URL: $downloadUrl")
                return -1
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            val fileName = "WebToApp_${versionName}.APK"
            
            val request = DownloadManager.Request(Uri.parse(safeUrl))
                .setTitle("WebToApp 更新")
                .setDescription("正在下载 $versionName ...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载失败", e)
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
            AppLogger.e(TAG, "Installation failed", e)
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
