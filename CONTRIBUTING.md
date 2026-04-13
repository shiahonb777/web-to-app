# Contributing Guide

**English** | [简体中文](CONTRIBUTING_CN.md)

## Project Structure

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

## Structure Notes

These notes are kept in the contributing guide so README stays user-facing.

### Cloud Layering

- `app/src/main/java/com/webtoapp/core/cloud/api`
    Domain-specific cloud APIs such as activation, backup, and notifications live here. Keep request wiring out of `CloudApiClient.kt`.
- `app/src/main/java/com/webtoapp/core/cloud/internal`
    Shared cloud internals such as JSON parsing, error mapping, and request helpers live here.
- `app/src/main/java/com/webtoapp/core/cloud/model`
    Cloud DTOs are centralized here so response models stop leaking across random files.

### i18n Layering

- `app/src/main/java/com/webtoapp/core/i18n/strings`
    `Strings.kt` now acts as a compatibility facade while grouped string sources are split here by feature. Common, create, cloud, community, AI, module, and shell text should keep moving into dedicated files instead of bloating the old 30k-line monster again.

### AI Layering

- `app/src/main/java/com/webtoapp/core/ai/model`
    AI model discovery, fallback catalog parsing, capability guessing, and pricing/context heuristics now live here instead of bloating `AiApiClient.kt`.
- `app/src/main/java/com/webtoapp/core/ai/provider`
    Provider response parsing and image-related helpers now live here so `AiApiClient.kt` can stay closer to a facade.

### Feature Screens

- `app/src/main/java/com/webtoapp/ui/screens/create/webview/cards`
    WebView creation cards are split by responsibility into `interaction`, `display`, and `system`.
- `app/src/main/java/com/webtoapp/ui/screens/extensionmodule/editor`
    Extension editor UI is split into `tabs` and `dialogs`, leaving `ModuleEditorScreen.kt` as a thin composition entry.
- `app/src/main/java/com/webtoapp/ui/screens/home/components`
    Home list card UI now moves here first. `AppCard`, its menu, and card-level status/chip rendering belong here instead of bloating `HomeScreen.kt`.

### Extension Runtime

- `app/src/main/java/com/webtoapp/core/extension/snippets`
    `CodeSnippets.kt` is now only a compatibility facade. Snippet models, grouped category builders, search, and popular-item wiring live here.
- `app/src/main/java/com/webtoapp/core/extension/panel`
    Extension panel icon mapping, the injected panel script, and helper script now live here so `ExtensionPanelScript.kt` no longer acts as a giant string dump.

### Model & ViewModel

- `app/src/main/java/com/webtoapp/data/model/webapp/config`
    `WebApp.kt` now stays focused on the entity itself, while WebView/runtime/media/export/appearance config types live in dedicated files under this package. `WebAppConfigAliases.kt` keeps legacy imports working during migration.
- `app/src/main/java/com/webtoapp/ui/viewmodel/main`
    `MainViewModel` helpers such as save assembly, PWA import coordination, and state factories live here so the ViewModel stops acting like a 2,000-line god object.

### Create Flow Shared Layer

- `app/src/main/java/com/webtoapp/ui/screens/create/common`
    Shared state models, block abstractions, save/preview control, and project import contracts for the create flow live here. This layer keeps `AppFlowSpec` and `ProjectImportAnalysis` flowing consistently across creation screens.
- `app/src/main/java/com/webtoapp/ui/screens/create/runtime`
    Runtime-specific project importers and analyzers for Node, PHP, Python, and Go live here. They scan entry files, dependencies, environment variables, ports, and other runtime details, then produce a standard `ProjectImportAnalysis`.

### WebView Layering

- `app/src/main/java/com/webtoapp/core/webview/WebViewManager.kt`
    This now keeps only coordination responsibilities: assembling config, maintaining session state, and dispatching navigation, interception, compatibility, injection, and cleanup to dedicated components.
- `app/src/main/java/com/webtoapp/core/webview/session`
    Stores `WebViewSessionState` and keeps page session state such as current config, primary URL, extension runtime, and diagnostic counters in one place.
- `app/src/main/java/com/webtoapp/core/webview/navigation`
    Handles URL strategy, special schemes, external browser / Custom Tab launching, and new-window dispatch.
- `app/src/main/java/com/webtoapp/core/webview/intercept`
    Handles request interception, cleartext fallback, cross-origin proxying, local / encrypted resource loading, and MIME detection.
- `app/src/main/java/com/webtoapp/core/webview/compat`
    Handles Strict Host, OAuth compatibility, Requested-With allow-lists, conservative script mode, and scriptless mode.
- `app/src/main/java/com/webtoapp/core/webview/injection`
    Splits normal script injection from extension runtime injection. `ScriptInjectionCoordinator` handles regular injection, while `ExtensionRuntimeCoordinator` handles extension runtime and panel registration.
- `app/src/main/java/com/webtoapp/core/webview/lifecycle`
    Handles WebView, Cookie / WebStorage, JS bridge, and extension runtime cleanup.
- `app/src/main/java/com/webtoapp/ui/webview/rememberWebViewManager.kt`
    Unifies `WebViewManager` creation for the editor and shell flows so the two entry points do not drift apart.

## Module Development Examples

```javascript
// Example 1: auto-hide ads
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

// Example 2: save images with NativeBridge
document.querySelectorAll('img').forEach(img => {
    img.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        NativeBridge.saveImageToGallery(img.src);
        NativeBridge.vibrate(50);
        NativeBridge.showToast('Image saved');
    });
});

// Example 3: share the current page
function shareCurrentPage() {
    NativeBridge.share(document.title, 'Sharing a page you may like', location.href);
}
```
