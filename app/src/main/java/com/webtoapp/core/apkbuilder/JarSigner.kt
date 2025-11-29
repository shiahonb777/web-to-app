package com.webtoapp.core.apkbuilder

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import java.io.*
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import java.util.zip.*
import javax.security.auth.x500.X500Principal

/**
 * JAR v1 签名工具
 * 使用标准的 JAR 签名格式对 APK 进行签名
 */
class JarSigner(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "WebToAppKey"
        private const val DIGEST_ALGORITHM = "SHA-256"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    }

    private var privateKey: PrivateKey? = null
    private var certificate: X509Certificate? = null

    init {
        initializeKey()
    }

    private fun initializeKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKey()
            }

            privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
            certificate = keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    ANDROID_KEYSTORE
                )

                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .setCertificateSubject(X500Principal("CN=WebToApp, O=WebToApp, C=CN"))
                    .setCertificateSerialNumber(java.math.BigInteger.ONE)
                    .setCertificateNotBefore(Date())
                    .setCertificateNotAfter(Date(System.currentTimeMillis() + 25L * 365 * 24 * 60 * 60 * 1000))
                    .build()

                keyPairGenerator.initialize(spec)
                keyPairGenerator.generateKeyPair()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 对 APK 进行 JAR v1 签名
     */
    fun sign(inputApk: File, outputApk: File): Boolean {
        val key = privateKey
        val cert = certificate
        if (key == null || cert == null) {
            Log.e("JarSigner", "密钥或证书为空，无法进行签名 key=$key cert=$cert")
            return false
        }

        Log.d(
            "JarSigner",
            "开始签名 APK: input=${inputApk.absolutePath} (size=${inputApk.length()}), " +
                    "output=${outputApk.absolutePath}"
        )

        return try {
            val signerConfig = ApkSigner.SignerConfig.Builder(
                KEY_ALIAS,
                key,
                listOf(cert)
            ).build()

            val builder = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)

            Log.d("JarSigner", "调用 ApkSigner.sign()...")
            builder.build().sign()
            Log.d("JarSigner", "ApkSigner.sign() 完成，开始使用 ApkVerifier 校验")

            val verified = verifyApk(outputApk)
            Log.d("JarSigner", "ApkVerifier 校验结果: $verified")
            verified
        } catch (e: Exception) {
            Log.e("JarSigner", "签名过程中发生异常", e)
            false
        }
    }

    private fun verifyApk(apk: File): Boolean {
        return try {
            Log.d("JarSigner", "开始校验 APK: path=${apk.absolutePath}, size=${apk.length()}")
            val verifier = ApkVerifier.Builder(apk).build()
            val result = verifier.verify()

            Log.d(
                "JarSigner",
                "APK 验证完成: isVerified=${result.isVerified}, " +
                        "errors=${result.errors.size}, warnings=${result.warnings.size}"
            )

            if (result.errors.isNotEmpty()) {
                result.errors.forEachIndexed { index, error ->
                    Log.e("JarSigner", "验证错误[$index]: $error")
                }
            }

            if (result.warnings.isNotEmpty()) {
                result.warnings.forEachIndexed { index, warning ->
                    Log.w("JarSigner", "验证警告[$index]: $warning")
                }
            }

            result.isVerified
        } catch (e: Exception) {
            Log.e("JarSigner", "验证 APK 时发生异常", e)
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

        // 构建简化的 PKCS#7 SignedData 结构
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
        
        // 构建完整结构
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
}
