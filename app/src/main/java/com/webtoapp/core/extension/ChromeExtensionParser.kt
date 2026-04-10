package com.webtoapp.core.extension

import com.webtoapp.core.logging.AppLogger
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Chrome 浏览器扩展解析器
 * 
 * 解析 manifest.json 中的 content_scripts 配置，
 * 将 Chrome 扩展转换为一组 ExtensionModule。
 * 
 * 支持 Manifest V2 和 V3 格式。
 */
object ChromeExtensionParser {
    
    private const val TAG = "ChromeExtensionParser"
    
    /**
     * Chrome permission → ModulePermission mapping
     */
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
    
    /**
     * Permissions that are completely unsupported in WebView context
     */
    private val UNSUPPORTED_PERMISSIONS = setOf(
        "nativeMessaging", "debugger", "proxy",
        "webRequestBlocking", "management",
        "devtools", "bookmarks", "topSites",
        "identity", "tts", "ttsEngine",
        "tabCapture", "desktopCapture", "pageCapture",
        "browsingData", "fontSettings", "privacy"
    )
    
    /**
     * 解析结果
     */
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
    
    /**
     * 从已解压的扩展目录解析
     * @param extensionDir 解压后的扩展根目录
     * @param overrideExtensionId 可选：覆盖扩展 ID（当调用者知道正确的 ID 时使用，
     *        例如 ExtensionFileManager 传入 UUID 目录名以确保与文件系统路径一致）
     * @return 解析结果
     */
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
            
            // 解析 content_scripts
            val contentScripts = manifest.optJSONArray("content_scripts")
            if (contentScripts == null || contentScripts.length() == 0) {
                warnings.add("No content_scripts found in manifest.json")
                return ParseResult(
                    extensionName = name,
                    extensionVersion = version,
                    extensionDescription = description,
                    modules = emptyList(),
                    isValid = true,
                    warnings = warnings
                )
            }
            
            val modules = mutableListOf<ExtensionModule>()
            // Use overrideExtensionId if provided, otherwise derive from directory name
            // overrideExtensionId ensures consistency with the filesystem path used by
            // ExtensionResourceInterceptor for resource loading
            val extensionId = overrideExtensionId?.takeIf { it.isNotBlank() }
                ?: extensionDir.name.takeIf { it.isNotBlank() && it != "." }
                ?: UUID.randomUUID().toString().take(8)
            
            // Parse background script path (MV3: service_worker, MV2: scripts array)
            val backgroundScript = try {
                val bg = manifest.optJSONObject("background")
                bg?.optString("service_worker", "")?.takeIf { it.isNotBlank() }
                    ?: bg?.optJSONArray("scripts")?.let { arr ->
                        if (arr.length() > 0) arr.getString(0) else null
                    }
                    ?: ""
            } catch (_: Exception) { "" }
            
            for (i in 0 until contentScripts.length()) {
                val cs = contentScripts.getJSONObject(i)
                
                // 匹配 URL 规则
                val matches = mutableListOf<String>()
                cs.optJSONArray("matches")?.let { arr ->
                    for (j in 0 until arr.length()) matches.add(arr.getString(j))
                }
                
                val excludeMatches = mutableListOf<String>()
                cs.optJSONArray("exclude_matches")?.let { arr ->
                    for (j in 0 until arr.length()) excludeMatches.add(arr.getString(j))
                }
                
                // JS 文件
                val jsFiles = mutableListOf<String>()
                cs.optJSONArray("js")?.let { arr ->
                    for (j in 0 until arr.length()) jsFiles.add(arr.getString(j))
                }
                
                // CSS 文件
                val cssFiles = mutableListOf<String>()
                cs.optJSONArray("css")?.let { arr ->
                    for (j in 0 until arr.length()) cssFiles.add(arr.getString(j))
                }
                
                // 运行时机
                val runAt = when (cs.optString("run_at", "document_idle")) {
                    "document_start" -> ModuleRunTime.DOCUMENT_START
                    "document_end" -> ModuleRunTime.DOCUMENT_END
                    "document_idle" -> ModuleRunTime.DOCUMENT_IDLE
                    else -> ModuleRunTime.DOCUMENT_IDLE
                }
                
                // all_frames
                val allFrames = cs.optBoolean("all_frames", false)
                
                // world (MV3: "MAIN" or "ISOLATED", default "ISOLATED")
                val world = cs.optString("world", "ISOLATED").uppercase().let {
                    if (it == "MAIN") "MAIN" else "ISOLATED"
                }
                
                // 读取 JS 文件内容
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
                
                // 读取 CSS 文件内容
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
                
                // 跳过空的 content_script
                if (jsCode.isBlank() && cssCode.isBlank()) {
                    warnings.add("content_scripts[$i] has no JS or CSS content, skipped")
                    continue
                }
                
                // 构建 URL 匹配规则
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
                
                // 模块名称
                val moduleName = if (contentScripts.length() == 1) {
                    name
                } else {
                    "$name [${i + 1}]"
                }
                
                modules.add(ExtensionModule(
                    id = "${extensionId}_cs_$i",
                    name = moduleName,
                    description = description,
                    icon = "extension", // Chrome 扩展图标
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
                    noframes = !allFrames
                ))
            }
            
