/**
 * 反调试和环境检测
 */

#include "crypto_engine.h"
#include <android/log.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/stat.h>
#include <fstream>
#include <cstring>
#include <dirent.h>
#include <dlfcn.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#define LOG_TAG "AntiDebug"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// 检测调试器
bool AntiDebug::isDebuggerAttached() {
    // 方法1: 检查 TracerPid
    std::ifstream status("/proc/self/status");
    if (status.is_open()) {
        std::string line;
        while (std::getline(status, line)) {
            if (line.find("TracerPid:") != std::string::npos) {
                int tracer_pid = 0;
                sscanf(line.c_str(), "TracerPid: %d", &tracer_pid);
                if (tracer_pid != 0) {
                    LOGW("TracerPid detected: %d", tracer_pid);
                    return true;
                }
                break;
            }
        }
    }
    
    // 方法2: 尝试 ptrace 自己
    if (ptrace(PTRACE_TRACEME, 0, nullptr, nullptr) == -1) {
        // 如果失败，可能已经被调试
        // 但这不是绝对的，因为某些情况下也会失败
    }
    
    return false;
}

// 检测 tracer
bool AntiDebug::isTracerAttached() {
    char path[256];
    snprintf(path, sizeof(path), "/proc/%d/status", getpid());
    
    std::ifstream status(path);
    if (!status.is_open()) return false;
    
    std::string line;
    while (std::getline(status, line)) {
        if (line.find("TracerPid:") != std::string::npos) {
            int tracer_pid = 0;
            sscanf(line.c_str(), "TracerPid: %d", &tracer_pid);
            return tracer_pid != 0;
        }
    }
    
    return false;
}

// 检测 Frida
bool AntiDebug::detectFrida() {
    // 方法1: 检查 Frida 默认端口
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) return false;
    
    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(27042);  // Frida 默认端口
    inet_pton(AF_INET, "127.0.0.1", &addr.sin_addr);
    
    // 设置超时
    struct timeval timeout;
    timeout.tv_sec = 0;
    timeout.tv_usec = 100000;  // 100ms
    setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));
    
    bool frida_detected = (connect(sock, (struct sockaddr*)&addr, sizeof(addr)) == 0);
    close(sock);
    
    if (frida_detected) {
        LOGW("Frida port detected");
        return true;
    }
    
    // 方法2: 检查 /proc/self/maps 中的 frida 相关库
    std::ifstream maps("/proc/self/maps");
    if (maps.is_open()) {
        std::string line;
        while (std::getline(maps, line)) {
            if (line.find("frida") != std::string::npos ||
                line.find("gadget") != std::string::npos) {
                LOGW("Frida library detected in maps");
                return true;
            }
        }
    }
    
    // 方法3: 检查 frida-server 进程
    DIR* dir = opendir("/proc");
    if (dir) {
        struct dirent* entry;
        while ((entry = readdir(dir)) != nullptr) {
            if (entry->d_type == DT_DIR) {
                char cmdline_path[256];
                snprintf(cmdline_path, sizeof(cmdline_path), "/proc/%s/cmdline", entry->d_name);
                
                std::ifstream cmdline(cmdline_path);
                if (cmdline.is_open()) {
                    std::string cmd;
                    std::getline(cmdline, cmd);
                    if (cmd.find("frida") != std::string::npos) {
                        closedir(dir);
                        LOGW("Frida process detected");
                        return true;
                    }
                }
            }
        }
        closedir(dir);
    }
    
    return false;
}

// 检测 Xposed
bool AntiDebug::detectXposed() {
    // 方法1: 检查 Xposed 类
    // 这需要在 Java 层检查，这里检查文件系统
    
    const char* xposed_paths[] = {
        "/system/framework/XposedBridge.jar",
        "/system/bin/app_process.orig",
        "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so",
        "/data/data/de.robv.android.xposed.installer",
        "/data/user/0/de.robv.android.xposed.installer",
        nullptr
    };
    
    for (int i = 0; xposed_paths[i] != nullptr; i++) {
        struct stat st;
        if (stat(xposed_paths[i], &st) == 0) {
            LOGW("Xposed path detected: %s", xposed_paths[i]);
            return true;
        }
    }
    
    // 方法2: 检查 /proc/self/maps
    std::ifstream maps("/proc/self/maps");
    if (maps.is_open()) {
        std::string line;
        while (std::getline(maps, line)) {
            if (line.find("XposedBridge") != std::string::npos ||
                line.find("libxposed") != std::string::npos) {
                LOGW("Xposed library detected in maps");
                return true;
            }
        }
    }
    
    return false;
}

// 检测模拟器
bool AntiDebug::isRunningInEmulator() {
    // 检查常见模拟器特征文件
    const char* emulator_files[] = {
        "/dev/socket/qemud",
        "/dev/qemu_pipe",
        "/system/lib/libc_malloc_debug_qemu.so",
        "/sys/qemu_trace",
        "/system/bin/qemu-props",
        "/dev/goldfish_pipe",
        nullptr
    };
    
    for (int i = 0; emulator_files[i] != nullptr; i++) {
        struct stat st;
        if (stat(emulator_files[i], &st) == 0) {
            return true;
        }
    }
    
    // 检查 /proc/cpuinfo
    std::ifstream cpuinfo("/proc/cpuinfo");
    if (cpuinfo.is_open()) {
        std::string line;
        while (std::getline(cpuinfo, line)) {
            if (line.find("goldfish") != std::string::npos ||
                line.find("ranchu") != std::string::npos) {
                return true;
            }
        }
    }
    
    return false;
}

// 检测 Root
bool AntiDebug::isRooted() {
    const char* root_paths[] = {
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/magisk/.core",
        "/sbin/.magisk",
        nullptr
    };
    
    for (int i = 0; root_paths[i] != nullptr; i++) {
        struct stat st;
        if (stat(root_paths[i], &st) == 0) {
            return true;
        }
    }
    
    // 检查 su 命令是否可执行
    if (access("/system/xbin/su", X_OK) == 0 ||
        access("/system/bin/su", X_OK) == 0) {
        return true;
    }
    
    return false;
}
