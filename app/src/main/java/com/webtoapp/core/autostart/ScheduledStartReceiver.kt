package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.webtoapp.WebToAppApplication
import com.webtoapp.ui.shell.ShellActivity
import com.webtoapp.ui.webview.WebViewActivity
import java.util.Calendar

/**
 * 定时启动广播接收器
 * 接收定时闹钟触发的广播，启动配置的应用
 */
class ScheduledStartReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ScheduledStartReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AutoStartManager.ACTION_SCHEDULED_START) {
            Log.d(TAG, "收到定时启动广播")
            
            // Check是否为 Shell 模式（导出的独立 APK）
            val isShellMode = try {
                WebToAppApplication.shellMode.isShellMode()
            } catch (e: Exception) {
                false
            }
            
            if (isShellMode) {
                // Shell 模式：检查配置中是否启用了定时自启动
                val config = WebToAppApplication.shellMode.getConfig()
                val autoStartConfig = config?.autoStartConfig
                
                if (autoStartConfig?.scheduledStartEnabled == true) {
                    // Check今天是否在启动日期列表中
                    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1
                    
                    if (autoStartConfig.scheduledDays.contains(dayOfWeek)) {
                        Log.d(TAG, "Shell 模式：今天在启动日期列表中，启动应用")
                        launchShellApp(context)
                    } else {
                        Log.d(TAG, "Shell 模式：今天不在启动日期列表中，跳过启动")
                    }
                }
            } else {
                // 主应用模式
                val autoStartManager = AutoStartManager(context)
                val config = autoStartManager.getScheduledStartConfig()
                
                if (config != null) {
                    // Check今天是否在启动日期列表中
                    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    // Calendar.DAY_OF_WEEK: 1=周日, 2=周一, ..., 7=周六
                    // 转换为我们的格式: 1=周一, ..., 7=周日
                    val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1
                    
                    if (config.days.contains(dayOfWeek)) {
                        Log.d(TAG, "今天在启动日期列表中，启动应用: ${config.appId}")
                        launchApp(context, config.appId)
                    } else {
                        Log.d(TAG, "今天不在启动日期列表中，跳过启动")
                    }
                }
            }
        }
    }
    
    /**
     * 启动主应用中的 WebView 应用
     */
    private fun launchApp(context: Context, appId: Long) {
        try {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra("app_id", appId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "启动应用失败", e)
        }
    }
    
    /**
     * 启动 Shell 模式应用
     */
    private fun launchShellApp(context: Context) {
        try {
            val intent = Intent(context, ShellActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "启动 Shell 应用失败", e)
        }
    }
}
