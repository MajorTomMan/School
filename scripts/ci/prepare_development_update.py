#!/usr/bin/env python3
import base64
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
DIST = ROOT / "dist"
APK_SOURCE = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
APK_OUTPUT = DIST / "school-debug.apk"


def require_version():
    version_name = os.environ.get("VERSION_NAME", "").strip()
    version_code_text = os.environ.get("VERSION_CODE", "").strip()
    if not re.fullmatch(r"\d+\.\d+\.\d+", version_name):
        raise SystemExit("VERSION_NAME 必须使用 x.y.z 格式")
    if not version_code_text.isdigit() or int(version_code_text) <= 0:
        raise SystemExit("VERSION_CODE 必须是正整数")
    return version_name, int(version_code_text)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_changes():
    notes_file = ROOT / ".release-notes/current.md"
    if not notes_file.is_file():
        return []
    changes = []
    for line in notes_file.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped.startswith("- ") and stripped[2:].strip():
            changes.append(stripped[2:].strip())
    return changes


def decode_private_key():
    encoded = os.environ.get("SCHOOL_UPDATE_PRIVATE_KEY_B64", "").strip()
    if not encoded:
        encoded = (ROOT / "signing/school-update-development-private.pem.b64").read_text(encoding="utf-8")
    compact = "".join(encoded.split())
    try:
        return base64.b64decode(compact, validate=True)
    except ValueError as error:
        raise SystemExit("更新清单私钥不是有效的 Base64") from error


def sign_manifest(manifest_path):
    private_key_bytes = decode_private_key()
    public_key = ROOT / "signing/school-update-development-public.pem"
    signature = DIST / "update-manifest.sig"
    private_key_path = None
    try:
        with tempfile.NamedTemporaryFile(prefix="school-update-private-", suffix=".pem", delete=False) as file:
            file.write(private_key_bytes)
            private_key_path = Path(file.name)
        private_key_path.chmod(0o600)
        subprocess.run(["openssl", "dgst", "-sha256", "-sign", str(private_key_path), "-out", str(signature), str(manifest_path)], check=True)
        subprocess.run(["openssl", "dgst", "-sha256", "-verify", str(public_key), "-signature", str(signature), str(manifest_path)], check=True)
    finally:
        if private_key_path is not None:
            private_key_path.unlink(missing_ok=True)


def main():
    version_name, version_code = require_version()
    if not APK_SOURCE.is_file():
        raise SystemExit(f"Debug APK 不存在：{APK_SOURCE}")

    DIST.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(APK_SOURCE, APK_OUTPUT)
    apk_sha256 = sha256(APK_OUTPUT)
    (DIST / "school-debug.apk.sha256").write_text(f"{apk_sha256}  dist/school-debug.apk\n", encoding="utf-8")

    certificate_sha256 = (ROOT / "signing/school-development.cert.sha256").read_text(encoding="utf-8").strip().replace(":", "").lower()
    manifest = {
        "schemaVersion": 2,
        "channel": "development",
        "versionCode": version_code,
        "versionName": version_name,
        "minimumSupportedVersionCode": 0,
        "mandatory": False,
        "publishedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "changes": read_changes(),
        "fixes": [],
        "apk": {
            "fileName": "school-debug.apk",
            "size": APK_OUTPUT.stat().st_size,
            "sha256": apk_sha256,
            "certificateSha256": certificate_sha256,
        },
    }

    manifest_path = DIST / "update-manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    sign_manifest(manifest_path)
    print(f"Development update package 已生成：{DIST}")


if __name__ == "__main__":
    main()
