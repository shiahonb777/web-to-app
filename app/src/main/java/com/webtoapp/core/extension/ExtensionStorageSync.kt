package com.webtoapp.core.extension

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object ExtensionStorageSync {

    private const val TAG = "ExtStorageSync"
    private const val PREFS_NAME = "chrome_extension_storage"
    private const val KEY_PREFIX = "ext:"

    enum class Area(val wireName: String, val persisted: Boolean) {
        LOCAL("local", true),
        SYNC("sync", true),
        SESSION("session", false);

        companion object {
            fun fromWireName(name: String?): Area {
                return entries.firstOrNull { it.wireName.equals(name?.trim(), ignoreCase = true) } ?: LOCAL
            }
        }
    }

    @Volatile
    private var appContext: Context? = null

    private val sessionStore = ConcurrentHashMap<String, String>()

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun get(extId: String, key: String, area: Area = Area.LOCAL): String {
        return when (area) {
            Area.SESSION -> sessionStore[compositeKey(extId, area, key)] ?: ""
            else -> readPersistentValue(extId, area, key) ?: ""
        }
    }

    fun set(extId: String, key: String, value: String, area: Area = Area.LOCAL) {
        when (area) {
            Area.SESSION -> sessionStore[compositeKey(extId, area, key)] = value
            else -> persistentPrefs()?.edit()?.putString(compositeKey(extId, area, key), value)?.apply()
        }
    }

    fun remove(extId: String, key: String, area: Area = Area.LOCAL) {
        when (area) {
            Area.SESSION -> sessionStore.remove(compositeKey(extId, area, key))
            else -> persistentPrefs()?.edit()?.remove(compositeKey(extId, area, key))?.apply()
        }
    }

    fun getAll(extId: String, area: Area = Area.LOCAL): String {
        val result = JSONObject()
        try {
            when (area) {
                Area.SESSION -> {
                    val prefix = prefixFor(extId, area)
                    for ((k, v) in sessionStore) {
                        if (k.startsWith(prefix)) {
                            result.put(k.removePrefix(prefix), v)
                        }
                    }
                }
                else -> {
                    val prefix = prefixFor(extId, area)
                    val entries = persistentPrefs()?.all.orEmpty()
                    for ((k, v) in entries) {
                        if (k.startsWith(prefix) && v is String) {
                            result.put(k.removePrefix(prefix), v)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getAll error for $extId area=${area.wireName}", e)
        }
        return result.toString()
    }

    fun clear(extId: String, area: Area = Area.LOCAL) {
        when (area) {
            Area.SESSION -> {
                val prefix = prefixFor(extId, area)
                val keysToRemove = sessionStore.keys().toList().filter { it.startsWith(prefix) }
                keysToRemove.forEach(sessionStore::remove)
                AppLogger.d(TAG, "Cleared ${keysToRemove.size} session keys for $extId")
            }
            else -> {
                val prefs = persistentPrefs() ?: return
                val prefix = prefixFor(extId, area)
                val keysToRemove = prefs.all.keys.filter { it.startsWith(prefix) }
                val editor = prefs.edit()
                keysToRemove.forEach(editor::remove)
                editor.apply()
                AppLogger.d(TAG, "Cleared ${keysToRemove.size} ${area.wireName} keys for $extId")
            }
        }
    }

    fun clearAll() {
        sessionStore.clear()
        persistentPrefs()?.edit()?.clear()?.apply()
    }

    private fun readPersistentValue(extId: String, area: Area, key: String): String? {
        return persistentPrefs()?.getString(compositeKey(extId, area, key), null)
    }

    private fun persistentPrefs() =
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun compositeKey(extId: String, area: Area, key: String): String {
        return "${prefixFor(extId, area)}$key"
    }

    private fun prefixFor(extId: String, area: Area): String {
        return "$KEY_PREFIX${area.wireName}:$extId:"
    }
}
