package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.crypto.EnhancedCrypto
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 运行时自保护盾 (RASP - Runtime Application Self-Protection)
 * 
 * 区别于传统加固的被动检测，RASP 提供主动的运行时保护：
 * 
 * 1. DEX CRC 自校验：
 *    - 预计算 classes.dex 各 section 的 CRC32
 *    - 运行时周期性验证，检测热补丁/内存修改
 *    - 多点校验（多个线程在不同时间检查不同区域）
 * 
 * 2. 内存完整性监控：
 *    - 关键数据结构的 HMAC 标记
 *    - 定期验证 HMAC，检测内存篡改
 *    - 使用 canary 值检测缓冲区溢出
 * 
 * 3. JNI 调用链验证：
 *    - 验证 JNI 调用来源的合法性
 *    - 检查调用栈中是否有非预期的帧
 *    - 防止通过伪造 JNI 调用绕过安全检查
 * 
 * 4. 时序检测（反加速/减速）：
 *    - 使用多个时间源（SystemClock/System.nanoTime/NTP）
 *    - 检测时间流速异常（加速器/调试器单步执行）
 *    - 采用统计方法（移动平均）过滤正常波动
 * 
 * 5. 堆栈轨迹清洗：
 *    - 过滤内部实现细节
 *    - 替换敏感类名/方法名
 *    - 防止通过异常堆栈推断内部逻辑
 * 
 * 6. 多点签名验证：
 *    - Java 层验证（PackageManager）
 *    - Native 层验证（直接读取 APK 证书）
 *    - 延迟验证（应用运行一段时间后才检查）
 *    - 三方交叉验证（Java/Native/延迟 三个结果互相校验）
 * 
 * 7. APK 校验和验证：
 *    - DEX 文件 SHA-256 校验
 *    - 资源文件完整性校验
 *    - AndroidManifest.xml 校验
 *    - 签名块完整性验证
 * 
 * 8. 威胁响应策略：
 *    - 静默退出（延迟随机时间）
 *    - 随机崩溃（伪装成正常 bug）
 *    - 数据擦除（清除敏感数据）
 *    - 假数据注入（蜜罐模式）
 *    - 自毁机制（极端情况）
 */
class RuntimeShield(private val context: Context) {
    
    companion object {
        private const val TAG = "RuntimeShield"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }
    
    /**
     * RASP 配置
     */
    data class RaspConfig(
        val dexCrcVerify: Boolean = true,
        val memoryIntegrity: Boolean = false,
        val jniCallValidation: Boolean = false,
        val timingCheck: Boolean = false,
        val stackTraceFilter: Boolean = true,
        val multiPointSignatureVerify: Boolean = true,
        val apkChecksumValidation: Boolean = true,
        val resourceIntegrity: Boolean = false,
        val certificatePinning: Boolean = false,
        val responseStrategy: String = "SILENT_EXIT",
        val responseDelay: Int = 0,
        val enableHoneypot: Boolean = false,
        val enableSelfDestruct: Boolean = false
    )
    
    private val secureRandom = SecureRandom()
    
    /**
     * 写入 RASP 配置到 APK
     */
    fun writeRaspConfig(
        zipOut: ZipOutputStream,
        config: RaspConfig,
        packageName: String,
        signatureHash: ByteArray?
    ) {
        AppLogger.d(TAG, "写入 RASP 配置")
        
        // 写入主配置
        val configData = generateRaspConfigData(config)
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/rasp_config.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(configData)
        zipOut.closeEntry()
        
        // 写入签名验证数据
        if (config.multiPointSignatureVerify && signatureHash != null) {
            writeSignatureVerificationData(zipOut, packageName, signatureHash)
        }
        
        // 写入 APK 校验和数据
        if (config.apkChecksumValidation) {
            writeApkChecksumData(zipOut, packageName)
        }
        
        // 写入威胁响应配置
        writeThreatResponseConfig(zipOut, config)
        
        // 写入堆栈清洗规则
        if (config.stackTraceFilter) {
            writeStackFilterRules(zipOut)
        }
        
        // 写入时序检测配置
        if (config.timingCheck) {
            writeTimingCheckConfig(zipOut)
        }
        
        // 写入蜜罐配置
        if (config.enableHoneypot) {
            writeHoneypotConfig(zipOut)
        }
        
        AppLogger.d(TAG, "RASP 配置写入完成")
    }
    
