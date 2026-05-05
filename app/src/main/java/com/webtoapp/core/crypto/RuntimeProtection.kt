package com.webtoapp.core.crypto

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.Process
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger






@SuppressLint("StaticFieldLeak")
class RuntimeProtection(private val context: Context) {

    companion object {
        private const val TAG = "RuntimeProtection"


        const val THREAT_NONE = 0
        const val THREAT_LOW = 1
        const val THREAT_MEDIUM = 2
        const val THREAT_HIGH = 3
        const val THREAT_CRITICAL = 4


        private const val CHECK_INTERVAL_MS = 5000L

        @Volatile
        private var instance: RuntimeProtection? = null

        fun getInstance(context: Context): RuntimeProtection {
            return instance ?: synchronized(this) {
                instance ?: RuntimeProtection(context.applicationContext).also { instance = it }
            }
        }
    }


    private val isMonitoring = AtomicBoolean(false)
    private val threatLevel = AtomicInteger(THREAT_NONE)
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null


    @Volatile
    private var lastCheckResult: ProtectionResult? = null
    private var lastCheckTime = 0L


    private var onThreatDetected: ((ProtectionResult) -> Unit)? = null




    fun setThreatCallback(callback: (ProtectionResult) -> Unit) {
        onThreatDetected = callback
    }




    fun startMonitoring() {
        if (isMonitoring.getAndSet(true)) return

        monitorJob = monitorScope.launch {
            while (isActive && isMonitoring.get()) {
                try {
                    val result = performCheck()
                    if (result.threatLevel >= THREAT_MEDIUM) {
                        onThreatDetected?.invoke(result)
                    }
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Monitor error", e)
                }
            }
        }

        AppLogger.d(TAG, "Runtime protection monitoring started")
    }




    fun stopMonitoring() {
        isMonitoring.set(false)
        monitorJob?.cancel()
        monitorJob = null
    }




