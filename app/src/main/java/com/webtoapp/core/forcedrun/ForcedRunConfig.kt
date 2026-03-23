package com.webtoapp.core.forcedrun

/**
 * 强制运行模式
 */
enum class ForcedRunMode {
    FIXED_TIME,      // 固定时间段（如每天 8:00-12:00）
    COUNTDOWN,       // 倒计时模式（如运行 2 小时后自动退出）
    DURATION         // 限时模式（如只能在特定时间段内进入）
}

/**
 * 强制运行配置
 * 
 * 用于配置应用的强制运行行为：
 * - 在指定时间段内强制运行，无法退出
 * - 沉浸式全屏，屏蔽系统窗口
 * - 时间段结束后自动退出
 */
data class ForcedRunConfig(
    val enabled: Boolean = false,                    // Yes否启用强制运行
    val mode: ForcedRunMode = ForcedRunMode.FIXED_TIME, // 运行模式
    
    // 固定时间段模式配置
    val startTime: String = "08:00",                 // Start时间（HH:mm 格式）
    val endTime: String = "12:00",                   // End时间（HH:mm 格式）
    val activeDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 生效日期（1-7 代表周一到周日）
    
    // 倒计时模式配置
    val countdownMinutes: Int = 60,                  // 倒计时时长（分钟）
    
    // 限时模式配置（只能在特定时间段内进入）
    val accessStartTime: String = "08:00",           // 可进入开始时间
    val accessEndTime: String = "22:00",             // 可进入结束时间
    val accessDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 可进入日期
    
    // 防护级别配置
    val protectionLevel: ProtectionLevel = ProtectionLevel.MAXIMUM, // 防护级别
    
    // 通用配置
    val blockSystemUI: Boolean = true,               // 屏蔽系统UI（状态栏、导航栏）
    val blockBackButton: Boolean = true,             // 屏蔽返回键
    val blockHomeButton: Boolean = true,             // 屏蔽Home键（通过辅助功能实现）
    val blockRecentApps: Boolean = true,             // 屏蔽最近任务键（通过辅助功能实现）
    val blockNotifications: Boolean = true,          // 屏蔽通知
    val blockPowerButton: Boolean = false,           // 屏蔽电源键（需要特殊权限）
    val showCountdown: Boolean = true,               // Show剩余时间倒计时
    val allowEmergencyExit: Boolean = false,         // Allow紧急退出（连续点击5次特定区域）
    val emergencyPassword: String? = null,           // 紧急退出密码（可选）
    
    // 提示配置
    val showStartNotification: Boolean = true,       // Start时显示通知
    val showEndNotification: Boolean = true,         // End时显示通知
    val warningBeforeEnd: Int = 5                    // End前几分钟提醒
) {
    companion object {
        /** 禁用 */
        val DISABLED = ForcedRunConfig(enabled = false)
        
        /** 学习模式预设（工作日 8:00-12:00, 14:00-18:00） */
        val STUDY_MODE = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.FIXED_TIME,
            startTime = "08:00",
            endTime = "12:00",
            activeDays = listOf(1, 2, 3, 4, 5), // 周一到周五
            blockSystemUI = true,
            blockBackButton = true,
            blockHomeButton = true,
            showCountdown = true
        )
        
        /** 专注模式预设（倒计时 25 分钟，番茄工作法） */
        val FOCUS_MODE = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.COUNTDOWN,
            countdownMinutes = 25,
            blockSystemUI = true,
            blockBackButton = true,
            showCountdown = true,
            allowEmergencyExit = true
        )
        
        /** 儿童模式预设（限制使用时间） */
        val KIDS_MODE = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.DURATION,
            accessStartTime = "08:00",
            accessEndTime = "20:00",
            accessDays = listOf(1, 2, 3, 4, 5, 6, 7),
            blockSystemUI = true,
            blockBackButton = true,
            blockHomeButton = true,
            blockRecentApps = true,
            emergencyPassword = "1234"
        )
    }
}
