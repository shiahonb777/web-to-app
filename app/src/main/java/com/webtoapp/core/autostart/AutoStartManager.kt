package com.webtoapp.core.autostart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Note.
 * Note.
 *
 * （v2）：
 * 1. （setExactAndAllowWhileIdle） setRepeating， Doze
 * 2. （ days + + ）
 * 3. Android 12+ SCHEDULE_EXACT_ALARM
 * 4. TIME_SET / TIMEZONE_CHANGED
 * 5. ""， UI
 * 6. （）
 * 7.
 * 8. （debug）
 * 9. OEM ROM
 */
class AutoStartManager(private val context: Context) {

    companion object {
        private const val TAG = "AutoStartManager"
        const val ACTION_SCHEDULED_START = "com.webtoapp.ACTION_SCHEDULED_START"
        const val EXTRA_APP_ID = "app_id"
        const val PREFS_NAME = "auto_start_prefs"
        const val KEY_BOOT_START_APP_ID = "boot_start_app_id"
        const val KEY_BOOT_DELAY_MS = "boot_start_delay_ms"
        const val KEY_SCHEDULED_START_APP_ID = "scheduled_start_app_id"
        const val KEY_SCHEDULED_TIME = "scheduled_time"
        const val KEY_SCHEDULED_TIMES = "scheduled_times"
        const val KEY_SCHEDULED_DAYS = "scheduled_days"
        const val KEY_LAST_SCHEDULE_HASH = "last_schedule_hash"
        const val KEY_LAST_TRIGGER_TIME = "last_trigger_time"
        const val KEY_TRIGGER_COUNT = "trigger_count"

        /** — */
        const val DEFAULT_BOOT_DELAY_MS = 5000L

        /** Note. */
        const val MIN_BOOT_DELAY_MS = 1000L

        /** （30s，） */
        const val MAX_BOOT_DELAY_MS = 30000L

        /** （，60s） */
        private const val MIN_ALARM_INTERVAL_MS = 60_000L

        /** ： scheduleNextAlarm （1s） */
        private const val SCHEDULE_DEBOUNCE_MS = 1000L
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Note. */
    @Volatile
    private var lastScheduleTimestamp = 0L

    // ═══════════════════════════════════════
    // Note.
    // ═══════════════════════════════════════

    /**
     * Note.
     * @param delayMs parameter
     */
    fun setBootStart(appId: Long, enabled: Boolean, delayMs: Long = DEFAULT_BOOT_DELAY_MS) {
        val safeDelay = delayMs.coerceIn(MIN_BOOT_DELAY_MS, MAX_BOOT_DELAY_MS)
        prefs.edit().apply {
            if (enabled) {
                putLong(KEY_BOOT_START_APP_ID, appId)
                putLong(KEY_BOOT_DELAY_MS, safeDelay)
            } else {
                remove(KEY_BOOT_START_APP_ID)
                remove(KEY_BOOT_DELAY_MS)
            }
            apply()
        }
        AppLogger.d(TAG, "开机自启动 ${if (enabled) "已启用 (delay=${safeDelay}ms)" else "已禁用"}, appId=$appId")
    }

    /**
     * ID
     */
    fun getBootStartAppId(): Long {
        return prefs.getLong(KEY_BOOT_START_APP_ID, -1L)
    }

    /**
     * （）
     */
    fun getBootDelay(): Long {
        return prefs.getLong(KEY_BOOT_DELAY_MS, DEFAULT_BOOT_DELAY_MS)
    }

    // ═══════════════════════════════════════
    // （）
    // ═══════════════════════════════════════

    /**
     * Note.
     * @param appId parameter
     * @param enabled parameter
     * @param time parameter
     * @param times parameter
     * @param days parameter
     */
    fun setScheduledStart(
        appId: Long,
        enabled: Boolean,
        time: String = "08:00",
        times: List<String> = emptyList(),
        days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
    ) {
        if (enabled) {
            val effectiveTimes = times.ifEmpty { listOf(time) }
            // +
            val normalizedTimes = effectiveTimes.map { normalizeTime(it) }.distinct().sorted()

            val scheduleHash = "${appId}_${normalizedTimes.joinToString(",")}_${days.sorted().joinToString(",")}"

            prefs.edit().apply {
                putLong(KEY_SCHEDULED_START_APP_ID, appId)
                putString(KEY_SCHEDULED_TIME, normalizedTimes.firstOrNull() ?: time)
                putString(KEY_SCHEDULED_TIMES, normalizedTimes.joinToString(","))
                putString(KEY_SCHEDULED_DAYS, days.joinToString(","))
                putString(KEY_LAST_SCHEDULE_HASH, scheduleHash)
                apply()
            }
            scheduleNextAlarm(appId, normalizedTimes, days)
        } else {
            prefs.edit().apply {
                remove(KEY_SCHEDULED_START_APP_ID)
                remove(KEY_SCHEDULED_TIME)
                remove(KEY_SCHEDULED_TIMES)
                remove(KEY_SCHEDULED_DAYS)
                remove(KEY_LAST_SCHEDULE_HASH)
                apply()
            }
            cancelAlarm()
        }
        AppLogger.d(TAG, "定时自启动 ${if (enabled) "已启用" else "已禁用"}, appId=$appId, time=$time, times=${times}, days=$days")
    }

    /**
     * Note.
     */
    fun getScheduledStartConfig(): ScheduledStartConfig? {
        val appId = prefs.getLong(KEY_SCHEDULED_START_APP_ID, -1L)
        if (appId == -1L) return null

        val time = prefs.getString(KEY_SCHEDULED_TIME, "08:00") ?: "08:00"
        val timesStr = prefs.getString(KEY_SCHEDULED_TIMES, null)
        val times = timesStr?.split(",")?.filter { it.isNotBlank() } ?: listOf(time)
        val daysStr = prefs.getString(KEY_SCHEDULED_DAYS, "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }

        return ScheduledStartConfig(appId, time, days, times)
    }

    // ═══════════════════════════════════════
    // Note.
    // ═══════════════════════════════════════

    /**
     * （ + days + ）
     *
     * @param times parameter
     * @param days parameter
     * @return result
     */
    fun calculateNextTriggerTime(times: List<String>, days: List<Int>): Calendar? {
        if (days.isEmpty() || times.isEmpty()) return null

        val now = Calendar.getInstance()
        var earliest: Calendar? = null

        for (time in times) {
            val candidate = calculateNextTriggerTimeForSingleTime(time, days, now)
            if (candidate != null) {
                if (earliest == null || candidate.timeInMillis < earliest.timeInMillis) {
                    earliest = candidate
                }
            }
        }

        return earliest
    }

    /**
     * ：
     */
    fun calculateNextTriggerTime(time: String, days: List<Int>): Calendar? {
        return calculateNextTriggerTime(listOf(time), days)
    }

    private fun calculateNextTriggerTimeForSingleTime(time: String, days: List<Int>, now: Calendar): Calendar? {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        // 8 （ + ），
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // （+ 60s），
            if (candidate.timeInMillis <= now.timeInMillis + MIN_ALARM_INTERVAL_MS) {
                if (dayOffset == 0) continue
            }

            // Calendar.DAY_OF_WEEK (1=..7=) → (1=..7=)
            val calendarDow = candidate.get(Calendar.DAY_OF_WEEK)
            val ourDow = if (calendarDow == Calendar.SUNDAY) 7 else calendarDow - 1

            if (days.contains(ourDow)) {
                return candidate
            }
        }

        return null
    }

    /**
     * （ UI ， i18n）
     * @return result
     */
    fun getNextTriggerTimeDisplay(): String? {
        val config = getScheduledStartConfig() ?: return null
        val effectiveTimes = config.times.ifEmpty { listOf(config.time) }
        val nextTrigger = calculateNextTriggerTime(effectiveTimes, config.days) ?: return null

        val now = Calendar.getInstance()
        val daysDiff = ((nextTrigger.timeInMillis - now.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextTrigger.time)

        return when (daysDiff) {
            0 -> "今天 $timeStr"
            1 -> "明天 $timeStr"
            else -> {
                val dayNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                val calendarDow = nextTrigger.get(Calendar.DAY_OF_WEEK)
                val ourDow = if (calendarDow == Calendar.SUNDAY) 7 else calendarDow - 1
                "${dayNames[ourDow - 1]} $timeStr"
            }
        }
    }

    /**
     * （）
     */
    private fun scheduleNextAlarm(appId: Long, times: List<String>, days: List<Int>) {
        // Note.
        val now = System.currentTimeMillis()
        if (now - lastScheduleTimestamp < SCHEDULE_DEBOUNCE_MS) {
            AppLogger.d(TAG, "防重复调度：距上次调度不足 ${SCHEDULE_DEBOUNCE_MS}ms，跳过")
            return
        }
        lastScheduleTimestamp = now

        val nextTrigger = calculateNextTriggerTime(times, days)
        if (nextTrigger == null) {
            AppLogger.w(TAG, "无法计算下一次启动时间 (times=$times, days=$days)")
            return
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

        // Note.
        try {
            if (canScheduleExactAlarms()) {
                // — Doze
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger.timeInMillis,
                    pendingIntent
                )
                AppLogger.d(TAG, "精确闹钟已设置: ${formatDate(nextTrigger.time)}")
            } else {
                // ，
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger.timeInMillis,
                    pendingIntent
                )
                AppLogger.w(TAG, "精确闹钟权限不可用，使用近似闹钟: ${formatDate(nextTrigger.time)}")
            }
        } catch (e: SecurityException) {
            // Android 12+ SecurityException
            AppLogger.w(TAG, "设置精确闹钟被拒绝，降级为近似闹钟", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger.timeInMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                AppLogger.e(TAG, "设置闹钟完全失败", ex)
            }
        }
    }

    /** ： */
    private fun scheduleNextAlarm(appId: Long, time: String, days: List<Int>) {
        scheduleNextAlarm(appId, listOf(time), days)
    }

    /**
     * Note.
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
        AppLogger.d(TAG, "定时闹钟已取消")
    }

    /**
     * Reschedule the next alarm after current trigger.
     */
    fun rescheduleAfterTrigger() {
        // Note.
        recordTrigger()
        val config = getScheduledStartConfig() ?: return
        val effectiveTimes = config.times.ifEmpty { listOf(config.time) }
        scheduleNextAlarm(config.appId, effectiveTimes, config.days)
    }

    /**
     * Restore schedule after boot, timezone, or time changes.
     */
    fun rescheduleAlarmIfNeeded() {
        val config = getScheduledStartConfig() ?: return
        val effectiveTimes = config.times.ifEmpty { listOf(config.time) }
        scheduleNextAlarm(config.appId, effectiveTimes, config.days)
    }

    // ═══════════════════════════════════════
    // Trigger records
    // ═══════════════════════════════════════

    private fun recordTrigger() {
        prefs.edit().apply {
            putLong(KEY_LAST_TRIGGER_TIME, System.currentTimeMillis())
            putLong(KEY_TRIGGER_COUNT, prefs.getLong(KEY_TRIGGER_COUNT, 0) + 1)
            apply()
        }
    }

    /**
     * Get last trigger time for diagnostics.
     */
    fun getLastTriggerDisplay(): String? {
        val lastTrigger = prefs.getLong(KEY_LAST_TRIGGER_TIME, 0)
        if (lastTrigger == 0L) return null
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastTrigger))
    }

    /**
     * Note.
     */
    fun getTriggerCount(): Long {
        return prefs.getLong(KEY_TRIGGER_COUNT, 0)
    }

    // ═══════════════════════════════════════
    // Note.
    // ═══════════════════════════════════════

    /**
     * Note.
     * Android 12 (API 31) SCHEDULE_EXACT_ALARM
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11
        }
    }

    /**
     * Check whether battery optimization is ignored.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Get OEM-specific auto-start settings intent if available.
     *
     * Some OEM ROMs require extra whitelist setup for background start.
     */
    fun getOemAutoStartIntent(): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return try {
            when {
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                    Intent().apply {
                        setClassName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    Intent().apply {
                        setClassName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                    Intent().apply {
                        setClassName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.startupapp.StartupAppListActivity"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                    Intent().apply {
                        setClassName(
                            "com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                manufacturer.contains("oneplus") -> {
                    Intent().apply {
                        setClassName(
                            "com.oneplus.security",
                            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                manufacturer.contains("samsung") -> {
                    Intent().apply {
                        setClassName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                manufacturer.contains("meizu") -> {
                    Intent().apply {
                        setClassName(
                            "com.meizu.safe",
                            "com.meizu.safe.security.SHOW_APPSEC"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * OEM ROM （ UI ）
     */
    fun getOemBrandName(): String? {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> "小米/Redmi"
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> "华为/荣耀"
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> "OPPO/realme"
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "vivo/iQOO"
            manufacturer.contains("oneplus") -> "一加/OnePlus"
            manufacturer.contains("samsung") -> "三星/Samsung"
            manufacturer.contains("meizu") -> "魅族/Meizu"
            else -> null
        }
    }

    // ═══════════════════════════════════════
    // Helper methods
    // ═══════════════════════════════════════

    /**
     * HH:mm
     */
    private fun normalizeTime(time: String): String {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
    }
}

/**
 * Note.
 * @property times （v2 ， time）
 */
data class ScheduledStartConfig(
    val appId: Long,
    val time: String,
    val days: List<Int>,
    val times: List<String> = emptyList()
)
