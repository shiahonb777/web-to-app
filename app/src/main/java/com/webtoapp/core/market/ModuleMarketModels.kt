package com.webtoapp.core.market

import com.google.gson.annotations.SerializedName
import com.webtoapp.core.extension.ModuleAuthor
import com.webtoapp.core.extension.UrlMatchRule

/**
 * Lightweight entry returned by `registry.json` — enough to render the market
 * listing without downloading every module's full source.
 *
 * The contract lives in https://github.com/shiahonb777/web-to-app/blob/main/modules/README.md
 */
data class ModuleMarketEntry(
    @SerializedName("id")
    val id: String,

    /** Folder name under `modules/` in the GitHub repo. */
    @SerializedName("path")
    val path: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String = "",

    @SerializedName("icon")
    val icon: String = "package",

    @SerializedName("category")
    val category: String = "OTHER",

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("version")
    val version: String = "1.0.0",

    /** Minimum WebToApp `versionCode` required. Older clients hide the entry. */
    @SerializedName("minAppVersion")
    val minAppVersion: Int = 0,

    @SerializedName("author")
    val author: ModuleAuthor? = null,

    @SerializedName("runAt")
    val runAt: String = "DOCUMENT_END",

    @SerializedName("permissions")
    val permissions: List<String> = emptyList(),

    @SerializedName("urlMatches")
    val urlMatches: List<UrlMatchRule> = emptyList(),

    /** Whether the module ships a `style.css` next to `main.js`. */
    @SerializedName("hasCss")
    val hasCss: Boolean = false
)

/**
 * Wire format of `registry.json`.
 */
data class ModuleMarketRegistry(
    @SerializedName("schema")
    val schema: Int = 1,

    @SerializedName("updatedAt")
    val updatedAt: String = "",

    @SerializedName("modules")
    val modules: List<ModuleMarketEntry> = emptyList()
)

/**
 * Indicates whether a market entry is already present in the user's local
 * extension manager and, if so, whether the remote version is newer.
 */
enum class MarketInstallState {
    NotInstalled,
    UpToDate,
    UpdateAvailable
}

/**
 * UI-friendly view of a market entry combined with local state.
 */
data class MarketModuleView(
    val entry: ModuleMarketEntry,
    val state: MarketInstallState,
    /** The version currently installed on this device, when applicable. */
    val installedVersion: String? = null
)
