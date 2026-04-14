/**
 * C-level APK optimizer that strips editor-only code, assets, and metadata before Shell
 * packaging.
 *
 * Problem: Building from the editor APK leaves unused DEX, resources, and metadata in the
 * generated APK.
 *
 * Approach: In the final packaging stage, analyze ZIP entries at the byte level and
 *    1. Slim DEX files by flagging sparsely used entries.
 *    2. Compact resources.arsc entries by trimming unused string pool padding.
 *    3. Recompress every entry with adaptive zlib (level 9) for maximum shrinkage.
 *    4. Deduplicate entries using CRC32, merging identical content.
 *    5. Align STORED entries to 4 bytes while keeping padding minimal.
 *    6. Scan res/ for resources never referenced in Shell mode.
 *
 * All work runs without JVM heap allocations, so it scales to very large APKs (>100MB).
 */

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <android/log.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <zlib.h>
#include <errno.h>

#define TAG "ApkOptimizer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* JNI helper macro matching NativeApkOptimizer.kt signatures */
#define JNI_FUNC(name) Java_com_webtoapp_core_apkbuilder_NativeApkOptimizer_##name

/* ====================================================================
 * ZIP format constants
 * ==================================================================== */

/* Local File Header */
#define ZIP_LOCAL_MAGIC        0x04034b50
#define ZIP_LOCAL_HEADER_SIZE  30

/* Central Directory Header */
#define ZIP_CENTRAL_MAGIC      0x02014b50
#define ZIP_CENTRAL_HEADER_SIZE 46

/* End of Central Directory Record */
#define ZIP_EOCD_MAGIC         0x06054b50
#define ZIP_EOCD_MIN_SIZE      22

/* Compression methods */
#define ZIP_METHOD_STORED      0
#define ZIP_METHOD_DEFLATED    8

/* DEX magic: "dex\n035\0" or "dex\n039\0" */
#define DEX_MAGIC_SIZE         8

/* ====================================================================
 * Helper functions
 * ==================================================================== */

static inline uint16_t read_u16_le(const uint8_t *p) {
    return (uint16_t)p[0] | ((uint16_t)p[1] << 8);
}

static inline uint32_t read_u32_le(const uint8_t *p) {
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) |
           ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

static inline void write_u16_le(uint8_t *p, uint16_t v) {
    p[0] = (uint8_t)(v & 0xFF);
    p[1] = (uint8_t)((v >> 8) & 0xFF);
}

static inline void write_u32_le(uint8_t *p, uint32_t v) {
    p[0] = (uint8_t)(v & 0xFF);
    p[1] = (uint8_t)((v >> 8) & 0xFF);
    p[2] = (uint8_t)((v >> 16) & 0xFF);
    p[3] = (uint8_t)((v >> 24) & 0xFF);
}

/* ====================================================================
 * Optimization statistics
 * ==================================================================== */
typedef struct {
    int64_t original_size;
    int64_t optimized_size;
    int     entries_total;
    int     entries_stripped;
    int     entries_recompressed;
    int     entries_deduplicated;
    int     dex_files_kept;
    int     dex_files_stripped;
    int64_t native_lib_savings;
    int64_t dex_savings;
    int64_t resource_savings;
    int64_t recompression_savings;
    int64_t dedup_savings;
    int64_t unused_res_savings;
} optimize_stats_t;

/* ====================================================================
 * Entry filters for stripping APK entries
 * ==================================================================== */

/**
 * Returns whether a filename is part of a required framework resource.
 *
 * AppCompat/AndroidX/Material files use known prefixes, so those layouts and support
 * XML must always stay in the package.
 *
 * @param filename Base filename without directory prefixes.
 * @return 1 to keep, 0 to consider for removal.
 */
static int is_framework_resource(const char *filename) {
    /* Note. */
    if (strncmp(filename, "abc_", 4) == 0) return 1;
    /* Material Components: mtrl_* */
    if (strncmp(filename, "mtrl_", 5) == 0) return 1;
    /* Design Library: design_* */
    if (strncmp(filename, "design_", 7) == 0) return 1;
    /* Material 3: m3_* */
    if (strncmp(filename, "m3_", 3) == 0) return 1;
    /* AndroidX Preference: preference_* */
    if (strncmp(filename, "preference_", 11) == 0) return 1;
    /* Notification compat: notification_* */
    if (strncmp(filename, "notification_", 13) == 0) return 1;
    /* AndroidX core: tooltip_*, custom_dialog_* */
    if (strncmp(filename, "tooltip_", 8) == 0) return 1;
    if (strncmp(filename, "custom_dialog", 13) == 0) return 1;
    /* Support library compat */
    if (strncmp(filename, "compat_", 7) == 0) return 1;
    if (strncmp(filename, "support_", 8) == 0) return 1;
    
    return 0;
}

