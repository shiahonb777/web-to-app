package com.webtoapp.core.isolation

import kotlin.random.Random

/**
 * 浏览器指纹生成器 — 生成完整一致的浏览器身份
 *
 * 关键设计原则：
 * 1. **一致性** — UA、platform、vendor、Client Hints、WebGL 必须互相匹配
 *    (如 UA 说 Mac + Chrome，则 platform=MacIntel, vendor=Google Inc., WebGL 用 Apple GPU)
 * 2. **现代化** — 使用 2024-2025 年真实浏览器版本号
 * 3. **确定性** — 同一 seed 始终生成完全相同的指纹（会话内一致）
 * 4. **真实性** — 每个组合都对应真实存在的浏览器/系统/硬件配置
 */
object FingerprintGenerator {

    // ==================== Consistent Browser Profiles ====================
    /**
     * A complete browser profile — all fields are internally consistent.
     * These represent real-world browser+OS+hardware combinations.
     */
    private data class BrowserProfile(
        val userAgent: String,
        val platform: String,
        val vendor: String,
        val appVersion: String,
        // Client Hints (Sec-CH-UA)
        val chUa: String,              // e.g. "\"Chromium\";v=\"131\", \"Google Chrome\";v=\"131\""
        val chUaPlatform: String,      // e.g. "\"Windows\""
        val chUaMobile: String,        // "?0"
        val chUaPlatformVersion: String, // e.g. "\"15.0.0\""
        val chUaFullVersion: String,   // e.g. "\"131.0.6778.86\""
        val chUaModel: String,         // "" for desktop
        val chUaArch: String,          // "\"x86\"" or "\"arm\""
        val chUaBitness: String,       // "\"64\""
        // WebGL — must match the OS
        val webglVendor: String,
        val webglRenderer: String,
        // Hardware
        val maxTouchPoints: Int,       // 0 for desktop
        val colorDepth: Int,
        // Browser type (for internal logic)
        val browserType: BrowserType
    )

    enum class BrowserType { CHROME, FIREFOX, SAFARI, EDGE }

