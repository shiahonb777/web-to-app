package com.webtoapp.core.forcedrun

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.Calendar




enum class ProtectionLevel {
    BASIC,
    STANDARD,
    MAXIMUM
}























@SuppressLint("StaticFieldLeak")
class ForcedRunManager(private val context: Context) {

    data class ProtectionRuntimeState(
        val level: ProtectionLevel,
        val accessibilityEnabled: Boolean,
        val usageStatsEnabled: Boolean
    ) {
        val canUseAccessibilityProtection: Boolean get() = accessibilityEnabled
        val canUseGuardService: Boolean get() = usageStatsEnabled
        val isDegraded: Boolean
            get() = when (level) {
                ProtectionLevel.BASIC -> false
                ProtectionLevel.STANDARD -> !accessibilityEnabled
                ProtectionLevel.MAXIMUM -> !accessibilityEnabled || !usageStatsEnabled
            }
    }

    companion object {
        private const val TAG = "ForcedRunManager"
        const val ACTION_FORCED_RUN_START = "com.webtoapp.ACTION_FORCED_RUN_START"
        const val ACTION_FORCED_RUN_END = "com.webtoapp.ACTION_FORCED_RUN_END"
        const val ACTION_FORCED_RUN_CHECK = "com.webtoapp.ACTION_FORCED_RUN_CHECK"
        const val EXTRA_APP_ID = "app_id"
        const val PREFS_NAME = "forced_run_prefs"


        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_MODE = "mode"
        private const val KEY_COUNTDOWN_END_TIME = "countdown_end_time"
        private const val KEY_CONFIG_JSON = "config_json"
        private const val KEY_START_TIMESTAMP = "start_timestamp"


        private const val ALARM_REQUEST_CODE_END = 10001
        private const val ALARM_REQUEST_CODE_START = 10002
        private const val ALARM_REQUEST_CODE_CHECK = 10003


        private const val CHECK_INTERVAL_NORMAL = 10_000L
        private const val CHECK_INTERVAL_CLOSING = 1_000L
        private const val CLOSING_THRESHOLD_MS = 2 * 60 * 1000L

        @Volatile
        private var instance: ForcedRunManager? = null

        fun getInstance(context: Context): ForcedRunManager {
            return instance ?: synchronized(this) {
                instance ?: ForcedRunManager(context.applicationContext).also { instance = it }
            }
        }




        fun checkProtectionPermissions(context: Context, level: ProtectionLevel): PermissionStatus {
            val hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
            val hasUsageStats = ForcedRunGuardService.hasUsageStatsPermission(context)
            return when (level) {
                ProtectionLevel.BASIC -> PermissionStatus(
                    level = level,
                    hasAccessibility = hasAccessibility,
                    hasUsageStats = hasUsageStats,
                    message = "基础防护无需额外权限"
                )
                ProtectionLevel.STANDARD -> {
                    PermissionStatus(
                        level = level,
                        hasAccessibility = hasAccessibility,
                        hasUsageStats = hasUsageStats,
                        if (hasAccessibility) "辅助功能已启用" else "需要启用辅助功能服务"
                    )
                }
                ProtectionLevel.MAXIMUM -> {
                    val allGranted = hasAccessibility && hasUsageStats
                    val message = buildString {
                        if (!hasAccessibility) append("需要启用辅助功能服务\n")
                        if (!hasUsageStats) append("需要授权使用情况访问权限")
                        if (allGranted) append("所有权限已授权")
                    }.trim()
                    PermissionStatus(level, hasAccessibility, hasUsageStats, message)
                }
            }
        }

        fun resolveProtectionRuntimeState(
            context: Context,
            level: ProtectionLevel
        ): ProtectionRuntimeState {
            val hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
            val hasUsageStats = ForcedRunGuardService.hasUsageStatsPermission(context)
            return when (level) {
                ProtectionLevel.BASIC -> ProtectionRuntimeState(
                    level = level,
                    accessibilityEnabled = false,
                    usageStatsEnabled = false
                )
                ProtectionLevel.STANDARD -> ProtectionRuntimeState(
                    level = level,
                    accessibilityEnabled = hasAccessibility,
                    usageStatsEnabled = false
                )
                ProtectionLevel.MAXIMUM -> ProtectionRuntimeState(
                    level = level,
                    accessibilityEnabled = hasAccessibility,
                    usageStatsEnabled = hasUsageStats
                )
            }
        }
    }




