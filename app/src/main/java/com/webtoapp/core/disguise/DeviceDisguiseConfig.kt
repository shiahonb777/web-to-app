package com.webtoapp.core.disguise

import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.UserAgentVersions

































data class DeviceDisguiseConfig(
    val enabled: Boolean = false,
    val deviceType: DeviceType = DeviceType.PHONE,
    val deviceOS: DeviceOS = DeviceOS.ANDROID,
    val deviceBrand: DeviceBrand = DeviceBrand.SAMSUNG,
    val deviceModel: String = "",
    val deviceModelName: String = "",
    val customUserAgent: String? = null,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val pixelDensity: Float = 0f,
    val isDesktopViewport: Boolean = false,
    val isCustomDevice: Boolean = false
) {



    fun toUserAgentMode(): UserAgentMode {
        if (!enabled) return UserAgentMode.DEFAULT
        if (customUserAgent != null) return UserAgentMode.CUSTOM
        return when (deviceType) {
            DeviceType.PHONE -> when (deviceOS) {
                DeviceOS.IOS -> UserAgentMode.SAFARI_MOBILE
                else -> UserAgentMode.CHROME_MOBILE
            }
            DeviceType.TABLET -> when (deviceOS) {
                DeviceOS.IOS -> UserAgentMode.SAFARI_MOBILE
                else -> UserAgentMode.CHROME_MOBILE
            }
            DeviceType.DESKTOP, DeviceType.LAPTOP -> when (deviceOS) {
                DeviceOS.MACOS -> UserAgentMode.SAFARI_DESKTOP
                DeviceOS.LINUX -> UserAgentMode.FIREFOX_DESKTOP
                else -> UserAgentMode.CHROME_DESKTOP
            }
            DeviceType.WATCH -> UserAgentMode.SAFARI_MOBILE
            DeviceType.TV -> UserAgentMode.CHROME_MOBILE
        }
    }




    fun generateUserAgent(): String {
        if (!enabled) return ""
        if (!customUserAgent.isNullOrBlank()) return customUserAgent

        val cv = UserAgentVersions.CHROME
        val sv = UserAgentVersions.SAFARI
        val fv = UserAgentVersions.FIREFOX
        val model = deviceModel.ifBlank { getDefaultModel() }

        return when (deviceType) {
            DeviceType.PHONE -> when (deviceOS) {
                DeviceOS.IOS -> {
                    val iosVer = getIOSVersion()
                    "Mozilla/5.0 (iPhone; CPU iPhone OS ${iosVer.replace(".", "_")} like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$sv.0 Mobile/15E148 Safari/604.1"
                }
                DeviceOS.HARMONYOS -> "Mozilla/5.0 (Linux; Android 14; $model; HMSCore 6.14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Mobile Safari/537.36"
                else -> "Mozilla/5.0 (Linux; Android 15; $model) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Mobile Safari/537.36"
            }
            DeviceType.TABLET -> when (deviceOS) {
                DeviceOS.IOS -> "Mozilla/5.0 (iPad; CPU OS ${getIOSVersion().replace(".", "_")} like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$sv.0 Mobile/15E148 Safari/604.1"
                else -> "Mozilla/5.0 (Linux; Android 15; $model) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
            }
            DeviceType.DESKTOP -> when (deviceOS) {
                DeviceOS.WINDOWS -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
                DeviceOS.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$sv.0 Safari/605.1.15"
                DeviceOS.LINUX -> "Mozilla/5.0 (X11; Linux x86_64; rv:$fv.0) Gecko/20100101 Firefox/$fv.0"
                DeviceOS.CHROMEOS -> "Mozilla/5.0 (X11; CrOS x86_64 15217.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
                else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
            }
            DeviceType.LAPTOP -> when (deviceOS) {
                DeviceOS.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$sv.0 Safari/605.1.15"
                DeviceOS.LINUX -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
                DeviceOS.CHROMEOS -> "Mozilla/5.0 (X11; CrOS x86_64 15217.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
                else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
            }
            DeviceType.WATCH -> when (deviceOS) {
                DeviceOS.WATCHOS -> "Mozilla/5.0 (Watch; CPU Watch OS 12_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$sv.0 Mobile/15E148 Safari/604.1"
                DeviceOS.WEAROS -> "Mozilla/5.0 (Linux; Android 15; $model) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Mobile Safari/537.36"
                else -> "Mozilla/5.0 (Linux; Android 15; $model) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Mobile Safari/537.36"
            }
            DeviceType.TV -> "Mozilla/5.0 (SMART-TV; Linux; Tizen 9.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$cv.0.0.0 Safari/537.36"
        }
    }

    private fun getDefaultModel(): String = when (deviceBrand) {
        DeviceBrand.SAMSUNG -> "SM-S938B"
        DeviceBrand.GOOGLE_PIXEL -> "Pixel 10 Pro"
        DeviceBrand.HUAWEI -> "ALT-AL10"
        DeviceBrand.XIAOMI -> "25015PN20G"
        DeviceBrand.OPPO -> "PHH110"
        DeviceBrand.VIVO -> "V2432A"
        DeviceBrand.ONEPLUS -> "CPH2691"
        DeviceBrand.SONY -> "XQ-EC72"
        DeviceBrand.MOTOROLA -> "XT2451-2"
        DeviceBrand.NOTHING -> "A073"
        DeviceBrand.HONOR -> "FNE-AN10"
        DeviceBrand.APPLE -> if (deviceType == DeviceType.TABLET) "iPad" else "iPhone"
        DeviceBrand.GENERIC_WINDOWS -> "Windows PC"
        DeviceBrand.GENERIC_MAC -> "Mac"
        DeviceBrand.GENERIC_LINUX -> "Linux PC"
        DeviceBrand.MICROSOFT_SURFACE -> "Surface Pro"
        DeviceBrand.LENOVO -> "ThinkPad X1"
        DeviceBrand.DELL -> "XPS 15"
        DeviceBrand.HP -> "Spectre x360"
        DeviceBrand.APPLE_WATCH -> "Apple Watch"
        DeviceBrand.SAMSUNG_WATCH -> "SM-L500"
        DeviceBrand.GOOGLE_WATCH -> "Pixel Watch 3"
        DeviceBrand.HUAWEI_WATCH -> "WATCH GT 5 Pro"
        DeviceBrand.SAMSUNG_TAB -> "SM-X910N"
        DeviceBrand.IPAD -> "iPad16,5"
        DeviceBrand.HUAWEI_PAD -> "GOT-W29"
        DeviceBrand.XIAOMI_PAD -> "Xiaomi Pad 7 Pro"
        DeviceBrand.CHROMEBOOK -> "Chromebook"
    }

    private fun getIOSVersion(): String = "19.0"




    fun requiresDesktopViewport(): Boolean {
        if (!enabled) return false
        if (isDesktopViewport) return true
        return deviceType in listOf(DeviceType.DESKTOP, DeviceType.LAPTOP)
    }
}




