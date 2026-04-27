---
title: Shell 模块构建与维护指南
description: Shell 模块源码同步机制、白名单策略、覆盖文件维护、常见问题排查
---

# Shell 模块构建与维护指南

## 一、核心架构

Shell 模块（`:shell`）是导出 APK 的模板工程。它**不维护独立源码**，而是通过 Gradle Sync 任务从 `:app` 模块复制运行时所需的源码，排除编辑器专用代码。

```
app/src/main/java/          ← 原始源码（编辑器 + 运行时）
        │
        │  syncShellRuntimeSources
        │  (白名单 include + 黑名单 exclude)
        ▼
shell/src/main/java/         ← 同步后的运行时源码（仅运行时）
shell/src/main/java-overrides/ ← Shell 专用覆盖文件（手动维护，不受 Sync 影响）
```

**源码优先级**：`java-overrides/` > `java/`（Gradle sourceSets 配置）

---

## 二、syncShellRuntimeSources 任务

### 2.1 任务定义位置

`shell/build.gradle.kts` 中的 `val syncShellRuntimeSources by tasks.registering(Sync::class)`

### 2.2 工作流程

1. 从 `../app/src/main/java` 读取源码
2. 按 **include 白名单** 筛选需要的包/文件
3. 按 **exclude 黑名单** 从白名单结果中移除编辑器专用文件
4. 输出到 `shell/src/main/java`
5. 编译任务自动依赖此 Sync 任务

### 2.3 白名单（include）— 当前完整列表

```kotlin
include(
    // ═══ Shell UI ═══
    "**/ui/shell/**",           // ShellActivity, ShellScreen, ShellDialogs 等
    "**/ui/theme/**",           // ShellTheme, 主题相关
    "**/ui/shared/**",          // WindowHelper 等共享 UI 工具

    // ═══ Shell 核心运行时 ═══
    "**/core/shell/**",         // ShellModeManager, ShellConfig, ShellLogger
    "**/core/activation/**",   // ActivationManager（激活码验证）
    "**/core/announcement/**", // AnnouncementManager（公告弹窗）
    "**/core/adblock/**",      // AdBlocker（广告拦截）
    "**/core/webview/**",      // WebViewManager, WebViewPool, 各种 Bridge
    "**/core/crypto/**",       // AssetDecryptor, AesCryptoEngine（运行时解密）
    "**/core/i18n/**",         // Strings（三语国际化）
    "**/core/logging/**",      // AppLogger
    "**/core/dns/**",          // DnsManager, DoH（DNS-over-HTTPS）
    "**/core/forcedrun/**",    // ForcedRunManager（强制运行/防退出）
    "**/core/floatingwindow/**",// FloatingWindowService（悬浮窗）
    "**/core/isolation/**",    // IsolationManager（独立环境/多开）
    "**/core/disguise/**",     // DisguiseEngine（伪装/反指纹）
    "**/core/blacktech/**",    // BlackTechEngine（黑科技功能）
    "**/core/perf/**",         // SystemPerfOptimizer（性能优化）
    "**/core/port/**",         // PortManager（端口管理）
    "**/core/extension/**",    // ExtensionManager（扩展模块加载）
    "**/core/gecko/**",        // GeckoViewEngine（Firefox 内核）
    "**/core/notification/**", // NotificationPollingService（轮询通知）
    "**/core/backgroundrun/**",// BackgroundRunService（后台运行）
    "**/core/translate/**",    // TranslateBridge（网页翻译）
    "**/core/bgm/**",          // 背景音乐播放器
    "**/core/engine/**",       // BrowserEngine 抽象层 + Shields
    "**/core/hardening/**",    // AppHardeningEngine（加固）
    "**/core/scraper/**",      // WebsiteScraper（网页抓取）
    "**/core/script/**",       // UserScriptStorage（用户脚本）
    "**/core/ads/**",          // AdManager（广告管理）
    "**/core/network/**",      // NetworkModule（OkHttp 配置）
    "**/core/errorpage/**",    // 自定义错误页面
    "**/core/golang/**",       // GoRuntime（Go 服务器模式）
    "**/core/python/**",       // PythonRuntime（Python 服务器模式）
    "**/core/nodejs/**",       // NodeJsRuntime（Node.js 服务器模式）
    "**/core/php/**",          // PhpRuntime（PHP 服务器模式）
    "**/core/wordpress/**",    // WordPressRuntime（WordPress 服务器模式）
    "**/core/autostart/**",    // AutoStartManager（开机/定时自启动）
    "**/core/background/**",   // BackgroundRunService（后台运行）
    "**/core/linux/**",        // LinuxEnvironmentManager, NodeProjectBuilder
    "**/core/download/**",     // DependencyDownloadEngine（WordPress 依赖下载）
    "**/core/sample/**",       // SampleProjectExtractor（WordPress 示例提取）
    "**/core/frontend/**",     // FrontendFramework（Node.js 前端框架）
    "**/core/kernel/**",       // BrowserKernel（WebViewManager/OAuth 依赖）

    // ═══ 数据模型 ═══
    "com/webtoapp/data/model/**",  // WebApp, 各种 Config data class
    "**/ui/data/model/**",         // 物理路径在 ui/data/model，包名是 data.model
    "**/ui/data/converter/**",     // Converters（Gson 序列化 + Room TypeConverter）

    // ═══ UI 组件（仅 Shell 运行时需要的）═══
    "**/ui/components/announcement/AnnouncementTemplates.kt",
    "**/ui/components/PremiumComponents.kt",
    "**/ui/components/PermissionRationale.kt",
    "**/ui/components/ThemeSelector.kt",
    "**/ui/components/StatusBarPreview.kt",
    "**/ui/components/EdgeSwipeRefreshLayout.kt",
    "**/ui/components/VirtualNavigationBar.kt",
    "**/ui/components/StatusBarBackground.kt",
    "**/ui/components/LongPressMenu.kt",
    "**/ui/components/ForcedRunCountdownOverlay.kt",

    // ═══ 工具类 ═══
    "**/util/**"
)
```

