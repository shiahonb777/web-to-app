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
- 💻 **HTML to App** - Convert HTML/CSS/JS projects into standalone apps (New)
- 🤖 **AI Coding Assistant** - AI-powered HTML code generation with live preview (New)
- 🎵 **AI Subtitle Generator** - Generate time-synced LRC lyrics from audio (New)
- 🎨 **Theme System** - Beautiful themes with animation customization (New)
- 📦 **One-Click Build** - Generate installable APKs without a PC
- 🛡️ **Built-in Tools** - Ad blocker, activation codes, BGM, announcements
- ⚡ **App Modifier** - Change icons and names of installed apps

## 🎯 Use Cases

- 📱 Wrap frequently used websites as standalone apps
- 🎞️ Create digital albums, video wallpapers, product showcases
- 💻 Package frontend projects as Android apps
- 🎵 Generate synchronized lyrics for audio files
- 🏢 Quickly app-ify enterprise internal systems
- 🎮 Package H5 games/tools as independent apps
- 🔧 Customize icons for any installed app

## 📋 Feature Details

### Core Features
- **URL to App**: Enter any website URL to generate a standalone app
- **Media to App**: Convert images/videos into independent apps
- **HTML to App**: Convert HTML/CSS/JS projects into standalone apps (v1.5)
- **Custom Icon**: Select custom app icons from gallery
- **Custom Name**: Customize the app display name

### Integrated Features
- **Splash Screen**: Support image/video splash animations with built-in video trimmer
- **User Scripts**: Support custom JavaScript injection
- **Background Music**: Add BGM to apps with LRC lyrics sync (v1.5)
- **Activation Code**: Built-in activation mechanism to restrict app usage
- **Announcements**: Display announcements on startup with link support
- **Ad Blocking**: Built-in ad blocking engine to filter web ads
- **Ad Integration**: Reserved ad SDK interfaces (banner/interstitial/splash)

### Export Options
- **Desktop Shortcut**: Create desktop icons, launch like native apps
- **Build APK**: Generate standalone APK packages without Android Studio
- **Project Template**: Export complete Android Studio projects

### Media App Features
- **Image to App**: Full-screen image display with fill screen option
- **Video to App**: Video playback with loop, audio toggle, autoplay
- **APK Export**: Media apps support standalone APK export

### AI Features (v1.5)
- **AI LRC Generation**: Analyze audio with AI to generate time-synced LRC lyrics
- **Multi-Provider Support**: Google Gemini, OpenAI GPT-4o, GLM, Volcano, MiniMax, OpenRouter, etc.
- **AI HTML Coding**: AI-assisted HTML/CSS/JS code generation
- **Session Management**: Multi-session, templates, style customization
- **Live Preview**: Preview generated code in real-time
- **AI Settings**: Unified API key and model management
- **Task Management**: View and manage LRC generation tasks

### Theme System (v1.5)
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

## 📦 Installation

Download the latest APK from [Releases](https://github.com/shiahonb777/web-to-app/releases)

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

## 📝 License

MIT License

## 📜 Changelog

### v1.5.0
**New Features**
- AI LRC Subtitle Generation: AI-powered audio-to-LRC conversion
  - Multiple AI providers: Google Gemini, OpenAI GPT-4o, GLM, Volcano, MiniMax, OpenRouter, etc.
  - Precise time alignment, multi-language support (CN/EN/JP/KR)
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
