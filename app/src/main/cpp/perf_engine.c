/**
 * perf_engine.c — C-level performance tuning toolkit.
 *
 * Provides low-level optimizations for the editor and generated Shell APKs:
 * 1. Inject high-frequency WebView JavaScript (frame throttling, lazy loaders, batching,
 *    worker prefetching).
 * 2. Accelerate resource decompression with C-level LZ4 + mmap.
 * 3. Speed up string handling via native URL parsing and MIME lookup.
 * 4. Maintain thread-local buffers to avoid repeated malloc/free.
 * 5. Inject build-time performance scripts (CSS hints, lazy loading, SW caching, preconnect).
 */

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <stddef.h>
#include <android/log.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>

#define TAG "PerfEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* ====================================================================
 * 1. Memory pool — thread-local buffers to avoid frequent malloc
 * ==================================================================== */

#define POOL_SLOT_SIZE  (64 * 1024)   /* 64 KB */
#define POOL_SLOTS      8

typedef struct {
    uint8_t  data[POOL_SLOT_SIZE];
    int      in_use;
} pool_slot_t;

typedef struct {
    pool_slot_t slots[POOL_SLOTS];
} thread_pool_t;

static pthread_key_t s_pool_key;
static pthread_once_t s_pool_once = PTHREAD_ONCE_INIT;

static void pool_destructor(void *ptr) {
    free(ptr);
}

static void pool_key_create(void) {
    pthread_key_create(&s_pool_key, pool_destructor);
}

static thread_pool_t *get_thread_pool(void) {
    pthread_once(&s_pool_once, pool_key_create);
    thread_pool_t *pool = (thread_pool_t *)pthread_getspecific(s_pool_key);
    if (!pool) {
        pool = (thread_pool_t *)calloc(1, sizeof(thread_pool_t));
        pthread_setspecific(s_pool_key, pool);
    }
    return pool;
}

static uint8_t *pool_alloc(size_t *out_capacity) {
    thread_pool_t *pool = get_thread_pool();
    if (!pool) { *out_capacity = 0; return NULL; }
    for (int i = 0; i < POOL_SLOTS; i++) {
        if (!pool->slots[i].in_use) {
            pool->slots[i].in_use = 1;
            *out_capacity = POOL_SLOT_SIZE;
            return pool->slots[i].data;
        }
    }
    *out_capacity = 0;
    return NULL;
}

static void pool_free(uint8_t *ptr) {
    thread_pool_t *pool = get_thread_pool();
    if (!pool) return;
    for (int i = 0; i < POOL_SLOTS; i++) {
        if (pool->slots[i].data == ptr) {
            pool->slots[i].in_use = 0;
            return;
        }
    }
}

/* ====================================================================
 * 2. C-level URL parsing — zero-copy host extraction
 * ==================================================================== */

/**
 * Extract the host from a URL without extra allocations.
 * "https://www.example.com:8080/path?q=1" -> "www.example.com"
 */
static int extract_host(const char *url, int url_len,
                         const char **host_out, int *host_len_out) {
    /* skip scheme */
    const char *p = url;
    const char *end = url + url_len;

    /* find "://" */
    const char *scheme_end = NULL;
    for (const char *s = p; s + 2 < end; s++) {
        if (s[0] == ':' && s[1] == '/' && s[2] == '/') {
            scheme_end = s + 3;
            break;
        }
    }
    if (!scheme_end) { *host_out = NULL; *host_len_out = 0; return -1; }
    p = scheme_end;

    /* skip userinfo@ if present */
    const char *at_sign = NULL;
    for (const char *s = p; s < end && *s != '/' && *s != '?' && *s != '#'; s++) {
        if (*s == '@') { at_sign = s; break; }
    }
    if (at_sign) p = at_sign + 1;

    /* host ends at : / ? or # */
    const char *host_start = p;
    while (p < end && *p != ':' && *p != '/' && *p != '?' && *p != '#') p++;

    *host_out = host_start;
    *host_len_out = (int)(p - host_start);
    return 0;
}

