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

/**
 * 强制运行辅助功能服务
 * 
 * 核心防护组件 + 黑科技硬件拦截层
 * 
 * 能力：
 * 1. 监控窗口变化 → 强制运行拉回
 * 2. 拦截硬件按键 → 音量键/返回键屏蔽 (onKeyEvent)
 * 3. 覆盖全屏透明层 → 触摸屏蔽 (WindowManager overlay)
 * 4. 监听屏幕关闭 → 电源键防护 (强制唤醒)
 * 5. 控制系统亮度 → Settings.System 亮度控制 (WRITE_SETTINGS)
 */
class ForcedRunAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "ForcedRunA11yService"
        
        // 服务状态
        @Volatile
        private var instance: ForcedRunAccessibilityService? = null
        
        @Volatile
        var isServiceRunning = false
            private set
        
        // ===== 强制运行状态 =====
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
        
        // ===== 黑科技按键/触摸拦截状态 =====
        
        /** 是否拦截音量键（由 ForcedRunHardwareController 设置）*/
        @Volatile
        var blockVolumeKeys: Boolean = false
        
        /** 是否拦截返回键 */
        @Volatile
        var blockBackKey: Boolean = false
        
        /** 是否启用触摸屏蔽覆盖层 */
        @Volatile
        var blockTouchOverlay: Boolean = false
            set(value) {
                field = value
                instance?.updateTouchBlockOverlay(value)
            }
        
        /** 是否拦截电源键（通过屏幕关闭检测 + 强制唤醒实现）*/
        @Volatile
        var blockPowerKey: Boolean = false
        
        /** 是否启用全黑屏模式（覆盖层 + 亮度=0）*/
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
        
        /**
         * 启动强制运行防护
         */
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
        
        /**
         * 停止强制运行防护
         */
        fun stopForcedRun() {
            AppLogger.d(TAG, "停止强制运行防护")
            isForcedRunActive = false
            targetPackageName = null
            targetActivityClass = null
            allowedPackages = emptySet()
        }
        
        /**
         * 检查辅助功能服务是否已启用
         */
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
        
        /**
         * 打开辅助功能设置页面
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        
        /**
         * 通过 Settings.System 设置系统亮度 (需要 WRITE_SETTINGS)
         * 
         * 这是非 root 设备上控制亮度的唯一可靠方式：
         * - 不依赖 sysfs（被 SELinux 阻止）
         * - 不依赖 Window.attributes（只影响当前 Activity）
         * - 通过 ContentResolver 全局生效
         * 
         * @param context 应用上下文
         * @param brightness 0-255 亮度值
         * @return 是否设置成功
         */
        fun setSystemBrightnessGlobal(context: Context, brightness: Int): Boolean {
            return try {
                if (!Settings.System.canWrite(context)) {
                    AppLogger.w(TAG, "无 WRITE_SETTINGS 权限，无法控制系统亮度")
                    return false
                }
                // 切换到手动亮度模式
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                // 设置亮度值 (0-255)
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
        
        /**
         * 请求 WRITE_SETTINGS 权限
         */
        fun requestWriteSettingsPermission(context: Context) {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
        
        /**
         * 停止所有黑科技拦截功能
         */
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
    
    // 触摸屏蔽覆盖层
    private var touchBlockView: View? = null
    private var windowManager: WindowManager? = null
    
    // 电源键防护：屏幕关闭广播接收器
    private var screenOffReceiver: BroadcastReceiver? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 亮度原值缓存
    private var originalBrightness: Int = -1
    private var originalBrightnessMode: Int = -1
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLogger.d(TAG, "辅助功能服务已连接")
        
        instance = this
        isServiceRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // 配置服务 — 包含按键过滤
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
        
        // 注册屏幕关闭广播（电源键防护）
        registerScreenOffReceiver()
    }
    
    // ======================== 按键拦截 ========================
    
    /**
     * 核心按键拦截 — AccessibilityService 的 onKeyEvent
     * 
     * 这是 Android 上非 root 拦截硬件按键的**唯一**合法方式。
     * 返回 true 表示消费该事件（阻止传递到系统/应用）。
     * 
     * 注意：无法拦截电源键（系统保留），电源键通过 screenOff 检测实现。
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        
        val keyCode = event.keyCode
        
        // 音量键拦截
        if (blockVolumeKeys) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || 
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                AppLogger.d(TAG, "拦截音量键: keyCode=$keyCode action=${event.action}")
                return true // 消费事件，阻止音量变化
            }
        }
        
        // 返回键拦截
        if (blockBackKey) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                AppLogger.d(TAG, "拦截返回键: action=${event.action}")
                return true
            }
        }
        
        // Home 键和 Recent 键无法在此拦截（系统直接处理）
        // 强制运行通过 onAccessibilityEvent 窗口监控实现
        
        return false // 不拦截，正常传递
    }
    
    // ======================== 触摸屏蔽 ========================
    
    /**
     * 创建/移除触摸屏蔽覆盖层
     * 
     * 使用 WindowManager 添加一个全屏透明层，消费所有触摸事件。
     * 这比 Activity 级别的 dispatchTouchEvent 更可靠，因为：
     * - 覆盖层在 Activity 之上，无法被绕过
     * - 即使 Activity 被意外重建，覆盖层依然存在
     * - 使用 SYSTEM_ALERT_WINDOW 权限，优先级最高
     */
    @Suppress("DEPRECATION")
    private fun updateTouchBlockOverlay(enable: Boolean) {
        handler.post {
            if (enable && touchBlockView == null) {
                try {
                    val view = View(this).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnTouchListener { _, _ -> true } // 消费所有触摸
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
    
    /**
     * 全黑屏覆盖层 — 黑色不透明 + 触摸消费
     */
    @Suppress("DEPRECATION")
    private fun updateBlackScreenOverlay(enable: Boolean) {
        handler.post {
            if (enable) {
                // 先移除透明层
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
    
    // ======================== 电源键防护 ========================
    
    /**
     * 注册屏幕关闭广播接收器
     * 
     * 原理：电源键无法被 AccessibilityService 拦截，但我们可以：
     * 1. 监听 ACTION_SCREEN_OFF 广播
     * 2. 收到后立即通过 WakeLock 唤醒屏幕
     * 3. 效果：用户按电源键 → 屏幕闪灭一下立即亮起
     */
    private fun registerScreenOffReceiver() {
        if (screenOffReceiver != null) return
        
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!blockPowerKey) return
                if (intent?.action != Intent.ACTION_SCREEN_OFF) return
                
                AppLogger.d(TAG, "检测到屏幕关闭（电源键），立即唤醒")
                
                // 立即唤醒屏幕
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
    
    /**
     * 强制唤醒屏幕
     */
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
            wakeLock?.acquire(5000L) // 保持 5 秒
            
            AppLogger.d(TAG, "屏幕已强制唤醒")
        } catch (e: Exception) {
            AppLogger.e(TAG, "强制唤醒屏幕失败", e)
        }
    }
    
    // ======================== 亮度控制 ========================
    
    /**
     * 通过 Settings.System 设置系统亮度
     */
    private fun setSystemBrightness(brightness: Int) {
        try {
            if (!Settings.System.canWrite(this)) {
                AppLogger.w(TAG, "无 WRITE_SETTINGS 权限")
                return
            }
            
            // 缓存原始值
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
            
            // 切换到手动模式
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            // 设置亮度
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
    
    /**
     * 恢复原始系统亮度
     */
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
    
    // ======================== 窗口监控（强制运行）========================
    
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
    
    // ======================== 生命周期 ========================
    
    override fun onInterrupt() {
        AppLogger.w(TAG, "辅助功能服务被中断")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d(TAG, "辅助功能服务销毁")
        
        // 清理覆盖层
        removeTouchBlockOverlay()
        
        // 注销广播
        screenOffReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        screenOffReceiver = null
        
        // 释放 WakeLock
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        
        // 恢复亮度
        restoreSystemBrightness()
        
        instance = null
        isServiceRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}
