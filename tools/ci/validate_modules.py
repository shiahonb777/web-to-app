#!/usr/bin/env python3
"""
Validate the WebToApp Module Market catalog at `modules/`.

This is the source of truth for what `modules/README.md` claims gets
checked at CI time. Every rule here corresponds to a real failure mode
that would either reject a module at install time or quietly waste space
on disk.

The validator is intentionally written with only Python's standard
library — no pip install step, no extra container — so the CI job stays
fast and deterministic.

Run locally:

    python3 tools/ci/validate_modules.py

Exit codes:
    0 = catalog is valid
    1 = at least one error was found (CI fails)
"""

from __future__ import annotations

import json
import os
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable


# ───────────────────────── enum values (must mirror the Kotlin source) ─

# `ModuleCategory` enum in `core/extension/ExtensionModule.kt`.
ALLOWED_CATEGORIES: set[str] = {
    "CONTENT_FILTER", "CONTENT_ENHANCE", "STYLE_MODIFIER", "THEME",
    "FUNCTION_ENHANCE", "AUTOMATION", "NAVIGATION", "DATA_EXTRACT",
    "DATA_SAVE", "INTERACTION", "ACCESSIBILITY", "MEDIA", "VIDEO",
    "IMAGE", "AUDIO", "SECURITY", "ANTI_TRACKING", "SOCIAL", "SHOPPING",
    "READING", "TRANSLATE", "DEVELOPER", "OTHER",
}

# `ModuleRunTime` enum.
ALLOWED_RUN_AT: set[str] = {
    "DOCUMENT_START", "DOCUMENT_END", "DOCUMENT_IDLE",
    "CONTEXT_MENU", "BEFORE_UNLOAD",
}

# `ModulePermission` enum.
ALLOWED_PERMISSIONS: set[str] = {
    "DOM_ACCESS", "DOM_OBSERVE", "CSS_INJECT", "STORAGE", "COOKIE",
    "INDEXED_DB", "CACHE", "NETWORK", "WEBSOCKET", "FETCH_INTERCEPT",
    "CLIPBOARD", "NOTIFICATION", "ALERT", "KEYBOARD", "MOUSE", "TOUCH",
    "LOCATION", "CAMERA", "MICROPHONE", "DEVICE_INFO", "MEDIA",
    "FULLSCREEN", "PICTURE_IN_PICTURE", "SCREEN_CAPTURE", "DOWNLOAD",
    "FILE_ACCESS", "EVAL", "IFRAME", "WINDOW_OPEN", "HISTORY",
    "NAVIGATION",
}

# `ConfigItemType` enum.
ALLOWED_CONFIG_TYPES: set[str] = {
    "TEXT", "TEXTAREA", "NUMBER", "BOOLEAN", "SELECT", "MULTI_SELECT",
    "RADIO", "CHECKBOX", "COLOR", "URL", "EMAIL", "PASSWORD", "REGEX",
    "CSS_SELECTOR", "JAVASCRIPT", "JSON", "RANGE", "DATE", "TIME",
    "DATETIME", "FILE", "IMAGE",
}


# ───────────────────────── regex helpers ───────────────────────────────

KEBAB_CASE_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
SEMVER_RE = re.compile(r"^\d+(?:\.\d+){0,3}(?:[-+][\w.-]+)?$")

# Files we let people drop in besides the three the runtime actually
# downloads (module.json, main.js, style.css).
ALLOWED_EXTRA_FILES: set[str] = {
    "README.md",       # nice for module pages on GitHub
    "CHANGELOG.md",
    "LICENSE",
    "LICENSE.md",
    ".gitkeep",
}


@dataclass
class Report:
    """Accumulates diagnostics and pretty-prints them at the end."""

    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    def error(self, where: str, message: str) -> None:
        self.errors.append(f"  ❌ {where}: {message}")

    def warning(self, where: str, message: str) -> None:
        self.warnings.append(f"  ⚠️  {where}: {message}")

    def ok(self) -> bool:
        return not self.errors

    def render(self) -> str:
        lines: list[str] = []
        if self.errors:
            lines.append(f"\n{len(self.errors)} error(s):")
            lines.extend(self.errors)
        if self.warnings:
            lines.append(f"\n{len(self.warnings)} warning(s):")
            lines.extend(self.warnings)
        if not lines:
            lines.append("\nAll module checks passed.")
        return "\n".join(lines)


