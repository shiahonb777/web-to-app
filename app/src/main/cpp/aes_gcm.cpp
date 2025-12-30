/**
 * AES-GCM 加密实现
 * 使用纯 C++ 实现，不依赖外部库
 */

#include "crypto_engine.h"
#include <cstring>
#include <algorithm>

// AES S-Box
static const uint8_t SBOX[256] = {
    0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
    0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
    0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
    0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
    0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
    0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
    0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
    0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
    0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
    0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
    0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
    0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
    0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
    0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
    0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
    0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
};

// AES 逆 S-Box
static const uint8_t INV_SBOX[256] = {
    0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
    0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
    0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
    0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
    0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
    0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
    0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
    0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
    0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
    0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
    0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
    0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
    0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
    0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
    0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
    0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
};

// Rcon
static const uint8_t RCON[11] = {
    0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
};

// AES 密钥扩展
static void aes_key_expansion(const uint8_t* key, uint8_t* round_keys, int key_size) {
    int nk = key_size / 4;  // 密钥字数
    int nr = nk + 6;        // 轮数
    int nb = 4;             // 块字数
    
    // 复制原始密钥
    memcpy(round_keys, key, key_size);
    
    uint8_t temp[4];
    int i = nk;
    
    while (i < nb * (nr + 1)) {
        memcpy(temp, round_keys + (i - 1) * 4, 4);
        
        if (i % nk == 0) {
            // RotWord
            uint8_t t = temp[0];
            temp[0] = temp[1];
            temp[1] = temp[2];
            temp[2] = temp[3];
            temp[3] = t;
            
            // SubWord
            for (int j = 0; j < 4; j++) {
                temp[j] = SBOX[temp[j]];
            }
            
            temp[0] ^= RCON[i / nk];
        } else if (nk > 6 && i % nk == 4) {
            // SubWord for 256-bit key
            for (int j = 0; j < 4; j++) {
                temp[j] = SBOX[temp[j]];
            }
        }
        
        for (int j = 0; j < 4; j++) {
            round_keys[i * 4 + j] = round_keys[(i - nk) * 4 + j] ^ temp[j];
        }
        i++;
    }
}

// GF(2^128) 乘法
static void gf_mult(const uint8_t* x, const uint8_t* y, uint8_t* result) {
    uint8_t v[16];
    memcpy(v, y, 16);
    memset(result, 0, 16);
    
    for (int i = 0; i < 16; i++) {
        for (int j = 0; j < 8; j++) {
            if ((x[i] >> (7 - j)) & 1) {
                for (int k = 0; k < 16; k++) {
                    result[k] ^= v[k];
                }
            }
            
            // v = v * x (在 GF(2^128) 中)
            bool carry = v[15] & 1;
            for (int k = 15; k > 0; k--) {
                v[k] = (v[k] >> 1) | ((v[k-1] & 1) << 7);
            }
            v[0] >>= 1;
            
            if (carry) {
                v[0] ^= 0xe1;  // 约简多项式
            }
        }
    }
}

// GHASH
static void ghash(const uint8_t* h, const uint8_t* data, size_t len, uint8_t* result) {
    memset(result, 0, 16);
    
    for (size_t i = 0; i < len; i += 16) {
        size_t block_len = std::min((size_t)16, len - i);
        
        for (size_t j = 0; j < block_len; j++) {
            result[j] ^= data[i + j];
        }
        
        uint8_t temp[16];
        gf_mult(result, h, temp);
        memcpy(result, temp, 16);
    }
}

