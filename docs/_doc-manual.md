# Doc Manual

> **This file is the meta-specification for all project documentation.** You must read and follow this manual before creating or modifying any document.

---

## 1. Documentation System

```
docs/                                              ← Source directory (Git tracked)
├── _doc-manual.md              # Doc Manual (this file) — Read before creating/modifying docs
├── _doc-maintenance.md         # Doc Maintenance — Audit & correction process
├── project-overview.md         # Project Overview
├── architecture-guide.md       # Architecture — Layered design, dual-track mode, core subsystems
├── build-and-release-guide.md  # Build & Release — Build config, signing, Shell template, release flow
├── data-model-guide.md         # Data Model — WebApp model, nested configs, enums, serialization
├── extension-module-guide.md   # Extension Module — Module development, URL matching, config, injection
├── shell-mode-guide.md         # Shell Mode — Shell architecture, config loading, lifecycle, Manifest
├── security-features-guide.md  # Security — Encryption, hardening, privacy isolation, shields, activation
├── unit-test-guide.md          # Unit Test — Test checklist, coverage matrix, conventions, how to run
├── feedback-guide.md           # Feedback — Standard format, screenshot rules, great examples
└── i18n-localization-guide.md  # i18n Guide

app/src/main/assets/docs/                          ← Client rendering directory (must sync manually)
├── README.md                  # Chinese version: README_CN.md
├── _doc-manual.md
├── _doc-maintenance.md
├── project-overview.md
├── ...（mirrors docs/ directory）
└── feedback-guide.md
```

> ⚠️ **Critical**: `docs/` is the source directory, `app/src/main/assets/docs/` is the client rendering directory. They must be kept in sync — see Section 5.

### File Naming Convention

| Rule | Description | Example |
|------|-------------|---------|
| Lowercase + hyphens | Filenames use only lowercase letters and hyphens | `i18n-localization-guide.md` |
| `_` prefix | System-level meta-docs start with underscore, sort first | `_doc-manual.md` |
| `_CN` suffix | Chinese version of a doc uses `_CN` suffix | `architecture-guide_CN.md` |
| Semantic naming | Filename should directly reflect the content topic | `project-overview.md` not `doc1.md` |

### Bilingual Naming

Each document has two versions:

| File | Language | Loading condition |
|------|----------|-----------------|
| `architecture-guide.md` | English | Non-Chinese users |
| `architecture-guide_CN.md` | Chinese | Chinese users |

`DocsScreen` uses the `chineseFileName` field on `DocItem` to automatically select the correct version based on the app language setting.

---

## 2. Creating New Documents

### 2.1 Pre-creation Checklist

- [ ] Does a similar document already exist? Avoid creating documents with overlapping topics
- [ ] Is the document topic clear? One document = one topic
- [ ] Does the filename follow naming conventions?
- [ ] Do you need to update the `project-overview.md` document index?
- [ ] Do you need to sync to `app/src/main/assets/docs/`? (See Section 5)
- [ ] Have you created both English and Chinese versions? (See bilingual naming above)

### 2.2 Document Template

Every document **must** contain the following structure:

```markdown
# Document Title

> **One-line summary** explaining the document's purpose and applicable scenarios.

---

## 1. [Topic Section]

### Subsection

Content...

---

## 2. [Topic Section]

Content...

---

## Revision History

| Date | Changes | Author |
|------|---------|--------|
| YYYY-MM-DD | Initial creation | xxx |
```

### 2.3 Mandatory Rules

1. **Summary line**: File must start with a `>` blockquote summary explaining the document's purpose
2. **Section numbering**: Use Arabic numerals (1, 2, 3...), subsections use `###`
3. **Code references**: Use backticks + relative path when referencing project files, e.g. `` `app/src/main/java/.../Strings.kt` ``
4. **Code blocks**: Specify language type, e.g. ` ```kotlin `
5. **Tables**: Use Markdown tables, must have headers
6. **No hardcoded line numbers**: Line numbers change with iterations — reference by function/class/property name
7. **No static to-do lists**: Don't maintain "pending file lists" in docs; instead provide **discovery methods** (e.g., grep commands + judgment rules)
8. **No hardcoded numbers**: Code line counts, file counts etc. change — use "approximately" or provide commands to get current values
9. **Language**: English for the default version, Chinese for the `_CN` version. Code examples and technical terms keep original English
10. **Revision history**: Append a row to the revision history table for every substantive content change

---

## 3. Modifying Existing Documents

### 3.1 Pre-modification Checklist

- [ ] Is the modification consistent with the document's topic? Don't stuff Topic B content into a Topic A document
- [ ] Do other documents need sync updates? (e.g., `project-overview.md` index)
- [ ] Is the summary line still accurate after modification?
- [ ] Do you need to append a revision history entry?
- [ ] Do you need to sync to `app/src/main/assets/docs/`? (See Section 5)
- [ ] Did you update both English and Chinese versions?

### 3.2 Modification Principles

1. **Balanced additions/deletions**: When adding a section, evaluate whether it should be a separate document
2. **No stale content accumulation**: Delete outdated content — don't keep it with strikethrough (~~like this~~), just delete
3. **Methods over lists**: Prefer providing "how to discover/verify" methodology over static enumeration
4. **Generic examples**: Example code should reflect general patterns, don't hardcode specific business data

---

## 4. Prohibitions

| Prohibited | Reason |
|------------|--------|
| Maintaining "pending file lists" in docs | Projects iterate continuously; static lists become outdated fast — provide scanning methods instead |
| Hardcoding line numbers | Line numbers change with iterations — use function/property names for reference |
| Creating duplicate-topic documents | One topic = one document; update existing docs instead |
| Writing temporary notes/drafts in docs | Docs are formal deliverables; drafts belong in personal notes |
| Copying large content blocks between docs | Maintain single source of truth — reference other documents |

---

## 5. Syncing Documents to Client

> ⚠️ **This is the most easily forgotten step!** After every document creation or modification, you must perform the following sync operations, or users will see outdated documents in the app.

### 5.1 Why Syncing is Needed

Project documentation exists in two copies:

| Location | Purpose | Notes |
|----------|---------|-------|
| `docs/` | Source directory | Git tracked, developers edit directly |
| `app/src/main/assets/docs/` | Client rendering directory | In-app doc viewer loads from here |

The app's `DocsScreen` component reads Markdown from `assets/docs/` and renders it as Compose UI. **It does not automatically read the `docs/` directory**, so you must manually sync after every source file modification.

### 5.2 Sync Operation Checklist

After every document creation or modification, complete **all steps**:

#### Step 1: Copy files to assets

```bash
# New document
cp docs/<filename>.md app/src/main/assets/docs/<filename>.md

