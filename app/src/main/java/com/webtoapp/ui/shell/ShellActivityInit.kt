package com.webtoapp.ui.shell

import android.view.KeyEvent
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig

/**
 * ShellActivity 的 onCreate 初始化辅助
 *
 * 将 onCreate 中大量的配置初始化逻辑提取到独立函数中，
 * 减少 ShellActivity.onCreate() 的行数。
 */
object ShellActivityInit {

    /**
     * 初始化日志系统（尽早调用以捕获崩溃）
     */
    fun initLogger(activity: AppCompatActivity) {
        try {
            val tempConfig = WebToAppApplication.shellMode.getConfig()
            val versionName = try {
                activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) { "1.0.0" }

            com.webtoapp.core.shell.ShellLogger.init(
                context = activity,
                appName = tempConfig?.appName ?: "ShellApp",
                appVersion = versionName
            )
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "onCreate 开始")
            
            // C 级系统层 Activity 优化 (Window flags, CPU 亲和性, 动画自适应)
            com.webtoapp.core.perf.SystemPerfOptimizer.optimizeActivity(activity)
        } catch (e: Exception) {
            AppLogger.e("ShellActivity", "日志系统初始化失败", e)
        }
    }

    /**
     * 初始化强制运行管理器
     */
    fun initForcedRunManager(
        activity: AppCompatActivity,
        config: ShellConfig,
        forcedRunManager: ForcedRunManager,
        onStateChanged: (Boolean, com.webtoapp.core.forcedrun.ForcedRunConfig?) -> Unit
    ) {
        // 设置硬件控制器的目标 Activity
        try {
            val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(activity)
            hardwareController.setTargetActivity(activity)
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "硬件控制器初始化成功")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "硬件控制器初始化失败", e)
        }

        // 初始化强制运行管理器
        if (config.forcedRunConfig?.enabled == true) {
            try {
                forcedRunManager.setTargetActivity(
                    packageName = activity.packageName,
                    activityClass = activity::class.java.name
                )
                forcedRunManager.setOnStateChangedCallback { active, forcedConfig ->
                    activity.runOnUiThread {
                        onStateChanged(active, forcedConfig)
                    }
                }
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "强制运行管理器初始化成功")
            } catch (e: Exception) {
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "强制运行管理器初始化失败", e)
            }
        }
    }

    /**
     * 注册自启动配置
     * 仅在配置变更时重新调度，避免每次 Activity 创建都重复注册
     */
    fun initAutoStart(activity: AppCompatActivity, config: ShellConfig) {
        config.autoStartConfig?.let { autoStartConfig ->
            try {
                val autoStartManager = com.webtoapp.core.autostart.AutoStartManager(activity)
                
                // 注册开机自启动
                autoStartManager.setBootStart(
                    appId = 0L, 
                    enabled = autoStartConfig.bootStartEnabled,
                    delayMs = com.webtoapp.core.autostart.AutoStartManager.DEFAULT_BOOT_DELAY_MS
                )
                
                // 注册定时自启动
                if (autoStartConfig.scheduledStartEnabled) {
                    autoStartManager.setScheduledStart(
                        appId = 0L,
                        enabled = true,
                        time = autoStartConfig.scheduledTime,
                        days = autoStartConfig.scheduledDays
                    )
                } else {
                    autoStartManager.setScheduledStart(appId = 0L, enabled = false)
                }
                
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "自启动配置已注册: 开机=${autoStartConfig.bootStartEnabled}, 定时=${autoStartConfig.scheduledStartEnabled}")
            } catch (e: Exception) {
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "自启动配置注册失败", e)
            }
        }
    }

    /**
     * 初始化独立环境/多开配置
     */
    fun initIsolation(activity: AppCompatActivity, config: ShellConfig) {
        if (config.isolationEnabled && config.isolationConfig != null) {
            try {
                val isolationConfig = config.isolationConfig.toIsolationConfig()
                val isolationManager = com.webtoapp.core.isolation.IsolationManager.getInstance(activity)
                isolationManager.initialize(isolationConfig)
                AppLogger.d("ShellActivity", "独立环境已初始化: enabled=${isolationConfig.enabled}")
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "独立环境已初始化")
            } catch (e: Exception) {
                AppLogger.e("ShellActivity", "独立环境初始化失败", e)
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "独立环境初始化失败", e)
            }
        }
    }

    /**
     * 初始化后台运行服务
     */
    fun initBackgroundService(activity: AppCompatActivity, config: ShellConfig) {
        if (config.backgroundRunEnabled) {
            try {
                val bgConfig = config.backgroundRunConfig
                com.webtoapp.core.background.BackgroundRunService.start(
                    context = activity,
                    appName = config.appName,
                    notificationTitle = bgConfig?.notificationTitle?.ifEmpty { null },
                    notificationContent = bgConfig?.notificationContent?.ifEmpty { null },
                    showNotification = bgConfig?.showNotification ?: true,
                    keepCpuAwake = bgConfig?.keepCpuAwake ?: true
                )
                AppLogger.d("ShellActivity", "后台运行服务已启动")
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "后台运行服务已启动")
            } catch (e: Exception) {
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "后台运行服务启动失败", e)
            }
        }
    }

    /**
     * 设置任务列表中显示的应用名称
     */
    fun setTaskDescription(activity: AppCompatActivity, appName: String) {
        try {
            @Suppress("DEPRECATION")
            activity.setTaskDescription(android.app.ActivityManager.TaskDescription(appName))
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "setTaskDescription 失败", e)
        }
    }

    /**
     * 创建返回键处理回调
     */
    fun createBackPressedCallback(
        activity: AppCompatActivity,
        forcedRunManager: ForcedRunManager,
        getCustomView: () -> android.view.View?,
        getWebView: () -> WebView?,
        hideCustomView: () -> Unit
    ): OnBackPressedCallback {
        return object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (forcedRunManager.handleKeyEvent(KeyEvent.KEYCODE_BACK)) {
                    Toast.makeText(activity, Strings.cannotExitDuringForcedRun, Toast.LENGTH_SHORT).show()
                    return
                }
                when {
                    getCustomView() != null -> hideCustomView()
                    else -> {
                        // 先向 WebView 派发 ESC 按键事件，让 JS 脚本有机会处理
                        val wv = getWebView()
                        if (wv != null) {
                            wv.evaluateJavascript("""
                                (function() {
                                    var evt = new KeyboardEvent('keydown', {
                                        key: 'Escape', code: 'Escape',
                                        keyCode: 27, which: 27,
                                        bubbles: true, cancelable: true
                                    });
                                    return !document.dispatchEvent(evt);
                                })();
                            """.trimIndent()) { result ->
                                if (result == "true") {
                                    // JS 脚本调用了 preventDefault()，不执行原生返回
                                    return@evaluateJavascript
                                }
                                // JS 未拦截，执行原生返回行为
                                // Skip about:blank in history to avoid flashing blank page
                                val backList = wv.copyBackForwardList()
                                val currentIndex = backList.currentIndex
                                if (wv.canGoBack() && currentIndex > 0) {
                                    val prevUrl = backList.getItemAtIndex(currentIndex - 1)?.url
                                    if (prevUrl == "about:blank") {
                                        activity.finish()
                                    } else {
                                        wv.goBack()
                                    }
                                } else {
                                    activity.finish()
                                }
                            }
                        } else {
                            activity.finish()
                        }
                    }
                }
            }
        }
    }
}
