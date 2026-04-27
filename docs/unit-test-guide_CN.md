# 单元测试指南

> **WebToApp 项目 `app` 模块的单元测试完整文档**，涵盖测试体系、已有测试清单、模块覆盖矩阵、编写规范和运行方法。

---

## 一、测试体系概览

### 1.1 技术栈

| 组件 | 版本/说明 |
|------|----------|
| 测试框架 | JUnit 4 (`junit:junit:4.13.2`) |
| 断言库 | Google Truth (`com.google.common.truth:truth:1.4.2`) |
| Mock 框架 | Mockito + Mockito-Kotlin（部分测试使用） |
| 运行方式 | `./gradlew :app:testDebugUnitTest` — 纯 JVM，无需设备 |
| 测试目录 | `app/src/test/java/com/webtoapp/` |

### 1.2 核心原则

- **只写纯 JVM 单元测试**，不写 Android Instrumented Test（`androidTest` 目录不存在）
- **优先测试纯逻辑类**：data class、enum、object 工具方法、不含 Android Context 的业务逻辑
- **避免测试 UI 层**：Compose/Activity/Fragment 依赖 Android 框架，不适合纯 JVM 测试
- **断言使用 Truth**：`assertThat(actual).isEqualTo(expected)` 风格，可读性优于 JUnit Assert

### 1.3 当前规模

- **65 个测试类**，**502 个测试用例**，**0 失败**
- 覆盖 `core/` 下 16 个子模块 + `util/` + `data/` + `ui/data/` + `core/shell/`

---

## 二、测试清单

### 2.1 激活模块 (`core/activation/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `ActivationCodeTest` | 5 | `fromJson`（有效 JSON、遗留字符串、畸形 JSON）、`fromLegacyString`、`toJson`/`fromJson` 往返 |
| `ActivationCodeExtendedTest` | 15 | `fromJson` 全类型解析（PERMANENT/TIME_LIMITED/USAGE_LIMITED/DEVICE_BOUND/COMBINED）、数组 JSON、前导空格、`toJson` 序列化、`ActivationCodeType` 枚举完整性 |
| `ActivationStatusTest` | 18 | `isValid`（未激活/已激活/已过期/用量超限/用量正常）、`isExpired`、`isUsageExceeded`、`remainingTimeMs`、`remainingUsage` |

### 2.2 计费模块 (`core/billing/`)

> 无独立测试类。`PurchaseState`/`SubscriptionInfo` 为纯数据结构，`BillingManager` 依赖 `BillingClient`（Android 框架），不适合纯 JVM 测试。

### 2.3 黑科技模块 (`core/blacktech/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `BlackTechConfigTest` | 19 | `DISABLED`/`SILENT_MODE`/`ALARM_MODE`/`SOS_SIGNAL`/`NUCLEAR_MODE`/`STEALTH_MODE` 预设验证、`morseSignal`/`hotspotMode`/`customAlarm` 工厂方法、data class copy、默认值 |

### 2.4 伪装模块 (`core/disguise/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `DisguiseConfigTest` | 33 | `DISABLED`/`MULTI_ICON_3`/`MULTI_ICON_5`/`SUBTLE_FLOOD`/`ICON_FLOOD`/`ICON_STORM`/`EXTREME`/`RESEARCH` 预设、`custom`/`fromMode` 工厂、`IconStormMode` 枚举及 `suggestedCount` 单调性、`getAliasCount`、`assessImpactLevel`（6 级边界）、`estimateManifestOverhead`、`getEstimatedOverhead`（B/KB/MB 格式） |
| `DeviceDisguiseConfigTest` | 23 | `toUserAgentMode`（8 种设备/OS 组合映射）、`generateUserAgent`（disabled/custom/Android/iOS/Windows）、`requiresDesktopViewport`（5 种场景）、`DeviceBrand.getBrandsForType`（3 种设备类型）、`DeviceType`/`DeviceOS` 枚举完整性 |

