<div align="center">

# WebToApp

### 任意网站。一键。一个应用。

无需 IDE。无需构建服务器。无需电脑。

[English](README.md) · **简体中文**

[![Stars](https://img.shields.io/github/stars/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/stargazers)
[![Forks](https://img.shields.io/github/forks/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/network/members)
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
<img src="png/1.png" width="19%" /><img src="png/2.png" width="19%" /><img src="png/3.png" width="19%" /><img src="png/4.png" width="19%" /><img src="png/5.png" width="19%" />
<img src="png/6.png" width="19%" /><img src="png/7.png" width="19%" /><img src="png/8.png" width="19%" /><img src="png/9.png" width="19%" /><img src="png/10.png" width="19%" />
</div>

---

## 项目介绍

WebToApp 把网站、HTML 项目、媒体素材，乃至完整的服务端应用，**在手机上**直接
打包成可安装的 Android APK。

输入网址、勾选你想要的功能，几秒钟后就拿到一个能装、能分发的 APK。背后是一
个深度加固的 WebView、五种可选的本地运行时（Node.js / PHP / Python / Go /
WordPress），以及完全在设备内通过
[`com.android.tools.build:apksig`](https://mvnrepository.com/artifact/com.android.tools.build/apksig)
完成签名打包的 APK 构建器。**不会向任何远程构建服务器发请求。**

**支持的应用类型：** 网站 · HTML · React · Vue · WordPress · Node.js · PHP ·
Python · Go · 图片 · 视频 · 图库 · 多网站

## 核心亮点

- **设备端一键打包 APK** —— 编译、签名、安装一台手机搞定，全程不联远程构建
- **双浏览器引擎** —— 系统 WebView + 可选 GeckoView (Firefox) 后端
- **GitHub 驱动的模块市场** —— 一键安装社区模块，无需更新 App，目录就在本仓库
- **真正的 Chrome 扩展运行时** —— 在 WebView 里直接跑未修改的 MV3 扩展，
  内置 BewlyCat 作为示例
- **本地服务端运行时** —— Node.js / PHP / Python / Go 通过本地 HTTP 服务跑在
  设备上，WordPress 跑在内置 PHP 之上
- **深度可定制的 WebView** —— UA 伪装、28 维度指纹伪装、广告拦截、
  DNS-over-HTTPS、JS/CSS 注入、支付协议处理
- **应用修改器** —— 克隆已安装 APK，修改图标/名称/包名（二进制 AXML 修补 +
  重签名）
- **AI 助手** —— 生成扩展模块、HTML 项目、APK 图标；agent 架构的 AI Coding V2
  可以直接写完整网页
- **每个 APK 的使用统计** —— Stats 页面 + 健康监测 + Vico 图表
- **完整三语 UI** —— 中文、英文、阿拉伯语开箱即用

---

## 模块市场

模块市场让用户一键安装社区贡献的 JS/CSS 扩展模块——而**整个目录就托管在这个仓库**。
没有后端、没有提交平台、没有审核排队，就是普通的 GitHub PR 流程。

```
modules/                                    ← 已发布目录
├── registry.json                           ← App 首次拉取的索引
├── README.md                               ← 贡献者指南
├── hello-world/                            ← 示例：浮动横幅
│   ├── module.json
│   └── main.js
└── night-shift/                            ← 示例：暖色护眼
    ├── module.json
    ├── main.js
    └── style.css
```

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
  JS/CSS 注入
- 弹窗拦截，新窗口行为可选 `SAME_WINDOW` / `EXTERNAL_BROWSER` / `POPUP_WINDOW`
  / `BLOCK`
- HTTP、SOCKS5、PAC 三种代理，支持身份验证和绕过规则
- DNS-over-HTTPS 七大预设（Cloudflare、Google、AdGuard、NextDNS、CleanBrowsing、
  Quad9、Mullvad）+ 自定义 endpoint
- PWA 离线支持，可选缓存策略；自定义错误页
- 每个应用独立的 hosts 文件覆写；支付协议处理
  （`alipay://` / `weixin://` / `paypal://` 等）
- WebView 兼容/隐私组合（默认全开）：GPC 头、Cookie 同意拦截、追踪器拦截、
  blob 下载拦截、滚动记忆、图片修复、HTTPS 升级、内核伪装、剪贴板/方向 polyfill、
  Native Bridge、内网桥接、Referrer Policy

</details>

<details>
<summary><b>浏览器指纹伪装（28 维度）</b></summary>

5 档预设：`Stealth` → `Ghost` → `Phantom` → `Specter` → `Custom`。可伪装：

| 类别 | 维度 |
| --- | --- |
| 反检测基线 | `X-Requested-With` 移除、UA 清洗、隐藏 `webdriver`、模拟 Chrome `window`、伪装 plugins、伪装 vendor |
| 硬件指纹 | Canvas 噪声、WebGL 渲染器（7 种 GPU 配置）、AudioContext 噪声、屏幕配置（7 种设备分辨率预设）、ClientRects 噪声 |
| 环境指纹 | 时区、语言、平台、`hardwareConcurrency`、`deviceMemory` |
| 隐私指纹 | MediaDevices、WebRTC IP 屏蔽、字体枚举拦截、电池屏蔽 |
| 网络指纹 | Connection、Permissions、Performance Timing、Storage Estimate、Notification、CSS Media |
| 加固 | `Native.toString` 保护、iframe 伪装传播、错误堆栈清理 |

覆盖率分级：`OFF` → `BASIC` → `MODERATE` → `ADVANCED` → `DEEP` → `MAXIMUM`。

</details>

<details>
<summary><b>应用内 OAuth（30+ 提供商）</b></summary>

通过逐家反检测脚本，让未修改的 Chrome OAuth 流程能在 WebView 内完成。识别的提
供商：

Google、Facebook、Apple、Microsoft、Amazon、Twitter / X、GitHub、Discord、Reddit、
LinkedIn、Spotify、Twitch、LINE、Kakao、Naver、微信、QQ、支付宝、TikTok / 抖音、
Yahoo Japan、Yahoo、VK、Yandex、Mail.ru、Shopify、Dropbox、Notion、Slack、Zoom、
PayPal、Stripe、Square，加上 reCAPTCHA / hCaptcha / Cloudflare Turnstile 兼容。

Google OAuth 在 WebView 内被拒时，回退到 Chrome Custom Tab（`androidx.browser`）
共享 cookie 完成登录。

</details>

<details>
<summary><b>扩展模块</b></summary>

- **11 个内置 JS 模块：** 视频下载、B 站 / 抖音 / 小红书 提取器、视频增强、
  网页分析、页内查找、暗色模式、隐私保护、内容增强、元素拦截
- **1 个内置 Chrome 扩展**（`assets/extensions/bewlycat/`）：BewlyCat 给 B 站
  换皮肤，是 MV3 运行时跑真实扩展的活样板
- **3 种用户模块来源：** 纯 JavaScript、Greasemonkey/Tampermonkey 油猴脚本
  （`.user.js`）、Chrome MV3 扩展（`manifest.json`）
- 完整的 `GM_*` 桥接（`GM_setValue`、`GM_xmlhttpRequest` 等），按脚本声明授权
- MV3 `chrome.declarativeNetRequest` 引擎——block / allow / redirect /
  modifyHeaders
- 模块通过分享码或二维码（ZXing）传播
- AI 模块开发者页面——给一段 prompt，AI 生成可直接安装的模块代码
- 上文介绍的 **模块市场**

</details>

<details>
<summary><b>视觉与体验</b></summary>

- Aurora 主题系统，动态色彩生成
- 自定义启动屏——图片或视频，点击跳过、视频裁剪范围、固定方向
- 背景音乐播放列表 + LRC 同步，6 种歌词动画（淡入、上滑、左滑、缩放、打字机、
  卡拉OK），3 种位置，自定义字体/颜色/描边/阴影主题。在线音乐搜索 + 20+ 标签
- 状态栏主题——颜色、深/浅色图标、透明度、高度，深色模式独立配置
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
- 跟踪每个打包应用的打开次数、总时长、最近打开时间、按日使用情况
- App Health Monitor 定期对每个应用的 URL 做 `HEAD` 请求，标出不可达的站点

</details>

<details>
<summary><b>本地服务端运行时</b></summary>

- **Node.js** —— 4 种构建模式（静态 / SSR / API 后端 / 全栈），环境变量、npm
  依赖管理器、示例项目库；底层包装原生 `node_launcher` C++ 可执行文件
- **PHP** —— 8.4 二进制在构建时从
  [`pmmp/PHP-Binaries`](https://github.com/pmmp/PHP-Binaries) 下载一次，
  支持 Composer，可自定义 document root
- **Python** —— Flask、Django 或内置 HTTP 服务器，支持 pip 依赖解析
- **Go** —— 设备端二进制编译 + 静态文件服务，底层是 `go_exec_loader` C++ 包装层
- **WordPress** —— 跑在内置 PHP 之上，支持主题 + 插件
- **Linux 环境** —— 内置工具链 + 一个统一管理 5 种运行时构建/依赖/端口的页面
- **端口管理器** —— 通过广播 receiver 实现跨应用端口协调，避免多个打包应用抢
  同一端口

</details>

<details>
<summary><b>应用类型专属能力</b></summary>

- **图库应用** —— 分类的图片/视频；网格 / 列表 / 时间线视图；顺序 / 随机 /
  单循 播放；按自定义 / 名称 / 日期 / 类型 排序；缩略图条、媒体信息浮层、
  视频自动下一个、记忆播放位置
- **多网站应用** —— 标签 / 卡片 / 信息流 / 抽屉 四种布局；每站点独立图标、
  主题色、内容提取 CSS 选择器；可配置刷新间隔
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
- 自定义 URL Scheme，可配置 host 匹配
- 开机自启（`BOOT_COMPLETED`、`QUICKBOOT_POWERON`、`MY_PACKAGE_REPLACED`、
  时区/时间变化）
- 通过 `SCHEDULE_EXACT_ALARM` 实现定时启动
- 后台运行前台服务，自定义通知、CPU wake lock、电池优化白名单提示

</details>

<details>
<summary><b>生成 APK 的安全与加固</b></summary>

下面这些功能作用于 **WebToApp 生成的 APK**。WebToApp 本体（宿主）的权限清单
是刻意精简的，详见 `AndroidManifest.xml`。

- **APK 加密** —— PBKDF2 10 万次迭代，支持自定义密码
- **应用隔离** —— 独立数据目录、独立 WebView 进程
- **浏览器与设备指纹伪装** —— 见上文
- **广告拦截** —— hosts 规则引擎 + cosmetic MutationObserver 过滤；内置 12
  个社区过滤源（EasyList、EasyPrivacy、uBlock、AdGuard 系列、StevenBlack、
  Peter Lowe、1Hosts Lite 以及多个区域列表）
- **激活码门控** —— 每次启动 / 持久；激活码可设为 无限 / 时限 / 设备绑定
- **应用加固流水线** —— DEX 加密 + 拆分、控制流平坦化、原生 SO 加密、ELF
  混淆、符号剥离、反 dump
- **反逆向** —— 反 Frida / Xposed / Magisk / 调试 / 内存 dump / 截屏；
  模拟器 / VirtualApp / VPN / USB 调试 检测
- **代码混淆** —— 字符串加密、类名混淆、不透明谓词、多点签名校验、证书固定
- **威胁响应** —— 运行时屏障、蜜罐、自毁模式

</details>

<details>
<summary><b>强制运行、BlackTech、图标风暴</b></summary>

这些功能用于演示 Android 系统的能力边界。**必须在用户知情同意下使用。**

- **强制运行** —— 三种模式（`FIXED_TIME` / `COUNTDOWN` / `DURATION`）。屏蔽
  系统 UI、返回 / Home / 最近 / 通知。倒计时跨进程持久化。可设密码紧急退出，
  结束前预警
- **BlackTech** —— `BlackTechConfig` 中声明的每一个开关：
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
  watchOS、Wear OS、tvOS）；28 个具体设备预设，包括 iPhone 17 Pro Max、
  Galaxy S26 Ultra、Pixel 10 Pro XL、Mate 70 Pro+、OnePlus 15、MacBook Pro
  M5、Surface Pro 11、Apple Watch Ultra 3 等
- **图标风暴 (Icon Storm)** —— 多启动器图标伪装。打包出来的 App 可以拥有
  从 `2`（Subtle）到 `5000`（Research）个启动器别名，模式包括：
  `Subtle Flood (25)`、`Icon Flood (100)`、`Icon Storm (500)`、
  `Extreme Storm (1000)`、`Research (5000)`、自定义。每个别名约 520 字节
  manifest 开销，UI 会实时给出影响估算

</details>

<details>
<summary><b>APK 导出选项</b></summary>

- 自定义包名、`versionName`、`versionCode`
- 架构选择：通用 / ARM64 / ARM32
- 性能优化 —— 图片压缩、WebP 转换、代码压缩、懒加载、DNS 预取、preload 提示
- **打包时按勾选注入子 APK 的运行时权限**（摄像头、麦克风、定位、存储、蓝牙、
  NFC、短信、联系人、日历、传感器、前台服务、wake lock、安装包、系统弹窗）。
  这些**不会**进入宿主 manifest
- Banner、插屏、开屏 广告，可配置 ID 与时长
- 完整应用数据备份/恢复；项目导出/导入

</details>

---

## 技术栈

让 WebToApp 不止是"普通 Compose 应用"的关键依赖：

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Koin** —— 依赖注入
- **Room 2.7.2** + **KSP** —— 数据持久化
- **OkHttp 4.12.0** + `okhttp-dnsoverhttps` —— 网络层
- **`com.android.tools.build:apksig` 8.3.0** —— 设备端 APK 签名核心
- **GeckoView**（Firefox 内核）—— 可选 WebView 替代
- **Coil**（`compose` + `video` + `gif`）—— 图片加载
- **AndroidX Security Crypto** + **AndroidX DataStore** —— 加密存储
- **Vico** Compose-M3 —— Stats 页面图表
- **ZXing** —— 模块二维码分享
- **Apache Commons Compress** + **xz** —— 网站爬虫和项目导入解压
- **Native C++ via JNI** —— `node_launcher` 和 `go_exec_loader` 是真实的
  CMake target，按 ABI 编译
- **Robolectric** —— 单元测试

完整列表见 [`app/build.gradle.kts`](app/build.gradle.kts)。

---

## 从源码构建

**要求：** Android Studio Hedgehog 或更新版本、JDK 17、Gradle 8.14+。

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

Release 构建请在 `app/build.gradle.kts` 中配置签名。首次 release 构建会下载
PHP 二进制；可以提前用 `./gradlew :app:downloadPhpBinary` 缓存好。

---

## 参与贡献

三条路径，按投入由小到大：

| 路径 | 内容 | 指南 |
| --- | --- | --- |
| `modules/` | 给应用内市场提交一个社区模块 | [`modules/README.md`](modules/README.md) |
| Issues | 报告 Bug 或申请功能 | [GitHub Issues](https://github.com/shiahonb777/web-to-app/issues) |
| 代码 | 修 Bug 或在 Android 客户端做新功能 | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

## 联系方式

开发者：**shiaho**

| 平台 | 链接 |
| --- | --- |
| GitHub | [github.com/shiahonb777/web-to-app](https://github.com/shiahonb777/web-to-app) |
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