    private val profiles = listOf(
        // ==================== Chrome on Windows ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            platform = "Win32", vendor = "Google Inc.",
            appVersion = "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            chUa = "\"Chromium\";v=\"131\", \"Google Chrome\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            chUaPlatform = "\"Windows\"", chUaMobile = "?0", chUaPlatformVersion = "\"15.0.0\"",
            chUaFullVersion = "\"131.0.6778.86\"", chUaModel = "\"\"",
            chUaArch = "\"x86\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (NVIDIA)", webglRenderer = "ANGLE (NVIDIA, NVIDIA GeForce RTX 4060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.CHROME
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
            platform = "Win32", vendor = "Google Inc.",
            appVersion = "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
            chUa = "\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"",
            chUaPlatform = "\"Windows\"", chUaMobile = "?0", chUaPlatformVersion = "\"15.0.0\"",
            chUaFullVersion = "\"130.0.6723.117\"", chUaModel = "\"\"",
            chUaArch = "\"x86\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (Intel)", webglRenderer = "ANGLE (Intel, Intel(R) UHD Graphics 770 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.CHROME
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
            platform = "Win32", vendor = "Google Inc.",
            appVersion = "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
            chUa = "\"Chromium\";v=\"129\", \"Google Chrome\";v=\"129\", \"Not=A?Brand\";v=\"8\"",
            chUaPlatform = "\"Windows\"", chUaMobile = "?0", chUaPlatformVersion = "\"10.0.0\"",
            chUaFullVersion = "\"129.0.6668.100\"", chUaModel = "\"\"",
            chUaArch = "\"x86\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (AMD)", webglRenderer = "ANGLE (AMD, AMD Radeon RX 7800 XT Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.CHROME
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
            platform = "Win32", vendor = "Google Inc.",
            appVersion = "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
            chUa = "\"Chromium\";v=\"128\", \"Google Chrome\";v=\"128\", \"Not;A=Brand\";v=\"24\"",
            chUaPlatform = "\"Windows\"", chUaMobile = "?0", chUaPlatformVersion = "\"10.0.0\"",
            chUaFullVersion = "\"128.0.6613.137\"", chUaModel = "\"\"",
            chUaArch = "\"x86\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (NVIDIA)", webglRenderer = "ANGLE (NVIDIA, NVIDIA GeForce GTX 1660 SUPER Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.CHROME
        ),
        // ==================== Chrome on macOS ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            platform = "MacIntel", vendor = "Google Inc.",
            appVersion = "5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            chUa = "\"Chromium\";v=\"131\", \"Google Chrome\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            chUaPlatform = "\"macOS\"", chUaMobile = "?0", chUaPlatformVersion = "\"14.7.1\"",
            chUaFullVersion = "\"131.0.6778.86\"", chUaModel = "\"\"",
            chUaArch = "\"arm\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (Apple)", webglRenderer = "ANGLE (Apple, Apple M2, OpenGL 4.1)",
            maxTouchPoints = 0, colorDepth = 30, browserType = BrowserType.CHROME
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
            platform = "MacIntel", vendor = "Google Inc.",
            appVersion = "5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
            chUa = "\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"",
            chUaPlatform = "\"macOS\"", chUaMobile = "?0", chUaPlatformVersion = "\"14.6.0\"",
            chUaFullVersion = "\"130.0.6723.117\"", chUaModel = "\"\"",
            chUaArch = "\"arm\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (Apple)", webglRenderer = "ANGLE (Apple, Apple M1 Pro, OpenGL 4.1)",
            maxTouchPoints = 0, colorDepth = 30, browserType = BrowserType.CHROME
        ),
        // ==================== Chrome on Linux ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            platform = "Linux x86_64", vendor = "Google Inc.",
            appVersion = "5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            chUa = "\"Chromium\";v=\"131\", \"Google Chrome\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            chUaPlatform = "\"Linux\"", chUaMobile = "?0", chUaPlatformVersion = "\"6.8.0\"",
            chUaFullVersion = "\"131.0.6778.86\"", chUaModel = "\"\"",
            chUaArch = "\"x86\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (NVIDIA Corporation)", webglRenderer = "ANGLE (NVIDIA Corporation, NVIDIA GeForce RTX 3070/PCIe/SSE2, OpenGL 4.5)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.CHROME
        ),
        // ==================== Firefox on Windows ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
            platform = "Win32", vendor = "",
            appVersion = "5.0 (Windows)",
            chUa = "", chUaPlatform = "", chUaMobile = "", chUaPlatformVersion = "",
            chUaFullVersion = "", chUaModel = "", chUaArch = "", chUaBitness = "",
            webglVendor = "Mozilla", webglRenderer = "Mozilla -- ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Ti Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.FIREFOX
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0",
            platform = "Win32", vendor = "",
            appVersion = "5.0 (Windows)",
            chUa = "", chUaPlatform = "", chUaMobile = "", chUaPlatformVersion = "",
            chUaFullVersion = "", chUaModel = "", chUaArch = "", chUaBitness = "",
            webglVendor = "Mozilla", webglRenderer = "Mozilla -- ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.FIREFOX
        ),
        // ==================== Firefox on macOS ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:133.0) Gecko/20100101 Firefox/133.0",
            platform = "MacIntel", vendor = "",
            appVersion = "5.0 (Macintosh)",
            chUa = "", chUaPlatform = "", chUaMobile = "", chUaPlatformVersion = "",
            chUaFullVersion = "", chUaModel = "", chUaArch = "", chUaBitness = "",
            webglVendor = "Mozilla", webglRenderer = "Mozilla -- Apple M2 -- Apple GPU",
            maxTouchPoints = 0, colorDepth = 30, browserType = BrowserType.FIREFOX
        ),
        // ==================== Firefox on Linux ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0",
            platform = "Linux x86_64", vendor = "",
            appVersion = "5.0 (X11)",
            chUa = "", chUaPlatform = "", chUaMobile = "", chUaPlatformVersion = "",
            chUaFullVersion = "", chUaModel = "", chUaArch = "", chUaBitness = "",
            webglVendor = "Mozilla", webglRenderer = "Mozilla -- NVIDIA Corporation NVIDIA GeForce RTX 3070/PCIe/SSE2 -- OpenGL",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.FIREFOX
        ),
        // ==================== Safari on macOS ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Safari/605.1.15",
            platform = "MacIntel", vendor = "Apple Computer, Inc.",
            appVersion = "5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Safari/605.1.15",
            chUa = "", chUaPlatform = "", chUaMobile = "", chUaPlatformVersion = "",
            chUaFullVersion = "", chUaModel = "", chUaArch = "", chUaBitness = "",
            webglVendor = "Apple Inc.", webglRenderer = "Apple M2 Pro",
            maxTouchPoints = 0, colorDepth = 30, browserType = BrowserType.SAFARI
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15",
            platform = "MacIntel", vendor = "Apple Computer, Inc.",
            appVersion = "5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15",
            chUa = "", chUaPlatform = "", chUaMobile = "", chUaPlatformVersion = "",
            chUaFullVersion = "", chUaModel = "", chUaArch = "", chUaBitness = "",
            webglVendor = "Apple Inc.", webglRenderer = "Apple M1",
            maxTouchPoints = 0, colorDepth = 30, browserType = BrowserType.SAFARI
        ),
        // ==================== Edge on Windows ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.2903.70",
            platform = "Win32", vendor = "Google Inc.",
            appVersion = "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.2903.70",
            chUa = "\"Chromium\";v=\"131\", \"Microsoft Edge\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            chUaPlatform = "\"Windows\"", chUaMobile = "?0", chUaPlatformVersion = "\"15.0.0\"",
            chUaFullVersion = "\"131.0.2903.70\"", chUaModel = "\"\"",
            chUaArch = "\"x86\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (Intel)", webglRenderer = "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.EDGE
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.2849.80",
            platform = "Win32", vendor = "Google Inc.",
            appVersion = "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.2849.80",
            chUa = "\"Chromium\";v=\"130\", \"Microsoft Edge\";v=\"130\", \"Not?A_Brand\";v=\"99\"",
            chUaPlatform = "\"Windows\"", chUaMobile = "?0", chUaPlatformVersion = "\"15.0.0\"",
            chUaFullVersion = "\"130.0.2849.80\"", chUaModel = "\"\"",
            chUaArch = "\"x86\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (NVIDIA)", webglRenderer = "ANGLE (NVIDIA, NVIDIA GeForce RTX 3080 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            maxTouchPoints = 0, colorDepth = 24, browserType = BrowserType.EDGE
        ),
        // ==================== Edge on macOS ====================
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.2903.70",
            platform = "MacIntel", vendor = "Google Inc.",
            appVersion = "5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.2903.70",
            chUa = "\"Chromium\";v=\"131\", \"Microsoft Edge\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            chUaPlatform = "\"macOS\"", chUaMobile = "?0", chUaPlatformVersion = "\"14.7.1\"",
            chUaFullVersion = "\"131.0.2903.70\"", chUaModel = "\"\"",
            chUaArch = "\"arm\"", chUaBitness = "\"64\"",
            webglVendor = "Google Inc. (Apple)", webglRenderer = "ANGLE (Apple, Apple M3, OpenGL 4.1)",
            maxTouchPoints = 0, colorDepth = 30, browserType = BrowserType.EDGE
        )
    )

    // ==================== Languages ====================
    private val languages = listOf(
        "zh-CN,zh;q=0.9,en;q=0.8",
        "zh-TW,zh;q=0.9,en;q=0.8",
        "en-US,en;q=0.9",
        "en-GB,en;q=0.9",
        "ja-JP,ja;q=0.9,en;q=0.8",
        "ko-KR,ko;q=0.9,en;q=0.8",
        "de-DE,de;q=0.9,en;q=0.8",
        "fr-FR,fr;q=0.9,en;q=0.8",
        "es-ES,es;q=0.9,en;q=0.8",
        "pt-BR,pt;q=0.9,en;q=0.8",
        "ru-RU,ru;q=0.9,en;q=0.8"
    )

    // ==================== Timezones ====================
    private val timezones = listOf(
        "Asia/Shanghai", "Asia/Tokyo", "Asia/Seoul", "Asia/Singapore",
        "Asia/Hong_Kong", "Asia/Taipei", "Asia/Kolkata",
        "America/New_York", "America/Chicago", "America/Denver",
        "America/Los_Angeles", "America/Toronto", "America/Sao_Paulo",
        "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Moscow",
        "Australia/Sydney", "Pacific/Auckland"
    )

    // ==================== Screen Resolutions ====================
    private val screenResolutions = listOf(
        Pair(1920, 1080), Pair(2560, 1440), Pair(3840, 2160),
        Pair(1366, 768), Pair(1536, 864), Pair(1440, 900),
        Pair(1280, 720), Pair(1600, 900), Pair(2560, 1600),
        Pair(3440, 1440), Pair(1280, 800), Pair(1680, 1050)
    )

    // ==================== Hardware configs ====================
    private val hardwareConcurrencyOptions = listOf(4, 6, 8, 10, 12, 16, 20, 24)
    private val deviceMemoryOptions = listOf(4, 8, 16, 32)

    // ==================== Public API ====================

    /**
     * 生成完整一致的浏览器指纹
     * 同一 seed 保证每次生成完全相同的结果
     */
    fun generateFingerprint(seed: String? = null): GeneratedFingerprint {
        val random = if (seed != null) Random(seed.hashCode().toLong()) else Random

        val profile = profiles[random.nextInt(profiles.size)]
        val resolution = screenResolutions[random.nextInt(screenResolutions.size)]
        val language = languages[random.nextInt(languages.size)]
        val timezone = timezones[random.nextInt(timezones.size)]

        return GeneratedFingerprint(
            userAgent = profile.userAgent,
            platform = profile.platform,
            vendor = profile.vendor,
            appVersion = profile.appVersion,
            language = language,
            timezone = timezone,
            screenWidth = resolution.first,
            screenHeight = resolution.second,
            colorDepth = profile.colorDepth,
            hardwareConcurrency = hardwareConcurrencyOptions[random.nextInt(hardwareConcurrencyOptions.size)],
            deviceMemory = deviceMemoryOptions[random.nextInt(deviceMemoryOptions.size)],
            maxTouchPoints = profile.maxTouchPoints,
            canvasNoiseSeed = random.nextLong(),
            audioNoiseSeed = random.nextLong(),
            webglVendor = profile.webglVendor,
            webglRenderer = profile.webglRenderer,
            // Client Hints
            chUa = profile.chUa,
            chUaPlatform = profile.chUaPlatform,
            chUaMobile = profile.chUaMobile,
            chUaPlatformVersion = profile.chUaPlatformVersion,
            chUaFullVersion = profile.chUaFullVersion,
            chUaModel = profile.chUaModel,
            chUaArch = profile.chUaArch,
            chUaBitness = profile.chUaBitness,
            browserType = profile.browserType.name
        )
    }

    // ==================== IP Generation ====================

    fun generateRandomIp(range: IpRange = IpRange.USA, searchKeyword: String? = null): String {
        return when (range) {
            IpRange.USA -> generateIpFromRanges(USA_IP_RANGES)
            IpRange.SEARCH -> searchKeyword?.let { generateIpByCountry(it) } ?: generateGlobalIp()
            IpRange.GLOBAL -> generateGlobalIp()
        }
    }

    fun generateIpByCountry(keyword: String): String {
        val k = keyword.lowercase().trim()
        val ranges = COUNTRY_IP_RANGES.entries.firstOrNull { (keys, _) ->
            keys.any { k.contains(it) }
        }?.value ?: GLOBAL_IP_RANGES
        return generateIpFromRanges(ranges)
    }

    fun getSupportedCountries(): List<String> = listOf(
        "中国", "美国", "日本", "韩国", "英国", "德国", "法国", "俄罗斯",
        "巴西", "印度", "澳大利亚", "加拿大", "新加坡", "香港", "台湾", "欧洲", "亚洲"
    )

    // ==================== IP Ranges (corrected, region-specific) ====================

    private data class IpBlock(val first: Int, val secondStart: Int, val secondEnd: Int)

    private val USA_IP_RANGES = listOf(
        IpBlock(3, 0, 255), IpBlock(4, 0, 255), IpBlock(6, 0, 255),
        IpBlock(8, 0, 255), IpBlock(13, 0, 255), IpBlock(15, 0, 255),
        IpBlock(16, 0, 255), IpBlock(18, 0, 255), IpBlock(20, 0, 255),
        IpBlock(23, 0, 255), IpBlock(32, 0, 255), IpBlock(34, 0, 255),
        IpBlock(35, 0, 255), IpBlock(40, 0, 255), IpBlock(44, 0, 255),
        IpBlock(52, 0, 255), IpBlock(54, 0, 255), IpBlock(63, 0, 255),
        IpBlock(64, 0, 127), IpBlock(65, 0, 255), IpBlock(66, 0, 255),
        IpBlock(67, 0, 255), IpBlock(68, 0, 255), IpBlock(69, 0, 255),
        IpBlock(71, 0, 255), IpBlock(72, 0, 255), IpBlock(73, 0, 255),
        IpBlock(74, 0, 255), IpBlock(75, 0, 255), IpBlock(76, 0, 255),
        IpBlock(96, 0, 255), IpBlock(97, 0, 255), IpBlock(98, 0, 255),
        IpBlock(99, 0, 255), IpBlock(104, 0, 255), IpBlock(107, 0, 255),
        IpBlock(108, 0, 255), IpBlock(142, 0, 255), IpBlock(143, 0, 255),
        IpBlock(147, 0, 255), IpBlock(149, 0, 255), IpBlock(155, 0, 255),
        IpBlock(157, 0, 255), IpBlock(162, 0, 255), IpBlock(172, 0, 255),
        IpBlock(173, 0, 255), IpBlock(174, 0, 255), IpBlock(184, 0, 255),
        IpBlock(198, 0, 255), IpBlock(199, 0, 255), IpBlock(204, 0, 255),
        IpBlock(205, 0, 255), IpBlock(206, 0, 255), IpBlock(207, 0, 255),
        IpBlock(208, 0, 255), IpBlock(209, 0, 255), IpBlock(216, 0, 255)
    )

    // Country keyword → IP ranges map (corrected region-specific allocations)
    private val COUNTRY_IP_RANGES: Map<Set<String>, List<IpBlock>> = mapOf(
        setOf("中国", "china", "cn") to listOf(
            IpBlock(1, 80, 83), IpBlock(14, 0, 31), IpBlock(27, 0, 31),
            IpBlock(36, 0, 47), IpBlock(42, 0, 63), IpBlock(58, 16, 63),
            IpBlock(59, 32, 127), IpBlock(60, 0, 255), IpBlock(61, 0, 255),
            IpBlock(101, 0, 255), IpBlock(106, 0, 127), IpBlock(110, 0, 255),
            IpBlock(111, 0, 255), IpBlock(112, 0, 255), IpBlock(113, 0, 255),
            IpBlock(114, 0, 255), IpBlock(115, 0, 255), IpBlock(116, 0, 255),
            IpBlock(117, 0, 255), IpBlock(118, 0, 255), IpBlock(119, 0, 255),
            IpBlock(120, 0, 255), IpBlock(121, 0, 127), IpBlock(122, 0, 255),
            IpBlock(123, 0, 255), IpBlock(124, 0, 255), IpBlock(125, 0, 255),
            IpBlock(180, 76, 255), IpBlock(182, 0, 255), IpBlock(183, 0, 255),
            IpBlock(202, 96, 127), IpBlock(211, 64, 159), IpBlock(218, 0, 127),
            IpBlock(219, 128, 255), IpBlock(220, 0, 255), IpBlock(221, 0, 255),
            IpBlock(222, 0, 255), IpBlock(223, 0, 255)
        ),
        setOf("日本", "japan", "jp") to listOf(
            IpBlock(1, 33, 36), IpBlock(14, 192, 207), IpBlock(27, 80, 127),
            IpBlock(36, 48, 63), IpBlock(42, 124, 127), IpBlock(43, 224, 255),
            IpBlock(49, 96, 127), IpBlock(59, 128, 191), IpBlock(60, 32, 63),
            IpBlock(61, 192, 223), IpBlock(101, 102, 111), IpBlock(103, 0, 15),
            IpBlock(106, 128, 191), IpBlock(110, 0, 63), IpBlock(111, 64, 127),
            IpBlock(113, 32, 63), IpBlock(118, 0, 63), IpBlock(119, 224, 239),
            IpBlock(126, 0, 255), IpBlock(133, 0, 255), IpBlock(150, 0, 63),
            IpBlock(153, 0, 63), IpBlock(157, 0, 31), IpBlock(163, 32, 63),
            IpBlock(175, 0, 31), IpBlock(202, 32, 63), IpBlock(210, 128, 191),
            IpBlock(211, 0, 63), IpBlock(219, 0, 127)
        ),
        setOf("韩国", "korea", "kr") to listOf(
            IpBlock(1, 208, 223), IpBlock(14, 32, 63), IpBlock(27, 0, 31),
            IpBlock(39, 0, 31), IpBlock(49, 0, 31), IpBlock(58, 64, 95),
            IpBlock(59, 0, 31), IpBlock(61, 32, 95), IpBlock(106, 0, 31),
            IpBlock(110, 64, 95), IpBlock(112, 128, 191), IpBlock(114, 64, 127),
            IpBlock(115, 64, 127), IpBlock(118, 128, 191), IpBlock(119, 192, 223),
            IpBlock(121, 128, 191), IpBlock(175, 192, 223), IpBlock(203, 224, 255),
            IpBlock(210, 192, 223), IpBlock(211, 160, 255), IpBlock(218, 128, 191),
            IpBlock(220, 64, 127), IpBlock(221, 128, 191), IpBlock(222, 96, 127)
        ),
        setOf("英国", "uk", "britain") to listOf(
            IpBlock(2, 24, 31), IpBlock(5, 56, 63), IpBlock(31, 48, 55),
            IpBlock(51, 0, 63), IpBlock(77, 64, 127), IpBlock(78, 128, 191),
            IpBlock(79, 64, 127), IpBlock(80, 0, 63), IpBlock(81, 64, 127),
            IpBlock(82, 0, 63), IpBlock(86, 0, 63), IpBlock(90, 192, 255),
            IpBlock(92, 0, 63), IpBlock(109, 0, 31), IpBlock(176, 24, 31),
            IpBlock(178, 128, 159), IpBlock(185, 0, 31), IpBlock(193, 0, 63),
            IpBlock(194, 128, 159), IpBlock(212, 0, 63), IpBlock(217, 128, 191)
        ),
        setOf("德国", "germany", "de") to listOf(
            IpBlock(2, 16, 23), IpBlock(5, 0, 15), IpBlock(31, 0, 15),
            IpBlock(46, 0, 31), IpBlock(62, 0, 63), IpBlock(78, 0, 63),
            IpBlock(79, 192, 255), IpBlock(80, 64, 127), IpBlock(81, 0, 63),
            IpBlock(84, 0, 63), IpBlock(85, 0, 63), IpBlock(87, 128, 191),
            IpBlock(88, 64, 127), IpBlock(89, 0, 63), IpBlock(91, 0, 63),
            IpBlock(93, 0, 63), IpBlock(109, 32, 63), IpBlock(176, 0, 23),
            IpBlock(178, 0, 31), IpBlock(185, 32, 63), IpBlock(188, 0, 31),
            IpBlock(193, 128, 159), IpBlock(194, 0, 31), IpBlock(195, 0, 63),
            IpBlock(212, 64, 127), IpBlock(213, 0, 63), IpBlock(217, 0, 63)
        ),
        setOf("法国", "france", "fr") to listOf(
            IpBlock(2, 0, 15), IpBlock(5, 32, 47), IpBlock(31, 32, 47),
            IpBlock(37, 0, 63), IpBlock(46, 32, 63), IpBlock(62, 128, 191),
            IpBlock(77, 128, 191), IpBlock(78, 192, 255), IpBlock(80, 128, 191),
            IpBlock(82, 64, 127), IpBlock(83, 0, 63), IpBlock(86, 64, 127),
            IpBlock(88, 128, 191), IpBlock(89, 64, 127), IpBlock(90, 0, 63),
            IpBlock(91, 64, 127), IpBlock(109, 192, 223), IpBlock(176, 128, 191),
            IpBlock(193, 192, 223), IpBlock(194, 160, 191), IpBlock(195, 64, 127),
            IpBlock(212, 128, 191), IpBlock(213, 64, 127)
        ),
        setOf("俄罗斯", "russia", "ru") to listOf(
            IpBlock(2, 56, 63), IpBlock(5, 128, 191), IpBlock(31, 128, 191),
            IpBlock(37, 128, 191), IpBlock(46, 128, 191), IpBlock(62, 64, 127),
            IpBlock(77, 0, 63), IpBlock(78, 64, 127), IpBlock(79, 128, 191),
            IpBlock(80, 192, 255), IpBlock(81, 128, 191), IpBlock(83, 128, 191),
            IpBlock(85, 128, 191), IpBlock(87, 0, 63), IpBlock(89, 128, 191),
            IpBlock(91, 192, 255), IpBlock(93, 128, 191), IpBlock(95, 0, 127),
            IpBlock(109, 64, 127), IpBlock(176, 192, 255), IpBlock(178, 160, 191),
            IpBlock(185, 64, 127), IpBlock(188, 128, 191), IpBlock(193, 224, 255),
            IpBlock(194, 192, 223), IpBlock(195, 128, 191), IpBlock(212, 192, 255),
            IpBlock(213, 128, 191), IpBlock(217, 64, 127)
        ),
        setOf("巴西", "brazil", "br") to listOf(
            IpBlock(131, 0, 63), IpBlock(138, 0, 63), IpBlock(143, 0, 63),
            IpBlock(152, 0, 63), IpBlock(164, 0, 63), IpBlock(168, 0, 127),
            IpBlock(170, 0, 127), IpBlock(177, 0, 255), IpBlock(179, 0, 255),
            IpBlock(186, 0, 255), IpBlock(187, 0, 255), IpBlock(189, 0, 255),
            IpBlock(191, 0, 255), IpBlock(200, 0, 255), IpBlock(201, 0, 255)
        ),
        setOf("印度", "india") to listOf(
            IpBlock(1, 0, 15), IpBlock(14, 128, 191), IpBlock(27, 32, 63),
            IpBlock(36, 224, 255), IpBlock(39, 32, 63), IpBlock(42, 64, 95),
            IpBlock(43, 192, 223), IpBlock(49, 32, 63), IpBlock(59, 192, 223),
            IpBlock(103, 16, 63), IpBlock(106, 192, 255), IpBlock(110, 224, 255),
            IpBlock(112, 64, 127), IpBlock(115, 128, 191), IpBlock(117, 192, 255),
            IpBlock(122, 128, 191), IpBlock(125, 16, 63), IpBlock(175, 96, 127),
            IpBlock(182, 64, 95), IpBlock(202, 0, 31), IpBlock(203, 0, 31),
            IpBlock(210, 0, 31)
        ),
        setOf("澳大利亚", "australia", "au") to listOf(
            IpBlock(1, 120, 127), IpBlock(14, 0, 15), IpBlock(27, 64, 79),
            IpBlock(43, 128, 159), IpBlock(49, 128, 175), IpBlock(58, 0, 15),
            IpBlock(59, 160, 175), IpBlock(101, 0, 31), IpBlock(103, 64, 95),
            IpBlock(110, 128, 159), IpBlock(112, 0, 31), IpBlock(114, 128, 159),
            IpBlock(116, 0, 63), IpBlock(120, 128, 159), IpBlock(121, 192, 223),
            IpBlock(122, 192, 255), IpBlock(124, 0, 63), IpBlock(175, 32, 63),
            IpBlock(180, 0, 31), IpBlock(202, 128, 159), IpBlock(203, 0, 63)
        ),
        setOf("加拿大", "canada", "ca") to listOf(
            IpBlock(24, 32, 63), IpBlock(64, 128, 191), IpBlock(65, 0, 63),
            IpBlock(66, 128, 191), IpBlock(67, 192, 255), IpBlock(69, 0, 63),
            IpBlock(70, 0, 63), IpBlock(72, 0, 63), IpBlock(74, 192, 255),
            IpBlock(96, 0, 63), IpBlock(99, 192, 255), IpBlock(104, 128, 191),
            IpBlock(142, 128, 191), IpBlock(144, 0, 63), IpBlock(154, 0, 63),
            IpBlock(174, 128, 191), IpBlock(184, 0, 63), IpBlock(192, 0, 63),
            IpBlock(198, 128, 191), IpBlock(204, 64, 127), IpBlock(206, 0, 63),
            IpBlock(207, 0, 63), IpBlock(209, 0, 63)
        ),
        setOf("新加坡", "singapore", "sg") to listOf(
            IpBlock(1, 32, 39), IpBlock(13, 228, 231), IpBlock(14, 224, 239),
            IpBlock(27, 96, 111), IpBlock(42, 96, 103), IpBlock(43, 160, 175),
            IpBlock(49, 176, 191), IpBlock(52, 76, 79), IpBlock(58, 96, 111),
            IpBlock(101, 96, 103), IpBlock(103, 96, 111), IpBlock(106, 240, 255),
            IpBlock(110, 160, 175), IpBlock(116, 64, 79), IpBlock(118, 192, 207),
            IpBlock(119, 64, 79), IpBlock(122, 0, 15), IpBlock(175, 64, 79),
            IpBlock(202, 160, 175), IpBlock(203, 64, 95), IpBlock(210, 32, 47)
        ),
        setOf("香港", "hongkong", "hk") to listOf(
            IpBlock(1, 64, 79), IpBlock(14, 240, 255), IpBlock(27, 112, 127),
            IpBlock(42, 104, 111), IpBlock(43, 176, 191), IpBlock(49, 192, 207),
            IpBlock(58, 112, 127), IpBlock(59, 176, 191), IpBlock(101, 64, 79),
            IpBlock(103, 112, 127), IpBlock(110, 176, 191), IpBlock(112, 32, 47),
            IpBlock(116, 192, 207), IpBlock(119, 80, 95), IpBlock(124, 64, 79),
            IpBlock(175, 80, 95), IpBlock(180, 32, 47), IpBlock(202, 176, 191),
            IpBlock(203, 128, 159), IpBlock(210, 48, 63), IpBlock(218, 192, 207)
        ),
        setOf("台湾", "taiwan", "tw") to listOf(
            IpBlock(1, 160, 175), IpBlock(14, 208, 223), IpBlock(27, 128, 143),
            IpBlock(36, 64, 79), IpBlock(42, 112, 119), IpBlock(49, 208, 223),
            IpBlock(59, 224, 239), IpBlock(60, 64, 95), IpBlock(61, 128, 159),
            IpBlock(101, 80, 95), IpBlock(103, 128, 143), IpBlock(106, 64, 79),
            IpBlock(110, 192, 207), IpBlock(111, 0, 31), IpBlock(114, 0, 63),
            IpBlock(118, 64, 95), IpBlock(122, 64, 95), IpBlock(175, 128, 143),
            IpBlock(180, 48, 63), IpBlock(202, 64, 95), IpBlock(203, 160, 191),
            IpBlock(210, 64, 95), IpBlock(211, 192, 223), IpBlock(218, 208, 223),
            IpBlock(220, 128, 191)
        ),
        setOf("欧洲", "europe", "eu") to listOf(
            IpBlock(2, 0, 63), IpBlock(5, 0, 63), IpBlock(31, 0, 63),
            IpBlock(37, 0, 127), IpBlock(46, 0, 127), IpBlock(62, 0, 255),
            IpBlock(77, 0, 255), IpBlock(78, 0, 255), IpBlock(79, 0, 255),
            IpBlock(80, 0, 255), IpBlock(81, 0, 255), IpBlock(82, 0, 255),
            IpBlock(83, 0, 255), IpBlock(84, 0, 255), IpBlock(85, 0, 255),
            IpBlock(86, 0, 255), IpBlock(87, 0, 255), IpBlock(88, 0, 255),
            IpBlock(89, 0, 255), IpBlock(90, 0, 255), IpBlock(91, 0, 255),
            IpBlock(92, 0, 255), IpBlock(93, 0, 255), IpBlock(94, 0, 255),
            IpBlock(95, 0, 255), IpBlock(109, 0, 255), IpBlock(176, 0, 255),
            IpBlock(178, 0, 255), IpBlock(185, 0, 255), IpBlock(188, 0, 255),
            IpBlock(193, 0, 255), IpBlock(194, 0, 255), IpBlock(195, 0, 255),
            IpBlock(212, 0, 255), IpBlock(213, 0, 255), IpBlock(217, 0, 255)
        ),
        setOf("亚洲", "asia") to listOf(
            IpBlock(1, 0, 255), IpBlock(14, 0, 255), IpBlock(27, 0, 255),
            IpBlock(36, 0, 255), IpBlock(42, 0, 255), IpBlock(43, 0, 255),
            IpBlock(49, 0, 255), IpBlock(58, 0, 255), IpBlock(59, 0, 255),
            IpBlock(60, 0, 255), IpBlock(61, 0, 255), IpBlock(101, 0, 255),
            IpBlock(103, 0, 255), IpBlock(106, 0, 255), IpBlock(110, 0, 255),
            IpBlock(111, 0, 255), IpBlock(112, 0, 255), IpBlock(113, 0, 255),
            IpBlock(114, 0, 255), IpBlock(115, 0, 255), IpBlock(116, 0, 255),
            IpBlock(117, 0, 255), IpBlock(118, 0, 255), IpBlock(119, 0, 255),
            IpBlock(120, 0, 255), IpBlock(121, 0, 255), IpBlock(122, 0, 255),
            IpBlock(123, 0, 255), IpBlock(124, 0, 255), IpBlock(125, 0, 255),
            IpBlock(126, 0, 255), IpBlock(175, 0, 255), IpBlock(180, 0, 255),
            IpBlock(182, 0, 255), IpBlock(183, 0, 255), IpBlock(202, 0, 255),
            IpBlock(203, 0, 255), IpBlock(210, 0, 255), IpBlock(211, 0, 255),
            IpBlock(218, 0, 255), IpBlock(219, 0, 255), IpBlock(220, 0, 255),
            IpBlock(221, 0, 255), IpBlock(222, 0, 255), IpBlock(223, 0, 255)
        )
    )

    private val GLOBAL_IP_RANGES = listOf(
        IpBlock(3, 0, 255), IpBlock(8, 0, 255), IpBlock(13, 0, 255),
        IpBlock(17, 0, 255), IpBlock(18, 0, 255), IpBlock(23, 0, 255),
        IpBlock(34, 0, 255), IpBlock(40, 0, 255), IpBlock(52, 0, 255),
        IpBlock(64, 0, 255), IpBlock(66, 0, 255), IpBlock(72, 0, 255),
        IpBlock(74, 0, 255), IpBlock(77, 0, 255), IpBlock(80, 0, 255),
        IpBlock(88, 0, 255), IpBlock(91, 0, 255), IpBlock(104, 0, 255),
        IpBlock(108, 0, 255), IpBlock(110, 0, 255), IpBlock(142, 0, 255),
        IpBlock(157, 0, 255), IpBlock(162, 0, 255), IpBlock(172, 0, 255),
        IpBlock(175, 0, 255), IpBlock(184, 0, 255), IpBlock(193, 0, 255),
        IpBlock(198, 0, 255), IpBlock(204, 0, 255), IpBlock(212, 0, 255)
    )

    private fun generateIpFromRanges(ranges: List<IpBlock>): String {
        val block = ranges.random()
        val second = Random.nextInt(block.secondStart, block.secondEnd + 1)
        return "${block.first}.$second.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }

    private fun generateGlobalIp(): String = generateIpFromRanges(GLOBAL_IP_RANGES)
}

/**
 * 生成的指纹数据 — 完整一致的浏览器身份
 */
data class GeneratedFingerprint(
    val userAgent: String,
    val platform: String,
    val vendor: String,
    val appVersion: String,
    val language: String,
    val timezone: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val colorDepth: Int,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val maxTouchPoints: Int,
    // Noise seeds (deterministic per session, not random per call)
    val canvasNoiseSeed: Long,
    val audioNoiseSeed: Long,
    // WebGL
    val webglVendor: String,
    val webglRenderer: String,
    // Client Hints (empty for Firefox/Safari which don't support CH)
    val chUa: String,
    val chUaPlatform: String,
    val chUaMobile: String,
    val chUaPlatformVersion: String,
    val chUaFullVersion: String,
    val chUaModel: String,
    val chUaArch: String,
    val chUaBitness: String,
    // Browser type
    val browserType: String
) {
    // Backward compat: old code used canvasNoise/audioNoise as Float
    val canvasNoise: Float get() = (canvasNoiseSeed % 10000) / 100000000f
    val audioNoise: Float get() = (audioNoiseSeed % 10000) / 100000000f
}