### 2.5 引擎模块 (`core/engine/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `EngineTypeTest` | 2 | 枚举值完整性 |
| `EngineFileManagerTest` | 5 | 文件路径管理 |
| `GeckoEngineDownloaderTest` | 5 | Gecko 引擎下载逻辑 |

### 2.6 引擎护盾 (`core/engine/shields/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `HttpsUpgraderTest` | 5 | URL 升级、SSL 错误回退、白名单域名管理 |
| `HttpsUpgraderExtendedTest` | 12 | `tryHttpFallback`（首次回退/重复回退/多域名独立）、`.local`/`.localhost` 域名白名单、`127.0.0.1`/`0.0.0.0`、172.16-31.x 内网 IP、所有 IP 地址跳过、`onSslError` null/非 pending 输入 |
| `TrackerBlockerTest` | 5 | 追踪器拦截规则 |
| `BrowserShieldsTest` | 4 | 浏览器护盾配置 |
| `ReaderModeTest` | 4 | 阅读模式逻辑 |
| `ShieldScriptGeneratorsTest` | 3 | 脚本生成器 |
| `ShieldsStatsTest` | 3 | 护盾统计 |

### 2.7 错误页模块 (`core/errorpage/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `ErrorPageManagerTest` | 6 | 错误页生成与管理 |
| `ErrorPageStylesAndGamesTest` | 4 | 错误页样式与内置小游戏 |

### 2.8 扩展模块 (`core/extension/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `ExtensionModuleTest` | 5 | 模块元数据、`generateExecutableCode`、配置嵌入 |
| `ExtensionManagerTest` | 3 | 模块管理器 |
| `ModulePresetManagerTest` | 4 | 预设模块管理 |

### 2.9 强制运行模块 (`core/forcedrun/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `ForcedRunConfigTest` | 19 | `DISABLED`/`STUDY_MODE`/`FOCUS_MODE`/`KIDS_MODE` 预设验证、`ForcedRunMode`/`ProtectionLevel` 枚举、data class copy |

### 2.10 前端模块 (`core/frontend/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `ProjectDetectorTest` | 5 | 前端项目类型检测 |

### 2.11 国际化模块 (`core/i18n/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `RandomAppNameGeneratorTest` | 2 | 随机应用名生成 |
| `AppLanguageTest` | 2 | 语言枚举 |
| `AiPromptManagerTest` | 4 | AI 提示词管理 |

### 2.12 隔私隔离模块 (`core/isolation/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `FingerprintGeneratorTest` | 4 | 浏览器指纹生成 |
| `IsolationConfigTest` | 3 | 隔离配置 |
| `IsolationScriptInjectorTest` | 4 | 隔离脚本注入逻辑 |

### 2.13 网络模块 (`core/network/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `NetworkModuleTest` | 11 | 网络配置 |
| `UserAgentVersionsTest` | 12 | UA 版本号解析与格式化 |

### 2.14 PWA 模块 (`core/pwa/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `PwaModelsTest` | 16 | `PwaIcon.maxSizePixels`（7 种输入：空/null/any/单尺寸/多尺寸/非正方/不可解析）、`PwaManifest`/`PwaAnalysisResult` 默认值、`PwaDataSource` 枚举、`PwaAnalysisState` 密封类 |
| `PwaAnalyzerTest` | 13 | `extractHost`（https/http/端口/裸域名）、`suggestDeepLinkHosts`（scope/startUrl/www 变体/去重/排序/空结果） |

### 2.15 云服务模块 (`core/shell/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `CloudSdkConfigTest` | 10 | `isValid()`（4 种场景）、`DISABLED` 预设、`getSdkApiUrl`（默认/自定义 baseUrl 含/不含尾斜杠）、默认值验证 |

