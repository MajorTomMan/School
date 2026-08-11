#!/usr/bin/env python3
"""Prepare textbook-derived course data for School.

Textbook wording, instructional stages and visual design are authored and reviewed manually.
This module only performs packaging work: it converts legacy crop placeholders, overlays
manually reviewed section files and attaches reviewed textbook figure/table references.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
from typing import Any

GENERIC_EXCERPT_TEXTS = (
    "教材原式",
    "教材原图",
    "教材图示",
    "教材示例",
    "教材例",
)

MANUAL_ROOT = Path(__file__).resolve().parent / "manual"
REFERENCE_FILE_NAME = "textbook-references.json"
MANUAL_EXTENSION_FILES = {
    "assessments.json",
    "knowledge-points.json",
    "asset-crops.json",
    "asset-decisions.json",
    "review.json",
    "review-decisions.json",
    REFERENCE_FILE_NAME,
}
REFERENCE_PATTERN = re.compile(r"(?:图|表)\s*[0-9０-９]+(?:[.．][0-9０-９]+)*\s*[-－—–]\s*[0-9０-９]+")
FULLWIDTH_DIGITS = str.maketrans("０１２３４５６７８９", "0123456789")


def iter_pages(course: dict[str, Any]):
    for chapter in course.get("chapters", []):
        for section in chapter.get("sections", []):
            for page in section.get("pages", []):
                yield chapter, section, page
        review = chapter.get("review") or {}
        for page in review.get("pages", []):
            yield chapter, review, page


def meaningful_fallback(value: str) -> bool:
    text = value.strip()
    if not text:
        return False
    if len(text) < 32 and any(text.startswith(prefix) for prefix in GENERIC_EXCERPT_TEXTS):
        return False
    return True


def textbook_text(value: str) -> dict[str, str]:
    return {"type": "textbook_text", "text": value.strip()}


def convert_excerpts(page: dict[str, Any]) -> int:
    converted = 0
    blocks: list[dict[str, Any]] = []
    for block in page.get("blocks", []):
        if block.get("type") != "source_excerpt":
            blocks.append(block)
            continue
        fallback = str(block.get("fallbackText") or "").strip()
        if meaningful_fallback(fallback):
            blocks.append(textbook_text(fallback))
        converted += 1
    page["blocks"] = blocks
    return converted


def replace_page_blocks(page: dict[str, Any], blocks: list[dict[str, Any]]) -> None:
    page["blocks"] = blocks


def curate_pep_7_1(course: dict[str, Any]) -> None:
    """Keep previously reviewed pages outside the manually overlaid 1.1 section."""
    pages = {page["id"]: page for _, _, page in iter_pages(course)}

    intro = pages.get("pep-math-7-1-01-01-p001-1")
    if intro:
        replace_page_blocks(intro, [
            textbook_text("在小学，我们从日常生活中的实例出发，学习了自然数、小数、分数及其运算。在日常生活、生产和科研中，还会遇到另外一些数的表示问题。例如："),
            {"type": "prompt", "text": "（1）北京冬季某一天的最高气温为零上3摄氏度，最低气温为零下3摄氏度。如何用数区分“零上3摄氏度”和“零下3摄氏度”？"},
            {"type": "prompt", "text": "（2）某公司今年7月份盈利50万元，8月份亏损10万元。该公司在记账时如何用数分别表示“盈利50万元”和“亏损10万元”？"},
            {"type": "prompt", "text": "（3）某年，我国棉花产量比上年增长7.8%，玉米产量比上年减少0.7%。统计这两种农作物产量的变化情况时，如何用数分别表示“增长7.8%”和“减少0.7%”？"},
            textbook_text("上面的问题都涉及意义相反的两个量，为了能用数表示像这样具有相反意义的两个量，需要引入负数。本章我们将认识负数的意义，把数的范围扩大到有理数，并在有理数范围内学习数的表示和大小比较等。"),
        ])

    rational_intro = pages.get("1.2.1-p07-a")
    if rational_intro:
        replace_page_blocks(rational_intro, [
            textbook_text("在小学阶段和上一节中，我们认识了很多数。回想一下，到目前为止，我们认识了哪些数？"),
            textbook_text("我们学习过正整数，如1，2，3，…；0；负整数，如−1，−2，−3，…。正整数、0、负整数统称为整数。"),
            textbook_text("我们还学习过正分数，如1/2，2/3，1又5/7，0.1，5.32，0.3循环，…；负分数，如−5/2，−2/3，−1/7，−0.5，−150.5，…。它们都是分数。"),
            textbook_text("事实上，有限小数和无限循环小数都可以化为分数，因此它们也可以看成分数。"),
        ])

    integer_fraction = pages.get("1.2.1-p07-b")
    if integer_fraction:
        replace_page_blocks(integer_fraction, [
            textbook_text("进一步地，正整数可以写成正分数的形式，例如2=2/1；负整数可以写成负分数的形式，例如−3=−3/1；0也可以写成分数的形式0/1。这样，整数可以写成分数的形式。"),
            {"type": "explanation", "text": "给任何整数补上分母1，它的大小没有改变，却显露出统一的分数形式。"},
            {"type": "visualization", "renderer": "integer_to_fraction", "params": {"title": "整数写成分数形式"}},
            {"type": "conclusion", "text": "整数不是分数之外的另一套数；它们都能写成分母不为0的分数形式。"},
        ])

    concept = pages.get("1.2.1-p07-c")
    if concept:
        replace_page_blocks(concept, [
            textbook_text("可以写成分数形式的数称为有理数（rational number）。其中，可以写成正分数形式的数为正有理数，可以写成负分数形式的数为负有理数。"),
            textbook_text("这样，引入负数后，我们对数的认识就扩大到了有理数范围。"),
            {"type": "explanation", "text": "判断一个数是否为有理数，关键不是它眼前写成整数、小数还是分数，而是它能否写成两个整数之比，且分母不为0。"},
        ])

    rational_example = pages.get("1.2.1-p07-d")
    if rational_example:
        replace_page_blocks(rational_example, [
            {
                "type": "worked_example",
                "label": "例1",
                "statement": "指出下列各数中的正有理数、负有理数，并分别指出其中的正整数、负整数：13，4.3，−3/8，8.5%，−30，−12%，1/9，−7.5，20，−60，1.2循环。",
                "steps": [
                    "正有理数：13，4.3，8.5%，1/9，20，1.2循环；其中正整数有13，20。",
                    "负有理数：−3/8，−30，−12%，−7.5，−60；其中负整数有−30，−60。",
                ],
                "result": "先按符号辨认正、负，再判断其中哪些本身是整数。",
            },
        ])


def manual_section_paths(directory: Path) -> list[Path]:
    """Return only manually reviewed course-section JSON files, never package extensions."""
    return [
        path
        for path in sorted(directory.glob("*.json"))
        if path.name not in MANUAL_EXTENSION_FILES
    ]


def source_range(section: dict[str, Any]) -> tuple[int, int] | None:
    pages = section.get("pages")
    if not isinstance(pages, list) or not pages:
        return None
    starts = [page.get("sourcePage") for page in pages if isinstance(page, dict) and isinstance(page.get("sourcePage"), int)]
    ends = [
        page.get("sourcePageEnd", page.get("sourcePage"))
        for page in pages
        if isinstance(page, dict) and isinstance(page.get("sourcePage"), int)
    ]
    if not starts or not ends:
        return None
    return min(starts), max(int(value) for value in ends if isinstance(value, int))


def apply_manual_sections(course: dict[str, Any]) -> int:
    textbook_id = str(course.get("textbook", {}).get("id") or "").strip()
    directory = MANUAL_ROOT / textbook_id
    if not directory.is_dir():
        return 0

    targets_by_section_id: dict[str, list[tuple[str, dict[str, Any], int, tuple[int, int] | None]]] = {}
    for chapter in course.get("chapters", []):
        chapter_id = str(chapter.get("id") or "").strip()
        for index, section in enumerate(chapter.get("sections", [])):
            section_id = str(section.get("id") or "").strip()
            targets_by_section_id.setdefault(section_id, []).append((chapter_id, chapter, index, source_range(section)))

    applied = 0
    for path in manual_section_paths(directory):
        override = json.loads(path.read_text(encoding="utf-8"))
        section_id = str(override.get("id") or "").strip()
        chapter_id = str(override.get("chapterId") or "").strip()
        if not section_id:
            raise SystemExit(f"{path}: manual section id is empty")
        candidates = targets_by_section_id.get(section_id, [])
        if chapter_id:
            candidates = [candidate for candidate in candidates if candidate[0] == chapter_id]
            if not candidates:
                raise SystemExit(f"{path}: section {section_id!r} was not found in chapter {chapter_id!r}")
        elif len(candidates) > 1:
            override_range = source_range(override)
            if override_range is not None:
                start, end = override_range
                ranged = [
                    candidate for candidate in candidates
                    if candidate[3] is not None and candidate[3][0] <= start and end <= candidate[3][1]
                ]
                if len(ranged) == 1:
                    candidates = ranged
        if not candidates:
            raise SystemExit(f"{path}: section {section_id!r} was not found in generated course")
        if len(candidates) != 1:
            chapters = ", ".join(candidate[0] or "<unknown>" for candidate in candidates)
            raise SystemExit(
                f"{path}: section {section_id!r} is ambiguous across chapters [{chapters}]; "
                "add chapterId to the manual section"
            )

        _, chapter, index, _ = candidates[0]
        pages = override.get("pages")
        if not isinstance(pages, list) or not pages:
            raise SystemExit(f"{path}: manual section has no pages")
        runtime_override = dict(override)
        runtime_override.pop("chapterId", None)
        chapter["sections"][index] = runtime_override
        applied += 1
    return applied


def normalize_reference_label(value: str) -> str:
    return (
        value.translate(FULLWIDTH_DIGITS)
        .replace("．", ".")
        .replace("－", "-")
        .replace("—", "-")
        .replace("–", "-")
        .replace(" ", "")
        .strip()
    )


def block_text_fragments(block: dict[str, Any]):
    for key in ("text", "statement", "result", "label"):
        value = block.get(key)
        if isinstance(value, str) and value:
            yield value
    for key in ("items", "steps", "choices", "hints"):
        values = block.get(key)
        if isinstance(values, list):
            for value in values:
                if isinstance(value, str) and value:
                    yield value


def load_reference_map(textbook_id: str) -> dict[str, int]:
    path = MANUAL_ROOT / textbook_id / REFERENCE_FILE_NAME
    if not path.is_file():
        return {}
    document = json.loads(path.read_text(encoding="utf-8"))
    if document.get("textbookId") != textbook_id:
        raise SystemExit(f"{path}: textbookId does not match {textbook_id}")
    raw = document.get("references")
    if not isinstance(raw, dict):
        raise SystemExit(f"{path}: references must be an object")
    result: dict[str, int] = {}
    for label, source_page in raw.items():
        normalized = normalize_reference_label(str(label))
        if not normalized or not isinstance(source_page, int) or source_page <= 0:
            raise SystemExit(f"{path}: invalid reference {label!r}: {source_page!r}")
        if normalized in result:
            raise SystemExit(f"{path}: duplicate reference {normalized}")
        result[normalized] = source_page
    return result


def attach_source_references(course: dict[str, Any]) -> int:
    textbook_id = str(course.get("textbook", {}).get("id") or "").strip()
    reference_map = load_reference_map(textbook_id)
    if not reference_map:
        return 0
    attached = 0
    for _, _, page in iter_pages(course):
        existing = page.get("sourceReferences")
        references = list(existing) if isinstance(existing, list) else []
        seen = {
            (str(item.get("label") or ""), item.get("sourcePage"))
            for item in references
            if isinstance(item, dict)
        }
        for block in page.get("blocks", []):
            if not isinstance(block, dict):
                continue
            for text in block_text_fragments(block):
                for match in REFERENCE_PATTERN.finditer(text):
                    normalized = normalize_reference_label(match.group(0))
                    source_page = reference_map.get(normalized)
                    if source_page is None:
                        continue
                    label = normalized
                    key = (label, source_page)
                    if key in seen:
                        continue
                    references.append({"label": label, "sourcePage": source_page})
                    seen.add(key)
                    attached += 1
        if references:
            page["sourceReferences"] = references
    return attached


def process_course(path: Path) -> tuple[int, int, int, int]:
    course = json.loads(path.read_text(encoding="utf-8"))
    converted = 0
    for _, _, page in iter_pages(course):
        converted += convert_excerpts(page)

    if course.get("textbook", {}).get("id") == "pep-math-7-1":
        curate_pep_7_1(course)

    manual_sections = apply_manual_sections(course)
    references = attach_source_references(course)

    remaining = sum(
        block.get("type") == "source_excerpt"
        for _, _, page in iter_pages(course)
        for block in page.get("blocks", [])
    )
    if remaining:
        raise SystemExit(f"{path}: {remaining} source_excerpt blocks remain")

    path.write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return converted, remaining, manual_sections, references


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    args = parser.parse_args()

    total_converted = 0
    total_manual = 0
    total_references = 0
    for path in sorted(args.source_root.glob("pep-math-*/course.json")):
        converted, _, manual_sections, references = process_course(path)
        total_converted += converted
        total_manual += manual_sections
        total_references += references
        print(
            f"{path.parent.name}: converted {converted} legacy excerpts, "
            f"applied {manual_sections} manually reviewed sections, "
            f"attached {references} textbook references"
        )
    print(
        f"converted total: {total_converted}; manual sections total: {total_manual}; "
        f"textbook references total: {total_references}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
