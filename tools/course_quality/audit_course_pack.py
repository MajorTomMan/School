#!/usr/bin/env python3
"""Audit a School course pack before publication.

The auditor is intentionally dependency-free so it can run in CI and on the
course-authoring machine. It validates course.json, knowledge-points.json and
assessments.json together, checks page order and coverage, and emits a concise
machine-readable report. PDF text comparison can be supplied through a
`pdftotext -layout` output file without introducing a PDF dependency here.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any, Iterable


ALLOWED_BLOCK_TYPES = {
    "heading",
    "text",
    "formula",
    "list",
    "example",
    "exercise",
    "conclusion",
    "scene",
}
TEXT_BLOCK_TYPES = {"heading", "text", "conclusion"}
REQUIRED_TEXT_STYLES = {"textbook", "explanation", "prompt", "caption", "history"}
BAD_MATH_PATTERNS = {
    "ascii_minus_between_numbers": re.compile(r"(?<=\d)\s-\s(?=\d)"),
    "ascii_multiply": re.compile(r"(?<=\d)\s*[xX*]\s*(?=[a-zA-Z\d(])"),
    "ascii_divide": re.compile(r"(?<=\d)\s*/\s*(?=\d)"),
    "double_space": re.compile(r"[^\n]\s{2,}[^\n]"),
}


@dataclass(frozen=True)
class Finding:
    severity: str
    code: str
    location: str
    message: str


class Audit:
    def __init__(self) -> None:
        self.findings: list[Finding] = []

    def add(self, severity: str, code: str, location: str, message: str) -> None:
        self.findings.append(Finding(severity, code, location, message))

    def error(self, code: str, location: str, message: str) -> None:
        self.add("error", code, location, message)

    def warning(self, code: str, location: str, message: str) -> None:
        self.add("warning", code, location, message)

    def info(self, code: str, location: str, message: str) -> None:
        self.add("info", code, location, message)


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise SystemExit(f"缺少文件：{path}") from exc
    except json.JSONDecodeError as exc:
        raise SystemExit(f"JSON 格式错误：{path}:{exc.lineno}:{exc.colno} {exc.msg}") from exc


def walk_course(course: dict[str, Any]) -> Iterable[tuple[int, dict[str, Any], int, dict[str, Any], int, dict[str, Any]]]:
    for chapter_index, chapter in enumerate(course.get("chapters", []), start=1):
        for section_index, section in enumerate(chapter.get("sections", []), start=1):
            for page_index, page in enumerate(section.get("pages", []), start=1):
                yield chapter_index, chapter, section_index, section, page_index, page


def text_fragments(block: dict[str, Any]) -> Iterable[str]:
    block_type = block.get("type")
    if block_type in TEXT_BLOCK_TYPES:
        yield str(block.get("text", ""))
    elif block_type == "formula":
        yield str(block.get("expression", ""))
        yield from (str(value) for value in block.get("conditions", []))
    elif block_type == "list":
        yield from (str(value) for value in block.get("items", []))
    elif block_type in {"example", "exercise"}:
        for key in ("label", "statement", "stem", "result", "answer", "analysis", "explanation"):
            value = block.get(key)
            if value:
                yield str(value)
        for key in ("steps", "choices", "hints"):
            yield from (str(value) for value in block.get(key, []))


def audit_course_structure(course: dict[str, Any], audit: Audit) -> dict[str, Any]:
    textbook = course.get("textbook")
    if not isinstance(textbook, dict):
        audit.error("course.textbook.missing", "course.json", "缺少 textbook 对象")
    chapters = course.get("chapters")
    if not isinstance(chapters, list) or not chapters:
        audit.error("course.chapters.missing", "course.json", "缺少章节列表")
        return {}

    ids: dict[str, str] = {}
    source_pages: list[tuple[int, str]] = []
    scene_counter: Counter[str] = Counter()
    block_counter: Counter[str] = Counter()
    chapter_stats: list[dict[str, Any]] = []

    def register_id(value: Any, location: str) -> None:
        if not isinstance(value, str) or not value.strip():
            audit.error("id.missing", location, "缺少稳定 id")
            return
        previous = ids.get(value)
        if previous:
            audit.error("id.duplicate", location, f"id {value!r} 已在 {previous} 使用")
        else:
            ids[value] = location

    for ci, chapter in enumerate(chapters, start=1):
        chapter_location = f"chapter[{ci}]"
        register_id(chapter.get("id"), chapter_location)
        sections = chapter.get("sections", [])
        if not sections:
            audit.error("chapter.sections.empty", chapter_location, "章节没有任何小节")
        pages_in_chapter = 0
        scenes_in_chapter = 0

        for si, section in enumerate(sections, start=1):
            section_location = f"{chapter_location}/section[{si}]"
            register_id(section.get("id"), section_location)
            pages = section.get("pages", [])
            if not pages:
                audit.warning("section.pages.empty", section_location, "小节没有课程页面")

            for pi, page in enumerate(pages, start=1):
                page_location = f"{section_location}/page[{pi}]"
                register_id(page.get("id"), page_location)
                pages_in_chapter += 1
                title = str(page.get("title", "")).strip()
                if not title:
                    audit.error("page.title.missing", page_location, "页面标题为空")

                source_page = page.get("sourcePage")
                if isinstance(source_page, int):
                    source_pages.append((source_page, page_location))
                else:
                    audit.warning("page.sourcePage.missing", page_location, "未标注教材来源页码")

                blocks = page.get("blocks")
                if not isinstance(blocks, list) or not blocks:
                    audit.error("page.blocks.empty", page_location, "页面没有内容块")
                    continue

                page_has_explanation = False
                page_has_conclusion = False
                for bi, block in enumerate(blocks, start=1):
                    block_location = f"{page_location}/block[{bi}]"
                    block_type = block.get("type")
                    block_counter[str(block_type)] += 1
                    if block_type not in ALLOWED_BLOCK_TYPES:
                        audit.error("block.type.invalid", block_location, f"未知内容块类型：{block_type!r}")
                        continue
                    if block_type == "text":
                        style = block.get("style")
                        if style not in REQUIRED_TEXT_STYLES:
                            audit.error("block.text.style.invalid", block_location, f"未知文字样式：{style!r}")
                        page_has_explanation |= style in {"textbook", "explanation"}
                    if block_type == "conclusion":
                        page_has_conclusion = True
                    if block_type == "scene":
                        template = str(block.get("template", "")).strip()
                        scene_counter[template] += 1
                        scenes_in_chapter += 1
                        if not template:
                            audit.error("scene.template.missing", block_location, "可视化场景缺少 template")
                        if not isinstance(block.get("data"), dict):
                            audit.error("scene.data.invalid", block_location, "可视化场景 data 必须是对象")

                    combined_text = "\n".join(text_fragments(block)).strip()
                    if block_type != "scene" and not combined_text:
                        audit.warning("block.text.empty", block_location, "内容块没有可显示文字")
                    for pattern_name, pattern in BAD_MATH_PATTERNS.items():
                        if pattern.search(combined_text):
                            audit.warning(
                                f"typography.{pattern_name}",
                                block_location,
                                "发现可能不规范的数学排版，请对照教材人工复核",
                            )

                if not page_has_explanation:
                    audit.warning("page.explanation.missing", page_location, "页面缺少教材正文或解释块")
                if not page_has_conclusion:
                    audit.info("page.conclusion.missing", page_location, "页面没有总结块")

        chapter_stats.append(
            {
                "number": chapter.get("number", ci),
                "title": chapter.get("title", ""),
                "sections": len(sections),
                "pages": pages_in_chapter,
                "scenes": scenes_in_chapter,
            }
        )

    for (previous_page, previous_location), (current_page, current_location) in zip(source_pages, source_pages[1:]):
        if current_page < previous_page:
            audit.error(
                "sourcePage.order",
                current_location,
                f"教材页码 {current_page} 小于上一课程页 {previous_page}（{previous_location}）",
            )
        elif current_page - previous_page > 2:
            audit.warning(
                "sourcePage.gap",
                current_location,
                f"教材来源页从 {previous_page} 跳到 {current_page}，请确认中间页面没有遗漏",
            )

    return {
        "chapters": chapter_stats,
        "pageCount": sum(item["pages"] for item in chapter_stats),
        "sceneCount": sum(scene_counter.values()),
        "sceneTemplates": dict(sorted(scene_counter.items())),
        "blockTypes": dict(sorted(block_counter.items())),
        "sourcePageMin": min((value for value, _ in source_pages), default=None),
        "sourcePageMax": max((value for value, _ in source_pages), default=None),
    }


def collect_ids(value: Any, id_locations: dict[str, list[str]], location: str = "root") -> None:
    if isinstance(value, dict):
        for key, child in value.items():
            child_location = f"{location}.{key}"
            if key.lower().endswith("id") and isinstance(child, str) and child:
                id_locations[child].append(child_location)
            collect_ids(child, id_locations, child_location)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            collect_ids(child, id_locations, f"{location}[{index}]")


def audit_knowledge_points(data: Any, audit: Audit) -> dict[str, Any]:
    entries = data if isinstance(data, list) else data.get("knowledgePoints", []) if isinstance(data, dict) else []
    if not entries:
        audit.error("knowledge.empty", "knowledge-points.json", "知识点列表为空")
        return {"count": 0}
    ids: set[str] = set()
    chapter_counts: Counter[str] = Counter()
    for index, item in enumerate(entries, start=1):
        location = f"knowledge[{index}]"
        item_id = item.get("id") if isinstance(item, dict) else None
        if not isinstance(item_id, str) or not item_id:
            audit.error("knowledge.id.missing", location, "知识点缺少 id")
        elif item_id in ids:
            audit.error("knowledge.id.duplicate", location, f"重复知识点 id：{item_id}")
        else:
            ids.add(item_id)
        title = str(item.get("title", "")).strip() if isinstance(item, dict) else ""
        if not title:
            audit.error("knowledge.title.missing", location, "知识点标题为空")
        chapter_counts[str(item.get("chapterId", "unassigned"))] += 1
    return {"count": len(entries), "chapterCounts": dict(sorted(chapter_counts.items()))}


def audit_assessments(data: Any, audit: Audit) -> dict[str, Any]:
    entries = data if isinstance(data, list) else data.get("assessments", []) if isinstance(data, dict) else []
    if not entries:
        audit.error("assessment.empty", "assessments.json", "题目列表为空")
        return {"count": 0}
    ids: set[str] = set()
    type_counts: Counter[str] = Counter()
    unanswered = 0
    for index, item in enumerate(entries, start=1):
        location = f"assessment[{index}]"
        if not isinstance(item, dict):
            audit.error("assessment.invalid", location, "题目必须是对象")
            continue
        item_id = item.get("id")
        if not isinstance(item_id, str) or not item_id:
            audit.error("assessment.id.missing", location, "题目缺少 id")
        elif item_id in ids:
            audit.error("assessment.id.duplicate", location, f"重复题目 id：{item_id}")
        else:
            ids.add(item_id)
        item_type = str(item.get("type", "unknown"))
        type_counts[item_type] += 1
        stem = str(item.get("stem", item.get("question", ""))).strip()
        if not stem:
            audit.error("assessment.stem.missing", location, "题干为空")
        answer = item.get("answer", item.get("correctAnswer"))
        if answer in (None, "", []):
            unanswered += 1
            audit.error("assessment.answer.missing", location, "题目缺少标准答案")
        explanation = str(item.get("analysis", item.get("explanation", ""))).strip()
        if not explanation:
            audit.warning("assessment.analysis.missing", location, "题目缺少解析")
        if item_type in {"choice", "single_choice", "multiple_choice"}:
            choices = item.get("choices", item.get("options", []))
            if not isinstance(choices, list) or len(choices) < 2:
                audit.error("assessment.choices.invalid", location, "选择题至少需要两个选项")
    return {
        "count": len(entries),
        "typeCounts": dict(sorted(type_counts.items())),
        "missingAnswerCount": unanswered,
    }


def audit_pdf_text(pdf_text_path: Path | None, course: dict[str, Any], audit: Audit) -> dict[str, Any]:
    if pdf_text_path is None:
        return {"enabled": False}
    text = pdf_text_path.read_text(encoding="utf-8", errors="replace")
    normalized_pdf = re.sub(r"\s+", "", text)
    checked = 0
    missing = 0
    for ci, chapter, si, section, pi, page in walk_course(course):
        location = f"chapter[{ci}]/section[{si}]/page[{pi}]"
        candidates = [str(page.get("title", ""))]
        candidates.extend(str(alias) for alias in page.get("aliases", []))
        candidates = [re.sub(r"\s+", "", value) for value in candidates if len(re.sub(r"\s+", "", value)) >= 4]
        if not candidates:
            continue
        checked += 1
        if not any(candidate in normalized_pdf for candidate in candidates):
            missing += 1
            audit.warning("pdf.title.not_found", location, "页面标题或别名未在教材提取文本中找到")
    return {"enabled": True, "checkedPageTitles": checked, "notFound": missing}


def build_report(pack_dir: Path, pdf_text_path: Path | None) -> dict[str, Any]:
    audit = Audit()
    course = load_json(pack_dir / "course.json")
    knowledge = load_json(pack_dir / "knowledge-points.json")
    assessments = load_json(pack_dir / "assessments.json")

    stats = {
        "course": audit_course_structure(course, audit),
        "knowledgePoints": audit_knowledge_points(knowledge, audit),
        "assessments": audit_assessments(assessments, audit),
        "pdfTextComparison": audit_pdf_text(pdf_text_path, course, audit),
    }
    severity_counts = Counter(finding.severity for finding in audit.findings)
    return {
        "pack": pack_dir.name,
        "passed": severity_counts["error"] == 0,
        "summary": {
            "errors": severity_counts["error"],
            "warnings": severity_counts["warning"],
            "info": severity_counts["info"],
        },
        "stats": stats,
        "findings": [asdict(finding) for finding in audit.findings],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="逐页审计 School 课程包")
    parser.add_argument("pack_dir", type=Path, help="包含三个 JSON 文件的课程包目录")
    parser.add_argument("--pdf-text", type=Path, help="可选：pdftotext -layout 生成的教材文本")
    parser.add_argument("--output", type=Path, help="JSON 报告输出路径；默认输出到 stdout")
    parser.add_argument("--warnings-as-errors", action="store_true", help="有警告时也返回失败")
    args = parser.parse_args()

    report = build_report(args.pack_dir, args.pdf_text)
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        sys.stdout.write(rendered)

    if not report["passed"]:
        return 1
    if args.warnings_as_errors and report["summary"]["warnings"]:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
