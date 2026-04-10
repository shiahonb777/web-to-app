/**
 * hardware_control.c — WebToApp 底层硬件控制原生库
 * 
 * 通过 Linux sysfs / ioctl / input 子系统实现底层硬件控制，
 * 绕过 Android Java API 的限制，在更多设备上生效。
 * 
 * 功能：
 * 1. 音量控制 — ALSA tinyalsa / sysfs
 * 2. 闪光灯/手电筒 — sysfs LED 子系统
 * 3. 震动马达 — sysfs / timed_output / ff-input
 * 4. 屏幕亮度 — sysfs backlight
 * 5. CPU 性能模式 — cpufreq governor
 * 6. 输入事件注入 — /dev/input/
 * 7. 进程优先级 — setpriority / ioprio
 * 
 * ⚠️ 警告：部分功能需要 root 或特殊权限，使用前会自动探测并降级。
 */

#define _GNU_SOURCE   /* expose cpu_set_t / CPU_ZERO / CPU_SET / sched_setaffinity */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <time.h>
#include <pthread.h>
#include <signal.h>
#include <math.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/resource.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <linux/input.h>

#include <android/log.h>

#define LOG_TAG "NativeHwCtrl"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// JNI 包名: com.webtoapp.core.forcedrun.NativeHardwareController
#define JNI_FUNC(name) Java_com_webtoapp_core_forcedrun_NativeHardwareController_##name

// ==================== 工具函数 ====================

/**
 * 尝试向 sysfs 节点写入字符串值
 * @return 0 成功, -1 失败
 */
static int sysfs_write(const char *path, const char *value) {
    int fd = open(path, O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        LOGD("sysfs_write: cannot open %s: %s", path, strerror(errno));
        return -1;
    }
    
    ssize_t len = strlen(value);
    ssize_t written = write(fd, value, len);
    close(fd);
    
    if (written != len) {
        LOGD("sysfs_write: partial write to %s (%zd/%zd): %s", 
             path, written, len, strerror(errno));
        return -1;
    }
    
    LOGD("sysfs_write: %s => '%s' OK", path, value);
    return 0;
}

/**
 * 从 sysfs 节点读取字符串值
 * @return 读取的字节数, -1 失败
 */
static int sysfs_read(const char *path, char *buf, size_t buf_len) {
    int fd = open(path, O_RDONLY | O_NONBLOCK);
    if (fd < 0) {
        return -1;
    }
    
    ssize_t n = read(fd, buf, buf_len - 1);
    close(fd);
    
    if (n < 0) return -1;
    
    buf[n] = '\0';
    // 去掉末尾换行
    while (n > 0 && (buf[n - 1] == '\n' || buf[n - 1] == '\r')) {
        buf[--n] = '\0';
    }
    
    return (int)n;
}

/**
 * 检查路径是否可写
 */
static int is_writable(const char *path) {
    return access(path, W_OK) == 0;
}

/**
 * 检查路径是否存在
 */
static int path_exists(const char *path) {
    return access(path, F_OK) == 0;
}

/**
 * 查找匹配模式的 sysfs 路径（从多个候选路径中选择第一个存在的）
 * @return 找到的路径索引, -1 表示未找到
 */
static int find_sysfs_path(const char **candidates, int count) {
    for (int i = 0; i < count; i++) {
        if (path_exists(candidates[i])) {
            return i;
        }
    }
    return -1;
}

// ==================== 闪光灯(LED/手电筒)控制 ====================

// 已知的闪光灯 sysfs 路径 (不同厂商/芯片)
static const char *flashlight_brightness_paths[] = {
    "/sys/class/leds/flashlight/brightness",
    "/sys/class/leds/torch-light0/brightness",
    "/sys/class/leds/torch-light/brightness",
    "/sys/class/leds/led:flash_torch/brightness",
    "/sys/class/leds/led:torch_0/brightness",
    "/sys/class/leds/led:torch_1/brightness",
    "/sys/class/leds/flashlight_0/brightness",
    "/sys/class/leds/flashlight_1/brightness",
    "/sys/class/leds/torch/brightness",
    "/sys/class/leds/spotlight/brightness",
    "/sys/class/leds/led_torch/brightness",
    // Qualcomm (Snapdragon 8 Gen 1/2/3, 7 series)
    "/sys/class/leds/led:switch_0/brightness",
    "/sys/class/leds/led:switch/brightness",
    "/sys/class/leds/led:flash_0/brightness",
    "/sys/class/leds/led:flash_1/brightness",
    "/sys/class/leds/led:torch_2/brightness",
    // MediaTek (Dimensity 700/800/900/1200/8100/9000/9200)
    "/sys/class/leds/mt6370_pmu_fled1/brightness",
    "/sys/class/leds/mt6370_pmu_fled2/brightness",
    "/sys/class/leds/mt6360_pmu_fled1/brightness",
    "/sys/class/leds/mt6360_pmu_fled2/brightness",
    "/sys/class/leds/mt6375_fled1/brightness",
    "/sys/class/leds/mt6375_fled2/brightness",
    "/sys/class/leds/aw36518_led0/brightness",
    "/sys/class/leds/aw36518_led1/brightness",
    // Samsung Exynos (S21/S22/S23/S24)
    "/sys/class/leds/flash-sec1/brightness",
    "/sys/class/leds/flash-sec2/brightness",
    "/sys/class/leds/s2mu106-flash-led/brightness",
    // UNISOC (Tiger T610/T618/T700/T770)
    "/sys/class/leds/flash_torch/brightness",
    "/sys/class/leds/sc2703-flash/brightness",
    // Kirin (Huawei/Honor)
    "/sys/class/leds/torch_flash/brightness",
    // Google Tensor (Pixel 6/7/8)
    "/sys/class/leds/flash-led0/brightness",
    "/sys/class/leds/flash-led1/brightness",
};
static const int flashlight_path_count = sizeof(flashlight_brightness_paths) / sizeof(flashlight_brightness_paths[0]);

static int cached_flash_path_idx = -2; // -2=未探测, -1=未找到, >=0=已找到
static char cached_flash_max_brightness[16] = "255";

// 动态探测发现的路径缓存
static char dynamic_flash_path[256] = {0};

/**
 * 探测可用的闪光灯 sysfs 路径
 * 
 * 策略：
 * 1. 优先匹配静态已知路径列表（快速）
 * 2. 失败时动态扫描 /sys/class/leds/ 目录，
 *    查找包含 "flash" / "torch" / "fled" 关键字的 LED 节点
 */
