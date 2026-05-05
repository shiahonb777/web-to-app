package com.webtoapp.util

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.ApkRuntimePermissions
import java.util.UUID

data class SavedPermissionPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val permissions: ApkRuntimePermissions,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object PermissionPresetStorage {
    private const val PREFS_NAME = "permission_presets"
    private const val KEY_PRESETS = "presets"

    private val presetListType = object : TypeToken<List<SavedPermissionPreset>>() {}.type

    fun load(context: Context): List<SavedPermissionPreset> {
        val json = prefs(context).getString(KEY_PRESETS, null) ?: return emptyList()
        return runCatching {
            GsonProvider.gson.fromJson<List<SavedPermissionPreset>>(json, presetListType)
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, name: String, permissions: ApkRuntimePermissions): List<SavedPermissionPreset> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return load(context)

        val now = System.currentTimeMillis()
        val current = load(context)
        val existing = current.firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
        val next = if (existing == null) {
            current + SavedPermissionPreset(
                name = trimmedName,
                permissions = permissions,
                createdAt = now,
                updatedAt = now
            )
        } else {
            current.map {
                if (it.id == existing.id) {
                    it.copy(name = trimmedName, permissions = permissions, updatedAt = now)
                } else {
                    it
                }
            }
        }
        persist(context, next.sortedBy { it.name.lowercase() })
        return load(context)
    }

    fun delete(context: Context, presetId: String): List<SavedPermissionPreset> {
        val next = load(context).filterNot { it.id == presetId }
        persist(context, next)
        return next
    }

    private fun persist(context: Context, presets: List<SavedPermissionPreset>) {
        prefs(context).edit()
            .putString(KEY_PRESETS, GsonProvider.gson.toJson(presets))
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
