package com.webtoapp.core.market

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.webtoapp.BuildConfig
import com.webtoapp.core.extension.ConfigItemType
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.ModuleConfigItem
import com.webtoapp.core.extension.ModulePermission
import com.webtoapp.core.extension.ModuleRunTime
import com.webtoapp.core.extension.ModuleSourceType
import com.webtoapp.core.extension.ModuleVersion
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Pulls the community module catalog straight from
 * `github.com/shiahonb777/web-to-app/tree/main/modules` and feeds it into
 * the in-app Module Market UI.
 *
 * The flow is intentionally simple to keep this server-less:
 *
 * ```
 * registry.json   ─►  in-memory list  ─►  user picks a module
 *                                              │
 *                                              ▼
 *                                  module.json + main.js (+ optional style.css)
 *                                              │
 *                                              ▼
 *                                  ExtensionManager.addModule(...)
 * ```
 *
 * GitHub raw is the primary source. jsDelivr CDN is a fallback that mirrors
 * GitHub content with HTTPS-friendly caching, which is helpful for users in
 * regions where `raw.githubusercontent.com` is flaky.
 */
class ModuleMarketRepository private constructor(
    private val context: Context,
    private val extensionManager: ExtensionManager
) {

    companion object {
        private const val TAG = "ModuleMarket"

        // GitHub repo coordinates. Kept as constants so a future fork can swap
        // them via a single edit.
        private const val OWNER = "shiahonb777"
        private const val REPO = "web-to-app"
        private const val BRANCH = "main"
        private const val MODULES_DIR = "modules"

        /** Cache TTL for the registry index. */
        private const val REGISTRY_TTL_MS = 60 * 60 * 1000L // 1 hour

        private const val CACHE_DIR_NAME = "module_market"
        private const val REGISTRY_CACHE_FILE = "registry.json"

        @Volatile
        private var INSTANCE: ModuleMarketRepository? = null

        fun getInstance(context: Context, extensionManager: ExtensionManager): ModuleMarketRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModuleMarketRepository(context.applicationContext, extensionManager).also { INSTANCE = it }
            }
        }

        /**
         * Sources tried in order. Each is a base URL; concrete file URLs are
         * built by appending the relative path under `modules/`.
         */
        private val SOURCES: List<String> = listOf(
            "https://raw.githubusercontent.com/$OWNER/$REPO/$BRANCH/$MODULES_DIR",
            // jsDelivr proxies GitHub with global CDN caching — useful where
            // raw.githubusercontent is throttled or blocked.
            "https://cdn.jsdelivr.net/gh/$OWNER/$REPO@$BRANCH/$MODULES_DIR"
        )
    }

    private val gson: Gson = GsonBuilder().setLenient().create()
    private val httpClient = NetworkModule.defaultClient

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    }

    private val _state = MutableStateFlow<MarketState>(MarketState.Idle)
    val state: StateFlow<MarketState> = _state.asStateFlow()

    /**
     * Combined view: the registry entries enriched with each module's local
     * install state. Recomputes whenever the registry refreshes or the user's
     * extension list changes.
     */
    val views: kotlinx.coroutines.flow.Flow<List<MarketModuleView>> =
        combine(_state, extensionManager.modules, extensionManager.builtInModules) { st, user, builtIn ->
            val entries = (st as? MarketState.Loaded)?.entries ?: emptyList()
            val installed = (user + builtIn).associateBy { it.id }
            entries.map { entry ->
                val local = installed[entry.id]
                if (local == null) {
                    MarketModuleView(entry, MarketInstallState.NotInstalled, null)
                } else {
                    val cmp = compareSemver(entry.version, local.version.name)
                    val state = if (cmp > 0) MarketInstallState.UpdateAvailable else MarketInstallState.UpToDate
                    MarketModuleView(entry, state, local.version.name)
                }
            }
        }

    /**
     * Fetch the registry. Uses a cached copy if it is fresh, otherwise hits
     * the network. Pass `force = true` for pull-to-refresh.
     */
    suspend fun refresh(force: Boolean = false) = withContext(Dispatchers.IO) {
        val cached = readCachedRegistry()
        if (!force && cached != null && cacheAgeMs() < REGISTRY_TTL_MS) {
            _state.value = MarketState.Loaded(filterEntries(cached.modules), fromCache = true)
            return@withContext
        }

        _state.value = MarketState.Loading
        val raw = fetchRaw("registry.json")
        if (raw == null) {
            // Network failed — fall back to whatever we cached previously so
            // the user at least sees stale data instead of an empty screen.
            if (cached != null) {
                _state.value = MarketState.Loaded(filterEntries(cached.modules), fromCache = true)
            } else {
                _state.value = MarketState.Error("Could not reach the module market. Check your network and try again.")
            }
            return@withContext
        }

        val parsed = try {
            gson.fromJson(raw, ModuleMarketRegistry::class.java)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse registry.json", e)
            null
        }

        if (parsed == null) {
            _state.value = MarketState.Error("Module registry was malformed.")
            return@withContext
        }

        writeCachedRegistry(raw)
        _state.value = MarketState.Loaded(filterEntries(parsed.modules), fromCache = false)
    }

    /**
     * Download a module's full payload, build an [ExtensionModule], and hand
     * it to [ExtensionManager]. Re-installing an existing module overwrites
     * it (used to apply updates).
     */
    suspend fun install(entry: ModuleMarketEntry): Result<ExtensionModule> = withContext(Dispatchers.IO) {
        try {
            val manifestRaw = fetchRaw("${entry.path}/module.json")
                ?: return@withContext Result.failure(IOException("module.json download failed"))
            val mainJs = fetchRaw("${entry.path}/main.js")
                ?: return@withContext Result.failure(IOException("main.js download failed"))
            val styleCss = if (entry.hasCss) fetchRaw("${entry.path}/style.css").orEmpty() else ""

            val manifest = try {
                gson.fromJson(manifestRaw, RemoteManifest::class.java)
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalStateException("module.json is malformed", e))
            }

            val module = manifest.toExtensionModule(
                fallbackId = entry.id,
                fallbackName = entry.name,
                code = mainJs,
                cssCode = styleCss
            )

            // Reuse `addModule` which already handles upsert by id, validation,
            // and persistence — so updating a module is just installing again.
            extensionManager.addModule(module)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Install failed for ${entry.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Builds a public GitHub URL pointing at the module's source folder, for
     * a "View on GitHub" link in the UI.
     */
    fun githubUrl(entry: ModuleMarketEntry): String =
        "https://github.com/$OWNER/$REPO/tree/$BRANCH/$MODULES_DIR/${entry.path}"

    /** URL of the contributing guide (the README in the modules directory). */
    val contributingUrl: String =
        "https://github.com/$OWNER/$REPO/blob/$BRANCH/$MODULES_DIR/README.md"


    // ─── HTTP plumbing ──────────────────────────────────────────────────────

    private fun fetchRaw(relativePath: String): String? {
        for (base in SOURCES) {
            val url = "$base/$relativePath"
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WebToApp/${BuildConfig.VERSION_NAME}")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        return resp.body?.string()
                    }
                    AppLogger.w(TAG, "fetch $url -> HTTP ${resp.code}")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "fetch $url failed: ${e.message}")
            }
        }
        return null
    }

    private fun readCachedRegistry(): ModuleMarketRegistry? {
        val file = File(cacheDir, REGISTRY_CACHE_FILE)
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), ModuleMarketRegistry::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCachedRegistry(raw: String) {
        try {
            File(cacheDir, REGISTRY_CACHE_FILE).writeText(raw)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to write registry cache: ${e.message}")
        }
    }

    private fun cacheAgeMs(): Long {
        val file = File(cacheDir, REGISTRY_CACHE_FILE)
        if (!file.exists()) return Long.MAX_VALUE
        return System.currentTimeMillis() - file.lastModified()
    }

    private fun filterEntries(entries: List<ModuleMarketEntry>): List<ModuleMarketEntry> {
        // Drop entries that require a newer client than the user has installed.
        return entries.filter { it.minAppVersion <= BuildConfig.VERSION_CODE }
    }


    // ─── Remote manifest schema (mirrors `module.json`) ─────────────────────

    private data class RemoteManifest(
        val id: String? = null,
        val name: String? = null,
        val description: String? = null,
        val icon: String? = null,
        val category: String? = null,
        val tags: List<String> = emptyList(),
        val version: ModuleVersion? = null,
        val author: com.webtoapp.core.extension.ModuleAuthor? = null,
        val runAt: String? = null,
        val urlMatches: List<com.webtoapp.core.extension.UrlMatchRule> = emptyList(),
        val permissions: List<String> = emptyList(),
        val configItems: List<ModuleConfigItem> = emptyList()
    ) {
        fun toExtensionModule(
            fallbackId: String,
            fallbackName: String,
            code: String,
            cssCode: String
        ): ExtensionModule {
            return ExtensionModule(
                id = id?.takeIf { it.isNotBlank() } ?: fallbackId,
                name = name?.takeIf { it.isNotBlank() } ?: fallbackName,
                description = description.orEmpty(),
                icon = icon ?: "package",
                category = parseEnum(category, ModuleCategory.OTHER),
                tags = tags,
                version = version ?: ModuleVersion(),
                author = author,
                code = code,
                cssCode = cssCode,
                runAt = parseEnum(runAt, ModuleRunTime.DOCUMENT_END),
                urlMatches = urlMatches,
                permissions = permissions.mapNotNull { p ->
                    runCatching { ModulePermission.valueOf(p) }.getOrNull()
                },
                configItems = configItems,
                enabled = true,
                builtIn = false,
                sourceType = ModuleSourceType.CUSTOM
            )
        }

        private inline fun <reified T : Enum<T>> parseEnum(value: String?, default: T): T {
            if (value.isNullOrBlank()) return default
            return runCatching { enumValueOf<T>(value) }.getOrElse { default }
        }
    }
}

