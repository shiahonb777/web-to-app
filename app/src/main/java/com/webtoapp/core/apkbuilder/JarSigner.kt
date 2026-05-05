package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.core.i18n.Strings
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
        private const val VALIDITY_YEARS = 20L


        private const val DEFAULT_PKCS12_FILE = "webtoapp_keystore.p12"
        private const val CUSTOM_PKCS12_FILE = "custom_keystore.p12"


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




    enum class SignerType {
        ANDROID_KEYSTORE,
        PKCS12_AUTO,
        PKCS12_CUSTOM
    }

    private var privateKey: PrivateKey? = null
    private var certificate: X509Certificate? = null
    private var initError: String? = null
    private var currentSignerType: SignerType = SignerType.ANDROID_KEYSTORE

    init {
        initializeKey()
    }




    fun getSignerType(): SignerType = currentSignerType





    fun getCertificateSignatureHash(): ByteArray {
        val cert = certificate ?: throw IllegalStateException("证书未初始化")
        return java.security.MessageDigest.getInstance("SHA-256").digest(cert.encoded)
    }




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





    private fun initializeKey() {

        if (tryLoadOrCreateFallbackKey()) {
            AppLogger.d(TAG, "成功使用 PKCS12 密钥方案")
            return
        }


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




    private fun tryGenerateAndroidKeyStoreKey(): Boolean {
        return try {

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




    private fun tryLoadOrCreateFallbackKey(): Boolean {

        if (tryLoadCustomPkcs12()) {
            currentSignerType = SignerType.PKCS12_CUSTOM
            return true
        }


        if (tryLoadOrCreateAutoPkcs12()) {
            currentSignerType = SignerType.PKCS12_AUTO
            return true
        }

        return false
    }




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


            createNewPkcs12(keyStoreFile, keyStorePassword, FALLBACK_KEY_ALIAS)
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "自动 PKCS12 密钥方案失败", e)
            false
        }
    }





    private fun getOrCreateKeystorePassword(): CharArray {
        val passwordFile = File(context.filesDir, ".ks_credential")


        if (passwordFile.exists()) {
            try {
                val saved = passwordFile.readText().trim()
                if (saved.isNotEmpty()) return saved.toCharArray()
            } catch (e: Exception) {
                AppLogger.w(TAG, "读取密码文件失败", e)
            }
        }


        val keyStoreFile = File(context.filesDir, DEFAULT_PKCS12_FILE)
        val legacyPassword = "webtoapp_sign"
        if (keyStoreFile.exists()) {
            try {
                val ks = KeyStore.getInstance("PKCS12")
                FileInputStream(keyStoreFile).use { fis -> ks.load(fis, legacyPassword.toCharArray()) }

                passwordFile.writeText(legacyPassword)
                AppLogger.d(TAG, "迁移旧密码到文件存储")
                return legacyPassword.toCharArray()
            } catch (_: Exception) {

            }
        }


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




    private fun loadPkcs12(file: File, password: CharArray, alias: String? = null): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("PKCS12")
            FileInputStream(file).use { fis ->
                keyStore.load(fis, password)
            }


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








    private fun createNewPkcs12(file: File, password: CharArray, alias: String) {
        AppLogger.d(TAG, "创建新的 PKCS12 密钥库...")


        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()
        AppLogger.d(TAG, "RSA 密钥对已生成 (${KEY_SIZE} bit)")


        val cert = try {
            generateCertViaAndroidKeyStore(keyPair)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Android KeyStore 证书生成失败，使用手写 ASN.1 回退: ${e.message}")
            generateSelfSignedCertificate(keyPair)
        }


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










    private fun generateCertViaAndroidKeyStore(softwareKeyPair: KeyPair): X509Certificate {
        val tempAlias = "webtoapp_cert_gen_temp_${System.currentTimeMillis()}"

        try {

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


            val aksKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            aksKeyStore.load(null)
            val aksCert = aksKeyStore.getCertificate(tempAlias) as X509Certificate


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







    fun importPkcs12(sourceFile: File, password: String): Boolean {
        return try {
            val passwordChars = password.toCharArray()


            val keyStore = KeyStore.getInstance("PKCS12")
            FileInputStream(sourceFile).use { fis ->
                keyStore.load(fis, passwordChars)
            }


            val keyAlias = keyStore.aliases().toList().firstOrNull { keyStore.isKeyEntry(it) }
            if (keyAlias == null) {
                AppLogger.e(TAG, "导入失败: PKCS12 中找不到密钥")
                return false
            }


            val targetFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
            sourceFile.copyTo(targetFile, overwrite = true)


            val passwordFile = File(context.filesDir, "custom_keystore_password.txt")
            passwordFile.writeText(password)


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








    fun importKeystore(sourceFile: File, password: String): Boolean {
        val passwordChars = password.toCharArray()


        val keystoreTypes = listOf("PKCS12", "BKS", "JKS")

        for (type in keystoreTypes) {
            try {
                val keyStore = KeyStore.getInstance(type)
                FileInputStream(sourceFile).use { fis ->
                    keyStore.load(fis, passwordChars)
                }


                val keyAlias = keyStore.aliases().toList().firstOrNull { keyStore.isKeyEntry(it) }
                if (keyAlias == null) {
                    AppLogger.w(TAG, "$type 中找不到密钥条目，跳过")
                    continue
                }

                AppLogger.d(TAG, "成功以 $type 格式解析签名文件, alias=$keyAlias")

                if (type == "PKCS12") {

                    val targetFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
                    sourceFile.copyTo(targetFile, overwrite = true)
                } else {

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


                val passwordFile = File(context.filesDir, "custom_keystore_password.txt")
                passwordFile.writeText(password)


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




    fun removeCustomPkcs12(): Boolean {
        return try {
            val customFile = File(context.filesDir, CUSTOM_PKCS12_FILE)
            val passwordFile = File(context.filesDir, "custom_keystore_password.txt")

            customFile.delete()
            passwordFile.delete()


            initializeKey()

            AppLogger.d(TAG, "已删除自定义 PKCS12，回退到: $currentSignerType")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "删除自定义 PKCS12 失败", e)
            false
        }
    }




    fun getPkcs12FilePath(): String? {
        return when (currentSignerType) {
            SignerType.PKCS12_CUSTOM -> File(context.filesDir, CUSTOM_PKCS12_FILE).absolutePath
            SignerType.PKCS12_AUTO -> File(context.filesDir, DEFAULT_PKCS12_FILE).absolutePath
            else -> null
        }
    }





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




    private fun createX509Certificate(
        subject: X500Principal,
        issuer: X500Principal,
        serialNumber: BigInteger,
        notBefore: Date,
        notAfter: Date,
        publicKey: PublicKey,
        privateKey: PrivateKey
    ): X509Certificate {

        val tbsCert = buildTBSCertificate(
            subject, issuer, serialNumber, notBefore, notAfter, publicKey
        )


        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(tbsCert)
        val signatureBytes = signature.sign()


        val certDer = buildCertificateDER(tbsCert, signatureBytes)


        val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate
    }




    private fun buildTBSCertificate(
        subject: X500Principal,
        issuer: X500Principal,
        serialNumber: BigInteger,
        notBefore: Date,
        notAfter: Date,
        publicKey: PublicKey
    ): ByteArray {
        val out = ByteArrayOutputStream()


        out.write(byteArrayOf(0xA0.toByte(), 0x03, 0x02, 0x01, 0x02))


        val serialBytes = serialNumber.toByteArray()
        out.write(wrapWithTag(0x02, serialBytes))


        out.write(buildSignatureAlgorithmId())


        out.write(issuer.encoded)


        out.write(buildValidity(notBefore, notAfter))


        out.write(subject.encoded)


        out.write(publicKey.encoded)

        return wrapWithTag(0x30, out.toByteArray())
    }

    private fun buildSignatureAlgorithmId(): ByteArray {

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







    private fun encodeTime(date: Date): ByteArray {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)

        return if (year >= 2050) {

            val timeStr = GENERALIZED_TIME_FORMAT.get()!!.format(date)
            wrapWithTag(0x18, timeStr.toByteArray(Charsets.US_ASCII))
        } else {

            val timeStr = UTC_TIME_FORMAT.get()!!.format(date)
            wrapWithTag(0x17, timeStr.toByteArray(Charsets.US_ASCII))
        }
    }

    private fun buildCertificateDER(tbsCert: ByteArray, signature: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tbsCert)
        out.write(buildSignatureAlgorithmId())

        out.write(wrapWithTag(0x03, byteArrayOf(0x00) + signature))
        return wrapWithTag(0x30, out.toByteArray())
    }







    fun sign(inputApk: File, outputApk: File): Boolean {

        if (!validateInputs(inputApk, outputApk)) {
            throw IllegalStateException(Strings.signInputValidationFailed.format(inputApk.absolutePath))
        }

        val key = privateKey
        val cert = certificate
        if (key == null || cert == null) {
            AppLogger.e(TAG, "密钥或证书为空，尝试重新初始化...")

            File(context.filesDir, DEFAULT_PKCS12_FILE).delete()
            File(context.filesDir, ".ks_credential").delete()
            initializeKey()
            if (privateKey == null || certificate == null) {
                val errorDetail = initError ?: "key=${privateKey != null}, cert=${certificate != null}"
                throw IllegalStateException(Strings.signKeyInitFailed.format(errorDetail))
            }
        }

        AppLogger.d(TAG, "开始签名 APK: input=${inputApk.absolutePath} (size=${inputApk.length()})")
        AppLogger.d(TAG, "签名类型: $currentSignerType")


        return trySignWithRetry(inputApk, outputApk, maxRetries = 2)
    }




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


        val outputDir = outputApk.parentFile
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                AppLogger.e(TAG, "无法创建输出目录: ${outputDir.absolutePath}")
                return false
            }
        }


        if (outputApk.exists()) {
            outputApk.delete()
        }

        return true
    }









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


                if (outputApk.exists()) outputApk.delete()


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
        throw RuntimeException(Strings.signApkFailed.format(configs.size, detail), lastException)
    }




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


        if (!outputApk.exists() || outputApk.length() == 0L) {
            AppLogger.e(TAG, "签名后输出文件不存在或为空")
            return false
        }

        AppLogger.d(TAG, "签名输出: ${outputApk.length() / 1024} KB")


        verifyApkDetailed(outputApk, "ApkSigner-${if(v3) "V3" else if(v2) "V2" else "V1"}")
        return true
    }




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
            .setMinSdkVersion(23)

        try {
            val apkSigner = builder.build()
            apkSigner.sign()
        } catch (e: Exception) {
            AppLogger.e(TAG, "ApkSigner 异常: ${e.message}")
            throw e
        }


        if (!outputApk.exists() || outputApk.length() == 0L) {
            AppLogger.e(TAG, "ApkSigner: 输出文件无效")
            return false
        }

        return verifyApkDetailed(outputApk, "ApkSigner")
    }





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


                    entries["resources.arsc"]?.let { content ->
                        ZipUtils.writeEntryStored(zos, "resources.arsc", content)
                    }

                    entries.forEach { (name, content) ->
                        if (name == "resources.arsc") return@forEach
                        val entry = ZipEntry(name)
                        zos.putNextEntry(entry)
                        zos.write(content)
                        zos.closeEntry()
                    }
                }
            }








            val verified = verifyApkDetailed(outputApk, "V1")


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


        val manifestDigest = Base64.encodeToString(
            MessageDigest.getInstance(DIGEST_ALGORITHM).digest(manifest),
            Base64.NO_WRAP
        )
        sb.append("SHA-256-Digest-Manifest: $manifestDigest\r\n")
        sb.append("\r\n")


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




    private fun createSignatureBlock(sfContent: ByteArray): ByteArray {

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(sfContent)
        val signatureBytes = signature.sign()


        return buildPkcs7SignedData(signatureBytes, certificate!!)
    }




    private fun buildPkcs7SignedData(signature: ByteArray, cert: X509Certificate): ByteArray {
        val certBytes = cert.encoded


        val contentInfo = buildContentInfo(signature, certBytes)


        val signedDataOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0x0D, 0x01, 0x07, 0x02
        )


        val innerContent = ByteArrayOutputStream()
        innerContent.write(signedDataOid)


        val explicitTag = wrapWithTag(0xA0.toByte(), contentInfo)
        innerContent.write(explicitTag)


        return wrapWithTag(0x30, innerContent.toByteArray())
    }

    private fun buildContentInfo(signature: ByteArray, certBytes: ByteArray): ByteArray {
        val content = ByteArrayOutputStream()


        content.write(byteArrayOf(0x02, 0x01, 0x01))


        val digestAlgSet = buildDigestAlgorithmSet()
        content.write(digestAlgSet)


        val dataContentInfo = buildDataContentInfo()
        content.write(dataContentInfo)


        val certsImplicit = wrapWithTag(0xA0.toByte(), certBytes)
        content.write(certsImplicit)


        val signerInfos = buildSignerInfos(signature, certificate!!)
        content.write(signerInfos)

        return wrapWithTag(0x30, content.toByteArray())
    }

    private fun buildDigestAlgorithmSet(): ByteArray {

        val sha256Oid = byteArrayOf(
            0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01
        )
        val algId = wrapWithTag(0x30, sha256Oid + byteArrayOf(0x05, 0x00))
        return wrapWithTag(0x31, algId)
    }

    private fun buildDataContentInfo(): ByteArray {

        val dataOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0x0D, 0x01, 0x07, 0x01
        )
        return wrapWithTag(0x30, dataOid)
    }

    private fun buildSignerInfos(signature: ByteArray, cert: X509Certificate): ByteArray {
        val signerInfo = ByteArrayOutputStream()


        signerInfo.write(byteArrayOf(0x02, 0x01, 0x01))


        val issuerAndSerial = buildIssuerAndSerial(cert)
        signerInfo.write(issuerAndSerial)


        val sha256Oid = byteArrayOf(
            0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01
        )
        signerInfo.write(wrapWithTag(0x30, sha256Oid + byteArrayOf(0x05, 0x00)))


        val rsaOid = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0x0D, 0x01, 0x01, 0x0B
        )
        signerInfo.write(wrapWithTag(0x30, rsaOid + byteArrayOf(0x05, 0x00)))


        signerInfo.write(wrapWithTag(0x04, signature))

        val signerInfoSeq = wrapWithTag(0x30, signerInfo.toByteArray())
        return wrapWithTag(0x31, signerInfoSeq)
    }

    private fun buildIssuerAndSerial(cert: X509Certificate): ByteArray {
        val content = ByteArrayOutputStream()


        content.write(cert.issuerX500Principal.encoded)


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




    fun resetKeys(): Boolean {
        return try {
            AppLogger.d(TAG, "重置所有签名密钥...")


            File(context.filesDir, DEFAULT_PKCS12_FILE).delete()
            File(context.filesDir, CUSTOM_PKCS12_FILE).delete()
            File(context.filesDir, "custom_keystore_password.txt").delete()


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


            privateKey = null
            certificate = null


            initializeKey()

            AppLogger.d(TAG, "密钥重置完成，当前类型: $currentSignerType")
            privateKey != null && certificate != null
        } catch (e: Exception) {
            AppLogger.e(TAG, "重置密钥失败", e)
            false
        }
    }
}
