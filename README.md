<div align="center">

# WebToApp

### Any website. One tap. An app.

No IDE. No build server. No PC.

**English** | [简体中文](README_CN.md)

[![Stars](https://img.shields.io/github/stars/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/stargazers)
[![Forks](https://img.shields.io/github/forks/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/network/members)
[![License](https://img.shields.io/badge/License-Unlicense-blue?style=for-the-badge)](LICENSE)

</div>

---

## Screenshots

<div align="center">
<img src="png/1.png" width="24%" /><img src="png/2.png" width="24%" /><img src="png/3.png" width="24%" /><img src="png/4.png" width="24%" />
<img src="png/5.png" width="24%" /><img src="png/6.png" width="24%" /><img src="png/7.png" width="24%" /><img src="png/8.png" width="24%" />
<img src="png/9.png" width="24%" /><img src="png/10.png" width="24%" />
</div>

## What It Does

WebToApp converts websites, HTML projects, media files, and server-side applications into standalone Android APKs — all from your phone, no PC required.

**Supported types:** Website / HTML / React / Vue / WordPress / Node.js / PHP / Python / Go / Image / Video / Gallery / Multi-Web

## Key Features

- **One-Click APK Build** — Generate installable APKs directly on device
- **Dual Browser Engines** — WebView + GeckoView (Firefox) for maximum compatibility
- **Extension Modules** — 10 built-in modules + custom JS/CSS injection with NativeBridge API
- **AI Assistance** — AI-powered module development, icon generation, and HTML coding
- **Security** — APK encryption, browser fingerprint spoofing, ad blocking, DNS-over-HTTPS
- **Customization** — Splash screens, background music with lyrics, themes, activation codes, announcements
- **App Modifier** — Clone and rebrand installed apps with new icons and names
- **Cloud Services** — Optional Pro/Ultra: cloud projects, analytics, push notifications, remote config

## Core Technical Implementations

- **Deep-Modified WebView Kernel** — Native-level WebView hardening: UA sanitization, X-Requested-With removal, prototype chain protection, iframe propagation. WebView is indistinguishable from real Chrome.
- **OAuth In-App Login** — 30+ OAuth providers (Google, Facebook, GitHub, Discord, WeChat, Alipay, PayPal, etc.) work directly inside WebView via per-provider anti-detection JS injection. Google OAuth falls back to Chrome Custom Tab with shared cookie session.
- **Chrome Extension Runtime** — Full `chrome.*` API polyfill (runtime, storage, tabs, messaging) enables desktop Chrome extensions to run inside WebView. Background scripts execute in isolated WebViews with same-origin fetch and shared cookie state.
- **22-Vector Browser Fingerprint Spoofing** — Canvas, WebGL, AudioContext, screen, fonts, GPU info — all spoofed with consistent cross-vector values. Prototype chain hooks return `[native code]` on `.toString()`.
- **Local Server Runtimes** — Node.js / PHP / Python / Go run inside the app via on-device compilation and local HTTP serving. No remote server needed.

## App Configuration Features

Every generated app comes with a full configuration surface:

**WebView Control** — Desktop mode, custom UA, JS/CSS injection (DOCUMENT_START/END/IDLE), popup blocker, new window behavior (same tab / external browser / popup / block), proxy (HTTP/SOCKS5/PAC), DNS-over-HTTPS (7 providers + custom), screen orientation (7 modes), screen awake + brightness, keyboard adjust, viewport modes, payment schemes, cross-origin isolation, PWA offline support, error page customization

**Floating Window** — Resizable floating window with adjustable size, opacity, corner radius, border style (subtle/glow/accent), edge snapping, position lock, auto-hide title bar, start minimized, remember position

**Status Bar** — Full status bar theming: color, dark/light icons, background type (color/image), alpha, height — with separate dark mode config

**Security & Privacy** — APK encryption (100000 PBKDF2 iterations, custom password support), app isolation (separate data directory), browser/device fingerprint disguise, ad blocker (hosts rules + cosmetic MutationObserver), forced run, activation code gating (per-time or persistent)

**App Hardening** — DEX encryption + splitting + VMP + control flow flattening, native SO encryption + ELF obfuscation + symbol strip + anti-dump, anti-Frida/Xposed/Magisk/debug/memory dump/screen capture, emulator/VirtualApp/VPN/USB debugging detection, string encryption + class name obfuscation + call indirection + opaque predicates, DEX CRC verify + memory integrity + JNI validation + timing check, multi-point signature verify + APK checksum + resource integrity + certificate pinning, threat response + honeypot + self-destruct

**Forced Run** — 3 modes: fixed time period / countdown / access window. Blocks system UI, back button, home button, recent apps, notifications. Countdown persistence across process kills. Emergency exit with password. Pre-end warning.

**BlackTech** — Volume control (force max/mute/block keys), flashlight modes (strobe/SOS/Morse code/heartbeat/breathing/emergency + custom alarm pattern with vibe sync), system control (block power key, max performance), screen control (black screen, rotation, block touch, force awake), network control (WiFi hotspot with SSID/password, disable WiFi/Bluetooth/mobile data), nuclear mode (all-out), stealth mode (mute + black screen + block touch + disconnect)

**Device Disguise** — Spoof device identity: device type (phone/tablet/desktop), OS (Android/iOS/HarmonyOS/macOS/Windows/Linux), brand + model presets, screen resolution, pixel ratio, timezone, locale

**Splash & BGM** — Custom splash screen (image/video with audio, click-to-skip, trim range, orientation), background music playlist with LRC lyrics sync, 7 lyrics animation types (fade/slide/scale/typewriter/karaoke), 3 positions, custom lyrics theme (font/size/colors/stroke/shadow), 20 music tags, loop/sequential/shuffle modes

**Announcements** — 10 template styles (Minimal/Xiaohongshu/Gradient/Glassmorphism/Neon/Cute/Elegant/Festive/Dark/Nature), trigger on launch / interval / no-network

**Translation** — In-page translation overlay with 20 target languages, 5 engines (Google/MyMemory/LibreTranslate/Lingva/Auto), floating button toggle, auto-translate on load

**Notifications** — Web API Notification polyfill + URL polling with foreground service, configurable interval and JSON parsing

**Deep Links** — Custom scheme support, configurable host patterns

**Auto Start** — Boot auto-start, scheduled launch at specific time

**Background Run** — Keep app running with foreground service, custom notification title/content, CPU wake lock, battery optimization bypass

**Gallery App** — Categorized image/video gallery with grid/list/timeline views, sequential/shuffle/single-loop play, sort by custom/name/date/type, thumbnail bar, media info, video auto-next, remember playback position

**Multi-Web App** — Multiple sites in tabs/cards/feed/drawer layout, per-site icon and CSS selector, auto-refresh interval

**Server-Side Runtimes** — Node.js (4 build modes: Static/SSR/API Backend/Fullstack, env vars), WordPress (theme + plugins, built-in PHP), PHP (Composer support, custom document root), Python (Flask/Django/builtin server, pip deps), Go (binary compilation, static file serving)

**Website Scraper** — Offline pack creator: crawl entire website frontend (HTML/CSS/JS/images/fonts), concurrent download, recursive CSS url() resolution, absolute-to-relative path rewriting, same-domain restriction, depth/size limits

**App Modifier** — Clone and rebrand installed APKs: replace icon, name, package name via binary manifest patching

**Extension Modules** — 10 built-in modules (video downloader, Bilibili/Douyin/Xiaohongshu extractor, video enhancer, web analyzer, dark mode, privacy protection, content enhancer, element blocker) + custom modules with 3 source types (custom JS / userscript .user.js / Chrome extension manifest.json). Full Greasemonkey/Tampermonkey GM_* API bridge. MV3 declarativeNetRequest rule engine (block/allow/redirect/modifyHeaders). Module sharing via export codes.

**APK Export** — Custom package name/version, architecture selection (Universal/ARM64/ARM32), performance optimization (image compression/WebP conversion, code minification, lazy loading, DNS prefetch, preload hints), runtime permissions (camera/mic/location/storage/Bluetooth/NFC/SMS/contacts/calendar/sensors/foreground service/wake lock/install packages/system alert window)

**Ads** — Banner + interstitial + splash ad support with configurable IDs and durations

**Themes** — Aurora theme system with dynamic color generation

**Data Backup** — Full app data backup and restore, project export/import

## Quick Start

1. Install WebToApp on your Android device
2. Tap "+" to create a new app — enter URL or import project
3. Customize icon, settings, and features
4. Tap "Build APK" — install and done

## Build from Source

**Requirements:** Android Studio Hedgehog+, JDK 17, Gradle 8.14+

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

For release builds, configure signing in `app/build.gradle.kts`.

## Tech Stack

Kotlin / Jetpack Compose / Material Design 3 / Room / GeckoView / OkHttp / KSP / Native C++ (JNI)

## Contact

Developed by shiaho.

| Platform | Link |
|----------|------|
| GitHub | [github.com/shiahonb777/web-to-app](https://github.com/shiahonb777/web-to-app) |
| Telegram | [t.me/webtoapp777](https://t.me/webtoapp777) |
| X (Twitter) | [x.com/@shiaho777](https://x.com/@shiaho777) |
| Bilibili | [b23.tv/8mGDo2N](https://b23.tv/8mGDo2N) |
| QQ Group | 1041130206 |

## License

The Unlicense. Advanced features (e.g. Forced Run) are for technical demonstration only and must be used with informed consent.

<div align="center">

**Open Source · Free Forever · Star to Support**

</div>
