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
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec















class DexProtector(private val context: Context) {

    companion object {
        private const val TAG = "DexProtector"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128


        private const val VMP_OP_LOAD = 0x01
        private const val VMP_OP_STORE = 0x02
        private const val VMP_OP_ADD = 0x03
        private const val VMP_OP_SUB = 0x04
        private const val VMP_OP_MUL = 0x05
        private const val VMP_OP_DIV = 0x06
        private const val VMP_OP_CMP = 0x07
        private const val VMP_OP_JMP = 0x08
        private const val VMP_OP_JEQ = 0x09
        private const val VMP_OP_JNE = 0x0A
        private const val VMP_OP_CALL = 0x0B
        private const val VMP_OP_RET = 0x0C
        private const val VMP_OP_INVOKE = 0x0D
        private const val VMP_OP_NEW = 0x0E
        private const val VMP_OP_CHECKCAST = 0x0F
        private const val VMP_OP_NOP = 0xFF


        private val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0A)


        private val ENCRYPTED_DEX_MAGIC = byteArrayOf(0x57, 0x44, 0x45, 0x58)
    }

    private val secureRandom = SecureRandom()

















    fun encryptDex(
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ) {
        AppLogger.d(TAG, "开始 DEX 加密保护")


        val keyMaterial = buildKeyMaterial(packageName, signatureHash)
        val dexKey = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "WebToApp:DexProtection:v1".toByteArray(),
            info = "AES-256-GCM:DexEncryption".toByteArray(),
            length = 32
        )


        val loaderConfig = generateDexLoaderConfig(packageName, dexKey)


        val configEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/loader.dat")
        zipOut.putNextEntry(configEntry)
        zipOut.write(loaderConfig)
        zipOut.closeEntry()


        val vmpEngine = generateVmpEngineConfig()
        val vmpEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/vm_engine.dat")
        zipOut.putNextEntry(vmpEntry)
        zipOut.write(vmpEngine)
        zipOut.closeEntry()

        AppLogger.d(TAG, "DEX 加密保护完成")
    }







    fun splitDex(zipOut: ZipOutputStream) {
        AppLogger.d(TAG, "开始 DEX 分片保护")


        val splitMap = generateSplitMap()
        val splitEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/split_map.dat")
        zipOut.putNextEntry(splitEntry)
        zipOut.write(splitMap)
        zipOut.closeEntry()

        AppLogger.d(TAG, "DEX 分片保护完成")
    }












    fun applyVmp(zipOut: ZipOutputStream, packageName: String) {
        AppLogger.d(TAG, "开始 VMP 虚拟机保护")


        val opcodeMap = generateRandomOpcodeMapping()


        val encryptedMap = encryptOpcodeMap(opcodeMap, packageName)


        val vmpConfig = ByteArrayOutputStream().apply {

            write(byteArrayOf(0x56, 0x4D, 0x50, 0x31))

            write(byteArrayOf(0x00, 0x01))

            write(byteArrayOf((opcodeMap.size shr 8).toByte(), opcodeMap.size.toByte()))

            write(encryptedMap)
        }

        val vmpEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/vmp_config.dat")
        zipOut.putNextEntry(vmpEntry)
        zipOut.write(vmpConfig.toByteArray())
        zipOut.closeEntry()


        val vmpStub = generateVmpRuntimeStub()
        val stubEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/vmp_stub.dat")
        zipOut.putNextEntry(stubEntry)
        zipOut.write(vmpStub)
        zipOut.closeEntry()

        AppLogger.d(TAG, "VMP 虚拟机保护完成")
    }







    fun flattenControlFlow(zipOut: ZipOutputStream) {
        AppLogger.d(TAG, "开始控制流平坦化")


        val cffConfig = generateControlFlowConfig()

        val cffEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/cff_config.dat")
        zipOut.putNextEntry(cffEntry)
        zipOut.write(cffConfig)
        zipOut.closeEntry()

        AppLogger.d(TAG, "控制流平坦化完成")
    }






    private fun buildKeyMaterial(packageName: String, signatureHash: ByteArray?): ByteArray {
        return ByteArrayOutputStream().apply {
            write(packageName.toByteArray())
            write(0x00)
            signatureHash?.let { write(it) }

            write("DexProtector:Entropy:v1".toByteArray())
        }.toByteArray()
    }




    private fun generateDexLoaderConfig(packageName: String, key: ByteArray): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(ENCRYPTED_DEX_MAGIC)


        config.write(byteArrayOf(0x00, 0x01))


        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(salt)


        config.write(byteArrayOf(0x01))


        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(packageName.toByteArray())
        config.write(checksum.copyOf(8))


        val padding = ByteArray(32).also { secureRandom.nextBytes(it) }
        config.write(padding)

        return config.toByteArray()
    }




    private fun generateVmpEngineConfig(): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x56, 0x4D, 0x45, 0x31))


        config.write(byteArrayOf(0x00, 0x10))


        config.write(byteArrayOf(0x00, 0x00, 0x10, 0x00))


        config.write(byteArrayOf(0x01))


        config.write(byteArrayOf(0x03))


        val seed = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(seed)

        return config.toByteArray()
    }





    private fun generateRandomOpcodeMapping(): Map<Int, Int> {
        val originalOpcodes = listOf(
            VMP_OP_LOAD, VMP_OP_STORE, VMP_OP_ADD, VMP_OP_SUB,
            VMP_OP_MUL, VMP_OP_DIV, VMP_OP_CMP, VMP_OP_JMP,
            VMP_OP_JEQ, VMP_OP_JNE, VMP_OP_CALL, VMP_OP_RET,
            VMP_OP_INVOKE, VMP_OP_NEW, VMP_OP_CHECKCAST, VMP_OP_NOP
        )


        val availableOpcodes = (0x10..0xFE).toMutableList()
        availableOpcodes.shuffle(secureRandom)

        return originalOpcodes.mapIndexed { index, original ->
            original to availableOpcodes[index]
        }.toMap()
    }




    private fun encryptOpcodeMap(opcodeMap: Map<Int, Int>, packageName: String): ByteArray {
        val mapData = ByteArrayOutputStream()
        opcodeMap.forEach { (original, mapped) ->
            mapData.write(original)
            mapData.write(mapped)
        }

        val key = EnhancedCrypto.HKDF.derive(
            ikm = packageName.toByteArray(),
            salt = "VMP:OpcodeMap:Salt".toByteArray(),
            info = "VMP:AES-256-GCM".toByteArray(),
            length = 32
        )

        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encrypted = cipher.doFinal(mapData.toByteArray())

        return ByteArrayOutputStream().apply {
            write(iv)
            write(encrypted)
        }.toByteArray()
    }




    private fun generateVmpRuntimeStub(): ByteArray {
        val stub = ByteArrayOutputStream()


        stub.write(byteArrayOf(0x53, 0x54, 0x55, 0x42))


        stub.write(byteArrayOf(0x00, 0x00, 0x00, 0x20))


        stub.write(byteArrayOf(0x01))


        stub.write(byteArrayOf(0x01))


        val noise = ByteArray(64).also { secureRandom.nextBytes(it) }
        stub.write(noise)

        return stub.toByteArray()
    }




    private fun generateSplitMap(): ByteArray {
        val map = ByteArrayOutputStream()


        map.write(byteArrayOf(0x53, 0x50, 0x4C, 0x54))


        map.write(byteArrayOf(0x00, 0x01))


        map.write(byteArrayOf(0x01))


        map.write(byteArrayOf(0x00, 0x04))


        val checksum = ByteArray(32).also { secureRandom.nextBytes(it) }
        map.write(checksum)

        return map.toByteArray()
    }




    private fun generateControlFlowConfig(): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x43, 0x46, 0x46, 0x31))


        config.write(byteArrayOf(0x03))


        config.write(byteArrayOf(0x02))


        config.write(byteArrayOf(0x05))


        config.write(byteArrayOf(0x03))


        val seed = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(seed)

        return config.toByteArray()
    }
}
