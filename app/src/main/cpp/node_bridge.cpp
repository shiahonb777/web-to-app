/**
 * Node.js JNI Bridge for WebToApp
 *
 * 通过 JNI 调用 nodejs-mobile 的 libnode.so 共享库。
 * libnode.so 不是独立可执行文件，必须通过 dlopen + node::Start() 在进程内启动。
 *
 * 工作原理：
 * 1. Kotlin 层调用 startNode(args) — 在后台线程中
 * 2. JNI 层 dlopen libnode.so，查找 node::Start 符号
 * 3. 调用 node::Start(argc, argv) — 此调用会阻塞直到 Node.js 事件循环退出
 * 4. 返回 exit code 给 Kotlin 层
 *
 * 注意：node::Start() 只能调用一次（nodejs-mobile 限制），重启需要重启整个进程。
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstring>
#include <cstdlib>
#include <dlfcn.h>
#include <unistd.h>
#include <sys/stat.h>
#include <cerrno>
#include <csignal>
#include <csetjmp>

#define LOG_TAG "NodeBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// node::Start 函数签名
typedef int (*node_start_func)(int argc, char *argv[]);

// 全局状态
static void *g_node_handle = nullptr;
static node_start_func g_node_start = nullptr;
static bool g_node_started = false;
static int g_node_exit_code = -1;

// 重定向 stdout/stderr 到 Android logcat 的管道
static int g_stdout_pipe[2] = {-1, -1};
static int g_stderr_pipe[2] = {-1, -1};
static int g_original_stdout = -1;
static int g_original_stderr = -1;

// SIGABRT 恢复点 — 用于捕获 Node.js 的 abort() 调用
static sigjmp_buf g_abort_jmpbuf;
static volatile bool g_abort_handler_active = false;
static struct sigaction g_old_sigabrt_action;

// JVM 引用（用于回调）
static JavaVM *g_jvm = nullptr;
static jobject g_callback_ref = nullptr;

/**
 * SIGABRT 信号处理器 — 捕获 Node.js 的 abort() 调用，跳回恢复点而非崩溃
 */
static void sigabrt_handler(int sig) {
    if (g_abort_handler_active) {
        LOGW("Caught SIGABRT from Node.js — recovering gracefully");
        g_abort_handler_active = false;
        // 恢复原始信号处理器
        sigaction(SIGABRT, &g_old_sigabrt_action, nullptr);
        // 跳回恢复点
        siglongjmp(g_abort_jmpbuf, 1);
    } else {
        // 不在保护区域内，执行默认行为
        sigaction(SIGABRT, &g_old_sigabrt_action, nullptr);
        raise(SIGABRT);
    }
}

/**
 * 安装 SIGABRT 处理器
 */
static void install_abort_handler() {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = sigabrt_handler;
    sa.sa_flags = 0; // 不设置 SA_RESTART，让阻塞调用中断
    sigemptyset(&sa.sa_mask);
    sigaction(SIGABRT, &sa, &g_old_sigabrt_action);

    // 忽略 SIGPIPE（网络连接断开时不崩溃）
    signal(SIGPIPE, SIG_IGN);
}

/**
 * 卸载 SIGABRT 处理器，恢复原始行为
 */
static void uninstall_abort_handler() {
    g_abort_handler_active = false;
    sigaction(SIGABRT, &g_old_sigabrt_action, nullptr);
}

/**
 * 日志读取线程 — 从管道读取 Node.js 的 stdout/stderr 并转发到 logcat + Java 回调
 */
