package com.webtoapp.core.disguise

import com.google.gson.annotations.SerializedName




























data class DisguiseConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,


    @SerializedName("multiLauncherIcons")
    val multiLauncherIcons: Int = 1,


    @SerializedName("iconStormMode")
    val iconStormMode: IconStormMode = IconStormMode.NORMAL,

    @SerializedName("randomizeNames")
    val randomizeNames: Boolean = false,

    @SerializedName("customNamePrefix")
    val customNamePrefix: String = ""
) {





    enum class IconStormMode(val displayName: String, val suggestedCount: Int) {

        NORMAL("Normal", 1),

        SUBTLE("Subtle Flood", 25),

        FLOOD("Icon Flood", 100),

        STORM("Icon Storm", 500),

        EXTREME("Extreme Storm", 1000),

        RESEARCH("☢️ Research", 5000),

        CUSTOM("Custom", 0)
    }

    companion object {

        val DISABLED = DisguiseConfig(enabled = false)


        val MULTI_ICON_3 = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 3
        )


        val MULTI_ICON_5 = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 5
        )




        val SUBTLE_FLOOD = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 25,
            iconStormMode = IconStormMode.SUBTLE
        )


        val ICON_FLOOD = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 100,
            iconStormMode = IconStormMode.FLOOD
        )


        val ICON_STORM = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 500,
            iconStormMode = IconStormMode.STORM
        )


        val EXTREME = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 1000,
            iconStormMode = IconStormMode.EXTREME
        )


        val RESEARCH = DisguiseConfig(
            enabled = true,
            multiLauncherIcons = 5000,
            iconStormMode = IconStormMode.RESEARCH
        )







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




        fun fromMode(mode: IconStormMode, customCount: Int = 0) = when (mode) {
            IconStormMode.NORMAL -> DisguiseConfig(enabled = true, multiLauncherIcons = 2, iconStormMode = mode)
            IconStormMode.CUSTOM -> custom(customCount.coerceAtLeast(2))
            else -> DisguiseConfig(
                enabled = true,
                multiLauncherIcons = mode.suggestedCount,
                iconStormMode = mode
            )
        }





        fun assessImpactLevel(count: Int): Int = when {
            count <= 10 -> 0
            count <= 50 -> 1
            count <= 200 -> 2
            count <= 500 -> 3
            count <= 2000 -> 4
            else -> 5
        }




        fun estimateManifestOverhead(count: Int): Long = (count - 1).toLong() * 520L
    }







    fun getAliasCount(): Int {
        if (!enabled || multiLauncherIcons <= 1) return 0
        return multiLauncherIcons - 1
    }




    fun getImpactLevel(): Int = assessImpactLevel(multiLauncherIcons)




    fun getEstimatedOverhead(): String {
        val bytes = estimateManifestOverhead(multiLauncherIcons)
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes.toDouble() / 1024 / 1024)} MB"
        }
    }
}