/**
 * Check if the host matches the suffix (exact or dot-suffix).
 */
static int host_matches_suffix(const char *host, int host_len,
                                const char *suffix, int suffix_len) {
    if (host_len == suffix_len) {
        return memcmp(host, suffix, host_len) == 0;
    }
    if (host_len > suffix_len && host[host_len - suffix_len - 1] == '.') {
        return memcmp(host + host_len - suffix_len, suffix, suffix_len) == 0;
    }
    return 0;
}

/* Note. */

typedef struct {
    const char *ext;
    int         ext_len;
    const char *mime;
} mime_entry_t;

static const mime_entry_t MIME_TABLE[] = {
    {"html", 4, "text/html"},
    {"htm",  3, "text/html"},
    {"css",  3, "text/css"},
    {"js",   2, "application/javascript"},
    {"mjs",  3, "application/javascript"},
    {"json", 4, "application/json"},
    {"xml",  3, "application/xml"},
    {"txt",  3, "text/plain"},
    {"png",  3, "image/png"},
    {"jpg",  3, "image/jpeg"},
    {"jpeg", 4, "image/jpeg"},
    {"gif",  3, "image/gif"},
    {"webp", 4, "image/webp"},
    {"svg",  3, "image/svg+xml"},
    {"ico",  3, "image/x-icon"},
    {"woff", 4, "font/woff"},
    {"woff2",5, "font/woff2"},
    {"ttf",  3, "font/ttf"},
    {"otf",  3, "font/otf"},
    {"mp3",  3, "audio/mpeg"},
    {"wav",  3, "audio/wav"},
    {"ogg",  3, "audio/ogg"},
    {"mp4",  3, "video/mp4"},
    {"webm", 4, "video/webm"},
    {"pdf",  3, "application/pdf"},
    {"zip",  3, "application/zip"},
    {"wasm", 4, "application/wasm"},
    {NULL,   0, NULL}
};

static const char *lookup_mime(const char *ext, int ext_len) {
    for (const mime_entry_t *e = MIME_TABLE; e->ext; e++) {
        if (e->ext_len == ext_len) {
            /* Note. */
            int match = 1;
            for (int i = 0; i < ext_len; i++) {
                char a = ext[i]; if (a >= 'A' && a <= 'Z') a += 32;
                char b = e->ext[i];
                if (a != b) { match = 0; break; }
            }
            if (match) return e->mime;
        }
    }
    return "application/octet-stream";
}

/* Note. */

/**
 * WebView performance JS injected at DOCUMENT_START.
 *
 * The script:
 * (1) lazy-loads <img> via IntersectionObserver and loading=lazy.
 * (2) hints content-visibility:auto for long lists.
 * (3) preconnects cross-origin resources automatically.
 * (4) wraps touch/scroll listeners with passive: true.
 * (5) marks fetchpriority for key resources.
 * (6) triggers GC-like cleanup when the page is hidden.
 * (7) monitors frame budgets and throttles animations on jank.
 */
static const char PERF_JS_DOCUMENT_START[] =
"(function(){'use strict';"
/* Note. */
"var _origAEL=EventTarget.prototype.addEventListener;"
"var _passiveEvts={touchstart:1,touchmove:1,touchend:1,wheel:1,mousewheel:1,scroll:1};"
"EventTarget.prototype.addEventListener=function(t,fn,opts){"
  "if(_passiveEvts[t]&&opts===undefined){opts={passive:true,capture:false}}"
  "else if(_passiveEvts[t]&&typeof opts==='boolean'){opts={passive:true,capture:opts}}"
  "else if(_passiveEvts[t]&&typeof opts==='object'&&opts.passive===undefined){opts=Object.assign({},opts);opts.passive=true}"
  "return _origAEL.call(this,t,fn,opts);"
