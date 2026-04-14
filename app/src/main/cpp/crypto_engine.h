#ifndef CRYPTO_ENGINE_H
#define CRYPTO_ENGINE_H

#include <jni.h>
#include <string>
#include <vector>
#include <cstdint>

// Encryption constants
namespace CryptoConstants {
    constexpr int AES_KEY_SIZE = 32;        // 256 bits
    constexpr int AES_GCM_IV_SIZE = 12;     // 96 bits
    constexpr int AES_GCM_TAG_SIZE = 16;    // 128 bits
    constexpr int PBKDF2_ITERATIONS = 10000;
    
    // Encrypted file magic "WTAE"
    constexpr uint32_t ENCRYPTED_MAGIC = 0x57544145;
}

// Encryption result
struct CryptoResult {
    bool success;
    std::vector<uint8_t> data;
    std::string error;
};

// AES-GCM encrypt/decrypt
class AesGcm {
public:
    static CryptoResult encrypt(
        const uint8_t* plaintext, size_t plaintext_len,
        const uint8_t* key, size_t key_len,
        const uint8_t* iv, size_t iv_len,
        const uint8_t* aad, size_t aad_len
    );
    
    static CryptoResult decrypt(
        const uint8_t* ciphertext, size_t ciphertext_len,
        const uint8_t* key, size_t key_len,
        const uint8_t* iv, size_t iv_len,
        const uint8_t* aad, size_t aad_len
    );
};

// Key derivation
class KeyDerivation {
public:
    static std::vector<uint8_t> deriveKey(
        const std::string& password,
        const uint8_t* salt, size_t salt_len,
        int iterations,
        int key_length
    );
    
    static std::vector<uint8_t> sha256(const uint8_t* data, size_t len);
};

// Anti-debugging detection
class AntiDebug {
public:
    static bool isDebuggerAttached();
    static bool isTracerAttached();
    static bool detectFrida();
    static bool detectXposed();
    static bool isRunningInEmulator();
    static bool isRooted();
    
    // Additional helpers
    static void setStrictMode(bool enabled);  // enable strict mode
    static int getSecurityThreatLevel();      // return threat level (0-100)
    static bool shouldBlockSensitiveOperation(); // whether to block sensitive operations
};

// Integrity checks
class IntegrityCheck {
public:
    static bool verifySignature(JNIEnv* env, jobject context, const std::string& expected_hash);
    static bool verifyApkIntegrity(JNIEnv* env, jobject context);
    static std::string getSignatureHash(JNIEnv* env, jobject context);
};

#endif // CRYPTO_ENGINE_H
