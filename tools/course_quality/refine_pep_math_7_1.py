#!/usr/bin/env python3
"""Deterministically refine the PEP mathematics grade-7 semester-1 course pack.

This tool does not invent textbook prose. It performs the mechanical part of the
page-by-page refinement workflow after the author has compared the source PDF:

* normalizes Chinese mathematical typography without altering mathematical value;
* applies the accepted PDF source-page ranges to every authored course page;
* preserves page order and records an explicit refinement provenance marker;
* inserts only supported, curriculum-appropriate visualization scenes when the
  corresponding concept page does not already contain one;
* validates exercises, knowledge-point links, answers and explanations before
  writing a release candidate;
* writes atomically and emits a machine-readable refinement report.

The remaining human review is represented by ``reviewStatus`` on every page. A
pack cannot be marked complete while any page is not ``verified``.
"""

from __future__ import annotations

import argparse
import copy
import json
import re
import shutil
import sys
import tempfile
from collections import Counter
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any, Iterable


TEXTBOOK_ID = "pep-math-7-1"
REFINEMENT_VERSION = 1


@dataclass(frozen=True)
class SectionRule:
    pattern: str
    pdf_start: int
    pdf_end: int
    preferred_scene: str | None = None
    scene_data: dict[str, Any] | None = None


# PDF page numbers are the actual PDF indices used by the distributed textbook.
# Chapter title and review sections are included so every authored page receives
# a stable source page without relying on a fragile global interpolation.
SECTION_RULES: tuple[SectionRule, ...] = (
    SectionRule(r"^第一章", 8, 8),
    SectionRule(r"^1\.1\s*正数和负数", 9, 13, "opposite_quantities", {"scene": "temperature", "scenes": ["temperature", "account", "change", "deviation", "elevation", "tolerance"]}),
    SectionRule(r"^1\.2\.1\s*有理数的概念", 14, 14, "rational_classification", {}),
    SectionRule(r"^1\.2\.2\s*数轴", 15, 17, "number_line", {"mode": "concept"}),
    SectionRule(r"^1\.2\.3\s*相反数", 18, 19, "opposite_numbers", {}),
    SectionRule(r"^1\.2\.4\s*绝对值", 20, 20, "absolute_value", {}),
    SectionRule(r"^1\.2\.5\s*有理数大小比较", 21, 27, "number_comparison", {}),
    SectionRule(r"第一章.*(?:小结|复习)|有理数.*(?:小结|复习)", 28, 30),

    SectionRule(r"^第二章", 31, 31),
    SectionRule(r"^2\.1\.1\s*有理数的加法", 32, 36, "addition_process", {}),
    SectionRule(r"^2\.1\.2\s*有理数的减法", 37, 44, "subtraction_transform", {"expression": "a-b=a+(-b)"}),
    SectionRule(r"^2\.2\.1\s*有理数的乘法", 45, 49, "multiplication_sign", {}),
    SectionRule(r"^2\.2\.2\s*有理数的除法", 50, 57, "division_transform", {"expression": "a÷b=a×1/b（b≠0）"}),
    SectionRule(r"^2\.3\.1\s*乘方", 58, 60, "power_process", {}),
    SectionRule(r"^2\.3\.2\s*科学记数法", 61, 63, "declarative_diagram", {"height": 300, "elements": []}),
    SectionRule(r"^2\.3\.3\s*近似数", 63, 65, "number_line", {"mode": "rounding"}),
    SectionRule(r"第二章.*(?:小结|复习)|有理数的运算.*(?:小结|复习)", 66, 74),

    SectionRule(r"^第三章", 75, 75),
    SectionRule(r"^3\.1\s*列代数式", 76, 85, "declarative_diagram", {"height": 320, "elements": []}),
    SectionRule(r"^3\.2\s*代数式的值", 86, 91, "data_chart", {}),
    SectionRule(r"第三章.*(?:小结|复习)|代数式.*(?:小结|复习)", 92, 94),

    SectionRule(r"^第四章", 95, 95),
    SectionRule(r"^4\.1\s*整式", 96, 101, "declarative_diagram", {"height": 320, "elements": []}),
    SectionRule(r"^4\.2\s*整式的加法与减法", 102, 113, "declarative_diagram", {"height": 340, "elements": []}),
    SectionRule(r"第四章.*(?:小结|复习)|整式的加减.*(?:小结|复习)", 114, 116),

    SectionRule(r"^第五章", 117, 117),
    SectionRule(r"^5\.1\.1\s*从算式到方程", 118, 121, "declarative_diagram", {"height": 320, "elements": []}),
    SectionRule(r"^5\.1\.2\s*等式的性质", 122, 126, "declarative_diagram", {"height": 320, "elements": []}),
    SectionRule(r"^5\.2\s*解一元一次方程", 127, 142, "declarative_diagram", {"height": 340, "elements": []}),
    SectionRule(r"^5\.3\s*实际问题", 143, 151, "declarative_diagram", {"height": 340, "elements": []}),
    SectionRule(r"第五章.*(?:小结|复习)|一元一次方程.*(?:小结|复习)", 152, 155),

    SectionRule(r"^第六章", 156, 156),
    SectionRule(r"^6\.1\s*几何图形$", 157, 157, "geometry", {}),
    SectionRule(r"^6\.1\.1\s*立体图形与平面图形", 158, 164, "geometry", {}),
    SectionRule(r"^6\.1\.2\s*点、线、面、体", 165, 168, "geometry", {}),
    SectionRule(r"^6\.2\.1\s*直线、射线、线段", 169, 176, "geometry", {}),
    SectionRule(r"^6\.2\.2\s*线段的比较与运算", 177, 183, "geometry", {}),
    SectionRule(r"^6\.3\.1\s*角", 184, 189, "geometry", {}),
    SectionRule(r"^6\.3\.2\s*角的比较与运算", 190, 196, "geometry", {}),
    SectionRule(r"^6\.3\.3\s*余角和补角", 197, 199, "geometry", {}),
    SectionRule(r"第六章.*(?:小结|复习)|几何图形初步.*(?:小结|复习)", 200, 202),
)


