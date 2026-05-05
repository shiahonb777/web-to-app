package com.webtoapp.core.crypto




object CryptoConstants {

    const val ENCRYPTED_HEADER_MAGIC = 0x57544145
    const val ENCRYPTED_HEADER_VERSION = 2


    const val AES_KEY_SIZE = 256
    const val AES_GCM_IV_SIZE = 12
    const val AES_GCM_TAG_SIZE = 128
    const val PBKDF2_KEY_LENGTH = 32


    const val PBKDF2_ITERATIONS_FAST = 5000
    const val PBKDF2_ITERATIONS_STANDARD = 10000
    const val PBKDF2_ITERATIONS_HIGH = 50000
    const val PBKDF2_ITERATIONS_PARANOID = 100000


    const val PBKDF2_ITERATIONS = PBKDF2_ITERATIONS_STANDARD


    const val ENCRYPTED_EXTENSION = ".enc"


    const val CONFIG_FILE = "app_config.json"
    const val ENCRYPTED_CONFIG_FILE = "app_config.json.enc"
    const val ENCRYPTION_META_FILE = "encryption_meta.json"


    const val ENCRYPTED_ASSETS_DIR = "encrypted"
    const val ENCRYPTED_HTML_DIR = "encrypted/html"
    const val ENCRYPTED_MEDIA_DIR = "encrypted/media"



    @Deprecated("盐值现在由 deriveKeyFromPackage() 动态生成，此常量仅用于旧版兼容")
    val KEY_DERIVATION_SALT = byteArrayOf(
        0x57, 0x65, 0x62, 0x54, 0x6F, 0x41, 0x70, 0x70,
        0x45, 0x6E, 0x63, 0x72, 0x79, 0x70, 0x74, 0x21
    )


    val HKDF_SALT = "WebToApp:KeyDerivation:v2".toByteArray()
    val HKDF_INFO_ENCRYPTION = "AES-256-GCM:AppEncryption".toByteArray()
    val HKDF_INFO_INTEGRITY = "HMAC-SHA256:Integrity".toByteArray()
    val HKDF_INFO_OBFUSCATION = "XOR:StringObfuscation".toByteArray()


    const val THREAT_CHECK_INTERVAL_MS = 5000L
    const val MAX_THREAT_LEVEL_ALLOWED = 2


    const val INTEGRITY_CHECK_ENABLED = "integrity_check"
    const val ANTI_DEBUG_ENABLED = "anti_debug"
    const val ANTI_TAMPER_ENABLED = "anti_tamper"


    const val NATIVE_LIB_NAME = "crypto_engine"
}
