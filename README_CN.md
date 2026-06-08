<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="WebToApp 图标" />

# WebToApp

### 在手机上把 Web 项目打包成可安装的 Android APK。

**WebToApp 是一个运行在设备端的 APK 构建器，支持网站、HTML 应用、媒体项目，以及 Node.js、PHP、Python、Go、WordPress 等本地运行时。**
你可以把一个网址、一个项目文件夹，或一组媒体素材，变成能预览、签名、安装、分享和导出的 Android 应用，整个构建过程不需要远程服务器。

[English](README.md) · **简体中文**

[![Stars](https://img.shields.io/github/stars/shiaho777/web-to-app?style=for-the-badge)](https://github.com/shiaho777/web-to-app/stargazers)
[![Forks](https://img.shields.io/github/forks/shiaho777/web-to-app?style=for-the-badge)](https://github.com/shiaho777/web-to-app/network/members)
[![License](https://img.shields.io/badge/License-Unlicense-blue?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)

</div>

<p align="center">
  <a href="#为什么是-webtoapp">为什么是 WebToApp</a> ·
  <a href="#能打包什么">能打包什么</a> ·
  <a href="#核心亮点">核心亮点</a> ·
  <a href="#模块市场">模块市场</a> ·
  <a href="#能力地图">能力地图</a> ·
  <a href="#从源码构建">构建</a>
</p>

---

<div align="center">
<img src=".github/assets/social-preview.jpg" width="90%" alt="WebToApp：我的应用首页、创建应用类型选择、主工具箱，以及单个应用的 APK 操作都运行在 Android 手机上" />
</div>

---

## 为什么是 WebToApp

很多“网站转 App”工具只是套一个 WebView。WebToApp 更像一个放在手机里的 APK 工作台：WebView 配置、本地运行时、APK 签名、扩展模块、项目导入导出、应用管理都在同一个 Android 应用里完成。

- **设备端构建** - APK 打包和签名都在 App 内完成，不排远程构建队列。
- **不只包装网页** - 支持网站、HTML/前端构建、Node.js、PHP、Python、Go、WordPress、媒体应用、图库和多网站应用。
- **产物可控** - 包名、图标、权限、签名密钥、签名方案、运行时选项、导出格式都可以配置。
- **发布后还能扩展** - 通过 JS/CSS 模块、油猴脚本或 MV3 Chrome 扩展给应用补能力，不必重新发布宿主。
- **开源可检查** - Android 客户端、模块目录和打包逻辑都在这个仓库里。

## 能打包什么

| 输入 | 输出 | 适合场景 |
| --- | --- | --- |
| 网站 URL | 基于 WebView 的 APK | 官网、工具、后台、文档、内部系统 |
| HTML / 静态前端 | 走 localhost 的 APK | React、Vue、Vite、静态构建、离线 Web 应用 |
| Node.js / PHP / Python / Go | 带设备端本地服务的 APK | 小型服务端应用、管理工具、演示、原型 |
| WordPress | 本地 PHP + SQLite 承载的 APK | 便携站点、主题/插件演示、本地内容包 |
| 图片 / 视频 / 图库 | 媒体型 APK | 相册、课程材料、作品集、离线浏览 |
| 多个网站 | 标签/卡片/信息流/抽屉布局 APK | 导航合集、门户、应用集合 |
| 已安装 APK | 重命名克隆或桌面快捷方式伪装 | 图标/名称/包名实验、APK 重打包研究 |

当前 `AppType` 覆盖 Web、HTML、Frontend、WordPress、Node.js、PHP、Python、Go、Image、Video、Gallery、Multi-Web。

## 使用流程

1. **创建**：从网址、项目文件夹、媒体素材或运行时模板开始。
2. **配置**：调整 WebView、工具栏、启动屏、模块、权限、签名和运行时行为。
3. **预览**：先在手机上看效果，再生成最终 APK。
4. **打包签名**：通过 `com.android.tools.build:apksig` 在设备端完成签名。
5. **安装、分享、导出、备份**：生成的 APK 和项目数据都能继续管理。

## 核心亮点

| 方向 | 亮点 |
| --- | --- |
| APK 构建器 | 二进制 AXML/ARSC 修补、资源注入、权限裁剪、V1/V2/V3 签名、AAB 元数据生成 |
| WebView 控制 | User-Agent、桌面模式、JS/CSS 注入、DNS-over-HTTPS、代理、自定义错误页、PWA 缓存策略 |
| 浏览器引擎 | 默认系统 WebView，可选 GeckoView 运行时提供 Firefox 风格渲染 |
| 本地运行时 | Node.js、PHP 8.4、Python、Go、WordPress 通过本地 HTTP 服务跑在设备上 |
| 扩展能力 | 内置模块、带 `GM_*` API 的油猴脚本、MV3 Chrome 扩展内容脚本、二维码/分享码传播 |
| 隐私与加固 | 广告拦截、资源加密、运行时检测、WebView 隔离、激活码门控 |
| 应用体验 | 启动屏、BGM/LRC 歌词、悬浮窗、状态栏主题、通知、深链、使用统计 |
| AI 编程 | 在移动端工作流里通过 prompt 生成网页、扩展模块和本地运行时项目 |

## 获取应用

版本发布在 [GitHub Releases](https://github.com/shiaho777/web-to-app/releases)。

WebToApp 会有意保持 `targetSdk = 28`，这样生成的应用才能像 Termux 一样，从 app 存储里运行本地原生二进制。因此目前通过 GitHub 分发比 Google Play 更合适。

## 模块市场

WebToApp 有一个由 GitHub 驱动的模块市场，用来分发社区贡献的 JS/CSS 扩展。目录本质上就是这个仓库里的文件，所以贡献流程就是普通 PR。

```
modules/
├── registry.json        # App 读取的目录
├── submissions.json     # CI 生成的 PR / 提交者元数据
├── README.md            # 贡献者指南
├── hello-world/
├── night-shift/
├── reading-mode/
├── floating-search/
└── auto-scroll/
```

App 会同时拉取 `registry.json` 和 `submissions.json`，只展示两边都存在的模块，保证应用内市场和已经合并的 PR 对齐。

- 用户打开 **扩展模块** 页面，点击右上角商店图标即可安装。
- 贡献者在 `modules/` 下添加文件夹，更新 `registry.json`，然后提交 PR。
- 客户端默认缓存 1 小时，模块合并后不需要发新版 App。

[模块贡献指南](modules/README.md) · [项目通用贡献指南](CONTRIBUTING.md)

## 能力地图

WebToApp 的开关很多。下面按使用场景分组，保留关键细节，但不让 README 一上来变成设置页。

<details>
<summary><b>浏览器引擎与网络</b></summary>

- 桌面模式、自定义 User-Agent，以及 document start / end / idle 三种时机的 JS/CSS 注入。
- 内核风味伪装：让生成的 APK 对外表现为 Chrome、Edge、Samsung Internet、Firefox 或 Safari 风格，但真实引擎不变。
- 弹窗处理：当前窗口、外部浏览器、弹窗窗口或直接拦截。
- 静态 HTTP/HTTPS/SOCKS5 代理、PAC 代理、身份验证、绕过规则和本地 HTTP-to-SOCKS 桥。
- DNS-over-HTTPS：Cloudflare、Google、AdGuard、NextDNS、CleanBrowsing、Quad9、Mullvad，以及自定义 endpoint。
- PWA 离线缓存策略、自定义错误页、每应用 hosts 覆写和支付协议处理。
- 可选兼容开关：blob 下载、滚动记忆、图片修复、剪贴板、方向、通知 polyfill、内网桥接和 Native Bridge 能力门控。

</details>

<details>
<summary><b>扩展与自动化</b></summary>

- 内置模块：视频下载、B 站/抖音/小红书提取、视频增强、网页分析、页内查找、暗色模式、隐私工具、内容增强、元素拦截。
- 支持 Greasemonkey/Tampermonkey 风格的 `.user.js` 油猴脚本。
- `GM_*` 桥接：存储、请求、样式、菜单命令，以及按授权开放的 Promise 风格 `GM.*` API。
- MV3 Chrome 扩展运行时，支持 manifest 内容脚本在 isolated 或 main world 注入。
- `chrome.*` polyfill 覆盖 runtime、storage、tabs、scripting 和 declarative network request 解析。
- 通过 `WTA1:` gzip + Base64 分享码或 ZXing 二维码传播模块。
- AI Coding skill 可生成扩展模块、油猴脚本、MV3 扩展、前端应用和本地运行时项目。

</details>

<details>
<summary><b>设备端运行时</b></summary>

- **Node.js** 跑在独立 `:nodejs` OS 进程中，底层由原生 `node_launcher` 加载 `libnode.so`。
- **PHP** 使用 PHP 8.4，首次使用时从 `pmmp/PHP-Binaries` 下载，支持 Composer。
- **Python** 支持 Flask、Django、FastAPI/uvicorn、Tornado、内置 HTTP server，并把 pip 依赖解析到 `.pypackages`。
- **Go** 支持设备端 `go build`、`vendor/` 离线构建、静态文件服务和原生 `go_exec_loader` 包装层。
- **WordPress** 跑在本地 PHP 之上，用 `sqlite-database-integration` 接 SQLite，支持主题和插件导入。
- Linux Environment 页面管理 Node、PHP、Python 的工具链和依赖。
- Port Manager 通过广播 receiver 协调不同生成应用之间的运行时端口。

</details>

<details>
<summary><b>应用体验</b></summary>

- 图片或视频启动屏，支持跳过、视频裁剪区间和固定方向。
- 背景音乐播放列表 + LRC 同步歌词、歌词动画、自定义字体/颜色/描边/阴影和在线音乐搜索。
- 工具栏、状态栏、深色状态栏、导航栏行为、悬浮窗模式和长按菜单样式。
- 公告模板，可在启动、定时或无网络时触发。
- 页内翻译覆盖层，支持 20 种目标语言，以及 Google、MyMemory、LibreTranslate、Lingva、Auto 引擎。
- Web Notification polyfill、URL 轮询前台服务、深链、开机自启、定时启动和后台运行服务。
- 每个 APK 的使用统计、Vico 图表和 URL 健康监测。

</details>

<details>
<summary><b>安全、隐私与访问控制</b></summary>

- 用 PBKDF2 + AES-256-GCM 加密打包进去的配置、HTML、媒体和 BGM。
- 可设置自定义加密密码，比默认的包名/证书派生密钥更能抵御逆向提取。
- 开启资源加密后，可启用反调试、反 Frida 和 DEX 篡改检测。
- 威胁响应可选：只记录、静默退出或随机崩溃。
- WebView/内容隔离覆盖存储、WebRTC、Canvas、Audio、WebGL、字体、请求头和 IP 暴露面。
- 28 维浏览器指纹伪装，包括 UA、WebGL、Canvas、AudioContext、ClientRects、时区、语言、内存、媒体设备、WebRTC、字体、电池、权限、性能、存储、通知、CSS media、iframe 传播和错误栈清理。
- hosts 规则广告拦截 + cosmetic MutationObserver 过滤，内置 23 个社区过滤源。
- 激活码门控支持本地验证，或接入你自己的 HTTPS 接口并用 EC P-256 验签。接口契约见 [remote activation 文档](docs/remote-activation.md)。

</details>

<details>
<summary><b>APK 导出与签名</b></summary>

- 自定义包名、`versionName`、`versionCode`、图标、名称、架构目标和导出格式。
- 按生成 APK 的实际勾选注入权限，并从模板 manifest 中裁剪未使用权限。
- 性能选项：图片压缩、WebP 转换、代码压缩、懒加载、DNS 预取、preload 提示。
- 完整项目备份/恢复和应用数据备份/恢复。
- 设备端导出 AAB，并在本地生成 protobuf 元数据。
- 密钥库创建、导入、导出、删除和证书指纹查看。
- 支持 PKCS12/PFX/JKS/BKS 导入，包括 Android Studio upload key 那种 store 密码和 key 密码不同的情况。
- V1、V2、V3 签名方案独立控制，并可对旧证书兼容性自动回退。
- 自定义 V1 签名文件名，对应 `META-INF/<name>.SF` 和 `META-INF/<name>.RSA`。

</details>

<details>
<summary><b>专项工具与研究功能</b></summary>

- 网站爬虫用于生成离线包：HTML、CSS、JS、图片、字体、CSS `url()`、`srcset`、`@import`、路径重写、同域限制、深度限制、体积限制。
- 多网站应用支持标签、卡片、信息流、抽屉布局，每站点独立图标、主题色、提取选择器、刷新间隔和共享 JS/CSS。
- 图库应用支持媒体分类、网格/列表/时间线视图、随机/单循、排序、缩略图条、浮层、视频自动下一个和播放记忆。
- 应用修改器支持桌面快捷方式伪装，也支持真正的二进制克隆、manifest/资源修补和重签名。
- 强制运行、BlackTech、设备伪装、图标风暴属于技术演示能力，必须在用户知情同意下使用。

</details>

## 架构说明

- 仓库有两个 Gradle 模块：`app` 是完整构建器和宿主，`shell` 是嵌入生成 APK 的运行时宿主。
- 运行时代码以 `app` 为唯一事实来源，再同步到 `shell`，所以共享 WebView/运行时行为只维护一份。
- APK 构建器会在二进制 AXML/ARSC 层修补模板 APK，注入配置与资源，裁剪权限，并用 `apksig` 签名。
- 宿主有意保持 `targetSdk = 28`，以便本地原生运行时从 app 存储中 `fork` 和 `exec`。
- 服务端运行时和可选 GeckoView 原生运行时不会打进基础 APK，而是在首次使用时下载。

## 技术栈

- Kotlin、Jetpack Compose、Material 3
- Koin 依赖注入
- Room 2.7.2 + KSP 数据持久化
- OkHttp 4.12.0 + `okhttp-dnsoverhttps`
- `com.android.tools.build:apksig` 8.3.0 用于 APK 签名
- `protobuf-javalite` 3.25.5 用于 AAB 元数据
- GeckoView 作为可选浏览器引擎
- Coil 负责图片、视频、GIF 加载
- AndroidX Security Crypto + DataStore 存储密钥
- Vico Compose-M3 绘制图表
- ZXing 用于二维码分享
- Apache Commons Compress + xz 用于项目导入和网站爬虫
- JNI 原生 C++ 目标：`node_launcher` 和 `go_exec_loader`
- Robolectric 单元测试

完整依赖见 [app/build.gradle.kts](app/build.gradle.kts)。

## 从源码构建

要求：Android Studio Hedgehog 或更新版本，JDK 17。Gradle wrapper 已锁定 Gradle 9.4.1。

```bash
git clone https://github.com/shiaho777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

Release 构建请通过 `local.properties` 和 `app/build.gradle.kts` 配置签名。

## 参与贡献

| 路径 | 内容 | 指南 |
| --- | --- | --- |
| `modules/` | 给应用内市场提交一个社区模块 | [modules/README.md](modules/README.md) |
| Issues | 报告 Bug 或申请功能 | [GitHub Issues](https://github.com/shiaho777/web-to-app/issues) |
| 代码 | 修 Bug 或在 Android 客户端做新功能 | [CONTRIBUTING.md](CONTRIBUTING.md) |

## 联系方式

开发者：**shiaho**

| 平台 | 链接 |
| --- | --- |
| GitHub | [github.com/shiaho777/web-to-app](https://github.com/shiaho777/web-to-app) |
| Telegram | [t.me/webtoapp777](https://t.me/webtoapp777) |
| X (Twitter) | [@shiaho777](https://x.com/shiaho777) |
| Bilibili | [b23.tv/8mGDo2N](https://b23.tv/8mGDo2N) |
| QQ 群 | 1041130206 |

## 许可证

[The Unlicense](LICENSE)。

强制运行、BlackTech、设备伪装、图标风暴等高级功能仅用于技术演示，必须在用户知情同意下使用。

<div align="center">

**开源 · 为 Android 高阶用户打造 · Star 一下支持项目**

</div>
