#!/usr/bin/env python3
"""Upload immutable School course releases through the course Worker and publish a channel."""

from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

IDENTIFIER = re.compile(r"^[A-Za-z0-9._-]+$")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_identifier(value: str, label: str) -> str:
    result = value.strip()
    if not IDENTIFIER.fullmatch(result):
        raise SystemExit(f"{label} is invalid: {result}")
    return result


def api_json(method: str, url: str, token: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {
        "Accept": "application/json",
        "Authorization": f"Bearer {token}",
        "User-Agent": "School-Course-Publisher/1.0",
    }
    if body is not None:
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            raw = response.read()
    except urllib.error.HTTPError as error:
        message = error.read().decode("utf-8", errors="replace")
        raise SystemExit(f"{method} {url} returned HTTP {error.code}: {message}") from error
    except urllib.error.URLError as error:
        raise SystemExit(f"cannot reach course Worker: {error}") from error
    if not raw:
        return {}
    decoded = json.loads(raw)
    if not isinstance(decoded, dict):
        raise SystemExit("course Worker returned a non-object JSON response")
    return decoded


def content_type(path: Path) -> str:
    explicit = {
        ".json": "application/json; charset=utf-8",
        ".zip": "application/zip",
        ".pdf": "application/pdf",
    }
    return explicit.get(path.suffix.lower()) or mimetypes.guess_type(path.name)[0] or "application/octet-stream"


def object_metadata(base_url: str, token: str, remote_path: str) -> dict[str, Any] | None:
    query = urllib.parse.urlencode({"path": remote_path})
    request = urllib.request.Request(
        f"{base_url}/cloud/course/object?{query}",
        method="GET",
        headers={
            "Accept": "application/json",
            "Authorization": f"Bearer {token}",
            "User-Agent": "School-Course-Publisher/1.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.loads(response.read())
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        message = error.read().decode("utf-8", errors="replace")
        raise SystemExit(f"metadata query failed with HTTP {error.code}: {message}") from error


def upload_file(base_url: str, token: str, local_path: Path, remote_path: str) -> None:
    size = local_path.stat().st_size
    digest = sha256(local_path)
    existing = object_metadata(base_url, token, remote_path)
    if existing is not None and existing.get("size") == size and existing.get("sha256") == digest:
        print(f"skip unchanged: {remote_path}")
        return

    media_type = content_type(local_path)
    signed = api_json(
        "POST",
        f"{base_url}/cloud/course/upload-url",
        token,
        {
            "path": remote_path,
            "size": size,
            "sha256": digest,
            "content_type": media_type,
            "expires_in": 1800,
        },
    )
    signed_url = signed.get("url")
    if not isinstance(signed_url, str) or not signed_url:
        raise SystemExit("course Worker did not return an upload URL")

    completed = subprocess.run(
        [
            "curl", "--fail-with-body", "--silent", "--show-error", "--retry", "3",
            "--retry-delay", "2", "--request", "PUT",
            "--header", f"Content-Type: {media_type}",
            "--header", f"Content-Length: {size}",
            "--upload-file", str(local_path), signed_url,
        ],
        text=True,
        capture_output=True,
    )
    if completed.returncode != 0:
        details = (completed.stderr or completed.stdout).strip()
        raise SystemExit(f"upload failed for {remote_path}: {details}")

    api_json(
        "POST",
        f"{base_url}/cloud/course/upload-complete",
        token,
        {"path": remote_path, "size": size, "sha256": digest},
    )
    print(f"uploaded: {remote_path} ({size} bytes)")


def verify_manifest_urls(root: Path, base_url: str, release_id: str) -> None:
    manifest_path = root / "manifest.json"
    if not manifest_path.is_file():
        raise SystemExit(f"missing manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if set(manifest) != {"textbooks"} or not isinstance(manifest["textbooks"], list):
        raise SystemExit("manifest.json does not use the current APK distribution contract")
    prefix = f"{base_url}/cloud/course/public/releases/{release_id}/"
    for textbook in manifest["textbooks"]:
        for spec in [textbook["package"], *textbook["files"]]:
            url = spec.get("url")
            if not isinstance(url, str) or not url.startswith(prefix):
                raise SystemExit(f"manifest URL is outside release {release_id}: {url}")


def publish_channel(base_url: str, token: str, release_id: str, channel: str) -> dict[str, Any]:
    manifest_path = f"releases/{release_id}/manifest.json"
    metadata = object_metadata(base_url, token, manifest_path)
    if metadata is None:
        raise SystemExit(f"release manifest does not exist: {manifest_path}")
    digest = metadata.get("sha256")
    if not isinstance(digest, str) or len(digest) != 64:
        raise SystemExit("release manifest metadata does not contain SHA-256")
    result = api_json(
        "POST",
        f"{base_url}/cloud/course/channel/publish",
        token,
        {"channel": channel, "source_path": manifest_path, "sha256": digest},
    )
    print(f"published {channel}: {result.get('publicUrl', '')}")
    return result


def upload_release(args: argparse.Namespace, base_url: str, token: str) -> None:
    release_id = require_identifier(args.release_id, "release id")
    root = args.root.resolve()
    if not root.is_dir():
        raise SystemExit(f"release directory not found: {root}")
    verify_manifest_urls(root, base_url, release_id)

    files = sorted(path for path in root.rglob("*") if path.is_file())
    files.sort(key=lambda path: path.name == "manifest.json")
    if not files:
        raise SystemExit("release directory is empty")
    for path in files:
        relative = PurePosixPath(path.relative_to(root).as_posix())
        upload_file(base_url, token, path, f"releases/{release_id}/{relative.as_posix()}")
    publish_channel(base_url, token, release_id, args.channel)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--base-url",
        default=os.environ.get("COURSE_BASE_URL", "https://course.flashnamesl.workers.dev"),
    )
    parser.add_argument("--api-token", default=os.environ.get("COURSE_API_TOKEN", ""))
    subparsers = parser.add_subparsers(dest="command", required=True)

    upload = subparsers.add_parser("upload")
    upload.add_argument("--root", type=Path, required=True)
    upload.add_argument("--release-id", required=True)
    upload.add_argument("--channel", choices=("testing", "stable"), default="testing")

    promote = subparsers.add_parser("promote")
    promote.add_argument("--release-id", required=True)
    promote.add_argument("--channel", choices=("testing", "stable"), default="stable")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    base_url = args.base_url.strip().rstrip("/")
    token = args.api_token.strip()
    if not base_url.startswith("https://") and not base_url.startswith("http://127.0.0.1"):
        raise SystemExit("course base URL must use HTTPS")
    if len(token) < 32:
        raise SystemExit("COURSE_API_TOKEN is missing or too short")
    if args.command == "upload":
        upload_release(args, base_url, token)
    else:
        release_id = require_identifier(args.release_id, "release id")
        publish_channel(base_url, token, release_id, args.channel)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