static int probe_flashlight_path(void) {
    if (cached_flash_path_idx != -2) {
        return cached_flash_path_idx;
    }
    
    cached_flash_path_idx = -1;
    
    // 策略1: 静态路径匹配
    for (int i = 0; i < flashlight_path_count; i++) {
        if (path_exists(flashlight_brightness_paths[i])) {
            char max_path[256];
            const char *p = strrchr(flashlight_brightness_paths[i], '/');
            if (p) {
                size_t dir_len = p - flashlight_brightness_paths[i];
                snprintf(max_path, sizeof(max_path), "%.*s/max_brightness", (int)dir_len, flashlight_brightness_paths[i]);
                
                char max_val[16];
                if (sysfs_read(max_path, max_val, sizeof(max_val)) > 0) {
                    strncpy(cached_flash_max_brightness, max_val, sizeof(cached_flash_max_brightness) - 1);
                }
            }
            
            cached_flash_path_idx = i;
            LOGI("Flashlight sysfs found (static): %s (max=%s)", 
                 flashlight_brightness_paths[i], cached_flash_max_brightness);
            return cached_flash_path_idx;
        }
    }
    
    // 策略2: 动态扫描 /sys/class/leds/ 目录
    DIR *dir = opendir("/sys/class/leds");
    if (dir) {
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            const char *name = entry->d_name;
            // 查找包含 flash/torch/fled 关键字的 LED 节点
            if (strstr(name, "flash") || strstr(name, "torch") || 
                strstr(name, "fled") || strstr(name, "Flash") || strstr(name, "Torch")) {
                
                snprintf(dynamic_flash_path, sizeof(dynamic_flash_path),
                         "/sys/class/leds/%s/brightness", name);
                
                if (path_exists(dynamic_flash_path)) {
                    // 读取 max_brightness
                    char max_path[256];
                    snprintf(max_path, sizeof(max_path),
                             "/sys/class/leds/%s/max_brightness", name);
                    char max_val[16];
                    if (sysfs_read(max_path, max_val, sizeof(max_val)) > 0) {
                        strncpy(cached_flash_max_brightness, max_val, sizeof(cached_flash_max_brightness) - 1);
                    }
                    
                    // 使用特殊索引 9999 表示动态发现
                    cached_flash_path_idx = 9999;
                    LOGI("Flashlight sysfs found (dynamic scan): %s (max=%s)", 
                         dynamic_flash_path, cached_flash_max_brightness);
                    closedir(dir);
                    return cached_flash_path_idx;
                }
            }
        }
        closedir(dir);
    }
    
    LOGW("No flashlight sysfs path found (static: %d paths + dynamic scan), falling back to Java API",
         flashlight_path_count);
    
    return cached_flash_path_idx;
}

/**
 * 获取闪光灯 sysfs 路径（支持静态列表 + 动态扫描）
 */
static const char *get_flash_path(int idx) {
    if (idx == 9999) return dynamic_flash_path; // 动态扫描发现
    if (idx >= 0 && idx < flashlight_path_count) return flashlight_brightness_paths[idx];
    return NULL;
}

/**
 * 原生闪光灯开关
 */
static int native_set_flashlight(int on) {
    int idx = probe_flashlight_path();
    if (idx < 0) return -1;
    
    const char *path = get_flash_path(idx);
    if (!path) return -1;
    
    const char *value = on ? cached_flash_max_brightness : "0";
    return sysfs_write(path, value);
}

// 爆闪线程
static volatile int strobe_running = 0;
static pthread_t strobe_thread;

static void *strobe_thread_func(void *arg) {
    int interval_us = *((int *)arg);
    free(arg);
    
    int idx = probe_flashlight_path();
    if (idx < 0) {
        LOGE("Strobe: no flashlight path");
        return NULL;
    }
    
    const char *path = get_flash_path(idx);
    if (!path) return NULL;
    int on = 0;
    
    while (strobe_running) {
        on = !on;
        sysfs_write(path, on ? cached_flash_max_brightness : "0");
        usleep(interval_us);
    }
    
    // 退出时关闭
    sysfs_write(path, "0");
    return NULL;

}

// ==================== 闪光灯模式引擎 (摩斯电码 / 自定义序列) ====================

/**
 * 闪光灯序列元素: (亮持续ms, 灭持续ms)
 * 例如摩斯电码 "." = (dot_ms, gap_ms), "-" = (dash_ms, gap_ms)
 */
typedef struct {
    int on_ms;
    int off_ms;
} flash_element_t;

// 模式播放线程
static volatile int pattern_running = 0;
static pthread_t pattern_thread;

// 模式数据 (由 JNI 传入)
static flash_element_t *pattern_data = NULL;
static int pattern_length = 0;
static int pattern_loop = 0; // 是否循环播放

static void *flash_pattern_thread_func(void *arg) {
    (void)arg;
    
    int idx = probe_flashlight_path();
    if (idx < 0) {
        LOGE("Pattern: no flashlight path");
        return NULL;
    }
    
    const char *path = get_flash_path(idx);
    if (!path) return NULL;
    
    do {
        for (int i = 0; i < pattern_length && pattern_running; i++) {
            // 亮
            if (pattern_data[i].on_ms > 0) {
                sysfs_write(path, cached_flash_max_brightness);
                usleep(pattern_data[i].on_ms * 1000);
            }
            
            if (!pattern_running) break;
            
            // 灭
            if (pattern_data[i].off_ms > 0) {
                sysfs_write(path, "0");
                usleep(pattern_data[i].off_ms * 1000);
            }
        }
    } while (pattern_running && pattern_loop);
    
    // 退出时确保关闭
    sysfs_write(path, "0");
    
    LOGI("Flash pattern playback finished");
    return NULL;
}

/**
 * 停止当前模式播放
 */
static void stop_pattern_playback(void) {
    if (pattern_running) {
        pattern_running = 0;
        pthread_join(pattern_thread, NULL);
    }
    if (pattern_data) {
        free(pattern_data);
        pattern_data = NULL;
    }
    pattern_length = 0;
}

// ========== 摩斯电码编码表 (ITU 标准) ==========

/**
 * 摩斯电码: "." = dit (短), "-" = dah (长)
 * ITU 标准时序比例:
 *   dit = 1 单位
 *   dah = 3 单位
 *   元素间隔 (dit/dah 之间) = 1 单位
 *   字符间隔 = 3 单位
 *   单词间隔 (空格) = 7 单位
 */
