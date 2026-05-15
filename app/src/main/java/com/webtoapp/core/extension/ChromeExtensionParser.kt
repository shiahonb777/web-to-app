package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import org.json.JSONObject
import java.io.File
import java.util.UUID









object ChromeExtensionParser {

    private const val TAG = "ChromeExtensionParser"




    private val PERMISSION_MAP = mapOf(
        "activeTab" to ModulePermission.DOM_ACCESS,
        "tabs" to ModulePermission.DOM_ACCESS,
        "storage" to ModulePermission.STORAGE,
        "notifications" to ModulePermission.NOTIFICATION,
        "clipboardRead" to ModulePermission.CLIPBOARD,
        "clipboardWrite" to ModulePermission.CLIPBOARD,
        "cookies" to ModulePermission.COOKIE,
        "webRequest" to ModulePermission.NETWORK,
        "webNavigation" to ModulePermission.NAVIGATION,
        "history" to ModulePermission.HISTORY,
        "downloads" to ModulePermission.DOWNLOAD,
        "geolocation" to ModulePermission.LOCATION,
        "alarms" to ModulePermission.STORAGE,
        "contextMenus" to ModulePermission.DOM_ACCESS
    )




    private val UNSUPPORTED_PERMISSIONS = setOf(
        "nativeMessaging", "debugger", "proxy",
        "webRequestBlocking", "management",
        "devtools", "bookmarks", "topSites",
        "identity", "tts", "ttsEngine",
        "tabCapture", "desktopCapture", "pageCapture",
        "browsingData", "fontSettings", "privacy"
    )




    data class ParseResult(
        val extensionName: String,
        val extensionVersion: String,
        val extensionDescription: String,
        val modules: List<ExtensionModule>,
        val isValid: Boolean,
        val warnings: List<String> = emptyList(),
        val supportedPermissions: List<String> = emptyList(),
        val unsupportedPermissions: List<String> = emptyList(),
        val mappedPermissions: List<ModulePermission> = emptyList()
    )

    data class StaticRuleResource(
        val id: String,
        val enabled: Boolean,
        val path: String
    )








