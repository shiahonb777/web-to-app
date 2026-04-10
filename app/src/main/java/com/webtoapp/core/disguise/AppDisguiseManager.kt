package com.webtoapp.core.disguise

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.logging.AppLogger

/**
 * 应用伪装管理器 v2.0
 * 
 * ## Icon Storm 引擎
 * 
 * 核心机制：利用 Android 原生 activity-alias 的 MAIN/LAUNCHER intent-filter
 * 注入大量桌面图标。每个 alias 在 Launcher 眼中都是独立的可启动应用入口。
 * 
 * ### 技术架构
 * ```
 * DisguiseConfig.multiLauncherIcons = N
 *     ↓ (APK Build Time)
 * AxmlRebuilder.addActivityAliases(N-1)
 *     ↓ (For each i in 1..N-1)
 *     <activity-alias
 *         android:name=".LauncherAlias{i}"
 *         android:targetActivity="com.webtoapp.ui.shell.ShellActivity"
 *         android:exported="true">
 *         <intent-filter>
 *             <action android:name="android.intent.action.MAIN" />
 *             <category android:name="android.intent.category.LAUNCHER" />
 *         </intent-filter>
 *     </activity-alias>
 *     ↓ (Install Time)
 * PackageManager 解析所有 LAUNCHER intent-filter
 *     ↓
 * Launcher 为每个 entry 创建桌面图标
 * ```
 * 
 * ### Launcher 影响机制
 * 
 * Android Launcher (如 Pixel Launcher, One UI Home, MIUI Home) 在以下时刻解析应用图标：
 * 1. **安装时**: PackageManagerService 扫描 manifest，发现所有 LAUNCHER category
 * 2. **启动时**: LauncherApps.getActivityList() 返回所有可启动的 ActivityInfo
 * 3. **渲染时**: 为每个 ActivityInfo 创建 ShortcutInfo 并分配桌面位置
 * 
 * 当图标数量极大 (500+) 时：
 * - PMS 解析 manifest 耗时线性增长
 * - Launcher 的 RecyclerView/GridView 需要为每个图标创建 ViewHolder
 * - 图标 Bitmap 缓存消耗大量内存
 * - 部分 Launcher 使用 SQLite 存储布局，大量 INSERT 导致 I/O 瓶颈
 * 
 * 这是 Android 框架的架构设计特性，非安全漏洞。
 */
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
        
        /**
         * 释放单例实例
         */
        fun release() {
            instance = null
        }
        
        /**
         * 随机图标名称生成器
         * 用于 Icon Storm 的 randomizeNames 模式
         * 
         * 生成看起来像合法应用名称的随机字符串
         */
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
        
        /**
         * 为第 i 个 alias 生成标签名称
         * 
         * @param index alias 序号 (1-based)
         * @param appName 原始应用名
         * @param randomize 是否随机化
         * @param prefix 自定义前缀 (空则使用 appName)
         */
        fun generateAliasLabel(
            index: Int,
            appName: String,
            randomize: Boolean,
            prefix: String
        ): String {
            return when {
                randomize -> {
                    // 从假名列表中循环选择，加上随机后缀避免完全重复
                    val baseName = FAKE_APP_NAMES[index % FAKE_APP_NAMES.size]
                    if (index > FAKE_APP_NAMES.size) "$baseName ${index / FAKE_APP_NAMES.size + 1}" else baseName
                }
                prefix.isNotEmpty() -> "$prefix $index"
                else -> appName
            }
        }
    }
    
    /**
     * 获取当前应用的包名
     * 可用于检查是否在导出的 APK 中运行
     */
    fun getCurrentPackageName(): String {
        return context.packageName
    }
    
    /**
     * 检查当前是否在 Shell 模式中运行
     * Shell 模式表示运行在导出的 APK 中
     */
    fun isRunningInShellMode(): Boolean {
        return try {
            // Export的 APK 包名不是 com.webtoapp
            context.packageName != "com.webtoapp"
        } catch (e: Exception) {
            AppLogger.d(TAG, "检查 Shell 模式失败", e)
            false
        }
    }
    
    /**
     * 多桌面图标说明：
     * 
     * 多桌面图标功能现在通过 AndroidManifest 的 activity-alias 实现，
     * 安装后自动显示多个图标，无需任何运行时权限。
     * 
     * 配置方式：
     * - 在 DisguiseConfig 中设置 multiLauncherIcons 数量
     * - 构建 APK 时，AxmlRebuilder 会自动添加对应数量的 activity-alias
     * - 每个 alias 都有 MAIN/LAUNCHER intent-filter，因此会显示为独立的桌面图标
     * 
     * v2.0 新增：
     * - 无上限图标注入 (Icon Storm)
     * - 分级模式选择 (Normal → Research)
     * - 随机化图标名称
     * - 自定义名称前缀
     * - 实时影响评估
     */
    fun getMultiIconInfo(): String {
        return "多桌面图标功能已改用 activity-alias 实现，在 APK 构建时自动处理。v2.0 支持无上限注入。"
    }
    
    /**
     * 评估给定图标数量对设备的预期影响
     */
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
    
    /**
     * 设备影响报告
     */
    data class DeviceImpactReport(
        val iconCount: Int,
        val aliasCount: Int,
        val manifestOverheadKb: Long,
        val impactLevel: Int,
        val expectedLauncherLag: String,
        val estimatedInstallTime: String
    )
}