### 2.16 统计模块 (`core/stats/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `AppUsageStatsTest` | 8 | `formattedTotalUsage`（5 种时长格式化）、`HealthStatus` 枚举、`AppHealthRecord` 默认值、`AppHealthSummary` |
| `BatchImportServiceParseTest` | 14 | URL 解析（单条/管道分隔/空格分隔/多条）、注释与空行跳过、URL 去重、无效输入、`extractName` 域名提取 |

### 2.17 云服务社区模块 (`core/cloud/`)

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|----------|
| `InstalledItemsTrackerTest` | 16 | 版本号解析（正常/畸形/无等号）、版本名解析（正常/含等号/空）、`findEntryForId`（匹配/缺失/空集）、版本比较（高/同/低/零）、集成查找+解析 |
| `AppReviewsResponseTest` | 14 | `ratingDistribution` 默认值/保留/部分数据、fallback 从评论计算分布（正常/空/单条）、`AppReviewItem` 默认值/全字段、版本比较（4 种场景）、`ModuleVersion` 默认值/自定义 |
| `CommentSortLogicTest` | 8 | 按时间排序（最新/最早）、按评分排序（正常/缺失数据/同评分稳定排序）、边界（空列表/单条/null createdAt） |

### 2.18 其他模块

| 测试类 | 所属包 | 用例数 | 覆盖内容 |
|--------|--------|--------|----------|
| `AdBlockerTest` | `core/adblock/` | 7 | 广告拦截规则 |
| `AiConfigManagerTest` | `core/ai/` | 8 | AI 配置管理 |
| `AppResultTest` | `core/common/` | 26 | 通用结果封装 |
| `EncryptionConfigTest` | `core/crypto/` | 5 | `shouldEncrypt`（禁用/按类别/媒体检测）、`getKeyDerivationIterations`、`hasSecurityProtection` |
| `EncryptionConfigExtendedTest` | `core/crypto/` | 9 | `EncryptionLevel` 枚举完整性/单调递增/STANDARD/HIGH 迭代次数、全类别禁用边界、`encryptBgm` 独立测试、`hasSecurityProtection` 逐项测试 |
| `EnhancedCryptoTest` | `core/crypto/` | 6 | 增强加密逻辑 |
| `ExportSecurityRegressionTest` | `core/apkbuilder/` | 4 | 导出安全回归测试 |
| `GoDependencyManagerTest` | `core/golang/` | 3 | Go 依赖管理 |
| `NodeDependencyManagerTest` | `core/nodejs/` | 5 | Node.js 依赖管理 |
| `PythonDependencyManagerTest` | `core/python/` | 4 | Python 依赖管理 |
| `PortManagerTest` | `core/port/` | 6 | 端口管理 |
| `ProcessPortScannerTest` | `core/port/` | 3 | 进程端口扫描 |
| `WebAppModelTest` | `data/model/` | 6 | `getAllActivationCodes`/`getActivationCodeStrings`、`HtmlConfig.getValidEntryFile`、`GalleryConfig`、`ApkArchitecture.fromName`、`ApkEncryptionConfig.toEncryptionConfig` |
| `WebAppModelExtendedTest` | `data/model/` | 16 | `AppType` 枚举完整性（12 值）、`ApkArchitecture` 枚举/abiFilters/fromName、`HtmlConfig` 边界、`GalleryConfig` 空列表、`ActivationCode` 空列表边界、`ApkEncryptionConfig.EncryptionLevel` 迭代次数/DISABLED/BASIC 预设 |
| `WebViewConfigDarkModeTest` | `data/model/` | 12 | Dark mode 状态栏默认值、light/dark 独立性、copy 保留、枚举完整性 |
| `WebViewShellConfigDarkModeTest` | `core/shell/` | 3 | Shell 配置 dark mode 默认值、独立性、copy 保留 |
| `GsonProviderTest` | `util/` | 2 | Gson 单例与序列化 |
| `ProcessCompatTest` | `util/` | 2 | 进程兼容性 |
| `ThreadLocalCompatTest` | `util/` | 3 | ThreadLocal 兼容性 |
| `AppUpdateCheckerTest` | `util/` | 3 | 应用更新检查 |

