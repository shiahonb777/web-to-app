package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.AppHardeningConfig
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream














class AppHardeningEngine(private val context: Context) {

    companion object {
        private const val TAG = "AppHardeningEngine"


        const val HARDENING_META_FILE = "hardening_meta.json"
        const val HARDENING_ASSETS_DIR = "hardening"
        const val HARDENING_DEX_DIR = "hardening/dex"
        const val HARDENING_SO_DIR = "hardening/so"


        const val HARDENING_VERSION = 1


        const val HARDENING_MAGIC = 0x57544148
    }

    private val dexProtector = DexProtector(context)
    private val nativeProtector = NativeProtector(context)
    private val antiReverseEngine = AntiReverseEngine(context)
    private val environmentDetector = EnvironmentDetector(context)
    private val codeObfuscator = CodeObfuscator(context)
    private val runtimeShield = RuntimeShield(context)




    data class HardeningResult(
        val success: Boolean,
        val protectedFeatures: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList(),
        val stats: HardeningStats = HardeningStats()
    )




    data class HardeningStats(
        val dexFilesProtected: Int = 0,
        val soFilesProtected: Int = 0,
        val stringsEncrypted: Int = 0,
        val classesObfuscated: Int = 0,
        val antiReverseChecks: Int = 0,
        val totalProtectionLayers: Int = 0,
        val hardeningTimeMs: Long = 0
    )











    fun performHardening(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray? = null,
        onProgress: ((Int, String) -> Unit)? = null
    ): HardeningResult {
        if (!config.enabled) {
            return HardeningResult(success = true)
        }

        val startTime = System.currentTimeMillis()
        val protectedFeatures = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var stats = HardeningStats()

        try {
            AppLogger.d(TAG, "开始应用加固")
            onProgress?.invoke(0, Strings.hardeningInit)


            onProgress?.invoke(10, Strings.hardeningDex)
            val dexResult = performDexProtection(config, zipOut, packageName, signatureHash)
            if (dexResult.success) {
                protectedFeatures.addAll(dexResult.features)
                stats = stats.copy(dexFilesProtected = dexResult.count)
            } else {
                warnings.addAll(dexResult.warnings)
            }


            onProgress?.invoke(25, Strings.hardeningNativeSo)
            val soResult = performNativeProtection(config, zipOut, packageName)
            if (soResult.success) {
                protectedFeatures.addAll(soResult.features)
                stats = stats.copy(soFilesProtected = soResult.count)
            } else {
                warnings.addAll(soResult.warnings)
            }


            onProgress?.invoke(40, Strings.hardeningCodeObfuscation)
            val obfResult = performCodeObfuscation(config, zipOut, packageName, signatureHash)
            if (obfResult.success) {
                protectedFeatures.addAll(obfResult.features)
                stats = stats.copy(
                    stringsEncrypted = obfResult.stringsCount,
                    classesObfuscated = obfResult.classesCount
                )
            }


            onProgress?.invoke(55, Strings.hardeningAntiReverse)
            val antiRevResult = performAntiReverseSetup(config, zipOut)
            if (antiRevResult.success) {
                protectedFeatures.addAll(antiRevResult.features)
            }


            onProgress?.invoke(70, Strings.hardeningRasp)
            val raspResult = performRaspSetup(config, zipOut, packageName, signatureHash)
            if (raspResult.success) {
                protectedFeatures.addAll(raspResult.features)
            }


            onProgress?.invoke(80, Strings.hardeningEnvDetection)
            val envResult = performEnvironmentDetectionSetup(config, zipOut)
            if (envResult.success) {
                protectedFeatures.addAll(envResult.features)
            }


            onProgress?.invoke(90, Strings.hardeningWriteMeta)
            writeHardeningMetadata(config, zipOut, packageName, signatureHash, protectedFeatures)


            onProgress?.invoke(95, Strings.hardeningFinalCheck)
            val totalLayers = protectedFeatures.size
            stats = stats.copy(
                antiReverseChecks = countAntiReverseChecks(),
                totalProtectionLayers = totalLayers,
                hardeningTimeMs = System.currentTimeMillis() - startTime
            )

            onProgress?.invoke(100, Strings.hardeningComplete)
            AppLogger.d(TAG, "加固完成，保护层数: $totalLayers, 耗时: ${stats.hardeningTimeMs}ms")

            return HardeningResult(
                success = true,
                protectedFeatures = protectedFeatures,
                warnings = warnings,
                stats = stats
            )

        } catch (e: Exception) {
            AppLogger.e(TAG, "加固过程异常", e)
            errors.add(Strings.hardeningError.format(e.message))
            return HardeningResult(
                success = false,
                protectedFeatures = protectedFeatures,
                warnings = warnings,
                errors = errors,
                stats = stats.copy(hardeningTimeMs = System.currentTimeMillis() - startTime)
            )
        }
    }




