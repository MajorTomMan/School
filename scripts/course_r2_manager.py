#!/usr/bin/env python3
"""School course storage manager: Worker-first CRUD, direct R2 fallback, and immutable release publishing."""
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
from typing import Any

DEFAULT_REPO = "MajorTomMan/School"
DEFAULT_COURSE_URL = "https://course.flashnamesl.workers.dev"
IDENTIFIER = re.compile(r"^[A-Za-z0-9._-]+$")
VAR_CACHE: dict[tuple[str, str], str] = {}


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def key(value: str) -> str:
    value = value.strip().replace("\\", "/").lstrip("/")
    path = PurePosixPath(value)
    if not value or value == "." or ".." in path.parts:
        raise SystemExit(f"invalid R2 key: {value!r}")
    return path.as_posix()


def clean_prefix(value: str) -> str:
    value = value.strip().replace("\\", "/").lstrip("/")
    if not value:
        return ""
    trailing = value.endswith("/")
    normalized = key(value.rstrip("/"))
    return normalized + "/" if trailing else normalized


def prefix(value: str) -> str:
    return key(value).rstrip("/") + "/"


def media_type(path: Path) -> str:
    explicit = {".json": "application/json; charset=utf-8", ".zip": "application/zip", ".pdf": "application/pdf"}
    return explicit.get(path.suffix.lower()) or mimetypes.guess_type(path.name)[0] or "application/octet-stream"


def repo_name(value: str | None) -> str:
    return (value or "").strip() or os.environ.get("GITHUB_REPOSITORY", "").strip() or DEFAULT_REPO


def repo_var(name: str, repo: str) -> str:
    cache_key = (repo, name)
    if cache_key in VAR_CACHE:
        return VAR_CACHE[cache_key]
    try:
        result = subprocess.run(["gh", "variable", "get", name, "--repo", repo], text=True, capture_output=True)
        value = result.stdout.strip() if result.returncode == 0 else ""
    except FileNotFoundError:
        value = ""
    VAR_CACHE[cache_key] = value
    return value


def config(explicit: str | None, name: str, repo: str, *, variable: bool = True, default: str = "") -> str:
    value = (explicit or "").strip() or os.environ.get(name, "").strip()
    if value:
        return value
    value = repo_var(name, repo) if variable else ""
    return value or default


def require_id(value: str) -> str:
    value = value.strip()
    if not IDENTIFIER.fullmatch(value):
        raise SystemExit(f"invalid release id: {value}")
    return value


def guard_release(remote: str, allow: bool) -> None:
    if remote.startswith("releases/") and not allow:
        raise SystemExit(f"refusing to mutate immutable release object: {remote}; publish a new release id instead")


def print_objects(rows: list[dict[str, Any]], as_json: bool) -> None:
    if as_json:
        print(json.dumps(rows, ensure_ascii=False, indent=2, default=str))
        return
    print("\n".join(f"{int(row.get('size') or 0):>12}  {row.get('key') or row.get('path')}" for row in rows) if rows else "(no objects)")


