package com.webtoapp.core.forcedrun

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.webtoapp.core.logging.AppLogger










class ForcedRunReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ForcedRunReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appId = intent.getLongExtra(ForcedRunManager.EXTRA_APP_ID, -1L)

        AppLogger.i(TAG, "收到广播: action=$action, appId=$appId")

        when (action) {
            ForcedRunManager.ACTION_FORCED_RUN_START -> {
                AppLogger.i(TAG, "启动强制运行: appId=$appId")
                handleForcedRunStart(context)
            }

            ForcedRunManager.ACTION_FORCED_RUN_END -> {
                AppLogger.i(TAG, "结束强制运行: appId=$appId")
                handleForcedRunEnd(context)
            }

            ForcedRunManager.ACTION_FORCED_RUN_CHECK -> {
                AppLogger.i(TAG, "定时检查强制运行状态")
                handleForcedRunCheck(context)
            }

            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                AppLogger.i(TAG, "开机启动，检查强制运行状态")
                handleBootCompleted(context)
            }
        }
    }






    private fun handleForcedRunStart(context: Context) {
        val manager = ForcedRunManager.getInstance(context)


        if (manager.isInForcedRunMode.value) {
            AppLogger.d(TAG, "强制运行已在运行中，忽略启动请求")
            return
        }


        val restored = manager.restoreFromPersistence()
        if (restored) {
            AppLogger.i(TAG, "强制运行已从持久化恢复")

            bringTargetActivityToFront(context, manager)
        } else {
            AppLogger.d(TAG, "无持久化状态可恢复")
        }
    }




    private fun handleForcedRunEnd(context: Context) {
        val manager = ForcedRunManager.getInstance(context)

        if (manager.isInForcedRunMode.value) {
            AppLogger.i(TAG, "AlarmManager 触发：停止强制运行")
            manager.stopForcedRunMode()
        } else {
            AppLogger.d(TAG, "强制运行未在运行中，忽略结束请求")
        }
    }








    private fun handleForcedRunCheck(context: Context) {
        val manager = ForcedRunManager.getInstance(context)

        if (!manager.isInForcedRunMode.value) {

            val restored = manager.restoreFromPersistence()
            if (restored) {
                AppLogger.i(TAG, "定时检查：恢复了中断的强制运行")
                bringTargetActivityToFront(context, manager)
            }
        } else {
            AppLogger.d(TAG, "定时检查：强制运行正常运行中")
        }
    }






    private fun handleBootCompleted(context: Context) {
        val manager = ForcedRunManager.getInstance(context)

        val restored = manager.restoreFromPersistence()
        if (restored) {
            AppLogger.i(TAG, "开机恢复：强制运行已恢复")
            bringTargetActivityToFront(context, manager)
        } else {
            AppLogger.d(TAG, "开机恢复：无需恢复的强制运行状态")
        }
    }




    private fun bringTargetActivityToFront(context: Context, manager: ForcedRunManager) {
        val packageName = manager.getTargetPackageName()
        val activityClass = manager.getTargetActivityClass()

        if (packageName != null && activityClass != null) {
            try {
                val launchIntent = Intent().apply {
                    component = android.content.ComponentName(packageName, activityClass)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                }
                context.startActivity(launchIntent)
                AppLogger.i(TAG, "目标 Activity 已拉起: $packageName/$activityClass")
            } catch (e: Exception) {
                AppLogger.e(TAG, "拉起目标 Activity 失败，尝试 Launch Intent", e)

                try {
                    val fallbackIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    fallbackIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    fallbackIntent?.let { context.startActivity(it) }
                } catch (e2: Exception) {
                    AppLogger.e(TAG, "降级拉起也失败", e2)
                }
            }
        } else {

            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                launchIntent?.let { context.startActivity(it) }
                AppLogger.i(TAG, "使用自身 LaunchIntent 拉起")
            } catch (e: Exception) {
                AppLogger.e(TAG, "自身 LaunchIntent 也失败", e)
            }
        }
    }
}
