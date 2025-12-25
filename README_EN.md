<div align="center">

# WebToApp 🚀

**English | [简体中文](README.md)**

**Transform any website or media into a standalone Android app with zero coding!**

[![GitHub stars](https://img.shields.io/github/stars/shiahonb777/web-to-app?style=social)](https://github.com/shiahonb777/web-to-app)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

WebToApp is a powerful Android application that enables anyone to convert websites, images, and videos into independent apps. Build APK packages directly on your phone – no Android Studio required.

## ✨ Key Features

- 🌐 **Website to App** - Wrap any URL into a standalone WebView app
- 🎬 **Media to App** - Convert images/videos into fullscreen showcase apps
- 💻 **HTML to App** - Convert HTML/CSS/JS projects into standalone apps
- 🧩 **Extension Module System** - Tampermonkey-like scripts, 9 feature-rich built-in modules
- 🤖 **AI Module Development** - AI Agent assists in developing extension modules
- 🎨 **AI Icon Generator** - Generate beautiful app icons using AI
- 🎵 **Online Music Search** - Search and download music online for BGM
- 📢 **Announcement Templates** - 10 beautiful announcement popup templates
- 🌐 **Auto Web Translation** - Automatic webpage content translation
- 📦 **One-Click Build** - Generate installable APKs without a PC
- 🛡️ **Built-in Tools** - Ad blocker, activation codes, BGM, announcements
- ⚡ **App Modifier** - Change icons and names of installed apps

## 🎯 Use Cases

- 📱 Wrap frequently used websites as standalone apps
- 🎞️ Create digital albums, video wallpapers, product showcases
- 💻 Package frontend projects as Android apps

- 🏢 Quickly app-ify enterprise internal systems
- 🎮 Package H5 games/tools as independent apps
- 🔧 Customize icons for any installed app
- 🧩 Add custom features to webpages (ad blocking, dark mode, etc.)

## 📋 Feature Details

### Core Features
- **URL to App**: Enter any website URL to generate a standalone app
- **Media to App**: Convert images/videos into independent apps
- **HTML to App**: Convert HTML/CSS/JS projects into standalone apps
- **Custom Icon**: Select from gallery or generate with AI
- **Custom Name**: Customize the app display name

### Extension Module System
- **Tampermonkey-like Scripts**: Inject custom JavaScript/CSS into webpages
- **9 Built-in Modules**: Video downloader, Bilibili/Douyin/Xiaohongshu video extractor, video enhancer, web analyzer, dark mode, privacy protection, content enhancer
- **30+ Code Templates**: Quickly create common functionality modules
- **Module Categories**: 20+ categories (content filter, style modifier, function enhance, media, etc.)
- **URL Match Rules**: Support wildcards and regex patterns
- **Config System**: Modules support user-customizable settings
- **Permission Declaration**: Fine-grained permission control
- **Share Code**: One-click generate share code for easy module sharing
- **Import/Export**: Support module file import and export

### AI Module Development Agent
- **Natural Language Development**: Describe requirements in natural language, AI generates module code
- **Syntax Check**: Automatic JavaScript/CSS syntax error detection
- **Security Scan**: Detect XSS, eval, and other security issues
- **Auto Fix**: AI automatically fixes detected errors
- **Code Snippet Library**: Quick insert common code snippets
- **Debug Test Pages**: Built-in test pages to verify module effects

### AI Features
- **Multi-Provider Support**: Google Gemini, OpenAI GPT-4o, GLM, Volcano, MiniMax, OpenRouter, etc.
- **AI HTML Coding**: AI-assisted HTML/CSS/JS code generation
- **AI Icon Generator**: Generate app icons using AI
- **Icon Library**: Collect and manage generated icons
- **Session Management**: Multi-session, templates, style customization
- **Live Preview**: Preview generated code in real-time
- **AI Settings**: Unified API key and model management

### Integrated Features
- **Splash Screen**: Support image/video splash animations with built-in video trimmer
- **Background Music**: Add BGM to apps with LRC lyrics sync
- **Online Music Search**: Search and download music online for BGM
- **Activation Code**: Built-in activation mechanism to restrict app usage
- **Announcements**: Display announcements on startup with link support
- **Announcement Templates**: 10 beautiful templates (Xiaohongshu, gradient, glassmorphism, neon, etc.)
- **Ad Blocking**: Built-in ad blocking engine to filter web ads
- **Auto Web Translation**: Automatic webpage translation, supports CN/EN/JP, etc.
- **Ad Integration**: Reserved ad SDK interfaces (banner/interstitial/splash)

### Export Options
- **Desktop Shortcut**: Create desktop icons, launch like native apps
- **Build APK**: Generate standalone APK packages without Android Studio
- **Project Template**: Export complete Android Studio projects

### Media App Features
- **Image to App**: Full-screen image display with fill screen option
- **Video to App**: Video playback with loop, audio toggle, autoplay
- **Display Config**: Audio toggle, loop, autoplay, fill screen options
- **APK Export**: Media apps support standalone APK export

### Theme System
- **Multiple Themes**: Beautiful built-in theme styles
- **Dark Mode**: Follow system or manual toggle
- **Animations**: Customizable animation effects and speed
- **Particle Effects**: Some themes support particle backgrounds

### App Modifier
- **App Scanner**: Automatically scan all installed apps
- **Icon/Name Modifier**: Freely modify any app's icon and name
- **Clone Install**: Install modified apps with independent package names
- **Shortcut Launch**: Create shortcuts with new icons to launch original apps

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Repository
- **Database**: Room
- **Network**: OkHttp
- **Image Loading**: Coil
- **Min SDK**: Android 7.0 (API 24)

## 📁 Project Structure

```
app/src/main/java/com/webtoapp/
├── WebToAppApplication.kt      # Application class
├── core/                       # Core modules
│   ├── activation/            # Activation code management
│   ├── adblock/              # Ad blocking
│   ├── ads/                  # Ad integration
│   ├── ai/                   # AI features
│   │   ├── AiApiClient.kt   # AI API client
│   │   ├── AiConfigManager.kt # AI config management
│   │   └── htmlcoding/      # AI HTML coding
│   ├── announcement/         # Announcement management
│   ├── apkbuilder/          # APK builder
│   │   ├── ApkBuilder.kt    # Build core
│   │   ├── ApkSigner.kt     # APK signing
│   │   ├── ArscEditor.kt    # Resource table editor
│   │   ├── AxmlEditor.kt    # Manifest editor
│   │   └── ApkTemplate.kt   # Template management
│   ├── appmodifier/         # App modifier
│   ├── bgm/                 # Background music
│   │   ├── BgmPlayer.kt     # Player
│   │   ├── OnlineMusicApi.kt # Online music API
│   │   └── OnlineMusicDownloader.kt # Music downloader
│   ├── export/              # Export features
│   ├── extension/           # Extension module system
│   │   ├── ExtensionModule.kt # Module data model
│   │   ├── ExtensionManager.kt # Module manager
│   │   ├── BuiltInModules.kt # 9 built-in modules
│   │   ├── ModuleTemplates.kt # 30+ code templates
│   │   ├── CodeSnippets.kt  # Code snippet library
│   │   └── agent/           # AI Agent system
│   │       ├── ModuleAgentEngine.kt # Agent engine
│   │       ├── AgentToolExecutor.kt # Tool executor
│   │       └── AgentTool.kt # Tool definitions
│   ├── shell/               # Shell mode management
│   └── webview/             # WebView management
│       ├── WebViewManager.kt
│       └── TranslateBridge.kt # Translation bridge
├── data/                      # Data layer
│   ├── model/               # Data models
│   │   └── WebApp.kt
│   └── repository/          # Data repository
├── ui/                        # UI layer
│   ├── MainActivity.kt      # Main Activity
│   ├── media/               # Media app
│   ├── navigation/          # Navigation
│   ├── screens/             # Screens
│   │   ├── HomeScreen.kt    # Home
│   │   ├── CreateAppScreen.kt # Create app
│   │   ├── CreateMediaAppScreen.kt # Create media app
│   │   ├── CreateHtmlAppScreen.kt # Create HTML app
│   │   ├── ExtensionModuleScreen.kt # Extension modules
│   │   ├── ModuleEditorScreen.kt # Module editor
│   │   ├── AiModuleDeveloperScreen.kt # AI module dev
│   │   ├── HtmlCodingScreen.kt # AI HTML coding
│   │   ├── AiSettingsScreen.kt # AI settings
│   │   ├── ThemeSettingsScreen.kt # Theme settings
│   │   ├── AboutScreen.kt   # About
│   │   └── AppModifierScreen.kt # App modifier
│   ├── components/          # Reusable components
│   │   ├── announcement/    # Announcement templates
│   │   ├── IconGeneratorDialog.kt # AI icon generator
│   │   ├── IconLibraryDialog.kt # Icon library
│   │   ├── OnlineMusicSelector.kt # Online music
│   │   ├── ManualLrcAligner.kt # Manual LRC align
│   │   ├── ExtensionModuleSelector.kt # Module selector
│   │   └── CodeSnippetSelector.kt # Code snippets
│   ├── theme/               # Theme system
│   └── viewmodel/           # ViewModel
└── util/                      # Utilities
    ├── MediaStorage.kt      # Media file storage
    └── IconLibraryStorage.kt # Icon library storage
```

## 📖 Usage Guide

### Create an App
1. Click "Create App" button on home screen
2. Enter app name and website URL
3. (Optional) Select custom icon or generate with AI
4. (Optional) Configure activation code, announcement, ad blocking, etc.
5. (Optional) Select extension modules to enhance functionality
6. Click Save

### Use Extension Modules
1. When creating/editing an app, expand the "Extension Modules" card
2. Click "Select Modules" to browse available modules
3. Select desired modules (e.g., dark mode, ad blocker)
4. Modules will automatically inject and execute when the app runs

### Create Custom Modules
1. Go to "Extension Modules" page
2. Click "+" to create a new module
3. Choose a template or start from scratch
4. Configure module name, category, URL match rules
5. Write JavaScript/CSS code
6. Save and test

### AI-Assisted Module Development
1. Go to "AI Module Development" page
2. Describe the functionality you want in natural language
3. AI will automatically generate module code
4. Automatic syntax check and security scan
5. Preview the effect and save

### Run an App
- Click app card to preview directly
- Long press or click menu for more options

### Create Desktop Shortcut
1. Click the menu on the right side of app card
2. Select "Create Shortcut"
3. Confirm adding to desktop

### Build APK Package
1. Click the menu on the right side of app card
2. Select "Build APK"
3. Click "Start Build"
4. Installation dialog appears automatically when complete

### Export as Project Template
1. Click the menu on the right side of app card
2. Select "Export Project"
3. Find the project folder in export directory
4. Open with Android Studio and compile

### Use App Modifier
1. Click the app icon button in the top right of home screen
2. Search or filter target app in the app list
3. Click app to enter modification interface
4. Select new icon, enter new name
5. Choose operation:
   - **Shortcut**: Create desktop shortcut with new icon
   - **Clone Install**: Generate new APK and install as independent app

## 🔧 Build from Source

### Requirements
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Gradle 8.2

### Build Steps
```bash
# Clone the repository
git clone https://github.com/shiahonb777/web-to-app.git

# Enter project directory
cd web-to-app

# Build Debug version
./gradlew assembleDebug

# Build Release version
./gradlew assembleRelease
```

### Signing Configuration
For Release builds, configure signing in `app/build.gradle.kts`:
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

## 🧩 Extension Module System

### Built-in Modules
| Module | Function |
|--------|----------|
| ⬇️ Video Downloader | Auto-detect webpage videos, supports MP4 and Blob stream download |
| 📺 Bilibili Video Extractor | Extract Bilibili highest quality video and audio stream URLs |
| 🎬 Douyin Video Extractor | Extract Douyin watermark-free video URLs |
| 📱 Xiaohongshu Video Extractor | Extract Xiaohongshu video playback URLs |
| ⚡ Video Enhancer | Speed control (0.5x-5x), picture-in-picture, background play, block app redirect |
| 🔧 Web Analyzer | Element inspector, network monitor, cookie manager, console injection |
| 🌙 Advanced Dark Mode | Smart color inversion, image brightness control, scheduled toggle |
| 🛡️ Privacy Protection | Ad blocking, anti-fingerprint tracking, click hijack protection, external link warning |
| 📝 Content Enhancer | Force copy, selection translate, long screenshot, Markdown conversion |

### Module Categories
- Content Filter, Content Enhance, Style Modifier, Theme
- Function Enhance, Automation, Navigation, Data Extract
- Media, Video Enhance, Image Processing, Audio Control
- Security & Privacy, Anti-Tracking, Social, Shopping
- Reading Mode, Translation, Developer Tools, Other

### Module Development Example
```javascript
// Example: Auto hide ads
const selectors = getConfig('selectors', '.ad-banner').split('\n');
function hideAds() {
    selectors.forEach(sel => {
        document.querySelectorAll(sel).forEach(el => {
            el.style.display = 'none';
        });
    });
}
hideAds();
new MutationObserver(hideAds).observe(document.body, { childList: true, subtree: true });
```

## 📢 Announcement Templates

10 beautiful announcement popup templates:
- **Minimal** - Clean and simple
- **Xiaohongshu Style** - Lively and cute
- **Gradient** - Modern and stylish
- **Glassmorphism** - Transparent texture
- **Neon** - Cool glowing effect
- **Cute** - Pink and sweet
- **Elegant** - Golden and noble
- **Festive** - Celebratory and lively
- **Dark** - Mysterious and deep
- **Nature** - Fresh and green

## 📝 Notes

1. Some websites may have anti-crawling mechanisms, loading may be limited
2. Network permission required for normal use
3. Exported projects need to be compiled with Android Studio on PC
4. Activation codes are verified locally only, extend for server-side verification if needed
5. Extension modules execute in WebView, some sites may have CSP restrictions

## 📜 License

MIT License

## 📜 Changelog

### v1.7.0
**Bug Fixes & Improvements**
- Fixed dozens of known issues
- Optimized AI Agent programming architecture
- Immersive fullscreen mode for exported APKs
  - Status bar and navigation bar fully transparent
  - Content fills the entire screen
  - Support for notch/punch-hole displays
  - Auto-hide virtual buttons during video playback
- Extended app name length support (up to ~60 Chinese characters)

### v1.6.0
**New Features**
- Extension Module System: Tampermonkey-like JS/CSS injection system
  - 9 feature-rich built-in modules (video download, platform video extractors, video enhancer, web analyzer, dark mode, privacy protection, content enhancer)
  - 30+ code templates for quick module creation
  - 20+ module categories covering common needs
  - URL match rules support (wildcards/regex)
  - User-configurable settings
  - Share code import/export
- AI Module Development Agent: AI-assisted extension module development
  - Natural language requirement description, auto code generation
  - Automatic syntax check and security scan
  - Auto-fix detected errors
  - Code snippet library for quick insertion
- AI Icon Generator: Generate app icons using AI
- Icon Library: Collect and manage generated icons
- Online Music Search: Search and download music online for BGM
- Announcement Template System: 10 beautiful popup templates
- Auto Web Translation: Automatic webpage content translation

**Improvements**
- Refactored extension module architecture for more flexible development
- Optimized AI features, support more models and providers

### v1.5.0
**New Features**
- AI HTML Coding Assistant: AI-powered code generation
  - Multiple text/image generation models
  - Session management, templates, styles
  - Code block parsing, live preview
- AI Settings: Unified API key and model management
  - Multiple API keys, real-time connection testing
  - Custom Base URL, model list fetched from API
- HTML App: Convert HTML/CSS/JS projects into standalone Android apps
- Theme System: Brand new theme customization
  - Multiple beautiful theme styles, dark mode support
  - Customizable animation effects and speed
- Background Music (BGM): Add BGM with LRC lyrics sync

**Improvements**
- Home UI integration with AI Coding, Theme, AI Settings entries
- FAB menu adds HTML app creation

### v1.3.0
**New Features**
- Media App: Convert images/videos into standalone apps
- User Scripts: Support custom JavaScript injection
- Splash Screen: Support image/video with audio toggle, landscape, fill screen
- Video Trimmer: Visual video segment selection with real-time preview

**Improvements**
- Data model refactoring for video trimming configuration persistence
- Shell mode (APK export) fully supports splash screen playback
- Optimized MediaPlayer for precise seek and auto-stop

**Bug Fixes**
- Fixed shortcut icon incorrectly using splash image
- Fixed database schema mismatch causing crashes

### v1.2.x
- APK icon cropping fix (Android Adaptive Icon compliance)
- Release build custom icon fix
- Fullscreen mode support

### v1.1.0
- One-click APK building
- App modifier with clone install
- Desktop mode for web pages
- Material Design 3 UI

### v1.0.0
- Initial release
- URL to shortcut
- Activation codes, announcements, ad blocking
- Project template export

## 📬 Contact

- **QQ Group**: 1041130206
- **Author QQ**: 2711674184
- Developed independently by shihao
- Looking for AI programming teammates!

---

<div align="center">

**Open Source · Free Forever · Star ⭐ to Support**

</div>