    private fun performDexProtection(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()
        var count = 0

        try {
            dexProtector.encryptDex(zipOut, packageName, signatureHash)
            features.add("DEX_ENCRYPTION")
            count++

            dexProtector.splitDex(zipOut)
            features.add("DEX_SPLITTING")
            count++

            dexProtector.applyVmp(zipOut, packageName)
            features.add("DEX_VMP")
            count++

            dexProtector.flattenControlFlow(zipOut)
            features.add("CONTROL_FLOW_FLATTENING")
            count++

            return ProtectionPhaseResult(true, features, count = count)
        } catch (e: Exception) {
            AppLogger.e(TAG, "DEX 保护失败", e)
            return ProtectionPhaseResult(false, features, listOf(Strings.hardeningDexError.format(e.message)), count)
        }
    }




    private fun performNativeProtection(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()
        var count = 0

        try {
            nativeProtector.encryptSections(zipOut, packageName)
            features.add("SO_SECTION_ENCRYPTION")
            count++

            nativeProtector.obfuscateElfHeaders(zipOut)
            features.add("ELF_HEADER_OBFUSCATION")
            count++

            nativeProtector.stripAndFakeSymbols(zipOut)
            features.add("SYMBOL_STRIP_FAKE")
            count++

            nativeProtector.enableAntiDump(zipOut)
            features.add("ANTI_MEMORY_DUMP")
            count++

            return ProtectionPhaseResult(true, features, count = count)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native SO 保护失败", e)
            return ProtectionPhaseResult(false, features, listOf(Strings.hardeningSoError.format(e.message)), count)
        }
    }




    private fun performCodeObfuscation(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ): CodeObfuscationResult {
        val features = mutableListOf<String>()
        var stringsCount = 0
        var classesCount = 0

        try {
            stringsCount = codeObfuscator.encryptStrings(zipOut, packageName, signatureHash)
            features.add("STRING_ENCRYPTION")

            classesCount = codeObfuscator.obfuscateClassNames(zipOut)
            features.add("CLASS_NAME_OBFUSCATION")

            codeObfuscator.applyCallIndirection(zipOut)
            features.add("CALL_INDIRECTION")

            codeObfuscator.injectOpaquePredicates(zipOut)
            features.add("OPAQUE_PREDICATES")

            return CodeObfuscationResult(true, features, stringsCount, classesCount)
        } catch (e: Exception) {
            AppLogger.e(TAG, "代码混淆失败", e)
            return CodeObfuscationResult(false, features, stringsCount, classesCount)
        }
    }




    private fun performAntiReverseSetup(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()

        try {
            val antiReverseConfig = AntiReverseEngine.AntiReverseConfig(
                multiLayerAntiDebug = true,
                advancedFridaDetection = true,
                deepXposedDetection = true,
                magiskDetection = true,
                antiMemoryDump = true,
                antiScreenCapture = true
            )

            antiReverseEngine.writeAntiReverseConfig(zipOut, antiReverseConfig)

            features.add("MULTI_LAYER_ANTI_DEBUG")
            features.add("ADVANCED_FRIDA_DETECTION")
            features.add("DEEP_XPOSED_DETECTION")
            features.add("MAGISK_DETECTION")
            features.add("ANTI_MEMORY_DUMP_RUNTIME")
            features.add("ANTI_SCREEN_CAPTURE")

            return ProtectionPhaseResult(true, features)
        } catch (e: Exception) {
            AppLogger.e(TAG, "反逆向配置失败", e)
            return ProtectionPhaseResult(false, features, listOf(Strings.hardeningAntiReverseError.format(e.message)))
        }
    }