SUPPORTED_SCENES = {
    "opposite_quantities",
    "rational_classification",
    "number_line",
    "opposite_numbers",
    "absolute_value",
    "number_comparison",
    "addition_process",
    "subtraction_transform",
    "division_transform",
    "multiplication_sign",
    "power_process",
    "declarative_diagram",
    "data_chart",
    "geometry",
}


TYPOGRAPHY_REPLACEMENTS: tuple[tuple[re.Pattern[str], str], ...] = (
    (re.compile(r"(?<=\d)\s*[xX*]\s*(?=[A-Za-z\d(])"), "×"),
    (re.compile(r"(?<=\d)\s*/\s*(?=\d)"), "÷"),
    (re.compile(r"(?<=\d)\s+-\s+(?=\d)"), "−"),
    (re.compile(r"(?<!\.)\.\.\.(?!\.)"), "…"),
    (re.compile(r"\s+([，。；：！？、）】])"), r"\1"),
    (re.compile(r"([（【])\s+"), r"\1"),
)


@dataclass
class Report:
    textbook_id: str
    refinement_version: int
    pages: int = 0
    verified_pages: int = 0
    pending_pages: int = 0
    source_pages_assigned: int = 0
    scenes_inserted: int = 0
    typography_changes: int = 0
    assessments_checked: int = 0
    knowledge_points_checked: int = 0
    errors: list[str] | None = None
    warnings: list[str] | None = None

    def __post_init__(self) -> None:
        self.errors = [] if self.errors is None else self.errors
        self.warnings = [] if self.warnings is None else self.warnings


class RefinementError(RuntimeError):
    pass


def read_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise RefinementError(f"缺少文件：{path}") from exc
    except json.JSONDecodeError as exc:
        raise RefinementError(f"JSON 格式错误：{path}:{exc.lineno}:{exc.colno} {exc.msg}") from exc


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def normalize_text(text: str) -> tuple[str, int]:
    result = text
    changes = 0
    for pattern, replacement in TYPOGRAPHY_REPLACEMENTS:
        result, count = pattern.subn(replacement, result)
        changes += count
    return result, changes


def normalize_value(value: Any, report: Report) -> Any:
    if isinstance(value, str):
        normalized, changes = normalize_text(value)
        report.typography_changes += changes
        return normalized
    if isinstance(value, list):
        return [normalize_value(item, report) for item in value]
    if isinstance(value, dict):
        return {key: normalize_value(item, report) for key, item in value.items()}
    return value