static const char *morse_table[128] = {
    // ASCII 0-47: 非字母数字
    [' '] = " ",  // 单词间隔 (特殊处理)
    ['!'] = "-.-.--",
    ['"'] = ".-..-.",
    ['#'] = NULL,
    ['$'] = "...-..-",
    ['%'] = NULL,
    ['&'] = ".-...",
    ['\''] = ".----.",
    ['('] = "-.--.",
    [')'] = "-.--.-",
    ['*'] = NULL,
    ['+'] = ".-.-.",
    [','] = "--..--",
    ['-'] = "-....-",
    ['.'] = ".-.-.-",
    ['/'] = "-..-.",

    // 数字 0-9
    ['0'] = "-----",
    ['1'] = ".----",
    ['2'] = "..---",
    ['3'] = "...--",
    ['4'] = "....-",
    ['5'] = ".....",
    ['6'] = "-....",
    ['7'] = "--...",
    ['8'] = "---..",
    ['9'] = "----.",

    // 特殊符号
    [':'] = "---...",
    [';'] = "-.-.-.",
    ['<'] = NULL,
    ['='] = "-...-",
    ['>'] = NULL,
    ['?'] = "..--..",
    ['@'] = ".--.-.",

    // 大写字母 A-Z
    ['A'] = ".-",
    ['B'] = "-...",
    ['C'] = "-.-.",
    ['D'] = "-..",
    ['E'] = ".",
    ['F'] = "..-.",
    ['G'] = "--.",
    ['H'] = "....",
    ['I'] = "..",
    ['J'] = ".---",
    ['K'] = "-.-",
    ['L'] = ".-..",
    ['M'] = "--",
    ['N'] = "-.",
    ['O'] = "---",
    ['P'] = ".--.",
    ['Q'] = "--.-",
    ['R'] = ".-.",
    ['S'] = "...",
    ['T'] = "-",
    ['U'] = "..-",
    ['V'] = "...-",
    ['W'] = ".--",
    ['X'] = "-..-",
    ['Y'] = "-.--",
    ['Z'] = "--..",
};

/**
 * 将文本转换为摩斯电码闪烁序列
 * 
 * @param text 输入文本 (ASCII)
 * @param unit_ms 基本时间单位 (毫秒), 推荐 100-300ms
 * @param out_elements 输出序列数组 (调用者释放)
 * @param out_count 输出序列长度
 * @return 0 成功, -1 失败
 */
static int encode_morse(const char *text, int unit_ms, 
                        flash_element_t **out_elements, int *out_count) {
    if (!text || !out_elements || !out_count) return -1;
    
    int text_len = strlen(text);
    if (text_len == 0) return -1;
    
    // 预分配足够大的数组 (每个字符最多 6 个元素 + 字符间隔)
    int max_elements = text_len * 8;
    flash_element_t *elements = (flash_element_t *)calloc(max_elements, sizeof(flash_element_t));
    if (!elements) return -1;
    
    int dit_ms = unit_ms;          // 短信号: 1 单位
    int dah_ms = unit_ms * 3;      // 长信号: 3 单位
    int element_gap = unit_ms;     // 元素间隔: 1 单位
    int char_gap = unit_ms * 3;    // 字符间隔: 3 单位
    int word_gap = unit_ms * 7;    // 单词间隔: 7 单位
    
    int count = 0;
    
    for (int i = 0; i < text_len && count < max_elements - 2; i++) {
        char ch = text[i];
        
        // 转为大写
        if (ch >= 'a' && ch <= 'z') ch = ch - 'a' + 'A';
        
        // 空格 = 单词间隔
        if (ch == ' ') {
            if (count > 0) {
                // 追加额外的间隔 (总共 7 单位, 已有 char_gap 3 单位)
                elements[count - 1].off_ms += (word_gap - char_gap);
            }
            continue;
        }
        
        // 查找摩斯电码
        if (ch < 0 || ch > 127) continue;
        const char *code = morse_table[(int)ch];
        if (!code) continue;
        
        int code_len = strlen(code);
        
        for (int j = 0; j < code_len && count < max_elements; j++) {
            int on_time = (code[j] == '.') ? dit_ms : dah_ms;
            int off_time = (j < code_len - 1) ? element_gap : char_gap;
            
            elements[count].on_ms = on_time;
            elements[count].off_ms = off_time;
            count++;
        }
    }
    
    if (count == 0) {
        free(elements);
        return -1;
    }
    
    *out_elements = elements;
    *out_count = count;
    
    LOGI("Morse encoded: '%s' -> %d elements (unit=%dms)", text, count, unit_ms);
    return 0;
}

// ===== 摩斯电码 / 自定义模式 JNI 函数 =====

/**
 * 播放摩斯电码
 * @param text 摩斯电码文本 (ASCII 字母、数字、空格、标点)
 * @param unitMs 基本时间单位 (ms)
 * @param loop 是否循环播放
 */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeStartMorseCode)(JNIEnv *env, jobject thiz, 
                                jstring text, jint unitMs, jboolean loop) {
    (void)thiz;
    
    // 停止之前的模式
    stop_pattern_playback();
    
    // 获取文本
    const char *c_text = (*env)->GetStringUTFChars(env, text, NULL);
    if (!c_text) return JNI_FALSE;
    
    // 编码
    flash_element_t *elements = NULL;
    int count = 0;
    int ret = encode_morse(c_text, unitMs, &elements, &count);
    
    (*env)->ReleaseStringUTFChars(env, text, c_text);
    
    if (ret != 0) return JNI_FALSE;
    
    // 设置模式数据
    pattern_data = elements;
    pattern_length = count;
    pattern_loop = loop ? 1 : 0;
    pattern_running = 1;
    
    // 启动播放线程
    if (pthread_create(&pattern_thread, NULL, flash_pattern_thread_func, NULL) != 0) {
        pattern_running = 0;
        free(pattern_data);
        pattern_data = NULL;
        return JNI_FALSE;
    }
    
    return JNI_TRUE;
}

