package com.webtoapp.core.cloud

import android.content.Context
import android.content.SharedPreferences

/**
 * 追踪已安装的商店模块/应用，持久化到 SharedPreferences。
 *
 * 使用方式:
 *   tracker.markInstalled(moduleId)
 *   tracker.isInstalled(moduleId)  // true
 *   tracker.getInstalledIds()      // setOf(moduleId)
 */
class InstalledItemsTracker(context: Context) {

    companion object {
        private const val PREFS_NAME = "installed_store_items"
        private const val KEY_INSTALLED_IDS = "installed_ids"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 标记某个 module/app 已安装 */
    fun markInstalled(itemId: Int) {
        val ids = getInstalledIds().toMutableSet()
        ids.add(itemId.toString())
        prefs.edit().putStringSet(KEY_INSTALLED_IDS, ids).apply()
    }

    /** 取消安装标记 (卸载时) */
    fun markUninstalled(itemId: Int) {
        val ids = getInstalledIds().toMutableSet()
        ids.remove(itemId.toString())
        prefs.edit().putStringSet(KEY_INSTALLED_IDS, ids).apply()
    }

    /** 检查是否已安装 */
    fun isInstalled(itemId: Int): Boolean =
        getInstalledIds().contains(itemId.toString())

    /** 获取所有已安装的 ID 集合 */
    fun getInstalledIds(): Set<String> =
        prefs.getStringSet(KEY_INSTALLED_IDS, emptySet()) ?: emptySet()
}
