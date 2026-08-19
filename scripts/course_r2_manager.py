#!/usr/bin/env python3
"""Cloudflare R2 CRUD and School course release manager."""

from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Iterable

IDENTIFIER = re.compile(r"^[A-Za-z0-9._-]+$")
DEFAULT_COURSE_BASE_URL = "https://course.flashnamesl.workers.dev"
RELEASE_PREFIX = "releases/"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalize_key(value: str) -> str:
    key = value.strip().replace("\\", "/").lstrip("/")
    if not key or key == ".":
        raise SystemExit("R2 object key cannot be empty")
    path = PurePosixPath(key)
    if ".." in path.parts:
        raise SystemExit(f"R2 object key cannot contain '..': {value}")
    return path.as_posix()


def normalize_prefix(value: str) -> str:
    prefix = normalize_key(value).rstrip("/")
    return prefix + "/"


def require_identifier(value: str, label: str) -> str:
    result = value.strip()
    if not IDENTIFIER.fullmatch(result):
        raise SystemExit(f"{label} is invalid: {result}")
    return result


def content_type(path: Path) -> str:
    explicit = {".json": "application/json; charset=utf-8", ".zip": "application/zip", ".pdf": "application/pdf"}
    return explicit.get(path.suffix.lower()) or mimetypes.guess_type(path.name)[0] or "application/octet-stream"


def print_json(value: Any) -> None:
    print(json.dumps(value, ensure_ascii=False, indent=2, default=str))


def require_boto3() -> Any:
    try:
        import boto3
    except ImportError as error:
        raise SystemExit("Direct R2 CRUD requires boto3: python -m pip install boto3") from error
    return boto3