/**
 * 播放自定义闪烁序列
 * @param onDurations 每个步骤的亮灯时长数组 (ms)
 * @param offDurations 每个步骤的灭灯时长数组 (ms)
 * @param count 序列长度
 * @param loop 是否循环
 */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeStartCustomPattern)(JNIEnv *env, jobject thiz,
                                    jintArray onDurations, jintArray offDurations,
                                    jint count, jboolean loop) {
    (void)thiz;
    
    stop_pattern_playback();
    
    if (count <= 0) return JNI_FALSE;
    
    jint *on_arr = (*env)->GetIntArrayElements(env, onDurations, NULL);
    jint *off_arr = (*env)->GetIntArrayElements(env, offDurations, NULL);
    
    if (!on_arr || !off_arr) {
        if (on_arr) (*env)->ReleaseIntArrayElements(env, onDurations, on_arr, 0);
        if (off_arr) (*env)->ReleaseIntArrayElements(env, offDurations, off_arr, 0);
        return JNI_FALSE;
    }
    
    flash_element_t *elements = (flash_element_t *)calloc(count, sizeof(flash_element_t));
    if (!elements) {
        (*env)->ReleaseIntArrayElements(env, onDurations, on_arr, 0);
        (*env)->ReleaseIntArrayElements(env, offDurations, off_arr, 0);
        return JNI_FALSE;
    }
    
    for (int i = 0; i < count; i++) {
        elements[i].on_ms = on_arr[i];
        elements[i].off_ms = off_arr[i];
    }
    
    (*env)->ReleaseIntArrayElements(env, onDurations, on_arr, 0);
    (*env)->ReleaseIntArrayElements(env, offDurations, off_arr, 0);
    
    pattern_data = elements;
    pattern_length = count;
    pattern_loop = loop ? 1 : 0;
    pattern_running = 1;
    
    if (pthread_create(&pattern_thread, NULL, flash_pattern_thread_func, NULL) != 0) {
        pattern_running = 0;
        free(pattern_data);
        pattern_data = NULL;
        return JNI_FALSE;
    }
    
    LOGI("Custom flash pattern started: %d steps, loop=%d", count, loop);
    return JNI_TRUE;
}

/**
 * 停止模式播放
 */
JNIEXPORT void JNICALL
JNI_FUNC(nativeStopPattern)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    stop_pattern_playback();
}

// ==================== 震动马达控制 ====================

static const char *vibrator_paths[] = {
    // timed_output (旧版)
    "/sys/class/timed_output/vibrator/enable",
    // leds 类型 (新版)
    "/sys/class/leds/vibrator/activate",
    "/sys/class/leds/vibrator/duration",
    // Qualcomm haptic
    "/sys/class/leds/vibrator/state",
    // input force-feedback 设备 (最新 Android 设备)
    "/dev/input/event0",
};
static const int vibrator_path_count = sizeof(vibrator_paths) / sizeof(vibrator_paths[0]);

static volatile int vibration_running = 0;
static pthread_t vibration_thread;

// 震动方式枚举
typedef enum {
    VIB_TIMED_OUTPUT = 0,   // /sys/class/timed_output/vibrator/enable
    VIB_LEDS_ACTIVATE = 1,  // /sys/class/leds/vibrator/ (duration + activate)
    VIB_FORCE_FEEDBACK = 2, // /dev/input/eventX  (force feedback)
    VIB_NONE = -1
} vibrator_type_t;

static vibrator_type_t cached_vib_type = VIB_NONE;
static char cached_vib_path[256] = {0};
static int cached_vib_probed = 0;

static void probe_vibrator(void) {
    if (cached_vib_probed) return;
    cached_vib_probed = 1;
    
    // 1. 检查 timed_output
    if (path_exists("/sys/class/timed_output/vibrator/enable")) {
        cached_vib_type = VIB_TIMED_OUTPUT;
        strncpy(cached_vib_path, "/sys/class/timed_output/vibrator/enable", sizeof(cached_vib_path) - 1);
        LOGI("Vibrator: timed_output");
        return;
    }
    
    // 2. 检查 leds 方式（标准路径）
    if (path_exists("/sys/class/leds/vibrator/duration") && 
        path_exists("/sys/class/leds/vibrator/activate")) {
        cached_vib_type = VIB_LEDS_ACTIVATE;
        strncpy(cached_vib_path, "/sys/class/leds/vibrator/", sizeof(cached_vib_path) - 1);
        LOGI("Vibrator: leds/activate");
        return;
    }
    
    // 2.1 检查替代 leds 路径（不同厂商命名）
    static const char *alt_vib_leds[] = {
        "/sys/class/leds/vibrator_aw8697/",   // Awinic haptic (OnePlus/OPPO/Realme)
        "/sys/class/leds/vibrator_cs40l25/",  // Cirrus Logic (Google Pixel 6/7)
        "/sys/class/leds/vibrator_cs40l26/",  // Cirrus Logic (Google Pixel 8)
        "/sys/class/leds/vibrator_aw86927/",  // Awinic (Xiaomi)
        "/sys/class/leds/aw8624_haptic/",     // Awinic older
        "/sys/class/leds/vibrator_tfa9xxx/",  // NXP (Samsung A-series)
    };
    for (int i = 0; i < (int)(sizeof(alt_vib_leds) / sizeof(alt_vib_leds[0])); i++) {
        char dur_path[300], act_path[300];
        snprintf(dur_path, sizeof(dur_path), "%sduration", alt_vib_leds[i]);
        snprintf(act_path, sizeof(act_path), "%sactivate", alt_vib_leds[i]);
        if (path_exists(dur_path) && path_exists(act_path)) {
            cached_vib_type = VIB_LEDS_ACTIVATE;
            strncpy(cached_vib_path, alt_vib_leds[i], sizeof(cached_vib_path) - 1);
            LOGI("Vibrator: alt leds at %s", alt_vib_leds[i]);
            return;
        }
    }
    
    // 2.2 动态扫描 /sys/class/leds/ 查找振动器节点
    DIR *leds_dir = opendir("/sys/class/leds");
    if (leds_dir) {
        struct dirent *led_entry;
        while ((led_entry = readdir(leds_dir)) != NULL) {
            if (strstr(led_entry->d_name, "vibrat") || strstr(led_entry->d_name, "haptic")) {
                char dur_path[300], act_path[300];
                snprintf(dur_path, sizeof(dur_path), "/sys/class/leds/%s/duration", led_entry->d_name);
                snprintf(act_path, sizeof(act_path), "/sys/class/leds/%s/activate", led_entry->d_name);
                if (path_exists(dur_path) && path_exists(act_path)) {
                    cached_vib_type = VIB_LEDS_ACTIVATE;
                    snprintf(cached_vib_path, sizeof(cached_vib_path), "/sys/class/leds/%s/", led_entry->d_name);
                    closedir(leds_dir);
                    LOGI("Vibrator: dynamic scan found %s", cached_vib_path);
                    return;
                }
            }
        }
        closedir(leds_dir);
    }
    
    // 3. 查找 force-feedback 输入设备
    DIR *dir = opendir("/dev/input");
    if (dir) {
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            if (strncmp(entry->d_name, "event", 5) != 0) continue;
            
            char dev_path[256];
            snprintf(dev_path, sizeof(dev_path), "/dev/input/%s", entry->d_name);
            
            int fd = open(dev_path, O_RDWR);
            if (fd < 0) continue;
            
            // 检查设备是否支持 force feedback
            unsigned long features[4] = {0};
            if (ioctl(fd, EVIOCGBIT(EV_FF, sizeof(features)), features) >= 0) {
                // 检查 FF_RUMBLE 位
                if (features[FF_RUMBLE / (sizeof(unsigned long) * 8)] & 
                    (1UL << (FF_RUMBLE % (sizeof(unsigned long) * 8)))) {
                    cached_vib_type = VIB_FORCE_FEEDBACK;
                    strncpy(cached_vib_path, dev_path, sizeof(cached_vib_path) - 1);
                    close(fd);
                    closedir(dir);
                    LOGI("Vibrator: force-feedback at %s", dev_path);
                    return;
                }
            }
            close(fd);
        }
        closedir(dir);
    }
    
    LOGW("No native vibrator path found");
}

