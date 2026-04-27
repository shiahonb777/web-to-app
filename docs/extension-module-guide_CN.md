---
title: 扩展模块开发指南
description: 如何开发、测试、分享 WebToApp 浏览器扩展模块
---

# 扩展模块开发指南

## 一、概述

WebToApp 扩展模块系统允许用户为导出的 WebView 应用注入自定义 JavaScript/CSS 代码。类似于油猴脚本（Userscript）和 Chrome 扩展，但专为 WebToApp 的 Shell 模式设计。

**核心文件**：
- `core/extension/ExtensionModule.kt` — 数据模型
- `core/extension/ExtensionManager.kt` — 管理器（单例）
- `core/extension/ModulePreset.kt` — 预设方案
- `core/extension/BuiltInModules.kt` — 内置模块定义

---

## 二、模块数据模型

### 2.1 ExtensionModule 核心字段

```kotlin
data class ExtensionModule(
    val id: String = UUID.randomUUID().toString(),  // 唯一标识
    val name: String,                                // 模块名称（必填）
    val description: String = "",                    // 描述
    val icon: String = "package",                    // 图标 ID
    
    // 分类与标签
    val category: ModuleCategory = OTHER,
    val tags: List<String> = emptyList(),
    
    // 版本与作者
    val version: ModuleVersion = ModuleVersion(),
    val author: ModuleAuthor? = null,
    
    // 代码内容
    val code: String = "",          // JavaScript 代码
    val cssCode: String = "",       // CSS 代码
    val codeFiles: Map<String, String> = emptyMap(),  // 多文件代码
    
    // 执行配置
    val runAt: ModuleRunTime = DOCUMENT_END,  // 执行时机
    val urlMatches: List<UrlMatchRule> = emptyList(),  // URL 匹配规则
    
    // 配置项
    val configItems: List<ModuleConfigItem> = emptyList(),
    val configValues: Map<String, String> = emptyMap(),
    
    // 状态
    val enabled: Boolean = true,
    val builtIn: Boolean = false,
    
    // UI
    val uiConfig: ModuleUiConfig = ModuleUiConfig.DEFAULT,
    val runMode: ModuleRunMode = INTERACTIVE,
    
    // 来源
    val sourceType: ModuleSourceType = CUSTOM
)
```

### 2.2 执行时机 (ModuleRunTime)

| 值 | 等价于 | 说明 |
|----|--------|------|
| `DOCUMENT_START` | `document_start` | DOM 加载前，最早执行 |
| `DOCUMENT_END` | `document_end` | DOM 加载完成（默认） |
| `DOCUMENT_IDLE` | `document_idle` | 空闲时执行，最晚 |

### 2.3 运行模式 (ModuleRunMode)

| 值 | 说明 |
|----|------|
| `INTERACTIVE` | 交互模式：可在 FAB 面板中操作，也可启动独立窗口 |
| `AUTO` | 自动模式：页面加载时自动执行，无 UI 操作界面 |

### 2.4 模块来源 (ModuleSourceType)

| 值 | 说明 |
|----|------|
| `CUSTOM` | 用户自建 / AI 生成 |
| `USERSCRIPT` | 油猴脚本 (.user.js) |
| `CHROME_EXTENSION` | Chrome 扩展 (manifest.json) |

---

## 三、URL 匹配规则

### 3.1 UrlMatchRule

```kotlin
data class UrlMatchRule(
    val pattern: String,       // 匹配模式
    val isRegex: Boolean = false,  // 是否正则
    val exclude: Boolean = false   // 是否排除
)
```

### 3.2 匹配逻辑

```
1. 如果 urlMatches 为空 → 匹配所有 URL
2. 先检查 include 规则（exclude=false），任一匹配则通过
3. 再检查 exclude 规则（exclude=true），任一匹配则排除
4. glob 模式: * 匹配任意字符，? 匹配单个字符
5. 正则模式: isRegex=true 时 pattern 作为正则表达式
```

### 3.3 示例

```kotlin
// 仅匹配 GitHub
urlMatches = listOf(UrlMatchRule(pattern = "https://github.com/*"))

// 匹配所有 .com 域名但排除登录页
urlMatches = listOf(
    UrlMatchRule(pattern = "https://*.com/*"),
    UrlMatchRule(pattern = "*/login*", exclude = true)
)

// 使用正则匹配
urlMatches = listOf(UrlMatchRule(pattern = "https://(www\\.)?example\\.com/.*", isRegex = true))
```

---

## 四、配置项系统

