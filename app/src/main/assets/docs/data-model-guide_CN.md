---
title: 数据模型文档
description: WebApp 核心数据模型、嵌套配置类、枚举类型、字段依赖关系、序列化机制
---

# 数据模型文档

## 一、概述

核心数据模型定义在 `ui/data/model/WebApp.kt`（~1400 行），包含 1 个 Room Entity、30+ 个 data class、20+ 个枚举。这是整个应用的数据骨架，编辑器 UI、APK 构建、Shell 运行时都依赖它。

**关键文件**：
- `ui/data/model/WebApp.kt` — 主模型文件
- `ui/data/converter/Converters.kt` — Room TypeConverter + Gson 配置
- `ui/data/dao/WebAppDao.kt` — Room DAO
- `ui/data/database/AppDatabase.kt` — Room 数据库定义

---

## 二、WebApp（核心 Entity）

```kotlin
@Entity(tableName = "web_apps", indices = [updatedAt, categoryId, isActivated])
@TypeConverters(Converters::class)
@Stable
data class WebApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // 必填
    val url: String,                     // 必填
    val iconPath: String? = null,
    val packageName: String? = null,
    val appType: AppType = AppType.WEB,
    // ... 30+ 可选配置字段
)
```

### 2.1 字段分组

| 分组 | 字段 | 默认值 | 说明 |
|------|------|--------|------|
| **基础** | `name`, `url` | 无（必填） | 应用名和目标 URL |
| **基础** | `iconPath`, `packageName` | null | 图标路径、包名 |
| **类型** | `appType` | `WEB` | 应用类型（见 AppType 枚举） |
| **运行时配置** | `mediaConfig`, `galleryConfig`, `htmlConfig`, `wordpressConfig`, `nodejsConfig`, `phpAppConfig`, `pythonAppConfig`, `goAppConfig`, `multiWebConfig` | null | 按 appType 选用 |
| **激活** | `activationEnabled`, `activationCodes`, `activationCodeList`, `activationRequireEveryTime`, `isActivated` | false/empty | 激活码系统 |
| **广告** | `adsEnabled`, `adConfig` | false/null | AdMob 广告 |
| **公告** | `announcementEnabled`, `announcement` | false/null | 应用内公告 |
| **广告拦截** | `adBlockEnabled`, `adBlockRules` | false/empty | 广告拦截规则 |
| **WebView** | `webViewConfig` | `WebViewConfig()` | WebView 行为配置 |
| **闪屏** | `splashEnabled`, `splashConfig` | false/null | 启动闪屏 |
| **BGM** | `bgmEnabled`, `bgmConfig` | false/null | 背景音乐 |
| **APK 导出** | `apkExportConfig` | null | 导出配置（加密/加固/架构等） |
| **主题** | `themeType` | `"AURORA"` | 主题名称 |
| **翻译** | `translateEnabled`, `translateConfig` | false/null | 网页翻译 |
| **扩展** | `extensionEnabled`, `extensionModuleIds`, `extensionFabIcon` | false/empty/null | 浏览器扩展模块 |
| **高级** | `autoStartConfig`, `forcedRunConfig`, `blackTechConfig`, `disguiseConfig`, `browserDisguiseConfig`, `deviceDisguiseConfig`, `activationDialogConfig` | null | 高级功能 |
| **分类/云** | `categoryId`, `cloudConfig` | null | 分类和云同步 |
| **时间戳** | `createdAt`, `updatedAt` | 当前时间 | Room 索引字段 |

### 2.2 appType 与运行时配置的对应关系

