# 贡献指南

[English](CONTRIBUTING.md) | **简体中文**

## 项目结构

```
app/src/main/java/com/webtoapp/
├── WebToAppApplication.kt        # Application class
├── core/                         # Core runtime and business modules
│   ├── activation/ adblock/ ads/ ai/ announcement/
│   ├── appmodifier/ auth/ autostart/ background/
│   ├── backup/ bgm/ billing/ blacktech/ common/
│   ├── crypto/ disguise/ download/ engine/ errorpage/
│   ├── export/ extension/ floatingwindow/ forcedrun/
│   ├── frontend/ golang/ hardening/ i18n/ isolation/
│   ├── kernel/ linux/ logging/ network/ nodejs/
│   ├── perf/ php/ port/ pwa/ python/ sample/
│   ├── shell/ startup/ stats/ usecase/ wordpress/
│   ├── apkbuilder/
│   │   ├── ApkAnalyzer.kt
│   │   ├── ApkBuilder.kt
│   │   ├── ApkTemplate.kt
│   │   ├── BuildLogger.kt
│   │   ├── NativeApkOptimizer.kt
│   │   ├── ShellTemplateProvider.kt
│   │   ├── assets/
│   │   ├── config/
│   │   ├── manifest/
│   │   ├── packager/
│   │   ├── signing/
│   │   └── zip/
│   ├── cloud/
│   │   ├── AppDownloadManager.kt
│   │   ├── CloudApiClient.kt
│   │   ├── CloudRepository.kt
│   │   ├── InstalledItemsTracker.kt
│   │   ├── api/
│   │   ├── internal/
│   │   └── model/
│   └── webview/
│       ├── DownloadBridge.kt
│       ├── LocalHttpServer.kt
│       ├── LongPressHandler.kt
│       ├── NativeBridge.kt
│       ├── OAuthCompatEngine.kt
│       ├── PwaOfflineSupport.kt
│       ├── ShareBridge.kt
│       ├── TranslateBridge.kt
│       ├── WebViewCallbacks.kt
│       ├── WebViewManager.kt
│       ├── WebViewPool.kt
│       ├── client/ compat/
│       ├── config/ injection/ intercept/
│       └── lifecycle/ navigation/ session/
├── di/
│   └── AppModule.kt
├── ui/
│   ├── MainActivity.kt
│   ├── animation/ codepreview/ components/
│   ├── gallery/ icons/ media/
│   ├── navigation/
│   │   ├── AppNavigation.kt / AppNavigationGraph.kt / AppNavigationScaffold.kt
│   │   ├── AppFlowSpec.kt / AppNavigationEffects.kt / AppNavigationRoutes.kt
│   │   ├── AppToolNavigation.kt / AppCreationNavigation.kt / AppPreviewNavigation.kt
│   │   ├── AppCommunityNavigation.kt / AppAccountNavigation.kt / AppNavigationTabContent.kt
│   │   └── AppNavigationGraphDependencies.kt / AiExportCoordinator.kt / CreateHtmlRouteArgs.kt / PreviewStarter.kt
│   ├── shared/ shell/ splash/ theme/ viewmodel/
│   ├── screens/
│   │   ├── aimodule/ community/ extensionmodule/
│   │   ├── AppStoreScreen.kt
│   │   ├── CreateHtmlAppScreen.kt
│   │   ├── ExtensionModuleScreen.kt
│   │   ├── appstore/
│   │   │   ├── details/ downloads/
│   │   │   └── management/ published/
│   │   └── htmlimport/
│   └── webview/
│       ├── WebAppPreviewCoordinator.kt
│       ├── WebViewDownloadBridge.kt
│       ├── ConsolePanel.kt
│       ├── PreviewStates.kt
│       ├── rememberWebViewManager.kt
│       ├── ServerPreviewOverlays.kt
│       ├── WebViewActivity.kt
│       ├── WebViewLongPressMenu.kt
│       ├── WebViewSplashOverlay.kt
│       ├── WebViewStrictHostFallback.kt
│       ├── WebViewUtils.kt
│       └── screen/
│           └── WebViewScreen.kt
└── util/
```

## 结构说明

