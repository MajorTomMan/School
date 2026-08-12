#!/usr/bin/env python3
"""Build one immutable Cloudflare R2 release from an authored course.json."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil

from course_release_bundle import collect_bundled_files, file_spec, public_release_url, safe_identifier, write_deterministic_zip


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True, help="Validated authored course.json")
    parser.add_argument("--pdf", type=Path, required=True, help="Verified textbook PDF")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--release-id", required=True)
    parser.add_argument("--public-base-url", default="https://course.flashnamesl.workers.dev/cloud/course/public")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    release_id = safe_identifier(args.release_id, "release id")
    source, pdf = args.source.resolve(), args.pdf.resolve()
    if not source.is_file():
        raise SystemExit(f"course source not found: {source}")
    if not pdf.is_file() or pdf.stat().st_size <= 0 or pdf.read_bytes()[:5] != b"%PDF-":
        raise SystemExit("--pdf does not point to a PDF file")

    course = json.loads(source.read_text(encoding="utf-8"))
    if set(course) != {"textbook", "knowledgePoints", "chapters"}:
        raise SystemExit("course source is not the authored Lesson contract")
    textbook = course["textbook"]
    textbook_id = safe_identifier(str(textbook["id"]), "textbook id")

    output = args.output.resolve()
    if output.exists():
        shutil.rmtree(output)
    book_root = output / textbook_id
    book_root.mkdir(parents=True)

    course_output = book_root / "course.json"
    course_output.write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    bundled = collect_bundled_files(book_root)
    package_output = book_root / "course.zip"
    write_deterministic_zip(bundled, package_output)

    pdf_output = book_root / "textbook.pdf"
    shutil.copyfile(pdf, pdf_output)
    files = [
        file_spec(path, file, public_release_url(args.public_base_url, release_id, textbook_id, path), True)
        for path, file in bundled.items()
    ]
    files.append(file_spec(
        str(textbook["pdf"]["path"]), pdf_output,
        public_release_url(args.public_base_url, release_id, textbook_id, "textbook.pdf"), False,
    ))
    manifest = {"textbooks": [{
        "id": textbook_id,
        "package": file_spec(
            f"{textbook_id}.zip", package_output,
            public_release_url(args.public_base_url, release_id, textbook_id, "course.zip"),
        ),
        "files": files,
    }]}
    manifest_path = output / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"release id: {release_id}\nmanifest: {manifest_path}\nbundled files: {len(bundled)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