# ───────────────────────── primitive checks ────────────────────────────

def _is_str(value: Any) -> bool:
    return isinstance(value, str)


def _expect_str(report: Report, where: str, value: Any, field: str) -> str | None:
    if value is None:
        return None
    if not _is_str(value):
        report.error(where, f"`{field}` must be a string, got {type(value).__name__}")
        return None
    return value


def _expect_list_of_str(report: Report, where: str, value: Any, field: str) -> list[str]:
    if value is None:
        return []
    if not isinstance(value, list) or any(not _is_str(v) for v in value):
        report.error(where, f"`{field}` must be a list of strings")
        return []
    return value


def _validate_url_matches(
    report: Report, where: str, url_matches: Any
) -> None:
    if url_matches is None:
        return
    if not isinstance(url_matches, list):
        report.error(where, "`urlMatches` must be a list")
        return
    for i, rule in enumerate(url_matches):
        rule_loc = f"{where}::urlMatches[{i}]"
        if not isinstance(rule, dict):
            report.error(rule_loc, "must be an object with at least `pattern`")
            continue
        pattern = rule.get("pattern")
        if not _is_str(pattern) or not pattern:
            report.error(rule_loc, "`pattern` is required and must be a non-empty string")
        for flag in ("isRegex", "exclude"):
            if flag in rule and not isinstance(rule[flag], bool):
                report.error(rule_loc, f"`{flag}` must be a boolean")


def _validate_author(report: Report, where: str, author: Any) -> None:
    if author is None:
        return
    if not isinstance(author, dict):
        report.error(where, "`author` must be an object")
        return
    name = author.get("name")
    if not _is_str(name) or not name.strip():
        report.error(where, "`author.name` is required")
    for opt_key in ("email", "url", "qq"):
        if opt_key in author and author[opt_key] is not None and not _is_str(author[opt_key]):
            report.error(where, f"`author.{opt_key}` must be a string")


# ───────────────────────── manifest checks ─────────────────────────────