```
AppType.WEB          → url (WebView 加载远程网页)
AppType.IMAGE        → mediaConfig (单图片查看器)
AppType.VIDEO        → mediaConfig (单视频播放器)
AppType.HTML         → htmlConfig (本地 HTML 项目)
AppType.GALLERY      → galleryConfig (图片/视频画廊)
AppType.FRONTEND     → htmlConfig (前端项目，含 projectDir)
AppType.WORDPRESS    → wordpressConfig (WordPress + PHP)
AppType.NODEJS_APP   → nodejsConfig (Node.js 后端)
AppType.PHP_APP      → phpAppConfig (PHP 应用)
AppType.PYTHON_APP   → pythonAppConfig (Python 应用)
AppType.GO_APP       → goAppConfig (Go 应用)
AppType.MULTI_WEB    → multiWebConfig (多站点聚合)
```

**注意**：每个 appType 只使用对应的运行时配置，其他配置字段为 null/默认值。

---

## 三、AppType 枚举

```kotlin
enum class AppType {
    WEB, IMAGE, VIDEO, HTML, GALLERY, FRONTEND,
    WORDPRESS, NODEJS_APP, PHP_APP, PYTHON_APP, GO_APP, MULTI_WEB
}
```

| 值 | 类型 | Shell 行为 |
|----|------|-----------|
| `WEB` | 远程网页 | WebView.loadUrl(url) |
| `IMAGE` | 本地图片 | ImageView 全屏显示 |
| `VIDEO` | 本地视频 | VideoView 全屏播放 |
| `HTML` | 本地 HTML | WebView.loadUrl(file:///android_asset/...) |
| `GALLERY` | 图库 | 自定义 Gallery Activity |
| `FRONTEND` | 前端项目 | Node.js 构建 → WebView 加载 |
| `WORDPRESS` | WordPress | PHP + WordPress 运行时 |
| `NODEJS_APP` | Node.js | Node 运行时启动 HTTP 服务 |
| `PHP_APP` | PHP 应用 | PHP 运行时启动 HTTP 服务 |
| `PYTHON_APP` | Python 应用 | Python 运行时启动 HTTP 服务 |
| `GO_APP` | Go 应用 | Go 二进制启动 HTTP 服务 |
| `MULTI_WEB` | 多站点 | Tab/Cards 切换多个 WebView |

---

## 四、WebViewConfig

最复杂的嵌套配置（~70 个字段），控制 WebView 的所有行为：

### 4.1 核心字段

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `javaScriptEnabled` | true | JS 开关 |
| `domStorageEnabled` | true | DOM 存储 |
| `userAgentMode` | DEFAULT | UA 伪装模式 |
| `customUserAgent` | null | 自定义 UA 字符串 |
| `desktopMode` | false | [已废弃] 用 orientationMode |
| `orientationMode` | PORTRAIT | 屏幕方向 |
| `hideToolbar` | false | 隐藏工具栏 |
| `landscapeMode` | false | [已废弃] 用 orientationMode |

### 4.2 安全字段

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `allowFileAccess` | false | 文件访问 |
| `allowFileAccessFromFileURLs` | false | ⚠️ 仅 HTML/FRONTEND 可为 true |
| `allowUniversalAccessFromFileURLs` | false | ⚠️ 同上 |
| `disableShields` | true | 禁用护盾（默认关闭） |

### 4.3 UI 字段

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `statusBarColorMode` | THEME | 状态栏颜色模式 |
| `longPressMenuStyle` | FULL | 长按菜单样式 |
| `screenAwakeMode` | OFF | 屏幕常亮 |
| `keyboardAdjustMode` | RESIZE | 键盘调整模式 |
| `viewportMode` | DEFAULT | 视口模式 |
| `newWindowBehavior` | SAME_WINDOW | window.open 行为 |

### 4.4 代理字段

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `proxyMode` | "NONE" | 代理模式 |
| `proxyHost`, `proxyPort` | "", 0 | 代理地址 |
| `proxyType` | "HTTP" | 代理类型 |
| `pacUrl` | "" | PAC 自动配置 URL |

---

## 五、ApkExportConfig

导出 APK 时的配置，包含多个子配置：

```kotlin
data class ApkExportConfig(
    val customPackageName: String? = null,       // 自定义包名
    val customVersionName: String? = null,        // 自定义版本名
    val customVersionCode: Int? = null,           // 自定义版本号
    val architecture: ApkArchitecture = UNIVERSAL, // CPU 架构
    val runtimePermissions: ApkRuntimePermissions, // 运行时权限
    val encryptionConfig: ApkEncryptionConfig,      // 加密配置
    val hardeningConfig: AppHardeningConfig,        // 加固配置
    val isolationConfig: IsolationConfig,           // 隐私隔离
    val backgroundRunEnabled: Boolean = false,      // 后台运行
    val backgroundRunConfig: BackgroundRunExportConfig,
    val engineType: String = "SYSTEM_WEBVIEW",      // 引擎类型
    val deepLinkEnabled: Boolean = false,           // Deep Link
    val customDeepLinkHosts: List<String>,          // Deep Link 域名
    val performanceOptimization: Boolean = false,   // 性能优化
    val performanceConfig: PerformanceOptimizationConfig,
    val notificationEnabled: Boolean = false,       // 通知
    val notificationConfig: NotificationExportConfig
)
```

### 5.1 隐含依赖关系（⚠️ 重要）

ApkBuilder 在构建时会自动注入权限，以下功能开关会触发自动权限联动：

| 功能开关 | 自动注入的权限 |
|---------|--------------|
| `backgroundRunEnabled = true` | FOREGROUND_SERVICE + SPECIAL_USE + WAKE_LOCK + POST_NOTIFICATIONS + REQUEST_IGNORE_BATTERY_OPTIMIZATIONS |
| `notificationEnabled = true` | POST_NOTIFICATIONS + FOREGROUND_SERVICE + SPECIAL_USE |
| `autoStartConfig.bootStartEnabled = true` | RECEIVE_BOOT_COMPLETED |
| `floatingWindowConfig.enabled = true` | SYSTEM_ALERT_WINDOW |
| `forcedRunConfig` 启用 | FOREGROUND_SERVICE + SPECIAL_USE + WAKE_LOCK |

**开发者注意**：不要手动添加这些权限到 `runtimePermissions`，ApkBuilder 会自动处理。

---

## 六、ApkEncryptionConfig

### 6.1 字段

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | false | 总开关 |
| `encryptConfig` | true | 加密 app_config.json |
| `encryptHtml` | true | 加密 HTML 文件 |
| `encryptMedia` | false | 加密媒体文件 |
| `encryptSplash` | false | 加密闪屏 |
| `encryptBgm` | false | 加密背景音乐 |
| `customPassword` | null | 自定义密码（null=签名派生） |
| `enableIntegrityCheck` | true | 完整性校验 |
| `enableAntiDebug` | true | 反调试 |
| `enableAntiTamper` | true | 反篡改 |
| `obfuscateStrings` | false | 字符串混淆 |
| `encryptionLevel` | STANDARD | 加密强度 |

### 6.2 EncryptionLevel

| 级别 | PBKDF2 迭代次数 | 适用场景 |
|------|----------------|---------|
| FAST | 5,000 | 开发测试 |
| STANDARD | 10,000 | 一般应用 |
| HIGH | 50,000 | 敏感数据 |
| PARANOID | 100,000 | 最高安全 |

### 6.3 预设配置

| 预设 | 说明 |
|------|------|
| `DISABLED` | 不加密 |
| `BASIC` | 加密 config + html，完整性校验，STANDARD 级别 |
| `FULL` | 全部加密 + 全部安全保护，HIGH 级别 |
| `MAXIMUM` | 全部加密 + 字符串混淆，PARANOID 级别 |

### 6.4 toEncryptionConfig() 转换

`ApkEncryptionConfig` 是 UI 层数据模型，通过 `toEncryptionConfig()` 转换为 `core.crypto.EncryptionConfig` 供 AssetEncryptor 使用。转换时：
- `EncryptionLevel` 枚举值一一映射
- `enableRootDetection` / `enableEmulatorDetection` 在 UI 模型中默认 false
- `enableRuntimeProtection` = `enableIntegrityCheck || enableAntiDebug || enableAntiTamper`

---

## 七、AppHardeningConfig

最复杂的配置之一（~50 个字段），控制 APK 加固策略：

### 7.1 分组

| 分组 | 字段数 | 说明 |
|------|--------|------|
| DEX 保护 | 4 | dexEncryption, dexSplitting, dexVmp, dexControlFlowFlattening |
| Native SO 保护 | 4 | soEncryption, soElfObfuscation, soSymbolStrip, soAntiDump |
| 反逆向 | 6 | antiDebugMultiLayer, antiFridaAdvanced, antiXposedDeep, antiMagiskDetect, antiMemoryDump, antiScreenCapture |
| 环境检测 | 5 | detectEmulatorAdvanced, detectVirtualApp, detectUSBDebugging, detectVPN, detectDeveloperOptions |
| 代码混淆 | 4 | stringEncryption, classNameObfuscation, callIndirection, opaquePredicates |
| RASP | 5 | dexCrcVerify, memoryIntegrity, jniCallValidation, timingCheck, stackTraceFilter |
| 反篡改 | 4 | multiPointSignatureVerify, apkChecksumValidation, resourceIntegrity, certificatePinning |
| 威胁响应 | 4 | responseStrategy, responseDelay, enableHoneypot, enableSelfDestruct |

### 7.2 HardeningLevel 预设

| 级别 | DEX | SO | 反逆向 | 混淆 | 威胁响应 |
|------|-----|-----|--------|------|---------|
| BASIC | 加密 | — | 反调试+反Frida | — | — |
| STANDARD | 加密 | 加密+Strip | +反Xposed+虚拟App检测 | 字符串加密 | — |
| ADVANCED | 加密+Split+VMP | 加密+混淆+Strip+AntiDump | +Magisk+内存Dump+USB调试 | 全部混淆 | SILENT_EXIT |
| FORTRESS | 全部 | 全部 | 全部+截屏+模拟器+VPN+开发者选项 | 全部混淆 | CRASH_RANDOM+蜜罐+自毁 |

### 7.3 ThreatResponse

| 值 | 行为 |
|----|------|
| LOG_ONLY | 仅记录日志 |
| SILENT_EXIT | 静默退出 |
| CRASH_RANDOM | 随机崩溃 |
| DATA_WIPE | 数据擦除 |
| FAKE_DATA | 返回假数据 |

---

## 八、ApkArchitecture

```kotlin
enum class ApkArchitecture(val abiFilters: List<String>) {
    UNIVERSAL(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")),
    ARM64(listOf("arm64-v8a", "x86_64")),
    ARM32(listOf("armeabi-v7a", "x86"));
}
```

| 架构 | 包含 ABI | 适用设备 |
|------|---------|---------|
| UNIVERSAL | 全部 4 个 | 所有设备（体积最大） |
| ARM64 | arm64-v8a + x86_64 | 现代手机（2018+） |
| ARM32 | armeabi-v7a + x86 | 旧设备 |

**注意**：`fromName()` 精确匹配枚举名称（区分大小写），未匹配则回退 UNIVERSAL。

---

## 九、运行时配置类

### 9.1 NodeJsConfig

| 字段 | 说明 |
|------|------|
| `projectId` | 项目目录 ID |
| `sourceProjectPath` | 源项目路径（可选，用于同步） |
| `framework` | 框架名（express, koa 等） |
| `buildMode` | STATIC / SSR / API_BACKEND / FULLSTACK |
| `entryFile` | 入口文件（默认 index.js） |
| `serverPort` | 服务端口（0=自动分配） |
| `envVars` | 环境变量 |

### 9.2 PythonAppConfig

| 字段 | 说明 |
|------|------|
| `entryFile` | 入口文件（默认 app.py） |
| `serverType` | 服务器类型（builtin / gunicorn 等） |
| `requirementsFile` | 依赖文件（默认 requirements.txt） |
| `hasPipDeps` | 是否有 pip 依赖 |

**特殊**：构建时自动预安装依赖到 `.pypackages/`，并嵌入 `sitecustomize.py` 修复 Android 运行时问题。

### 9.3 WordPressConfig / PhpAppConfig / GoAppConfig

结构类似，均包含 `projectId`、`framework`、`serverPort`/`phpPort`、`envVars` 等字段。

---

## 十、GalleryConfig

图库配置，包含分类、排序、播放模式：

```kotlin
data class GalleryConfig(
    val items: List<GalleryItem>,       // 媒体项列表
    val categories: List<GalleryCategory>, // 分类
    val playMode: GalleryPlayMode,       // SEQUENTIAL / SHUFFLE / SINGLE_LOOP
    val defaultView: GalleryViewMode,    // GRID / LIST / TIMELINE
    val sortOrder: GallerySortOrder,     // CUSTOM / NAME_* / DATE_* / TYPE
    // ... 更多 UI 配置
)
```

计算属性：
- `imageCount` / `videoCount` / `totalCount` — 统计
- `getItemsByCategory(categoryId)` — 按分类过滤
- `getSortedItems(categoryId)` — 排序后返回

---

## 十一、序列化机制

### 11.1 Room 持久化

- `WebApp` 是 Room Entity，复杂字段（List、嵌套 data class）通过 `TypeConverters` 转为 JSON 字符串存储
- `Converters.kt` 使用 Gson 实现转换
- 索引：`updatedAt`、`categoryId`、`isActivated`

### 11.2 APK 配置序列化

编辑器 → APK 构建的数据转换链：

```
WebApp (UI 模型)
  → toApkConfigWithModules() → ApkConfig (构建模型)
  → Gson.toJson() → app_config.json (嵌入 APK assets)

Shell 运行时:
  app_config.json → Gson.fromJson() → ShellConfig (运行时模型)
```

### 11.3 前后向兼容

`ManifestUtils.fromManifestJson()` 使用 `mergeMissingDefaults` 机制：
- 解析旧版本 JSON 时，用默认值填充新增字段
- 确保旧版本导出的 APK 配置在新版本中仍可正常加载

---

## 十二、UserAgentMode

WebView UA 伪装，支持 10 种预设 + 自定义：

| 模式 | 伪装目标 |
|------|---------|
| DEFAULT | Android WebView 默认 |
| CHROME_MOBILE | Chrome Android |
| CHROME_DESKTOP | Chrome Windows |
| SAFARI_MOBILE | Safari iOS |
| SAFARI_DESKTOP | Safari macOS |
| FIREFOX_MOBILE | Firefox Android |
| FIREFOX_DESKTOP | Firefox Windows |
| EDGE_MOBILE | Edge Android |
| EDGE_DESKTOP | Edge Windows |
| CUSTOM | 自定义字符串 |

版本号集中在 `UserAgentVersions` 对象中管理（Chrome 131, Firefox 133, Safari 18）。

---

## 十三、通知配置

```kotlin
enum class NotificationType(val key: String) {
    NONE("none"), WEB_API("web_api"), POLLING("polling")
}

data class NotificationExportConfig(
    val type: NotificationType = NONE,
    val pollUrl: String = "",             // 轮询 URL
    val pollIntervalMinutes: Int = 15,    // 轮询间隔（≥5分钟）
    val pollMethod: String = "GET",       // GET/POST
    val pollHeaders: String = "",         // 自定义 Headers (JSON)
    val clickUrl: String = ""             // 点击跳转路径
)
```

- **WEB_API**：零配置，WebView Notification polyfill 自动生效
- **POLLING**：配置轮询 URL + 间隔，后台前台服务定时请求，解析 JSON 弹通知
