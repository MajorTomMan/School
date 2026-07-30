#!/usr/bin/env python3
"""Deterministically prepare a page-by-page refinement candidate for PEP math 7-1.

The tool handles only auditable mechanical work. It never claims that a textbook
page was checked merely because a script ran. ``sourcePage`` always means the
printed textbook page (1..195); the bundled PDF has seven front-matter pages and
uses ``pageIndexOffset = 7`` when opening the corresponding PDF page.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import shutil
import sys
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any


TEXTBOOK_ID = "pep-math-7-1"
REFINEMENT_VERSION = 2
EXPECTED_PDF_PAGE_COUNT = 202
EXPECTED_PAGE_INDEX_OFFSET = 7
EXPECTED_PRINTED_PAGE_MIN = 1
EXPECTED_PRINTED_PAGE_MAX = 195
GENERIC_TITLE_PATTERN = re.compile(r"^教材第\d+页(?:（\d+）)?$")


@dataclass(frozen=True)
class SectionRule:
    pattern: str
    printed_start: int
    printed_end: int
    preferred_scene: str | None = None
    scene_data: dict[str, Any] | None = None


# These are printed textbook pages, not PDF indices. PDF index = sourcePage + 7.
SECTION_RULES: tuple[SectionRule, ...] = (
    SectionRule(r"^第一章", 1, 1),
    SectionRule(r"^1\.1\s*正数和负数", 2, 6, "opposite_quantities", {"scene": "temperature", "scenes": ["temperature", "account", "change", "deviation", "elevation", "tolerance"]}),
    SectionRule(r"^1\.2\.1\s*有理数的概念", 7, 7, "rational_classification", {}),
    SectionRule(r"^1\.2\.2\s*数轴", 8, 10, "number_line", {"mode": "concept"}),
    SectionRule(r"^1\.2\.3\s*相反数", 11, 12, "opposite_numbers", {}),
    SectionRule(r"^1\.2\.4\s*绝对值", 13, 13, "absolute_value", {}),
    SectionRule(r"^1\.2\.5\s*有理数大小比较", 14, 20, "number_comparison", {}),
    SectionRule(r"第一章.*(?:小结|复习)|有理数.*(?:小结|复习)", 21, 23),

    SectionRule(r"^第二章", 24, 24),
    SectionRule(r"^2\.1\.1\s*有理数的加法", 25, 29, "addition_process", {}),
    SectionRule(r"^2\.1\.2\s*有理数的减法", 30, 37, "subtraction_transform", {"expression": "a-b=a+(-b)"}),
    SectionRule(r"^2\.2\.1\s*有理数的乘法", 38, 42, "multiplication_sign", {}),
    SectionRule(r"^2\.2\.2\s*有理数的除法", 43, 50, "division_transform", {"expression": "a÷b=a×1/b（b≠0）"}),
    SectionRule(r"^2\.3\.1\s*乘方", 51, 53, "power_process", {}),
    SectionRule(r"^2\.3\.2\s*科学记数法", 54, 56),
    SectionRule(r"^2\.3\.3\s*近似数", 56, 58, "number_line", {"mode": "rounding"}),
    SectionRule(r"第二章.*(?:小结|复习)|有理数的运算.*(?:小结|复习)", 59, 67),

    SectionRule(r"^第三章", 68, 68),
    SectionRule(r"^3\.1\s*列代数式", 69, 78),
    SectionRule(r"^3\.2\s*代数式的值", 79, 84, "data_chart", {}),
    SectionRule(r"第三章.*(?:小结|复习)|代数式.*(?:小结|复习)", 85, 87),

    SectionRule(r"^第四章", 88, 88),
    SectionRule(r"^4\.1\s*整式", 89, 94),
    SectionRule(r"^4\.2\s*整式的加法与减法", 95, 106),
    SectionRule(r"第四章.*(?:小结|复习)|整式的加减.*(?:小结|复习)", 107, 109),

    SectionRule(r"^第五章", 110, 110),
    SectionRule(r"^5\.1\.1\s*从算式到方程", 111, 114),
    SectionRule(r"^5\.1\.2\s*等式的性质", 115, 119),
    SectionRule(r"^5\.2\s*解一元一次方程", 120, 135),
    SectionRule(r"^5\.3\s*实际问题", 136, 144),
    SectionRule(r"第五章.*(?:小结|复习)|一元一次方程.*(?:小结|复习)", 145, 148),

    SectionRule(r"^第六章", 149, 149),
    SectionRule(r"^6\.1\s*几何图形$", 150, 150, "geometry", {}),
    SectionRule(r"^6\.1\.1\s*立体图形与平面图形", 151, 157, "geometry", {}),
    SectionRule(r"^6\.1\.2\s*点、线、面、体", 158, 161, "geometry", {}),
    SectionRule(r"^6\.2\.1\s*直线、射线、线段", 162, 169, "geometry", {}),
    SectionRule(r"^6\.2\.2\s*线段的比较与运算", 170, 176, "geometry", {}),
    SectionRule(r"^6\.3\.1\s*角", 177, 182, "geometry", {}),
    SectionRule(r"^6\.3\.2\s*角的比较与运算", 183, 189, "geometry", {}),
    SectionRule(r"^6\.3\.3\s*余角和补角", 190, 192, "geometry", {}),
    SectionRule(r"第六章.*(?:小结|复习)|几何图形初步.*(?:小结|复习)", 193, 195),
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
    "data_chart",
    "geometry",
}

TYPOGRAPHY_REPLACEMENTS: tuple[tuple[re.Pattern[str], str], ...] = (
    (re.compile(r"(?<=\d)\s*[xX*]\s*(?=[A-Za-z\d(])"), "×"),
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
    printed_pages_covered: int = 0
    missing_printed_pages: list[int] | None = None
    generic_titles: int = 0
    scenes_inserted: int = 0
    typography_changes: int = 0
    assessments_checked: int = 0
    knowledge_points_checked: int = 0
    errors: list[str] | None = None
    warnings: list[str] | None = None

    def __post_init__(self) -> None:
        self.missing_printed_pages = [] if self.missing_printed_pages is None else self.missing_printed_pages
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
    label = " ".join(
        value for value in (
            str(section.get("number", "")).strip(),
            str(section.get("title", "")).strip(),
        ) if value
    )
    label = re.sub(r"\s+", " ", label)
    return next((rule for rule in SECTION_RULES if re.search(rule.pattern, label)), None)


def assign_source_pages(pages: list[dict[str, Any]], start: int, end: int) -> int:
    if not pages:
        return 0
    span = max(0, end - start)
    changed = 0
    for index, page in enumerate(pages):
        source_page = start if len(pages) == 1 else start + round(span * index / (len(pages) - 1))
        if page.get("sourcePage") != source_page:
            page["sourcePage"] = source_page
            changed += 1
    return changed


def page_contains_scene(page: dict[str, Any], template: str) -> bool:
    return any(
        isinstance(block, dict)
        and block.get("type") == "scene"
        and block.get("template") == template
        for block in page.get("blocks", [])
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
            page for page in pages
            if any(isinstance(block, dict) and block.get("type") in {"text", "example"} for block in page.get("blocks", []))
        ),
        pages[0],
    )
    blocks = target.setdefault("blocks", [])
    insertion = next((index for index, block in enumerate(blocks) if block.get("type") == "conclusion"), len(blocks))
    blocks.insert(insertion, {"type": "scene", "template": template, "data": copy.deepcopy(rule.scene_data or {})})
    report.scenes_inserted += 1


def page_digest(page: dict[str, Any]) -> str:
    review_value = {key: value for key, value in page.items() if key not in {"reviewStatus", "refinement"}}
    payload = json.dumps(review_value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def load_verification_manifest(path: Path | None) -> dict[str, str]:
    if path is None:
        return {}
    data = read_json(path)
    entries = data.get("verifiedPages", data) if isinstance(data, dict) else {}
    if not isinstance(entries, dict):
        raise RefinementError("verification manifest 必须是 pageId 到 SHA-256 的对象")
    result: dict[str, str] = {}
    for page_id, digest in entries.items():
        digest_text = str(digest).lower().strip()
        if not re.fullmatch(r"[0-9a-f]{64}", digest_text):
            raise RefinementError(f"页面 {page_id} 的验证摘要不是 SHA-256")
        result[str(page_id)] = digest_text
    return result


def validate_textbook_contract(course: dict[str, Any], report: Report) -> None:
    textbook = course.get("textbook", {}) if isinstance(course, dict) else {}
    if textbook.get("id") != TEXTBOOK_ID:
        raise RefinementError(f"课程包不是 {TEXTBOOK_ID}：{textbook.get('id')!r}")
    pdf = textbook.get("pdf", {}) if isinstance(textbook.get("pdf"), dict) else {}
    offset = pdf.get("pageIndexOffset")
    if offset != EXPECTED_PAGE_INDEX_OFFSET:
        report.errors.append(f"textbook.pdf.pageIndexOffset 应为 {EXPECTED_PAGE_INDEX_OFFSET}，实际为 {offset!r}")
    page_count = pdf.get("pageCount")
    if page_count is not None and page_count != EXPECTED_PDF_PAGE_COUNT:
        report.errors.append(f"教材 PDF 页数应为 {EXPECTED_PDF_PAGE_COUNT}，实际为 {page_count!r}")


def refine_pages(course: dict[str, Any], report: Report, verified_digests: dict[str, str]) -> None:
    chapters = course.get("chapters")
    if not isinstance(chapters, list) or len(chapters) != 6:
        raise RefinementError("七上课程包必须包含六章")

    previous_source_page = 0
    covered: set[int] = set()
    seen_page_ids: set[str] = set()
    for chapter in chapters:
        for section in chapter.get("sections", []):
            pages = section.get("pages", [])
            if not isinstance(pages, list):
                report.errors.append(f"小节 {section.get('title')} 的 pages 不是数组")
                continue
            rule = section_rule(section)
            if rule is None:
                report.warnings.append(f"未找到印刷页规则：{section.get('number', '')} {section.get('title', '')}")
            else:
                report.source_pages_assigned += assign_source_pages(pages, rule.printed_start, rule.printed_end)
                insert_preferred_scene(section, rule, report)

            for page in pages:
                report.pages += 1
                page_id = str(page.get("id", "")).strip()
                if not page_id:
                    report.errors.append("课程页面缺少 id")
                elif page_id in seen_page_ids:
                    report.errors.append(f"重复课程页面 id：{page_id}")
                seen_page_ids.add(page_id)

                source_page = page.get("sourcePage")
                if not isinstance(source_page, int):
                    report.errors.append(f"页面 {page_id or '<unknown>'} 缺少 sourcePage")
                elif not EXPECTED_PRINTED_PAGE_MIN <= source_page <= EXPECTED_PRINTED_PAGE_MAX:
                    report.errors.append(f"页面 {page_id} 的 sourcePage 越界：{source_page}")
                elif source_page < previous_source_page:
                    report.errors.append(f"页面 {page_id} 的 sourcePage 顺序倒退")
                else:
                    previous_source_page = source_page
                    covered.add(source_page)

                title = str(page.get("title", "")).strip()
                if not title:
                    report.errors.append(f"页面 {page_id} 缺少标题")
                elif GENERIC_TITLE_PATTERN.fullmatch(title):
                    report.generic_titles += 1
                    report.warnings.append(f"页面 {page_id} 仍是临时标题：{title}")

                current_digest = page_digest(page)
                expected_digest = verified_digests.get(page_id)
                status = "verified" if expected_digest == current_digest else "pending"
                if expected_digest is not None and expected_digest != current_digest:
                    report.warnings.append(f"页面 {page_id} 内容已变化，撤销旧的 verified 状态")
                page["reviewStatus"] = status
                page["refinement"] = {
                    "textbookId": TEXTBOOK_ID,
                    "version": REFINEMENT_VERSION,
                    "sourcePage": source_page,
                    "pdfPage": source_page + EXPECTED_PAGE_INDEX_OFFSET if isinstance(source_page, int) else None,
                    "contentSha256": current_digest,
                }
                if status == "verified":
                    report.verified_pages += 1
                else:
                    report.pending_pages += 1

    expected = set(range(EXPECTED_PRINTED_PAGE_MIN, EXPECTED_PRINTED_PAGE_MAX + 1))
    report.printed_pages_covered = len(covered)
    report.missing_printed_pages = sorted(expected - covered)
    if report.missing_printed_pages:
        report.errors.append("缺失教材印刷页：" + ", ".join(map(str, report.missing_printed_pages)))
    unknown_verified = sorted(set(verified_digests) - seen_page_ids)
    if unknown_verified:
        report.errors.append("验证清单包含未知页面：" + ", ".join(unknown_verified))


def knowledge_entries(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, list):
        return [item for item in data if isinstance(item, dict)]
    if isinstance(data, dict):
        return [item for item in data.get("knowledgePoints", []) if isinstance(item, dict)]
    return []


def assessment_entries(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, list):
        return [item for item in data if isinstance(item, dict)]
    if isinstance(data, dict):
        return [item for item in data.get("assessments", []) if isinstance(item, dict)]
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
        if not str(item.get("stem", item.get("question", ""))).strip():
            report.errors.append(f"题目 {item_id} 缺少题干")
        if item.get("answer", item.get("correctAnswer")) in (None, "", []):
            report.errors.append(f"题目 {item_id} 缺少标准答案")
        if not str(item.get("explanation", item.get("analysis", ""))).strip():
            report.errors.append(f"题目 {item_id} 缺少解析")
        linked = item.get("knowledgePointIds", item.get("knowledgePoints", []))
        if isinstance(linked, str):
            linked = [linked]
        if not linked:
            report.warnings.append(f"题目 {item_id} 未关联知识点")
        else:
            unknown = sorted(str(value) for value in linked if str(value) not in knowledge_ids)
            if unknown:
                report.errors.append(f"题目 {item_id} 引用了未知知识点：{', '.join(unknown)}")


def refine_pack(source: Path, output: Path, report_path: Path, verification_manifest: Path | None) -> Report:
    course = read_json(source / "course.json")
    knowledge = read_json(source / "knowledge-points.json")
    assessments = read_json(source / "assessments.json")
    report = Report(textbook_id=TEXTBOOK_ID, refinement_version=REFINEMENT_VERSION)

    course = normalize_value(course, report)
    knowledge = normalize_value(knowledge, report)
    assessments = normalize_value(assessments, report)
    validate_textbook_contract(course, report)
    verified_digests = load_verification_manifest(verification_manifest)
    refine_pages(course, report, verified_digests)
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
    parser.add_argument("output", type=Path, help="精校候选输出目录")
    parser.add_argument("--report", type=Path, default=Path("pep-math-7-1-refinement-report.json"))
    parser.add_argument(
        "--verification-manifest",
        type=Path,
        help="逐页人工核对清单；值必须是当前页面内容的 SHA-256，内容变化会自动撤销验证",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        report = refine_pack(args.source, args.output, args.report, args.verification_manifest)
    except RefinementError as exc:
        print(f"精校失败：{exc}", file=sys.stderr)
        return 2

    print(json.dumps(asdict(report), ensure_ascii=False, indent=2))
    if report.errors:
        print(f"精校候选仍有 {len(report.errors)} 个错误，禁止发布。", file=sys.stderr)
        return 1
    if report.pending_pages or report.generic_titles:
        print(
            f"仍有 {report.pending_pages} 页未核对、{report.generic_titles} 个临时标题，禁止标记完成。",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