def _validate_module_json(
    report: Report, folder: Path, manifest: dict[str, Any]
) -> None:
    """Validate a per-module `module.json`."""

    where = f"modules/{folder.name}/module.json"

    # Required fields.
    if not _is_str(manifest.get("id")) or not manifest["id"].strip():
        report.error(where, "`id` is required and must be a non-empty string")
    if not _is_str(manifest.get("name")) or not manifest["name"].strip():
        report.error(where, "`name` is required and must be a non-empty string")

    # Enums (with a friendly hint when wrong).
    category = manifest.get("category", "OTHER")
    if not _is_str(category) or category not in ALLOWED_CATEGORIES:
        report.error(
            where,
            f"`category` must be one of {sorted(ALLOWED_CATEGORIES)}, got {category!r}",
        )

    run_at = manifest.get("runAt", "DOCUMENT_END")
    if not _is_str(run_at) or run_at not in ALLOWED_RUN_AT:
        report.error(
            where,
            f"`runAt` must be one of {sorted(ALLOWED_RUN_AT)}, got {run_at!r}",
        )

    # Version (object form).
    version = manifest.get("version")
    if version is None:
        report.error(where, "`version` is required (object with `code`, `name`, `changelog`)")
    elif not isinstance(version, dict):
        report.error(where, "`version` must be an object — see modules/README.md schema")
    else:
        v_code = version.get("code")
        if not isinstance(v_code, int) or v_code < 1:
            report.error(where, "`version.code` must be a positive integer")
        v_name = version.get("name")
        if not _is_str(v_name) or not SEMVER_RE.match(v_name or ""):
            report.error(where, f"`version.name` must be semver-shaped, got {v_name!r}")
        if "changelog" in version and version["changelog"] is not None and not _is_str(version["changelog"]):
            report.error(where, "`version.changelog` must be a string")

    # Permissions.
    for perm in _expect_list_of_str(report, where, manifest.get("permissions"), "permissions"):
        if perm not in ALLOWED_PERMISSIONS:
            report.error(where, f"unknown permission {perm!r}")

    # urlMatches and author.
    _validate_url_matches(report, where, manifest.get("urlMatches"))
    _validate_author(report, where, manifest.get("author"))

    # configItems.
    config_items = manifest.get("configItems", [])
    if config_items is None:
        config_items = []
    if not isinstance(config_items, list):
        report.error(where, "`configItems` must be a list")
        return
    seen_keys: set[str] = set()
    for i, item in enumerate(config_items):
        item_loc = f"{where}::configItems[{i}]"
        if not isinstance(item, dict):
            report.error(item_loc, "must be an object")
            continue
        key = item.get("key")
        if not _is_str(key) or not key.strip():
            report.error(item_loc, "`key` is required")
        elif key in seen_keys:
            report.error(item_loc, f"duplicate key {key!r}")
        else:
            seen_keys.add(key)

        if not _is_str(item.get("name")) or not item["name"].strip():
            report.error(item_loc, "`name` is required")

        type_ = item.get("type", "TEXT")
        if not _is_str(type_) or type_ not in ALLOWED_CONFIG_TYPES:
            report.error(item_loc, f"`type` must be one of {sorted(ALLOWED_CONFIG_TYPES)}")

        if "required" in item and not isinstance(item["required"], bool):
            report.error(item_loc, "`required` must be a boolean")

        for str_field in ("description", "defaultValue", "placeholder", "validation"):
            if str_field in item and item[str_field] is not None and not _is_str(item[str_field]):
                report.error(item_loc, f"`{str_field}` must be a string")

        # SELECT / MULTI_SELECT / RADIO need `options`.
        if type_ in {"SELECT", "MULTI_SELECT", "RADIO"}:
            options = item.get("options")
            if not isinstance(options, list) or not options:
                report.error(item_loc, f"`options` is required for type {type_}")


# ───────────────────────── registry checks ─────────────────────────────

def _validate_registry_entry(
    report: Report, entry: dict[str, Any], index: int
) -> None:
    where = f"registry.json::modules[{index}]"

    for required in ("id", "path", "name"):
        if not _is_str(entry.get(required)) or not entry[required].strip():
            report.error(where, f"`{required}` is required and must be a non-empty string")

    path = entry.get("path", "")
    if _is_str(path) and not KEBAB_CASE_RE.match(path):
        report.error(where, f"`path` must be kebab-case, got {path!r}")

    version = entry.get("version")
    if not _is_str(version) or not SEMVER_RE.match(version or ""):
        report.error(where, f"`version` must be a semver string, got {version!r}")

    category = entry.get("category", "OTHER")
    if not _is_str(category) or category not in ALLOWED_CATEGORIES:
        report.error(where, f"`category` must be one of the allowed values, got {category!r}")

    run_at = entry.get("runAt", "DOCUMENT_END")
    if not _is_str(run_at) or run_at not in ALLOWED_RUN_AT:
        report.error(where, f"`runAt` must be one of the allowed values, got {run_at!r}")

    for perm in _expect_list_of_str(report, where, entry.get("permissions"), "permissions"):
        if perm not in ALLOWED_PERMISSIONS:
            report.error(where, f"unknown permission {perm!r}")

    _validate_url_matches(report, where, entry.get("urlMatches"))
    _validate_author(report, where, entry.get("author"))

    if "minAppVersion" in entry and not isinstance(entry["minAppVersion"], int):
        report.error(where, "`minAppVersion` must be an integer")

    if "hasCss" in entry and not isinstance(entry["hasCss"], bool):
        report.error(where, "`hasCss` must be a boolean")


