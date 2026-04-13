package com.webtoapp.data.model

import com.google.gson.JsonParser
import com.webtoapp.data.converter.Converters

/**
 * 将 WebApp 序列化为 Manifest JSON 字符串。
 * 用于云端同步、备份、跨设备迁移。
 */
fun WebApp.toManifestJson(): String = Converters.gson.toJson(this)

/**
 * Manifest 工具类。
 */
object ManifestUtils {
    fun fromManifestJson(json: String, overrideId: Long? = null): WebApp? {
        return try {
            val parsed = JsonParser.parseString(json)
            val defaultWebApp = WebApp(name = "", url = "")
            val defaultJson = Converters.gson.toJsonTree(defaultWebApp)
            val merged = Converters.mergeMissingDefaults(defaultJson, parsed)
            val restored = Converters.gson.fromJson(merged, WebApp::class.java)
            if (overrideId != null) {
                restored?.copy(id = overrideId, updatedAt = System.currentTimeMillis())
            } else {
                restored
            }
        } catch (_: Exception) {
            null
        }
    }
}
