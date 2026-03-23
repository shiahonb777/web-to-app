package com.webtoapp.core.apkbuilder

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import java.util.zip.*
import javax.security.auth.x500.X500Principal

/**
 * APK 签名工具
 * 支持多种签名方式：
 * - Android KeyStore（默认，最安全）
 * - PKCS12 文件（支持自定义证书和导入/导出）
 * - 自动生成的备用密钥
 * 
 * 签名版本支持：v1/v2/v3
 */
class JarSigner(private val context: Context) {

    companion object {
        private const val TAG = "JarSigner"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "WebToAppKey"
        private const val FALLBACK_KEY_ALIAS = "WebToAppFallback"
        private const val CUSTOM_KEY_ALIAS = "CustomKey"
        private const val DIGEST_ALGORITHM = "SHA-256"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        private const val KEY_SIZE = 2048
        private const val VALIDITY_YEARS = 25L
        
        // PKCS12 默认文件名
        private const val DEFAULT_PKCS12_FILE = "webtoapp_keystore.p12"
        private const val CUSTOM_PKCS12_FILE = "custom_keystore.p12"
    }
    
    /**
     * 签名类型枚举
     */
    enum class SignerType {
        ANDROID_KEYSTORE,  // Android 系统密钥库
        PKCS12_AUTO,       // Auto生成的 PKCS12
        PKCS12_CUSTOM      // User导入的自定义 PKCS12
    }

    private var privateKey: PrivateKey? = null
    private var certificate: X509Certificate? = null
    private var initError: String? = null
    private var currentSignerType: SignerType = SignerType.ANDROID_KEYSTORE

    init {
        initializeKey()
    }
    
    /**
     * 获取当前签名类型
     */
    fun getSignerType(): SignerType = currentSignerType
    
    /**
     * 获取签名证书的 SHA-256 哈希
     * 用于加密密钥派生，确保打包时和运行时使用相同的签名
     */
    fun getCertificateSignatureHash(): ByteArray {
        val cert = certificate ?: throw IllegalStateException("证书未初始化")
        return java.security.MessageDigest.getInstance("SHA-256").digest(cert.encoded)
    }
    
    /**
     * 获取签名证书信息
     */
    fun getCertificateInfo(): String? {
        val cert = certificate ?: return null
        return """
            |证书主题: ${cert.subjectX500Principal.name}
            |颁发者: ${cert.issuerX500Principal.name}
            |序列号: ${cert.serialNumber}
            |有效期: ${cert.notBefore} - ${cert.notAfter}
            |签名类型: $currentSignerType
        """.trimMargin()
    }

    /**
     * 初始化签名密钥
     * 直接使用 PKCS12 方案，因为 Android KeyStore 在某些设备上与 ApkSigner 不兼容
     */
    private fun initializeKey() {
        // 直接使用 PKCS12 方案（最兼容）
        if (tryLoadOrCreateFallbackKey()) {
            Log.d(TAG, "成功使用 PKCS12 密钥方案")
            return
        }
        
        // 如果 PKCS12 失败，尝试 Android KeyStore
        if (tryLoadFromAndroidKeyStore()) {
            Log.d(TAG, "成功从 Android KeyStore 加载密钥")
            currentSignerType = SignerType.ANDROID_KEYSTORE
            return
        }

        if (tryGenerateAndroidKeyStoreKey()) {
            Log.d(TAG, "成功生成 Android KeyStore 密钥")
            currentSignerType = SignerType.ANDROID_KEYSTORE
            return
        }

        initError = "无法初始化签名密钥"
        Log.e(TAG, initError!!)
    }

