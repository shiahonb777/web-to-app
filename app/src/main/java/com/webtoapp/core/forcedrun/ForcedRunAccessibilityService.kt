package com.webtoapp.core.forcedrun

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * 强制运行辅助功能服务
 * 
 * 这是真正有效的强制运行核心组件！
 * 
 * 原理：
 * 1. AccessibilityService 可以监控所有窗口变化事件
 * 2. 当检测到用户试图离开应用（切换到其他应用）时
 * 3. 立即将应用拉回前台
 * 
 * 这是 Android 系统允许的合法方式，无需 root，
 * 只需用户在设置中启用辅助功能即可。
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
        
        // 强制运行状态
        @Volatile
        var isForcedRunActive = false
        
        @Volatile
        var targetPackageName: String? = null
        
        @Volatile
        var targetActivityClass: String? = null
        
        // Configure
        @Volatile
        var bringBackDelay: Long = 50L // 拉回延迟（毫秒），越小越激进
        
        @Volatile
        var allowedPackages: Set<String> = emptySet() // Allow的包名白名单
        
        /**
         * 启动强制运行防护
         */
        fun startForcedRun(
            packageName: String,
            activityClass: String,
            allowedPkgs: Set<String> = emptySet()
        ) {
            Log.d(TAG, "启动强制运行防护: package=$packageName, activity=$activityClass")
            targetPackageName = packageName
            targetActivityClass = activityClass
            allowedPackages = allowedPkgs + setOf(
                packageName,
                "com.android.systemui", // Allow状态栏短暂显示
            )
            isForcedRunActive = true
        }
        
        /**
         * 停止强制运行防护
         */
        fun stopForcedRun() {
            Log.d(TAG, "停止强制运行防护")
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
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
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
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var lastBringBackTime = 0L
    private var consecutiveBringBacks = 0
    private val bringBackRunnable = Runnable { bringAppToFront() }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "辅助功能服务已连接")
        
        instance = this
        isServiceRunning = true
        
        // Configure服务
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50 // 50ms 超时，快速响应
        } ?: AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
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
                // 窗口层级变化，可能是其他应用覆盖
                if (shouldBringBack(eventPackage)) {
                    scheduleBringBack()
                }
            }
        }
    }
    
    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""
        
        Log.v(TAG, "窗口变化: package=$packageName, class=$className")
        
        // Check是否需要拉回
        if (shouldBringBack(packageName)) {
            Log.d(TAG, "检测到离开应用: $packageName, 准备拉回")
            scheduleBringBack()
        }
    }
    
    /**
     * 判断是否需要拉回应用
     */
    private fun shouldBringBack(currentPackage: String): Boolean {
        val target = targetPackageName ?: return false
        
        // 如果当前就是目标应用，不需要拉回
        if (currentPackage == target) {
            consecutiveBringBacks = 0
            return false
        }
        
        // Check是否在白名单中
        if (currentPackage in allowedPackages) {
            return false
        }
        
        // System关键组件临时允许
        val systemComponents = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher"
        )
        
        // 即使是系统组件，如果持续显示也要拉回
        if (currentPackage in systemComponents) {
            consecutiveBringBacks++
            // Allow短暂显示（如下拉状态栏），但超过3次就拉回
            return consecutiveBringBacks > 3
        }
        
        return true
    }
    
    /**
     * 调度拉回操作（带防抖）
     */
    private fun scheduleBringBack() {
        handler.removeCallbacks(bringBackRunnable)
        handler.postDelayed(bringBackRunnable, bringBackDelay)
    }
    
    /**
     * 将应用拉回前台 - 核心方法
     */
    private fun bringAppToFront() {
        val pkg = targetPackageName ?: return
        val activity = targetActivityClass ?: return
        
        val now = System.currentTimeMillis()
        
        // 防止过于频繁的拉回（最小间隔 100ms）
        if (now - lastBringBackTime < 100) {
            return
        }
        lastBringBackTime = now
        
        Log.d(TAG, "执行拉回: package=$pkg, activity=$activity")
        
        try {
            // Method1：直接启动目标 Activity
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
            
            // Method2：如果方法1失败，使用 moveTaskToFront
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
                Log.w(TAG, "moveTaskToFront failed", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "拉回失败", e)
            
            // 备用方案：通过包管理器启动
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                launchIntent?.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                launchIntent?.let { startActivity(it) }
            } catch (e2: Exception) {
                Log.e(TAG, "备用拉回也失败", e2)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "辅助功能服务被中断")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "辅助功能服务销毁")
        instance = null
        isServiceRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}
