package com.webtoapp.core.extension

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger

/**
 * 内置 Chrome 浏览器扩展
 * 
 * 从 Android assets 加载预编译的 Chrome 扩展，
 * 将其转换为 ExtensionModule 以复用现有的注入和管理系统。
 * 
 * 与 BuiltInModules 不同，此处的模块需要：
 * 1. Chrome Extension API Polyfill（由 WebViewManager 在注入前处理）
 * 2. 大型 JS/CSS 文件从 assets 懒加载
 * 3. 支持 MAIN world 脚本
 */
object BuiltInChromeExtensions {
    
    private const val TAG = "BuiltInChromeExtensions"
    
    // BewlyCat 扩展标识符
    private const val BEWLYCAT_EXT_ID = "bewlycat"
    private const val BEWLYCAT_VERSION = "1.5.7"
    
    // B站 URL 匹配规则
    // 包含 m.bilibili.com：移动设备上 www.bilibili.com 会 302 重定向到 m.bilibili.com
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
     * 获取所有内置 Chrome 扩展模块
     * 
     * @param context Android Context（用于读取 assets）
     * @return ExtensionModule 列表
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
     * 加载 BewlyCat 扩展
     * 
     * BewlyCat 有两个 content_script：
     * 1. ISOLATED world - 主内容脚本 + CSS（2.2MB JS + 702KB CSS）
     * 2. MAIN world - 注入脚本（7.5KB，处理 DOM 拦截和设置同步）
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
                enabled = false,  // 默认不启用，让用户自己开启
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
                enabled = false,  // 与主模块同步启用
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
     * 从 assets 读取文件内容
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
