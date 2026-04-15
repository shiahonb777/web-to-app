package com.webtoapp.data.model

import com.google.gson.JsonParser
import com.webtoapp.data.converter.Converters

/**
 * Serialize a WebApp to the manifest JSON string.
 * Used for cloud sync, backup, and cross-device migration.
 */
fun WebApp.toManifestJson(): String = Converters.gson.toJson(this)

/**
 * Manifest utilities.
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