    fun parseFromDirectory(extensionDir: File, overrideExtensionId: String? = null): ParseResult {
        val warnings = mutableListOf<String>()

        val manifestFile = extensionDir.resolve("manifest.json")
        if (!manifestFile.exists()) {
            return ParseResult(
                extensionName = extensionDir.name,
                extensionVersion = "",
                extensionDescription = "",
                modules = emptyList(),
                isValid = false,
                warnings = listOf("manifest.json not found in extension directory")
            )
        }

        return try {
            val manifestJson = manifestFile.readText()
            val manifest = JSONObject(manifestJson)

            val name = manifest.optString("name", extensionDir.name)
            val version = manifest.optString("version", "1.0")
            val description = manifest.optString("description", "")
            val manifestVersion = manifest.optInt("manifest_version", 2)

            AppLogger.d(TAG, "Parsing extension: $name v$version (manifest v$manifestVersion)")

            val popupPath = extractPopupPath(manifest)
            val optionsPagePath = extractOptionsPagePath(manifest)
            val backgroundScript = extractBackgroundScript(manifest)
            val staticRuleResources = extractStaticRuleResources(manifest)


            val contentScripts = manifest.optJSONArray("content_scripts")
            if (contentScripts == null || contentScripts.length() == 0) {
                val extensionId = overrideExtensionId?.takeIf { it.isNotBlank() }
                    ?: extensionDir.name.takeIf { it.isNotBlank() && it != "." }
                    ?: UUID.randomUUID().toString().take(8)
                val syntheticModules = mutableListOf<ExtensionModule>()
                val hasExtensionUi = popupPath.isNotBlank() || optionsPagePath.isNotBlank()
                val hasBackgroundRuntime = backgroundScript.isNotBlank() || staticRuleResources.isNotEmpty()

                if (hasExtensionUi || hasBackgroundRuntime) {
                    syntheticModules += ExtensionModule(
                        id = "${extensionId}_ui_0",
                        name = name,
                        description = description,
                        icon = "extension",
                        category = ModuleCategory.FUNCTION_ENHANCE,
                        version = ModuleVersion(name = version),
                        code = "",
                        cssCode = "",
                        runAt = ModuleRunTime.DOCUMENT_IDLE,
                        urlMatches = listOf(UrlMatchRule(pattern = "*")),
                        enabled = true,
                        sourceType = ModuleSourceType.CHROME_EXTENSION,
                        chromeExtId = extensionId,
                        world = "ISOLATED",
                        backgroundScript = backgroundScript,
                        popupPath = popupPath,
                        optionsPagePath = optionsPagePath,
                        manifestJson = manifestJson,
                        noframes = false
                    )
                    warnings.add(buildSyntheticImportWarning(hasExtensionUi, hasBackgroundRuntime))
                } else {
                    warnings.add(
                        "This extension has no content_scripts, popup, options page, background runtime, or declarative net request rules. Only Chrome extensions with page scripts, extension UI, or runtime/background capabilities are currently supported."
                    )
                }
                return buildParseResult(
                    extensionName = name,
                    extensionVersion = version,
                    extensionDescription = description,
                    manifest = manifest,
                    modules = syntheticModules,
                    warnings = warnings
                )
            }

            val modules = mutableListOf<ExtensionModule>()



            val extensionId = overrideExtensionId?.takeIf { it.isNotBlank() }
                ?: extensionDir.name.takeIf { it.isNotBlank() && it != "." }
                ?: UUID.randomUUID().toString().take(8)


            for (i in 0 until contentScripts.length()) {
                val cs = contentScripts.getJSONObject(i)


                val matches = mutableListOf<String>()
                cs.optJSONArray("matches")?.let { arr ->
                    for (j in 0 until arr.length()) matches.add(arr.getString(j))
                }

                val excludeMatches = mutableListOf<String>()
                cs.optJSONArray("exclude_matches")?.let { arr ->
                    for (j in 0 until arr.length()) excludeMatches.add(arr.getString(j))
                }


                val jsFiles = mutableListOf<String>()
                cs.optJSONArray("js")?.let { arr ->
                    for (j in 0 until arr.length()) jsFiles.add(arr.getString(j))
                }


                val cssFiles = mutableListOf<String>()
                cs.optJSONArray("css")?.let { arr ->
                    for (j in 0 until arr.length()) cssFiles.add(arr.getString(j))
                }


                val runAt = when (cs.optString("run_at", "document_idle")) {
                    "document_start" -> ModuleRunTime.DOCUMENT_START
                    "document_end" -> ModuleRunTime.DOCUMENT_END
                    "document_idle" -> ModuleRunTime.DOCUMENT_IDLE
                    else -> ModuleRunTime.DOCUMENT_IDLE
                }


                val allFrames = cs.optBoolean("all_frames", false)


                val world = cs.optString("world", "ISOLATED").uppercase().let {
                    if (it == "MAIN") "MAIN" else "ISOLATED"
                }


                val jsCode = StringBuilder()
                jsFiles.forEach { jsPath ->
                    val jsFile = extensionDir.resolve(jsPath)
                    if (jsFile.exists()) {
                        jsCode.appendLine("// === $jsPath ===")
                        jsCode.appendLine(jsFile.readText())
                        jsCode.appendLine()
                    } else {
                        warnings.add("JS file not found: $jsPath")
                    }
                }


                val cssCode = StringBuilder()
                cssFiles.forEach { cssPath ->
                    val cssFile = extensionDir.resolve(cssPath)
                    if (cssFile.exists()) {
                        cssCode.appendLine("/* === $cssPath === */")
                        cssCode.appendLine(cssFile.readText())
                        cssCode.appendLine()
                    } else {
                        warnings.add("CSS file not found: $cssPath")
                    }
                }


                if (jsCode.isBlank() && cssCode.isBlank()) {
                    warnings.add("content_scripts[$i] has no JS or CSS content, skipped")
                    continue
                }


                val urlMatchRules = mutableListOf<UrlMatchRule>()
                matches.forEach { pattern ->
                    urlMatchRules.add(UrlMatchRule(
                        pattern = convertChromeMatchPattern(pattern),
                        isRegex = false,
                        exclude = false
                    ))
                }
                excludeMatches.forEach { pattern ->
                    urlMatchRules.add(UrlMatchRule(
                        pattern = convertChromeMatchPattern(pattern),
                        isRegex = false,
                        exclude = true
                    ))
                }


                val moduleName = if (contentScripts.length() == 1) {
                    name
                } else {
                    "$name [${i + 1}]"
                }

                modules.add(ExtensionModule(
                    id = "${extensionId}_cs_$i",
                    name = moduleName,
                    description = description,
                    icon = "extension",
                    category = ModuleCategory.FUNCTION_ENHANCE,
                    version = ModuleVersion(name = version),
                    code = jsCode.toString(),
                    cssCode = cssCode.toString(),
                    runAt = runAt,
                    urlMatches = urlMatchRules,
                    enabled = true,
                    sourceType = ModuleSourceType.CHROME_EXTENSION,
                    chromeExtId = extensionId,
                    world = world,
                    backgroundScript = if (i == 0) backgroundScript else "",
                    popupPath = if (i == 0) popupPath else "",
                    optionsPagePath = if (i == 0) optionsPagePath else "",
                    manifestJson = manifestJson,
                    noframes = !allFrames
                ))
            }


            buildParseResult(
                extensionName = name,
                extensionVersion = version,
                extensionDescription = description,
                manifest = manifest,
                modules = modules,
                warnings = warnings
            ).also {
                AppLogger.i(
                    TAG,
                    "Parsed Chrome extension: name='$name', content_scripts=${it.modules.size}, warnings=${warnings.size}"
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse Chrome extension", e)
            ParseResult(
                extensionName = extensionDir.name,
                extensionVersion = "",
                extensionDescription = "",
                modules = emptyList(),
                isValid = false,
                warnings = listOf("Parse error: ${e.message}")
            )
        }
    }

    private fun extractPopupPath(manifest: JSONObject): String {
        return manifest.optJSONObject("action")?.optString("default_popup", "")?.takeIf { it.isNotBlank() }
            ?: manifest.optJSONObject("browser_action")?.optString("default_popup", "")?.takeIf { it.isNotBlank() }
            ?: manifest.optJSONObject("page_action")?.optString("default_popup", "")?.takeIf { it.isNotBlank() }
            ?: ""
    }

    private fun extractOptionsPagePath(manifest: JSONObject): String {
        return manifest.optString("options_page", "").takeIf { it.isNotBlank() }
            ?: manifest.optJSONObject("options_ui")?.optString("page", "")?.takeIf { it.isNotBlank() }
            ?: ""
    }

    private fun extractBackgroundScript(manifest: JSONObject): String {
        return try {
            val bg = manifest.optJSONObject("background")
            bg?.optString("service_worker", "")?.takeIf { it.isNotBlank() }
                ?: bg?.optJSONArray("scripts")?.let { arr ->
                    if (arr.length() > 0) arr.getString(0) else null
                }
                ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractStaticRuleResources(manifest: JSONObject): List<StaticRuleResource> {
        val ruleResources = manifest.optJSONObject("declarative_net_request")
            ?.optJSONArray("rule_resources")
            ?: return emptyList()
        val resources = mutableListOf<StaticRuleResource>()
        for (i in 0 until ruleResources.length()) {
            val resource = ruleResources.optJSONObject(i) ?: continue
            val path = resource.optString("path", "").trim()
            val id = resource.optString("id", "").trim()
            if (path.isBlank()) continue
            resources += StaticRuleResource(
                id = id,
                enabled = resource.optBoolean("enabled", true),
                path = path
            )
        }
        return resources
    }

    private fun buildSyntheticImportWarning(
        hasExtensionUi: Boolean,
        hasBackgroundRuntime: Boolean
    ): String {
        return when {
            hasExtensionUi && hasBackgroundRuntime ->
                "This extension has no content_scripts. Imported using extension UI and background/runtime capabilities."
            hasExtensionUi ->
                "This extension has no content_scripts. Imported as popup/options UI only."
            hasBackgroundRuntime ->
                "This extension has no content_scripts. Imported as background/declarativeNetRequest runtime only."
            else ->
                "This extension has no content_scripts."
        }
    }

    private fun collectRawPermissions(manifest: JSONObject): List<String> {
        val rawPermissions = mutableListOf<String>()
        manifest.optJSONArray("permissions")?.let { arr ->
            for (j in 0 until arr.length()) rawPermissions.add(arr.getString(j))
        }
        manifest.optJSONArray("host_permissions")?.let { arr ->
            for (j in 0 until arr.length()) rawPermissions.add(arr.getString(j))
        }
        manifest.optJSONArray("optional_permissions")?.let { arr ->
            for (j in 0 until arr.length()) rawPermissions.add(arr.getString(j))
        }
        return rawPermissions
    }

    private fun buildParseResult(
        extensionName: String,
        extensionVersion: String,
        extensionDescription: String,
        manifest: JSONObject,
        modules: List<ExtensionModule>,
        warnings: MutableList<String>
    ): ParseResult {
        val rawPermissions = collectRawPermissions(manifest)
        val apiPermissions = rawPermissions.filter { !it.contains("://") && it != "<all_urls>" }
        val mapped = apiPermissions.mapNotNull { PERMISSION_MAP[it] }.distinct()
        val supported = apiPermissions.filter { it in PERMISSION_MAP }
        val unsupported = apiPermissions.filter { it in UNSUPPORTED_PERMISSIONS }

        if (unsupported.isNotEmpty()) {
            warnings.add("Unsupported permissions: ${unsupported.joinToString()}")
        }

        AppLogger.d(
            TAG,
            "Extension permissions: raw=$rawPermissions, supported=$supported, unsupported=$unsupported"
        )

        val modulesWithPermissions = modules.map { module ->
            module.copy(permissions = (module.permissions + mapped).distinct())
        }

        return ParseResult(
            extensionName = extensionName,
            extensionVersion = extensionVersion,
            extensionDescription = extensionDescription,
            modules = modulesWithPermissions,
            isValid = true,
            warnings = warnings,
            supportedPermissions = supported,
            unsupportedPermissions = unsupported,
            mappedPermissions = mapped
        )
    }






    private fun convertChromeMatchPattern(pattern: String): String {
        if (pattern == "<all_urls>") return "*"

        return pattern
    }





    fun isCrxFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { stream ->
                val magic = ByteArray(4)
                stream.read(magic)

                magic[0] == 'C'.code.toByte() &&
                magic[1] == 'r'.code.toByte() &&
                magic[2] == '2'.code.toByte() &&
                magic[3] == '4'.code.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }









    fun getCrxZipOffset(file: File): Long {
        return try {
            file.inputStream().use { stream ->
                val buf = ByteArray(16)
                stream.read(buf)

                val version = readLittleEndianInt(buf, 4)

                when (version) {
                    3 -> {

                        val headerSize = readLittleEndianInt(buf, 8)
                        12L + headerSize
                    }
                    2 -> {

                        val pkLen = readLittleEndianInt(buf, 8)
                        val sigLen = readLittleEndianInt(buf, 12)
                        16L + pkLen + sigLen
                    }
                    else -> {
                        AppLogger.w(TAG, "Unknown CRX version: $version, trying as raw ZIP")
                        0L
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read CRX header", e)
            0L
        }
    }

    private fun readLittleEndianInt(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toInt() and 0xFF) or
            ((buf[offset + 1].toInt() and 0xFF) shl 8) or
            ((buf[offset + 2].toInt() and 0xFF) shl 16) or
            ((buf[offset + 3].toInt() and 0xFF) shl 24)
    }
}
