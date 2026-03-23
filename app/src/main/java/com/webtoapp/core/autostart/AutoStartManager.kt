package com.webtoapp.core.autostart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * 自启动管理器
 * 管理开机自启动和定时自启动功能
 */
class AutoStartManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AutoStartManager"
        const val ACTION_SCHEDULED_START = "com.webtoapp.ACTION_SCHEDULED_START"
        const val EXTRA_APP_ID = "app_id"
        const val PREFS_NAME = "auto_start_prefs"
        const val KEY_BOOT_START_APP_ID = "boot_start_app_id"
        const val KEY_SCHEDULED_START_APP_ID = "scheduled_start_app_id"
        const val KEY_SCHEDULED_TIME = "scheduled_time"
        const val KEY_SCHEDULED_DAYS = "scheduled_days"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * 设置开机自启动
     */
    fun setBootStart(appId: Long, enabled: Boolean) {
        prefs.edit().apply {
            if (enabled) {
                putLong(KEY_BOOT_START_APP_ID, appId)
            } else {
                remove(KEY_BOOT_START_APP_ID)
            }
            apply()
        }
        Log.d(TAG, "开机自启动 ${if (enabled) "已启用" else "已禁用"}, appId=$appId")
    }
    
    /**
     * 获取开机自启动的应用ID
     */
    fun getBootStartAppId(): Long {
        return prefs.getLong(KEY_BOOT_START_APP_ID, -1L)
    }
    
    /**
     * 设置定时自启动
     * @param appId 应用ID
     * @param enabled 是否启用
     * @param time 启动时间（HH:mm 格式）
     * @param days 启动日期列表（1-7 代表周一到周日）
     */
    fun setScheduledStart(appId: Long, enabled: Boolean, time: String = "08:00", days: List<Int> = listOf(1,2,3,4,5,6,7)) {
        if (enabled) {
            prefs.edit().apply {
                putLong(KEY_SCHEDULED_START_APP_ID, appId)
                putString(KEY_SCHEDULED_TIME, time)
                putString(KEY_SCHEDULED_DAYS, days.joinToString(","))
                apply()
            }
            scheduleAlarm(appId, time, days)
        } else {
            prefs.edit().apply {
                remove(KEY_SCHEDULED_START_APP_ID)
                remove(KEY_SCHEDULED_TIME)
                remove(KEY_SCHEDULED_DAYS)
                apply()
            }
            cancelAlarm()
        }
        Log.d(TAG, "定时自启动 ${if (enabled) "已启用" else "已禁用"}, appId=$appId, time=$time")
    }
    
    /**
     * 获取定时自启动配置
     */
    fun getScheduledStartConfig(): ScheduledStartConfig? {
        val appId = prefs.getLong(KEY_SCHEDULED_START_APP_ID, -1L)
        if (appId == -1L) return null
        
        val time = prefs.getString(KEY_SCHEDULED_TIME, "08:00") ?: "08:00"
        val daysStr = prefs.getString(KEY_SCHEDULED_DAYS, "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        val days = daysStr.split(",").mapNotNull { it.toIntOrNull() }
        
        return ScheduledStartConfig(appId, time, days)
    }
    
    /**
     * 设置定时闹钟
     */
    private fun scheduleAlarm(appId: Long, time: String, days: List<Int>) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 如果时间已过，设置为明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val intent = Intent(context, ScheduledStartReceiver::class.java).apply {
            action = ACTION_SCHEDULED_START
            putExtra(EXTRA_APP_ID, appId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 使用 setRepeating 每天重复
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        
        Log.d(TAG, "定时闹钟已设置: ${calendar.time}")
    }
    
    /**
     * 取消定时闹钟
     */
    private fun cancelAlarm() {
        val intent = Intent(context, ScheduledStartReceiver::class.java).apply {
            action = ACTION_SCHEDULED_START
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "定时闹钟已取消")
    }
    
    /**
     * 重新设置闹钟（用于开机后恢复）
     */
    fun rescheduleAlarmIfNeeded() {
        val config = getScheduledStartConfig() ?: return
        scheduleAlarm(config.appId, config.time, config.days)
    }
}

/**
 * 定时启动配置
 */
data class ScheduledStartConfig(
    val appId: Long,
    val time: String,
    val days: List<Int>
)
