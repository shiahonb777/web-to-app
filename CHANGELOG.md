# Changelog

**English** | [简体中文](CHANGELOG_CN.md)

## v1.9.5
**New Features**
- Cookies persistence feature
- Multi API key management configuration
- Model name search feature
- Hide URL preview feature
- Popup blocker feature

**Improvements**
- Optimized custom API endpoint adaptation
- Optimized model name display
- Optimized multi-language content adaptation

**Bug Fixes**
- Fixed gallery app build path issue
- Fixed microphone permission issue
- Fixed zoom property not working issue
- Fixed activation code language display issue
- Fixed frontend and gallery app filename display issue
- Fixed core config edit for some app types not working
- Fixed keyboard initialization issue

## v1.9.0
**New Features**
- Browser Engine: Support for custom WebView kernel configuration
- Browser Spoofing: User-Agent and browser fingerprint spoofing
- Hosts Blocking: Custom hosts file for domain-level blocking
- Long Press Menu: Enhanced long-press context menu options
- APK Architecture: Support for selecting target CPU architectures (arm64-v8a, armeabi-v7a, x86, x86_64)
- Media Gallery: Built-in media gallery for managing images and videos

**Improvements**
- Optimized extension module functionality
- Improved English and Arabic translation support
- Enhanced theme interactions and performance
- Optimized API configuration testing

**Bug Fixes**
- Fixed app name displaying excessive spaces
- Fixed popup announcement navigation issues
- Fixed crash when calling external browser
- Fixed download errors
- Fixed crash when editing modules
- Fixed AI image generation not working
- Fixed downloader and player coordination issues

## v1.8.5
**New Features**
- App Category: Organize apps into custom categories
- Website Favicon Fetch: Automatically fetch website icons as app icons
- Random App Name: Generate random app names with one click
- Multi App Icons: Support multiple icon options per app

**Improvements**
- Optimized data backup functionality
- Optimized BlackTech feature

**Bug Fixes**
- Fixed element blocker issues
- Fixed background run feature crash
- Fixed multi-language string adaptation issues

## v1.8.0
**New Features**
- Multi-language Support: Chinese, English, and Arabic languages
- Share APK Feature: Share built APK files
- Element Blocker Extension Module: Visually block webpage elements
- Forced Run Mode: Support app forced run mode
- Linux One-Click Build: Support one-click frontend project build in Linux environment
- Vue/React/Vite to APK: Convert frontend framework projects to APK

**Improvements**
- Optimized theme functionality
- Optimized About page UI

**Bug Fixes**
- Fixed status bar issue in fullscreen mode
- Fixed crash issue on some devices/emulators

## v1.7.7
**New Features**
- Status bar style configuration: Custom height, background color/image, transparency
- APK encryption protection: Encrypt configuration files and resources

## v1.7.6
**New Features & Improvements**
- Boot auto-start and scheduled auto-start functionality
- Data backup: One-click export/import all app data
- Transparent status bar overlay in fullscreen mode

## v1.7.5
**New Features & Improvements**
- Show status bar option in fullscreen mode: Solves the navigation bar issue when not using fullscreen mode
- Fixed long-press text cannot be copied in HTML projects
- Support Android 6.0 (API 23)

## v1.7.4
**Bug Fixes**
- Fixed HTML app not showing status bar issue
- Fixed some system apps showing empty names
- Fixed code block content overlay issue in AI Module Developer
- Fixed tool calling failure in AI HTML Coding
- Optimized AI HTML Coding prompts and model compatibility

## v1.7.3
**New Features & Improvements**
- Status bar color follows theme: Defaults to theme color, light background for light theme, dark background for dark theme
- Support custom status bar background color: Choose transparent, follow theme, or custom color
- Fixed status bar text visibility issue

## v1.7.2
**Bug Fixes**
- Fixed JS file picker compatibility issue on some systems
- Fixed video fullscreen not auto-rotating to landscape, now auto-rotates and fills screen

## v1.7.1
**Bug Fixes & New Features**
- Fixed long-press image save not working on Xiaohongshu and similar sites
- Added Xiaohongshu image downloader module with batch download support
- Fixed Blob format file (e.g., JSON) export failure
- Fixed CSS/JS not working after HTML project import
- Fixed duplicate app name display in recent tasks list

## v1.7.0
**Bug Fixes & Improvements**
- Fixed dozens of known issues
- Optimized AI Agent programming architecture
- Immersive fullscreen mode for exported APKs
  - Status bar and navigation bar fully transparent
  - Content fills the entire screen
  - Support for notch/punch-hole displays
  - Auto-hide virtual buttons during video playback
- Extended app name length support (up to ~60 Chinese characters)

## v1.6.0
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

## v1.5.0
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

## v1.3.0
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

## v1.2.x
- APK icon cropping fix (Android Adaptive Icon compliance)
- Release build custom icon fix
- Fullscreen mode support

## v1.1.0
- One-click APK building
- App modifier with clone install
- Desktop mode for web pages
- Material Design 3 UI

## v1.0.0
- Initial release
- URL to shortcut
- Activation codes, announcements, ad blocking
- Project template export
