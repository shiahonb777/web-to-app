package com.webtoapp.core.appmodifier

import android.graphics.drawable.Drawable




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




data class AppModifyConfig(
    val originalApp: InstalledAppInfo,
    val newAppName: String,
    val newIconPath: String? = null,


    val splashEnabled: Boolean = false,
    val splashType: String = "IMAGE",
    val splashPath: String? = null,
    val splashDuration: Int = 3,
    val splashClickToSkip: Boolean = true,
    val splashVideoStartMs: Long = 0,
    val splashVideoEndMs: Long = 5000,
    val splashLandscape: Boolean = false,
    val splashFillScreen: Boolean = true,
    val splashEnableAudio: Boolean = false,


    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val activationRequireEveryTime: Boolean = false,


    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementContent: String = "",
    val announcementLink: String? = null,
    val announcementTemplate: String = "XIAOHONGSHU",
    val announcementShowEmoji: Boolean = true,
    val announcementAnimationEnabled: Boolean = true,


    val bgmEnabled: Boolean = false,
    val bgmConfig: com.webtoapp.data.model.BgmConfig? = null
)




sealed class AppModifyResult {



    data object ShortcutSuccess : AppModifyResult()




    data class CloneSuccess(val apkPath: String) : AppModifyResult()




    data class Error(val message: String) : AppModifyResult()
}




enum class AppFilterType {
    ALL,
    USER,
    SYSTEM
}
