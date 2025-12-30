/**
 * 完整性检查
 */

#include "crypto_engine.h"
#include <android/log.h>
#include <string>
#include <sys/stat.h>

#define LOG_TAG "IntegrityCheck"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
    
    env->ReleaseStringUTFChars(sourceDir, apkPath);
    
    return exists;
}