class R2:
    """Low-level direct bucket backend. Use only when explicit bucket credentials are available."""

    def __init__(self, args: argparse.Namespace) -> None:
        try:
            import boto3
        except ImportError as error:
            raise SystemExit("direct R2 backend requires boto3: python -m pip install boto3") from error
        repo = repo_name(args.github_repo)
        account = config(args.r2_account_id, "R2_ACCOUNT_ID", repo)
        access = config(args.r2_access_key_id, "R2_ACCESS_KEY_ID", repo)
        secret = config(args.r2_secret_access_key, "R2_SECRET_ACCESS_KEY", repo, variable=False)
        self.bucket = config(args.r2_bucket_name, "R2_BUCKET_NAME", repo)
        missing = [name for name, value in (("R2_ACCOUNT_ID", account), ("R2_ACCESS_KEY_ID", access), ("R2_SECRET_ACCESS_KEY", secret), ("R2_BUCKET_NAME", self.bucket)) if not value]
        if missing:
            raise SystemExit(f"missing direct R2 configuration: {', '.join(missing)}")
        self.s3 = boto3.client("s3", endpoint_url=f"https://{account}.r2.cloudflarestorage.com", aws_access_key_id=access, aws_secret_access_key=secret, region_name="auto")

    def head(self, remote: str) -> dict[str, Any] | None:
        try:
            return self.s3.head_object(Bucket=self.bucket, Key=remote)
        except self.s3.exceptions.ClientError as error:
            code = error.response.get("Error", {}).get("Code")
            status = error.response.get("ResponseMetadata", {}).get("HTTPStatusCode")
            if status == 404 or code in {"404", "NoSuchKey", "NotFound"}:
                return None
            raise

    def remote_sha(self, remote: str, head: dict[str, Any]) -> str:
        saved = (head.get("Metadata") or {}).get("sha256")
        if saved and len(saved) == 64:
            return saved.lower()
        h = hashlib.sha256()
        body = self.s3.get_object(Bucket=self.bucket, Key=remote)["Body"]
        try:
            for chunk in iter(lambda: body.read(1024 * 1024), b""):
                h.update(chunk)
        finally:
            body.close()
        return h.hexdigest()

    def put(self, local: Path, remote: str, digest: str | None = None) -> None:
        if not local.is_file():
            raise SystemExit(f"local file not found: {local}")
        digest = digest or sha256(local)
        self.s3.upload_file(str(local), self.bucket, remote, ExtraArgs={"ContentType": media_type(local), "Metadata": {"sha256": digest}})

    def create(self, local: Path, remote: str) -> None:
        if self.head(remote):
            raise SystemExit(f"object exists: {remote}; use update")
        self.put(local, remote)
        print(f"created: {remote}")

    def read(self, remote: str, output: Path | None, force: bool) -> None:
        head = self.head(remote)
        if not head:
            raise SystemExit(f"object not found: {remote}")
        if output:
            if output.exists() and not force:
                raise SystemExit(f"local file exists: {output}; use --force")
            output.parent.mkdir(parents=True, exist_ok=True)
            self.s3.download_file(self.bucket, remote, str(output))
            print(f"downloaded: {remote} -> {output}")
            return
        print(json.dumps({"key": remote, "size": head.get("ContentLength"), "content_type": head.get("ContentType"), "etag": str(head.get("ETag", "")).strip('"'), "last_modified": head.get("LastModified"), "sha256": (head.get("Metadata") or {}).get("sha256")}, ensure_ascii=False, indent=2, default=str))

    def update(self, local: Path, remote: str, allow: bool) -> None:
        head = self.head(remote)
        if not head:
            raise SystemExit(f"object not found: {remote}; use create")
        guard_release(remote, allow)
        digest = sha256(local)
        if head.get("ContentLength") == local.stat().st_size and self.remote_sha(remote, head) == digest:
            print(f"unchanged: {remote}")
            return
        self.put(local, remote, digest)
        print(f"updated: {remote}")

    def delete(self, remote: str, allow: bool) -> None:
        if not self.head(remote):
            raise SystemExit(f"object not found: {remote}")
        guard_release(remote, allow)
        self.s3.delete_object(Bucket=self.bucket, Key=remote)
        print(f"deleted: {remote}")

    def objects(self, pfx: str = "") -> list[dict[str, Any]]:
        rows: list[dict[str, Any]] = []
        for page in self.s3.get_paginator("list_objects_v2").paginate(Bucket=self.bucket, Prefix=pfx):
            rows.extend({"key": item["Key"], "size": item.get("Size"), "etag": str(item.get("ETag", "")).strip('"'), "last_modified": item.get("LastModified")} for item in page.get("Contents", []))
        return rows

    def dir_exists(self, pfx: str) -> bool:
        return bool(self.s3.list_objects_v2(Bucket=self.bucket, Prefix=pfx, MaxKeys=1).get("Contents"))

    def dir_create(self, pfx: str, local: Path | None) -> None:
        pfx = prefix(pfx)
        if self.dir_exists(pfx):
            raise SystemExit(f"directory exists: {pfx}; use dir-update")
        if local and not local.is_dir():
            raise SystemExit(f"local directory not found: {local}")
        self.s3.put_object(Bucket=self.bucket, Key=pfx, Body=b"", ContentType="application/x-directory")
        count = 0
        if local:
            for file in sorted(x for x in local.rglob("*") if x.is_file()):
                self.put(file, pfx + PurePosixPath(file.relative_to(local).as_posix()).as_posix())
                count += 1
        print(f"created directory: {pfx} ({count} files)")

    def dir_read(self, pfx: str, recursive: bool, output: Path | None, force: bool, as_json: bool) -> None:
        pfx = prefix(pfx)
        if output:
            if not self.dir_exists(pfx):
                raise SystemExit(f"directory not found: {pfx}")
            output.mkdir(parents=True, exist_ok=True)
            count = 0
            for item in self.objects(pfx):
                if item["key"] == pfx:
                    continue
                target = output.joinpath(*PurePosixPath(item["key"][len(pfx):]).parts)
                if target.exists() and not force:
                    raise SystemExit(f"local file exists: {target}; use --force")
                target.parent.mkdir(parents=True, exist_ok=True)
                self.s3.download_file(self.bucket, item["key"], str(target))
                count += 1
            print(f"downloaded directory: {pfx} -> {output} ({count} files)")
            return
        kwargs: dict[str, Any] = {"Bucket": self.bucket, "Prefix": pfx}
        if not recursive:
            kwargs["Delimiter"] = "/"
        rows: list[dict[str, Any]] = []
        for page in self.s3.get_paginator("list_objects_v2").paginate(**kwargs):
            rows.extend({"type": "directory", "key": item["Prefix"]} for item in page.get("CommonPrefixes", []))
            rows.extend({"type": "file", "key": item["Key"], "size": item.get("Size")} for item in page.get("Contents", []) if item["Key"] != pfx)
        rows.sort(key=lambda x: (x["type"] != "directory", x["key"]))
        if as_json:
            print(json.dumps(rows, ensure_ascii=False, indent=2))
        else:
            print("\n".join(f"{'[DIR]' if row['type'] == 'directory' else row['size']:>12}  {row['key']}" for row in rows) if rows else "(empty directory)")

    def dir_update(self, local: Path, pfx: str, delete_extra: bool, allow: bool) -> None:
        pfx = prefix(pfx)
        if not local.is_dir() or not self.dir_exists(pfx):
            raise SystemExit(f"local or remote directory not found: {local} / {pfx}")
        guard_release(pfx, allow)
        local_files = {pfx + PurePosixPath(file.relative_to(local).as_posix()).as_posix(): file for file in local.rglob("*") if file.is_file()}
        remote = {item["key"] for item in self.objects(pfx) if item["key"] != pfx}
        created = updated = unchanged = 0
        for remote_key, file in sorted(local_files.items()):
            guard_release(remote_key, allow)
            head = self.head(remote_key)
            if not head:
                self.put(file, remote_key); created += 1; continue
            digest = sha256(file)
            if head.get("ContentLength") == file.stat().st_size and self.remote_sha(remote_key, head) == digest:
                unchanged += 1
            else:
                self.put(file, remote_key, digest); updated += 1
        stale = sorted(remote - set(local_files)) if delete_extra else []
        for remote_key in stale:
            guard_release(remote_key, allow)
        self.bulk_delete(stale)
        print(f"updated directory: {pfx} (created={created}, updated={updated}, unchanged={unchanged}, deleted={len(stale)})")

    def bulk_delete(self, keys: list[str]) -> None:
        for i in range(0, len(keys), 1000):
            batch = keys[i:i + 1000]
            if batch:
                result = self.s3.delete_objects(Bucket=self.bucket, Delete={"Objects": [{"Key": x} for x in batch], "Quiet": True})
                if result.get("Errors"):
                    raise SystemExit(f"bulk delete failed: {result['Errors']}")

    def dir_delete(self, pfx: str, allow: bool) -> None:
        pfx = prefix(pfx)
        keys = [item["key"] for item in self.objects(pfx)]
        for remote in keys:
            guard_release(remote, allow)
        self.bulk_delete(keys)
        print(f"deleted directory: {pfx} ({len(keys)} objects)")