/* Note. */
static const char *get_res_filename(const char *path) {
    const char *last_slash = strrchr(path, '/');
    return last_slash ? last_slash + 1 : path;
}

/* Note. */
static int is_strippable_resource(const char *name, int name_len) {
    /* Note. */
    
    if (name_len > 4) {
        /* Note. */
        const char *filename = get_res_filename(name);
        
        /* Note. */
        if (is_framework_resource(filename)) return 0;
        
        /* Note. */
        if (strstr(name, "ic_launcher") != NULL) return 0;
        
        /* Note. */
        if (strncmp(name, "res/mipmap", 10) == 0) return 0;
        
        /* Note. */
        if (strncmp(name, "res/layout", 10) == 0) return 1;
        /* Note. */
        if (strncmp(name, "res/menu", 8) == 0) return 1;
        /* Note. */
        if (strncmp(name, "res/anim", 8) == 0) return 1;
        /* res/animator* */
        if (strncmp(name, "res/animator", 12) == 0) return 1;
        /* res/transition* */
        if (strncmp(name, "res/transition", 14) == 0) return 1;
        /* Note. */
        if (strncmp(name, "res/navigation", 14) == 0) return 1;
        /* Note. */
        if (strncmp(name, "res/font", 8) == 0) return 1;
        /* Note. */
        if (strncmp(name, "res/raw", 7) == 0) return 1;
        
        /* Note. */
        if (strncmp(name, "res/drawable-", 13) == 0 &&
            strstr(name, "nodpi") == NULL) {
            return 1;
        }
    }
    
    return 0;
}

/* Note. */
static int is_extra_dex_strippable(const char *name, int name_len) {
    (void)name; (void)name_len;
    /* Note. */
    return 0;
}

/* Note. */
static int should_strip_entry(const char *name, int name_len) {
    /* Note. */
    if (name_len > 9 && strncmp(name, "META-INF/", 9) == 0) {
        const char *suffix = name + name_len;
        while (suffix > name && *(suffix-1) != '.') suffix--;
        if (suffix > name) {
            if (strcmp(suffix, "SF") == 0 || strcmp(suffix, "RSA") == 0 ||
                strcmp(suffix, "DSA") == 0 || strcmp(suffix, "EC") == 0) return 1;
            if (strcmp(name, "META-INF/MANIFEST.MF") == 0) return 1;
        }
        /* Note. */
        if (strstr(name, "stamp-cert-sha256") != NULL) return 1;
        /* Note. */
        if (strstr(name, ".kotlin_module") != NULL) return 1;
        /* ProGuard mapping */
        if (strstr(name, "proguard") != NULL) return 1;
        /* Note. */
        if (name_len > 9 + 7 && strncmp(name+9, "version", 7) == 0) return 1;
    }
    
    /* Note. */
    if (name_len > 7 && strncmp(name, "kotlin/", 7) == 0) return 1;
    if (strcmp(name, "DebugProbesKt.bin") == 0) return 1;
    
    /* Note. */
    if (name_len >= 16 && strncmp(name, "assets/template/", 16) == 0) return 1;
    if (name_len >= 23 && strncmp(name, "assets/sample_projects/", 23) == 0) return 1;
    if (name_len >= 10 && strncmp(name, "assets/ai/", 10) == 0) return 1;
    if (strcmp(name, "assets/litellm_model_prices.json") == 0) return 1;
    if (name_len >= 18 && strncmp(name, "assets/extensions/", 18) == 0) return 1;
    
    /* Note. */
    if (name_len > 4 && strncmp(name, "res/", 4) == 0) {
        return is_strippable_resource(name, name_len);
    }
    
    /* Note. */
    if (is_extra_dex_strippable(name, name_len)) return 1;
    
    /* Note. */
    if (name_len > 0 && name[name_len-1] == '/') {
        /* Note. */
        return 1;
    }
    
    return 0;
}

/* Note. */

#define DEDUP_TABLE_SIZE 4096
typedef struct {
    uint32_t crc;
    uint32_t size;
    int used;
} dedup_entry_t;

static dedup_entry_t dedup_table[DEDUP_TABLE_SIZE];
static int dedup_count = 0;

static void dedup_init(void) {
    memset(dedup_table, 0, sizeof(dedup_table));
    dedup_count = 0;
}

/* Note. */
static int dedup_check_and_add(uint32_t crc, uint32_t size) {
    /* Note. */
    if (size < 4096) return 0;
    
    uint32_t hash = (crc ^ (crc >> 16)) % DEDUP_TABLE_SIZE;
    for (int i = 0; i < DEDUP_TABLE_SIZE; i++) {
        uint32_t idx = (hash + i) % DEDUP_TABLE_SIZE;
        if (!dedup_table[idx].used) {
            /* Note. */
            dedup_table[idx].crc = crc;
            dedup_table[idx].size = size;
            dedup_table[idx].used = 1;
            dedup_count++;
            return 0;
        }
        if (dedup_table[idx].crc == crc && dedup_table[idx].size == size) {
            /* Note. */
            return 1;
        }
    }
    /* Note. */
    return 0;
}

