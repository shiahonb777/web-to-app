/**
 * C-level system tuning helpers for affinity, priority, OOM, I/O, memory hints,
 * thermal monitoring, TCP, scheduling, and descriptor limits.
 *
 * All actions use kernel interfaces where possible, run best-effort, and ignore
 * failures so that partial tuning never crashes the app.
 */

#define _GNU_SOURCE   /* expose cpu_set_t / CPU_ZERO / CPU_SET / sched_setaffinity */

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <sched.h>
#include <pthread.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/sysinfo.h>
#include <android/log.h>

#define TAG "SysOptimizer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* ====================================================================
 * 1. CPU core discovery
 * ==================================================================== */

#define MAX_CORES 16

typedef struct {
    int core_id;
    long max_freq_khz;   /*/sys/devices/system/cpu/cpuN/cpufreq/cpuinfo_max_freq */
    int is_big;          /* 1=big, 0=little */
} core_info_t;

typedef struct {
    int num_cores;
    int num_big;
    int num_little;
    core_info_t cores[MAX_CORES];
    long big_threshold;  /* big core frequency threshold */
} cpu_topology_t;

static cpu_topology_t s_cpu_topo;
static int s_topo_detected = 0;

/**
 * Detect CPU big.LITTLE topology by sampling each core's max frequency.
 */
static void detect_cpu_topology(void) {
    if (s_topo_detected) return;

    int num = sysconf(_SC_NPROCESSORS_CONF);
    if (num <= 0 || num > MAX_CORES) num = MAX_CORES;

    long max_freq = 0;
    long min_freq = LONG_MAX;

    for (int i = 0; i < num; i++) {
        char path[256];
        snprintf(path, sizeof(path),
                 "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i);

        s_cpu_topo.cores[i].core_id = i;
        s_cpu_topo.cores[i].max_freq_khz = 0;

        FILE *f = fopen(path, "r");
        if (f) {
            long freq = 0;
            if (fscanf(f, "%ld", &freq) == 1) {
                s_cpu_topo.cores[i].max_freq_khz = freq;
                if (freq > max_freq) max_freq = freq;
                if (freq < min_freq) min_freq = freq;
            }
            fclose(f);
        }
    }

    /* classify big cores when freq > midpoint of max/min */
    long threshold = (max_freq + min_freq) / 2;
    s_cpu_topo.big_threshold = threshold;
    s_cpu_topo.num_cores = num;
    s_cpu_topo.num_big = 0;
    s_cpu_topo.num_little = 0;

    for (int i = 0; i < num; i++) {
        if (s_cpu_topo.cores[i].max_freq_khz > threshold) {
            s_cpu_topo.cores[i].is_big = 1;
            s_cpu_topo.num_big++;
        } else {
            s_cpu_topo.cores[i].is_big = 0;
            s_cpu_topo.num_little++;
        }
    }

    LOGI("CPU topology: %d cores (%d big + %d little), threshold=%ld kHz",
         num, s_cpu_topo.num_big, s_cpu_topo.num_little, threshold);
    for (int i = 0; i < num; i++) {
        LOGD("  cpu%d: %ld kHz (%s)", i,
             s_cpu_topo.cores[i].max_freq_khz,
             s_cpu_topo.cores[i].is_big ? "BIG" : "LITTLE");
    }

    s_topo_detected = 1;
}

/**
 * Bind the current thread to the detected big cores.
 */
static int bind_to_big_cores(void) {
    detect_cpu_topology();

    if (s_cpu_topo.num_big == 0) {
        LOGW("No big cores detected, skipping CPU affinity");
        return -1;
    }

    cpu_set_t mask;
    CPU_ZERO(&mask);
    for (int i = 0; i < s_cpu_topo.num_cores; i++) {
        if (s_cpu_topo.cores[i].is_big) {
            CPU_SET(i, &mask);
        }
    }

    int ret = sched_setaffinity(0, sizeof(mask), &mask);
    if (ret == 0) {
        LOGI("Thread bound to big cores (%d cores)", s_cpu_topo.num_big);
    } else {
        LOGW("sched_setaffinity failed: errno=%d", errno);
    }
    return ret;
}

/* ====================================================================
 * 2. Thread priority tuning
 * ==================================================================== */

/**
 * Raise the current thread's priority within the Android range (-20..19).
 * Process.THREAD_PRIORITY_URGENT_DISPLAY = -8
 * Process.THREAD_PRIORITY_DISPLAY = -4
 */
static int boost_thread_priority(int tid, int nice_value) {
    int ret = setpriority(PRIO_PROCESS, tid, nice_value);
    if (ret == 0) {
        LOGD("Thread %d priority set to %d", tid, nice_value);
    } else {
        LOGW("setpriority(%d, %d) failed: errno=%d", tid, nice_value, errno);
    }
    return ret;
}

/* ====================================================================
 * 3. OOM score tuning
 * ==================================================================== */

/**
 * Lower the process's OOM adjustment by writing /proc/self/oom_score_adj (-1000..1000).
 * Lower values reduce the chance of being killed. Root or CAP_SYS_RESOURCE is required.
 */
static int adjust_oom_score(int score) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/oom_score_adj", getpid());

    int fd = open(path, O_WRONLY);
    if (fd < 0) {
        LOGW("Cannot open oom_score_adj (errno=%d, need root)", errno);
        return -1;
    }

    char buf[16];
    int len = snprintf(buf, sizeof(buf), "%d\n", score);
    int ret = (int)write(fd, buf, len);
    close(fd);

    if (ret > 0) {
        LOGI("OOM score adjusted to %d", score);
    } else {
        LOGW("Write oom_score_adj failed (errno=%d)", errno);
    }
    return ret > 0 ? 0 : -1;
}

