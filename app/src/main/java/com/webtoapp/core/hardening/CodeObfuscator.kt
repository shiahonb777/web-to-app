package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.crypto.EnhancedCrypto
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec





























class CodeObfuscator(private val context: Context) {

    companion object {
        private const val TAG = "CodeObfuscator"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128


        private const val CUSTOM_BASE64_TABLE =
            "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm0123456789+/"
    }

    private val secureRandom = SecureRandom()






    fun encryptStrings(
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ): Int {
        AppLogger.e(TAG, "开始多层字符串加密")


        val keyMaterial = buildKeyMaterial(packageName, signatureHash)

        val layer1Key = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "StringEncrypt:Layer1:Salt".toByteArray(),
            info = "AES-256-GCM:L1".toByteArray(),
            length = 32
        )

        val layer2Key = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "StringEncrypt:Layer2:Salt".toByteArray(),
            info = "CustomBase64:L2".toByteArray(),
            length = 32
        )

        val layer3Key = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "StringEncrypt:Layer3:Salt".toByteArray(),
            info = "XOR:L3".toByteArray(),
            length = 32
        )


        val config = generateStringEncryptionConfig(layer1Key, layer2Key, layer3Key)
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/str_enc.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()


        val tableEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/b64_table.dat")
        zipOut.putNextEntry(tableEntry)

        val encryptedTable = encryptData(
            CUSTOM_BASE64_TABLE.toByteArray(), layer1Key
        )
        zipOut.write(encryptedTable)
        zipOut.closeEntry()

        AppLogger.e(TAG, "多层字符串加密完成")
        return 1
    }






    fun obfuscateClassNames(zipOut: ZipOutputStream): Int {
        AppLogger.e(TAG, "开始类名混淆")


        val config = generateClassObfuscationConfig()

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/cls_obf.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()

        AppLogger.e(TAG, "类名混淆配置完成")
        return 1
    }




    fun applyCallIndirection(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始方法调用间接化")

        val config = generateCallIndirectionConfig()

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/call_ind.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()

        AppLogger.e(TAG, "方法调用间接化完成")
    }




    fun injectOpaquePredicates(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始不透明谓词注入")

        val config = generateOpaquePredicateConfig()

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/opaque_pred.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()

        AppLogger.e(TAG, "不透明谓词注入完成")
    }



    private fun buildKeyMaterial(packageName: String, signatureHash: ByteArray?): ByteArray {
        return ByteArrayOutputStream().apply {
            write(packageName.toByteArray())
            write(0x00)
            signatureHash?.let { write(it) }
            write("CodeObfuscator:Entropy:v1".toByteArray())
        }.toByteArray()
    }




    private fun generateStringEncryptionConfig(
        layer1Key: ByteArray,
        layer2Key: ByteArray,
        layer3Key: ByteArray
    ): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x53, 0x54, 0x52, 0x45))


        config.write(byteArrayOf(0x00, 0x01))


        config.write(byteArrayOf(0x03))


        val salt1 = ByteArray(16).also { secureRandom.nextBytes(it) }
        val salt2 = ByteArray(16).also { secureRandom.nextBytes(it) }
        val salt3 = ByteArray(16).also { secureRandom.nextBytes(it) }

        config.write(salt1)
        config.write(salt2)
        config.write(salt3)


        config.write(byteArrayOf(
            0x01,
            0x02,
            0x03
        ))


        val order = intArrayOf(2, 0, 1)
        order.forEach { config.write(it.toByte().toInt()) }


        val checksum = ByteArray(8).also { secureRandom.nextBytes(it) }
        config.write(checksum)

        return config.toByteArray()
    }




    private fun generateClassObfuscationConfig(): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x43, 0x4C, 0x4F, 0x42))


        config.write(byteArrayOf(0x02))


        val confusables = listOf(

            'a' to 'а',
            'c' to 'с',
            'e' to 'е',
            'o' to 'о',
            'p' to 'р',
            'x' to 'х',
            'y' to 'у',
            'A' to 'А',
            'B' to 'В',
            'C' to 'С',
            'E' to 'Е',
            'H' to 'Н',
            'K' to 'К',
            'M' to 'М',
            'O' to 'О',
            'P' to 'Р',
            'T' to 'Т',
            'X' to 'Х'
        )

        config.write(confusables.size.toByte().toInt())
        confusables.forEach { (from, to) ->
            config.write(byteArrayOf(
                (from.code shr 8).toByte(), from.code.toByte(),
                (to.code shr 8).toByte(), to.code.toByte()
            ))
        }


        config.write(byteArrayOf(0x00, 0x80.toByte()))


        val excludes = listOf("android.", "androidx.", "kotlin.", "java.", "javax.")
        config.write(excludes.size.toByte().toInt())
        excludes.forEach { prefix ->
            val bytes = prefix.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }

        return config.toByteArray()
    }




    private fun generateCallIndirectionConfig(): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x43, 0x41, 0x4C, 0x49))


        config.write(byteArrayOf(
            0x01,
            0x01,
            0x01
        ))


        config.write(byteArrayOf(0x02))


        config.write(byteArrayOf(
            0x01,
            0x01,
            0x01,
            0x00
        ))

        return config.toByteArray()
    }




    private fun generateOpaquePredicateConfig(): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x4F, 0x50, 0x52, 0x44))


        config.write(byteArrayOf(0x04))


        config.write(byteArrayOf(0x01, 0x05))


        config.write(byteArrayOf(0x02, 0x03))


        config.write(byteArrayOf(0x03, 0x02))


        config.write(byteArrayOf(0x04, 0x04))


        config.write(byteArrayOf(0x0A, 0x32))


        val seed = ByteArray(8).also { secureRandom.nextBytes(it) }
        config.write(seed)

        return config.toByteArray()
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
}