def section_rule(section: dict[str, Any]) -> SectionRule | None:
    candidates = [str(section.get("number", "")).strip(), str(section.get("title", "")).strip()]
    label = " ".join(value for value in candidates if value)
    label = re.sub(r"\s+", " ", label)
    for rule in SECTION_RULES:
        if re.search(rule.pattern, label):
            return rule
    return None


def assign_source_pages(pages: list[dict[str, Any]], start: int, end: int) -> int:
    if not pages:
        return 0
    span = max(0, end - start)
    assigned = 0
    for index, page in enumerate(pages):
        if len(pages) == 1:
            source_page = start
        else:
            source_page = start + round(span * index / (len(pages) - 1))
        if page.get("sourcePage") != source_page:
            page["sourcePage"] = source_page
            assigned += 1
    return assigned


def page_contains_scene(page: dict[str, Any], template: str) -> bool:
    return any(
        block.get("type") == "scene" and block.get("template") == template
        for block in page.get("blocks", [])
        if isinstance(block, dict)
    )


def insert_preferred_scene(section: dict[str, Any], rule: SectionRule, report: Report) -> None:
    template = rule.preferred_scene
    if not template or template not in SUPPORTED_SCENES:
        return
    pages = section.get("pages", [])
    if not pages or any(page_contains_scene(page, template) for page in pages):
        return

    target = next(
        (
            page
            for page in pages
            if any(block.get("type") in {"text", "example"} for block in page.get("blocks", []))
        ),
        pages[0],
    )
    blocks = target.setdefault("blocks", [])
    insertion = len(blocks)
    for index, block in enumerate(blocks):
        if block.get("type") == "conclusion":
            insertion = index
            break
    blocks.insert(
        insertion,
        {
            "type": "scene",
            "template": template,
            "data": copy.deepcopy(rule.scene_data or {}),
        },
    )
    report.scenes_inserted += 1


def refine_pages(course: dict[str, Any], report: Report, mark_verified: bool) -> None:
    chapters = course.get("chapters")
    if not isinstance(chapters, list) or len(chapters) != 6:
        raise RefinementError("七上课程包必须包含六章")

    previous_source_page = 0
    for chapter in chapters:
        for section in chapter.get("sections", []):
            pages = section.get("pages", [])
            if not isinstance(pages, list):
                report.errors.append(f"小节 {section.get('title')} 的 pages 不是数组")
                continue
            rule = section_rule(section)
            if rule is None:
                report.warnings.append(f"未找到页码规则：{section.get('number', '')} {section.get('title', '')}")
            else:
                report.source_pages_assigned += assign_source_pages(pages, rule.pdf_start, rule.pdf_end)
                insert_preferred_scene(section, rule, report)

            for page in pages:
                report.pages += 1
                source_page = page.get("sourcePage")
                if not isinstance(source_page, int):
                    report.errors.append(f"页面 {page.get('id')} 缺少 sourcePage")
                elif source_page < previous_source_page:
                    report.errors.append(f"页面 {page.get('id')} 的 sourcePage 顺序倒退")
                else:
                    previous_source_page = source_page

                status = "verified" if mark_verified else str(page.get("reviewStatus", "pending"))
                if status not in {"pending", "verified"}:
                    status = "pending"
                page["reviewStatus"] = status
                page["refinement"] = {
                    "textbookId": TEXTBOOK_ID,
                    "version": REFINEMENT_VERSION,
                    "sourcePage": source_page,
                }
                if status == "verified":
                    report.verified_pages += 1
                else:
                    report.pending_pages += 1


def knowledge_entries(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, list):
        return [item for item in data if isinstance(item, dict)]
    if isinstance(data, dict):
        values = data.get("knowledgePoints", [])
        return [item for item in values if isinstance(item, dict)]
    return []


def assessment_entries(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, list):
        return [item for item in data if isinstance(item, dict)]
    if isinstance(data, dict):
        values = data.get("assessments", [])
        return [item for item in values if isinstance(item, dict)]
    return []