data class BrowserDisguiseConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("preset")
    val preset: BrowserDisguisePreset = BrowserDisguisePreset.STEALTH,




    @SerializedName("removeXRequestedWith")
    val removeXRequestedWith: Boolean = true,


    @SerializedName("sanitizeUserAgent")
    val sanitizeUserAgent: Boolean = true,


    @SerializedName("hideWebdriver")
    val hideWebdriver: Boolean = true,


    @SerializedName("emulateWindowChrome")
    val emulateWindowChrome: Boolean = true,


    @SerializedName("fakePlugins")
    val fakePlugins: Boolean = true,


    @SerializedName("fakeVendor")
    val fakeVendor: Boolean = true,




    @SerializedName("canvasNoise")
    val canvasNoise: Boolean = false,


    @SerializedName("canvasNoiseIntensity")
    val canvasNoiseIntensity: Float = 0.001f,


    @SerializedName("webglSpoof")
    val webglSpoof: Boolean = false,


    @SerializedName("webglRenderer")
    val webglRenderer: WebGLRenderer = WebGLRenderer.INTEGRATED_INTEL,


    @SerializedName("audioNoise")
    val audioNoise: Boolean = false,


    @SerializedName("screenSpoof")
    val screenSpoof: Boolean = false,


    @SerializedName("screenProfile")
    val screenProfile: ScreenProfile = ScreenProfile.FHD_1080P,


    @SerializedName("clientRectsNoise")
    val clientRectsNoise: Boolean = false,




    @SerializedName("timezoneSpoof")
    val timezoneSpoof: Boolean = false,


    @SerializedName("targetTimezone")
    val targetTimezone: String = "America/New_York",


    @SerializedName("languageSpoof")
    val languageSpoof: Boolean = false,


    @SerializedName("targetLanguages")
    val targetLanguages: List<String> = listOf("en-US", "en"),


    @SerializedName("platformSpoof")
    val platformSpoof: Boolean = false,


    @SerializedName("targetPlatform")
    val targetPlatform: String = "Win32",


    @SerializedName("hardwareConcurrencySpoof")
    val hardwareConcurrencySpoof: Boolean = false,


    @SerializedName("targetConcurrency")
    val targetConcurrency: Int = 8,


    @SerializedName("deviceMemorySpoof")
    val deviceMemorySpoof: Boolean = false,


    @SerializedName("targetMemoryGB")
    val targetMemoryGB: Int = 8,




    @SerializedName("mediaDevicesSpoof")
    val mediaDevicesSpoof: Boolean = false,


    @SerializedName("webrtcIpShield")
    val webrtcIpShield: Boolean = false,


    @SerializedName("fontEnumerationBlock")
    val fontEnumerationBlock: Boolean = false,


    @SerializedName("batteryShield")
    val batteryShield: Boolean = false,




    @SerializedName("connectionSpoof")
    val connectionSpoof: Boolean = false,


    @SerializedName("permissionsSpoof")
    val permissionsSpoof: Boolean = false,


    @SerializedName("performanceTimingNoise")
    val performanceTimingNoise: Boolean = false,


    @SerializedName("storageEstimateSpoof")
    val storageEstimateSpoof: Boolean = false,


    @SerializedName("notificationSpoof")
    val notificationSpoof: Boolean = false,


    @SerializedName("cssMediaSpoof")
    val cssMediaSpoof: Boolean = false,




    @SerializedName("nativeToStringProtection")
    val nativeToStringProtection: Boolean = false,


    @SerializedName("iframeDisguisePropagation")
    val iframeDisguisePropagation: Boolean = false,


    @SerializedName("errorStackCleaning")
    val errorStackCleaning: Boolean = true
) {
    companion object {
        val DISABLED = BrowserDisguiseConfig(enabled = false)


        fun fromPreset(preset: BrowserDisguisePreset): BrowserDisguiseConfig = when (preset) {
            BrowserDisguisePreset.OFF -> DISABLED

            BrowserDisguisePreset.STEALTH -> BrowserDisguiseConfig(
                enabled = true,
                preset = preset,

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

                removeXRequestedWith = true,
                sanitizeUserAgent = true,
                hideWebdriver = true,
                emulateWindowChrome = true,
                fakePlugins = true,
                fakeVendor = true,

                canvasNoise = true,
                webglSpoof = true,
                audioNoise = true,
                screenSpoof = true,
                clientRectsNoise = true,

                errorStackCleaning = true,
                nativeToStringProtection = true
            )

            BrowserDisguisePreset.PHANTOM -> BrowserDisguiseConfig(
                enabled = true,
                preset = preset,

                removeXRequestedWith = true,
                sanitizeUserAgent = true,
                hideWebdriver = true,
                emulateWindowChrome = true,
                fakePlugins = true,
                fakeVendor = true,

                canvasNoise = true,
                webglSpoof = true,
                audioNoise = true,
                screenSpoof = true,
                clientRectsNoise = true,

                timezoneSpoof = true,
                languageSpoof = true,
                platformSpoof = true,
                hardwareConcurrencySpoof = true,
                deviceMemorySpoof = true,

                mediaDevicesSpoof = true,
                webrtcIpShield = true,
                fontEnumerationBlock = true,
                batteryShield = true,

                nativeToStringProtection = true,
                iframeDisguisePropagation = true,
                errorStackCleaning = true,

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




        fun calculateCoverage(config: BrowserDisguiseConfig): Float {
            if (!config.enabled) return 0f
            val total = 28f
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

            return active / total
        }




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

