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
├── README.md                  ← this file
└── <module-path>/             ← one folder per module
    ├── module.json            ← module manifest (required)
    ├── main.js                ← module source (required)
    └── style.css              ← optional CSS, auto-injected on load
```

When a user opens the market, the app fetches `registry.json` and renders
each entry. Tapping **Install** then downloads `<module-path>/module.json`
and `main.js` (plus `style.css` when `hasCss` is `true`) and hands the
result to the local extension manager. The registry is cached for one hour;
the refresh button bypasses the cache.

> **Heads-up:** the `icon` field in both `registry.json` and `module.json`
> is a [Material Icons](https://fonts.google.com/icons) string name (e.g.
> `"auto_awesome"`, `"dark_mode"`). The app does **not** download a separate
> icon file — bundling an `icon.png` will not affect the listing.

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
   [reviewer checklist](#reviewer-checklist) and merges.
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
  "hasCss": false
}
```

`minAppVersion` lets you ship a module that needs APIs only present from a
specific WebToApp `versionCode` onwards — older clients hide the entry.
The current `versionCode` is **33** (`v1.9.6`); set this only if you
genuinely depend on a newer build.

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
├── README.md                  ← 本文件
└── <模块目录>/
    ├── module.json            ← 模块清单（必需）
    ├── main.js                ← 模块代码（必需）
    └── style.css              ← 可选 CSS，加载时自动注入
```

App 打开市场时拉 `registry.json` 渲染列表；点击安装才拉对应模块的
`module.json` 和 `main.js`（外加 `style.css`，如果 `hasCss` 是 `true`），
然后交给本地的扩展管理器。registry 缓存 1 小时，刷新按钮可绕开缓存。

> **注意：** `registry.json` 和 `module.json` 里的 `icon` 字段是
> [Material Icons](https://fonts.google.com/icons) 的名字（如
> `"auto_awesome"`、`"dark_mode"`），App **不会**单独下载图标文件，放
> `icon.png` 进去也没用。

### 提交流程

1. Fork [`shiahonb777/web-to-app`](https://github.com/shiahonb777/web-to-app)
2. 在 `modules/` 下新建一个唯一的 kebab-case 目录，比如
   `modules/dark-reader-lite/`。文件夹名就是 `registry.json` 里的 `path`
3. 至少包含：
   - `module.json` — 清单（schema 见英文段）
   - `main.js` — 在 WebView 中执行的代码
4. 在 [`registry.json`](registry.json) 中加一条对应记录，保持 `id`、`name`、
   `version`、`runAt`、`permissions` 在两个文件之间一致
5. 提 PR；维护者按 [审核 Checklist](#审核-checklist) 审查并合并
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
- [ ] 不要塞 `icon.png` 或其他冗余文件（运行时会忽略它们，徒增仓库体积）
