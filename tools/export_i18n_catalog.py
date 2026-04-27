#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


ROOT_DIR = Path(__file__).resolve().parents[1]
STRINGS_DIR = ROOT_DIR / "app" / "src" / "main" / "java" / "com" / "webtoapp" / "core" / "i18n" / "strings"
RES_DIR = ROOT_DIR / "app" / "src" / "main" / "res"

SECTION_RE = re.compile(r"^\s*//\s*=+\s*(.*?)\s*=+\s*$")
INLINE_GETTER_RE = re.compile(r"^\s*val\s+([A-Za-z0-9_]+)\s*:\s*String\s*get\(\)\s*=\s*(.+?)\s*$")
VALUE_DECL_RE = re.compile(r"^\s*val\s+([A-Za-z0-9_]+)\s*:\s*String\s*$")
GETTER_RE = re.compile(r"^\s*get\(\)\s*=\s*(.+?)\s*$")
WHEN_RE = re.compile(r"^when\s*\(\s*lang\s*\)\s*\{")
LANG_BRANCH_RE = re.compile(r"^\s*AppLanguage\.(CHINESE|ENGLISH|ARABIC)\s*->\s*(.+?)\s*$")
ELSE_BRANCH_RE = re.compile(r"^\s*else\s*->\s*(.+?)\s*$")
RESOURCE_RE = re.compile(r"Strings\.resourceString\(\s*R\.string\.([A-Za-z0-9_]+)\s*\)")
PLACEHOLDER_RE = re.compile(r"(%\d*\$?[sdif]|%\.\d+f|\$\{[^}]+\}|\{[A-Za-z0-9_]+\})")

LANGUAGE_COLUMNS = {
    "CHINESE": "chinese",
    "ENGLISH": "english",
    "ARABIC": "arabic",
}

RESOURCE_LOCALES = {
    "chinese": ["values-zh", "values"],
    "english": ["values-en"],
    "arabic": ["values-ar"],
}


@dataclass
class TranslationEntry:
    group: str
    key: str
    source_file: str
    line: int
    section: str
    kind: str
    chinese: str = ""
    english: str = ""
    arabic: str = ""
    resource_ref: str = ""
    notes: str = ""
    translator_notes: str = ""
    has_chinese: bool = field(default=False, repr=False, compare=False, metadata={"export": False})
    has_english: bool = field(default=False, repr=False, compare=False, metadata={"export": False})
    has_arabic: bool = field(default=False, repr=False, compare=False, metadata={"export": False})


@dataclass
class ParseIssue:
    source_file: str
    line: int
    key: str
    message: str