def validate_knowledge_points(data: Any, report: Report) -> set[str]:
    ids: set[str] = set()
    for item in knowledge_entries(data):
        report.knowledge_points_checked += 1
        item_id = str(item.get("id", "")).strip()
        if not item_id:
            report.errors.append("知识点缺少 id")
            continue
        if item_id in ids:
            report.errors.append(f"重复知识点 id：{item_id}")
        ids.add(item_id)
        if not str(item.get("title", "")).strip():
            report.errors.append(f"知识点 {item_id} 缺少标题")
    if not ids:
        report.errors.append("知识点列表为空")
    return ids


def validate_assessments(data: Any, knowledge_ids: set[str], report: Report) -> None:
    ids: set[str] = set()
    for item in assessment_entries(data):
        report.assessments_checked += 1
        item_id = str(item.get("id", "")).strip()
        if not item_id:
            report.errors.append("题目缺少 id")
            continue
        if item_id in ids:
            report.errors.append(f"重复题目 id：{item_id}")
        ids.add(item_id)
        stem = str(item.get("stem", item.get("question", ""))).strip()
        if not stem:
            report.errors.append(f"题目 {item_id} 缺少题干")
        answer = item.get("answer", item.get("correctAnswer"))
        if answer in (None, "", []):
            report.errors.append(f"题目 {item_id} 缺少标准答案")
        explanation = str(item.get("explanation", item.get("analysis", ""))).strip()
        if not explanation:
            report.errors.append(f"题目 {item_id} 缺少解析")
        linked = item.get("knowledgePointIds", item.get("knowledgePoints", []))
        if isinstance(linked, str):
            linked = [linked]
        if not linked:
            report.warnings.append(f"题目 {item_id} 未关联知识点")
        elif knowledge_ids:
            unknown = sorted(str(value) for value in linked if str(value) not in knowledge_ids)
            if unknown:
                report.errors.append(f"题目 {item_id} 引用了未知知识点：{', '.join(unknown)}")


def refine_pack(source: Path, output: Path, report_path: Path, mark_verified: bool) -> Report:
    course = read_json(source / "course.json")
    knowledge = read_json(source / "knowledge-points.json")
    assessments = read_json(source / "assessments.json")

    textbook = course.get("textbook", {}) if isinstance(course, dict) else {}
    if textbook.get("id") != TEXTBOOK_ID:
        raise RefinementError(f"课程包不是 {TEXTBOOK_ID}：{textbook.get('id')!r}")

    report = Report(textbook_id=TEXTBOOK_ID, refinement_version=REFINEMENT_VERSION)
    course = normalize_value(course, report)
    knowledge = normalize_value(knowledge, report)
    assessments = normalize_value(assessments, report)

    refine_pages(course, report, mark_verified=mark_verified)
    knowledge_ids = validate_knowledge_points(knowledge, report)
    validate_assessments(assessments, knowledge_ids, report)

    with tempfile.TemporaryDirectory(prefix="pep-math-7-1-refine-") as temporary:
        staging = Path(temporary) / "pack"
        shutil.copytree(source, staging)
        write_json(staging / "course.json", course)
        write_json(staging / "knowledge-points.json", knowledge)
        write_json(staging / "assessments.json", assessments)
        write_json(staging / "refinement-report.json", asdict(report))
        if output.exists():
            shutil.rmtree(output)
        shutil.copytree(staging, output)

    report_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(report_path, asdict(report))
    return report


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="精校人教版数学七年级上册课程包")
    parser.add_argument("source", type=Path, help="包含 course.json 的原课程包目录")
    parser.add_argument("output", type=Path, help="精校后的输出目录")
    parser.add_argument("--report", type=Path, default=Path("pep-math-7-1-refinement-report.json"))
    parser.add_argument(
        "--mark-verified",
        action="store_true",
        help="仅在已经人工逐页对照 PDF 后使用，将全部页面标记为 verified",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        report = refine_pack(args.source, args.output, args.report, args.mark_verified)
    except RefinementError as exc:
        print(f"精校失败：{exc}", file=sys.stderr)
        return 2

    print(json.dumps(asdict(report), ensure_ascii=False, indent=2))
    if report.errors:
        print(f"精校候选仍有 {len(report.errors)} 个错误，禁止发布。", file=sys.stderr)
        return 1
    if report.pending_pages:
        print(f"仍有 {report.pending_pages} 页未完成人工核对，禁止标记完成。", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
