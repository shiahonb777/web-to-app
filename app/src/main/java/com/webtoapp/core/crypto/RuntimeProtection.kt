package com.webtoapp.core.crypto

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 运行时保护
 * 
 * 提供多层次的运行时安全检测和保护
 */
class RuntimeProtection(private val context: Context) {
    
    companion object {
        private const val TAG = "RuntimeProtection"
        
        // 威胁等级
        const val THREAT_NONE = 0
        const val THREAT_LOW = 1
        const val THREAT_MEDIUM = 2
        const val THREAT_HIGH = 3
        const val THREAT_CRITICAL = 4
        
        // 检测间隔
        private const val CHECK_INTERVAL_MS = 5000L
        
        @Volatile
        private var instance: RuntimeProtection? = null
        
        fun getInstance(context: Context): RuntimeProtection {
            return instance ?: synchronized(this) {
                instance ?: RuntimeProtection(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // 检测状态
    private val isMonitoring = AtomicBoolean(false)
    private val threatLevel = AtomicInteger(THREAT_NONE)
    private var monitorThread: Thread? = null
    
    // 检测结果缓存
    @Volatile
    private var lastCheckResult: ProtectionResult? = null
    private var lastCheckTime = 0L
    
    // Callback
    private var onThreatDetected: ((ProtectionResult) -> Unit)? = null
    
    /**
     * 设置威胁检测回调
     */
    fun setThreatCallback(callback: (ProtectionResult) -> Unit) {
        onThreatDetected = callback
    }
    
    /**
     * 启动持续监控
     */
    fun startMonitoring() {
        if (isMonitoring.getAndSet(true)) return
        
        monitorThread = Thread({
            while (isMonitoring.get()) {
                try {
                    val result = performCheck()
                    if (result.threatLevel >= THREAT_MEDIUM) {
                        onThreatDetected?.invoke(result)
                    }
                    Thread.sleep(CHECK_INTERVAL_MS)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Monitor error", e)
                }
            }
        }, "RuntimeProtection-Monitor")
        monitorThread?.isDaemon = true
        monitorThread?.start()
        
        Log.d(TAG, "Runtime protection monitoring started")
    }
    
    /**
     * 停止监控
     */
    fun stopMonitoring() {
        isMonitoring.set(false)
        monitorThread?.interrupt()
        monitorThread = null
    }
    
    /**
     * 执行完整检查
     */
    fun performCheck(forceRefresh: Boolean = false): ProtectionResult {
        val now = System.currentTimeMillis()
        
        // 使用缓存结果（5秒内）
        if (!forceRefresh && lastCheckResult != null && now - lastCheckTime < 5000) {
            return lastCheckResult!!
        }
        
        val threats = mutableListOf<ThreatInfo>()
        var maxLevel = THREAT_NONE
        
        // 1. 调试器检测
        if (isDebuggerAttached()) {
            threats.add(ThreatInfo("debugger", "调试器已连接", THREAT_HIGH))
            maxLevel = maxOf(maxLevel, THREAT_HIGH)
        }
        
        // 2. Frida 检测
        val fridaResult = detectFrida()
        if (fridaResult.detected) {
            threats.add(ThreatInfo("frida", fridaResult.details, THREAT_CRITICAL))
            maxLevel = maxOf(maxLevel, THREAT_CRITICAL)
        }
        
        // 3. Xposed/LSPosed 检测
        val xposedResult = detectXposed()
        if (xposedResult.detected) {
            threats.add(ThreatInfo("xposed", xposedResult.details, THREAT_MEDIUM))
            maxLevel = maxOf(maxLevel, THREAT_MEDIUM)
        }
        
        // 4. Root 检测
        if (isRooted()) {
            threats.add(ThreatInfo("root", "设备已 Root", THREAT_LOW))
            maxLevel = maxOf(maxLevel, THREAT_LOW)
        }
        
        // 5. 模拟器检测（仅在严格模式下）
        if (isEmulator()) {
            threats.add(ThreatInfo("emulator", "运行在模拟器中", THREAT_LOW))
            // 模拟器不提升威胁等级
        }
        
        // 6. 签名验证
        if (!verifySignature()) {
            threats.add(ThreatInfo("signature", "签名验证失败", THREAT_HIGH))
            maxLevel = maxOf(maxLevel, THREAT_HIGH)
        }
        
        // 7. 应用完整性检查
        if (!verifyAppIntegrity()) {
            threats.add(ThreatInfo("integrity", "应用完整性检查失败", THREAT_HIGH))
            maxLevel = maxOf(maxLevel, THREAT_HIGH)
        }
        
        // 8. 内存篡改检测
        if (detectMemoryTampering()) {
            threats.add(ThreatInfo("memory", "检测到内存篡改", THREAT_CRITICAL))
            maxLevel = maxOf(maxLevel, THREAT_CRITICAL)
        }
        
        val result = ProtectionResult(
            threatLevel = maxLevel,
            threats = threats,
            timestamp = now,
            isEmulator = isEmulator()
        )
        
        lastCheckResult = result
        lastCheckTime = now
        threatLevel.set(maxLevel)
        
        return result
    }
    
    /**
     * 快速检查（仅关键项）
     */
    fun quickCheck(): Boolean {
        // 模拟器环境下放宽检查
        if (isEmulator()) {
            return true
        }
        
        return !isDebuggerAttached() && !detectFrida().detected
    }
    
    /**
     * 获取当前威胁等级
     */
    fun getThreatLevel(): Int = threatLevel.get()
    
    // ==================== 检测方法 ====================
    
    /**
     * 检测调试器
     */
    private fun isDebuggerAttached(): Boolean {
        // Method1: Debug.isDebuggerConnected()
        if (Debug.isDebuggerConnected()) {
            return true
        }
        
        // Method2: 检查 TracerPid
        try {
            BufferedReader(FileReader("/proc/self/status")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("TracerPid:")) {
                        val tracerPid = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                        if (tracerPid != 0) {
                            return true
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略
        }
        
        // Method3: 检查调试标志
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable && Debug.isDebuggerConnected()) {
            return true
        }
        
        return false
    }
    
    /**
     * 检测 Frida
     */
    private fun detectFrida(): DetectionResult {
        val details = mutableListOf<String>()
        
        // Method1: 检查 Frida 端口
        val fridaPorts = listOf(27042, 27043, 27044, 27045)
        for (port in fridaPorts) {
            try {
                Socket().use { socket ->
                    socket.soTimeout = 100
                    socket.connect(InetSocketAddress("127.0.0.1", port), 100)
                    details.add("Frida 端口 $port 开放")
                    return DetectionResult(true, details.joinToString("; "))
                }
            } catch (e: Exception) {
                // 端口未开放，继续检查
            }
        }
        
        // Method2: 检查 /proc/self/maps
        try {
            BufferedReader(FileReader("/proc/self/maps")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val lowerLine = line!!.lowercase()
                    if (lowerLine.contains("frida") || 
                        lowerLine.contains("gadget") ||
                        (lowerLine.contains("agent") && lowerLine.contains(".so"))) {
                        details.add("Frida 库: $line")
                        return DetectionResult(true, details.joinToString("; "))
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略
        }
        
        // Method3: 检查 Frida 文件
        val fridaFiles = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/data/local/tmp/frida-agent.so"
        )
        for (file in fridaFiles) {
            if (File(file).exists()) {
                details.add("Frida 文件: $file")
                return DetectionResult(true, details.joinToString("; "))
            }
        }
        
        // Method4: 检查 D-Bus
        try {
            val dbus = File("/proc/${Process.myPid()}/fd")
            dbus.listFiles()?.forEach { fd ->
                try {
                    val link = fd.canonicalPath
                    if (link.contains("frida") || link.contains("linjector")) {
                        details.add("Frida FD: $link")
                        return DetectionResult(true, details.joinToString("; "))
                    }
                } catch (e: Exception) {
                    // 忽略
                }
            }
        } catch (e: Exception) {
            // 忽略
        }
        
        return DetectionResult(false, "")
    }
    
    /**
     * 检测 Xposed/LSPosed
     */
    private fun detectXposed(): DetectionResult {
        val details = mutableListOf<String>()
        
        // Method1: 检查 Xposed 类
        val xposedClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers",
            "de.robv.android.xposed.XC_MethodHook"
        )
        for (className in xposedClasses) {
            try {
                Class.forName(className)
                details.add("Xposed 类: $className")
                return DetectionResult(true, details.joinToString("; "))
            } catch (e: ClassNotFoundException) {
                // Class不存在，继续检查
            }
        }
        
        // Method2: 检查文件路径
        val xposedPaths = listOf(
            "/system/framework/XposedBridge.jar",
            "/system/lib/libxposed_art.so",
            "/system/lib64/libxposed_art.so",
            "/data/adb/lspd",
            "/data/adb/modules/zygisk_lsposed",
            "/data/adb/modules/riru_lsposed"
        )
        for (path in xposedPaths) {
            if (File(path).exists()) {
                details.add("Xposed 路径: $path")
                return DetectionResult(true, details.joinToString("; "))
            }
        }
        
        // Method3: 检查堆栈
        try {
            throw Exception("Stack trace check")
        } catch (e: Exception) {
            val stackTrace = e.stackTrace.joinToString("\n") { it.toString() }
            if (stackTrace.contains("xposed", ignoreCase = true) ||
                stackTrace.contains("lsposed", ignoreCase = true) ||
                stackTrace.contains("edxposed", ignoreCase = true)) {
                details.add("堆栈中发现 Xposed")
                return DetectionResult(true, details.joinToString("; "))
            }
        }
        
        return DetectionResult(false, "")
    }
    
    /**
     * 检测 Root
     */
    private fun isRooted(): Boolean {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/data/adb/magisk"
        )
        
        return rootPaths.any { File(it).exists() }
    }
    
    /**
     * 检测模拟器
     */
    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
    }
    
    /**
     * 验证签名
     */
    @Suppress("DEPRECATION")
    private fun verifySignature(): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                packageInfo.signatures
            }
            
            !signatures.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed", e)
            false
        }
    }
    