/* ====================================================================
 * 4. I/O readahead tuning
 * ==================================================================== */

/**
 * Use posix_fadvise to request sequential + WILLNEED hints before reading.
 */
static int advise_file_readahead(const char *path) {
    int fd = open(path, O_RDONLY);
    if (fd < 0) return -1;

    struct stat st;
    if (fstat(fd, &st) == 0 && st.st_size > 0) {
        /* tell the kernel to prefetch this file */
        posix_fadvise(fd, 0, st.st_size, POSIX_FADV_SEQUENTIAL);
        posix_fadvise(fd, 0, st.st_size, POSIX_FADV_WILLNEED);
        LOGD("Readahead advised: %s (%ld bytes)", path, (long)st.st_size);
    }

    close(fd);
    return 0;
}

/* ====================================================================
 * 5. Thermal monitoring
 * ==================================================================== */

#define MAX_THERMAL_ZONES 20

typedef struct {
    int zone_id;
    int temp_milli_c;    /* temperature in millidegrees */
    char type[64];       /* thermal zone name */
} thermal_zone_t;

/**
 * Read all thermal zones and return the number of zones sampled.
 */
static int read_thermal_zones(thermal_zone_t *zones, int max_zones) {
    int count = 0;

    for (int i = 0; i < max_zones && i < MAX_THERMAL_ZONES; i++) {
        char temp_path[128];
        char type_path[128];
        snprintf(temp_path, sizeof(temp_path),
                 "/sys/class/thermal/thermal_zone%d/temp", i);
        snprintf(type_path, sizeof(type_path),
                 "/sys/class/thermal/thermal_zone%d/type", i);

        FILE *f = fopen(temp_path, "r");
        if (!f) break;

        int temp = 0;
        if (fscanf(f, "%d", &temp) == 1) {
            zones[count].zone_id = i;
            zones[count].temp_milli_c = temp;
            fclose(f);

            /* read the zone type */
            zones[count].type[0] = '\0';
            f = fopen(type_path, "r");
            if (f) {
                if (fgets(zones[count].type, sizeof(zones[count].type), f)) {
                    /* trim newline */
                    char *nl = strchr(zones[count].type, '\n');
                    if (nl) *nl = '\0';
                }
                fclose(f);
            }
            count++;
        } else {
            fclose(f);
        }
    }

    return count;
}

/* ====================================================================
 * 6. File descriptor limits
 * ==================================================================== */

/**
 * Raise the FD limit because WebView issues many concurrent requests.
 */
