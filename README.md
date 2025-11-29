# WebToApp - 网站转App生成器

一款Android原生应用，可将任意网站URL转换为独立的Android App。

## 功能特性

### 核心功能
- **URL转App**：输入任意网址，一键生成独立应用
- **自定义图标**：支持从相册选择自定义应用图标
- **自定义名称**：自定义应用显示名称

### 集成功能
- **激活码验证**：内置激活码机制，可限制应用使用
- **弹窗公告**：启动时显示公告信息，支持链接跳转
- **广告拦截**：内置广告拦截引擎，自动过滤网页广告
- **广告集成**：预留广告SDK接口（横幅/插屏/开屏）

### 导出功能
- **桌面快捷方式**：创建桌面图标，像原生App一样启动
- **构建APK安装包**：直接生成独立APK并安装，无需Android Studio
- **项目模板导出**：导出完整Android Studio项目，可自行编译APK

### 应用图标修改器（新功能）
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
│   ├── announcement/         # 公告管理
│   ├── apkbuilder/          # APK构建器（新）
│   │   ├── ApkBuilder.kt    # 构建核心
│   │   ├── ApkSigner.kt     # APK签名
│   │   └── ApkTemplate.kt   # 模板管理
│   ├── appmodifier/         # 应用修改器（新）
│   │   ├── AppCloner.kt     # 应用克隆
│   │   ├── AppListProvider.kt # 应用列表
│   │   └── InstalledAppInfo.kt # 应用信息
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
│   ├── navigation/          # 导航
│   ├── screens/             # 页面
│   │   ├── HomeScreen.kt    # 主页
│   │   ├── CreateAppScreen.kt # 创建应用
│   │   └── AppModifierScreen.kt # 应用修改器（新）
│   ├── theme/               # 主题
│   ├── viewmodel/           # ViewModel
│   └── webview/             # WebView Activity
└── util/                      # 工具类
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
