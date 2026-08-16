#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "visualization" / "src" / "main" / "java"

FORBIDDEN_IMPORT_PREFIXES = (
    "android.content.Context",
    "android.content.ContentResolver",
    "android.database",
    "android.net",
    "android.provider",
    "android.webkit",
    "androidx.datastore",
    "androidx.room",
    "java.io",
    "java.net",
    "java.nio.file",
    "kotlin.io",
    "kotlinx.coroutines",
    "okhttp3",
    "retrofit2",
    "com.google.firebase",
    "com.majortomman.school.learning",
    "com.majortomman.school.data",
    "com.majortomman.school.network",
    "com.majortomman.school.update",
)

FORBIDDEN_TOKENS = (
    "http://",
    "https://",
    "content://",
    "file://",
    "WebView",
    "ContentResolver",
    "DataStore",
    "RoomDatabase",
    "SQLiteDatabase",
)

IMPORT_PATTERN = re.compile(r"(?m)^\s*import\s+([^\s]+)")


def main() -> int:
    if not SOURCE_ROOT.is_dir():
        raise SystemExit(f"missing visualization source root: {SOURCE_ROOT}")
    violations: list[str] = []
    source_files = sorted(SOURCE_ROOT.rglob("*.kt"))
    if not source_files:
        raise SystemExit("visualization module contains no Kotlin sources")

    for path in source_files:
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(ROOT)
        for imported in IMPORT_PATTERN.findall(text):
            if imported.startswith(FORBIDDEN_IMPORT_PREFIXES):
                violations.append(f"{relative}: forbidden import {imported}")
        for token in FORBIDDEN_TOKENS:
            if token in text:
                violations.append(f"{relative}: forbidden external-data token {token!r}")

    if violations:
        raise SystemExit("visualization isolation check failed:\n" + "\n".join(f"- {item}" for item in violations))
    print(f"visualization isolation check passed: {len(source_files)} Kotlin files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