static int raise_fd_limit(void) {
    struct rlimit rl;
    if (getrlimit(RLIMIT_NOFILE, &rl) != 0) {
        LOGW("getrlimit(NOFILE) failed");
        return -1;
    }

    rlim_t old_soft = rl.rlim_cur;

    /* try to raise to the hard limit, capped at 65536 */
    rlim_t target = rl.rlim_max;
    if (target > 65536) target = 65536;

    if (rl.rlim_cur < target) {
        rl.rlim_cur = target;
        if (setrlimit(RLIMIT_NOFILE, &rl) == 0) {
            LOGI("FD limit raised: %lu -> %lu", (unsigned long)old_soft, (unsigned long)target);
            return 0;
        } else {
            LOGW("setrlimit(NOFILE, %lu) failed: errno=%d", (unsigned long)target, errno);
        }
    } else {
        LOGD("FD limit already at max: %lu", (unsigned long)rl.rlim_cur);
    }
    return -1;
}

/* ====================================================================
 * 7. Process scheduling tweaks
 * ==================================================================== */

/**
 * Set the process nice value; -5 boosts priority slightly (no root), -10 needs root.
 */
static int adjust_process_nice(int nice_val) {
    int ret = setpriority(PRIO_PROCESS, 0, nice_val);
    if (ret == 0) {
        LOGI("Process nice set to %d", nice_val);
    } else {
        LOGW("setpriority(nice=%d) failed: errno=%d", nice_val, errno);
    }
    return ret;
}

/* ====================================================================
 * 8. System profiling for adaptive tuning
 * ==================================================================== */

typedef struct {
    int   num_cores;
    int   num_big_cores;
    int   num_little_cores;
    long  total_ram_mb;
    long  free_ram_mb;
    int   max_cpu_freq_mhz;      /* top core frequency */
    int   max_thermal_temp_c;    /* hottest thermal zone (C) */
    int   fd_limit;              /* current FD limit */
    int   uptime_sec;            /* system uptime */
} system_profile_t;

static void build_system_profile(system_profile_t *prof) {
    memset(prof, 0, sizeof(*prof));

    detect_cpu_topology();
    prof->num_cores = s_cpu_topo.num_cores;
    prof->num_big_cores = s_cpu_topo.num_big;
    prof->num_little_cores = s_cpu_topo.num_little;

    /* top core frequency */
    long max_freq = 0;
    for (int i = 0; i < s_cpu_topo.num_cores; i++) {
        if (s_cpu_topo.cores[i].max_freq_khz > max_freq)
            max_freq = s_cpu_topo.cores[i].max_freq_khz;
    }
    prof->max_cpu_freq_mhz = (int)(max_freq / 1000);

    /* RAM usage */
    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        prof->total_ram_mb = (long)(si.totalram * si.mem_unit / (1024 * 1024));
        prof->free_ram_mb = (long)(si.freeram * si.mem_unit / (1024 * 1024));
        prof->uptime_sec = (int)si.uptime;
    }

    /* thermal range */
    thermal_zone_t zones[MAX_THERMAL_ZONES];
    int n = read_thermal_zones(zones, MAX_THERMAL_ZONES);
    int max_temp = 0;
    for (int i = 0; i < n; i++) {
        if (zones[i].temp_milli_c > max_temp) max_temp = zones[i].temp_milli_c;
    }
    prof->max_thermal_temp_c = max_temp / 1000;

    /* FD limit capture */
    struct rlimit rl;
    if (getrlimit(RLIMIT_NOFILE, &rl) == 0) {
        prof->fd_limit = (int)rl.rlim_cur;
    }
}

/* ====================================================================
 * 9. JNI bindings
 * ==================================================================== */

#define JNI_FUNC(name) Java_com_webtoapp_core_perf_NativeSysOptimizer_##name

/**
 * Run the system optimizer in priority order and return how many steps succeeded.
 */
JNIEXPORT jint JNICALL
JNI_FUNC(nativeOptimizeSystem)(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    int success_count = 0;

    LOGI("=== System-level optimization start ===");

    /* 1. detect CPU topology */
    detect_cpu_topology();
    success_count++;

    /* 2. raise FD limit */
    if (raise_fd_limit() == 0) success_count++;

    /* 3. boost process nice (-5) */
    if (adjust_process_nice(-5) == 0) success_count++;

    /* 4. boost current (UI) thread priority */
    if (boost_thread_priority(0, -8) == 0) success_count++;

    /* 5. lower OOM score (root only, safe to fail) */
    adjust_oom_score(-100);

    LOGI("=== System optimization done: %d items succeeded ===", success_count);
    return success_count;
}

