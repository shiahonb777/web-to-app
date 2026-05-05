package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.crypto.EnhancedCrypto
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
















class NativeProtector(private val context: Context) {

    companion object {
        private const val TAG = "NativeProtector"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128


        private val ELF_MAGIC = byteArrayOf(0x7F, 0x45, 0x4C, 0x46)


        private val PROTECTED_SO_MAGIC = byteArrayOf(0x57, 0x53, 0x4F, 0x50)
    }

    private val secureRandom = SecureRandom()












    fun encryptSections(zipOut: ZipOutputStream, packageName: String) {
        AppLogger.e(TAG, "开始 SO Section 加密保护")


        val sectionKey = EnhancedCrypto.HKDF.derive(
            ikm = packageName.toByteArray(),
            salt = "WebToApp:SOProtection:v1".toByteArray(),
            info = "AES-256-GCM:SectionEncryption".toByteArray(),
            length = 32
        )

        val config = generateSectionEncryptionConfig(sectionKey)

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_SO_DIR}/section_config.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()

        AppLogger.e(TAG, "SO Section 加密保护完成")
    }













    fun obfuscateElfHeaders(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始 ELF 头混淆")

        val config = generateElfObfuscationConfig()

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_SO_DIR}/elf_obf_config.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()

        AppLogger.e(TAG, "ELF 头混淆完成")
    }











    fun stripAndFakeSymbols(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始符号表处理")


        val fakeSymbols = generateFakeSymbols()
        val config = ByteArrayOutputStream().apply {

            write(byteArrayOf(0x53, 0x59, 0x4D, 0x46))

            write(byteArrayOf(
                (fakeSymbols.size shr 8).toByte(),
                fakeSymbols.size.toByte()
            ))

            fakeSymbols.forEach { symbol ->
                val bytes = symbol.toByteArray(Charsets.UTF_8)
                write(bytes.size.toByte().toInt())
                write(bytes)
            }
        }

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_SO_DIR}/sym_config.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config.toByteArray())
        zipOut.closeEntry()

        AppLogger.e(TAG, "符号表处理完成，注入 ${fakeSymbols.size} 个假符号")
    }










    fun enableAntiDump(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始反 Dump 保护配置")

        val config = generateAntiDumpConfig()

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_SO_DIR}/anti_dump.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()

        AppLogger.e(TAG, "反 Dump 保护配置完成")
    }






    private fun generateSectionEncryptionConfig(key: ByteArray): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(PROTECTED_SO_MAGIC)


        config.write(byteArrayOf(0x00, 0x01))


        val sections = listOf(".text", ".rodata", ".data", ".init_array")
        config.write(sections.size.toByte().toInt())
        sections.forEach { section ->
            val bytes = section.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }


        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(salt)


        config.write(byteArrayOf(0x00, 0x01, 0x00, 0x00))


        val checksum = ByteArray(8).also { secureRandom.nextBytes(it) }
        config.write(checksum)

        return config.toByteArray()
    }




    private fun generateElfObfuscationConfig(): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x45, 0x4F, 0x42, 0x46))


        val strategyBits = 0x1F
        config.write(byteArrayOf(strategyBits.toByte()))


        config.write(byteArrayOf(0x08))


        val fakeSections = listOf(
            ".gnu.hash2", ".note.android2", ".dynsym2", ".dynstr2",
            ".rela.dyn2", ".rela.plt2", ".plt2", ".got2"
        )
        fakeSections.forEach { name ->
            val bytes = name.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }


        val padding = ByteArray(32).also { secureRandom.nextBytes(it) }
        config.write(padding)

        return config.toByteArray()
    }




    private fun generateFakeSymbols(): List<String> {
        return listOf(

            "EVP_EncryptInit_ex", "EVP_DecryptInit_ex", "EVP_CipherUpdate",
            "AES_set_encrypt_key", "AES_cbc_encrypt", "RSA_public_encrypt",
            "HMAC_Init_ex", "SHA256_Update", "EC_KEY_generate_key",


            "SSL_CTX_new", "SSL_connect", "SSL_write", "SSL_read",
            "curl_easy_perform", "send_encrypted_payload", "verify_certificate",
            "tls_handshake_complete", "ssl_pinning_check",


            "sqlite3_exec_encrypted", "realm_write_secure", "leveldb_put_secure",
            "decrypt_database_key", "init_secure_storage",


            "check_root_access", "verify_device_integrity", "anti_tamper_check",
            "validate_license_key", "decrypt_config_data", "init_drm_module",
            "phone_home_callback", "report_threat_level",
            "obfuscation_layer_init", "vm_protect_entry",


            "detect_substrate_hooks", "scan_inline_hooks", "verify_got_table",
            "check_plt_integrity", "monitor_syscall_table",


            "derive_master_key", "unwrap_session_key", "rotate_encryption_key",
            "store_key_in_tee", "fetch_remote_key"
        )
    }




    private fun generateAntiDumpConfig(): ByteArray {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x41, 0x44, 0x4D, 0x50))


        config.write(byteArrayOf(
            0x01,
            0x01,
            0x01,
            0x01
        ))


        config.write(byteArrayOf(0x00, 0x00, 0x13, 0x88.toByte()))


        val paths = listOf("/proc/self/maps", "/proc/self/mem", "/proc/self/pagemap")
        config.write(paths.size.toByte().toInt())
        paths.forEach { path ->
            val bytes = path.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }


        val padding = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(padding)

        return config.toByteArray()
    }
}