### 4.1 ModuleConfigItem

```kotlin
data class ModuleConfigItem(
    val key: String,                              // 配置键名
    val name: String,                             // 显示名称
    val description: String = "",                 // 说明
    val type: ConfigItemType = TEXT,               // 类型
    val defaultValue: String = "",                 // 默认值
    val options: List<String> = emptyList(),       // 选项列表
    val required: Boolean = false,                 // 是否必填
    val placeholder: String = "",                  // 占位提示
    val validation: String? = null                 // 验证正则
)
```

### 4.2 ConfigItemType

支持 20 种配置类型：

| 类型 | 用途 |
|------|------|
| TEXT, TEXTAREA | 文本输入 |
| NUMBER | 数字输入 |
| BOOLEAN, CHECKBOX | 布尔开关 |
| SELECT, MULTI_SELECT, RADIO | 选择列表 |
| COLOR | 颜色选择器 |
| URL, EMAIL, PASSWORD | 格式化输入 |
| REGEX, CSS_SELECTOR, JAVASCRIPT, JSON | 代码输入 |
| RANGE | 滑块 |
| DATE, TIME, DATETIME | 日期时间 |
| FILE, IMAGE | 文件选择 |

### 4.3 在代码中读取配置

模块代码中通过 `__wtm_config__` 全局对象读取配置值：

```javascript
const config = JSON.parse(__wtm_config__ || '{}');
const mySetting = config.myKey || 'default_value';
```

---

## 五、代码注入机制

### 5.1 注入流程

```
WebView 页面加载
  → shouldOverrideUrlLoading / onPageStarted
  → ExtensionManager.generateInjectionCode(url, runAt)
    → getModulesForUrl(url) — 按 URL 过滤启用的模块
    → ensureCodeLoaded(module) — 懒加载代码
    → module.generateExecutableCode() — 生成可执行代码
  → WebView.evaluateJavascript(injectionCode)
```

### 5.2 代码包装

每个模块的代码被独立包装在 IIFE 中，错误隔离：

```javascript
// ========== Dark Mode (1.0.0) ==========
(function() {
    try {
        // 模块代码
    } catch(__moduleError__) {
        console.error('[WebToApp Module Error] Dark Mode:', __moduleError__);
    }
})();
```

### 5.3 多文件代码

`codeFiles` 支持多文件模块（如 ZIP 包导入的大型扩展）：

```kotlin
codeFiles = mapOf(
    "lib/utils.js" to "function helper() { ... }",
    "main.js" to "// main entry\nhelper();"
)
```

运行时按 key 排序后依次注入。

### 5.4 特殊来源处理

- **USERSCRIPT**：通过 `injectGreasemonkeyPolyfills()` 注入 GM_* API
- **CHROME_EXTENSION**：通过 `injectChromeExtensionPolyfills()` 注入 chrome.* API

这两种来源不走 `generateInjectionCode()`，有独立的注入路径。

---

## 六、模块分类 (ModuleCategory)

| 分类 | 说明 |
|------|------|
| APPEARANCE | 外观（暗色模式、字体等） |
| PRODUCTIVITY | 效率（自动滚动、阅读模式等） |
| PRIVACY | 隐私（广告拦截、追踪器拦截等） |
| MEDIA | 媒体（视频速度、图片下载等） |
| DEVELOPER | 开发者（调试工具、代码注入等） |
| SOCIAL | 社交（分享、评论等） |
| ACCESSIBILITY | 无障碍 |
| SECURITY | 安全 |
| OTHER | 其他 |

---

## 七、内置模块

内置模块定义在 `BuiltInModules.kt` 中，包括：

| 模块 ID | 名称 | 功能 |
|---------|------|------|
| `builtin-dark-mode` | 暗色模式 | 页面暗色主题切换 |
| `builtin-reading-mode` | 阅读模式 | Readability 提取正文 |
| `builtin-custom-font` | 自定义字体 | 替换页面字体 |
| `builtin-auto-scroll` | 自动滚动 | 自动向下滚动页面 |
| `builtin-adblocker-enhanced` | 增强广告拦截 | 高级广告拦截规则 |
| `builtin-element-blocker` | 元素屏蔽 | 选择并隐藏页面元素 |
| `builtin-video-speed` | 视频速度 | 视频播放速度控制 |
| `builtin-image-downloader` | 图片下载 | 批量下载页面图片 |
| `builtin-copy-protection-remover` | 复制保护移除 | 移除禁止复制的限制 |
| `builtin-translate-helper` | 翻译助手 | 网页翻译辅助 |
| `builtin-scroll-to-top` | 回到顶部 | 一键回到页面顶部 |
| `builtin-night-shield` | 夜间护盾 | 降低亮度和蓝光 |