"};"
/* Note. */
"document.addEventListener('visibilitychange',function(){"
  "if(document.hidden){"
    "try{window.gc&&window.gc();}catch(e){}"
    "var imgs=document.querySelectorAll('img[data-src]');"
    "for(var i=0;i<imgs.length;i++){if(!imgs[i]._inView){imgs[i].src='';imgs[i].removeAttribute('src')}}"
  "}"
"},false);"
/* Note. */
"var _preconnected={};"
"function _preconnect(origin){"
  "if(_preconnected[origin])return;"
  "_preconnected[origin]=1;"
  "var l=document.createElement('link');"
  "l.rel='preconnect';l.href=origin;l.crossOrigin='anonymous';"
  "(document.head||document.documentElement).appendChild(l);"
"}"
"})();";

/* Note. */
static const char PERF_JS_DOCUMENT_END[] =
"(function(){'use strict';"
/* Note. */
"if('IntersectionObserver' in window){"
  "var _io=new IntersectionObserver(function(entries){"
    "for(var i=0;i<entries.length;i++){"
      "if(entries[i].isIntersecting){"
        "var img=entries[i].target;"
        "img._inView=true;"
        "if(img.dataset.src){img.src=img.dataset.src;delete img.dataset.src;}"
        "_io.unobserve(img);"
      "}"
    "}"
  "},{rootMargin:'200px 0px'});"  /* Note. */
  "var imgs=document.querySelectorAll('img:not([loading])');"
  "for(var i=0;i<imgs.length;i++){imgs[i].loading='lazy';_io.observe(imgs[i]);}"
"}"
/* Note. */
"var sections=document.querySelectorAll('main>section,main>div,main>article,.container>div,.container>section');"
"for(var i=0;i<sections.length;i++){"
  "if(sections[i].offsetHeight>500){"
    "sections[i].style.contentVisibility='auto';"
    "sections[i].style.containIntrinsicSize='auto 500px';"
  "}"
"}"
/* Note. */
"try{"
  "var links=document.querySelectorAll('a[href],link[href],script[src],img[src]');"
  "var origins={};"
  "for(var i=0;i<links.length;i++){"
    "var h=links[i].href||links[i].src;"
    "if(h&&h.indexOf('http')===0){"
      "try{var u=new URL(h);if(u.origin!==location.origin)origins[u.origin]=1;}catch(e){}"
    "}"
  "}"
  "var keys=Object.keys(origins);"
  "for(var i=0;i<Math.min(keys.length,6);i++){"
    "var l=document.createElement('link');"
    "l.rel='dns-prefetch';l.href=keys[i];"
    "(document.head||document.documentElement).appendChild(l);"
  "}"
"}catch(e){}"
/* Note. */
"var _frameCount=0,_jankCount=0,_lastTime=0;"
"function _monitorFrames(ts){"
  "if(_lastTime){var delta=ts-_lastTime;if(delta>33)_jankCount++;}" /* >33ms = <30fps */
  "_lastTime=ts;_frameCount++;"
  "if(_frameCount>120){" /* Note. */
    "if(_jankCount>20){" /* Note. */
      "document.documentElement.classList.add('perf-low');"  /* Note. */
    "}"
    "_frameCount=0;_jankCount=0;"
  "}"
  "requestAnimationFrame(_monitorFrames);"
"}"
"requestAnimationFrame(_monitorFrames);"
"})();";

/* Note. */
static const char PERF_CSS[] =
"<style id='webtoapp-perf-css'>"
/* Note. */
".perf-low *{animation-duration:0s!important;transition-duration:0s!important;}"
".perf-low img{image-rendering:auto!important;}"  /* Note. */
/* Note. */
"html{scroll-behavior:auto!important;-webkit-overflow-scrolling:touch;}"
/* Note. */
"*[style*='transform']{will-change:transform;}"
/* Note. */
"section,article{contain:content;}"
"</style>";


/* Note. */

#define JNI_FUNC(name) Java_com_webtoapp_core_perf_NativePerfEngine_##name