    private fun performRaspSetup(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()

        try {
            val raspConfig = RuntimeShield.RaspConfig(
                dexCrcVerify = true,
                memoryIntegrity = true,
                jniCallValidation = true,
                timingCheck = true,
                stackTraceFilter = true,
                multiPointSignatureVerify = true,
                apkChecksumValidation = true,
                resourceIntegrity = true,
                certificatePinning = true,
                responseStrategy = "CRASH_RANDOM",
                responseDelay = 5,
                enableHoneypot = true,
                enableSelfDestruct = true
            )

            runtimeShield.writeRaspConfig(zipOut, raspConfig, packageName, signatureHash)

            features.add("DEX_CRC_VERIFY")
            features.add("MEMORY_INTEGRITY")
            features.add("JNI_CALL_VALIDATION")
            features.add("TIMING_CHECK")
            features.add("STACK_TRACE_FILTER")
            features.add("MULTI_POINT_SIGNATURE")
            features.add("APK_CHECKSUM")
            features.add("RESOURCE_INTEGRITY")
            features.add("CERTIFICATE_PINNING")
            features.add("HONEYPOT")
            features.add("SELF_DESTRUCT")

            return ProtectionPhaseResult(true, features)
        } catch (e: Exception) {
            AppLogger.e(TAG, "RASP 配置失败", e)
            return ProtectionPhaseResult(false, features, listOf("RASP: ${e.message}"))
        }
    }




    private fun performEnvironmentDetectionSetup(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()

        try {
            val envConfig = EnvironmentDetector.EnvironmentConfig(
                advancedEmulatorDetection = true,
                virtualAppDetection = true,
                usbDebuggingDetection = true,
                vpnDetection = true,
                developerOptionsDetection = true
            )

            environmentDetector.writeEnvironmentConfig(zipOut, envConfig)

            features.add("ADVANCED_EMULATOR_DETECTION")
            features.add("VIRTUAL_APP_DETECTION")
            features.add("USB_DEBUGGING_DETECTION")
            features.add("VPN_DETECTION")
            features.add("DEVELOPER_OPTIONS_DETECTION")

            return ProtectionPhaseResult(true, features)
        } catch (e: Exception) {
            AppLogger.e(TAG, "环境检测配置失败", e)
            return ProtectionPhaseResult(false, features, listOf(Strings.hardeningEnvError.format(e.message)))
        }
    }




    private fun writeHardeningMetadata(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?,
        protectedFeatures: List<String>
    ) {
        val metadata = buildString {
            appendLine("{")
            appendLine("  \"version\": $HARDENING_VERSION,")
            appendLine("  \"magic\": $HARDENING_MAGIC,")
            appendLine("  \"hardeningLevel\": \"FORTRESS\",")
            appendLine("  \"packageName\": \"$packageName\",")
            appendLine("  \"timestamp\": ${System.currentTimeMillis()},")
            appendLine("  \"protectedFeatures\": [")
            protectedFeatures.forEachIndexed { index, feature ->
                val comma = if (index < protectedFeatures.size - 1) "," else ""
                appendLine("    \"$feature\"$comma")
            }
            appendLine("  ],")
            appendLine("  \"responseStrategy\": \"CRASH_RANDOM\",")
            appendLine("  \"responseDelay\": 5,")
            appendLine("  \"enableHoneypot\": true,")
            appendLine("  \"enableSelfDestruct\": true,")

            appendLine("  \"antiDebugMultiLayer\": true,")
            appendLine("  \"antiFridaAdvanced\": true,")
            appendLine("  \"antiXposedDeep\": true,")
            appendLine("  \"antiMagiskDetect\": true,")
            appendLine("  \"antiMemoryDump\": true,")
            appendLine("  \"antiScreenCapture\": true,")

            appendLine("  \"detectEmulatorAdvanced\": true,")
            appendLine("  \"detectVirtualApp\": true,")
            appendLine("  \"detectUSBDebugging\": true,")
            appendLine("  \"detectVPN\": true,")
            appendLine("  \"detectDeveloperOptions\": true,")

            appendLine("  \"dexCrcVerify\": true,")
            appendLine("  \"memoryIntegrity\": true,")
            appendLine("  \"jniCallValidation\": true,")
            appendLine("  \"timingCheck\": true,")
            appendLine("  \"stackTraceFilter\": true,")
            appendLine("  \"multiPointSignatureVerify\": true,")
            appendLine("  \"apkChecksumValidation\": true,")
            appendLine("  \"resourceIntegrity\": true,")
            appendLine("  \"certificatePinning\": true")
            appendLine("}")
        }

        val entry = ZipEntry("assets/$HARDENING_META_FILE")
        zipOut.putNextEntry(entry)
        zipOut.write(metadata.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }




    private fun countAntiReverseChecks(): Int = 27




    data class ProtectionPhaseResult(
        val success: Boolean,
        val features: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val count: Int = 0
    )




    data class CodeObfuscationResult(
        val success: Boolean,
        val features: List<String> = emptyList(),
        val stringsCount: Int = 0,
        val classesCount: Int = 0
    )
}
