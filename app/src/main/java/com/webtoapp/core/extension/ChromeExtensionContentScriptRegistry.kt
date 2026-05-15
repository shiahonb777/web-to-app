package com.webtoapp.core.extension

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.GsonProvider
import java.util.concurrent.ConcurrentHashMap

object ChromeExtensionContentScriptRegistry {

    private const val TAG = "ChromeExtScriptRegistry"
    private const val PREFS_NAME = "chrome_extension_content_scripts"
    private const val EXTENSIONS_DIR = "extensions"

    private val gson get() = GsonProvider.gson
    private val registeredScripts = ConcurrentHashMap<String, MutableList<RegisteredContentScript>>()
    private val scriptListType = object : TypeToken<List<RegisteredContentScript>>() {}.type

    @Volatile
    private var appContext: Context? = null

    data class RegisteredContentScript(
        @SerializedName("id")
        val id: String,
        @SerializedName("matches")
        val matches: List<String> = emptyList(),
        @SerializedName("excludeMatches")
        val excludeMatches: List<String> = emptyList(),
        @SerializedName("js")
        val js: List<String> = emptyList(),
        @SerializedName("css")
        val css: List<String> = emptyList(),
        @SerializedName("runAt")
        val runAt: String = "document_idle",
        @SerializedName("allFrames")
        val allFrames: Boolean = false,
        @SerializedName("matchOriginAsFallback")
        val matchOriginAsFallback: Boolean = false,
        @SerializedName("persistAcrossSessions")
        val persistAcrossSessions: Boolean = true,
        @SerializedName("world")
        val world: String = "ISOLATED"
    )

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    @Synchronized
    fun registerContentScripts(extensionId: String, scriptsJson: String) {
        val normalizedExtensionId = extensionId.trim()
        if (normalizedExtensionId.isEmpty()) return
        val parsedScripts = parseScripts(scriptsJson)
        if (parsedScripts.isEmpty()) return

        val existing = getScriptsMutable(normalizedExtensionId)
        val incomingIds = parsedScripts.map { it.id }.toSet()
        existing.removeAll { it.id in incomingIds }
        existing.addAll(parsedScripts)
        persist(normalizedExtensionId)
        AppLogger.d(
            TAG,
            "Registered ${parsedScripts.size} content script(s) for $normalizedExtensionId"
        )
    }

    @Synchronized
    fun unregisterContentScripts(extensionId: String, filterJson: String = "{}") {
        val normalizedExtensionId = extensionId.trim()
        if (normalizedExtensionId.isEmpty()) return
        val existing = getScriptsMutable(normalizedExtensionId)
        if (existing.isEmpty()) return

        val ids = parseFilterIds(filterJson)
        if (ids == null) {
            existing.clear()
        } else if (ids.isNotEmpty()) {
            existing.removeAll { it.id in ids }
        }
        persist(normalizedExtensionId)
        AppLogger.d(TAG, "Unregistered content script(s) for $normalizedExtensionId filter=$filterJson")
    }

    @Synchronized
    fun getRegisteredContentScriptsJson(extensionId: String, filterJson: String = "{}"): String {
        val normalizedExtensionId = extensionId.trim()
        if (normalizedExtensionId.isEmpty()) return "[]"
        val scripts = getScriptsMutable(normalizedExtensionId)
        val ids = parseFilterIds(filterJson)
        val filtered = when {
            ids == null -> scripts
            ids.isEmpty() -> emptyList()
            else -> scripts.filter { it.id in ids }
        }
        return gson.toJson(filtered)
    }

    fun buildModules(context: Context, extensionId: String): List<ExtensionModule> {
        initialize(context)
        val normalizedExtensionId = extensionId.trim()
        if (normalizedExtensionId.isEmpty()) return emptyList()
        return getScriptsMutable(normalizedExtensionId).mapNotNull { script ->
            script.toExtensionModule(context, normalizedExtensionId)
        }
    }

    @Synchronized
    fun clearAllForTest() {
        registeredScripts.clear()
        preferencesOrNull()?.edit()?.clear()?.apply()
    }

    private fun RegisteredContentScript.toExtensionModule(
        context: Context,
        extensionId: String
    ): ExtensionModule? {
        val jsCode = loadResourceBundle(context, extensionId, js, isCss = false)
        val cssCode = loadResourceBundle(context, extensionId, css, isCss = true)
        if (jsCode.isBlank() && cssCode.isBlank()) return null

        val matchRules = mutableListOf<UrlMatchRule>()
        matches.forEach { pattern ->
            matchRules += UrlMatchRule(pattern = normalizeMatchPattern(pattern))
        }
        excludeMatches.forEach { pattern ->
            matchRules += UrlMatchRule(
                pattern = normalizeMatchPattern(pattern),
                exclude = true
            )
        }

        val permissions = buildList {
            add(ModulePermission.DOM_ACCESS)
            if (cssCode.isNotBlank()) add(ModulePermission.CSS_INJECT)
        }.distinct()

        return ExtensionModule(
            id = "${extensionId}_regcs_${id}",
            name = "Registered Content Script [$id]",
            description = "Dynamically registered Chrome content script",
            icon = "extension",
            category = ModuleCategory.FUNCTION_ENHANCE,
            version = ModuleVersion(name = "dynamic"),
            code = jsCode,
            cssCode = cssCode,
            runAt = parseRunAt(runAt),
            urlMatches = matchRules,
            permissions = permissions,
            enabled = true,
            runMode = ModuleRunMode.AUTO,
            sourceType = ModuleSourceType.CHROME_EXTENSION,
            chromeExtId = extensionId,
            world = if (world.equals("MAIN", ignoreCase = true)) "MAIN" else "ISOLATED",
            noframes = !allFrames
        )
    }

