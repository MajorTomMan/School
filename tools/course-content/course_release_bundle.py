#!/usr/bin/env python3
"""Shared deterministic bundle helpers for immutable course releases."""
from __future__ import annotations

import hashlib
from pathlib import Path, PurePosixPath
import re
import shutil
import zipfile

IDENTIFIER = re.compile(r"^[A-Za-z0-9._-]+$")
EXTENSION_FILES = ("assessments.json", "knowledge-points.json")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def safe_identifier(value: str, label: str) -> str:
    result = value.strip()
    if not IDENTIFIER.fullmatch(result):
        raise SystemExit(f"{label} is invalid: {result}")
    return result


def public_release_url(base_url: str, release_id: str, *parts: str) -> str:
    base = base_url.strip().rstrip("/")
    if not base.startswith("https://"):
        raise SystemExit("--public-base-url must use https://")
    encoded = "/".join(PurePosixPath(part).as_posix().strip("/") for part in parts)
    return f"{base}/releases/{release_id}/{encoded}"


def file_spec(path: str, source: Path, url: str, bundled: bool | None = None) -> dict[str, object]:
    result: dict[str, object] = {
        "path": path, "url": url, "size": source.stat().st_size, "sha256": sha256(source),
    }
    if bundled is not None:
        result["bundled"] = bundled
    return result


def copy_optional_extensions(source_root: Path, target_root: Path) -> None:
    present = [(source_root / name).is_file() for name in EXTENSION_FILES]
    if any(present) and not all(present):
        raise SystemExit(f"{source_root}: assessment extension files must appear together")
    for name in EXTENSION_FILES:
        source = source_root / name
        if source.is_file():
            shutil.copyfile(source, target_root / name)
    assets = source_root / "assets"
    if assets.is_dir():
        shutil.copytree(assets, target_root / "assets", dirs_exist_ok=True)


def collect_bundled_files(root: Path) -> dict[str, Path]:
    required = root / "course.json"
    if not required.is_file():
        raise SystemExit(f"missing course.json: {root}")
    files: dict[str, Path] = {"course.json": required}
    present = [(root / name).is_file() for name in EXTENSION_FILES]
    if any(present) and not all(present):
        raise SystemExit(f"{root}: assessment extension files must appear together")
    for name in EXTENSION_FILES:
        path = root / name
        if path.is_file():
            files[name] = path
    assets = root / "assets"
    if assets.is_dir():
        for path in sorted(assets.rglob("*")):
            if path.is_file():
                relative = path.relative_to(root).as_posix()
                if relative == "assets/textbook.pdf":
                    raise SystemExit("textbook PDF must remain an external manifest file")
                files[relative] = path
    return dict(sorted(files.items()))


def write_deterministic_zip(files: dict[str, Path], target: Path) -> None:
    with zipfile.ZipFile(target, "w", allowZip64=True) as archive:
        for relative, source in sorted(files.items()):
            info = zipfile.ZipInfo(relative, date_time=(2020, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, source.read_bytes())
