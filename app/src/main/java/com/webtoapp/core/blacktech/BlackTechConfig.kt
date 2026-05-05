package com.webtoapp.core.blacktech

import com.google.gson.annotations.SerializedName














data class BlackTechConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,


    @SerializedName("forceMaxVolume")
    val forceMaxVolume: Boolean = false,
    @SerializedName("forceMuteMode")
    val forceMuteMode: Boolean = false,
    @SerializedName("forceBlockVolumeKeys")
    val forceBlockVolumeKeys: Boolean = false,


    @SerializedName("forceMaxVibration")
    val forceMaxVibration: Boolean = false,
    @SerializedName("forceFlashlight")
    val forceFlashlight: Boolean = false,
    @SerializedName("flashlightStrobeMode")
    val flashlightStrobeMode: Boolean = false,


    @SerializedName("flashlightMorseMode")
    val flashlightMorseMode: Boolean = false,
    @SerializedName("flashlightMorseText")
    val flashlightMorseText: String = "",
    @SerializedName("flashlightMorseUnitMs")
    val flashlightMorseUnitMs: Int = 200,
    @SerializedName("flashlightSosMode")
    val flashlightSosMode: Boolean = false,
    @SerializedName("flashlightHeartbeatMode")
    val flashlightHeartbeatMode: Boolean = false,
    @SerializedName("flashlightBreathingMode")
    val flashlightBreathingMode: Boolean = false,
    @SerializedName("flashlightEmergencyMode")
    val flashlightEmergencyMode: Boolean = false,


    @SerializedName("customAlarmEnabled")
    val customAlarmEnabled: Boolean = false,
    @SerializedName("customAlarmPattern")
    val customAlarmPattern: String = "",
    @SerializedName("customAlarmVibSync")
    val customAlarmVibSync: Boolean = true,


    @SerializedName("forceAirplaneMode")
    val forceAirplaneMode: Boolean = false,
    @SerializedName("forceMaxPerformance")
    val forceMaxPerformance: Boolean = false,
    @SerializedName("forceBlockPowerKey")
    val forceBlockPowerKey: Boolean = false,


    @SerializedName("forceBlackScreen")
    val forceBlackScreen: Boolean = false,
    @SerializedName("forceScreenRotation")
    val forceScreenRotation: Boolean = false,
    @SerializedName("forceBlockTouch")
    val forceBlockTouch: Boolean = false,
    @SerializedName("forceScreenAwake")
    val forceScreenAwake: Boolean = false,


    @SerializedName("forceWifiHotspot")
    val forceWifiHotspot: Boolean = false,
    @SerializedName("hotspotSsid")
    val hotspotSsid: String = "WebToApp_AP",
    @SerializedName("hotspotPassword")
    val hotspotPassword: String = "12345678",
    @SerializedName("forceDisableWifi")
    val forceDisableWifi: Boolean = false,
    @SerializedName("forceDisableBluetooth")
    val forceDisableBluetooth: Boolean = false,
    @SerializedName("forceDisableMobileData")
    val forceDisableMobileData: Boolean = false,


    @SerializedName("nuclearMode")
    val nuclearMode: Boolean = false,
    @SerializedName("stealthMode")
    val stealthMode: Boolean = false
) {
    companion object {

        val DISABLED = BlackTechConfig(enabled = false)


        val SILENT_MODE = BlackTechConfig(
            enabled = true,
            forceMuteMode = true,
            forceBlockVolumeKeys = true
        )


        val ALARM_MODE = BlackTechConfig(
            enabled = true,
            forceMaxVolume = true,
            forceMaxVibration = true,
            forceFlashlight = true,
            flashlightStrobeMode = true
        )


        val SOS_SIGNAL = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightSosMode = true
        )


        fun morseSignal(text: String, unitMs: Int = 200) = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightMorseMode = true,
            flashlightMorseText = text,
            flashlightMorseUnitMs = unitMs
        )









        val NUCLEAR_MODE = BlackTechConfig(
            enabled = true,
            nuclearMode = true,
            forceMaxVolume = true,
            forceMaxVibration = true,
            forceFlashlight = true,
            flashlightStrobeMode = true,
            forceMaxPerformance = true,
            forceBlockVolumeKeys = true,
            forceBlockPowerKey = true,
            forceScreenAwake = true
        )







        val STEALTH_MODE = BlackTechConfig(
            enabled = true,
            stealthMode = true,
            forceMuteMode = true,
            forceBlockVolumeKeys = true,
            forceBlockPowerKey = true,
            forceBlackScreen = true,
            forceBlockTouch = true,
            forceDisableWifi = true,
            forceDisableBluetooth = true
        )






        fun hotspotMode(ssid: String = "WebToApp_AP", password: String = "12345678") = BlackTechConfig(
            enabled = true,
            forceWifiHotspot = true,
            hotspotSsid = ssid,
            hotspotPassword = password,
            forceScreenAwake = true
        )








        fun customAlarm(pattern: String, withVibration: Boolean = true) = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            customAlarmEnabled = true,
            customAlarmPattern = pattern,
            customAlarmVibSync = withVibration
        )
    }
}