/**
 * Top-level state of the market screen.
 */
sealed class MarketState {
    object Idle : MarketState()
    object Loading : MarketState()
    data class Loaded(val entries: List<ModuleMarketEntry>, val fromCache: Boolean) : MarketState()
    data class Error(val message: String) : MarketState()
}

/**
 * Compares two semver-ish version strings ("1.2.3"). Falls back to lexical
 * comparison when either input is not strictly numeric.
 *
 * Returns a positive number when `a` is newer than `b`.
 */
internal fun compareSemver(a: String, b: String): Int {
    val ap = a.split(".").mapNotNull { it.toIntOrNull() }
    val bp = b.split(".").mapNotNull { it.toIntOrNull() }
    if (ap.isEmpty() || bp.isEmpty()) return a.compareTo(b)
    val len = maxOf(ap.size, bp.size)
    for (i in 0 until len) {
        val av = ap.getOrElse(i) { 0 }
        val bv = bp.getOrElse(i) { 0 }
        if (av != bv) return av - bv
    }
    return 0
}

// Suppress unused for ConfigItemType import — it's actually used via reflection
// inside the Gson deserializer for `configItems`, so keep the import alive.
@Suppress("unused")
private val configItemTypeAnchor: Class<ConfigItemType> = ConfigItemType::class.java
