package com.webtoapp.core.blacktech

import com.google.gson.annotations.SerializedName

/**
 * 黑科技功能配置
 * 
 * 独立的功能模块，包含各种高级/危险功能配置
 * 与强制运行功能分离，可独立启用
 * 
 * v2.0 新增：
 * - 网络控制（WiFi 热点、断网）
 * - 核弹模式（一键全开）
 * - 隐身模式（一键全隐）
 * - 自定义警报序列
 * - 屏幕常亮
 */
data class BlackTechConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,                     // 是否启用黑科技功能
    
    // ===== Volume控制 =====
    @SerializedName("forceMaxVolume")
    val forceMaxVolume: Boolean = false,              // 强制音量最大
    @SerializedName("forceMuteMode")
    val forceMuteMode: Boolean = false,               // 强制静音模式
    @SerializedName("forceBlockVolumeKeys")
    val forceBlockVolumeKeys: Boolean = false,        // 强制屏蔽音量键
    
    // ===== 震动与闪光 =====
    @SerializedName("forceMaxVibration")
    val forceMaxVibration: Boolean = false,           // 强制最大震动
    @SerializedName("forceFlashlight")
    val forceFlashlight: Boolean = false,             // 强制打开闪光灯
    @SerializedName("flashlightStrobeMode")
    val flashlightStrobeMode: Boolean = false,        // 闪光灯爆闪模式
    
    // 闪光灯高级模式 (与 forceFlashlight 配合使用)
    @SerializedName("flashlightMorseMode")
    val flashlightMorseMode: Boolean = false,         // 闪光灯摩斯电码模式
    @SerializedName("flashlightMorseText")
    val flashlightMorseText: String = "",             // 摩斯电码文本内容
    @SerializedName("flashlightMorseUnitMs")
    val flashlightMorseUnitMs: Int = 200,             // 摩斯电码基本时间单位 (ms)
    @SerializedName("flashlightSosMode")
    val flashlightSosMode: Boolean = false,           // 闪光灯 SOS 求救模式
    @SerializedName("flashlightHeartbeatMode")
    val flashlightHeartbeatMode: Boolean = false,     // 闪光灯心跳模式
    @SerializedName("flashlightBreathingMode")
    val flashlightBreathingMode: Boolean = false,     // 闪光灯呼吸灯模式
    @SerializedName("flashlightEmergencyMode")
    val flashlightEmergencyMode: Boolean = false,     // 闪光灯紧急三闪模式
    
    // ===== 自定义警报序列 =====
    @SerializedName("customAlarmEnabled")
    val customAlarmEnabled: Boolean = false,          // 启用自定义警报
    @SerializedName("customAlarmPattern")
    val customAlarmPattern: String = "",              // 自定义警报节奏 (逗号分隔：亮ms,灭ms,亮ms,灭ms...)
    @SerializedName("customAlarmVibSync")
    val customAlarmVibSync: Boolean = true,           // 警报震动与闪光同步
    
    // ===== System控制 =====
    @SerializedName("forceAirplaneMode")
    val forceAirplaneMode: Boolean = false,           // 强制开启飞行模式（需要系统权限）
    @SerializedName("forceMaxPerformance")
    val forceMaxPerformance: Boolean = false,         // 强制最大性能模式（高CPU/GPU占用）
    @SerializedName("forceBlockPowerKey")
    val forceBlockPowerKey: Boolean = false,          // 强制屏蔽电源键
    
    // ===== 屏幕控制 =====
    @SerializedName("forceBlackScreen")
    val forceBlackScreen: Boolean = false,            // 强制全黑屏无法滑动
    @SerializedName("forceScreenRotation")
    val forceScreenRotation: Boolean = false,         // 强制屏幕持续翻转
    @SerializedName("forceBlockTouch")
    val forceBlockTouch: Boolean = false,             // 强制屏蔽触摸
    @SerializedName("forceScreenAwake")
    val forceScreenAwake: Boolean = false,            // 强制屏幕常亮
    
    // ===== 网络控制 (v2.0) =====
    @SerializedName("forceWifiHotspot")
    val forceWifiHotspot: Boolean = false,            // 强制开启 WiFi 热点
    @SerializedName("hotspotSsid")
    val hotspotSsid: String = "WebToApp_AP",          // 热点名称
    @SerializedName("hotspotPassword")
    val hotspotPassword: String = "12345678",         // 热点密码 (至少8位)
    @SerializedName("forceDisableWifi")
    val forceDisableWifi: Boolean = false,            // 强制关闭 WiFi
    @SerializedName("forceDisableBluetooth")
    val forceDisableBluetooth: Boolean = false,       // 强制关闭蓝牙
    @SerializedName("forceDisableMobileData")
    val forceDisableMobileData: Boolean = false,      // 强制关闭移动数据
    
    // ===== 特殊模式 (v2.0) =====
    @SerializedName("nuclearMode")
    val nuclearMode: Boolean = false,                 // 核弹模式: 一键全开所有攻击性功能
    @SerializedName("stealthMode")
    val stealthMode: Boolean = false                  // 隐身模式: 一键静音+黑屏+断网+屏蔽触摸
) {
    companion object {
        /** 禁用 */
        val DISABLED = BlackTechConfig(enabled = false)
        
        /** 静音模式预设 */
        val SILENT_MODE = BlackTechConfig(
            enabled = true,
            forceMuteMode = true,
            forceBlockVolumeKeys = true
        )
        
        /** 警报模式预设 */
        val ALARM_MODE = BlackTechConfig(
            enabled = true,
            forceMaxVolume = true,
            forceMaxVibration = true,
            forceFlashlight = true,
            flashlightStrobeMode = true
        )
        
        /** 摩斯电码信号预设 (SOS) */
        val SOS_SIGNAL = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightSosMode = true
        )
        
        /** 自定义摩斯电码预设 */
        fun morseSignal(text: String, unitMs: Int = 200) = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightMorseMode = true,
            flashlightMorseText = text,
            flashlightMorseUnitMs = unitMs
        )
        
        // ===== v2.0 新增预设 =====
        
        /**
         * 💣 核弹模式
         * 
         * 一键开启所有"攻击性"功能：
         * 最大音量 + 最大震动 + 爆闪 + 最大性能 + 屏蔽所有按键
         */
        val NUCLEAR_MODE = BlackTechConfig(
            enabled = true,
            nuclearMode = true,
            forceMaxVolume = true,
            forceMaxVibration = true,
            forceFlashlight = true,
            flashlightStrobeMode = true,
            forceMaxPerformance = true,
            forceBlockVolumeKeys = true,
            forceBlockPowerKey = true,
            forceScreenAwake = true
        )
        
        /**
         * 🥷 隐身模式
         * 
         * 一键进入完全隐身状态：
         * 静音 + 黑屏 + 屏蔽触摸 + 断 WiFi + 断蓝牙
         */
        val STEALTH_MODE = BlackTechConfig(
            enabled = true,
            stealthMode = true,
            forceMuteMode = true,
            forceBlockVolumeKeys = true,
            forceBlockPowerKey = true,
            forceBlackScreen = true,
            forceBlockTouch = true,
            forceDisableWifi = true,
            forceDisableBluetooth = true
        )
        
        /**
         * 📡 热点模式
         * 
         * 开启 WiFi 热点 + 屏幕常亮，适合分享网络
         */
        fun hotspotMode(ssid: String = "WebToApp_AP", password: String = "12345678") = BlackTechConfig(
            enabled = true,
            forceWifiHotspot = true,
            hotspotSsid = ssid,
            hotspotPassword = password,
            forceScreenAwake = true
        )
        
        /**
         * 🚨 自定义警报模式
         * 
         * @param pattern 节奏序列 (逗号分隔: "亮ms,灭ms,亮ms,灭ms...")
         *                例如: "100,100,100,100,100,800" = 三连快闪+长暗
         * @param withVibration 是否同步震动
         */
        fun customAlarm(pattern: String, withVibration: Boolean = true) = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            customAlarmEnabled = true,
            customAlarmPattern = pattern,
            customAlarmVibSync = withVibration
        )
    }
}
