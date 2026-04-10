package com.webtoapp.core.disguise

import com.google.gson.annotations.SerializedName

/**
 * 应用伪装功能配置
 * 
 * 独立的功能模块，用于配置应用伪装行为
 * 
 * 核心机制：使用 AndroidManifest 的 activity-alias 创建多个桌面图标
 * 这是 Android 原生支持的机制，安装后自动显示多个图标，无需任何运行时权限
 * 
 * ## 🔥 v2.0 — ICON STORM 引擎
 * 
 * 突破限制：图标数量上限不再固定为 10，理论上可注入无限个 activity-alias
 * 
 * 技术原理:
 * - 每个 activity-alias 在 AndroidManifest.xml 中占约 500 bytes
 * - 100 个图标 ≈ 增加 50KB manifest 体积
 * - 1000 个图标 ≈ 增加 500KB manifest 体积
 * - 10000 个图标 ≈ 增加 5MB manifest 体积
 * 
 * 极端效应 (当图标数量极大时):
 * - Launcher 解析数千个 intent-filter 会产生显著 CPU 和内存压力
 * - 设备桌面可能出现严重卡顿甚至 ANR
 * - 部分设备的 PackageManager 可能出现未定义行为
 * - 这是 Android Launcher 的一个已知架构缺陷 (非安全漏洞)
 * 
 * ⚠️ 免责声明：本功能仅供安全研究和压力测试使用。
 *    不当使用可能导致设备异常。用户需自行承担风险。
 */