class R2Client:
    def __init__(self, args: argparse.Namespace) -> None:
        boto3 = require_boto3()
        account_id = (args.r2_account_id or "").strip()
        access_key_id = (args.r2_access_key_id or "").strip()
        secret_access_key = (args.r2_secret_access_key or "").strip()
        self.bucket = (args.r2_bucket_name or "").strip()
        missing = [name for name, value in (("R2_ACCOUNT_ID", account_id), ("R2_ACCESS_KEY_ID", access_key_id), ("R2_SECRET_ACCESS_KEY", secret_access_key), ("R2_BUCKET_NAME", self.bucket)) if not value]
        if missing:
            raise SystemExit(f"Missing direct R2 configuration: {', '.join(missing)}")
        self.client = boto3.client("s3", endpoint_url=f"https://{account_id}.r2.cloudflarestorage.com", aws_access_key_id=access_key_id, aws_secret_access_key=secret_access_key, region_name="auto")

    def head(self, key: str) -> dict[str, Any] | None:
        try:
            return self.client.head_object(Bucket=self.bucket, Key=key)
        except self.client.exceptions.ClientError as error:
            status = error.response.get("ResponseMetadata", {}).get("HTTPStatusCode")
            code = error.response.get("Error", {}).get("Code")
            if status == 404 or code in {"404", "NoSuchKey", "NotFound"}:
                return None
            raise

    def remote_sha256(self, key: str, head: dict[str, Any] | None = None) -> str:
        head = head or self.head(key)
        if head is None:
            raise SystemExit(f"R2 object does not exist: {key}")
        saved = (head.get("Metadata") or {}).get("sha256")
        if isinstance(saved, str) and len(saved) == 64:
            return saved.lower()
        digest = hashlib.sha256()
        response = self.client.get_object(Bucket=self.bucket, Key=key)
        body = response["Body"]
        try:
            for chunk in iter(lambda: body.read(1024 * 1024), b""):
                digest.update(chunk)
        finally:
            body.close()
        return digest.hexdigest()

    def upload(self, local_path: Path, key: str, digest: str | None = None) -> None:
        if not local_path.is_file():
            raise SystemExit(f"local file not found: {local_path}")
        digest = digest or sha256_file(local_path)
        self.client.upload_file(str(local_path), self.bucket, key, ExtraArgs={"ContentType": content_type(local_path), "Metadata": {"sha256": digest}})

    def create(self, local_path: Path, key: str) -> None:
        if self.head(key) is not None:
            raise SystemExit(f"R2 object already exists: {key}. Use update instead.")
        self.upload(local_path, key)
        print(f"created: {key}")

    def describe(self, key: str) -> dict[str, Any]:
        head = self.head(key)
        if head is None:
            raise SystemExit(f"R2 object does not exist: {key}")
        return {"key": key, "size": head.get("ContentLength"), "content_type": head.get("ContentType"), "etag": str(head.get("ETag", "")).strip('"'), "last_modified": head.get("LastModified"), "sha256": (head.get("Metadata") or {}).get("sha256"), "metadata": head.get("Metadata") or {}}

    def download(self, key: str, output: Path, force: bool) -> None:
        if self.head(key) is None:
            raise SystemExit(f"R2 object does not exist: {key}")
        if output.exists() and not force:
            raise SystemExit(f"local file already exists: {output}. Use --force to overwrite it.")
        output.parent.mkdir(parents=True, exist_ok=True)
        self.client.download_file(self.bucket, key, str(output))
        print(f"downloaded: {key} -> {output}")

    def update(self, local_path: Path, key: str, allow_release_mutation: bool) -> None:
        head = self.head(key)
        if head is None:
            raise SystemExit(f"R2 object does not exist: {key}. Use create instead.")
        self.guard_release_mutation(key, allow_release_mutation)
        digest = sha256_file(local_path)
        if head.get("ContentLength") == local_path.stat().st_size and self.remote_sha256(key, head) == digest:
            print(f"unchanged: {key}")
            return
        self.upload(local_path, key, digest)
        print(f"updated: {key}")

    def delete(self, key: str, allow_release_mutation: bool) -> None:
        if self.head(key) is None:
            raise SystemExit(f"R2 object does not exist: {key}")
        self.guard_release_mutation(key, allow_release_mutation)
        self.client.delete_object(Bucket=self.bucket, Key=key)
        print(f"deleted: {key}")

    def list(self, prefix: str = "") -> Iterable[dict[str, Any]]:
        paginator = self.client.get_paginator("list_objects_v2")
        for page in paginator.paginate(Bucket=self.bucket, Prefix=prefix):
            for item in page.get("Contents", []):
                yield {"key": item["Key"], "size": item.get("Size"), "etag": str(item.get("ETag", "")).strip('"'), "last_modified": item.get("LastModified")}

    def directory_exists(self, prefix: str) -> bool:
        prefix = normalize_prefix(prefix)
        response = self.client.list_objects_v2(Bucket=self.bucket, Prefix=prefix, MaxKeys=1)
        return bool(response.get("Contents"))

    def create_dir(self, prefix: str, local_dir: Path | None) -> None:
        prefix = normalize_prefix(prefix)
        if self.directory_exists(prefix):
            raise SystemExit(f"R2 directory already exists: {prefix}. Use dir-update instead.")
        if local_dir is not None and not local_dir.is_dir():
            raise SystemExit(f"local directory not found: {local_dir}")
        self.client.put_object(Bucket=self.bucket, Key=prefix, Body=b"", ContentType="application/x-directory", Metadata={"r2-directory": "true"})
        count = 0
        if local_dir is not None:
            for local_path in sorted(path for path in local_dir.rglob("*") if path.is_file()):
                relative = PurePosixPath(local_path.relative_to(local_dir).as_posix())
                self.upload(local_path, prefix + relative.as_posix())
                count += 1
        print(f"created directory: {prefix} ({count} files)")

    def list_dir(self, prefix: str, recursive: bool) -> list[dict[str, Any]]:
        prefix = normalize_prefix(prefix)
        kwargs: dict[str, Any] = {"Bucket": self.bucket, "Prefix": prefix}
        if not recursive:
            kwargs["Delimiter"] = "/"
        rows: list[dict[str, Any]] = []
        paginator = self.client.get_paginator("list_objects_v2")
        for page in paginator.paginate(**kwargs):
            if not recursive:
                rows.extend({"type": "directory", "key": item["Prefix"]} for item in page.get("CommonPrefixes", []))
            for item in page.get("Contents", []):
                if item["Key"] != prefix:
                    rows.append({"type": "file", "key": item["Key"], "size": item.get("Size"), "etag": str(item.get("ETag", "")).strip('"'), "last_modified": item.get("LastModified")})
        rows.sort(key=lambda item: (item["type"] != "directory", item["key"]))
        return rows

    def download_dir(self, prefix: str, output: Path, force: bool) -> None:
        prefix = normalize_prefix(prefix)
        if not self.directory_exists(prefix):
            raise SystemExit(f"R2 directory does not exist: {prefix}")
        output.mkdir(parents=True, exist_ok=True)
        count = 0
        for item in self.list(prefix):
            if item["key"] == prefix:
                continue
            relative = PurePosixPath(item["key"][len(prefix):])
            target = output.joinpath(*relative.parts)
            if target.exists() and not force:
                raise SystemExit(f"local file already exists: {target}. Use --force to overwrite it.")
            target.parent.mkdir(parents=True, exist_ok=True)
            self.client.download_file(self.bucket, item["key"], str(target))
            count += 1
        print(f"downloaded directory: {prefix} -> {output} ({count} files)")

    def update_dir(self, local_dir: Path, prefix: str, delete_extra: bool, allow_release_mutation: bool) -> None:
        prefix = normalize_prefix(prefix)
        if not local_dir.is_dir():
            raise SystemExit(f"local directory not found: {local_dir}")
        if not self.directory_exists(prefix):
            raise SystemExit(f"R2 directory does not exist: {prefix}. Use dir-create instead.")
        self.guard_release_mutation(prefix, allow_release_mutation)
        local_files = {prefix + PurePosixPath(path.relative_to(local_dir).as_posix()).as_posix(): path for path in local_dir.rglob("*") if path.is_file()}
        remote_keys = {item["key"] for item in self.list(prefix) if item["key"] != prefix}
        created = updated = unchanged = 0
        for key, local_path in sorted(local_files.items()):
            head = self.head(key)
            if head is None:
                self.upload(local_path, key)
                created += 1
                continue
            digest = sha256_file(local_path)
            if head.get("ContentLength") == local_path.stat().st_size and self.remote_sha256(key, head) == digest:
                unchanged += 1
            else:
                self.guard_release_mutation(key, allow_release_mutation)
                self.upload(local_path, key, digest)
                updated += 1
        deleted = 0
        if delete_extra:
            stale = sorted(remote_keys - set(local_files))
            for key in stale:
                self.guard_release_mutation(key, allow_release_mutation)
            deleted = self.delete_keys(stale)
        print(f"updated directory: {prefix} (created={created}, updated={updated}, unchanged={unchanged}, deleted={deleted})")

    def delete_dir(self, prefix: str, allow_release_mutation: bool) -> None:
        prefix = normalize_prefix(prefix)
        keys = [item["key"] for item in self.list(prefix)]
        if not keys:
            print(f"(no objects under {prefix})")
            return
        for key in keys:
            self.guard_release_mutation(key, allow_release_mutation)
        deleted = self.delete_keys(keys)
        print(f"deleted directory: {prefix} ({deleted} objects)")

    def delete_keys(self, keys: list[str]) -> int:
        deleted = 0
        for index in range(0, len(keys), 1000):
            batch = keys[index:index + 1000]
            if not batch:
                continue
            result = self.client.delete_objects(Bucket=self.bucket, Delete={"Objects": [{"Key": key} for key in batch], "Quiet": True})
            errors = result.get("Errors", [])
            if errors:
                details = "; ".join(f"{item.get('Key')}: {item.get('Code')} {item.get('Message')}" for item in errors)
                raise SystemExit(f"bulk deletion partially failed: {details}")
            deleted += len(batch)
        return deleted

    @staticmethod
    def guard_release_mutation(key: str, allow: bool) -> None:
        if key.startswith(RELEASE_PREFIX) and not allow:
            raise SystemExit(f"refusing to mutate immutable release object: {key}\nPublish a new release id instead, or pass --allow-release-mutation for deliberate recovery work.")