static void *log_reader_thread(void *arg) {
    int fd = *((int *)arg);
    const char *tag = (fd == g_stdout_pipe[0]) ? "NodeJS" : "NodeJS-err";
    int level = (fd == g_stdout_pipe[0]) ? ANDROID_LOG_INFO : ANDROID_LOG_WARN;

    char buf[4096];
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf) - 1)) > 0) {
        buf[n] = '\0';
        // 按行分割输出
        char *line = buf;
        char *nl;
        while ((nl = strchr(line, '\n')) != nullptr) {
            *nl = '\0';
            if (strlen(line) > 0) {
                __android_log_print(level, tag, "%s", line);

                // 回调到 Java 层
                if (g_jvm && g_callback_ref) {
                    JNIEnv *env = nullptr;
                    bool attached = false;
                    int status = g_jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
                    if (status == JNI_EDETACHED) {
                        if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
                            attached = true;
                        }
                    }
                    if (env) {
                        jclass cls = env->GetObjectClass(g_callback_ref);
                        jmethodID mid = env->GetMethodID(cls, "onOutput", "(Ljava/lang/String;Z)V");
                        if (mid) {
                            jstring jline = env->NewStringUTF(line);
                            jboolean isErr = (fd == g_stderr_pipe[0]) ? JNI_TRUE : JNI_FALSE;
                            env->CallVoidMethod(g_callback_ref, mid, jline, isErr);
                            env->DeleteLocalRef(jline);
                        }
                        env->DeleteLocalRef(cls);
                        if (attached) {
                            g_jvm->DetachCurrentThread();
                        }
                    }
                }
            }
            line = nl + 1;
        }
        // 处理没有换行符的剩余部分
        if (strlen(line) > 0) {
            __android_log_print(level, tag, "%s", line);
        }
    }
    return nullptr;
}

/**
 * 设置 stdout/stderr 重定向到管道
 */
static bool setup_output_redirect() {
    if (pipe(g_stdout_pipe) != 0 || pipe(g_stderr_pipe) != 0) {
        LOGE("Failed to create pipes: %s", strerror(errno));
        return false;
    }

    // 保存原始 fd
    g_original_stdout = dup(STDOUT_FILENO);
    g_original_stderr = dup(STDERR_FILENO);

    // 重定向 stdout/stderr 到管道写端
    dup2(g_stdout_pipe[1], STDOUT_FILENO);
    dup2(g_stderr_pipe[1], STDERR_FILENO);
    close(g_stdout_pipe[1]);
    close(g_stderr_pipe[1]);

    // 启动日志读取线程
    pthread_t stdout_thread, stderr_thread;
    pthread_create(&stdout_thread, nullptr, log_reader_thread, &g_stdout_pipe[0]);
    pthread_create(&stderr_thread, nullptr, log_reader_thread, &g_stderr_pipe[0]);
    pthread_detach(stdout_thread);
    pthread_detach(stderr_thread);

    return true;
}

/**
 * 恢复 stdout/stderr
 */
static void restore_output() {
    if (g_original_stdout >= 0) {
        dup2(g_original_stdout, STDOUT_FILENO);
        close(g_original_stdout);
        g_original_stdout = -1;
    }
    if (g_original_stderr >= 0) {
        dup2(g_original_stderr, STDERR_FILENO);
        close(g_original_stderr);
        g_original_stderr = -1;
    }
    if (g_stdout_pipe[0] >= 0) { close(g_stdout_pipe[0]); g_stdout_pipe[0] = -1; }
    if (g_stderr_pipe[0] >= 0) { close(g_stderr_pipe[0]); g_stderr_pipe[0] = -1; }
}

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

/**
 * 加载 libnode.so 共享库
 *
 * @param nodePath libnode.so 的完整路径（nativeLibraryDir 或下载缓存）
 * @return true 如果加载成功
 */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_nodejs_NodeBridge_nativeLoadNode(
        JNIEnv *env,
        jclass clazz,
        jstring nodePath) {

    if (g_node_handle) {
        LOGI("libnode.so already loaded");
        return JNI_TRUE;
    }

    const char *path = env->GetStringUTFChars(nodePath, nullptr);
    if (!path) {
        LOGE("Failed to get node path string");
        return JNI_FALSE;
    }

    LOGI("Loading libnode.so from: %s", path);

    // 检查文件是否存在
    struct stat st;
    if (stat(path, &st) != 0) {
        LOGE("libnode.so not found: %s (errno=%d: %s)", path, errno, strerror(errno));
        env->ReleaseStringUTFChars(nodePath, path);
        return JNI_FALSE;
    }
    LOGI("libnode.so size: %lld bytes", (long long)st.st_size);

    // dlopen 加载共享库
    g_node_handle = dlopen(path, RTLD_LAZY);
    env->ReleaseStringUTFChars(nodePath, path);

    if (!g_node_handle) {
        LOGE("dlopen failed: %s", dlerror());
        return JNI_FALSE;
    }

    // 查找 node::Start 符号
    // nodejs-mobile 导出的符号名为 _ZN4node5StartEiPPc (mangled C++ name)
    // 也可能导出 C 风格的 node_start
    g_node_start = (node_start_func)dlsym(g_node_handle, "node_start");
    if (!g_node_start) {
        // 尝试 C++ mangled name: node::Start(int, char**)
        g_node_start = (node_start_func)dlsym(g_node_handle, "_ZN4node5StartEiPPc");
    }
    if (!g_node_start) {
        LOGE("Failed to find node::Start symbol: %s", dlerror());
        dlclose(g_node_handle);
        g_node_handle = nullptr;
        return JNI_FALSE;
    }

    LOGI("libnode.so loaded successfully, node::Start found");
    return JNI_TRUE;
}