EXPORT_FIELD_NAMES = [
    name
    for name, field_info in TranslationEntry.__dataclass_fields__.items()
    if field_info.metadata.get("export", True)
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export WebToApp grouped i18n strings into translator-friendly CSV/JSON catalogs."
    )
    parser.add_argument(
        "--format",
        choices=["csv", "json", "all"],
        default=None,
        help="Export format. Defaults to all when not using --check.",
    )
    parser.add_argument(
        "--output-dir",
        default=str(ROOT_DIR / "build" / "i18n"),
        help="Directory for exported catalog files.",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate parsing and print a summary without requiring a build.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    resource_index = load_resource_index()
    entries, issues = parse_grouped_string_files(resource_index)
    entries.sort(key=lambda item: (item.source_file, item.line, item.key))

    missing_language_count = count_missing_language_cells(entries)
    resource_backed_count = sum(1 for entry in entries if entry.kind == "resource-backed")
    shared_literal_count = sum(1 for entry in entries if entry.kind == "shared-literal")

    export_format = args.format if args.format is not None else ("all" if not args.check else None)

    if export_format is not None:
        output_dir = Path(args.output_dir).resolve()
        output_dir.mkdir(parents=True, exist_ok=True)
        write_catalog(entries, output_dir, export_format)

    print(
        "Parsed "
        f"{len(entries)} entries from {len(list(STRINGS_DIR.glob('*Strings.kt')))} grouped string files. "
        f"resource-backed={resource_backed_count}, shared-literal={shared_literal_count}, "
        f"missing-language-cells={missing_language_count}"
    )

    if issues:
        print("\nParse issues:", file=sys.stderr)
        for issue in issues:
            print(
                f"- {issue.source_file}:{issue.line} [{issue.key}] {issue.message}",
                file=sys.stderr,
            )
        return 1

    return 0


def parse_grouped_string_files(resource_index: dict[str, dict[str, str]]) -> tuple[list[TranslationEntry], list[ParseIssue]]:
    entries: list[TranslationEntry] = []
    issues: list[ParseIssue] = []

    for file_path in sorted(STRINGS_DIR.glob("*Strings.kt")):
        file_entries, file_issues = parse_grouped_string_file(file_path, resource_index)
        entries.extend(file_entries)
        issues.extend(file_issues)

    return entries, issues


def parse_grouped_string_file(
    file_path: Path,
    resource_index: dict[str, dict[str, str]],
) -> tuple[list[TranslationEntry], list[ParseIssue]]:
    lines = file_path.read_text(encoding="utf-8").splitlines()
    entries: list[TranslationEntry] = []
    issues: list[ParseIssue] = []
    section = "General"
    relative_path = file_path.relative_to(ROOT_DIR).as_posix()
    group = file_path.stem

    line_index = 0
    while line_index < len(lines):
        line = lines[line_index]
        section_match = SECTION_RE.match(line)
        if section_match:
            section = clean_section(section_match.group(1))
            line_index += 1
            continue

        property_match = INLINE_GETTER_RE.match(line)
        getter_line_index = line_index
        if property_match:
            key = property_match.group(1)
            expression = property_match.group(2).strip()
        else:
            value_decl = VALUE_DECL_RE.match(line)
            if not value_decl:
                line_index += 1
                continue

            key = value_decl.group(1)
            getter_result = find_getter_expression(lines, line_index + 1)
            if getter_result is None:
                issues.append(
                    ParseIssue(
                        source_file=relative_path,
                        line=line_index + 1,
                        key=key,
                        message="Could not find getter expression for String property.",
                    )
                )
                line_index += 1
                continue

            getter_line_index, expression = getter_result

        entry, entry_issues, consumed_until = parse_property_expression(
            lines=lines,
            key=key,
            group=group,
            relative_path=relative_path,
            property_line_index=line_index,
            getter_line_index=getter_line_index,
            expression=expression,
            section=section,
            resource_index=resource_index,
        )
        if entry is not None:
            entries.append(entry)
        issues.extend(entry_issues)
        line_index = max(consumed_until + 1, line_index + 1)

    return entries, issues


def parse_property_expression(
    lines: list[str],
    key: str,
    group: str,
    relative_path: str,
    property_line_index: int,
    getter_line_index: int,
    expression: str,
    section: str,
    resource_index: dict[str, dict[str, str]],
) -> tuple[TranslationEntry | None, list[ParseIssue], int]:
    issues: list[ParseIssue] = []
    expression = expression.strip()

    if WHEN_RE.match(expression):
        block_lines = [expression]
        brace_balance = brace_delta(expression)
        consumed_until = getter_line_index
        while brace_balance > 0:
            consumed_until += 1
            if consumed_until >= len(lines):
                issues.append(
                    ParseIssue(
                        source_file=relative_path,
                        line=property_line_index + 1,
                        key=key,
                        message="Unclosed when(lang) block.",
                    )
                )
                return None, issues, getter_line_index
            next_line = lines[consumed_until]
            block_lines.append(next_line)
            brace_balance += brace_delta(next_line)

        entry, block_issues = parse_when_block(
            key=key,
            group=group,
            relative_path=relative_path,
            property_line=property_line_index + 1,
            section=section,
            block_lines=block_lines,
            resource_index=resource_index,
        )
        issues.extend(block_issues)
        return entry, issues, consumed_until

    parsed_entry = build_entry_from_expression(
        key=key,
        group=group,
        relative_path=relative_path,
        property_line=property_line_index + 1,
        section=section,
        kind="shared-literal",
        expression=expression,
        resource_index=resource_index,
    )
    if isinstance(parsed_entry, ParseIssue):
        issues.append(parsed_entry)
        return None, issues, getter_line_index
    return parsed_entry, issues, getter_line_index


def parse_when_block(
    key: str,
    group: str,
    relative_path: str,
    property_line: int,
    section: str,
    block_lines: list[str],
    resource_index: dict[str, dict[str, str]],
) -> tuple[TranslationEntry | None, list[ParseIssue]]:
    issues: list[ParseIssue] = []
    entry = TranslationEntry(
        group=group,
        key=key,
        source_file=relative_path,
        line=property_line,
        section=section,
        kind="language-map",
    )

    explicit_languages = 0

    for raw_line in block_lines[1:]:
        stripped = raw_line.strip()
        if stripped == "}":
            continue

        lang_match = LANG_BRANCH_RE.match(raw_line)
        if lang_match:
            language_key = LANGUAGE_COLUMNS[lang_match.group(1)]
            branch_expression = lang_match.group(2).strip()
            branch_result = parse_expression_value(branch_expression, resource_index)
            if isinstance(branch_result, ParseIssue):
                branch_result.source_file = relative_path
                branch_result.line = property_line
                branch_result.key = key
                issues.append(branch_result)
                continue

            explicit_languages += 1
            if branch_result["kind"] == "literal":
                setattr(entry, language_key, branch_result["value"])
                setattr(entry, f"has_{language_key}", True)
            elif branch_result["kind"] == "resource":
                entry.resource_ref = branch_result["resource_ref"]
                fill_entry_from_resource(entry, branch_result["resource_ref"], resource_index)
                entry.kind = "resource-backed"
            continue

        else_match = ELSE_BRANCH_RE.match(raw_line)
        if else_match:
            branch_expression = else_match.group(1).strip()
            branch_result = parse_expression_value(branch_expression, resource_index)
            if isinstance(branch_result, ParseIssue):
                branch_result.source_file = relative_path
                branch_result.line = property_line
                branch_result.key = key
                issues.append(branch_result)
                continue
            if branch_result["kind"] == "resource":
                entry.kind = "resource-backed"
                entry.resource_ref = branch_result["resource_ref"]
                fill_entry_from_resource(entry, branch_result["resource_ref"], resource_index)
            continue

    if explicit_languages == 0 and entry.kind != "resource-backed":
        issues.append(
            ParseIssue(
                source_file=relative_path,
                line=property_line,
                key=key,
                message="when(lang) block did not contain any supported language branches.",
            )
        )
        return None, issues

    append_placeholder_notes(entry)
    return entry, issues


def build_entry_from_expression(
    key: str,
    group: str,
    relative_path: str,
    property_line: int,
    section: str,
    kind: str,
    expression: str,
    resource_index: dict[str, dict[str, str]],
) -> TranslationEntry | ParseIssue:
    value = parse_expression_value(expression, resource_index)
    if isinstance(value, ParseIssue):
        value.source_file = relative_path
        value.line = property_line
        value.key = key
        return value

    entry = TranslationEntry(
        group=group,
        key=key,
        source_file=relative_path,
        line=property_line,
        section=section,
        kind=kind,
    )

    if value["kind"] == "literal":
        entry.chinese = value["value"]
        entry.english = value["value"]
        entry.arabic = value["value"]
        entry.has_chinese = True
        entry.has_english = True
        entry.has_arabic = True
        entry.notes = "Shared literal getter. Keep product terms, placeholders, and punctuation unchanged unless the feature owner requests otherwise."
        return entry

    entry.kind = "resource-backed"
    entry.resource_ref = value["resource_ref"]
    fill_entry_from_resource(entry, value["resource_ref"], resource_index)
    entry.notes = "Android string resource-backed entry. Update the matching strings.xml locale files instead of Strings.kt."
    append_placeholder_notes(entry)
    return entry


def parse_expression_value(
    expression: str,
    resource_index: dict[str, dict[str, str]],
) -> dict[str, str] | ParseIssue:
    expression = expression.strip()

    literal = parse_kotlin_string(expression)
    if literal is not None:
        return {"kind": "literal", "value": literal}

    resource_match = RESOURCE_RE.search(expression)
    if resource_match:
        return {"kind": "resource", "resource_ref": resource_match.group(1)}

    if expression.startswith('"""'):
        return ParseIssue("", 0, "", "Triple-quoted Kotlin strings are not supported by the exporter yet.")

    return ParseIssue("", 0, "", f"Unsupported getter expression: {expression}")


def parse_kotlin_string(expression: str) -> str | None:
    if not expression.startswith('"') or expression.startswith('"""'):
        return None

    result: list[str] = []
    index = 1
    escaping = False

    while index < len(expression):
        char = expression[index]
        if escaping:
            if char == "n":
                result.append("\n")
            elif char == "t":
                result.append("\t")
            elif char == "r":
                result.append("\r")
            elif char == "b":
                result.append("\b")
            elif char == "$":
                result.append("$")
            elif char == "\\":
                result.append("\\")
            elif char == '"':
                result.append('"')
            elif char == "u":
                hex_digits = expression[index + 1 : index + 5]
                if len(hex_digits) != 4 or not all(ch in "0123456789abcdefABCDEF" for ch in hex_digits):
                    return None
                result.append(chr(int(hex_digits, 16)))
                index += 4
            else:
                result.append(char)
            escaping = False
            index += 1
            continue

        if char == "\\":
            escaping = True
            index += 1
            continue

        if char == '"':
            if expression[index + 1 :].strip():
                return None
            return "".join(result)

        result.append(char)
        index += 1

    return None


def fill_entry_from_resource(
    entry: TranslationEntry,
    resource_ref: str,
    resource_index: dict[str, dict[str, str]],
) -> None:
    chinese_values = resource_index.get("chinese", {})
    english_values = resource_index.get("english", {})
    arabic_values = resource_index.get("arabic", {})

    entry.has_chinese = resource_ref in chinese_values
    entry.has_english = resource_ref in english_values
    entry.has_arabic = resource_ref in arabic_values

    entry.chinese = chinese_values.get(resource_ref, "")
    entry.english = english_values.get(resource_ref, "")
    entry.arabic = arabic_values.get(resource_ref, "")

    if not entry.notes:
        entry.notes = "Android string resource-backed entry."


def load_resource_index() -> dict[str, dict[str, str]]:
    resource_index: dict[str, dict[str, str]] = {
        "chinese": {},
        "english": {},
        "arabic": {},
    }

    for language, folders in RESOURCE_LOCALES.items():
        for folder_name in folders:
            file_path = RES_DIR / folder_name / "strings.xml"
            if not file_path.exists():
                continue
            for key, value in parse_android_string_file(file_path).items():
                resource_index[language].setdefault(key, value)

    return resource_index


def parse_android_string_file(file_path: Path) -> dict[str, str]:
    try:
        root = ET.fromstring(file_path.read_text(encoding="utf-8"))
    except ET.ParseError:
        return {}

    values: dict[str, str] = {}
    for element in root.findall("string"):
        name = element.attrib.get("name")
        if not name:
            continue
        values[name] = "".join(element.itertext()).strip()
    return values


def find_getter_expression(lines: list[str], start_index: int) -> tuple[int, str] | None:
    cursor = start_index
    while cursor < len(lines):
        line = lines[cursor]
        if not line.strip():
            cursor += 1
            continue
        getter_match = GETTER_RE.match(line)
        if getter_match:
            return cursor, getter_match.group(1).strip()
        break
    return None


def write_catalog(entries: Iterable[TranslationEntry], output_dir: Path, format_name: str) -> None:
    if format_name in {"csv", "all"}:
        csv_path = output_dir / "i18n_catalog.csv"
        with csv_path.open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(
                handle,
                fieldnames=EXPORT_FIELD_NAMES,
            )
            writer.writeheader()
            for entry in entries:
                writer.writerow(export_entry(entry))
        print(f"Wrote {display_path(csv_path)}")

    if format_name in {"json", "all"}:
        json_path = output_dir / "i18n_catalog.json"
        payload = [export_entry(entry) for entry in entries]
        json_path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"Wrote {display_path(json_path)}")


