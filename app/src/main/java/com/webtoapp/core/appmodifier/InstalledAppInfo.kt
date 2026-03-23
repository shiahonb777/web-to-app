package com.webtoapp.core.appmodifier

import android.graphics.drawable.Drawable

/**
 * 已安装应用信息
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val apkPath: String,
    val isSystemApp: Boolean,
    val installedTime: Long,
    val updatedTime: Long,
    val apkSize: Long
) {
    /**
     * 格式化的 APK 大小
     */
    val formattedSize: String
        get() {
            val kb = apkSize / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1 -> String.format("%.1f MB", mb)
                kb >= 1 -> String.format("%.1f KB", kb)
                else -> "$apkSize B"
            }
        }
}

/**
 * 应用修改配置
 */
data class AppModifyConfig(
    val originalApp: InstalledAppInfo,
    val newAppName: String,
    val newIconPath: String? = null,
    
    // Start画面配置
    val splashEnabled: Boolean = false,
    val splashType: String = "IMAGE",      // "IMAGE" or "VIDEO"
    val splashPath: String? = null,        // Media文件路径
    val splashDuration: Int = 3,           // Image显示时长（秒）
    val splashClickToSkip: Boolean = true, // Yes否允许点击跳过
    val splashVideoStartMs: Long = 0,      // Video裁剪起始（毫秒）
    val splashVideoEndMs: Long = 5000,     // Video裁剪结束（毫秒）
    val splashLandscape: Boolean = false,  // Yes否横屏显示
    val splashFillScreen: Boolean = true,  // Yes否铺满屏幕
    val splashEnableAudio: Boolean = false, // Yes否启用视频音频
    
    // Activation码配置
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val activationRequireEveryTime: Boolean = false,
    
    // 弹窗公告配置
    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementContent: String = "",
    val announcementLink: String? = null,
    
    // Background music配置
    val bgmEnabled: Boolean = false,
    val bgmConfig: com.webtoapp.data.model.BgmConfig? = null
)

/**
 * 应用修改结果
 */
sealed class AppModifyResult {
    /**
     * 快捷方式创建成功
     */
    data object ShortcutSuccess : AppModifyResult()
    
    /**
     * 克隆安装成功
     */
    data class CloneSuccess(val apkPath: String) : AppModifyResult()
    
    /**
     * 操作失败
     */
    data class Error(val message: String) : AppModifyResult()
}

/**
 * 应用筛选类型
 */
enum class AppFilterType {
    ALL,        // 所有应用
    USER,       // User应用
    SYSTEM      // System应用
}
