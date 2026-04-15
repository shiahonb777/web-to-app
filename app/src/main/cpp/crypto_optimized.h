#ifndef CRYPTO_OPTIMIZED_H
#define CRYPTO_OPTIMIZED_H

/* Note. */

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Note. */

/* Note. */
static inline uint8_t ct_sbox_lookup(const uint8_t sbox[256], uint8_t idx) {
    uint8_t result = 0;
    for (int i = 0; i < 256; i++) {
        /* Note. */
        uint8_t mask = (uint8_t)(-(int8_t)(idx == i));
        result |= sbox[i] & mask;
    }
    return result;
}

/* Note. */
static inline int ct_memcmp(const void *a, const void *b, size_t n) {
    const uint8_t *pa = (const uint8_t *)a;
    const uint8_t *pb = (const uint8_t *)b;
    uint8_t diff = 0;
    for (size_t i = 0; i < n; i++) {
        diff |= pa[i] ^ pb[i];
    }
    return diff;
}

/* Note. */
static inline void secure_zero(void *p, size_t n) {
    volatile uint8_t *vp = (volatile uint8_t *)p;
    while (n--) {
        *vp++ = 0;
    }
}

/* Note. */

#define AES256_KEY_SIZE     32
#define AES256_BLOCK_SIZE   16
#define AES256_ROUNDS       14
#define AES256_EXPANDED_KEY_SIZE (4 * AES256_BLOCK_SIZE * (AES256_ROUNDS + 1))

typedef struct {
    uint8_t round_keys[AES256_EXPANDED_KEY_SIZE];
    int     use_hw_aes;  /* Note. */
} aes256_ctx_t;

/* Note. */
void aes256_init(aes256_ctx_t *ctx, const uint8_t key[AES256_KEY_SIZE]);

/* Note. */
void aes256_encrypt_block(const aes256_ctx_t *ctx,
                          const uint8_t in[AES256_BLOCK_SIZE],
                          uint8_t out[AES256_BLOCK_SIZE]);

/* Note. */
void aes256_encrypt_4blocks(const aes256_ctx_t *ctx,
                            const uint8_t in[4][AES256_BLOCK_SIZE],
                            uint8_t out[4][AES256_BLOCK_SIZE]);

/* Note. */

#define GCM_IV_SIZE  12
#define GCM_TAG_SIZE 16

typedef struct {
    aes256_ctx_t aes;
    uint8_t      H[16];       /* Note. */
    uint8_t      H_table[16][16]; /* Note. */
} gcm_ctx_t;

/* Note. */
void gcm_init(gcm_ctx_t *ctx, const uint8_t key[AES256_KEY_SIZE]);

/* Note. */
int gcm_encrypt(
    const gcm_ctx_t *ctx,
    const uint8_t   *plaintext, size_t pt_len,
    const uint8_t   iv[GCM_IV_SIZE],
    const uint8_t   *aad, size_t aad_len,
    uint8_t         *ciphertext,       /* [pt_len] */
    uint8_t         tag[GCM_TAG_SIZE]  /* [16] */
);

/* Note. */
int gcm_decrypt(
    const gcm_ctx_t *ctx,
    const uint8_t   *ciphertext, size_t ct_len,
    const uint8_t   iv[GCM_IV_SIZE],
    const uint8_t   *aad, size_t aad_len,
    const uint8_t   tag[GCM_TAG_SIZE],
    uint8_t         *plaintext         /* [ct_len] */
);

/* Note. */
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

/* Note. */
void sha256(const uint8_t *data, size_t len, uint8_t digest[SHA256_DIGEST_SIZE]);

typedef struct {
    sha256_ctx_t inner;
    sha256_ctx_t outer;
    uint8_t      opad_key[SHA256_BLOCK_SIZE];
} hmac_sha256_ctx_t;

void hmac_sha256_init(hmac_sha256_ctx_t *ctx, const uint8_t *key, size_t key_len);
void hmac_sha256_update(hmac_sha256_ctx_t *ctx, const uint8_t *data, size_t len);
void hmac_sha256_final(hmac_sha256_ctx_t *ctx, uint8_t mac[SHA256_DIGEST_SIZE]);

/* Note. */
void hmac_sha256(const uint8_t *key, size_t key_len,
                 const uint8_t *data, size_t data_len,
                 uint8_t mac[SHA256_DIGEST_SIZE]);

/* ====================================================================
 * PBKDF2-HMAC-SHA256 (RFC 2898)
 * ==================================================================== */

/* Note. */
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

/* Note. */

/* Note. */
int crypto_has_hw_aes(void);

#ifdef __cplusplus
}
#endif

#endif /* CRYPTO_OPTIMIZED_H */

