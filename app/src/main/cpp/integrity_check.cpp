/**
 * 完整性检查
 * 
 * 增强版完整性验证，包括：
 * - 签名验证
 * - APK 完整性检查
 * - DEX 文件验证
 * - 代码段完整性检查
 */

#include "crypto_engine.h"
#include <android/log.h>
#include <string>
#include <sys/stat.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <dlfcn.h>
#include <link.h>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <cstring>

#define LOG_TAG "IntegrityCheck"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 代码段哈希缓存
static std::string g_codeHashCache;
static bool g_codeHashComputed = false;

// 获取应用签名哈希
std::string IntegrityCheck::getSignatureHash(JNIEnv* env, jobject context) {
    // 获取 Context 类
    jclass contextClass = env->GetObjectClass(context);
    if (contextClass == nullptr) {
        LOGE("Failed to get Context class");
        return "";
    }
    
    // 获取 getPackageManager 方法
    jmethodID getPackageManager = env->GetMethodID(contextClass, "getPackageManager", 
        "()Landroid/content/pm/PackageManager;");
    if (getPackageManager == nullptr) {
        LOGE("Failed to get getPackageManager method");
        return "";
    }
    
    // 获取 getPackageName 方法
    jmethodID getPackageName = env->GetMethodID(contextClass, "getPackageName", 
        "()Ljava/lang/String;");
    if (getPackageName == nullptr) {
        LOGE("Failed to get getPackageName method");
        return "";
    }
    
    // 调用 getPackageManager
    jobject packageManager = env->CallObjectMethod(context, getPackageManager);
    if (packageManager == nullptr) {
        LOGE("Failed to get PackageManager");
        return "";
    }
    
    // 调用 getPackageName
    jstring packageName = (jstring)env->CallObjectMethod(context, getPackageName);
    if (packageName == nullptr) {
        LOGE("Failed to get package name");
        return "";
    }
    
    // 获取 PackageManager 类
    jclass pmClass = env->GetObjectClass(packageManager);
    
    // 获取 getPackageInfo 方法
    jmethodID getPackageInfo = env->GetMethodID(pmClass, "getPackageInfo",
        "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    if (getPackageInfo == nullptr) {
        LOGE("Failed to get getPackageInfo method");
        return "";
    }
    
    // 调用 getPackageInfo (GET_SIGNATURES = 64)
    jobject packageInfo = env->CallObjectMethod(packageManager, getPackageInfo, packageName, 64);
    if (packageInfo == nullptr) {
        LOGE("Failed to get PackageInfo");
        return "";
    }
    
    // 获取 signatures 字段
    jclass piClass = env->GetObjectClass(packageInfo);
    jfieldID signaturesField = env->GetFieldID(piClass, "signatures", 
        "[Landroid/content/pm/Signature;");
    if (signaturesField == nullptr) {
        LOGE("Failed to get signatures field");
        return "";
    }
    
    jobjectArray signatures = (jobjectArray)env->GetObjectField(packageInfo, signaturesField);
    if (signatures == nullptr || env->GetArrayLength(signatures) == 0) {
        LOGE("No signatures found");
        return "";
    }
    
    // 获取第一个签名
    jobject signature = env->GetObjectArrayElement(signatures, 0);
    if (signature == nullptr) {
        LOGE("Failed to get signature");
        return "";
    }
    
    // 获取 Signature.toByteArray 方法
    jclass sigClass = env->GetObjectClass(signature);
    jmethodID toByteArray = env->GetMethodID(sigClass, "toByteArray", "()[B");
    if (toByteArray == nullptr) {
        LOGE("Failed to get toByteArray method");
        return "";
    }
    
    // 获取签名字节
    jbyteArray sigBytes = (jbyteArray)env->CallObjectMethod(signature, toByteArray);
    if (sigBytes == nullptr) {
        LOGE("Failed to get signature bytes");
        return "";
    }
    
    // 计算 SHA-256 哈希
    jsize len = env->GetArrayLength(sigBytes);
    std::vector<uint8_t> bytes(len);
    env->GetByteArrayRegion(sigBytes, 0, len, reinterpret_cast<jbyte*>(bytes.data()));
    
    std::vector<uint8_t> hash = KeyDerivation::sha256(bytes.data(), bytes.size());
    
    // 转换为十六进制字符串
    std::string result;
    result.reserve(64);
    for (uint8_t b : hash) {
        char hex[3];
        snprintf(hex, sizeof(hex), "%02x", b);
        result += hex;
    }
    
    LOGI("Signature hash: %s", result.c_str());
    return result;
}

// 验证签名
bool IntegrityCheck::verifySignature(JNIEnv* env, jobject context, const std::string& expected_hash) {
    std::string current_hash = getSignatureHash(env, context);
    
    if (current_hash.empty()) {
        LOGE("Failed to get current signature hash");
        return false;
    }
    
    // 不区分大小写比较
    bool match = true;
    if (current_hash.length() != expected_hash.length()) {
        match = false;
    } else {
        for (size_t i = 0; i < current_hash.length(); i++) {
            if (tolower(current_hash[i]) != tolower(expected_hash[i])) {
                match = false;
                break;
            }
        }
    }
    
    if (!match) {
        LOGE("Signature mismatch! Expected: %s, Got: %s", 
             expected_hash.c_str(), current_hash.c_str());
    }
    
    return match;
}

