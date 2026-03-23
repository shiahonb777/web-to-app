# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Check lint issues
./gradlew lint
```

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Gradle 8.2+
- Android SDK with compileSdk 35, minSdk 23

## Architecture Overview

WebToApp is an Android app that converts websites, media files, and HTML projects into standalone Android APKs—all on-device without requiring a PC.

### Tech Stack
- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Repository pattern
- **Database**: Room (current version 14)
- **Networking**: OkHttp
- **Image Loading**: Coil
- **APK Signing**: JarSigner + Android apksig library
- **Native Code**: C++ via NDK for crypto engine (AES-GCM encryption, anti-debug, integrity checks)

### Key Architectural Layers

```
app/src/main/java/com/webtoapp/
├── WebToAppApplication.kt    # Application class with global singletons
├── core/                     # Core business logic modules
├── ui/                       # UI layer (Compose screens, ViewModels)
│   ├── screens/             # Compose screens
│   ├── viewmodel/           # ViewModels
│   ├── components/          # Reusable UI components
│   ├── data/                # Data layer (Room DB, Repository, Models)
│   └── theme/               # Theme system
└── util/                    # Utility classes
```

### Core Modules (in `core/`)

| Module | Purpose |
|--------|---------|
| `apkbuilder/` | On-device APK building (ApkBuilder, JarSigner, ARSC/AXML editing) |
| `extension/` | Tampermonkey-like JS/CSS injection system with built-in modules |
| `extension/agent/` | AI-powered module development agent |
| `crypto/` | AES-256-GCM encryption for APK resources |
| `ai/` | Multi-provider AI integration (Gemini, OpenAI, etc.) |
| `isolation/` | Browser fingerprint spoofing and multi-instance isolation |
| `forcedrun/` | Forced run mode with hardware control |
| `bgm/` | Background music with LRC lyrics sync |
| `adblock/` | Ad blocking engine |
| `webview/` | WebView management and JS bridges |

### Data Flow

1. **WebApp Entity** (`data/model/WebApp.kt`): Central data model storing all app configuration (URL/media, icon, activation codes, extensions, encryption settings, etc.)
2. **WebAppDao** (`data/dao/WebAppDao.kt`): Room DAO for CRUD operations
3. **WebAppRepository** (`data/repository/WebAppRepository.kt`): Repository layer exposing Flow-based data access
4. **ViewModels**: Consume repository, expose UI state as StateFlow

### APK Building Pipeline

The on-device APK builder (`core/apkbuilder/`) works by:
1. **ApkTemplate**: Extracts base APK template from assets
2. **ApkBuilder**: Streams configuration and resources into APK, handles encryption
3. **ArscEditor/AxmlEditor**: Modifies Android binary XML and resources
4. **JarSigner**: Signs the APK using v1/v2/v3 signatures

### Extension Module System

Extension modules (`core/extension/`) inject JS/CSS into WebViews:
- `ExtensionModule`: Data class defining module metadata, scripts, URL match rules
- `ExtensionManager`: Singleton managing module lifecycle
- `BuiltInModules`: 10 pre-built modules (video downloader, dark mode, privacy protection, etc.)
- `ModuleTemplates`: 30+ code templates for quick module creation
- AI Agent (`agent/`): Natural language to module code generation

### Room Database Migrations

When adding new fields to `WebApp`:
1. Add column to `WebApp` data class
2. Add TypeConverter in `Converters.kt` if needed
3. Increment database version in `AppDatabase.kt`
4. Add migration using `createAddColumnMigration()` helper or custom Migration object

### Package Structure Notes

- Data classes use `com.webtoapp.data.*` package (note: not under `ui/`)
- UI data models are in `com.webtoapp.ui.data.model`
- Core features are standalone modules in `com.webtoapp.core.*`
- Native crypto engine is in `app/src/main/cpp/`

### Key Configuration Files

- `app/build.gradle.kts`: App-level dependencies and signing config
- `gradle.properties`: Gradle performance settings and Android config
- `proguard-rules.pro`: ProGuard/R8 keep rules for data models and serialization
