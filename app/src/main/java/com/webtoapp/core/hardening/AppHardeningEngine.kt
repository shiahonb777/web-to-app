package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.AppHardeningConfig
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 应用加固引擎 - 总控制器
 * 
 * 协调所有加固模块，按顺序执行加固流程：
 * 1. DEX 保护（加壳/分片/VMP）
 * 2. Native SO 保护（加密/混淆/符号处理）
 * 3. 代码混淆（字符串加密/类名混淆/控制流）
 * 4. 反逆向工程保护写入
 * 5. 运行时自保护 (RASP) 配置写入
 * 6. 环境检测配置写入
 * 7. 防篡改校验数据生成
 * 8. 威胁响应策略配置
 */
class AppHardeningEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "AppHardeningEngine"
        
        // 加固元数据文件
        const val HARDENING_META_FILE = "hardening_meta.json"
        const val HARDENING_ASSETS_DIR = "hardening"
        const val HARDENING_DEX_DIR = "hardening/dex"
        const val HARDENING_SO_DIR = "hardening/so"
        
        // 加固版本（用于兼容性判断）
        const val HARDENING_VERSION = 1
        
        // 加固魔数
        const val HARDENING_MAGIC = 0x57544148 // "WTAH" = WebToApp Hardening
    }
    
    private val dexProtector = DexProtector(context)
    private val nativeProtector = NativeProtector(context)
    private val antiReverseEngine = AntiReverseEngine(context)
    private val environmentDetector = EnvironmentDetector(context)
    private val codeObfuscator = CodeObfuscator(context)
    private val runtimeShield = RuntimeShield(context)
    
    /**
     * 加固结果
     */
    data class HardeningResult(
        val success: Boolean,
        val protectedFeatures: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList(),
        val stats: HardeningStats = HardeningStats()
    )
    
    /**
     * 加固统计
     */
    data class HardeningStats(
        val dexFilesProtected: Int = 0,
        val soFilesProtected: Int = 0,
        val stringsEncrypted: Int = 0,
        val classesObfuscated: Int = 0,
        val antiReverseChecks: Int = 0,
        val totalProtectionLayers: Int = 0,
        val hardeningTimeMs: Long = 0
    )
    
    /**
     * 执行完整加固流程
     * 
     * @param config 加固配置
     * @param zipOut APK 输出流
     * @param packageName 包名
     * @param signatureHash 签名哈希
     * @param onProgress 进度回调
     * @return 加固结果
     */
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
            AppLogger.d(TAG, "开始应用加固，等级: ${config.hardeningLevel}")
            onProgress?.invoke(0, "初始化加固引擎...")
            
            // ========== 阶段 1: DEX 保护 ==========
            onProgress?.invoke(10, "DEX 保护处理中...")
            val dexResult = performDexProtection(config, zipOut, packageName, signatureHash)
            if (dexResult.success) {
                protectedFeatures.addAll(dexResult.features)
                stats = stats.copy(dexFilesProtected = dexResult.count)
            } else {
                warnings.addAll(dexResult.warnings)
            }
            
            // ========== 阶段 2: Native SO 保护 ==========
            onProgress?.invoke(25, "Native SO 保护处理中...")
            val soResult = performNativeProtection(config, zipOut, packageName)
            if (soResult.success) {
                protectedFeatures.addAll(soResult.features)
                stats = stats.copy(soFilesProtected = soResult.count)
            } else {
                warnings.addAll(soResult.warnings)
            }
            
            // ========== 阶段 3: 代码混淆 ==========
            onProgress?.invoke(40, "代码混淆处理中...")
            val obfResult = performCodeObfuscation(config, zipOut, packageName, signatureHash)
            if (obfResult.success) {
                protectedFeatures.addAll(obfResult.features)
                stats = stats.copy(
                    stringsEncrypted = obfResult.stringsCount,
                    classesObfuscated = obfResult.classesCount
                )
            }
            
            // ========== 阶段 4: 反逆向工程配置 ==========
            onProgress?.invoke(55, "反逆向工程保护配置中...")
            val antiRevResult = performAntiReverseSetup(config, zipOut)
            if (antiRevResult.success) {
                protectedFeatures.addAll(antiRevResult.features)
            }
            
            // ========== 阶段 5: 运行时自保护 (RASP) ==========
            onProgress?.invoke(70, "运行时自保护配置中...")
            val raspResult = performRaspSetup(config, zipOut, packageName, signatureHash)
            if (raspResult.success) {
                protectedFeatures.addAll(raspResult.features)
            }
            
            // ========== 阶段 6: 环境检测配置 ==========
            onProgress?.invoke(80, "环境检测配置中...")
            val envResult = performEnvironmentDetectionSetup(config, zipOut)
            if (envResult.success) {
                protectedFeatures.addAll(envResult.features)
            }
            
            // ========== 阶段 7: 写入加固元数据 ==========
            onProgress?.invoke(90, "写入加固元数据...")
            writeHardeningMetadata(config, zipOut, packageName, signatureHash, protectedFeatures)
            
            // ========== 阶段 8: 最终校验 ==========
            onProgress?.invoke(95, "最终校验...")
            val totalLayers = protectedFeatures.size
            stats = stats.copy(
                antiReverseChecks = countAntiReverseChecks(config),
                totalProtectionLayers = totalLayers,
                hardeningTimeMs = System.currentTimeMillis() - startTime
            )
            
            onProgress?.invoke(100, "加固完成")
            AppLogger.d(TAG, "加固完成，保护层数: $totalLayers, 耗时: ${stats.hardeningTimeMs}ms")
            
            return HardeningResult(
                success = true,
                protectedFeatures = protectedFeatures,
                warnings = warnings,
                stats = stats
            )
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "加固过程异常", e)
            errors.add("加固异常: ${e.message}")
            return HardeningResult(
                success = false,
                protectedFeatures = protectedFeatures,
                warnings = warnings,
                errors = errors,
                stats = stats.copy(hardeningTimeMs = System.currentTimeMillis() - startTime)
            )
        }
    }
    
    /**
     * DEX 保护处理
     */
    private fun performDexProtection(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()
        var count = 0
        
        try {
            if (config.dexEncryption) {
                dexProtector.encryptDex(zipOut, packageName, signatureHash)
                features.add("DEX_ENCRYPTION")
                count++
            }
            
            if (config.dexSplitting) {
                dexProtector.splitDex(zipOut)
                features.add("DEX_SPLITTING")
                count++
            }
            
            if (config.dexVmp) {
                dexProtector.applyVmp(zipOut, packageName)
                features.add("DEX_VMP")
                count++
            }
            
            if (config.dexControlFlowFlattening) {
                dexProtector.flattenControlFlow(zipOut)
                features.add("CONTROL_FLOW_FLATTENING")
                count++
            }
            
            return ProtectionPhaseResult(true, features, count = count)
        } catch (e: Exception) {
            AppLogger.e(TAG, "DEX 保护失败", e)
            return ProtectionPhaseResult(false, features, listOf("DEX 保护: ${e.message}"), count)
        }
    }
    
    /**
     * Native SO 保护处理
     */
    private fun performNativeProtection(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()
        var count = 0
        
        try {
            if (config.soEncryption) {
                nativeProtector.encryptSections(zipOut, packageName)
                features.add("SO_SECTION_ENCRYPTION")
                count++
            }
            
            if (config.soElfObfuscation) {
                nativeProtector.obfuscateElfHeaders(zipOut)
                features.add("ELF_HEADER_OBFUSCATION")
                count++
            }
            
            if (config.soSymbolStrip) {
                nativeProtector.stripAndFakeSymbols(zipOut)
                features.add("SYMBOL_STRIP_FAKE")
                count++
            }
            
            if (config.soAntiDump) {
                nativeProtector.enableAntiDump(zipOut)
                features.add("ANTI_MEMORY_DUMP")
                count++
            }
            
            return ProtectionPhaseResult(true, features, count = count)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native SO 保护失败", e)
            return ProtectionPhaseResult(false, features, listOf("SO 保护: ${e.message}"), count)
        }
    }
    
    /**
     * 代码混淆处理
     */
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
            if (config.stringEncryption) {
                stringsCount = codeObfuscator.encryptStrings(zipOut, packageName, signatureHash)
                features.add("STRING_ENCRYPTION")
            }
            
            if (config.classNameObfuscation) {
                classesCount = codeObfuscator.obfuscateClassNames(zipOut)
                features.add("CLASS_NAME_OBFUSCATION")
            }
            
            if (config.callIndirection) {
                codeObfuscator.applyCallIndirection(zipOut)
                features.add("CALL_INDIRECTION")
            }
            
            if (config.opaquePredicates) {
                codeObfuscator.injectOpaquePredicates(zipOut)
                features.add("OPAQUE_PREDICATES")
            }
            
            return CodeObfuscationResult(true, features, stringsCount, classesCount)
        } catch (e: Exception) {
            AppLogger.e(TAG, "代码混淆失败", e)
            return CodeObfuscationResult(false, features, stringsCount, classesCount)
        }
    }
    
    /**
     * 反逆向工程配置
     */
    private fun performAntiReverseSetup(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()
        
        try {
            val antiReverseConfig = AntiReverseEngine.AntiReverseConfig(
                multiLayerAntiDebug = config.antiDebugMultiLayer,
                advancedFridaDetection = config.antiFridaAdvanced,
                deepXposedDetection = config.antiXposedDeep,
                magiskDetection = config.antiMagiskDetect,
                antiMemoryDump = config.antiMemoryDump,
                antiScreenCapture = config.antiScreenCapture
            )
            
            antiReverseEngine.writeAntiReverseConfig(zipOut, antiReverseConfig)
            
            if (config.antiDebugMultiLayer) features.add("MULTI_LAYER_ANTI_DEBUG")
            if (config.antiFridaAdvanced) features.add("ADVANCED_FRIDA_DETECTION")
            if (config.antiXposedDeep) features.add("DEEP_XPOSED_DETECTION")
            if (config.antiMagiskDetect) features.add("MAGISK_DETECTION")
            if (config.antiMemoryDump) features.add("ANTI_MEMORY_DUMP_RUNTIME")
            if (config.antiScreenCapture) features.add("ANTI_SCREEN_CAPTURE")
            
            return ProtectionPhaseResult(true, features)
        } catch (e: Exception) {
            AppLogger.e(TAG, "反逆向配置失败", e)
            return ProtectionPhaseResult(false, features, listOf("反逆向: ${e.message}"))
        }
    }
    
    /**
     * 运行时自保护 (RASP) 配置
     */
    private fun performRaspSetup(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()
        
        try {
            val raspConfig = RuntimeShield.RaspConfig(
                dexCrcVerify = config.dexCrcVerify,
                memoryIntegrity = config.memoryIntegrity,
                jniCallValidation = config.jniCallValidation,
                timingCheck = config.timingCheck,
                stackTraceFilter = config.stackTraceFilter,
                multiPointSignatureVerify = config.multiPointSignatureVerify,
                apkChecksumValidation = config.apkChecksumValidation,
                resourceIntegrity = config.resourceIntegrity,
                certificatePinning = config.certificatePinning,
                responseStrategy = config.responseStrategy.name,
                responseDelay = config.responseDelay,
                enableHoneypot = config.enableHoneypot,
                enableSelfDestruct = config.enableSelfDestruct
            )
            
            runtimeShield.writeRaspConfig(zipOut, raspConfig, packageName, signatureHash)
            
            if (config.dexCrcVerify) features.add("DEX_CRC_VERIFY")
            if (config.memoryIntegrity) features.add("MEMORY_INTEGRITY")
            if (config.jniCallValidation) features.add("JNI_CALL_VALIDATION")
            if (config.timingCheck) features.add("TIMING_CHECK")
            if (config.stackTraceFilter) features.add("STACK_TRACE_FILTER")
            if (config.multiPointSignatureVerify) features.add("MULTI_POINT_SIGNATURE")
            if (config.apkChecksumValidation) features.add("APK_CHECKSUM")
            if (config.resourceIntegrity) features.add("RESOURCE_INTEGRITY")
            if (config.certificatePinning) features.add("CERTIFICATE_PINNING")
            if (config.enableHoneypot) features.add("HONEYPOT")
            if (config.enableSelfDestruct) features.add("SELF_DESTRUCT")
            
            return ProtectionPhaseResult(true, features)
        } catch (e: Exception) {
            AppLogger.e(TAG, "RASP 配置失败", e)
            return ProtectionPhaseResult(false, features, listOf("RASP: ${e.message}"))
        }
    }
    
    /**
     * 环境检测配置
     */
    private fun performEnvironmentDetectionSetup(
        config: AppHardeningConfig,
        zipOut: ZipOutputStream
    ): ProtectionPhaseResult {
        val features = mutableListOf<String>()
        
        try {
            val envConfig = EnvironmentDetector.EnvironmentConfig(
                advancedEmulatorDetection = config.detectEmulatorAdvanced,
                virtualAppDetection = config.detectVirtualApp,
                usbDebuggingDetection = config.detectUSBDebugging,
                vpnDetection = config.detectVPN,
                developerOptionsDetection = config.detectDeveloperOptions
            )
            
            environmentDetector.writeEnvironmentConfig(zipOut, envConfig)
            
            if (config.detectEmulatorAdvanced) features.add("ADVANCED_EMULATOR_DETECTION")
            if (config.detectVirtualApp) features.add("VIRTUAL_APP_DETECTION")
            if (config.detectUSBDebugging) features.add("USB_DEBUGGING_DETECTION")
            if (config.detectVPN) features.add("VPN_DETECTION")
            if (config.detectDeveloperOptions) features.add("DEVELOPER_OPTIONS_DETECTION")
            
            return ProtectionPhaseResult(true, features)
        } catch (e: Exception) {
            AppLogger.e(TAG, "环境检测配置失败", e)
            return ProtectionPhaseResult(false, features, listOf("环境检测: ${e.message}"))
        }
    }
    
    /**
     * 写入加固元数据
     */
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
            appendLine("  \"hardeningLevel\": \"${config.hardeningLevel.name}\",")
            appendLine("  \"packageName\": \"$packageName\",")
            appendLine("  \"timestamp\": ${System.currentTimeMillis()},")
            appendLine("  \"protectedFeatures\": [")
            protectedFeatures.forEachIndexed { index, feature ->
                val comma = if (index < protectedFeatures.size - 1) "," else ""
                appendLine("    \"$feature\"$comma")
            }
            appendLine("  ],")
            appendLine("  \"responseStrategy\": \"${config.responseStrategy.name}\",")
            appendLine("  \"responseDelay\": ${config.responseDelay},")
            appendLine("  \"enableHoneypot\": ${config.enableHoneypot},")
            appendLine("  \"enableSelfDestruct\": ${config.enableSelfDestruct},")
            // 反逆向
            appendLine("  \"antiDebugMultiLayer\": ${config.antiDebugMultiLayer},")
            appendLine("  \"antiFridaAdvanced\": ${config.antiFridaAdvanced},")
            appendLine("  \"antiXposedDeep\": ${config.antiXposedDeep},")
            appendLine("  \"antiMagiskDetect\": ${config.antiMagiskDetect},")
            appendLine("  \"antiMemoryDump\": ${config.antiMemoryDump},")
            appendLine("  \"antiScreenCapture\": ${config.antiScreenCapture},")
            // 环境检测
            appendLine("  \"detectEmulatorAdvanced\": ${config.detectEmulatorAdvanced},")
            appendLine("  \"detectVirtualApp\": ${config.detectVirtualApp},")
            appendLine("  \"detectUSBDebugging\": ${config.detectUSBDebugging},")
            appendLine("  \"detectVPN\": ${config.detectVPN},")
            appendLine("  \"detectDeveloperOptions\": ${config.detectDeveloperOptions},")
            // RASP
            appendLine("  \"dexCrcVerify\": ${config.dexCrcVerify},")
            appendLine("  \"memoryIntegrity\": ${config.memoryIntegrity},")
            appendLine("  \"jniCallValidation\": ${config.jniCallValidation},")
            appendLine("  \"timingCheck\": ${config.timingCheck},")
            appendLine("  \"stackTraceFilter\": ${config.stackTraceFilter},")
            appendLine("  \"multiPointSignatureVerify\": ${config.multiPointSignatureVerify},")
            appendLine("  \"apkChecksumValidation\": ${config.apkChecksumValidation},")
            appendLine("  \"resourceIntegrity\": ${config.resourceIntegrity},")
            appendLine("  \"certificatePinning\": ${config.certificatePinning}")
            appendLine("}")
        }
        
        val entry = ZipEntry("assets/$HARDENING_META_FILE")
        zipOut.putNextEntry(entry)
        zipOut.write(metadata.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }
    
    /**
     * 计算反逆向检查总数
     */
    private fun countAntiReverseChecks(config: AppHardeningConfig): Int {
        var count = 0
        if (config.antiDebugMultiLayer) count += 4   // ptrace + timing + signal + thread
        if (config.antiFridaAdvanced) count += 5     // port + maps + process + memory + thread name
        if (config.antiXposedDeep) count += 3        // class + maps + stack
        if (config.antiMagiskDetect) count += 2      // mount + prop
        if (config.antiMemoryDump) count += 2        // mprotect + inotify
        if (config.antiScreenCapture) count += 1     // FLAG_SECURE
        if (config.detectEmulatorAdvanced) count += 4 // hardware + sensor + temp + cpu
        if (config.detectVirtualApp) count += 3       // package + path + prop
        if (config.detectUSBDebugging) count += 1
        if (config.detectVPN) count += 1
        if (config.detectDeveloperOptions) count += 1
        return count
    }
    
    /**
     * 保护阶段结果
     */
    data class ProtectionPhaseResult(
        val success: Boolean,
        val features: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val count: Int = 0
    )
    
    /**
     * 代码混淆结果
     */
    data class CodeObfuscationResult(
        val success: Boolean,
        val features: List<String> = emptyList(),
        val stringsCount: Int = 0,
        val classesCount: Int = 0
    )
}
