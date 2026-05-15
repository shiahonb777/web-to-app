<div align="center">

# WebToApp

### Any website. One tap. An app.

No IDE. No build server. No PC.

**English** · [简体中文](README_CN.md)

[![Stars](https://img.shields.io/github/stars/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/stargazers)
[![Forks](https://img.shields.io/github/forks/shiahonb777/web-to-app?style=for-the-badge)](https://github.com/shiahonb777/web-to-app/network/members)
[![License](https://img.shields.io/badge/License-Unlicense-blue?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)

</div>

<p align="center">
  <a href="#what-it-does">What it does</a> ·
  <a href="#highlights">Highlights</a> ·
  <a href="#module-market">Module Market</a> ·
  <a href="#feature-catalog">Feature catalog</a> ·
  <a href="#build-from-source">Build from source</a> ·
  <a href="#contributing">Contributing</a> ·
  <a href="#contact">Contact</a>
</p>

---

<div align="center">
<img src="png/1.png" width="19%" /><img src="png/2.png" width="19%" /><img src="png/3.png" width="19%" /><img src="png/4.png" width="19%" /><img src="png/5.png" width="19%" />
<img src="png/6.png" width="19%" /><img src="png/7.png" width="19%" /><img src="png/8.png" width="19%" /><img src="png/9.png" width="19%" /><img src="png/10.png" width="19%" />
</div>

---

## What it does

WebToApp turns websites, HTML projects, media libraries, and even server-side
applications into installable Android APKs — entirely on the device.

Drop in a URL, pick the bits you want, and you walk away with an APK you can
install, share, or sideload to friends. Behind the scenes the app stitches
together a hardened WebView, optional native runtimes (Node.js, PHP, Python,
Go), and an APK builder that signs and packages everything in-process.

**Supported types:** Website · HTML · React · Vue · WordPress · Node.js · PHP ·
Python · Go · Image · Video · Gallery · Multi-Web

## Highlights

- **One-tap APK builds** — sign and install on the same device
- **Two browser engines** — system WebView plus an optional GeckoView (Firefox) backend
- **GitHub-backed Module Market** — install JS/CSS modules vetted on this repo, no app update needed
- **AI assistants** — generate icons, modules, and HTML on demand
- **App Modifier** — clone an installed APK, swap its icon, name, and package
- **Deeply customisable WebView** — desktop UA spoofing, fingerprint disguise, ad blocking, DNS-over-HTTPS, JS/CSS injection
- **Server-side runtimes** — Node.js, PHP, Python, and Go execute locally with no remote server

---

## Module Market

The Module Market lets users install community-built JS/CSS extension modules
in one tap — and the entire catalog lives **in this repository**. There's no
backend, no submission portal, no review queue beyond a regular GitHub PR.

```
modules/                                    ← published catalog
├── registry.json                           ← index the app downloads first
├── README.md                               ← contributor's guide
├── hello-world/                            ← example module (banner)
│   ├── module.json
│   └── main.js
└── night-shift/                            ← example module (amber overlay)
    ├── module.json
    ├── main.js
    └── style.css
```

**For users:** open the app, navigate to *Extension Modules* and tap the
storefront icon in the top bar. Modules update automatically when you publish a
new version.

**For contributors:** fork the repo, drop a folder into `modules/`, add an
entry to `registry.json`, and open a PR. Once it's merged, every WebToApp
client sees it on the next refresh.

→ [Full contributor guide](modules/README.md)
→ [General contributing guide](CONTRIBUTING.md)

---

## Feature catalog

WebToApp has grown a wide configuration surface. Click a section to expand.

<details>
<summary><b>Browser engine &amp; networking</b></summary>

- Desktop mode, custom User-Agent, JS/CSS injection at `DOCUMENT_START` /
  `END` / `IDLE`
- Popup blocker, configurable new-window behaviour (same tab / external /
  popup / blocked)
- HTTP, SOCKS5, and PAC proxies
- DNS-over-HTTPS with seven providers plus custom endpoints
- Cross-origin isolation, PWA offline support, custom error pages
- 22-vector browser fingerprint spoof (Canvas, WebGL, AudioContext, fonts,
  GPU, screen…) with consistent cross-vector values
- Chrome extension runtime polyfill (`chrome.runtime`, `storage`, `tabs`,
  `messaging`) so unmodified Chrome extensions can run inside the WebView
- 30+ in-app OAuth providers (Google, Facebook, GitHub, Discord, WeChat,
  Alipay, PayPal…) via per-provider anti-detection scripts; Google falls back
  to a Chrome Custom Tab with shared cookies

</details>

<details>
<summary><b>Look &amp; feel</b></summary>

- Aurora theme system with dynamic colour generation
- Custom splash screens (image or video, click-to-skip, trim range, fixed
  orientation)
- Background music playlists with LRC lyric sync, 7 lyric animations
  (fade / slide / scale / typewriter / karaoke), 3 positions, custom theme
- Status bar theming (colour, dark/light icons, alpha, height, separate dark
  mode config)
- Floating window mode with adjustable size, opacity, corner radius, edge
  snap, position lock, auto-hide title bar
- 10 announcement template styles (Minimal, Xiaohongshu, Gradient,
  Glassmorphism, Neon, Cute, Elegant, Festive, Dark, Nature) triggered on
  launch / interval / no-network
- 7 screen orientation modes, screen-on lock with brightness control

</details>

<details>
<summary><b>Extension modules</b></summary>

- 10 built-in modules (video downloader, Bilibili / Douyin / Xiaohongshu
  extractors, video enhancer, web analyzer, dark mode, privacy protection,
  content enhancer, element blocker)
- Custom modules from three sources: plain JS, userscripts (`.user.js`),
  Chrome extensions (`manifest.json`)
- Full Greasemonkey / Tampermonkey `GM_*` bridge
- MV3 `declarativeNetRequest` engine — block, allow, redirect, modify headers
- Module sharing via export codes
- **Module Market** powered by this repository — see above

</details>

<details>
<summary><b>Security &amp; privacy</b></summary>

- APK encryption (PBKDF2, 100 000 iterations, custom passwords supported)
- App isolation with a separate data directory
- Browser and device fingerprint disguise
- Ad blocker (hosts rules + cosmetic MutationObserver filtering)
- Activation code gating (per-launch or persistent)
- App hardening: DEX encryption + splitting + VMP + control flow flattening,
  native SO encryption + ELF obfuscation + symbol strip + anti-dump
- Anti-Frida, Xposed, Magisk, debug, memory dump, screen capture
- Detection of emulators, VirtualApp, VPN, USB debugging
- String encryption, class name obfuscation, opaque predicates, multi-point
  signature verification, certificate pinning, threat response, honeypot,
  self-destruct

</details>

<details>
<summary><b>Forced run, BlackTech, device disguise</b></summary>

- **Forced run** in three modes (fixed period, countdown, access window).
  Blocks system UI, back/home/recents, and notifications. Countdown survives
  process kills. Emergency exit with a password and pre-end warning
- **BlackTech**: volume control (force max / mute / block keys), flashlight
  modes (strobe, SOS, Morse, heartbeat, breathing, emergency, custom alarm
  with vibe sync), system control (block power key, max performance), screen
  control (black screen, rotation, block touch), network control (WiFi
  hotspot SSID/password, disable WiFi / Bluetooth / mobile data), nuclear and
  stealth modes
- **Device disguise**: phone/tablet/desktop, OS spoofing
  (Android/iOS/HarmonyOS/macOS/Windows/Linux), brand and model presets,
  resolution, pixel ratio, timezone, locale

</details>

<details>
<summary><b>Server-side runtimes</b></summary>

- **Node.js** — 4 build modes (Static, SSR, API backend, Fullstack) with env vars
- **PHP** — Composer support, custom document root
- **Python** — Flask / Django / built-in server, pip dependencies
- **Go** — binary compilation and static file serving
- **WordPress** — themes + plugins, ships with bundled PHP

All runtimes execute on-device behind a local HTTP server. No remote backend.

</details>

<details>
<summary><b>App-type specific features</b></summary>

- **Gallery app** — categorised images and videos, grid / list / timeline
  views, sequential / shuffle / single-loop, thumbnail bar, media info,
  video auto-next, remember playback position
- **Multi-Web app** — tabs / cards / feed / drawer layout, per-site icon and
  CSS selector, auto-refresh interval
- **Website Scraper** — offline pack creator: crawl entire frontend
  (HTML / CSS / JS / images / fonts), concurrent download, recursive CSS
  `url()` resolution, absolute-to-relative path rewriting, same-domain
  restriction, depth/size limits
- **App Modifier** — clone and rebrand installed APKs (icon, name, package)
  via binary manifest patching

</details>

<details>
<summary><b>Translation, notifications, deep linking</b></summary>

- In-page translation overlay with 20 target languages and 5 engines
  (Google, MyMemory, LibreTranslate, Lingva, Auto), floating button toggle,
  auto-translate on load
- Web API `Notification` polyfill plus URL-polling foreground service with
  configurable interval and JSON parsing
- Custom URL schemes with configurable host patterns
- Boot auto-start, scheduled launch at a specific time
- Background-run foreground service with custom notification, CPU wake lock,
  battery optimisation bypass

</details>

<details>
<summary><b>APK export &amp; ads</b></summary>

- Custom package name and version
- Architecture targeting (Universal, ARM64, ARM32)
- Performance optimisations: image compression, WebP conversion, code
  minification, lazy loading, DNS prefetch, preload hints
- Granular runtime permissions (camera, mic, location, storage, Bluetooth,
  NFC, SMS, contacts, calendar, sensors, foreground service, wake lock,
  install packages, system alert window)
- Banner, interstitial, and splash ads with configurable IDs and durations
- Full app data backup and restore, project export/import

</details>

---

## Build from source

**Requirements:** Android Studio Hedgehog or newer, JDK 17, Gradle 8.14+.

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

For release builds, set up signing in `app/build.gradle.kts`.

## Tech stack

Kotlin · Jetpack Compose · Material Design 3 · Room · GeckoView · OkHttp · KSP
· Native C++ (JNI)

## Contributing

Three ways to help, listed roughly in increasing scope:

| Where | What | Guide |
| --- | --- | --- |
| `modules/` | Publish a community module to the in-app market | [`modules/README.md`](modules/README.md) |
| Issues | File a bug or request a feature | GitHub Issues |
| Code | Pick up something from the issue tracker or propose a change | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

## Contact

Developed by **shiaho**.

| Platform | Link |
| --- | --- |
| GitHub | [github.com/shiahonb777/web-to-app](https://github.com/shiahonb777/web-to-app) |
| Telegram | [t.me/webtoapp777](https://t.me/webtoapp777) |
| X (Twitter) | [@shiaho777](https://x.com/shiaho777) |
| Bilibili | [b23.tv/8mGDo2N](https://b23.tv/8mGDo2N) |
| QQ Group | 1041130206 |

## License

[The Unlicense](LICENSE). Advanced features (e.g. forced run) are intended for
technical demonstration and must only be used with informed user consent.

<div align="center">

**Open source · Free forever · Star to support**

</div>
