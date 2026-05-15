# Contributing to WebToApp

Thanks for taking the time to help. WebToApp grows fastest when contributors
focus on small, well-scoped changes — pick one of the lanes below and ignore
the rest.

> **English** · [简体中文](#贡献-webtoapp)

---

## Lanes

| You want to… | Go to | Effort |
| --- | --- | --- |
| Publish a JS/CSS module to the in-app **Module Market** | [`modules/README.md`](modules/README.md) | hours |
| File a bug, request a feature, or ask a question | [GitHub Issues](https://github.com/shiahonb777/web-to-app/issues) | minutes |
| Fix a bug or build a feature in the Android client | This guide ↓ | days |

If you're not sure which lane fits, open a discussion or an issue first — no
need to write code before there's agreement on the shape of the change.

---

## Module Market submissions

The fastest way to ship something useful to every WebToApp user is to publish
a module. The full schema, reviewer checklist, and an example PR template live
at [`modules/README.md`](modules/README.md). The short version:

1. Fork the repo.
2. Add `modules/<your-module>/module.json` and `main.js` (plus `style.css` if
   you need CSS).
3. Add an entry to `modules/registry.json`.
4. Open a PR. We review for safety and merge.

Once merged, every client picks up the new module on its next market refresh
(default cache is one hour).

---

## Code contributions

### Before you write code

- Search [issues](https://github.com/shiahonb777/web-to-app/issues) for prior
  discussion of the same idea.
- For non-trivial changes, open an issue first describing the problem you're
  solving and the approach you have in mind. This is much cheaper than
  rewriting after a review.
- Avoid adding new dependencies. The dependency list in
  `app/build.gradle.kts` is intentionally restrained.

### Local setup

You'll need:

- Android Studio Hedgehog or newer
- JDK 17
- Gradle 8.14+ (the wrapper handles this)

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

Run the project's checks before submitting:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:test
```

### Coding conventions

WebToApp leans on Kotlin idioms and Jetpack Compose patterns already in the
codebase. A few rules worth calling out:

- **Match the surrounding style.** The project mixes a few generations of UI
  components (`Premium*`, `Wta*`). When you touch a screen, use whatever's
  already used there rather than reaching for something else.
- **Reuse the design tokens** in `ui/design/WtaTokens.kt` for spacing, radius,
  alpha, and elevation. Don't hard-code numbers.
- **Strings live in `core/i18n/Strings.kt`.** Add the new string to all three
  language branches (Chinese, English, Arabic). If you can't translate, add
  the English text to all three and flag it in the PR.
- **No new top-level singletons** unless you discuss it first. The DI graph
  in `di/AppModule.kt` is the source of truth.
- **Avoid catching `Exception` to silence errors.** If recovery is impossible,
  log via `AppLogger` and re-throw or return a failed `Result`.

### Commit messages

We don't enforce Conventional Commits, but a clear subject line and a wrapped
body that explains *why* the change exists make reviews much faster. Example:

```
ModuleMarket: cache registry.json for an hour

Repeatedly hitting raw.githubusercontent.com on every screen open is wasteful
and triggers GitHub's anonymous rate limit on slow connections. Cache the
parsed registry under cache/module_market/ and treat anything fresher than
an hour as authoritative; refresh button always bypasses the cache.
```

### Pull requests

- Branch from `main`. Keep PRs focused — one logical change per PR.
- Describe the user-visible effect in the PR body, not just the code change.
- If your PR touches the build system, native code, or APK packaging, please
  attach the output of `./gradlew :app:assembleDebug` (or note the failure if
  it fails on your machine).
- CI runs on every PR. A green CI is required before merge.

### Reviewer expectations

Maintainer reviews look for:

- **Correctness.** Does the change do what the PR description says?
- **Safety.** Anything touching the WebView, file IO, or APK signing gets
  extra scrutiny — these have downstream impact on every generated app.
- **Scope discipline.** Drive-by refactors expand the review surface. Land
  them in a separate PR.
- **Style fit.** See *Coding conventions* above.

---

## Code of conduct

Be respectful. Disagree about code, not about people. If a discussion stops
being productive, take a break and come back later.

---

<a id="贡献-webtoapp"></a>
## 贡献 WebToApp（中文）

非常感谢你愿意花时间。WebToApp 的迭代速度取决于"小而聚焦"的贡献——下面三条路
里挑一条走，其他的可以先忽略。

### 你想做什么？

| 你想…… | 路径 | 投入 |
| --- | --- | --- |
| 给应用内的 **模块市场** 提交一个 JS/CSS 模块 | [`modules/README.md`](modules/README.md) | 几小时 |
| 报 Bug、提 Feature、问问题 | [GitHub Issues](https://github.com/shiahonb777/web-to-app/issues) | 几分钟 |
| 修 Bug 或在 Android 客户端里做新功能 | 见下方代码贡献小节 | 几天 |

如果不确定走哪条，先开个 issue 或 discussion——在还没动手前对齐方向比写完再
返工便宜得多。

### 模块市场贡献

让你的工作触达每一个 WebToApp 用户最快的方式就是给市场提一个模块。Schema、
审核 Checklist 和 PR 模板都在 [`modules/README.md`](modules/README.md)。简化
版流程：

1. Fork 本仓库
2. 新建 `modules/<你的模块>/module.json` 和 `main.js`（需要 CSS 时再加
   `style.css`）
3. 在 `modules/registry.json` 里加一行索引
4. 提 PR，等待审核合并

合并后所有客户端在下次刷新（默认 1 小时缓存）就能看到。

### 代码贡献

**动手前**

- 在 [issues](https://github.com/shiahonb777/web-to-app/issues) 里先搜索一下
  类似讨论
- 较大的改动请先开 issue 说明你打算解决的问题和方案
- 谨慎引入新依赖，`app/build.gradle.kts` 的依赖列表是刻意保持精简的

**本地环境**

- Android Studio Hedgehog 或更新版本
- JDK 17
- Gradle 8.14+（项目自带 wrapper）

```bash
git clone https://github.com/shiahonb777/web-to-app.git
cd web-to-app
./gradlew assembleDebug
```

提交前请跑通：

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:test
```

**代码风格**

- 跟随你修改的文件周围的现有风格——项目里 `Premium*` 和 `Wta*` 两套组件并存，
  在哪个屏幕里就用哪一套
- 复用 `ui/design/WtaTokens.kt` 里的设计 token，别硬编码数字
- 字符串统一加到 `core/i18n/Strings.kt`，三种语言（中、英、阿拉伯）都要补
- 引入新的全局单例前请先讨论
- 不要 catch 然后吞掉异常；用 `AppLogger` 记录后重新抛出或返回失败的 `Result`

**Commit 消息**

不强制 Conventional Commits，但请把"为什么"写在正文里。示例见英文段。

**Pull Request**

- 从 `main` 分出分支，每个 PR 只解决一件事
- PR 描述写"用户看得到的效果"，不只是代码 diff
- 改动涉及构建系统、原生代码或 APK 打包时，附上
  `./gradlew :app:assembleDebug` 的结果
- CI 必须绿色才会合并

**Review 标准**

审核会重点看：

- **正确性**——是否真的做了 PR 描述里写的事
- **安全**——动到 WebView、文件 IO、APK 签名的改动会更严格
- **聚焦**——顺手做的重构请单独发 PR
- **风格**——参考上面那条

### 行为准则

请保持尊重。可以批评代码，不要批评人。讨论失控时先停一下再回来。
