package com.webtoapp.core.forcedrun

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent













class ForcedRunAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ForcedRunA11yService"


        @Volatile
        private var instance: ForcedRunAccessibilityService? = null

        @Volatile
        var isServiceRunning = false
            private set


        @Volatile
        var isForcedRunActive = false

        @Volatile
        var targetPackageName: String? = null

        @Volatile
        var targetActivityClass: String? = null

        @Volatile
        var bringBackDelay: Long = 50L

        @Volatile
        var allowedPackages: Set<String> = emptySet()




        @Volatile
        var blockVolumeKeys: Boolean = false


        @Volatile
        var blockBackKey: Boolean = false


        @Volatile
        var blockTouchOverlay: Boolean = false
            set(value) {
                field = value
                instance?.updateTouchBlockOverlay(value)
            }


        @Volatile
        var blockPowerKey: Boolean = false


        @Volatile
        var blackScreenMode: Boolean = false
            set(value) {
                field = value
                if (value) {
                    blockTouchOverlay = true
                    instance?.updateBlackScreenOverlay(true)
                    instance?.setSystemBrightness(0)
                } else {
                    instance?.updateBlackScreenOverlay(false)
                    instance?.restoreSystemBrightness()
                }
            }




        fun startForcedRun(
            packageName: String,
            activityClass: String,
            allowedPkgs: Set<String> = emptySet()
        ) {
            AppLogger.d(TAG, "启动强制运行防护: package=$packageName, activity=$activityClass")
            targetPackageName = packageName
            targetActivityClass = activityClass
            allowedPackages = allowedPkgs + setOf(
                packageName,
                "com.android.systemui",
            )
            isForcedRunActive = true
        }




        fun stopForcedRun() {
            AppLogger.d(TAG, "停止强制运行防护")
            isForcedRunActive = false
            targetPackageName = null
            targetActivityClass = null
            allowedPackages = emptySet()
        }




        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${ForcedRunAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(':').any {
                it.equals(serviceName, ignoreCase = true) ||
                it.contains(ForcedRunAccessibilityService::class.java.simpleName, ignoreCase = true)
            }
        }




        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }













        fun setSystemBrightnessGlobal(context: Context, brightness: Int): Boolean {
            return try {
                if (!Settings.System.canWrite(context)) {
                    AppLogger.w(TAG, "无 WRITE_SETTINGS 权限，无法控制系统亮度")
                    return false
                }

                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )

                val clamped = brightness.coerceIn(0, 255)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    clamped
                )
                AppLogger.d(TAG, "系统亮度已设置: $clamped")
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "设置系统亮度失败", e)
                false
            }
        }




        fun requestWriteSettingsPermission(context: Context) {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }




        fun stopAllBlackTech() {
            blockVolumeKeys = false
            blockBackKey = false
            blockTouchOverlay = false
            blockPowerKey = false
            blackScreenMode = false
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBringBackTime = 0L
    private var consecutiveBringBacks = 0
    private val bringBackRunnable = Runnable { bringAppToFront() }


    private var touchBlockView: View? = null
    private var windowManager: WindowManager? = null


    private var screenOffReceiver: BroadcastReceiver? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null


    private var originalBrightness: Int = -1
    private var originalBrightnessMode: Int = -1

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLogger.d(TAG, "辅助功能服务已连接")

        instance = this
        isServiceRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager


        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                   AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 50
        } ?: AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 50
        }


        registerScreenOffReceiver()
    }











    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        val keyCode = event.keyCode


        if (blockVolumeKeys) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                AppLogger.d(TAG, "拦截音量键: keyCode=$keyCode action=${event.action}")
                return true
            }
        }


        if (blockBackKey) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                AppLogger.d(TAG, "拦截返回键: action=${event.action}")
                return true
            }
        }




        return false
    }












    @Suppress("DEPRECATION")
    private fun updateTouchBlockOverlay(enable: Boolean) {
        handler.post {
            if (enable && touchBlockView == null) {
                try {
                    val view = View(this).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnTouchListener { _, _ -> true }
                    }

                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        else
                            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.START
                    }

                    windowManager?.addView(view, params)
                    touchBlockView = view
                    AppLogger.d(TAG, "触摸屏蔽覆盖层已添加")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "添加触摸屏蔽覆盖层失败", e)
                }
            } else if (!enable && touchBlockView != null) {
                removeTouchBlockOverlay()
            }
        }
    }




    @Suppress("DEPRECATION")
    private fun updateBlackScreenOverlay(enable: Boolean) {
        handler.post {
            if (enable) {

                removeTouchBlockOverlay()

                try {
                    val view = View(this).apply {
                        setBackgroundColor(Color.BLACK)
                        setOnTouchListener { _, _ -> true }
                    }

                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        else
                            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.OPAQUE
                    ).apply {
                        gravity = Gravity.TOP or Gravity.START
                    }

                    windowManager?.addView(view, params)
                    touchBlockView = view
                    AppLogger.d(TAG, "全黑屏覆盖层已添加")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "添加全黑屏覆盖层失败", e)
                }
            } else {
                removeTouchBlockOverlay()
            }
        }
    }

    private fun removeTouchBlockOverlay() {
        touchBlockView?.let { view ->
            try {
                windowManager?.removeView(view)
                AppLogger.d(TAG, "覆盖层已移除")
            } catch (e: Exception) {
                AppLogger.e(TAG, "移除覆盖层失败", e)
            }
        }
        touchBlockView = null
    }











    private fun registerScreenOffReceiver() {
        if (screenOffReceiver != null) return

        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!blockPowerKey) return
                if (intent?.action != Intent.ACTION_SCREEN_OFF) return

                AppLogger.d(TAG, "检测到屏幕关闭（电源键），立即唤醒")


                wakeUpScreen()
            }
        }

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }

        AppLogger.d(TAG, "屏幕关闭广播接收器已注册")
    }




    @Suppress("DEPRECATION")
    private fun wakeUpScreen() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }

            wakeLock = powerManager?.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "WebToApp:PowerKeyBlock"
            )
            wakeLock?.acquire(5000L)

            AppLogger.d(TAG, "屏幕已强制唤醒")
        } catch (e: Exception) {
            AppLogger.e(TAG, "强制唤醒屏幕失败", e)
        }
    }






    private fun setSystemBrightness(brightness: Int) {
        try {
            if (!Settings.System.canWrite(this)) {
                AppLogger.w(TAG, "无 WRITE_SETTINGS 权限")
                return
            }


            if (originalBrightness == -1) {
                originalBrightness = Settings.System.getInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                originalBrightnessMode = Settings.System.getInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                )
            }


            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )

            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness.coerceIn(0, 255)
            )

            AppLogger.d(TAG, "系统亮度已设置: $brightness (原值: $originalBrightness)")
        } catch (e: Exception) {
            AppLogger.e(TAG, "设置系统亮度失败", e)
        }
    }




    private fun restoreSystemBrightness() {
        try {
            if (originalBrightness == -1) return
            if (!Settings.System.canWrite(this)) return

            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                originalBrightnessMode
            )
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                originalBrightness
            )

            AppLogger.d(TAG, "系统亮度已恢复: $originalBrightness")
            originalBrightness = -1
            originalBrightnessMode = -1
        } catch (e: Exception) {
            AppLogger.e(TAG, "恢复系统亮度失败", e)
        }
    }



    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isForcedRunActive || event == null) return

        val eventPackage = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(eventPackage, event)
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (shouldBringBack(eventPackage)) {
                    scheduleBringBack()
                }
            }
            else -> Unit
        }
    }

    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""

        AppLogger.i(TAG, "窗口变化: package=$packageName, class=$className")

        if (shouldBringBack(packageName)) {
            AppLogger.d(TAG, "检测到离开应用: $packageName, 准备拉回")
            scheduleBringBack()
        }
    }

    private fun shouldBringBack(currentPackage: String): Boolean {
        val target = targetPackageName ?: return false

        if (currentPackage == target) {
            consecutiveBringBacks = 0
            return false
        }

        if (currentPackage in allowedPackages) return false

        val systemComponents = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher"
        )

        if (currentPackage in systemComponents) {
            consecutiveBringBacks++
            return consecutiveBringBacks > 3
        }

        return true
    }

    private fun scheduleBringBack() {
        handler.removeCallbacks(bringBackRunnable)
        handler.postDelayed(bringBackRunnable, bringBackDelay)
    }

    private fun bringAppToFront() {
        val pkg = targetPackageName ?: return
        val activity = targetActivityClass ?: return

        val now = System.currentTimeMillis()
        if (now - lastBringBackTime < 100) return
        lastBringBackTime = now

        AppLogger.d(TAG, "执行拉回: package=$pkg, activity=$activity")

        try {
            val intent = Intent().apply {
                component = ComponentName(pkg, activity)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(intent)

            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val tasks = activityManager.appTasks
                for (task in tasks) {
                    val taskInfo = task.taskInfo
                    if (taskInfo.baseActivity?.packageName == pkg) {
                        task.moveToFront()
                        break
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "moveTaskToFront failed", e)
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "拉回失败", e)
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                launchIntent?.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                launchIntent?.let { startActivity(it) }
            } catch (e2: Exception) {
                AppLogger.e(TAG, "备用拉回也失败", e2)
            }
        }
    }



    override fun onInterrupt() {
        AppLogger.w(TAG, "辅助功能服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d(TAG, "辅助功能服务销毁")


        removeTouchBlockOverlay()


        screenOffReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        screenOffReceiver = null


        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null


        restoreSystemBrightness()

        instance = null
        isServiceRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}
