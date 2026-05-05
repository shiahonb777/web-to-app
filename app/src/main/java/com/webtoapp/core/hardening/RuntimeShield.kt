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


















































class RuntimeShield(private val context: Context) {

    companion object {
        private const val TAG = "RuntimeShield"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }




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




    fun writeRaspConfig(
        zipOut: ZipOutputStream,
        config: RaspConfig,
        packageName: String,
        signatureHash: ByteArray?
    ) {
        AppLogger.d(TAG, "写入 RASP 配置")


        val configData = generateRaspConfigData(config)
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/rasp_config.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(configData)
        zipOut.closeEntry()


        if (config.multiPointSignatureVerify && signatureHash != null) {
            writeSignatureVerificationData(zipOut, packageName, signatureHash)
        }


        if (config.apkChecksumValidation) {
            writeApkChecksumData(zipOut, packageName)
        }


        writeThreatResponseConfig(zipOut, config)


        if (config.stackTraceFilter) {
            writeStackFilterRules(zipOut)
        }


        if (config.timingCheck) {
            writeTimingCheckConfig(zipOut)
        }


        if (config.enableHoneypot) {
            writeHoneypotConfig(zipOut)
        }

        AppLogger.d(TAG, "RASP 配置写入完成")
    }




    private fun generateRaspConfigData(config: RaspConfig): ByteArray {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x52, 0x41, 0x53, 0x50))


        data.write(byteArrayOf(0x00, 0x01))


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


        var bitmap2 = 0
        if (config.certificatePinning) bitmap2 = bitmap2 or 0x01
        if (config.enableHoneypot) bitmap2 = bitmap2 or 0x02
        if (config.enableSelfDestruct) bitmap2 = bitmap2 or 0x04
        data.write(byteArrayOf(bitmap2.toByte()))


        if (config.dexCrcVerify) {
            data.write(byteArrayOf(0x00, 0x00, 0x1B, 0x58))
        }


        if (config.memoryIntegrity) {
            data.write(byteArrayOf(0x00, 0x00, 0x27, 0x10))
        }


        if (config.timingCheck) {
            data.write(byteArrayOf(0x0A))
        }


        val padding = ByteArray(16)
        secureRandom.nextBytes(padding)
        data.write(padding)

        return data.toByteArray()
    }











    private fun writeSignatureVerificationData(
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray
    ) {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x53, 0x49, 0x47, 0x56))


        data.write(byteArrayOf(0x03))


        val encKey = EnhancedCrypto.HKDF.derive(
            ikm = packageName.toByteArray(),
            salt = "SignatureVerify:Salt".toByteArray(),
            info = "AES-256-GCM:SigHash".toByteArray(),
            length = 32
        )


        val encSig1 = encryptData(signatureHash, encKey)
        data.write(byteArrayOf((encSig1.size shr 8).toByte(), encSig1.size.toByte()))
        data.write(encSig1)


        val hmacKey = EnhancedCrypto.HKDF.derive(
            ikm = packageName.toByteArray(),
            salt = "SignatureVerify:HMAC".toByteArray(),
            info = "HMAC-SHA256".toByteArray(),
            length = 32
        )
        val hmac = computeHmac(signatureHash, hmacKey)
        data.write(byteArrayOf((hmac.size shr 8).toByte(), hmac.size.toByte()))
        data.write(hmac)


        val doubleHash = MessageDigest.getInstance("SHA-256").let { md ->
            md.update(signatureHash)
            md.update(packageName.toByteArray())
            md.digest()
        }
        data.write(byteArrayOf((doubleHash.size shr 8).toByte(), doubleHash.size.toByte()))
        data.write(doubleHash)


        data.write(byteArrayOf(0x00, 0x1E))

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/sig_verify.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }




    private fun writeApkChecksumData(zipOut: ZipOutputStream, packageName: String) {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x43, 0x4B, 0x53, 0x4D))


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


        data.write(byteArrayOf(0x02))


        data.write(byteArrayOf(
            0x01,
            0x01,
            0x01
        ))


        data.write(byteArrayOf(0x00, 0x3C))

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/apk_checksum.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }




    private fun writeThreatResponseConfig(zipOut: ZipOutputStream, config: RaspConfig) {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x54, 0x48, 0x52, 0x54))


        val strategyCode = when (config.responseStrategy) {
            "LOG_ONLY" -> 0x00
            "SILENT_EXIT" -> 0x01
            "CRASH_RANDOM" -> 0x02
            "DATA_WIPE" -> 0x03
            "FAKE_DATA" -> 0x04
            else -> 0x01
        }
        data.write(byteArrayOf(strategyCode.toByte()))


        data.write(byteArrayOf(
            (config.responseDelay shr 8).toByte(),
            config.responseDelay.toByte()
        ))


        data.write(byteArrayOf(0x00, 0x05))


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


        if (config.enableSelfDestruct) {
            data.write(byteArrayOf(0x01))

            data.write(byteArrayOf(0x50))

            data.write(byteArrayOf(
                0x01,
                0x01,
                0x01,
                0x00
            ))
        }

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/threat_resp.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }




    private fun writeStackFilterRules(zipOut: ZipOutputStream) {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x53, 0x54, 0x4B, 0x46))


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




    private fun writeTimingCheckConfig(zipOut: ZipOutputStream) {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x54, 0x49, 0x4D, 0x43))


        data.write(byteArrayOf(0x03))
        data.write(byteArrayOf(
            0x01,
            0x02,
            0x03
        ))


        data.write(byteArrayOf(0x0A))


        data.write(byteArrayOf(0x03))


        data.write(byteArrayOf(0x00, 0x64))


        data.write(byteArrayOf(0x03))

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/timing_check.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray())
        zipOut.closeEntry()
    }







    private fun writeHoneypotConfig(zipOut: ZipOutputStream) {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x48, 0x4F, 0x4E, 0x59))


        data.write(byteArrayOf(0x03))


        data.write(byteArrayOf(0x01))
        val fakeApiKey = "sk-fake-${generateRandomHex(32)}"
        val fakeKeyBytes = fakeApiKey.toByteArray()
        data.write(byteArrayOf((fakeKeyBytes.size shr 8).toByte(), fakeKeyBytes.size.toByte()))
        data.write(fakeKeyBytes)


        data.write(byteArrayOf(0x02))
        val fakeServer = "https://honeypot-${generateRandomHex(8)}.example.com/api"
        val fakeServerBytes = fakeServer.toByteArray()
        data.write(byteArrayOf((fakeServerBytes.size shr 8).toByte(), fakeServerBytes.size.toByte()))
        data.write(fakeServerBytes)


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
