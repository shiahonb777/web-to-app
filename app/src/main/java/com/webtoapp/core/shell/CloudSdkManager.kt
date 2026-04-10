package com.webtoapp.core.shell

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

/**
 * Cloud SDK 运行时管理器
 *
 * 在导出的 APK (Shell 模式) 中运行，负责:
 * 1. 更新检查 — 启动时检查新版本
 * 2. 公告展示 — 获取活跃公告并弹窗展示
 * 3. 远程配置 — 下载远程 KV 配置并缓存
 * 4. 激活码验证 — 通过服务器验证激活码
 * 5. 统计上报 — 上报设备信息和使用数据
 *
 * 仅使用 android.* 和 java.* API，不依赖三方库（减少 APK 体积影响）。
 */
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
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_REMOTE_CONFIG = "remote_config_cache"
        private const val KEY_REMOTE_SCRIPTS = "remote_scripts_cache"
        private const val KEY_OPEN_COUNT = "open_count"
        private const val KEY_CRASH_COUNT = "crash_count"
        private const val CONNECT_TIMEOUT = 10_000 // 10s
        private const val READ_TIMEOUT = 15_000     // 15s
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activity: Activity? = null
    private var updateManager: AppUpdateManager? = null

    /** 当前设备唯一 ID（安装 UUID） */
    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    // ═══════════════════════════════════════════
    //  初始化（Shell 启动时调用）
    // ═══════════════════════════════════════════

    /**
     * 初始化 SDK — 在 ShellActivity.onCreate 中调用
     */
    fun initialize(activity: Activity) {
        if (!config.isValid()) {
            AppLogger.d(TAG, "Cloud SDK disabled or invalid config, skipping init")
            return
        }

        this.activity = activity
        AppLogger.i(TAG, "Cloud SDK initializing for project: ${config.projectKey}")

        // 记录一次打开
        val openCount = prefs.getInt(KEY_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_OPEN_COUNT, openCount).apply()

        // 安装未捕获异常处理器（崩溃统计）
        if (config.reportCrashes) {
            installCrashHandler()
        }

        // 并行执行各项 SDK 功能
        scope.launch {
            val jobs = mutableListOf<Job>()

            // 1. 更新检查
            if (config.updateCheckEnabled) {
                jobs += launch { checkUpdateIfNeeded() }
            }

            // 2. 公告
            if (config.announcementEnabled) {
                jobs += launch { fetchAndShowAnnouncements() }
            }

            // 3. 远程配置
            if (config.remoteConfigEnabled) {
                jobs += launch { fetchRemoteConfig() }
            }

            // 4. 统计上报
            if (config.statsReportEnabled) {
                jobs += launch { reportStatsIfNeeded() }
            }

            // 5. 远程脚本热更
            if (config.remoteScriptEnabled) {
                jobs += launch { fetchRemoteScripts() }
            }

            // 等待所有任务完成（不阻塞 Activity）
            jobs.joinAll()
            AppLogger.i(TAG, "Cloud SDK initialization complete")
        }
    }

    /**
     * 检查是否需要激活码验证（在页面显示前调用）
     * @return true = 已激活或不需要激活，false = 需要激活
     */
    fun isActivated(): Boolean {
        if (!config.isValid() || !config.activationCodeEnabled) return true
        return prefs.getBoolean(KEY_ACTIVATION_VERIFIED, false)
    }

    /**
     * 验证激活码
     */
    suspend fun verifyActivationCode(code: String): ActivationResult {
        if (!config.isValid()) return ActivationResult(false, "SDK not configured")

        return withContext(Dispatchers.IO) {
            try {
                val deviceParam = if (config.activationBindDevice) "&device_id=$deviceId" else ""
                val url = config.getSdkApiUrl("verify-code") + "?code=$code$deviceParam"
                val response = httpGet(url)

                if (response.statusCode == 200) {
                    val json = JSONObject(response.body)
                    val valid = json.optBoolean("valid", false)
                    val message = json.optString("message", "")

                    if (valid) {
                        prefs.edit().putBoolean(KEY_ACTIVATION_VERIFIED, true).apply()
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

    /**
     * 获取远程配置值
     */
    fun getConfigValue(key: String, default: String = ""): String {
        val cache = prefs.getString(KEY_REMOTE_CONFIG, null) ?: return default
        return try {
            val json = JSONObject(cache)
            json.optString(key, default)
        } catch (e: Exception) {
            default
        }
    }

    /**
     * 释放资源
     */
    fun destroy() {
        scope.cancel()
        updateManager?.destroy()
        updateManager = null
        activity = null
    }

    // ═══════════════════════════════════════════
    //  更新检查
    // ═══════════════════════════════════════════

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
                    // 服务端返回嵌套的 latest 对象
                    val latest = json.optJSONObject("latest")
                    if (latest != null) {
                        val latestVersion = latest.optString("version_name", "")
                        val changelog = latest.optString("changelog", "")
                        val isForce = latest.optBoolean("is_force_update", false)
                        
                        // 下载链接优先级: R2 > GitHub > Gitee
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
                        // 兼容旧的扁平格式
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
                val title = config.updateDialogTitle.ifBlank { "发现新版本 v$version" }
                val message = if (changelog.isNotBlank()) changelog else "有新版本可用，建议更新。"
                val buttonText = config.updateDialogButtonText.ifBlank { "立即更新" }

                val builder = AlertDialog.Builder(act)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(buttonText) { _, _ ->
                        if (config.inAppDownload) {
                            // 应用内下载 + 自动安装
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
                            // 跳转浏览器下载
                            openInBrowser(act, downloadUrl)
                        }
                    }

                if (!isForce) {
                    builder.setNegativeButton("稍后") { d, _ -> d.dismiss() }
                }
                builder.setCancelable(!isForce)
                builder.show()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to show update dialog", e)
            }
        }
    }

    /**
     * 在浏览器中打开下载链接（回退方案）
     */
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

    // ═══════════════════════════════════════════
    //  公告
    // ═══════════════════════════════════════════

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

                    // 是否只显示一次
                    if (config.announcementShowOnce && seenIds.contains(id)) continue

                    // 展示公告
                    showAnnouncementDialog(title, content)

                    seenIds.add(id)
                    prefs.edit().putStringSet(KEY_SEEN_ANNOUNCEMENTS, seenIds).apply()

                    // 只显示第一条未读公告
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
                    .setPositiveButton("知道了") { d, _ -> d.dismiss() }
                    .setCancelable(true)
                    .show()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to show announcement", e)
            }
        }
    }

    // ═══════════════════════════════════════════
    //  远程配置
    // ═══════════════════════════════════════════

    private suspend fun fetchRemoteConfig() {
        try {
            val url = config.getSdkApiUrl("config")
            val response = httpGet(url)

            if (response.statusCode == 200) {
                val json = JSONObject(response.body)
                val configs = json.optJSONArray("configs") ?: return

                val configMap = JSONObject()
                for (i in 0 until configs.length()) {
                    val item = configs.getJSONObject(i)
                    val key = item.optString("key", "")
                    val value = item.optString("value", "")
                    if (key.isNotBlank()) {
                        configMap.put(key, value)
                    }
                }

                prefs.edit().putString(KEY_REMOTE_CONFIG, configMap.toString()).apply()
                AppLogger.d(TAG, "Remote config cached: ${configMap.length()} keys")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Remote config fetch failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════
    //  统计上报
    // ═══════════════════════════════════════════

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
                // 重置计数
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

    // ═══════════════════════════════════════════
    //  崩溃捕获
    // ═══════════════════════════════════════════

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val count = prefs.getInt(KEY_CRASH_COUNT, 0) + 1
            prefs.edit().putInt(KEY_CRASH_COUNT, count).apply()
            AppLogger.e(TAG, "Uncaught exception recorded (total: $count)", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    // ═══════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════

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

    // ═══════════════════════════════════════════
    //  远程脚本热更
    // ═══════════════════════════════════════════

    /**
     * 从服务端获取远程脚本并缓存
     */
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

    /**
     * 获取缓存的远程脚本代码用于 WebView 注入
     * @param runAt 运行时机 (document_start / document_end / document_idle)
     * @param url 当前页面 URL，用于匹配 url_pattern
     * @return 拼接后的 JavaScript 代码，空字符串表示无脚本
     */
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
                    // 简单的 glob 匹配：支持 * 通配符
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

/**
 * 激活码验证结果
 */
data class ActivationResult(
    val success: Boolean,
    val message: String
)
