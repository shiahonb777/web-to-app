package com.webtoapp.util

import android.app.DownloadManager
import com.webtoapp.core.i18n.Strings
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













object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"


    private const val API_BASE_URL = "https://api.shiaho.sbs"
    private const val CHECK_UPDATE_URL = "$API_BASE_URL/api/v1/app-version/check"


    private const val DOWNLOAD_URL_TEMPLATE = "https://gh-proxy.org/https://github.com/shiahonb777/web-to-app/releases/download/v{VERSION}/web-to-app-{VERSION}.APK"


    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L


    private const val CACHE_TTL_MS = 30 * 60 * 1000L


    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
    private const val KEY_LAST_AUTO_CHECK_TIME = "last_auto_check_time"
    private const val AUTO_CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L

    @Volatile
    private var cachedUpdateInfo: UpdateInfo? = null
    private var cacheTimestamp: Long = 0

    private val client get() = NetworkModule.defaultClient

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)




    fun isAutoCheckEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_CHECK_UPDATE, true)




    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_CHECK_UPDATE, enabled).apply()
    }




    fun shouldAutoCheck(context: Context): Boolean {
        if (!isAutoCheckEnabled(context)) return false
        val lastCheck = getPrefs(context).getLong(KEY_LAST_AUTO_CHECK_TIME, 0L)
        return System.currentTimeMillis() - lastCheck > AUTO_CHECK_COOLDOWN_MS
    }




    fun recordAutoCheck(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_AUTO_CHECK_TIME, System.currentTimeMillis()).apply()
    }




    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int = 0,
        val downloadUrl: String,
        val hasUpdate: Boolean,
        val isForceUpdate: Boolean = false,
        val title: String = "",
        val releaseNotes: String = "",
        val fileSize: Long = 0
    )








    suspend fun checkUpdate(
        currentVersionName: String,
        currentVersionCode: Int = 0,
        forceRefresh: Boolean = false
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {

        if (!forceRefresh && isCacheValid()) {
            cachedUpdateInfo?.let { cached ->

                val hasUpdate = if (currentVersionCode > 0 && cached.versionCode > 0) {
                    cached.versionCode > currentVersionCode
                } else {
                    compareVersions(cached.versionName.removePrefix("v"), currentVersionName) > 0
                }
                return@withContext Result.success(cached.copy(hasUpdate = hasUpdate))
            }
        }


        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {

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

        Result.failure(lastException ?: Exception("检查更新失败"))
    }





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
                return Result.failure(Exception(Strings.updateServerError.format(response.code)))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")

            if (data == null || !data.has("has_update") || !data.get("has_update").asBoolean) {

                return Result.success(UpdateInfo(
                    versionName = "v$currentVersionName",
                    versionCode = vCode,
                    downloadUrl = "",
                    hasUpdate = false
                ))
            }


            val latest = data.getAsJsonObject("latest_version") ?: data
            val serverVersionName = latest.get("version_name")?.asString ?: ""
            val serverDownloadUrl = latest.get("download_url")?.asString ?: ""



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




    private fun isCacheValid(): Boolean {
        return cachedUpdateInfo != null &&
               System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS
    }




    fun clearCache() {
        cachedUpdateInfo = null
        cacheTimestamp = 0
    }




    private fun buildGitHubDownloadUrl(version: String): String {
        return DOWNLOAD_URL_TEMPLATE.replace("{VERSION}", version)
    }




    fun compareVersions(v1: String, v2: String): Int {
        val code1 = parseVersionToCode(v1)
        val code2 = parseVersionToCode(v2)
        return code1 - code2
    }






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
