#!/usr/bin/env python3
"""Build one immutable Cloudflare R2 course release using the current APK manifest contract."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import zipfile

from normalize_course_contract import normalize_course


IDENTIFIER = re.compile(r"^[A-Za-z0-9._-]+$")


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
        "path": path,
        "url": url,
        "size": source.stat().st_size,
        "sha256": sha256(source),
    }
    if bundled is not None:
        result["bundled"] = bundled
    return result


def write_deterministic_zip(source: Path, target: Path) -> None:
    info = zipfile.ZipInfo("course.json", date_time=(2020, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    with zipfile.ZipFile(target, "w", allowZip64=True) as archive:
        archive.writestr(info, source.read_bytes())


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True, help="Strict or legacy course.json")
    parser.add_argument("--pdf", type=Path, required=True, help="Verified textbook PDF")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--release-id", required=True)
    parser.add_argument(
        "--public-base-url",
        default="https://course.flashnamesl.workers.dev/cloud/course/public",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    release_id = safe_identifier(args.release_id, "release id")
    source = args.source.resolve()
    pdf = args.pdf.resolve()
    if not source.is_file():
        raise SystemExit(f"course source not found: {source}")
    if not pdf.is_file() or pdf.stat().st_size <= 0:
        raise SystemExit(f"textbook PDF not found: {pdf}")
    with pdf.open("rb") as stream:
        if stream.read(5) != b"%PDF-":
            raise SystemExit("--pdf does not point to a PDF file")

    normalized = normalize_course(json.loads(source.read_text(encoding="utf-8")))
    textbook = normalized["textbook"]
    textbook_id = safe_identifier(str(textbook["id"]), "textbook id")
    pdf_path = str(textbook["pdf"]["path"])

    output = args.output.resolve()
    if output.exists():
        shutil.rmtree(output)
    book_root = output / textbook_id
    book_root.mkdir(parents=True)

    course_output = book_root / "course.json"
    course_output.write_text(json.dumps(normalized, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    package_output = book_root / "course.zip"
    write_deterministic_zip(course_output, package_output)
    pdf_output = book_root / "textbook.pdf"
    shutil.copyfile(pdf, pdf_output)

    manifest = {
        "textbooks": [
            {
                "id": textbook_id,
                "package": file_spec(
                    f"{textbook_id}.zip",
                    package_output,
                    public_release_url(args.public_base_url, release_id, textbook_id, "course.zip"),
                ),
                "files": [
                    file_spec(
                        "course.json",
                        course_output,
                        public_release_url(args.public_base_url, release_id, textbook_id, "course.json"),
                        True,
                    ),
                    file_spec(
                        pdf_path,
                        pdf_output,
                        public_release_url(args.public_base_url, release_id, textbook_id, "textbook.pdf"),
                        False,
                    ),
                ],
            }
        ]
    }
    manifest_path = output / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"release id: {release_id}")
    print(f"manifest:   {manifest_path}")
    print(f"course:     {course_output}")
    print(f"package:    {package_output}")
    print(f"textbook:   {pdf_output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
