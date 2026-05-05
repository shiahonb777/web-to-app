package com.webtoapp.util

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.NetworkTrustConfig
import java.util.UUID

data class SavedNetworkTrustPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val config: NetworkTrustConfig,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SavedApkExportPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val config: ApkExportConfig,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object ConfigPresetStorage {
    private const val PREFS_NAME = "config_presets"
    private const val KEY_NETWORK_TRUST = "network_trust_presets"
    private const val KEY_APK_EXPORT = "apk_export_presets"

    private val networkPresetListType = object : TypeToken<List<SavedNetworkTrustPreset>>() {}.type
    private val exportPresetListType = object : TypeToken<List<SavedApkExportPreset>>() {}.type

    fun loadNetworkTrust(context: Context): List<SavedNetworkTrustPreset> {
        val json = prefs(context).getString(KEY_NETWORK_TRUST, null) ?: return emptyList()
        return runCatching {
            GsonProvider.gson.fromJson<List<SavedNetworkTrustPreset>>(json, networkPresetListType)
        }.getOrDefault(emptyList())
    }

    fun saveNetworkTrust(
        context: Context,
        name: String,
        config: NetworkTrustConfig
    ): List<SavedNetworkTrustPreset> {
        val next = upsert(loadNetworkTrust(context), name) { existing, trimmedName, now ->
            if (existing == null) {
                SavedNetworkTrustPreset(name = trimmedName, config = config, createdAt = now, updatedAt = now)
            } else {
                existing.copy(name = trimmedName, config = config, updatedAt = now)
            }
        }
        prefs(context).edit().putString(KEY_NETWORK_TRUST, GsonProvider.gson.toJson(next)).apply()
        return loadNetworkTrust(context)
    }

    fun deleteNetworkTrust(context: Context, presetId: String): List<SavedNetworkTrustPreset> {
        val next = loadNetworkTrust(context).filterNot { it.id == presetId }
        prefs(context).edit().putString(KEY_NETWORK_TRUST, GsonProvider.gson.toJson(next)).apply()
        return next
    }

    fun loadApkExport(context: Context): List<SavedApkExportPreset> {
        val json = prefs(context).getString(KEY_APK_EXPORT, null) ?: return emptyList()
        return runCatching {
            GsonProvider.gson.fromJson<List<SavedApkExportPreset>>(json, exportPresetListType)
        }.getOrDefault(emptyList())
    }

    fun saveApkExport(
        context: Context,
        name: String,
        config: ApkExportConfig
    ): List<SavedApkExportPreset> {
        val next = upsert(loadApkExport(context), name) { existing, trimmedName, now ->
            if (existing == null) {
                SavedApkExportPreset(name = trimmedName, config = config, createdAt = now, updatedAt = now)
            } else {
                existing.copy(name = trimmedName, config = config, updatedAt = now)
            }
        }
        prefs(context).edit().putString(KEY_APK_EXPORT, GsonProvider.gson.toJson(next)).apply()
        return loadApkExport(context)
    }

    fun deleteApkExport(context: Context, presetId: String): List<SavedApkExportPreset> {
        val next = loadApkExport(context).filterNot { it.id == presetId }
        prefs(context).edit().putString(KEY_APK_EXPORT, GsonProvider.gson.toJson(next)).apply()
        return next
    }

    private inline fun <T> upsert(
        current: List<T>,
        name: String,
        build: (existing: T?, trimmedName: String, now: Long) -> T
    ): List<T> where T : Any {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return current
        val now = System.currentTimeMillis()
        val existing = current.firstOrNull {
            when (it) {
                is SavedNetworkTrustPreset -> it.name.equals(trimmedName, ignoreCase = true)
                is SavedApkExportPreset -> it.name.equals(trimmedName, ignoreCase = true)
                else -> false
            }
        }
        val nextItem = build(existing, trimmedName, now)
        val next = if (existing == null) {
            current + nextItem
        } else {
            current.map { item -> if (item === existing) nextItem else item }
        }
        return next.sortedBy {
            when (it) {
                is SavedNetworkTrustPreset -> it.name.lowercase()
                is SavedApkExportPreset -> it.name.lowercase()
                else -> ""
            }
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
