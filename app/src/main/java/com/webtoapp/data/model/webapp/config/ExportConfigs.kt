package com.webtoapp.data.model.webapp.config

enum class ApkArchitecture(
    val abiFilters: List<String>
) {
    UNIVERSAL(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")),
    ARM64(listOf("arm64-v8a", "x86_64")),
    ARM32(listOf("armeabi-v7a", "x86"));
    
    val displayName: String get() = when (this) {
        UNIVERSAL -> com.webtoapp.core.i18n.Strings.archUniversal
        ARM64 -> com.webtoapp.core.i18n.Strings.archArm64
        ARM32 -> com.webtoapp.core.i18n.Strings.archArm32
    }
    
    val description: String get() = when (this) {
        UNIVERSAL -> com.webtoapp.core.i18n.Strings.archUniversalDesc
        ARM64 -> com.webtoapp.core.i18n.Strings.archArm64Desc
        ARM32 -> com.webtoapp.core.i18n.Strings.archArm32Desc
    }
    
    companion object {
        fun fromName(name: String): ApkArchitecture {
            return entries.find { it.name == name } ?: UNIVERSAL
        }
    }
}

data class ApkExportConfig(
    val customPackageName: String? = null,
    val customVersionName: String? = null,
    val customVersionCode: Int? = null,
    val architecture: ApkArchitecture = ApkArchitecture.UNIVERSAL,
    val encryptionConfig: ApkEncryptionConfig = ApkEncryptionConfig(),
    val hardeningConfig: AppHardeningConfig = AppHardeningConfig(),
    val isolationConfig: com.webtoapp.core.isolation.IsolationConfig = com.webtoapp.core.isolation.IsolationConfig(),
    val backgroundRunEnabled: Boolean = false,
    val backgroundRunConfig: BackgroundRunExportConfig = BackgroundRunExportConfig(),
    val engineType: String = "SYSTEM_WEBVIEW",
    val deepLinkEnabled: Boolean = false,
    val customDeepLinkHosts: List<String> = emptyList(),
    val performanceOptimization: Boolean = false,
    val performanceConfig: PerformanceOptimizationConfig = PerformanceOptimizationConfig()
)

data class PerformanceOptimizationConfig(
    val compressImages: Boolean = true,
    val imageQuality: Int = 80,
    val convertToWebP: Boolean = true,
    val minifyCode: Boolean = true,
    val minifySvg: Boolean = true,
    val removeUnusedResources: Boolean = true,
    val parallelProcessing: Boolean = true,
    val enableCache: Boolean = true,
    val injectPreloadHints: Boolean = true,
    val injectLazyLoading: Boolean = true,
    val optimizeScripts: Boolean = true,
    val injectDnsPrefetch: Boolean = true,
    val injectPerformanceScript: Boolean = true
) {
    fun toOptimizerConfig(): com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig {
        return com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig(
            compressImages = compressImages,
            imageQuality = imageQuality,
            convertToWebP = convertToWebP,
            minifyCode = minifyCode,
            minifySvg = minifySvg,
            removeUnusedResources = removeUnusedResources,
            parallelProcessing = parallelProcessing,
            enableCache = enableCache,
            injectPreloadHints = injectPreloadHints,
            injectLazyLoading = injectLazyLoading,
            optimizeScripts = optimizeScripts,
            injectDnsPrefetch = injectDnsPrefetch,
            injectPerformanceScript = injectPerformanceScript
        )
    }
}

data class BackgroundRunExportConfig(
    val notificationTitle: String = "",
    val notificationContent: String = "",
    val showNotification: Boolean = true,
    val keepCpuAwake: Boolean = true
)

data class ApkEncryptionConfig(
    val enabled: Boolean = false,
    val encryptConfig: Boolean = true,
    val encryptHtml: Boolean = true,
    val encryptMedia: Boolean = false,
    val encryptSplash: Boolean = false,
    val encryptBgm: Boolean = false,
    val customPassword: String? = null,
    val enableIntegrityCheck: Boolean = true,
    val enableAntiDebug: Boolean = true,
    val enableAntiTamper: Boolean = true,
    val obfuscateStrings: Boolean = false,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD
) {
    enum class EncryptionLevel(val iterations: Int) {
        FAST(5000),
        STANDARD(10000),
        HIGH(50000),
        PARANOID(100000);
        
        val description: String get() = when (this) {
            FAST -> com.webtoapp.core.i18n.Strings.encryptLevelFast
            STANDARD -> com.webtoapp.core.i18n.Strings.encryptLevelStandard
            HIGH -> com.webtoapp.core.i18n.Strings.encryptLevelHigh
            PARANOID -> com.webtoapp.core.i18n.Strings.encryptLevelParanoid
        }
    }
    
    companion object {
        val DISABLED = ApkEncryptionConfig(enabled = false)
        
        val BASIC = ApkEncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = false,
            enableIntegrityCheck = true,
            enableAntiDebug = false,
            encryptionLevel = EncryptionLevel.STANDARD
        )
        
        val FULL = ApkEncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = true,
            encryptSplash = true,
            encryptBgm = true,
            enableIntegrityCheck = true,
            enableAntiDebug = true,
            enableAntiTamper = true,
            encryptionLevel = EncryptionLevel.HIGH
        )
        
        val MAXIMUM = ApkEncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = true,
            encryptSplash = true,
            encryptBgm = true,
            enableIntegrityCheck = true,
            enableAntiDebug = true,
            enableAntiTamper = true,
            obfuscateStrings = true,
            encryptionLevel = EncryptionLevel.PARANOID
        )
    }
    
    fun toEncryptionConfig(): com.webtoapp.core.crypto.EncryptionConfig {
        return com.webtoapp.core.crypto.EncryptionConfig(
            enabled = enabled,
            encryptConfig = encryptConfig,
            encryptHtml = encryptHtml,
            encryptMedia = encryptMedia,
            encryptSplash = encryptSplash,
            encryptBgm = encryptBgm,
            customPassword = customPassword,
            enableIntegrityCheck = enableIntegrityCheck,
            enableAntiDebug = enableAntiDebug,
            enableAntiTamper = enableAntiTamper,
            enableRootDetection = false,
            enableEmulatorDetection = false,
            obfuscateStrings = obfuscateStrings,
            encryptionLevel = when (encryptionLevel) {
                EncryptionLevel.FAST -> com.webtoapp.core.crypto.EncryptionLevel.FAST
                EncryptionLevel.STANDARD -> com.webtoapp.core.crypto.EncryptionLevel.STANDARD
                EncryptionLevel.HIGH -> com.webtoapp.core.crypto.EncryptionLevel.HIGH
                EncryptionLevel.PARANOID -> com.webtoapp.core.crypto.EncryptionLevel.PARANOID
            },
            enableRuntimeProtection = enableIntegrityCheck || enableAntiDebug || enableAntiTamper,
            blockOnThreat = false
        )
    }
}

