/* Note. */

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

// Note.
typedef int (*node_start_func)(int argc, char *argv[]);

// Note.
static void *g_node_handle = nullptr;
static node_start_func g_node_start = nullptr;
static bool g_node_started = false;
static int g_node_exit_code = -1;

// Note.
static int g_stdout_pipe[2] = {-1, -1};
static int g_stderr_pipe[2] = {-1, -1};
static int g_original_stdout = -1;
static int g_original_stderr = -1;

// Note.
static sigjmp_buf g_abort_jmpbuf;
static volatile bool g_abort_handler_active = false;
static struct sigaction g_old_sigabrt_action;

// Note.
static JavaVM *g_jvm = nullptr;
static jobject g_callback_ref = nullptr;

/* Note. */
static void sigabrt_handler(int sig) {
    if (g_abort_handler_active) {
        LOGW("Caught SIGABRT from Node.js — recovering gracefully");
        g_abort_handler_active = false;
        // Note.
        sigaction(SIGABRT, &g_old_sigabrt_action, nullptr);
        // Note.
        siglongjmp(g_abort_jmpbuf, 1);
    } else {
        // Note.
        sigaction(SIGABRT, &g_old_sigabrt_action, nullptr);
        raise(SIGABRT);
    }
}

/* Note. */
static void install_abort_handler() {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = sigabrt_handler;
    sa.sa_flags = 0; // Note.
    sigemptyset(&sa.sa_mask);
    sigaction(SIGABRT, &sa, &g_old_sigabrt_action);

    // Note.
    signal(SIGPIPE, SIG_IGN);
}

/* Note. */
static void uninstall_abort_handler() {
    g_abort_handler_active = false;
    sigaction(SIGABRT, &g_old_sigabrt_action, nullptr);
}

/* Note. */
static void *log_reader_thread(void *arg) {
    int fd = *((int *)arg);
    const char *tag = (fd == g_stdout_pipe[0]) ? "NodeJS" : "NodeJS-err";
    int level = (fd == g_stdout_pipe[0]) ? ANDROID_LOG_INFO : ANDROID_LOG_WARN;

    char buf[4096];
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf) - 1)) > 0) {
        buf[n] = '\0';
        // Note.
        char *line = buf;
        char *nl;
        while ((nl = strchr(line, '\n')) != nullptr) {
            *nl = '\0';
            if (strlen(line) > 0) {
                __android_log_print(level, tag, "%s", line);

                // Note.
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
        // Note.
        if (strlen(line) > 0) {
            __android_log_print(level, tag, "%s", line);
        }
    }
    return nullptr;
}

/* Note. */
static bool setup_output_redirect() {
    if (pipe(g_stdout_pipe) != 0 || pipe(g_stderr_pipe) != 0) {
        LOGE("Failed to create pipes: %s", strerror(errno));
        return false;
    }

    // Note.
    g_original_stdout = dup(STDOUT_FILENO);
    g_original_stderr = dup(STDERR_FILENO);

    // Note.
    dup2(g_stdout_pipe[1], STDOUT_FILENO);
    dup2(g_stderr_pipe[1], STDERR_FILENO);
    close(g_stdout_pipe[1]);
    close(g_stderr_pipe[1]);

    // Note.
    pthread_t stdout_thread, stderr_thread;
    pthread_create(&stdout_thread, nullptr, log_reader_thread, &g_stdout_pipe[0]);
    pthread_create(&stderr_thread, nullptr, log_reader_thread, &g_stderr_pipe[0]);
    pthread_detach(stdout_thread);
    pthread_detach(stderr_thread);

    return true;
}

/* Note. */
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

/* Note. */
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

    // Note.
    struct stat st;
    if (stat(path, &st) != 0) {
        LOGE("libnode.so not found: %s (errno=%d: %s)", path, errno, strerror(errno));
        env->ReleaseStringUTFChars(nodePath, path);
        return JNI_FALSE;
    }
    LOGI("libnode.so size: %lld bytes", (long long)st.st_size);

    // Note.
    g_node_handle = dlopen(path, RTLD_LAZY);
    env->ReleaseStringUTFChars(nodePath, path);

    if (!g_node_handle) {
        LOGE("dlopen failed: %s", dlerror());
        return JNI_FALSE;
    }

    // Note.
    // Note.
    // Note.
    g_node_start = (node_start_func)dlsym(g_node_handle, "node_start");
    if (!g_node_start) {
        // Note.
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

/* Note. */
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

    // Note.
    if (callback) {
        g_callback_ref = env->NewGlobalRef(callback);
    }

    // Note.
    setup_output_redirect();

    // Note.
    jsize argc = env->GetArrayLength(arguments);

    // Note.
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

    // Note.
    install_abort_handler();

    int result;
    // Note.
    if (sigsetjmp(g_abort_jmpbuf, 1) == 0) {
        // Note.
        g_abort_handler_active = true;
        result = g_node_start(argc, argv);
        g_abort_handler_active = false;
        LOGI("Node.js exited with code: %d", result);
    } else {
        // Note.
        result = -99;
        LOGE("Node.js aborted (SIGABRT caught). Returning error code %d instead of crashing.", result);
    }

    // Note.
    uninstall_abort_handler();
    g_node_exit_code = result;

    // Note.
    free(argv);
    free(args_buffer);
    restore_output();

    if (g_callback_ref) {
        env->DeleteGlobalRef(g_callback_ref);
        g_callback_ref = nullptr;
    }

    return result;
}

/* Note. */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_nodejs_NodeBridge_nativeIsStarted(
        JNIEnv *env,
        jclass clazz) {
    return g_node_started ? JNI_TRUE : JNI_FALSE;
}

/* Note. */
JNIEXPORT jboolean JNICALL
Java_com_webtoapp_core_nodejs_NodeBridge_nativeIsLoaded(
        JNIEnv *env,
        jclass clazz) {
    return (g_node_handle != nullptr && g_node_start != nullptr) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"

