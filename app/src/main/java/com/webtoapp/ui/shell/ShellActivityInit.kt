package com.webtoapp.ui.shell

import android.view.KeyEvent
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.shell.ShellRuntimeServices

/**
 * ShellActivity onCreate initialize
 *
 * map onCreate configinitialize,
 * ShellActivity. onCreate( ) .
 */
object ShellActivityInit {

    /**
     * initialize system( call)
     */
    fun initLogger(activity: AppCompatActivity) {
        try {
            val tempConfig = ShellRuntimeServices.shellMode.getConfig()
            val versionName = try {
                activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) { "1.0.0" }

            com.webtoapp.core.shell.ShellLogger.init(
                context = activity,
                appName = tempConfig?.appName ?: "ShellApp",
                appVersion = versionName
            )
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "onCreate 开始")
            
            // C system Activity( Window flags, CPU, animation)
            com.webtoapp.core.perf.SystemPerfOptimizer.optimizeActivity(activity)
        } catch (e: Exception) {
            AppLogger.e("ShellActivity", "日志系统初始化失败", e)
        }
    }

    /**
     * initializeforce- runmanager
     */
    fun initForcedRunManager(
        activity: AppCompatActivity,
        config: ShellConfig,
        forcedRunManager: ForcedRunManager,
        onStateChanged: (Boolean, com.webtoapp.core.forcedrun.ForcedRunConfig?) -> Unit
    ) {
        // settingshardware Activity
        try {
            val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(activity)
            hardwareController.setTargetActivity(activity)
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "硬件控制器初始化成功")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "硬件控制器初始化失败", e)
        }

        // initializeforce- runmanager
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
     * register config
     * only config, Activity create register
     */
    fun initAutoStart(activity: AppCompatActivity, config: ShellConfig) {
        config.autoStartConfig?.let { autoStartConfig ->
            try {
                val autoStartManager = com.webtoapp.core.autostart.AutoStartManager(activity)
                
                // register
                autoStartManager.setBootStart(
                    appId = 0L, 
                    enabled = autoStartConfig.bootStartEnabled,
                    delayMs = com.webtoapp.core.autostart.AutoStartManager.DEFAULT_BOOT_DELAY_MS
                )
                
                // register
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
     * initialize / config
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
     * initialize run
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
     * settings list display app
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
     * createback handle
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
                        // WebView dispatch ESC, JS handle
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
                                    // JS call preventDefault( ) , execute back
                                    return@evaluateJavascript
                                }
                                // JS intercept, execute back
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
