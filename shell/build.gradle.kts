plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")


}

android {
    namespace = "com.webtoapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.webtoapp"
        minSdk = 23
        targetSdk = 36
        versionCode = 33
        versionName = "1.9.6"

        buildConfigField("boolean", "SHELL_RUNTIME_ONLY", "true")

        vectorDrawables {
            useSupportLibrary = true
        }


        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }


        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }


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




    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDirs("src/main/java", "src/main/java-overrides")
            res.srcDirs("../app/src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }


    splits {
        abi {
            isEnable = false
        }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true

            excludes += "**/libxul.so"
            excludes += "**/libmozglue.so"
            excludes += "**/libgeckoffi.so"
            excludes += "**/libmozavutil.so"
            excludes += "**/libmozavcodec.so"

            excludes += "**/libphp.so"
        }
    }
    androidResources {
        ignoreAssetsPattern = ""
    }
}



val syncShellRuntimeSources by tasks.registering(Sync::class) {
    description = "Sync runtime-only Kotlin sources from app module to shell"
    group = "build"

    from("../app/src/main/java")


    include(

        "**/ui/shell/**",
        "**/ui/theme/**",
        "**/ui/shared/**",



        "**/core/shell/**",
        "**/core/activation/**",
        "**/core/announcement/**",
        "**/core/adblock/**",
        "**/core/webview/**",
        "**/core/crypto/**",
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
        "**/core/extension/**",
        "**/core/gecko/**",
        "**/core/notification/**",
        "**/core/backgroundrun/**",
        "**/core/translate/**",
        "**/core/bgm/**",
        "**/core/engine/**",
        "**/core/hardening/**",
        "**/core/scraper/**",
        "**/core/script/**",
        "**/core/ads/**",
        "**/core/network/**",
        "**/core/errorpage/**",
        "**/core/golang/**",
        "**/core/python/**",
        "**/core/nodejs/**",
        "**/core/php/**",
        "**/core/wordpress/**",
        "**/core/autostart/**",
        "**/core/background/**",
        "**/core/linux/**",
        "**/core/download/**",
        "**/core/sample/**",
        "**/core/frontend/**",
        "**/core/kernel/**",


        "com/webtoapp/data/model/**",
        "com/webtoapp/data/converter/**",


        "**/ui/components/announcement/AnnouncementTemplates.kt",
        "**/ui/components/PremiumComponents.kt",
        "**/ui/components/PermissionRationale.kt",
        "**/ui/components/ThemeSelector.kt",
        "**/ui/components/StatusBarPreview.kt",
        "**/ui/components/EdgeSwipeRefreshLayout.kt",
        "**/ui/components/VirtualNavigationBar.kt",
        "**/ui/components/StatusBarBackground.kt",
        "**/ui/components/LongPressMenu.kt",
        "**/ui/components/ForcedRunCountdownOverlay.kt",


        "**/util/**"
    )


    exclude(

        "**/WebToAppApplication.kt",

        "**/core/crypto/EncryptedApkBuilder.kt",
        "**/core/crypto/SecurityInitializer.kt",

        "**/core/extension/AiModuleDeveloper.kt",
        "**/core/extension/agent/**",

        "**/core/autostart/AutoStartLauncher.kt",
        "**/core/autostart/BootReceiver.kt",
        "**/core/autostart/ScheduledStartReceiver.kt",

        "**/util/AppUpdateChecker.kt",
        "**/util/FaviconFetcher.kt",
        "**/util/UrlMetadataFetcher.kt",
        "**/util/MediaStorage.kt",
        "**/util/ZipProjectImporter.kt",
        "**/util/HtmlProjectHelper.kt",
        "**/util/OfflineManager.kt",

        "**/core/frontend/GitHubRepoFetcher.kt"
    )

    into("src/main/java")
}


tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(syncShellRuntimeSources)
}




val syncShellRuntimeAssets by tasks.registering(Copy::class) {
    description = "Mirror runtime-only asset files from app module to shell template (single source of truth: app/src/main/assets)."
    group = "build"

    from("../app/src/main/assets") {

        include("php_router_server.php")
    }

    into("src/main/assets")
}


tasks.matching { it.name.startsWith("merge") && it.name.contains("Assets") }.configureEach {
    dependsOn(syncShellRuntimeAssets)
}
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(syncShellRuntimeAssets)
}

dependencies {




    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.documentfile:documentfile:1.0.1")


    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")


    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("dev.chrisbanes.haze:haze:0.7.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")



    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-video:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")


    implementation("com.google.code.gson:gson:2.10.1")


    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")


    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")


    implementation("androidx.webkit:webkit:1.9.0")


    implementation("androidx.datastore:datastore-preferences:1.0.0")


    implementation("androidx.security:security-crypto:1.1.0-alpha06")


    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("org.tukaani:xz:1.9")


    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:137.0.20250414091429")


    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")




    implementation("com.android.billingclient:billing-ktx:7.0.0")


    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.browser:browser:1.8.0")


    implementation("com.android.tools.build:apksig:8.3.0")
}
