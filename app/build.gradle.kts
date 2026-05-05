import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val releaseSigningStoreFile = localProperties.getProperty("signing.storeFile")
    ?.takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }
val hasReleaseSigningConfig = releaseSigningStoreFile?.isFile == true &&
    !localProperties.getProperty("signing.storePassword").isNullOrBlank() &&
    !localProperties.getProperty("signing.keyAlias").isNullOrBlank() &&
    !localProperties.getProperty("signing.keyPassword").isNullOrBlank()

android {

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = releaseSigningStoreFile
                storePassword = localProperties.getProperty("signing.storePassword")
                keyAlias = localProperties.getProperty("signing.keyAlias")
                keyPassword = localProperties.getProperty("signing.keyPassword")
            }
        }
    }
    namespace = "com.webtoapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.webtoapp"
        minSdk = 23
        targetSdk = 36
        versionCode = 33
        versionName = "1.9.6"
        buildConfigField("boolean", "SHELL_RUNTIME_ONLY", "false")

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
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName(if (hasReleaseSigningConfig) "release" else "debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
        }
    }
    androidResources {
        ignoreAssetsPattern = ""
    }
}

val shellTemplateOutput = project(":shell").layout.buildDirectory.file("outputs/apk/release/shell-release.apk")

tasks.register<Copy>("syncShellTemplateApk") {
    description = "Builds the dedicated shell template APK and copies it into app assets."
    group = "build"
    dependsOn(":shell:assembleRelease")
    from(shellTemplateOutput)
    into(file("src/main/assets/template"))
    rename { "webview_shell.apk" }
}

tasks.named("preBuild").configure {
    dependsOn("syncShellTemplateApk")
}

tasks.register("testClasses") {
    group = "verification"
    description = "Compatibility alias for JVM-style test class compilation in the Android app module."
    dependsOn("compileDebugUnitTestSources")
}

tasks.register("unitTestClasses") {
    group = "verification"
    description = "Compatibility alias for Android unit test class compilation in the Android app module."
    dependsOn("compileDebugUnitTestSources")
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
    debugImplementation("androidx.compose.ui:ui-tooling")


    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")


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


    implementation("com.android.tools.build:apksig:8.3.0")


    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:137.0.20250414091429")


    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")


    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-beta.3")


    implementation("com.android.billingclient:billing-ktx:7.0.0")


    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.browser:browser:1.8.0")


    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}









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
        fun runCommand(vararg args: String) {
            val exitCode = ProcessBuilder(*args).inheritIO().start().waitFor()
            if (exitCode != 0) {
                throw GradleException("Command failed (${args.joinToString(" ")}): exit code $exitCode")
            }
        }

        println("Downloading PHP $phpVersion for Android arm64...")
        runCommand("curl", "-L", "-f", "-o", tarFile.absolutePath, url)

        println("Extracting PHP binary...")
        runCommand("tar", "-xzf", tarFile.absolutePath, "-C", tempDir.absolutePath)


        val extracted = File(tempDir, "bin/php").takeIf { it.exists() }
            ?: tempDir.walkTopDown().firstOrNull { it.name == "php" && it.isFile }
            ?: throw GradleException("PHP binary not found in archive")

        extracted.copyTo(outputFile, overwrite = true)
        outputFile.setExecutable(true)
        tempDir.deleteRecursively()

        println("PHP binary installed: ${outputFile.relativeTo(rootDir)}")
    }
}
