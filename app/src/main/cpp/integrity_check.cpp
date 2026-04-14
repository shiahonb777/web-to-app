/* Note. */

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

// Note.
static std::string g_codeHashCache;
static bool g_codeHashComputed = false;

// Note.
std::string IntegrityCheck::getSignatureHash(JNIEnv* env, jobject context) {
    // Note.
    jclass contextClass = env->GetObjectClass(context);
    if (contextClass == nullptr) {
        LOGE("Failed to get Context class");
        return "";
    }
    
    // Note.
    jmethodID getPackageManager = env->GetMethodID(contextClass, "getPackageManager", 
        "()Landroid/content/pm/PackageManager;");
    if (getPackageManager == nullptr) {
        LOGE("Failed to get getPackageManager method");
        return "";
    }
    
    // Note.
    jmethodID getPackageName = env->GetMethodID(contextClass, "getPackageName", 
        "()Ljava/lang/String;");
    if (getPackageName == nullptr) {
        LOGE("Failed to get getPackageName method");
        return "";
    }
    
    // Note.
    jobject packageManager = env->CallObjectMethod(context, getPackageManager);
    if (packageManager == nullptr) {
        LOGE("Failed to get PackageManager");
        return "";
    }
    
    // Note.
    jstring packageName = (jstring)env->CallObjectMethod(context, getPackageName);
    if (packageName == nullptr) {
        LOGE("Failed to get package name");
        return "";
    }
    
    // Note.
    jclass pmClass = env->GetObjectClass(packageManager);
    
    // Note.
    jmethodID getPackageInfo = env->GetMethodID(pmClass, "getPackageInfo",
        "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    if (getPackageInfo == nullptr) {
        LOGE("Failed to get getPackageInfo method");
        return "";
    }
    
    // Note.
    jobject packageInfo = env->CallObjectMethod(packageManager, getPackageInfo, packageName, 64);
    if (packageInfo == nullptr) {
        LOGE("Failed to get PackageInfo");
        return "";
    }
    
    // Note.
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
    
    // Note.
    jobject signature = env->GetObjectArrayElement(signatures, 0);
    if (signature == nullptr) {
        LOGE("Failed to get signature");
        return "";
    }
    
    // Note.
    jclass sigClass = env->GetObjectClass(signature);
    jmethodID toByteArray = env->GetMethodID(sigClass, "toByteArray", "()[B");
    if (toByteArray == nullptr) {
        LOGE("Failed to get toByteArray method");
        return "";
    }
    
    // Note.
    jbyteArray sigBytes = (jbyteArray)env->CallObjectMethod(signature, toByteArray);
    if (sigBytes == nullptr) {
        LOGE("Failed to get signature bytes");
        return "";
    }
    
    // Note.
    jsize len = env->GetArrayLength(sigBytes);
    std::vector<uint8_t> bytes(len);
    env->GetByteArrayRegion(sigBytes, 0, len, reinterpret_cast<jbyte*>(bytes.data()));
    
    std::vector<uint8_t> hash = KeyDerivation::sha256(bytes.data(), bytes.size());
    
    // Note.
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

// Note.
bool IntegrityCheck::verifySignature(JNIEnv* env, jobject context, const std::string& expected_hash) {
    std::string current_hash = getSignatureHash(env, context);
    
    if (current_hash.empty()) {
        LOGE("Failed to get current signature hash");
        return false;
    }
    
    // Note.
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

// Note.
bool IntegrityCheck::verifyApkIntegrity(JNIEnv* env, jobject context) {
    // Note.
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
    
    // Note.
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
    
    // Note.
    struct stat st;
    bool exists = (stat(apkPath, &st) == 0);
    
    // Note.
    bool sizeValid = exists && st.st_size > 1024;
    
    // Note.
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

/* Note. */
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

/* Note. */
static bool verifyNativeLibraryIntegrity() {
    // Note.
    Dl_info info;
    if (dladdr((void*)verifyNativeLibraryIntegrity, &info) == 0) {
        LOGW("Failed to get library info");
        return true; // Note.
    }
    
    LOGI("Library: %s, base: %p", info.dli_fname, info.dli_fbase);
    
    // Note.
    std::ifstream maps("/proc/self/maps");
    if (!maps.is_open()) {
        LOGW("Failed to open /proc/self/maps");
        return true;
    }
    
    std::string line;
    while (std::getline(maps, line)) {
        // Note.
        if (line.find(info.dli_fname) != std::string::npos && 
            line.find("r-xp") != std::string::npos) {
            
            // Note.
            size_t dashPos = line.find('-');
            if (dashPos == std::string::npos) continue;
            
            unsigned long startAddr = std::stoul(line.substr(0, dashPos), nullptr, 16);
            unsigned long endAddr = std::stoul(line.substr(dashPos + 1, 12), nullptr, 16);
            size_t size = endAddr - startAddr;
            
            // Note.
            std::string currentHash = computeMemoryHash(
                reinterpret_cast<void*>(startAddr), size);
            
            if (!g_codeHashComputed) {
                // Note.
                g_codeHashCache = currentHash;
                g_codeHashComputed = true;
                LOGI("Code segment hash computed: %s (size: %zu)", 
                     currentHash.substr(0, 16).c_str(), size);
            } else {
                // Note.
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

/* Note. */
static bool detectHookFramework() {
    // Note.
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

/* Note. */
bool performComprehensiveIntegrityCheck(JNIEnv* env, jobject context) {
    bool passed = true;
    
    // Note.
    if (!IntegrityCheck::verifyApkIntegrity(env, context)) {
        LOGE("APK integrity check failed");
        passed = false;
    }
    
    // Note.
    if (!verifyNativeLibraryIntegrity()) {
        LOGE("Native library integrity check failed");
        passed = false;
    }
    
    // Note.
    if (detectHookFramework()) {
        LOGW("Hook framework detected");
        // Note.
    }
    
    // Note.
    if (AntiDebug::isDebuggerAttached()) {
        LOGW("Debugger attached");
        // Note.
    }
    
    return passed;
}

