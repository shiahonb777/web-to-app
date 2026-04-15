/**
 * WebToApp Native Crypto Engine
 *
 * Provides native encryption/decryption to increase reverse-engineering effort.
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>
#include <random>
#include "crypto_engine.h"

#define LOG_TAG "NativeCrypto"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Global cache
static std::vector<uint8_t> g_cached_key;
static bool g_key_cached = false;
static bool g_integrity_checked = false;
static bool g_integrity_passed = false;

/**
 * Derive the AES key from the package name and signature instead of hardcoding.
 */
static std::vector<uint8_t> deriveKeyFromPackage(
    JNIEnv* env,
    const std::string& packageName,
    jbyteArray signature
) {
    // extract signature bytes
    jsize sig_len = env->GetArrayLength(signature);
    std::vector<uint8_t> sig_bytes(sig_len);
    env->GetByteArrayRegion(signature, 0, sig_len, reinterpret_cast<jbyte*>(sig_bytes.data()));
    
    // build password from package name and signature hash
    std::vector<uint8_t> sig_hash = KeyDerivation::sha256(sig_bytes.data(), sig_bytes.size());
    std::string password = packageName + ":";
    for (auto b : sig_hash) {
        char hex[3];
        snprintf(hex, sizeof(hex), "%02x", b);
        password += hex;
    }
    
    // combine fixed salt with package hash
    const uint8_t base_salt[] = {
        0x57, 0x65, 0x62, 0x54, 0x6F, 0x41, 0x70, 0x70,  // "WebToApp"
        0x45, 0x6E, 0x63, 0x72, 0x79, 0x70, 0x74, 0x21   // "Encrypt!"
    };
    
    std::vector<uint8_t> pkg_hash = KeyDerivation::sha256(
        reinterpret_cast<const uint8_t*>(packageName.c_str()),
        packageName.length()
    );
    
    std::vector<uint8_t> salt(base_salt, base_salt + sizeof(base_salt));
    salt.insert(salt.end(), pkg_hash.begin(), pkg_hash.begin() + 16);
    
    // derive the key via PBKDF2
    return KeyDerivation::deriveKey(
        password,
        salt.data(), salt.size(),
        CryptoConstants::PBKDF2_ITERATIONS,
        CryptoConstants::AES_KEY_SIZE
    );
}

