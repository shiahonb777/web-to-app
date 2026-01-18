package com.webtoapp.core.disguise

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * 应用伪装管理器
 * 
 * 实现以下功能：
 * 1. 伪装为系统应用 - 通过设备管理器权限防止卸载
 * 2. 多桌面图标 - 创建多个启动器快捷方式，删除任意一个则全部消失
 */
class AppDisguiseManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AppDisguiseManager"
        private const val SHORTCUT_PREFIX = "multi_icon_"
        
        @Volatile
        private var instance: AppDisguiseManager? = null
        
        fun getInstance(context: Context): AppDisguiseManager {
            return instance ?: synchronized(this) {
                instance ?: AppDisguiseManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * 创建多个桌面图标
     * 
     * @param count 图标数量 (1-20)
     * @param activity 主Activity，用于获取图标和标签
     */
    fun createMultiLauncherIcons(count: Int, activity: Activity) {
        if (count <= 1) {
            Log.d(TAG, "图标数量为1，跳过创建额外图标")
            return
        }
        
        val safeCount = count.coerceIn(1, 20)
        
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(context.packageName, 0)
            val appLabel = packageManager.getApplicationLabel(appInfo).toString()
            val appIcon = IconCompat.createWithResource(context, appInfo.icon)
            
            // 创建主Activity的Intent
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(context.packageName)
            }
            
            val mainActivityInfo = packageManager.queryIntentActivities(mainIntent, 0)
                .firstOrNull()?.activityInfo
            
            if (mainActivityInfo == null) {
                Log.e(TAG, "无法找到主Activity")
                return
            }
            
            // 清除旧的快捷方式
            removeAllMultiIcons()
            
            // 创建多个快捷方式
            val shortcuts = mutableListOf<ShortcutInfoCompat>()
            
            for (i in 1 until safeCount) {
                val shortcutId = "$SHORTCUT_PREFIX$i"
                
                val shortcutIntent = Intent(context, activity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", shortcutId)
                    putExtra("multi_icon_index", i)
                }
                
                val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
                    .setShortLabel(appLabel)
                    .setLongLabel(appLabel)
                    .setIcon(appIcon)
                    .setIntent(shortcutIntent)
                    .build()
                
                shortcuts.add(shortcut)
            }
            
            // 批量添加快捷方式到桌面
            shortcuts.forEach { shortcut ->
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                    Log.d(TAG, "创建快捷方式: ${shortcut.id}")
                }
            }
            
            Log.d(TAG, "成功创建 ${safeCount - 1} 个额外桌面图标")
            
        } catch (e: Exception) {
            Log.e(TAG, "创建多桌面图标失败", e)
        }
    }
    
    /**
     * 移除所有多图标快捷方式
     */
    fun removeAllMultiIcons() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                val pinnedShortcuts = shortcutManager?.pinnedShortcuts ?: emptyList()
                
                val multiIconIds = pinnedShortcuts
                    .filter { it.id.startsWith(SHORTCUT_PREFIX) }
                    .map { it.id }
                
                if (multiIconIds.isNotEmpty()) {
                    shortcutManager?.disableShortcuts(multiIconIds, "应用已移除")
                    Log.d(TAG, "已禁用 ${multiIconIds.size} 个多图标快捷方式")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "移除多图标快捷方式失败", e)
        }
    }
    
    /**
     * 检测是否有任何多图标被删除，如果是则触发自毁
     * 应在应用启动时调用
     */
    fun checkMultiIconIntegrity(expectedCount: Int, onIntegrityBroken: () -> Unit) {
        if (expectedCount <= 1) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                val pinnedShortcuts = shortcutManager?.pinnedShortcuts ?: emptyList()
                
                val multiIconCount = pinnedShortcuts.count { it.id.startsWith(SHORTCUT_PREFIX) }
                val expectedMultiIcons = expectedCount - 1 // 减去主图标
                
                // 如果多图标数量少于预期，说明有图标被删除
                if (multiIconCount < expectedMultiIcons) {
                    Log.w(TAG, "检测到多图标被删除: 期望 $expectedMultiIcons, 实际 $multiIconCount")
                    onIntegrityBroken()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查多图标完整性失败", e)
        }
    }
    
    /**
     * 启用设备管理器权限（伪装系统应用）
     * 通过设备管理器权限可以防止应用被卸载
     */
    fun enableDeviceAdmin(activity: Activity) {
        try {
            val componentName = ComponentName(
                context.packageName,
                "${context.packageName}.receiver.DeviceAdminReceiver"
            )
            
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "启用设备管理器以保护应用不被卸载"
                )
            }
            
            activity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
            Log.d(TAG, "请求启用设备管理器")
            
        } catch (e: Exception) {
            Log.e(TAG, "启用设备管理器失败", e)
        }
    }
    
    /**
     * 检查设备管理器是否已启用
     */
    fun isDeviceAdminEnabled(): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) 
                as android.app.admin.DevicePolicyManager
            val componentName = ComponentName(
                context.packageName,
                "${context.packageName}.receiver.DeviceAdminReceiver"
            )
            devicePolicyManager.isAdminActive(componentName)
        } catch (e: Exception) {
            Log.e(TAG, "检查设备管理器状态失败", e)
            false
        }
    }
    
    /**
     * 禁用设备管理器
     */
    fun disableDeviceAdmin() {
        try {
            if (isDeviceAdminEnabled()) {
                val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) 
                    as android.app.admin.DevicePolicyManager
                val componentName = ComponentName(
                    context.packageName,
                    "${context.packageName}.receiver.DeviceAdminReceiver"
                )
                devicePolicyManager.removeActiveAdmin(componentName)
                Log.d(TAG, "设备管理器已禁用")
            }
        } catch (e: Exception) {
            Log.e(TAG, "禁用设备管理器失败", e)
        }
    }
    
    /**
     * 隐藏应用图标（从启动器中隐藏）
     * 配合多图标使用可以实现只显示快捷方式图标
     */
    fun hideAppIcon() {
        try {
            val packageManager = context.packageManager
            val componentName = ComponentName(context, "${context.packageName}.MainActivity")
            
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            
            Log.d(TAG, "应用图标已隐藏")
        } catch (e: Exception) {
            Log.e(TAG, "隐藏应用图标失败", e)
        }
    }
    
    /**
     * 显示应用图标
     */
    fun showAppIcon() {
        try {
            val packageManager = context.packageManager
            val componentName = ComponentName(context, "${context.packageName}.MainActivity")
            
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            
            Log.d(TAG, "应用图标已显示")
        } catch (e: Exception) {
            Log.e(TAG, "显示应用图标失败", e)
        }
    }
    
    companion object RequestCodes {
        const val REQUEST_CODE_ENABLE_ADMIN = 9001
    }
}