def _validate_registry(report: Report, registry: dict[str, Any]) -> list[dict[str, Any]]:
    where = "registry.json"
    schema = registry.get("schema")
    if schema != 1:
        report.error(where, f"`schema` must be 1 (got {schema!r}); future versions need a parser update")
    if "updatedAt" in registry and not _is_str(registry["updatedAt"]):
        report.error(where, "`updatedAt` must be a string")

    modules = registry.get("modules")
    if not isinstance(modules, list):
        report.error(where, "`modules` must be a list")
        return []

    seen_ids: set[str] = set()
    seen_paths: set[str] = set()
    for i, entry in enumerate(modules):
        if not isinstance(entry, dict):
            report.error(f"{where}::modules[{i}]", "must be an object")
            continue
        _validate_registry_entry(report, entry, i)

        eid = entry.get("id") if _is_str(entry.get("id")) else None
        if eid:
            if eid in seen_ids:
                report.error(f"{where}::modules[{i}]", f"duplicate id {eid!r}")
            seen_ids.add(eid)

        epath = entry.get("path") if _is_str(entry.get("path")) else None
        if epath:
            if epath in seen_paths:
                report.error(f"{where}::modules[{i}]", f"duplicate path {epath!r}")
            seen_paths.add(epath)

    return [m for m in modules if isinstance(m, dict)]


# ───────────────────────── cross-file consistency ─────────────────────

def _validate_cross_consistency(
    report: Report,
    entry: dict[str, Any],
    manifest: dict[str, Any],
    folder: Path,
) -> None:
    """`module.json` and `registry.json` must agree on the shared fields."""

    where = f"modules/{folder.name}"

    # id
    if entry.get("id") != manifest.get("id"):
        report.error(
            where,
            f"`id` mismatch: registry says {entry.get('id')!r}, manifest says {manifest.get('id')!r}",
        )

    # name
    if entry.get("name") != manifest.get("name"):
        report.error(
            where,
            f"`name` mismatch: registry says {entry.get('name')!r}, manifest says {manifest.get('name')!r}",
        )

    # version: registry stores the string, manifest stores an object.
    reg_version = entry.get("version")
    man_version_obj = manifest.get("version") or {}
    man_version = man_version_obj.get("name") if isinstance(man_version_obj, dict) else None
    if reg_version != man_version:
        report.error(
            where,
            f"`version` mismatch: registry={reg_version!r}, module.json::version.name={man_version!r}",
        )

    # runAt
    if entry.get("runAt") and manifest.get("runAt") and entry["runAt"] != manifest["runAt"]:
        report.error(
            where,
            f"`runAt` mismatch: registry={entry['runAt']!r}, manifest={manifest['runAt']!r}",
        )

    # permissions: registry should be a superset (the listing surface) of manifest.
    reg_perms = set(entry.get("permissions") or [])
    man_perms = set(manifest.get("permissions") or [])
    missing_in_registry = man_perms - reg_perms
    if missing_in_registry:
        report.error(
            where,
            f"manifest declares permissions {sorted(missing_in_registry)} that registry.json does not list",
        )

    # hasCss must match style.css presence on disk.
    has_css_flag = bool(entry.get("hasCss"))
    style_present = (folder / "style.css").is_file()
    if has_css_flag and not style_present:
        report.error(
            where,
            "registry says `hasCss: true` but no `style.css` file is present",
        )
    if style_present and not has_css_flag:
        report.error(
            where,
            "`style.css` exists but registry has `hasCss: false` — it will not be downloaded",
        )


# ───────────────────────── per-folder structural checks ───────────────

def _validate_folder_layout(report: Report, folder: Path) -> None:
    """Each module folder must look like the schema says it does."""

    where = f"modules/{folder.name}"

    if not KEBAB_CASE_RE.match(folder.name):
        report.error(where, f"folder name must be kebab-case, got {folder.name!r}")

    if not (folder / "module.json").is_file():
        report.error(where, "missing required `module.json`")

    if not (folder / "main.js").is_file():
        report.error(where, "missing required `main.js`")

    # Flag stray files. The runtime ignores them, so they only bloat the repo.
    expected = {"module.json", "main.js", "style.css"}
    for child in folder.iterdir():
        if child.name in expected or child.name in ALLOWED_EXTRA_FILES:
            continue
        if child.is_dir():
            report.warning(
                where,
                f"unexpected sub-directory `{child.name}/` — the runtime won't read it",
            )
        else:
            report.warning(
                where,
                f"unexpected file `{child.name}` — the runtime won't download it",
            )


