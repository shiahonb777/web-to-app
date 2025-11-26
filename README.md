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
- **项目模板导出**：导出完整Android Studio项目，可自行编译APK

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

### 导出为独立APK
1. 点击应用卡片右侧菜单
2. 选择 "导出项目"
3. 在导出目录找到项目文件夹
4. 使用Android Studio打开并编译

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

### v1.0.0
- 初始版本发布
- 支持URL转App基本功能
- 支持激活码、公告、广告拦截
- 支持快捷方式创建和项目导出
