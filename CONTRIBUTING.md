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
│   ├── shell/ stats/ usecase/ wordpress/
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
│       ├── client/
│       ├── config/
│       ├── injection/
│       ├── intercept/
│       └── navigation/
├── di/
│   └── AppModule.kt
├── ui/
│   ├── MainActivity.kt
│   ├── animation/ codepreview/ components/
│   ├── data/ gallery/ icons/ media/
│   ├── navigation/
│   │   ├── AppNavigation.kt / AppNavigationGraph.kt / AppNavigationScaffold.kt
│   │   ├── AppToolNavigation.kt / AppCreationNavigation.kt / AppPreviewNavigation.kt
│   │   ├── AppCommunityNavigation.kt / AppAccountNavigation.kt / AppNavigationTabContent.kt
│   │   └── AiExportCoordinator.kt / CreateHtmlRouteArgs.kt / PreviewStarter.kt
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
