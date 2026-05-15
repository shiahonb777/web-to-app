package com.webtoapp.core.extension

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger












object BuiltInChromeExtensions {

    private const val TAG = "BuiltInChromeExtensions"


    private const val BEWLYCAT_EXT_ID = "bewlycat"
    private const val BEWLYCAT_VERSION = "1.5.7"



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







    fun getAll(context: Context): List<ExtensionModule> {
        return try {
            bewlyCat(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load built-in Chrome extensions", e)
            emptyList()
        }
    }








    private fun bewlyCat(context: Context): List<ExtensionModule> {
        val modules = mutableListOf<ExtensionModule>()
        val manifestJson = loadAsset(context, "extensions/bewlycat/manifest.json").orEmpty()


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
                enabled = false,
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
                manifestJson = manifestJson,
                noframes = false
            ))
        } else {
            AppLogger.w(TAG, "BewlyCat content.js not found in assets")
        }


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
                enabled = false,
                runAt = ModuleRunTime.DOCUMENT_START,
                urlMatches = BILIBILI_URL_MATCHES,
                permissions = listOf(ModulePermission.DOM_ACCESS),
                code = injectJs,
                sourceType = ModuleSourceType.CHROME_EXTENSION,
                chromeExtId = BEWLYCAT_EXT_ID,
                world = "MAIN",
                manifestJson = manifestJson,
                noframes = false
            ))
        } else {
            AppLogger.w(TAG, "BewlyCat inject.js not found in assets")
        }

        AppLogger.i(TAG, "Loaded BewlyCat: ${modules.size} modules")
        return modules
    }




    private fun loadAsset(context: Context, path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to load asset: $path", e)
            null
        }
    }
}
