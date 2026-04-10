package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * 跨 WebView Storage 同步中枢
 *
 * 提供线程安全的 key-value 存储，同时供 Background WebView 和 Content WebView 使用。
 * 解决 chrome.storage 在两个 WebView 中各自独立 localStorage 的隔离问题。
 *
 * 数据存储在内存中（ConcurrentHashMap），页面生命周期内持久。
 * 如果需要跨进程/跨 Activity 持久化，可扩展为 SharedPreferences 或 SQLite 后端。
 */
object ExtensionStorageSync {

    private const val TAG = "ExtStorageSync"

    // extId:key -> value (JSON string)
    private val store = ConcurrentHashMap<String, String>()

    /**
     * 获取值
     * @param extId 扩展 ID（用于命名空间隔离）
     * @param key 存储键名
     * @return JSON 值字符串，不存在则返回空字符串
     */
    fun get(extId: String, key: String): String {
        return store["$extId:$key"] ?: ""
    }

    /**
     * 设置值
     * @param extId 扩展 ID
     * @param key 存储键名
     * @param value JSON 值字符串
     */
    fun set(extId: String, key: String, value: String) {
        store["$extId:$key"] = value
    }

    /**
     * 移除值
     */
    fun remove(extId: String, key: String) {
        store.remove("$extId:$key")
    }

    /**
     * 获取扩展的所有存储数据
     * @return JSON 对象字符串 {"key1":"val1","key2":"val2"}
     */
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

    /**
     * 清除扩展的所有存储数据
     */
    fun clear(extId: String) {
        val prefix = "$extId:"
        val keysToRemove = store.keys().toList().filter { it.startsWith(prefix) }
        keysToRemove.forEach { store.remove(it) }
        AppLogger.d(TAG, "Cleared ${keysToRemove.size} keys for $extId")
    }

    /**
     * 清除所有扩展的所有存储数据
     */
    fun clearAll() {
        store.clear()
    }
}
