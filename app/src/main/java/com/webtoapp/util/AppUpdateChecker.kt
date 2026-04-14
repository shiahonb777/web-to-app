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
 * Note.
 * 
 * （）：
 * - ： API（ KB JSON ）
 * - APK ： GitHub Releases（，）
 * - ： API ，fallback GitHub releases
 * 
 * ：
 * API：~1 KB/
 * GitHub ：0 （GitHub ）
 */
object AppUpdateChecker {
    
    private const val TAG = "AppUpdateChecker"
    
    // API（， JSON，）
    private const val API_BASE_URL = "https://api.shiaho.sbs"
    private const val CHECK_UPDATE_URL = "$API_BASE_URL/api/v1/app-version/check"
    
    // APK （ gh-proxy ，，）
    private const val DOWNLOAD_URL_TEMPLATE = "https://gh-proxy.org/https://github.com/shiahonb777/web-to-app/releases/download/v{VERSION}/web-to-app-{VERSION}.APK"
    
    // GitHub releases （）
    private const val GITHUB_RELEASES_URL = "https://github.com/shiahonb777/web-to-app/releases"
    
    // Note.
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L
    
    // Cache
    private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30
    
    // Note.
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
    private const val KEY_LAST_AUTO_CHECK_TIME = "last_auto_check_time"
    private const val AUTO_CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6
    
    // Pre-compiled regex for fallback version extraction
    private val VERSION_REGEX = Regex("""releases/(?:tag|download)/v?(\d+\.\d+\.\d+)""")
    
    @Volatile
    private var cachedUpdateInfo: UpdateInfo? = null
    private var cacheTimestamp: Long = 0
    
    private val client get() = NetworkModule.defaultClient
    
    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * （）
     */
    fun isAutoCheckEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_CHECK_UPDATE, true)
    
    /**
     * Note.
     */
    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_CHECK_UPDATE, enabled).apply()
    }
    
    /**
     * （）
     */
    fun shouldAutoCheck(context: Context): Boolean {
        if (!isAutoCheckEnabled(context)) return false
        val lastCheck = getPrefs(context).getLong(KEY_LAST_AUTO_CHECK_TIME, 0L)
        return System.currentTimeMillis() - lastCheck > AUTO_CHECK_COOLDOWN_MS
    }
    
    /**
     * Note.
     */
    fun recordAutoCheck(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_AUTO_CHECK_TIME, System.currentTimeMillis()).apply()
    }
    
    /**
     * Note.
     */
    data class UpdateInfo(
        val versionName: String,      // Version， "v1.9.5"
        val versionCode: Int = 0,     // Note.
        val downloadUrl: String,      // APK （GitHub）
        val hasUpdate: Boolean,       // Note.
        val isForceUpdate: Boolean = false, // Note.
        val title: String = "",       // Note.
        val releaseNotes: String = "", // Note.
        val fileSize: Long = 0       // Note.
    )
    
    /**
     * （）
     * @param currentVersionName parameter
     * @param currentVersionCode parameter
     * @param forceRefresh parameter
     * @return result
     */
    suspend fun checkUpdate(
        currentVersionName: String,
        currentVersionCode: Int = 0,
        forceRefresh: Boolean = false
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        // Check
        if (!forceRefresh && isCacheValid()) {
            cachedUpdateInfo?.let { cached ->
                // hasUpdate
                val hasUpdate = if (currentVersionCode > 0 && cached.versionCode > 0) {
                    cached.versionCode > currentVersionCode
                } else {
                    compareVersions(cached.versionName.removePrefix("v"), currentVersionName) > 0
                }
                return@withContext Result.success(cached.copy(hasUpdate = hasUpdate))
            }
        }
        
        // ： API， fallback GitHub
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                // ： API（）
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
        
        // Fallback：GitHub releases
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
     * API
     * ： 1KB（JSON ）
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
                // Note.
                return Result.success(UpdateInfo(
                    versionName = "v$currentVersionName",
                    versionCode = vCode,
                    downloadUrl = "",
                    hasUpdate = false
                ))
            }
            
            // Note.
            val latest = data.getAsJsonObject("latest_version") ?: data
            val serverVersionName = latest.get("version_name")?.asString ?: ""
            val serverDownloadUrl = latest.get("download_url")?.asString ?: ""
            
            // GitHub ，；
            // GitHub
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
     * Fallback： GitHub releases （）
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
     * Note.
     */
    private fun isCacheValid(): Boolean {
        return cachedUpdateInfo != null && 
               System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS
    }
    
    /**
     * Note.
     */
    fun clearCache() {
        cachedUpdateInfo = null
        cacheTimestamp = 0
    }
    
    /**
     * releases （fallback ）
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
     * GitHub （）
     */
    private fun buildGitHubDownloadUrl(version: String): String {
        return DOWNLOAD_URL_TEMPLATE.replace("{VERSION}", version)
    }
    
    /**
     * Note.
     */
    fun compareVersions(v1: String, v2: String): Int {
        val code1 = parseVersionToCode(v1)
        val code2 = parseVersionToCode(v2)
        return code1 - code2
    }
    
    /**
     * Note.
     * : "1.5.0", "v1.5.0", "1.5", "v1.5"
     * : major * 10000 + minor * 100 + patch
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
     * DownloadManager APK
     * @return result
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
     * APK
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
     * Note.
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