---

## 三、模块覆盖矩阵

| 模块 | 有测试 | 无测试（不适合纯 JVM） | 说明 |
|------|:------:|:-----------------------:|------|
| `core/activation/` | ✅ | | 纯数据类 + JSON 解析 |
| `core/blacktech/` | ✅ | | 纯配置 data class + 工厂方法 |
| `core/disguise/` | ✅ | | 配置类 + UA 生成 + 设备枚举 |
| `core/engine/` | ✅ | | 引擎类型 + 文件管理 |
| `core/engine/shields/` | ✅ | | URL 升级 + 拦截规则 |
| `core/errorpage/` | ✅ | | 错误页生成 |
| `core/extension/` | ✅ | | 模块元数据 + 代码生成 |
| `core/forcedrun/` | ✅ | | 配置预设 + 枚举 |
| `core/frontend/` | ✅ | | 项目检测 |
| `core/i18n/` | ✅ | | 随机名 + 语言 + AI 提示词 |
| `core/isolation/` | ✅ | | 指纹 + 脚本注入 |
| `core/network/` | ✅ | | UA 版本 + 网络配置 |
| `core/pwa/` | ✅ | | PWA 模型 + URL 解析 |
| `core/shell/` | ✅ | | CloudSdk 配置 |
| `core/stats/` | ✅ | | 使用统计 + 批量导入解析 |
| `core/adblock/` | ✅ | | 广告拦截 |
| `core/ai/` | ✅ | | AI 配置 |
| `core/common/` | ✅ | | 通用结果封装 |
| `core/crypto/` | ✅ | | 加密配置 |
| `core/apkbuilder/` | ✅ | | 安全回归测试 |
| `core/golang/` | ✅ | | Go 依赖 |
| `core/nodejs/` | ✅ | | Node 依赖 |
| `core/python/` | ✅ | | Python 依赖 |
| `core/port/` | ✅ | | 端口管理 |
| `core/billing/` | | ❌ | `BillingManager` 依赖 `BillingClient`（Android 框架） |
| `core/notification/` | | ❌ | `NotificationPollingService` 是 Android 前台服务 |
| `core/scraper/` | | ❌ | 网站抓取引擎依赖 OkHttp + 文件 IO |
| `core/cloud/` | ✅ | | 数据模型 + 解析逻辑 + 排序逻辑（API 调用仍依赖网络，不测试） |
| `ui/` | | ❌ | Compose UI + ViewModel + Activity 依赖 Android 框架 |

---

## 四、编写规范

### 4.1 文件与类命名

```
app/src/test/java/com/webtoapp/<package>/<ClassName>Test.kt
```

- 测试类名 = 被测类名 + `Test` 后缀
- 同包下已有同名测试类时，新测试类命名为 `<ClassName>ExtendedTest` 或按功能命名

### 4.2 测试方法命名

使用反引号描述性命名：

```kotlin
@Test
fun `isValid returns false when disabled`() { ... }
```

**禁止事项**：
- 方法名中不要包含 `.` 字符（Kotlin 编译错误：`Name contains illegal characters`）
- 不要使用 `testXxx` 前缀（反引号已提供描述性）

### 4.3 测试结构模板

```kotlin
package com.webtoapp.<package>

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MyClassTest {

    // ═══════════════════════════════════════════
    // 功能分组标题
    // ═══════════════════════════════════════════

    @Test
    fun `方法名 描述预期行为`() {
        // Arrange
        val input = ...

        // Act
        val result = ...

        // Assert
        assertThat(result).isEqualTo(expected)
    }
}
```

### 4.4 断言选择