### 2.4 黑名单（exclude）— 当前完整列表

```kotlin
exclude(
    // WebToAppApplication — 使用 shell 专用版本
    "**/WebToAppApplication.kt",
    // crypto 中的编辑器专用文件
    "**/core/crypto/EncryptedApkBuilder.kt",
    "**/core/crypto/SecurityInitializer.kt",
    // extension 中的编辑器专用文件
    "**/core/extension/AiModuleDeveloper.kt",
    "**/core/extension/agent/**",
    // autostart 中引用 WebViewActivity 的文件（使用 shell 专用覆盖）
    "**/core/autostart/AutoStartLauncher.kt",
    "**/core/autostart/BootReceiver.kt",
    "**/core/autostart/ScheduledStartReceiver.kt",
    // util 中的编辑器专用工具
    "**/util/AppUpdateChecker.kt",
    "**/util/FaviconFetcher.kt",
    "**/util/UrlMetadataFetcher.kt",
    "**/util/MediaStorage.kt",
    "**/util/ZipProjectImporter.kt",
    "**/util/HtmlProjectHelper.kt",
    "**/util/OfflineManager.kt"
)
```

---

## 三、Shell 专用覆盖文件

### 3.1 为什么需要覆盖

某些源文件**编译期必须存在**（被其他同步代码引用），但其原始版本 import 了编辑器专用类（如 `WebViewActivity`、`BillingManager`、`AppDatabase`）。Shell 覆盖版提供相同 public API，但移除编辑器依赖。

### 3.2 覆盖文件列表

| 覆盖文件 | 原始文件 | 覆盖原因 |
|----------|---------|---------|
| `java-overrides/.../WebToAppApplication.kt` | `app/.../WebToAppApplication.kt` | 不初始化 Koin/Database/Billing，直接创建运行时单例 |
| `java-overrides/.../ui/splash/SplashLauncherActivity.kt` | `app/.../ui/splash/SplashLauncherActivity.kt` | 仅保留 `ActivationDialog`，不含编辑器 Activity |
| `java-overrides/.../core/autostart/AutoStartLauncher.kt` | `app/.../core/autostart/AutoStartLauncher.kt` | 仅启动 `ShellActivity`，不引用 `WebViewActivity` |
| `java-overrides/.../core/autostart/BootReceiver.kt` | `app/.../core/autostart/BootReceiver.kt` | 仅处理 Shell 模式，不引用 `WebViewActivity` |
| `java-overrides/.../core/autostart/ScheduledStartReceiver.kt` | `app/.../core/autostart/ScheduledStartReceiver.kt` | 仅处理 Shell 模式，不引用 `WebViewActivity` |

