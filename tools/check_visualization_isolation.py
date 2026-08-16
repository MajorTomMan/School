#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "visualization" / "src" / "main" / "java"
BUILD_FILE = ROOT / "visualization" / "build.gradle.kts"

FORBIDDEN_IMPORT_PREFIXES = (
    "android.app",
    "android.bluetooth",
    "android.content",
    "android.database",
    "android.hardware",
    "android.location",
    "android.media",
    "android.net",
    "android.nfc",
    "android.os",
    "android.provider",
    "android.telephony",
    "android.view",
    "android.webkit",
    "android.widget",
    "androidx.activity",
    "androidx.compose.ui.platform",
    "androidx.compose.ui.viewinterop",
    "androidx.datastore",
    "androidx.lifecycle",
    "androidx.room",
    "androidx.work",
    "dalvik.system",
    "java.io",
    "java.lang.reflect",
    "java.net",
    "java.nio.file",
    "kotlin.io",
    "kotlin.reflect",
    "kotlinx.coroutines",
    "kotlinx.serialization",
    "okhttp3",
    "org.json",
    "retrofit2",
    "com.google.firebase",
    "com.majortomman.school.BuildConfig",
    "com.majortomman.school.ai",
    "com.majortomman.school.data",
    "com.majortomman.school.learning",
    "com.majortomman.school.network",
    "com.majortomman.school.startup",
    "com.majortomman.school.ui",
    "com.majortomman.school.update",
)

FORBIDDEN_TOKENS = (
    "http://",
    "https://",
    "content://",
    "file://",
    "AndroidView",
    "Class.forName",
    "ClassLoader",
    "ContentResolver",
    "DataStore",
    "DexClassLoader",
    "LocalContext",
    "LocalView",
    "PathClassLoader",
    "ProcessBuilder",
    "RoomDatabase",
    "Runtime.getRuntime",
    "SQLiteDatabase",
    "SharedPreferences",
    "System.getenv",
    "System.getProperties",
    "System.getProperty",
    "WebView",
    "assets.open",
    "getSharedPreferences",
    "nativeCanvas",
    "openFileInput",
    "openFileOutput",
)

ALLOWED_PLUGIN_IDS = {
    "com.android.library",
    "org.jetbrains.kotlin.plugin.compose",
}

ALLOWED_DEPENDENCIES = {
    "androidx.compose:compose-bom:2026.06.00",
    "androidx.compose.foundation:foundation",
    "androidx.compose.material3:material3",
    "androidx.compose.ui:ui",
    "androidx.compose.ui:ui-tooling-preview",
    "androidx.compose.ui:ui-tooling",
    "junit:junit:4.13.2",
}

IMPORT_PATTERN = re.compile(r"(?m)^\s*import\s+([^\s]+)")
PLUGIN_PATTERN = re.compile(r'id\("([^"]+)"\)')
DEPENDENCY_PATTERN = re.compile(r'^\s*(implementation|api|compileOnly|runtimeOnly|debugImplementation|testImplementation)\((.+)\)\s*$')
STRING_ARGUMENT_PATTERN = re.compile(r'^"([^"]+)"$')


def validate_build_file() -> list[str]:
    if not BUILD_FILE.is_file():
        return [f"missing build file: {BUILD_FILE.relative_to(ROOT)}"]
    violations: list[str] = []
    text = BUILD_FILE.read_text(encoding="utf-8")
    plugins = set(PLUGIN_PATTERN.findall(text))
    unexpected_plugins = plugins - ALLOWED_PLUGIN_IDS
    if unexpected_plugins:
        violations.append(f"{BUILD_FILE.relative_to(ROOT)}: unexpected plugins {sorted(unexpected_plugins)}")
    if "project(" in text or "files(" in text or "fileTree(" in text:
        violations.append(f"{BUILD_FILE.relative_to(ROOT)}: project/file dependencies are not allowed")

    for line_number, line in enumerate(text.splitlines(), start=1):
        match = DEPENDENCY_PATTERN.match(line)
        if match is None:
            continue
        argument = match.group(2).strip()
        if argument == "composeBom":
            continue
        string_match = STRING_ARGUMENT_PATTERN.match(argument)
        if string_match is None:
            violations.append(f"{BUILD_FILE.relative_to(ROOT)}:{line_number}: dependency expression must be allowlisted: {argument}")
            continue
        dependency = string_match.group(1)
        if dependency not in ALLOWED_DEPENDENCIES:
            violations.append(f"{BUILD_FILE.relative_to(ROOT)}:{line_number}: dependency is not allowlisted: {dependency}")
    return violations


def main() -> int:
    if not SOURCE_ROOT.is_dir():
        raise SystemExit(f"missing visualization source root: {SOURCE_ROOT}")
    violations = validate_build_file()
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
                violations.append(f"{relative}: forbidden visualization token {token!r}")

    if violations:
        raise SystemExit("visualization isolation check failed:\n" + "\n".join(f"- {item}" for item in violations))
    print(f"visualization isolation check passed: {len(source_files)} Kotlin files and allowlisted module dependencies")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
