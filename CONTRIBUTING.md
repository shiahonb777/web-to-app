# Contributing to WebToApp

Thanks for taking the time to help. WebToApp moves fastest when contributions
stay **small and well-scoped** — pick one of the lanes below and ignore the
rest.

> **English** · [简体中文](#贡献-webtoapp中文)

This guide targets **WebToApp 2.0.0** (`versionCode 35`).

---

## Lanes

| You want to… | Go to | Effort |
| --- | --- | --- |
| Publish a JS/CSS module to the in-app **Module Market** | [`modules/README.md`](modules/README.md) | hours |
| File a bug, request a feature, or ask a question | [GitHub Issues](https://github.com/shiahonb777/web-to-app/issues) | minutes |
| Fix a bug or build a feature in the Android client | This guide ↓ | days |

If you're not sure which lane fits, open an issue or a discussion first. There
is no need to write code before there's agreement on the shape of the change.

---

## Module Market submissions

The fastest way to ship something useful to every WebToApp user is to publish
a module. The full schema, reviewer checklist, and PR template live in
[`modules/README.md`](modules/README.md). The short version:

1. Fork the repo.
2. Add `modules/<your-module>/module.json` and `main.js` (plus `style.css` if
   you need CSS).
3. Add an entry to `modules/registry.json`.
4. Open a PR.

The catalog has **no backend**. CI regenerates `modules/submissions.json` on
every push to `main`, and a client only shows a module once it appears in both
`registry.json` and `submissions.json` — that's how the market guarantees it
lists only modules whose PR has actually merged. Once merged, every client
picks up the module on its next refresh (default cache is one hour).

Module changes are validated in CI by `tools/ci/validate_modules.py`
(see `.github/workflows/modules-check.yml`). Run it locally before opening a
PR:

```bash
python3 tools/ci/validate_modules.py
```

---

## Code contributions

### Before you write code

- Search [issues](https://github.com/shiahonb777/web-to-app/issues) for prior
  discussion of the same idea.
- For non-trivial changes, open an issue first describing the problem and the
  approach you have in mind. This is much cheaper than rewriting after review.
- **Avoid adding new dependencies.** The list in `app/build.gradle.kts` is
  intentionally restrained — the project signs and packages APKs in-process
  and pins `targetSdk = 28` on purpose (see the rationale comment in that
  file). New dependencies need a strong justification.

### Local setup

You'll need:

- Android Studio Hedgehog or newer
- JDK 17
- The Gradle wrapper pins Gradle 9.4.1 — no system Gradle install required

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

The repo has two modules: **`app`** (the full builder) and **`shell`** (the
runtime-only host embedded in generated APKs). A change to shared runtime code
usually needs both to compile.

Run the checks before submitting:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :shell:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

> Native code (`node_launcher`, `go_exec_loader`, the APK optimizer) builds
> via CMake per-ABI and needs the Android NDK + CMake installed through the
> SDK Manager. CI installs `cmake;3.22.1` and `ndk;28.2.13676358`.

### Where things live

| Area | Path |
| --- | --- |
| App types & central config (`AppType`, `WebApp`) | `app/src/main/java/com/webtoapp/data/model/` |
| On-device APK builder / signer | `app/src/main/java/com/webtoapp/core/apkbuilder/` |
| Server runtimes (Node / PHP / Python / Go / WordPress) | `app/src/main/java/com/webtoapp/core/{nodejs,php,python,golang,wordpress}/` |
| WebView engine, native bridge, fingerprint disguise | `app/src/main/java/com/webtoapp/core/{webview,engine,appearance}/` |
| Extension modules, Module Market, AI Coding | `app/src/main/java/com/webtoapp/core/{extension,market,aicoding}/` |
| Compose UI screens & design system | `app/src/main/java/com/webtoapp/ui/` |
| DI graph (source of truth) | `app/src/main/java/com/webtoapp/di/AppModule.kt` |

### Coding conventions

WebToApp leans on the Kotlin and Jetpack Compose patterns already in the
codebase. A few rules worth calling out:

- **This codebase ships without comments.** Source files are kept
  comment-free by convention — let clear names and small functions carry the
  intent. Don't add `//`, `/* */`, or KDoc blocks; if a block needs a comment
  to be understood, prefer rewriting it. (Strings inside code, e.g. URLs, are
  obviously fine.)
- **Build new UI on the Wta design system.** Everything renders through
  `com.webtoapp.ui.design`. The older `Premium*` / `Enhanced*` / `Settings*`
  components are retained as permanent alias layers over the Wta internals —
  don't add new ones, and you don't need to rip them out. A build-time audit
  (`tools/audit_ui_design_system.py`, wired into `build.gradle.kts`) tracks
  legacy UI debt against `tools/ui_design_allowlist.txt`.
- **Reuse the design tokens** in `ui/design/WtaTokens.kt` for spacing, radius,
  alpha, and elevation. Don't hard-code numbers.
- **Strings are trilingual.** Add new strings to all three language branches —
  Chinese, English, Arabic — in `core/i18n/Strings.kt`. If you can't
  translate, use the English text in all three and flag it in the PR. A
  translation-parity unit test guards this.
- **No new top-level singletons** unless you discuss it first. The DI graph in
  `di/AppModule.kt` is the source of truth.
- **The Native Bridge is capability-gated.** Any method exposed to web content
  via `@JavascriptInterface` must be guarded by the per-capability allow-list
  (`NativeBridgeCapabilities`). Never expose a native capability to arbitrary
  pages without a gate. PRs that bypass this will be rejected.
- **Avoid catching `Exception` to silence errors.** If recovery is impossible,
  log via `AppLogger` and re-throw or return a failed `Result`.

### Security

Anything touching the WebView, file IO, APK signing, or the native bridge has
downstream impact on **every generated app**, so it gets extra scrutiny.
Specifically, PRs will be declined if they:

- store or expose credentials/secrets to web content,
- widen the native surface exposed to arbitrary pages without a capability
  gate,
- weaken APK signing, isolation, or the fingerprint-disguise defaults,
- bundle unrelated changes inside a large diff.

### Commit messages

We don't enforce Conventional Commits, but a clear subject line and a wrapped
body that explains *why* the change exists make reviews much faster. Example:

```
ModuleMarket: cache registry.json for an hour

Repeatedly hitting raw.githubusercontent.com on every screen open is wasteful
and triggers GitHub's anonymous rate limit on slow connections. Cache the
parsed registry under cache/module_market/ and treat anything fresher than
an hour as authoritative; the refresh button always bypasses the cache.
```

### Pull requests

- Branch from `main`. Keep PRs focused — one logical change per PR.
- Describe the user-visible effect in the PR body, not just the code change.
- If your PR touches the build system, native code, or APK packaging, attach
  the output of `./gradlew :app:assembleDebug` (or note the failure if it
  fails on your machine).
- CI runs on every PR. A green CI is required before merge.

### Reviewer expectations

Maintainer reviews look for:

- **Correctness.** Does the change do what the PR description says?
- **Safety.** See *Security* above.
- **Scope discipline.** Drive-by refactors expand the review surface. Land
  them in a separate PR.
- **Style fit.** See *Coding conventions* above.

---

## Code of conduct

Feedback is required to be precise. "This code is garbage" is acceptable only
when accompanied by which code, why, and what would be less so. The maintainer
(**shiaho**) has on file a standing waiver covering criticism of himself, his
judgment, and his life choices. Praise unaccompanied by a working patch is
logged and otherwise disregarded.

---

<a id="贡献-webtoapp中文"></a>
## 贡献 WebToApp（中文）

非常感谢你愿意花时间。WebToApp 的迭代速度取决于"小而聚焦"的贡献——下面三条路
里挑一条走，其他的先忽略。

本指南对应 **WebToApp 2.0.0**（`versionCode 35`）。

### 你想做什么？

| 你想…… | 路径 | 投入 |
| --- | --- | --- |
| 给应用内的 **模块市场** 提交一个 JS/CSS 模块 | [`modules/README.md`](modules/README.md) | 几小时 |
| 报 Bug、提 Feature、问问题 | [GitHub Issues](https://github.com/shiahonb777/web-to-app/issues) | 几分钟 |
| 修 Bug 或在 Android 客户端里做新功能 | 见下方代码贡献小节 | 几天 |

不确定走哪条，先开 issue 或 discussion——动手前对齐方向，比写完再返工便宜
得多。

### 模块市场贡献

让你的工作触达每一个用户最快的方式就是提一个模块。Schema、审核 Checklist 和
PR 模板都在 [`modules/README.md`](modules/README.md)。简化流程：

1. Fork 本仓库
2. 新建 `modules/<你的模块>/module.json` 和 `main.js`（需要 CSS 时再加
   `style.css`）
3. 在 `modules/registry.json` 里加一行索引
4. 提 PR

市场**没有后端**。CI 在每次推送 `main` 时重新生成 `modules/submissions.json`，
客户端只展示同时出现在 `registry.json` 和 `submissions.json` 里的模块——以此
保证只列出 PR 已真正合并的模块。合并后所有客户端在下次刷新（默认 1 小时缓存）
就能看到。

模块改动由 CI 的 `tools/ci/validate_modules.py` 校验（见
`.github/workflows/modules-check.yml`），提 PR 前可本地先跑：

```bash
python3 tools/ci/validate_modules.py
```

### 代码贡献

**动手前**

- 在 [issues](https://github.com/shiahonb777/web-to-app/issues) 里先搜一下
  类似讨论
- 较大的改动请先开 issue 说明要解决的问题和方案
- **谨慎引入新依赖**。`app/build.gradle.kts` 的依赖列表刻意保持精简——本项目
  全程在设备内签名打包 APK，并特意把 `targetSdk` 锁在 28（理由见该文件里的
  注释）。新依赖需要充分理由。

**本地环境**

- Android Studio Hedgehog 或更新版本
- JDK 17
- Gradle wrapper 已锁定 Gradle 9.4.1，无需系统安装 Gradle

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

仓库有两个模块：**`app`**（完整打包器）和 **`shell`**（嵌入生成 APK 的纯运行
时宿主）。改动共享的运行时代码通常两个模块都要能编译。

提交前请跑通：

```bash
./gradlew :app:compileDebugKotlin
./gradlew :shell:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

> 原生代码（`node_launcher`、`go_exec_loader`、APK 优化器）按 ABI 经 CMake
> 编译，需要通过 SDK Manager 安装 Android NDK + CMake。CI 安装的是
> `cmake;3.22.1` 与 `ndk;28.2.13676358`。

**代码大致位置**

| 区域 | 路径 |
| --- | --- |
| 应用类型与核心配置（`AppType`、`WebApp`） | `app/src/main/java/com/webtoapp/data/model/` |
| 设备端 APK 打包 / 签名 | `app/src/main/java/com/webtoapp/core/apkbuilder/` |
| 服务端运行时（Node / PHP / Python / Go / WordPress） | `app/src/main/java/com/webtoapp/core/{nodejs,php,python,golang,wordpress}/` |
| WebView 引擎、原生桥、指纹伪装 | `app/src/main/java/com/webtoapp/core/{webview,engine,appearance}/` |
| 扩展模块、模块市场、AI 编程 | `app/src/main/java/com/webtoapp/core/{extension,market,aicoding}/` |
| Compose UI 与设计系统 | `app/src/main/java/com/webtoapp/ui/` |
| DI 依赖图（单一事实来源） | `app/src/main/java/com/webtoapp/di/AppModule.kt` |

**代码风格**

- **本仓库不写注释。** 源码按约定保持无注释——用清晰的命名和小函数表达意图。
  不要加 `//`、`/* */` 或 KDoc；如果某段代码非得靠注释才能看懂，优先重写它。
  （代码里字符串内的内容，比如 URL，当然不受此限。）
- **新 UI 一律构建在 Wta 设计系统之上**——所有界面通过 `com.webtoapp.ui.design`
  渲染。旧的 `Premium*` / `Enhanced*` / `Settings*` 组件作为 Wta 内部实现的
  永久别名层保留——不要再新增，也无需强行替换。构建期有一个审计脚本
  （`tools/audit_ui_design_system.py`，接进 `build.gradle.kts`）按
  `tools/ui_design_allowlist.txt` 跟踪历史 UI 债务。
- 复用 `ui/design/WtaTokens.kt` 里的设计 token（间距、圆角、透明度、高度），
  别硬编码数字
- **字符串三语**：新字符串要在 `core/i18n/Strings.kt` 的中、英、阿拉伯三个
  分支都补上；不会翻就三处都填英文并在 PR 里标注。有翻译一致性单测在守这点
- 引入新的全局单例前请先讨论；`di/AppModule.kt` 是单一事实来源
- **原生桥是按能力门禁的**：任何通过 `@JavascriptInterface` 暴露给网页的方法，
  都必须经过逐能力白名单（`NativeBridgeCapabilities`）。绝不要在没有门禁的
  情况下把原生能力暴露给任意页面。绕过门禁的 PR 会被拒
- 不要 catch 然后吞掉异常；用 `AppLogger` 记录后重新抛出或返回失败的 `Result`

**安全**

动到 WebView、文件 IO、APK 签名或原生桥的改动会影响**每一个生成出来的
应用**，因此审核更严。出现以下情况的 PR 会被拒：

- 把凭据 / 密钥存储或暴露给网页内容
- 在没有能力门禁的情况下扩大暴露给任意页面的原生面
- 削弱 APK 签名、隔离或指纹伪装的默认强度
- 在一个大 diff 里夹带不相关的改动

**Commit 消息**

不强制 Conventional Commits，但请把"为什么"写进正文。示例见英文段。

**Pull Request**

- 从 `main` 分出分支，每个 PR 只解决一件事
- PR 描述写"用户看得到的效果"，不只是代码 diff
- 改动涉及构建系统、原生代码或 APK 打包时，附上 `./gradlew :app:assembleDebug`
  的结果
- CI 必须绿色才会合并

**Review 标准**

- **正确性**——是否真的做了 PR 描述里写的事
- **安全**——见上面《安全》小节
- **聚焦**——顺手做的重构请单独发 PR
- **风格**——参考上面那条

### 行为准则

反馈须精准。"这代码是坨屎"仅在同时说明"哪段代码、为何如此、怎样才不至于"
时方予受理。维护者(**shiaho**)已就针对其本人、其判断力及其人生选择的批评
出具长期豁免一份并存档。未附可用补丁的赞美,予以记录,余不受理。
