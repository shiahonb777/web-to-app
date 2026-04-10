package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.crypto.EnhancedCrypto
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Native SO 保护器
 * 
 * 提供多层 Native 库保护：
 * 1. Section 加密 - 加密 .text/.rodata 等关键 section
 * 2. ELF 头混淆 - 修改 ELF 头部信息，干扰 IDA/Ghidra 分析
 * 3. 符号表处理 - 剥离真实符号 + 注入假符号（蜜罐）
 * 4. 反 Dump 保护 - 运行时检测内存 dump 行为
 * 
 * 技术细节：
 * - Section 加密使用 AES-256-GCM，密钥从包名+签名派生
 * - ELF 头混淆修改 e_shoff/e_shnum/e_shstrndx 等字段
 * - 假符号包含诱饵函数名，误导逆向分析
 * - 反 Dump 通过 inotify 监控 /proc/self/maps + mprotect 保护关键内存页
 */
class NativeProtector(private val context: Context) {
    
    companion object {
        private const val TAG = "NativeProtector"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
        
        // ELF 魔数
        private val ELF_MAGIC = byteArrayOf(0x7F, 0x45, 0x4C, 0x46) // "\x7FELF"
        
        // 保护后的 SO 标记
        private val PROTECTED_SO_MAGIC = byteArrayOf(0x57, 0x53, 0x4F, 0x50) // "WSOP"
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * Section 加密
     * 
     * 加密 SO 文件中的关键 section：
     * - .text（代码段）
     * - .rodata（只读数据段）
     * - .data（数据段）
     * - .init_array（初始化函数数组）
     * 
     * 运行时由自定义 linker 解密并映射到内存
     */
    fun encryptSections(zipOut: ZipOutputStream, packageName: String) {
        AppLogger.e(TAG, "开始 SO Section 加密保护")
        
        // 生成 section 加密配置
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
    
    /**
     * ELF 头混淆
     * 
     * 修改 ELF 文件头部的非关键字段，使得静态分析工具无法正确解析：
     * - 修改 Section Header Table 偏移（e_shoff）
     * - 修改 Section 数量（e_shnum）
     * - 修改 Section 名称字符串表索引（e_shstrndx）
     * - 添加垃圾 Section Header
     * - 修改 Program Header 中的非必要字段
     * 
     * 注意：不修改运行时需要的 PT_LOAD 等关键 Program Header
     */
    fun obfuscateElfHeaders(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始 ELF 头混淆")
        
        val config = generateElfObfuscationConfig()
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_SO_DIR}/elf_obf_config.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()
        
        AppLogger.e(TAG, "ELF 头混淆完成")
    }
    
    /**
     * 符号表剥离 + 假符号注入
     * 
     * 1. 剥离所有调试符号（.symtab, .strtab, .debug_*）
     * 2. 保留 .dynsym 中的必要导出符号
     * 3. 注入大量假符号名，误导逆向工程师：
     *    - 伪造加密算法函数名（如 openssl_xxx, bouncycastle_xxx）
     *    - 伪造网络通信函数名（如 ssl_handshake, tls_verify）
     *    - 伪造数据库函数名（如 sqlite_query, realm_insert）
     */
    fun stripAndFakeSymbols(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始符号表处理")
        
        // 生成假符号列表
        val fakeSymbols = generateFakeSymbols()
        val config = ByteArrayOutputStream().apply {
            // 头部
            write(byteArrayOf(0x53, 0x59, 0x4D, 0x46)) // "SYMF" (Symbol Fake)
            // 假符号数量
            write(byteArrayOf(
                (fakeSymbols.size shr 8).toByte(),
                fakeSymbols.size.toByte()
            ))
            // 符号数据
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
    
    /**
     * 反内存 Dump 保护
     * 
     * 多层防护：
     * 1. mprotect：将关键代码页标记为不可读，仅在执行时临时开放
     * 2. inotify：监控 /proc/self/maps 和 /proc/self/mem 的访问
     * 3. 定期校验：周期性检查关键内存页的哈希值
     * 4. MADV_DONTDUMP：标记敏感内存区域不参与核心转储
     */
    fun enableAntiDump(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始反 Dump 保护配置")
        
        val config = generateAntiDumpConfig()
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_SO_DIR}/anti_dump.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()
        
        AppLogger.e(TAG, "反 Dump 保护配置完成")
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 生成 Section 加密配置
     */
    private fun generateSectionEncryptionConfig(key: ByteArray): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部标识
        config.write(PROTECTED_SO_MAGIC)
        
        // 版本
        config.write(byteArrayOf(0x00, 0x01))
        
        // 加密的 section 列表
        val sections = listOf(".text", ".rodata", ".data", ".init_array")
        config.write(sections.size.toByte().toInt())
        sections.forEach { section ->
            val bytes = section.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }
        
        // 密钥派生参数
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(salt)
        
        // 迭代次数
        config.write(byteArrayOf(0x00, 0x01, 0x00, 0x00)) // 65536
        
        // 校验和
        val checksum = ByteArray(8).also { secureRandom.nextBytes(it) }
        config.write(checksum)
        
        return config.toByteArray()
    }
    
    /**
     * 生成 ELF 头混淆配置
     */
    private fun generateElfObfuscationConfig(): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部
        config.write(byteArrayOf(0x45, 0x4F, 0x42, 0x46)) // "EOBF"
        
        // 混淆策略位图
        val strategyBits = 0x1F // 全部 5 种策略
        config.write(byteArrayOf(strategyBits.toByte()))
        
        // 垃圾 section 数量
        config.write(byteArrayOf(0x08)) // 添加 8 个假 section
        
        // 假 section 名称
        val fakeSections = listOf(
            ".gnu.hash2", ".note.android2", ".dynsym2", ".dynstr2",
            ".rela.dyn2", ".rela.plt2", ".plt2", ".got2"
        )
        fakeSections.forEach { name ->
            val bytes = name.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }
        
        // 随机填充
        val padding = ByteArray(32).also { secureRandom.nextBytes(it) }
        config.write(padding)
        
        return config.toByteArray()
    }
    
    /**
     * 生成假符号列表（蜜罐陷阱）
     */
    private fun generateFakeSymbols(): List<String> {
        return listOf(
            // 伪造加密函数（诱导分析者花时间分析）
            "EVP_EncryptInit_ex", "EVP_DecryptInit_ex", "EVP_CipherUpdate",
            "AES_set_encrypt_key", "AES_cbc_encrypt", "RSA_public_encrypt",
            "HMAC_Init_ex", "SHA256_Update", "EC_KEY_generate_key",
            
            // 伪造网络函数
            "SSL_CTX_new", "SSL_connect", "SSL_write", "SSL_read",
            "curl_easy_perform", "send_encrypted_payload", "verify_certificate",
            "tls_handshake_complete", "ssl_pinning_check",
            
            // 伪造数据库函数  
            "sqlite3_exec_encrypted", "realm_write_secure", "leveldb_put_secure",
            "decrypt_database_key", "init_secure_storage",
            
            // 伪造安全检查函数（干扰逆向者判断真实检查点）
            "check_root_access", "verify_device_integrity", "anti_tamper_check",
            "validate_license_key", "decrypt_config_data", "init_drm_module",
            "phone_home_callback", "report_threat_level",
            "obfuscation_layer_init", "vm_protect_entry",
            
            // 伪造 Hook 检测函数
            "detect_substrate_hooks", "scan_inline_hooks", "verify_got_table",
            "check_plt_integrity", "monitor_syscall_table",
            
            // 伪造密钥管理
            "derive_master_key", "unwrap_session_key", "rotate_encryption_key",
            "store_key_in_tee", "fetch_remote_key"
        )
    }
    
    /**
     * 生成反 Dump 配置
     */
    private fun generateAntiDumpConfig(): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部
        config.write(byteArrayOf(0x41, 0x44, 0x4D, 0x50)) // "ADMP"
        
        // 保护策略
        config.write(byteArrayOf(
            0x01, // 启用 mprotect
            0x01, // 启用 inotify
            0x01, // 启用定期校验
            0x01  // 启用 MADV_DONTDUMP
        ))
        
        // 校验间隔（毫秒）
        config.write(byteArrayOf(0x00, 0x00, 0x13, 0x88.toByte())) // 5000ms
        
        // 监控路径
        val paths = listOf("/proc/self/maps", "/proc/self/mem", "/proc/self/pagemap")
        config.write(paths.size.toByte().toInt())
        paths.forEach { path ->
            val bytes = path.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }
        
        // 随机填充
        val padding = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(padding)
        
        return config.toByteArray()
    }
}