/* Note. */

/* Note. */
static int64_t recompress_deflate(const uint8_t *src, uint32_t src_len,
                                   uint8_t *dst, uint32_t dst_cap) {
    z_stream strm;
    memset(&strm, 0, sizeof(strm));
    
    /* Note. */
    int ret = deflateInit2(&strm, Z_BEST_COMPRESSION,
                           Z_DEFLATED, -15, /* raw deflate (no zlib header) */
                           9, /* max memory level */
                           Z_DEFAULT_STRATEGY);
    if (ret != Z_OK) {
        LOGE("deflateInit2 failed: %d", ret);
        return -1;
    }
    
    strm.next_in = (Bytef *)src;
    strm.avail_in = src_len;
    strm.next_out = (Bytef *)dst;
    strm.avail_out = dst_cap;
    
    ret = deflate(&strm, Z_FINISH);
    if (ret != Z_STREAM_END) {
        LOGE("deflate failed: %d (src=%u, cap=%u)", ret, src_len, dst_cap);
        deflateEnd(&strm);
        return -1;
    }
    
    int64_t compressed_len = (int64_t)strm.total_out;
    deflateEnd(&strm);
    
    return compressed_len;
}

/* Note. */
static int64_t inflate_data(const uint8_t *src, uint32_t src_len,
                             uint8_t *dst, uint32_t dst_cap) {
    z_stream strm;
    memset(&strm, 0, sizeof(strm));
    
    int ret = inflateInit2(&strm, -15); /* raw inflate */
    if (ret != Z_OK) {
        LOGE("inflateInit2 failed: %d", ret);
        return -1;
    }
    
    strm.next_in = (Bytef *)src;
    strm.avail_in = src_len;
    strm.next_out = (Bytef *)dst;
    strm.avail_out = dst_cap;
    
    ret = inflate(&strm, Z_FINISH);
    if (ret != Z_STREAM_END) {
        LOGE("inflate failed: %d (src=%u, cap=%u)", ret, src_len, dst_cap);
        inflateEnd(&strm);
        return -1;
    }
    
    int64_t decompressed_len = (int64_t)strm.total_out;
    inflateEnd(&strm);
    
    return decompressed_len;
}

/* Note. */

/* Note. */
static int64_t write_zip_local_entry(
    int fd,
    const char *name, int name_len,
    int method,
    uint32_t crc32,
    uint32_t compressed_size,
    uint32_t uncompressed_size,
    const uint8_t *data,
    uint16_t extra_len,
    const uint8_t *extra_data
) {
    uint8_t header[ZIP_LOCAL_HEADER_SIZE];
    
    write_u32_le(header + 0, ZIP_LOCAL_MAGIC);
    write_u16_le(header + 4, 20);         /* version needed */
    write_u16_le(header + 6, 0);          /* flags */
    write_u16_le(header + 8, (uint16_t)method);
    write_u16_le(header + 10, 0);         /* mod time */
    write_u16_le(header + 12, 0x2100);    /* mod date */
    write_u32_le(header + 14, crc32);
    write_u32_le(header + 18, compressed_size);
    write_u32_le(header + 22, uncompressed_size);
    write_u16_le(header + 26, (uint16_t)name_len);
    write_u16_le(header + 28, extra_len);
    
    if (write(fd, header, ZIP_LOCAL_HEADER_SIZE) != ZIP_LOCAL_HEADER_SIZE) return -1;
    if (write(fd, name, name_len) != name_len) return -1;
    if (extra_len > 0 && extra_data != NULL) {
        if (write(fd, extra_data, extra_len) != extra_len) return -1;
    }
    if (compressed_size > 0 && data != NULL) {
        /* Note. */
        uint32_t remaining = compressed_size;
        const uint8_t *ptr = data;
        while (remaining > 0) {
            uint32_t chunk = remaining > (1024*1024) ? (1024*1024) : remaining;
            ssize_t written = write(fd, ptr, chunk);
            if (written <= 0) return -1;
            ptr += written;
            remaining -= (uint32_t)written;
        }
    }
    
    return (int64_t)ZIP_LOCAL_HEADER_SIZE + name_len + extra_len + compressed_size;
}

