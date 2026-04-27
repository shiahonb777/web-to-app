plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // 注意：不使用 KSP — Shell 运行时不初始化 Room 数据库
    // Room 类保留仅用于编译期满足 WebToAppApplication 导入
}

android {
    namespace = "com.webtoapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.webtoapp"
        minSdk = 23
        targetSdk = 36
        versionCode = 32
        versionName = "1.9.5"

        buildConfigField("boolean", "SHELL_RUNTIME_ONLY", "true")

        vectorDrawables {
            useSupportLibrary = true
        }

        // NDK 配置 — 与 app 模块一致，确保 native 库可用
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // CMake 配置
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    // 外部 Native 构建配置 — 共享 app 模块的 CMakeLists.txt
    externalNativeBuild {
        cmake {
            path = file("../app/src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ★ 核心变更：使用 sync 任务从 app 模块复制运行时源码
    // shell 模块不再维护独立的最小化 stub，而是通过 syncShellRuntimeSources 任务
    // 从 app 模块复制仅运行时需要的源文件，排除编辑器专用代码
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDirs("src/main/java", "src/main/java-overrides")  // overrides 优先
            res.srcDirs("../app/src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }

    // 禁用 ABI splits — 与 app 模块一致
    splits {
        abi {
            isEnable = false
        }
    }

    // 允许以 "." 开头的 assets 目录被打包
    aaptOptions {
        ignoreAssetsPattern = ""
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            // 排除 GeckoView 原生库 — 与 app 模块一致
            excludes += "**/libxul.so"
            excludes += "**/libmozglue.so"
            excludes += "**/libgeckoffi.so"
            excludes += "**/libmozavutil.so"
            excludes += "**/libmozavcodec.so"
            // 排除 PHP 二进制 — 30MB，仅 PHP 服务器模式需要，ApkBuilder 按需注入
            excludes += "**/libphp.so"
        }
    }
}

// ★ 运行时源码同步任务 — 从 app 模块复制仅 Shell 运行时需要的 Kotlin 源文件
// 使用白名单（include）策略：只同步运行时需要的包，避免编辑器代码级联依赖
val syncShellRuntimeSources by tasks.registering(Sync::class) {
    description = "Sync runtime-only Kotlin sources from app module to shell"
    group = "build"

    from("../app/src/main/java")

    // ★ 白名单：仅包含 Shell 运行时需要的包
    include(
        // === Shell UI ===
        "**/ui/shell/**",
        "**/ui/theme/**",
        "**/ui/shared/**",
        // ui/splash — 使用 shell 专用覆盖（仅 ActivationDialog，不含 SplashLauncherActivity）

        // === Shell 核心运行时 ===
        "**/core/shell/**",
        "**/core/activation/**",
        "**/core/announcement/**",
        "**/core/adblock/**",
        "**/core/webview/**",
        "**/core/crypto/**",         // AssetDecryptor, AesCryptoEngine（运行时解密）
        "**/core/i18n/**",
        "**/core/logging/**",
        "**/core/dns/**",
        "**/core/forcedrun/**",
        "**/core/floatingwindow/**",
        "**/core/isolation/**",
        "**/core/disguise/**",
        "**/core/blacktech/**",
        "**/core/perf/**",
        "**/core/port/**",
        "**/core/extension/**",      // ExtensionManager（运行时加载扩展模块）
        "**/core/gecko/**",          // GeckoViewEngine
        "**/core/notification/**",
        "**/core/backgroundrun/**",
        "**/core/translate/**",
        "**/core/bgm/**",            // 背景音乐
        "**/core/engine/**",         // BrowserEngine 抽象层
        "**/core/hardening/**",      // AppHardeningEngine
        "**/core/scraper/**",        // WebsiteScraper
        "**/core/script/**",         // UserScriptStorage
        "**/core/ads/**",            // AdManager
        "**/core/network/**",         // NetworkModule（OkHttp 配置）
        "**/core/errorpage/**",       // 自定义错误页面
        "**/core/golang/**",          // GoRuntime（Go 服务器模式）
        "**/core/python/**",         // PythonRuntime（Python 服务器模式）
        "**/core/nodejs/**",         // NodeJsRuntime（Node.js 服务器模式）
        "**/core/php/**",            // PhpRuntime（PHP 服务器模式）
        "**/core/wordpress/**",      // WordPressRuntime（WordPress 服务器模式）
        "**/core/autostart/**",      // AutoStartManager（开机/定时自启动）
        "**/core/background/**",     // BackgroundRunService（后台运行）
        "**/core/linux/**",          // PerformanceOptimizer（WebApp.kt 引用）
        "**/core/download/**",      // DependencyDownloadEngine（WordPress 引用）
        "**/core/sample/**",         // SampleProjectExtractor（WordPress 引用）
        "**/core/frontend/**",      // FrontendFramework/BuildLogEntry（NodeProjectBuilder 引用）
        "**/core/kernel/**",         // BrowserKernel（WebViewManager/OAuthCompatEngine 引用）

        // === 数据模型 ===
        "com/webtoapp/data/model/**",
        // ui/data 中的数据模型（WebApp.kt 声明包 com.webtoapp.data.model 但物理路径在 ui/data/model/）
        "**/ui/data/model/**",
        "**/ui/data/converter/**",

        // === UI 组件（仅 Shell 运行时需要的）===
        "**/ui/components/announcement/AnnouncementTemplates.kt",  // AnnouncementDialog/Config/Template
        "**/ui/components/PremiumComponents.kt",   // PremiumButton（ActivationDialog 需要）
        "**/ui/components/PermissionRationale.kt", // 权限说明
        "**/ui/components/ThemeSelector.kt",       // 主题选择
        "**/ui/components/StatusBarPreview.kt",   // 状态栏预览
        "**/ui/components/EdgeSwipeRefreshLayout.kt", // 下拉刷新
        "**/ui/components/VirtualNavigationBar.kt",  // 虚拟导航栏
        "**/ui/components/StatusBarBackground.kt",   // 状态栏背景/StatusBarOverlay
        "**/ui/components/LongPressMenu.kt",         // FloatingBubbleLongPressMenu, ContextMenuLongPressMenu
        "**/ui/components/ForcedRunCountdownOverlay.kt", // 强制运行倒计时

        // === 工具类 ===
        "**/util/**"
    )

    // ★ 黑名单：从白名单中进一步排除编辑器专用文件
    exclude(
        // WebToAppApplication — 使用 shell 专用版本
        "**/WebToAppApplication.kt",
        // crypto 中的编辑器专用文件
        "**/core/crypto/EncryptedApkBuilder.kt",
        "**/core/crypto/SecurityInitializer.kt",
        // extension 中的编辑器专用文件
        "**/core/extension/AiModuleDeveloper.kt",
        "**/core/extension/agent/**",
        // autostart 中引用 WebViewActivity 的文件（使用 shell 专用覆盖）
        "**/core/autostart/AutoStartLauncher.kt",
        "**/core/autostart/BootReceiver.kt",
        "**/core/autostart/ScheduledStartReceiver.kt",
        // util 中的编辑器专用工具
        "**/util/AppUpdateChecker.kt",
        "**/util/FaviconFetcher.kt",
        "**/util/UrlMetadataFetcher.kt",
        "**/util/MediaStorage.kt",
        "**/util/ZipProjectImporter.kt",
        "**/util/HtmlProjectHelper.kt",
        "**/util/OfflineManager.kt"
    )

    into("src/main/java")
}

// 确保编译前源码已同步
tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(syncShellRuntimeSources)
}

dependencies {
    // ===== 与 app 模块完全一致的依赖 =====
    // R8 会在 release 构建中裁剪掉未使用的依赖

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Material Design 3
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("dev.chrisbanes.haze:haze:0.7.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Room Database — 编译期需要（WebToAppApplication 导入），运行时 SHELL_RUNTIME_ONLY 不初始化
    // 不使用 KSP，Room 注解处理器不运行，但编译期 API 仍可用
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-video:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")

    // Koin for dependency injection — 编译期需要，运行时 SHELL_RUNTIME_ONLY 不初始化
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    // WebKit for advanced WebView features
    implementation("androidx.webkit:webkit:1.9.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Security - EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Apache Commons Compress — 运行时 Node.js/PHP 等需要
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("org.tukaani:xz:1.9")

    // GeckoView (Firefox 内核) — API 编译进 dex，原生 .so 排除
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:137.0.20250414091429")

    // ZXing — 运行时 WebView 二维码扫描可能需要
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Markdown rendering — 运行时文档查看可能需要
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")

    // Google Play Billing — 编译期需要，运行时 R8 裁剪
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    // Google Sign-In — 编译期需要
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.browser:browser:1.8.0")

    // APK 签名库 — JarSigner 引用（但 JarSigner 已排除，仅保留传递依赖）
    implementation("com.android.tools.build:apksig:8.3.0")
}