    /**
     * 验证应用完整性
     */
    private fun verifyAppIntegrity(): Boolean {
        return try {
            val apkPath = context.applicationInfo.sourceDir
            val apkFile = File(apkPath)
            
            // Check APK 文件存在
            if (!apkFile.exists()) return false
            
            // Check文件大小合理
            if (apkFile.length() < 1024) return false
            
            // 可以添加更多检查，如 DEX 文件哈希验证
            true
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed", e)
            false
        }
    }
    
    /**
     * 检测内存篡改
     */
    private fun detectMemoryTampering(): Boolean {
        // 简单的内存完整性检查
        // 实际生产环境可以添加更复杂的检查
        return try {
            // Check关键类是否被修改
            val cryptoClass = AesCryptoEngine::class.java
            val methods = cryptoClass.declaredMethods
            
            // 如果方法数量异常，可能被 Hook
            // 这是一个简化的检查
            methods.size < 5
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 保护检查结果
 */
data class ProtectionResult(
    val threatLevel: Int,
    val threats: List<ThreatInfo>,
    val timestamp: Long,
    val isEmulator: Boolean
) {
    val isSecure: Boolean
        get() = threatLevel <= RuntimeProtection.THREAT_LOW
    
    val shouldBlock: Boolean
        get() = threatLevel >= RuntimeProtection.THREAT_HIGH
}

/**
 * 威胁信息
 */
data class ThreatInfo(
    val type: String,
    val description: String,
    val level: Int
)

/**
 * 检测结果
 */
data class DetectionResult(
    val detected: Boolean,
    val details: String
)
