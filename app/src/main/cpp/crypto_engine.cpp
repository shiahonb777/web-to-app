/**
 * WebToApp Native Crypto Engine
 * 
 * 提供 Native 层的加密/解密功能，增加逆向难度
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

// 全局缓存
static std::vector<uint8_t> g_cached_key;
static bool g_key_cached = false;
static bool g_integrity_checked = false;
static bool g_integrity_passed = false;

/**
 * 从包名和签名派生密钥
 * 密钥不硬编码，而是动态计算
 */
static std::vector<uint8_t> deriveKeyFromPackage(
    JNIEnv* env,
    const std::string& packageName,
    jbyteArray signature
) {
    // 获取签名字节
    jsize sig_len = env->GetArrayLength(signature);
    std::vector<uint8_t> sig_bytes(sig_len);
    env->GetByteArrayRegion(signature, 0, sig_len, reinterpret_cast<jbyte*>(sig_bytes.data()));
    
    // 组合密码：包名 + ":" + 签名哈希
    std::vector<uint8_t> sig_hash = KeyDerivation::sha256(sig_bytes.data(), sig_bytes.size());
    std::string password = packageName + ":";
    for (auto b : sig_hash) {
        char hex[3];
        snprintf(hex, sizeof(hex), "%02x", b);
        password += hex;
    }
    
    // 固定盐 + 包名哈希
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
    
    // PBKDF2 派生密钥
    return KeyDerivation::deriveKey(
        password,
        salt.data(), salt.size(),
        CryptoConstants::PBKDF2_ITERATIONS,
        CryptoConstants::AES_KEY_SIZE
    );
}

extern "C" {

/**
 * 初始化加密引擎
 */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_init(
    JNIEnv* env,
    jobject thiz,
    jobject context
) {
    LOGI("Initializing crypto engine");
    
    // 执行完整性检查（宽松模式，避免在模拟器上误判）
    if (!g_integrity_checked) {
        // 检查是否在模拟器中运行
        bool isEmulator = AntiDebug::isRunningInEmulator();
        
        if (isEmulator) {
            // 模拟器环境：跳过严格检查，允许正常运行
            LOGW("Running in emulator, skipping strict integrity checks");
            g_integrity_passed = true;
        } else {
            // 真机环境：执行完整性检查
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
 * 解密数据
 * 
 * @param encrypted 加密的数据
 * @param packageName 包名
 * @param signature 应用签名
 * @return 解密后的数据，失败返回 null
 */
JNIEXPORT jbyteArray JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_decrypt(
    JNIEnv* env,
    jobject thiz,
    jbyteArray encrypted,
    jstring packageName,
    jbyteArray signature
) {
    // 1. 安全检查（宽松模式）
    // 在模拟器环境下跳过调试器检测，避免误判导致解密失败
    if (!g_integrity_passed && !AntiDebug::isRunningInEmulator()) {
        // 非模拟器环境，再次检查调试器
        if (AntiDebug::isDebuggerAttached()) {
            LOGE("Debugger detected, refusing to decrypt");
            return nullptr;
        }
    }
    
    // 2. 获取参数
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
    
    // 3. 获取或派生密钥
    std::vector<uint8_t> key;
    if (g_key_cached) {
        key = g_cached_key;
    } else {
        key = deriveKeyFromPackage(env, pkg, signature);
        g_cached_key = key;
        g_key_cached = true;
    }
    
    // 4. 获取加密数据
    jsize enc_len = env->GetArrayLength(encrypted);
    if (enc_len < CryptoConstants::AES_GCM_IV_SIZE + CryptoConstants::AES_GCM_TAG_SIZE) {
        LOGE("Encrypted data too short");
        return nullptr;
    }
    
    std::vector<uint8_t> enc_data(enc_len);
    env->GetByteArrayRegion(encrypted, 0, enc_len, reinterpret_cast<jbyte*>(enc_data.data()));
    
    // 5. 解析加密数据格式：[4 bytes: path_len][path][IV][ciphertext+tag]
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
    
    // 提取路径（用作 AAD）
    std::vector<uint8_t> aad(enc_data.begin() + 4, enc_data.begin() + 4 + path_len);
    
    // 提取 IV
    size_t iv_offset = 4 + path_len;
    std::vector<uint8_t> iv(enc_data.begin() + iv_offset, 
                           enc_data.begin() + iv_offset + CryptoConstants::AES_GCM_IV_SIZE);
    
    // 提取密文
    size_t ct_offset = iv_offset + CryptoConstants::AES_GCM_IV_SIZE;
    std::vector<uint8_t> ciphertext(enc_data.begin() + ct_offset, enc_data.end());
    
    // 6. 解密
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
    
    // 7. 返回解密数据
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
 * 验证完整性
 */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_verifyIntegrity(
    JNIEnv* env,
    jobject thiz,
    jobject context
) {
    bool passed = true;
    
    // 检查调试器
    if (AntiDebug::isDebuggerAttached()) {
        LOGW("Debugger attached");
        passed = false;
    }
    
    // 检查 Frida
    if (AntiDebug::detectFrida()) {
        LOGW("Frida detected");
        passed = false;
    }
    
    // 检查 Xposed
    if (AntiDebug::detectXposed()) {
        LOGW("Xposed detected");
        passed = false;
    }
    
    // 检查 Root
    if (AntiDebug::isRooted()) {
        LOGW("Device is rooted");
        // Root 不一定阻止运行，只是警告
    }
    
    // 检查模拟器
    if (AntiDebug::isRunningInEmulator()) {
        LOGW("Running in emulator");
        // 模拟器不一定阻止运行，只是警告
    }
    
    g_integrity_passed = passed;
    return passed ? JNI_TRUE : JNI_FALSE;
}

/**
 * 清除缓存的密钥
 */
JNIEXPORT void JNICALL
Java_com_webtoapp_core_crypto_NativeCrypto_clearCache(
    JNIEnv* env,
    jobject thiz
) {
    // 安全清除密钥
    if (g_key_cached) {
        std::fill(g_cached_key.begin(), g_cached_key.end(), 0);
        g_cached_key.clear();
        g_key_cached = false;
    }
    
    LOGI("Cache cleared");
}

/**
 * 获取签名哈希
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
