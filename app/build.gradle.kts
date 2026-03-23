plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {

    signingConfigs {
        create("shiaho") {
            storeFile = file("/Users/shiaho/Desktop/shiaho-key.jks")
            storePassword = "shiaho"
            keyAlias = "shiaho"
            keyPassword = "shiaho"
        }
    }
    namespace = "com.webtoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.webtoapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 32
        versionName = "1.9.5"

        vectorDrawables {
            useSupportLibrary = true
        }
        
        // NDK 配置
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
    
    // 外部 Native 构建配置
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            // 禁用代码混淆和资源压缩
            // WebToApp 需要使用自身 APK 作为模板，混淆会导致 Shell APK 无法运行
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("shiaho")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // 禁用 ABI splits，生成单一完整 APK
    // 这对于 WebToApp 是必要的，因为应用需要使用自身 APK 作为模板
    splits {
        abi {
            isEnable = false
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
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // 确保 native 库不被压缩，否则安装会失败
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Material Design 3
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // WebKit for advanced WebView features
    implementation("androidx.webkit:webkit:1.9.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Apache Commons Compress for tar.gz/xz extraction (Linux environment)
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("org.tukaani:xz:1.9")
    
    // APK 签名库（支持 v1/v2/v3 签名）
    implementation("com.android.tools.build:apksig:8.3.0")
    
    // ZXing 二维码生成和扫描
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.5")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
