<div align="center">

# WebToApp

### 任意网站。一键。一个应用。

无需 IDE。无需构建服务器。无需电脑。

[English](README.md) | **简体中文**

[![Stars](https://img.shields.io/github/stars/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/stargazers)
[![Forks](https://img.shields.io/github/forks/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/network/members)
[![License](https://img.shields.io/badge/License-Unlicense-blue?style=for-the-badge)](LICENSE)

</div>

---

## 截图

<div align="center">
<img src="png/1.png" width="24%" /><img src="png/2.png" width="24%" /><img src="png/3.png" width="24%" /><img src="png/4.png" width="24%" />
<img src="png/5.png" width="24%" /><img src="png/6.png" width="24%" /><img src="png/7.png" width="24%" /><img src="png/8.png" width="24%" />
<img src="png/9.png" width="24%" /><img src="png/10.png" width="24%" />
</div>

## 功能

WebToApp 将网站、HTML 项目、媒体文件和服务端应用转换为独立 Android APK — 手机上完成，无需电脑。

**支持类型：** 网站 / HTML / React / Vue / WordPress / Node.js / PHP / Python / Go / 图片 / 视频 / 图库 / 多网站

## 核心特性

- **一键构建 APK** — 设备上直接生成可安装 APK
- **双浏览器引擎** — WebView + GeckoView (Firefox) 最大兼容性
- **扩展模块** — 10 个内置模块 + 自定义 JS/CSS 注入 + NativeBridge API
- **AI 辅助** — AI 驱动模块开发、图标生成、HTML 编码
- **安全** — APK 加密、浏览器指纹伪装、广告拦截、DNS-over-HTTPS
- **自定义** — 启动屏、背景音乐+歌词、主题、激活码、公告
- **应用修改器** — 克隆已安装应用，替换图标和名称
- **云服务** — 可选 Pro/Ultra：云端项目、数据分析、推送通知、远程配置

## 核心技术实现

- **深度修改 WebView 内核** — 原生级 WebView 加固：UA 清洗、X-Requested-With 移除、原型链保护、iframe 传播。WebView 与真实 Chrome 无法区分。
- **OAuth 应用内登录** — 30+ OAuth 提供商（Google、Facebook、GitHub、Discord、微信、支付宝、PayPal 等）通过逐提供商反检测 JS 注入直接在 WebView 内工作。Google OAuth 回退到 Chrome Custom Tab 共享 cookie 会话。
- **Chrome 扩展运行时** — 完整 `chrome.*` API polyfill（runtime、storage、tabs、messaging）使桌面 Chrome 扩展在 WebView 内运行。后台脚本在隔离 WebView 中执行，同源 fetch + 共享 cookie 状态。
- **22 向量浏览器指纹伪装** — Canvas、WebGL、AudioContext、屏幕、字体、GPU 信息 — 全部伪装且跨向量一致。原型链钩子 `.toString()` 返回 `[native code]`。
- **本地服务端运行时** — Node.js / PHP / Python / Go 通过设备端编译和本地 HTTP 服务在应用内运行。无需远程服务器。

## 应用配置功能

每个生成的应用拥有完整配置面：

**WebView 控制** — 桌面模式、自定义 UA、JS/CSS 注入 (DOCUMENT_START/END/IDLE)、弹窗拦截、新窗口行为（同标签/外部浏览器/弹窗/阻止）、代理 (HTTP/SOCKS5/PAC)、DNS-over-HTTPS (7 个预设+自定义)、屏幕方向 (7 种)、屏幕常亮+亮度、键盘调整、视口模式、支付方案、跨域隔离、PWA 离线支持、错误页自定义

**悬浮窗** — 可调整大小悬浮窗，尺寸/透明度/圆角/边框样式(微妙/发光/强调)/边缘吸附/位置锁定/自动隐藏标题栏/启动最小化/记住位置

**状态栏** — 完整状态栏主题：颜色、深/浅色图标、背景类型(颜色/图片)、透明度、高度 — 深色模式独立配置

**安全与隐私** — APK 加密 (100000 PBKDF2 迭代，支持自定义密码)，应用隔离(独立数据目录)，浏览器/设备指纹伪装，广告拦截(hosts 规则+cosmetic MutationObserver)，强制运行，激活码门控(按时或持久)

**应用加固** — DEX 加密+拆分+VMP+控制流平坦化，原生 SO 加密+ELF 混淆+符号剥离+反 dump，反 Frida/Xposed/Magisk/调试/内存 dump/截屏，模拟器/VirtualApp/VPN/USB 调试检测，字符串加密+类名混淆+调用间接+不透明谓词，DEX CRC 校验+内存完整性+JNI 验证+时序检查，多点签名验证+APK 校验和+资源完整性+证书固定，威胁响应+蜜罐+自毁

**强制运行** — 3 种模式：固定时段/倒计时/访问窗口。阻止系统 UI、返回键、Home 键、最近应用、通知。倒计时跨进程杀死持久化。密码紧急退出。结束前预警。

**BlackTech** — 音量控制(强制最大/静音/阻止按键)，手电筒模式(频闪/SOS/摩尔斯码/心跳/呼吸/紧急+自定义警报+震动同步)，系统控制(阻止电源键、最大性能)，屏幕控制(黑屏、旋转、阻止触摸、强制唤醒)，网络控制(WiFi 热点+SSID/密码、禁用 WiFi/蓝牙/移动数据)，核弹模式(全开)，隐身模式(静音+黑屏+阻止触摸+断网)

**设备伪装** — 伪装设备身份：设备类型(手机/平板/桌面)，操作系统(Android/iOS/HarmonyOS/macOS/Windows/Linux)，品牌+型号预设，屏幕分辨率，像素比，时区，语言

**启动屏与 BGM** — 自定义启动屏(图片/视频+音频、点击跳过、裁剪范围、方向)，背景音乐播放列表+LRC 歌词同步，7 种歌词动画(淡入/滑动/缩放/打字机/卡拉OK)，3 种位置，自定义歌词主题(字体/大小/颜色/描边/阴影)，20 个音乐标签，循环/顺序/随机模式

**公告** — 10 种模板风格(极简/小红书/渐变/毛玻璃/霓虹/可爱/优雅/节日/暗黑/自然)，触发时机：启动/定时/无网络

**翻译** — 页内翻译覆盖层，20 种目标语言，5 个引擎(Google/MyMemory/LibreTranslate/Lingva/Auto)，悬浮按钮切换，加载时自动翻译

**通知** — Web API Notification polyfill + URL 轮询前台服务，可配置间隔和 JSON 解析

**深度链接** — 自定义 scheme 支持，可配置 host 匹配

**自动启动** — 开机自启，定时启动

**后台运行** — 前台服务保活，自定义通知标题/内容，CPU wake lock，电池优化白名单

**图库应用** — 分类图片/视频图库，网格/列表/时间线视图，顺序/随机/单曲循环播放，按自定义/名称/日期/类型排序，缩略图条，媒体信息，视频自动下一个，记忆播放位置

**多网站应用** — 多站点标签/卡片/信息流/抽屉布局，每站图标和 CSS 选择器，自动刷新间隔

**服务端运行时** — Node.js (4 种构建模式: 静态/SSR/API 后端/全栈，环境变量)，WordPress (主题+插件，内置 PHP)，PHP (Composer 支持，自定义文档根)，Python (Flask/Django/builtin server，pip 依赖)，Go (二进制编译，静态文件服务)

**网站爬虫** — 离线包创建器：爬取整个网站前端 (HTML/CSS/JS/图片/字体)，并发下载，递归 CSS url() 解析，绝对转相对路径重写，同域限制，深度/大小限制

**应用修改器** — 克隆已安装 APK：替换图标、名称、包名，通过二进制 Manifest 修补

**扩展模块** — 10 个内置模块(视频下载器、B站/抖音/小红书提取器、视频增强、网页分析、暗色模式、隐私保护、内容增强、元素拦截) + 自定义模块 3 种来源(自定义 JS / userscript .user.js / Chrome 扩展 manifest.json)。完整 Greasemonkey/Tampermonkey GM_* API 桥接。MV3 declarativeNetRequest 规则引擎(block/allow/redirect/modifyHeaders)。模块分享通过导出码。

**APK 导出** — 自定义包名/版本，架构选择(通用/ARM64/ARM32)，性能优化(图片压缩/WebP 转换、代码压缩、懒加载、DNS 预取、预加载提示)，运行时权限(摄像头/麦克风/位置/存储/蓝牙/NFC/短信/通讯录/日历/传感器/前台服务/wake lock/安装包/系统弹窗)

**广告** — Banner + 插屏 + 开屏广告支持，可配置 ID 和时长

**主题** — Aurora 主题系统，动态颜色生成

**数据备份** — 完整应用数据备份和恢复，项目导出/导入

## 快速开始

1. 在 Android 设备上安装 WebToApp
2. 点击 "+" 创建新应用 — 输入 URL 或导入项目
3. 自定义图标、设置和功能
4. 点击 "构建 APK" — 安装完成

## 从源码构建

**要求：** Android Studio Hedgehog+、JDK 17、Gradle 8.14+

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

Release 构建需在 `app/build.gradle.kts` 中配置签名。

## 技术栈

Kotlin / Jetpack Compose / Material Design 3 / Room / GeckoView / OkHttp / KSP / Native C++ (JNI)

## 联系方式

开发者：shiaho

| 平台 | 链接 |
|------|------|
| GitHub | [github.com/shiahonb777/web-to-app](https://github.com/shiahonb777/web-to-app) |
| Telegram | [t.me/webtoapp777](https://t.me/webtoapp777) |
| X (Twitter) | [x.com/@shiaho777](https://x.com/@shiaho777) |
| Bilibili | [b23.tv/8mGDo2N](https://b23.tv/8mGDo2N) |
| QQ 群 | 1041130206 |

## 许可证

The Unlicense. 高级功能（如强制运行）仅供技术演示，须在知情同意下使用。

<div align="center">

**开源 · 永久免费 · Star 支持一下**

</div>