# Modified document (overwrite)
cp docs/<filename>.md app/src/main/assets/docs/<filename>.md

# Don't forget the _CN version too!
cp docs/<filename>_CN.md app/src/main/assets/docs/<filename>_CN.md
```

#### Step 2: Register in DocsScreen

In `app/src/main/java/com/webtoapp/ui/screens/DocsScreen.kt`:

1. **`DOC_ITEMS` list**: Add `DocItem` entry with `chineseFileName` (only for new documents)
   ```kotlin
   DocItem("feedback-guide.md", "feedbackGuide", "feedbackGuideDesc", Icons.Outlined.Feedback, chineseFileName = "feedback-guide_CN.md"),
   ```

2. **`resolveTitle()`**: Add title mapping
   ```kotlin
   "feedbackGuide" -> Strings.docFeedbackGuide
   ```

3. **`resolveDesc()`**: Add description mapping
   ```kotlin
   "feedbackGuide" -> Strings.docFeedbackGuideDesc
   ```

#### Step 3: Add i18n strings

In `app/src/main/java/com/webtoapp/core/i18n/Strings.kt`, add trilingual title and description strings:

```kotlin
val docFeedbackGuide: String get() = when (lang) {
    AppLanguage.CHINESE -> "用户反馈规范"
    AppLanguage.ENGLISH -> "Feedback Guide"
    AppLanguage.ARABIC -> "دليل الملاحظات"
}
val docFeedbackGuideDesc: String get() = when (lang) {
    AppLanguage.CHINESE -> "反馈标准格式、截图规范、优秀示例"
    AppLanguage.ENGLISH -> "Feedback format, screenshot rules & great examples"
    AppLanguage.ARABIC -> "تنسيق الملاحظات، قواعد لقطات الشاشة وأمثلة رائعة"
}
```

#### Step 4: Update document index

Add a new entry in the `docs/project-overview.md` document index table (only for new documents).

### 5.3 Verify Sync Integrity

```bash
# Check that docs/ and assets/docs/ files correspond
diff <(ls docs/*.md | xargs -n1 basename | sort) \
     <(ls app/src/main/assets/docs/*.md | xargs -n1 basename | sort)
```

If diff has output, some files are not synced.

### 5.4 Commonly Missed Scenarios

| Scenario | Easily missed operation | Consequence |
|----------|------------------------|-------------|
| New document | Forget to copy to `assets/docs/` | New doc invisible in app |
| New document | Forget to register `DocItem` in `DocsScreen` | Doc doesn't appear in list |
| New document | Forget to add i18n strings | Title/description shows raw key |
| Content modification | Forget to overwrite old version in `assets/docs/` | App shows old content |
| Content modification | No need to update `DocItem` or i18n | — (content changes don't need registration updates) |

---

## 6. DocsScreen Technical Details

`DocsScreen` is the in-app Markdown document viewer, based on `commonmark-java` parsing + Jetpack Compose rendering.

### Core Components

| Component | File | Responsibility |
|-----------|------|----------------|
| `DocsScreen` | `ui/screens/DocsScreen.kt` | Main entry, list/detail switching |
| `DocListScreen` | Same file | Document card list |
| `DocDetailScreen` | Same file | Load Markdown from assets and render |
| `MarkdownContent` | Same file | commonmark AST → Compose UI recursive rendering |

### Supported Markdown Features

- Headings (H1-H6, H1 with emphasis underline)
- Paragraphs, bold, italic, inline code
- Ordered/unordered lists (nested)
- Code blocks (rounded card style)
- Blockquotes (left emphasis bar)
- Horizontal rules
- GFM tables (header highlight + zebra striping)
- Links (underline + theme color)

### Chinese Version Support

`DocItem` has an optional `chineseFileName` field. When the app language is Chinese, it automatically loads the Chinese version:

```kotlin
DocItem("README.md", "readme", "readmeDesc", Icons.Outlined.Article, chineseFileName = "README_CN.md")
```

---

## Revision History

| Date | Changes | Author |
|------|---------|--------|
| 2026-04-19 | Initial creation | Cascade |
| 2026-04-25 | Added Section 5 (doc sync to client), Section 6 (DocsScreen technical details); updated doc tree and checklists; added bilingual naming convention | Cascade |
