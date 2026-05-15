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
  <a href="#从源码构建">从源码构建</a> ·
  <a href="#参与贡献">参与贡献</a> ·
  <a href="#联系方式">联系方式</a>
</p>

---

<div align="center">
<img src="png/1.png" width="19%" /><img src="png/2.png" width="19%" /><img src="png/3.png" width="19%" /><img src="png/4.png" width="19%" /><img src="png/5.png" width="19%" />
<img src="png/6.png" width="19%" /><img src="png/7.png" width="19%" /><img src="png/8.png" width="19%" /><img src="png/9.png" width="19%" /><img src="png/10.png" width="19%" />
</div>

---

## 项目介绍

WebToApp 把网站、HTML 项目、媒体素材、甚至完整的服务端应用，**在手机上**直接
打包成可安装的 Android APK。

输入一个网址，挑好你想要的功能，几秒钟后就拿到一个能装、能分发的 APK。背后是
一个深度加固过的 WebView、若干可选的本地运行时（Node.js / PHP / Python /
Go），以及一个完全在设备内完成签名打包的 APK 构建器。

**支持类型：** 网站 · HTML · React · Vue · WordPress · Node.js · PHP · Python ·
Go · 图片 · 视频 · 图库 · 多网站

## 核心亮点

- **一键打包 APK** — 签名、安装一台手机搞定
- **双浏览器引擎** — 系统 WebView + 可选 GeckoView (Firefox) 后端
- **GitHub 驱动的模块市场** — 一键安装社区模块，无需更新 App
- **AI 助手** — 生成图标、模块、HTML 代码
- **应用修改器** — 克隆已安装 APK，换图标、改名、改包名
- **高度可定制的 WebView** — 桌面 UA、指纹伪装、广告拦截、DoH、JS/CSS 注入
- **本地服务端运行时** — Node.js / PHP / Python / Go 全部跑在设备上

---

## 模块市场

模块市场让用户一键安装社区贡献的 JS/CSS 扩展模块——而**整个目录就托管在这个仓库里**。
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
里加一行 → 开 PR。合并之后，所有用户下次刷新就能看到。

→ [完整贡献者指南](modules/README.md)
→ [项目通用贡献指南](CONTRIBUTING.md)

---

## 功能目录

WebToApp 的配置面相当广，按下方分组展开查看。

<details>
<summary><b>浏览器引擎与网络</b></summary>

- 桌面模式、自定义 User-Agent、`DOCUMENT_START` / `END` / `IDLE` 三时机的
  JS/CSS 注入
- 弹窗拦截，新窗口行为可选（同标签 / 外部浏览器 / 弹窗 / 阻止）
- HTTP、SOCKS5、PAC 代理
- DNS-over-HTTPS 七大预设 + 自定义
- 跨源隔离、PWA 离线支持、自定义错误页
- 22 维度浏览器指纹伪装（Canvas、WebGL、AudioContext、字体、GPU、屏幕…）
  跨向量值保持一致
- Chrome 扩展运行时 polyfill（`chrome.runtime` / `storage` / `tabs` /
  `messaging`），未修改的桌面 Chrome 扩展可直接在 WebView 中运行
- 30+ 应用内 OAuth 提供商（Google、Facebook、GitHub、Discord、微信、支付宝、
  PayPal…）通过逐家反检测脚本实现；Google 回退到 Chrome Custom Tab 共享 cookie

</details>

<details>
<summary><b>视觉与体验</b></summary>

- Aurora 主题系统，动态色彩生成
- 自定义启动屏（图片或视频、点击跳过、裁剪范围、固定方向）
- 背景音乐播放列表 + LRC 歌词同步，7 种歌词动画（淡入 / 滑动 / 缩放 / 打字机 /
  卡拉OK），3 种位置，自定义主题
- 状态栏主题（颜色、深/浅色图标、透明度、高度，深色模式独立配置）
- 悬浮窗模式（尺寸、不透明度、圆角、边缘吸附、位置锁定、自动隐藏标题栏）
- 10 种公告模板（极简、小红书、渐变、毛玻璃、霓虹、可爱、优雅、节日、暗黑、
  自然），可在启动 / 定时 / 无网络时触发
- 7 种屏幕方向、屏幕常亮 + 亮度控制

</details>

<details>
<summary><b>扩展模块</b></summary>

- 10 个内置模块（视频下载、B站 / 抖音 / 小红书 提取器、视频增强、网页分析、
  暗色模式、隐私保护、内容增强、元素拦截）
- 自定义模块支持三种来源：纯 JS、油猴脚本（`.user.js`）、Chrome 扩展
  （`manifest.json`）
- 完整的 Greasemonkey / Tampermonkey `GM_*` 桥接
- MV3 `declarativeNetRequest` 引擎——block / allow / redirect / modifyHeaders
- 通过分享码导出/导入模块
- **模块市场**——见上文，由本仓库驱动

</details>

<details>
<summary><b>安全与隐私</b></summary>

