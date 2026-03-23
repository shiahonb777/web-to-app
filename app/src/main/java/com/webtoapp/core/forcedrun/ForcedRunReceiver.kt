package com.webtoapp.core.forcedrun

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 强制运行广播接收器
 * 
 * 处理强制运行相关的广播事件：
 * - 开始强制运行
 * - 结束强制运行
 * - 定时检查
 */
class ForcedRunReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ForcedRunReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appId = intent.getLongExtra(ForcedRunManager.EXTRA_APP_ID, -1L)
        
        Log.d(TAG, "收到广播: action=$action, appId=$appId")
        
        when (action) {
            ForcedRunManager.ACTION_FORCED_RUN_START -> {
                // Start强制运行
                Log.d(TAG, "启动强制运行: appId=$appId")
                // 这里可以启动应用的 Activity
            }
            
            ForcedRunManager.ACTION_FORCED_RUN_END -> {
                // End强制运行
                Log.d(TAG, "结束强制运行: appId=$appId")
                val manager = ForcedRunManager.getInstance(context)
                manager.stopForcedRunMode()
            }
            
            ForcedRunManager.ACTION_FORCED_RUN_CHECK -> {
                // 定时检查
                Log.d(TAG, "定时检查强制运行状态")
            }
            
            Intent.ACTION_BOOT_COMPLETED -> {
                // 开机启动，检查是否需要恢复强制运行状态
                Log.d(TAG, "开机启动，检查强制运行状态")
            }
        }
    }
}
