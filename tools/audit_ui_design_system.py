#!/usr/bin/env python3
"""Audit Compose UI files for design-system migration debt.

This script is intentionally non-failing by default. It reports legacy patterns
so migrations can reduce the count over time without blocking unrelated work.
Use --enforce-baseline with tools/ui_design_allowlist.txt to block new debt
while allowing documented historical debt to remain during migration.
"""

from __future__ import annotations

import argparse
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_TARGETS = (
    REPO_ROOT / "app/src/main/java/com/webtoapp/ui/screens",
    REPO_ROOT / "app/src/main/java/com/webtoapp/ui/components",
)
DEFAULT_ALLOWLIST = REPO_ROOT / "tools/ui_design_allowlist.txt"


@dataclass(frozen=True)
class Rule:
    key: str
    label: str
    pattern: re.Pattern[str]


RULES = (
    Rule(
        key="hardcoded_color",
        label="Direct Color(0x...)",
        pattern=re.compile(r"\bColor\s*\(\s*0x[0-9A-Fa-f_]+"),
    ),
    Rule(
        key="raw_corner_shape",
        label="Raw RoundedCornerShape(dp)",
        pattern=re.compile(r"\bRoundedCornerShape\s*\(\s*\d+(?:\.\d+)?\.dp"),
    ),
    Rule(
        key="raw_card",
        label="Raw Card/ElevatedCard/OutlinedCard",
        pattern=re.compile(r"(?<!Enhanced)(?<!Wta)\b(?:Card|ElevatedCard|OutlinedCard)\s*\("),
    ),
    Rule(
        key="raw_surface",
        label="Raw Surface",
        pattern=re.compile(r"(?<!Wta)\bSurface\s*\("),
    ),
    Rule(
        key="raw_button",
        label="Raw Button",
        pattern=re.compile(r"(?<!Icon)(?<!Text)(?<!Outlined)(?<!Tonal)(?<!Premium)(?<!Wta)\bButton\s*\("),
    ),
)


def iter_kotlin_files(target: Path) -> list[Path]:
    if target.is_file():
        return [target]
    return sorted(target.rglob("*.kt"))


def resolve_target(target_text: str) -> Path:
    target = Path(target_text)
    if not target.is_absolute():
        target = REPO_ROOT / target
    return target


def display_targets(targets: list[Path]) -> str:
    labels = []
    for target in targets:
        labels.append(
            str(target.relative_to(REPO_ROOT))
            if target.is_relative_to(REPO_ROOT)
            else str(target)
        )
    return ", ".join(labels)


def strip_line_comment(line: str) -> str:
    return line.split("//", 1)[0]


def audit_file(path: Path) -> dict[str, list[int]]:
    findings: dict[str, list[int]] = defaultdict(list)
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        lines = path.read_text(errors="ignore").splitlines()

    for line_no, line in enumerate(lines, start=1):
        candidate = strip_line_comment(line)
        for rule in RULES:
            if rule.pattern.search(candidate):
                findings[rule.key].append(line_no)
    return findings


def relative_repo_path(path: Path) -> str:
    if path.is_relative_to(REPO_ROOT):
        return path.relative_to(REPO_ROOT).as_posix()
    return path.as_posix()


def load_allowlist(path: Path) -> dict[str, Counter[str]]:
    allowlist: dict[str, Counter[str]] = defaultdict(Counter)
    if not path.exists():
        return allowlist

    for line_no, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|")
        if len(parts) != 3:
            raise ValueError(f"Invalid allowlist line {line_no}: {raw_line}")
        rel_path, rule_key, count_text = parts
        if rule_key not in {rule.key for rule in RULES}:
            raise ValueError(f"Unknown rule '{rule_key}' on allowlist line {line_no}")
        try:
            count = int(count_text)
        except ValueError as exc:
            raise ValueError(f"Invalid count '{count_text}' on allowlist line {line_no}") from exc
        if count < 0:
            raise ValueError(f"Negative count on allowlist line {line_no}")
        allowlist[rel_path][rule_key] = count
    return allowlist


