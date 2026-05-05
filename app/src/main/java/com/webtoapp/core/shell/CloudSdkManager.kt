package com.webtoapp.core.shell

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale
import java.util.UUID













class CloudSdkManager(
    private val context: Context,
    private val config: CloudSdkConfig
) {
    companion object {
        private const val TAG = "CloudSDK"
        private const val PREFS_NAME = "cloud_sdk_prefs"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_LAST_STATS_REPORT = "last_stats_report"
        private const val KEY_SEEN_ANNOUNCEMENTS = "seen_announcements"
        private const val KEY_ACTIVATION_VERIFIED = "activation_verified"
        private const val KEY_ACTIVATION_CODE = "activation_code"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_REMOTE_CONFIG = "remote_config_cache"
        private const val KEY_REMOTE_SCRIPTS = "remote_scripts_cache"
        private const val KEY_OPEN_COUNT = "open_count"
        private const val KEY_CRASH_COUNT = "crash_count"
        private const val KEY_LAST_HEARTBEAT = "last_heartbeat"
        private const val CONNECT_TIMEOUT = 10_000
        private const val READ_TIMEOUT = 15_000
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activity: Activity? = null
    private var updateManager: AppUpdateManager? = null


    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }








    fun initialize(activity: Activity) {
        if (!config.isValid()) {
            AppLogger.d(TAG, "Cloud SDK disabled or invalid config, skipping init")
            return
        }

        this.activity = activity
        AppLogger.i(TAG, "Cloud SDK initializing for runtime: ${config.resolvedRuntimeKey()}")


        val openCount = prefs.getInt(KEY_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_OPEN_COUNT, openCount).apply()


        if (config.reportCrashes) {
            installCrashHandler()
        }


        scope.launch {
            val jobs = mutableListOf<Job>()


            if (config.updateCheckEnabled) {
                jobs += launch { checkUpdateIfNeeded() }
            }


            if (config.announcementEnabled) {
                jobs += launch { fetchAndShowAnnouncements() }
            }


            if (config.remoteConfigEnabled) {
                jobs += launch { fetchRemoteConfig() }
            }


            if (config.statsReportEnabled) {
                jobs += launch { reportStatsIfNeeded() }
            }


            if (config.remoteScriptEnabled) {
                jobs += launch { fetchRemoteScripts() }
            }

            jobs += launch { sendHeartbeatIfNeeded() }


            jobs.joinAll()
            AppLogger.i(TAG, "Cloud SDK initialization complete")
        }
    }





    fun isActivated(): Boolean {
        if (!config.isValid() || !config.activationCodeEnabled) return true
        return prefs.getBoolean(KEY_ACTIVATION_VERIFIED, false)
    }




    suspend fun verifyActivationCode(code: String): ActivationResult {
        if (!config.isValid()) return ActivationResult(false, "SDK not configured")

        return withContext(Dispatchers.IO) {
            try {
                val encodedCode = URLEncoder.encode(code, "UTF-8")
                val deviceParam = if (config.activationBindDevice) "&device_id=${URLEncoder.encode(deviceId, "UTF-8")}" else ""
                val url = config.getSdkApiUrl("verify-activation") + "?code=$encodedCode$deviceParam"
                val response = httpPost(url, "")

                if (response.statusCode == 200) {
                    val json = JSONObject(response.body)
                    val data = json.optJSONObject("data") ?: json
                    val valid = data.optBoolean("valid", false)
                    val message = data.optString("message", json.optString("message", ""))

                    if (valid) {
                        prefs.edit().putBoolean(KEY_ACTIVATION_VERIFIED, true).apply()
                        prefs.edit().putString(KEY_ACTIVATION_CODE, code).apply()
                    }

                    ActivationResult(valid, message)
                } else {
                    val json = runCatching { JSONObject(response.body) }.getOrNull()
                    ActivationResult(false, json?.optString("detail") ?: "Verification failed")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Activation verify failed", e)
                ActivationResult(false, "Network error: ${e.message}")
            }
        }
    }




    fun getConfigValue(key: String, default: String = ""): String {
        val cache = prefs.getString(KEY_REMOTE_CONFIG, null) ?: return default
        return try {
            val json = JSONObject(cache)
            val value = json.opt(key)
            when (value) {
                null, JSONObject.NULL -> default
                is JSONObject -> {
                    when {
                        value.has("value") && !value.isNull("value") -> value.opt("value")?.toString() ?: default
                        value.has("config_value") && !value.isNull("config_value") -> value.opt("config_value")?.toString() ?: default
                        else -> value.toString()
                    }
                }
                else -> value.toString()
            }
        } catch (e: Exception) {
            default
        }
    }




    fun destroy() {
        scope.cancel()
        updateManager?.destroy()
        updateManager = null
        activity = null
    }





    private suspend fun checkUpdateIfNeeded() {
        val now = System.currentTimeMillis() / 1000
        val lastCheck = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0)

        if (now - lastCheck < config.updateCheckInterval) {
            AppLogger.d(TAG, "Update check skipped (interval not reached)")
            return
        }

        try {
            val versionCode = getAppVersionCode()
            val url = config.getSdkApiUrl("check-update") + "?current_version=$versionCode"
            val response = httpGet(url)

            prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, now).apply()

            if (response.statusCode == 200) {
                val json = JSONObject(response.body)
                val hasUpdate = json.optBoolean("has_update", false)

                if (hasUpdate) {

                    val latest = json.optJSONObject("latest")
                    if (latest != null) {
                        val latestVersion = latest.optString("version_name", "")
                        val changelog = latest.optString("changelog", "")
                        val isForce = latest.optBoolean("is_force_update", false)


                        val downloadUrls = latest.optJSONObject("download_urls")
                        val downloadUrl = downloadUrls?.optString("r2", "")?.ifBlank {
                            downloadUrls.optString("github", "").ifBlank {
                                downloadUrls.optString("gitee", "")
                            }
                        } ?: ""

                        if (downloadUrl.isNotBlank()) {
                            showUpdateDialog(latestVersion, changelog, downloadUrl, isForce)
                        }
                    } else {

                        val latestVersion = json.optString("latest_version_name", "")
                        val changelog = json.optString("changelog", "")
                        val downloadUrlGithub = json.optString("download_url_github", "")
                        val downloadUrlGitee = json.optString("download_url_gitee", "")
                        val isForce = json.optBoolean("is_force_update", false)
                        val downloadUrl = downloadUrlGithub.ifBlank { downloadUrlGitee }

                        if (downloadUrl.isNotBlank()) {
                            showUpdateDialog(latestVersion, changelog, downloadUrl, isForce)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Update check failed: ${e.message}")
        }
    }

    private fun showUpdateDialog(version: String, changelog: String, downloadUrl: String, isForce: Boolean) {
        val act = activity ?: return
        act.runOnUiThread {
            try {
                val title = config.updateDialogTitle.ifBlank { Strings.cloudSdkNewVersionTitle.format(version) }
                val message = if (changelog.isNotBlank()) changelog else Strings.cloudSdkNewVersionMessage
                val buttonText = config.updateDialogButtonText.ifBlank { Strings.cloudSdkUpdateNow }

                val builder = AlertDialog.Builder(act)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(buttonText) { _, _ ->
                        if (config.inAppDownload) {

                            try {
                                if (updateManager == null) {
                                    updateManager = AppUpdateManager(context)
                                }
                                updateManager?.downloadAndInstall(
                                    downloadUrl = downloadUrl,
                                    versionName = version,
                                    showNotification = config.showDownloadNotification,
                                    autoInstall = config.autoInstallAfterDownload
                                )
                                AppLogger.i(TAG, "In-app download started for v$version")
                            } catch (e: Exception) {
                                AppLogger.e(TAG, "In-app download failed, falling back to browser", e)
                                openInBrowser(act, downloadUrl)
                            }
                        } else {

                            openInBrowser(act, downloadUrl)
                        }
                    }

                if (!isForce) {
                    builder.setNegativeButton(Strings.cloudSdkLater) { d, _ -> d.dismiss() }
                }
                builder.setCancelable(!isForce)
                builder.show()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to show update dialog", e)
            }
        }
    }




    private fun openInBrowser(act: Activity, url: String) {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            )
            act.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to open download URL in browser", e)
        }
    }





    private suspend fun fetchAndShowAnnouncements() {
        try {
            val lang = Locale.getDefault().language.let { if (it == "zh") "zh" else "en" }
            val url = config.getSdkApiUrl("announcements") + "?lang=$lang"
            val response = httpGet(url)

            if (response.statusCode == 200) {
                val json = JSONObject(response.body)
                val announcements = json.optJSONArray("announcements") ?: return

                if (announcements.length() == 0) return

                val seenIds = prefs.getStringSet(KEY_SEEN_ANNOUNCEMENTS, emptySet())?.toMutableSet()
                    ?: mutableSetOf()

                for (i in 0 until announcements.length()) {
                    val ann = announcements.getJSONObject(i)
                    val id = ann.optString("id", "")
                    val title = ann.optString("title", "")
                    val content = ann.optString("content", "")

                    if (id.isBlank() || title.isBlank()) continue


                    if (config.announcementShowOnce && seenIds.contains(id)) continue


                    showAnnouncementDialog(title, content)

                    seenIds.add(id)
                    prefs.edit().putStringSet(KEY_SEEN_ANNOUNCEMENTS, seenIds).apply()


                    break
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Announcement fetch failed: ${e.message}")
        }
    }

    private fun showAnnouncementDialog(title: String, content: String) {
        val act = activity ?: return
        act.runOnUiThread {
            try {
                AlertDialog.Builder(act)
                    .setTitle(title)
                    .setMessage(content)
                    .setPositiveButton(Strings.cloudSdkGotIt) { d, _ -> d.dismiss() }
                    .setCancelable(true)
                    .show()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to show announcement", e)
            }
        }
    }





    private suspend fun fetchRemoteConfig() {
        try {
            val url = config.getSdkApiUrl("config")
            val response = httpGet(url)

            if (response.statusCode == 200) {
                val json = JSONObject(response.body)
                val configMap = JSONObject()

                val nestedConfigs = json.optJSONObject("data")?.optJSONObject("configs")
                    ?: json.optJSONObject("config")
                    ?: json.optJSONObject("configs")
                if (nestedConfigs != null) {
                    val keys = nestedConfigs.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val rawValue = nestedConfigs.opt(key)
                        val value = when (rawValue) {
                            null, JSONObject.NULL -> ""
                            is JSONObject -> rawValue.opt("value")?.toString()
                                ?: rawValue.opt("config_value")?.toString()
                                ?: rawValue.toString()
                            else -> rawValue.toString()
                        }
                        if (key.isNotBlank()) {
                            configMap.put(key, value)
                        }
                    }
                } else {
                    val configs = json.optJSONArray("configs") ?: json.optJSONArray("data") ?: return
                    for (i in 0 until configs.length()) {
                        val item = configs.getJSONObject(i)
                        val key = item.optString("key", "")
                        val value = item.optString("value", "")
                        if (key.isNotBlank()) {
                            configMap.put(key, value)
                        }
                    }
                }

                prefs.edit().putString(KEY_REMOTE_CONFIG, configMap.toString()).apply()
                AppLogger.d(TAG, "Remote config cached: ${configMap.length()} keys")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Remote config fetch failed: ${e.message}")
        }
    }





    private suspend fun reportStatsIfNeeded() {
        val now = System.currentTimeMillis() / 1000
        val lastReport = prefs.getLong(KEY_LAST_STATS_REPORT, 0)

        if (now - lastReport < config.statsReportInterval) return

        try {
            val openCount = prefs.getInt(KEY_OPEN_COUNT, 0)
            val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)

            val params = buildString {
                append("?installs=0")
                append("&opens=$openCount")
                append("&active=1")
                append("&device_id=$deviceId")
                append("&device_model=${Build.MODEL}")
                append("&os_version=Android ${Build.VERSION.RELEASE}")
                append("&app_version=${getAppVersionCode()}")
                append("&country=${Locale.getDefault().country}")
                append("&crash=$crashCount")
            }

            val url = config.getSdkApiUrl("stats") + params
            val response = httpPost(url, "")

            if (response.statusCode == 200) {

                prefs.edit()
                    .putLong(KEY_LAST_STATS_REPORT, now)
                    .putInt(KEY_OPEN_COUNT, 0)
                    .putInt(KEY_CRASH_COUNT, 0)
                    .apply()
                AppLogger.d(TAG, "Stats reported successfully")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Stats report failed: ${e.message}")
        }
    }

    private suspend fun sendHeartbeatIfNeeded() {
        val now = System.currentTimeMillis() / 1000
        val lastHeartbeat = prefs.getLong(KEY_LAST_HEARTBEAT, 0)
        if (now - lastHeartbeat < 60) return
        try {
            val params = buildString {
                append("?device_id=${URLEncoder.encode(deviceId, "UTF-8")}")
                val activationCode = prefs.getString(KEY_ACTIVATION_CODE, null).orEmpty()
                if (activationCode.isNotBlank()) {
                    append("&activation_code=${URLEncoder.encode(activationCode, "UTF-8")}")
                }
                append("&ip_address=")
                append("&device_model=${Build.MODEL}")
                append("&os_version=Android ${Build.VERSION.RELEASE}")
                append("&app_version=${getAppVersionCode()}")
            }
            val response = httpPost(config.getSdkApiUrl("heartbeat") + params, "")
            if (response.statusCode in 200..299) {
                prefs.edit().putLong(KEY_LAST_HEARTBEAT, now).apply()
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Heartbeat failed: ${e.message}")
        }
    }





    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val count = prefs.getInt(KEY_CRASH_COUNT, 0) + 1
            prefs.edit().putInt(KEY_CRASH_COUNT, count).apply()
            AppLogger.e(TAG, "Uncaught exception recorded (total: $count)", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }





    private fun getAppVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun httpGet(urlStr: String): HttpResponse {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", "WebToApp-SDK/1.0")
        conn.setRequestProperty("Accept", "application/json")

        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            HttpResponse(code, body)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPost(urlStr: String, body: String): HttpResponse {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.doOutput = true
        conn.setRequestProperty("User-Agent", "WebToApp-SDK/1.0")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        return try {
            if (body.isNotEmpty()) {
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val respBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
            HttpResponse(code, respBody)
        } finally {
            conn.disconnect()
        }
    }

    private data class HttpResponse(val statusCode: Int, val body: String)








    private fun fetchRemoteScripts() {
        try {
            val apiUrl = config.getSdkApiUrl("remote-scripts")
            val resp = httpGet(apiUrl)
            if (resp.statusCode in 200..299) {
                prefs.edit().putString(KEY_REMOTE_SCRIPTS, resp.body).apply()
                AppLogger.i(TAG, "Remote scripts fetched and cached")
            } else {
                AppLogger.w(TAG, "Failed to fetch remote scripts: ${resp.statusCode}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Remote scripts fetch error", e)
        }
    }







    fun getRemoteScriptCode(runAt: String, url: String = ""): String {
        val cached = prefs.getString(KEY_REMOTE_SCRIPTS, null) ?: return ""
        return try {
            val json = JSONObject(cached)
            val scripts = json.optJSONArray("scripts") ?: return ""
            val code = StringBuilder()
            for (i in 0 until scripts.length()) {
                val script = scripts.getJSONObject(i)
                val scriptRunAt = script.optString("run_at", "document_end")
                if (scriptRunAt != runAt) continue
                val pattern = script.optString("url_pattern", "")
                if (pattern.isNotBlank() && url.isNotBlank()) {

                    val regex = pattern.replace(".", "\\.").replace("*", ".*")
                    if (!url.matches(Regex(regex, RegexOption.IGNORE_CASE))) continue
                }
                val name = script.optString("name", "unknown")
                val scriptCode = script.optString("code", "")
                if (scriptCode.isNotBlank()) {
                    code.append("// ===== Remote Script: $name =====\n")
                    code.append("(function(){try{\n")
                    code.append(scriptCode)
                    code.append("\n}catch(e){console.error('[RemoteScript]$name:',e);}})();\n\n")
                }
            }
            code.toString()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse remote scripts", e)
            ""
        }
    }
}




data class ActivationResult(
    val success: Boolean,
    val message: String
)