static int native_vibrate_ms(int duration_ms) {
    probe_vibrator();
    
    char buf[32];
    
    switch (cached_vib_type) {
        case VIB_TIMED_OUTPUT:
            snprintf(buf, sizeof(buf), "%d", duration_ms);
            return sysfs_write(cached_vib_path, buf);
            
        case VIB_LEDS_ACTIVATE: {
            char dur_path[256], act_path[256];
            snprintf(dur_path, sizeof(dur_path), "%sduration", cached_vib_path);
            snprintf(act_path, sizeof(act_path), "%sactivate", cached_vib_path);
            snprintf(buf, sizeof(buf), "%d", duration_ms);
            if (sysfs_write(dur_path, buf) != 0) return -1;
            return sysfs_write(act_path, "1");
        }
        
        case VIB_FORCE_FEEDBACK: {
            int fd = open(cached_vib_path, O_RDWR);
            if (fd < 0) return -1;
            
            struct ff_effect effect;
            memset(&effect, 0, sizeof(effect));
            effect.type = FF_RUMBLE;
            effect.id = -1;
            effect.u.rumble.strong_magnitude = 0xFFFF;
            effect.u.rumble.weak_magnitude = 0xFFFF;
            effect.replay.length = duration_ms;
            effect.replay.delay = 0;
            
            if (ioctl(fd, EVIOCSFF, &effect) < 0) {
                close(fd);
                return -1;
            }
            
            struct input_event play;
            memset(&play, 0, sizeof(play));
            play.type = EV_FF;
            play.code = effect.id;
            play.value = 1;
            write(fd, &play, sizeof(play));
            
            // 等待震动完成后清理
            usleep(duration_ms * 1000);
            
            play.value = 0;
            write(fd, &play, sizeof(play));
            
            ioctl(fd, EVIOCRMFF, effect.id);
            close(fd);
            return 0;
        }
        
        default:
            return -1;
    }
}

static void *continuous_vibration_func(void *arg) {
    (void)arg;
    while (vibration_running) {
        if (native_vibrate_ms(1000) != 0) {
            // 如果原生震动失败，退出线程让 Java 层接管
            LOGW("Native vibration failed, thread exiting");
            break;
        }
        usleep(50000); // 50ms 间隔防止出现间断
    }
    return NULL;
}

// ==================== 屏幕亮度控制 ====================

static const char *brightness_paths[] = {
    "/sys/class/backlight/panel0-backlight/brightness",
    "/sys/class/backlight/panel/brightness",
    "/sys/class/backlight/panel1-backlight/brightness",
    "/sys/class/leds/lcd-backlight/brightness",
    "/sys/class/leds/wled/brightness",
    "/sys/class/backlight/sprd_backlight/brightness",
    // Samsung (S21/S22/S23/S24 AMOLED)
    "/sys/class/backlight/s6e3ha6-backlight/brightness",
    "/sys/class/backlight/s6e3ha8-backlight/brightness",
    "/sys/class/backlight/s6e3hab-backlight/brightness",
    "/sys/class/backlight/s6e3hac-backlight/brightness",
    "/sys/class/backlight/s6e3fc3-backlight/brightness",
    // Qualcomm
    "/sys/class/backlight/qcom-spmi-wled-1/brightness",
    "/sys/class/backlight/spmi-wled/brightness",
    // Google Tensor / Pixel
    "/sys/class/backlight/panel0-backlight-bl/brightness",
    // MediaTek
    "/sys/class/leds/lcd_backlight/brightness",
    "/sys/class/leds/mtkfb_backlight/brightness",
    // Kirin / HiSilicon
    "/sys/class/leds/hisi_backlight/brightness",
    // UNISOC
    "/sys/class/backlight/sprd_backlight_pwm/brightness",
    // OnePlus / OPPO
    "/sys/class/backlight/panel0-backlight-dcs/brightness",
    // Xiaomi
    "/sys/class/backlight/panel0-backlight-max/brightness",
};
static const int brightness_path_count = sizeof(brightness_paths) / sizeof(brightness_paths[0]);

static int cached_brightness_idx = -2;
static int cached_max_brightness = 255;
static char dynamic_brightness_path[256] = {0};