def write_allowlist(path: Path, current_counts: dict[str, Counter[str]]) -> None:
    lines = [
        "# WebToApp UI design-system historical debt baseline.",
        "# Format: relative/path.kt|rule_key|allowed_count",
        "# Regenerate only after intentionally accepting a new migration checkpoint.",
    ]
    for rel_path in sorted(current_counts):
        counts = current_counts[rel_path]
        for rule in RULES:
            count = counts[rule.key]
            if count > 0:
                lines.append(f"{rel_path}|{rule.key}|{count}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def find_baseline_violations(
    current_counts: dict[str, Counter[str]],
    allowlist: dict[str, Counter[str]],
) -> list[tuple[str, str, int, int]]:
    violations: list[tuple[str, str, int, int]] = []
    for rel_path in sorted(current_counts):
        counts = current_counts[rel_path]
        allowed = allowlist.get(rel_path, Counter())
        for rule in RULES:
            current = counts[rule.key]
            baseline = allowed[rule.key]
            if current > baseline:
                violations.append((rel_path, rule.key, current, baseline))
    return violations


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "targets",
        nargs="*",
        help="Files or directories to scan. Defaults to app ui/screens and ui/components.",
    )
    parser.add_argument(
        "--fail-on-findings",
        action="store_true",
        help="Exit with status 1 if any finding is detected.",
    )
    parser.add_argument(
        "--enforce-baseline",
        action="store_true",
        help="Exit with status 1 only when current findings exceed the allowlist baseline.",
    )
    parser.add_argument(
        "--allowlist",
        default=str(DEFAULT_ALLOWLIST),
        help="Baseline allowlist file for --enforce-baseline.",
    )
    parser.add_argument(
        "--write-allowlist",
        help="Write the current finding counts as an allowlist baseline.",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=20,
        help="Number of highest-debt files to print.",
    )
    args = parser.parse_args()

    targets = [resolve_target(target) for target in args.targets] if args.targets else list(DEFAULT_TARGETS)

    files = sorted(
        {file_path.resolve() for target in targets for file_path in iter_kotlin_files(target)}
    )
    totals: Counter[str] = Counter()
    per_file: list[tuple[Path, Counter[str], dict[str, list[int]]]] = []
    current_counts: dict[str, Counter[str]] = {}

    for file_path in files:
        findings = audit_file(file_path)
        counts = Counter({key: len(lines) for key, lines in findings.items()})
        if counts:
            totals.update(counts)
            per_file.append((file_path, counts, findings))
            current_counts[relative_repo_path(file_path)] = counts

    print("WebToApp UI design-system audit")
    print(f"Targets: {display_targets(targets)}")
    print(f"Files scanned: {len(files)}")
    print()

    if args.write_allowlist:
        write_path = Path(args.write_allowlist)
        if not write_path.is_absolute():
            write_path = REPO_ROOT / write_path
        write_allowlist(write_path, current_counts)
        print(f"Allowlist written: {relative_repo_path(write_path)}")
        print()

    if not totals:
        print("No findings.")
        return 0

    print("Totals:")
    for rule in RULES:
        print(f"  {rule.label}: {totals[rule.key]}")

    print()
    print(f"Top {args.top} files:")
    per_file.sort(key=lambda item: sum(item[1].values()), reverse=True)
    for file_path, counts, findings in per_file[: args.top]:
        rel = file_path.relative_to(REPO_ROOT)
        total = sum(counts.values())
        detail = ", ".join(
            f"{rule.key}={counts[rule.key]}"
            for rule in RULES
            if counts[rule.key]
        )
        first_lines = []
        for rule in RULES:
            lines = findings.get(rule.key)
            if lines:
                first_lines.append(f"{rule.key}@{','.join(map(str, lines[:5]))}")
        print(f"  {rel}: {total} ({detail})")
        print(f"    first: {'; '.join(first_lines)}")

    if args.enforce_baseline:
        allowlist_path = Path(args.allowlist)
        if not allowlist_path.is_absolute():
            allowlist_path = REPO_ROOT / allowlist_path
        allowlist = load_allowlist(allowlist_path)
        violations = find_baseline_violations(current_counts, allowlist)
        print()
        if violations:
            print("Baseline enforcement failed:")
            for rel_path, rule_key, current, baseline in violations[:50]:
                print(f"  {rel_path}|{rule_key}: current={current}, allowed={baseline}")
            return 1
        print("Baseline enforcement passed.")

    return 1 if args.fail_on_findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