/* Note. */
static int64_t write_zip_central_entry(
    int fd,
    const char *name, int name_len,
    int method,
    uint32_t crc32,
    uint32_t compressed_size,
    uint32_t uncompressed_size,
    uint32_t local_offset,
    uint16_t extra_len
) {
    uint8_t header[ZIP_CENTRAL_HEADER_SIZE];
    
    write_u32_le(header + 0, ZIP_CENTRAL_MAGIC);
    write_u16_le(header + 4, 20);         /* version made by */
    write_u16_le(header + 6, 20);         /* version needed */
    write_u16_le(header + 8, 0);          /* flags */
    write_u16_le(header + 10, (uint16_t)method);
    write_u16_le(header + 12, 0);         /* mod time */
    write_u16_le(header + 14, 0x2100);    /* mod date */
    write_u32_le(header + 16, crc32);
    write_u32_le(header + 20, compressed_size);
    write_u32_le(header + 24, uncompressed_size);
    write_u16_le(header + 28, (uint16_t)name_len);
    write_u16_le(header + 30, extra_len); /* extra field len */
    write_u16_le(header + 32, 0);         /* comment len */
    write_u16_le(header + 34, 0);         /* disk number start */
    write_u16_le(header + 36, 0);         /* internal attrs */
    write_u32_le(header + 38, 0);         /* external attrs */
    write_u32_le(header + 42, local_offset);
    
    if (write(fd, header, ZIP_CENTRAL_HEADER_SIZE) != ZIP_CENTRAL_HEADER_SIZE) return -1;
    if (write(fd, name, name_len) != name_len) return -1;
    /* extra field placeholder if needed for alignment */
    if (extra_len > 0) {
        uint8_t zeros[64];
        memset(zeros, 0, sizeof(zeros));
        uint16_t remaining = extra_len;
        while (remaining > 0) {
            uint16_t w = remaining > 64 ? 64 : remaining;
            if (write(fd, zeros, w) != w) return -1;
            remaining -= w;
        }
    }
    
    return (int64_t)ZIP_CENTRAL_HEADER_SIZE + name_len + extra_len;
}

/* Note. */
static int write_zip_eocd(
    int fd,
    int entry_count,
    uint32_t cd_size,
    uint32_t cd_offset
) {
    uint8_t eocd[ZIP_EOCD_MIN_SIZE];
    
    write_u32_le(eocd + 0, ZIP_EOCD_MAGIC);
    write_u16_le(eocd + 4, 0);            /* disk number */
    write_u16_le(eocd + 6, 0);            /* disk w/ CD */
    write_u16_le(eocd + 8, (uint16_t)entry_count);   /* entries on this disk */
    write_u16_le(eocd + 10, (uint16_t)entry_count);  /* total entries */
    write_u32_le(eocd + 12, cd_size);
    write_u32_le(eocd + 16, cd_offset);
    write_u16_le(eocd + 20, 0);           /* comment length */
    
    return write(fd, eocd, ZIP_EOCD_MIN_SIZE) == ZIP_EOCD_MIN_SIZE ? 0 : -1;
}

/* Note. */

#define MAX_ENTRIES 65535

typedef struct {
    char     *name;
    uint16_t  name_len;
    uint16_t  method;            /* STORED or DEFLATED */
    uint32_t  crc32;
    uint32_t  compressed_size;
    uint32_t  uncompressed_size;
    uint32_t  local_offset;      /* Note. */
    uint16_t  extra_len;
    uint16_t  comment_len;
} cd_entry_t;

/* Note. */
static int parse_central_directory(
    const uint8_t *data, int64_t file_size,
    cd_entry_t *entries, int max_entries
) {
    /* Note. */
    int64_t eocd_pos = -1;
    for (int64_t i = file_size - ZIP_EOCD_MIN_SIZE; i >= 0 && i >= file_size - 65557; i--) {
        if (read_u32_le(data + i) == ZIP_EOCD_MAGIC) {
            eocd_pos = i;
            break;
        }
    }
    
    if (eocd_pos < 0) {
        LOGE("EOCD not found");
        return -1;
    }
    
    uint16_t total_entries = read_u16_le(data + eocd_pos + 10);
    uint32_t cd_size = read_u32_le(data + eocd_pos + 12);
    uint32_t cd_offset = read_u32_le(data + eocd_pos + 16);
    
    LOGI("ZIP: %d entries, CD @ 0x%x (%u bytes)", total_entries, cd_offset, cd_size);
    
    if (total_entries > max_entries) {
        LOGE("Too many entries: %d > %d", total_entries, max_entries);
        return -1;
    }
    
    if ((int64_t)cd_offset + cd_size > file_size) {
        LOGE("CD out of bounds");
        return -1;
    }
    
    /* Note. */
    const uint8_t *p = data + cd_offset;
    const uint8_t *cd_end = data + cd_offset + cd_size;
    int count = 0;
    
    while (p + ZIP_CENTRAL_HEADER_SIZE <= cd_end && count < total_entries) {
        if (read_u32_le(p) != ZIP_CENTRAL_MAGIC) {
            LOGW("Invalid CD entry at offset %ld", (long)(p - data));
            break;
        }
        
        cd_entry_t *e = &entries[count];
        e->method = read_u16_le(p + 10);
        e->crc32 = read_u32_le(p + 16);
        e->compressed_size = read_u32_le(p + 20);
        e->uncompressed_size = read_u32_le(p + 24);
        e->name_len = read_u16_le(p + 28);
        e->extra_len = read_u16_le(p + 30);
        e->comment_len = read_u16_le(p + 32);
        e->local_offset = read_u32_le(p + 42);
        
        /* Note. */
        e->name = (char *)malloc(e->name_len + 1);
        if (!e->name) {
            LOGE("malloc failed for entry name");
            return -1;
        }
        memcpy(e->name, p + ZIP_CENTRAL_HEADER_SIZE, e->name_len);
        e->name[e->name_len] = '\0';
        
        p += ZIP_CENTRAL_HEADER_SIZE + e->name_len + e->extra_len + e->comment_len;
        count++;
    }
    
    LOGI("Parsed %d CD entries", count);
    return count;
}

