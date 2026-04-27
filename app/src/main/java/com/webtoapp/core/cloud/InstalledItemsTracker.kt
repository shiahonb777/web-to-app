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
        private const val KEY_VERSION_CODES = "version_codes"
        private const val KEY_VERSION_NAMES = "version_names"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 标记某个 module/app 已安装（带版本信息） */
    fun markInstalled(itemId: Int, versionCode: Int = 0, versionName: String = "") {
        val ids = getInstalledIds().toMutableSet()
        ids.add(itemId.toString())
        prefs.edit()
            .putStringSet(KEY_INSTALLED_IDS, ids)
            .apply()
        saveVersionInfo(itemId, versionCode, versionName)
    }

    private fun saveVersionInfo(itemId: Int, versionCode: Int, versionName: String) {
        val codes = getVersionCodes().toMutableMap()
        codes[itemId.toString()] = versionCode
        val names = getVersionNames().toMutableMap()
        names[itemId.toString()] = versionName
        prefs.edit()
            .putStringSet(KEY_VERSION_CODES, codes.map { "${it.key}=${it.value}" }.toSet())
            .putStringSet(KEY_VERSION_NAMES, names.map { "${it.key}=${it.value}" }.toSet())
            .apply()
    }

    /** 获取已安装模块的 versionCode */
    fun getInstalledVersionCode(itemId: Int): Int =
        getVersionCodes()[itemId.toString()] ?: 0

    /** 获取已安装模块的 versionName */
    fun getInstalledVersionName(itemId: Int): String =
        getVersionNames()[itemId.toString()] ?: ""

    private fun getVersionCodes(): Map<String, Int> =
        (prefs.getStringSet(KEY_VERSION_CODES, null) ?: emptySet())
            .associate { val p = it.split("="); p[0] to (p.getOrNull(1)?.toIntOrNull() ?: 0) }

    private fun getVersionNames(): Map<String, String> =
        (prefs.getStringSet(KEY_VERSION_NAMES, null) ?: emptySet())
            .associate { val p = it.split("=", limit = 2); p[0] to (p.getOrNull(1) ?: "") }

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
