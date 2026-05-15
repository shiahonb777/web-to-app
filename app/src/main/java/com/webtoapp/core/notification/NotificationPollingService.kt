package com.webtoapp.core.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.Strings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import java.net.HttpURLConnection
import java.net.URL










class NotificationPollingService : Service() {

    companion object {
        private const val TAG = "NotificationPollingService"
        private const val CHANNEL_ID = "polling_notification_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 10002
        private const val MIN_INTERVAL_MINUTES = 5
        private const val PREFS_NAME = "polling_notification_config"
        private const val ACTION_RESTART = "com.webtoapp.action.RESTART_POLLING_NOTIFICATION"
        private const val RESTART_REQUEST_CODE = 41002
        private const val RESTART_DELAY_MS = 60_000L

        private const val EXTRA_POLL_URL = "poll_url"
        private const val EXTRA_POLL_INTERVAL = "poll_interval"
        private const val EXTRA_POLL_METHOD = "poll_method"
        private const val EXTRA_POLL_HEADERS = "poll_headers"
        private const val EXTRA_CLICK_URL = "click_url"
        private const val EXTRA_APP_NAME = "app_name"

        private const val ACTION_STOP = "com.webtoapp.action.STOP_POLLING_NOTIFICATION"

        @Volatile
        private var isRunning = false

        fun start(
            context: Context,
            appName: String = "",
            pollUrl: String,
            pollIntervalMinutes: Int = 15,
            pollMethod: String = "GET",
            pollHeaders: String = "",
            clickUrl: String = ""
        ) {
            if (isRunning) {
                AppLogger.w(TAG, "轮询通知服务已在运行")
                return
            }

            val interval = pollIntervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES)
            val intent = Intent(context, NotificationPollingService::class.java).apply {
                putExtra(EXTRA_POLL_URL, pollUrl)
                putExtra(EXTRA_POLL_INTERVAL, interval)
                putExtra(EXTRA_POLL_METHOD, pollMethod)
                putExtra(EXTRA_POLL_HEADERS, pollHeaders)
                putExtra(EXTRA_CLICK_URL, clickUrl)
                putExtra(EXTRA_APP_NAME, appName)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                AppLogger.i(TAG, "轮询通知服务启动请求已发送")
            } catch (e: Exception) {
                AppLogger.e(TAG, "启动轮询通知服务失败", e)
            }
        }

        fun stop(context: Context) {
            try {
                clearPersistedConfig(context)
                cancelRestart(context)
                context.stopService(Intent(context, NotificationPollingService::class.java))
                AppLogger.i(TAG, "轮询通知服务停止请求已发送")
            } catch (e: Exception) {
                AppLogger.e(TAG, "停止轮询通知服务失败", e)
            }
        }

        fun isServiceRunning(): Boolean = isRunning

        private fun clearPersistedConfig(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        }

