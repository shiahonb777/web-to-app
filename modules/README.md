# Module Market

This directory **is** the WebToApp Module Market. Every JS/CSS module the
in-app market shows is fetched directly from this folder over
`raw.githubusercontent.com`, with `cdn.jsdelivr.net/gh/` as a CDN fallback.
There is no other backend. A merged PR is published the moment it lands on
`main`.

> **English** · [简体中文](#中文)

---

## At a glance

```
modules/
├── registry.json              ← index file the app downloads first
├── submissions.json           ← CI-generated PR / submitter metadata
├── README.md                  ← this file
└── <module-path>/             ← one folder per module
    ├── module.json            ← module manifest (required)
    ├── main.js                ← module source (required)
    ├── style.css              ← optional CSS, auto-injected on load
    └── icon.png               ← optional 256 KB-max icon (also .svg/.webp/.jpg)
```

When a user opens the market, the app fetches both `registry.json` and
`submissions.json`, then renders each entry that appears in **both** —
that's how we guarantee the catalog only shows modules whose PR has
actually been merged. Tapping **Install** then downloads
`<module-path>/module.json` and `main.js` (plus `style.css` when `hasCss`
is `true`) and hands the result to the local extension manager. The
registry is cached for one hour; the refresh button bypasses the cache.

> **Heads-up:** `registry.json` and `module.json` both have an `icon`
> field that takes a [Material Icons](https://fonts.google.com/icons) name
> (e.g. `"auto_awesome"`, `"dark_mode"`). For a real branded picture, set
> the registry-level **`iconUrl`** to either a relative path (`"icon.png"`)
> or an absolute `https://` URL. The relative form must reference one of
> `icon.png` / `icon.svg` / `icon.webp` / `icon.jpg` / `icon.jpeg` next to
> `main.js` and stay under 256 KB — larger or differently-named files will
> fail CI validation. When neither field is set, the app shows a
> first-letter avatar instead.

---

## Submitting a module

1. **Fork** [`shiahonb777/web-to-app`](https://github.com/shiahonb777/web-to-app).
2. Create a unique kebab-case folder under `modules/`, e.g.
   `modules/dark-reader-lite/`. The folder name is your `path` in
   `registry.json`.
3. Add at minimum:
   - `module.json` — manifest, see [schema](#modulejson-schema)
   - `main.js` — code that runs in the WebView
4. Add a matching entry to [`registry.json`](registry.json). Keep `id`,
   `name`, `version`, `runAt`, and `permissions` consistent between both
   files (the registry is the listing surface, the manifest is what gets
   installed).
5. Open a pull request. A maintainer reviews using the
   [reviewer checklist](#reviewer-checklist) and merges. CI validates the
   catalog automatically — see [Local validation](#local-validation) below
   if you want to self-check first.
6. Once merged, every client picks up the new module on its next refresh.

There is no separate developer account, no API key, no submission portal.

---

## `module.json` schema

```json
{
  "id": "globally-unique-id",
  "name": "Display Name",
  "description": "Paragraph shown on the install page.",
  "icon": "material-icon-name",
  "category": "OTHER",
  "tags": ["tag1", "tag2"],
  "version": {
    "code": 1,
    "name": "1.0.0",
    "changelog": "Initial release"
  },
  "author": {
    "name": "Your Name",
    "url": "https://github.com/your-handle",
    "email": "optional@example.com"
  },
  "runAt": "DOCUMENT_END",
  "urlMatches": [
    { "pattern": "*", "isRegex": false, "exclude": false }
  ],
  "permissions": ["DOM_ACCESS"],
  "configItems": [
    {
      "key": "greeting",
      "name": "Greeting text",
      "description": "Shown in the floating banner.",
      "type": "TEXT",
      "defaultValue": "Hello, WebToApp!",
      "required": false
    }
  ]
}
```

Note that `version` here is an object with `code` (monotonically increasing
integer), `name` (semver string), and `changelog`. In `registry.json` the
matching field is just the semver string — same number, different shape.

### Allowed `category` values

`CONTENT_FILTER`, `CONTENT_ENHANCE`, `STYLE_MODIFIER`, `THEME`,
`FUNCTION_ENHANCE`, `AUTOMATION`, `NAVIGATION`, `DATA_EXTRACT`, `DATA_SAVE`,
`INTERACTION`, `ACCESSIBILITY`, `MEDIA`, `VIDEO`, `IMAGE`, `AUDIO`, `SECURITY`,
`ANTI_TRACKING`, `SOCIAL`, `SHOPPING`, `READING`, `TRANSLATE`, `DEVELOPER`,
`OTHER`.

Unknown values fall back to `OTHER` — it does not break the install, just
hides the module from the category filter chips.

### Allowed `runAt` values

`DOCUMENT_START`, `DOCUMENT_END`, `DOCUMENT_IDLE`, `CONTEXT_MENU`,
`BEFORE_UNLOAD`. Defaults to `DOCUMENT_END` if omitted.

### Allowed `permissions` values

The list is informational on the install screen; the runtime does not
sandbox by it. Reviewers use it to spot dangerous capabilities.

`DOM_ACCESS`, `DOM_OBSERVE`, `CSS_INJECT`, `STORAGE`, `COOKIE`, `INDEXED_DB`,
`CACHE`, `NETWORK`, `WEBSOCKET`, `FETCH_INTERCEPT`, `CLIPBOARD`,
`NOTIFICATION`, `ALERT`, `KEYBOARD`, `MOUSE`, `TOUCH`, `LOCATION`, `CAMERA`,
`MICROPHONE`, `DEVICE_INFO`, `MEDIA`, `FULLSCREEN`, `PICTURE_IN_PICTURE`,
`SCREEN_CAPTURE`, `DOWNLOAD`, `FILE_ACCESS`, `EVAL`, `IFRAME`, `WINDOW_OPEN`,
`HISTORY`, `NAVIGATION`.

The dangerous ones (`COOKIE`, `INDEXED_DB`, `NETWORK`, `WEBSOCKET`,
`FETCH_INTERCEPT`, `CLIPBOARD`, `LOCATION`, `CAMERA`, `MICROPHONE`,
`SCREEN_CAPTURE`, `FILE_ACCESS`, `EVAL`, `IFRAME`) get extra scrutiny on
review.

### Allowed `configItems[].type` values

`TEXT`, `TEXTAREA`, `NUMBER`, `BOOLEAN`, `SELECT`, `MULTI_SELECT`, `RADIO`,
`CHECKBOX`, `COLOR`, `URL`, `EMAIL`, `PASSWORD`, `REGEX`, `CSS_SELECTOR`,
`JAVASCRIPT`, `JSON`, `RANGE`, `DATE`, `TIME`, `DATETIME`, `FILE`, `IMAGE`.

### `urlMatches` patterns

Two pattern flavours:

- **`isRegex: false`** (recommended): a Chrome-extension-style glob.
  - `*` matches any number of characters
  - `*://...` expands to `(https?|ftp|file)://`
  - `<all_urls>` and `*` both mean "every URL"
- **`isRegex: true`**: a Java-style regex evaluated with a 200ms timeout
  per URL match.

Set `exclude: true` to make a rule subtract from the match set. If only
exclude rules are present, the module matches every other URL.

---

## `registry.json` entry schema

The registry is the index the app fetches first to render the list. Each
entry mirrors the module manifest plus a `path` (folder name) and a
`hasCss` flag.

```json
{
  "id": "globally-unique-id",
  "path": "folder-name-under-modules",
  "name": "Display Name",
  "description": "One-line summary",
  "icon": "material-icon-name",
  "category": "OTHER",
  "tags": ["tag1", "tag2"],
  "version": "1.0.0",
  "minAppVersion": 33,
  "author": {
    "name": "Your Name",
    "url": "https://github.com/your-handle"
  },
  "runAt": "DOCUMENT_END",
  "permissions": ["DOM_ACCESS", "CSS_INJECT"],
  "urlMatches": [
    { "pattern": "*", "isRegex": false, "exclude": false }
  ],
  "hasCss": false,
  "iconUrl": "icon.png"
}
```

`minAppVersion` lets you ship a module that needs APIs only present from a
specific WebToApp `versionCode` onwards — older clients hide the entry.
The current `versionCode` is **39** (`v2.0.4`); set this only if you
genuinely depend on a newer build.

`iconUrl` is optional. Either a relative path (`"icon.png"`, `"icon.svg"`,
`"icon.webp"`, `"icon.jpg"`, `"icon.jpeg"`) referencing a file in your
module folder, or an absolute `https://` URL pointing at off-repo hosting.
Relative icons must stay under 256 KB. When `iconUrl` is absent, the app
shows a circular avatar with the module's first letter.

---

## Who shows up in the catalog (`submissions.json`)

`submissions.json` is generated by the
[`Module Market Publish`](../.github/workflows/modules-publish.yml) workflow
on every push to `main`. It contains one entry per module that has actually
landed on `main`, with PR number, merge timestamp, and the GitHub identity
of whoever opened the PR.

**The in-app market only shows modules that appear in `submissions.json`.**
That's the entire mechanism behind "only merged PRs are visible" — there is
no allow-listing inside the app, no client-side filter to bypass; if your
module isn't in this file, users won't see it.

The publish workflow:

1. On push to `main` that touches `modules/`, walks every folder.
2. For each folder, finds the introducing commit via
   `git log --diff-filter=A`.
3. Asks `GET /repos/{owner}/{repo}/commits/{sha}/pulls` whether that
   commit was part of a merged PR. If yes → records PR number, URL,
   `merged_at`, and the PR author's GitHub login + avatar.
4. Otherwise treats it as a maintainer direct-push and records the
   commit author — but only when the GitHub login matches the
   `MAINTAINERS` allow-list in the workflow. Anything else is left out
   on purpose.
5. Commits the regenerated file back to `main`.

You don't have to do anything as a contributor — just merge a PR and wait
a few seconds for the publish workflow to run.

---

## How `main.js` runs

The runtime wraps your code in an IIFE before injection, with these
globals available:

| Global | Value |
| --- | --- |
| `__MODULE_INFO__` | `{ id, name, icon, version, uiConfig, runMode }` |
| `__MODULE_CONFIG__` | object of `{ key: value }` from saved config |
| `__MODULE_UI_CONFIG__` | the UI panel config (mirrors `__MODULE_INFO__.uiConfig`) |
| `__MODULE_RUN_MODE__` | `'INTERACTIVE'` or `'AUTO'` |
| `getConfig(key, defaultValue)` | convenience accessor for `__MODULE_CONFIG__` |

Your code is also wrapped in a `try/catch` — uncaught errors are logged to
`console.error` with the module name as a prefix and do **not** abort the
page. So you do not need to wrap everything yourself, but you do need to
think about silent failures.

If you ship `style.css` (and set `hasCss: true` in `registry.json`), it is
injected as a `<style>` tag with `id="ext-module-<module-id>"` immediately
before your code runs.

A minimal hello-world:

```js
(function () {
  var greeting = getConfig('greeting', 'Hello!');
  var banner = document.createElement('div');
  banner.textContent = greeting;
  banner.style.cssText = 'position:fixed;top:24px;left:50%;' +
    'transform:translateX(-50%);padding:10px 16px;background:#111;' +
    'color:#fff;border-radius:12px;z-index:2147483647;';
  document.body.appendChild(banner);
  setTimeout(function () { banner.remove(); }, 3000);
})();
```

See [`hello-world/main.js`](hello-world/main.js) and
[`night-shift/main.js`](night-shift/main.js) for fuller examples.

### Optional: register a panel button

If your module ships an interactive UI, register it with the floating
panel so the user can summon it:

```js
__WTA_MODULE_UI__.register({
  id: __MODULE_INFO__.id,
  name: __MODULE_INFO__.name,
  icon: __MODULE_INFO__.icon,
  uiConfig: __MODULE_UI_CONFIG__,
  runMode: __MODULE_RUN_MODE__,
  onClick: function () {
    // open your UI
  }
});
```

If you skip this and your module has any `configItems`, the runtime will
auto-register a default entry so the user can still open module settings.

---

## Versioning

Bump `version.code` and `version.name` in `module.json` **and** `version`
in `registry.json` together when you publish an update. The client
compares semver against the locally installed copy and offers a one-tap
upgrade.

User config is preserved across updates: keys still declared in the new
manifest's `configItems` keep their values, keys you removed get cleaned
up. So renaming a config key resets it to its default — bump the version
and document the change in `version.changelog`.

---

## Local validation

The repo ships a Python validator that mirrors the install-time checks the
app performs, plus a few correctness rules CI uses to gate PRs:

```
python3 tools/ci/validate_modules.py
```

It uses only the standard library, so no `pip install` step is needed.
The same script runs in [GitHub Actions](../.github/workflows/modules-check.yml)
on every PR that touches `modules/`. A green CI run is required before a
maintainer can merge.

The validator catches:

- Invalid JSON in `registry.json` or any `module.json`
- Missing required fields (`id`, `name`, `version`)
- Unknown values for `category`, `runAt`, `permissions`,
  `configItems[].type`
- Cross-file mismatches between `registry.json` and `module.json`
  (`id`, `name`, `version`, `runAt`, missing permissions)
- Folder names that aren't kebab-case
- Modules folder ↔ registry entry mismatches (orphan folders / ghost
  registry entries)
- Duplicate `id`s or `path`s
- Missing required files (`module.json`, `main.js`)
- Stray files (warns about `icon.png`, extra dirs, etc.)
- `hasCss` flag disagreeing with whether `style.css` is present on disk
- `iconUrl` referencing a missing or oversized file in the module folder
- Top-level `return` in `main.js` (would break inside the IIFE wrapper)
- `getConfig(...)` calls without matching `configItems`

---

## Reviewer checklist

For maintainers — this is what gets checked before a merge.

- [ ] Folder name and `id` are unique and kebab-case
- [ ] `module.json` and `registry.json` agree on `id`, `name`, `version`,
      `runAt`, `permissions`, and `urlMatches`
- [ ] `main.js` is readable; no obfuscation without a source link in the PR
- [ ] No unconditional network calls to third-party endpoints
- [ ] No reading of `document.cookie` or auth tokens unless declared in
      `permissions`
- [ ] `urlMatches` is appropriately scoped (avoid `*` for invasive modules)
- [ ] `main.js` runs cleanly inside the IIFE wrapper (no top-level `return`)
- [ ] `runAt` matches what the code expects — `DOCUMENT_START` modules must
      not assume `document.body` exists
- [ ] `version.code` was bumped if `version.name` changed
- [ ] `hasCss` is `true` if and only if `style.css` is present in the folder
- [ ] If `iconUrl` is set, the file exists, is under 256 KB, and uses one
      of the allowed extensions (`png`, `svg`, `webp`, `jpg`, `jpeg`)
- [ ] No `icon.png` or other extra files (the runtime ignores them — they
      just bloat the repo)

---

<a id="中文"></a>
## 中文

这个目录就是 WebToApp 的**模块市场**。所有用户在 App 里看到的市场模块都是直
接从这个文件夹通过 `raw.githubusercontent.com` 拉取的，CDN 兜底是
`cdn.jsdelivr.net/gh/`。**没有其他后端**。PR 一旦合并到 `main`，下一刻就上
线。

### 目录结构

```
modules/
├── registry.json              ← App 首次下载的索引
├── submissions.json           ← CI 生成：PR / 提交者元数据
├── README.md                  ← 本文件
└── <模块目录>/
    ├── module.json            ← 模块清单（必需）
    ├── main.js                ← 模块代码（必需）
    ├── style.css              ← 可选 CSS，加载时自动注入
    └── icon.png               ← 可选图标，最大 256 KB（也支持 .svg/.webp/.jpg）
```

App 打开市场时同时拉 `registry.json` 和 `submissions.json`，**只渲染两边都
出现的条目**——这是"只展示已合并 PR" 这条承诺的实现机制。点击安装时才拉
对应模块的 `module.json` 和 `main.js`（外加 `style.css`，如果 `hasCss` 是
`true`），然后交给本地的扩展管理器。registry 缓存 1 小时，刷新按钮可绕开
缓存。

> **图标：** `registry.json` 里的 `iconUrl` 字段可以指向模块目录里的图片
> （`"icon.png"`、`"icon.svg"`、`"icon.webp"`、`"icon.jpg"`、
> `"icon.jpeg"` 之一，最大 256 KB），也可以是绝对 `https://` URL。如果
> 不填，App 会用模块名首字母自动生成圆形头像。`icon`/`module.json::icon`
> 是 [Material Icons](https://fonts.google.com/icons) 的字符串名，
> 当前主要用于本地内置模块。

### `submissions.json`：决定谁出现在市场

`submissions.json` 由
[`Module Market Publish`](../.github/workflows/modules-publish.yml) 工作
流在每次 `main` 分支推送时自动生成，每条对应一个真正落地到 `main` 的模块，
带 PR 编号、合并时间、PR 作者的 GitHub 身份。

**App 端的市场只会展示出现在这个文件里的模块**——这是"只展示已合并 PR"的
全部机制：客户端没有别的过滤，模块如果不在这里，用户根本看不到。

工作流逻辑：

1. 推送到 `main` 且改了 `modules/`，遍历所有模块目录。
2. 用 `git log --diff-filter=A` 找出每个目录的引入提交。
3. 调用 `GET /repos/{owner}/{repo}/commits/{sha}/pulls` 看这个提交是不是
   某个已合并 PR 的一部分。是的话记录 PR 编号、URL、`merged_at`、PR 作者
   的 GitHub login + 头像。
4. 否则当作维护者直推处理，记录提交作者——但**只有当 GitHub login 在
   workflow 的 `MAINTAINERS` 白名单里**才会被记录。其他直推故意不进。
5. 把生成的文件 commit 回 `main`。

作为贡献者你不用做任何事——PR 合并几秒钟之后这个工作流自动跑完，下次有
人打开市场就能看到你的模块。

### 提交流程

1. Fork [`shiahonb777/web-to-app`](https://github.com/shiahonb777/web-to-app)
2. 在 `modules/` 下新建一个唯一的 kebab-case 目录，比如
   `modules/dark-reader-lite/`。文件夹名就是 `registry.json` 里的 `path`
3. 至少包含：
   - `module.json` — 清单（schema 见英文段）
   - `main.js` — 在 WebView 中执行的代码
4. 在 [`registry.json`](registry.json) 中加一条对应记录，保持 `id`、`name`、
   `version`、`runAt`、`permissions` 在两个文件之间一致
5. 提 PR；维护者按 [审核 Checklist](#审核-checklist) 审查并合并。CI 会自动
   校验 modules/ 目录——见下方 [本地校验](#本地校验)，可以提交前先自查
6. 合并后所有客户端在下次刷新（默认 1 小时缓存）即可看到

### 字段差异

`module.json` 里的 `version` 是对象（`code` 整数、`name` 字符串、
`changelog`），`registry.json` 里的 `version` 直接是 `name` 那个字符串。
版本号要保持一致。

`category`、`runAt`、`permissions`、`configItems[].type` 的允许值与英文段
完全相同。

### `urlMatches` 模式

- `isRegex: false`（推荐）：Chrome 扩展风格的 glob
  - `*` 匹配任意字符
  - `*://...` 展开为 `(https?|ftp|file)://`
  - `<all_urls>` 和 `*` 都表示"匹配所有 URL"
- `isRegex: true`：Java 正则，每次匹配有 200ms 超时

设 `exclude: true` 让一条规则从结果集中扣除。如果只有 exclude 规则，模块匹
配除此之外的所有 URL。

### `main.js` 注入合约

代码会被包在 IIFE 里执行，注入的全局变量：

| 全局 | 值 |
| --- | --- |
| `__MODULE_INFO__` | `{ id, name, icon, version, uiConfig, runMode }` |
| `__MODULE_CONFIG__` | 用户保存的 `{ key: value }` 配置 |
| `__MODULE_UI_CONFIG__` | 面板 UI 配置 |
| `__MODULE_RUN_MODE__` | `'INTERACTIVE'` 或 `'AUTO'` |
| `getConfig(key, defaultValue)` | `__MODULE_CONFIG__` 的便捷访问器 |

代码外层还有一层 `try/catch` —— 抛出的异常会以模块名为前缀写到
`console.error`，不会中断页面其他注入。所以你不必自己再包一层 `try/catch`，
但要意识到错误是静默吞掉的。

如果带 `style.css`（同时把 `registry.json` 里的 `hasCss` 设为 `true`），
它会以 `id="ext-module-<模块id>"` 的 `<style>` 标签在你代码运行之前注入。

如果模块带了 `configItems`、但你没调用 `__WTA_MODULE_UI__.register({...})`
注册一个面板按钮，运行时会自动给用户加一个入口，至少能让用户打开模块设置。

### 版本升级

发新版时**同时**升 `module.json` 的 `version.code`/`version.name` 和
`registry.json` 的 `version`。客户端会用 semver 比较版本提示更新。

更新会保留用户配置：新清单 `configItems` 里仍然存在的 key，值会被保留；移
除的 key 会被自动清掉。所以重命名 config key 等于重置它——把更新写进
`version.changelog`。

### 本地校验

仓库里有一份 Python 校验脚本，模拟 App 安装时做的检查 + CI 用来卡 PR 的几条
规则：

```
python3 tools/ci/validate_modules.py
```

只用了标准库，无需 `pip install`。同样的脚本会在
[GitHub Actions](../.github/workflows/modules-check.yml) 里跑——任何动到
`modules/` 的 PR 都必须 CI 通过才能合并。

校验内容：

- `registry.json` 或任何 `module.json` 的 JSON 解析错误
- 必填字段缺失（`id`、`name`、`version`）
- `category` / `runAt` / `permissions` / `configItems[].type` 的非法枚举值
- `registry.json` 和 `module.json` 之间的字段不一致（`id`、`name`、
  `version`、`runAt`、漏报权限）
- 文件夹名不是 kebab-case
- modules 目录与 registry 条目不对齐（孤立目录 / 鬼条目）
- 重复的 `id` 或 `path`
- 缺少必需文件（`module.json`、`main.js`）
- 多余文件（`icon.png`、子目录等会给 warning）
- `hasCss` 标志和 `style.css` 文件存在与否不匹配
- `iconUrl` 引用了不存在或超过 256 KB 的图片
- `main.js` 顶层 `return`（在 IIFE 包裹里会变成语法错误）
- `getConfig(...)` 调用但 `configItems` 没声明对应字段

### 审核 Checklist

- [ ] 目录名和 `id` 唯一且为 kebab-case
- [ ] `module.json` 和 `registry.json` 的 `id`/`name`/`version`/`runAt`/
      `permissions`/`urlMatches` 一致
- [ ] `main.js` 可读；没有特殊说明的话不接受混淆/压缩代码
- [ ] 没有无条件调用第三方网络
- [ ] 没有未在 `permissions` 中声明就读取 `document.cookie` 或鉴权 token
- [ ] `urlMatches` 范围合理（侵入性强的模块不要无脑写 `*`）
- [ ] 代码能在 IIFE 包裹下正常执行（不要写顶层 `return`）
- [ ] `runAt` 与代码预期一致（`DOCUMENT_START` 不能假设 `document.body`
      已存在）
- [ ] `version.name` 变了的话 `version.code` 也要随之 +1
- [ ] `hasCss` 当且仅当目录里有 `style.css` 时才为 `true`
- [ ] 如果设置了 `iconUrl`，对应文件存在、不超过 256 KB，扩展名是
      `png`/`svg`/`webp`/`jpg`/`jpeg` 之一
- [ ] 不要塞 `icon.png` 或其他冗余文件（运行时会忽略它们，徒增仓库体积）