def count_missing_language_cells(entries: Iterable[TranslationEntry]) -> int:
    missing = 0
    for entry in entries:
        if not entry.has_chinese:
            missing += 1
        if not entry.has_english:
            missing += 1
        if not entry.has_arabic:
            missing += 1
    return missing


def export_entry(entry: TranslationEntry) -> dict[str, str | int]:
    return {field_name: getattr(entry, field_name) for field_name in EXPORT_FIELD_NAMES}


def append_placeholder_notes(entry: TranslationEntry) -> None:
    placeholders = find_placeholders(entry.chinese, entry.english, entry.arabic)
    if not placeholders:
        return

    placeholder_note = f"Keep placeholders unchanged: {', '.join(placeholders)}"
    if entry.notes:
        if placeholder_note not in entry.notes:
            entry.notes = f"{entry.notes} {placeholder_note}"
        return
    entry.notes = placeholder_note


def find_placeholders(*values: str) -> list[str]:
    found: list[str] = []
    seen: set[str] = set()
    for value in values:
        for match in PLACEHOLDER_RE.findall(value):
            if match in seen:
                continue
            seen.add(match)
            found.append(match)
    return found


def clean_section(raw_section: str) -> str:
    return raw_section.strip().strip("=") or "General"


def display_path(path: Path) -> str:
    try:
        return path.resolve().relative_to(ROOT_DIR).as_posix()
    except ValueError:
        return path.as_posix()


def brace_delta(text: str) -> int:
    delta = 0
    in_string = False
    escaping = False
    index = 0

    while index < len(text):
        char = text[index]

        if not in_string and char == "/" and index + 1 < len(text) and text[index + 1] == "/":
            break

        if escaping:
            escaping = False
            index += 1
            continue

        if char == "\\":
            escaping = True
            index += 1
            continue

        if char == '"':
            in_string = not in_string
            index += 1
            continue

        if in_string:
            index += 1
            continue

        if char == "{":
            delta += 1
        elif char == "}":
            delta -= 1

        index += 1

    return delta


if __name__ == "__main__":
    sys.exit(main())