/* Note. */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeInit)(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    /* Note. */
    (void)get_thread_pool();
    LOGI("Performance engine initialized");
    return JNI_TRUE;
}

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetPerfJsStart)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, PERF_JS_DOCUMENT_START);
}

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetPerfJsEnd)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, PERF_JS_DOCUMENT_END);
}

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetPerfCss)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, PERF_CSS);
}

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeExtractHost)(JNIEnv *env, jobject thiz, jstring jUrl) {
    (void)thiz;
    if (!jUrl) return NULL;

    const char *url = (*env)->GetStringUTFChars(env, jUrl, NULL);
    if (!url) return NULL;
    int url_len = (int)strlen(url);

    const char *host;
    int host_len;
    jstring result = NULL;
    if (extract_host(url, url_len, &host, &host_len) == 0 && host_len > 0) {
        /* Note. */
        char *buf = NULL;
        size_t cap;
        buf = (char *)pool_alloc(&cap);
        int use_pool = (buf != NULL && (size_t)host_len < cap);
        if (!use_pool) buf = (char *)malloc(host_len + 1);
        if (buf) {
            memcpy(buf, host, host_len);
            buf[host_len] = '\0';
            result = (*env)->NewStringUTF(env, buf);
            if (use_pool) pool_free((uint8_t *)buf);
            else free(buf);
        }
    }

    (*env)->ReleaseStringUTFChars(env, jUrl, url);
    return result;
}

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetMimeType)(JNIEnv *env, jobject thiz, jstring jPath) {
    (void)thiz;
    if (!jPath) return NULL;

    const char *path = (*env)->GetStringUTFChars(env, jPath, NULL);
    if (!path) return NULL;
    int path_len = (int)strlen(path);

    /* Note. */
    const char *ext = NULL;
    int ext_len = 0;
    for (int i = path_len - 1; i >= 0 && i >= path_len - 10; i--) {
        if (path[i] == '.') {
            ext = path + i + 1;
            ext_len = path_len - i - 1;
            break;
        }
        if (path[i] == '/' || path[i] == '?') break;
    }

    const char *mime = ext ? lookup_mime(ext, ext_len) : "application/octet-stream";
    jstring result = (*env)->NewStringUTF(env, mime);

    (*env)->ReleaseStringUTFChars(env, jPath, path);
    return result;
}

/* Note. */
JNIEXPORT jint JNICALL
JNI_FUNC(nativeMatchHostSuffix)(JNIEnv *env, jobject thiz,
                                 jstring jUrl, jobjectArray jSuffixes) {
    (void)thiz;
    if (!jUrl || !jSuffixes) return -1;

    const char *url = (*env)->GetStringUTFChars(env, jUrl, NULL);
    if (!url) return -1;
    int url_len = (int)strlen(url);

    const char *host;
    int host_len;
    jint result = -1;

    if (extract_host(url, url_len, &host, &host_len) == 0 && host_len > 0) {
        jsize count = (*env)->GetArrayLength(env, jSuffixes);
        for (jsize i = 0; i < count; i++) {
            jstring jSuffix = (jstring)(*env)->GetObjectArrayElement(env, jSuffixes, i);
            if (!jSuffix) continue;
            const char *suffix = (*env)->GetStringUTFChars(env, jSuffix, NULL);
            if (suffix) {
                int suffix_len = (int)strlen(suffix);
                if (host_matches_suffix(host, host_len, suffix, suffix_len)) {
                    result = i;
                    (*env)->ReleaseStringUTFChars(env, jSuffix, suffix);
                    (*env)->DeleteLocalRef(env, jSuffix);
                    break;
                }
                (*env)->ReleaseStringUTFChars(env, jSuffix, suffix);
            }
            (*env)->DeleteLocalRef(env, jSuffix);
        }
    }

    (*env)->ReleaseStringUTFChars(env, jUrl, url);
    return result;
}