// AES 单块加密
static void aes_encrypt_block(const uint8_t* input, uint8_t* output, const uint8_t* round_keys, int nr) {
    uint8_t state[16];
    memcpy(state, input, 16);
    
    // AddRoundKey
    for (int i = 0; i < 16; i++) {
        state[i] ^= round_keys[i];
    }
    
    for (int round = 1; round < nr; round++) {
        // SubBytes
        for (int i = 0; i < 16; i++) {
            state[i] = SBOX[state[i]];
        }
        
        // ShiftRows
        uint8_t temp = state[1];
        state[1] = state[5]; state[5] = state[9]; state[9] = state[13]; state[13] = temp;
        temp = state[2]; state[2] = state[10]; state[10] = temp;
        temp = state[6]; state[6] = state[14]; state[14] = temp;
        temp = state[15];
        state[15] = state[11]; state[11] = state[7]; state[7] = state[3]; state[3] = temp;
        
        // MixColumns
        for (int i = 0; i < 4; i++) {
            uint8_t a[4];
            for (int j = 0; j < 4; j++) a[j] = state[i*4 + j];
            
            uint8_t h[4];
            for (int j = 0; j < 4; j++) {
                h[j] = (a[j] & 0x80) ? ((a[j] << 1) ^ 0x1b) : (a[j] << 1);
            }
            
            state[i*4 + 0] = h[0] ^ a[3] ^ a[2] ^ h[1] ^ a[1];
            state[i*4 + 1] = h[1] ^ a[0] ^ a[3] ^ h[2] ^ a[2];
            state[i*4 + 2] = h[2] ^ a[1] ^ a[0] ^ h[3] ^ a[3];
            state[i*4 + 3] = h[3] ^ a[2] ^ a[1] ^ h[0] ^ a[0];
        }
        
        // AddRoundKey
        for (int i = 0; i < 16; i++) {
            state[i] ^= round_keys[round * 16 + i];
        }
    }
    
    // 最后一轮
    for (int i = 0; i < 16; i++) state[i] = SBOX[state[i]];
    
    uint8_t temp = state[1];
    state[1] = state[5]; state[5] = state[9]; state[9] = state[13]; state[13] = temp;
    temp = state[2]; state[2] = state[10]; state[10] = temp;
    temp = state[6]; state[6] = state[14]; state[14] = temp;
    temp = state[15];
    state[15] = state[11]; state[11] = state[7]; state[7] = state[3]; state[3] = temp;
    
    for (int i = 0; i < 16; i++) {
        state[i] ^= round_keys[nr * 16 + i];
    }
    
    memcpy(output, state, 16);
}

// 递增计数器
static void inc_counter(uint8_t* counter) {
    for (int i = 15; i >= 12; i--) {
        if (++counter[i] != 0) break;
    }
}

