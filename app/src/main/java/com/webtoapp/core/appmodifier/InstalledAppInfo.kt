package com.webtoapp.core.appmodifier

import android.graphics.drawable.Drawable

/**
 * Note: brief English comment.
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
     * Note: brief English comment.
     */
    val formattedSize: String
        get() {
            val kb = apkSize / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1 -> String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
                kb >= 1 -> String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
                else -> "$apkSize B"
            }
        }
}

/**
 * Note: brief English comment.
 */
data class AppModifyConfig(
    val originalApp: InstalledAppInfo,
    val newAppName: String,
    val newIconPath: String? = null,
    
    // Note: brief English comment.
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
    
    // Note: brief English comment.
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val activationRequireEveryTime: Boolean = false,
    
    // Note: brief English comment.
    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementContent: String = "",
    val announcementLink: String? = null,
    val announcementTemplate: String = "XIAOHONGSHU",
    val announcementShowEmoji: Boolean = true,
    val announcementAnimationEnabled: Boolean = true,
    
    // Note: brief English comment.
    val bgmEnabled: Boolean = false,
    val bgmConfig: com.webtoapp.data.model.BgmConfig? = null
)

/**
 * Note: brief English comment.
 */
sealed class AppModifyResult {
    /**
     * Note: brief English comment.
     */
    data object ShortcutSuccess : AppModifyResult()
    
    /**
     * Note: brief English comment.
     */
    data class CloneSuccess(val apkPath: String) : AppModifyResult()
    
    /**
     * Note: brief English comment.
     */
    data class Error(val message: String) : AppModifyResult()
}

/**
 * Note: brief English comment.
 */
enum class AppFilterType {
    ALL,        // 所有应用
    USER,       // User应用
    SYSTEM      // System应用
}
