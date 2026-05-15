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
  <a href="#tech-stack">Tech stack</a> ·
  <a href="#build-from-source">Build</a> ·
  <a href="#contributing">Contributing</a>
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
install, share, or sideload. Behind the scenes the app stitches together a
hardened WebView, optional native runtimes (Node.js, PHP, Python, Go), and an
APK builder that signs and packages everything in-process via
[`com.android.tools.build:apksig`](https://mvnrepository.com/artifact/com.android.tools.build/apksig).
No remote build server is ever contacted.

**Supported app types:** Website · HTML · React · Vue · WordPress · Node.js ·
PHP · Python · Go · Image · Video · Gallery · Multi-Web

## Highlights

- **One-tap on-device APK builds** — packaged, signed, and installed without
  leaving the phone.
- **Two browser engines** — system WebView plus an optional GeckoView
  (Firefox) backend.
- **GitHub-backed Module Market** — install community JS/CSS modules without
  shipping an app update; the catalog lives in this repository.
- **Bundled Chrome extension support** — runs unmodified MV3 extensions
  inside the WebView. Ships with the BewlyCat extension as a working example.
- **Local server runtimes** — Node.js, PHP, Python, and Go execute on-device
  via a local HTTP server. WordPress runs against the bundled PHP.
- **Deeply customisable WebView** — UA spoofing, 28-vector fingerprint
  disguise, ad blocking, DNS-over-HTTPS, JS/CSS injection, payment scheme
  handlers.
- **App Modifier** — clone an installed APK and re-brand it (icon, name,
  package) by patching its binary AXML manifest and re-signing.
- **AI assistants** — generate extension modules, HTML projects, and app
  icons; build entire web pages with the agent-based AI Coding V2 runtime.
- **Per-app usage analytics** — Stats screen with health monitoring and Vico
  charts.
- **Trilingual UI** — Chinese, English, Arabic out of the box.

---

## Module Market

The Module Market lets users install community-built JS/CSS extension modules
in one tap, and the entire catalog **lives in this repository**. There's no
backend, no submission portal, no review queue beyond a regular GitHub PR.

```
modules/                                    ← published catalog
├── registry.json                           ← index the app fetches first
├── README.md                               ← contributor guide
├── hello-world/                            ← example: floating banner
│   ├── module.json
│   └── main.js
└── night-shift/                            ← example: amber overlay
    ├── module.json
    ├── main.js
    └── style.css
```

**For users:** open the app, navigate to *Extension Modules*, tap the
storefront icon in the top bar. Modules update automatically when new
versions are published.

**For contributors:** fork the repo, drop a folder under `modules/`, add an
entry to `registry.json`, and open a PR. Once it's merged, every WebToApp
client picks up the module on its next refresh (default cache is one hour).

→ [Full contributor guide](modules/README.md)
→ [General contributing guide](CONTRIBUTING.md)

---

## Feature catalog

Click any section to expand. Every claim below is backed by a class or enum
in this repository.

<details>
<summary><b>Browser engine &amp; networking</b></summary>

- Desktop mode, custom User-Agent, JS/CSS injection at `DOCUMENT_START` /
  `END` / `IDLE`.
- Popup blocker; new-window behaviour selectable as `SAME_WINDOW` /
  `EXTERNAL_BROWSER` / `POPUP_WINDOW` / `BLOCK`.
- HTTP, SOCKS5, and PAC proxies with optional authentication and bypass
  rules.
- DNS-over-HTTPS with seven providers (Cloudflare, Google, AdGuard, NextDNS,
  CleanBrowsing, Quad9, Mullvad) plus custom endpoints.
- PWA offline support with selectable caching strategy; custom error pages.
- Per-app hosts file overrides; payment scheme handlers (`alipay://`,
  `weixin://`, `paypal://`, etc.).
- A WebView compatibility suite that ships on-by-default: GPC header, cookie
  consent blocker, tracker blocker, blob download interception, scroll
  memory, image repair, HTTPS upgrade, kernel disguise, clipboard /
  orientation polyfills, Native Bridge, private-network bridge, Referrer
  Policy enforcement.

</details>

<details>
<summary><b>Browser fingerprint disguise (28 vectors)</b></summary>

Five preset levels (`Stealth` → `Ghost` → `Phantom` → `Specter` → `Custom`).
The disguise engine spoofs all of:

| Group | Vectors |
| --- | --- |
| Anti-detection baseline | `X-Requested-With` removal, UA sanitisation, hide `webdriver`, emulate Chrome `window`, fake plugins, fake vendor |
| Hardware fingerprints | Canvas noise, WebGL renderer (7 GPU profiles), AudioContext noise, Screen profile (7 device-class presets), ClientRects noise |
| Environment fingerprints | Timezone, language, platform, `hardwareConcurrency`, `deviceMemory` |
| Privacy fingerprints | MediaDevices, WebRTC IP shield, font enumeration block, battery shield |
| Network fingerprints | Connection, Permissions, Performance Timing, Storage Estimate, Notification, CSS Media |
| Hardening | `Native.toString` protection, iframe disguise propagation, error-stack cleaning |

Coverage levels: `OFF` → `BASIC` → `MODERATE` → `ADVANCED` → `DEEP` →
`MAXIMUM`.

</details>

<details>
<summary><b>OAuth in-WebView (30+ providers)</b></summary>

Per-provider anti-detection scripts let unmodified Chrome OAuth flows
complete inside the WebView. Recognised providers:

Google, Facebook, Apple, Microsoft, Amazon, Twitter / X, GitHub, Discord,
Reddit, LinkedIn, Spotify, Twitch, LINE, Kakao, Naver, WeChat, QQ, Alipay,
TikTok / Douyin, Yahoo Japan, Yahoo, VK, Yandex, Mail.ru, Shopify, Dropbox,
Notion, Slack, Zoom, PayPal, Stripe, Square, plus reCAPTCHA / hCaptcha /
Cloudflare Turnstile compatibility.

Google flows fall back to a Chrome Custom Tab with shared cookies (via
`androidx.browser`) when in-WebView completion is blocked.

</details>

<details>
<summary><b>Extension modules</b></summary>

- **11 built-in JS modules:** Video Downloader, Bilibili / Douyin /
  Xiaohongshu extractors, Video Enhancer, Web Analyzer, Find-in-Page, Dark
  Mode, Privacy Protection, Content Enhancer, Element Blocker.
- **1 built-in Chrome extension** (`assets/extensions/bewlycat/`): BewlyCat
  for Bilibili. Demonstrates the MV3 runtime working with a real-world
  extension.
- **3 module sources** for user-submitted modules: plain JavaScript,
  Greasemonkey/Tampermonkey userscripts (`.user.js`), and Chrome MV3
  extensions (`manifest.json`).
- Full `GM_*` API bridge for Tampermonkey scripts; per-script grants
  (`GM_setValue`, `GM_xmlhttpRequest`, etc.).
- MV3 `chrome.declarativeNetRequest` engine — block, allow, redirect, modify
  headers.
- Module sharing via export codes and QR codes (ZXing).
- AI Module Developer screen — write modules from a prompt, get the source
  back ready to install.
- The community **Module Market** described above.

</details>

<details>
<summary><b>Look &amp; feel</b></summary>

- Aurora theme system with dynamic colour generation.
- Custom splash screens — image or video, click-to-skip, video trim range,
  fixed orientation.
- Background music playlists with LRC sync, 6 lyric animations (fade,
  slide-up, slide-left, scale, typewriter, karaoke), 3 positions, custom
  font/colour/stroke/shadow theme. Online music search with 20+ tags.
- Status bar theming — colour, dark/light icons, alpha, height, separate
  dark-mode config.
- Floating window mode with adjustable size, opacity, corner radius, edge
  snap, position lock, auto-hide title bar, "start minimised" option.
- 10 announcement template styles (Minimal, Xiaohongshu, Gradient,
  Glassmorphism, Neon, Cute, Elegant, Festive, Dark, Nature) triggered on
  launch / interval / no-network.
- 7 screen orientation modes; screen-on lock with brightness control.
- 5 long-press menu styles (Simple, Full, iOS, Floating, Context).

</details>

<details>
<summary><b>Per-app usage analytics</b></summary>

- Stats screen with charts powered by Vico Compose.
- Tracks open count, total time, last open, and per-day usage per packaged
  app.
- App Health Monitor periodically `HEAD` s every app's URL and surfaces
  unreachable hosts.

</details>

<details>
<summary><b>Server-side runtimes (on-device)</b></summary>

- **Node.js** — 4 build modes (Static / SSR / API Backend / Fullstack), env
  vars, npm dependency manager, sample project gallery. Wraps a native
  `node_launcher` C++ executable.
- **PHP** — 8.4 binary downloaded once at build time from
  [`pmmp/PHP-Binaries`](https://github.com/pmmp/PHP-Binaries), Composer
  support, custom document root.
- **Python** — Flask, Django, or built-in HTTP server with pip dependency
  resolution.
- **Go** — on-device binary compilation and static file serving via the
  `go_exec_loader` C++ wrapper.
- **WordPress** — runs against the bundled PHP, theme + plugin support.
- **Linux environment** — bundled toolchain plus a screen for managing
  builds, dependencies, and ports across all five runtimes.
- **Port Manager** — cross-app port coordination via broadcast receivers, so
  multiple packaged apps don't fight for the same port.

</details>

<details>
<summary><b>App-type specific features</b></summary>

- **Gallery app** — categorised images and videos; grid / list / timeline
  views; sequential / shuffle / single-loop playback; sort by custom / name /
  date / type; thumbnail bar, media info overlay, video auto-next, remember
  playback position.
- **Multi-Web app** — sites in tabs / cards / feed / drawer layouts;
  per-site icon, theme colour, CSS selectors for content extraction;
  configurable refresh interval.
- **Website Scraper** — offline pack creator that crawls the entire
  frontend (HTML / CSS / JS / images / fonts), 6 concurrent downloads,
  recursive CSS `url()` / `srcset` / `@import` resolution, absolute-to-
  relative path rewriting, same-domain restriction, depth and size limits.
- **App Modifier** — two flavours: a launcher-shortcut disguise, or a real
  binary clone that patches AXML manifest, replaces icon resources via
  ARSC, and re-signs with `JarSigner` before installing through
  `FileProvider`.

</details>

<details>
<summary><b>Translation, notifications, deep linking, lifecycle</b></summary>

- **Translation overlay** — 20 target languages and 5 engines (Google,
  MyMemory, LibreTranslate, Lingva, Auto); floating button toggle;
  auto-translate on load.
- **Web Notification polyfill** plus a URL-polling foreground service with
  configurable interval (5 min minimum), JSON parsing, and GET / POST with
  custom headers.
- Custom URL schemes with configurable host patterns.
- Boot auto-start (`BOOT_COMPLETED`, `QUICKBOOT_POWERON`,
  `MY_PACKAGE_REPLACED`, time / timezone change).
- Scheduled launch at a specific time via `SCHEDULE_EXACT_ALARM`.
- Background-run foreground service with custom notification, CPU wake
  lock, and a battery-optimisation bypass prompt.

</details>

<details>
<summary><b>Generated-APK security &amp; hardening</b></summary>

The features below apply to the apps **WebToApp generates**. The host
itself ships with a minimal permission set (see `AndroidManifest.xml`).

- **APK encryption** — PBKDF2 with 100 000 iterations, custom passwords
  supported.
- **App isolation** — separate data directory, separate process for the
  packaged WebView.
- **Browser and device fingerprint disguise** — see above.
- **Ad blocker** — hosts-rule engine plus a cosmetic MutationObserver
  filter, with built-in support for 12 community filter lists (EasyList,
  EasyPrivacy, uBlock, AdGuard family, StevenBlack, Peter Lowe, 1Hosts Lite,
  regional lists, etc.).
- **Activation code gating** — per-launch or persistent; codes can be
  unlimited, time-limited, or device-bound.
- **App hardening pipeline** — DEX encryption + splitting, control-flow
  flattening, native SO encryption, ELF obfuscation, symbol strip,
  anti-dump.
- **Anti-reverse engineering** — anti-Frida / Xposed / Magisk / debug /
  memory dump / screen capture; emulator / VirtualApp / VPN / USB-debugging
  detection.
- **Code obfuscation** — string encryption, class name mangling, opaque
  predicates, multi-point signature verification, certificate pinning.
- **Threat response** — runtime shield with honeypot and self-destruct
  modes.

</details>

<details>
<summary><b>Forced run, BlackTech, multi-icon disguise</b></summary>

These exist for technical demonstration of Android's surface area. They
must be used with informed user consent.

- **Forced run** — three modes (`FIXED_TIME`, `COUNTDOWN`, `DURATION`).
  Blocks system UI, back / home / recents, and notifications. Countdown
  survives process kills. Optional emergency-exit password and a
  pre-end warning.
- **BlackTech** — every toggle declared in `BlackTechConfig`:
  - Volume control (force max / mute / block volume keys)
  - Flashlight modes — strobe, SOS, Morse code (with custom text and unit
    duration), heartbeat, breathing, emergency, custom alarm pattern with
    vibration sync
  - System control (block power key, max performance, airplane mode)
  - Screen control (black screen, force rotation, block touch, force awake)
  - Network control (WiFi hotspot with SSID/password, disable WiFi /
    Bluetooth / mobile data)
  - Pre-baked profiles: `SILENT_MODE`, `ALARM_MODE`, `SOS_SIGNAL`,
    `NUCLEAR_MODE`, `STEALTH_MODE`
- **Device disguise** — 6 device types (Phone / Tablet / Desktop / Laptop /
  Watch / TV) × 10 OSes (Android, iOS, HarmonyOS, Windows, macOS, Linux,
  ChromeOS, watchOS, Wear OS, tvOS); 28 brand-specific device presets
  including iPhone 17 Pro Max, Galaxy S26 Ultra, Pixel 10 Pro XL,
  Mate 70 Pro+, OnePlus 15, MacBook Pro M5, Surface Pro 11, Apple Watch
  Ultra 3, etc.
- **Icon Storm** — multi-launcher icon disguise. A packaged app can ship
  anywhere from `2` (Subtle) to `5000` (Research) launcher aliases. Modes:
  `Subtle Flood (25)`, `Icon Flood (100)`, `Icon Storm (500)`,
  `Extreme Storm (1000)`, `Research (5000)`, plus a custom count. Each
  alias adds roughly 520 bytes of manifest overhead — the screen estimates
  the impact for you.

</details>

<details>
<summary><b>APK export options</b></summary>

- Custom package name, `versionName`, and `versionCode`.
- Architecture targeting: Universal, ARM64, ARM32.
- Performance optimisations — image compression, WebP conversion, code
  minification, lazy loading, DNS prefetch, preload hints.
- Granular runtime permissions injected per-APK at build time (camera, mic,
  location, storage, Bluetooth, NFC, SMS, contacts, calendar, sensors,
  foreground service, wake lock, install packages, system alert window).
  These are **not** declared in the host manifest.
- Banner, interstitial, and splash ads with configurable IDs and durations.
- Full app data backup and restore; project export/import.

</details>

---

## Tech stack

The pieces that distinguish this from a basic Compose app:

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Koin** for dependency injection
- **Room 2.7.2** + **KSP** for persistence
- **OkHttp 4.12.0** + `okhttp-dnsoverhttps` for networking
- **`com.android.tools.build:apksig` 8.3.0** — the on-device signer
- **GeckoView** (Firefox engine) as an optional WebView replacement
- **Coil** (`compose` + `video` + `gif`) for images
- **AndroidX Security Crypto** + **AndroidX DataStore** for stored secrets
- **Vico** Compose-M3 for the Stats charts
- **ZXing** for QR-code module sharing
- **Apache Commons Compress** + **xz** for the website scraper and project
  imports
- **Native C++ via JNI** — `node_launcher` and `go_exec_loader` are real
  CMake targets compiled per-ABI
- **Robolectric** for the unit-test layer

See [`app/build.gradle.kts`](app/build.gradle.kts) for the full list.

---

## Build from source

**Requirements:** Android Studio Hedgehog or newer, JDK 17, Gradle 8.14+.

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

For release builds, configure signing in `app/build.gradle.kts`. The first
release build will download the PHP binary; you can pre-fetch it with
`./gradlew :app:downloadPhpBinary`.

---

## Contributing

Three lanes, in increasing scope:

| Lane | What you do | Guide |
| --- | --- | --- |
| `modules/` | Publish a community module to the in-app market | [`modules/README.md`](modules/README.md) |
| Issues | Report a bug or request a feature | [GitHub Issues](https://github.com/shiahonb777/web-to-app/issues) |
| Code | Fix a bug or build a feature in the Android client | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

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

[The Unlicense](LICENSE). Advanced features (e.g. forced run, BlackTech,
icon storm) are intended for technical demonstration and must only be used
with informed user consent.

<div align="center">

**Open source · Free forever · Star to support**

</div>