    /**
     * 生成 RASP 配置数据
     */
    private fun generateRaspConfigData(config: RaspConfig): ByteArray {
        val data = ByteArrayOutputStream()
        
        // 魔数
        data.write(byteArrayOf(0x52, 0x41, 0x53, 0x50)) // "RASP"
        
        // 版本
        data.write(byteArrayOf(0x00, 0x01))
        
        // 配置位图
        var bitmap = 0
        if (config.dexCrcVerify) bitmap = bitmap or 0x01
        if (config.memoryIntegrity) bitmap = bitmap or 0x02
        if (config.jniCallValidation) bitmap = bitmap or 0x04
        if (config.timingCheck) bitmap = bitmap or 0x08
        if (config.stackTraceFilter) bitmap = bitmap or 0x10
        if (config.multiPointSignatureVerify) bitmap = bitmap or 0x20
        if (config.apkChecksumValidation) bitmap = bitmap or 0x40
        if (config.resourceIntegrity) bitmap = bitmap or (0x80)
        data.write(byteArrayOf(
            (bitmap shr 8).toByte(),
            bitmap.toByte()
        ))
        
        // 附加位图
        var bitmap2 = 0
        if (config.certificatePinning) bitmap2 = bitmap2 or 0x01
        if (config.enableHoneypot) bitmap2 = bitmap2 or 0x02
        if (config.enableSelfDestruct) bitmap2 = bitmap2 or 0x04
        data.write(byteArrayOf(bitmap2.toByte()))
        
        // DEX CRC 校验间隔（毫秒）
        if (config.dexCrcVerify) {
            data.write(byteArrayOf(0x00, 0x00, 0x1B, 0x58)) // 7000ms
        }
        
        // 内存完整性检查间隔（毫秒）
        if (config.memoryIntegrity) {
            data.write(byteArrayOf(0x00, 0x00, 0x27, 0x10)) // 10000ms
        }
        
        // 时序检测窗口大小
        if (config.timingCheck) {
            data.write(byteArrayOf(0x0A)) // 10 个采样点
        }
        
        // 随机填充
        val padding = ByteArray(16)
        secureRandom.nextBytes(padding)
        data.write(padding)
        
        return data.toByteArray()
    }
    
