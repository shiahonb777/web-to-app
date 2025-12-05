<div align="center">

# WebToApp 🚀

**[English](README_EN.md) | 简体中文**

**无需编程，一键将任意网站或媒体转换为独立 Android 应用！**

[![GitHub stars](https://img.shields.io/github/stars/shiahonb777/web-to-app?style=social)](https://github.com/shiahonb777/web-to-app)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

WebToApp 是一款功能强大的 Android 原生应用，让你无需任何编程知识，即可将网站、图片、视频转换为独立的 App。支持直接构建 APK 安装包，真正做到「零门槛出包」。

## ✨ 核心亮点

- 🌐 **网站转App** - 输入任意网址，生成独立 WebView 应用
- 🎬 **媒体转App** - 图片/视频一键转换为全屏展示应用
- 💻 **HTML转App** - HTML/CSS/JS 项目转换为独立应用（新）
- 🤖 **AI 编程助手** - AI 辅助生成 HTML 代码，实时预览（新）
- 🎵 **AI 字幕生成** - 音频自动生成 LRC 歌词，时间轴精准（新）
- 🎨 **主题系统** - 多种精美主题，支持动画效果定制（新）
- 📦 **一键出包** - 无需 Android Studio，直接生成可安装 APK
- 🛡️ **内置功能** - 广告拦截、激活码验证、背景音乐、公告弹窗
- ⚡ **应用修改器** - 修改已安装应用的图标和名称

## 🎯 适用场景

- 📱 将常用网站封装为独立 App，桌面一键直达
- 🎞️ 制作电子相册、视频壁纸、产品展示应用
- 💻 前端项目快速打包成 Android 应用
- 🎵 为音频生成同步歌词字幕
- 🏢 企业内部系统快速 App 化
- 🎮 游戏/工具类 H5 应用独立封装
- 🔧 给已安装应用换个喜欢的图标

## 📋 功能特性

### 核心功能
- **URL转App**：输入任意网址，一键生成独立应用
- **媒体转App**：支持图片/视频转换为独立应用
- **HTML转App**：支持 HTML/CSS/JS 项目转换为独立应用（v1.5 新增）
- **自定义图标**：支持从相册选择自定义应用图标
- **自定义名称**：自定义应用显示名称

### 集成功能
- **启动画面**：支持图片/视频启动动画，内置视频裁剪器
- **用户脚本注入**：支持自定义 JavaScript 脚本注入
- **背景音乐**：支持为应用添加 BGM，可配合歌词同步显示（v1.5 新增）
- **激活码验证**：内置激活码机制，可限制应用使用
- **弹窗公告**：启动时显示公告信息，支持链接跳转
- **广告拦截**：内置广告拦截引擎，自动过滤网页广告
- **广告集成**：预留广告SDK接口（横幅/插屏/开屏）

### 导出功能
- **桌面快捷方式**：创建桌面图标，像原生App一样启动
- **构建APK安装包**：直接生成独立APK并安装，无需Android Studio
- **项目模板导出**：导出完整Android Studio项目，可自行编译APK

### 媒体应用功能
- **图片转App**：选择图片生成全屏展示应用
- **视频转App**：选择视频生成循环播放应用
- **显示配置**：支持音频开关、循环播放、自动播放、铺满屏幕
- **APK打包**：媒体应用支持导出为独立APK

### AI 功能（v1.5 新增）
- **AI LRC 字幕生成**：使用 AI 分析音频，自动生成时间轴精准的 LRC 歌词
- **多供应商支持**：Google Gemini、OpenAI GPT-4o、智谱 GLM、火山引擎、MiniMax、OpenRouter 等
- **AI HTML 编程**：AI 辅助生成和修改 HTML/CSS/JS 代码
- **会话管理**：支持多会话、模板选择、样式定制
- **实时预览**：代码生成后可直接预览效果
- **AI 设置**：统一管理 API 密钥和模型配置
- **任务管理**：查看和管理 LRC 生成任务状态

### 主题系统（v1.5 新增）
- **多种主题**：内置多款精美主题风格
- **深色模式**：支持跟随系统、手动切换
- **动画效果**：可自定义动画开关和速度
- **粒子特效**：部分主题支持粒子背景效果

### 应用图标修改器
- **应用列表扫描**：自动获取设备上已安装的应用列表
- **图标/名称修改**：自由修改任意应用的图标和显示名称
- **克隆安装**：将修改后的应用作为新应用安装（独立包名）
- **快捷方式启动**：创建使用新图标的快捷方式，启动原应用

## 技术栈

- **语言**：Kotlin 1.9+
- **UI框架**：Jetpack Compose + Material Design 3
- **架构**：MVVM + Repository
- **数据库**：Room
- **网络**：OkHttp
- **图片加载**：Coil
- **最低支持**：Android 7.0 (API 24)

## 项目结构

```
app/src/main/java/com/webtoapp/
├── WebToAppApplication.kt      # Application类
├── core/                       # 核心功能模块
│   ├── activation/            # 激活码管理
│   ├── adblock/              # 广告拦截
│   ├── ads/                  # 广告集成
│   ├── ai/                   # AI 功能（v1.5 新增）
│   │   ├── AiApiClient.kt   # AI API 客户端
│   │   ├── AiConfigManager.kt # AI 配置管理
│   │   ├── LrcGenerationService.kt # LRC 生成服务
│   │   ├── LrcTaskManager.kt # 任务管理
│   │   └── htmlcoding/      # AI HTML 编程（v1.5 新增）
│   ├── announcement/         # 公告管理
│   ├── apkbuilder/          # APK构建器
│   │   ├── ApkBuilder.kt    # 构建核心
│   │   ├── ApkSigner.kt     # APK签名
│   │   └── ApkTemplate.kt   # 模板管理
│   ├── appmodifier/         # 应用修改器
│   │   ├── AppCloner.kt     # 应用克隆
│   │   ├── AppListProvider.kt # 应用列表
│   │   └── InstalledAppInfo.kt # 应用信息
│   ├── bgm/                 # 背景音乐（v1.5 新增）
│   ├── export/              # 导出功能
│   └── webview/             # WebView管理
├── data/                      # 数据层
│   ├── converter/           # 类型转换器
│   ├── dao/                 # 数据访问对象
│   ├── database/            # Room数据库
│   ├── model/               # 数据模型
│   └── repository/          # 数据仓库
├── ui/                        # UI层
│   ├── MainActivity.kt      # 主Activity
│   ├── media/               # 媒体应用
│   │   └── MediaAppActivity.kt # 媒体展示Activity
│   ├── navigation/          # 导航
│   ├── screens/             # 页面
│   │   ├── HomeScreen.kt    # 主页
│   │   ├── CreateAppScreen.kt # 创建应用
│   │   ├── CreateMediaAppScreen.kt # 创建媒体应用
│   │   ├── CreateHtmlAppScreen.kt # 创建HTML应用（v1.5 新增）
│   │   ├── HtmlCodingScreen.kt # AI HTML编程（v1.5 新增）
│   │   ├── AiSettingsScreen.kt # AI设置（v1.5 新增）
│   │   ├── LrcTaskManagerScreen.kt # LRC任务管理（v1.5 新增）
│   │   ├── ThemeSettingsScreen.kt # 主题设置（v1.5 新增）
│   │   ├── AboutScreen.kt   # 关于作者
│   │   └── AppModifierScreen.kt # 应用修改器
│   ├── theme/               # 主题系统（v1.5 新增）
│   ├── viewmodel/           # ViewModel
│   └── webview/             # WebView Activity
└── util/                      # 工具类
    └── MediaStorage.kt      # 媒体文件存储
```

## 使用说明

### 创建应用
1. 点击主页的 "创建应用" 按钮
2. 填写应用名称和网站地址
3. （可选）选择自定义图标
4. （可选）配置激活码、公告、广告拦截等功能
5. 点击保存

### 运行应用
- 点击应用卡片直接预览运行
- 长按或点击菜单可进行更多操作

### 创建桌面快捷方式
1. 点击应用卡片右侧菜单
2. 选择 "创建快捷方式"
3. 确认添加到桌面

### 构建APK安装包（新功能）
1. 点击应用卡片右侧菜单
2. 选择 "构建 APK"
3. 点击 "开始构建"
4. 构建完成后自动弹出安装界面

### 导出为项目模板
1. 点击应用卡片右侧菜单
2. 选择 "导出项目"
3. 在导出目录找到项目文件夹
4. 使用Android Studio打开并编译

### 使用应用修改器（新功能）
1. 点击主页右上角的应用图标按钮
2. 在应用列表中搜索或筛选目标应用
3. 点击应用进入修改界面
4. 选择新图标、输入新名称
5. 选择操作方式：
   - **快捷方式**：创建使用新图标的桌面快捷方式
   - **克隆安装**：生成新APK并安装为独立应用

## 编译说明

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Gradle 8.2

### 编译步骤
```bash
# 克隆项目
git clone <repository_url>

# 进入项目目录
cd 网站转app

# 编译Debug版本
./gradlew assembleDebug

# 编译Release版本
./gradlew assembleRelease
```

### 签名配置
Release版本需要配置签名，在 `app/build.gradle.kts` 中添加：
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("your-keystore.jks")
        storePassword = "your-store-password"
        keyAlias = "your-key-alias"
        keyPassword = "your-key-password"
    }
}
```

## 广告拦截规则

内置常见广告域名拦截，支持自定义规则：
- 域名规则：`||example.com` 或直接输入域名
- 通配符规则：`*ads*`、`*/banner/*`

## 激活码机制

- 支持批量设置多个激活码
- 激活状态本地持久化
- 支持SHA-256加密校验

## 注意事项

1. 部分网站可能有反爬虫机制，加载可能受限
2. 需要网络权限才能正常使用
3. 导出的项目需要在PC端用Android Studio编译
4. 激活码仅本地验证，如需服务端验证请自行扩展

## License

MIT License

## 更新日志

### v1.5.0
**新增功能**
- AI LRC 字幕生成：使用 AI 分析音频自动生成 LRC 格式歌词
  - 支持多种 AI 供应商：Google Gemini、OpenAI GPT-4o、智谱 GLM、火山引擎、MiniMax、OpenRouter 等
  - 时间轴精准对齐，支持中/英/日/韩多语言
- AI HTML 编程助手：使用 AI 辅助生成和修改 HTML 代码
  - 支持多种文本/图像生成模型
  - 会话管理、模板选择、样式定制
  - 代码块解析、实时预览
- AI 设置界面：统一管理 API 密钥和模型
  - 支持添加多个 API Key，实时测试连接
  - 支持自定义 Base URL，模型列表从 API 实时获取
- HTML 应用：支持将 HTML/CSS/JS 项目转换为独立 Android 应用
- 主题系统：全新的主题定制功能
  - 内置多款精美主题风格，支持深色模式
  - 可自定义动画效果开关和速度
- 背景音乐（BGM）：为应用添加背景音乐
  - 支持 LRC 歌词同步显示、循环播放

**优化改进**
- 主页 UI 整合 AI 编程、主题设置、AI 设置入口
- FAB 菜单新增 HTML 应用创建入口

### v1.3.0
**新增功能**
- 媒体应用：支持图片/视频转换为独立 App
  - 图片转 App：全屏展示，支持铺满屏幕
  - 视频转 App：支持循环播放、音频开关、自动播放
  - 媒体应用支持导出为独立 APK
- 用户脚本注入：支持自定义 JavaScript 脚本
  - 支持多个脚本管理（启用/禁用）
  - 支持页面加载前/后执行时机
  - 导出 APK 完整支持脚本注入
- 启动画面（Splash Screen）：支持设置应用启动时显示的图片或视频
  - 支持图片启动画面，可设置显示时长
  - 支持视频启动画面，内置视频裁剪器（时长不限）
  - 视频启动画面支持音频开关
  - 支持点击跳过、横屏显示、铺满屏幕
- 视频裁剪组件：可视化选择视频片段，实时预览

**优化改进**
- 数据模型重构，支持视频裁剪配置持久化
- Shell 模式（导出 APK）完整支持启动画面播放
- 优化 MediaPlayer 视频播放，支持精确 seek 和自动停止
- 主页 FAB 改为展开菜单，支持创建网页应用和媒体应用

**Bug 修复**
- 修复快捷方式图标错误使用启动图片的问题
- 修复数据库 schema 不匹配导致的闪退问题

### v1.2.3
**Bug 修复**
- 修复构建 APK 图标被放大裁剪的问题
  - 根因：未遵循 Android Adaptive Icon 规范，图标直接填满前景层导致被形状遮罩裁剪
  - 方案：为图标预留 safe zone 边距（72dp 安全区域 + 18dp 边距）
- 遵循 Android Adaptive Icon 规范处理图标
- 提升图标清晰度（使用 xxxhdpi 432px 分辨率）

### v1.2.2
**Bug 修复**
- 修复 Release 版本构建 APK 时自定义图标不生效的问题
  - 根因：AAPT2 资源路径缩短优化导致资源路径被混淆
  - 方案：禁用资源路径缩短，确保图标替换逻辑正常工作
- 优化 ArscEditor 图标路径替换，支持 `drawable`、`drawable-v24`、`drawable-anydpi-v24` 等多目录
- 清理冗余调试代码，优化代码结构

### v1.2.1
**新增功能**
- 全屏模式：隐藏工具栏，无浏览器特征，让 WebApp 更像原生应用

### v1.2.0
**Bug 修复**
- 修复导出APK包名非法（段首数字）导致安装失败
- 修复导出APK权限/Provider冲突问题（自定义权限、FileProvider、StartupProvider）
- 修复克隆应用多次克隆同一应用时包名重复问题

### v1.1.0
**新增功能**
- 一键构建独立 APK 安装包（无需 Android Studio）
- 应用修改器：修改已安装应用的图标和名称
- 克隆安装：生成独立包名的克隆应用
- 访问电脑版：强制桌面模式加载网页
- 启动自动请求运行时权限
- 关于作者页面（QQ群：1041130206）

**优化改进**
- 全新 Material Design 3 界面
- 优化图标替换逻辑（支持自适应图标）
- 使用官方 apksig 签名库

**Bug 修复**
- 修复 APK 签名冲突问题
- 修复主页点击卡片空白问题
- 修复 resources.arsc 压缩导致安装失败

### v1.0.0
- 初始版本发布
- 支持 URL 转快捷方式基本功能
- 支持激活码、公告、广告拦截
- 支持项目模板导出

## 联系作者

- **QQ群**：1041130206（作者每天互动，发布更新消息和最新安装包）
- **作者QQ**：2711674184
- 本应用由作者（shihao）独立开发，有任何问题都可以找我
- 招 AI 编程队友，有想法可以一起实现！