class CourseWorkerClient:
    def __init__(self, base_url: str, token: str) -> None:
        self.base_url = base_url.strip().rstrip("/")
        self.token = token.strip()
        if not self.base_url.startswith("https://") and not self.base_url.startswith("http://127.0.0.1"):
            raise SystemExit("course base URL must use HTTPS")
        if len(self.token) < 32:
            raise SystemExit("COURSE_API_TOKEN is missing or too short")

    def api_json(self, method: str, path: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers = {"Accept": "application/json", "Authorization": f"Bearer {self.token}", "User-Agent": "School-Course-R2-Manager/2.0"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(f"{self.base_url}{path}", data=body, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                raw = response.read()
        except urllib.error.HTTPError as error:
            message = error.read().decode("utf-8", errors="replace")
            raise SystemExit(f"{method} {path} returned HTTP {error.code}: {message}") from error
        except urllib.error.URLError as error:
            raise SystemExit(f"cannot reach course Worker: {error}") from error
        if not raw:
            return {}
        decoded = json.loads(raw)
        if not isinstance(decoded, dict):
            raise SystemExit("course Worker returned a non-object JSON response")
        return decoded

    def object_metadata(self, remote_path: str) -> dict[str, Any] | None:
        query = urllib.parse.urlencode({"path": remote_path})
        request = urllib.request.Request(f"{self.base_url}/cloud/course/object?{query}", method="GET", headers={"Accept": "application/json", "Authorization": f"Bearer {self.token}", "User-Agent": "School-Course-R2-Manager/2.0"})
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.loads(response.read())
        except urllib.error.HTTPError as error:
            if error.code == 404:
                return None
            message = error.read().decode("utf-8", errors="replace")
            raise SystemExit(f"metadata query failed with HTTP {error.code}: {message}") from error

    def upload_file(self, local_path: Path, remote_path: str) -> None:
        size = local_path.stat().st_size
        digest = sha256_file(local_path)
        existing = self.object_metadata(remote_path)
        if existing is not None:
            if existing.get("size") == size and existing.get("sha256") == digest:
                print(f"skip unchanged: {remote_path}")
                return
            raise SystemExit(f"immutable release object already exists with different content: {remote_path}\nCreate a new release id instead.")
        media_type = content_type(local_path)
        signed = self.api_json("POST", "/cloud/course/upload-url", {"path": remote_path, "size": size, "sha256": digest, "content_type": media_type, "expires_in": 1800})
        signed_url = signed.get("url")
        if not isinstance(signed_url, str) or not signed_url:
            raise SystemExit("course Worker did not return an upload URL")
        completed = subprocess.run(["curl", "--fail-with-body", "--silent", "--show-error", "--retry", "3", "--retry-delay", "2", "--request", "PUT", "--header", f"Content-Type: {media_type}", "--header", f"Content-Length: {size}", "--upload-file", str(local_path), signed_url], text=True, capture_output=True)
        if completed.returncode != 0:
            raise SystemExit(f"upload failed for {remote_path}: {(completed.stderr or completed.stdout).strip()}")
        self.api_json("POST", "/cloud/course/upload-complete", {"path": remote_path, "size": size, "sha256": digest})
        print(f"uploaded: {remote_path} ({size} bytes)")

    def publish(self, release_id: str, channel: str) -> None:
        manifest_path = f"releases/{release_id}/manifest.json"
        metadata = self.object_metadata(manifest_path)
        if metadata is None:
            raise SystemExit(f"release manifest does not exist: {manifest_path}")
        digest = metadata.get("sha256")
        if not isinstance(digest, str) or len(digest) != 64:
            raise SystemExit("release manifest metadata does not contain SHA-256")
        result = self.api_json("POST", "/cloud/course/channel/publish", {"channel": channel, "source_path": manifest_path, "sha256": digest})
        print(f"published {channel}: {result.get('publicUrl', '')}")


def verify_manifest(root: Path, base_url: str, release_id: str) -> None:
    manifest_path = root / "manifest.json"
    if not manifest_path.is_file():
        raise SystemExit(f"missing manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if set(manifest) != {"textbooks"} or not isinstance(manifest["textbooks"], list):
        raise SystemExit("manifest.json does not use the current APK distribution contract")
    prefix = f"{base_url.rstrip('/')}/cloud/course/public/releases/{release_id}/"
    for textbook in manifest["textbooks"]:
        for spec in [textbook["package"], *textbook["files"]]:
            url = spec.get("url")
            if not isinstance(url, str) or not url.startswith(prefix):
                raise SystemExit(f"manifest URL is outside release {release_id}: {url}")


def add_r2_options(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--r2-account-id", default=os.environ.get("R2_ACCOUNT_ID", ""))
    parser.add_argument("--r2-access-key-id", default=os.environ.get("R2_ACCESS_KEY_ID", ""))
    parser.add_argument("--r2-secret-access-key", default=os.environ.get("R2_SECRET_ACCESS_KEY", ""))
    parser.add_argument("--r2-bucket-name", default=os.environ.get("R2_BUCKET_NAME", ""))


def add_worker_options(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--course-base-url", default=os.environ.get("COURSE_BASE_URL", DEFAULT_COURSE_BASE_URL))
    parser.add_argument("--course-api-token", default=os.environ.get("COURSE_API_TOKEN", ""))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Cloudflare R2 CRUD + School course release manager")
    sub = parser.add_subparsers(dest="command", required=True)

    command = sub.add_parser("create")
    add_r2_options(command)
    command.add_argument("local", type=Path)
    command.add_argument("key")

    command = sub.add_parser("read")
    add_r2_options(command)
    command.add_argument("key")
    command.add_argument("--output", type=Path)
    command.add_argument("--force", action="store_true")

    command = sub.add_parser("update")
    add_r2_options(command)
    command.add_argument("local", type=Path)
    command.add_argument("key")
    command.add_argument("--allow-release-mutation", action="store_true")

    command = sub.add_parser("delete")
    add_r2_options(command)
    command.add_argument("key")
    command.add_argument("--yes", action="store_true")
    command.add_argument("--allow-release-mutation", action="store_true")

    command = sub.add_parser("list")
    add_r2_options(command)
    command.add_argument("prefix", nargs="?", default="")
    command.add_argument("--json", action="store_true")

    command = sub.add_parser("dir-create")
    add_r2_options(command)
    command.add_argument("prefix")
    command.add_argument("--from", dest="local", type=Path)

    command = sub.add_parser("dir-read")
    add_r2_options(command)
    command.add_argument("prefix")
    command.add_argument("--recursive", action="store_true")
    command.add_argument("--output", type=Path)
    command.add_argument("--force", action="store_true")
    command.add_argument("--json", action="store_true")

    command = sub.add_parser("dir-update")
    add_r2_options(command)
    command.add_argument("local", type=Path)
    command.add_argument("prefix")
    command.add_argument("--delete-extra", action="store_true")
    command.add_argument("--allow-release-mutation", action="store_true")

    command = sub.add_parser("dir-delete")
    add_r2_options(command)
    command.add_argument("prefix")
    command.add_argument("--yes", action="store_true")
    command.add_argument("--allow-release-mutation", action="store_true")

    command = sub.add_parser("release-upload")
    add_worker_options(command)
    command.add_argument("--root", type=Path, required=True)
    command.add_argument("--release-id", required=True)
    command.add_argument("--channel", choices=("none", "testing", "stable"), default="testing")

    command = sub.add_parser("publish")
    add_worker_options(command)
    command.add_argument("--release-id", required=True)
    command.add_argument("--channel", choices=("testing", "stable"), default="stable")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.command in {"create", "read", "update", "delete", "list", "dir-create", "dir-read", "dir-update", "dir-delete"}:
        r2 = R2Client(args)
        if args.command == "create":
            r2.create(args.local.resolve(), normalize_key(args.key))
        elif args.command == "read":
            key = normalize_key(args.key)
            r2.download(key, args.output.resolve(), args.force) if args.output else print_json(r2.describe(key))
        elif args.command == "update":
            r2.update(args.local.resolve(), normalize_key(args.key), args.allow_release_mutation)
        elif args.command == "delete":
            if not args.yes:
                raise SystemExit("delete requires --yes")
            r2.delete(normalize_key(args.key), args.allow_release_mutation)
        elif args.command == "list":
            rows = list(r2.list(args.prefix.strip().replace("\\", "/").lstrip("/")))
            if args.json:
                print_json(rows)
            else:
                print("\n".join(f"{item['size']:>12}  {item['key']}" for item in rows) if rows else "(no objects)")
        elif args.command == "dir-create":
            r2.create_dir(args.prefix, args.local.resolve() if args.local else None)
        elif args.command == "dir-read":
            if args.output:
                r2.download_dir(args.prefix, args.output.resolve(), args.force)
            else:
                rows = r2.list_dir(args.prefix, args.recursive)
                if args.json:
                    print_json(rows)
                else:
                    print("\n".join(f"{'[DIR]' if item['type'] == 'directory' else item['size']:>12}  {item['key']}" for item in rows) if rows else "(empty directory)")
        elif args.command == "dir-update":
            r2.update_dir(args.local.resolve(), args.prefix, args.delete_extra, args.allow_release_mutation)
        else:
            if not args.yes:
                raise SystemExit("dir-delete requires --yes")
            r2.delete_dir(args.prefix, args.allow_release_mutation)
        return 0

    release_id = require_identifier(args.release_id, "release id")
    worker = CourseWorkerClient(args.course_base_url, args.course_api_token)
    if args.command == "publish":
        worker.publish(release_id, args.channel)
        return 0

    root = args.root.resolve()
    if not root.is_dir():
        raise SystemExit(f"release directory not found: {root}")
    verify_manifest(root, args.course_base_url, release_id)
    files = sorted(path for path in root.rglob("*") if path.is_file())
    files.sort(key=lambda path: path.name == "manifest.json")
    if not files:
        raise SystemExit("release directory is empty")
    for path in files:
        relative = PurePosixPath(path.relative_to(root).as_posix())
        worker.upload_file(path, f"releases/{release_id}/{relative.as_posix()}")
    if args.channel != "none":
        worker.publish(release_id, args.channel)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("cancelled", file=sys.stderr)
        raise SystemExit(130)
