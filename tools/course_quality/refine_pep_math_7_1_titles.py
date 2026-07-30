#!/usr/bin/env python3
"""Refine generated page titles and printed-page coverage for PEP math 7-1.

This tool performs auditable structural refinement only. It does not mark any page
as manually verified. ``sourcePage`` remains the printed textbook page (1..195),
while the bundled PDF uses ``pageIndexOffset = 7``.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
import tempfile
from collections import Counter
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

TEXTBOOK_ID = "pep-math-7-1"
PRINTED_PAGE_MIN = 1
PRINTED_PAGE_MAX = 195
GENERIC_TITLE = re.compile(r"^教材第\d+页(?:（\d+）)?$")

BLOCK_LABELS = {
    "heading": "知识导入",
    "text": "知识讲解",
    "formula": "公式与法则",
    "example": "例题解析",
    "exercise": "课堂练习",
    "list": "要点整理",
    "conclusion": "归纳小结",
    "scene": "动态探究",
}

CONTENT_HINTS: tuple[tuple[re.Pattern[str], str], ...] = (
    (re.compile(r"例\s*\d+|例题"), "例题解析"),
    (re.compile(r"练习|巩固|习题"), "课堂练习"),
    (re.compile(r"探究|观察|思考|讨论"), "探究与思考"),
    (re.compile(r"归纳|小结|总结"), "归纳小结"),
    (re.compile(r"定义|叫做|称为"), "概念与定义"),
    (re.compile(r"性质|法则|规律"), "性质与法则"),
    (re.compile(r"证明|理由"), "推理与证明"),
    (re.compile(r"实际问题|应用"), "实际问题"),
)


@dataclass
class RefinementReport:
    pages: int = 0
    generic_titles_before: int = 0
    titles_replaced: int = 0
    generic_titles_after: int = 0
    printed_pages_before: int = 0
    printed_pages_after: int = 0
    missing_before: list[int] | None = None
    missing_after: list[int] | None = None
    source_pages_changed: int = 0
    errors: list[str] | None = None

    def __post_init__(self) -> None:
        self.missing_before = [] if self.missing_before is None else self.missing_before
        self.missing_after = [] if self.missing_after is None else self.missing_after
        self.errors = [] if self.errors is None else self.errors


def read_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise RuntimeError(f"缺少文件：{path}") from exc
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"JSON 格式错误：{path}:{exc.lineno}:{exc.colno} {exc.msg}") from exc


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def all_pages(course: dict[str, Any]) -> list[tuple[dict[str, Any], dict[str, Any], dict[str, Any]]]:
    result: list[tuple[dict[str, Any], dict[str, Any], dict[str, Any]]] = []
    for chapter in course.get("chapters", []):
        for section in chapter.get("sections", []):
            for page in section.get("pages", []):
                if isinstance(page, dict):
                    result.append((chapter, section, page))
    return result


def block_text(block: dict[str, Any]) -> str:
    pieces: list[str] = []
    for key in ("text", "title", "label", "statement", "expression", "result"):
        value = block.get(key)
        if isinstance(value, str):
            pieces.append(value)
    for key in ("items", "steps", "choices", "hints"):
        value = block.get(key)
        if isinstance(value, list):
            pieces.extend(str(item) for item in value if isinstance(item, (str, int, float)))
    return " ".join(pieces)


def page_text(page: dict[str, Any]) -> str:
    return " ".join(
        block_text(block)
        for block in page.get("blocks", [])
        if isinstance(block, dict)
    )


def primary_block_type(page: dict[str, Any]) -> str:
    counts: Counter[str] = Counter()
    for block in page.get("blocks", []):
        if not isinstance(block, dict):
            continue
        block_type = str(block.get("type", "")).strip()
        if block_type:
            weight = 3 if block_type in {"example", "exercise", "formula", "scene"} else 1
            counts[block_type] += weight
    return counts.most_common(1)[0][0] if counts else "text"


def concise_topic(text: str) -> str | None:
    cleaned = re.sub(r"\s+", "", text)
    cleaned = re.sub(r"^[（(]?\d+[）).、]", "", cleaned)
    segments = re.split(r"[。！？；：]", cleaned)
    for segment in segments:
        segment = segment.strip("，、（）()[]【】“”‘’")
        if 4 <= len(segment) <= 18:
            return segment
        if len(segment) > 18:
            return segment[:18]
    return None


def infer_title(section: dict[str, Any], page: dict[str, Any], occurrence: int) -> str:
    text = page_text(page)
    category = next((label for pattern, label in CONTENT_HINTS if pattern.search(text)), None)
    if category is None:
        category = BLOCK_LABELS.get(primary_block_type(page), "知识讲解")

    topic = concise_topic(text)
    section_title = str(section.get("title", "")).strip()
    section_number = str(section.get("number", "")).strip()
    prefix = " ".join(value for value in (section_number, section_title) if value)

    if topic and topic not in prefix:
        base = f"{category}：{topic}"
    elif prefix:
        base = f"{prefix}·{category}"
    else:
        base = category
    return base if occurrence == 1 else f"{base}（{occurrence}）"


def replace_generic_titles(course: dict[str, Any], report: RefinementReport) -> None:
    used: Counter[str] = Counter()
    for _, section, page in all_pages(course):
        report.pages += 1
        old_title = str(page.get("title", "")).strip()
        if not GENERIC_TITLE.fullmatch(old_title):
            used[old_title] += 1
            continue
        report.generic_titles_before += 1
        seed = infer_title(section, page, 1)
        used[seed] += 1
        new_title = infer_title(section, page, used[seed])
        page["title"] = new_title
        page.setdefault("refinement", {})["titleSource"] = "content-inference-pending-manual-review"
        report.titles_replaced += 1

    report.generic_titles_after = sum(
        1 for _, _, page in all_pages(course)
        if GENERIC_TITLE.fullmatch(str(page.get("title", "")).strip())
    )


def covered_pages(course: dict[str, Any]) -> set[int]:
    return {
        value
        for _, _, page in all_pages(course)
        for value in [page.get("sourcePage")]
        if isinstance(value, int) and PRINTED_PAGE_MIN <= value <= PRINTED_PAGE_MAX
    }


def repair_missing_source_pages(course: dict[str, Any], report: RefinementReport) -> None:
    expected = set(range(PRINTED_PAGE_MIN, PRINTED_PAGE_MAX + 1))
    before = covered_pages(course)
    report.printed_pages_before = len(before)
    report.missing_before = sorted(expected - before)

    pages = all_pages(course)
    for missing in report.missing_before:
        candidates: list[tuple[int, int, dict[str, Any]]] = []
        for index, (_, _, page) in enumerate(pages):
            current = page.get("sourcePage")
            if not isinstance(current, int):
                continue
            distance = abs(current - missing)
            candidates.append((distance, index, page))
        candidates.sort(key=lambda item: (item[0], item[1]))

        changed = False
        for _, index, page in candidates:
            previous_value = pages[index - 1][2].get("sourcePage") if index > 0 else PRINTED_PAGE_MIN
            next_value = pages[index + 1][2].get("sourcePage") if index + 1 < len(pages) else PRINTED_PAGE_MAX
            if not isinstance(previous_value, int) or not isinstance(next_value, int):
                continue
            if previous_value <= missing <= next_value:
                old = page.get("sourcePage")
                page["sourcePage"] = missing
                page.setdefault("refinement", {})["sourcePageRepair"] = {
                    "previous": old,
                    "assigned": missing,
                    "reason": "fill-missing-printed-page-pending-manual-review",
                }
                report.source_pages_changed += 1
                changed = True
                break
        if not changed:
            report.errors.append(f"无法在不破坏顺序的情况下补齐印刷页 {missing}")

    after = covered_pages(course)
    report.printed_pages_after = len(after)
    report.missing_after = sorted(expected - after)
    if report.missing_after:
        report.errors.append("仍缺失教材印刷页：" + ", ".join(map(str, report.missing_after)))


def validate_contract(course: dict[str, Any], report: RefinementReport) -> None:
    textbook = course.get("textbook", {})
    if textbook.get("id") != TEXTBOOK_ID:
        report.errors.append(f"课程包不是 {TEXTBOOK_ID}")
    pdf = textbook.get("pdf", {}) if isinstance(textbook.get("pdf"), dict) else {}
    if pdf.get("pageIndexOffset") != 7:
        report.errors.append("textbook.pdf.pageIndexOffset 必须为 7")

    previous = PRINTED_PAGE_MIN
    ids: set[str] = set()
    for _, _, page in all_pages(course):
        page_id = str(page.get("id", "")).strip()
        if not page_id:
            report.errors.append("课程页面缺少 id")
        elif page_id in ids:
            report.errors.append(f"重复课程页面 id：{page_id}")
        ids.add(page_id)
        source = page.get("sourcePage")
        if not isinstance(source, int) or not PRINTED_PAGE_MIN <= source <= PRINTED_PAGE_MAX:
            report.errors.append(f"页面 {page_id or '<unknown>'} 的 sourcePage 无效：{source!r}")
        elif source < previous:
            report.errors.append(f"页面 {page_id} 的 sourcePage 顺序倒退：{previous} → {source}")
        else:
            previous = source


def refine(source: Path, output: Path, report_path: Path) -> RefinementReport:
    course_path = source / "course.json"
    course = read_json(course_path)
    if not isinstance(course, dict):
        raise RuntimeError("course.json 根节点必须是对象")

    report = RefinementReport()
    replace_generic_titles(course, report)
    repair_missing_source_pages(course, report)
    validate_contract(course, report)

    with tempfile.TemporaryDirectory(prefix="pep-math-7-1-title-refine-") as temporary:
        staging = Path(temporary) / "pack"
        shutil.copytree(source, staging)
        write_json(staging / "course.json", course)
        write_json(staging / "title-refinement-report.json", asdict(report))
        if output.exists():
            shutil.rmtree(output)
        shutil.copytree(staging, output)

    report_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(report_path, asdict(report))
    return report


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="精校七上自动标题并补齐印刷页覆盖")
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--report", type=Path, default=Path("pep-math-7-1-title-report.json"))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        report = refine(args.source, args.output, args.report)
    except RuntimeError as exc:
        print(f"精校失败：{exc}", file=sys.stderr)
        return 2

    print(json.dumps(asdict(report), ensure_ascii=False, indent=2))
    if report.errors or report.generic_titles_after or report.missing_after:
        print("结构精校仍有阻断项，禁止发布。", file=sys.stderr)
        return 1
    print("结构精校候选已生成；页面仍须逐页人工对照 PDF 后才能标记 verified。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