// AES-GCM 加密实现
CryptoResult AesGcm::encrypt(
    const uint8_t* plaintext, size_t plaintext_len,
    const uint8_t* key, size_t key_len,
    const uint8_t* iv, size_t iv_len,
    const uint8_t* aad, size_t aad_len
) {
    CryptoResult result;
    result.success = false;
    
    if (key_len != 16 && key_len != 24 && key_len != 32) {
        result.error = "Invalid key length";
        return result;
    }
    
    int nr = (key_len == 16) ? 10 : (key_len == 24) ? 12 : 14;
    
    // 密钥扩展
    std::vector<uint8_t> round_keys((nr + 1) * 16);
    aes_key_expansion(key, round_keys.data(), key_len);
    
    // 计算 H
    uint8_t h[16] = {0};
    aes_encrypt_block(h, h, round_keys.data(), nr);
    
    // 初始化计数器
    uint8_t j0[16] = {0};
    if (iv_len == 12) {
        memcpy(j0, iv, 12);
        j0[15] = 1;
    } else {
        // 对于非 96 位 IV，使用 GHASH
        size_t padded_len = ((iv_len + 15) / 16) * 16 + 16;
        std::vector<uint8_t> padded(padded_len, 0);
        memcpy(padded.data(), iv, iv_len);
        padded[padded_len - 1] = (iv_len * 8) & 0xff;
        padded[padded_len - 2] = ((iv_len * 8) >> 8) & 0xff;
        ghash(h, padded.data(), padded_len, j0);
    }
    
    // 加密
    result.data.resize(plaintext_len + 16);  // 密文 + tag
    uint8_t counter[16];
    memcpy(counter, j0, 16);
    
    for (size_t i = 0; i < plaintext_len; i += 16) {
        inc_counter(counter);
        
        uint8_t keystream[16];
        aes_encrypt_block(counter, keystream, round_keys.data(), nr);
        
        size_t block_len = std::min((size_t)16, plaintext_len - i);
        for (size_t j = 0; j < block_len; j++) {
            result.data[i + j] = plaintext[i + j] ^ keystream[j];
        }
    }
    
    // 计算认证标签
    size_t aad_padded_len = ((aad_len + 15) / 16) * 16;
    size_t ct_padded_len = ((plaintext_len + 15) / 16) * 16;
    std::vector<uint8_t> auth_data(aad_padded_len + ct_padded_len + 16, 0);
    
    if (aad_len > 0) memcpy(auth_data.data(), aad, aad_len);
    memcpy(auth_data.data() + aad_padded_len, result.data.data(), plaintext_len);
    
    // 长度块
    uint64_t aad_bits = aad_len * 8;
    uint64_t ct_bits = plaintext_len * 8;
    for (int i = 0; i < 8; i++) {
        auth_data[aad_padded_len + ct_padded_len + i] = (aad_bits >> (56 - i * 8)) & 0xff;
        auth_data[aad_padded_len + ct_padded_len + 8 + i] = (ct_bits >> (56 - i * 8)) & 0xff;
    }
    
    uint8_t tag[16];
    ghash(h, auth_data.data(), auth_data.size(), tag);
    
    // E(K, J0) XOR tag
    uint8_t ej0[16];
    aes_encrypt_block(j0, ej0, round_keys.data(), nr);
    for (int i = 0; i < 16; i++) {
        tag[i] ^= ej0[i];
    }
    
    memcpy(result.data.data() + plaintext_len, tag, 16);
    result.success = true;
    return result;
}

// AES-GCM 解密实现
CryptoResult AesGcm::decrypt(
    const uint8_t* ciphertext, size_t ciphertext_len,
    const uint8_t* key, size_t key_len,
    const uint8_t* iv, size_t iv_len,
    const uint8_t* aad, size_t aad_len
) {
    CryptoResult result;
    result.success = false;
    
    if (ciphertext_len < 16) {
        result.error = "Ciphertext too short";
        return result;
    }
    
    size_t ct_len = ciphertext_len - 16;
    const uint8_t* tag = ciphertext + ct_len;
    
    // 先加密（GCM 加密和解密使用相同操作）
    CryptoResult enc_result = encrypt(ciphertext, ct_len, key, key_len, iv, iv_len, aad, aad_len);
    if (!enc_result.success) {
        result.error = enc_result.error;
        return result;
    }
    
    // 验证标签
    const uint8_t* computed_tag = enc_result.data.data() + ct_len;
    bool tag_valid = true;
    for (int i = 0; i < 16; i++) {
        if (computed_tag[i] != tag[i]) {
            tag_valid = false;
        }
    }
    
    if (!tag_valid) {
        result.error = "Authentication failed";
        return result;
    }
    
    result.data.assign(enc_result.data.begin(), enc_result.data.begin() + ct_len);
    result.success = true;
    return result;
}