这些说明放在贡献指南里，避免 README 继续塞进“代码怎么分层”的细节。

### 云服务分层

- `app/src/main/java/com/webtoapp/core/cloud/api`
    云服务分域 API 放这里，例如激活、备份、通知。`CloudApiClient.kt` 只保留门面协调，不要再继续塞请求细节。
- `app/src/main/java/com/webtoapp/core/cloud/internal`
    云服务共享内部实现放这里，例如请求辅助、错误映射、JSON 解析。
- `app/src/main/java/com/webtoapp/core/cloud/model`
    云端 DTO 统一收口到这里，别再让响应模型散落在巨型文件里。

### i18n 分层

- `app/src/main/java/com/webtoapp/core/i18n/strings`
    `Strings.kt` 现在只是兼容门面，不再直接养文案。真正的功能文案已经按 `CommonStrings`、`CreateStrings`、`CloudStrings`、`CommunityStrings`、`AiStrings`、`AiCodingStrings`、`AiConfigStrings`、`ModuleStrings`、`ExtensionStrings`、`ShellStrings`、`WebViewStrings`、`ProjectStrings`、`SnippetStrings`、`StoreStrings`、`BillingStrings`、`MusicStrings`、`UiStrings`、`BuildStrings`、`CompatStrings`、`SampleStrings` 这些分组拆开。后续新增文案和翻译都优先进对应分组文件，别再把旧怪物喂胖。

### 翻译贡献

- `app/src/main/java/com/webtoapp/core/i18n/strings`
    翻译优先改这里的分组 Kotlin 文件。不要直接往 `Strings.kt` 里加或改译文，它现在只负责兼容转发。
- `python tools/export_i18n_catalog.py --format all`
    先导出 `build/i18n/i18n_catalog.csv` 和 `build/i18n/i18n_catalog.json`，翻译人员可以直接对照表格工作，不用硬啃 Kotlin。
- `kind=resource-backed`
    这类条目不是 Kotlin 里的硬编码，而是走 Android `R.string.*` 资源。要去改 `app/src/main/res/values-zh`、`app/src/main/res/values-en`、`app/src/main/res/values-ar` 里的对应 `strings.xml`，不要去改分组 Kotlin 文案。
- 占位符别乱动
    `%s`、`%1$s`、`${VERSION}`、`{name}`、换行标记、产品名和格式相关标点都别翻、别删、别改顺序。导出表的 `notes` 列会尽量把这些危险点标出来。
- 不会 Kotlin 也能贡献翻译
    如果你不想直接改源码，可以把改好的 CSV/JSON 附在 PR/issue 里，或者直接列出 `source_file + key + 目标语言 + 译文`，由维护者帮你回填到对应分组文件。
- 新增文案也别倒车
    新字符串要放进 `core/i18n/strings` 下最接近功能的分组文件，不要又把字面量回灌到 `Strings.kt` 这个旧粪坑里。
- `python tools/export_i18n_catalog.py --check`
    跑一次静态解析检查，顺手看缺失语言统计。这样至少能先确认 getter 结构没被改坏，再让别人去编译。

### AI 分层

- `app/src/main/java/com/webtoapp/core/ai/model`
    `AiApiClient.kt` 里的模型列表解析、能力推断、上下文长度和价格猜测已经拆到这里，别再把这些规则回灌到门面文件。
- `app/src/main/java/com/webtoapp/core/ai/provider`
    AI 供应商响应解析和图像辅助逻辑已经拆到这里，`AiApiClient.kt` 继续往门面协调方向收敛。

### 功能页面

- `app/src/main/java/com/webtoapp/ui/screens/create/webview/cards`
    创建页 WebView 卡片已按 `interaction / display / system` 拆开，功能专属卡片放这里。
- `app/src/main/java/com/webtoapp/ui/screens/extensionmodule/editor`
    扩展模块编辑页已按 `tabs / dialogs` 拆开，`ModuleEditorScreen.kt` 只负责页面装配。
- `app/src/main/java/com/webtoapp/ui/screens/home/components`
    首页卡片 UI 开始往这里下沉；`AppCard`、卡片菜单、状态点和标签这类纯界面细节都放这里，别再继续把 `HomeScreen.kt` 堆成大粪球。

