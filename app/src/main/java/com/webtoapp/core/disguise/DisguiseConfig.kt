package com.webtoapp.core.disguise

import com.google.gson.annotations.SerializedName

/**
 * 应用伪装功能配置
 * 
 * 独立的功能模块，用于配置应用伪装行为
 * 
 * 核心机制：使用 AndroidManifest 的 activity-alias 创建多个桌面图标
 * 这是 Android 原生支持的机制，安装后自动显示多个图标，无需任何运行时权限
 */
data class DisguiseConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,                     // Yes否启用伪装功能
    
    // 多桌面图标（使用 activity-alias 实现，构建时生效）
    @SerializedName("multiLauncherIcons")
    val multiLauncherIcons: Int = 1                   // 桌面图标数量（1=仅主图标，>1=添加额外别名图标）
) {
    companion object {
        /** 禁用 */
        val DISABLED = DisguiseConfig(enabled = false)
        
        /** 多图标预设：3个桌面图标 */
        val MULTI_ICON_3 = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 3
        )
        
        /** 多图标预设：5个桌面图标 */
        val MULTI_ICON_5 = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 5
        )
    }
    
    /**
     * 计算需要添加的 activity-alias 数量
     * 主图标算一个，需要 multiLauncherIcons-1 个 alias
     */
    fun getAliasCount(): Int {
        if (!enabled || multiLauncherIcons <= 1) return 0
        return multiLauncherIcons - 1
    }
}