    fun performCheck(forceRefresh: Boolean = false): ProtectionResult {
        val now = System.currentTimeMillis()


        if (!forceRefresh && now - lastCheckTime < 5000) {
            lastCheckResult?.let { return it }
        }

        val threats = mutableListOf<ThreatInfo>()
        var maxLevel = THREAT_NONE


        if (isDebuggerAttached()) {
            threats.add(ThreatInfo("debugger", "调试器已连接", THREAT_HIGH))
            maxLevel = maxOf(maxLevel, THREAT_HIGH)
        }


        val fridaResult = detectFrida()
        if (fridaResult.detected) {
            threats.add(ThreatInfo("frida", fridaResult.details, THREAT_CRITICAL))
            maxLevel = maxOf(maxLevel, THREAT_CRITICAL)
        }


        val xposedResult = detectXposed()
        if (xposedResult.detected) {
            threats.add(ThreatInfo("xposed", xposedResult.details, THREAT_MEDIUM))
            maxLevel = maxOf(maxLevel, THREAT_MEDIUM)
        }


        if (isRooted()) {
            threats.add(ThreatInfo("root", "设备已 Root", THREAT_LOW))
            maxLevel = maxOf(maxLevel, THREAT_LOW)
        }


        if (isEmulator()) {
            threats.add(ThreatInfo("emulator", "运行在模拟器中", THREAT_LOW))

        }


        if (!verifySignature()) {
            threats.add(ThreatInfo("signature", "签名验证失败", THREAT_HIGH))
            maxLevel = maxOf(maxLevel, THREAT_HIGH)
        }


        if (!verifyAppIntegrity()) {
            threats.add(ThreatInfo("integrity", "应用完整性检查失败", THREAT_HIGH))
            maxLevel = maxOf(maxLevel, THREAT_HIGH)
        }


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




    fun quickCheck(): Boolean {

        if (isEmulator()) {
            return true
        }

        return !isDebuggerAttached() && !detectFrida().detected
    }




    fun getThreatLevel(): Int = threatLevel.get()






    private fun isDebuggerAttached(): Boolean {

        if (Debug.isDebuggerConnected()) {
            return true
        }


        try {
            BufferedReader(FileReader("/proc/self/status")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.startsWith("TracerPid:")) {
                        val tracerPid = currentLine.substringAfter(":").trim().toIntOrNull() ?: 0
                        if (tracerPid != 0) {
                            return true
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {

        }


        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable && Debug.isDebuggerConnected()) {
            return true
        }

        return false
    }




    private fun detectFrida(): DetectionResult {
        val details = mutableListOf<String>()


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

            }
        }


        try {
            BufferedReader(FileReader("/proc/self/maps")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val lowerLine = (line ?: continue).lowercase()
                    if (lowerLine.contains("frida") ||
                        lowerLine.contains("gadget") ||
                        (lowerLine.contains("agent") && lowerLine.contains(".so"))) {
                        details.add("Frida 库: $line")
                        return DetectionResult(true, details.joinToString("; "))
                    }
                }
            }
        } catch (e: Exception) {

        }


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

                }
            }
        } catch (e: Exception) {

        }


        try {
            val taskDir = File("/proc/self/task")
            taskDir.listFiles()?.forEach { tidDir ->
                try {
                    val commFile = File(tidDir, "comm")
                    if (commFile.exists()) {
                        val threadName = commFile.readText().trim().lowercase()
                        if (threadName.contains("frida") ||
                            threadName.contains("gmain") ||
                            threadName == "gum-js-loop" ||
                            threadName == "pool-frida") {
                            details.add("Frida 线程: $threadName")
                            return DetectionResult(true, details.joinToString("; "))
                        }
                    }
                } catch (e: Exception) {

                }
            }
        } catch (e: Exception) {

        }


        val extraFridaPaths = listOf(
            "/data/local/tmp/frida-server-arm",
            "/data/local/tmp/frida-server-arm64",
            "/data/local/tmp/frida-server-x86",
            "/data/local/tmp/frida-server-x86_64",
            "/data/local/tmp/frida-gadget.so",
            "/data/local/tmp/frida-agent-64.so",
            "/data/local/tmp/frida-agent-32.so"
        )
        for (path in extraFridaPaths) {
            if (File(path).exists()) {
                details.add("Frida 文件: $path")
                return DetectionResult(true, details.joinToString("; "))
            }
        }

        return DetectionResult(false, "")
    }




    private fun detectXposed(): DetectionResult {
        val details = mutableListOf<String>()


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

            }
        }


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
            AppLogger.e(TAG, "Signature verification failed", e)
            false
        }
    }




    private fun verifyAppIntegrity(): Boolean {
        return try {
            val apkPath = context.applicationInfo.sourceDir
            val apkFile = File(apkPath)


            if (!apkFile.exists()) return false


            if (apkFile.length() < 1024) return false


            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Integrity check failed", e)
            false
        }
    }






    private fun detectMemoryTampering(): Boolean {
        return try {
            val apkPath = context.applicationInfo.sourceDir
            val zipFile = java.util.zip.ZipFile(apkPath)
            val dexEntry = zipFile.getEntry("classes.dex") ?: return false
            val currentCrc = dexEntry.crc
            zipFile.close()

            val prefs = context.getSharedPreferences("_rt_prot", Context.MODE_PRIVATE)
            val savedCrc = prefs.getLong("dex_crc", -1L)

            if (savedCrc == -1L) {

                prefs.edit().putLong("dex_crc", currentCrc).apply()
                return false
            }


            currentCrc != savedCrc
        } catch (e: Exception) {
            AppLogger.w(TAG, "DEX CRC check failed: ${e.message}")
            false
        }
    }
}




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




data class ThreatInfo(
    val type: String,
    val description: String,
    val level: Int
)




data class DetectionResult(
    val detected: Boolean,
    val details: String
)