class Worker:
    """Course-scoped backend exposed by the Cloudflare Worker and protected by COURSE_API_TOKEN."""

    def __init__(self, base_url: str, token: str) -> None:
        self.base = base_url.rstrip("/")
        self.token = token.strip()
        if not self.base.startswith("https://") and not self.base.startswith("http://127.0.0.1"):
            raise SystemExit("course base URL must use HTTPS")
        if len(self.token) < 32:
            raise SystemExit("COURSE_API_TOKEN is missing or too short; inject it through CLI/env/Actions Secret")

    def request(self, method: str, path: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode()
        headers = {"Accept": "application/json", "Authorization": f"Bearer {self.token}", "User-Agent": "School-Course-R2-Manager/3.0"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(self.base + path, data=body, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=120) as response:
                raw = response.read()
        except urllib.error.HTTPError as error:
            response_body = error.read().decode(errors="replace")
            hint = ""
            if error.code == 403 and "delete_disabled" in response_body:
                hint = " (enable COURSE_ALLOW_DELETE=true on the course Worker)"
            raise SystemExit(f"{method} {path} HTTP {error.code}: {response_body}{hint}") from error
        except urllib.error.URLError as error:
            raise SystemExit(f"cannot reach course Worker: {error}") from error
        return json.loads(raw) if raw else {}

    def metadata(self, remote: str) -> dict[str, Any] | None:
        try:
            return self.request("GET", "/cloud/course/object?" + urllib.parse.urlencode({"path": remote}))
        except SystemExit as error:
            if "HTTP 404:" in str(error):
                return None
            raise

    def objects(self, pfx: str = "") -> list[dict[str, Any]]:
        rows: list[dict[str, Any]] = []
        cursor = ""
        seen: set[str] = set()
        while True:
            query: dict[str, str] = {"prefix": clean_prefix(pfx), "limit": "200"}
            if cursor:
                query["cursor"] = cursor
            result = self.request("GET", "/cloud/course/objects?" + urllib.parse.urlencode(query))
            for item in result.get("objects", []):
                rows.append({"key": item.get("path"), "size": item.get("size"), "etag": item.get("etag"), "last_modified": item.get("uploaded"), "sha256": item.get("sha256"), "content_type": item.get("contentType")})
            if not result.get("truncated"):
                break
            cursor = str(result.get("cursor") or "")
            if not cursor or cursor in seen:
                raise SystemExit("course Worker returned an invalid pagination cursor")
            seen.add(cursor)
        return rows

    def upload(self, local: Path, remote: str, *, overwrite: bool = False) -> None:
        if not local.is_file():
            raise SystemExit(f"local file not found: {local}")
        size, digest = local.stat().st_size, sha256(local)
        existing = self.metadata(remote)
        if existing:
            if existing.get("size") == size and existing.get("sha256") == digest:
                print(f"unchanged: {remote}")
                return
            if not overwrite:
                raise SystemExit(f"object exists with different content: {remote}; use update")
        ctype = media_type(local)
        signed = self.request("POST", "/cloud/course/upload-url", {"path": remote, "size": size, "sha256": digest, "content_type": ctype, "expires_in": 1800})
        url = signed.get("url")
        if not url:
            raise SystemExit("course Worker did not return an upload URL")
        result = subprocess.run(["curl", "--fail-with-body", "--silent", "--show-error", "--retry", "3", "--request", "PUT", "--header", f"Content-Type: {ctype}", "--header", f"Content-Length: {size}", "--upload-file", str(local), url], text=True, capture_output=True)
        if result.returncode:
            raise SystemExit(f"upload failed for {remote}: {(result.stderr or result.stdout).strip()}")
        self.request("POST", "/cloud/course/upload-complete", {"path": remote, "size": size, "sha256": digest})
        print(f"{'updated' if existing else 'created'}: {remote} ({size} bytes)")

    def create(self, local: Path, remote: str) -> None:
        self.upload(local, remote, overwrite=False)

    def read(self, remote: str, output: Path | None, force: bool) -> None:
        meta = self.metadata(remote)
        if meta is None:
            raise SystemExit(f"object not found: {remote}")
        if output is None:
            print(json.dumps(meta, ensure_ascii=False, indent=2, default=str))
            return
        if output.exists() and not force:
            raise SystemExit(f"local file exists: {output}; use --force")
        signed = self.request("POST", "/cloud/course/download-url", {"path": remote, "expires_in": 1800, "download_name": output.name})
        url = signed.get("url")
        if not url:
            raise SystemExit("course Worker did not return a download URL")
        output.parent.mkdir(parents=True, exist_ok=True)
        result = subprocess.run(["curl", "--fail-with-body", "--silent", "--show-error", "--retry", "3", "--location", "--output", str(output), url], text=True, capture_output=True)
        if result.returncode:
            output.unlink(missing_ok=True)
            raise SystemExit(f"download failed for {remote}: {(result.stderr or result.stdout).strip()}")
        print(f"downloaded: {remote} -> {output}")

    def update(self, local: Path, remote: str, allow: bool) -> None:
        guard_release(remote, allow)
        if self.metadata(remote) is None:
            raise SystemExit(f"object not found: {remote}; use create")
        self.upload(local, remote, overwrite=True)

    def delete(self, remote: str, allow: bool) -> None:
        guard_release(remote, allow)
        meta = self.metadata(remote)
        if meta is None:
            raise SystemExit(f"object not found: {remote}")
        payload: dict[str, Any] = {"path": remote, "confirm": remote}
        if meta.get("etag"):
            payload["expected_etag"] = meta["etag"]
        if meta.get("size") is not None:
            payload["expected_size"] = meta["size"]
        self.request("DELETE", "/cloud/course/object", payload)
        print(f"deleted: {remote}")

    def dir_exists(self, pfx: str) -> bool:
        return bool(self.objects(prefix(pfx)))

    def dir_create(self, pfx: str, local: Path | None) -> None:
        pfx = prefix(pfx)
        if self.objects(pfx):
            raise SystemExit(f"directory exists: {pfx}; use dir-update")
        if local is None:
            raise SystemExit("Worker backend cannot create an empty R2 prefix; provide --from with at least one file")
        if not local.is_dir():
            raise SystemExit(f"local directory not found: {local}")
        files = sorted(x for x in local.rglob("*") if x.is_file())
        if not files:
            raise SystemExit("Worker backend cannot create an empty R2 prefix")
        for file in files:
            self.create(file, pfx + PurePosixPath(file.relative_to(local).as_posix()).as_posix())
        print(f"created directory: {pfx} ({len(files)} files)")

    def dir_rows(self, pfx: str, recursive: bool) -> list[dict[str, Any]]:
        pfx = prefix(pfx)
        objects = self.objects(pfx)
        if recursive:
            return [{"type": "file", **item} for item in objects]
        rows: list[dict[str, Any]] = []
        directories: set[str] = set()
        for item in objects:
            remote = str(item.get("key") or "")
            suffix = remote[len(pfx):]
            if "/" in suffix:
                directories.add(pfx + suffix.split("/", 1)[0] + "/")
            else:
                rows.append({"type": "file", **item})
        rows.extend({"type": "directory", "key": directory} for directory in sorted(directories))
        rows.sort(key=lambda row: (row["type"] != "directory", str(row.get("key") or "")))
        return rows

    def dir_read(self, pfx: str, recursive: bool, output: Path | None, force: bool, as_json: bool) -> None:
        pfx = prefix(pfx)
        objects = self.objects(pfx)
        if not objects:
            raise SystemExit(f"directory not found or empty: {pfx}")
        if output:
            output.mkdir(parents=True, exist_ok=True)
            for item in objects:
                remote = str(item["key"])
                target = output.joinpath(*PurePosixPath(remote[len(pfx):]).parts)
                self.read(remote, target, force)
            print(f"downloaded directory: {pfx} -> {output} ({len(objects)} files)")
            return
        rows = self.dir_rows(pfx, recursive)
        if as_json:
            print(json.dumps(rows, ensure_ascii=False, indent=2, default=str))
        else:
            print("\n".join(f"{'[DIR]' if row['type'] == 'directory' else int(row.get('size') or 0):>12}  {row['key']}" for row in rows) if rows else "(empty directory)")

    def dir_update(self, local: Path, pfx: str, delete_extra: bool, allow: bool) -> None:
        pfx = prefix(pfx)
        if not local.is_dir():
            raise SystemExit(f"local directory not found: {local}")
        remote_rows = self.objects(pfx)
        if not remote_rows:
            raise SystemExit(f"remote directory not found or empty: {pfx}")
        guard_release(pfx, allow)
        local_files = {pfx + PurePosixPath(file.relative_to(local).as_posix()).as_posix(): file for file in local.rglob("*") if file.is_file()}
        remote = {str(item["key"]): item for item in remote_rows}
        created = updated = unchanged = 0
        for remote_key, file in sorted(local_files.items()):
            guard_release(remote_key, allow)
            item = remote.get(remote_key)
            digest = sha256(file)
            if item is None:
                self.create(file, remote_key); created += 1
            elif item.get("size") == file.stat().st_size and item.get("sha256") == digest:
                unchanged += 1
            else:
                self.update(file, remote_key, allow); updated += 1
        stale = sorted(set(remote) - set(local_files)) if delete_extra else []
        for remote_key in stale:
            self.delete(remote_key, allow)
        print(f"updated directory: {pfx} (created={created}, updated={updated}, unchanged={unchanged}, deleted={len(stale)})")

    def dir_delete(self, pfx: str, allow: bool) -> None:
        pfx = prefix(pfx)
        rows = self.objects(pfx)
        if not rows:
            raise SystemExit(f"directory not found or empty: {pfx}")
        for item in rows:
            self.delete(str(item["key"]), allow)
        if self.objects(pfx):
            raise SystemExit(f"directory delete verification failed: {pfx}")
        print(f"deleted directory: {pfx} ({len(rows)} objects)")

    def purge(self, allow: bool) -> None:
        rows = self.objects("")
        if not rows:
            print("course storage is already empty")
            return
        for item in rows:
            guard_release(str(item["key"]), allow)
        print(f"purging course storage: {len(rows)} objects")
        for item in rows:
            self.delete(str(item["key"]), allow)
        remaining = self.objects("")
        if remaining:
            raise SystemExit(f"purge verification failed: {len(remaining)} objects remain")
        print(f"purged course storage: {len(rows)} objects deleted")

    def publish(self, release_id: str, channel: str) -> None:
        remote = f"releases/{release_id}/manifest.json"
        meta = self.metadata(remote)
        digest = meta.get("sha256") if meta else None
        if not digest or len(digest) != 64:
            raise SystemExit(f"release manifest missing or invalid: {remote}")
        result = self.request("POST", "/cloud/course/channel/publish", {"channel": channel, "source_path": remote, "sha256": digest})
        print(f"published {channel}: {result.get('publicUrl', '')}")


def verify_manifest(root: Path, base: str, release_id: str) -> None:
    path = root / "manifest.json"
    if not path.is_file():
        raise SystemExit(f"missing manifest: {path}")
    manifest = json.loads(path.read_text(encoding="utf-8"))
    if set(manifest) != {"textbooks"} or not isinstance(manifest["textbooks"], list):
        raise SystemExit("manifest.json does not use the current APK distribution contract")
    expected = f"{base.rstrip('/')}/cloud/course/public/releases/{release_id}/"
    for textbook in manifest["textbooks"]:
        for spec in [textbook["package"], *textbook["files"]]:
            if not isinstance(spec.get("url"), str) or not spec["url"].startswith(expected):
                raise SystemExit(f"manifest URL is outside release {release_id}: {spec.get('url')}")


def github_opts(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--github-repo", default=os.environ.get("GITHUB_REPOSITORY", DEFAULT_REPO))


def worker_opts(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--course-base-url", default="")
    parser.add_argument("--course-api-token", default="")


def r2_opts(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--r2-account-id", default="")
    parser.add_argument("--r2-access-key-id", default="")
    parser.add_argument("--r2-secret-access-key", default="")
    parser.add_argument("--r2-bucket-name", default="")


def storage_opts(parser: argparse.ArgumentParser) -> None:
    github_opts(parser)
    parser.add_argument("--backend", choices=("worker", "direct"), default="worker")
    worker_opts(parser)
    r2_opts(parser)


def parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="School course storage CRUD + immutable release manager")
    sub = p.add_subparsers(dest="command", required=True)
    for name in ("create", "read", "update", "delete", "list", "dir-create", "dir-read", "dir-update", "dir-delete", "purge"):
        cmd = sub.add_parser(name); storage_opts(cmd)
        if name == "create": cmd.add_argument("local", type=Path); cmd.add_argument("key")
        elif name == "read": cmd.add_argument("key"); cmd.add_argument("--output", type=Path); cmd.add_argument("--force", action="store_true")
        elif name == "update": cmd.add_argument("local", type=Path); cmd.add_argument("key"); cmd.add_argument("--allow-release-mutation", action="store_true")
        elif name == "delete": cmd.add_argument("key"); cmd.add_argument("--yes", action="store_true"); cmd.add_argument("--allow-release-mutation", action="store_true")
        elif name == "list": cmd.add_argument("prefix", nargs="?", default=""); cmd.add_argument("--json", action="store_true")
        elif name == "dir-create": cmd.add_argument("prefix"); cmd.add_argument("--from", dest="local", type=Path)
        elif name == "dir-read": cmd.add_argument("prefix"); cmd.add_argument("--recursive", action="store_true"); cmd.add_argument("--output", type=Path); cmd.add_argument("--force", action="store_true"); cmd.add_argument("--json", action="store_true")
        elif name == "dir-update": cmd.add_argument("local", type=Path); cmd.add_argument("prefix"); cmd.add_argument("--delete-extra", action="store_true"); cmd.add_argument("--allow-release-mutation", action="store_true")
        elif name == "dir-delete": cmd.add_argument("prefix"); cmd.add_argument("--yes", action="store_true"); cmd.add_argument("--allow-release-mutation", action="store_true")
        else: cmd.add_argument("--yes", action="store_true"); cmd.add_argument("--allow-release-mutation", action="store_true")
    upload = sub.add_parser("release-upload"); github_opts(upload); worker_opts(upload); upload.add_argument("--root", type=Path, required=True); upload.add_argument("--release-id", required=True); upload.add_argument("--channel", choices=("none", "testing", "stable"), default="testing")
    publish = sub.add_parser("publish"); github_opts(publish); worker_opts(publish); publish.add_argument("--release-id", required=True); publish.add_argument("--channel", choices=("testing", "stable"), default="stable")
    return p


def worker_from_args(args: argparse.Namespace) -> Worker:
    repo = repo_name(args.github_repo)
    base = config(args.course_base_url, "COURSE_BASE_URL", repo, default=DEFAULT_COURSE_URL)
    token = config(args.course_api_token, "COURSE_API_TOKEN", repo, variable=False)
    return Worker(base, token)


def storage_from_args(args: argparse.Namespace) -> R2 | Worker:
    return R2(args) if args.backend == "direct" else worker_from_args(args)


def main() -> int:
    args = parser().parse_args()
    storage_commands = {"create", "read", "update", "delete", "list", "dir-create", "dir-read", "dir-update", "dir-delete", "purge"}
    if args.command in storage_commands:
        if args.command == "purge" and args.backend != "worker":
            raise SystemExit("purge is Worker-only so it cannot accidentally wipe a shared R2 bucket")
        storage = storage_from_args(args)
        if args.command == "create": storage.create(args.local.resolve(), key(args.key))
        elif args.command == "read": storage.read(key(args.key), args.output.resolve() if args.output else None, args.force)
        elif args.command == "update": storage.update(args.local.resolve(), key(args.key), args.allow_release_mutation)
        elif args.command == "delete":
            if not args.yes: raise SystemExit("delete requires --yes")
            storage.delete(key(args.key), args.allow_release_mutation)
        elif args.command == "list": print_objects(storage.objects(clean_prefix(args.prefix)), args.json)
        elif args.command == "dir-create": storage.dir_create(args.prefix, args.local.resolve() if args.local else None)
        elif args.command == "dir-read": storage.dir_read(args.prefix, args.recursive, args.output.resolve() if args.output else None, args.force, args.json)
        elif args.command == "dir-update": storage.dir_update(args.local.resolve(), args.prefix, args.delete_extra, args.allow_release_mutation)
        elif args.command == "dir-delete":
            if not args.yes: raise SystemExit("dir-delete requires --yes")
            storage.dir_delete(args.prefix, args.allow_release_mutation)
        else:
            if not args.yes: raise SystemExit("purge requires --yes")
            assert isinstance(storage, Worker)
            storage.purge(args.allow_release_mutation)
        return 0
    release_id = require_id(args.release_id)
    worker = worker_from_args(args)
    if args.command == "publish": worker.publish(release_id, args.channel); return 0
    root = args.root.resolve()
    if not root.is_dir(): raise SystemExit(f"release directory not found: {root}")
    verify_manifest(root, worker.base, release_id)
    files = sorted((x for x in root.rglob("*") if x.is_file()), key=lambda x: x.name == "manifest.json")
    if not files: raise SystemExit("release directory is empty")
    for file in files:
        worker.upload(file, f"releases/{release_id}/{PurePosixPath(file.relative_to(root).as_posix()).as_posix()}")
    if args.channel != "none": worker.publish(release_id, args.channel)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("cancelled", file=sys.stderr)
        raise SystemExit(130)