// PBKDF2-HMAC-SHA256 密钥派生
std::vector<uint8_t> KeyDerivation::deriveKey(
    const std::string& password,
    const uint8_t* salt, size_t salt_len,
    int iterations,
    int key_length
) {
    std::vector<uint8_t> result(key_length);
    
    // 简化实现：使用多轮 SHA256
    // 实际生产环境应使用完整的 PBKDF2
    std::vector<uint8_t> data;
    data.insert(data.end(), password.begin(), password.end());
    data.insert(data.end(), salt, salt + salt_len);
    
    std::vector<uint8_t> hash = sha256(data.data(), data.size());
    
    for (int i = 1; i < iterations; i++) {
        // 组合上一轮哈希和密码
        std::vector<uint8_t> input;
        input.insert(input.end(), hash.begin(), hash.end());
        input.insert(input.end(), password.begin(), password.end());
        hash = sha256(input.data(), input.size());
    }
    
    // 截取所需长度
    size_t copy_len = std::min((size_t)key_length, hash.size());
    memcpy(result.data(), hash.data(), copy_len);
    
    // 如果需要更长的密钥，继续派生
    while (copy_len < (size_t)key_length) {
        std::vector<uint8_t> input;
        input.insert(input.end(), hash.begin(), hash.end());
        input.push_back((copy_len / 32) & 0xff);
        hash = sha256(input.data(), input.size());
        
        size_t chunk = std::min((size_t)32, key_length - copy_len);
        memcpy(result.data() + copy_len, hash.data(), chunk);
        copy_len += chunk;
    }
    
    return result;
}

// SHA-256 实现
std::vector<uint8_t> KeyDerivation::sha256(const uint8_t* data, size_t len) {
    // SHA-256 常量
    static const uint32_t K[64] = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };
    
    // 初始哈希值
    uint32_t h[8] = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };
    
    // 填充消息
    size_t padded_len = ((len + 9 + 63) / 64) * 64;
    std::vector<uint8_t> padded(padded_len, 0);
    memcpy(padded.data(), data, len);
    padded[len] = 0x80;
    
    uint64_t bit_len = len * 8;
    for (int i = 0; i < 8; i++) {
        padded[padded_len - 1 - i] = (bit_len >> (i * 8)) & 0xff;
    }
    
    // 处理每个块
    for (size_t block = 0; block < padded_len; block += 64) {
        uint32_t w[64];
        
        for (int i = 0; i < 16; i++) {
            w[i] = (padded[block + i*4] << 24) | (padded[block + i*4 + 1] << 16) |
                   (padded[block + i*4 + 2] << 8) | padded[block + i*4 + 3];
        }
        
        for (int i = 16; i < 64; i++) {
            uint32_t s0 = ((w[i-15] >> 7) | (w[i-15] << 25)) ^ ((w[i-15] >> 18) | (w[i-15] << 14)) ^ (w[i-15] >> 3);
            uint32_t s1 = ((w[i-2] >> 17) | (w[i-2] << 15)) ^ ((w[i-2] >> 19) | (w[i-2] << 13)) ^ (w[i-2] >> 10);
            w[i] = w[i-16] + s0 + w[i-7] + s1;
        }
        
        uint32_t a = h[0], b = h[1], c = h[2], d = h[3];
        uint32_t e = h[4], f = h[5], g = h[6], hh = h[7];
        
        for (int i = 0; i < 64; i++) {
            uint32_t S1 = ((e >> 6) | (e << 26)) ^ ((e >> 11) | (e << 21)) ^ ((e >> 25) | (e << 7));
            uint32_t ch = (e & f) ^ ((~e) & g);
            uint32_t temp1 = hh + S1 + ch + K[i] + w[i];
            uint32_t S0 = ((a >> 2) | (a << 30)) ^ ((a >> 13) | (a << 19)) ^ ((a >> 22) | (a << 10));
            uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
            uint32_t temp2 = S0 + maj;
            
            hh = g; g = f; f = e; e = d + temp1;
            d = c; c = b; b = a; a = temp1 + temp2;
        }
        
        h[0] += a; h[1] += b; h[2] += c; h[3] += d;
        h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
    }
    
    std::vector<uint8_t> result(32);
    for (int i = 0; i < 8; i++) {
        result[i*4] = (h[i] >> 24) & 0xff;
        result[i*4 + 1] = (h[i] >> 16) & 0xff;
        result[i*4 + 2] = (h[i] >> 8) & 0xff;
        result[i*4 + 3] = h[i] & 0xff;
    }
    
    return result;
}
