# WebToApp Module Market

This directory is the source of truth for the in-app **Module Market**. Every
module here is fetched directly by the WebToApp client over `raw.githubusercontent.com`,
so a merged PR is published the instant it lands on `main`.

No backend, no submission portal — just GitHub.

---

## Directory layout

```
modules/
├── registry.json              ← index file the app downloads first
├── README.md                  ← this file
└── <module-path>/             ← one folder per module
    ├── module.json            ← module manifest
    ├── main.js                ← module source code (required)
    ├── style.css              ← optional CSS injected with the module
    └── icon.png               ← optional 256x256 icon
```

The app reads `registry.json`, then for each module the user installs it
fetches `<module-path>/module.json` and `<module-path>/main.js` (plus
`style.css` if `hasCss` is `true`).

---

## Submitting a module

1. **Fork** this repository.
2. Pick a unique, kebab-case folder name under `modules/`, e.g.
   `modules/awesome-tool/`.
3. Add `module.json` and `main.js` (see schemas below).
4. Add an entry to `registry.json` with the same metadata.
5. Open a PR. The maintainer will review for safety and merge.

Once merged, every WebToApp user sees the new module on next market refresh
(default cache is 1 hour).

---

## `registry.json` schema

```json
{
  "schema": 1,
  "updatedAt": "ISO 8601 timestamp",
  "modules": [
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
        "url": "https://github.com/your-handle",
        "email": "optional@example.com"
      },
      "runAt": "DOCUMENT_END",
      "permissions": ["DOM_ACCESS", "CSS_INJECT"],
      "urlMatches": [
        { "pattern": "*", "isRegex": false, "exclude": false }
      ],
      "hasCss": false
    }
  ]
}
```

### `category` values

`CONTENT_FILTER`, `CONTENT_ENHANCE`, `STYLE_MODIFIER`, `THEME`,
`FUNCTION_ENHANCE`, `AUTOMATION`, `NAVIGATION`, `DATA_EXTRACT`, `DATA_SAVE`,
`INTERACTION`, `ACCESSIBILITY`, `MEDIA`, `VIDEO`, `IMAGE`, `AUDIO`, `SECURITY`,
`ANTI_TRACKING`, `SOCIAL`, `SHOPPING`, `READING`, `TRANSLATE`, `DEVELOPER`,
`OTHER`.

### `runAt` values

`DOCUMENT_START`, `DOCUMENT_END`, `DOCUMENT_IDLE`, `CONTEXT_MENU`, `BEFORE_UNLOAD`.

---

## `module.json` schema

```json
{
  "id": "globally-unique-id",
  "name": "Display Name",
  "description": "Detailed description shown on the install page.",
  "icon": "material-icon-name",
  "category": "OTHER",
  "tags": ["tag1"],
  "version": { "code": 1, "name": "1.0.0", "changelog": "Initial release" },
  "author": { "name": "Your Name", "url": "https://github.com/you" },
  "runAt": "DOCUMENT_END",
  "urlMatches": [
    { "pattern": "*", "isRegex": false, "exclude": false }
  ],
  "permissions": ["DOM_ACCESS"],
  "configItems": [
    {
      "key": "greeting",
      "name": "Greeting text",
      "description": "What to show in the banner.",
      "type": "TEXT",
      "defaultValue": "Hello, WebToApp!",
      "required": false
    }
  ]
}
```

The runtime executes `main.js` inside an IIFE with these injected globals:

- `__MODULE_CONFIG__` — object of `{ key: value }` from the user's saved config.
- `getConfig(key, defaultValue)` — convenience accessor.
- `__MODULE_INFO__` — `{ id, name, icon, version }`.

CSS in `style.css` is auto-injected as a `<style>` tag when the module loads.

---

## Versioning

Bump `version.name` in both `registry.json` and `module.json` when you publish
an update. The app compares semver against the installed version and offers an
update.

---

## Review checklist (for maintainers)

- [ ] No obfuscated / minified code without a readable source link.
- [ ] No unconditional network calls to third-party endpoints.
- [ ] No reading of `document.cookie` or auth tokens unless declared in `permissions`.
- [ ] `urlMatches` is appropriately scoped (avoid `*` for invasive modules).
- [ ] `module.json` and `registry.json` agree on `id`, `name`, `version`.
- [ ] `main.js` runs cleanly inside the IIFE wrapper (no top-level `return`).
