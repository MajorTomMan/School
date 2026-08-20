#!/usr/bin/env python3
"""Thin GitHub Actions wrapper for scripts/course_r2_manager.py.

The workflow stays declarative: this script reads workflow_dispatch inputs from
GITHUB_EVENT_PATH, validates CI-only concerns, prepares temporary source files,
and delegates every storage operation to course_r2_manager.py.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import stat
import subprocess
import sys
from typing import Any
from urllib.parse import urlparse
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MANAGER = ROOT / "scripts" / "course_r2_manager.py"
INPUT_DIR = ROOT / "build" / "r2-input"
OUTPUT_DIR = ROOT / "build" / "r2-output"
ACTIONS = {
    "list", "read", "create", "update", "delete",
    "dir-list", "dir-read", "dir-create", "dir-update", "dir-delete",
    "purge", "release-upload", "publish",
}


def parse_bool(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    return str(value or "").strip().lower() in {"1", "true", "yes", "on"}


def load_event_inputs() -> dict[str, Any]:
    event_path = os.environ.get("GITHUB_EVENT_PATH", "").strip()
    if not event_path:
        return {}
    path = Path(event_path)
    if not path.is_file():
        raise SystemExit(f"GITHUB_EVENT_PATH not found: {path}")
    payload = json.loads(path.read_text(encoding="utf-8"))
    inputs = payload.get("inputs")
    return inputs if isinstance(inputs, dict) else {}


def parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="GitHub Actions wrapper for course_r2_manager.py")
    p.add_argument("--action", choices=sorted(ACTIONS))
    p.add_argument("--backend", choices=("worker", "direct"))
    p.add_argument("--target")
    p.add_argument("--source-url")
    p.add_argument("--release-id")
    p.add_argument("--channel", choices=("none", "testing", "stable"))
    p.add_argument("--recursive")
    p.add_argument("--delete-extra")
    p.add_argument("--allow-release-mutation")
    p.add_argument("--confirm-destructive")
    return p


def resolve_inputs() -> argparse.Namespace:
    args = parser().parse_args()
    event = load_event_inputs()
    defaults = {
        "action": "list",
        "backend": "worker",
        "target": "",
        "source_url": "",
        "release_id": "",
        "channel": "none",
        "recursive": True,
        "delete_extra": False,
        "allow_release_mutation": False,
        "confirm_destructive": False,
    }
    for name, default in defaults.items():
        cli_value = getattr(args, name)
        event_value = event.get(name, event.get(name.replace("_", "-")))
        value = cli_value if cli_value is not None else event_value
        setattr(args, name, default if value is None else value)
    for name in ("recursive", "delete_extra", "allow_release_mutation", "confirm_destructive"):
        setattr(args, name, parse_bool(getattr(args, name)))
    if args.action not in ACTIONS:
        raise SystemExit(f"unsupported action: {args.action}")
    if args.backend not in {"worker", "direct"}:
        raise SystemExit(f"unsupported backend: {args.backend}")
    return args


def require(value: str, label: str, action: str) -> str:
    value = (value or "").strip()
    if not value:
        raise SystemExit(f"{action} requires {label}")
    return value


def require_source_url(args: argparse.Namespace) -> str:
    value = require(args.source_url, "source_url", args.action)
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise SystemExit("source_url must use http:// or https://")
    return value


def require_course_config() -> None:
    missing = [name for name in ("COURSE_BASE_URL", "COURSE_API_TOKEN") if not os.environ.get(name, "").strip()]
    if missing:
        raise SystemExit(f"missing course configuration: {', '.join(missing)}")


def require_direct_config() -> None:
    missing = [name for name in ("R2_ACCOUNT_ID", "R2_ACCESS_KEY_ID", "R2_BUCKET_NAME", "R2_SECRET_ACCESS_KEY") if not os.environ.get(name, "").strip()]
    if missing:
        raise SystemExit(f"missing direct R2 configuration: {', '.join(missing)}")


def ensure_boto3() -> None:
    try:
        __import__("boto3")
        return
    except ImportError:
        pass
    if os.environ.get("GITHUB_ACTIONS", "").lower() != "true":
        raise SystemExit("direct R2 backend requires boto3: python -m pip install boto3")
    subprocess.run([sys.executable, "-m", "pip", "install", "--disable-pip-version-check", "--quiet", "boto3"], check=True)


def prepare() -> None:
    shutil.rmtree(INPUT_DIR, ignore_errors=True)
    shutil.rmtree(OUTPUT_DIR, ignore_errors=True)
    INPUT_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)


def run(command: list[str], *, output: Path | None = None) -> str:
    result = subprocess.run(command, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    text = result.stdout or ""
    if text:
        print(text, end="" if text.endswith("\n") else "\n")
    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(text, encoding="utf-8")
    if result.returncode:
        raise SystemExit(result.returncode)
    return text


def manager(*args: str, output: Path | None = None) -> str:
    return run([sys.executable, str(MANAGER), *args], output=output)


def curl_download(url: str, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    run(["curl", "--fail", "--location", "--retry", "4", "--retry-delay", "2", "--silent", "--show-error", "--output", str(output), url])


def download_object_source(args: argparse.Namespace) -> Path:
    url = require_source_url(args)
    name = PurePosixPath(args.target or "").name or "object.bin"
    output = INPUT_DIR / name
    curl_download(url, output)
    return output


def safe_extract_zip(archive: Path, output: Path) -> None:
    shutil.rmtree(output, ignore_errors=True)
    output.mkdir(parents=True, exist_ok=True)
    try:
        with zipfile.ZipFile(archive) as source:
            for item in source.infolist():
                path = PurePosixPath(item.filename.replace("\\", "/"))
                if path.is_absolute() or ".." in path.parts:
                    raise SystemExit(f"ZIP contains unsafe path: {item.filename}")
                mode = (item.external_attr >> 16) & 0o170000
                if mode == stat.S_IFLNK:
                    raise SystemExit(f"ZIP symlink is not allowed: {item.filename}")
            source.extractall(output)
    except zipfile.BadZipFile as error:
        raise SystemExit(f"source_url did not return a valid ZIP: {archive}") from error


def extract_zip_source(args: argparse.Namespace) -> Path:
    archive = INPUT_DIR / "source.zip"
    output = INPUT_DIR / "source"
    curl_download(require_source_url(args), archive)
    safe_extract_zip(archive, output)
    return output


def resolve_release_root(root: Path) -> Path:
    if (root / "manifest.json").is_file():
        return root
    candidates = [path.parent for path in root.glob("*/manifest.json") if path.is_file()]
    if len(candidates) == 1:
        return candidates[0]
    raise SystemExit("release ZIP root or its only first-level directory must contain manifest.json")


def storage_flags(args: argparse.Namespace) -> list[str]:
    return ["--backend", args.backend]


def release_flags(args: argparse.Namespace) -> list[str]:
    return ["--allow-release-mutation"] if args.allow_release_mutation else []


def validate_backend(args: argparse.Namespace) -> None:
    if args.action in {"release-upload", "publish"}:
        require_course_config()
        return
    if args.backend == "worker":
        require_course_config()
    else:
        require_direct_config()
        ensure_boto3()


def execute(args: argparse.Namespace) -> None:
    validate_backend(args)
    target = (args.target or "").strip()
    release_id = (args.release_id or "").strip()
    result_file = OUTPUT_DIR / "result.txt"
    list_file = OUTPUT_DIR / "list.json"

    if args.action == "release-upload":
        require(release_id, "release_id", args.action)
        if args.channel == "stable":
            raise SystemExit("release-upload cannot publish stable directly; upload to none/testing, then use publish")
        root = resolve_release_root(extract_zip_source(args))
        manager("release-upload", "--root", str(root), "--release-id", release_id, "--channel", args.channel, output=result_file)
        return

    if args.action == "publish":
        require(release_id, "release_id", args.action)
        if args.channel == "none":
            raise SystemExit("publish channel must be testing or stable")
        manager("publish", "--release-id", release_id, "--channel", args.channel, output=result_file)
        return

    if args.action == "list":
        manager("list", target, "--json", *storage_flags(args), output=list_file)
        return

    if args.action == "read":
        require(target, "target", args.action)
        output = OUTPUT_DIR / (PurePosixPath(target).name or "object.bin")
        manager("read", target, "--output", str(output), "--force", *storage_flags(args), output=result_file)
        return

    if args.action in {"create", "update"}:
        require(target, "target", args.action)
        source = download_object_source(args)
        command = [args.action, str(source), target, *storage_flags(args)]
        if args.action == "update":
            command.extend(release_flags(args))
        manager(*command, output=result_file)
        return

    if args.action == "delete":
        require(target, "target", args.action)
        if not args.confirm_destructive:
            raise SystemExit("delete requires confirm_destructive=true")
        manager("delete", target, "--yes", *storage_flags(args), *release_flags(args), output=result_file)
        return

    if args.action == "dir-list":
        require(target, "target", args.action)
        command = ["dir-read", target, "--json", *storage_flags(args)]
        if args.recursive:
            command.append("--recursive")
        manager(*command, output=list_file)
        return

    if args.action == "dir-read":
        require(target, "target", args.action)
        manager("dir-read", target, "--output", str(OUTPUT_DIR / "directory"), "--force", *storage_flags(args), output=result_file)
        return

    if args.action == "dir-create":
        require(target, "target", args.action)
        command = ["dir-create", target, *storage_flags(args)]
        if (args.source_url or "").strip():
            command.extend(["--from", str(extract_zip_source(args))])
        manager(*command, output=result_file)
        return

    if args.action == "dir-update":
        require(target, "target", args.action)
        source = extract_zip_source(args)
        command = ["dir-update", str(source), target, *storage_flags(args)]
        if args.delete_extra:
            command.append("--delete-extra")
        command.extend(release_flags(args))
        manager(*command, output=result_file)
        return

    if args.action == "dir-delete":
        require(target, "target", args.action)
        if not args.confirm_destructive:
            raise SystemExit("dir-delete requires confirm_destructive=true")
        manager("dir-delete", target, "--yes", *storage_flags(args), *release_flags(args), output=result_file)
        return

    if args.action == "purge":
        if args.backend != "worker":
            raise SystemExit("purge is Worker-only to prevent wiping a shared bucket")
        if not args.confirm_destructive:
            raise SystemExit("purge requires confirm_destructive=true")
        if not args.allow_release_mutation:
            raise SystemExit("purge requires allow_release_mutation=true")
        manager("purge", "--yes", "--allow-release-mutation", "--backend", "worker", output=result_file)
        return

    raise SystemExit(f"unsupported action: {args.action}")


def write_summary(args: argparse.Namespace) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY", "").strip()
    if not path:
        return
    lines = ["### R2 Storage Manager", f"- Action: `{args.action}`"]
    if args.action not in {"release-upload", "publish"}:
        lines.append(f"- Backend: `{args.backend}`")
    if (args.release_id or "").strip():
        lines.append(f"- Release: `{args.release_id.strip()}`")
    if (args.target or "").strip():
        lines.append(f"- Target: `{args.target.strip()}`")
    if args.action in {"release-upload", "publish"}:
        lines.append(f"- Channel: `{args.channel}`")
    if os.environ.get("GITHUB_RUN_ID", "").strip():
        lines.append(f"- Run: `{os.environ['GITHUB_RUN_ID']}`")
    with Path(path).open("a", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def main() -> int:
    args = resolve_inputs()
    prepare()
    try:
        execute(args)
    finally:
        write_summary(args)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("cancelled", file=sys.stderr)
        raise SystemExit(130)