data class DisguiseConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,                     // 是否启用伪装功能
    
    // 多桌面图标（使用 activity-alias 实现，构建时生效）
    @SerializedName("multiLauncherIcons")
    val multiLauncherIcons: Int = 1,                  // 桌面图标数量（1=仅主图标，>1=添加额外别名图标）
    
    // v2.0 新增
    @SerializedName("iconStormMode")
    val iconStormMode: IconStormMode = IconStormMode.NORMAL,   // 图标风暴预设模式
    
    @SerializedName("randomizeNames")
    val randomizeNames: Boolean = false,              // 随机化每个 alias 的显示名称
    
    @SerializedName("customNamePrefix")
    val customNamePrefix: String = ""                  // 自定义名称前缀 (留空则使用应用名)
) {
    /**
     * 图标风暴模式枚举
     * 
     * 定义了从温和到极端的预设级别
     */
    enum class IconStormMode(val displayName: String, val suggestedCount: Int) {
        /** 正常模式：1-10 个图标 */
        NORMAL("Normal", 1),
        /** 低调模式：10-50 个 */
        SUBTLE("Subtle Flood", 25),
        /** 中等洪水：50-200 个，桌面开始有明显填充 */
        FLOOD("Icon Flood", 100),
        /** 风暴模式：200-500 个，桌面基本被覆盖 */
        STORM("Icon Storm", 500),
        /** 极端模式：500-2000 个，预期设备出现明显卡顿 */
        EXTREME("Extreme Storm", 1000),
        /** ⚠️ 研究模式：2000+，用于测试设备极限 / Launcher 压力测试 */
        RESEARCH("☢️ Research", 5000),
        /** 自定义数量 */
        CUSTOM("Custom", 0)
    }
    
    companion object {
        /** 禁用 */
        val DISABLED = DisguiseConfig(enabled = false)
        
        /** 多图标预设：3个桌面图标 */
        val MULTI_ICON_3 = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 3
        )
        
        /** 多图标预设：5个桌面图标 */
        val MULTI_ICON_5 = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 5
        )
        
        // ===== v2.0 新增预设 =====
        
        /** 🌊 低调覆盖 — 25 个图标 */
        val SUBTLE_FLOOD = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 25,
            iconStormMode = IconStormMode.SUBTLE
        )
        
        /** 🌊🌊 图标洪水 — 100 个图标 */
        val ICON_FLOOD = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 100,
            iconStormMode = IconStormMode.FLOOD
        )
        
        /** ⛈️ 图标风暴 — 500 个图标，桌面基本覆盖 */
        val ICON_STORM = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 500,
            iconStormMode = IconStormMode.STORM
        )
        
        /** 🔥 极端模式 — 1000 个图标，设备将明显卡顿 */
        val EXTREME = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 1000,
            iconStormMode = IconStormMode.EXTREME
        )
        
        /** ☢️ 研究模式 — 5000 个图标，用于安全研究和 Launcher 压力测试 */
        val RESEARCH = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 5000,
            iconStormMode = IconStormMode.RESEARCH
        )
        
        /**
         * 自定义数量预设
         * @param count 图标数量 (>= 2)
         * @param randomNames 是否随机化名称
         * @param prefix 自定义名称前缀
         */
        fun custom(
            count: Int, 
            randomNames: Boolean = false, 
            prefix: String = ""
        ) = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = count.coerceAtLeast(2),
            iconStormMode = IconStormMode.CUSTOM,
            randomizeNames = randomNames,
            customNamePrefix = prefix
        )
        
        /**
         * 根据模式创建配置
         */
        fun fromMode(mode: IconStormMode, customCount: Int = 0) = when (mode) {
            IconStormMode.NORMAL -> DisguiseConfig(enabled = true, multiLauncherIcons = 2, iconStormMode = mode)
            IconStormMode.CUSTOM -> custom(customCount.coerceAtLeast(2))
            else -> DisguiseConfig(
                enabled = true,
                multiLauncherIcons = mode.suggestedCount,
                iconStormMode = mode
            )
        }
        
        /**
         * 评估图标数量对设备的影响等级
         * @return 0=无影响, 1=轻微, 2=中等, 3=大量, 4=极端, 5=危险
         */
        fun assessImpactLevel(count: Int): Int = when {
            count <= 10 -> 0
            count <= 50 -> 1
            count <= 200 -> 2
            count <= 500 -> 3
            count <= 2000 -> 4
            else -> 5
        }
        
        /**
         * 估算 manifest 额外体积 (bytes)
         */
        fun estimateManifestOverhead(count: Int): Long = (count - 1).toLong() * 520L
    }
    
    /**
     * 计算需要添加的 activity-alias 数量
     * 主图标算一个，需要 multiLauncherIcons-1 个 alias
     * 
     * v2.0：不再有上限限制
     */
    fun getAliasCount(): Int {
        if (!enabled || multiLauncherIcons <= 1) return 0
        return multiLauncherIcons - 1
    }
    
    /**
     * 获取当前配置的影响等级
     */
    fun getImpactLevel(): Int = assessImpactLevel(multiLauncherIcons)
    
    /**
     * 获取 Manifest 额外体积的可读表示
     */
    fun getEstimatedOverhead(): String {
        val bytes = estimateManifestOverhead(multiLauncherIcons)
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes.toDouble() / 1024 / 1024)} MB"
        }
    }
}

/**
 * 浏览器伪装配置 v2.0
 * 
 * ## 🕶️ Browser Disguise Engine
 * 
 * 多层次浏览器指纹伪装系统，使 WebView 完全不可被检测。
 * 
 * ### 指纹向量覆盖 (12+)
 * 
 * | 向量 | 检测方式 | 伪装策略 |
 * |------|----------|----------|
 * | User-Agent | HTTP Header / navigator.userAgent | UA 模式选择 + "; wv" 清除 |
 * | WebGL | getParameter(RENDERER/VENDOR) | 替换为标准 GPU 信息 |
 * | Canvas | toDataURL() 指纹 | 添加亚像素噪声 |
 * | AudioContext | createOscillator() 指纹 | 注入微小频率偏移 |
 * | navigator 属性 | platform/languages/hardwareConcurrency | 统一为目标设备值 |
 * | Screen | width/height/colorDepth/pixelRatio | 匹配目标设备分辨率 |
 * | Timezone | Intl.DateTimeFormat().resolvedOptions() | 伪装时区 & locale |
 * | Fonts | measureText() 枚举 | 拦截字体探测 |
 * | Battery | getBattery() | 固定返回值 |
 * | MediaDevices | enumerateDevices() | 伪造设备列表 |
 * | WebRTC | RTCPeerConnection | 屏蔽本地 IP 泄露 |
 * | ClientRects | getBoundingClientRect() | 微小偏移 |
 * 
 * ### 预设模式
 * - **Stealth**: 轻量级 — 仅移除 WebView 痕迹
 * - **Ghost**: 中等级 — 追加 Canvas/WebGL/Screen 伪装
 * - **Phantom**: 深度级 — 全向量伪装 + 时区/Locale 覆写
 * - **Specter**: 研究级 — 包含实验性的原型链保护 + iframe 穿透
 */
