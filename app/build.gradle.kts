import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

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
    description = "Builds the dedicated shell template APK and copies it into the app assets."
    group = "build"
    dependsOn(":shell:assembleRelease")
    from(shellTemplateOutput)
    into(file("src/main/assets/template"))
    rename { "webview_shell.apk" }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("syncShellTemplateApk")
    // PHP 二进制不能 dependsOn，因为它走外网（github releases）下载。
    // 网络抽风时不应阻断整个 debug 构建。开发者要发 release 版前请手动跑：
    //   ./gradlew :app:downloadPhpBinary
    // 该 task 的 onlyIf 会缓存结果，已下载的话只是 NO-SOURCE，不会重复下。
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

abstract class SyncNativeExecutableJniLibsTask : DefaultTask() {
    @get:Input
    abstract val variantName: org.gradle.api.provider.Property<String>

    @get:Input
    abstract val buildTypeName: org.gradle.api.provider.Property<String>

    @get:Input
    abstract val executableName: org.gradle.api.provider.Property<String>

    @get:Input
    abstract val packagedLibraryName: org.gradle.api.provider.Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cxxRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val cxxRootDir = cxxRoot.asFile.get()
        if (!cxxRootDir.exists()) {
            throw GradleException("CXX output not found for ${variantName.get()}: ${cxxRootDir.absolutePath}")
        }

        val executableTargets = cxxRootDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.name == executableName.get() &&
                    file.parentFile?.parentFile?.name == "obj"
            }
            .toList()

        if (executableTargets.isEmpty()) {
            throw GradleException("${executableName.get()} artifacts not found for ${variantName.get()} under ${cxxRootDir.absolutePath}")
        }

        val outputRoot = outputDir.get().asFile
        outputRoot.deleteRecursively()
        outputRoot.mkdirs()

        executableTargets.forEach { binary ->
            val abi = binary.parentFile.name
            val destFile = outputRoot.resolve("$abi/${packagedLibraryName.get()}")
            destFile.parentFile.mkdirs()
            binary.copyTo(destFile, overwrite = true)
            destFile.setExecutable(true, false)
        }
    }
}

androidComponents {
    onVariants { variant ->
        val capName = variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val variantBuildTypeName = variant.buildType ?: "release"
        val cxxBuildType = if (variantBuildTypeName.equals("debug", ignoreCase = true)) "Debug" else "RelWithDebInfo"
        val nativeBuildTaskName = "buildCMake$cxxBuildType"
        val syncNodeLauncherTask = tasks.register<SyncNativeExecutableJniLibsTask>("syncNodeLauncherJniLibs$capName") {
            group = "build"
            description = "Copies ABI-specific node launcher executables into generated jniLibs for ${variant.name}."
            variantName.set(variant.name)
            buildTypeName.set(variantBuildTypeName)
            executableName.set("node_launcher")
            packagedLibraryName.set("libnode_launcher.so")
            cxxRoot.set(layout.buildDirectory.dir("intermediates/cxx/$cxxBuildType"))
            outputDir.set(layout.buildDirectory.dir("generated/jniLibs/nodeLauncher/${variant.name}"))
            dependsOn(nativeBuildTaskName)
        }

        val syncGoLoaderTask = tasks.register<SyncNativeExecutableJniLibsTask>("syncGoExecLoaderJniLibs$capName") {
            group = "build"
            description = "Copies ABI-specific Go exec loader executables into generated jniLibs for ${variant.name}."
            variantName.set(variant.name)
            buildTypeName.set(variantBuildTypeName)
            executableName.set("go_exec_loader")
            packagedLibraryName.set("libgo_exec_loader.so")
            cxxRoot.set(layout.buildDirectory.dir("intermediates/cxx/$cxxBuildType"))
            outputDir.set(layout.buildDirectory.dir("generated/jniLibs/goExecLoader/${variant.name}"))
            dependsOn(nativeBuildTaskName)
        }

        tasks.matching { it.name == "merge${capName}NativeLibs" }.configureEach {
            dependsOn(syncNodeLauncherTask)
            dependsOn(syncGoLoaderTask)
        }

        variant.sources.jniLibs?.addGeneratedSourceDirectory(
            syncNodeLauncherTask,
            SyncNativeExecutableJniLibsTask::outputDir
        )
        variant.sources.jniLibs?.addGeneratedSourceDirectory(
            syncGoLoaderTask,
            SyncNativeExecutableJniLibsTask::outputDir
        )
    }
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

    implementation("androidx.credentials:credentials:1.3.0")
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
    // 在配置阶段就解析路径，避免 doLast 闭包里访问 project.layout / Task.project
    // （Gradle configuration cache 9.x 禁止执行期访问这些）
    val tempDirRoot = layout.buildDirectory.dir("tmp/php-download").get().asFile
    val rootProjectDirCapture = rootDir

    onlyIf { !outputFile.exists() }

    doLast {
        jniLibsDir.mkdirs()
        val url = "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-${phpVersion}-latest/PHP-${phpVersion}-Android-arm64-PM5.tar.gz"
        tempDirRoot.mkdirs()
        val tarFile = File(tempDirRoot, "php.tar.gz")
        fun runCommand(vararg args: String) {
            val exitCode = ProcessBuilder(*args).inheritIO().start().waitFor()
            if (exitCode != 0) {
                throw GradleException("Command failed (${args.joinToString(" ")}): exit code $exitCode")
            }
        }

        println("Downloading PHP $phpVersion for Android arm64...")
        runCommand("curl", "-L", "-f", "-o", tarFile.absolutePath, url)

        println("Extracting PHP binary...")
        runCommand("tar", "-xzf", tarFile.absolutePath, "-C", tempDirRoot.absolutePath)


        val extracted = File(tempDirRoot, "bin/php").takeIf { it.exists() }
            ?: tempDirRoot.walkTopDown().firstOrNull { it.name == "php" && it.isFile }
            ?: throw GradleException("PHP binary not found in archive")

        extracted.copyTo(outputFile, overwrite = true)
        outputFile.setExecutable(true)
        tempDirRoot.deleteRecursively()

        println("PHP binary installed: ${outputFile.relativeTo(rootProjectDirCapture)}")
    }
}
