package com.webtoapp.core.disguise

import android.content.Context
import android.util.Log

/**
 * 应用伪装管理器
 * 
 * 注意：多桌面图标功能现在通过 AndroidManifest 的 activity-alias 实现，
 * 在 APK 构建时由 AxmlRebuilder 动态添加，不再需要运行时处理。
 * 
 * 此类保留作为工具类，提供一些辅助功能。
 */
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
            Log.e(TAG, "检查 Shell 模式失败", e)
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
     */
    fun getMultiIconInfo(): String {
        return "多桌面图标功能已改用 activity-alias 实现，在 APK 构建时自动处理"
    }
}
