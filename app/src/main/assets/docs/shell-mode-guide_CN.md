---
title: Shell 模式开发指南
description: Shell 模式架构、配置加载、生命周期、子系统初始化、Manifest 合并规则
---

# Shell 模式开发指南

## 一、概述

Shell 模式是 WebToApp 最独特的架构特征。导出的 APK 不包含编辑器 UI，仅包含 WebView 运行时 + 用户配置，由 `app_config.json` 驱动所有行为。

**核心文件**：
- `core/shell/ShellModeManager.kt` — Shell 模式检测与配置加载
- `core/shell/ShellConfig.kt` — Shell 运行时配置模型（位于 `shell/` 模块）
- `ui/shell/ShellActivity.kt` — Shell 模式主 Activity
- `ui/shell/ShellActivityInit.kt` — 子系统初始化
- `core/apkbuilder/ApkTemplate.kt` — APK 配置注入

---

## 二、Shell 模式判定

### 2.1 判定逻辑

```kotlin
// ShellModeManager
fun isShellMode(): Boolean = loadConfig() != null

private fun loadConfig(): ShellConfig? {
    // 1. 尝试 AssetDecryptor 加载 (加密场景)
    // 2. 回退到直接 assets.open() (未加密场景)
    // 3. Gson 解析为 ShellConfig
    // 4. 验证配置有效性
}
```

判定条件：
- `assets/app_config.json` 存在且可解析
- 配置有效性验证通过（见下文）

### 2.2 配置有效性验证

```kotlin
val isValid = when {
    normalizedAppType == "HTML" || normalizedAppType == "FRONTEND" ->
        config.htmlConfig.entryFile.isNotBlank()
    normalizedAppType in listOf("IMAGE", "VIDEO", "GALLERY", "WORDPRESS", ...) ->
        true  // 这些类型总是有效
    else ->
        !config.targetUrl.isNullOrBlank()  // WEB 类型需要 URL
}
```

---

## 三、ShellConfig（运行时配置）

ShellConfig 是 `app_config.json` 的 Kotlin 映射，包含导出 APK 运行所需的全部信息：

```
ShellConfig
├── appName: String                    # 应用名
├── targetUrl: String?                 # 目标 URL (WEB 类型)
├── appType: String                    # 应用类型
├── packageName: String                # 包名
├── versionCode: Int                   # 版本号
├── versionName: String                # 版本名
├── iconPath: String?                  # 图标路径
├── webViewConfig: WebViewShellConfig  # WebView 配置（含 dnsMode + dnsConfig）
├── htmlConfig: HtmlShellConfig?       # HTML 项目配置
├── splashConfig: SplashShellConfig?   # 闪屏配置
├── bgmConfig: BgmShellConfig?         # BGM 配置
├── activationConfig: ActivationShellConfig?  # 激活码配置
├── adBlockEnabled: Boolean            # 广告拦截
├── translateConfig: TranslateShellConfig?    # 翻译配置
├── extensionModuleIds: List<String>   # 扩展模块 ID
├── embeddedExtensionModules: List<ExtensionModule>  # 嵌入的模块
├── encryptionConfig: EncryptionShellConfig?  # 加密配置
├── isolationConfig: IsolationShellConfig?    # 隐私隔离
├── notificationEnabled: Boolean      # 通知开关
├── notificationConfig: NotificationShellConfig?  # 通知配置
├── backgroundRunEnabled: Boolean      # 后台运行
├── backgroundRunConfig: BackgroundRunShellConfig? # 后台运行配置
├── cloudSdkConfig: CloudSdkConfig?    # 云 SDK 配置
├── autoStartEnabled: Boolean          # 开机自启
├── forcedRunEnabled: Boolean          # 强制运行
├── floatingWindowEnabled: Boolean     # 悬浮窗
├── customManifestComponents: List<...> # 自定义 Manifest 组件
└── deepLinkHosts: List<String>        # Deep Link 域名
```

---

## 四、Shell 模式启动流程

### 4.1 Application 初始化

