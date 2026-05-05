package com.webtoapp.core.extension

import com.google.gson.annotations.SerializedName
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.GsonProvider
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


private const val REGEX_TIMEOUT_MS = 200L


private val regexExecutor by lazy {
    Executors.newSingleThreadExecutor { r ->
        Thread(r, "SafeRegexMatcher").apply { isDaemon = true }
    }
}


private val regexCache = object : LinkedHashMap<String, Regex>(32, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Regex>?) = size > 64
}


private fun safeRegexMatch(pattern: String, input: String): Boolean {
    return try {
        val compiledRegex = synchronized(regexCache) {
            regexCache.getOrPut(pattern) { Regex(pattern) }
        }
        val future = regexExecutor.submit<Boolean> { compiledRegex.containsMatchIn(input) }
        future.get(REGEX_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    } catch (e: TimeoutException) {
        false
    } catch (e: Exception) {
        false
    }
}


private fun String.escapeForJsSingleQuote(): String =
    this.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")


private fun String.escapeForJsTemplate(): String =
    this.replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("\${", "\\\${")
        .replace("\n", "\\n")
        .replace("\r", "\\r")




enum class ModuleCategory(val icon: String) {
    CONTENT_FILTER("block"),
    CONTENT_ENHANCE("auto_awesome"),
    STYLE_MODIFIER("palette"),
    THEME("rainbow"),
    FUNCTION_ENHANCE("bolt"),
    AUTOMATION("smart_toy"),
    NAVIGATION("explore"),
    DATA_EXTRACT("analytics"),
    DATA_SAVE("save"),
    INTERACTION("mouse"),
    ACCESSIBILITY("accessibility"),
    MEDIA("movie"),
    VIDEO("videocam"),
    IMAGE("image"),
    AUDIO("music_note"),
    SECURITY("lock"),
    ANTI_TRACKING("person_search"),
    SOCIAL("chat"),
    SHOPPING("shopping_cart"),
    READING("book"),
    TRANSLATE("globe"),
    DEVELOPER("wrench"),
    OTHER("package");

    fun getDisplayName(): String = when (this) {
        CONTENT_FILTER -> Strings.catContentFilter
        CONTENT_ENHANCE -> Strings.catContentEnhance
        STYLE_MODIFIER -> Strings.catStyleModifier
        THEME -> Strings.catTheme
        FUNCTION_ENHANCE -> Strings.catFunctionEnhance
        AUTOMATION -> Strings.catAutomation
        NAVIGATION -> Strings.catNavigation
        DATA_EXTRACT -> Strings.catDataExtract
        DATA_SAVE -> Strings.catDataSave
        INTERACTION -> Strings.catInteraction
        ACCESSIBILITY -> Strings.catAccessibility
        MEDIA -> Strings.catMedia
        VIDEO -> Strings.catVideo
        IMAGE -> Strings.catImage
        AUDIO -> Strings.catAudio
        SECURITY -> Strings.catSecurity
        ANTI_TRACKING -> Strings.catAntiTracking
        SOCIAL -> Strings.catSocial
        SHOPPING -> Strings.catShopping
        READING -> Strings.catReading
        TRANSLATE -> Strings.catTranslate
        DEVELOPER -> Strings.catDeveloper
        OTHER -> Strings.catOther
    }

    fun getDescription(): String = when (this) {
        CONTENT_FILTER -> Strings.catContentFilterDesc
        CONTENT_ENHANCE -> Strings.catContentEnhanceDesc
        STYLE_MODIFIER -> Strings.catStyleModifierDesc
        THEME -> Strings.catThemeDesc
        FUNCTION_ENHANCE -> Strings.catFunctionEnhanceDesc
        AUTOMATION -> Strings.catAutomationDesc
        NAVIGATION -> Strings.catNavigationDesc
        DATA_EXTRACT -> Strings.catDataExtractDesc
        DATA_SAVE -> Strings.catDataSaveDesc
        INTERACTION -> Strings.catInteractionDesc
        ACCESSIBILITY -> Strings.catAccessibilityDesc
        MEDIA -> Strings.catMediaDesc
        VIDEO -> Strings.catVideoDesc
        IMAGE -> Strings.catImageDesc
        AUDIO -> Strings.catAudioDesc
        SECURITY -> Strings.catSecurityDesc
        ANTI_TRACKING -> Strings.catAntiTrackingDesc
        SOCIAL -> Strings.catSocialDesc
        SHOPPING -> Strings.catShoppingDesc
        READING -> Strings.catReadingDesc
        TRANSLATE -> Strings.catTranslateDesc
        DEVELOPER -> Strings.catDeveloperDesc
        OTHER -> Strings.catOtherDesc
    }
}




enum class ModuleRunTime(val jsEvent: String) {
    DOCUMENT_START(""),
    DOCUMENT_END("DOMContentLoaded"),
    DOCUMENT_IDLE("load"),
    CONTEXT_MENU("contextmenu"),
    BEFORE_UNLOAD("beforeunload");

    fun getDisplayName(): String = when (this) {
        DOCUMENT_START -> Strings.runTimeDocStart
        DOCUMENT_END -> Strings.runTimeDocEnd
        DOCUMENT_IDLE -> Strings.runTimeDocIdle
        CONTEXT_MENU -> Strings.runTimeContextMenu
        BEFORE_UNLOAD -> Strings.runTimeBeforeUnload
    }

    fun getDescription(): String = when (this) {
        DOCUMENT_START -> Strings.runTimeDocStartDesc
        DOCUMENT_END -> Strings.runTimeDocEndDesc
        DOCUMENT_IDLE -> Strings.runTimeDocIdleDesc
        CONTEXT_MENU -> Strings.runTimeContextMenuDesc
        BEFORE_UNLOAD -> Strings.runTimeBeforeUnloadDesc
    }
}




enum class ModuleTrigger {
    AUTO,
    MANUAL,
    INTERVAL,
    MUTATION,
    SCROLL,
    CLICK,
    HOVER,
    FOCUS,
    INPUT,
    VISIBILITY;

    val displayName: String get() = when (this) {
        AUTO -> Strings.triggerAuto
        MANUAL -> Strings.triggerManual
        INTERVAL -> Strings.triggerInterval
        MUTATION -> Strings.triggerMutation
        SCROLL -> Strings.triggerScroll
        CLICK -> Strings.triggerClick
        HOVER -> Strings.triggerHover
        FOCUS -> Strings.triggerFocus
        INPUT -> Strings.triggerInput
        VISIBILITY -> Strings.triggerVisibility
    }

    val description: String get() = when (this) {
        AUTO -> Strings.triggerAutoDesc
        MANUAL -> Strings.triggerManualDesc
        INTERVAL -> Strings.triggerIntervalDesc
        MUTATION -> Strings.triggerMutationDesc
        SCROLL -> Strings.triggerScrollDesc
        CLICK -> Strings.triggerClickDesc
        HOVER -> Strings.triggerHoverDesc
        FOCUS -> Strings.triggerFocusDesc
        INPUT -> Strings.triggerInputDesc
        VISIBILITY -> Strings.triggerVisibilityDesc
    }
}




enum class ModulePermission(val dangerous: Boolean = false) {
    DOM_ACCESS,
    DOM_OBSERVE,
    CSS_INJECT,
    STORAGE,
    COOKIE(true),
    INDEXED_DB(true),
    CACHE,
    NETWORK(true),
    WEBSOCKET(true),
    FETCH_INTERCEPT(true),
    CLIPBOARD(true),
    NOTIFICATION,
    ALERT,
    KEYBOARD,
    MOUSE,
    TOUCH,
    LOCATION(true),
    CAMERA(true),
    MICROPHONE(true),
    DEVICE_INFO,
    MEDIA,
    FULLSCREEN,
    PICTURE_IN_PICTURE,
    SCREEN_CAPTURE(true),
    DOWNLOAD,
    FILE_ACCESS(true),
    EVAL(true),
    IFRAME(true),
    WINDOW_OPEN,
    HISTORY,
    NAVIGATION;

    val displayName: String get() = when (this) {
        DOM_ACCESS -> Strings.permDomAccess
        DOM_OBSERVE -> Strings.permDomObserve
        CSS_INJECT -> Strings.permCssInject
        STORAGE -> Strings.permStorage
        COOKIE -> Strings.permCookie
        INDEXED_DB -> Strings.permIndexedDb
        CACHE -> Strings.permCache
        NETWORK -> Strings.permNetwork
        WEBSOCKET -> Strings.permWebsocket
        FETCH_INTERCEPT -> Strings.permFetchIntercept
        CLIPBOARD -> Strings.permClipboard
        NOTIFICATION -> Strings.permNotification
        ALERT -> Strings.permAlert
        KEYBOARD -> Strings.permKeyboard
        MOUSE -> Strings.permMouse
        TOUCH -> Strings.permTouch
        LOCATION -> Strings.permLocation
        CAMERA -> Strings.permCamera
        MICROPHONE -> Strings.permMicrophone
        DEVICE_INFO -> Strings.permDeviceInfo
        MEDIA -> Strings.permMedia
        FULLSCREEN -> Strings.permFullscreen
        PICTURE_IN_PICTURE -> Strings.permPip
        SCREEN_CAPTURE -> Strings.permScreenCapture
        DOWNLOAD -> Strings.permDownload
        FILE_ACCESS -> Strings.permFileAccess
        EVAL -> Strings.permEval
        IFRAME -> Strings.permIframe
        WINDOW_OPEN -> Strings.permWindowOpen
        HISTORY -> Strings.permHistory
        NAVIGATION -> Strings.permNavigation
    }

    val description: String get() = when (this) {
        DOM_ACCESS -> Strings.permDomAccessDesc
        DOM_OBSERVE -> Strings.permDomObserveDesc
        CSS_INJECT -> Strings.permCssInjectDesc
        STORAGE -> Strings.permStorageDesc
        COOKIE -> Strings.permCookieDesc
        INDEXED_DB -> Strings.permIndexedDbDesc
        CACHE -> Strings.permCacheDesc
        NETWORK -> Strings.permNetworkDesc
        WEBSOCKET -> Strings.permWebsocketDesc
        FETCH_INTERCEPT -> Strings.permFetchInterceptDesc
        CLIPBOARD -> Strings.permClipboardDesc
        NOTIFICATION -> Strings.permNotificationDesc
        ALERT -> Strings.permAlertDesc
        KEYBOARD -> Strings.permKeyboardDesc
        MOUSE -> Strings.permMouseDesc
        TOUCH -> Strings.permTouchDesc
        LOCATION -> Strings.permLocationDesc
        CAMERA -> Strings.permCameraDesc
        MICROPHONE -> Strings.permMicrophoneDesc
        DEVICE_INFO -> Strings.permDeviceInfoDesc
        MEDIA -> Strings.permMediaDesc
        FULLSCREEN -> Strings.permFullscreenDesc
        PICTURE_IN_PICTURE -> Strings.permPipDesc
        SCREEN_CAPTURE -> Strings.permScreenCaptureDesc
        DOWNLOAD -> Strings.permDownloadDesc
        FILE_ACCESS -> Strings.permFileAccessDesc
        EVAL -> Strings.permEvalDesc
        IFRAME -> Strings.permIframeDesc
        WINDOW_OPEN -> Strings.permWindowOpenDesc
        HISTORY -> Strings.permHistoryDesc
        NAVIGATION -> Strings.permNavigationDesc
    }
}




enum class ConfigItemType {
    TEXT, TEXTAREA, NUMBER, BOOLEAN,
    SELECT, MULTI_SELECT, RADIO, CHECKBOX,
    COLOR, URL, EMAIL, PASSWORD,
    REGEX, CSS_SELECTOR, JAVASCRIPT, JSON,
    RANGE, DATE, TIME, DATETIME,
    FILE, IMAGE;

    val displayName: String get() = when (this) {
        TEXT -> Strings.configTypeText
        TEXTAREA -> Strings.configTypeTextarea
        NUMBER -> Strings.configTypeNumber
        BOOLEAN -> Strings.configTypeBoolean
        SELECT -> Strings.configTypeSelect
        MULTI_SELECT -> Strings.configTypeMultiSelect
        RADIO -> Strings.configTypeRadio
        CHECKBOX -> Strings.configTypeCheckbox
        COLOR -> Strings.configTypeColor
        URL -> Strings.configTypeUrl
        EMAIL -> Strings.configTypeEmail
        PASSWORD -> Strings.configTypePassword
        REGEX -> Strings.configTypeRegex
        CSS_SELECTOR -> Strings.configTypeCssSelector
        JAVASCRIPT -> Strings.configTypeJavascript
        JSON -> Strings.configTypeJson
        RANGE -> Strings.configTypeRange
        DATE -> Strings.configTypeDate
        TIME -> Strings.configTypeTime
        DATETIME -> Strings.configTypeDatetime
        FILE -> Strings.configTypeFile
        IMAGE -> Strings.configTypeImage
    }

    val description: String get() = when (this) {
        TEXT -> Strings.configTypeTextDesc
        TEXTAREA -> Strings.configTypeTextareaDesc
        NUMBER -> Strings.configTypeNumberDesc
        BOOLEAN -> Strings.configTypeBooleanDesc
        SELECT -> Strings.configTypeSelectDesc
        MULTI_SELECT -> Strings.configTypeMultiSelectDesc
        RADIO -> Strings.configTypeRadioDesc
        CHECKBOX -> Strings.configTypeCheckboxDesc
        COLOR -> Strings.configTypeColorDesc
        URL -> Strings.configTypeUrlDesc
        EMAIL -> Strings.configTypeEmailDesc
        PASSWORD -> Strings.configTypePasswordDesc
        REGEX -> Strings.configTypeRegexDesc
        CSS_SELECTOR -> Strings.configTypeCssSelectorDesc
        JAVASCRIPT -> Strings.configTypeJavascriptDesc
        JSON -> Strings.configTypeJsonDesc
        RANGE -> Strings.configTypeRangeDesc
        DATE -> Strings.configTypeDateDesc
        TIME -> Strings.configTypeTimeDesc
        DATETIME -> Strings.configTypeDatetimeDesc
        FILE -> Strings.configTypeFileDesc
        IMAGE -> Strings.configTypeImageDesc
    }
}




data class ModuleConfigItem(
    @SerializedName("key")
    val key: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("type")
    val type: ConfigItemType = ConfigItemType.TEXT,
    @SerializedName("defaultValue")
    val defaultValue: String = "",
    @SerializedName("options")
    val options: List<String> = emptyList(),
    @SerializedName("required")
    val required: Boolean = false,
    @SerializedName("placeholder")
    val placeholder: String = "",
    @SerializedName("validation")
    val validation: String? = null
)






enum class ModuleUiType {
    FLOATING_BUTTON
}










data class ModuleUiConfig(
    @SerializedName("type")
    val type: ModuleUiType = ModuleUiType.FLOATING_BUTTON,


    @SerializedName("autoHide")
    val autoHide: Boolean = false,
    @SerializedName("autoHideDelay")
    val autoHideDelay: Int = 3000,
    @SerializedName("initiallyHidden")
    val initiallyHidden: Boolean = false,
    @SerializedName("showOnlyOnMatch")
    val showOnlyOnMatch: Boolean = true
) {
    companion object {

        val DEFAULT = ModuleUiConfig()
    }
}




data class ModuleAuthor(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("qq")
    val qq: String? = null
)




data class ModuleVersion(
    @SerializedName("code")
    val code: Int = 1,
    @SerializedName("name")
    val name: String = "1.0.0",
    @SerializedName("changelog")
    val changelog: String = ""
)




data class UrlMatchRule(
    @SerializedName("pattern")
    val pattern: String,
    @SerializedName("isRegex")
    val isRegex: Boolean = false,
    @SerializedName("exclude")
    val exclude: Boolean = false
)





enum class ModuleSourceType {
    CUSTOM,
    USERSCRIPT,
    CHROME_EXTENSION
}





enum class ModuleRunMode {
    INTERACTIVE,
    AUTO;

    fun getDisplayName(): String = when (this) {
        INTERACTIVE -> Strings.runModeInteractive
        AUTO -> Strings.runModeAuto
    }

    fun getDescription(): String = when (this) {
        INTERACTIVE -> Strings.runModeInteractiveDesc
        AUTO -> Strings.runModeAutoDesc
    }

    fun getIcon(): String = when (this) {
        INTERACTIVE -> "desktop_windows"
        AUTO -> "bolt"
    }
}






data class ExtensionModule(

    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("icon")
    val icon: String = "package",


    @SerializedName("category")
    val category: ModuleCategory = ModuleCategory.OTHER,
    @SerializedName("tags")
    val tags: List<String> = emptyList(),


    @SerializedName("version")
    val version: ModuleVersion = ModuleVersion(),
    @SerializedName("author")
    val author: ModuleAuthor? = null,


    @SerializedName("code")
    val code: String = "",
    @SerializedName("cssCode")
    val cssCode: String = "",




    @SerializedName("codeFiles")
    val codeFiles: Map<String, String> = emptyMap(),


    @SerializedName("runAt")
    val runAt: ModuleRunTime = ModuleRunTime.DOCUMENT_END,
    @SerializedName("urlMatches")
    val urlMatches: List<UrlMatchRule> = emptyList(),


    @SerializedName("permissions")
    val permissions: List<ModulePermission> = emptyList(),


    @SerializedName("configItems")
    val configItems: List<ModuleConfigItem> = emptyList(),
    @SerializedName("configValues")
    val configValues: Map<String, String> = emptyMap(),


    @SerializedName("dependencies")
    val dependencies: List<String> = emptyList(),


    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("builtIn")
    val builtIn: Boolean = false,


    @SerializedName("uiConfig")
    val uiConfig: ModuleUiConfig = ModuleUiConfig.DEFAULT,


    @SerializedName("runMode")
    val runMode: ModuleRunMode = ModuleRunMode.INTERACTIVE,


    @SerializedName("sourceType")
    val sourceType: ModuleSourceType = ModuleSourceType.CUSTOM,


    @SerializedName("chromeExtId")
    val chromeExtId: String = "",
    @SerializedName("world")
    val world: String = "ISOLATED",
    @SerializedName("backgroundScript")
    val backgroundScript: String = "",


    @SerializedName("gmGrants")
    val gmGrants: List<String> = emptyList(),
    @SerializedName("requireUrls")
    val requireUrls: List<String> = emptyList(),
    @SerializedName("resources")
    val resources: Map<String, String> = emptyMap(),
    @SerializedName("noframes")
    val noframes: Boolean = false,


    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson get() = GsonProvider.gson


        private const val SHARE_CODE_PREFIX_V1 = "WTA1:"
        private const val SHARE_CODE_PREFIX_V0 = ""




        fun fromJson(json: String): ExtensionModule? {
            return try {
                gson.fromJson(json, ExtensionModule::class.java)
            } catch (e: Exception) {
                null
            }
        }





        fun fromShareCode(shareCode: String): ExtensionModule? {
            return try {
                val json = when {

                    shareCode.startsWith(SHARE_CODE_PREFIX_V1) -> {
                        val compressed = android.util.Base64.decode(
                            shareCode.removePrefix(SHARE_CODE_PREFIX_V1),
                            android.util.Base64.DEFAULT
                        )
                        decompressGzip(compressed)
                    }

                    else -> {
                        String(android.util.Base64.decode(shareCode, android.util.Base64.DEFAULT))
                    }
                }
                fromJson(json)
            } catch (e: Exception) {
                null
            }
        }




        private fun decompressGzip(compressed: ByteArray): String {
            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(compressed)).use { gzip ->
                return gzip.bufferedReader().readText()
            }
        }




        private fun compressGzip(data: String): ByteArray {
            val bos = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(bos).use { gzip ->
                gzip.write(data.toByteArray())
            }
            return bos.toByteArray()
        }
    }




    fun toJson(): String = gson.toJson(this)





    fun toShareCode(): String {
        val compressed = compressGzip(toJson())
        return SHARE_CODE_PREFIX_V1 + android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP)
    }




    fun toShareCodeLegacy(): String {
        return android.util.Base64.encodeToString(toJson().toByteArray(), android.util.Base64.NO_WRAP)
    }




    fun matchesUrl(url: String): Boolean {
        if (urlMatches.isEmpty()) return true

        val includeRules = urlMatches.filter { !it.exclude }
        val excludeRules = urlMatches.filter { it.exclude }


        for (rule in excludeRules) {
            if (matchRule(url, rule)) return false
        }


        if (includeRules.isEmpty()) return true


        return includeRules.any { matchRule(url, it) }
    }

    private fun matchRule(url: String, rule: UrlMatchRule): Boolean {
        return if (rule.isRegex) {
            safeRegexMatch(rule.pattern, url)
        } else {


            val pattern = rule.pattern
            if (pattern == "*" || pattern == "<all_urls>") return true






            val regexPattern = buildString {
                append("^")
                var i = 0
                while (i < pattern.length) {
                    val c = pattern[i]
                    when {

                        c == '*' && pattern.startsWith("*://", i) -> {
                            append("(https?|ftp|file)://")
                            i += 4
                        }

                        c == '*' -> {
                            append(".*")
                            i++
                        }

                        c in ".+?^\${}()|[]\\/" -> {
                            append("\\")
                            append(c)
                            i++
                        }
                        else -> {
                            append(c)
                            i++
                        }
                    }
                }
                append("$")
            }
            try {
                Regex(regexPattern, RegexOption.IGNORE_CASE).matches(url)
            } catch (e: Exception) {

                url.contains(pattern, ignoreCase = true)
            }
        }
    }


    @Transient
    @Volatile
    private var _cachedExecutableCode: String? = null





    fun generateExecutableCode(): String {
        _cachedExecutableCode?.let { return it }
        val configJson = gson.toJson(configValues)
        val uiConfigJson = gson.toJson(mapOf(
            "type" to uiConfig.type.name,
            "autoHide" to uiConfig.autoHide,
            "autoHideDelay" to uiConfig.autoHideDelay,
            "initiallyHidden" to uiConfig.initiallyHidden,
            "showOnlyOnMatch" to uiConfig.showOnlyOnMatch
        ))
        val runModeStr = runMode.name

        val effectiveCode = if (codeFiles.isNotEmpty()) {

            val entryNames = setOf("main.js", "index.js", "app.js", "script.js", "content.js")
            val sortedFiles = codeFiles.entries.sortedWith(
                compareByDescending<Map.Entry<String, String>> { it.key.substringAfterLast('/').lowercase() in entryNames }
                    .thenBy { it.key }
            )
            sortedFiles.joinToString("\n\n") { (path, content) ->
                "// ========== $path ==========\n$content"
            }
        } else {
            code
        }

        return """
            (function() {
                'use strict';
                // Module配置
                const __MODULE_CONFIG__ = $configJson;
                const __MODULE_UI_CONFIG__ = $uiConfigJson;
                const __MODULE_RUN_MODE__ = '$runModeStr';
                const __MODULE_INFO__ = {
                    id: '${id.escapeForJsSingleQuote()}',
                    name: '${name.escapeForJsSingleQuote()}',
                    icon: '${icon.escapeForJsSingleQuote()}',
                    version: '${version.name}',
                    uiConfig: __MODULE_UI_CONFIG__,
                    runMode: __MODULE_RUN_MODE__
                };

                // Configure访问函数
                function getConfig(key, defaultValue) {
                    return __MODULE_CONFIG__[key] !== undefined ? __MODULE_CONFIG__[key] : defaultValue;
                }

                // CSS 注入
                ${if (cssCode.isNotBlank()) """
                (function() {
                    const style = document.createElement('style');
                    style.id = 'ext-module-${id}';
                    style.textContent = `${cssCode.escapeForJsTemplate()}`;
                    (document.head || document.documentElement).appendChild(style);
                })();
                """ else ""}

                // User代码
                try {
                    $effectiveCode
                } catch(e) {
                    console.error('[ExtModule: ${name.escapeForJsSingleQuote()}] Error:', e);
                }

                // 自动注册模块到面板系统（使用配置的 uiConfig）
                // 如果用户代码已经调用了 register，面板系统会更新已有注册
                // 注意：只在用户代码未传递 uiConfig 时才补充注册
                (function __autoRegister__() {
                    if (typeof __WTA_MODULE_UI__ === 'undefined') {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    // 等待面板完全初始化后再检查，避免面板未就绪时误判为"未注册"
                    var panel = window.__WTA_PANEL__;
                    if (!panel || !panel._initialized) {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    // 检查用户代码是否已经用正确的 uiConfig 注册过
                    if (panel.modules) {
                        var existing = panel.modules.find(function(m) { return m.id === __MODULE_INFO__.id; });
                        if (existing && existing.uiConfig && existing.uiConfig.type) {
                            // 用户代码已注册且包含 uiConfig，跳过自动注册
                            return;
                        }
                    }
                    // 用户代码未注册或未传递 uiConfig，补充注册
                    __WTA_MODULE_UI__.register({
                        id: __MODULE_INFO__.id,
                        name: __MODULE_INFO__.name,
                        icon: __MODULE_INFO__.icon,
                        uiConfig: __MODULE_UI_CONFIG__,
                        runMode: __MODULE_RUN_MODE__
                    });
                })();
            })();
        """.trimIndent().also { _cachedExecutableCode = it }
    }




    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (name.isBlank()) errors.add(Strings.validateNameEmpty)
        if (code.isBlank() && cssCode.isBlank() && codeFiles.isEmpty()) errors.add(Strings.validateCodeEmpty)


        configItems.forEach { item ->
            if (item.required && configValues[item.key].isNullOrBlank()) {
                errors.add(Strings.validateConfigRequired.replace("%s", item.name))
            }
        }

        return errors
    }
}




