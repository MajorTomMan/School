#!/usr/bin/env python3
"""Apply manually reviewed PEP math 7-1 page-title overrides to a course pack."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

TEXTBOOK_ID = "pep-math-7-1"
GENERIC_TITLE = re.compile(r"^教材第\d+页(?:（\d+）)?$")


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def iter_pages(course: dict[str, Any]):
    for chapter in course.get("chapters", []):
        for section in chapter.get("sections", []):
            for page in section.get("pages", []):
                if isinstance(page, dict):
                    yield chapter, section, page


def apply(course: dict[str, Any], override_document: dict[str, Any]) -> dict[str, int]:
    if course.get("textbook", {}).get("id") != TEXTBOOK_ID:
        raise ValueError(f"课程包不是 {TEXTBOOK_ID}")
    if override_document.get("textbookId") != TEXTBOOK_ID:
        raise ValueError("标题覆盖文件的 textbookId 不匹配")

    overrides = override_document.get("overrides")
    if not isinstance(overrides, dict) or not overrides:
        raise ValueError("标题覆盖文件没有 overrides")

    pages = {str(page.get("id", "")): page for _, _, page in iter_pages(course)}
    unknown = sorted(set(overrides) - set(pages))
    if unknown:
        raise ValueError("标题覆盖包含未知页面：" + ", ".join(unknown))

    changed = 0
    already_applied = 0
    for page_id, title in overrides.items():
        title = str(title).strip()
        if not title or GENERIC_TITLE.fullmatch(title):
            raise ValueError(f"页面 {page_id} 的目标标题无效：{title!r}")
        page = pages[page_id]
        if page.get("title") == title:
            already_applied += 1
            continue
        page["title"] = title
        refinement = page.setdefault("refinement", {})
        refinement["titleReview"] = "manual-page-content-review"
        changed += 1

    remaining_generic = sum(
        1 for _, _, page in iter_pages(course)
        if GENERIC_TITLE.fullmatch(str(page.get("title", "")).strip())
    )
    return {
        "overrides": len(overrides),
        "changed": changed,
        "alreadyApplied": already_applied,
        "remainingGenericTitles": remaining_generic,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="应用七上逐页人工标题精校结果")
    parser.add_argument("course", type=Path)
    parser.add_argument("overrides", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    course = read_json(args.course)
    override_document = read_json(args.overrides)
    report = apply(course, override_document)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    write_json(args.output, course)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