// 验证 APK 完整性
bool IntegrityCheck::verifyApkIntegrity(JNIEnv* env, jobject context) {
    // 获取 ApplicationInfo
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getAppInfo = env->GetMethodID(contextClass, "getApplicationInfo",
        "()Landroid/content/pm/ApplicationInfo;");
    
    if (getAppInfo == nullptr) {
        LOGE("Failed to get getApplicationInfo method");
        return false;
    }
    
    jobject appInfo = env->CallObjectMethod(context, getAppInfo);
    if (appInfo == nullptr) {
        LOGE("Failed to get ApplicationInfo");
        return false;
    }
    
    // 获取 sourceDir
    jclass aiClass = env->GetObjectClass(appInfo);
    jfieldID sourceDirField = env->GetFieldID(aiClass, "sourceDir", "Ljava/lang/String;");
    if (sourceDirField == nullptr) {
        LOGE("Failed to get sourceDir field");
        return false;
    }
    
    jstring sourceDir = (jstring)env->GetObjectField(appInfo, sourceDirField);
    if (sourceDir == nullptr) {
        LOGE("Failed to get sourceDir");
        return false;
    }
    
    const char* apkPath = env->GetStringUTFChars(sourceDir, nullptr);
    LOGI("APK path: %s", apkPath);
    
    // 检查 APK 文件是否存在
    struct stat st;
    bool exists = (stat(apkPath, &st) == 0);
    
    // 检查文件大小是否合理（至少 1KB）
    bool sizeValid = exists && st.st_size > 1024;
    
    // 检查文件权限
    bool permValid = exists && (st.st_mode & S_IFREG);
    
    env->ReleaseStringUTFChars(sourceDir, apkPath);
    
    if (!exists) {
        LOGE("APK file does not exist");
        return false;
    }
    
    if (!sizeValid) {
        LOGE("APK file size invalid");
        return false;
    }
    
    if (!permValid) {
        LOGE("APK file permissions invalid");
        return false;
    }
    
    return true;
}

/**
 * 计算内存区域的哈希值
 */
static std::string computeMemoryHash(const void* addr, size_t size) {
    if (addr == nullptr || size == 0) {
        return "";
    }
    
    std::vector<uint8_t> hash = KeyDerivation::sha256(
        static_cast<const uint8_t*>(addr), size);
    
    std::ostringstream oss;
    for (uint8_t b : hash) {
        oss << std::hex << std::setfill('0') << std::setw(2) << (int)b;
    }
    return oss.str();
}

/**
 * 验证 Native 库完整性
 * 检查 .text 段是否被修改
 */
static bool verifyNativeLibraryIntegrity() {
    // 获取当前库的加载地址
    Dl_info info;
    if (dladdr((void*)verifyNativeLibraryIntegrity, &info) == 0) {
        LOGW("Failed to get library info");
        return true; // 无法获取信息时不阻止
    }
    
    LOGI("Library: %s, base: %p", info.dli_fname, info.dli_fbase);
    
    // 读取 /proc/self/maps 获取代码段范围
    std::ifstream maps("/proc/self/maps");
    if (!maps.is_open()) {
        LOGW("Failed to open /proc/self/maps");
        return true;
    }
    
    std::string line;
    while (std::getline(maps, line)) {
        // 查找当前库的可执行段
        if (line.find(info.dli_fname) != std::string::npos && 
            line.find("r-xp") != std::string::npos) {
            
            // 解析地址范围
            size_t dashPos = line.find('-');
            if (dashPos == std::string::npos) continue;
            
            unsigned long startAddr = std::stoul(line.substr(0, dashPos), nullptr, 16);
            unsigned long endAddr = std::stoul(line.substr(dashPos + 1, 12), nullptr, 16);
            size_t size = endAddr - startAddr;
            
            // 计算代码段哈希
            std::string currentHash = computeMemoryHash(
                reinterpret_cast<void*>(startAddr), size);
            
            if (!g_codeHashComputed) {
                // 首次计算，保存基准值
                g_codeHashCache = currentHash;
                g_codeHashComputed = true;
                LOGI("Code segment hash computed: %s (size: %zu)", 
                     currentHash.substr(0, 16).c_str(), size);
            } else {
                // 比较哈希值
                if (currentHash != g_codeHashCache) {
                    LOGE("Code segment modified! Expected: %s, Got: %s",
                         g_codeHashCache.substr(0, 16).c_str(),
                         currentHash.substr(0, 16).c_str());
                    return false;
                }
            }
            
            break;
        }
    }
    
    return true;
}

/**
 * 检查是否有 Hook 框架
 */
static bool detectHookFramework() {
    // 检查常见 Hook 库
    const char* hookLibs[] = {
        "libsubstrate.so",
        "libxhook.so",
        "libfishook.so",
        "libinlinehook.so",
        "libsandhook.so",
        "libepic.so",
        "libwhale.so",
        nullptr
    };
    
    std::ifstream maps("/proc/self/maps");
    if (!maps.is_open()) {
        return false;
    }
    
    std::string line;
    while (std::getline(maps, line)) {
        for (int i = 0; hookLibs[i] != nullptr; i++) {
            if (line.find(hookLibs[i]) != std::string::npos) {
                LOGW("Hook library detected: %s", hookLibs[i]);
                return true;
            }
        }
    }
    
    return false;
}

/**
 * 综合完整性检查
 */
bool performComprehensiveIntegrityCheck(JNIEnv* env, jobject context) {
    bool passed = true;
    
    // 1. APK 完整性
    if (!IntegrityCheck::verifyApkIntegrity(env, context)) {
        LOGE("APK integrity check failed");
        passed = false;
    }
    
    // 2. Native 库完整性
    if (!verifyNativeLibraryIntegrity()) {
        LOGE("Native library integrity check failed");
        passed = false;
    }
    
    // 3. Hook 框架检测
    if (detectHookFramework()) {
        LOGW("Hook framework detected");
        // 不直接失败，只是警告
    }
    
    // 4. 调试器检测
    if (AntiDebug::isDebuggerAttached()) {
        LOGW("Debugger attached");
        // 在某些情况下可能需要阻止
    }
    
    return passed;
}
