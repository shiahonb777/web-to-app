<div align="center">

# WebToApp

### 任意网站。一键。一个应用。

无需 IDE。无需构建服务器。无需电脑。

[English](README.md) · **简体中文**

[![Stars](https://img.shields.io/github/stars/shiaho777/web-to-app?style=for-the-badge)](https://github.com/shiaho777/web-to-app/stargazers)
[![Forks](https://img.shields.io/github/forks/shiaho777/web-to-app?style=for-the-badge)](https://github.com/shiaho777/web-to-app/network/members)
[![License](https://img.shields.io/badge/License-Unlicense-blue?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)

</div>

<p align="center">
  <a href="#项目介绍">项目介绍</a> ·
  <a href="#核心亮点">核心亮点</a> ·
  <a href="#模块市场">模块市场</a> ·
  <a href="#功能目录">功能目录</a> ·
  <a href="#技术栈">技术栈</a> ·
  <a href="#从源码构建">构建</a> ·
  <a href="#参与贡献">贡献</a>
</p>

---

<div align="center">
<img src="social-preview.jpg" width="90%" alt="WebToApp:「我的应用」主界面(已生成的 Web/HTML/Python/Image 应用)、「创建应用」12 种类型选择器、主菜单(AI Coding、Extension Modules、App Modifier、Local Build Environment、Runtime Manager、Port Manager、Browser Kernel、Hosts Blocking、Usage Stats)、单应用操作菜单(Edit、Create Shortcut、Build APK、Share APK、Export APK)——全部在手机端完成 APK 打包,无需电脑" />
</div>

---

## 项目介绍

WebToApp 把网站、HTML 项目、媒体素材，乃至完整的服务端应用，**在手机上**直接
打包成可安装的 Android APK。

输入网址、勾选你想要的功能，几秒钟后就拿到一个能装、能分发的 APK。背后是一
个可深度配置的 WebView、四种可选的本地运行时（Node.js / PHP / Python / Go，
WordPress 跑在本地 PHP 之上），以及完全在设备内通过
[`com.android.tools.build:apksig`](https://mvnrepository.com/artifact/com.android.tools.build/apksig)
完成签名打包的 APK 构建器。**不会向任何远程构建服务器发请求。**

**支持的应用类型**（`AppType` 枚举）：网站 · HTML · 前端工程（React / Vue /
静态构建）· WordPress · Node.js · PHP · Python · Go · 图片 · 视频 · 图库 ·
多网站。

## 核心亮点

- **设备端一键打包 APK** —— 编译、签名、安装一台手机搞定，全程不联远程构建
- **双浏览器引擎** —— 系统 WebView + 可选 GeckoView (Firefox) 后端；GeckoView
  仅提供 arm64-v8a，其原生运行时在首次选用时下载，不打进 APK
- **GitHub 驱动的模块市场** —— 一键安装社区模块，无需更新 App，目录就在本仓库
- **真正的 Chrome 扩展运行时** —— 在 WebView 里直接跑未修改的 MV3 扩展，
  内置 BewlyCat 作为示例
- **本地服务端运行时** —— Node.js / PHP / Python / Go 通过本地 HTTP 服务跑在
  设备上，WordPress 跑在本地 PHP 之上
- **可定制的 WebView** —— UA 伪装、28 维度指纹伪装、广告拦截、
  DNS-over-HTTPS、JS/CSS 注入、支付协议处理
- **应用修改器** —— 克隆已安装 APK，修改图标/名称/包名（二进制 AXML 修补 +
  重签名）
- **设备端 APK 签名** —— 在手机上新建、导入、导出、查看密钥库
  （MD5 / SHA-1 / SHA-256 指纹）；可选 V1/V2/V3 签名方案与自定义 V1 签名文件名
- **AI 编程** —— 基于 Skill 的 agent，支持在手机上构建网页、扩展模块以及
  各类应用类型（HTML、React、Vue、Node、PHP、Python、Go、WordPress 等），
  内嵌预览并自带图像生成能力
- **每个 APK 的使用统计** —— Stats 页面 + 健康监测 + Vico 图表
- **完整三语 UI** —— 中文、英文、阿拉伯语开箱即用

---

## 模块市场

模块市场让用户一键安装社区贡献的 JS/CSS 扩展模块——而**整个目录就托管在这个仓库**。
没有后端、没有提交平台、没有审核排队，就是普通的 GitHub PR 流程。

```
modules/                                    ← 已发布目录
├── registry.json                           ← App 首次拉取的索引
├── submissions.json                        ← CI 生成：PR / 提交者元数据
├── README.md                               ← 贡献者指南
├── hello-world/                            ← 示例：浮动横幅
│   ├── module.json
│   └── main.js
├── night-shift/                            ← 示例：暖色护眼
│   ├── module.json
│   ├── main.js
│   └── style.css
├── reading-mode/                           ← 阅读模式
├── floating-search/                        ← 划词操作条
└── auto-scroll/                            ← 自动滚动遥控
```

App 会**同时**拉取 `registry.json` 和 `submissions.json`，只渲染两边都出现的
模块——以此保证目录只展示 PR 已真正合并的模块（`submissions.json` 由 CI 在每次
推送 `main` 时重新生成）。

**用户侧：** 打开 App → *扩展模块* 页面 → 点击右上角商店图标。模块发新版本时
App 会自动检测并提示更新。

**贡献者侧：** Fork 仓库 → 在 `modules/` 下加一个文件夹 → 在 `registry.json`
里加一行 → 开 PR。合并之后所有用户在下一次刷新时（默认缓存 1 小时）就能看到。

→ [完整贡献者指南](modules/README.md)
→ [项目通用贡献指南](CONTRIBUTING.md)

---

## 功能目录

下面每条都对应一个真实的类或枚举。点击展开。

<details>
<summary><b>浏览器引擎与网络</b></summary>

- 桌面模式、自定义 User-Agent、`DOCUMENT_START` / `END` / `IDLE` 三时机的
  JS/CSS 注入（`ScriptRunTime`）
- 打包时的**内核风味**伪装（`KernelFlavor`）：让生成的 APK 对外表现成
  Chrome / Edge / 三星浏览器（Blink）、Firefox（Gecko）或 Safari（WebKit）——
  对 UA、`navigator.userAgentData` / Sec-CH-UA 品牌、`window.chrome` 在否、
  `vendor` 做一致伪装。客户端提示请求头通过官方
  `WebSettingsCompat.setUserAgentMetadata` API 下发（在 WebView 支持时）。不更换真实引擎
- 弹窗拦截，新窗口行为（`NewWindowBehavior`）可选 `SAME_WINDOW` /
  `EXTERNAL_BROWSER` / `POPUP_WINDOW` / `BLOCK`
- 静态代理（HTTP / HTTPS / SOCKS5）与 PAC，支持身份验证和绕过规则；SOCKS5
  通过本地 HTTP-to-SOCKS 桥转发
- DNS-over-HTTPS 七大预设（Cloudflare、Google、AdGuard、NextDNS、CleanBrowsing、
  Quad9、Mullvad）+ 自定义 endpoint（`DnsProvider`）
- PWA 离线支持，可选缓存策略；自定义错误页
- 每个应用独立的 hosts 文件覆写；支付协议处理
  （`alipay://` / `weixin://` / `paypal://` 等）
- 一组按需开启的 WebView 兼容开关（默认全部关闭）：blob 下载拦截、滚动记忆、
  图片修复、内核伪装（`KernelDisguiseLevel`）、剪贴板 / 方向 / 通知 polyfill、
  Native Bridge（带逐能力白名单）、内网桥接

</details>

<details>
<summary><b>浏览器指纹伪装（28 维度）</b></summary>

5 档预设：`STEALTH` → `GHOST` → `PHANTOM` → `SPECTER` → `CUSTOM`（外加
`OFF`）。伪装引擎（`BrowserDisguiseConfig`）可伪装：

| 类别 | 维度 |
| --- | --- |
| 反检测基线 | `X-Requested-With` 移除、UA 清洗、隐藏 `webdriver`、模拟 Chrome `window`、伪装 plugins、伪装 vendor |
| 硬件指纹 | Canvas 噪声、WebGL 渲染器（7 种 GPU 配置）、AudioContext 噪声、屏幕配置（7 种设备分辨率预设）、ClientRects 噪声 |
| 环境指纹 | 时区、语言、平台、`hardwareConcurrency`、`deviceMemory` |
| 隐私指纹 | MediaDevices、WebRTC IP 屏蔽、字体枚举拦截、电池屏蔽 |
| 网络指纹 | Connection、Permissions、Performance Timing、Storage Estimate、Notification、CSS Media |
| 加固 | `Native.toString` 保护、iframe 伪装传播、错误堆栈清理 |

覆盖率按激活的维度数量分级显示：`OFF` → `BASIC` → `MODERATE` → `ADVANCED` →
`DEEP` → `MAXIMUM`。

</details>

<details>
<summary><b>应用内 OAuth（30+ 提供商）</b></summary>

通过逐家反检测脚本，让未修改的 Chrome OAuth 流程能在 WebView 内完成。
`OAuthCompatEngine.Provider` 枚举识别 32 个品牌提供商：

Google、Facebook、Apple、Microsoft、Amazon、Twitter / X、GitHub、Discord、Reddit、
LinkedIn、Spotify、Twitch、LINE、Kakao、Naver、微信、QQ、支付宝、TikTok / 抖音、
Yahoo Japan、Yahoo、VK、Yandex、Mail.ru、Shopify、Dropbox、Notion、Slack、Zoom、
PayPal、Stripe、Square——外加 reCAPTCHA / hCaptcha / Cloudflare Turnstile 兼容
和一个通用 OAuth 兜底。

当识别到的 OAuth 流程无法在 WebView 内完成时，回退到 Chrome Custom Tab
（`androidx.browser`）共享 cookie 完成登录。

</details>

<details>
<summary><b>扩展模块</b></summary>

- **11 个内置 JS 模块**（`BuiltInModules`）：视频下载、B 站 / 抖音 / 小红书
  提取器、视频增强、网页分析、页内查找、暗色模式、隐私保护、内容增强、元素拦截
- **1 个内置 Chrome 扩展**（`assets/extensions/bewlycat/`）：BewlyCat 给 B 站
  换皮肤，是 MV3 运行时跑真实扩展的活样板（以 ISOLATED + MAIN 两个 world 脚本
  加载）
- **3 种模块来源**（`ModuleSourceType`）：纯 JavaScript（`CUSTOM`）、
  Greasemonkey/Tampermonkey 油猴脚本（`USERSCRIPT`，`.user.js`）、Chrome MV3
  扩展（`CHROME_EXTENSION`，`manifest.json`）
- `GM_*` 桥接（`GM_setValue`、`GM_xmlhttpRequest`、`GM_addStyle`、菜单命令、
  Promise 风格的 `GM.*` 接口等），按脚本声明授权
- MV3 `chrome.declarativeNetRequest` 引擎（`ActionType`）：block / allow /
  redirect / upgrade-scheme / allow-all-requests 已生效，modify-headers 规则
  会被解析但暂未应用；另有一套广泛的 `chrome.*` polyfill（runtime、storage、
  tabs、scripting 等）支撑 MV3 扩展
- 模块通过分享码（`WTA1:` gzip+Base64）或二维码（ZXing）传播
- **AI 编程** agent 可经由 `module-js` / `module-style` / `module-userscript`
  / `module-chrome-mv3` 这几个 skill，从一段 prompt 生成扩展模块
- 上文介绍的 **模块市场**

</details>

<details>
<summary><b>视觉与体验</b></summary>

- 单一的精校单色主题（`AppThemes.KimiNoNawa`），含独立的浅色/深色配色；
  Material You 动态取色为可选项（默认关闭）
- 自定义启动屏——图片或视频，点击跳过、视频裁剪范围、固定方向
- 背景音乐播放列表 + LRC 同步，6 种歌词动画（淡入、上滑、左滑、缩放、打字机、
  卡拉OK），3 种位置（顶 / 中 / 底），自定义字体/颜色/描边/阴影主题。在线音乐
  搜索 + 20 个曲风标签
- 状态栏主题——颜色、深/浅色图标、高度，深色模式独立配置
- 悬浮窗模式——尺寸、透明度、圆角、边缘吸附、位置锁定、自动隐藏标题栏、
  启动即最小化
- 10 种公告模板（极简、小红书、渐变、毛玻璃、霓虹、可爱、优雅、节日、暗黑、
  自然），可在启动 / 定时 / 无网络时触发
- 7 种屏幕方向；屏幕常亮 + 亮度控制
- 5 种长按菜单样式（Simple / Full / iOS / Floating / Context）

</details>

<details>
<summary><b>每个 APK 的使用统计</b></summary>

- Stats 页面，图表用 Vico Compose 绘制
- 跟踪每个打包应用的打开次数、总时长、最近打开时间、最近一次会话时长
- App Health Monitor 定期对每个应用的 URL 做 `HEAD` 请求，标出不可达的站点
  （`HealthStatus`：UNKNOWN / ONLINE / SLOW / OFFLINE）

</details>

<details>
<summary><b>本地服务端运行时</b></summary>

- **Node.js** —— 环境变量、npm 依赖管理器、示例项目库（Express / Fastify /
  Koa）。运行时跑在独立的 `:nodejs` OS 进程里，底层包装原生 `node_launcher`
  C++ 可执行文件（`dlopen` 加载 `libnode.so`，这样用户脚本崩溃也带不垮宿主）
- **PHP** —— PHP 8.4，首次使用时从
  [`pmmp/PHP-Binaries`](https://github.com/pmmp/PHP-Binaries) 下载一次
  （arm64-v8a），支持 Composer，可自定义 document root
- **Python** —— Flask、Django、FastAPI（经 uvicorn）、Tornado 或内置 HTTP
  服务器，支持 pip 依赖解析到 `.pypackages`
- **Go** —— 设备端 `go build`（支持 `vendor/` 离线构建）+ 静态文件服务，
  经 `go_exec_loader` C++ 包装层执行
- **WordPress** —— 跑在本地 PHP 运行时之上，由 SQLite 承载（WordPress 6.9.1 +
  `sqlite-database-integration` 插件），支持主题 + 插件导入
- **Linux 环境** —— 一个按需安装工具链、管理 Node / PHP / Python 构建与依赖
  的页面（Go 和 WordPress 由各自的依赖管理器单独管理）
- **端口管理器** —— 通过广播 receiver（`PortQueryReceiver` /
  `PortReleaseReceiver`）实现跨应用端口协调，避免多个打包应用抢同一端口；
  每种运行时各占一个端口段

</details>

<details>
<summary><b>应用类型专属能力</b></summary>

- **图库应用** —— 分类的图片/视频；网格 / 列表 / 时间线视图；顺序 / 随机 /
  单循 播放；按自定义 / 名称 / 日期 / 类型 排序；缩略图条、媒体信息浮层、
  视频自动下一个、记忆播放位置
- **多网站应用** —— 标签 / 卡片 / 信息流 / 抽屉 四种布局；每站点独立图标、
  主题色、内容提取 CSS 选择器；可配置刷新间隔；可直接粘贴 HTML/JS 作为内联
  站点（无需上传文件）；可为所有站点注入自定义 JS/CSS
- **网站爬虫** —— 离线包创建器：抓取整站前端（HTML / CSS / JS / 图片 /
  字体），6 路并发下载；递归解析 CSS `url()` / `srcset` / `@import`；绝对路
  径转相对；同域限制；深度和大小上限
- **应用修改器** —— 两种形态：基于桌面快捷方式的伪装，或真正的二进制克隆
  （AXML manifest 修补 + ARSC 资源中的图标替换 + `JarSigner` 重签名 +
  `FileProvider` 安装）

</details>

<details>
<summary><b>翻译、通知、深度链接、生命周期</b></summary>

- **页内翻译覆盖层** —— 20 种目标语言、5 个引擎（Google、MyMemory、
  LibreTranslate、Lingva、Auto），悬浮按钮切换，加载即翻译
- **Web `Notification` polyfill** + URL 轮询前台服务（最低 5 分钟间隔），
  支持 JSON 解析以及带自定义 header 的 GET / POST
- 自定义 URL Scheme（深链），可配置 host 匹配
- 开机自启（`BOOT_COMPLETED`、`QUICKBOOT_POWERON`、`MY_PACKAGE_REPLACED`、
  时区/时间变化）
- 通过 `SCHEDULE_EXACT_ALARM` 实现定时启动
- 后台运行前台服务，自定义通知、CPU wake lock、电池优化白名单提示

</details>

<details>
<summary><b>生成 APK 的安全</b></summary>

下面这些功能作用于 **WebToApp 生成的 APK**。WebToApp 本体（宿主）的权限清单
是刻意精简的，详见 `AndroidManifest.xml`。

- **资源加密** —— `PBKDF2WithHmacSHA256` + AES-256-GCM（10 万次 PBKDF2 迭代），
  加密打包进 APK 的资源（配置、HTML、媒体、BGM）。不设自定义密码时，密钥由
  包名 + 签名证书派生——这两者在 APK 里都是公开的——此时加密只能挡住「直接
  解压查看」；要获得能抵御逆向的保护，请设置**自定义密码**。
- **运行时保护** —— 随资源加密一起启用：开启加密后，生成的应用会在启动时运行
  反调试、反 Frida 与 DEX 篡改（CRC）检测。可配置的**威胁响应**（`ThreatResponse`）
  决定命中高危威胁时的动作——`LOG_ONLY`（默认，不影响正常用户）、`SILENT_EXIT`
  或 `CRASH_RANDOM`。这是抬高动态分析门槛的轻量防护，无法阻止对开源宿主代码的
  静态逆向。
- **WebView / 内容隔离**（`IsolationConfig`）—— 存储隔离、WebRTC 屏蔽、
  Canvas / Audio / WebGL / 字体保护，以及为打包 WebView 做的指纹 / 头 / IP
  伪装
- **浏览器与设备指纹伪装** —— 见上文
- **广告拦截** —— hosts 规则引擎 + cosmetic MutationObserver 过滤；内置 23
  个社区过滤源（EasyList、EasyPrivacy、uBlock、AdGuard 系列、StevenBlack、
  AdAway、Peter Lowe、1Hosts Lite、Anti-AD 以及多个区域列表）
- **激活码门控** —— 每次启动 / 持久；激活码可设为 永久 / 时限 / 次数限制 /
  设备绑定 / 组合（`ActivationCodeType`）。默认在本地校验，也可改为对接**你自己的
  HTTPS 接口**（`RemoteActivationConfig`），从而无需重新打包即可吊销、动态发码——
  服务器响应由你掌握的 EC P-256 公钥验签，并可配置离线策略。在线校验仍在客户端运行，
  只是抬高门槛，无法阻止有决心的绕过。服务端接口契约与参考实现见
  [`docs/remote-activation.md`](docs/remote-activation.md)。

</details>

<details>
<summary><b>强制运行、BlackTech、图标风暴</b></summary>

这些功能用于演示 Android 系统的能力边界。**必须在用户知情同意下使用。**

- **强制运行** —— 三种模式（`FIXED_TIME` / `COUNTDOWN` / `DURATION`）。屏蔽
  系统 UI、返回 / Home / 最近 / 通知。倒计时跨进程持久化。可设密码紧急退出，
  结束前预警
- **BlackTech**（`DeviceActionsConfig`，序列化字段名 `blackTechConfig`）：
  - 音量控制（强制最大 / 静音 / 屏蔽音量键）
  - 闪光灯模式 —— 频闪、SOS、摩尔斯码（自定义文本和单位时长）、心跳、呼吸、
    紧急、自定义警报模式（可同步震动）
  - 系统控制（屏蔽电源键、最大性能、飞行模式）
  - 屏幕控制（黑屏、强制旋转、屏蔽触摸、强制唤醒）
  - 网络控制（WiFi 热点 SSID/密码、关闭 WiFi / 蓝牙 / 移动数据）
  - 预设档位：`SILENT_MODE` / `ALARM_MODE` / `SOS_SIGNAL` / `NUCLEAR_MODE` /
    `STEALTH_MODE`
- **设备伪装** —— 6 种设备类型（手机 / 平板 / 桌面 / 笔记本 / 手表 / 电视）×
  10 种 OS（Android、iOS、HarmonyOS、Windows、macOS、Linux、ChromeOS、
  watchOS、Wear OS、tvOS）；49 个具体设备预设，包括 iPhone 17 Pro Max、
  Galaxy S26 Ultra、Pixel 10 Pro XL、Mate 70 Pro+、OnePlus 15、MacBook Pro
  M5、Surface Pro 11、Apple Watch Ultra 3 等
- **图标风暴 (Icon Storm)**（`IconStormMode`）—— 多启动器图标伪装。打包出来
  的 App 可以拥有从一个自定义下限直到 `5000` 个启动器别名，模式包括：
  `Subtle Flood (25)`、`Icon Flood (100)`、`Icon Storm (500)`、
  `Extreme Storm (1000)`、`Research (5000)`、自定义。每个别名约 520 字节
  manifest 开销，UI 会实时给出影响估算

</details>

<details>
<summary><b>APK 导出选项</b></summary>

- 自定义包名、`versionName`、`versionCode`
- 架构选择（`ApkArchitecture`）：通用 / ARM64 / ARM32
- 性能优化 —— 图片压缩、WebP 转换、代码压缩、懒加载、DNS 预取、preload 提示
- **打包时按勾选注入子 APK 的运行时权限**（摄像头、麦克风、定位、存储、蓝牙、
  NFC、短信、联系人、日历、传感器、前台服务、wake lock、安装包、系统弹窗）。
  这些**不会**进入宿主 manifest
- Banner、插屏、开屏 广告配置（配置与管线已就绪；未捆绑广告 SDK，因此投放为
  占位实现）
- 完整应用数据备份/恢复；项目导出/导入
- AAB 导出（用于 Play 式分发；bundle 的 protobuf 元数据在设备端生成）

</details>

<details>
<summary><b>APK 签名与密钥库管理</b></summary>

签名全程在设备端由 `JarSigner` 完成，底层驱动
`com.android.tools.build:apksig`。签名身份是**全局设置**——这里选定的签名会用于
**所有**新打包的 APK。

- **三种签名身份**（`JarSigner.SignerType`）：`PKCS12_CUSTOM`（你新建或导入的
  keystore）、`PKCS12_AUTO`（首次运行自动生成）、`ANDROID_KEYSTORE`（系统托管
  兜底）。每次启动自定义签名优先生效
- **设备端新建签名**（`createCustomKeystore` / `CertificateSpec`）——生成全新的
  RSA 密钥对（2048 / 4096）和自签名证书，可填完整 X.500 主题
  （CN / O / OU / L / ST / C）和有效期（默认 30 年），无需电脑或 `keytool`
- **导入** PKCS12 / PFX / JKS / BKS（`importKeystore`），支持 JKS /
  Android Studio「upload key」那种 key 密码与 store 密码不同的情形。非 PKCS12
  格式会转存为 PKCS12，并把真实 alias 落盘到 sidecar，使自定义签名跨进程重启
  后依然生效
- **导出**当前密钥为带密码的 `.p12` 备份（`exportPkcs12`），以及**删除**自定义
  keystore 回退到自动生成的签名
- **查看指纹**（`getCertificateFingerprints` → `CertificateFingerprints`）——
  证书的 MD5 / SHA-1 / SHA-256，点击即可复制
- **签名方案选择**（`SigningSchemeOptions`）——独立开关 V1（JAR）、V2（完整 APK）、
  V3（密钥轮换）。一个**自动回退**开关会在旧证书算法与新方案不兼容时，逐级丢弃
  最新的方案（V3 → V2）并重新校验；关闭后则严格只用你勾选的方案签名
- **自定义 V1 签名文件名**——设置 `META-INF/<名称>.SF` 与 `META-INF/<名称>.RSA`
  里的 `<名称>`。留空则自动从签名密钥证书的 CN 派生，并带实时预览

</details>

---

## 技术栈

让 WebToApp 不止是"普通 Compose 应用"的关键依赖：

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Koin** —— 依赖注入
- **Room 2.7.2** + **KSP** —— 数据持久化
- **OkHttp 4.12.0** + `okhttp-dnsoverhttps` —— 网络层
- **`com.android.tools.build:apksig` 8.3.0** —— 设备端 APK 签名核心
- **`protobuf-javalite` 3.25.5** —— 为设备端 AAB 导出编码元数据
- **GeckoView**（Firefox 内核，arm64-v8a）—— 可选 WebView 替代，原生运行时
  在首次选用时下载，不打进 APK
- **Coil**（`compose` + `video` + `gif`）—— 图片加载
- **AndroidX Security Crypto** + **AndroidX DataStore** —— 加密存储
- **Vico** Compose-M3 —— Stats 页面图表
- **ZXing** —— 模块二维码分享
- **Apache Commons Compress** + **xz** —— 网站爬虫和项目导入解压
- **Native C++ via JNI** —— `node_launcher` 和 `go_exec_loader` 是真实的
  CMake target，按 ABI 编译
- **Robolectric** —— 单元测试

完整列表见 [`app/build.gradle.kts`](app/build.gradle.kts)。

> **为什么 `targetSdk = 28`？** WebToApp 需要从 app 存储里 `fork`+`exec`
> 原生二进制（PHP、Go 等）。Android 10+ 在 SELinux 下禁止 `untrusted_app`
> 这么做，所以——和 Termux 一样——本 app 把 `targetSdk` 锁在 28，并通过
> GitHub Releases 而非 Google Play 分发。完整理由见 `app/build.gradle.kts`
> 里的注释。

---

## 从源码构建

**要求：** Android Studio Hedgehog 或更新版本、JDK 17。Gradle wrapper 已锁定
Gradle 9.4.1，无需系统安装 Gradle。

```bash
git clone https://github.com/shiaho777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

Release 构建请在 `app/build.gradle.kts`（经 `local.properties`）中配置签名。
服务端运行时（PHP、Node.js、Python、Go、GeckoView 引擎）**不会**打进 APK——
它们在 App 内首次使用时才下载，以保持宿主体积精简。

---

## 参与贡献

三条路径，按投入由小到大：

| 路径 | 内容 | 指南 |
| --- | --- | --- |
| `modules/` | 给应用内市场提交一个社区模块 | [`modules/README.md`](modules/README.md) |
| Issues | 报告 Bug 或申请功能 | [GitHub Issues](https://github.com/shiaho777/web-to-app/issues) |
| 代码 | 修 Bug 或在 Android 客户端做新功能 | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

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

[The Unlicense](LICENSE)。高级功能（强制运行、BlackTech、图标风暴）仅供技术
演示，必须在用户知情同意下使用。

<div align="center">

**开源 · 永久免费 · Star 一下支持**

</div>