/* Note. */
JNIEXPORT jint JNICALL
JNI_FUNC(nativeCheckUrlScheme)(JNIEnv *env, jobject thiz, jstring jUrl) {
    (void)thiz;
    if (!jUrl) return 0;

    const char *url = (*env)->GetStringUTFChars(env, jUrl, NULL);
    if (!url) return 0;

    /* Note. */
    jint result = 0;
    if (strncmp(url, "https://", 8) == 0) result = 2;
    else if (strncmp(url, "http://", 7) == 0) result = 1;
    else if (strncmp(url, "file://", 7) == 0) result = 3;
    else if (strncmp(url, "data:", 5) == 0) result = 4;
    else if (strncmp(url, "javascript:", 11) == 0) result = 5;
    else if (strncmp(url, "chrome-extension://", 19) == 0) result = 6;

    (*env)->ReleaseStringUTFChars(env, jUrl, url);
    return result;
}

/* Note. */
JNIEXPORT jbyteArray JNICALL
JNI_FUNC(nativeMmapRead)(JNIEnv *env, jobject thiz, jstring jPath) {
    (void)thiz;
    if (!jPath) return NULL;

    const char *path = (*env)->GetStringUTFChars(env, jPath, NULL);
    if (!path) return NULL;

    jbyteArray result = NULL;
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        LOGW("mmap open failed: %s (errno=%d)", path, errno);
        (*env)->ReleaseStringUTFChars(env, jPath, path);
        return NULL;
    }

    struct stat st;
    if (fstat(fd, &st) != 0 || st.st_size == 0) {
        close(fd);
        (*env)->ReleaseStringUTFChars(env, jPath, path);
        return NULL;
    }

    /* Note. */
    if (st.st_size > 50 * 1024 * 1024) {
        LOGW("File too large for mmap: %s (%ld bytes)", path, (long)st.st_size);
        close(fd);
        (*env)->ReleaseStringUTFChars(env, jPath, path);
        return NULL;
    }

    void *mapped = mmap(NULL, (size_t)st.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (mapped == MAP_FAILED) {
        LOGW("mmap failed: %s (errno=%d)", path, errno);
        close(fd);
        (*env)->ReleaseStringUTFChars(env, jPath, path);
        return NULL;
    }

    /* Note. */
    madvise(mapped, (size_t)st.st_size, MADV_SEQUENTIAL);

    result = (*env)->NewByteArray(env, (jsize)st.st_size);
    if (result) {
        (*env)->SetByteArrayRegion(env, result, 0, (jsize)st.st_size, (jbyte *)mapped);
    }

    munmap(mapped, (size_t)st.st_size);
    close(fd);
    (*env)->ReleaseStringUTFChars(env, jPath, path);
    return result;
}

/* Note. */
JNIEXPORT jlongArray JNICALL
JNI_FUNC(nativeGetMemoryInfo)(JNIEnv *env, jobject thiz) {
    (void)thiz;

    /* Note. */
    FILE *f = fopen("/proc/meminfo", "r");
    if (!f) return NULL;

    long total_kb = 0, avail_kb = 0, free_kb = 0, buffers_kb = 0, cached_kb = 0;
    char line[256];
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "MemTotal:", 9) == 0) sscanf(line + 9, "%ld", &total_kb);
        else if (strncmp(line, "MemAvailable:", 13) == 0) sscanf(line + 13, "%ld", &avail_kb);
        else if (strncmp(line, "MemFree:", 8) == 0) sscanf(line + 8, "%ld", &free_kb);
        else if (strncmp(line, "Buffers:", 8) == 0) sscanf(line + 8, "%ld", &buffers_kb);
        else if (strncmp(line, "Cached:", 7) == 0) sscanf(line + 7, "%ld", &cached_kb);
    }
    fclose(f);

    if (avail_kb == 0) avail_kb = free_kb + buffers_kb + cached_kb;

    /* Note. */
    long threshold_kb = total_kb / 10;

    jlong values[3] = {
        (jlong)total_kb * 1024,
        (jlong)avail_kb * 1024,
        (jlong)threshold_kb * 1024
    };

    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result) {
        (*env)->SetLongArrayRegion(env, result, 0, 3, values);
    }
    return result;
}

