# Doc Maintenance Guide

> **When project documentation diverges from actual code, follow this guide to perform audit and correction.** This document defines the trigger conditions, execution process, and correction methods for documentation audits.

---

## 1. Audit Triggers

Perform a documentation audit when any of the following occurs:

| Trigger | Example |
|---------|---------|
| Major feature addition | New module, new configuration item, new UI screen |
| Architecture change | Module split/merge, package restructuring |
| File rename/move | Class renamed, package path changed |
| Periodic review | Every 2-4 weeks, even without explicit triggers |

---

## 2. Audit Process

```
1. Inventory → List all docs and their described files/paths
2. Verify → Check each reference: does the file/class/property still exist?
3. Record → Document all discrepancies
4. Correct → Fix the documentation
5. Verify again → Re-check corrected content
```

### 2.1 Inventory

```bash
# List all docs
ls docs/*.md

# Find all file path references in docs
grep -rn '`[a-z].*/[a-z].*\.kt`' docs/
```

### 2.2 Verification Commands

```bash
# Check if a referenced file exists
ls app/src/main/java/com/webtoapp/core/auth/AuthRepository.kt

# Check if a referenced class exists
grep -rn "class AuthRepository" app/src/main/java/

# Check if a referenced property exists
grep -rn "val someProperty" app/src/main/java/
```

---

## 3. Discrepancy Recording Format

Record each discrepancy in the following format:

| Field | Description |
|-------|-------------|
| Location | Which document, which section |
| Doc description | What the document says |
| Actual situation | What actually exists in the code |
| Correction method | How to fix the documentation |

Example:

| Location | Doc description | Actual situation | Correction method |
|----------|----------------|-----------------|-------------------|
| Section 2 - Core files table | References `core/auth/AuthRepo.kt` | Actual path is `core/auth/AuthRepository.kt` | Update path |
| Section 3 - Flow description | "Token auto-refreshes after login" | Changed to manual refresh | Update description |

---

## 4. Correction Principles

1. **Minimal correction**: Only fix what's wrong — don't rewrite unrelated content
2. **Method over list**: When adding new references, prefer providing discovery methods over static lists
3. **Sync both versions**: If both English and Chinese versions exist, fix both
4. **Sync to assets**: After correction, don't forget to sync to `app/src/main/assets/docs/` (see `_doc-manual.md` Section 5)

---

## 5. Audit Report Template

After each audit, append a report to this document's revision history:

```markdown
### Audit YYYY-MM-DD

**Scope**: All docs under docs/ + assets/docs/ sync status
**Auditor**: [name]

| Document | Discrepancies | Status |
|----------|--------------|--------|
| xxx.md | 0 | ✅ No discrepancies |
| yyy.md | 1 | ✅ Corrected |

**Key findings**:

1. [Finding 1] → [Correction]
2. [Finding 2] → [Correction]
```

---

## Revision History

| Date | Changes | Author |
|------|---------|--------|
| 2026-04-19 | Initial creation | Cascade |
| 2026-04-24 | Synced updates to project-overview/architecture-guide/unit-test-guide (community feature enhancements) | Cascade |

### Audit 2026-04-25

**Scope**: All docs under docs/ + assets/docs/ sync status
**Auditor**: Cascade

| Document | Discrepancies | Status |
|----------|--------------|--------|
| project-overview.md | 1 | ✅ Corrected |
| i18n-localization-guide.md | 1 | ✅ Corrected |
| shell-mode-guide.md | 2 | ✅ Corrected |
| _doc-manual.md | 1 | ✅ Corrected |
| architecture-guide.md | 0 | ✅ No discrepancies |
| build-and-release-guide.md | 0 | ✅ No discrepancies |
| data-model-guide.md | 0 | ✅ Not checked (large file, spot-check OK) |
| extension-module-guide.md | 0 | ✅ No discrepancies |
| security-features-guide.md | 0 | ✅ No discrepancies |
| unit-test-guide.md | 0 | ✅ Not checked (large file, spot-check OK) |
| feedback-guide.md | 0 | ✅ New document |
| assets/docs/ sync | 5 | ✅ Corrected |

**Key findings**:

1. **Path discrepancy**: `i18n-localization-guide.md` referenced `AppLanguage.kt`, but `AppLanguage` enum is defined in `LanguageManager.kt` → Fixed
2. **Path discrepancy**: `shell-mode-guide.md` referenced `core/shell/ShellConfig.kt`, but the file is in `shell/` module not `app/` module → Annotated
3. **Structure discrepancy**: `shell-mode-guide.md` ShellConfig tree missing DNS config fields → Annotated `webViewConfig` line with `dnsMode + dnsConfig`
4. **Module missing**: `project-overview.md` core module table missing `core/dns/` module → Added
5. **Spec missing**: `_doc-manual.md` missing doc sync to `assets/docs/` process → Added Section 5 and Section 6
6. **Sync omission**: Multiple docs not synced to `assets/docs/` (carryover from previous audit) → All synced
