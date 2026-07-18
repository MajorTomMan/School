#!/usr/bin/env python3
"""Build a School cloud-course release containing course data and its textbook PDF."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil
import zipfile


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def drive_download_url(file_id: str) -> str:
    value = file_id.strip()
    return f"https://drive.google.com/uc?export=download&id={value}" if value else ""


def safe_relative_path(value: str) -> str:
    normalized = value.strip().replace("\\", "/")
    path = PurePosixPath(normalized)
    if not normalized or path.is_absolute() or any(part in {"", ".", ".."} for part in path.parts):
        raise SystemExit(f"unsafe package path: {value}")
    return path.as_posix()


def file_spec(path: str, local_file: Path, file_id: str) -> dict[str, object]:
    return {
        "path": path,
        "url": drive_download_url(file_id),
        "size": local_file.stat().st_size,
        "sha256": sha256(local_file),
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True, help="Path to source course.json")
    parser.add_argument("--pdf", type=Path, required=True, help="Path to the textbook PDF")
    parser.add_argument("--output", type=Path, required=True, help="Release output directory")
    parser.add_argument("--textbook-version", type=int, default=1)
    parser.add_argument("--content-version", type=int, default=1)
    parser.add_argument("--minimum-app-version", type=int, default=1)
    parser.add_argument("--full-file-id", default="", help="Google Drive ID of the uploaded ZIP")
    parser.add_argument("--course-file-id", default="", help="Google Drive ID of the uploaded course.json")
    parser.add_argument("--pdf-file-id", default="", help="Google Drive ID of the uploaded textbook PDF")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source = args.source.resolve()
    pdf_source = args.pdf.resolve()
    if not source.is_file():
        raise SystemExit(f"course source not found: {source}")
    if not pdf_source.is_file():
        raise SystemExit(f"textbook PDF not found: {pdf_source}")
    with pdf_source.open("rb") as stream:
        if stream.read(5) != b"%PDF-":
            raise SystemExit("--pdf does not point to a PDF file")

    payload = json.loads(source.read_text(encoding="utf-8"))
    if payload.get("schemaVersion") != 1:
        raise SystemExit("only schemaVersion=1 is supported")
    textbook = payload.get("textbook") or {}
    textbook_id = str(textbook.get("id") or "").strip()
    textbook_title = str(textbook.get("title") or "").strip()
    if not textbook_id or not textbook_title:
        raise SystemExit("textbook.id and textbook.title are required")

    pdf_metadata = textbook.get("pdf") or {}
    pdf_path = safe_relative_path(str(pdf_metadata.get("path") or "assets/textbook.pdf"))
    if not pdf_path.lower().endswith(".pdf"):
        raise SystemExit("textbook.pdf.path must end with .pdf")
    page_count = int(pdf_metadata.get("pageCount") or 0)
    if page_count <= 0:
        raise SystemExit("textbook.pdf.pageCount must be greater than zero")
    page_index_offset = int(pdf_metadata.get("pageIndexOffset") or 0)
    if not -10_000 <= page_index_offset <= 10_000:
        raise SystemExit("textbook.pdf.pageIndexOffset is out of range")

    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    pdf_output = output / pdf_path
    pdf_output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(pdf_source, pdf_output)

    textbook["pdf"] = {
        "path": pdf_path,
        "sha256": sha256(pdf_output),
        "pageCount": page_count,
        "pageIndexOffset": page_index_offset,
    }
    payload["textbook"] = textbook
    course_output = output / "course.json"
    course_output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    package_name = f"{textbook_id}-v{args.textbook_version}.zip"
    package_path = output / package_name
    with zipfile.ZipFile(package_path, "w", compression=zipfile.ZIP_DEFLATED, allowZip64=True) as archive:
        archive.write(course_output, arcname="course.json")
        archive.write(pdf_output, arcname=pdf_path)

    manifest = {
        "schemaVersion": 1,
        "contentVersion": args.content_version,
        "generatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "textbooks": [
            {
                "id": textbook_id,
                "title": textbook_title,
                "version": args.textbook_version,
                "minimumAppVersion": args.minimum_app_version,
                "fullPackage": file_spec(package_name, package_path, args.full_file_id),
                "files": [
                    file_spec("course.json", course_output, args.course_file_id),
                    file_spec(pdf_path, pdf_output, args.pdf_file_id),
                ],
                "deletedFiles": [],
            }
        ],
    }
    manifest_path = output / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"course:   {course_output}")
    print(f"PDF:      {pdf_output}")
    print(f"full ZIP: {package_path}")
    print(f"manifest: {manifest_path}")
    if not args.full_file_id or not args.course_file_id or not args.pdf_file_id:
        print("note: upload course.json, the PDF and the ZIP to Google Drive, then rerun with all file IDs")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
