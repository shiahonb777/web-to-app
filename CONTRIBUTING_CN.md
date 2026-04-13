# 贡献指南

[English](CONTRIBUTING.md) | **简体中文**

## 项目结构

```
app/src/main/java/com/webtoapp/
├── WebToAppApplication.kt        # Application类
├── core/                         # 核心运行时与业务模块
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

## 模块开发示例

```javascript
// 示例1：自动隐藏广告
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

// 示例2：一键保存图片（使用 NativeBridge）
document.querySelectorAll('img').forEach(img => {
    img.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        NativeBridge.saveImageToGallery(img.src);
        NativeBridge.vibrate(50);
        NativeBridge.showToast('图片已保存');
    });
});

// 示例3：分享当前页面
function shareCurrentPage() {
    NativeBridge.share(document.title, '分享给你一个有趣的页面', location.href);
}
```
