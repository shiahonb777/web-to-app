---
title: 架构设计文档
description: WebToApp 整体架构、分层设计、双轨模式、核心子系统协作关系
---

# 架构设计文档

## 一、项目定位

WebToApp 是一个 Android 应用，将网站/前端项目/后端运行时打包成独立 APK。用户在编辑器中配置参数，一键导出可安装的独立应用。

**核心能力**：网站 → 独立 APK（含 WebView/GeckoView 运行时 + 可选加密/护盾/扩展模块）。

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────┐
│                    Android App                       │
├──────────────┬──────────────┬───────────────────────┤
│   UI 层      │  业务逻辑层   │     数据层            │
│  (Compose)   │  (core/)     │  (Room + DataStore)   │
├──────────────┼──────────────┼───────────────────────┤
│  screens/    │  activation/ │  ui/data/database/     │
│  components/ │  apkbuilder/ │  ui/data/repository/   │
│  shell/      │  engine/     │  ui/data/model/       │
│  webview/    │  extension/  │  ui/data/converter/   │
│  theme/      │  crypto/     │                       │
│  ...         │  ...         │                       │
├──────────────┴──────────────┴───────────────────────┤
│              依赖注入 (Koin)                          │
├─────────────────────────────────────────────────────┤
│              平台层 (Android SDK + CMake/JNI)        │
└─────────────────────────────────────────────────────┘
```

### 2.1 分层职责

| 层 | 目录 | 职责 | 依赖方向 |
|----|------|------|---------|
| **UI 层** | `ui/screens/`, `ui/components/` | Compose 界面、用户交互 | ↓ 依赖 ViewModel |
| **ViewModel 层** | `ui/viewmodel/` | 状态管理、业务编排 | ↓ 依赖 Repository/Manager |
| **业务逻辑层** | `core/` (50+ 子模块) | 纯逻辑：加密、护盾、扩展、构建、激活 | ↓ 依赖 Data 层 |
| **数据层** | `ui/data/` | Room DAO、Repository、TypeConverter | ↓ 依赖 Room/DataStore |
| **平台层** | `WebToAppApplication`, `di/`, CMake | 应用生命周期、DI、JNI | 被所有层依赖 |

---

## 三、双轨模式（核心架构特征）

项目存在两条完全不同的运行路径，共享大部分代码但初始化方式不同：

### 3.1 编辑器模式（主 App）

```
WebToAppApplication.onCreate()
  → startKoin { modules(appModules) }     // Koin DI 初始化
  → shellModeManager.isShellMode() == false
  → SecurityInitializer.initialize()
  → WebViewPool.prewarm()
  → SystemPerfOptimizer.initSystem()
  → billingManager.connect()
  → startBackgroundServices()
```

- 包含完整 UI（HomeScreen、编辑器、设置、云同步等）
- 通过 Koin 注入所有依赖
- `BuildConfig.SHELL_RUNTIME_ONLY == false`

### 3.2 Shell 模式（导出的 APK）

```
WebToAppApplication.onCreate()
  → BuildConfig.SHELL_RUNTIME_ONLY == true
  → initShellRuntime()                    // 无 Koin
  → ShellModeManager(this)                // 直接实例化
  → ShellModeManager.isShellMode() == true
  → ShellActivity 启动 → 加载 app_config.json → WebView
```

- 不包含 Koin 框架（减小 APK 体积）
- 通过 `companion object` 单例获取依赖：`WebToAppApplication.shellMode`
- 依赖 `app_config.json`（在 assets 中）驱动所有行为

### 3.3 双轨 DI 模式

```kotlin
// AppModule.kt 中的双轨设计
val managerModule = module {
    single { ExtensionManager.getInstance(androidContext()) }  // Koin 返回与 getInstance() 相同实例
}

// 编辑器侧：Koin 注入
val manager: ExtensionManager by inject()