static int probe_brightness(void) {
    if (cached_brightness_idx != -2) return cached_brightness_idx;
    
    // 策略1: 静态路径匹配
    cached_brightness_idx = find_sysfs_path(brightness_paths, brightness_path_count);
    
    if (cached_brightness_idx >= 0) {
        char max_path[256];
        const char *p = strrchr(brightness_paths[cached_brightness_idx], '/');
        if (p) {
            size_t dir_len = p - brightness_paths[cached_brightness_idx];
            snprintf(max_path, sizeof(max_path), "%.*s/max_brightness", (int)dir_len, brightness_paths[cached_brightness_idx]);
            
            char max_val[16];
            if (sysfs_read(max_path, max_val, sizeof(max_val)) > 0) {
                cached_max_brightness = atoi(max_val);
                if (cached_max_brightness <= 0) cached_max_brightness = 255;
            }
        }
        LOGI("Brightness sysfs (static): %s (max=%d)", brightness_paths[cached_brightness_idx], cached_max_brightness);
        return cached_brightness_idx;
    }
    
    // 策略2: 动态扫描 /sys/class/backlight/
    DIR *dir = opendir("/sys/class/backlight");
    if (dir) {
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            if (entry->d_name[0] == '.') continue;
            
            snprintf(dynamic_brightness_path, sizeof(dynamic_brightness_path),
                     "/sys/class/backlight/%s/brightness", entry->d_name);
            
            if (path_exists(dynamic_brightness_path)) {
                // 读取 max_brightness
                char max_path[256];
                snprintf(max_path, sizeof(max_path),
                         "/sys/class/backlight/%s/max_brightness", entry->d_name);
                char max_val[16];
                if (sysfs_read(max_path, max_val, sizeof(max_val)) > 0) {
                    cached_max_brightness = atoi(max_val);
                    if (cached_max_brightness <= 0) cached_max_brightness = 255;
                }
                
                cached_brightness_idx = 9999; // 动态发现
                closedir(dir);
                LOGI("Brightness sysfs (dynamic scan): %s (max=%d)", 
                     dynamic_brightness_path, cached_max_brightness);
                return cached_brightness_idx;
            }
        }
        closedir(dir);
    }
    
    // 策略3: 动态扫描 /sys/class/leds/ 查找 backlight 节点
    dir = opendir("/sys/class/leds");
    if (dir) {
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            if (strstr(entry->d_name, "backlight") || strstr(entry->d_name, "lcd") ||
                strstr(entry->d_name, "wled")) {
                snprintf(dynamic_brightness_path, sizeof(dynamic_brightness_path),
                         "/sys/class/leds/%s/brightness", entry->d_name);
                
                if (path_exists(dynamic_brightness_path)) {
                    char max_path[256];
                    snprintf(max_path, sizeof(max_path),
                             "/sys/class/leds/%s/max_brightness", entry->d_name);
                    char max_val[16];
                    if (sysfs_read(max_path, max_val, sizeof(max_val)) > 0) {
                        cached_max_brightness = atoi(max_val);
                        if (cached_max_brightness <= 0) cached_max_brightness = 255;
                    }
                    
                    cached_brightness_idx = 9999;
                    closedir(dir);
                    LOGI("Brightness sysfs (leds scan): %s (max=%d)", 
                         dynamic_brightness_path, cached_max_brightness);
                    return cached_brightness_idx;
                }
            }
        }
        closedir(dir);
    }
    
    LOGW("No brightness sysfs path found (static: %d + dynamic scan)", brightness_path_count);
    return cached_brightness_idx;
}

static int native_set_brightness(int level) {
    int idx = probe_brightness();
    if (idx < 0) return -1;
    
    // 钳制到 [0, max]
    if (level < 0) level = 0;
    if (level > cached_max_brightness) level = cached_max_brightness;
    
    const char *path;
    if (idx == 9999) {
        path = dynamic_brightness_path; // 动态扫描发现
    } else if (idx >= 0 && idx < brightness_path_count) {
        path = brightness_paths[idx];
    } else {
        return -1;
    }
    
    char buf[16];
    snprintf(buf, sizeof(buf), "%d", level);
    return sysfs_write(path, buf);
}

// ==================== CPU 性能控制 ====================

#define MAX_CPU_CORES 16

static int get_cpu_count(void) {
    int count = 0;
    char path[128];
    for (int i = 0; i < MAX_CPU_CORES; i++) {
        snprintf(path, sizeof(path), "/sys/devices/system/cpu/cpu%d", i);
        if (path_exists(path)) {
            count++;
        } else {
            break;
        }
    }
    return count > 0 ? count : 1;
}

/**
 * 设置 CPU governor (performance / powersave / schedutil 等)
 */
static int native_set_cpu_governor(const char *governor) {
    int count = get_cpu_count();
    int success = 0;
    
    char path[256];
    for (int i = 0; i < count; i++) {
        // 确保 CPU 在线
        snprintf(path, sizeof(path), "/sys/devices/system/cpu/cpu%d/online", i);
        sysfs_write(path, "1");
        
        // 设置 governor
        snprintf(path, sizeof(path), "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_governor", i);
        if (sysfs_write(path, governor) == 0) {
            success++;
        }
        
        // 如果设置 performance 模式，同时设置最大频率
        if (strcmp(governor, "performance") == 0) {
            char max_freq[32] = {0};
            char max_path[256], cur_path[256];
            snprintf(max_path, sizeof(max_path), 
                     "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i);
            snprintf(cur_path, sizeof(cur_path), 
                     "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq", i);
            
            if (sysfs_read(max_path, max_freq, sizeof(max_freq)) > 0) {
                sysfs_write(cur_path, max_freq);
            }
        }
    }
    
    LOGI("CPU governor set to '%s': %d/%d cores", governor, success, count);
    return success > 0 ? 0 : -1;
}

// 高性能线程
static volatile int perf_mode_running = 0;
static pthread_t *perf_threads = NULL;
static int perf_thread_count = 0;

static void *cpu_burn_thread_func(void *arg) {
    int core_id = *((int *)arg);
    free(arg);
    
    // 尝试绑定到特定 CPU 核心 (提升缓存命中率)
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(core_id, &cpuset);
    sched_setaffinity(0, sizeof(cpuset), &cpuset);
    
    // 设置最高调度优先级
    setpriority(PRIO_PROCESS, 0, -20);
    
    // 分配内存块
    volatile double *mem_block = (volatile double *)mmap(
        NULL, 1024 * 1024, // 1MB
        PROT_READ | PROT_WRITE,
        MAP_PRIVATE | MAP_ANONYMOUS, -1, 0
    );
    
    volatile double result = 1.0;
    volatile long counter = 0;
    
    while (perf_mode_running) {
        // 1. 浮点密集计算 (FPU stress)
        for (int i = 0; i < 50000 && perf_mode_running; i++) {
            result += sin((double)i) * cos((double)i);
            result += sqrt(fabs(result));
            result *= 1.0000001;
        }
        
        // 2. 整数运算 (ALU stress)
        volatile int ival = (int)result;
        for (int i = 0; i < 100000 && perf_mode_running; i++) {
            ival ^= (i * 31 + 7);
            ival = (ival << 13) | ((unsigned int)ival >> 19); // rotate
            counter++;
        }
        
        // 3. 内存带宽压力
        if (mem_block) {
            for (int i = 0; i < 1024 * 1024 / (int)sizeof(double) && perf_mode_running; i += 8) {
                mem_block[i] = result + (double)i;
                result += mem_block[i] * 0.0000001;
            }
        }
        
        // 防止编译器优化
        if (result == 0.0 && counter == 0) {
            LOGD("perf: %f %ld", result, counter);
        }
    }
    
    if (mem_block) {
        munmap((void *)mem_block, 1024 * 1024);
    }
    
    return NULL;
}

// ==================== 输入事件注入 ====================

/**
 * 列举可用的 input 设备
 * 用于查找键盘/触摸屏设备
 */
typedef enum {
    INPUT_DEV_KEYBOARD = 0,
    INPUT_DEV_TOUCHSCREEN = 1,
    INPUT_DEV_POWER = 2
} input_dev_type_t;

static int find_input_device(input_dev_type_t type, char *out_path, size_t out_len) {
    DIR *dir = opendir("/dev/input");
    if (!dir) return -1;
    
    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strncmp(entry->d_name, "event", 5) != 0) continue;
        
        char dev_path[256];
        snprintf(dev_path, sizeof(dev_path), "/dev/input/%s", entry->d_name);
        
        int fd = open(dev_path, O_RDONLY);
        if (fd < 0) continue;
        
        unsigned long evbits[(EV_MAX + 1) / (sizeof(unsigned long) * 8) + 1] = {0};
        unsigned long keybits[(KEY_MAX + 1) / (sizeof(unsigned long) * 8) + 1] = {0};
        unsigned long absbits[(ABS_MAX + 1) / (sizeof(unsigned long) * 8) + 1] = {0};
        
        ioctl(fd, EVIOCGBIT(0, sizeof(evbits)), evbits);
        ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keybits)), keybits);
        ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(absbits)), absbits);
        
        int found = 0;
        switch (type) {
            case INPUT_DEV_TOUCHSCREEN:
                // 触摸屏支持 EV_ABS + ABS_MT_POSITION_X
                if ((absbits[ABS_MT_POSITION_X / (sizeof(unsigned long) * 8)] & 
                     (1UL << (ABS_MT_POSITION_X % (sizeof(unsigned long) * 8))))) {
                    found = 1;
                }
                break;
                
            case INPUT_DEV_POWER:
                // 电源键
                if ((keybits[KEY_POWER / (sizeof(unsigned long) * 8)] & 
                     (1UL << (KEY_POWER % (sizeof(unsigned long) * 8))))) {
                    found = 1;
                }
                break;
                
            case INPUT_DEV_KEYBOARD:
                // 音量键
                if ((keybits[KEY_VOLUMEUP / (sizeof(unsigned long) * 8)] & 
                     (1UL << (KEY_VOLUMEUP % (sizeof(unsigned long) * 8))))) {
                    found = 1;
                }
                break;
        }
        
        close(fd);
        
        if (found) {
            strncpy(out_path, dev_path, out_len - 1);
            out_path[out_len - 1] = '\0';
            closedir(dir);
            LOGI("Input device [type=%d]: %s", type, dev_path);
            return 0;
        }
    }
    
    closedir(dir);
    return -1;
}

