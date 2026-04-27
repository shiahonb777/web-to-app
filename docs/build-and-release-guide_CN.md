---
title: 构建与发布指南
description: 项目构建配置、签名设置、Shell 模板打包、运行时二进制准备、发布流程
---

# 构建与发布指南

## 一、环境要求

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| Android Studio | Hedgehog+ | 推荐 Iguana |
| JDK | 17 | `jvmTarget = "17"` |
| Android SDK | compileSdk 36 | minSdk 23, targetSdk 36 |
| NDK | 侧载 | CMake 3.22.1 |
| CMake | 3.22.1 | `externalNativeBuild.cmake.version` |
| Git | 2.x | 版本控制 |

---

## 二、项目结构

```
web-to-app-main/
├── app/                    # 主应用模块
│   ├── build.gradle.kts    # 构建配置
│   ├── proguard-rules.pro  # 混淆规则
│   └── src/
│       ├── main/
│       │   ├── java/       # Kotlin 源码
│       │   ├── cpp/        # CMake/JNI (Node bridge, 性能优化)
│       │   ├── jniLibs/    # 预置 native 库 (libphp.so)
│       │   └── assets/     # 静态资源 + Shell 模板
│       └── test/           # 单元测试
├── shell/                  # Shell 模板 APK 模块 ★
│   ├── build.gradle.kts
│   └── src/main/
├── docs/                   # 项目文档
└── build.gradle.kts        # 根构建文件
```

---

## 三、首次构建

### 3.1 克隆与配置

```bash
git clone <repo-url>
cd web-to-app-main

# 配置签名信息 (必须，否则 Release 构建失败)
cat > local.properties << EOF
signing.storeFile=/path/to/keystore.jks
signing.storePassword=your_store_password
signing.keyAlias=your_key_alias
signing.keyPassword=your_key_password
EOF
```

### 3.2 下载运行时二进制

```bash
# PHP 二进制 (WordPress/PHP App 运行时需要)
./gradlew downloadPhpBinary
# 下载到 app/src/main/jniLibs/arm64-v8a/libphp.so

# Node.js / Python / Go 二进制
# 这些在应用内按需下载 (NodeDependencyManager / PythonDependencyManager)
# 首次构建不需要，运行时会自动下载到应用私有目录
```

### 3.3 构建 Shell 模板

```bash
# 构建 Shell 模板 APK 并复制到 app/assets/template/
./gradlew syncShellTemplateApk
# 输出: app/src/main/assets/template/webview_shell.apk
```

**重要**：`preBuild` 任务自动依赖 `syncShellTemplateApk`，所以正常构建会自动触发。但如果 Shell 模板 APK 不存在，构建仍会成功（回退到自身 APK 作为模板）。

### 3.4 完整构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建 (需要签名配置)
./gradlew assembleRelease
```

---

## 四、构建配置详解

### 4.1 签名配置

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("shiaho") {
        storeFile = file(localProperties.getProperty("signing.storeFile", ""))
        storePassword = localProperties.getProperty("signing.storePassword", "")
        keyAlias = localProperties.getProperty("signing.keyAlias", "")
        keyPassword = localProperties.getProperty("signing.keyPassword", "")
    }
}

buildTypes {
    release {
        isMinifyEnabled = true       // R8 代码混淆
        isShrinkResources = true     // 资源压缩
        signingConfig = signingConfigs.getByName("shiaho")
        proguardFiles(...)
    }
}
```

**注意**：
- `local.properties` 不提交到 VCS（已在 `.gitignore` 中）
- Release 构建必须配置签名，否则编译失败
- 导出的 APK 使用独立的签名密钥（`JarSigner`），与应用本身的签名无关

### 4.2 NDK / CMake 配置

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
```

Native 代码用途：
- **Node Bridge** (`libnode_bridge.so`) — JNI dlopen Node.js 二进制并调用 `node::Start()`
- **性能优化** — CPU 亲和性、FD 上限、线程优先级等系统级调用

### 4.3 ABI Splits

```kotlin
splits {
    abi {
        isEnable = false  // 禁用！生成单一完整 APK
    }
}
```

**为什么禁用**：WebToApp 使用自身 APK 作为 Shell 模板，如果按 ABI 分裂，模板 APK 将不完整。

### 4.4 Packaging 选项

```kotlin
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    jniLibs {
        useLegacyPackaging = true  // native 库不压缩
        // 排除 GeckoView 原生库 — 按需下载，不内置
        excludes += "**/libxul.so"
        excludes += "**/libmozglue.so"
        excludes += "**/libgeckoffi.so"
    }
}
```

### 4.5 AAPT 选项

```kotlin
aaptOptions {
    ignoreAssetsPattern = ""  // 允许 dot-prefixed 文件 (如 .pypackages)
}
```

默认 AAPT 忽略 `.` 开头的文件，但 Python 预安装依赖存放在 `.pypackages/` 中，必须允许打包。

### 4.6 KSP (替代 KAPT)

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    ksp("androidx.room:room-compiler:2.6.1")  // 不用 kapt
}
```

---

## 五、Shell 模板 APK

### 5.1 双模块架构

项目包含两个 Gradle 模块：

| 模块 | 产物 | 用途 |
|------|------|------|
| `:app` | 主 APK | 编辑器应用 |
| `:shell` | Shell APK | 轻量 WebView 运行时模板 |

### 5.2 Shell 模板构建流程

```kotlin
// app/build.gradle.kts
val shellTemplateOutput = project(":shell")
    .layout.buildDirectory.file("outputs/apk/release/shell-release.apk")

tasks.register<Copy>("syncShellTemplateApk") {
    dependsOn(":shell:assembleRelease")
    from(shellTemplateOutput)
    into(file("src/main/assets/template"))
    rename { "webview_shell.apk" }
}

tasks.named("preBuild").configure {
    dependsOn("syncShellTemplateApk")
}
```

