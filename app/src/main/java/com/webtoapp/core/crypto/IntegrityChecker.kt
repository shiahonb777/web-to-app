package com.webtoapp.core.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * 完整性检查器
 * 检测 APK 是否被篡改
 */
class IntegrityChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "IntegrityChecker"
        
        // 预期的签名哈希（在打包时设置）
        // 这个值会被 ApkBuilder 替换为实际的签名哈希
        private const val EXPECTED_SIGNATURE_PLACEHOLDER = "SIGNATURE_HASH_PLACEHOLDER_DO_NOT_MODIFY"
    }
    
    private val keyManager = KeyManager(context)
    
    /**
     * 执行完整性检查
     * @return 检查结果
     */
    fun check(): IntegrityResult {
        val results = mutableListOf<CheckItem>()
        
        // 1. 签名检查
        results.add(checkSignature())
        
        // 2. 调试检查
        results.add(checkDebugMode())
        
        // 3. 安装来源检查
        results.add(checkInstaller())
        
        // 4. APK 完整性检查
        results.add(checkApkIntegrity())
        
        // 5. 运行环境检查
        results.add(checkEnvironment())
        
        val passed = results.all { it.passed }
        val failedChecks = results.filter { !it.passed }
        
        if (!passed) {
            Log.w(TAG, "完整性检查失败: ${failedChecks.map { it.name }}")
        }
        
        return IntegrityResult(
            passed = passed,
            checks = results,
            failedChecks = failedChecks,
            errors = failedChecks.map { it.message }
        )
    }
    
    /**
     * 执行所有验证
     */
    fun verifyAll(): IntegrityResult {
        return check()
    }
    
    /**
     * 快速检查（仅检查关键项）
     * 在模拟器环境下跳过检查，避免误判
     */
    fun quickCheck(): Boolean {
        // If it is模拟器环境，直接通过检查
        if (isEmulator()) {
            Log.d(TAG, "Running in emulator, skipping quick check")
            return true
        }
        return checkSignature().passed && checkDebugMode().passed
    }
    
    /**
     * 检查应用签名
     */
    private fun checkSignature(): CheckItem {
        return try {
            val currentSignature = keyManager.getAppSignature()
            val currentHash = currentSignature.toHexString()
            
            // If it is占位符，说明是开发版本，跳过检查
            if (EXPECTED_SIGNATURE_PLACEHOLDER.contains("PLACEHOLDER")) {
                Log.d(TAG, "开发版本，跳过签名检查")
                return CheckItem("signature", true, "开发版本")
            }
            
            val expectedHash = EXPECTED_SIGNATURE_PLACEHOLDER
            val passed = currentHash.equals(expectedHash, ignoreCase = true)
            
            CheckItem(
                name = "signature",
                passed = passed,
                message = if (passed) "签名验证通过" else "签名不匹配"
            )
        } catch (e: Exception) {
            Log.e(TAG, "签名检查失败", e)
            CheckItem("signature", false, "签名检查异常: ${e.message}")
        }
    }
    
    /**
     * 检查调试模式
     */
    private fun checkDebugMode(): CheckItem {
        val isDebuggable = (context.applicationInfo.flags and 
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        
        // Check是否有调试器连接
        val isDebuggerConnected = android.os.Debug.isDebuggerConnected()
        
        val passed = !isDebuggable && !isDebuggerConnected
        
        return CheckItem(
            name = "debug",
            passed = passed,
            message = when {
                isDebuggerConnected -> "检测到调试器"
                isDebuggable -> "应用处于调试模式"
                else -> "非调试模式"
            }
        )
    }
    
    /**
     * 检查安装来源
     */
    private fun checkInstaller(): CheckItem {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            
            // Allow的安装来源
            val allowedInstallers = setOf(
                "com.android.vending",           // Google Play
                "com.huawei.appmarket",          // 华为应用市场
                "com.xiaomi.market",             // 小米应用商店
                "com.oppo.market",               // OPPO 应用商店
                "com.vivo.appstore",             // vivo 应用商店
                "com.tencent.android.qqdownloader", // App宝
                null                              // Allow adb 安装（开发用）
            )
            
            val passed = installer in allowedInstallers || installer == null
            
            CheckItem(
                name = "installer",
                passed = passed,
                message = "安装来源: ${installer ?: "未知"}"
            )
        } catch (e: Exception) {
            // 无法获取安装来源，允许通过
            CheckItem("installer", true, "无法获取安装来源")
        }
    }
    
    /**
     * 检查 APK 完整性
     */
    private fun checkApkIntegrity(): CheckItem {
        return try {
            val apkPath = context.applicationInfo.sourceDir
            val apkFile = File(apkPath)
            
            if (!apkFile.exists()) {
                return CheckItem("apk_integrity", false, "APK 文件不存在")
            }
            
            // Check APK 是否可以正常打开
            ZipFile(apkFile).use { zip ->
                // Check关键文件是否存在
                val requiredEntries = listOf(
                    "AndroidManifest.xml",
                    "classes.dex",
                    "resources.arsc"
                )
                
                val missingEntries = requiredEntries.filter { zip.getEntry(it) == null }
                
                if (missingEntries.isNotEmpty()) {
                    return CheckItem(
                        "apk_integrity",
                        false,
                        "APK 缺少关键文件: $missingEntries"
                    )
                }
            }
            
            CheckItem("apk_integrity", true, "APK 完整性验证通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "APK 完整性检查失败", e)
            CheckItem("apk_integrity", false, "APK 完整性检查异常: ${e.message}")
        }
    }
    
    /**
     * 检查运行环境
     */
    private fun checkEnvironment(): CheckItem {
        val issues = mutableListOf<String>()
        
        // Check是否在模拟器中运行
        if (isEmulator()) {
            issues.add("模拟器环境")
        }
        
        // Check是否 Root
        if (isRooted()) {
            issues.add("Root 环境")
        }
        
        // Check是否有 Xposed 框架
        if (hasXposed()) {
            issues.add("Xposed 框架")
        }
        
        // Check是否有 Frida
        if (hasFrida()) {
            issues.add("Frida 检测")
        }
        
        val passed = issues.isEmpty()
        
        return CheckItem(
            name = "environment",
            passed = passed,
            message = if (passed) "环境检查通过" else "检测到: ${issues.joinToString(", ")}"
        )
    }
    
    /**
     * 检测模拟器
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }
    
    /**
     * 检测 Root
     */
    private fun isRooted(): Boolean {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        return rootPaths.any { File(it).exists() }
    }
    
    /**
     * 检测 Xposed 框架
     */
    private fun hasXposed(): Boolean {
        return try {
            // Check Xposed 类是否存在
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    /**
     * 检测 Frida
     */
    private fun hasFrida(): Boolean {
        // Check Frida 默认端口
        return try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress("127.0.0.1", 27042), 100)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 完整性检查结果
 */
data class IntegrityResult(
    val passed: Boolean,
    val checks: List<CheckItem>,
    val failedChecks: List<CheckItem>,
    val errors: List<String> = emptyList()
) {
    val isValid: Boolean get() = passed
}

/**
 * 单项检查结果
 */
data class CheckItem(
    val name: String,
    val passed: Boolean,
    val message: String
)