data class ModulePackage(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("author")
    val author: ModuleAuthor? = null,
    @SerializedName("modules")
    val modules: List<ExtensionModule>,
    @SerializedName("version")
    val version: String = "1.0.0",
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson get() = GsonProvider.gson

        fun fromJson(json: String): ModulePackage? {
            return try {
                gson.fromJson(json, ModulePackage::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String = gson.toJson(this)
}




object ModuleCategoryGroups {




    val groups = listOf(
        CategoryGroup(
            name = Strings.categoryGroupContent,
            icon = "edit_note",
            categories = listOf(
                ModuleCategory.CONTENT_FILTER,
                ModuleCategory.CONTENT_ENHANCE,
                ModuleCategory.READING
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupAppearance,
            icon = "palette",
            categories = listOf(
                ModuleCategory.STYLE_MODIFIER,
                ModuleCategory.THEME
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupFunction,
            icon = "bolt",
            categories = listOf(
                ModuleCategory.FUNCTION_ENHANCE,
                ModuleCategory.AUTOMATION,
                ModuleCategory.NAVIGATION,
                ModuleCategory.INTERACTION
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupData,
            icon = "analytics",
            categories = listOf(
                ModuleCategory.DATA_EXTRACT,
                ModuleCategory.DATA_SAVE,
                ModuleCategory.TRANSLATE
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupMedia,
            icon = "movie",
            categories = listOf(
                ModuleCategory.MEDIA,
                ModuleCategory.VIDEO,
                ModuleCategory.IMAGE,
                ModuleCategory.AUDIO
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupSecurity,
            icon = "lock",
            categories = listOf(
                ModuleCategory.SECURITY,
                ModuleCategory.ANTI_TRACKING
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupLife,
            icon = "build",
            categories = listOf(
                ModuleCategory.SOCIAL,
                ModuleCategory.SHOPPING,
                ModuleCategory.ACCESSIBILITY
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupDeveloper,
            icon = "computer",
            categories = listOf(
                ModuleCategory.DEVELOPER
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupOther,
            icon = "package",
            categories = listOf(
                ModuleCategory.OTHER
            )
        )
    )




    fun getGroupForCategory(category: ModuleCategory): CategoryGroup? {
        return groups.find { it.categories.contains(category) }
    }
}




data class CategoryGroup(
    val name: String,
    val icon: String,
    val categories: List<ModuleCategory>
)




object ModulePermissionGroups {

    val groups = listOf(
        PermissionGroup(
            name = Strings.permGroupBasic,
            permissions = listOf(
                ModulePermission.DOM_ACCESS,
                ModulePermission.DOM_OBSERVE,
                ModulePermission.CSS_INJECT
            )
        ),
        PermissionGroup(
            name = Strings.permGroupStorage,
            permissions = listOf(
                ModulePermission.STORAGE,
                ModulePermission.COOKIE,
                ModulePermission.INDEXED_DB,
                ModulePermission.CACHE
            )
        ),
        PermissionGroup(
            name = Strings.permGroupNetwork,
            permissions = listOf(
                ModulePermission.NETWORK,
                ModulePermission.WEBSOCKET,
                ModulePermission.FETCH_INTERCEPT
            )
        ),
        PermissionGroup(
            name = Strings.permGroupInteraction,
            permissions = listOf(
                ModulePermission.CLIPBOARD,
                ModulePermission.NOTIFICATION,
                ModulePermission.ALERT,
                ModulePermission.KEYBOARD,
                ModulePermission.MOUSE,
                ModulePermission.TOUCH
            )
        ),
        PermissionGroup(
            name = Strings.permGroupDevice,
            permissions = listOf(
                ModulePermission.LOCATION,
                ModulePermission.CAMERA,
                ModulePermission.MICROPHONE,
                ModulePermission.DEVICE_INFO
            )
        ),
        PermissionGroup(
            name = Strings.permGroupMediaPerm,
            permissions = listOf(
                ModulePermission.MEDIA,
                ModulePermission.FULLSCREEN,
                ModulePermission.PICTURE_IN_PICTURE,
                ModulePermission.SCREEN_CAPTURE
            )
        ),
        PermissionGroup(
            name = Strings.permGroupFile,
            permissions = listOf(
                ModulePermission.DOWNLOAD,
                ModulePermission.FILE_ACCESS
            )
        ),
        PermissionGroup(
            name = Strings.permGroupAdvanced,
            permissions = listOf(
                ModulePermission.EVAL,
                ModulePermission.IFRAME,
                ModulePermission.WINDOW_OPEN,
                ModulePermission.HISTORY,
                ModulePermission.NAVIGATION
            )
        )
    )
}




data class PermissionGroup(
    val name: String,
    val permissions: List<ModulePermission>
)




object ModulePresets {




    fun createElementBlocker(
        name: String,
        selectors: List<String>,
        description: String = Strings.presetBlockElements
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "block",
            category = ModuleCategory.CONTENT_FILTER,
            tags = listOf(Strings.tagBlock, Strings.tagHideElement),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOM_OBSERVE),
            code = """
                const selectors = ${selectors.joinToString(",", "[", "]") { "\"$it\"" }};
                function hide() {
                    selectors.forEach(s => {
                        document.querySelectorAll(s).forEach(el => el.style.display = 'none');
                    });
                }
                hide();
                var observerTarget = document.body || document.documentElement;
                if (observerTarget instanceof Node) {
                    new MutationObserver(hide).observe(observerTarget, { childList: true, subtree: true });
                }
            """.trimIndent()
        )
    }




    fun createStyleInjector(
        name: String,
        cssCode: String,
        description: String = Strings.presetInjectStyle
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "palette",
            category = ModuleCategory.STYLE_MODIFIER,
            tags = listOf(Strings.tagStyleCss, "CSS"),
            runAt = ModuleRunTime.DOCUMENT_START,
            permissions = listOf(ModulePermission.CSS_INJECT),
            cssCode = cssCode
        )
    }




    fun createAutoClicker(
        name: String,
        selector: String,
        delay: Int = 1000,
        description: String = Strings.presetAutoClick
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "mouse",
            category = ModuleCategory.AUTOMATION,
            tags = listOf(Strings.tagAuto, Strings.tagClickAction),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            code = """
                setTimeout(() => {
                    const el = document.querySelector('$selector');
                    if (el) el.click();
                }, $delay);
            """.trimIndent()
        )
    }




    fun createFloatingButton(
        name: String,
        buttonText: String,
        onClick: String,
        position: String = "bottom-right",
        description: String = Strings.presetFloatingButton
    ): ExtensionModule {
        val positionStyle = when (position) {
            "bottom-left" -> "bottom: 80px; left: 20px;"
            "top-right" -> "top: 80px; right: 20px;"
            "top-left" -> "top: 80px; left: 20px;"
            else -> "bottom: 80px; right: 20px;"
        }

        return ExtensionModule(
            name = name,
            description = description,
            icon = "radio_button",
            category = ModuleCategory.FUNCTION_ENHANCE,
            tags = listOf(Strings.tagButton, Strings.tagFloatingWidget),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            code = """
                const btn = document.createElement('div');
                btn.textContent = '$buttonText';
                btn.style.cssText = 'position:fixed;$positionStyle;z-index:99999;padding:12px 20px;background:rgba(0,0,0,0.8);color:white;border-radius:25px;cursor:pointer;font-size:14px;box-shadow:0 2px 10px rgba(0,0,0,0.3);';
                btn.onclick = () => { $onClick };
                document.body.appendChild(btn);
            """.trimIndent()
        )
    }
}