// Shell 侧：companion object 单例
val manager = ExtensionManager.getInstance(context)
```

**规则**：
- 编辑器侧新代码 → `by inject()` / `get()`
- Shell 侧代码 → `Xxx.getInstance(context)`
- Manager 类需同时支持两种获取方式

---

## 四、核心子系统

### 4.1 APK 构建系统 (`core/apkbuilder/`)

这是项目最核心的子系统，负责将用户配置打包成独立 APK。

```
构建流程：
1. 获取模板 APK (CompositeTemplateProvider: Asset优先 → 自身APK回退)
2. 并行准备资源 (模板复制 + 加密密钥生成 + 项目目录查找)
3. 修改 APK 内容:
   - 注入 app_config.json
   - 修改 AndroidManifest.xml (包名/权限/组件)
   - 修改 resources.arsc (应用名)
   - 替换图标
   - 嵌入资源 (HTML/媒体/BGM/运行时二进制)
   - 可选: 加密资源 (AES-GCM + HKDF)
   - 可选: 性能优化 (Linux perf)
4. 签名 (JarSigner: V1+V2+V3 → V1+V2 → V1-only 降级策略)
5. 验证 + 分析 + 清理
```

关键类：

| 类 | 职责 |
|----|------|
| `ApkBuilder` | 构建编排器，协程并行化 |
| `ShellTemplateProvider` | 模板来源策略接口 |
| `CompositeTemplateProvider` | Asset优先 → Self回退 |
| `JarSigner` | APK签名 (PKCS12/AndroidKeyStore) |
| `AxmlEditor` / `AxmlRebuilder` | 二进制XML编辑 |
| `ArscEditor` / `ArscRebuilder` | 资源表编辑 |
| `RuntimeAssetEmbedder` | 运行时文件嵌入统一化 |
| `AssetEncryptor` | 资源加密 |
| `BuildLogger` | 构建日志 |

### 4.2 引擎系统 (`core/engine/`)

支持多种 WebView 引擎和运行时：

| 引擎 | 实现 | 用途 |
|------|------|------|
| **Android WebView** | 系统内置 | 默认引擎，轻量 |
| **GeckoView** | Firefox 内核 | 高级护盾需求，按需下载 (~50MB) |

运行时引擎（后端服务）：

| 运行时 | 二进制位置 | 嵌入方式 |
|--------|-----------|---------|
| **Node.js** | `assets/node/{abi}/node` → `lib/{abi}/libnode.so` | Native library + JNI bridge |
| **PHP** | `lib/{abi}/libphp.so` | Native library (SELinux safe) |
| **Python** | `lib/{abi}/libpython3.so` + `libmusl-linker.so` | Native library + musl linker |
| **Go** | 项目内编译的二进制 | 直接嵌入 |
| **WordPress** | PHP binary + WP 项目文件 | PHP 复用 |

### 4.3 护盾系统 (`core/engine/shields/`)

多个护盾组件协作保护用户隐私：

```
BrowserShields (总控制器)
  ├── HttpsUpgrader     — HTTP→HTTPS 自动升级 + SSL错误回退
  ├── TrackerBlocker    — 追踪器拦截 (域名规则 + 路径模式)
  ├── AdBlocker         — 广告拦截 (独立于 TrackerBlocker)
  ├── ReaderMode        — 阅读模式 (Readability-lite 提取)
  ├── ShieldsStats      — 统计 (页面级 + 会话级)
  └── CookieConsent     — Cookie 横幅拦截
```

**协作关系**：
- `BrowserShields.onPageStarted()` → 重置页面统计 + 清除 HTTPS 升级 pending
- `HttpsUpgrader.tryUpgrade()` → 检查白名单(localhost/IP/.local) → 升级
- `HttpsUpgrader.onSslError()` → 标记失败域名 → `tryHttpFallback()` 回退
- `TrackerBlocker.shouldBlock()` → `AdBlocker.shouldBlock()` 各自独立判断

### 4.4 扩展模块系统 (`core/extension/`)

用户可安装/开发浏览器扩展模块：

```
ExtensionManager (单例, 文件持久化)
  ├── 内置模块 (dark-mode, auto-scroll, ad-blocker-lite...)
  ├── 用户模块 (addModule / removeModule)
  ├── 预设管理 (ModulePresetManager: 内置 + 用户预设)
  └── 分享码 (JSON 导出/导入)

