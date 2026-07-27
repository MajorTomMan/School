#!/usr/bin/env python3
"""Build six immutable junior-high mathematics releases for Cloudflare R2."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil

from build_course_release import file_spec, public_release_url, safe_identifier, write_deterministic_zip
from generate_math_courses import BOOKS, file_sha256
from normalize_course_contract import normalize_course


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--pdf-root", type=Path, required=True)
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
    source_root = args.source_root.resolve()
    pdf_root = args.pdf_root.resolve()
    output = args.output.resolve()
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)

    textbooks: list[dict[str, object]] = []
    for spec in BOOKS:
        source = source_root / spec.textbook_id / "course.json"
        pdf = pdf_root / spec.filename
        if not source.is_file():
            raise SystemExit(f"missing course source: {source}")
        if not pdf.is_file() or pdf.stat().st_size <= 0:
            raise SystemExit(f"missing textbook PDF: {pdf}")
        if file_sha256(pdf) != spec.sha256:
            raise SystemExit(f"textbook PDF digest mismatch: {pdf.name}")

        normalized = normalize_course(json.loads(source.read_text(encoding="utf-8")))
        textbook = normalized["textbook"]
        if textbook["id"] != spec.textbook_id:
            raise SystemExit(f"textbook id mismatch: {source}")
        if int(textbook["pdf"]["pageCount"]) != spec.page_count:
            raise SystemExit(f"textbook page count mismatch: {source}")

        book_root = output / spec.textbook_id
        book_root.mkdir(parents=True)
        course_output = book_root / "course.json"
        course_output.write_text(json.dumps(normalized, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        package_output = book_root / "course.zip"
        write_deterministic_zip(course_output, package_output)
        pdf_output = book_root / "textbook.pdf"
        shutil.copyfile(pdf, pdf_output)

        textbooks.append(
            {
                "id": spec.textbook_id,
                "package": file_spec(
                    f"{spec.textbook_id}.zip",
                    package_output,
                    public_release_url(args.public_base_url, release_id, spec.textbook_id, "course.zip"),
                ),
                "files": [
                    file_spec(
                        "course.json",
                        course_output,
                        public_release_url(args.public_base_url, release_id, spec.textbook_id, "course.json"),
                        True,
                    ),
                    file_spec(
                        str(textbook["pdf"]["path"]),
                        pdf_output,
                        public_release_url(args.public_base_url, release_id, spec.textbook_id, "textbook.pdf"),
                        False,
                    ),
                ],
            }
        )

    manifest = {"textbooks": textbooks}
    manifest_path = output / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"built {len(textbooks)} textbooks for release {release_id} in {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
