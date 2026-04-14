import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Read signing config from local.properties (not committed to VCS)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {

    signingConfigs {
        create("shiaho") {
            storeFile = file(localProperties.getProperty("signing.storeFile", ""))
            storePassword = localProperties.getProperty("signing.storePassword", "")
            keyAlias = localProperties.getProperty("signing.keyAlias", "")
            keyPassword = localProperties.getProperty("signing.keyPassword", "")
        }
    }
    namespace = "com.webtoapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.webtoapp"
        minSdk = 23
        targetSdk = 36
        versionCode = 32
        versionName = "1.9.5"

        vectorDrawables {
            useSupportLibrary = true
        }
        
        // NDK setup
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
        
        // CMake setup
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                // Enable 16 KB page size support for NDK r27+
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                )
            }
        }
    }
    
    // External native build setup
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            // Keep shrinkers on, but preserve class names for template reuse.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("shiaho")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Disable ABI splits so the app can reuse its own APK as a template.
    splits {
        abi {
            isEnable = false
        }
    }
    
    // Keep dot-prefixed assets such as .pypackages.
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
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Keep native libraries uncompressed.
        jniLibs {
            useLegacyPackaging = true
            // Exclude GeckoView native libs because they are downloaded on demand.
            excludes += "**/libxul.so"
            excludes += "**/libmozglue.so"
            excludes += "**/libgeckoffi.so"
            excludes += "**/libmozavutil.so"
            excludes += "**/libmozavcodec.so"
        }
    }
}

dependencies {
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
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

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
    
    // Koin for dependency injection
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    // WebKit for advanced WebView features
    implementation("androidx.webkit:webkit:1.9.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Security - EncryptedSharedPreferences for secure token storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Apache Commons Compress for tar.gz/xz extraction (Linux environment)
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("org.tukaani:xz:1.9")
    
    // APK signing
    implementation("com.android.tools.build:apksig:8.3.0")
    
    // GeckoView API only; native libs stay out of the base APK.
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:137.0.20250414091429")
    
    // ZXing for QR generation and scanning
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Vico charts
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-beta.3")
    
    // Note.
    implementation("com.android.billingclient:billing-ktx:7.0.0")
    
    // Google Sign-In (Credential Manager + Web OAuth fallback)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.browser:browser:1.8.0") // Chrome Custom Tab for OAuth fallback
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// ==================== PHP Binary Setup ====================
// Android 15+ enforces SELinux execute_no_trans denial for binaries in app data dir.
// Bundling PHP as a "native library" in jniLibs/ ensures it's extracted to nativeLibraryDir
// which has apk_data_file SELinux context (execute_no_trans allowed).
//
// Run: ./gradlew downloadPhpBinary
// This only needs to run once (the binary is gitignored).

tasks.register("downloadPhpBinary") {
    description = "Downloads pre-built PHP binary for Android arm64 and bundles it as native library"
    group = "setup"
    
    val phpVersion = "8.4"
    val jniLibsDir = file("src/main/jniLibs/arm64-v8a")
    val outputFile = File(jniLibsDir, "libphp.so")
    
    onlyIf { !outputFile.exists() }
    
    doLast {
        jniLibsDir.mkdirs()
        val url = "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-${phpVersion}-latest/PHP-${phpVersion}-Android-arm64-PM5.tar.gz"
        val tempDir = File(project.layout.buildDirectory.asFile.get(), "tmp/php-download")
        tempDir.mkdirs()
        val tarFile = File(tempDir, "php.tar.gz")
        
        println("Downloading PHP $phpVersion for Android arm64...")
        project.exec { commandLine("curl", "-L", "-f", "-o", tarFile.absolutePath, url) }
        
        println("Extracting PHP binary...")
        project.exec { commandLine("tar", "-xzf", tarFile.absolutePath, "-C", tempDir.absolutePath) }
        
        // pmmp archives may store the binary at bin/php or php.
        val extracted = File(tempDir, "bin/php").takeIf { it.exists() }
            ?: tempDir.walkTopDown().firstOrNull { it.name == "php" && it.isFile }
            ?: throw GradleException("PHP binary not found in archive")
        
        extracted.copyTo(outputFile, overwrite = true)
        outputFile.setExecutable(true)
        tempDir.deleteRecursively()
        
        println("PHP binary installed: ${outputFile.relativeTo(rootDir)}")
    }
}

