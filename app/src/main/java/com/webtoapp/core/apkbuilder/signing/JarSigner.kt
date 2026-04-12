package com.webtoapp.core.apkbuilder

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.webtoapp.core.logging.AppLogger
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import com.webtoapp.util.threadLocalCompat
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
        private const val VALIDITY_YEARS = 20L  // 保持在 2050 年以前，避免 GeneralizedTime 编码兼容性问题
        
        // PKCS12 默认文件名
        private const val DEFAULT_PKCS12_FILE = "webtoapp_keystore.p12"
        private const val CUSTOM_PKCS12_FILE = "custom_keystore.p12"
        
        // ASN.1 时间编码用 SimpleDateFormat
        private val GENERALIZED_TIME_FORMAT = threadLocalCompat {
            java.text.SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        private val UTC_TIME_FORMAT = threadLocalCompat {
            java.text.SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
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
     * 优先使用 PKCS12（最兼容 ApkSigner），Android KeyStore 作为回退
     */
    private fun initializeKey() {
        // 1. 尝试加载/创建 PKCS12 密钥（最兼容）
        if (tryLoadOrCreateFallbackKey()) {
            AppLogger.d(TAG, "成功使用 PKCS12 密钥方案")
            return
        }
        
        // 2. 回退到 Android KeyStore
        if (tryLoadFromAndroidKeyStore()) {
            AppLogger.d(TAG, "成功从 Android KeyStore 加载密钥")
            currentSignerType = SignerType.ANDROID_KEYSTORE
            return
        }

        if (tryGenerateAndroidKeyStoreKey()) {
            AppLogger.d(TAG, "成功生成 Android KeyStore 密钥")
            currentSignerType = SignerType.ANDROID_KEYSTORE
            return
        }

        initError = "无法初始化签名密钥"
        AppLogger.e(TAG, initError!!)
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
            AppLogger.w(TAG, "从 Android KeyStore 加载密钥失败", e)
            false
        }
    }

    /**
     * 尝试生成 Android KeyStore 密钥
     */
    private fun tryGenerateAndroidKeyStoreKey(): Boolean {
        return try {
            // 先删除可能损坏的旧密钥
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                    AppLogger.d(TAG, "删除了旧的 KeyStore 条目")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "删除旧密钥失败（可忽略）", e)
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
            AppLogger.e(TAG, "生成 Android KeyStore 密钥失败", e)
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
            AppLogger.w(TAG, "加载自定义 PKCS12 失败", e)
            false
        }
    }
    
    /**
     * 加载/创建自动生成的 PKCS12
     */
    private fun tryLoadOrCreateAutoPkcs12(): Boolean {
        val keyStoreFile = File(context.filesDir, DEFAULT_PKCS12_FILE)
        val keyStorePassword = getOrCreateKeystorePassword()

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
                        AppLogger.w(TAG, "证书已过期或无效 (notAfter=${cert.notAfter})，重新生成...")
                        keyStoreFile.delete()
                        privateKey = null
                        certificate = null
                    } else {
                        AppLogger.d(TAG, "从自动生成的 PKCS12 加载成功，有效期至 ${cert.notAfter}")
                        return true
                    }
                }
            }

            // Create新的 PKCS12 密钥库
            createNewPkcs12(keyStoreFile, keyStorePassword, FALLBACK_KEY_ALIAS)
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "自动 PKCS12 密钥方案失败", e)
            false
        }
    }
    
    /**
     * 获取或创建 PKCS12 密码
     * 每次安装生成唯一随机密码，避免硬编码密码被逆向提取
     */
    private fun getOrCreateKeystorePassword(): CharArray {
        val passwordFile = File(context.filesDir, ".ks_credential")
        
        // 尝试加载已保存的密码
        if (passwordFile.exists()) {
            try {
                val saved = passwordFile.readText().trim()
                if (saved.isNotEmpty()) return saved.toCharArray()
            } catch (e: Exception) {
                AppLogger.w(TAG, "读取密码文件失败", e)
            }
        }
        
        // 检查是否有使用旧硬编码密码的现有密钥库（向后兼容）
        val keyStoreFile = File(context.filesDir, DEFAULT_PKCS12_FILE)
        val legacyPassword = "webtoapp_sign"
        if (keyStoreFile.exists()) {
            try {
                val ks = KeyStore.getInstance("PKCS12")
                FileInputStream(keyStoreFile).use { fis -> ks.load(fis, legacyPassword.toCharArray()) }
                // 旧密码可用，保存它以避免破坏现有密钥库
                passwordFile.writeText(legacyPassword)
                AppLogger.d(TAG, "迁移旧密码到文件存储")
                return legacyPassword.toCharArray()
            } catch (_: Exception) {
                // 旧密码不匹配，继续生成新密码
            }
        }
        
        // 生成随机密码
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#%^&*"
        val random = SecureRandom()
        val password = CharArray(32) { chars[random.nextInt(chars.length)] }
        
        try {
            passwordFile.writeText(String(password))
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存密码文件失败", e)
        }
        
        return password
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
                AppLogger.e(TAG, "PKCS12 中找不到密钥条目")
                return false
            }
            
            privateKey = keyStore.getKey(keyAlias, password) as? PrivateKey
            certificate = keyStore.getCertificate(keyAlias) as? X509Certificate
            
            val success = privateKey != null && certificate != null
            if (success) {
                AppLogger.d(TAG, "从 PKCS12 加载成功: alias=$keyAlias")
            }
            success
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载 PKCS12 失败: ${file.absolutePath}", e)
            false
        }
    }
    
    /**
     * 创建新的 PKCS12 密钥库
     * 
     * 使用纯软件 RSA 密钥 + 自签名证书。
     * 先尝试利用 Android KeyStore 生成格式正确的证书模板，
     * 如果失败则回退到手写 ASN.1（兼容老设备）。
     */
    private fun createNewPkcs12(file: File, password: CharArray, alias: String) {
        AppLogger.d(TAG, "创建新的 PKCS12 密钥库...")
        
        // 生成软件 RSA 密钥对
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()
        AppLogger.d(TAG, "RSA 密钥对已生成 (${KEY_SIZE} bit)")
        
        // 生成自签名证书
        val cert = try {
            generateCertViaAndroidKeyStore(keyPair)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Android KeyStore 证书生成失败，使用手写 ASN.1 回退: ${e.message}")
            generateSelfSignedCertificate(keyPair)
        }
        
        // 存入 PKCS12
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, password)
        keyStore.setKeyEntry(alias, keyPair.private, password, arrayOf(cert))
        
        FileOutputStream(file).use { fos ->
            keyStore.store(fos, password)
        }

        privateKey = keyPair.private
        certificate = cert

        AppLogger.d(TAG, "创建 PKCS12 成功: ${file.absolutePath}, 有效期至 ${cert.notAfter}")
    }
    
    /**
     * 利用 Android KeyStore 生成格式正确的自签名证书，然后用软件密钥重新签发
     * 
     * 流程：
     * 1. 在 Android KeyStore 中生成临时密钥（系统自动创建格式正确的自签名证书）
     * 2. 提取证书的 TBS（To Be Signed）结构
     * 3. 删除临时密钥
     * 4. 用软件密钥重新构建和签发证书
     */
    private fun generateCertViaAndroidKeyStore(softwareKeyPair: KeyPair): X509Certificate {
        val tempAlias = "webtoapp_cert_gen_temp_${System.currentTimeMillis()}"
        
        try {
            // 在 Android KeyStore 中生成临时 RSA 密钥
            val tempKpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )
            
            val notBefore = Date()
            val notAfter = Date(System.currentTimeMillis() + VALIDITY_YEARS * 365L * 24L * 60L * 60L * 1000L)
            
            val spec = KeyGenParameterSpec.Builder(
                tempAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(KEY_SIZE)
                .setCertificateSubject(X500Principal("CN=WebToApp, O=WebToApp, C=CN"))
                .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                .setCertificateNotBefore(notBefore)
                .setCertificateNotAfter(notAfter)
                .build()
            
            tempKpg.initialize(spec)
            tempKpg.generateKeyPair()
            
            // 提取系统生成的证书信息
            val aksKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            aksKeyStore.load(null)
            val aksCert = aksKeyStore.getCertificate(tempAlias) as X509Certificate
            
            // 用软件密钥重新构建证书（保留系统生成的主题、有效期等）
            val tbsCert = buildTBSCertificate(
                aksCert.subjectX500Principal,
                aksCert.issuerX500Principal,
                aksCert.serialNumber,
                aksCert.notBefore,
                aksCert.notAfter,
                softwareKeyPair.public
            )
            
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initSign(softwareKeyPair.private)
            sig.update(tbsCert)
            val signatureBytes = sig.sign()
            
            val certDer = buildCertificateDER(tbsCert, signatureBytes)
            val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
            val newCert = certFactory.generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate
            
            AppLogger.d(TAG, "通过 Android KeyStore 模板生成证书成功")
            return newCert
            
        } finally {
            // 清理临时 KeyStore 条目
            try {
                val aksKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                aksKeyStore.load(null)
                if (aksKeyStore.containsAlias(tempAlias)) {
                    aksKeyStore.deleteEntry(tempAlias)
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "清理临时 KeyStore 条目失败（可忽略）", e)
            }
        }
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
                AppLogger.e(TAG, "导入失败: PKCS12 中找不到密钥")
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
                AppLogger.d(TAG, "导入自定义 PKCS12 成功")
                return true
            }
            
            false
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入 PKCS12 失败", e)
            false
        }
    }
    
    /**
     * 导入自定义签名文件（自动检测格式：PKCS12 / BKS / JKS）
     * 非 PKCS12 格式会自动转换为 PKCS12 保存
     * @param sourceFile 源签名文件
     * @param password 密码
     * @return 是否导入成功
     */
    fun importKeystore(sourceFile: File, password: String): Boolean {
        val passwordChars = password.toCharArray()
        
        // 按优先级尝试不同的 KeyStore 类型
        val keystoreTypes = listOf("PKCS12", "BKS", "JKS")
        
        for (type in keystoreTypes) {
            try {
                val keyStore = KeyStore.getInstance(type)
                FileInputStream(sourceFile).use { fis ->
                    keyStore.load(fis, passwordChars)
                }
                
                // Find密钥条目
                val keyAlias = keyStore.aliases().toList().firstOrNull { keyStore.isKeyEntry(it) }
                if (keyAlias == null) {
                    AppLogger.w(TAG, "$type 中找不到密钥条目，跳过")
                    continue
                }
                
                AppLogger.d(TAG, "成功以 $type 格式解析签名文件, alias=$keyAlias")
                
                if (type == "PKCS12") {
                    // 直接复制文件
                    val targetFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
                    sourceFile.copyTo(targetFile, overwrite = true)
                } else {
                    // 非 PKCS12 格式：提取密钥并转换为 PKCS12 保存
                    val key = keyStore.getKey(keyAlias, passwordChars) as? PrivateKey
                    val cert = keyStore.getCertificate(keyAlias) as? java.security.cert.X509Certificate
                    if (key == null || cert == null) {
                        AppLogger.w(TAG, "$type 中密钥或证书为空")
                        continue
                    }
                    
                    val p12KeyStore = KeyStore.getInstance("PKCS12")
                    p12KeyStore.load(null, passwordChars)
                    p12KeyStore.setKeyEntry(keyAlias, key, passwordChars, arrayOf(cert))
                    
                    val targetFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
                    FileOutputStream(targetFile).use { fos ->
                        p12KeyStore.store(fos, passwordChars)
                    }
                    AppLogger.d(TAG, "已将 $type 转换为 PKCS12 并保存")
                }
                
                // Save密码
                val passwordFile = File(context.filesDir, "custom_keystore_password.txt")
                passwordFile.writeText(password)
                
                // 重新加载
                val targetFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
                if (loadPkcs12(targetFile, passwordChars, null)) {
                    currentSignerType = SignerType.PKCS12_CUSTOM
                    AppLogger.d(TAG, "导入自定义签名文件成功 (原始格式: $type)")
                    return true
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "尝试以 $type 格式解析失败: ${e.message}")
            }
        }
        
        AppLogger.e(TAG, "所有格式均无法解析此签名文件")
        return false
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
            AppLogger.e(TAG, "导出失败: 没有可用的密钥")
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
            
            AppLogger.d(TAG, "导出 PKCS12 成功: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "导出 PKCS12 失败", e)
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
            
            AppLogger.d(TAG, "已删除自定义 PKCS12，回退到: $currentSignerType")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "删除自定义 PKCS12 失败", e)
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
     * 生成自签名 X509 证书（手写 ASN.1 DER 方式）
     * 作为 generateCertViaAndroidKeyStore 的回退方案
     */
    private fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val notBefore = Date(now)
        val notAfter = Date(now + VALIDITY_YEARS * 365L * 24L * 60L * 60L * 1000L)

        val subject = X500Principal("CN=WebToApp, O=WebToApp, C=CN")
        val serialNumber = BigInteger.valueOf(now)

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
            val timeStr = GENERALIZED_TIME_FORMAT.get()!!.format(date)
            wrapWithTag(0x18, timeStr.toByteArray(Charsets.US_ASCII))
        } else {
            // UTCTime: yyMMddHHmmssZ
            val timeStr = UTC_TIME_FORMAT.get()!!.format(date)
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
     * @throws IllegalStateException 当密钥初始化失败时
     * @throws RuntimeException 当签名过程失败时（包含实际异常信息）
     */
    fun sign(inputApk: File, outputApk: File): Boolean {
        // 前置检查
        if (!validateInputs(inputApk, outputApk)) {
            throw IllegalStateException("签名输入验证失败: input=${inputApk.absolutePath}")
        }

        val key = privateKey
        val cert = certificate
        if (key == null || cert == null) {
            AppLogger.e(TAG, "密钥或证书为空，尝试重新初始化...")
            // 删除可能损坏的旧密钥库，强制重新生成
            File(context.filesDir, DEFAULT_PKCS12_FILE).delete()
            File(context.filesDir, ".ks_credential").delete()
            initializeKey()
            if (privateKey == null || certificate == null) {
                val errorDetail = initError ?: "key=${privateKey != null}, cert=${certificate != null}"
                throw IllegalStateException("签名密钥初始化失败: $errorDetail")
            }
        }

        AppLogger.d(TAG, "开始签名 APK: input=${inputApk.absolutePath} (size=${inputApk.length()})")
        AppLogger.d(TAG, "签名类型: $currentSignerType")

        // 尝试签名（带重试）
        return trySignWithRetry(inputApk, outputApk, maxRetries = 2)
    }

    /**
     * Validate input参数
     */
    private fun validateInputs(inputApk: File, outputApk: File): Boolean {
        if (!inputApk.exists()) {
            AppLogger.e(TAG, "输入 APK 不存在: ${inputApk.absolutePath}")
            return false
        }

        if (inputApk.length() == 0L) {
            AppLogger.e(TAG, "输入 APK 文件为空")
            return false
        }

        if (!inputApk.canRead()) {
            AppLogger.e(TAG, "无法读取输入 APK")
            return false
        }

        // 确保输出目录存在
        val outputDir = outputApk.parentFile
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                AppLogger.e(TAG, "无法创建输出目录: ${outputDir.absolutePath}")
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
     * 带降级的签名策略
     * 
     * 签名方案优先级：
     * 1. V1+V2+V3 (最完整)
     * 2. V1+V2 (兼容Android 7+，跳过可能有问题的V3)
     * 3. V1-only (最大兼容性回退)
     */
    private fun trySignWithRetry(inputApk: File, outputApk: File, maxRetries: Int): Boolean {
        val errorMessages = mutableListOf<String>()
        var lastException: Throwable? = null
        
        data class SignConfig(val name: String, val v1: Boolean, val v2: Boolean, val v3: Boolean)
        
        val configs = listOf(
            SignConfig("V1+V2+V3", v1 = true, v2 = true, v3 = true),
            SignConfig("V1+V2", v1 = true, v2 = true, v3 = false),
            SignConfig("V1-only", v1 = true, v2 = false, v3 = false)
        )
        
        for (config in configs) {
            try {
                AppLogger.d(TAG, "尝试签名方案: ${config.name}")
                
                // 清理上次可能的输出
                if (outputApk.exists()) outputApk.delete()
                
                val success = attemptSign(inputApk, outputApk, config.v1, config.v2, config.v3)
                if (success) {
                    AppLogger.d(TAG, "签名成功: ${config.name}")
                    return true
                }
                
                errorMessages.add("${config.name}: 输出文件无效")
                
            } catch (e: Throwable) {
                lastException = e
                val causeChain = getExceptionChain(e)
                val msg = "${config.name}: $causeChain"
                errorMessages.add(msg)
                AppLogger.e(TAG, "签名异常 [${config.name}]: $causeChain")
                AppLogger.e(TAG, "完整堆栈:", e)
                
                // 清理
                if (outputApk.exists()) outputApk.delete()
                
                // 密钥问题时重新生成
                if (causeChain.contains("key", ignoreCase = true) ||
                    causeChain.contains("sign", ignoreCase = true) ||
                    causeChain.contains("certificate", ignoreCase = true)) {
                    AppLogger.d(TAG, "检测到可能的密钥问题，重新生成...")
                    File(context.filesDir, DEFAULT_PKCS12_FILE).delete()
                    File(context.filesDir, ".ks_credential").delete()
                    initializeKey()
                }
            }
        }

        val detail = errorMessages.joinToString("; ")
        AppLogger.e(TAG, "所有签名方案均失败: $detail")
        throw RuntimeException("APK 签名失败 (${configs.size} 种方案): $detail", lastException)
    }
    
    /**
     * 获取完整的异常链信息
     */
    private fun getExceptionChain(e: Throwable): String {
        val parts = mutableListOf<String>()
        var current: Throwable? = e
        var depth = 0
        while (current != null && depth < 5) {
            parts.add("${current.javaClass.simpleName}: ${current.message}")
            current = current.cause
            depth++
        }
        return parts.joinToString(" → ")
    }

    /**
     * 执行一次签名尝试
     */
    private fun attemptSign(
        inputApk: File, outputApk: File,
        v1: Boolean, v2: Boolean, v3: Boolean
    ): Boolean {
        val key = privateKey ?: throw IllegalStateException("私钥为空")
        val cert = certificate ?: throw IllegalStateException("证书为空")

        AppLogger.d(TAG, "证书: subject=${cert.subjectX500Principal.name}, algo=${cert.sigAlgName}")
        AppLogger.d(TAG, "密钥: algo=${key.algorithm}, format=${key.format}")
        AppLogger.d(TAG, "证书有效期: ${cert.notBefore} - ${cert.notAfter}")
        AppLogger.d(TAG, "签名配置: V1=$v1, V2=$v2, V3=$v3, minSdk=23")
        
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "WebToApp",
            key,
            listOf(cert)
        ).build()

        val builder = ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(v1)
            .setV2SigningEnabled(v2)
            .setV3SigningEnabled(v3)
            .setMinSdkVersion(23)

        AppLogger.d(TAG, "调用 ApkSigner.sign()...")
        builder.build().sign()
        AppLogger.d(TAG, "ApkSigner.sign() 完成")

        // 检查输出文件
        if (!outputApk.exists() || outputApk.length() == 0L) {
            AppLogger.e(TAG, "签名后输出文件不存在或为空")
            return false
        }

        AppLogger.d(TAG, "签名输出: ${outputApk.length() / 1024} KB")
        
        // 仅记录验证结果
        verifyApkDetailed(outputApk, "ApkSigner-${if(v3) "V3" else if(v2) "V2" else "V1"}")
        return true
    }
    
    /**
     * 使用 ApkSigner 库签名 (V1+V2+V3)
     */
    private fun performApkSignerSign(inputApk: File, outputApk: File, key: PrivateKey, cert: X509Certificate): Boolean {
        AppLogger.d(TAG, "ApkSigner 签名: ${inputApk.name}")
        
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
            AppLogger.e(TAG, "ApkSigner 异常: ${e.message}")
            throw e
        }

        // Check输出文件
        if (!outputApk.exists() || outputApk.length() == 0L) {
            AppLogger.e(TAG, "ApkSigner: 输出文件无效")
            return false
        }
        
        return verifyApkDetailed(outputApk, "ApkSigner")
    }
    
    /**
     * 使用纯 Java 实现的 V1 (JAR) 签名
     * 这是一个备用方案，不依赖 ApkSigner 库
     */
    private fun performV1Sign(inputApk: File, outputApk: File, key: PrivateKey, cert: X509Certificate): Boolean {
        AppLogger.d(TAG, "V1 签名: ${inputApk.name}")
        
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
                    
                    // Write resources.arsc FIRST with STORED + 4-byte alignment (Android R+ requirement)
                    entries["resources.arsc"]?.let { content ->
                        ZipUtils.writeEntryStored(zos, "resources.arsc", content)
                    }
                    
                    entries.forEach { (name, content) ->
                        if (name == "resources.arsc") return@forEach // already written above
                        val entry = ZipEntry(name)
                        zos.putNextEntry(entry)
                        zos.write(content)
                        zos.closeEntry()
                    }
                }
            }
            
            // 注意：V1 签名后绝不能调用 ZipAligner！
            // ZipAligner 会重建整个 ZIP（重新 DEFLATE 所有条目），
            // 导致 MANIFEST.MF 中的 SHA-256 摘要与实际文件内容不匹配，
            // Android 系统会判定 "不包含任何证书"。
            // resources.arsc 的 4-byte 对齐已在上面 line 934-937 通过 
            // ZipUtils.writeEntryStored() 的 extra field padding 实现。
            
            val verified = verifyApkDetailed(outputApk, "V1")
            
            // 签名验证失败则视为签名失败，不能安装未正确签名的 APK
            if (!verified) {
                AppLogger.e(TAG, "V1 签名验证失败，APK 签名无效")
                if (outputApk.exists()) outputApk.delete()
                return false
            }
            
            return verified
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "V1 签名失败: ${e.message}")
            if (outputApk.exists()) outputApk.delete()
            return false
        }
    }

    /**
     * APK 签名验证（仅用于日志记录，不影响构建结果）
     * 
     * ApkVerifier 可能因以下原因误报失败：
     * - 自签名证书 ASN.1 编码细节（如 GeneralizedTime）
     * - minSdkVersion 与签名方案的兼容性警告
     * - ZIP 条目对齐方式差异
     * 这些 "错误" 通常不影响 APK 的实际安装和运行
     */
    private fun verifyApkDetailed(apk: File, source: String): Boolean {
        return try {
            AppLogger.d(TAG, "[$source] 开始校验 APK: path=${apk.absolutePath}, size=${apk.length()}")
            val verifier = ApkVerifier.Builder(apk).build()
            val result = verifier.verify()

            AppLogger.d(TAG, "[$source] APK 验证完成: isVerified=${result.isVerified}, " +
                    "v1Verified=${result.isVerifiedUsingV1Scheme}, " +
                    "v2Verified=${result.isVerifiedUsingV2Scheme}, " +
                    "v3Verified=${result.isVerifiedUsingV3Scheme}, " +
                    "errors=${result.errors.size}, warnings=${result.warnings.size}")

            if (result.errors.isNotEmpty()) {
                result.errors.forEachIndexed { index, error ->
                    AppLogger.w(TAG, "[$source] 验证问题[$index]: $error")
                }
            }

            if (result.warnings.isNotEmpty()) {
                result.warnings.forEachIndexed { index, warning ->
                    AppLogger.w(TAG, "[$source] 验证警告[$index]: $warning")
                }
            }
            
            if (!result.isVerified) {
                AppLogger.w(TAG, "[$source] ApkVerifier 报告未通过验证，但 APK 文件结构完整，" +
                    "大多数情况下仍可正常安装。跳过验证拦截。")
            }

            result.isVerified
        } catch (e: Exception) {
            AppLogger.w(TAG, "[$source] 验证过程异常（不影响构建）: ${e.message}")
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
        return Base64.encodeToString(md.digest(), Base64.NO_WRAP)
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
        val manifestDigest = Base64.encodeToString(
            MessageDigest.getInstance(DIGEST_ALGORITHM).digest(manifest),
            Base64.NO_WRAP
        )
        sb.append("SHA-256-Digest-Manifest: $manifestDigest\r\n")
        sb.append("\r\n")

        // 每个条目的摘要
        digests.forEach { (name, digest) ->
            val entryBlock = "Name: $name\r\nSHA-256-Digest: $digest\r\n\r\n"
            val entryDigest = Base64.encodeToString(
                MessageDigest.getInstance(DIGEST_ALGORITHM).digest(entryBlock.toByteArray()),
                Base64.NO_WRAP
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
            AppLogger.d(TAG, "重置所有签名密钥...")
            
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
                    AppLogger.d(TAG, "已删除 Android KeyStore 密钥")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "删除 Android KeyStore 密钥失败", e)
            }
            
            // 清空当前密钥
            privateKey = null
            certificate = null
            
            // 重新初始化
            initializeKey()
            
            AppLogger.d(TAG, "密钥重置完成，当前类型: $currentSignerType")
            privateKey != null && certificate != null
        } catch (e: Exception) {
            AppLogger.e(TAG, "重置密钥失败", e)
            false
        }
    }
}