/**
 * 向 input 设备注入事件
 */
static int inject_input_event(const char *dev_path, __u16 type, __u16 code, __s32 value) {
    int fd = open(dev_path, O_WRONLY);
    if (fd < 0) return -1;
    
    struct input_event ev;
    memset(&ev, 0, sizeof(ev));
    gettimeofday(&ev.time, NULL);
    ev.type = type;
    ev.code = code;
    ev.value = value;
    
    ssize_t n = write(fd, &ev, sizeof(ev));
    
    // SYN report
    memset(&ev, 0, sizeof(ev));
    gettimeofday(&ev.time, NULL);
    ev.type = EV_SYN;
    ev.code = SYN_REPORT;
    ev.value = 0;
    write(fd, &ev, sizeof(ev));
    
    close(fd);
    return n == sizeof(struct input_event) ? 0 : -1;
}

// ==================== 能力探测结果缓存 ====================

typedef struct {
    int flashlight_available;
    int vibrator_available;
    int brightness_available;
    int cpu_governor_available;
    int input_injection_available;
    int probed;
} hw_capabilities_t;

static hw_capabilities_t hw_caps = {0, 0, 0, 0, 0, 0};

static void probe_all_capabilities(void) {
    if (hw_caps.probed) return;
    
    // 闪光灯
    int f_idx = probe_flashlight_path();
    const char *flash_path = get_flash_path(f_idx);
    hw_caps.flashlight_available = (f_idx >= 0 && flash_path && is_writable(flash_path));
    
    // 震动
    probe_vibrator();
    if (cached_vib_type == VIB_TIMED_OUTPUT || cached_vib_type == VIB_LEDS_ACTIVATE) {
        hw_caps.vibrator_available = is_writable(cached_vib_path);
    } else if (cached_vib_type == VIB_FORCE_FEEDBACK) {
        hw_caps.vibrator_available = (access(cached_vib_path, R_OK | W_OK) == 0);
    }
    
    // 亮度
    int b_idx = probe_brightness();
    if (b_idx == 9999) {
        hw_caps.brightness_available = is_writable(dynamic_brightness_path);
    } else {
        hw_caps.brightness_available = (b_idx >= 0 && b_idx < brightness_path_count && is_writable(brightness_paths[b_idx]));
    }
    
    // CPU governor
    hw_caps.cpu_governor_available = is_writable(
        "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
    
    // 输入注入
    char vol_dev[256];
    hw_caps.input_injection_available = (find_input_device(INPUT_DEV_KEYBOARD, vol_dev, sizeof(vol_dev)) == 0
                                          && access(vol_dev, W_OK) == 0);
    
    hw_caps.probed = 1;
    
    LOGI("HW Capabilities: flash=%d vib=%d bright=%d cpu_gov=%d input_inj=%d",
         hw_caps.flashlight_available, hw_caps.vibrator_available,
         hw_caps.brightness_available, hw_caps.cpu_governor_available,
         hw_caps.input_injection_available);
}

// ==================== JNI 导出函数 ====================

/**
 * 探测硬件能力 — 返回位掩码
 * bit 0: 闪光灯 sysfs
 * bit 1: 震动器 sysfs  
 * bit 2: 屏幕亮度 sysfs
 * bit 3: CPU governor
 * bit 4: 输入事件注入
 */
JNIEXPORT jint JNICALL
JNI_FUNC(nativeProbeCapabilities)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    
    probe_all_capabilities();
    
    int flags = 0;
    if (hw_caps.flashlight_available)      flags |= (1 << 0);
    if (hw_caps.vibrator_available)        flags |= (1 << 1);
    if (hw_caps.brightness_available)      flags |= (1 << 2);
    if (hw_caps.cpu_governor_available)    flags |= (1 << 3);
    if (hw_caps.input_injection_available) flags |= (1 << 4);
    
    return flags;
}

// ===== 闪光灯 =====

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeSetFlashlight)(JNIEnv *env, jobject thiz, jboolean on) {
    (void)env;
    (void)thiz;
    return native_set_flashlight(on ? 1 : 0) == 0;
}

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeStartStrobe)(JNIEnv *env, jobject thiz, jint intervalMs) {
    (void)env;
    (void)thiz;
    
    if (strobe_running) {
        strobe_running = 0;
        pthread_join(strobe_thread, NULL);
    }
    
    strobe_running = 1;
    int *interval = (int *)malloc(sizeof(int));
    *interval = intervalMs * 1000; // ms -> us
    
    if (pthread_create(&strobe_thread, NULL, strobe_thread_func, interval) != 0) {
        strobe_running = 0;
        free(interval);
        return JNI_FALSE;
    }
    
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
JNI_FUNC(nativeStopStrobe)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    
    if (strobe_running) {
        strobe_running = 0;
        pthread_join(strobe_thread, NULL);
    }
}