### 3.3 覆盖文件的关键约束

1. **包名和类名必须与原版完全一致** — 否则同步代码的 import 无法解析
2. **public API 必须兼容** — companion object 的属性/方法、public 方法签名不能缺失
3. **覆盖文件不会被 Sync 任务删除** — 因为 Sync 只操作 `src/main/java/`，不操作 `src/main/java-overrides/`
4. **覆盖文件必须加入黑名单** — 原版文件必须在 exclude 列表中，否则 Sync 会复制原版到 `src/main/java/`，导致类冲突

### 3.4 WebToAppApplication 覆盖版 API 对照

| API | 原版实现 | Shell 覆盖版实现 |
|-----|---------|----------------|
| `getInstance()` | 返回 Koin 注入的实例 | 返回直接创建的实例 |
| `shellMode` | Koin `by inject()` | `shellModeManagerLocal` |
| `activation` | Koin `by inject()` | `shellActivationManager` |
| `announcement` | Koin `by inject()` | `shellAnnouncementManager` |
| `adBlock` | Koin `by inject()` | `shellAdBlocker` |
| `repository` | Koin `by inject()` | ❌ 不提供（Shell 不需要） |
| `categoryRepository` | Koin `by inject()` | ❌ 不提供（Shell 不需要） |
| `database` | Koin `by inject()` | ❌ 不提供（Shell 不需要） |
| `billingManager` | Koin `by inject()` | ❌ 不提供（Shell 不需要） |

---

## 四、构建配置

### 4.1 KSP / Room

Shell 模块**不使用 KSP**。Room 注解（`@Entity`、`@TypeConverter`）保留在源码中但不被处理，编译期不会报错，运行时也不会初始化 Room 数据库。

### 4.2 R8 代码收缩

```kotlin
// shell/build.gradle.kts
buildTypes.release {
    isMinifyEnabled = true      // R8 代码收缩
    isShrinkResources = true    // 资源收缩
}
```

R8 会自动移除从 Manifest 组件不可达的编辑器代码（如 `BillingManager`、`AppDatabase`、`WebAppRepository`），即使它们被编译进了 dex。

### 4.3 Proguard 规则

```proguard
# 保留所有 com.webtoapp 类（项目开源，禁用混淆，仅防 R8 误删）
-keep class com.webtoapp.** { *; }
-keepclassmembers class com.webtoapp.** { *; }

# 不需要的第三方库仅保留 dontwarn
-dontwarn org.koin.**
-dontwarn com.android.apksig.**
```

### 4.4 BuildConfig

```kotlin
buildConfigField("boolean", "SHELL_RUNTIME_ONLY", "true")
```

原版 `WebToAppApplication` 使用此字段判断是否走 Shell 分支。Shell 覆盖版不检查此字段（因为覆盖版本身就是 Shell 专用）。

---

## 五、.gitignore 配置

同步生成的源码不应提交到 Git。`.gitignore` 必须覆盖所有同步目录：

```gitignore
# 同步的源码（由 syncShellRuntimeSources 任务生成）
/src/main/java/com/webtoapp/data/
/src/main/java/com/webtoapp/di/
/src/main/java/com/webtoapp/core/activation/
/src/main/java/com/webtoapp/core/adblock/
# ... 所有 core/* 包 ...
/src/main/java/com/webtoapp/ui/
/src/main/java/com/webtoapp/util/

# 保留手动创建的 shell 专用覆盖文件
!/src/main/java-overrides/
```

**重要**：每次在白名单中新增包时，必须同步更新 `.gitignore`。

---

## 六、AndroidManifest.xml

Shell Manifest 预声明了所有运行时需要的组件：