ExtensionModule (数据模型)
  ├── URL 匹配规则 (glob / regex, include / exclude)
  ├── 代码注入 (JS + CSS)
  ├── 配置项 (ModuleConfigItem: required/optional)
  ├── UI 类型 (FLOATING_BUTTON / SIDEBAR / PANEL)
  └── 运行时机 (document_start / document_end / document_idle)
```

### 4.5 加密系统 (`core/crypto/`)

多层加密保护导出 APK 的资源：

```
EncryptionConfig (配置)
  ├── 加密类别: config / html / media / splash / bgm
  ├── 安全保护: integrityCheck / antiDebug / antiTamper / rootDetection / emulatorDetection / runtimeProtection
  └── 加密级别: FAST(5K) / STANDARD(10K) / HIGH(50K) / PARANOID(100K) 迭代次数

AssetEncryptor (执行加密)
  └── AES-GCM + HKDF 密钥派生 (签名哈希 + 包名 + 自定义密码)

EnhancedCrypto (增强加密)
  ├── HKDF 密钥派生
  ├── 流式加密/解密
  └── SecurityKeyContainer (安全密钥容器)

KeyManager (密钥管理)
  └── generateKeyForPackage() — 基于签名证书哈希派生密钥
```

### 4.6 激活系统 (`core/activation/`)

控制导出 APK 的使用权限：

```
ActivationCode (激活码模型)
  ├── 类型: PERMANENT / TIME_LIMITED / USAGE_LIMITED / DEVICE_BOUND
  ├── 状态: ActivationStatus (isValid / isExpired / isUsageExceeded)
  └── 序列化: fromJson / toJson / fromLegacyString

ActivationManager (管理器)
  ├── 验证激活码
  ├── 设备绑定 (ANDROID_ID)
  └── 使用次数追踪
```

---

## 五、数据流

### 5.1 编辑器 → 导出 APK 数据流

```
用户在 HomeScreen 填写配置
  → MainViewModel.saveWebApp()
  → WebApp (data class) 持久化到 Room
  → 用户点击"导出 APK"
  → ApkBuilder.buildApk(webApp)
    → webApp.toApkConfigWithModules()  // 转换为 ApkConfig
    → ApkTemplate 注入配置到模板
    → JarSigner 签名
  → BuildResult.Success(signedApk)
  → 分享/安装
```

### 5.2 Shell 模式启动数据流

```
用户安装导出的 APK
  → Application.onCreate() (SHELL_RUNTIME_ONLY=true)
  → ShellModeManager.loadConfig()
    → AssetDecryptor.loadAssetAsString("app_config.json")
    → Gson.fromJson(jsonStr, ShellConfig::class.java)
  → ShellActivity 启动
    → ShellActivityInit 初始化各子系统
    → WebView.loadUrl(config.targetUrl)
    → 扩展模块注入
    → 护盾系统激活
    → 激活码验证
```

---

## 六、依赖注入架构

### 6.1 Koin 模块划分

| 模块 | 内容 |
|------|------|
| `databaseModule` | AppDatabase, WebAppDao, AppCategoryDao, AppUsageStatsDao |
| `repositoryModule` | WebAppRepository, AppCategoryRepository, AppStatsRepository |
| `managerModule` | ActivationManager, ShellModeManager, ExtensionManager, BillingManager, 等 |
| `useCaseModule` | SaveAppUseCase |
| `viewModelModule` | MainViewModel, AuthViewModel, CloudViewModel, CommunityViewModel（含评分分布/评论排序/精选推荐/更新检测） |

### 6.2 单例模式双轨兼容

Manager 类需实现 `companion object` 单例 + Koin 注册：

```kotlin
class ExtensionManager private constructor(context: Context) {
    companion object {
        @Volatile private var instance: ExtensionManager? = null
        fun getInstance(context: Context): ExtensionManager {
            return instance ?: synchronized(this) {
                instance ?: ExtensionManager(context.applicationContext).also { instance = it }
            }
        }
        fun release() { instance = null }
    }
}