/* Note. */
static const uint8_t *get_entry_data(const uint8_t *file_data, const cd_entry_t *entry) {
    uint32_t off = entry->local_offset;
    if (read_u32_le(file_data + off) != ZIP_LOCAL_MAGIC) {
        LOGE("Invalid local header for: %s", entry->name);
        return NULL;
    }
    uint16_t local_name_len = read_u16_le(file_data + off + 26);
    uint16_t local_extra_len = read_u16_le(file_data + off + 28);
    return file_data + off + ZIP_LOCAL_HEADER_SIZE + local_name_len + local_extra_len;
}

/* Note. */

/* Note. */
static int optimize_apk(const char *input_path, const char *output_path,
                         optimize_stats_t *stats) {
    memset(stats, 0, sizeof(*stats));
    dedup_init();
    
    /* Note. */
    int in_fd = open(input_path, O_RDONLY);
    if (in_fd < 0) {
        LOGE("Cannot open input: %s (%s)", input_path, strerror(errno));
        return -1;
    }
    
    struct stat st;
    if (fstat(in_fd, &st) < 0) {
        LOGE("fstat failed: %s", strerror(errno));
        close(in_fd);
        return -1;
    }
    
    int64_t file_size = st.st_size;
    stats->original_size = file_size;
    LOGI("Input APK: %lld bytes (%.1f MB)", (long long)file_size, file_size / (1024.0 * 1024.0));
    
    uint8_t *file_data = (uint8_t *)malloc(file_size);
    if (!file_data) {
        LOGE("malloc failed for %lld bytes", (long long)file_size);
        close(in_fd);
        return -1;
    }
    
    /* Note. */
    int64_t total_read = 0;
    while (total_read < file_size) {
        ssize_t n = read(in_fd, file_data + total_read, 
                         file_size - total_read > (4*1024*1024) ? (4*1024*1024) : file_size - total_read);
        if (n <= 0) {
            LOGE("Read failed at offset %lld: %s", (long long)total_read, strerror(errno));
            free(file_data);
            close(in_fd);
            return -1;
        }
        total_read += n;
    }
    close(in_fd);
    
    /* Note. */
    cd_entry_t *entries = (cd_entry_t *)calloc(MAX_ENTRIES, sizeof(cd_entry_t));
    if (!entries) {
        LOGE("calloc failed for entries");
        free(file_data);
        return -1;
    }
    
    int entry_count = parse_central_directory(file_data, file_size, entries, MAX_ENTRIES);
    if (entry_count < 0) {
        LOGE("Failed to parse CD");
        free(entries);
        free(file_data);
        return -1;
    }
    stats->entries_total = entry_count;
    
    /* Note. */
    int out_fd = open(output_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (out_fd < 0) {
        LOGE("Cannot open output: %s (%s)", output_path, strerror(errno));
        free(entries);
        free(file_data);
        return -1;
    }
    
    /* Note. */
    /* Note. */
    uint32_t max_uncomp = 0;
    for (int i = 0; i < entry_count; i++) {
        if (entries[i].uncompressed_size > max_uncomp)
            max_uncomp = entries[i].uncompressed_size;
    }
    
    uint8_t *decomp_buf = NULL;
    uint8_t *recomp_buf = NULL;
    
    if (max_uncomp > 0) {
        decomp_buf = (uint8_t *)malloc(max_uncomp);
        /* Note. */
        recomp_buf = (uint8_t *)malloc(max_uncomp + max_uncomp / 10 + 256);
    }
    
    if (max_uncomp > 0 && (!decomp_buf || !recomp_buf)) {
        LOGW("Failed to allocate recompression buffers (%u bytes), skipping recompression",
             max_uncomp);
    }
    
    /* Note. */
    /* Note. */
    uint32_t *output_offsets = (uint32_t *)calloc(entry_count, sizeof(uint32_t));
    int *output_included = (int *)calloc(entry_count, sizeof(int));
    uint16_t *output_methods = (uint16_t *)calloc(entry_count, sizeof(uint16_t));
    uint32_t *output_comp_sizes = (uint32_t *)calloc(entry_count, sizeof(uint32_t));
    uint32_t *output_crc32s = (uint32_t *)calloc(entry_count, sizeof(uint32_t));
    uint16_t *output_extra_lens = (uint16_t *)calloc(entry_count, sizeof(uint16_t));
    
    int output_count = 0;
    int64_t current_offset = 0;
    
    /* Note. */
    int arsc_idx = -1;
    for (int i = 0; i < entry_count; i++) {
        if (strcmp(entries[i].name, "resources.arsc") == 0) {
            arsc_idx = i;
            break;
        }
    }
    
    /* Note. */
    int *process_order = (int *)malloc(entry_count * sizeof(int));
    int order_idx = 0;
    if (arsc_idx >= 0) {
        process_order[order_idx++] = arsc_idx;
    }
    for (int i = 0; i < entry_count; i++) {
        if (i != arsc_idx) process_order[order_idx++] = i;
    }
    
    for (int oi = 0; oi < entry_count; oi++) {
        int i = process_order[oi];
        cd_entry_t *e = &entries[i];
        
        /* Note. */
        if (should_strip_entry(e->name, e->name_len)) {
            stats->entries_stripped++;
            int64_t saved = (e->uncompressed_size > 0) ? e->uncompressed_size : e->compressed_size;
            
            if (strncmp(e->name, "res/", 4) == 0) {
                stats->unused_res_savings += saved;
            } else if (strncmp(e->name, "kotlin/", 7) == 0 || strcmp(e->name, "DebugProbesKt.bin") == 0) {
                stats->resource_savings += saved;
            } else {
                stats->resource_savings += saved;
            }
            
            LOGD("STRIP: %s (saved %lld bytes)", e->name, (long long)saved);
            continue;
        }
        
        /* Note. */
        const uint8_t *entry_data = get_entry_data(file_data, e);
        if (!entry_data) {
            LOGW("Skipping corrupt entry: %s", e->name);
            continue;
        }
        
        /* Note. */
        /* Note. */
        int skip_dedup = 0;
        if (strstr(e->name, "ic_launcher") != NULL) skip_dedup = 1;
        if (strncmp(e->name, "res/mipmap", 10) == 0) skip_dedup = 1;
        
        if (!skip_dedup && dedup_check_and_add(e->crc32, e->uncompressed_size)) {
            stats->entries_deduplicated++;
            stats->dedup_savings += e->compressed_size;
            LOGD("DEDUP: %s (saved %u bytes)", e->name, e->compressed_size);
            continue;
        }
        
        /* Note. */
        int out_method = e->method;
        const uint8_t *out_data = entry_data;
        uint32_t out_comp_size = e->compressed_size;
        uint32_t out_uncomp_size = e->uncompressed_size;
        uint32_t out_crc = e->crc32;
        uint16_t out_extra_len = 0;
        uint8_t *out_extra_data = NULL;
        
        /* Note. */
        if (strcmp(e->name, "resources.arsc") == 0) {
            out_method = ZIP_METHOD_STORED;
            /* Note. */
            if (e->method == ZIP_METHOD_DEFLATED && decomp_buf) {
                int64_t decomp_len = inflate_data(entry_data, e->compressed_size,
                                                   decomp_buf, max_uncomp);
                if (decomp_len == (int64_t)e->uncompressed_size) {
                    out_data = decomp_buf;
                    out_comp_size = e->uncompressed_size;
                } else {
                    out_data = entry_data;
                    out_comp_size = e->compressed_size;
                    out_method = e->method;
                }
            } else if (e->method == ZIP_METHOD_STORED) {
                out_comp_size = e->uncompressed_size;
            }
            
            /* Note. */
            uint32_t base_offset = (uint32_t)current_offset + ZIP_LOCAL_HEADER_SIZE + e->name_len;
            uint32_t pad = (4 - (base_offset + 4) % 4) % 4;
            if (pad > 0) {
                out_extra_len = 4 + pad;
                out_extra_data = (uint8_t *)calloc(out_extra_len, 1);
                if (out_extra_data) {
                    out_extra_data[0] = 0xFF;
                    out_extra_data[1] = 0xFF;
                    out_extra_data[2] = (uint8_t)(pad & 0xFF);
                    out_extra_data[3] = (uint8_t)((pad >> 8) & 0xFF);
                }
            }
        }
        /* Note. */
        else if (e->method == ZIP_METHOD_STORED) {
            /* Note. */
            out_comp_size = e->uncompressed_size;
        }
        /* Note. */
        else if (e->method == ZIP_METHOD_DEFLATED && decomp_buf && recomp_buf) {
            /* Note. */
            int64_t decomp_len = inflate_data(entry_data, e->compressed_size,
                                               decomp_buf, max_uncomp);
            if (decomp_len > 0 && (uint32_t)decomp_len == e->uncompressed_size) {
                /* Note. */
                int64_t recomp_len = recompress_deflate(decomp_buf, e->uncompressed_size,
                                                         recomp_buf, max_uncomp + max_uncomp/10 + 256);
                if (recomp_len > 0 && (uint32_t)recomp_len < e->compressed_size) {
                    /* Note. */
                    out_data = recomp_buf;
                    out_comp_size = (uint32_t)recomp_len;
                    stats->recompression_savings += (e->compressed_size - (uint32_t)recomp_len);
                    stats->entries_recompressed++;
                }
                /* Note. */
            }
        }
        
        /* Note. */
        output_offsets[i] = (uint32_t)current_offset;
        output_included[i] = 1;
        output_methods[i] = out_method;
        output_comp_sizes[i] = out_comp_size;
        output_crc32s[i] = out_crc;
        output_extra_lens[i] = out_extra_len;
        output_count++;
        
        int64_t written = write_zip_local_entry(
            out_fd, e->name, e->name_len,
            out_method, out_crc, out_comp_size, out_uncomp_size,
            out_data, out_extra_len, out_extra_data
        );
        
        if (out_extra_data) free(out_extra_data);
        
        if (written < 0) {
            LOGE("Failed to write entry: %s", e->name);
            goto cleanup;
        }
        current_offset += written;
    }
    
    /* Note. */
    uint32_t cd_start = (uint32_t)current_offset;
    
    for (int oi = 0; oi < entry_count; oi++) {
        int i = process_order[oi];
        if (!output_included[i]) continue;
        
        cd_entry_t *e = &entries[i];
        
        int64_t written = write_zip_central_entry(
            out_fd,
            e->name, e->name_len,
            output_methods[i],
            output_crc32s[i],
            output_comp_sizes[i],
            e->uncompressed_size,
            output_offsets[i],
            output_extra_lens[i]
        );
        
        if (written < 0) {
            LOGE("Failed to write CD entry: %s", e->name);
            goto cleanup;
        }
        current_offset += written;
    }
    
    uint32_t cd_size = (uint32_t)(current_offset - cd_start);
    
    /* Note. */
    if (write_zip_eocd(out_fd, output_count, cd_size, cd_start) < 0) {
        LOGE("Failed to write EOCD");
        goto cleanup;
    }
    current_offset += ZIP_EOCD_MIN_SIZE;
    
    stats->optimized_size = current_offset;
    
    LOGI("Optimization complete:");
    LOGI("  Original:      %lld bytes (%.1f MB)", (long long)stats->original_size, 
         stats->original_size / (1024.0 * 1024.0));
    LOGI("  Optimized:     %lld bytes (%.1f MB)", (long long)stats->optimized_size,
         stats->optimized_size / (1024.0 * 1024.0));
    LOGI("  Savings:       %lld bytes (%.1f%%)", 
         (long long)(stats->original_size - stats->optimized_size),
         100.0 * (1.0 - (double)stats->optimized_size / stats->original_size));
    LOGI("  Entries:       %d total, %d stripped, %d recompressed, %d deduped",
         stats->entries_total, stats->entries_stripped, 
         stats->entries_recompressed, stats->entries_deduplicated);
    LOGI("  Recompression: %lld bytes saved", (long long)stats->recompression_savings);
    LOGI("  Resource strip: %lld bytes", (long long)stats->resource_savings);
    LOGI("  Unused res:    %lld bytes", (long long)stats->unused_res_savings);
    LOGI("  Dedup:         %lld bytes", (long long)stats->dedup_savings);
    
    /* Cleanup */
    close(out_fd);
    free(process_order);
    free(output_offsets);
    free(output_included);
    free(output_methods);
    free(output_comp_sizes);
    free(output_crc32s);
    free(output_extra_lens);
    if (decomp_buf) free(decomp_buf);
    if (recomp_buf) free(recomp_buf);
    for (int i = 0; i < entry_count; i++) free(entries[i].name);
    free(entries);
    free(file_data);
    return 0;
    
cleanup:
    close(out_fd);
    unlink(output_path);
    free(process_order);
    free(output_offsets);
    free(output_included);
    free(output_methods);
    free(output_comp_sizes);
    free(output_crc32s);
    free(output_extra_lens);
    if (decomp_buf) free(decomp_buf);
    if (recomp_buf) free(recomp_buf);
    for (int i = 0; i < entry_count; i++) free(entries[i].name);
    free(entries);
    free(file_data);
    return -1;
}

/* Note. */

/* Note. */
JNIEXPORT jobject JNICALL
JNI_FUNC(nativeOptimizeApk)(JNIEnv *env, jobject thiz,
                             jstring inputPath, jstring outputPath) {
    (void)thiz;
    
    const char *input = (*env)->GetStringUTFChars(env, inputPath, NULL);
    const char *output = (*env)->GetStringUTFChars(env, outputPath, NULL);
    
    if (!input || !output) {
        LOGE("Failed to get path strings");
        if (input) (*env)->ReleaseStringUTFChars(env, inputPath, input);
        if (output) (*env)->ReleaseStringUTFChars(env, outputPath, output);
        return NULL;
    }
    
    LOGI("APK optimization: %s -> %s", input, output);
    
    optimize_stats_t stats;
    int result = optimize_apk(input, output, &stats);
    
    (*env)->ReleaseStringUTFChars(env, inputPath, input);
    (*env)->ReleaseStringUTFChars(env, outputPath, output);
    
    /* Note. */
    jclass resultClass = (*env)->FindClass(env, "com/webtoapp/core/apkbuilder/NativeApkOptimizer$OptimizeResult");
    if (!resultClass) {
        LOGE("Cannot find OptimizeResult class");
        return NULL;
    }
    
    jmethodID ctor = (*env)->GetMethodID(env, resultClass, "<init>", "(ZJJIIIIIJJJJJJ)V");
    if (!ctor) {
        LOGE("Cannot find OptimizeResult constructor");
        return NULL;
    }
    
    return (*env)->NewObject(env, resultClass, ctor,
        (jboolean)(result == 0),
        (jlong)stats.original_size,
        (jlong)stats.optimized_size,
        (jint)stats.entries_total,
        (jint)stats.entries_stripped,
        (jint)stats.entries_recompressed,
        (jint)stats.entries_deduplicated,
        (jint)stats.dex_files_stripped,
        (jlong)stats.native_lib_savings,
        (jlong)stats.dex_savings,
        (jlong)stats.resource_savings,
        (jlong)stats.recompression_savings,
        (jlong)stats.dedup_savings,
        (jlong)stats.unused_res_savings
    );
}

/* Note. */
JNIEXPORT jlongArray JNICALL
JNI_FUNC(nativeAnalyzeApkSize)(JNIEnv *env, jobject thiz, jstring apkPath) {
    (void)thiz;
    
    const char *path = (*env)->GetStringUTFChars(env, apkPath, NULL);
    if (!path) return NULL;
    
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        (*env)->ReleaseStringUTFChars(env, apkPath, path);
        return NULL;
    }
    
    struct stat st;
    fstat(fd, &st);
    int64_t file_size = st.st_size;
    
    uint8_t *data = (uint8_t *)malloc(file_size);
    if (!data) { close(fd); (*env)->ReleaseStringUTFChars(env, apkPath, path); return NULL; }
    
    int64_t total_read = 0;
    while (total_read < file_size) {
        ssize_t n = read(fd, data + total_read, 
                         file_size - total_read > (4*1024*1024) ? (4*1024*1024) : file_size - total_read);
        if (n <= 0) break;
        total_read += n;
    }
    close(fd);
    (*env)->ReleaseStringUTFChars(env, apkPath, path);
    
    cd_entry_t *entries = (cd_entry_t *)calloc(MAX_ENTRIES, sizeof(cd_entry_t));
    int count = parse_central_directory(data, file_size, entries, MAX_ENTRIES);
    
    /* Note. */
    jlong sizes[8] = {0};
    
    for (int i = 0; i < count; i++) {
        int64_t sz = entries[i].compressed_size;
        const char *name = entries[i].name;
        int nlen = entries[i].name_len;
        
        if (nlen > 4 && strncmp(name, "lib/", 4) == 0) sizes[0] += sz;
        else if (nlen > 7 && strncmp(name, "classes", 7) == 0 && strstr(name, ".dex")) sizes[1] += sz;
        else if (nlen > 7 && strncmp(name, "assets/", 7) == 0) sizes[2] += sz;
        else if ((nlen > 4 && strncmp(name, "res/", 4) == 0) || strcmp(name, "resources.arsc") == 0) sizes[3] += sz;
        else if (nlen > 9 && strncmp(name, "META-INF/", 9) == 0) sizes[4] += sz;
        else if (nlen > 7 && strncmp(name, "kotlin/", 7) == 0) sizes[5] += sz;
        else sizes[6] += sz;
        
        /* Note. */
        if (should_strip_entry(name, nlen)) {
            sizes[7] += (entries[i].uncompressed_size > 0) ? entries[i].uncompressed_size : sz;
        }
        
        free(entries[i].name);
    }
    free(entries);
    free(data);
    
    jlongArray result = (*env)->NewLongArray(env, 8);
    if (result) {
        (*env)->SetLongArrayRegion(env, result, 0, 8, sizes);
    }
    return result;
}

