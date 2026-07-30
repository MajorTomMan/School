#!/usr/bin/env python3
"""Verify the page-by-page coverage of the PEP grade-7 semester-1 course.

`course.json` stores printed textbook page numbers in ``sourcePage``. The PDF has
seven front-matter pages, exposed through ``textbook.pdf.pageIndexOffset == 7``.
Therefore printed page 1 maps to PDF page index 8. This verifier deliberately
keeps those two coordinate systems separate so a refinement tool cannot shift
all authored pages by seven pages accidentally.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

TEXTBOOK_ID = "pep-math-7-1"
EXPECTED_PDF_PAGE_COUNT = 202
EXPECTED_PAGE_INDEX_OFFSET = 7
EXPECTED_PRINTED_PAGE_MIN = 1
EXPECTED_PRINTED_PAGE_MAX = 195


@dataclass(frozen=True)
class SectionPageContract:
    pattern: str
    printed_start: int
    printed_end: int


# Printed textbook page numbers, not raw PDF indices.
SECTION_CONTRACTS: tuple[SectionPageContract, ...] = (
    SectionPageContract(r"^第一章", 1, 1),
    SectionPageContract(r"^1\.1\s*正数和负数", 2, 6),
    SectionPageContract(r"^1\.2\.1\s*有理数的概念", 7, 8),
    SectionPageContract(r"^1\.2\.2\s*数轴", 8, 11),
    SectionPageContract(r"^1\.2\.3\s*相反数", 11, 12),
    SectionPageContract(r"^1\.2\.4\s*绝对值", 13, 13),
    SectionPageContract(r"^1\.2\.5\s*有理数.*大小比较", 14, 20),
    SectionPageContract(r"第一章.*(?:小结|复习)|^小结$", 21, 23),

    SectionPageContract(r"^第二章", 24, 24),
    SectionPageContract(r"^2\.1\.1\s*有理数的加法", 25, 29),
    SectionPageContract(r"^2\.1\.2\s*有理数的减法", 30, 37),
    SectionPageContract(r"^2\.2\.1\s*有理数的乘法", 38, 42),
    SectionPageContract(r"^2\.2\.2\s*有理数的除法", 43, 50),
    SectionPageContract(r"^2\.3\.1\s*乘方", 51, 53),
    SectionPageContract(r"^2\.3\.2\s*科学记数法.*2\.3\.3\s*近似数", 54, 58),
    SectionPageContract(r"第二章.*(?:小结|复习)|^小结$", 59, 67),

    SectionPageContract(r"^第三章", 68, 68),
    SectionPageContract(r"^3\.1\s*列代数式", 69, 78),
    SectionPageContract(r"^3\.2\s*代数式的值", 79, 84),
    SectionPageContract(r"第三章.*(?:小结|复习)|^小结$", 85, 87),

    SectionPageContract(r"^第四章", 88, 88),
    SectionPageContract(r"^4\.1\s*整式", 89, 94),
    SectionPageContract(r"^4\.2\s*整式的加法与减法", 95, 106),
    SectionPageContract(r"第四章.*(?:小结|复习)|^小结$", 107, 109),

    SectionPageContract(r"^第五章", 110, 110),
    SectionPageContract(r"^5\.1\.1\s*从算式到方程", 111, 114),
    SectionPageContract(r"^5\.1\.2\s*等式的性质", 115, 119),
    SectionPageContract(r"^5\.2\s*解一元一次方程", 120, 132),
    SectionPageContract(r"^5\.3\s*实际问题.*一元一次方程", 133, 144),
    SectionPageContract(r"第五章.*(?:小结|复习)|^小结$", 145, 148),

    SectionPageContract(r"^第六章", 149, 149),
    SectionPageContract(r"^6\.1\s*几何图形$", 150, 150),
    SectionPageContract(r"^6\.1\.1\s*立体图形与平面图形", 151, 154),
    SectionPageContract(r"^6\.1\.2\s*点、线、面、体", 155, 161),
    SectionPageContract(r"^6\.2\.1\s*直线、射线、线段", 162, 163),
    SectionPageContract(r"^6\.2\.2\s*线段的比较与运算", 164, 169),
    SectionPageContract(r"^6\.3\.1\s*角的概念", 170, 172),
    SectionPageContract(r"^6\.3\.2\s*角的比较与运算", 173, 175),
    SectionPageContract(r"^6\.3\.3\s*余角和补角", 176, 183),
    SectionPageContract(r"第六章.*(?:小结|复习)|^小结$", 184, 195),
)


@dataclass
class Finding:
    severity: str
    code: str
    location: str
    message: str


@dataclass
class Report:
    textbook_id: str
    passed: bool
    authored_pages: int
    printed_pages_covered: int
    expected_printed_pages: int
    generic_title_pages: int
    findings: list[Finding]
    chapter_stats: list[dict[str, Any]]


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise SystemExit(f"缺少课程文件：{path}") from exc
    except json.JSONDecodeError as exc:
        raise SystemExit(f"JSON 格式错误：{path}:{exc.lineno}:{exc.colno}") from exc
    if not isinstance(value, dict):
        raise SystemExit("course.json 顶层必须是对象")
    return value


def section_label(section: dict[str, Any]) -> str:
    values = [str(section.get("number", "")).strip(), str(section.get("title", "")).strip()]
    return re.sub(r"\s+", " ", " ".join(value for value in values if value))


def match_contract(section: dict[str, Any], chapter_index: int, section_index: int) -> SectionPageContract | None:
    label = section_label(section)
    # "小结" repeats in every chapter. Resolve it by ordinal range after all
    # chapter-specific patterns have had a chance to match.
    matches = [contract for contract in SECTION_CONTRACTS if re.search(contract.pattern, label)]
    if not matches:
        return None
    if label == "小结":
        summary_ranges = {
            1: (21, 23), 2: (59, 67), 3: (85, 87),
            4: (107, 109), 5: (145, 148), 6: (184, 195),
        }
        start, end = summary_ranges[chapter_index]
        return SectionPageContract(r"^小结$", start, end)
    return matches[0]


def verify(course: dict[str, Any]) -> Report:
    findings: list[Finding] = []
    textbook = course.get("textbook", {})
    if textbook.get("id") != TEXTBOOK_ID:
        findings.append(Finding("error", "textbook.id", "textbook", f"预期 {TEXTBOOK_ID}"))

    pdf = textbook.get("pdf", {})
    if pdf.get("pageCount") != EXPECTED_PDF_PAGE_COUNT:
        findings.append(Finding("error", "pdf.pageCount", "textbook.pdf", "PDF 应为 202 页"))
    if pdf.get("pageIndexOffset") != EXPECTED_PAGE_INDEX_OFFSET:
        findings.append(Finding("error", "pdf.pageIndexOffset", "textbook.pdf", "七页前置内容后，正文第1页应映射到PDF第8页"))

    authored_pages = 0
    generic_title_pages = 0
    covered: Counter[int] = Counter()
    previous_source = 0
    chapter_stats: list[dict[str, Any]] = []

    chapters = course.get("chapters", [])
    if not isinstance(chapters, list) or len(chapters) != 6:
        findings.append(Finding("error", "chapters.count", "chapters", "七上必须完整包含六章"))
        chapters = chapters if isinstance(chapters, list) else []

    for ci, chapter in enumerate(chapters, start=1):
        chapter_pages = 0
        chapter_scenes = 0
        for si, section in enumerate(chapter.get("sections", []), start=1):
            location = f"chapter[{ci}]/section[{si}]"
            contract = match_contract(section, ci, si)
            if contract is None:
                findings.append(Finding("error", "section.contract.missing", location, f"未识别小节：{section_label(section)}"))
                continue
            pages = section.get("pages", [])
            if not isinstance(pages, list) or not pages:
                findings.append(Finding("error", "section.pages.empty", location, "小节没有课程页面"))
                continue

            seen_in_section: set[int] = set()
            for pi, page in enumerate(pages, start=1):
                page_location = f"{location}/page[{pi}]"
                authored_pages += 1
                chapter_pages += 1
                source = page.get("sourcePage")
                if not isinstance(source, int):
                    findings.append(Finding("error", "sourcePage.missing", page_location, "缺少印刷页码 sourcePage"))
                    continue
                if not contract.printed_start <= source <= contract.printed_end:
                    findings.append(Finding(
                        "error", "sourcePage.outside_section", page_location,
                        f"印刷页码 {source} 不在小节范围 {contract.printed_start}～{contract.printed_end}",
                    ))
                if source < previous_source:
                    findings.append(Finding("error", "sourcePage.order", page_location, f"页码从 {previous_source} 倒退到 {source}"))
                previous_source = source
                covered[source] += 1
                seen_in_section.add(source)

                title = str(page.get("title", "")).strip()
                if re.fullmatch(r"教材第\d+页（\d+）", title):
                    generic_title_pages += 1
                blocks = page.get("blocks", [])
                chapter_scenes += sum(1 for block in blocks if isinstance(block, dict) and block.get("type") == "scene")

            missing_in_section = sorted(set(range(contract.printed_start, contract.printed_end + 1)) - seen_in_section)
            if missing_in_section:
                findings.append(Finding(
                    "error", "sourcePage.section_gap", location,
                    f"小节未覆盖印刷页：{', '.join(map(str, missing_in_section))}",
                ))

        chapter_stats.append({
            "number": chapter.get("number", ci),
            "title": chapter.get("title", ""),
            "authoredPages": chapter_pages,
            "scenes": chapter_scenes,
        })

    missing_book_pages = sorted(set(range(EXPECTED_PRINTED_PAGE_MIN, EXPECTED_PRINTED_PAGE_MAX + 1)) - set(covered))
    if missing_book_pages:
        findings.append(Finding(
            "error", "sourcePage.book_gap", "course.json",
            f"全书未覆盖印刷页：{', '.join(map(str, missing_book_pages))}",
        ))

    # Generic generated titles are a refinement backlog, not a structural error.
    if generic_title_pages:
        findings.append(Finding(
            "warning", "page.title.generic", "course.json",
            f"仍有 {generic_title_pages} 个页面使用“教材第N页（序号）”临时标题，逐页精校时必须改为内容标题",
        ))

    return Report(
        textbook_id=str(textbook.get("id", "")),
        passed=not any(item.severity == "error" for item in findings),
        authored_pages=authored_pages,
        printed_pages_covered=len(covered),
        expected_printed_pages=EXPECTED_PRINTED_PAGE_MAX,
        generic_title_pages=generic_title_pages,
        findings=findings,
        chapter_stats=chapter_stats,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="校验七上课程包的逐页教材覆盖")
    parser.add_argument("course_json", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--warnings-as-errors", action="store_true")
    args = parser.parse_args()

    report = verify(read_json(args.course_json))
    rendered = json.dumps(asdict(report), ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        sys.stdout.write(rendered)

    if not report.passed:
        return 1
    if args.warnings_as_errors and any(item.severity == "warning" for item in report.findings):
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
