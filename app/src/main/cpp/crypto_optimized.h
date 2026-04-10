#ifndef CRYPTO_OPTIMIZED_H
#define CRYPTO_OPTIMIZED_H

/**
 * crypto_optimized.h — 优化版加密引擎头文件
 *
 * 相比原始 crypto_engine.h 的改进：
 * 1. AES-NI / ARMv8-CE 硬件加速 (运行时检测)
 * 2. 恒定时间 S-Box 查找 (防止时序攻击)
 * 3. 标准 PBKDF2-HMAC-SHA256 (RFC 2898)
 * 4. 4x 流水线 AES-GCM-CTR (吞吐量提升)
 * 5. 批量加密接口 (减少 JNI 跨越开销)
 * 6. 安全内存管理 (volatile 擦除)
 */

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ====================================================================
 * 恒定时间工具
 * ==================================================================== */

/**
 * 恒定时间 S-Box 查找 (防止缓存时序侧信道)
 * 通过对所有 256 个条目做条件移动，确保查找时间恒定
 */
static inline uint8_t ct_sbox_lookup(const uint8_t sbox[256], uint8_t idx) {
    uint8_t result = 0;
    for (int i = 0; i < 256; i++) {
        /* 使用位运算实现条件选择，避免分支 */
        uint8_t mask = (uint8_t)(-(int8_t)(idx == i));
        result |= sbox[i] & mask;
    }
    return result;
}

/**
 * 恒定时间内存比较
 * 防止通过提前退出泄漏相等字节数
 */
static inline int ct_memcmp(const void *a, const void *b, size_t n) {
    const uint8_t *pa = (const uint8_t *)a;
    const uint8_t *pb = (const uint8_t *)b;
    uint8_t diff = 0;
    for (size_t i = 0; i < n; i++) {
        diff |= pa[i] ^ pb[i];
    }
    return diff;
}

/**
 * 安全内存擦除 (不会被编译器优化掉)
 */
static inline void secure_zero(void *p, size_t n) {
    volatile uint8_t *vp = (volatile uint8_t *)p;
    while (n--) {
        *vp++ = 0;
    }
}

/* ====================================================================
 * AES-256 核心 (优化实现)
 * ==================================================================== */

#define AES256_KEY_SIZE     32
#define AES256_BLOCK_SIZE   16
#define AES256_ROUNDS       14
#define AES256_EXPANDED_KEY_SIZE (4 * AES256_BLOCK_SIZE * (AES256_ROUNDS + 1))

typedef struct {
    uint8_t round_keys[AES256_EXPANDED_KEY_SIZE];
    int     use_hw_aes;  /* 是否使用硬件 AES */
} aes256_ctx_t;

/**
 * AES-256 密钥初始化
 * 运行时检测 ARMv8 Crypto Extensions，启用硬件加速
 */
void aes256_init(aes256_ctx_t *ctx, const uint8_t key[AES256_KEY_SIZE]);

/**
 * AES-256 单块加密 (ECB 模式)
 */
void aes256_encrypt_block(const aes256_ctx_t *ctx,
                          const uint8_t in[AES256_BLOCK_SIZE],
                          uint8_t out[AES256_BLOCK_SIZE]);

/**
 * AES-256 4 块并行加密 (用于 CTR 模式加速)
 * 在支持硬件 AES 的 CPU 上，4 块并行可利用流水线提高吞吐
 */
void aes256_encrypt_4blocks(const aes256_ctx_t *ctx,
                            const uint8_t in[4][AES256_BLOCK_SIZE],
                            uint8_t out[4][AES256_BLOCK_SIZE]);

/* ====================================================================
 * AES-256-GCM (优化实现)
 * ==================================================================== */

#define GCM_IV_SIZE  12
#define GCM_TAG_SIZE 16

typedef struct {
    aes256_ctx_t aes;
    uint8_t      H[16];       /* GHASH 子密钥 */
    uint8_t      H_table[16][16]; /* 预计算的 GHASH 表 (4-bit Shoup) */
} gcm_ctx_t;

/**
 * 初始化 GCM 上下文
 */
void gcm_init(gcm_ctx_t *ctx, const uint8_t key[AES256_KEY_SIZE]);