enum class DeviceType(val emoji: String, val displayOrder: Int) {
    PHONE("📱", 0),
    TABLET("📟", 1),
    DESKTOP("🖥️", 2),
    LAPTOP("💻", 3),
    WATCH("⌚", 4),
    TV("📺", 5)
}




enum class DeviceOS(val displayName: String) {
    ANDROID("Android"),
    IOS("iOS"),
    HARMONYOS("HarmonyOS"),
    WINDOWS("Windows"),
    MACOS("macOS"),
    LINUX("Linux"),
    CHROMEOS("ChromeOS"),
    WATCHOS("watchOS"),
    WEAROS("Wear OS"),
    TVOS("tvOS")
}




enum class DeviceBrand(
    val displayName: String,
    val supportedTypes: Set<DeviceType>,
    val supportedOS: Set<DeviceOS>
) {

    SAMSUNG("Samsung", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    GOOGLE_PIXEL("Google Pixel", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    HUAWEI("Huawei", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID, DeviceOS.HARMONYOS)),
    XIAOMI("Xiaomi", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    OPPO("OPPO", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    VIVO("vivo", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    ONEPLUS("OnePlus", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    SONY("Sony", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    MOTOROLA("Motorola", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    NOTHING("Nothing", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    HONOR("HONOR", setOf(DeviceType.PHONE), setOf(DeviceOS.ANDROID)),
    APPLE("Apple", setOf(DeviceType.PHONE), setOf(DeviceOS.IOS)),


    SAMSUNG_TAB("Samsung Tab", setOf(DeviceType.TABLET), setOf(DeviceOS.ANDROID)),
    IPAD("iPad", setOf(DeviceType.TABLET), setOf(DeviceOS.IOS)),
    HUAWEI_PAD("Huawei MatePad", setOf(DeviceType.TABLET), setOf(DeviceOS.ANDROID, DeviceOS.HARMONYOS)),
    XIAOMI_PAD("Xiaomi Pad", setOf(DeviceType.TABLET), setOf(DeviceOS.ANDROID)),


    GENERIC_WINDOWS("Windows PC", setOf(DeviceType.DESKTOP, DeviceType.LAPTOP), setOf(DeviceOS.WINDOWS)),
    GENERIC_MAC("Mac", setOf(DeviceType.DESKTOP, DeviceType.LAPTOP), setOf(DeviceOS.MACOS)),
    GENERIC_LINUX("Linux PC", setOf(DeviceType.DESKTOP, DeviceType.LAPTOP), setOf(DeviceOS.LINUX)),
    MICROSOFT_SURFACE("Surface", setOf(DeviceType.LAPTOP), setOf(DeviceOS.WINDOWS)),
    LENOVO("Lenovo", setOf(DeviceType.LAPTOP), setOf(DeviceOS.WINDOWS, DeviceOS.LINUX)),
    DELL("Dell", setOf(DeviceType.LAPTOP), setOf(DeviceOS.WINDOWS, DeviceOS.LINUX)),
    HP("HP", setOf(DeviceType.LAPTOP), setOf(DeviceOS.WINDOWS)),
    CHROMEBOOK("Chromebook", setOf(DeviceType.LAPTOP), setOf(DeviceOS.CHROMEOS)),


    APPLE_WATCH("Apple Watch", setOf(DeviceType.WATCH), setOf(DeviceOS.WATCHOS)),
    SAMSUNG_WATCH("Samsung Watch", setOf(DeviceType.WATCH), setOf(DeviceOS.WEAROS)),
    GOOGLE_WATCH("Pixel Watch", setOf(DeviceType.WATCH), setOf(DeviceOS.WEAROS)),
    HUAWEI_WATCH("Huawei Watch", setOf(DeviceType.WATCH), setOf(DeviceOS.ANDROID));

    companion object {
        fun getBrandsForType(type: DeviceType): List<DeviceBrand> =
            entries.filter { type in it.supportedTypes }.sortedBy { it.displayName }
    }
}







object DevicePresets {

    data class DevicePreset(
        val name: String,
        val type: DeviceType,
        val os: DeviceOS,
        val brand: DeviceBrand,
        val model: String,
        val modelName: String,
        val screenWidth: Int,
        val screenHeight: Int,
        val pixelDensity: Float
    ) {
        fun toConfig(): DeviceDisguiseConfig = DeviceDisguiseConfig(
            enabled = true,
            deviceType = type,
            deviceOS = os,
            deviceBrand = brand,
            deviceModel = model,
            deviceModelName = modelName,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            pixelDensity = pixelDensity,
            isDesktopViewport = type in listOf(DeviceType.DESKTOP, DeviceType.LAPTOP)
        )
    }




    val PHONES = listOf(

        DevicePreset("iPhone 17 Pro Max", DeviceType.PHONE, DeviceOS.IOS, DeviceBrand.APPLE, "iPhone18,2", "iPhone 17 Pro Max", 440, 956, 3f),
        DevicePreset("iPhone 17 Pro", DeviceType.PHONE, DeviceOS.IOS, DeviceBrand.APPLE, "iPhone18,1", "iPhone 17 Pro", 402, 874, 3f),
        DevicePreset("iPhone Air", DeviceType.PHONE, DeviceOS.IOS, DeviceBrand.APPLE, "iPhone18,3", "iPhone Air", 393, 852, 3f),


        DevicePreset("Galaxy S26 Ultra", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.SAMSUNG, "SM-S938B", "Galaxy S26 Ultra", 412, 915, 3.5f),
        DevicePreset("Galaxy S26+", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.SAMSUNG, "SM-S936B", "Galaxy S26+", 412, 892, 3f),
        DevicePreset("Galaxy S26", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.SAMSUNG, "SM-S931B", "Galaxy S26", 360, 780, 3f),


        DevicePreset("Pixel 10 Pro XL", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.GOOGLE_PIXEL, "Pixel 10 Pro XL", "Pixel 10 Pro XL", 412, 915, 3.5f),
        DevicePreset("Pixel 10 Pro", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.GOOGLE_PIXEL, "Pixel 10 Pro", "Pixel 10 Pro", 412, 892, 2.75f),
        DevicePreset("Pixel 10a", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.GOOGLE_PIXEL, "Pixel 10a", "Pixel 10a", 412, 892, 2.625f),


        DevicePreset("Xiaomi 17 Ultra", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.XIAOMI, "25015PN20G", "Xiaomi 17 Ultra", 412, 915, 3.5f),
        DevicePreset("Xiaomi 17", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.XIAOMI, "25015PN10G", "Xiaomi 17", 393, 873, 3f),


        DevicePreset("Pura 90 Pro", DeviceType.PHONE, DeviceOS.HARMONYOS, DeviceBrand.HUAWEI, "ALT-AL10", "Pura 90 Pro", 412, 915, 3.5f),
        DevicePreset("Mate 70 Pro+", DeviceType.PHONE, DeviceOS.HARMONYOS, DeviceBrand.HUAWEI, "BRN-AL00", "Mate 70 Pro+", 412, 915, 3f),


        DevicePreset("OnePlus 15", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.ONEPLUS, "CPH2691", "OnePlus 15", 412, 915, 3.5f),
        DevicePreset("OnePlus 15T", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.ONEPLUS, "CPH2711", "OnePlus 15T", 393, 873, 3f),


        DevicePreset("Find X9 Ultra", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.OPPO, "PHH110", "Find X9 Ultra", 412, 915, 3.5f),
        DevicePreset("Find X9 Pro", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.OPPO, "PHG110", "Find X9 Pro", 412, 915, 3f),


        DevicePreset("X300 Ultra", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.VIVO, "V2432A", "X300 Ultra", 412, 915, 3.5f),
        DevicePreset("X300 Pro", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.VIVO, "V2431A", "X300 Pro", 412, 892, 3f),


        DevicePreset("Magic 8 Pro", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.HONOR, "FNE-AN10", "Magic 8 Pro", 412, 915, 3.5f),


        DevicePreset("Xperia 1 VII", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.SONY, "XQ-EC72", "Xperia 1 VII", 411, 960, 3.5f),


        DevicePreset("Nothing Phone (3)", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.NOTHING, "A073", "Nothing Phone (3)", 412, 915, 2.75f),


        DevicePreset("Edge 50 Ultra", DeviceType.PHONE, DeviceOS.ANDROID, DeviceBrand.MOTOROLA, "XT2451-2", "Edge 50 Ultra", 412, 915, 3f)
    )




    val TABLETS = listOf(

        DevicePreset("iPad Pro 13\" M5", DeviceType.TABLET, DeviceOS.IOS, DeviceBrand.IPAD, "iPad17,1", "iPad Pro 13-inch (M5)", 1032, 1376, 2f),
        DevicePreset("iPad Pro 11\" M5", DeviceType.TABLET, DeviceOS.IOS, DeviceBrand.IPAD, "iPad17,3", "iPad Pro 11-inch (M5)", 834, 1194, 2f),
        DevicePreset("iPad Air 13\" M3", DeviceType.TABLET, DeviceOS.IOS, DeviceBrand.IPAD, "iPad16,1", "iPad Air 13-inch (M3)", 1032, 1376, 2f),


        DevicePreset("Galaxy Tab S11 Ultra", DeviceType.TABLET, DeviceOS.ANDROID, DeviceBrand.SAMSUNG_TAB, "SM-X910N", "Galaxy Tab S11 Ultra", 1200, 1848, 2f),
        DevicePreset("Galaxy Tab S11", DeviceType.TABLET, DeviceOS.ANDROID, DeviceBrand.SAMSUNG_TAB, "SM-X810N", "Galaxy Tab S11", 1200, 1600, 2f),


        DevicePreset("Xiaomi Pad 7 Pro", DeviceType.TABLET, DeviceOS.ANDROID, DeviceBrand.XIAOMI_PAD, "25043RPACC", "Xiaomi Pad 7 Pro", 1200, 1840, 2f),


        DevicePreset("MatePad Pro 13.2\"", DeviceType.TABLET, DeviceOS.HARMONYOS, DeviceBrand.HUAWEI_PAD, "GOT-W29", "MatePad Pro 13.2\"", 1200, 1840, 2f)
    )




    val DESKTOPS = listOf(

        DevicePreset("Windows PC 1080p", DeviceType.DESKTOP, DeviceOS.WINDOWS, DeviceBrand.GENERIC_WINDOWS, "WIN-DESKTOP-FHD", "Windows Desktop 1080p", 1920, 1080, 1f),
        DevicePreset("Windows PC 1440p", DeviceType.DESKTOP, DeviceOS.WINDOWS, DeviceBrand.GENERIC_WINDOWS, "WIN-DESKTOP-QHD", "Windows Desktop 1440p", 2560, 1440, 1.5f),
        DevicePreset("Windows PC 4K", DeviceType.DESKTOP, DeviceOS.WINDOWS, DeviceBrand.GENERIC_WINDOWS, "WIN-DESKTOP-4K", "Windows Desktop 4K", 3840, 2160, 2f),

        DevicePreset("iMac 24\" M4", DeviceType.DESKTOP, DeviceOS.MACOS, DeviceBrand.GENERIC_MAC, "iMac21,1-M4", "iMac 24\" (M4)", 2560, 1440, 2f),

        DevicePreset("Linux Workstation", DeviceType.DESKTOP, DeviceOS.LINUX, DeviceBrand.GENERIC_LINUX, "LINUX-WS-FHD", "Linux Workstation", 1920, 1080, 1f)
    )




    val LAPTOPS = listOf(

        DevicePreset("MacBook Pro 16\" M5", DeviceType.LAPTOP, DeviceOS.MACOS, DeviceBrand.GENERIC_MAC, "MacBookPro19,1", "MacBook Pro 16\" (M5 Pro)", 1728, 1117, 2f),
        DevicePreset("MacBook Pro 14\" M5", DeviceType.LAPTOP, DeviceOS.MACOS, DeviceBrand.GENERIC_MAC, "MacBookPro19,3", "MacBook Pro 14\" (M5 Pro)", 1512, 982, 2f),
        DevicePreset("MacBook Air 15\" M4", DeviceType.LAPTOP, DeviceOS.MACOS, DeviceBrand.GENERIC_MAC, "MacBookAir16,1", "MacBook Air 15\" (M4)", 1710, 1112, 2f),

        DevicePreset("Surface Pro 11", DeviceType.LAPTOP, DeviceOS.WINDOWS, DeviceBrand.MICROSOFT_SURFACE, "Surface-Pro-11", "Surface Pro 11", 2880, 1920, 2f),

        DevicePreset("ThinkPad X1 Carbon G12", DeviceType.LAPTOP, DeviceOS.WINDOWS, DeviceBrand.LENOVO, "ThinkPad-X1C-G12", "ThinkPad X1 Carbon Gen 12", 1920, 1200, 1.5f),

        DevicePreset("Dell XPS 15 2025", DeviceType.LAPTOP, DeviceOS.WINDOWS, DeviceBrand.DELL, "XPS-15-9530", "Dell XPS 15 (2025)", 1920, 1200, 1.5f),

        DevicePreset("HP Spectre x360 16\"", DeviceType.LAPTOP, DeviceOS.WINDOWS, DeviceBrand.HP, "Spectre-x360-16", "HP Spectre x360 16\" (2025)", 2560, 1600, 2f),

        DevicePreset("Chromebook Plus", DeviceType.LAPTOP, DeviceOS.CHROMEOS, DeviceBrand.CHROMEBOOK, "Chromebook-Plus-2025", "Chromebook Plus (2025)", 1920, 1080, 1f)
    )




    val WATCHES = listOf(

        DevicePreset("Apple Watch Ultra 3", DeviceType.WATCH, DeviceOS.WATCHOS, DeviceBrand.APPLE_WATCH, "Watch8,1", "Apple Watch Ultra 3", 205, 251, 2f),
        DevicePreset("Apple Watch Series 11", DeviceType.WATCH, DeviceOS.WATCHOS, DeviceBrand.APPLE_WATCH, "Watch8,3", "Apple Watch Series 11", 198, 242, 2f),

        DevicePreset("Galaxy Watch8 Classic", DeviceType.WATCH, DeviceOS.WEAROS, DeviceBrand.SAMSUNG_WATCH, "SM-L500", "Galaxy Watch8 Classic", 187, 187, 2f),
        DevicePreset("Galaxy Watch Ultra", DeviceType.WATCH, DeviceOS.WEAROS, DeviceBrand.SAMSUNG_WATCH, "SM-L705", "Galaxy Watch Ultra (2025)", 187, 187, 2f),

        DevicePreset("Pixel Watch 3", DeviceType.WATCH, DeviceOS.WEAROS, DeviceBrand.GOOGLE_WATCH, "Pixel Watch 3", "Pixel Watch 3", 192, 192, 2f),

        DevicePreset("HUAWEI WATCH GT 5 Pro", DeviceType.WATCH, DeviceOS.ANDROID, DeviceBrand.HUAWEI_WATCH, "WATCH-GT5-PRO", "HUAWEI WATCH GT 5 Pro", 194, 194, 2f)
    )

    val ALL = PHONES + TABLETS + DESKTOPS + LAPTOPS + WATCHES







    fun getPresetsForType(type: DeviceType): List<DevicePreset> = when (type) {
        DeviceType.PHONE -> PHONES
        DeviceType.TABLET -> TABLETS
        DeviceType.DESKTOP -> DESKTOPS
        DeviceType.LAPTOP -> LAPTOPS
        DeviceType.WATCH -> WATCHES
        DeviceType.TV -> emptyList()
    }
}