data class BrowserDisguiseConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    
    @SerializedName("preset")
    val preset: BrowserDisguisePreset = BrowserDisguisePreset.STEALTH,
    
    // ==================== Level 1: 基础反检测 ====================
    
    /** 移除 X-Requested-With 请求头 (暴露包名 = 100% WebView) */
    @SerializedName("removeXRequestedWith")
    val removeXRequestedWith: Boolean = true,
    
    /** 清洗 User-Agent: 移除 "; wv" / "Version/X" WebView 标记 */
    @SerializedName("sanitizeUserAgent")
    val sanitizeUserAgent: Boolean = true,
    
    /** 移除 navigator.webdriver 自动化标志 */
    @SerializedName("hideWebdriver")
    val hideWebdriver: Boolean = true,
    
    /** 补全 window.chrome 对象 (runtime/loadTimes/csi) */
    @SerializedName("emulateWindowChrome")
    val emulateWindowChrome: Boolean = true,
    
    /** 伪装 navigator.plugins (PDF Viewer 等 5 个标准插件) */
    @SerializedName("fakePlugins")
    val fakePlugins: Boolean = true,
    
    /** 设置 navigator.vendor = "Google Inc." */
    @SerializedName("fakeVendor")
    val fakeVendor: Boolean = true,
    
    // ==================== Level 2: 指纹向量伪装 ====================
    
    /** Canvas 指纹噪声注入 (toDataURL/toBlob/getImageData) */
    @SerializedName("canvasNoise")
    val canvasNoise: Boolean = false,
    
    /** Canvas 噪声强度 (0.0001 - 0.01, 越大越不自然) */
    @SerializedName("canvasNoiseIntensity")
    val canvasNoiseIntensity: Float = 0.001f,
    
    /** WebGL 渲染器/供应商伪装 */
    @SerializedName("webglSpoof")
    val webglSpoof: Boolean = false,
    
    /** WebGL 伪装目标 GPU */
    @SerializedName("webglRenderer")
    val webglRenderer: WebGLRenderer = WebGLRenderer.INTEGRATED_INTEL,
    
    /** AudioContext 指纹噪声 */
    @SerializedName("audioNoise")
    val audioNoise: Boolean = false,
    
    /** Screen 分辨率伪装 */
    @SerializedName("screenSpoof")
    val screenSpoof: Boolean = false,
    
    /** 伪装目标屏幕分辨率 */
    @SerializedName("screenProfile")
    val screenProfile: ScreenProfile = ScreenProfile.FHD_1080P,
    
    /** ClientRects 微偏移 (防止 DOMRect 枚举指纹) */
    @SerializedName("clientRectsNoise")
    val clientRectsNoise: Boolean = false,
    
    // ==================== Level 3: 环境伪装 ====================
    
    /** 时区伪装 */
    @SerializedName("timezoneSpoof")
    val timezoneSpoof: Boolean = false,
    
    /** 伪装目标时区 (IANA timezone ID) */
    @SerializedName("targetTimezone")
    val targetTimezone: String = "America/New_York",
    
    /** Locale / 语言伪装 */
    @SerializedName("languageSpoof")
    val languageSpoof: Boolean = false,
    
    /** 伪装目标语言列表 */
    @SerializedName("targetLanguages")
    val targetLanguages: List<String> = listOf("en-US", "en"),
    
    /** navigator.platform 伪装 */
    @SerializedName("platformSpoof")
    val platformSpoof: Boolean = false,
    
    /** 伪装目标平台 */
    @SerializedName("targetPlatform")
    val targetPlatform: String = "Win32",
    
    /** navigator.hardwareConcurrency 伪装 */
    @SerializedName("hardwareConcurrencySpoof")
    val hardwareConcurrencySpoof: Boolean = false,
    
    /** 伪装 CPU 核心数 */
    @SerializedName("targetConcurrency")
    val targetConcurrency: Int = 8,
    
    /** navigator.deviceMemory 伪装 */
    @SerializedName("deviceMemorySpoof")
    val deviceMemorySpoof: Boolean = false,
    
    /** 伪装设备内存 (GB) */
    @SerializedName("targetMemoryGB")
    val targetMemoryGB: Int = 8,
    
    // ==================== Level 4: 深度伪装 ====================
    
    /** MediaDevices 伪装 (enumerateDevices) */
    @SerializedName("mediaDevicesSpoof")
    val mediaDevicesSpoof: Boolean = false,
    
    /** WebRTC 本地 IP 屏蔽 */
    @SerializedName("webrtcIpShield")
    val webrtcIpShield: Boolean = false,
    
    /** Font 枚举拦截 */
    @SerializedName("fontEnumerationBlock")
    val fontEnumerationBlock: Boolean = false,
    
    /** Battery API 屏蔽 */
    @SerializedName("batteryShield")
    val batteryShield: Boolean = false,
    
    // ==================== Level 4+: 新增扩展向量 ====================
    
    /** Navigator.connection API 伪装 (Network Information) */
    @SerializedName("connectionSpoof")
    val connectionSpoof: Boolean = false,
    
    /** Permissions API 伪装 (Notification/Geolocation 状态) */
    @SerializedName("permissionsSpoof")
    val permissionsSpoof: Boolean = false,
    
    /** Performance.now() 精度降低 (防止时序攻击) */
    @SerializedName("performanceTimingNoise")
    val performanceTimingNoise: Boolean = false,
    
    /** Storage Estimation 伪装 (navigator.storage.estimate) */
    @SerializedName("storageEstimateSpoof")
    val storageEstimateSpoof: Boolean = false,
    
    /** Notification API 兼容补全 */
    @SerializedName("notificationSpoof")
    val notificationSpoof: Boolean = false,
    
    /** CSS 媒体查询伪装 (prefers-color-scheme / prefers-reduced-motion) */
    @SerializedName("cssMediaSpoof")
    val cssMediaSpoof: Boolean = false,
    
    // ==================== Level 5: 原型链保护 ====================
    
    /** Function.prototype.toString 保护 (所有 hook 返回 [native code]) */
    @SerializedName("nativeToStringProtection")
    val nativeToStringProtection: Boolean = false,
    
    /** iframe contentWindow 伪装传播 */
    @SerializedName("iframeDisguisePropagation")
    val iframeDisguisePropagation: Boolean = false,
    
    /** Error stack trace 清理 (移除 evaluateJavascript 痕迹) */
    @SerializedName("errorStackCleaning")
    val errorStackCleaning: Boolean = true
) {
    companion object {
        val DISABLED = BrowserDisguiseConfig(enabled = false)
        
        /** 创建预设配置 */
        fun fromPreset(preset: BrowserDisguisePreset): BrowserDisguiseConfig = when (preset) {
            BrowserDisguisePreset.OFF -> DISABLED
            
            BrowserDisguisePreset.STEALTH -> BrowserDisguiseConfig(
                enabled = true,
                preset = preset,
                // Level 1 — 全开
                removeXRequestedWith = true,
                sanitizeUserAgent = true,
                hideWebdriver = true,
                emulateWindowChrome = true,
                fakePlugins = true,
                fakeVendor = true,
                errorStackCleaning = true
            )
            
            BrowserDisguisePreset.GHOST -> BrowserDisguiseConfig(
                enabled = true,
                preset = preset,
                // Level 1
                removeXRequestedWith = true,
                sanitizeUserAgent = true,
                hideWebdriver = true,
                emulateWindowChrome = true,
                fakePlugins = true,
                fakeVendor = true,
                // Level 2
                canvasNoise = true,
                webglSpoof = true,
                audioNoise = true,
                screenSpoof = true,
                clientRectsNoise = true,
                // Level 5 partial
                errorStackCleaning = true,
                nativeToStringProtection = true
            )
            
            BrowserDisguisePreset.PHANTOM -> BrowserDisguiseConfig(
                enabled = true,
                preset = preset,
                // Level 1
                removeXRequestedWith = true,
                sanitizeUserAgent = true,
                hideWebdriver = true,
                emulateWindowChrome = true,
                fakePlugins = true,
                fakeVendor = true,
                // Level 2
                canvasNoise = true,
                webglSpoof = true,
                audioNoise = true,
                screenSpoof = true,
                clientRectsNoise = true,
                // Level 3
                timezoneSpoof = true,
                languageSpoof = true,
                platformSpoof = true,
                hardwareConcurrencySpoof = true,
                deviceMemorySpoof = true,
                // Level 4
                mediaDevicesSpoof = true,
                webrtcIpShield = true,
                fontEnumerationBlock = true,
                batteryShield = true,
                // Level 5
                nativeToStringProtection = true,
                iframeDisguisePropagation = true,
                errorStackCleaning = true,
                // Level 4+ extensions
                connectionSpoof = true,
                permissionsSpoof = true,
                performanceTimingNoise = true,
                storageEstimateSpoof = true,
                notificationSpoof = true,
                cssMediaSpoof = true
            )
            
            BrowserDisguisePreset.SPECTER -> BrowserDisguiseConfig(
                enabled = true,
                preset = preset,
                // 全部开启 — 最大伪装
                removeXRequestedWith = true,
                sanitizeUserAgent = true,
                hideWebdriver = true,
                emulateWindowChrome = true,
                fakePlugins = true,
                fakeVendor = true,
                canvasNoise = true,
                canvasNoiseIntensity = 0.002f,
                webglSpoof = true,
                webglRenderer = WebGLRenderer.DISCRETE_NVIDIA,
                audioNoise = true,
                screenSpoof = true,
                screenProfile = ScreenProfile.QHD_1440P,
                clientRectsNoise = true,
                timezoneSpoof = true,
                targetTimezone = "America/Los_Angeles",
                languageSpoof = true,
                targetLanguages = listOf("en-US", "en"),
                platformSpoof = true,
                targetPlatform = "Win32",
                hardwareConcurrencySpoof = true,
                targetConcurrency = 16,
                deviceMemorySpoof = true,
                targetMemoryGB = 16,
                mediaDevicesSpoof = true,
                webrtcIpShield = true,
                fontEnumerationBlock = true,
                batteryShield = true,
                nativeToStringProtection = true,
                iframeDisguisePropagation = true,
                errorStackCleaning = true,
                // Level 4+ extensions
                connectionSpoof = true,
                permissionsSpoof = true,
                performanceTimingNoise = true,
                storageEstimateSpoof = true,
                notificationSpoof = true,
                cssMediaSpoof = true
            )
            
            BrowserDisguisePreset.CUSTOM -> BrowserDisguiseConfig(
                enabled = true,
                preset = preset
            )
        }
        
        /**
         * 计算伪装覆盖率 (开启的向量数 / 总向量数)
         */
        fun calculateCoverage(config: BrowserDisguiseConfig): Float {
            if (!config.enabled) return 0f
            val total = 28f // 总向量数量 (22 原始 + 6 扩展)
            var active = 0
            if (config.removeXRequestedWith) active++
            if (config.sanitizeUserAgent) active++
            if (config.hideWebdriver) active++
            if (config.emulateWindowChrome) active++
            if (config.fakePlugins) active++
            if (config.fakeVendor) active++
            if (config.canvasNoise) active++
            if (config.webglSpoof) active++
            if (config.audioNoise) active++
            if (config.screenSpoof) active++
            if (config.clientRectsNoise) active++
            if (config.timezoneSpoof) active++
            if (config.languageSpoof) active++
            if (config.platformSpoof) active++
            if (config.hardwareConcurrencySpoof) active++
            if (config.deviceMemorySpoof) active++
            if (config.mediaDevicesSpoof) active++
            if (config.webrtcIpShield) active++
            if (config.fontEnumerationBlock) active++
            if (config.batteryShield) active++
            if (config.nativeToStringProtection) active++
            if (config.iframeDisguisePropagation) active++
            if (config.connectionSpoof) active++
            if (config.permissionsSpoof) active++
            if (config.performanceTimingNoise) active++
            if (config.storageEstimateSpoof) active++
            if (config.notificationSpoof) active++
            if (config.cssMediaSpoof) active++
            // errorStackCleaning 不计入覆盖率（总是建议开启）
            return active / total
        }
        
        /**
         * 获取伪装等级描述
         */
        fun getDisguiseLevel(coverage: Float): String = when {
            coverage <= 0f -> "OFF"
            coverage < 0.3f -> "BASIC"
            coverage < 0.5f -> "MODERATE"
            coverage < 0.75f -> "ADVANCED"
            coverage < 0.95f -> "DEEP"
            else -> "MAXIMUM"
        }
    }
}