流程：
1. `:shell:assembleRelease` — 构建 Shell 模板 APK
2. `syncShellTemplateApk` — 复制到 `app/src/main/assets/template/webview_shell.apk`
3. `preBuild` — 确保每次构建前 Shell 模板是最新的

### 5.3 模板选择策略

运行时由 `CompositeTemplateProvider` 决定使用哪个模板：

```
1. AssetTemplateProvider — 从 assets/template/webview_shell.apk 加载
   ✓ 支持: WEB, HTML, FRONTEND, IMAGE, VIDEO, GALLERY
   ✗ 不支持: NODEJS_APP, PHP_APP, PYTHON_APP, GO_APP, WORDPRESS

2. SelfAsTemplateProvider — 使用自身 APK
   ✓ 始终可用 (回退方案)
   ✗ 包含完整 Builder 代码，导出 APK 偏大
```

**注意**：运行时类型的应用（Node/PHP/Python/Go/WordPress）必须使用自身 APK 作为模板，因为需要完整的运行时管理代码。

---

## 六、运行时二进制管理

### 6.1 PHP

```bash
# 一次性下载 (Gradle task)
./gradlew downloadPhpBinary
# 来源: github.com/pmmp/PHP-Binaries
# 目标: app/src/main/jniLibs/arm64-v8a/libphp.so
# 原因: Android 15+ SELinux 要求 native library 在 nativeLibraryDir
```

### 6.2 Node.js

- **构建时**：`NodeDependencyManager` 从应用缓存目录获取
- **运行时**：嵌入为 `lib/{abi}/libnode.so` (native library)
- **首次使用**：应用内自动下载到 `filesDir/nodejs/`

### 6.3 Python

- **构建时**：`PythonDependencyManager` 自动下载 Python 运行时
- **嵌入内容**：
  - `lib/{abi}/libpython3.so` — Python 二进制
  - `lib/{abi}/libmusl-linker.so` — musl 动态链接器
  - `assets/python_runtime/lib/` — Python 标准库
  - `assets/python_app/sitecustomize.py` — Android 运行时修复
  - `assets/python_app/.pypackages/` — 预安装的 pip 依赖

### 6.4 Go

- **构建时**：使用项目内编译的二进制
- **嵌入方式**：`RuntimeAssetEmbedder.goConfig()` — 大文件流式写入

---

## 七、导出 APK 签名

导出 APK 的签名与主应用签名完全独立：

### 7.1 签名密钥来源

```
JarSigner 初始化优先级:
1. PKCS12 自动生成密钥 (最兼容 ApkSigner) → webtoapp_keystore.p12
2. Android KeyStore 密钥 (回退方案)
3. 自定义 PKCS12 密钥 (用户导入)
```

### 7.2 签名方案降级

```
尝试签名方案:
1. V1+V2+V3 (最完整)
2. V1+V2 (兼容 Android 7+，跳过可能有问题的 V3)
3. V1-only (最大兼容性回退)
```

### 7.3 密钥与加密的关系

加密密钥派生依赖签名证书哈希：

```kotlin
val signatureHash = signer.getCertificateSignatureHash()
keyManager.generateKeyForPackage(packageName, signatureHash, encryptionLevel, customPassword)
```

**重要**：如果签名密钥丢失或更换，已加密的 APK 将无法解密。PKCS12 密钥存储在应用私有目录，卸载重装后密钥丢失。

---

## 八、ProGuard / R8

Release 构建启用代码混淆和资源压缩：

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

需要注意的保留规则：
- Room Entity 和 DAO 类不能被混淆
- Gson 序列化的 data class 字段名必须保留
- Koin 注入的类名需要保留
- JNI native 方法不能被混淆
- Shell 模式通过反射加载的类需要保留

---

## 九、发布流程

### 9.1 版本号管理

```kotlin
// app/build.gradle.kts
defaultConfig {
    versionCode = 32      # 递增整数，Google Play 用于区分版本
    versionName = "1.9.5" # 用户可见的版本号
}
```

### 9.2 发布前检查清单

- [ ] 更新 `versionCode` 和 `versionName`
- [ ] 运行全部单元测试：`./gradlew :app:testDebugUnitTest`
- [ ] 构建 Release APK：`./gradlew assembleRelease`
- [ ] 在真机上测试导出 APK 流程
- [ ] 测试 Shell 模式启动
- [ ] 检查 ProGuard 是否导致运行时异常
- [ ] 确认 Shell 模板 APK 已更新

### 9.3 构建产物

```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk          # Debug 构建
└── release/
    └── app-release.apk        # Release 构建 (已签名)

shell/build/outputs/apk/
└── release/
    └── shell-release.apk      # Shell 模板 APK
```

---

## 十、常见构建问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `signing.storeFile` 为空 | `local.properties` 未配置 | 添加签名配置 |
| `libphp.so` not found | 未下载 PHP 二进制 | `./gradlew downloadPhpBinary` |
| Shell 模板 APK 缺失 | `:shell` 模块未构建 | `./gradlew syncShellTemplateApk` |
| GeckoView .so 冲突 | 打包时排除规则未生效 | 检查 `jniLibs.excludes` |
| ProGuard 运行时崩溃 | 混淆了反射使用的类 | 在 `proguard-rules.pro` 添加保留规则 |
| Python `.pypackages` 未打包 | AAPT 忽略 dot 文件 | 确认 `ignoreAssetsPattern = ""` |
| ABI splits 导致模板不完整 | 启用了 ABI splits | 确认 `splits.abi.isEnable = false` |
