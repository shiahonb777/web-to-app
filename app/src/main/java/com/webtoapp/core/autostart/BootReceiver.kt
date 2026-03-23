package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.webtoapp.WebToAppApplication
import com.webtoapp.ui.shell.ShellActivity
import com.webtoapp.ui.webview.WebViewActivity

/**
 * 开机广播接收器
 * 监听系统开机完成事件，自动启动配置的应用
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "收到开机完成广播")
            
            val autoStartManager = AutoStartManager(context)
            
            // Check是否为 Shell 模式（导出的独立 APK）
            val isShellMode = try {
                WebToAppApplication.shellMode.isShellMode()
            } catch (e: Exception) {
                false
            }
            
            if (isShellMode) {
                // Shell 模式：检查配置中是否启用了开机自启动
                val config = WebToAppApplication.shellMode.getConfig()
                if (config?.autoStartConfig?.bootStartEnabled == true) {
                    Log.d(TAG, "Shell 模式：启动开机自启动应用")
                    launchShellApp(context)
                }
            } else {
                // 主应用模式：检查是否有开机自启动的应用
                val bootStartAppId = autoStartManager.getBootStartAppId()
                if (bootStartAppId > 0) {
                    Log.d(TAG, "启动开机自启动应用: $bootStartAppId")
                    launchApp(context, bootStartAppId)
                }
            }
            
            // 重新设置定时闹钟（开机后闹钟会丢失）
            autoStartManager.rescheduleAlarmIfNeeded()
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