        private fun persistConfig(
            context: Context,
            appName: String,
            pollUrl: String,
            pollIntervalMinutes: Int,
            pollMethod: String,
            pollHeaders: String,
            clickUrl: String
        ) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(EXTRA_APP_NAME, appName)
                .putString(EXTRA_POLL_URL, pollUrl)
                .putInt(EXTRA_POLL_INTERVAL, pollIntervalMinutes)
                .putString(EXTRA_POLL_METHOD, pollMethod)
                .putString(EXTRA_POLL_HEADERS, pollHeaders)
                .putString(EXTRA_CLICK_URL, clickUrl)
                .apply()
        }

        private fun restoreIntent(context: Context): Intent? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pollUrl = prefs.getString(EXTRA_POLL_URL, "") ?: ""
            if (pollUrl.isBlank()) return null

            return Intent(context, NotificationPollingService::class.java).apply {
                action = ACTION_RESTART
                putExtra(EXTRA_APP_NAME, prefs.getString(EXTRA_APP_NAME, "") ?: "")
                putExtra(EXTRA_POLL_URL, pollUrl)
                putExtra(EXTRA_POLL_INTERVAL, prefs.getInt(EXTRA_POLL_INTERVAL, 15))
                putExtra(EXTRA_POLL_METHOD, prefs.getString(EXTRA_POLL_METHOD, "GET") ?: "GET")
                putExtra(EXTRA_POLL_HEADERS, prefs.getString(EXTRA_POLL_HEADERS, "") ?: "")
                putExtra(EXTRA_CLICK_URL, prefs.getString(EXTRA_CLICK_URL, "") ?: "")
            }
        }

        private fun cancelRestart(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NotificationPollingService::class.java).apply { action = ACTION_RESTART }
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    RESTART_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                )
            } else {
                PendingIntent.getService(
                    context,
                    RESTART_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                )
            }
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        private fun scheduleRestart(context: Context, reason: String) {
            val restartIntent = restoreIntent(context) ?: run {
                AppLogger.w(TAG, "无可恢复配置，跳过轮询服务重启调度: $reason")
                return
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    RESTART_REQUEST_CODE,
                    restartIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    RESTART_REQUEST_CODE,
                    restartIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + RESTART_DELAY_MS,
                pendingIntent
            )
            AppLogger.i(TAG, "已调度轮询服务重启: reason=$reason, delay=${RESTART_DELAY_MS}ms")
        }
    }

    private var pollUrl: String = ""
    private var pollIntervalMinutes: Int = 15
    private var pollMethod: String = "GET"
    private var pollHeaders: String = ""
    private var clickUrl: String = ""
    private var appName: String = ""
    private var allowAutoRestart: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            performPoll()
            handler.postDelayed(this, pollIntervalMinutes * 60 * 1000L)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val activePollCount = AtomicInteger(0)

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(TAG, "轮询通知服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            allowAutoRestart = false
            cancelRestart(this)
            clearPersistedConfig(this)
            stopSelf()
            return START_NOT_STICKY
        }


        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        pollUrl = intent?.getStringExtra(EXTRA_POLL_URL)
            ?: prefs.getString(EXTRA_POLL_URL, "") ?: ""
        pollIntervalMinutes = intent?.getIntExtra(EXTRA_POLL_INTERVAL, -1)
            ?.takeIf { it > 0 }?.coerceAtLeast(MIN_INTERVAL_MINUTES)
            ?: prefs.getInt(EXTRA_POLL_INTERVAL, 15).coerceAtLeast(MIN_INTERVAL_MINUTES)
        pollMethod = intent?.getStringExtra(EXTRA_POLL_METHOD)
            ?: prefs.getString(EXTRA_POLL_METHOD, "GET") ?: "GET"
        pollHeaders = intent?.getStringExtra(EXTRA_POLL_HEADERS)
            ?: prefs.getString(EXTRA_POLL_HEADERS, "") ?: ""
        clickUrl = intent?.getStringExtra(EXTRA_CLICK_URL)
            ?: prefs.getString(EXTRA_CLICK_URL, "") ?: ""
        appName = intent?.getStringExtra(EXTRA_APP_NAME)
            ?: prefs.getString(EXTRA_APP_NAME, "") ?: ""

        if (pollUrl.isBlank()) {
            AppLogger.e(TAG, "轮询 URL 为空，服务无法启动")
            stopSelf()
            return START_NOT_STICKY
        }


        prefs.edit()
            .putString(EXTRA_POLL_URL, pollUrl)
            .putInt(EXTRA_POLL_INTERVAL, pollIntervalMinutes)
            .putString(EXTRA_POLL_METHOD, pollMethod)
            .putString(EXTRA_POLL_HEADERS, pollHeaders)
            .putString(EXTRA_CLICK_URL, clickUrl)
            .putString(EXTRA_APP_NAME, appName)
            .apply()
        persistConfig(this, appName, pollUrl, pollIntervalMinutes, pollMethod, pollHeaders, clickUrl)
        cancelRestart(this)
        allowAutoRestart = true

        try {
            val notification = createForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }
            isRunning = true
        } catch (e: Exception) {
            AppLogger.e(TAG, "startForeground 失败", e)
            stopSelf()
            return START_NOT_STICKY
        }


        handler.removeCallbacks(pollRunnable)
        handler.postDelayed(pollRunnable, 10 * 1000L)

        AppLogger.i(TAG, "轮询通知服务已启动: url=$pollUrl, interval=${pollIntervalMinutes}min")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (allowAutoRestart) {
            scheduleRestart(this, "task_removed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        releaseWakeLock()
        if (allowAutoRestart) {
            scheduleRestart(this, "service_destroyed")
        }
        isRunning = false
        AppLogger.i(TAG, "轮询通知服务已销毁")
    }

    private fun performPoll() {

        acquireWakeLock()
        activePollCount.incrementAndGet()

        Thread {
            try {
                val result = fetchPollUrl()
                if (result != null) {
                    handlePollResult(result)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "轮询请求失败", e)
            } finally {
                if (activePollCount.decrementAndGet() == 0) {
                    releaseWakeLock()
                }
            }
        }.start()
    }

    private fun fetchPollUrl(): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(pollUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = pollMethod.uppercase()
                connectTimeout = 15_000
                readTimeout = 15_000


                if (pollHeaders.isNotBlank()) {
                    try {
                        val headersObj = JSONObject(pollHeaders)
                        val keys = headersObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            setRequestProperty(key, headersObj.getString(key))
                        }
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "解析自定义 Headers 失败", e)
                    }
                }


                if (getRequestProperty("Accept") == null) {
                    setRequestProperty("Accept", "application/json")
                }
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                AppLogger.w(TAG, "轮询请求返回非 2xx: $responseCode")
                return null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "网络请求异常", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    private fun handlePollResult(jsonString: String) {
        try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty() || trimmed == "[]" || trimmed == "{}" || trimmed == "null") {
                return
            }

            val notifications = mutableListOf<PollNotification>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    notifications.add(parseNotification(obj))
                }
            } else {
                val obj = JSONObject(trimmed)
                notifications.add(parseNotification(obj))
            }


            notifications.take(5).forEachIndexed { index, notif ->
                showPollNotification(notif, baseId = index)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "解析轮询结果失败: $jsonString", e)
        }
    }

    private fun parseNotification(obj: JSONObject): PollNotification {
        return PollNotification(
            title = obj.optString("title", "").ifBlank { appName.ifBlank { Strings.genericNotificationLabel } },
            body = obj.optString("body", ""),
            url = obj.optString("url", "").ifBlank { clickUrl }
        )
    }

    private fun showPollNotification(notif: PollNotification, baseId: Int) {
        try {
            val notificationManager = NotificationManagerCompat.from(this)
            if (!notificationManager.areNotificationsEnabled()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (notif.url.isNotBlank()) {

                    putExtra("notification_click_url", notif.url)
                }
            }

            val pendingIntent = if (intent != null) {
                PendingIntent.getActivity(
                    this,
                    baseId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val iconResId = applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_menu_info_details

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(notif.title)
                .setContentText(notif.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notif.body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .apply {
                    if (pendingIntent != null) setContentIntent(pendingIntent)
                }
                .build()

            try {
                notificationManager.notify(Math.abs(("poll_${System.currentTimeMillis()}_$baseId").hashCode()), notification)
            } catch (e: SecurityException) {
                AppLogger.e(TAG, "通知权限不可用", e)
                return
            }
            AppLogger.d(TAG, "弹出轮询通知: ${notif.title}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "弹出轮询通知失败", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                Strings.pollingNotificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = Strings.pollingNotificationChannelDescription
                setShowBadge(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val stopIntent = Intent(this, NotificationPollingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconResId = applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_menu_info_details

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle((if (appName.isNotBlank()) appName else Strings.genericAppLabel) + " - " + Strings.pollingNotificationServiceTitle)
            .setContentText(Strings.pollingNotificationForegroundText.format(pollUrl, pollIntervalMinutes))
            .setSmallIcon(iconResId)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                Strings.stop,
                stopPendingIntent
            )
            .build()
    }

    private fun acquireWakeLock() {
        synchronized(this) {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "WebToApp:PollingWakeLock"
                ).apply { setReferenceCounted(false) }
            }
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(30_000L)
                }
            }
        }
    }

    private fun releaseWakeLock() {
        synchronized(this) {
            wakeLock?.let {
                if (it.isHeld) {
                    try { it.release() } catch (_: Exception) {}
                }
            }
        }
    }

    private data class PollNotification(
        val title: String,
        val body: String,
        val url: String
    )
}