| 组件 | 类名 | 说明 |
|------|------|------|
| Activity | `.ui.shell.ShellActivity` | 主 Activity（LAUNCHER） |
| Service | `.core.background.BackgroundRunService` | 后台运行前台服务 |
| Service | `.core.notification.NotificationPollingService` | 轮询通知前台服务 |
| Service | `.core.floatingwindow.FloatingWindowService` | 悬浮窗前台服务 |
| Service | `.core.forcedrun.ForcedRunGuardService` | 强制运行守护服务 |
| Service | `.core.forcedrun.ForcedRunAccessibilityService` | 强制运行无障碍服务 |
| Receiver | `.core.autostart.BootReceiver` | 开机/时间变更广播 |
| Receiver | `.core.autostart.ScheduledStartReceiver` | 定时启动广播 |
| Receiver | `.core.forcedrun.ForcedRunReceiver` | 强制运行广播 |
| Provider | `FileProvider` | 文件分享 |

---

## 七、依赖管理

Shell 模块的依赖与 app 模块**完全一致**。R8 会在 release 构建中裁剪掉未使用的依赖。

**不需要的依赖**（R8 自动移除）：
- `billing-ktx` — Shell 不初始化 BillingManager
- `room-compiler` (KSP) — Shell 不运行 KSP
- `koin-*` — Shell 不初始化 Koin

**编译期需要但运行时裁剪的依赖**：
- `room-runtime` / `room-ktx` — `WebApp.kt` 的 `@Entity` 注解需要
- `koin-android` / `koin-androidx-compose` — `appModules` 等 Koin 模块定义引用
- `billing-ktx` — `BillingManager` 类定义被同步代码引用

---

## 八、维护操作手册

### 8.1 在 app 模块新增运行时功能

**场景**：在 `app` 模块新增了一个 `core/xxx/` 包，Shell 运行时也需要使用。

**步骤**：

1. **添加白名单** — 在 `shell/build.gradle.kts` 的 `include(...)` 中添加：
   ```kotlin
   "**/core/xxx/**",    // XxxManager（功能说明）
   ```

2. **更新 .gitignore** — 在 `shell/.gitignore` 中添加：
   ```gitignore
   /src/main/java/com/webtoapp/core/xxx/
   ```

3. **构建验证**：
   ```bash
   ./gradlew :shell:compileReleaseKotlin
   ```

4. **处理编译错误** — 如果新包引用了编辑器专用类：
   - 方案 A：将引用编辑器代码的文件加入 `exclude` 黑名单
   - 方案 B：在 `src/main/java-overrides/` 创建 Shell 专用覆盖

5. **最终验证**：
   ```bash
   ./gradlew :shell:assembleRelease
   ```

### 8.2 新增 Shell 覆盖文件

**场景**：某个同步文件引用了 `WebViewActivity`、`BillingManager` 等编辑器专用类。

**步骤**：

1. **创建覆盖文件** — 在 `src/main/java-overrides/` 下创建同包名同类名的文件：
   ```
   src/main/java-overrides/com/webtoapp/core/xxx/XxxClass.kt
   ```

2. **确保 API 兼容** — 覆盖版必须提供原版的所有 public API（companion object 属性、public 方法）

3. **添加黑名单** — 在 `shell/build.gradle.kts` 的 `exclude(...)` 中添加原版文件：
   ```kotlin
   "**/core/xxx/XxxClass.kt",
   ```

4. **不需要更新 .gitignore** — `java-overrides/` 目录已被 `.gitignore` 的 `!` 规则排除

5. **构建验证**：
   ```bash
   ./gradlew :shell:compileReleaseKotlin
   ```

### 8.3 排查编译错误

**典型错误**：`Unresolved reference: XxxClass`

**排查流程**：

```
1. 确认 XxxClass 在哪个包中
   → grep -r "class XxxClass" app/src/main/java/

2. 检查该包是否在白名单中
   → grep "core/xxx/" shell/build.gradle.kts

3. 如果不在白名单 → 添加到 include 列表 + .gitignore

4. 如果在白名单但文件被 exclude → 检查是否需要创建覆盖文件

5. 重新构建验证
   → ./gradlew :shell:compileReleaseKotlin
```

