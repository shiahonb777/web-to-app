package com.webtoapp.core.notification

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
import com.webtoapp.core.logging.AppLogger
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
                context.stopService(Intent(context, NotificationPollingService::class.java))
                AppLogger.i(TAG, "轮询通知服务停止请求已发送")
            } catch (e: Exception) {
                AppLogger.e(TAG, "停止轮询通知服务失败", e)
            }
        }

        fun isServiceRunning(): Boolean = isRunning
    }

    private var pollUrl: String = ""
    private var pollIntervalMinutes: Int = 15
    private var pollMethod: String = "GET"
    private var pollHeaders: String = ""
    private var clickUrl: String = ""
    private var appName: String = ""

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
            stopSelf()
            return START_NOT_STICKY
        }


        val prefs = getSharedPreferences("polling_notification_config", MODE_PRIVATE)
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        releaseWakeLock()
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
            title = obj.optString("title", "").ifBlank { appName.ifBlank { "Notification" } },
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
                "Polling Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from polling service"
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
            .setContentTitle((if (appName.isNotBlank()) appName else "App") + " - Notification Service")
            .setContentText("Polling: $pollUrl every ${pollIntervalMinutes}min")
            .setSmallIcon(iconResId)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
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
