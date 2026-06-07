<div align="center">

# WebToApp

### Any website. One tap. An app.

No IDE. No build server. No PC.

**English** · [简体中文](README_CN.md)

[![Stars](https://img.shields.io/github/stars/shiaho777/web-to-app?style=for-the-badge)](https://github.com/shiaho777/web-to-app/stargazers)
[![Forks](https://img.shields.io/github/forks/shiaho777/web-to-app?style=for-the-badge)](https://github.com/shiaho777/web-to-app/network/members)
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
<img src="social-preview.jpg" width="90%" alt="WebToApp: My Apps home with built Web/HTML/Python/Image apps, the 12-type Create App picker, the main menu (AI Coding, Extension Modules, App Modifier, Local Build Environment, Runtime Manager, Port Manager, Browser Kernel, Hosts Blocking, Usage Stats), and the per-app actions (Edit, Create Shortcut, Build APK, Share APK, Export APK) — all on-device APK building from your phone, no PC required" />
</div>

---

## What it does

WebToApp turns websites, HTML projects, media libraries, and even server-side
applications into installable Android APKs — entirely on the device.

Drop in a URL, pick the bits you want, and you walk away with an APK you can
install, share, or sideload. Behind the scenes the app stitches together a
configurable WebView, optional native runtimes (Node.js, PHP, Python, Go), and
an APK builder that signs and packages everything in-process via
[`com.android.tools.build:apksig`](https://mvnrepository.com/artifact/com.android.tools.build/apksig).
No remote build server is ever contacted.

**Supported app types** (the `AppType` enum): Web · HTML · Frontend
(React / Vue / static builds) · WordPress · Node.js · PHP · Python · Go ·
Image · Video · Gallery · Multi-Web.

## Highlights

- **One-tap on-device APK builds** — packaged, signed, and installed without
  leaving the phone.
- **Two browser engines** — system WebView plus an optional GeckoView
  (Firefox) backend. The GeckoView engine ships arm64-v8a only and its native
  runtime is downloaded on first use, not bundled.
- **GitHub-backed Module Market** — install community JS/CSS modules without
  shipping an app update; the catalog lives in this repository.
- **Bundled Chrome extension support** — runs unmodified MV3 extensions
  inside the WebView. Load your own via the Extension Modules screen.
- **Local server runtimes** — Node.js, PHP, Python, and Go execute on-device
  via a local HTTP server. WordPress runs against the on-device PHP runtime.
- **Customisable WebView** — UA spoofing, 28-vector fingerprint disguise, ad
  blocking, DNS-over-HTTPS, JS/CSS injection, payment scheme handlers.
- **App Modifier** — clone an installed APK and re-brand it (icon, name,
  package) by patching its binary AXML manifest and re-signing.
- **On-device APK signing** — create, import, export, and inspect keystores
  (MD5 / SHA-1 / SHA-256 fingerprints) right on the phone; choose V1/V2/V3
  signature schemes and a custom V1 signature filename.
- **AI Coding** — a skill-driven agent that builds web apps, extension
  modules, and full app types (HTML, React, Vue, Node, PHP, Python, Go,
  WordPress, etc.) on your phone, with inline preview and built-in image
  generation.
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
├── submissions.json                        ← CI-generated PR / submitter metadata
├── README.md                               ← contributor guide
├── hello-world/                            ← example: floating banner
│   ├── module.json
│   └── main.js
├── night-shift/                            ← example: amber overlay
│   ├── module.json
│   ├── main.js
│   └── style.css
├── reading-mode/                           ← reader view
├── floating-search/                        ← selection action bar
└── auto-scroll/                            ← auto-scroll remote
```

The app fetches **both** `registry.json` and `submissions.json`, and only
shows a module when it appears in both — that's how the catalog guarantees it
lists only modules whose PR has actually been merged (`submissions.json` is
regenerated by CI on every push to `main`).

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
  `END` / `IDLE` (`ScriptRunTime`).
- Build-time **kernel flavor** disguise (`KernelFlavor`): make the generated
  APK present itself as Chrome / Edge / Samsung Internet (Blink), Firefox
  (Gecko) or Safari (WebKit) — a consistent spoof of UA, `navigator.userAgentData`
  / Sec-CH-UA brands, `window.chrome` presence and `vendor`. Client-hint request
  headers are driven through the official `WebSettingsCompat.setUserAgentMetadata`
  API (where the WebView supports it). The real engine is unchanged.
- Popup blocker; new-window behaviour (`NewWindowBehavior`) selectable as
  `SAME_WINDOW` / `EXTERNAL_BROWSER` / `POPUP_WINDOW` / `BLOCK`.
- Static (HTTP / HTTPS / SOCKS5) and PAC proxies with optional authentication
  and bypass rules. SOCKS5 is routed through a local HTTP-to-SOCKS bridge.
- DNS-over-HTTPS with seven providers (Cloudflare, Google, AdGuard, NextDNS,
  CleanBrowsing, Quad9, Mullvad) plus custom endpoints (`DnsProvider`).
- PWA offline support with selectable caching strategy; custom error pages.
- Per-app hosts file overrides; payment scheme handlers (`alipay://`,
  `weixin://`, `paypal://`, etc.).
- Opt-in WebView compatibility toggles (all default off): blob download
  interception, scroll memory, image repair, kernel disguise
  (`KernelDisguiseLevel`), clipboard / orientation / notification polyfills,
  Native Bridge (with a per-capability allow-list), and a private-network
  bridge.

</details>

<details>
<summary><b>Browser fingerprint disguise (28 vectors)</b></summary>

Five preset levels (`STEALTH` → `GHOST` → `PHANTOM` → `SPECTER` → `CUSTOM`,
plus `OFF`). The disguise engine (`BrowserDisguiseConfig`) spoofs:

| Group | Vectors |
| --- | --- |
| Anti-detection baseline | `X-Requested-With` removal, UA sanitisation, hide `webdriver`, emulate Chrome `window`, fake plugins, fake vendor |
| Hardware fingerprints | Canvas noise, WebGL renderer (7 GPU profiles), AudioContext noise, Screen profile (7 device-class presets), ClientRects noise |
| Environment fingerprints | Timezone, language, platform, `hardwareConcurrency`, `deviceMemory` |
| Privacy fingerprints | MediaDevices, WebRTC IP shield, font enumeration block, battery shield |
| Network fingerprints | Connection, Permissions, Performance Timing, Storage Estimate, Notification, CSS Media |
| Hardening | `Native.toString` protection, iframe disguise propagation, error-stack cleaning |

Coverage is reported as `OFF` → `BASIC` → `MODERATE` → `ADVANCED` → `DEEP` →
`MAXIMUM` based on how many of the 28 vectors are active.

</details>

<details>
<summary><b>OAuth in-WebView (30+ providers)</b></summary>

Per-provider anti-detection scripts let unmodified Chrome OAuth flows
complete inside the WebView. The `OAuthCompatEngine.Provider` enum recognises
32 branded providers:

Google, Facebook, Apple, Microsoft, Amazon, Twitter / X, GitHub, Discord,
Reddit, LinkedIn, Spotify, Twitch, LINE, Kakao, Naver, WeChat, QQ, Alipay,
TikTok / Douyin, Yahoo Japan, Yahoo, VK, Yandex, Mail.ru, Shopify, Dropbox,
Notion, Slack, Zoom, PayPal, Stripe, Square — plus reCAPTCHA / hCaptcha /
Cloudflare Turnstile compatibility and a generic OAuth fallback.

When a recognised OAuth flow can't complete in-WebView, it falls back to a
Chrome Custom Tab with shared cookies (via `androidx.browser`).

</details>

<details>
<summary><b>Extension modules</b></summary>

- **11 built-in JS modules** (`BuiltInModules`): Video Downloader, Bilibili /
  Douyin / Xiaohongshu extractors, Video Enhancer, Web Analyzer, Find-in-Page,
  Dark Mode, Privacy Protection, Content Enhancer, Element Blocker.
- **MV3 Chrome extension runtime** — load unmodified Chrome MV3 extensions
  (manifest + ISOLATED / MAIN world content scripts) via the Extension Modules
  screen.
- **3 module sources** (`ModuleSourceType`): plain JavaScript (`CUSTOM`),
  Greasemonkey/Tampermonkey userscripts (`USERSCRIPT`, `.user.js`), and Chrome
  MV3 extensions (`CHROME_EXTENSION`, `manifest.json`).
- A `GM_*` API bridge for Tampermonkey scripts (`GM_setValue`,
  `GM_xmlhttpRequest`, `GM_addStyle`, menu commands, the Promise-based `GM.*`
  surface, etc.) with per-script grants.
- An MV3 `chrome.declarativeNetRequest` engine (`ActionType`): block, allow,
  redirect, upgrade-scheme, and allow-all-requests are honoured;
  modify-headers rules are parsed but not yet applied. A broad `chrome.*`
  polyfill (runtime, storage, tabs, scripting, etc.) backs MV3 extensions.
- Module sharing via export codes (`WTA1:` gzip+Base64) and QR codes (ZXing).
- An **AI Coding** agent that can generate extension modules from a prompt via
  the `module-js` / `module-style` / `module-userscript` /
  `module-chrome-mv3` skills.
- The community **Module Market** described above.

</details>

<details>
<summary><b>Look &amp; feel</b></summary>

- A single calibrated monochrome theme (`AppThemes.KimiNoNawa`) with separate
  light and dark schemes; Material You dynamic colour is opt-in (off by
  default).
- Custom splash screens — image or video, click-to-skip, video trim range,
  fixed orientation.
- Background music playlists with LRC sync, 6 lyric animations (fade,
  slide-up, slide-left, scale, typewriter, karaoke), 3 positions (top /
  center / bottom), custom font/colour/stroke/shadow theme. Online music
  search with 20 genre tags.
- Status bar theming — colour, dark/light icons, height, and a separate
  dark-mode config.
- Floating window mode with adjustable size, opacity, corner radius, edge
  snap, position lock, auto-hide title bar, and a "start minimised" option.
- 10 announcement template styles (Minimal, Xiaohongshu, Gradient,
  Glassmorphism, Neon, Cute, Elegant, Festive, Dark, Nature) triggered on
  launch / interval / no-network.
- 7 screen orientation modes; screen-on lock with brightness control.
- 5 long-press menu styles (Simple, Full, iOS, Floating, Context).

</details>

<details>
<summary><b>Per-app usage analytics</b></summary>

- Stats screen with charts powered by Vico Compose.
- Tracks open count, total time, last-open time, and last-session duration per
  packaged app.
- App Health Monitor periodically `HEAD`s every app's URL and surfaces
  unreachable hosts (`HealthStatus`: UNKNOWN / ONLINE / SLOW / OFFLINE).

</details>

<details>
<summary><b>Server-side runtimes (on-device)</b></summary>

- **Node.js** — env vars, an npm dependency manager, and a sample project
  gallery (Express / Fastify / Koa). The runtime runs inside a dedicated
  `:nodejs` OS process and wraps a native `node_launcher` C++ executable that
  `dlopen`s `libnode.so` (so a crashing `server.js` can't take down the host).
- **PHP** — PHP 8.4, downloaded once on first use from
  [`pmmp/PHP-Binaries`](https://github.com/pmmp/PHP-Binaries) (arm64-v8a),
  with Composer support and a configurable document root.
- **Python** — Flask, Django, FastAPI (via uvicorn), Tornado, or a built-in
  HTTP server, with pip dependency resolution into `.pypackages`.
- **Go** — on-device `go build` (with `vendor/` offline support) and static
  file serving, executed via the `go_exec_loader` C++ wrapper.
- **WordPress** — runs against the on-device PHP runtime, backed by SQLite
  (WordPress 6.9.1 + the `sqlite-database-integration` plugin), with theme +
  plugin import.
- **Linux environment** — a screen that installs toolchains on demand and
  manages builds/dependencies for the Node, PHP, and Python runtimes (Go and
  WordPress are managed by their own dependency managers).
- **Port Manager** — cross-app port coordination via broadcast receivers
  (`PortQueryReceiver` / `PortReleaseReceiver`), so multiple packaged apps
  don't fight for the same port. Each runtime gets its own port range.

</details>

<details>
<summary><b>App-type specific features</b></summary>

- **Gallery app** — categorised images and videos; grid / list / timeline
  views; sequential / shuffle / single-loop playback; sort by custom / name /
  date / type; thumbnail bar, media info overlay, video auto-next, remember
  playback position.
- **Multi-Web app** — sites in tabs / cards / feed / drawer layouts;
  per-site icon, theme colour, CSS selectors for content extraction;
  configurable refresh interval; add a site as inline HTML/JS pasted
  directly (no file needed); inject custom JS/CSS across all sites.
- **Website Scraper** — offline pack creator that crawls the entire
  frontend (HTML / CSS / JS / images / fonts), 6 concurrent downloads,
  recursive CSS `url()` / `srcset` / `@import` resolution, absolute-to-
  relative path rewriting, same-domain restriction, depth and size limits.
- **App Modifier** — two flavours: a launcher-shortcut disguise, or a real
  binary clone that patches the AXML manifest, replaces icon resources via
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
- Custom URL schemes (deep links) with configurable host patterns.
- Boot auto-start (`BOOT_COMPLETED`, `QUICKBOOT_POWERON`,
  `MY_PACKAGE_REPLACED`, time / timezone change).
- Scheduled launch at a specific time via `SCHEDULE_EXACT_ALARM`.
- Background-run foreground service with custom notification, CPU wake
  lock, and a battery-optimisation bypass prompt.

</details>

<details>
<summary><b>Generated-APK security</b></summary>

The features below apply to the apps **WebToApp generates**. The host
itself ships with a minimal permission set (see `AndroidManifest.xml`).

- **Resource encryption** — `PBKDF2WithHmacSHA256` + AES-256-GCM (100 000
  PBKDF2 iterations) encrypts the packaged assets (config, HTML, media, BGM).
  With no custom password the key is derived from the package name and signing
  certificate — both public in the APK — so encryption then only deters casual
  extraction; set a **custom password** for protection that survives
  reverse-engineering of the host.
- **Runtime protection** — bundled with resource encryption: when encryption
  is on, the generated app runs anti-debug, anti-Frida and DEX-tamper (CRC)
  checks at launch. A configurable **threat response** (`ThreatResponse`)
  decides what happens on a high-risk hit — `LOG_ONLY` (default, never affects
  normal users), `SILENT_EXIT`, or `CRASH_RANDOM`. This is lightweight
  protection that raises the bar for dynamic analysis; it cannot stop static
  reverse-engineering of the open-source host code.
- **WebView / content isolation** (`IsolationConfig`) — storage isolation,
  WebRTC blocking, Canvas / Audio / WebGL / font protection, and
  fingerprint / header / IP spoofing for the packaged WebView.
- **Browser and device fingerprint disguise** — see above.
- **Ad blocker** — a hosts-rule engine plus a cosmetic MutationObserver
  filter, with 23 built-in community filter lists (EasyList, EasyPrivacy,
  uBlock, AdGuard family, StevenBlack, AdAway, Peter Lowe, 1Hosts Lite,
  Anti-AD, and several regional lists).
- **Activation code gating** — per-launch or persistent; codes can be
  permanent, time-limited, usage-limited, device-bound, or combined
  (`ActivationCodeType`). Codes are verified locally by default, or against
  **your own HTTPS endpoint** (`RemoteActivationConfig`) so you can revoke and
  issue them without rebuilding — the server response is checked with an
  EC P-256 signature you control, with a configurable offline policy. Remote
  verification still runs on the client, so it raises the bar rather than
  preventing a determined bypass. See
  [`docs/remote-activation.md`](docs/remote-activation.md) for the server
  contract and a reference implementation.

</details>

<details>
<summary><b>Forced run, BlackTech, multi-icon disguise</b></summary>

These exist for technical demonstration of Android's surface area. They
must be used with informed user consent.

- **Forced run** — three modes (`FIXED_TIME`, `COUNTDOWN`, `DURATION`).
  Blocks system UI, back / home / recents, and notifications. Countdown
  survives process kills. Optional emergency-exit password and a
  pre-end warning.
- **BlackTech** (`DeviceActionsConfig`, serialised as `blackTechConfig`):
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
  ChromeOS, watchOS, Wear OS, tvOS); 49 brand-specific device presets
  including iPhone 17 Pro Max, Galaxy S26 Ultra, Pixel 10 Pro XL,
  Mate 70 Pro+, OnePlus 15, MacBook Pro M5, Surface Pro 11, Apple Watch
  Ultra 3, etc.
- **Icon Storm** (`IconStormMode`) — multi-launcher icon disguise. A packaged
  app can ship anywhere from a custom minimum up to `5000` launcher aliases.
  Modes: `Subtle Flood (25)`, `Icon Flood (100)`, `Icon Storm (500)`,
  `Extreme Storm (1000)`, `Research (5000)`, plus a custom count. Each
  alias adds roughly 520 bytes of manifest overhead — the screen estimates
  the impact for you.

</details>

<details>
<summary><b>APK export options</b></summary>

- Custom package name, `versionName`, and `versionCode`.
- Architecture targeting (`ApkArchitecture`): Universal, ARM64, ARM32.
- Performance optimisations — image compression, WebP conversion, code
  minification, lazy loading, DNS prefetch, preload hints.
- Granular runtime permissions injected per-APK at build time (camera, mic,
  location, storage, Bluetooth, NFC, SMS, contacts, calendar, sensors,
  foreground service, wake lock, install packages, system alert window).
  These are **not** declared in the host manifest.
- Banner, interstitial, and splash ad configuration (the config/plumbing is
  present; no ad SDK is bundled, so serving is a stub).
- Full app data backup and restore; project export/import.
- AAB export for Play-style distribution (the bundle's protobuf metadata is
  generated on-device).

</details>

<details>
<summary><b>APK signing &amp; keystore management</b></summary>

All signing happens on-device through `JarSigner`, which drives
`com.android.tools.build:apksig`. The signing identity is a global setting —
whatever you pick here is used for **every** newly built APK.

- **Three signer identities** (`JarSigner.SignerType`): `PKCS12_CUSTOM` (a
  keystore you created or imported), `PKCS12_AUTO` (an auto-generated keystore
  on first run), and `ANDROID_KEYSTORE` (system-backed fallback). Custom takes
  priority on every launch.
- **Create a keystore on-device** (`createCustomKeystore` /
  `CertificateSpec`) — generate a fresh RSA key pair (2048 / 4096) and a
  self-signed certificate with a full X.500 subject (CN / O / OU / L / ST / C)
  and a configurable validity (default 30 years), no PC or `keytool` required.
- **Import** PKCS12 / PFX / JKS / BKS keystores (`importKeystore`), including
  the JKS / Android-Studio "upload key" case where the key password differs
  from the store password. Non-PKCS12 formats are converted to PKCS12 and the
  real key alias is persisted in a sidecar so custom signing survives restarts.
- **Export** the active key as a password-protected `.p12` for backup
  (`exportPkcs12`), and **remove** a custom keystore to fall back to the
  auto-generated one.
- **View fingerprints** (`getCertificateFingerprints` →
  `CertificateFingerprints`) — MD5 / SHA-1 / SHA-256 of the certificate,
  tap-to-copy.
- **Signature scheme selection** (`SigningSchemeOptions`) — toggle V1 (JAR),
  V2 (full APK), and V3 (key rotation) independently. An **auto-fallback**
  switch progressively drops the newest scheme (V3 → V2) and re-verifies when
  a legacy certificate algorithm is incompatible with a newer scheme; turn it
  off to sign strictly with exactly the schemes you selected.
- **Custom V1 signature filename** — set the `<name>` in `META-INF/<name>.SF`
  and `META-INF/<name>.RSA`. Leave it blank to auto-derive the name from the
  signing key's certificate CN, with a live preview.

</details>

---

## Tech stack

The pieces that distinguish this from a basic Compose app:

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Koin** for dependency injection
- **Room 2.7.2** + **KSP** for persistence
- **OkHttp 4.12.0** + `okhttp-dnsoverhttps` for networking
- **`com.android.tools.build:apksig` 8.3.0** — the on-device signer
- **`protobuf-javalite` 3.25.5** — encodes the metadata for on-device AAB
  export
- **GeckoView** (Firefox engine, arm64-v8a) as an optional WebView
  replacement — the native runtime is downloaded on first use, not bundled
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

> **Why `targetSdk = 28`?** WebToApp must `fork`+`exec` native binaries
> (PHP, Go, etc.) from app storage. Android 10+ blocks this for
> `untrusted_app` under SELinux, so — like Termux — the app pins
> `targetSdk = 28` and distributes via GitHub Releases rather than Google
> Play. See the comment in `app/build.gradle.kts` for the full rationale.

---

## Build from source

**Requirements:** Android Studio Hedgehog or newer, JDK 17. The Gradle
wrapper pins Gradle 9.4.1, so you don't need a system Gradle install.

```bash
git clone https://github.com/shiaho777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

For release builds, configure signing in `app/build.gradle.kts` (via
`local.properties`). The server-side runtimes (PHP, Node.js, Python, Go, the
GeckoView engine) are **not** bundled in the APK — they download on first use
from inside the app, which keeps the host build small.

---

## Contributing

Three lanes, in increasing scope:

| Lane | What you do | Guide |
| --- | --- | --- |
| `modules/` | Publish a community module to the in-app market | [`modules/README.md`](modules/README.md) |
| Issues | Report a bug or request a feature | [GitHub Issues](https://github.com/shiaho777/web-to-app/issues) |
| Code | Fix a bug or build a feature in the Android client | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

## Contact

Developed by **shiaho**.

| Platform | Link |
| --- | --- |
| GitHub | [github.com/shiaho777/web-to-app](https://github.com/shiaho777/web-to-app) |
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