extern "C" {

/**
 * Initialize the crypto engine once per process.
 */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_init(
    JNIEnv* env,
    jobject thiz,
    jobject context
) {
    LOGI("Initializing crypto engine");
    
    // run integrity checks in relaxed mode to avoid emulator false positives
    if (!g_integrity_checked) {
        // detect emulator
        bool isEmulator = AntiDebug::isRunningInEmulator();
        
        if (isEmulator) {
            // emulator: skip strict checks and proceed
            LOGW("Running in emulator, skipping strict integrity checks");
            g_integrity_passed = true;
        } else {
            // on real devices, run debugger/Frida/Xposed checks
            g_integrity_passed = !AntiDebug::isDebuggerAttached() &&
                                !AntiDebug::detectFrida() &&
                                !AntiDebug::detectXposed();
            
            if (!g_integrity_passed) {
                LOGW("Integrity check failed on real device");
            }
        }
        g_integrity_checked = true;
    }
    
    return g_integrity_passed ? JNI_TRUE : JNI_FALSE;
}

/**
 * Decrypt data using the derived key tied to the package/signature.
 *
 * @param encrypted Ciphertext input
 * @param packageName Package name
 * @param signature App signature
 * @return Plaintext bytes or null on failure
 */
JNIEXPORT jbyteArray JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_decrypt(
    JNIEnv* env,
    jobject thiz,
    jbyteArray encrypted,
    jstring packageName,
    jbyteArray signature
) {
    // 1. relaxed safety checks
    // Skip debugger detection in emulators to prevent false negative decrypts
    if (!g_integrity_passed && !AntiDebug::isRunningInEmulator()) {
        // On real devices, check debugger again
        if (AntiDebug::isDebuggerAttached()) {
            LOGE("Debugger detected, refusing to decrypt");
            return nullptr;
        }
    }
    
    // 2. validate parameters
    if (encrypted == nullptr || packageName == nullptr || signature == nullptr) {
        LOGE("Invalid parameters");
        return nullptr;
    }
    
    const char* pkg_str = env->GetStringUTFChars(packageName, nullptr);
    if (pkg_str == nullptr) {
        LOGE("Failed to get package name");
        return nullptr;
    }
    std::string pkg(pkg_str);
    env->ReleaseStringUTFChars(packageName, pkg_str);
    
    // 3. fetch or derive key
    std::vector<uint8_t> key;
    if (g_key_cached) {
        key = g_cached_key;
    } else {
        key = deriveKeyFromPackage(env, pkg, signature);
        g_cached_key = key;
        g_key_cached = true;
    }
    
    // 4. read encrypted data
    jsize enc_len = env->GetArrayLength(encrypted);
    if (enc_len < CryptoConstants::AES_GCM_IV_SIZE + CryptoConstants::AES_GCM_TAG_SIZE) {
        LOGE("Encrypted data too short");
        return nullptr;
    }
    
    std::vector<uint8_t> enc_data(enc_len);
    env->GetByteArrayRegion(encrypted, 0, enc_len, reinterpret_cast<jbyte*>(enc_data.data()));
    
    // 5. parse format: [4 bytes path_len][path][IV][ciphertext+tag]
    if (enc_len < 4) {
        LOGE("Invalid encrypted format");
        return nullptr;
    }
    
    uint32_t path_len = (enc_data[0] << 24) | (enc_data[1] << 16) | 
                        (enc_data[2] << 8) | enc_data[3];
    
    if (path_len > 1024 || 4 + path_len + CryptoConstants::AES_GCM_IV_SIZE > enc_len) {
        LOGE("Invalid path length: %u", path_len);
        return nullptr;
    }
    
    // extract path as AAD
    std::vector<uint8_t> aad(enc_data.begin() + 4, enc_data.begin() + 4 + path_len);
    
    // extract IV
    size_t iv_offset = 4 + path_len;
    std::vector<uint8_t> iv(enc_data.begin() + iv_offset, 
                           enc_data.begin() + iv_offset + CryptoConstants::AES_GCM_IV_SIZE);
    
    // extract ciphertext
    size_t ct_offset = iv_offset + CryptoConstants::AES_GCM_IV_SIZE;
    std::vector<uint8_t> ciphertext(enc_data.begin() + ct_offset, enc_data.end());
    
    // 6. perform decryption
    CryptoResult result = AesGcm::decrypt(
        ciphertext.data(), ciphertext.size(),
        key.data(), key.size(),
        iv.data(), iv.size(),
        aad.data(), aad.size()
    );
    
    if (!result.success) {
        LOGE("Decryption failed: %s", result.error.c_str());
        return nullptr;
    }
    
    // 7. return decrypted bytes
    jbyteArray output = env->NewByteArray(result.data.size());
    if (output == nullptr) {
        LOGE("Failed to allocate output array");
        return nullptr;
    }
    
    env->SetByteArrayRegion(output, 0, result.data.size(), 
                           reinterpret_cast<const jbyte*>(result.data.data()));
    
    return output;
}

/**
 * Perform anti-debugging integrity checks.
 */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_verifyIntegrity(
    JNIEnv* env,
    jobject thiz,
    jobject context
) {
    bool passed = true;
    
    // check debugger
    if (AntiDebug::isDebuggerAttached()) {
        LOGW("Debugger attached");
        passed = false;
    }
    
    // check Frida
    if (AntiDebug::detectFrida()) {
        LOGW("Frida detected");
        passed = false;
    }
    
    // check Xposed
    if (AntiDebug::detectXposed()) {
        LOGW("Xposed detected");
        passed = false;
    }
    
    // check for root
    if (AntiDebug::isRooted()) {
        LOGW("Device is rooted");
        // root is a warning, not a fatal error
    }
    
    // check emulator
    if (AntiDebug::isRunningInEmulator()) {
        LOGW("Running in emulator");
        // emulator is just a warning
    }
    
    g_integrity_passed = passed;
    return passed ? JNI_TRUE : JNI_FALSE;
}

/**
 * Clear the cached derived key.
 */
JNIEXPORT void JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_clearCache(
    JNIEnv* env,
    jobject thiz
) {
    // securely wipe key material
    if (g_key_cached) {
        std::fill(g_cached_key.begin(), g_cached_key.end(), 0);
        g_cached_key.clear();
        g_key_cached = false;
    }
    
    LOGI("Cache cleared");
}

/**
 * Return the signature hash from IntegrityCheck.
 */
JNIEXPORT jstring JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_getSignatureHash(
    JNIEnv* env,
    jobject thiz,
    jobject context
) {
    std::string hash = IntegrityCheck::getSignatureHash(env, context);
    return env->NewStringUTF(hash.c_str());
}

} // extern "C"
