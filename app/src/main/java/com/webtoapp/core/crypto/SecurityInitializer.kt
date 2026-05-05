package com.webtoapp.core.crypto

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch






@SuppressLint("StaticFieldLeak")
object SecurityInitializer {

    private const val TAG = "SecurityInitializer"
    private val gson = com.webtoapp.util.GsonProvider.gson

    @Volatile
    private var isInitialized = false

    @Volatile
    private var securityConfig: SecurityConfig? = null

    private var runtimeProtection: RuntimeProtection? = null




    data class SecurityConfig(
        val enableIntegrityCheck: Boolean = true,
        val enableAntiDebug: Boolean = true,
        val enableAntiTamper: Boolean = true,
        val enableRootDetection: Boolean = false,
        val enableEmulatorDetection: Boolean = false,
        val enableRuntimeProtection: Boolean = true,
        val blockOnThreat: Boolean = false
    )








    fun initialize(
        context: Context,
        onThreatDetected: ((ProtectionResult) -> Unit)? = null
    ): Boolean {
        if (isInitialized) {
            AppLogger.d(TAG, "Security already initialized")
            return true
        }

        try {

            val config = loadSecurityConfig(context)
            securityConfig = config

            if (config == null) {
                AppLogger.d(TAG, "No security config found, skipping initialization")
                isInitialized = true
                return true
            }

            AppLogger.d(TAG, "Initializing security with config: $config")


            if (config.enableRuntimeProtection) {
                runtimeProtection = RuntimeProtection.getInstance(context)


                runtimeProtection?.setThreatCallback { result ->
                    AppLogger.w(TAG, "Threat detected: level=${result.threatLevel}, threats=${result.threats}")
                    onThreatDetected?.invoke(result)


                    if (config.blockOnThreat && result.shouldBlock) {
                        AppLogger.e(TAG, "Blocking due to high threat level")

                    }
                }
                runtimeProtection?.startMonitoring()
            }


            if (config.enableIntegrityCheck) {
                CoroutineScope(Dispatchers.IO).launch {
                    performIntegrityCheck(context)
                }
            }

            isInitialized = true
            AppLogger.d(TAG, "Security initialization completed")
            return true

        } catch (e: Exception) {
            AppLogger.e(TAG, "Security initialization failed", e)
            return false
        }
    }




    private fun loadSecurityConfig(context: Context): SecurityConfig? {
        return try {
            val inputStream = context.assets.open("encryption_meta.json")
            inputStream.bufferedReader().use { it.readText() }
            SecurityConfig()
        } catch (e: Exception) {
            AppLogger.d(TAG, "No encryption metadata found: ${e.message}")
            null
        }
    }




    private fun performIntegrityCheck(context: Context) {
        try {
            val checker = IntegrityChecker(context)
            val result = checker.verifyAll()

            if (!result.isValid) {
                AppLogger.e(TAG, "Integrity check failed: ${result.errors}")
            } else {
                AppLogger.d(TAG, "Integrity check passed")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Integrity check error", e)
        }
    }




    fun quickCheck(): Boolean {
        return runtimeProtection?.quickCheck() ?: true
    }




    fun getThreatLevel(): Int {
        return runtimeProtection?.getThreatLevel() ?: RuntimeProtection.THREAT_NONE
    }




    fun performFullCheck(): ProtectionResult? {
        return runtimeProtection?.performCheck(forceRefresh = true)
    }




    fun shutdown() {
        runtimeProtection?.stopMonitoring()
        SecureMemory.shutdown()
        isInitialized = false
    }




    fun isInitialized(): Boolean = isInitialized




    fun getConfig(): SecurityConfig? = securityConfig
}
