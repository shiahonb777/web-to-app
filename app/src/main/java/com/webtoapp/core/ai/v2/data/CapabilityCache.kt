package com.webtoapp.core.ai.v2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.reflect.TypeToken
import com.webtoapp.util.GsonProvider
import kotlinx.coroutines.flow.first

private val Context.aiCapStore: DataStore<Preferences> by preferencesDataStore(name = "ai_capabilities_v2")

class CapabilityCache(private val context: Context) {
    enum class ToolSupport { UNKNOWN, SUPPORTED, UNSUPPORTED }
    private val gson = GsonProvider.gson
    private data class Entry(val support: String, val ts: Long)

    suspend fun getToolSupport(provider: String, modelId: String): ToolSupport {
        val map = readMap(); val e = map["$provider::$modelId"] ?: return ToolSupport.UNKNOWN
        if(e.support == "UNSUPPORTED" && System.currentTimeMillis() - e.ts > 7*24*3600_000L) return ToolSupport.UNKNOWN
        return runCatching { ToolSupport.valueOf(e.support) }.getOrElse { ToolSupport.UNKNOWN }
    }

    suspend fun recordToolSupport(provider: String, modelId: String, support: ToolSupport) {
        context.aiCapStore.edit { prefs ->
            val map = readMapFromPrefs(prefs).toMutableMap()
            map["$provider::$modelId"] = Entry(support.name, System.currentTimeMillis())
            prefs[KEY] = gson.toJson(map)
        }
    }

    private suspend fun readMap(): Map<String, Entry> = readMapFromPrefs(context.aiCapStore.data.first())
    private fun readMapFromPrefs(prefs: Preferences): Map<String, Entry> {
        val raw = prefs[KEY] ?: return emptyMap()
        return runCatching { gson.fromJson<Map<String,Entry>>(raw, object:TypeToken<Map<String,Entry>>(){}.type) }.getOrElse { emptyMap() }
    }
    companion object { private val KEY = stringPreferencesKey("tool_support_v1") }
}
