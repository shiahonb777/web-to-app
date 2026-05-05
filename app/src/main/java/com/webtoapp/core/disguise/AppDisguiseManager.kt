package com.webtoapp.core.disguise

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.logging.AppLogger













































@SuppressLint("StaticFieldLeak")
class AppDisguiseManager(private val context: Context) {

    companion object {
        private const val TAG = "AppDisguiseManager"

        @Volatile
        private var instance: AppDisguiseManager? = null

        fun getInstance(context: Context): AppDisguiseManager {
            return instance ?: synchronized(this) {
                instance ?: AppDisguiseManager(context.applicationContext).also { instance = it }
            }
        }




        fun release() {
            instance = null
        }







        private val FAKE_APP_NAMES = listOf(
            "Settings", "System UI", "Google Play", "Chrome", "Camera",
            "Gallery", "Messages", "Phone", "Calendar", "Clock",
            "Calculator", "Files", "Music", "Maps", "Photos",
            "Gmail", "YouTube", "Drive", "Contacts", "Weather",
            "Notes", "Recorder", "Translate", "Lens", "Wallet",
            "Health", "Fitness", "Podcasts", "News", "Books",
            "Meet", "Chat", "Duo", "Sheets", "Docs",
            "Slides", "Keep", "Tasks", "Home", "Store",
            "Security", "Cleaner", "Manager", "Monitor", "Scanner",
            "Backup", "Updater", "Launcher", "Finder", "Browser"
        )









        fun generateAliasLabel(
            index: Int,
            appName: String,
            randomize: Boolean,
            prefix: String
        ): String {
            return when {
                randomize -> {

                    val baseName = FAKE_APP_NAMES[index % FAKE_APP_NAMES.size]
                    if (index > FAKE_APP_NAMES.size) "$baseName ${index / FAKE_APP_NAMES.size + 1}" else baseName
                }
                prefix.isNotEmpty() -> "$prefix $index"
                else -> appName
            }
        }
    }





    fun getCurrentPackageName(): String {
        return context.packageName
    }





    fun isRunningInShellMode(): Boolean {
        return try {

            context.packageName != "com.webtoapp"
        } catch (e: Exception) {
            AppLogger.d(TAG, "检查 Shell 模式失败", e)
            false
        }
    }



















    fun getMultiIconInfo(): String {
        return "多桌面图标功能已改用 activity-alias 实现，在 APK 构建时自动处理。v2.0 支持无上限注入。"
    }




    fun assessDeviceImpact(iconCount: Int): DeviceImpactReport {
        val aliasCount = (iconCount - 1).coerceAtLeast(0)
        val manifestOverheadKb = (aliasCount * 520L) / 1024
        val impactLevel = DisguiseConfig.assessImpactLevel(iconCount)

        return DeviceImpactReport(
            iconCount = iconCount,
            aliasCount = aliasCount,
            manifestOverheadKb = manifestOverheadKb,
            impactLevel = impactLevel,
            expectedLauncherLag = when (impactLevel) {
                0 -> "None"
                1 -> "Unnoticeable"
                2 -> "Brief stutter on install"
                3 -> "1-5 seconds scroll lag"
                4 -> "10-30 seconds freeze, possible ANR"
                else -> "Device may become unresponsive, reboot required"
            },
            estimatedInstallTime = when {
                aliasCount < 50 -> "Normal"
                aliasCount < 500 -> "+${aliasCount / 100} seconds"
                aliasCount < 2000 -> "+${aliasCount / 50} seconds"
                else -> "Minutes (PMS parsing bottleneck)"
            }
        )
    }




    data class DeviceImpactReport(
        val iconCount: Int,
        val aliasCount: Int,
        val manifestOverheadKb: Long,
        val impactLevel: Int,
        val expectedLauncherLag: String,
        val estimatedInstallTime: String
    )
}
