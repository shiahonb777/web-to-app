package com.webtoapp.core.disguise

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * 设备管理器接收器
 * 
 * 用于实现"伪装系统应用"功能
 * 当应用被设置为设备管理器时，无法通过正常方式卸载
 */
class AppDeviceAdminReceiver : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "设备管理器已启用 - 应用现在受保护，无法正常卸载")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "设备管理器已禁用 - 应用可以正常卸载")
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "用户请求禁用设备管理器")
        return "禁用后应用将可以被卸载，确定要继续吗？"
    }
    
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.d(TAG, "密码验证失败")
    }
    
    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Log.d(TAG, "密码验证成功")
    }
}