### 扩展运行时

- `app/src/main/java/com/webtoapp/core/extension/snippets`
    `CodeSnippets.kt` 现在只是兼容门面，代码片段分类、搜索和热门逻辑已经拆到这里，后续新增片段继续按职责落子文件。
- `app/src/main/java/com/webtoapp/core/extension/panel`
    扩展面板的图标映射、主注入脚本、辅助脚本已经拆到这里，`ExtensionPanelScript.kt` 只保留旧入口。

### 模型与 ViewModel

- `app/src/main/java/com/webtoapp/data/model/webapp/config`
    `WebApp.kt` 只保留实体本身，WebView、运行时、媒体、导出、外观行为配置都拆到这里，老引用先由 `WebAppConfigAliases.kt` 兼容兜底。
- `app/src/main/java/com/webtoapp/ui/viewmodel/main`
    `MainViewModel` 的保存拼装、PWA 导入、状态工厂 helper 放这里，别再把保存/导入/状态重建全糊回一个 ViewModel 文件。

### 创建流程共用层

- `app/src/main/java/com/webtoapp/ui/screens/create/common`
    放置创建流程共享的状态模型、区块抽象、保存/预览控制以及项目导入契约。此层负责统一流转 `AppFlowSpec` + `ProjectImportAnalysis`，让各创建页在收集用户输入后的行为保持一致。
- `app/src/main/java/com/webtoapp/ui/screens/create/runtime`
    各种运行时特定项目导入器与分析器（Node/PHP/Python/Go）。导入器扫描入口文件、依赖、环境变量、端口等信息，生成标准 `ProjectImportAnalysis` 供创建页侧快速回填表单。

### WebView 分层

- `app/src/main/java/com/webtoapp/core/webview/WebViewManager.kt`
    现在只保留协调职责：组装配置、维护会话级状态、把导航/拦截/兼容/注入/销毁分发给专职组件，不再继续包圆所有脏活。
- `app/src/main/java/com/webtoapp/core/webview/session`
    存放 `WebViewSessionState`，统一收口当前配置、主框架 URL、扩展运行时、诊断计数等页面会话状态。
- `app/src/main/java/com/webtoapp/core/webview/navigation`
    处理 URL 策略、特殊 scheme、外部浏览器/Custom Tab、新窗口分发。
- `app/src/main/java/com/webtoapp/core/webview/intercept`
    处理请求拦截、cleartext 回退、跨域代理、本地/加密资源加载以及 MIME 判断。
- `app/src/main/java/com/webtoapp/core/webview/compat`
    处理 Strict Host、OAuth 兼容、Requested-With allow-list、保守脚本模式与 scriptless 模式。
- `app/src/main/java/com/webtoapp/core/webview/injection`
    将普通脚本注入与扩展运行时拆开：`ScriptInjectionCoordinator` 只管常规注入，`ExtensionRuntimeCoordinator` 负责扩展运行时和面板注册。
- `app/src/main/java/com/webtoapp/core/webview/lifecycle`
    负责 WebView、Cookie/WebStorage、JS bridge、扩展 runtime 的销毁和清理。
- `app/src/main/java/com/webtoapp/ui/webview/rememberWebViewManager.kt`
    统一编辑器页和 Shell 页的 `WebViewManager` 创建入口，避免两边各自再抄一份 `remember { WebViewManager(...) }`。

## 模块开发示例

```javascript
// Example 1: Auto-hide ads
const selectors = getConfig('selectors', '.ad-banner').split('\n');
function hideAds() {
    selectors.forEach(sel => {
        document.querySelectorAll(sel).forEach(el => {
            el.style.display = 'none';
        });
    });
}
hideAds();
new MutationObserver(hideAds).observe(document.body, { childList: true, subtree: true });

// Example 2: One-click image save (with NativeBridge)
document.querySelectorAll('img').forEach(img => {
    img.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        NativeBridge.saveImageToGallery(img.src);
        NativeBridge.vibrate(50);
        NativeBridge.showToast('图片已保存');
    });
});

// Example 3: Share current page
function shareCurrentPage() {
    NativeBridge.share(document.title, '分享给你一个有趣的页面', location.href);
}
```