// ===== 震动 =====

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeVibrate)(JNIEnv *env, jobject thiz, jint durationMs) {
    (void)env;
    (void)thiz;
    return native_vibrate_ms(durationMs) == 0;
}

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeStartContinuousVibration)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    
    if (vibration_running) {
        vibration_running = 0;
        pthread_join(vibration_thread, NULL);
    }
    
    vibration_running = 1;
    if (pthread_create(&vibration_thread, NULL, continuous_vibration_func, NULL) != 0) {
        vibration_running = 0;
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
JNI_FUNC(nativeStopVibration)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    if (vibration_running) {
        vibration_running = 0;
        pthread_join(vibration_thread, NULL);
    }
}

// ===== 屏幕亮度 =====

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeSetBrightness)(JNIEnv *env, jobject thiz, jint level) {
    (void)env;
    (void)thiz;
    return native_set_brightness(level) == 0;
}

JNIEXPORT jint JNICALL
JNI_FUNC(nativeGetMaxBrightness)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    probe_brightness();
    return cached_max_brightness;
}

// ===== CPU 性能 =====

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeSetCpuPerformanceMode)(JNIEnv *env, jobject thiz, jboolean enable) {
    (void)env;
    (void)thiz;
    return native_set_cpu_governor(enable ? "performance" : "schedutil") == 0;
}

JNIEXPORT void JNICALL
JNI_FUNC(nativeStartCpuBurn)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    
    if (perf_mode_running) return;
    perf_mode_running = 1;
    
    int count = get_cpu_count();
    perf_thread_count = count;
    perf_threads = (pthread_t *)calloc(count, sizeof(pthread_t));
    
    for (int i = 0; i < count; i++) {
        int *core = (int *)malloc(sizeof(int));
        *core = i;
        pthread_create(&perf_threads[i], NULL, cpu_burn_thread_func, core);
    }
    
    LOGI("CPU burn started: %d threads", count);
}

JNIEXPORT void JNICALL
JNI_FUNC(nativeStopCpuBurn)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    
    if (!perf_mode_running) return;
    perf_mode_running = 0;
    
    for (int i = 0; i < perf_thread_count; i++) {
        pthread_join(perf_threads[i], NULL);
    }
    
    free(perf_threads);
    perf_threads = NULL;
    perf_thread_count = 0;
    
    LOGI("CPU burn stopped");
}

// ===== 输入事件注入 =====

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeInjectVolumeKey)(JNIEnv *env, jobject thiz, jboolean volumeUp) {
    (void)env;
    (void)thiz;
    
    char dev_path[256];
    if (find_input_device(INPUT_DEV_KEYBOARD, dev_path, sizeof(dev_path)) != 0) {
        return JNI_FALSE;
    }
    
    __u16 key = volumeUp ? KEY_VOLUMEUP : KEY_VOLUMEDOWN;
    
    // key down
    if (inject_input_event(dev_path, EV_KEY, key, 1) != 0) return JNI_FALSE;
    usleep(50000); // 50ms
    // key up
    return inject_input_event(dev_path, EV_KEY, key, 0) == 0;
}

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeInjectPowerKey)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    
    char dev_path[256];
    if (find_input_device(INPUT_DEV_POWER, dev_path, sizeof(dev_path)) != 0) {
        return JNI_FALSE;
    }
    
    // key down
    if (inject_input_event(dev_path, EV_KEY, KEY_POWER, 1) != 0) return JNI_FALSE;
    usleep(50000);
    // key up
    return inject_input_event(dev_path, EV_KEY, KEY_POWER, 0) == 0;
}

// ===== 进程优先级 =====

JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeSetProcessPriority)(JNIEnv *env, jobject thiz, jint priority) {
    (void)env;
    (void)thiz;
    
    // priority: -20 (最高) 到 19 (最低)
    int clamped = priority;
    if (clamped < -20) clamped = -20;
    if (clamped > 19) clamped = 19;
    
    int ret = setpriority(PRIO_PROCESS, 0, clamped);
    if (ret == 0) {
        LOGI("Process priority set to %d", clamped);
    } else {
        LOGW("Failed to set priority %d: %s", clamped, strerror(errno));
    }
    return ret == 0;
}

/**
 * 设置 I/O 调度优先级 (Android/Linux)
 * class: 1=RT(实时), 2=BE(最佳努力), 3=IDLE
 * priority: 0-7 (RT/BE), 与 class 搭配
 */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeSetIoPriority)(JNIEnv *env, jobject thiz, jint ioClass, jint ioPriority) {
    (void)env;
    (void)thiz;
    
    // ioprio_set(IOPRIO_WHO_PROCESS, 0, IOPRIO_PRIO_VALUE(class, priority))
    #define IOPRIO_WHO_PROCESS 1
    #define IOPRIO_PRIO_VALUE(cls, data) (((cls) << 13) | (data))
    
    long ret = syscall(SYS_ioprio_set, IOPRIO_WHO_PROCESS, 0, 
                       IOPRIO_PRIO_VALUE(ioClass, ioPriority));
    
    if (ret == 0) {
        LOGI("I/O priority set: class=%d priority=%d", ioClass, ioPriority);
    } else {
        LOGW("Failed to set IO priority: %s", strerror(errno));
    }
    return ret == 0;
}

// ===== 清理 =====

JNIEXPORT void JNICALL
JNI_FUNC(nativeCleanup)(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    
    LOGI("Native cleanup started");
    
    // 停止爆闪
    if (strobe_running) {
        strobe_running = 0;
        pthread_join(strobe_thread, NULL);
    }
    
    // 停止模式播放 (摩斯电码 / 自定义序列)
    stop_pattern_playback();
    
    // 停止震动
    if (vibration_running) {
        vibration_running = 0;
        pthread_join(vibration_thread, NULL);
    }
    
    // 停止 CPU burn
    if (perf_mode_running) {
        perf_mode_running = 0;
        for (int i = 0; i < perf_thread_count; i++) {
            pthread_join(perf_threads[i], NULL);
        }
        free(perf_threads);
        perf_threads = NULL;
        perf_thread_count = 0;
    }
    
    // 恢复闪光灯
    native_set_flashlight(0);
    
    LOGI("Native cleanup completed");
}