            // 解析 permissions 并分类
            val rawPermissions = mutableListOf<String>()
            manifest.optJSONArray("permissions")?.let { arr ->
                for (j in 0 until arr.length()) rawPermissions.add(arr.getString(j))
            }
            // Manifest V3: host_permissions 单独列出
            manifest.optJSONArray("host_permissions")?.let { arr ->
                for (j in 0 until arr.length()) rawPermissions.add(arr.getString(j))
            }
            // Manifest V3: optional_permissions
            manifest.optJSONArray("optional_permissions")?.let { arr ->
                for (j in 0 until arr.length()) rawPermissions.add(arr.getString(j))
            }
            
            // Filter out URL-pattern permissions (e.g. "http://*/", "<all_urls>")
            val apiPermissions = rawPermissions.filter { !it.contains("://") && it != "<all_urls>" }
            
            // Map to ModulePermission
            val mapped = apiPermissions.mapNotNull { PERMISSION_MAP[it] }.distinct()
            val supported = apiPermissions.filter { it in PERMISSION_MAP }
            val unsupported = apiPermissions.filter { it in UNSUPPORTED_PERMISSIONS }
            
            if (unsupported.isNotEmpty()) {
                warnings.add("Unsupported permissions: ${unsupported.joinToString()}")
            }
            
            AppLogger.d(TAG, "Extension permissions: raw=$rawPermissions, supported=$supported, unsupported=$unsupported")
            
            // Apply mapped permissions to all modules
            val modulesWithPermissions = modules.map { module ->
                module.copy(permissions = (module.permissions + mapped).distinct())
            }
            
            AppLogger.i(TAG, "Parsed Chrome extension: name='$name', " +
                "content_scripts=${modulesWithPermissions.size}, warnings=${warnings.size}")
            
            ParseResult(
                extensionName = name,
                extensionVersion = version,
                extensionDescription = description,
                modules = modulesWithPermissions,
                isValid = true,
                warnings = warnings,
                supportedPermissions = supported,
                unsupportedPermissions = unsupported,
                mappedPermissions = mapped
            )
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
    
    /**
     * 转换 Chrome match pattern 为通配符格式
     * Chrome match pattern: scheme://host/path
     * 特殊值: <all_urls>
     */
    private fun convertChromeMatchPattern(pattern: String): String {
        if (pattern == "<all_urls>") return "*"
        // Chrome match pattern 格式本身兼容通配符匹配
        return pattern
    }
    
    /**
     * 检测文件是否为 Chrome 扩展 (.crx)
     * CRX 文件头: "Cr24" magic bytes
     */
    fun isCrxFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { stream ->
                val magic = ByteArray(4)
                stream.read(magic)
                // CRX2: "Cr24", CRX3 also starts with "Cr24"
                magic[0] == 'C'.code.toByte() &&
                magic[1] == 'r'.code.toByte() &&
                magic[2] == '2'.code.toByte() &&
                magic[3] == '4'.code.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取 CRX 文件中 ZIP 数据的偏移量
     * 
     * CRX3 格式:
     *   [4 bytes magic] [4 bytes version] [4 bytes header_size] [header] [zip data]
     * CRX2 格式:
     *   [4 bytes magic] [4 bytes version] [4 bytes pk_len] [4 bytes sig_len] [pk] [sig] [zip data]
     */
    fun getCrxZipOffset(file: File): Long {
        return try {
            file.inputStream().use { stream ->
                val buf = ByteArray(16)
                stream.read(buf)
                
                val version = readLittleEndianInt(buf, 4)
                
                when (version) {
                    3 -> {
                        // CRX3: header size at offset 8
                        val headerSize = readLittleEndianInt(buf, 8)
                        12L + headerSize
                    }
                    2 -> {
                        // CRX2: public key length at offset 8, signature length at offset 12
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
