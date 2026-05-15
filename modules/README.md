# Module Market

This directory **is** the WebToApp Module Market. Every JS/CSS module the app
shows in its in-app market is fetched directly from this folder over
`raw.githubusercontent.com`, with `cdn.jsdelivr.net` as a fallback. There is
no other backend. A merged PR is published the moment it lands on `main`.

> **English** · [简体中文](#中文)

---

## At a glance

```
modules/
├── registry.json              ← index file the app downloads first
├── README.md                  ← this file
└── <module-path>/             ← one folder per module
    ├── module.json            ← module manifest
    ├── main.js                ← required: module source
    ├── style.css              ← optional: CSS auto-injected on load
    └── icon.png               ← optional: 256×256 icon
```

When a user opens the market, the app fetches `registry.json` and renders
each entry. Tapping **Install** then downloads `<module-path>/module.json` and
`main.js` (plus `style.css` if `hasCss` is `true`) and hands them to the local
extension manager.

---

## Submitting a module

1. **Fork** [`shiahonb777/web-to-app`](https://github.com/shiahonb777/web-to-app).
2. Create a unique kebab-case folder under `modules/`, e.g.
   `modules/dark-reader-lite/`.
3. Add at minimum:
   - `module.json` — the manifest, see schema below
   - `main.js` — the code that runs in the WebView
4. Add an entry to [`registry.json`](registry.json). Keep `id`, `name`, and
   `version` in sync with `module.json`.
5. Open a pull request. CI must pass; a maintainer will review for safety.
6. Once merged, every client sees the new module on its next market refresh
   (default cache is one hour, refresh button bypasses it).

That's the whole pipeline. There is no separate developer account, no API
key, no submission portal.

---

## `module.json` schema

```json
{
  "id": "globally-unique-id",
  "name": "Display Name",
  "description": "One paragraph shown on the install page.",
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

### Allowed `category` values

`CONTENT_FILTER`, `CONTENT_ENHANCE`, `STYLE_MODIFIER`, `THEME`,
`FUNCTION_ENHANCE`, `AUTOMATION`, `NAVIGATION`, `DATA_EXTRACT`, `DATA_SAVE`,
`INTERACTION`, `ACCESSIBILITY`, `MEDIA`, `VIDEO`, `IMAGE`, `AUDIO`, `SECURITY`,
`ANTI_TRACKING`, `SOCIAL`, `SHOPPING`, `READING`, `TRANSLATE`, `DEVELOPER`,
`OTHER`.

### Allowed `runAt` values

`DOCUMENT_START`, `DOCUMENT_END`, `DOCUMENT_IDLE`, `CONTEXT_MENU`,
`BEFORE_UNLOAD`.

### Allowed `permissions` values

`DOM_ACCESS`, `DOM_OBSERVE`, `CSS_INJECT`, `STORAGE`, `COOKIE`, `INDEXED_DB`,
`CACHE`, `NETWORK`, `WEBSOCKET`, `FETCH_INTERCEPT`, `CLIPBOARD`,
`NOTIFICATION`, `ALERT`, `KEYBOARD`, `MOUSE`, `TOUCH`, `LOCATION`, `CAMERA`,
`MICROPHONE`, `DEVICE_INFO`, `MEDIA`, `FULLSCREEN`, `PICTURE_IN_PICTURE`,
`SCREEN_CAPTURE`, `DOWNLOAD`, `FILE_ACCESS`, `EVAL`, `IFRAME`, `WINDOW_OPEN`,
`HISTORY`, `NAVIGATION`. Permissions tagged dangerous (cookies, network, file
access, etc.) get extra review scrutiny.

### Allowed `configItems[].type` values

`TEXT`, `TEXTAREA`, `NUMBER`, `BOOLEAN`, `SELECT`, `MULTI_SELECT`, `RADIO`,
`CHECKBOX`, `COLOR`, `URL`, `EMAIL`, `PASSWORD`, `REGEX`, `CSS_SELECTOR`,
`JAVASCRIPT`, `JSON`, `RANGE`, `DATE`, `TIME`, `DATETIME`, `FILE`, `IMAGE`.

---

## `registry.json` entry schema

Each entry mirrors the manifest plus a `path` pointing at the folder name and
`hasCss` flagging whether a `style.css` file ships next to `main.js`.

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

---

## How `main.js` runs

The module is wrapped in an IIFE before injection. These globals are
available:

| Global | Value |
| --- | --- |
| `__MODULE_INFO__` | `{ id, name, icon, version, uiConfig, runMode }` |
| `__MODULE_CONFIG__` | object of `{ key: value }` from saved config |
| `getConfig(key, defaultValue)` | convenience accessor for the above |

If you ship a `style.css`, it is injected as a `<style>` tag automatically
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

---

## Versioning

Bump `version.name` in **both** `module.json` and `registry.json` when you
release an update. The client compares semver against the locally installed
version and offers a one-tap upgrade. The installed module's saved config is
preserved across updates.

---

## Reviewer checklist

For maintainers — this is what gets checked before a merge.

- [ ] Folder name and `id` are unique and kebab-case
- [ ] `module.json` and `registry.json` agree on `id`, `name`, `version`
- [ ] `main.js` is readable; no obfuscation without a source link
- [ ] No unconditional network calls to third-party endpoints
- [ ] No reading of `document.cookie` or auth tokens unless declared in
      `permissions`
- [ ] `urlMatches` is appropriately scoped (avoid `*` for invasive modules)
- [ ] `main.js` runs cleanly inside the IIFE wrapper (no top-level `return`)
- [ ] `runAt` matches what the code expects (e.g. don't query `document.body`
      at `DOCUMENT_START` without a guard)

---

<a id="中文"></a>
## 中文

这个目录就是 WebToApp 的**模块市场**。所有用户在 App 里看到的市场模块都是直
接从这个文件夹通过 `raw.githubusercontent.com` 拉取的，CDN 兜底是
`cdn.jsdelivr.net`。**没有其他后端**。PR 一旦合并到 `main`，下一刻就上线。

### 目录结构

```
modules/
├── registry.json              ← App 首次下载的索引
├── README.md                  ← 本文件
└── <模块目录>/
    ├── module.json            ← 模块清单
    ├── main.js                ← 必需，模块代码
    ├── style.css              ← 可选，加载时自动注入
    └── icon.png               ← 可选，256×256 图标
```

用户打开市场，App 拉取 `registry.json` 并渲染列表；点击安装时再拉对应模块的
`module.json` 和 `main.js`（还有 `style.css`，如果 `hasCss` 是 `true`），
然后交给本地的扩展管理器。

### 提交流程

1. Fork [`shiahonb777/web-to-app`](https://github.com/shiahonb777/web-to-app)
2. 在 `modules/` 下新建一个唯一的 kebab-case 目录，比如
   `modules/dark-reader-lite/`
3. 至少包含：
   - `module.json` — 清单（schema 见英文段或下方示例）
   - `main.js` — 在 WebView 中执行的代码
4. 在 [`registry.json`](registry.json) 中加一条对应记录，保持 `id`、`name`、
   `version` 和 `module.json` 一致
5. 提 PR；CI 必须通过，维护者会做安全审核
6. 合并后所有客户端在下次刷新（默认 1 小时缓存）即可看到

整个流程就这么多——没有独立账号、没有 API key、没有提交平台。

### 字段枚举

- `category`、`runAt`、`permissions`、`configItems[].type` 的允许值与英文段
  完全一致，参考上方表格
- `minAppVersion` 用来限定只对 `versionCode` 大于等于该值的客户端显示该模块
- 模块 `main.js` 会被包裹在 IIFE 中执行，可用的全局变量见英文段

### 版本升级

发新版时同步在 `module.json` 和 `registry.json` 里把 `version.name` 升上去。
客户端会用 semver 比较版本并提示一键更新。**用户保存的 config 会被保留。**

### 审核 Checklist

- [ ] 目录名和 `id` 唯一且为 kebab-case
- [ ] `module.json` 和 `registry.json` 的 `id`/`name`/`version` 一致
- [ ] `main.js` 可读；没有特殊说明的话不接受混淆/压缩代码
- [ ] 没有无条件调用第三方网络
- [ ] 没有未在 `permissions` 中声明就读取 `document.cookie` 或鉴权 token
- [ ] `urlMatches` 范围合理（侵入性强的模块不要无脑写 `*`）
- [ ] 代码能在 IIFE 包裹下正常执行（不要写顶层 `return`）
- [ ] `runAt` 与代码预期一致（例如 `DOCUMENT_START` 不能直接访问
      `document.body`，需要做判空）