---

## 八、预设方案

预设方案是模块组合，方便用户一键启用多个模块：

| 预设 ID | 名称 | 包含模块 |
|---------|------|---------|
| `preset-reading` | 阅读模式 | dark-mode + reading-mode + custom-font + auto-scroll |
| `preset-adblock` | 广告拦截 | adblocker-enhanced + element-blocker |
| `preset-media` | 媒体增强 | video-speed + image-downloader |
| `preset-utility` | 实用工具 | copy-protection-remover + translate-helper + scroll-to-top |
| `preset-night` | 夜间模式 | dark-mode + night-shield |

用户也可创建自定义方案，保存在 `extension_modules/module_presets.json` 中。

---

## 九、模块存储

### 9.1 文件结构

```
filesDir/extension_modules/
├── modules.json                    # 用户模块元数据（不含代码）
├── builtin_states.json             # 内置模块启用状态
├── module_presets.json             # 用户预设方案
├── code/                           # 模块代码文件（懒加载）
│   ├── {moduleId}.js               # JS 代码
│   ├── {moduleId}.css              # CSS 代码
│   └── {moduleId}/                 # 多文件代码目录
│       ├── lib/utils.js
│       └── main.js
└── exports/                        # 导出文件临时目录
```

### 9.2 懒加载机制

为避免内存占用过大，模块代码采用懒加载：
- 初始加载仅读取元数据（不含 code/cssCode/codeFiles）
- 需要代码时调用 `ensureCodeLoaded(module)` 从文件加载
- 代码文件存储在 `code/` 目录下

---

## 十、导入导出

### 10.1 文件格式

- 单模块：`.wtamod` (WebToApp Module) — JSON 格式
- 模块包：`.wtapkg` (WebToApp Package) — ZIP 格式（含多模块 + 资源）

### 10.2 分享码

模块可通过 JSON 分享码导入导出，格式为 Base64 编码的 JSON 字符串。

---

## 十一、开发新模块的步骤

### 11.1 创建模块

```kotlin
val module = ExtensionModule(
    name = "我的模块",
    description = "模块描述",
    category = ModuleCategory.PRODUCTIVITY,
    code = """
        // 你的 JavaScript 代码
        console.log('Hello from my module!');
    """.trimIndent(),
    cssCode = """
        /* 可选的 CSS 代码 */
        body { background: #f0f0f0; }
    """.trimIndent(),
    runAt = ModuleRunTime.DOCUMENT_END,
    urlMatches = listOf(
        UrlMatchRule(pattern = "https://example.com/*")
    ),
    configItems = listOf(
        ModuleConfigItem(
            key = "color",
            name = "背景颜色",
            type = ConfigItemType.COLOR,
            defaultValue = "#f0f0f0"
        )
    )
)
```

### 11.2 注册模块

```kotlin
// 通过 ExtensionManager
val manager = ExtensionManager.getInstance(context)
manager.addModule(module)
```

### 11.3 测试模块

1. 在编辑器中创建 WebApp，启用扩展功能
2. 添加模块 ID 到 `extensionModuleIds`
3. 导出 APK → 安装 → 验证模块是否正确注入
4. 使用 Chrome DevTools 远程调试查看控制台输出

### 11.4 添加为内置模块

如果要添加为内置模块，修改 `BuiltInModules.kt`：

```kotlin
object BuiltInModules {
    fun getAll(): List<ExtensionModule> = listOf(
        // 现有模块...
        ExtensionModule(
            id = "builtin-my-module",  // 必须以 builtin- 开头
            name = Strings.myModuleName,  // 使用 i18n 字符串
            builtIn = true,
            // ...
        )
    )
}
```

---

## 十二、权限声明 (ModulePermission)

| 权限 | 说明 |
|------|------|
| DOM_ACCESS | 访问和修改 DOM |
| NETWORK_ACCESS | 网络请求 |
| STORAGE_ACCESS | 本地存储 |
| CLIPBOARD_ACCESS | 剪贴板 |
| NOTIFICATION_ACCESS | 通知 |
| FILE_ACCESS | 文件访问 |
| CAMERA_ACCESS | 摄像头 |
| MICROPHONE_ACCESS | 麦克风 |

权限目前仅作声明用途，不强制执行。未来可能增加权限审批机制。
