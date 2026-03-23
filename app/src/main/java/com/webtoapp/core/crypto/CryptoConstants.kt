package com.webtoapp.core.crypto

/**
 * 加密系统常量
 */
object CryptoConstants {
    // Encryption文件标记头（用于识别加密文件）
    const val ENCRYPTED_HEADER_MAGIC = 0x57544145 // "WTAE" = WebToApp Encrypted
    const val ENCRYPTED_HEADER_VERSION = 2  // 升级版本号
    
    // Encryption算法参数
    const val AES_KEY_SIZE = 256
    const val AES_GCM_IV_SIZE = 12
    const val AES_GCM_TAG_SIZE = 128
    const val PBKDF2_KEY_LENGTH = 32
    
    // PBKDF2 迭代次数（根据加密级别）
    const val PBKDF2_ITERATIONS_FAST = 5000
    const val PBKDF2_ITERATIONS_STANDARD = 10000
    const val PBKDF2_ITERATIONS_HIGH = 50000
    const val PBKDF2_ITERATIONS_PARANOID = 100000
    
    // Default迭代次数（backward compatible）
    const val PBKDF2_ITERATIONS = PBKDF2_ITERATIONS_STANDARD
    
    // Encryption文件扩展名
    const val ENCRYPTED_EXTENSION = ".enc"
    
    // Configure文件名
    const val CONFIG_FILE = "app_config.json"
    const val ENCRYPTED_CONFIG_FILE = "app_config.json.enc"
    const val ENCRYPTION_META_FILE = "encryption_meta.json"
    
    // Encryption资源目录
    const val ENCRYPTED_ASSETS_DIR = "encrypted"
    const val ENCRYPTED_HTML_DIR = "encrypted/html"
    const val ENCRYPTED_MEDIA_DIR = "encrypted/media"
    
    // Key派生盐（固定部分，会与包名组合）
    val KEY_DERIVATION_SALT = byteArrayOf(
        0x57, 0x65, 0x62, 0x54, 0x6F, 0x41, 0x70, 0x70,  // "WebToApp"
        0x45, 0x6E, 0x63, 0x72, 0x79, 0x70, 0x74, 0x21   // "Encrypt!"
    )
    
    // HKDF 相关常量
    val HKDF_SALT = "WebToApp:KeyDerivation:v2".toByteArray()
    val HKDF_INFO_ENCRYPTION = "AES-256-GCM:AppEncryption".toByteArray()
    val HKDF_INFO_INTEGRITY = "HMAC-SHA256:Integrity".toByteArray()
    val HKDF_INFO_OBFUSCATION = "XOR:StringObfuscation".toByteArray()
    
    // Security保护相关常量
    const val THREAT_CHECK_INTERVAL_MS = 5000L
    const val MAX_THREAT_LEVEL_ALLOWED = 2  // THREAT_MEDIUM
    
    // 完整性检查相关
    const val INTEGRITY_CHECK_ENABLED = "integrity_check"
    const val ANTI_DEBUG_ENABLED = "anti_debug"
    const val ANTI_TAMPER_ENABLED = "anti_tamper"
    
    // Native 库名称
    const val NATIVE_LIB_NAME = "webtoapp_crypto"
}
