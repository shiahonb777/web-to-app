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

/**
 * 强制运行防护级别
 */
enum class ProtectionLevel {
    BASIC,      // 基础：仅拦截返回键（几乎无效）
    STANDARD,   // 标准：辅助功能服务（需要用户授权）
    MAXIMUM     // 最强：辅助功能 + 前台守护服务 + UsageStats
}

/**
 * 强制运行管理器 - 优化版
 * 
 * 三层防护体系：
 * 1. AccessibilityService - 监控窗口变化，毫秒级响应拉回
 * 2. ForegroundService + UsageStats - 轮询检测前台应用
 * 3. Lock Task Mode - 系统级锁定（需要设备管理员权限）
 * 
 * 功能：
 * - 时间检测和自动启动/退出
 * - 真正有效的系统UI屏蔽
 * - 按键拦截（返回键）
 * - 窗口切换拦截（通过辅助功能）
 * - 倒计时显示
 * 
 * 优化点：
 * - 倒计时持久化：进程被杀后可恢复
 * - 时间戳计时：消除 Handler 累积漂移
 * - AlarmManager 精确唤醒：Doze 模式下也能触发
 * - 自适应检查频率：接近结束时加速检查
 * - 常量时间密码比较：防止时序攻击
 */
@SuppressLint("StaticFieldLeak")
class ForcedRunManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ForcedRunManager"
        const val ACTION_FORCED_RUN_START = "com.webtoapp.ACTION_FORCED_RUN_START"
        const val ACTION_FORCED_RUN_END = "com.webtoapp.ACTION_FORCED_RUN_END"
        const val ACTION_FORCED_RUN_CHECK = "com.webtoapp.ACTION_FORCED_RUN_CHECK"
        const val EXTRA_APP_ID = "app_id"
        const val PREFS_NAME = "forced_run_prefs"
        
        // 持久化 SharedPreferences 键名
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_MODE = "mode"
        private const val KEY_COUNTDOWN_END_TIME = "countdown_end_time"
        private const val KEY_CONFIG_JSON = "config_json"
        private const val KEY_START_TIMESTAMP = "start_timestamp"
        
        // AlarmManager PendingIntent 请求码
        private const val ALARM_REQUEST_CODE_END = 10001
        private const val ALARM_REQUEST_CODE_START = 10002
        private const val ALARM_REQUEST_CODE_CHECK = 10003
        
        // 时间检查间隔
        private const val CHECK_INTERVAL_NORMAL = 10_000L    // 正常：10 秒
        private const val CHECK_INTERVAL_CLOSING = 1_000L    // 临近结束（2分钟内）：1 秒
        private const val CLOSING_THRESHOLD_MS = 2 * 60 * 1000L  // 2 分钟阈值
        
        @Volatile
        private var instance: ForcedRunManager? = null
        
        fun getInstance(context: Context): ForcedRunManager {
            return instance ?: synchronized(this) {
                instance ?: ForcedRunManager(context.applicationContext).also { instance = it }
            }
        }
        
        /**
         * 检查当前防护级别的权限是否已授权
         */
        fun checkProtectionPermissions(context: Context, level: ProtectionLevel): PermissionStatus {
            return when (level) {
                ProtectionLevel.BASIC -> PermissionStatus(true, true, "基础防护无需额外权限")
                ProtectionLevel.STANDARD -> {
                    val hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
                    PermissionStatus(
                        hasAccessibility,
                        hasAccessibility,
                        if (hasAccessibility) "辅助功能已启用" else "需要启用辅助功能服务"
                    )
                }
                ProtectionLevel.MAXIMUM -> {
                    val hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
                    val hasUsageStats = ForcedRunGuardService.hasUsageStatsPermission(context)
                    val allGranted = hasAccessibility && hasUsageStats
                    val message = buildString {
                        if (!hasAccessibility) append("需要启用辅助功能服务\n")
                        if (!hasUsageStats) append("需要授权使用情况访问权限")
                        if (allGranted) append("所有权限已授权")
                    }.trim()
                    PermissionStatus(hasAccessibility, hasUsageStats, message)
                }
            }
        }
    }

    init {
        synchronized(Companion) {
            if (instance == null) {
                instance = this
            }
        }
    }
    
    /**
     * 权限状态
     */
    data class PermissionStatus(
        val hasAccessibility: Boolean,
        val hasUsageStats: Boolean,
        val message: String
    ) {
        val isFullyGranted: Boolean get() = hasAccessibility && hasUsageStats
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 状态流
    private val _isInForcedRunMode = MutableStateFlow(false)
    val isInForcedRunMode: StateFlow<Boolean> = _isInForcedRunMode.asStateFlow()
    
    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()
    
    private val _currentConfig = MutableStateFlow<ForcedRunConfig?>(null)
    val currentConfig: StateFlow<ForcedRunConfig?> = _currentConfig.asStateFlow()
    
    // 倒计时结束的绝对时间戳（用于 COUNTDOWN 模式，消除漂移）
    private var countdownEndTimeMs: Long = 0L
    
    // 倒计时更新 Runnable
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateRemainingTime()
            if (_isInForcedRunMode.value) {
                handler.postDelayed(this, 1000) // 每秒更新
            }
        }
    }
    
    // 自适应时间检查 Runnable
    private val timeCheckRunnable = object : Runnable {
        override fun run() {
            checkTimeAndUpdateState()
            if (_isInForcedRunMode.value) {
                // 自适应频率：接近结束时加速到每秒检查
                val interval = if (_remainingTimeMs.value in 1..CLOSING_THRESHOLD_MS) {
                    CHECK_INTERVAL_CLOSING
                } else {
                    CHECK_INTERVAL_NORMAL
                }
                handler.postDelayed(this, interval)
            }
        }
    }
    
    /**
     * 检查当前时间是否在强制运行时间段内
     */
    fun isInForcedRunPeriod(config: ForcedRunConfig): Boolean {
        if (!config.enabled) return false
        
        return when (config.mode) {
            ForcedRunMode.FIXED_TIME -> isInFixedTimePeriod(config)
            ForcedRunMode.COUNTDOWN -> _isInForcedRunMode.value // 倒计时模式由手动启动控制
            ForcedRunMode.DURATION -> isInAccessPeriod(config)
        }
    }
    
    /**
     * 检查是否在固定时间段内
     */
    private fun isInFixedTimePeriod(config: ForcedRunConfig): Boolean {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY = 1, 转换为我们的格式（周一=1）
        val dayIndex = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
        
        if (!config.activeDays.contains(dayIndex)) return false
        
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = parseTimeToMinutes(config.startTime)
        val endMinutes = parseTimeToMinutes(config.endTime)
        
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // 跨午夜的情况
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
    
    /**
     * 检查是否在可访问时间段内（限时模式）
     */
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
    
    /**
     * 检查是否可以进入应用（限时模式）
     */
    fun canEnterApp(config: ForcedRunConfig): Boolean {
        if (!config.enabled) return true
        
        return when (config.mode) {
            ForcedRunMode.FIXED_TIME -> true // 固定时间模式总是可以进入
            ForcedRunMode.COUNTDOWN -> true // 倒计时模式总是可以进入
            ForcedRunMode.DURATION -> isInAccessPeriod(config) // 限时模式检查时间
        }
    }
    
    /**
     * 获取距离下次可进入的剩余时间（毫秒）
     */
    fun getTimeUntilNextAccess(config: ForcedRunConfig): Long {
        if (!config.enabled || config.mode != ForcedRunMode.DURATION) return 0
        
        val calendar = Calendar.getInstance()
        val startMinutes = parseTimeToMinutes(config.accessStartTime)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        
        val minutesUntilStart = if (currentMinutes < startMinutes) {
            startMinutes - currentMinutes
        } else {
            // 明天的开始时间
            (24 * 60 - currentMinutes) + startMinutes
        }
        
        return minutesUntilStart * 60 * 1000L
    }
    
    // 当前目标 Activity 信息（用于辅助功能服务拉回）
    private var targetPackageName: String? = null
    private var targetActivityClass: String? = null
    
    // 状态变化回调
    private var onStateChangedCallback: ((Boolean, ForcedRunConfig?) -> Unit)? = null
    
    /**
     * 设置目标 Activity（用于辅助功能服务拉回）
     */
    fun setTargetActivity(packageName: String, activityClass: String) {
        targetPackageName = packageName
        targetActivityClass = activityClass
    }
    
    /**
     * 获取目标 Activity 信息
     */
    fun getTargetPackageName(): String? = targetPackageName
    fun getTargetActivityClass(): String? = targetActivityClass
    
    /**
     * 设置状态变化回调
     */
    fun setOnStateChangedCallback(callback: (Boolean, ForcedRunConfig?) -> Unit) {
        onStateChangedCallback = callback
    }
    
    /**
     * 处理按键事件
     * @return true 表示拦截该按键
     */
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
    
    /**
     * 验证紧急退出密码（常量时间比较，防止时序攻击）
     */
    fun verifyEmergencyPassword(password: String): Boolean {
        val config = _currentConfig.value ?: return false
        if (!config.allowEmergencyExit) return false
        
        val expected = config.emergencyPassword ?: return false
        return constantTimeEquals(password, expected)
    }
    
    /**
     * 常量时间字符串比较
     * 
     * 使用 MessageDigest.isEqual 避免时序攻击：
     * 普通 == 会在发现第一个不匹配字符时提前返回，
     * 攻击者可通过测量比较耗时推测正确密码。
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }
    
    /**
     * 紧急退出
     */
    fun emergencyExit(password: String): Boolean {
        if (verifyEmergencyPassword(password)) {
            stopForcedRunMode()
            return true
        }
        return false
    }
    
    /**
     * 格式化剩余时间
     */
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
    
    /**
     * 启动强制运行模式
     * 
     * @param config 强制运行配置
     * @param remainingMs 剩余时间（仅用于从持久化恢复倒计时模式时传入,
     *                     -1 表示全新启动）
     */
    fun startForcedRunMode(config: ForcedRunConfig, remainingMs: Long = -1L) {
        _currentConfig.value = config
        _isInForcedRunMode.value = true
        
        // 根据模式设置剩余时间
        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                if (remainingMs > 0) {
                    // 从持久化恢复：使用传入的剩余时间
                    countdownEndTimeMs = System.currentTimeMillis() + remainingMs
                    _remainingTimeMs.value = remainingMs
                    AppLogger.i(TAG, "从持久化恢复倒计时: 剩余 ${remainingMs / 1000}s")
                } else {
                    // 全新启动：计算结束时间戳
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
        
        // 持久化状态
        persistState(config)
        
        // 设置 AlarmManager 精确唤醒
        scheduleAlarms(config)
        
        // 启动倒计时更新
        handler.removeCallbacks(countdownRunnable)
        handler.post(countdownRunnable)
        
        // 启动时间检查
        handler.removeCallbacks(timeCheckRunnable)
        handler.post(timeCheckRunnable)
        
        onStateChangedCallback?.invoke(true, config)
        AppLogger.i(TAG, "Forced run mode started: mode=${config.mode}, " +
                "endTime=${if (config.mode == ForcedRunMode.COUNTDOWN) countdownEndTimeMs else "N/A"}")
    }
    
    /**
     * 停止强制运行模式
     */
    fun stopForcedRunMode() {
        _isInForcedRunMode.value = false
        _remainingTimeMs.value = 0L
        countdownEndTimeMs = 0L
        
        handler.removeCallbacks(countdownRunnable)
        handler.removeCallbacks(timeCheckRunnable)
        
        // 清除持久化状态
        clearPersistedState()
        
        // 取消所有闹钟
        cancelAlarms()
        
        val config = _currentConfig.value
        _currentConfig.value = null
        
        onStateChangedCallback?.invoke(false, config)
        AppLogger.i(TAG, "Forced run mode stopped")
    }
    
    /**
     * 从持久化状态恢复强制运行
     * 
     * 在进程重启（含开机自启）时调用，检查是否有未完成的强制运行会话。
     * 
     * @return true 如果成功恢复了一个活跃的强制运行会话
     */
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
    
    // ======================== 持久化 ========================
    
    /**
     * 将当前状态持久化到 SharedPreferences
     */
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
    
    /**
     * 清除持久化状态
     */
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
    
    /**
     * 序列化配置为 JSON 字符串（简化实现，不依赖 Gson）
     */
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
    
    /**
     * 反序列化 JSON 字符串为配置（简化手动解析）
     */
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
            // 检查 null 值
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
    
    // ======================== AlarmManager 精确调度 ========================
    
    /**
     * 根据配置设置 AlarmManager 精确唤醒
     * 
     * 使用 setExactAndAllowWhileIdle() 确保 Doze 模式下也能触发。
     */
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarms(config: ForcedRunConfig) {
        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                // 设置倒计时结束闹钟
                scheduleExactAlarm(
                    triggerAtMs = countdownEndTimeMs,
                    action = ACTION_FORCED_RUN_END,
                    requestCode = ALARM_REQUEST_CODE_END
                )
                AppLogger.d(TAG, "已设置倒计时结束闹钟: ${countdownEndTimeMs}")
            }
            ForcedRunMode.FIXED_TIME -> {
                // 设置固定时间段结束闹钟
                val endMs = getNextTimeMs(config.endTime)
                scheduleExactAlarm(
                    triggerAtMs = endMs,
                    action = ACTION_FORCED_RUN_END,
                    requestCode = ALARM_REQUEST_CODE_END
                )
                AppLogger.d(TAG, "已设置固定时间段结束闹钟: $endMs")
            }
            ForcedRunMode.DURATION -> {
                // 设置限时模式结束闹钟
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
    
    /**
     * 设置精确闹钟
     */
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
            // Android 12+ 需要 SCHEDULE_EXACT_ALARM 权限，降级使用非精确
            AppLogger.w(TAG, "精确闹钟权限不足，降级使用非精确闹钟", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }
    
    /**
     * 取消所有已设置的闹钟
     */
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
    
    /**
     * 将 "HH:mm" 时间字符串转为今天（或明天）的绝对毫秒时间戳
     */
    private fun getNextTimeMs(timeStr: String): Long {
        val minutes = parseTimeToMinutes(timeStr)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 如果已过今天的该时间，则设为明天
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
    
    // ======================== 时间更新 ========================
    
    /**
     * 更新剩余时间
     * 
     * 优化：COUNTDOWN 模式使用绝对时间戳计算，消除 Handler 累积漂移。
     */
    private fun updateRemainingTime() {
        val config = _currentConfig.value ?: return
        
        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                // 基于绝对时间戳计算，完全消除漂移
                val remaining = countdownEndTimeMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _remainingTimeMs.value = 0
                    stopForcedRunMode()
                } else {
                    _remainingTimeMs.value = remaining
                    // 每次更新时刷新持久化（仅在剩余时间有意义变化时）
                    if (config.persistCountdown && remaining % 30_000 < 1100) {
                        // 约每 30 秒刷新一次持久化，减少 IO
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
    
    /**
     * 检查时间并更新状态（用于固定时间/限时模式的自动启停）
     */
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
            else -> { /* countdown handled by updateRemainingTime */ }
        }
    }
    
    /**
     * 解析时间字符串为分钟数
     */
    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }
}
