package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap










object ExtensionStorageSync {

    private const val TAG = "ExtStorageSync"


    private val store = ConcurrentHashMap<String, String>()







    fun get(extId: String, key: String): String {
        return store["$extId:$key"] ?: ""
    }







    fun set(extId: String, key: String, value: String) {
        store["$extId:$key"] = value
    }




    fun remove(extId: String, key: String) {
        store.remove("$extId:$key")
    }





    fun getAll(extId: String): String {
        val prefix = "$extId:"
        val result = JSONObject()
        try {
            for ((k, v) in store) {
                if (k.startsWith(prefix)) {
                    val realKey = k.removePrefix(prefix)
                    result.put(realKey, v)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getAll error for $extId", e)
        }
        return result.toString()
    }




    fun clear(extId: String) {
        val prefix = "$extId:"
        val keysToRemove = store.keys().toList().filter { it.startsWith(prefix) }
        keysToRemove.forEach { store.remove(it) }
        AppLogger.d(TAG, "Cleared ${keysToRemove.size} keys for $extId")
    }




    fun clearAll() {
        store.clear()
    }
}