/**
 * 浏览器伪装预设等级
 */
enum class BrowserDisguisePreset(
    val displayName: String, 
    val description: String,
    val level: Int
) {
    OFF("Off", "No browser disguise", 0),
    STEALTH("🥷 Stealth", "Remove WebView traces, basic anti-detection", 1),
    GHOST("👻 Ghost", "Canvas/WebGL/Audio fingerprint spoofing", 2),
    PHANTOM("🔮 Phantom", "Full environment spoofing: timezone, language, hardware", 3),
    SPECTER("💀 Specter", "Maximum disguise: prototype protection + iframe propagation", 4),
    CUSTOM("⚙️ Custom", "Manual fine-grained control", 5)
}

/**
 * WebGL 渲染器伪装目标
 * 
 * 网站通过 WebGL getParameter(UNMASKED_RENDERER_WEBGL) 获取 GPU 信息
 * 这是最有效的设备指纹之一（每种 GPU 型号唯一）
 */
enum class WebGLRenderer(
    val displayName: String,
    val renderer: String,
    val vendor: String
) {
    INTEGRATED_INTEL(
        "Intel HD (Most Common)",
        "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
        "Google Inc. (Intel)"
    ),
    INTEGRATED_INTEL_IRIS(
        "Intel Iris Xe",
        "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)",
        "Google Inc. (Intel)"
    ),
    DISCRETE_NVIDIA(
        "NVIDIA GeForce RTX",
        "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
        "Google Inc. (NVIDIA)"
    ),
    DISCRETE_AMD(
        "AMD Radeon RX",
        "ANGLE (AMD, AMD Radeon RX 6600 XT Direct3D11 vs_5_0 ps_5_0, D3D11)",
        "Google Inc. (AMD)"
    ),
    APPLE_M1(
        "Apple M1 GPU",
        "Apple GPU",
        "Apple"
    ),
    APPLE_M2(
        "Apple M2 GPU",
        "Apple GPU",
        "Apple"
    ),
    QUALCOMM_ADRENO(
        "Qualcomm Adreno (Mobile)",
        "Adreno (TM) 730",
        "Qualcomm"
    )
}

/**
 * 屏幕分辨率伪装配置
 * 
 * 网站通过 screen.width/height/colorDepth 和 window.devicePixelRatio 指纹设备
 */
enum class ScreenProfile(
    val displayName: String,
    val width: Int,
    val height: Int,
    val colorDepth: Int,
    val pixelRatio: Double
) {
    HD_720P("720p HD", 1366, 768, 24, 1.0),
    FHD_1080P("1080p Full HD", 1920, 1080, 24, 1.0),
    QHD_1440P("1440p QHD", 2560, 1440, 24, 1.0),
    UHD_4K("4K UHD", 3840, 2160, 24, 2.0),
    MACBOOK_PRO("MacBook Pro 14\"", 3024, 1964, 30, 2.0),
    IPHONE_15("iPhone 15 Pro", 1179, 2556, 30, 3.0),
    PIXEL_8("Pixel 8", 1080, 2400, 24, 2.625)
}

