package com.webtoapp.core.isolation

import kotlin.random.Random

/**
 * 浏览器指纹生成器
 * 
 * 生成随机但一致的浏览器指纹，用于防检测
 */
object FingerprintGenerator {
    
    // 常见 User-Agent 列表
    private val userAgents = listOf(
        // Chrome Windows
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
        // Chrome Mac
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        // Firefox Windows
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
        // Firefox Mac
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:121.0) Gecko/20100101 Firefox/121.0",
        // Safari
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
        // Edge
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
    )
    
    // 常见平台
    private val platforms = listOf("Win32", "MacIntel", "Linux x86_64")
    
    // 常见厂商
    private val vendors = listOf("Google Inc.", "Apple Computer, Inc.", "")
    
    // 常见语言
    private val languages = listOf(
        "zh-CN,zh;q=0.9,en;q=0.8",
        "en-US,en;q=0.9",
        "en-GB,en;q=0.9",
        "ja-JP,ja;q=0.9,en;q=0.8",
        "ko-KR,ko;q=0.9,en;q=0.8"
    )
    
    // 常见时区
    private val timezones = listOf(
        "Asia/Shanghai",
        "America/New_York",
        "America/Los_Angeles",
        "Europe/London",
        "Europe/Paris",
        "Asia/Tokyo"
    )
    
    // 常见屏幕分辨率
    private val screenResolutions = listOf(
        Pair(1920, 1080),
        Pair(2560, 1440),
        Pair(1366, 768),
        Pair(1536, 864),
        Pair(1440, 900),
        Pair(1280, 720)
    )
    
    /**
     * 生成随机指纹
     */
    fun generateFingerprint(seed: String? = null): GeneratedFingerprint {
        val random = if (seed != null) Random(seed.hashCode().toLong()) else Random
        
        val resolution = screenResolutions[random.nextInt(screenResolutions.size)]
        
        return GeneratedFingerprint(
            userAgent = userAgents[random.nextInt(userAgents.size)],
            platform = platforms[random.nextInt(platforms.size)],
            vendor = vendors[random.nextInt(vendors.size)],
            language = languages[random.nextInt(languages.size)],
            timezone = timezones[random.nextInt(timezones.size)],
            screenWidth = resolution.first,
            screenHeight = resolution.second,
            colorDepth = listOf(24, 32)[random.nextInt(2)],
            hardwareConcurrency = listOf(2, 4, 6, 8, 12, 16)[random.nextInt(6)],
            deviceMemory = listOf(2, 4, 8, 16)[random.nextInt(4)],
            canvasNoise = random.nextFloat() * 0.0001f,
            audioNoise = random.nextFloat() * 0.0001f,
            webglVendor = listOf(
                "Google Inc. (NVIDIA)",
                "Google Inc. (Intel)",
                "Google Inc. (AMD)",
                "Intel Inc.",
                "NVIDIA Corporation"
            )[random.nextInt(5)],
            webglRenderer = listOf(
                "ANGLE (NVIDIA GeForce GTX 1080 Direct3D11 vs_5_0 ps_5_0)",
                "ANGLE (Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0)",
                "ANGLE (AMD Radeon RX 580 Direct3D11 vs_5_0 ps_5_0)",
                "Intel Iris OpenGL Engine",
                "AMD Radeon Pro 5500M OpenGL Engine"
            )[random.nextInt(5)]
        )
    }
    
    /**
     * 生成随机 IP 地址
     */
    fun generateRandomIp(range: IpRange = IpRange.USA, searchKeyword: String? = null): String {
        return when (range) {
            IpRange.USA -> generateUsaIp()
            IpRange.SEARCH -> searchKeyword?.let { generateIpByCountry(it) } ?: generateGlobalIp()
            IpRange.GLOBAL -> generateGlobalIp()
        }
    }
    
    /**
     * 根据国家/地区关键词生成 IP
     */
    fun generateIpByCountry(keyword: String): String {
        val k = keyword.lowercase().trim()
        
        return when {
            k.contains("中国") || k.contains("china") || k.contains("cn") -> generateChinaIp()
            k.contains("美国") || k.contains("usa") || k.contains("us") || k.contains("america") -> generateUsaIp()
            k.contains("日本") || k.contains("japan") || k.contains("jp") -> generateJapanIp()
            k.contains("韩国") || k.contains("korea") || k.contains("kr") -> generateKoreaIp()
            k.contains("英国") || k.contains("uk") || k.contains("britain") -> generateUkIp()
            k.contains("德国") || k.contains("germany") || k.contains("de") -> generateGermanyIp()
            k.contains("法国") || k.contains("france") || k.contains("fr") -> generateFranceIp()
            k.contains("俄罗斯") || k.contains("russia") || k.contains("ru") -> generateRussiaIp()
            k.contains("巴西") || k.contains("brazil") || k.contains("br") -> generateBrazilIp()
            k.contains("印度") || k.contains("india") -> generateIndiaIp()
            k.contains("澳大利亚") || k.contains("australia") || k.contains("au") -> generateAustraliaIp()
            k.contains("加拿大") || k.contains("canada") || k.contains("ca") -> generateCanadaIp()
            k.contains("新加坡") || k.contains("singapore") || k.contains("sg") -> generateSingaporeIp()
            k.contains("香港") || k.contains("hongkong") || k.contains("hk") -> generateHongKongIp()
            k.contains("台湾") || k.contains("taiwan") || k.contains("tw") -> generateTaiwanIp()
            k.contains("欧洲") || k.contains("europe") || k.contains("eu") -> generateEuropeIp()
            k.contains("亚洲") || k.contains("asia") -> generateAsiaIp()
            else -> generateGlobalIp()
        }
    }
    
    /**
     * 获取支持的国家/地区列表
     */
    fun getSupportedCountries(): List<String> = listOf(
        "中国", "美国", "日本", "韩国", "英国", "德国", "法国", "俄罗斯",
        "巴西", "印度", "澳大利亚", "加拿大", "新加坡", "香港", "台湾", "欧洲", "亚洲"
    )

    
    private fun generateChinaIp(): String {
        val ranges = listOf(14, 27, 36, 42, 58, 59, 60, 61, 101, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 222, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateUsaIp(): String {
        val ranges = listOf(3, 4, 6, 7, 8, 9, 11, 12, 13, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 32, 33, 34, 35, 38, 40, 44, 45, 47, 48, 50, 52, 54, 55, 56, 57, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateJapanIp(): String {
        val ranges = listOf(1, 14, 27, 36, 42, 43, 49, 59, 60, 61, 101, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 133, 150, 153, 157, 163, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateKoreaIp(): String {
        val ranges = listOf(1, 14, 27, 39, 42, 49, 58, 59, 61, 106, 110, 111, 112, 114, 115, 116, 117, 118, 119, 121, 122, 123, 124, 125, 175, 180, 182, 183, 203, 210, 211, 218, 219, 220, 221, 222, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateUkIp(): String {
        val ranges = listOf(2, 5, 31, 37, 46, 51, 62, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 109, 176, 178, 185, 188, 193, 194, 195, 212, 213, 217)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateGermanyIp(): String {
        val ranges = listOf(2, 5, 31, 37, 46, 62, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 109, 176, 178, 185, 188, 193, 194, 195, 212, 213, 217)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateFranceIp(): String {
        val ranges = listOf(2, 5, 31, 37, 46, 62, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 109, 176, 178, 185, 188, 193, 194, 195, 212, 213, 217)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateRussiaIp(): String {
        val ranges = listOf(2, 5, 31, 37, 46, 62, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 109, 176, 178, 185, 188, 193, 194, 195, 212, 213, 217)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateBrazilIp(): String {
        val ranges = listOf(131, 138, 139, 143, 146, 152, 161, 164, 168, 177, 179, 186, 187, 189, 191, 200, 201)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateIndiaIp(): String {
        val ranges = listOf(1, 14, 27, 36, 39, 42, 43, 49, 59, 61, 101, 103, 106, 110, 111, 112, 114, 115, 116, 117, 118, 119, 121, 122, 123, 124, 125, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateAustraliaIp(): String {
        val ranges = listOf(1, 14, 27, 36, 42, 43, 49, 58, 59, 60, 61, 101, 103, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 222, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateCanadaIp(): String {
        val ranges = listOf(24, 64, 65, 66, 67, 68, 69, 70, 71, 72, 74, 75, 76, 96, 97, 98, 99, 104, 107, 108, 142, 144, 147, 148, 149, 154, 155, 156, 158, 159, 162, 166, 167, 169, 170, 172, 173, 174, 184, 192, 198, 199, 204, 205, 206, 207, 208, 209, 216)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateSingaporeIp(): String {
        val ranges = listOf(1, 14, 27, 36, 42, 43, 49, 58, 59, 60, 61, 101, 103, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 222, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateHongKongIp(): String {
        val ranges = listOf(1, 14, 27, 36, 42, 43, 49, 58, 59, 60, 61, 101, 103, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 222, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateTaiwanIp(): String {
        val ranges = listOf(1, 14, 27, 36, 42, 43, 49, 58, 59, 60, 61, 101, 103, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 222, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateEuropeIp(): String {
        val ranges = listOf(2, 5, 31, 37, 46, 62, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 109, 176, 178, 185, 188, 193, 194, 195, 212, 213, 217)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateAsiaIp(): String {
        val ranges = listOf(1, 14, 27, 36, 39, 42, 43, 49, 58, 59, 60, 61, 101, 103, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 133, 150, 153, 157, 163, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 222, 223)
        val first = ranges.random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
    
    private fun generateGlobalIp(): String {
        val first = listOf((1..9).toList(), (11..126).toList(), (128..191).toList(), (192..223).toList()).flatten().random()
        return "$first.${Random.nextInt(256)}.${Random.nextInt(256)}.${Random.nextInt(1, 255)}"
    }
}

/**
 * 生成的指纹数据
 */
data class GeneratedFingerprint(
    val userAgent: String,
    val platform: String,
    val vendor: String,
    val language: String,
    val timezone: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val colorDepth: Int,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val canvasNoise: Float,
    val audioNoise: Float,
    val webglVendor: String,
    val webglRenderer: String
)