// AppModule.kt
single { ExtensionManager.getInstance(androidContext()) }
```

---

## 七、技术栈总览

| 类别 | 技术 | 用途 |
|------|------|------|
| 语言 | Kotlin | 主开发语言 |
| UI | Jetpack Compose + Material 3 | 全部界面 |
| 架构 | MVVM | ViewModel + StateFlow |
| DI | Koin | 编辑器模式依赖注入 |
| 数据库 | Room + KSP | 本地持久化 |
| 偏好 | DataStore | 键值存储 |
| 网络 | OkHttp | HTTP 客户端 |
| 图片 | Coil | 图片加载 (含 GIF/Video) |
| JSON | Gson | 序列化/反序列化 |
| 浏览器 | WebView + GeckoView | 双引擎 |
| 加密 | AES-GCM + HKDF + PBKDF2 | 资源加密 |
| 签名 | apksig (v1/v2/v3) | APK 签名 |
| 支付 | Google Play Billing | Pro/Ultra 订阅 |
| 认证 | Credential Manager + OAuth | Google 登录 |
| 测试 | JUnit4 + Truth + Robolectric | 单元测试 |
| Native | CMake + JNI | Node bridge / 性能优化 |
| 图表 | Vico | 数据看板 |
| 二维码 | ZXing | 生成和扫描 |

---

## 八、目录结构

```
app/src/main/java/com/webtoapp/
├── WebToAppApplication.kt     # Application 入口
├── di/AppModule.kt            # Koin DI 模块定义
├── core/                      # 业务逻辑层 (50+ 子模块)
│   ├── activation/            # 激活码系统
│   ├── adblock/               # 广告拦截
│   ├── ai/                    # AI 配置管理
│   ├── apkbuilder/            # APK 构建系统 ★
│   ├── crypto/                # 加密系统
│   ├── engine/                # 引擎 + 护盾
│   ├── extension/             # 扩展模块系统
│   ├── isolation/             # 隐私隔离 (指纹/UA/Canvas)
│   ├── shell/                 # Shell 模式管理
│   ├── nodejs/                # Node.js 运行时
│   ├── php/                   # PHP 运行时
│   ├── python/                # Python 运行时
│   ├── golang/                # Go 运行时
│   ├── wordpress/             # WordPress 支持
│   ├── frontend/              # 前端项目检测
│   ├── billing/               # Google Play 支付
│   ├── cloud/                 # 云同步 + 社区 API（评分分布/评论排序/模块市场/精选推荐）
│   ├── auth/                  # 用户认证
│   ├── port/                  # 端口管理
│   ├── notification/          # 通知轮询
│   ├── errorpage/             # 错误页
│   ├── i18n/                  # 国际化字符串
│   └── ...                    # 更多子模块
├── ui/                        # UI 层
│   ├── screens/               # 页面 (HomeScreen, SettingsScreen...)
│   ├── components/            # 可复用组件
│   ├── shell/                 # Shell 模式 UI
│   ├── viewmodel/             # ViewModel
│   ├── data/                  # 数据层
│   │   ├── model/             # 数据模型 (WebApp, AppType...)
│   │   ├── database/          # Room 数据库
│   │   ├── dao/               # DAO
│   │   ├── repository/        # Repository
│   │   └── converter/         # TypeConverter
│   ├── theme/                 # 主题
│   ├── navigation/            # 导航
│   └── webview/               # WebView 组件
└── util/                      # 工具类
    ├── GsonProvider.kt
    ├── AppUpdateChecker.kt
    ├── IconStorage.kt
    └── ...
```