    private fun loadResourceBundle(
        context: Context,
        extensionId: String,
        paths: List<String>,
        isCss: Boolean
    ): String {
        return ChromeExtensionResourceLoader.loadResourceBundle(
            context = context,
            extensionId = extensionId,
            paths = paths,
            isCss = isCss
        )
    }

    private fun normalizeMatchPattern(pattern: String): String {
        return if (pattern == "<all_urls>") "*" else pattern
    }

    private fun parseRunAt(runAt: String): ModuleRunTime {
        return when (runAt.trim().lowercase()) {
            "document_start" -> ModuleRunTime.DOCUMENT_START
            "document_end" -> ModuleRunTime.DOCUMENT_END
            else -> ModuleRunTime.DOCUMENT_IDLE
        }
    }

    private fun parseScripts(scriptsJson: String): List<RegisteredContentScript> {
        val trimmed = scriptsJson.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        return try {
            val root = JsonParser.parseString(trimmed)
            if (!root.isJsonArray) return emptyList()
            root.asJsonArray.mapNotNull { element ->
                val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val script = RegisteredContentScript(
                    id = obj.optString("id"),
                    matches = obj.optStringList("matches"),
                    excludeMatches = obj.optStringList("excludeMatches"),
                    js = obj.optStringList("js"),
                    css = obj.optStringList("css"),
                    runAt = obj.optString("runAt").ifBlank { "document_idle" },
                    allFrames = obj.optBoolean("allFrames", false),
                    matchOriginAsFallback = obj.optBoolean("matchOriginAsFallback", false),
                    persistAcrossSessions = obj.optBoolean("persistAcrossSessions", true),
                    world = obj.optString("world").ifBlank { "ISOLATED" }
                )
                script.takeIf {
                    it.id.isNotBlank() &&
                        it.matches.isNotEmpty() &&
                        (it.js.isNotEmpty() || it.css.isNotEmpty())
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse registered content scripts", e)
            emptyList()
        }
    }

    private fun parseFilterIds(filterJson: String): Set<String>? {
        val trimmed = filterJson.trim()
        if (trimmed.isEmpty() || trimmed == "{}" || trimmed == "null") return null
        return try {
            val root = JsonParser.parseString(trimmed)
            if (!root.isJsonObject) return emptySet()
            val ids = root.asJsonObject.get("ids")
            if (ids == null || !ids.isJsonArray) return emptySet()
            ids.asJsonArray.mapNotNull { element ->
                element.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)
            }.toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse content script filter", e)
            emptySet()
        }
    }

    private fun getScriptsMutable(extensionId: String): MutableList<RegisteredContentScript> {
        return registeredScripts.getOrPut(extensionId) {
            loadPersistedScripts(extensionId).toMutableList()
        }
    }

    private fun loadPersistedScripts(extensionId: String): List<RegisteredContentScript> {
        val prefs = preferencesOrNull() ?: return emptyList()
        val json = prefs.getString(extensionId, null).orEmpty()
        if (json.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<RegisteredContentScript>>(json, scriptListType) ?: emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load persisted content scripts for $extensionId", e)
            emptyList()
        }
    }

    private fun persist(extensionId: String) {
        val prefs = preferencesOrNull() ?: return
        val persistentScripts = getScriptsMutable(extensionId).filter { it.persistAcrossSessions }
        if (persistentScripts.isEmpty()) {
            prefs.edit().remove(extensionId).apply()
            return
        }
        prefs.edit().putString(extensionId, gson.toJson(persistentScripts)).apply()
    }

    private fun preferencesOrNull() =
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun JsonObject.optString(name: String): String {
        val value = get(name) ?: return ""
        return if (value.isJsonNull) "" else value.asString
    }

    private fun JsonObject.optBoolean(name: String, defaultValue: Boolean): Boolean {
        val value = get(name) ?: return defaultValue
        return if (value.isJsonPrimitive) value.asBoolean else defaultValue
    }

    private fun JsonObject.optStringList(name: String): List<String> {
        val value = get(name) ?: return emptyList()
        if (!value.isJsonArray) return emptyList()
        return value.asJsonArray.mapNotNull { element ->
            element.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)
        }
    }
}