| 场景 | 推荐写法 |
|------|---------|
| 相等判断 | `assertThat(actual).isEqualTo(expected)` |
| 布尔判断 | `assertThat(actual).isTrue()` / `isFalse()` |
| 集合大小 | `assertThat(list).hasSize(n)` |
| 集合包含 | `assertThat(list).contains(element)` |
| 精确包含 | `assertThat(list).containsExactly(a, b, c)` |
| Null 判断 | `assertThat(result).isNull()` / `isNotNull()` |
| 数值比较 | `assertThat(value).isGreaterThan(min)` |
| 字符串包含 | `assertThat(str).contains("substr")` |
| 浮点比较 | `assertThat(f).isWithin(tolerance).of(expected)` |

### 4.5 属性 vs 函数调用

Kotlin 中 `val` 是属性（直接访问），`fun` 是函数（需要括号）：

```kotlin
// 属性 — 不加括号
assertThat(config.enabled).isTrue()

// 函数 — 必须加括号
assertThat(config.isValid()).isTrue()
```

混淆会导致编译错误：`Function invocation 'isValid()' expected`

### 4.6 不可继承类的测试策略

Kotlin 类默认 `final`，无法创建匿名子类。对于依赖 Context/Repository 的服务类：

- **方案 A**：提取纯逻辑到顶层函数或 companion object，直接测试
- **方案 B**：在测试中复制核心算法进行验证（如 `BatchImportServiceParseTest`）
- **方案 C**：使用 Mockito mock 依赖后实例化（适用于构造函数注入的类）

---

## 五、运行测试

### 5.1 常用命令

```bash
# 运行全部单元测试
./gradlew :app:testDebugUnitTest

# 运行单个测试类
./gradlew :app:testDebugUnitTest --tests "com.webtoapp.core.activation.ActivationStatusTest"

# 运行匹配模式的测试
./gradlew :app:testDebugUnitTest --tests "*.activation.*"

# 查看测试报告
open app/build/reports/tests/testDebugUnitTest/index.html
```

### 5.2 测试结果位置

| 文件 | 说明 |
|------|------|
| `app/build/test-results/testDebugUnitTest/*.xml` | JUnit XML 结果（CI 解析用） |
| `app/build/reports/tests/testDebugUnitTest/index.html` | HTML 可视化报告 |

### 5.3 快速统计

```bash
# 测试总数
grep -h 'tests=' app/build/test-results/testDebugUnitTest/*.xml | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}'

# 失败数
grep -h 'failures=' app/build/test-results/testDebugUnitTest/*.xml | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}'

# 测试类数量
ls app/build/test-results/testDebugUnitTest/*.xml | wc -l
```

---

## 六、扩展测试的建议方向

以下模块当前无测试，但可通过重构或 Mock 实现可测试性：

| 模块 | 阻碍因素 | 建议方案 |
|------|---------|---------|
| `BillingManager` | 依赖 `BillingClient` | Mock `BillingClient`，测试购买状态流转逻辑 |
| `NotificationPollingService` | Android 前台服务 | 提取轮询逻辑到纯 Kotlin 类，测试 JSON 解析 + 去重 |
| `CloudApiService` | 网络请求 | Mock OkHttp，测试请求构造 + 响应解析 |
| `ScraperEngine` | 文件 IO + 网络 | 提取 HTML 解析逻辑到纯函数，测试 meta 标签提取 |
| `WebAppRepository` | Room 数据库 | Mock DAO，测试数据流转逻辑 |

---

## 修订记录

| 日期 | 修改内容 | 修改者 |
|------|---------|--------|
| 2026-04-24 | 初始创建 | Cascade |
| 2026-04-24 | 新增 `core/cloud/` 3 个测试类（38 用例），更新规模统计 | Cascade |
| 2026-04-28 | PR #61：新增 `WebViewConfigDarkModeTest`（12 用例）+ `WebViewShellConfigDarkModeTest`（3 用例），删除 fork 不兼容测试，更新规模统计（65 类/502 用例） | Cascade |
