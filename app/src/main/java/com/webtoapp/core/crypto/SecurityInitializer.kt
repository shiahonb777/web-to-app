package com.webtoapp.core.crypto

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 安全初始化器
 * 
 * 在应用启动时初始化安全保护机制
 */
object SecurityInitializer {
    
    private const val TAG = "SecurityInitializer"
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var securityConfig: SecurityConfig? = null
    
    private var runtimeProtection: RuntimeProtection? = null
    
    /**
     * 安全配置
     */
    data class SecurityConfig(
        val enableIntegrityCheck: Boolean = true,
        val enableAntiDebug: Boolean = true,
        val enableAntiTamper: Boolean = true,
        val enableRootDetection: Boolean = false,
        val enableEmulatorDetection: Boolean = false,
        val enableRuntimeProtection: Boolean = true,
        val blockOnThreat: Boolean = false,
        val encryptionLevel: String = "STANDARD"
    )
    
    /**
     * 初始化安全保护
     * 
     * @param context 应用上下文
     * @param onThreatDetected 威胁检测回调
     * @return 是否初始化成功
     */
    fun initialize(
        context: Context,
        onThreatDetected: ((ProtectionResult) -> Unit)? = null
    ): Boolean {
        if (isInitialized) {
            Log.d(TAG, "Security already initialized")
            return true
        }
        
        try {
            // 读取加密元数据
            val config = loadSecurityConfig(context)
            securityConfig = config
            
            if (config == null) {
                Log.d(TAG, "No security config found, skipping initialization")
                isInitialized = true
                return true
            }
            
            Log.d(TAG, "Initializing security with config: $config")
            
            // Initialize运行时保护
            if (config.enableRuntimeProtection) {
                runtimeProtection = RuntimeProtection.getInstance(context)
                
                // Set威胁回调
                runtimeProtection?.setThreatCallback { result ->
                    Log.w(TAG, "Threat detected: level=${result.threatLevel}, threats=${result.threats}")
                    onThreatDetected?.invoke(result)
                    
                    // 如果配置为阻止运行
                    if (config.blockOnThreat && result.shouldBlock) {
                        Log.e(TAG, "Blocking due to high threat level")
                        // 可以在这里执行阻止逻辑
                    }
                }
                
                // Execute初始检查
                val initialResult = runtimeProtection?.performCheck(forceRefresh = true)
                if (initialResult != null) {
                    Log.d(TAG, "Initial security check: level=${initialResult.threatLevel}")
                    
                    if (initialResult.shouldBlock && config.blockOnThreat) {
                        Log.e(TAG, "Initial check failed, blocking")
                        return false
                    }
                }
                
                // Start持续监控
                runtimeProtection?.startMonitoring()
            }
            
            // 完整性检查
            if (config.enableIntegrityCheck) {
                CoroutineScope(Dispatchers.IO).launch {
                    performIntegrityCheck(context)
                }
            }
            
            isInitialized = true
            Log.d(TAG, "Security initialization completed")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Security initialization failed", e)
            return false
        }
    }
    
    /**
     * 从 assets 加载安全配置
     */
    private fun loadSecurityConfig(context: Context): SecurityConfig? {
        return try {
            val inputStream = context.assets.open("encryption_meta.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            
            val metadata = Gson().fromJson(json, EncryptionMetadata::class.java)
            
            SecurityConfig(
                enableIntegrityCheck = metadata.enableIntegrityCheck,
                enableAntiDebug = metadata.enableAntiDebug,
                enableAntiTamper = metadata.enableAntiTamper,
                enableRootDetection = metadata.enableRootDetection,
                enableEmulatorDetection = metadata.enableEmulatorDetection,
                enableRuntimeProtection = metadata.enableRuntimeProtection,
                blockOnThreat = metadata.blockOnThreat,
                encryptionLevel = metadata.encryptionLevel
            )
        } catch (e: Exception) {
            Log.d(TAG, "No encryption metadata found: ${e.message}")
            null
        }
    }
    
    /**
     * 执行完整性检查
     */
    private fun performIntegrityCheck(context: Context) {
        try {
            val checker = IntegrityChecker(context)
            val result = checker.verifyAll()
            
            if (!result.isValid) {
                Log.e(TAG, "Integrity check failed: ${result.errors}")
            } else {
                Log.d(TAG, "Integrity check passed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check error", e)
        }
    }
    
    /**
     * 执行快速安全检查
     */
    fun quickCheck(): Boolean {
        return runtimeProtection?.quickCheck() ?: true
    }
    
    /**
     * 获取当前威胁等级
     */
    fun getThreatLevel(): Int {
        return runtimeProtection?.getThreatLevel() ?: RuntimeProtection.THREAT_NONE
    }
    
    /**
     * 执行完整安全检查
     */
    fun performFullCheck(): ProtectionResult? {
        return runtimeProtection?.performCheck(forceRefresh = true)
    }
    
    /**
     * 停止安全监控
     */
    fun shutdown() {
        runtimeProtection?.stopMonitoring()
        SecureMemory.shutdown()
        isInitialized = false
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * 获取当前安全配置
     */
    fun getConfig(): SecurityConfig? = securityConfig
}
