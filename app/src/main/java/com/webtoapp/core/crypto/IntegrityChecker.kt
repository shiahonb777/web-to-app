package com.webtoapp.core.crypto

import android.content.Context
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.util.zip.ZipFile





class IntegrityChecker(private val context: Context) {

    companion object {
        private const val TAG = "IntegrityChecker"



        private const val EXPECTED_SIGNATURE_PLACEHOLDER = "SIGNATURE_HASH_PLACEHOLDER_DO_NOT_MODIFY"
    }

    private val keyManager = KeyManager.getInstance(context)





    fun check(): IntegrityResult {
        val results = mutableListOf<CheckItem>()


        results.add(checkSignature())


        results.add(checkDebugMode())


        results.add(checkInstaller())


        results.add(checkApkIntegrity())


        results.add(checkEnvironment())

        val passed = results.all { it.passed }
        val failedChecks = results.filter { !it.passed }

        if (!passed) {
            AppLogger.w(TAG, "完整性检查失败: ${failedChecks.map { it.name }}")
        }

        return IntegrityResult(
            passed = passed,
            checks = results,
            failedChecks = failedChecks,
            errors = failedChecks.map { it.message }
        )
    }




    fun verifyAll(): IntegrityResult {
        return check()
    }





    fun quickCheck(): Boolean {

        if (isEmulator()) {
            AppLogger.d(TAG, "Running in emulator, performing signature check only")
            return checkSignature().passed
        }
        return checkSignature().passed && checkDebugMode().passed
    }




    private fun checkSignature(): CheckItem {
        return try {
            val currentSignature = keyManager.getAppSignature()
            val currentHash = currentSignature.toHexString()


            if (EXPECTED_SIGNATURE_PLACEHOLDER.contains("PLACEHOLDER")) {
                AppLogger.d(TAG, "开发版本，跳过签名检查")
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
            AppLogger.e(TAG, "签名检查失败", e)
            CheckItem("signature", false, "签名检查异常: ${e.message}")
        }
    }




    private fun checkDebugMode(): CheckItem {
        val isDebuggable = (context.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0


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




    private fun checkInstaller(): CheckItem {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }


            val allowedInstallers = setOf(
                "com.android.vending",
                "com.huawei.appmarket",
                "com.xiaomi.market",
                "com.oppo.market",
                "com.vivo.appstore",
                "com.tencent.android.qqdownloader",
                null
            )

            val passed = installer in allowedInstallers || installer == null

            CheckItem(
                name = "installer",
                passed = passed,
                message = "安装来源: ${installer ?: "未知"}"
            )
        } catch (e: Exception) {

            CheckItem("installer", true, "无法获取安装来源")
        }
    }




    private fun checkApkIntegrity(): CheckItem {
        return try {
            val apkPath = context.applicationInfo.sourceDir
            val apkFile = File(apkPath)

            if (!apkFile.exists()) {
                return CheckItem("apk_integrity", false, "APK 文件不存在")
            }


            ZipFile(apkFile).use { zip ->

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
            AppLogger.e(TAG, "APK 完整性检查失败", e)
            CheckItem("apk_integrity", false, "APK 完整性检查异常: ${e.message}")
        }
    }




    private fun checkEnvironment(): CheckItem {
        val issues = mutableListOf<String>()


        if (isEmulator()) {
            issues.add("模拟器环境")
        }


        if (isRooted()) {
            issues.add("Root 环境")
        }


        if (hasXposed()) {
            issues.add("Xposed 框架")
        }


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




    private fun hasXposed(): Boolean {
        return try {

            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }




    private fun hasFrida(): Boolean {

        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("127.0.0.1", 27042), 100)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}




data class IntegrityResult(
    val passed: Boolean,
    val checks: List<CheckItem>,
    val failedChecks: List<CheckItem>,
    val errors: List<String> = emptyList()
) {
    val isValid: Boolean get() = passed
}




data class CheckItem(
    val name: String,
    val passed: Boolean,
    val message: String
)