```
WebToAppApplication.onCreate()
  → BuildConfig.SHELL_RUNTIME_ONLY == true
  → initShellRuntime()
    → ShellModeManager(this)       // 直接实例化（无 Koin）
    → ActivationManager(this)
    → AnnouncementManager(this)
    → AdBlocker()
  → return  // 不初始化 Koin、Billing、Cloud 等
```

### 4.2 ShellActivity 启动

```
ShellActivity.onCreate()
  → ShellModeManager.getConfig()
  → ShellActivityInit.initAll(this, config)
    ├── initWebView()              # WebView 初始化
    ├── initSplashScreen()         # 闪屏（可选）
    ├── initActivation()           # 激活码验证（可选）
    ├── initBgm()                  # 背景音乐（可选）
    ├── initAdBlock()              # 广告拦截（可选）
    ├── initTranslate()            # 翻译（可选）
    ├── initExtensions()           # 扩展模块注入
    ├── initNotificationService()  # 通知服务（可选）
    ├── initAutoStart()            # 开机自启配置
    ├── initForcedRun()            # 强制运行配置
    ├── initFloatingWindow()       # 悬浮窗配置
    ├── initSecurityProtection()   # 安全保护
    └── initCloudSdk()             # 云 SDK（可选）
  → WebView.loadUrl(targetUrl)
```

### 4.3 依赖获取方式

Shell 模式不使用 Koin，通过 `WebToAppApplication` 的 companion object 获取：

```kotlin
// Shell 模式中获取依赖
val shellMode = WebToAppApplication.shellMode      // ShellModeManager
val activation = WebToAppApplication.activation    // ActivationManager
val adBlock = WebToAppApplication.adBlock          // AdBlocker
```

这些 getter 已标记 `@Deprecated`，编辑器侧应使用 Koin 注入。

---

## 五、配置注入过程

### 5.1 编辑器 → ApkConfig 转换

```kotlin
// WebApp.toApkConfigWithModules()
fun WebApp.toApkConfigWithModules(packageName: String, context: Context): ApkConfig {
    return ApkConfig(
        appName = name,
        targetUrl = url,
        appType = appType.name,
        packageName = packageName,
        // ... 所有配置字段映射
        embeddedExtensionModules = extensionModuleIds.mapNotNull { 
            ExtensionManager.getInstance(context).getModuleById(it) 
        }
    )
}
```

### 5.2 ApkConfig → app_config.json

```kotlin
// ApkTemplate
val json = gson.toJson(apkConfig)
// 写入 APK 的 assets/app_config.json
```

### 5.3 app_config.json → ShellConfig

```kotlin
// ShellModeManager.loadConfig()
val jsonStr = assetDecryptor.loadAssetAsString("app_config.json")
val config = gson.fromJson(jsonStr, ShellConfig::class.java)
```

---

## 六、加密配置的加载

当 APK 启用了加密，`app_config.json` 被加密为 `app_config.json.enc`：

```
加载流程:
1. AssetDecryptor.loadAssetAsString("app_config.json")
   → 先尝试解密加载 (app_config.json.enc)
   → 失败则直接读取 (app_config.json)

2. 如果需要自定义密码:
   → ShellModeManager.setCustomPassword(password)
   → 清除缓存 → 重新加载
```

### 6.1 密钥派生

```
签名证书 SHA-256 哈希
  + 包名
  + 自定义密码 (可选)
  → PBKDF2 (iterations 由 EncryptionLevel 决定)
  → AES-256 密钥
```

**关键**：打包时和运行时的签名必须一致，否则密钥不匹配，无法解密。

---

## 七、Manifest 合并

### 7.1 ApkBuilder 修改 Manifest

导出 APK 时，ApkBuilder 通过 `AxmlEditor` 修改 `AndroidManifest.xml`：

| 修改项 | 来源 |
|--------|------|
| `package` 属性 | `ApkConfig.packageName` |
| `android:label` | `ApkConfig.appName` |
| `<activity>` 配置 | 横屏/竖屏、主题等 |
| `<service>` 声明 | 通知/后台运行/开机自启 |
| `<receiver>` 声明 | 开机广播接收器 |
| 权限声明 | 自动权限联动 |