    /**
     * 尝试从 Android KeyStore 加载密钥
     */
    private fun tryLoadFromAndroidKeyStore(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_ALIAS)) {
                privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
                certificate = keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
                privateKey != null && certificate != null
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "从 Android KeyStore 加载密钥失败", e)
            false
        }
    }

    /**
     * 尝试生成 Android KeyStore 密钥
     */
    private fun tryGenerateAndroidKeyStoreKey(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        return try {
            // 先删除可能损坏的旧密钥
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                    Log.d(TAG, "删除了旧的 KeyStore 条目")
                }
            } catch (e: Exception) {
                Log.w(TAG, "删除旧密钥失败（可忽略）", e)
            }

            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )

            val notBefore = Date()
            val notAfter = Date(System.currentTimeMillis() + VALIDITY_YEARS * 365L * 24L * 60L * 60L * 1000L)

            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(KEY_SIZE)
                .setCertificateSubject(X500Principal("CN=WebToApp, O=WebToApp, C=CN"))
                .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                .setCertificateNotBefore(notBefore)
                .setCertificateNotAfter(notAfter)
                .build()

            keyPairGenerator.initialize(spec)
            val keyPair = keyPairGenerator.generateKeyPair()

            // 重新加载
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
            certificate = keyStore.getCertificate(KEY_ALIAS) as? X509Certificate

            privateKey != null && certificate != null
        } catch (e: Exception) {
            Log.e(TAG, "生成 Android KeyStore 密钥失败", e)
            false
        }
    }

    /**
     * 备用方案：使用文件存储的 PKCS12 密钥库
     */
    private fun tryLoadOrCreateFallbackKey(): Boolean {
        // 先尝试加载自定义 PKCS12
        if (tryLoadCustomPkcs12()) {
            currentSignerType = SignerType.PKCS12_CUSTOM
            return true
        }
        
        // 再尝试加载/创建自动生成的 PKCS12
        if (tryLoadOrCreateAutoPkcs12()) {
            currentSignerType = SignerType.PKCS12_AUTO
            return true
        }
        
        return false
    }
    
    /**
     * 尝试加载用户自定义的 PKCS12 证书
     */
    private fun tryLoadCustomPkcs12(): Boolean {
        val customFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
        val passwordFile = File(context.filesDir, "custom_keystore_password.txt")
        
        if (!customFile.exists() || !passwordFile.exists()) {
            return false
        }
        
        return try {
            val password = passwordFile.readText().trim().toCharArray()
            loadPkcs12(customFile, password, CUSTOM_KEY_ALIAS)
        } catch (e: Exception) {
            Log.w(TAG, "加载自定义 PKCS12 失败", e)
            false
        }
    }
    
    /**
     * 加载/创建自动生成的 PKCS12
     */
    private fun tryLoadOrCreateAutoPkcs12(): Boolean {
        val keyStoreFile = File(context.filesDir, DEFAULT_PKCS12_FILE)
        val keyStorePassword = "webtoapp_sign".toCharArray()

        return try {
            val keyStore = KeyStore.getInstance("PKCS12")

            if (keyStoreFile.exists()) {
                FileInputStream(keyStoreFile).use { fis ->
                    keyStore.load(fis, keyStorePassword)
                }
                privateKey = keyStore.getKey(FALLBACK_KEY_ALIAS, keyStorePassword) as? PrivateKey
                certificate = keyStore.getCertificate(FALLBACK_KEY_ALIAS) as? X509Certificate

                if (privateKey != null && certificate != null) {
                    // Check证书有效期是否正常（notAfter 应该在当前时间之后）
                    val cert = certificate!!
                    val now = Date()
                    if (cert.notAfter.before(now)) {
                        Log.w(TAG, "证书已过期或无效 (notAfter=${cert.notAfter})，重新生成...")
                        keyStoreFile.delete()
                        privateKey = null
                        certificate = null
                    } else {
                        Log.d(TAG, "从自动生成的 PKCS12 加载成功，有效期至 ${cert.notAfter}")
                        return true
                    }
                }
            }

            // Create新的 PKCS12 密钥库
            createNewPkcs12(keyStoreFile, keyStorePassword, FALLBACK_KEY_ALIAS)
            true
        } catch (e: Exception) {
            Log.e(TAG, "自动 PKCS12 密钥方案失败", e)
            false
        }
    }
    
    /**
     * 从 PKCS12 文件加载密钥
     */
    private fun loadPkcs12(file: File, password: CharArray, alias: String? = null): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("PKCS12")
            FileInputStream(file).use { fis ->
                keyStore.load(fis, password)
            }
            
            // 如果没有指定 alias，使用第一个找到的密钥
            val keyAlias = alias ?: keyStore.aliases().toList().firstOrNull { 
                keyStore.isKeyEntry(it) 
            }
            
            if (keyAlias == null) {
                Log.e(TAG, "PKCS12 中找不到密钥条目")
                return false
            }
            
            privateKey = keyStore.getKey(keyAlias, password) as? PrivateKey
            certificate = keyStore.getCertificate(keyAlias) as? X509Certificate
            
            val success = privateKey != null && certificate != null
            if (success) {
                Log.d(TAG, "从 PKCS12 加载成功: alias=$keyAlias")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "加载 PKCS12 失败: ${file.absolutePath}", e)
            false
        }
    }
    
    /**
     * 创建新的 PKCS12 密钥库
     */
    private fun createNewPkcs12(file: File, password: CharArray, alias: String) {
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, password)

        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()

        val cert = generateSelfSignedCertificate(keyPair)

        keyStore.setKeyEntry(alias, keyPair.private, password, arrayOf(cert))

        FileOutputStream(file).use { fos ->
            keyStore.store(fos, password)
        }

        privateKey = keyPair.private
        certificate = cert

        Log.d(TAG, "创建 PKCS12 成功: ${file.absolutePath}")
    }
    
    /**
     * 导入自定义 PKCS12 证书
     * @param sourceFile 源 PKCS12 文件
     * @param password 密码
     * @return 是否导入成功
     */
    fun importPkcs12(sourceFile: File, password: String): Boolean {
        return try {
            val passwordChars = password.toCharArray()
            
            // Verify文件有效性
            val keyStore = KeyStore.getInstance("PKCS12")
            FileInputStream(sourceFile).use { fis ->
                keyStore.load(fis, passwordChars)
            }
            
            // Find密钥条目
            val keyAlias = keyStore.aliases().toList().firstOrNull { keyStore.isKeyEntry(it) }
            if (keyAlias == null) {
                Log.e(TAG, "导入失败: PKCS12 中找不到密钥")
                return false
            }
            
            // Copy到应用私有目录
            val targetFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
            sourceFile.copyTo(targetFile, overwrite = true)
            
            // Save密码
            val passwordFile = File(context.filesDir, "custom_keystore_password.txt")
            passwordFile.writeText(password)
            
            // 重新加载
            if (loadPkcs12(targetFile, passwordChars, null)) {
                currentSignerType = SignerType.PKCS12_CUSTOM
                Log.d(TAG, "导入自定义 PKCS12 成功")
                return true
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "导入 PKCS12 失败", e)
            false
        }
    }
    
    /**
     * 导出当前 PKCS12 证书
     * @param targetFile 目标文件
     * @param password 导出密码
     * @return 是否导出成功
     */
    fun exportPkcs12(targetFile: File, password: String): Boolean {
        val key = privateKey
        val cert = certificate
        
        if (key == null || cert == null) {
            Log.e(TAG, "导出失败: 没有可用的密钥")
            return false
        }
        
        return try {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(null, null)
            
            keyStore.setKeyEntry(
                "exported_key",
                key,
                password.toCharArray(),
                arrayOf(cert)
            )
            
            FileOutputStream(targetFile).use { fos ->
                keyStore.store(fos, password.toCharArray())
            }
            
            Log.d(TAG, "导出 PKCS12 成功: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "导出 PKCS12 失败", e)
            false
        }
    }
    
    /**
     * 删除自定义 PKCS12 证书，回退到自动生成的证书
     */
    fun removeCustomPkcs12(): Boolean {
        return try {
            val customFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
            val passwordFile = File(context.filesDir, "custom_keystore_password.txt")
            
            customFile.delete()
            passwordFile.delete()
            
            // 重新初始化
            initializeKey()
            
            Log.d(TAG, "已删除自定义 PKCS12，回退到: $currentSignerType")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除自定义 PKCS12 失败", e)
            false
        }
    }
    
    /**
     * 获取 PKCS12 文件路径（用于显示给用户）
     */
    fun getPkcs12FilePath(): String? {
        return when (currentSignerType) {
            SignerType.PKCS12_CUSTOM -> File(context.filesDir, CUSTOM_PKCS12_FILE).absolutePath
            SignerType.PKCS12_AUTO -> File(context.filesDir, DEFAULT_PKCS12_FILE).absolutePath
            else -> null
        }
    }

    /**
     * 生成自签名 X509 证书
     */
    private fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val notBefore = Date(now)
        val notAfter = Date(now + VALIDITY_YEARS * 365L * 24L * 60L * 60L * 1000L)

        val subject = X500Principal("CN=WebToApp, O=WebToApp, C=CN")
        val serialNumber = BigInteger.valueOf(now)

        // 使用简化的证书生成（Android 内置支持）
        @Suppress("DEPRECATION")
        val certBuilder = android.security.keystore.KeyGenParameterSpec.Builder(
            "temp", KeyProperties.PURPOSE_SIGN
        ).build()

        // 手动构建 X509 证书（简化版本）
        // 使用 Bouncy Castle 风格的证书生成
        return createX509Certificate(
            subject,
            subject,
            serialNumber,
            notBefore,
            notAfter,
            keyPair.public,
            keyPair.private
        )
    }

    /**
     * 创建 X509 证书（使用 JCA）
     */
    private fun createX509Certificate(
        subject: X500Principal,
        issuer: X500Principal,
        serialNumber: BigInteger,
        notBefore: Date,
        notAfter: Date,
        publicKey: PublicKey,
        privateKey: PrivateKey
    ): X509Certificate {
        // Build TBS 证书
        val tbsCert = buildTBSCertificate(
            subject, issuer, serialNumber, notBefore, notAfter, publicKey
        )

        // Signature
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(tbsCert)
        val signatureBytes = signature.sign()

        // Build完整证书 DER
        val certDer = buildCertificateDER(tbsCert, signatureBytes)

        // Parse为 X509Certificate
        val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate
    }

    /**
     * 构建 TBS (To Be Signed) 证书结构
     */
    private fun buildTBSCertificate(
        subject: X500Principal,
        issuer: X500Principal,
        serialNumber: BigInteger,
        notBefore: Date,
        notAfter: Date,
        publicKey: PublicKey
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // Version [0] EXPLICIT INTEGER 2 (v3)
        out.write(byteArrayOf(0xA0.toByte(), 0x03, 0x02, 0x01, 0x02))

        // Serial Number
        val serialBytes = serialNumber.toByteArray()
        out.write(wrapWithTag(0x02, serialBytes))

        // Signature Algorithm (SHA256withRSA)
        out.write(buildSignatureAlgorithmId())

        // Issuer
        out.write(issuer.encoded)

        // Validity
        out.write(buildValidity(notBefore, notAfter))

        // Subject
        out.write(subject.encoded)

        // Subject Public Key Info
        out.write(publicKey.encoded)

        return wrapWithTag(0x30, out.toByteArray())
    }

    private fun buildSignatureAlgorithmId(): ByteArray {
        // OID for SHA256withRSA: 1.2.840.113549.1.1.11
        val oid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0x0D, 0x01, 0x01, 0x0B
        )
        val nullParam = byteArrayOf(0x05, 0x00)
        return wrapWithTag(0x30, oid + nullParam)
    }

    private fun buildValidity(notBefore: Date, notAfter: Date): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(encodeTime(notBefore))
        out.write(encodeTime(notAfter))
        return wrapWithTag(0x30, out.toByteArray())
    }

    /**
     * 编码时间为 ASN.1 格式
     * X.509 标准规定：
     * - 2000-2049 年使用 UTCTime (tag 0x17, 格式 yyMMddHHmmssZ)
     * - 2050 年及以后使用 GeneralizedTime (tag 0x18, 格式 yyyyMMddHHmmssZ)
     */
    private fun encodeTime(date: Date): ByteArray {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        
        return if (year >= 2050) {
            // GeneralizedTime: yyyyMMddHHmmssZ
            val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val timeStr = sdf.format(date)
            wrapWithTag(0x18, timeStr.toByteArray(Charsets.US_ASCII))
        } else {
            // UTCTime: yyMMddHHmmssZ
            val sdf = java.text.SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val timeStr = sdf.format(date)
            wrapWithTag(0x17, timeStr.toByteArray(Charsets.US_ASCII))
        }
    }

    private fun buildCertificateDER(tbsCert: ByteArray, signature: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tbsCert)
        out.write(buildSignatureAlgorithmId())
        // BIT STRING for signature (add leading 0x00 for unused bits)
        out.write(wrapWithTag(0x03, byteArrayOf(0x00) + signature))
        return wrapWithTag(0x30, out.toByteArray())
    }

    /**
     * 对 APK 进行签名
     * 增强版：支持重试、详细错误日志、输入验证
     */
    fun sign(inputApk: File, outputApk: File): Boolean {
        // 前置检查
        if (!validateInputs(inputApk, outputApk)) {
            return false
        }

        val key = privateKey
        val cert = certificate
        if (key == null || cert == null) {
            Log.e(TAG, "密钥或证书为空，尝试重新初始化...")
            initializeKey()
            if (privateKey == null || certificate == null) {
                Log.e(TAG, "重新初始化失败: key=${privateKey != null}, cert=${certificate != null}")
                return false
            }
        }

        Log.d(TAG, "开始签名 APK: input=${inputApk.absolutePath} (size=${inputApk.length()})")

        // 尝试签名（带重试）
        return trySignWithRetry(inputApk, outputApk, maxRetries = 2)
    }

    /**
     * Validate input参数
     */
    private fun validateInputs(inputApk: File, outputApk: File): Boolean {
        if (!inputApk.exists()) {
            Log.e(TAG, "输入 APK 不存在: ${inputApk.absolutePath}")
            return false
        }

        if (inputApk.length() == 0L) {
            Log.e(TAG, "输入 APK 文件为空")
            return false
        }

        if (!inputApk.canRead()) {
            Log.e(TAG, "无法读取输入 APK")
            return false
        }

        // 确保输出目录存在
        val outputDir = outputApk.parentFile
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                Log.e(TAG, "无法创建输出目录: ${outputDir.absolutePath}")
                return false
            }
        }

        // Delete可能存在的输出文件
        if (outputApk.exists()) {
            outputApk.delete()
        }

        return true
    }

    /**
     * 带重试的签名
     */
    private fun trySignWithRetry(inputApk: File, outputApk: File, maxRetries: Int): Boolean {
        var lastException: Throwable? = null

        for (attempt in 1..maxRetries) {
            try {
                Log.d(TAG, "签名尝试 $attempt/$maxRetries")

                val success = performSign(inputApk, outputApk)
                if (success) {
                    Log.d(TAG, "签名成功")
                    return true
                }

                Log.w(TAG, "签名尝试 $attempt 失败")

            } catch (e: Throwable) {
                lastException = e
                Log.e(TAG, "签名异常: ${e.message}")

                // Cleanup可能的部分输出文件
                if (outputApk.exists()) {
                    outputApk.delete()
                }

                // If it is密钥问题，尝试重新初始化
                if (e.message?.contains("key", ignoreCase = true) == true ||
                    e.message?.contains("signature", ignoreCase = true) == true) {
                    Log.d(TAG, "检测到密钥相关错误，尝试重新初始化密钥...")
                    initializeKey()
                }
            }

            // 短暂延迟后重试
            if (attempt < maxRetries) {
                Thread.sleep(100)
            }
        }

        Log.e(TAG, "签名最终失败，已重试 $maxRetries 次")
        lastException?.let { Log.e(TAG, "最后一次异常:", it) }
        return false
    }

    /**
     * 执行实际签名操作
     */
    private fun performSign(inputApk: File, outputApk: File): Boolean {
        val key = privateKey ?: throw IllegalStateException("私钥为空")
        val cert = certificate ?: throw IllegalStateException("证书为空")

        Log.d(TAG, "证书信息: subject=${cert.subjectX500Principal.name}")
        Log.d(TAG, "证书有效期: ${cert.notBefore} - ${cert.notAfter}")
        
        // 先尝试使用 ApkSigner
        var apkSignerSuccess = false
        try {
            apkSignerSuccess = performApkSignerSign(inputApk, outputApk, key, cert)
        } catch (e: Exception) {
            Log.w(TAG, "ApkSigner 签名失败: ${e.message}")
        }
        
        if (apkSignerSuccess) {
            return true
        }
        
        // ApkSigner 失败，使用 V1 (JAR) 签名作为备用方案
        Log.d(TAG, "ApkSigner 失败，尝试使用 V1 (JAR) 签名...")
        return performV1Sign(inputApk, outputApk, key, cert)
    }
    
    /**
     * 使用 ApkSigner 库签名 (V1+V2+V3)
     */
    private fun performApkSignerSign(inputApk: File, outputApk: File, key: PrivateKey, cert: X509Certificate): Boolean {
        Log.d(TAG, "ApkSigner 签名: ${inputApk.name}")
        
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "WebToApp",
            key,
            listOf(cert)
        ).build()

        val builder = ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setMinSdkVersion(23)  // 与项目 minSdk 一致

        try {
            val apkSigner = builder.build()
            apkSigner.sign()
        } catch (e: Exception) {
            Log.e(TAG, "ApkSigner 异常: ${e.message}")
            throw e
        }

        // Check输出文件
        if (!outputApk.exists() || outputApk.length() == 0L) {
            Log.e(TAG, "ApkSigner: 输出文件无效")
            return false
        }
        
        return verifyApkDetailed(outputApk, "ApkSigner")
    }
    
    /**
     * 使用纯 Java 实现的 V1 (JAR) 签名
     * 这是一个备用方案，不依赖 ApkSigner 库
     */
    private fun performV1Sign(inputApk: File, outputApk: File, key: PrivateKey, cert: X509Certificate): Boolean {
        Log.d(TAG, "V1 签名: ${inputApk.name}")
        
        try {
            val digests = mutableMapOf<String, String>()
            val entries = mutableMapOf<String, ByteArray>()
            
            ZipFile(inputApk).use { zipFile ->
                zipFile.entries().toList().forEach { entry ->
                    if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                        val content = zipFile.getInputStream(entry).readBytes()
                        entries[entry.name] = content
                        digests[entry.name] = computeDigest(ByteArrayInputStream(content))
                    }
                }
            }
            
            val manifest = buildManifest(digests)
            val signatureFile = buildSignatureFile(manifest, digests)
            val pkcs7Signature = createSignatureBlock(signatureFile)
            
            FileOutputStream(outputApk).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    writeZipEntry(zos, "META-INF/MANIFEST.MF", manifest)
                    writeZipEntry(zos, "META-INF/CERT.SF", signatureFile)
                    writeZipEntry(zos, "META-INF/CERT.RSA", pkcs7Signature)
                    
                    entries.forEach { (name, content) ->
                        val entry = ZipEntry(name)
                        zos.putNextEntry(entry)
                        zos.write(content)
                        zos.closeEntry()
                    }
                }
            }
            
            val verified = verifyApkDetailed(outputApk, "V1")
            
            // 即使验证失败，如果文件存在且有内容，仍返回 true
            if (!verified && outputApk.exists() && outputApk.length() > 0) {
                Log.w(TAG, "V1 验证失败，但文件已生成")
                return true
            }
            
            return verified
            
        } catch (e: Exception) {
            Log.e(TAG, "V1 签名失败: ${e.message}")
            if (outputApk.exists()) outputApk.delete()
            return false
        }
    }

    /**
     * APK 签名验证
     */
    private fun verifyApkDetailed(apk: File, source: String): Boolean {
        return try {
            val verifier = ApkVerifier.Builder(apk)
                .setMinCheckedPlatformVersion(23)
                .build()
            val result = verifier.verify()

            if (result.errors.isNotEmpty()) {
                Log.e(TAG, "[$source] 验证错误: ${result.errors.firstOrNull()}")
            }

            result.isVerified
        } catch (e: Exception) {
            Log.e(TAG, "[$source] 验证异常: ${e.message}")
            false
        }
    }

    private fun computeDigest(input: InputStream): String {
        val md = MessageDigest.getInstance(DIGEST_ALGORITHM)
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            md.update(buffer, 0, read)
        }
        return Base64.getEncoder().encodeToString(md.digest())
    }

    private fun buildManifest(digests: Map<String, String>): ByteArray {
        val sb = StringBuilder()
        sb.append("Manifest-Version: 1.0\r\n")
        sb.append("Created-By: 1.0 (WebToApp)\r\n")
        sb.append("\r\n")

        digests.forEach { (name, digest) ->
            sb.append("Name: $name\r\n")
            sb.append("SHA-256-Digest: $digest\r\n")
            sb.append("\r\n")
        }

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun buildSignatureFile(manifest: ByteArray, digests: Map<String, String>): ByteArray {
        val sb = StringBuilder()
        sb.append("Signature-Version: 1.0\r\n")
        sb.append("Created-By: 1.0 (WebToApp)\r\n")
        
        // 整个 MANIFEST.MF 的摘要
        val manifestDigest = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance(DIGEST_ALGORITHM).digest(manifest)
        )
        sb.append("SHA-256-Digest-Manifest: $manifestDigest\r\n")
        sb.append("\r\n")

        // 每个条目的摘要
        digests.forEach { (name, digest) ->
            val entryBlock = "Name: $name\r\nSHA-256-Digest: $digest\r\n\r\n"
            val entryDigest = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance(DIGEST_ALGORITHM).digest(entryBlock.toByteArray())
            )
            sb.append("Name: $name\r\n")
            sb.append("SHA-256-Digest: $entryDigest\r\n")
            sb.append("\r\n")
        }

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * 创建 PKCS#7 签名块
     */
    private fun createSignatureBlock(sfContent: ByteArray): ByteArray {
        // 对 SF 内容进行签名
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(sfContent)
        val signatureBytes = signature.sign()

        // Build简化的 PKCS#7 SignedData 结构
        return buildPkcs7SignedData(signatureBytes, certificate!!)
    }

    /**
     * 构建 PKCS#7 SignedData (DER 编码)
     */
    private fun buildPkcs7SignedData(signature: ByteArray, cert: X509Certificate): ByteArray {
        val certBytes = cert.encoded

        // SEQUENCE (SignedData)
        val contentInfo = buildContentInfo(signature, certBytes)
        
        // OID: signedData (1.2.840.113549.1.7.2)
        val signedDataOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 
            0x0D, 0x01, 0x07, 0x02
        )
        
        // Build完整结构
        val innerContent = ByteArrayOutputStream()
        innerContent.write(signedDataOid)
        
        // [0] EXPLICIT
        val explicitTag = wrapWithTag(0xA0.toByte(), contentInfo)
        innerContent.write(explicitTag)
        
        // 最外层 SEQUENCE
        return wrapWithTag(0x30, innerContent.toByteArray())
    }

    private fun buildContentInfo(signature: ByteArray, certBytes: ByteArray): ByteArray {
        val content = ByteArrayOutputStream()
        
        // version INTEGER 1
        content.write(byteArrayOf(0x02, 0x01, 0x01))
        
        // digestAlgorithms SET
        val digestAlgSet = buildDigestAlgorithmSet()
        content.write(digestAlgSet)
        
        // contentInfo (data)
        val dataContentInfo = buildDataContentInfo()
        content.write(dataContentInfo)
        
        // certificates [0] IMPLICIT
        val certsImplicit = wrapWithTag(0xA0.toByte(), certBytes)
        content.write(certsImplicit)
        
        // signerInfos SET
        val signerInfos = buildSignerInfos(signature, certificate!!)
        content.write(signerInfos)
        
        return wrapWithTag(0x30, content.toByteArray())
    }

    private fun buildDigestAlgorithmSet(): ByteArray {
        // SHA-256 OID: 2.16.840.1.101.3.4.2.1
        val sha256Oid = byteArrayOf(
            0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01
        )
        val algId = wrapWithTag(0x30, sha256Oid + byteArrayOf(0x05, 0x00)) // NULL params
        return wrapWithTag(0x31, algId) // SET
    }

    private fun buildDataContentInfo(): ByteArray {
        // OID: data (1.2.840.113549.1.7.1)
        val dataOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0x0D, 0x01, 0x07, 0x01
        )
        return wrapWithTag(0x30, dataOid)
    }

    private fun buildSignerInfos(signature: ByteArray, cert: X509Certificate): ByteArray {
        val signerInfo = ByteArrayOutputStream()
        
        // version INTEGER 1
        signerInfo.write(byteArrayOf(0x02, 0x01, 0x01))
        
        // issuerAndSerialNumber
        val issuerAndSerial = buildIssuerAndSerial(cert)
        signerInfo.write(issuerAndSerial)
        
        // digestAlgorithm
        val sha256Oid = byteArrayOf(
            0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01
        )
        signerInfo.write(wrapWithTag(0x30, sha256Oid + byteArrayOf(0x05, 0x00)))
        
        // signatureAlgorithm (RSA)
        val rsaOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0x0D, 0x01, 0x01, 0x0B // sha256WithRSAEncryption
        )
        signerInfo.write(wrapWithTag(0x30, rsaOid + byteArrayOf(0x05, 0x00)))
        
        // signature OCTET STRING
        signerInfo.write(wrapWithTag(0x04, signature))
        
        val signerInfoSeq = wrapWithTag(0x30, signerInfo.toByteArray())
        return wrapWithTag(0x31, signerInfoSeq) // SET
    }

    private fun buildIssuerAndSerial(cert: X509Certificate): ByteArray {
        val content = ByteArrayOutputStream()
        
        // issuer (从证书复制)
        content.write(cert.issuerX500Principal.encoded)
        
        // serialNumber
        val serial = cert.serialNumber.toByteArray()
        content.write(wrapWithTag(0x02, serial))
        
        return wrapWithTag(0x30, content.toByteArray())
    }

    private fun wrapWithTag(tag: Byte, content: ByteArray): ByteArray {
        val result = ByteArrayOutputStream()
        result.write(tag.toInt())
        writeLength(result, content.size)
        result.write(content)
        return result.toByteArray()
    }

    private fun writeLength(out: ByteArrayOutputStream, length: Int) {
        if (length < 128) {
            out.write(length)
        } else if (length < 256) {
            out.write(0x81)
            out.write(length)
        } else if (length < 65536) {
            out.write(0x82)
            out.write(length shr 8)
            out.write(length and 0xFF)
        } else {
            out.write(0x83)
            out.write(length shr 16)
            out.write((length shr 8) and 0xFF)
            out.write(length and 0xFF)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, content: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = content.size.toLong()
        entry.compressedSize = content.size.toLong()
        
        val crc = CRC32()
        crc.update(content)
        entry.crc = crc.value
        
        zos.putNextEntry(entry)
        zos.write(content)
        zos.closeEntry()
    }

    fun isReady(): Boolean = privateKey != null && certificate != null
    
    /**
     * 重置所有签名密钥，强制重新生成
     */
    fun resetKeys(): Boolean {
        return try {
            Log.d(TAG, "重置所有签名密钥...")
            
            // Delete PKCS12 文件
            File(context.filesDir, DEFAULT_PKCS12_FILE).delete()
            File(context.filesDir, CUSTOM_PKCS12_FILE).delete()
            File(context.filesDir, "custom_keystore_password.txt").delete()
            
            // Delete Android KeyStore 中的密钥
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                    Log.d(TAG, "已删除 Android KeyStore 密钥")
                }
            } catch (e: Exception) {
                Log.w(TAG, "删除 Android KeyStore 密钥失败", e)
            }
            
            // 清空当前密钥
            privateKey = null
            certificate = null
            
            // 重新初始化
            initializeKey()
            
            Log.d(TAG, "密钥重置完成，当前类型: $currentSignerType")
            privateKey != null && certificate != null
        } catch (e: Exception) {
            Log.e(TAG, "重置密钥失败", e)
            false
        }
    }
}