/**
 * GCM 加密
 * @return 0 成功, -1 失败
 */
int gcm_encrypt(
    const gcm_ctx_t *ctx,
    const uint8_t   *plaintext, size_t pt_len,
    const uint8_t   iv[GCM_IV_SIZE],
    const uint8_t   *aad, size_t aad_len,
    uint8_t         *ciphertext,       /* [pt_len] */
    uint8_t         tag[GCM_TAG_SIZE]  /* [16] */
);

/**
 * GCM 解密 + 认证验证
 * @return 0 成功 (认证通过), -1 认证失败, -2 参数错误
 */
int gcm_decrypt(
    const gcm_ctx_t *ctx,
    const uint8_t   *ciphertext, size_t ct_len,
    const uint8_t   iv[GCM_IV_SIZE],
    const uint8_t   *aad, size_t aad_len,
    const uint8_t   tag[GCM_TAG_SIZE],
    uint8_t         *plaintext         /* [ct_len] */
);

/**
 * 释放 GCM 上下文 (安全擦除密钥材料)
 */
void gcm_free(gcm_ctx_t *ctx);

/* ====================================================================
 * HMAC-SHA256
 * ==================================================================== */

#define SHA256_BLOCK_SIZE  64
#define SHA256_DIGEST_SIZE 32

typedef struct {
    uint32_t state[8];
    uint64_t count;
    uint8_t  buffer[SHA256_BLOCK_SIZE];
} sha256_ctx_t;

void sha256_init(sha256_ctx_t *ctx);
void sha256_update(sha256_ctx_t *ctx, const uint8_t *data, size_t len);
void sha256_final(sha256_ctx_t *ctx, uint8_t digest[SHA256_DIGEST_SIZE]);

/* 一次性 SHA-256 */
void sha256(const uint8_t *data, size_t len, uint8_t digest[SHA256_DIGEST_SIZE]);

typedef struct {
    sha256_ctx_t inner;
    sha256_ctx_t outer;
    uint8_t      opad_key[SHA256_BLOCK_SIZE];
} hmac_sha256_ctx_t;

void hmac_sha256_init(hmac_sha256_ctx_t *ctx, const uint8_t *key, size_t key_len);
void hmac_sha256_update(hmac_sha256_ctx_t *ctx, const uint8_t *data, size_t len);
void hmac_sha256_final(hmac_sha256_ctx_t *ctx, uint8_t mac[SHA256_DIGEST_SIZE]);

/* 一次性 HMAC-SHA256 */
void hmac_sha256(const uint8_t *key, size_t key_len,
                 const uint8_t *data, size_t data_len,
                 uint8_t mac[SHA256_DIGEST_SIZE]);

/* ====================================================================
 * PBKDF2-HMAC-SHA256 (RFC 2898)
 * ==================================================================== */

/**
 * 标准 PBKDF2-HMAC-SHA256 密钥派生
 * 
 * @param password    密码
 * @param pass_len    密码长度
 * @param salt        盐值
 * @param salt_len    盐长度
 * @param iterations  迭代次数
 * @param dk          输出密钥
 * @param dk_len      输出密钥长度
 */
void pbkdf2_hmac_sha256(
    const uint8_t *password, size_t pass_len,
    const uint8_t *salt, size_t salt_len,
    uint32_t iterations,
    uint8_t *dk, size_t dk_len
);

/* ====================================================================
 * HKDF (RFC 5869)
 * ==================================================================== */

/**
 * HKDF-Extract
 */
void hkdf_extract(
    const uint8_t *salt, size_t salt_len,
    const uint8_t *ikm, size_t ikm_len,
    uint8_t prk[SHA256_DIGEST_SIZE]
);

/**
 * HKDF-Expand
 */
void hkdf_expand(
    const uint8_t prk[SHA256_DIGEST_SIZE],
    const uint8_t *info, size_t info_len,
    uint8_t *okm, size_t okm_len
);

/* ====================================================================
 * 硬件能力检测
 * ==================================================================== */

/**
 * 检测 ARMv8 AES 硬件加速是否可用
 * @return 1 = 支持, 0 = 不支持
 */
int crypto_has_hw_aes(void);

#ifdef __cplusplus
}
#endif

#endif /* CRYPTO_OPTIMIZED_H */