**典型错误**：`Unresolved reference: WebViewActivity`

**原因**：某个同步文件 import 了编辑器专用类。

**解决**：
1. 找到引用文件：`grep -r "WebViewActivity" shell/src/main/java/`
2. 将该文件加入 exclude 黑名单
3. 在 `java-overrides/` 创建 Shell 专用覆盖

### 8.4 更新覆盖文件（原版 API 变更时）

**场景**：原版 `WebToAppApplication` 新增了 companion object 属性 `val xxx: XxxManager`。

**步骤**：

1. 检查哪些同步代码引用了新属性：
   ```bash
   grep -r "WebToAppApplication.xxx" shell/src/main/java/
   ```

2. 如果有引用 → 在 Shell 覆盖版中添加对应属性：
   ```kotlin
   // shell/src/main/java-overrides/.../WebToAppApplication.kt
   companion object {
       val xxx: XxxManager
           get() = instance.xxxManager ?: error("Shell XxxManager unavailable")
   }
   ```

3. 在 `initShellRuntime()` 中初始化：
   ```kotlin
   private var xxxManager: XxxManager? = null

   private fun initShellRuntime() {
       // ... 现有初始化 ...
       xxxManager = XxxManager(this)
   }
   ```

4. 构建验证

### 8.5 检查 R8 是否正确移除编辑器代码

```bash
# 检查 dex 中是否包含编辑器专用类
unzip -p shell/build/outputs/apk/release/shell-release.apk classes.dex | \
  strings | grep -E "com/webtoapp/(billing|data/repository|data/database|di)/"

# 预期结果：无匹配（R8 已移除）
```

---

## 九、APK 体积分析

| 组件 | 大小 | 说明 |
|------|------|------|
| GeckoView native (.so) | ~17 MB | arm64-v8a 的 libgkcodecs/libminidump_analyzer/libnss3 等 |
| GeckoView omni.ja | ~13 MB | Firefox 内置资源 |
| classes.dex | ~13 MB | Kotlin/Java 代码（R8 已收缩） |
| 其他 (res/assets/META-INF) | ~2.5 MB | 资源文件 |

**总计**：~27 MB（压缩后）

**减小体积的方向**：
- 不使用 GeckoView 的配置可排除 GeckoView 依赖（需修改 build.gradle.kts 条件依赖）
- R8 已自动移除所有不可达的编辑器代码

---

## 十、常见问题

### Q1: 为什么不直接用 app APK 作为模板？

app APK 包含编辑器 UI（HomeScreen、Settings 等），体积过大（>50MB），且编辑器代码在运行时无用。Shell 模板通过白名单同步仅保留运行时代码，R8 进一步收缩，最终 APK ~27MB。

### Q2: 为什么不全用 stub（空实现）？

stub 方案虽然体积最小，但无法提供完整运行时功能。白名单 + 覆盖方案在保持小体积的同时保留了所有运行时功能（广告拦截、强制运行、自启动、悬浮窗、通知、翻译、DoH、扩展模块等）。

### Q3: Room 注解在 Shell 中会有问题吗？

不会。`@Entity`、`@TypeConverter` 等注解只是编译时标记。Shell 不运行 KSP，Room 注解处理器不会执行。运行时 Shell 不初始化 `AppDatabase`，这些注解不会被使用。R8 会移除不可达的 Room 相关代码。

### Q4: 新增功能后如何确认 Shell 构建不受影响？

每次修改 `app` 模块后，运行：
```bash
./gradlew :shell:compileReleaseKotlin
```
如果编译失败，按第八节的排查流程处理。

### Q5: 覆盖文件和原版文件可以共存吗？

不可以。同一个类（相同包名+类名）不能同时存在于 `src/main/java/` 和 `src/main/java-overrides/`。原版文件必须加入 exclude 黑名单，确保 Sync 任务不会将其复制到 `src/main/java/`。

---

## 修订历史

| 日期 | 变更 | 作者 |
|------|------|------|
| 2026-04-26 | 初始版本 — 完整构建与维护文档 | Cascade |