/**
 * Bind the targeted thread to big cores, used by the WebView render thread.
 */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeBindToBigCores)(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    return bind_to_big_cores() == 0 ? JNI_TRUE : JNI_FALSE;
}

/**
 * Boost the specified thread priority.
 * @param tid  Thread ID (0 = current thread)
 * @param nice Nice level (-20 .. 19)
 */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeBoostThread)(JNIEnv *env, jobject thiz, jint tid, jint nice) {
    (void)env; (void)thiz;
    return boost_thread_priority(tid, nice) == 0 ? JNI_TRUE : JNI_FALSE;
}

/**
 * Prefetch the file into the page cache.
 * @param path File path
 */
JNIEXPORT void JNICALL
JNI_FUNC(nativeReadaheadFile)(JNIEnv *env, jobject thiz, jstring jPath) {
    (void)thiz;
    if (!jPath) return;
    const char *path = (*env)->GetStringUTFChars(env, jPath, NULL);
    if (path) {
        advise_file_readahead(path);
        (*env)->ReleaseStringUTFChars(env, jPath, path);
    }
}

/**
 * Return the highest CPU temperature (°C) for thermal throttling (>80°C).
 */
JNIEXPORT jint JNICALL
JNI_FUNC(nativeGetMaxThermalTemp)(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    thermal_zone_t zones[MAX_THERMAL_ZONES];
    int n = read_thermal_zones(zones, MAX_THERMAL_ZONES);
    int max_temp = 0;
    for (int i = 0; i < n; i++) {
        if (zones[i].temp_milli_c > max_temp) max_temp = zones[i].temp_milli_c;
    }
    return max_temp / 1000;  /* convert to degrees Celsius */
}

/**
 * Return a system profile int array: {numCores, numBig, numLittle, totalRamMb,
 * freeRamMb, maxCpuFreqMhz, maxThermalTempC, fdLimit, uptimeSec}.
 */
JNIEXPORT jintArray JNICALL
JNI_FUNC(nativeGetSystemProfile)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    system_profile_t prof;
    build_system_profile(&prof);

    jint values[9] = {
        prof.num_cores,
        prof.num_big_cores,
        prof.num_little_cores,
        (jint)prof.total_ram_mb,
        (jint)prof.free_ram_mb,
        prof.max_cpu_freq_mhz,
        prof.max_thermal_temp_c,
        prof.fd_limit,
        prof.uptime_sec
    };

    jintArray result = (*env)->NewIntArray(env, 9);
    if (result) {
        (*env)->SetIntArrayRegion(env, result, 0, 9, values);
    }
    return result;
}

/**
 * Return CPU topology details as "coreId:freqKhz:isBig,...".
 */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetCpuTopology)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    detect_cpu_topology();

    char buf[1024];
    int pos = 0;
    for (int i = 0; i < s_cpu_topo.num_cores && pos < (int)sizeof(buf) - 64; i++) {
        if (i > 0) buf[pos++] = ',';
        pos += snprintf(buf + pos, sizeof(buf) - pos, "%d:%ld:%d",
                        s_cpu_topo.cores[i].core_id,
                        s_cpu_topo.cores[i].max_freq_khz,
                        s_cpu_topo.cores[i].is_big);
    }
    buf[pos] = '\0';

    return (*env)->NewStringUTF(env, buf);
}

/**
 * Return all thermal zone info as "zoneId:tempMilliC:type,...".
 */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetThermalInfo)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    thermal_zone_t zones[MAX_THERMAL_ZONES];
    int n = read_thermal_zones(zones, MAX_THERMAL_ZONES);

    char buf[2048];
    int pos = 0;
    for (int i = 0; i < n && pos < (int)sizeof(buf) - 128; i++) {
        if (i > 0) buf[pos++] = ',';
        pos += snprintf(buf + pos, sizeof(buf) - pos, "%d:%d:%s",
                        zones[i].zone_id,
                        zones[i].temp_milli_c,
                        zones[i].type);
    }
    buf[pos] = '\0';

    return (*env)->NewStringUTF(env, buf);
}