### 7.2 自动权限联动

ApkBuilder 根据功能开关自动注入权限（不需要手动添加）：

| 功能 | 注入权限 |
|------|---------|
| 后台运行 | FOREGROUND_SERVICE + SPECIAL_USE + WAKE_LOCK + POST_NOTIFICATIONS + REQUEST_IGNORE_BATTERY_OPTIMIZATIONS |
| 通知 | POST_NOTIFICATIONS + FOREGROUND_SERVICE + SPECIAL_USE |
| 开机自启动 | RECEIVE_BOOT_COMPLETED |
| 悬浮小窗 | SYSTEM_ALERT_WINDOW |
| 强制运行 | FOREGROUND_SERVICE + SPECIAL_USE + WAKE_LOCK |

### 7.3 Shell 模板 Manifest

`:shell` 模块的 `AndroidManifest.xml` 已预声明了 Shell 模式所需的大部分组件：

```xml
<!-- Shell Manifest 预声明 -->
<activity android:name=".ShellActivity" />
<service android:name=".BackgroundRunService" />
<service android:name=".NotificationPollingService" />
<receiver android:name=".BootReceiver">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

## 八、扩展模块嵌入

### 8.1 嵌入方式

导出 APK 时，选中的扩展模块被嵌入 `app_config.json` 的 `embeddedExtensionModules` 字段：

```json
{
  "embeddedExtensionModules": [
    {
      "id": "builtin-dark-mode",
      "name": "Dark Mode",
      "code": "...",
      "runAt": "DOCUMENT_END",
      "urlMatches": []
    }
  ]
}
```

### 8.2 运行时加载

```kotlin
// ShellActivityInit.initExtensions()
val modules = config.embeddedExtensionModules
// 注入到 WebView
```

Shell 模式不使用 `ExtensionManager`（无 Koin），直接从 ShellConfig 读取嵌入的模块。

---

## 九、通知服务

### 9.1 两种通知类型

| 类型 | 实现 | 配置 |
|------|------|------|
| **WEB_API** | WebView Notification polyfill | 零配置，自动生效 |
| **POLLING** | NotificationPollingService | 需配置轮询 URL + 间隔 |

### 9.2 轮询通知

```
NotificationPollingService (前台服务)
  → 定时请求 pollUrl (GET/POST)
  → 解析 JSON 响应:
    { "title": "...", "content": "...", "url": "..." }
  → 弹出 Notification
  → 用户点击 → ShellActivity.onNewIntent() → 打开 clickUrl
```

---

## 十、后台运行

```
BackgroundRunService (前台服务)
  → 显示常驻通知
  → WakeLock 保持 CPU 唤醒 (3h 续期 + 4h 超时)
  → Android 14+ 需要 FOREGROUND_SERVICE_SPECIAL_USE
  → 通知包含"停止"按钮
  → 电池优化白名单引导
```

---

## 十一、开发 Shell 模式新功能的步骤

### 11.1 添加新配置字段

1. **ShellConfig** — 添加字段 + `@SerializedName`
2. **ApkConfig** — 添加对应字段
3. **ApkTemplate** — 添加 JSON 输出映射
4. **WebApp** / **ApkExportConfig** — 添加 UI 层字段
5. **ApkBuilder** — 添加 `WebApp → ApkConfig` 转换映射

### 11.2 添加新子系统初始化

1. 在 `ShellActivityInit.kt` 中添加 `initXxx(config)` 方法
2. 在 `ShellActivity.onCreate()` 中调用
3. 如需权限，在 ApkBuilder 中添加自动权限联动
4. 如需 Service/Receiver，在 Shell Manifest 中声明

### 11.3 注意事项

- Shell 模式无 Koin，不能使用 `by inject()`
- 使用 `WebToAppApplication.xxx` companion getter 获取依赖
- 新增 Manager 类需同时支持 `getInstance(context)` 和 Koin 注册
- Shell APK 体积敏感，避免引入大型依赖