/**
 * 启动 Node.js 引擎（阻塞调用，必须在后台线程中调用）
 *
 * @param arguments Node.js 参数数组，如 ["node", "/path/to/script.js"]
 * @param callback  输出回调对象（实现 onOutput(String, boolean) 方法）
 * @return Node.js 退出码
 */
JNIEXPORT jint JNICALL
Java_com_webtoapp_core_nodejs_NodeBridge_nativeStartNode(
        JNIEnv *env,
        jclass clazz,
        jobjectArray arguments,
        jobject callback) {

    if (!g_node_start) {
        LOGE("node::Start not loaded, call loadNode() first");
        return -1;
    }

    if (g_node_started) {
        LOGW("Node.js already started (can only start once per process)");
        return -2;
    }

    g_node_started = true;

    // 保存回调引用
    if (callback) {
        g_callback_ref = env->NewGlobalRef(callback);
    }

    // 设置 stdout/stderr 重定向
    setup_output_redirect();

    // 转换 Java String[] 到 C char*[]
    jsize argc = env->GetArrayLength(arguments);

    // node::Start 的 libuv 要求所有参数在连续内存中
    int total_size = 0;
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)env->GetObjectArrayElement(arguments, i);
        const char *str = env->GetStringUTFChars(arg, nullptr);
        total_size += strlen(str) + 1;
        env->ReleaseStringUTFChars(arg, str);
        env->DeleteLocalRef(arg);
    }

    char *args_buffer = (char *)calloc(total_size, sizeof(char));
    char **argv = (char **)malloc(argc * sizeof(char *));
    char *current = args_buffer;

    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)env->GetObjectArrayElement(arguments, i);
        const char *str = env->GetStringUTFChars(arg, nullptr);
        size_t len = strlen(str);
        strncpy(current, str, len);
        argv[i] = current;
        current += len + 1;
        env->ReleaseStringUTFChars(arg, str);
        env->DeleteLocalRef(arg);
    }

    LOGI("Starting Node.js with %d args:", argc);
    for (int i = 0; i < argc; i++) {
        LOGI("  argv[%d] = %s", i, argv[i]);
    }

    // 安装 SIGABRT 处理器，防止 Node.js 的 abort() 崩溃整个应用
    install_abort_handler();

    int result;
    // 设置恢复点 — 如果 Node.js 调用 abort()，会跳回这里
    if (sigsetjmp(g_abort_jmpbuf, 1) == 0) {
        // 正常路径：调用 node::Start — 这会阻塞直到 Node.js 事件循环退出
        g_abort_handler_active = true;
        result = g_node_start(argc, argv);
        g_abort_handler_active = false;
        LOGI("Node.js exited with code: %d", result);
    } else {
        // SIGABRT 恢复路径：Node.js 调用了 abort()
        result = -99;
        LOGE("Node.js aborted (SIGABRT caught). Returning error code %d instead of crashing.", result);
    }

    // 卸载信号处理器
    uninstall_abort_handler();
    g_node_exit_code = result;

    // 清理
    free(argv);
    free(args_buffer);
    restore_output();

    if (g_callback_ref) {
        env->DeleteGlobalRef(g_callback_ref);
        g_callback_ref = nullptr;
    }

    return result;
}

/**
 * 检查 Node.js 是否已经启动过
 */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_nodejs_NodeBridge_nativeIsStarted(
        JNIEnv *env,
        jclass clazz) {
    return g_node_started ? JNI_TRUE : JNI_FALSE;
}

/**
 * 检查 libnode.so 是否已加载
 */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_nodejs_NodeBridge_nativeIsLoaded(
        JNIEnv *env,
        jclass clazz) {
    return (g_node_handle != nullptr && g_node_start != nullptr) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
