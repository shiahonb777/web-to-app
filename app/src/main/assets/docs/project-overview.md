# Web-to-App Project Overview

## Project Introduction

WebToApp is an Android application that converts any website into a standalone Android app (APK). Users can configure WebView/GeckoView settings, inject custom JavaScript/CSS, and export a fully functional APK — no coding required.

---

## Project Structure

```
web-to-app/
├── app/                           # Main module (editor + builder)
│   └── src/main/java/com/webtoapp/
│       ├── core/                  # Core business logic
│       │   ├── auth/              # Authentication
│       │   ├── cloud/             # Cloud services
│       │   ├── i18n/              # Internationalization
│       │   ├── extension/         # Extension module system
│       │   ├── dns/               # Custom DNS / DoH
│       │   ├── scraper/           # Website packaging engine
│       │   ├── apkbuilder/        # APK builder
│       │   └── ...
│       ├── ui/                    # UI layer (Jetpack Compose)
│       │   ├── screens/           # Page-level Composables
│       │   ├── viewmodel/         # ViewModel layer
│       │   ├── components/        # Reusable UI components
│       │   └── ...
│       └── di/                    # Dependency injection
├── shell/                        # Shell module (WebView shell)
├── docs/                         # Development documentation
├── res/                          # Resource files
└── png/                          # Image assets
```

## Core Modules

| Module | Path | Description |
|--------|------|-------------|
| Auth | `core/auth/` | Login/Register/Token management/Avatar upload |
| Cloud | `core/cloud/` | Project management/Version publishing/APK upload/Activation codes/Announcements/Config/Backup/Module marketplace/Rating distribution/Comment sorting |
| i18n | `core/i18n/` | `Strings.kt` (33000+ lines) centralized UI string management |
| Extension | `core/extension/` | Module system/Share codes/Remote scripts |
| DNS | `core/dns/` | Custom DNS/DoH resolution/Bypass ISP DNS pollution |
| Scraper | `core/scraper/` | Offline website packaging engine |
| Community | `ui/viewmodel/CommunityViewModel.kt` | Posts/Modules/Comments/Notifications/Follow/Search/Rating distribution/Comment sorting |
| Cloud Mgmt | `ui/viewmodel/CloudViewModel.kt` | Projects/Versions/Activation codes/Backup/Push/Featured/Update detection |

## Code Scale

- Total code: 300K+ lines
- `Strings.kt`: 33000+ lines (largest single file, contains all localized strings)
- ViewModel files: average 1000-1800 lines

## Documentation Index

| Document | Description |
|----------|-------------|
| [_doc-manual.md](./_doc-manual.md) | Doc Manual — Meta-spec for creating/modifying docs |
| [_doc-maintenance.md](./_doc-maintenance.md) | Doc Maintenance — Audit & correction process |
| [architecture-guide.md](./architecture-guide.md) | Architecture — Layered design, dual-track mode, core subsystems |
| [build-and-release-guide.md](./build-and-release-guide.md) | Build & Release — Build config, signing, Shell template, release flow |
| [data-model-guide.md](./data-model-guide.md) | Data Model — WebApp model, nested configs, enums, serialization |
| [extension-module-guide.md](./extension-module-guide.md) | Extension Module — Module development, URL matching, config, injection |
| [shell-mode-guide.md](./shell-mode-guide.md) | Shell Mode — Shell architecture, config loading, lifecycle, Manifest |
| [security-features-guide.md](./security-features-guide.md) | Security — Encryption, hardening, privacy isolation, shields, activation |
| [i18n-localization-guide.md](./i18n-localization-guide.md) | i18n methodology, lessons & best practices |
| [unit-test-guide.md](./unit-test-guide.md) | Unit Test — Test checklist, coverage matrix, conventions, how to run |
| [feedback-guide.md](./feedback-guide.md) | Feedback — Standard format, screenshot rules, great examples |

---

## Revision History

| Date | Changes | Author |
|------|---------|--------|
| 2026-04-25 | Initial English version | Cascade |