    /**
     * 写入多点签名验证数据
     * 
     * 签名哈希以加密形式存储在多个位置：
     * 1. 主配置文件
     * 2. assets 中的隐藏文件
     * 3. Native SO 中的数据段
     * 
     * 运行时三个位置的值必须一致，否则判定为篡改
     */
    private fun writeSignatureVerificationData(
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray
    ) {
        val data = ByteArrayOutputStream()
        
        // 头部
        data.write(byteArrayOf(0x53, 0x49, 0x47, 0x56)) // "SIGV"
        
        // 验证点数量
        data.write(byteArrayOf(0x03)) // 3 个验证点
        
        // 加密签名哈希
        val encKey = EnhancedCrypto.HKDF.derive(
            ikm = packageName.toByteArray(),
            salt = "SignatureVerify:Salt".toByteArray(),
            info = "AES-256-GCM:SigHash".toByteArray(),
            length = 32
        )
        
        // 验证点 1: 直接加密存储
        val encSig1 = encryptData(signatureHash, encKey)
        data.write(byteArrayOf((encSig1.size shr 8).toByte(), encSig1.size.toByte()))
        data.write(encSig1)
        
        // 验证点 2: HMAC 存储
        val hmacKey = EnhancedCrypto.HKDF.derive(
            ikm = packageName.toByteArray(),
            salt = "SignatureVerify:HMAC".toByteArray(),
            info = "HMAC-SHA256".toByteArray(),
            length = 32
        )
        val hmac = computeHmac(signatureHash, hmacKey)
        data.write(byteArrayOf((hmac.size shr 8).toByte(), hmac.size.toByte()))
        data.write(hmac)
        
        // 验证点 3: 双重哈希存储
        val doubleHash = MessageDigest.getInstance("SHA-256").let { md ->
            md.update(signatureHash)
            md.update(packageName.toByteArray())
            md.digest()
        }
        data.write(byteArrayOf((doubleHash.size shr 8).toByte(), doubleHash.size.toByte()))
        data.write(doubleHash)
        
        // 延迟验证时间（秒，0=立即）
        data.write(byteArrayOf(0x00, 0x1E)) // 30 秒后执行延迟验证
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/sig_verify.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入 APK 校验和数据
     */
    private fun writeApkChecksumData(zipOut: ZipOutputStream, packageName: String) {
        val data = ByteArrayOutputStream()
        
        // 头部
        data.write(byteArrayOf(0x43, 0x4B, 0x53, 0x4D)) // "CKSM"
        
        // 校验项列表
        val checkItems = listOf(
            "classes.dex",
            "AndroidManifest.xml",
            "resources.arsc",
            "META-INF/"
        )
        data.write(checkItems.size.toByte().toInt())
        checkItems.forEach { item ->
            val bytes = item.toByteArray()
            data.write(bytes.size.toByte().toInt())
            data.write(bytes)
        }
        
        // 校验算法
        data.write(byteArrayOf(0x02)) // SHA-256
        
        // 校验策略
        data.write(byteArrayOf(
            0x01, // 启动时校验
            0x01, // 周期性校验
            0x01  // 敏感操作前校验
        ))
        
        // 校验间隔（秒）
        data.write(byteArrayOf(0x00, 0x3C)) // 60 秒
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/apk_checksum.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入威胁响应配置
     */
    private fun writeThreatResponseConfig(zipOut: ZipOutputStream, config: RaspConfig) {
        val data = ByteArrayOutputStream()
        
        // 头部
        data.write(byteArrayOf(0x54, 0x48, 0x52, 0x54)) // "THRT"
        
        // 响应策略编码
        val strategyCode = when (config.responseStrategy) {
            "LOG_ONLY" -> 0x00
            "SILENT_EXIT" -> 0x01
            "CRASH_RANDOM" -> 0x02
            "DATA_WIPE" -> 0x03
            "FAKE_DATA" -> 0x04
            else -> 0x01
        }
        data.write(byteArrayOf(strategyCode.toByte()))
        
        // 响应延迟（秒）
        data.write(byteArrayOf(
            (config.responseDelay shr 8).toByte(),
            config.responseDelay.toByte()
        ))
        
        // 延迟随机化范围（秒）
        data.write(byteArrayOf(0x00, 0x05)) // ±5 秒
        
        // 随机崩溃伪装异常列表
        if (config.responseStrategy == "CRASH_RANDOM") {
            val fakeExceptions = listOf(
                "java.lang.NullPointerException",
                "java.lang.ArrayIndexOutOfBoundsException",
                "java.lang.OutOfMemoryError",
                "android.view.WindowManager\$BadTokenException",
                "java.util.ConcurrentModificationException",
                "java.lang.IllegalStateException"
            )
            data.write(fakeExceptions.size.toByte().toInt())
            fakeExceptions.forEach { ex ->
                val bytes = ex.toByteArray()
                data.write(bytes.size.toByte().toInt())
                data.write(bytes)
            }
        }
        
        // 自毁配置
        if (config.enableSelfDestruct) {
            data.write(byteArrayOf(0x01)) // 启用
            // 自毁触发阈值（威胁等级）
            data.write(byteArrayOf(0x50)) // 80/100
            // 自毁范围
            data.write(byteArrayOf(
                0x01, // 清除 SharedPreferences
                0x01, // 清除数据库
                0x01, // 清除缓存
                0x00  // 不卸载应用（保留壳）
            ))
        }
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/threat_resp.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入堆栈清洗规则
     */
    private fun writeStackFilterRules(zipOut: ZipOutputStream) {
        val data = ByteArrayOutputStream()
        
        // 头部
        data.write(byteArrayOf(0x53, 0x54, 0x4B, 0x46)) // "STKF"
        
        // 需要过滤的包前缀
        val filterPrefixes = listOf(
            "com.webtoapp.core.crypto",
            "com.webtoapp.core.hardening",
            "com.webtoapp.core.protection",
            "sun.reflect",
            "java.lang.reflect"
        )
        data.write(filterPrefixes.size.toByte().toInt())
        filterPrefixes.forEach { prefix ->
            val bytes = prefix.toByteArray()
            data.write(bytes.size.toByte().toInt())
            data.write(bytes)
        }
        
        // 替换规则（将内部类名替换为通用名称）
        val replacements = listOf(
            "RuntimeShield" to "SystemService",
            "AntiReverse" to "AppConfig",
            "HardeningEngine" to "CoreManager",
            "DexProtector" to "ClassLoader",
            "SecurityInitializer" to "AppInit"
        )
        data.write(replacements.size.toByte().toInt())
        replacements.forEach { (from, to) ->
            val fromBytes = from.toByteArray()
            val toBytes = to.toByteArray()
            data.write(fromBytes.size.toByte().toInt())
            data.write(fromBytes)
            data.write(toBytes.size.toByte().toInt())
            data.write(toBytes)
        }
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/stack_filter.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入时序检测配置
     */
    private fun writeTimingCheckConfig(zipOut: ZipOutputStream) {
        val data = ByteArrayOutputStream()
        
        // 头部
        data.write(byteArrayOf(0x54, 0x49, 0x4D, 0x43)) // "TIMC"
        
        // 时间源
        data.write(byteArrayOf(0x03)) // 3 个时间源
        data.write(byteArrayOf(
            0x01, // SystemClock.elapsedRealtimeNanos
            0x02, // System.nanoTime
            0x03  // Thread.sleep 实际耗时
        ))
        
        // 采样窗口大小
        data.write(byteArrayOf(0x0A)) // 10
        
        // 异常阈值（倍率，超过此倍率判定为异常）
        data.write(byteArrayOf(0x03)) // 3x（允许 3 倍偏差）
        
        // 采样间隔（毫秒）
        data.write(byteArrayOf(0x00, 0x64)) // 100ms
        
        // 最小置信度（连续异常次数才触发报警）
        data.write(byteArrayOf(0x03)) // 连续 3 次
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/timing_check.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入蜜罐配置
     * 
     * 蜜罐模式：当检测到逆向行为时，不立即退出，
     * 而是注入虚假数据，让攻击者获得无用信息
     */
    private fun writeHoneypotConfig(zipOut: ZipOutputStream) {
        val data = ByteArrayOutputStream()
        
        // 头部
        data.write(byteArrayOf(0x48, 0x4F, 0x4E, 0x59)) // "HONY"
        
        // 蜜罐类型
        data.write(byteArrayOf(0x03)) // 3 种类型
        
        // 类型 1: 假 API 密钥
        data.write(byteArrayOf(0x01))
        val fakeApiKey = "sk-fake-${generateRandomHex(32)}"
        val fakeKeyBytes = fakeApiKey.toByteArray()
        data.write(byteArrayOf((fakeKeyBytes.size shr 8).toByte(), fakeKeyBytes.size.toByte()))
        data.write(fakeKeyBytes)
        
        // 类型 2: 假服务器地址
        data.write(byteArrayOf(0x02))
        val fakeServer = "https://honeypot-${generateRandomHex(8)}.example.com/api"
        val fakeServerBytes = fakeServer.toByteArray()
        data.write(byteArrayOf((fakeServerBytes.size shr 8).toByte(), fakeServerBytes.size.toByte()))
        data.write(fakeServerBytes)
        
        // 类型 3: 假数据库记录
        data.write(byteArrayOf(0x03))
        val fakeData = """{"users":[],"tokens":[],"status":"honeypot"}"""
        val fakeDataBytes = fakeData.toByteArray()
        data.write(byteArrayOf((fakeDataBytes.size shr 8).toByte(), fakeDataBytes.size.toByte()))
        data.write(fakeDataBytes)
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/honeypot.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }
    
    // ==================== 辅助方法 ====================
    
    private fun encryptData(data: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key.copyOf(32), "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        
        val encrypted = cipher.doFinal(data)
        
        return ByteArrayOutputStream().apply {
            write(iv)
            write(encrypted)
        }.toByteArray()
    }
    
    private fun computeHmac(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
    
    private fun generateRandomHex(length: Int): String {
        val bytes = ByteArray(length / 2)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
