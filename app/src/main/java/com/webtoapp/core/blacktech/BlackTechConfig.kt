package com.webtoapp.core.blacktech

import com.google.gson.annotations.SerializedName

/**
 * Black-tech feature configuration.
 * 
 * Standalone module for advanced/high-risk feature toggles.
 * Separated from forced-run features and can be enabled independently.
 * 
 * v2.0 additions:
 * - Network controls (hotspot, disconnect)
 * - Nuclear mode (one-tap full enable)
 * - Stealth mode (one-tap hide)
 * - Custom alert sequence
 * - Keep screen awake
 */
data class BlackTechConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,                     // Enable black-tech features
    
    // ===== Volume controls =====
    @SerializedName("forceMaxVolume")
    val forceMaxVolume: Boolean = false,              // Force max volume
    @SerializedName("forceMuteMode")
    val forceMuteMode: Boolean = false,               // Force mute mode
    @SerializedName("forceBlockVolumeKeys")
    val forceBlockVolumeKeys: Boolean = false,        // Block volume keys
    
    // ===== Vibration and flashlight =====
    @SerializedName("forceMaxVibration")
    val forceMaxVibration: Boolean = false,           // Force max vibration
    @SerializedName("forceFlashlight")
    val forceFlashlight: Boolean = false,             // Force flashlight on
    @SerializedName("flashlightStrobeMode")
    val flashlightStrobeMode: Boolean = false,        // Flashlight strobe mode
    
    // Advanced flashlight modes (used with forceFlashlight)
    @SerializedName("flashlightMorseMode")
    val flashlightMorseMode: Boolean = false,         // Morse flashlight mode
    @SerializedName("flashlightMorseText")
    val flashlightMorseText: String = "",             // Morse text payload
    @SerializedName("flashlightMorseUnitMs")
    val flashlightMorseUnitMs: Int = 200,             // Morse time unit (ms)
    @SerializedName("flashlightSosMode")
    val flashlightSosMode: Boolean = false,           // SOS flashlight mode
    @SerializedName("flashlightHeartbeatMode")
    val flashlightHeartbeatMode: Boolean = false,     // Heartbeat flashlight mode
    @SerializedName("flashlightBreathingMode")
    val flashlightBreathingMode: Boolean = false,     // Breathing flashlight mode
    @SerializedName("flashlightEmergencyMode")
    val flashlightEmergencyMode: Boolean = false,     // Emergency triple-flash mode
    
    // ===== Custom alert sequence =====
    @SerializedName("customAlarmEnabled")
    val customAlarmEnabled: Boolean = false,          // Enable custom alert
    @SerializedName("customAlarmPattern")
    val customAlarmPattern: String = "",              // Custom rhythm (comma-separated: on,off,... in ms)
    @SerializedName("customAlarmVibSync")
    val customAlarmVibSync: Boolean = true,           // Sync vibration with flash
    
    // ===== System controls =====
    @SerializedName("forceAirplaneMode")
    val forceAirplaneMode: Boolean = false,           // Force airplane mode (system permission required)
    @SerializedName("forceMaxPerformance")
    val forceMaxPerformance: Boolean = false,         // Force max performance (high CPU/GPU usage)
    @SerializedName("forceBlockPowerKey")
    val forceBlockPowerKey: Boolean = false,          // Block power key
    
    // ===== Screen controls =====
    @SerializedName("forceBlackScreen")
    val forceBlackScreen: Boolean = false,            // Force full black screen
    @SerializedName("forceScreenRotation")
    val forceScreenRotation: Boolean = false,         // Force continuous rotation
    @SerializedName("forceBlockTouch")
    val forceBlockTouch: Boolean = false,             // Block touch input
    @SerializedName("forceScreenAwake")
    val forceScreenAwake: Boolean = false,            // Keep screen always on
    
    // ===== Network controls (v2.0) =====
    @SerializedName("forceWifiHotspot")
    val forceWifiHotspot: Boolean = false,            // Force Wi-Fi hotspot on
    @SerializedName("hotspotSsid")
    val hotspotSsid: String = "WebToApp_AP",          // Hotspot SSID
    @SerializedName("hotspotPassword")
    val hotspotPassword: String = "12345678",         // Hotspot password (min 8 chars)
    @SerializedName("forceDisableWifi")
    val forceDisableWifi: Boolean = false,            // Force Wi-Fi off
    @SerializedName("forceDisableBluetooth")
    val forceDisableBluetooth: Boolean = false,       // Force Bluetooth off
    @SerializedName("forceDisableMobileData")
    val forceDisableMobileData: Boolean = false,      // Force mobile data off
    
    // ===== Special modes (v2.0) =====
    @SerializedName("nuclearMode")
    val nuclearMode: Boolean = false,                 // Nuclear mode: enable all aggressive features
    @SerializedName("stealthMode")
    val stealthMode: Boolean = false                  // Stealth mode: mute + black screen + disconnect + block touch
) {
    companion object {
        /** Disabled preset */
        val DISABLED = BlackTechConfig(enabled = false)
        
        /** Silent mode preset */
        val SILENT_MODE = BlackTechConfig(
            enabled = true,
            forceMuteMode = true,
            forceBlockVolumeKeys = true
        )
        
        /** Alert mode preset */
        val ALARM_MODE = BlackTechConfig(
            enabled = true,
            forceMaxVolume = true,
            forceMaxVibration = true,
            forceFlashlight = true,
            flashlightStrobeMode = true
        )
        
        /** Morse signal preset (SOS) */
        val SOS_SIGNAL = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightSosMode = true
        )
        
        /** Custom Morse signal preset */
        fun morseSignal(text: String, unitMs: Int = 200) = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightMorseMode = true,
            flashlightMorseText = text,
            flashlightMorseUnitMs = unitMs
        )
        
        // ===== v2.0 added presets =====
        
        /**
         * 💣 Nuclear mode
         * 
         * One tap to enable all aggressive features:
         * max volume + max vibration + strobe + max performance + block keys
         */
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
        
        /**
         * 🥷 Stealth mode
         * 
         * One tap to enter a full stealth state:
         * mute + black screen + block touch + disable Wi-Fi + disable Bluetooth
         */
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
        
        /**
         * 📡 Hotspot mode
         * 
         * Enable hotspot + keep screen awake for network sharing
         */
        fun hotspotMode(ssid: String = "WebToApp_AP", password: String = "12345678") = BlackTechConfig(
            enabled = true,
            forceWifiHotspot = true,
            hotspotSsid = ssid,
            hotspotPassword = password,
            forceScreenAwake = true
        )
        
        /**
         * 🚨 Custom alert mode
         * 
         * @param pattern Rhythm sequence (comma-separated: "onMs,offMs,onMs,offMs...")
         *                Example: "100,100,100,100,100,800" = triple quick flash + long off
         * @param withVibration Whether to sync vibration
         */
        fun customAlarm(pattern: String, withVibration: Boolean = true) = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            customAlarmEnabled = true,
            customAlarmPattern = pattern,
            customAlarmVibSync = withVibration
        )
    }
}