data class AppHardeningConfig(
    val enabled: Boolean = false,
    val hardeningLevel: HardeningLevel = HardeningLevel.STANDARD,
    val dexEncryption: Boolean = true,
    val dexSplitting: Boolean = false,
    val dexVmp: Boolean = false,
    val dexControlFlowFlattening: Boolean = false,
    val soEncryption: Boolean = true,
    val soElfObfuscation: Boolean = false,
    val soSymbolStrip: Boolean = true,
    val soAntiDump: Boolean = false,
    val antiDebugMultiLayer: Boolean = true,
    val antiFridaAdvanced: Boolean = true,
    val antiXposedDeep: Boolean = true,
    val antiMagiskDetect: Boolean = false,
    val antiMemoryDump: Boolean = false,
    val antiScreenCapture: Boolean = false,
    val detectEmulatorAdvanced: Boolean = false,
    val detectVirtualApp: Boolean = true,
    val detectUSBDebugging: Boolean = false,
    val detectVPN: Boolean = false,
    val detectDeveloperOptions: Boolean = false,
    val stringEncryption: Boolean = true,
    val classNameObfuscation: Boolean = false,
    val callIndirection: Boolean = false,
    val opaquePredicates: Boolean = false,
    val dexCrcVerify: Boolean = true,
    val memoryIntegrity: Boolean = false,
    val jniCallValidation: Boolean = false,
    val timingCheck: Boolean = false,
    val stackTraceFilter: Boolean = true,
    val multiPointSignatureVerify: Boolean = true,
    val apkChecksumValidation: Boolean = true,
    val resourceIntegrity: Boolean = false,
    val certificatePinning: Boolean = false,
    val responseStrategy: ThreatResponse = ThreatResponse.SILENT_EXIT,
    val responseDelay: Int = 0,
    val enableHoneypot: Boolean = false,
    val enableSelfDestruct: Boolean = false
) {
    enum class HardeningLevel {
        BASIC,
        STANDARD,
        ADVANCED,
        FORTRESS;
        
        val displayName: String get() = when (this) {
            BASIC -> com.webtoapp.core.i18n.Strings.hardeningLevelBasic
            STANDARD -> com.webtoapp.core.i18n.Strings.hardeningLevelStandard
            ADVANCED -> com.webtoapp.core.i18n.Strings.hardeningLevelAdvanced
            FORTRESS -> com.webtoapp.core.i18n.Strings.hardeningLevelFortress
        }
        
        val description: String get() = when (this) {
            BASIC -> com.webtoapp.core.i18n.Strings.hardeningLevelBasicDesc
            STANDARD -> com.webtoapp.core.i18n.Strings.hardeningLevelStandardDesc
            ADVANCED -> com.webtoapp.core.i18n.Strings.hardeningLevelAdvancedDesc
            FORTRESS -> com.webtoapp.core.i18n.Strings.hardeningLevelFortressDesc
        }
    }
    
    enum class ThreatResponse {
        LOG_ONLY,
        SILENT_EXIT,
        CRASH_RANDOM,
        DATA_WIPE,
        FAKE_DATA;
        
        val displayName: String get() = when (this) {
            LOG_ONLY -> com.webtoapp.core.i18n.Strings.threatResponseLogOnly
            SILENT_EXIT -> com.webtoapp.core.i18n.Strings.threatResponseSilentExit
            CRASH_RANDOM -> com.webtoapp.core.i18n.Strings.threatResponseCrashRandom
            DATA_WIPE -> com.webtoapp.core.i18n.Strings.threatResponseDataWipe
            FAKE_DATA -> com.webtoapp.core.i18n.Strings.threatResponseFakeData
        }
    }
    
    companion object {
        val DISABLED = AppHardeningConfig(enabled = false)
        
        val BASIC = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.BASIC,
            dexEncryption = true,
            soEncryption = false,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = false,
            stringEncryption = false,
            dexCrcVerify = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true
        )
        
        val STANDARD = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.STANDARD,
            dexEncryption = true,
            soEncryption = true,
            soSymbolStrip = true,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = true,
            detectVirtualApp = true,
            stringEncryption = true,
            dexCrcVerify = true,
            stackTraceFilter = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true
        )
        
        val ADVANCED = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.ADVANCED,
            dexEncryption = true,
            dexSplitting = true,
            dexVmp = true,
            dexControlFlowFlattening = true,
            soEncryption = true,
            soElfObfuscation = true,
            soSymbolStrip = true,
            soAntiDump = true,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = true,
            antiMagiskDetect = true,
            antiMemoryDump = true,
            detectVirtualApp = true,
            detectUSBDebugging = true,
            stringEncryption = true,
            classNameObfuscation = true,
            callIndirection = true,
            opaquePredicates = true,
            dexCrcVerify = true,
            memoryIntegrity = true,
            jniCallValidation = true,
            timingCheck = true,
            stackTraceFilter = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true,
            resourceIntegrity = true,
            responseStrategy = ThreatResponse.SILENT_EXIT,
            responseDelay = 3
        )
        
        val FORTRESS = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.FORTRESS,
            dexEncryption = true,
            dexSplitting = true,
            dexVmp = true,
            dexControlFlowFlattening = true,
            soEncryption = true,
            soElfObfuscation = true,
            soSymbolStrip = true,
            soAntiDump = true,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = true,
            antiMagiskDetect = true,
            antiMemoryDump = true,
            antiScreenCapture = true,
            detectEmulatorAdvanced = true,
            detectVirtualApp = true,
            detectUSBDebugging = true,
            detectVPN = true,
            detectDeveloperOptions = true,
            stringEncryption = true,
            classNameObfuscation = true,
            callIndirection = true,
            opaquePredicates = true,
            dexCrcVerify = true,
            memoryIntegrity = true,
            jniCallValidation = true,
            timingCheck = true,
            stackTraceFilter = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true,
            resourceIntegrity = true,
            certificatePinning = true,
            responseStrategy = ThreatResponse.CRASH_RANDOM,
            responseDelay = 5,
            enableHoneypot = true,
            enableSelfDestruct = true
        )
    }
}