    data class PermissionStatus(
        val level: ProtectionLevel,
        val hasAccessibility: Boolean,
        val hasUsageStats: Boolean,
        val message: String
    ) {
        val isFullyGranted: Boolean
            get() = when (level) {
                ProtectionLevel.BASIC -> true
                ProtectionLevel.STANDARD -> hasAccessibility
                ProtectionLevel.MAXIMUM -> hasAccessibility && hasUsageStats
            }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null


    private val _isInForcedRunMode = MutableStateFlow(false)
    val isInForcedRunMode: StateFlow<Boolean> = _isInForcedRunMode.asStateFlow()

    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()

    private val _currentConfig = MutableStateFlow<ForcedRunConfig?>(null)
    val currentConfig: StateFlow<ForcedRunConfig?> = _currentConfig.asStateFlow()


    private var countdownEndTimeMs: Long = 0L
    private var countdownCompletedAtMs: Long = 0L
    private var lastCountdownConfigFingerprint: String? = null


    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateRemainingTime()
            if (_isInForcedRunMode.value) {
                handler.postDelayed(this, 1000)
            }
        }
    }


    private val timeCheckRunnable = object : Runnable {
        override fun run() {
            checkTimeAndUpdateState()
            if (_isInForcedRunMode.value) {

                val interval = if (_remainingTimeMs.value in 1..CLOSING_THRESHOLD_MS) {
                    CHECK_INTERVAL_CLOSING
                } else {
                    CHECK_INTERVAL_NORMAL
                }
                handler.postDelayed(this, interval)
            }
        }
    }




    fun isInForcedRunPeriod(config: ForcedRunConfig): Boolean {
        if (!config.enabled) return false

        return when (config.mode) {
            ForcedRunMode.FIXED_TIME -> isInFixedTimePeriod(config)
            ForcedRunMode.COUNTDOWN -> _isInForcedRunMode.value
            ForcedRunMode.DURATION -> isInAccessPeriod(config)
        }
    }




    private fun isInFixedTimePeriod(config: ForcedRunConfig): Boolean {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val dayIndex = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1

        if (!config.activeDays.contains(dayIndex)) return false

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = parseTimeToMinutes(config.startTime)
        val endMinutes = parseTimeToMinutes(config.endTime)

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {

            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }




    private fun isInAccessPeriod(config: ForcedRunConfig): Boolean {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayIndex = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1

        if (!config.accessDays.contains(dayIndex)) return false

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = parseTimeToMinutes(config.accessStartTime)
        val endMinutes = parseTimeToMinutes(config.accessEndTime)

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }




    fun canEnterApp(config: ForcedRunConfig): Boolean {
        if (!config.enabled) return true

        return when (config.mode) {
            ForcedRunMode.FIXED_TIME -> true
            ForcedRunMode.COUNTDOWN -> true
            ForcedRunMode.DURATION -> isInAccessPeriod(config)
        }
    }




    fun getTimeUntilNextAccess(config: ForcedRunConfig): Long {
        if (!config.enabled || config.mode != ForcedRunMode.DURATION) return 0

        val calendar = Calendar.getInstance()
        val startMinutes = parseTimeToMinutes(config.accessStartTime)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val minutesUntilStart = if (currentMinutes < startMinutes) {
            startMinutes - currentMinutes
        } else {

            (24 * 60 - currentMinutes) + startMinutes
        }

        return minutesUntilStart * 60 * 1000L
    }


    private var targetPackageName: String? = null
    private var targetActivityClass: String? = null


    private var onStateChangedCallback: ((Boolean, ForcedRunConfig?) -> Unit)? = null




    fun setTargetActivity(packageName: String, activityClass: String) {
        targetPackageName = packageName
        targetActivityClass = activityClass
    }




    fun getTargetPackageName(): String? = targetPackageName
    fun getTargetActivityClass(): String? = targetActivityClass




    fun setOnStateChangedCallback(callback: (Boolean, ForcedRunConfig?) -> Unit) {
        onStateChangedCallback = callback
    }

    fun canAutoStart(config: ForcedRunConfig): Boolean {
        if (!config.enabled) return false
        return when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                val fingerprint = countdownFingerprint(config)
                lastCountdownConfigFingerprint != fingerprint || countdownCompletedAtMs == 0L
            }
            else -> true
        }
    }

    fun getNextStateCheckDelayMs(config: ForcedRunConfig): Long {
        if (!config.enabled) return 0L

        return when (config.mode) {
            ForcedRunMode.COUNTDOWN -> 0L
            ForcedRunMode.FIXED_TIME -> {
                if (isInFixedTimePeriod(config)) {
                    getTimeUntilWindowEnd(config.startTime, config.endTime)
                } else {
                    getTimeUntilNextWindowStart(config.activeDays, config.startTime)
                }
            }
            ForcedRunMode.DURATION -> {
                if (isInAccessPeriod(config)) {
                    getTimeUntilWindowEnd(config.accessStartTime, config.accessEndTime)
                } else {
                    getTimeUntilNextWindowStart(config.accessDays, config.accessStartTime)
                }
            }
        }
    }





    fun handleKeyEvent(keyCode: Int): Boolean {
        val config = _currentConfig.value ?: return false
        if (!_isInForcedRunMode.value) return false

        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> config.blockBackButton
            KeyEvent.KEYCODE_HOME -> config.blockHomeButton
            KeyEvent.KEYCODE_APP_SWITCH -> config.blockRecentApps
            KeyEvent.KEYCODE_POWER -> config.blockPowerButton
            else -> false
        }
    }




    fun verifyEmergencyPassword(password: String): Boolean {
        val config = _currentConfig.value ?: return false
        if (!config.allowEmergencyExit) return false

        val expected = config.emergencyPassword ?: return false
        return constantTimeEquals(password, expected)
    }








    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }




    fun emergencyExit(password: String): Boolean {
        if (verifyEmergencyPassword(password)) {
            stopForcedRunMode()
            return true
        }
        return false
    }




    fun formatRemainingTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }








    fun startForcedRunMode(config: ForcedRunConfig, remainingMs: Long = -1L) {
        if (config.mode == ForcedRunMode.COUNTDOWN) {
            val fingerprint = countdownFingerprint(config)
            if (remainingMs <= 0 && countdownCompletedAtMs > 0L && lastCountdownConfigFingerprint == fingerprint) {
                AppLogger.i(TAG, "倒计时模式已在当前配置下结束，跳过重复自动启动")
                return
            }
            lastCountdownConfigFingerprint = fingerprint
        } else {
            countdownCompletedAtMs = 0L
            lastCountdownConfigFingerprint = null
        }

        _currentConfig.value = config
        _isInForcedRunMode.value = true


        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                if (remainingMs > 0) {

                    countdownEndTimeMs = System.currentTimeMillis() + remainingMs
                    _remainingTimeMs.value = remainingMs
                    AppLogger.i(TAG, "从持久化恢复倒计时: 剩余 ${remainingMs / 1000}s")
                } else {

                    val totalMs = config.countdownMinutes * 60 * 1000L
                    countdownEndTimeMs = System.currentTimeMillis() + totalMs
                    _remainingTimeMs.value = totalMs
                }
            }
            ForcedRunMode.FIXED_TIME -> {
                updateRemainingTime()
            }
            ForcedRunMode.DURATION -> {
                updateRemainingTime()
            }
        }


        persistState(config)
        applyProtectionState(active = true, config = config)


        scheduleAlarms(config)


        handler.removeCallbacks(countdownRunnable)
        handler.post(countdownRunnable)


        handler.removeCallbacks(timeCheckRunnable)
        handler.post(timeCheckRunnable)

        onStateChangedCallback?.invoke(true, config)
        AppLogger.i(TAG, "Forced run mode started: mode=${config.mode}, " +
                "endTime=${if (config.mode == ForcedRunMode.COUNTDOWN) countdownEndTimeMs else "N/A"}")
    }




    fun stopForcedRunMode() {
        val config = _currentConfig.value
        if (config?.mode == ForcedRunMode.COUNTDOWN) {
            countdownCompletedAtMs = System.currentTimeMillis()
            if (lastCountdownConfigFingerprint == null) {
                lastCountdownConfigFingerprint = countdownFingerprint(config)
            }
        } else {
            countdownCompletedAtMs = 0L
            lastCountdownConfigFingerprint = null
        }

        _isInForcedRunMode.value = false
        _remainingTimeMs.value = 0L
        countdownEndTimeMs = 0L

        handler.removeCallbacks(countdownRunnable)
        handler.removeCallbacks(timeCheckRunnable)


        clearPersistedState()


        cancelAlarms()

        applyProtectionState(active = false, config = config)
        _currentConfig.value = null

        onStateChangedCallback?.invoke(false, config)
        AppLogger.i(TAG, "Forced run mode stopped")
    }








    fun restoreFromPersistence(): Boolean {
        if (!prefs.getBoolean(KEY_IS_ACTIVE, false)) {
            AppLogger.d(TAG, "无持久化状态需要恢复")
            return false
        }

        val configJson = prefs.getString(KEY_CONFIG_JSON, null)
        if (configJson == null) {
            AppLogger.w(TAG, "持久化状态损坏（缺少 config），清除")
            clearPersistedState()
            return false
        }

        val config = try {
            deserializeConfig(configJson)
        } catch (e: Exception) {
            AppLogger.e(TAG, "反序列化配置失败，清除持久化状态", e)
            clearPersistedState()
            return false
        }

        if (!config.enabled) {
            AppLogger.d(TAG, "持久化配置已禁用，清除")
            clearPersistedState()
            return false
        }

        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                val endTime = prefs.getLong(KEY_COUNTDOWN_END_TIME, 0L)
                val remaining = endTime - System.currentTimeMillis()

                if (remaining <= 0) {
                    AppLogger.i(TAG, "倒计时已在进程死亡期间过期，清除")
                    clearPersistedState()
                    return false
                }

                AppLogger.i(TAG, "恢复倒计时: 剩余 ${remaining / 1000}s (原结束时间 $endTime)")
                startForcedRunMode(config, remaining)
                return true
            }
            ForcedRunMode.FIXED_TIME -> {
                if (isInFixedTimePeriod(config)) {
                    AppLogger.i(TAG, "恢复固定时间段强制运行")
                    startForcedRunMode(config)
                    return true
                } else {
                    AppLogger.i(TAG, "固定时间段已过期，清除")
                    clearPersistedState()
                    return false
                }
            }
            ForcedRunMode.DURATION -> {
                if (isInAccessPeriod(config)) {
                    AppLogger.i(TAG, "恢复限时模式强制运行")
                    startForcedRunMode(config)
                    return true
                } else {
                    AppLogger.i(TAG, "限时时段已过期，清除")
                    clearPersistedState()
                    return false
                }
            }
        }
    }






    private fun persistState(config: ForcedRunConfig) {
        if (!config.persistCountdown && config.mode == ForcedRunMode.COUNTDOWN) {
            AppLogger.d(TAG, "persistCountdown=false，跳过持久化")
            return
        }

        prefs.edit().apply {
            putBoolean(KEY_IS_ACTIVE, true)
            putString(KEY_MODE, config.mode.name)
            putLong(KEY_START_TIMESTAMP, System.currentTimeMillis())
            putString(KEY_CONFIG_JSON, serializeConfig(config))

            if (config.mode == ForcedRunMode.COUNTDOWN) {
                putLong(KEY_COUNTDOWN_END_TIME, countdownEndTimeMs)
            }

            apply()
        }

        AppLogger.d(TAG, "状态已持久化: mode=${config.mode}")
    }




    private fun clearPersistedState() {
        prefs.edit().apply {
            remove(KEY_IS_ACTIVE)
            remove(KEY_MODE)
            remove(KEY_COUNTDOWN_END_TIME)
            remove(KEY_CONFIG_JSON)
            remove(KEY_START_TIMESTAMP)
            apply()
        }
        AppLogger.d(TAG, "持久化状态已清除")
    }




    private fun serializeConfig(config: ForcedRunConfig): String {
        return buildString {
            append("{")
            append("\"enabled\":${config.enabled},")
            append("\"mode\":\"${config.mode.name}\",")
            append("\"startTime\":\"${config.startTime}\",")
            append("\"endTime\":\"${config.endTime}\",")
            append("\"activeDays\":[${config.activeDays.joinToString(",")}],")
            append("\"countdownMinutes\":${config.countdownMinutes},")
            append("\"accessStartTime\":\"${config.accessStartTime}\",")
            append("\"accessEndTime\":\"${config.accessEndTime}\",")
            append("\"accessDays\":[${config.accessDays.joinToString(",")}],")
            append("\"protectionLevel\":\"${config.protectionLevel.name}\",")
            append("\"blockSystemUI\":${config.blockSystemUI},")
            append("\"blockBackButton\":${config.blockBackButton},")
            append("\"blockHomeButton\":${config.blockHomeButton},")
            append("\"blockRecentApps\":${config.blockRecentApps},")
            append("\"blockNotifications\":${config.blockNotifications},")
            append("\"blockPowerButton\":${config.blockPowerButton},")
            append("\"showCountdown\":${config.showCountdown},")
            append("\"allowEmergencyExit\":${config.allowEmergencyExit},")
            append("\"emergencyPassword\":${if (config.emergencyPassword != null) "\"${config.emergencyPassword}\"" else "null"},")
            append("\"showStartNotification\":${config.showStartNotification},")
            append("\"showEndNotification\":${config.showEndNotification},")
            append("\"warningBeforeEnd\":${config.warningBeforeEnd},")
            append("\"persistCountdown\":${config.persistCountdown}")
            append("}")
        }
    }




    private fun deserializeConfig(json: String): ForcedRunConfig {
        fun String.extractString(key: String): String? {
            val pattern = "\"$key\":\"([^\"]*)\""
            val match = Regex(pattern).find(this) ?: return null
            return match.groupValues[1]
        }

        fun String.extractBoolean(key: String): Boolean {
            val pattern = "\"$key\":(true|false)"
            val match = Regex(pattern).find(this) ?: return false
            return match.groupValues[1].toBoolean()
        }

        fun String.extractInt(key: String): Int {
            val pattern = "\"$key\":(\\d+)"
            val match = Regex(pattern).find(this) ?: return 0
            return match.groupValues[1].toInt()
        }

        fun String.extractIntList(key: String): List<Int> {
            val pattern = "\"$key\":\\[([^\\]]*)\\]"
            val match = Regex(pattern).find(this) ?: return emptyList()
            return match.groupValues[1].split(",").mapNotNull { it.trim().toIntOrNull() }
        }

        fun String.extractNullableString(key: String): String? {

            if (this.contains("\"$key\":null")) return null
            return extractString(key)
        }

        return ForcedRunConfig(
            enabled = json.extractBoolean("enabled"),
            mode = try { ForcedRunMode.valueOf(json.extractString("mode") ?: "FIXED_TIME") } catch (_: Exception) { ForcedRunMode.FIXED_TIME },
            startTime = json.extractString("startTime") ?: "08:00",
            endTime = json.extractString("endTime") ?: "12:00",
            activeDays = json.extractIntList("activeDays").ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
            countdownMinutes = json.extractInt("countdownMinutes").takeIf { it > 0 } ?: 60,
            accessStartTime = json.extractString("accessStartTime") ?: "08:00",
            accessEndTime = json.extractString("accessEndTime") ?: "22:00",
            accessDays = json.extractIntList("accessDays").ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
            protectionLevel = try { ProtectionLevel.valueOf(json.extractString("protectionLevel") ?: "MAXIMUM") } catch (_: Exception) { ProtectionLevel.MAXIMUM },
            blockSystemUI = json.extractBoolean("blockSystemUI"),
            blockBackButton = json.extractBoolean("blockBackButton"),
            blockHomeButton = json.extractBoolean("blockHomeButton"),
            blockRecentApps = json.extractBoolean("blockRecentApps"),
            blockNotifications = json.extractBoolean("blockNotifications"),
            blockPowerButton = json.extractBoolean("blockPowerButton"),
            showCountdown = json.extractBoolean("showCountdown"),
            allowEmergencyExit = json.extractBoolean("allowEmergencyExit"),
            emergencyPassword = json.extractNullableString("emergencyPassword"),
            showStartNotification = json.extractBoolean("showStartNotification"),
            showEndNotification = json.extractBoolean("showEndNotification"),
            warningBeforeEnd = json.extractInt("warningBeforeEnd").takeIf { it > 0 } ?: 5,
            persistCountdown = json.extractBoolean("persistCountdown")
        )
    }








    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarms(config: ForcedRunConfig) {
        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {

                scheduleExactAlarm(
                    triggerAtMs = countdownEndTimeMs,
                    action = ACTION_FORCED_RUN_END,
                    requestCode = ALARM_REQUEST_CODE_END
                )
                AppLogger.d(TAG, "已设置倒计时结束闹钟: ${countdownEndTimeMs}")
            }
            ForcedRunMode.FIXED_TIME -> {

                val endMs = getNextTimeMs(config.endTime)
                scheduleExactAlarm(
                    triggerAtMs = endMs,
                    action = ACTION_FORCED_RUN_END,
                    requestCode = ALARM_REQUEST_CODE_END
                )
                AppLogger.d(TAG, "已设置固定时间段结束闹钟: $endMs")
            }
            ForcedRunMode.DURATION -> {

                val endMs = getNextTimeMs(config.accessEndTime)
                scheduleExactAlarm(
                    triggerAtMs = endMs,
                    action = ACTION_FORCED_RUN_END,
                    requestCode = ALARM_REQUEST_CODE_END
                )
                AppLogger.d(TAG, "已设置限时模式结束闹钟: $endMs")
            }
        }
    }




    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleExactAlarm(triggerAtMs: Long, action: String, requestCode: Int) {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {

            AppLogger.w(TAG, "精确闹钟权限不足，降级使用非精确闹钟", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }




    private fun cancelAlarms() {
        listOf(
            ALARM_REQUEST_CODE_END to ACTION_FORCED_RUN_END,
            ALARM_REQUEST_CODE_START to ACTION_FORCED_RUN_START,
            ALARM_REQUEST_CODE_CHECK to ACTION_FORCED_RUN_CHECK
        ).forEach { (requestCode, action) ->
            val intent = Intent(action).apply {
                setPackage(context.packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        AppLogger.d(TAG, "所有闹钟已取消")
    }




    private fun getNextTimeMs(timeStr: String): Long {
        val minutes = parseTimeToMinutes(timeStr)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun getTimeUntilWindowEnd(startTime: String, endTime: String): Long {
        val now = Calendar.getInstance()
        val startMinutes = parseTimeToMinutes(startTime)
        val endMinutes = parseTimeToMinutes(endTime)

        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endMinutes / 60)
            set(Calendar.MINUTE, endMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (endMinutes <= startMinutes && now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) >= startMinutes) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return (endCalendar.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    private fun getTimeUntilNextWindowStart(days: List<Int>, startTime: String): Long {
        val now = Calendar.getInstance()
        val startMinutes = parseTimeToMinutes(startTime)

        repeat(8) { offset ->
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                set(Calendar.MINUTE, startMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dayOfWeekToIndex(candidate.get(Calendar.DAY_OF_WEEK)) in days &&
                candidate.timeInMillis > now.timeInMillis
            ) {
                return (candidate.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
            }
        }

        return 24 * 60 * 60 * 1000L
    }

    private fun dayOfWeekToIndex(dayOfWeek: Int): Int {
        return if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
    }








    private fun updateRemainingTime() {
        val config = _currentConfig.value ?: return

        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {

                val remaining = countdownEndTimeMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _remainingTimeMs.value = 0
                    stopForcedRunMode()
                } else {
                    _remainingTimeMs.value = remaining

                    if (config.persistCountdown && remaining % 30_000 < 1100) {

                        persistState(config)
                    }
                }
            }
            ForcedRunMode.FIXED_TIME -> {
                val calendar = Calendar.getInstance()
                val endMinutes = parseTimeToMinutes(config.endTime)
                val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                val currentSeconds = calendar.get(Calendar.SECOND)

                val remainingMinutes = if (endMinutes > currentMinutes) {
                    endMinutes - currentMinutes
                } else {
                    (24 * 60 - currentMinutes) + endMinutes
                }
                _remainingTimeMs.value = (remainingMinutes * 60L - currentSeconds) * 1000L
            }
            ForcedRunMode.DURATION -> {
                val calendar = Calendar.getInstance()
                val endMinutes = parseTimeToMinutes(config.accessEndTime)
                val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                val currentSeconds = calendar.get(Calendar.SECOND)

                val remainingMinutes = if (endMinutes > currentMinutes) {
                    endMinutes - currentMinutes
                } else {
                    0
                }
                _remainingTimeMs.value = (remainingMinutes * 60L - currentSeconds) * 1000L
            }
        }
    }




    private fun checkTimeAndUpdateState() {
        val config = _currentConfig.value ?: return

        when (config.mode) {
            ForcedRunMode.FIXED_TIME -> {
                if (!isInFixedTimePeriod(config) && _isInForcedRunMode.value) {
                    stopForcedRunMode()
                }
            }
            ForcedRunMode.DURATION -> {
                if (!isInAccessPeriod(config) && _isInForcedRunMode.value) {
                    stopForcedRunMode()
                }
            }
            else -> {  }
        }
    }

    private fun applyProtectionState(active: Boolean, config: ForcedRunConfig?) {
        if (!active || config == null) {
            ForcedRunAccessibilityService.blockBackKey = false
            ForcedRunAccessibilityService.stopForcedRun()
            ForcedRunGuardService.stop(context)
            return
        }

        val runtimeState = resolveProtectionRuntimeState(context, config.protectionLevel)
        if (runtimeState.isDegraded) {
            AppLogger.w(
                TAG,
                "强制运行保护已降级: level=${config.protectionLevel}, " +
                    "accessibility=${runtimeState.accessibilityEnabled}, usageStats=${runtimeState.usageStatsEnabled}"
            )
        }

        val packageName = targetPackageName ?: context.packageName
        val activityClass = targetActivityClass

        if (runtimeState.canUseAccessibilityProtection && activityClass != null) {
            ForcedRunAccessibilityService.blockBackKey = config.blockBackButton
            ForcedRunAccessibilityService.startForcedRun(
                packageName = packageName,
                activityClass = activityClass
            )
        } else {
            ForcedRunAccessibilityService.blockBackKey = false
            ForcedRunAccessibilityService.stopForcedRun()
        }

        if (runtimeState.canUseGuardService) {
            ForcedRunGuardService.start(
                context = context,
                packageName = packageName,
                activityClass = activityClass ?: packageName,
                aggressive = config.protectionLevel == ProtectionLevel.MAXIMUM && !runtimeState.canUseAccessibilityProtection,
                remainingMs = _remainingTimeMs.value
            )
        } else {
            ForcedRunGuardService.stop(context)
        }
    }

    private fun countdownFingerprint(config: ForcedRunConfig): String {
        return listOf(
            config.countdownMinutes,
            config.protectionLevel.name,
            config.blockSystemUI,
            config.blockBackButton,
            config.blockHomeButton,
            config.blockRecentApps,
            config.blockPowerButton,
            config.allowEmergencyExit,
            config.persistCountdown
        ).joinToString("|")
    }




    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }
}