- APK 加密（PBKDF2，10 万次迭代，支持自定义密码）
- 应用隔离，独立数据目录
- 浏览器与设备指纹伪装
- 广告拦截（hosts 规则 + cosmetic MutationObserver 过滤）
- 激活码门控（每次启动 / 持久）
- 应用加固：DEX 加密 + 拆分 + VMP + 控制流平坦化，原生 SO 加密 + ELF 混淆 +
  符号剥离 + 反 dump
- 反 Frida / Xposed / Magisk / 调试 / 内存 dump / 截屏
- 模拟器、VirtualApp、VPN、USB 调试检测
- 字符串加密、类名混淆、不透明谓词、多点签名校验、证书固定、威胁响应、
  蜜罐、自毁

</details>

<details>
<summary><b>强制运行、BlackTech、设备伪装</b></summary>

- **强制运行** 三种模式（固定时段 / 倒计时 / 访问窗口）。屏蔽系统 UI、返回 /
  Home / 最近、通知。倒计时跨进程持久化，密码紧急退出，结束前预警
- **BlackTech**：音量控制（强制最大 / 静音 / 屏蔽按键）、手电筒模式（频闪 /
  SOS / 摩尔斯 / 心跳 / 呼吸 / 紧急 / 自定义警报，可同步震动）、系统控制
  （屏蔽电源键、最大性能）、屏幕控制（黑屏、旋转、屏蔽触摸）、网络控制
  （WiFi 热点 SSID/密码、关闭 WiFi / 蓝牙 / 移动数据）、核弹与隐身模式
- **设备伪装**：手机 / 平板 / 桌面、操作系统伪装（Android / iOS / HarmonyOS /
  macOS / Windows / Linux）、品牌型号预设、分辨率、像素比、时区、语言

</details>

<details>
<summary><b>服务端运行时</b></summary>

- **Node.js**——4 种构建模式（静态 / SSR / API 后端 / 全栈），支持环境变量
- **PHP**——Composer 支持，自定义文档根
- **Python**——Flask / Django / 内置服务器，pip 依赖
- **Go**——二进制编译，静态文件服务
- **WordPress**——主题 + 插件，自带 PHP

所有运行时通过本地 HTTP 服务跑在设备上，**不需要远程后端**。

</details>

<details>
<summary><b>应用类型专属能力</b></summary>

- **图库应用**——分类图片/视频，网格 / 列表 / 时间线视图，顺序 / 随机 / 单循
  播放，缩略图条，媒体信息，视频自动下一个，记忆播放位置
- **多网站应用**——标签 / 卡片 / 信息流 / 抽屉布局，每站点独立图标和 CSS 选
  择器，自动刷新间隔
- **网站爬虫**——离线包创建器：抓取整站前端（HTML / CSS / JS / 图片 / 字体），
  并发下载、递归 CSS `url()` 解析、绝对转相对路径重写、同域限制、深度/大小
  限制
- **应用修改器**——克隆已安装 APK，替换图标、名称、包名（二进制 manifest 修补）

</details>

<details>
<summary><b>翻译、通知、深度链接</b></summary>

- 页内翻译覆盖层，20 种目标语言，5 个引擎（Google / MyMemory / LibreTranslate
  / Lingva / Auto），悬浮按钮、加载即翻译
- Web API `Notification` polyfill + URL 轮询前台服务，可配置间隔和 JSON 解析
- 自定义 URL Scheme，可配置 host 匹配
- 开机自启、定时启动
- 后台运行前台服务，自定义通知、CPU wake lock、电池优化白名单

</details>

<details>
<summary><b>APK 导出与广告</b></summary>

- 自定义包名与版本
- 架构选择（通用 / ARM64 / ARM32）
- 性能优化：图片压缩、WebP 转换、代码压缩、懒加载、DNS 预取、preload
- 细粒度运行时权限（摄像头、麦克风、定位、存储、蓝牙、NFC、短信、联系人、
  日历、传感器、前台服务、wake lock、安装包、系统弹窗）
- Banner、插屏、开屏广告，可配置 ID 和时长
- 完整应用数据备份/恢复，项目导出/导入

</details>

---

## 从源码构建

**要求：** Android Studio Hedgehog 或更新版本、JDK 17、Gradle 8.14+。

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

Release 包请在 `app/build.gradle.kts` 中配置签名。

## 技术栈

Kotlin · Jetpack Compose · Material Design 3 · Room · GeckoView · OkHttp · KSP
· Native C++ (JNI)

## 参与贡献

| 类别 | 内容 | 指南 |
| --- | --- | --- |
| `modules/` | 给应用内市场提交一个社区模块 | [`modules/README.md`](modules/README.md) |
| Issues | 报告 Bug 或申请新特性 | GitHub Issues |
| 代码 | 从 issue 列表里挑一个，或主动提改进 | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

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

[The Unlicense](LICENSE)。高级功能（如强制运行）仅供技术演示，须在用户知情同
意下使用。

<div align="center">

**开源 · 永久免费 · Star 一下支持**

</div>