def _validate_main_js(report: Report, folder: Path) -> None:
    """Cheap heuristics on `main.js`."""

    main_js = folder / "main.js"
    if not main_js.is_file():
        return
    where = f"modules/{folder.name}/main.js"

    try:
        content = main_js.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        report.error(where, "must be UTF-8")
        return

    if not content.strip():
        report.error(where, "is empty")
        return

    if len(content) > 512 * 1024:
        report.warning(where, f"is large ({len(content) // 1024} KB) — consider trimming")

    # No top-level `return` — the IIFE wrapper turns this into a syntax error.
    for lineno, line in enumerate(content.splitlines(), start=1):
        stripped = line.strip()
        if stripped.startswith("return ") or stripped == "return;" or stripped == "return":
            # Could still be inside a function literal at line 1; we can't
            # parse JS exactly, but this is a strong smell.
            indent = len(line) - len(line.lstrip())
            if indent == 0:
                report.error(
                    where,
                    f"line {lineno}: top-level `return` — the IIFE wrapper would make this a syntax error",
                )
                break

    # If `getConfig` is referenced but no configItems are declared in the
    # manifest, the user will see "undefined" defaults silently.
    manifest_path = folder / "module.json"
    if manifest_path.is_file() and "getConfig(" in content:
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            if not manifest.get("configItems"):
                report.warning(
                    where,
                    "uses `getConfig(...)` but `module.json` declares no `configItems`",
                )
        except (json.JSONDecodeError, OSError):
            # The JSON-level checks elsewhere will report the actual cause.
            pass


# ───────────────────────── entry point ─────────────────────────────────

def _load_json(report: Report, path: Path, where: str) -> dict[str, Any] | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        report.error(where, f"file not found: {path}")
    except json.JSONDecodeError as e:
        report.error(where, f"invalid JSON: {e.msg} (line {e.lineno}, col {e.colno})")
    except UnicodeDecodeError:
        report.error(where, "file must be UTF-8")
    return None


def main(repo_root: Path) -> int:
    modules_dir = repo_root / "modules"
    if not modules_dir.is_dir():
        print(f"❌ {modules_dir} does not exist", file=sys.stderr)
        return 1

    report = Report()

    # 1. Parse + validate the registry.
    registry = _load_json(report, modules_dir / "registry.json", "registry.json")
    registry_entries: list[dict[str, Any]] = []
    if isinstance(registry, dict):
        registry_entries = _validate_registry(report, registry)

    # 2. Walk module folders.
    folders = sorted(
        p for p in modules_dir.iterdir()
        if p.is_dir() and not p.name.startswith(".")
    )

    folder_paths = {p.name for p in folders}
    registry_paths = {e["path"] for e in registry_entries if _is_str(e.get("path"))}

    # Folders missing a registry entry.
    for orphan in folder_paths - registry_paths:
        report.error(f"modules/{orphan}", "module folder has no entry in registry.json")
    # Registry entries pointing at non-existent folders.
    for ghost in registry_paths - folder_paths:
        report.error("registry.json", f"entry refers to missing folder modules/{ghost}/")

    # 3. Per-folder validation + cross-file consistency.
    entries_by_path = {e["path"]: e for e in registry_entries if _is_str(e.get("path"))}
    for folder in folders:
        _validate_folder_layout(report, folder)
        _validate_main_js(report, folder)

        manifest_path = folder / "module.json"
        manifest = _load_json(report, manifest_path, f"modules/{folder.name}/module.json")
        if isinstance(manifest, dict):
            _validate_module_json(report, folder, manifest)
            entry = entries_by_path.get(folder.name)
            if entry:
                _validate_cross_consistency(report, entry, manifest, folder)

    print(report.render())
    return 0 if report.ok() else 1


if __name__ == "__main__":
    repo = Path(__file__).resolve().parents[2]
    sys.exit(main(repo))
