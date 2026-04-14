package com.webtoapp.core.cloud

import android.content.Context
import android.content.SharedPreferences

/**
 * Note: brief English comment.
 *
 * Note: brief English comment.
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

    /** Note: brief English comment. */
    fun markInstalled(itemId: Int) {
        val ids = getInstalledIds().toMutableSet()
        ids.add(itemId.toString())
        prefs.edit().putStringSet(KEY_INSTALLED_IDS, ids).apply()
    }

    /** Note: brief English comment. */
    fun markUninstalled(itemId: Int) {
        val ids = getInstalledIds().toMutableSet()
        ids.remove(itemId.toString())
        prefs.edit().putStringSet(KEY_INSTALLED_IDS, ids).apply()
    }

    /** Note: brief English comment. */
    fun isInstalled(itemId: Int): Boolean =
        getInstalledIds().contains(itemId.toString())

    /** Note: brief English comment. */
    fun getInstalledIds(): Set<String> =
        prefs.getStringSet(KEY_INSTALLED_IDS, emptySet()) ?: emptySet()
}
