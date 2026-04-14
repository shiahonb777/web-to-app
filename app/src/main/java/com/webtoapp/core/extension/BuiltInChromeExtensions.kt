package com.webtoapp.core.extension

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger

/**
 * Chrome extension.
 *
 * from Android assets Chrome extension.
 * as ExtensionModule use manage .
 *
 * and BuiltInModules not .
 * 1. Chrome Extension API Polyfill.
 * 2. large JS/CSS from assets.
 * 3. Supports MAIN world.
 */
object BuiltInChromeExtensions {
    
    private const val TAG = "BuiltInChromeExtensions"
    
    // BewlyCat extension.
    private const val BEWLYCAT_EXT_ID = "bewlycat"
    private const val BEWLYCAT_VERSION = "1.5.7"
    
    // B URL rules.
    // m.bilibili.com www.bilibili.com 302 to m.bilibili.com.
    private val BILIBILI_URL_MATCHES = listOf(
        UrlMatchRule("*://www.bilibili.com/*"),
        UrlMatchRule("*://m.bilibili.com/*"),
        UrlMatchRule("*://search.bilibili.com/*"),
        UrlMatchRule("*://t.bilibili.com/*"),
        UrlMatchRule("*://space.bilibili.com/*"),
        UrlMatchRule("*://message.bilibili.com/*"),
        UrlMatchRule("*://member.bilibili.com/*"),
        UrlMatchRule("*://account.bilibili.com/*"),
        UrlMatchRule("*://www.hdslb.com/*"),
        UrlMatchRule("*://passport.bilibili.com/*"),
        UrlMatchRule("*://music.bilibili.com/*")
    )
    
    /**
     * Get Chrome extension.
     *
     * @param context Android Context.
     * @return ExtensionModule.
     */
    fun getAll(context: Context): List<ExtensionModule> {
        return try {
            bewlyCat(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load built-in Chrome extensions", e)
            emptyList()
        }
    }
    
    /**
     * BewlyCat extension.
     *
     * BewlyCat content_script.
     * 1. ISOLATED world - + CSS2.2MB JS + 702KB CSS.
     * 2. MAIN world -.
     */
    private fun bewlyCat(context: Context): List<ExtensionModule> {
        val modules = mutableListOf<ExtensionModule>()
        
        // 1. ISOLATED world content script + CSS
        val contentJs = loadAsset(context, "extensions/bewlycat/content.js")
        val contentCss = loadAsset(context, "extensions/bewlycat/style.css")
        
        if (contentJs != null) {
            modules.add(ExtensionModule(
                id = "builtin-chrome-bewlycat-content",
                name = Strings.builtinBewlyCat,
                description = Strings.builtinBewlyCatDesc,
                icon = "drawable:ic_ext_bewlycat",
                category = ModuleCategory.CONTENT_ENHANCE,
                tags = listOf("Bilibili", "B站", "UI"),
                version = ModuleVersion(1, BEWLYCAT_VERSION, Strings.builtInVersion),
                author = ModuleAuthor("BewlyCat"),
                builtIn = true,
                enabled = false,  // Note.
                runAt = ModuleRunTime.DOCUMENT_START,
                urlMatches = BILIBILI_URL_MATCHES,
                permissions = listOf(
                    ModulePermission.DOM_ACCESS,
                    ModulePermission.STORAGE,
                    ModulePermission.CSS_INJECT
                ),
                code = contentJs,
                cssCode = contentCss ?: "",
                sourceType = ModuleSourceType.CHROME_EXTENSION,
                chromeExtId = BEWLYCAT_EXT_ID,
                world = "ISOLATED",
                backgroundScript = "background/index.js",
                noframes = false  // all_frames: true in manifest
            ))
        } else {
            AppLogger.w(TAG, "BewlyCat content.js not found in assets")
        }
        
        // 2. MAIN world inject script
        val injectJs = loadAsset(context, "extensions/bewlycat/inject.js")
        
        if (injectJs != null) {
            modules.add(ExtensionModule(
                id = "builtin-chrome-bewlycat-inject",
                name = "${Strings.builtinBewlyCat} (MAIN)",
                description = "BewlyCat MAIN world script",
                icon = "drawable:ic_ext_bewlycat",
                category = ModuleCategory.CONTENT_ENHANCE,
                version = ModuleVersion(1, BEWLYCAT_VERSION, Strings.builtInVersion),
                author = ModuleAuthor("BewlyCat"),
                builtIn = true,
                enabled = false,  // Note.
                runAt = ModuleRunTime.DOCUMENT_START,
                urlMatches = BILIBILI_URL_MATCHES,
                permissions = listOf(ModulePermission.DOM_ACCESS),
                code = injectJs,
                sourceType = ModuleSourceType.CHROME_EXTENSION,
                chromeExtId = BEWLYCAT_EXT_ID,
                world = "MAIN",
                noframes = false
            ))
        } else {
            AppLogger.w(TAG, "BewlyCat inject.js not found in assets")
        }
        
        AppLogger.i(TAG, "Loaded BewlyCat: ${modules.size} modules")
        return modules
    }
    
    /**
     * from assets.
     */
    private fun loadAsset(context: Context, path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to load asset: $path", e)
            null
        }
    }
}