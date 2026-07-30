#!/usr/bin/env python3
"""Generate auditable, content-based replacements for temporary course page titles.

This helper does not mark pages as verified. It replaces titles such as
``教材第25页（2）`` only when the page itself contains enough semantic evidence
(formula, example, exercise, conclusion, heading or explanatory text) to derive a
stable title. Ambiguous pages remain unchanged and are written to the report for
manual comparison with the PDF.
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Iterable

GENERIC_TITLE = re.compile(r"^教材第\d+页(?:（\d+）)?$")
SPACE = re.compile(r"\s+")
LEADING_LABEL = re.compile(
    r"^(?:例|例题|练习|思考|探究|观察|归纳|小结|问题|解|分析|提示)\s*[：:、.]?\s*",
)

BLOCK_PREFIX = {
    "example": "例题",
    "exercise": "练习",
    "formula": "公式",
    "conclusion": "结论",
    "list": "要点",
}

TEXT_STYLE_PREFIX = {
    "prompt": "问题",
    "history": "数学文化",
    "explanation": "方法",
    "caption": "图示",
    "textbook": "知识",
}


@dataclass
class TitleReport:
    pages: int = 0
    generic_before: int = 0
    replaced: int = 0
    ambiguous: int = 0
    generic_after: int = 0
    replacements: list[dict[str, Any]] | None = None
    pending: list[dict[str, Any]] | None = None

    def __post_init__(self) -> None:
        self.replacements = [] if self.replacements is None else self.replacements
        self.pending = [] if self.pending is None else self.pending


def normalize(value: Any) -> str:
    if value is None:
        return ""
    text = SPACE.sub(" ", str(value)).strip()
    return LEADING_LABEL.sub("", text).strip(" ，。；：、")


def shorten(text: str, limit: int = 24) -> str:
    text = normalize(text)
    if len(text) <= limit:
        return text
    cut = text[:limit]
    for punctuation in ("，", "。", "；", "：", "、"):
        position = cut.rfind(punctuation)
        if position >= 8:
            cut = cut[:position]
            break
    return cut.rstrip("，。；：、") + "…"


def first_nonblank(values: Iterable[Any]) -> str:
    for value in values:
        text = normalize(value)
        if text:
            return text
    return ""


def infer_from_block(block: dict[str, Any]) -> tuple[str, str] | None:
    block_type = str(block.get("type", "")).strip()
    if block_type == "heading":
        body = first_nonblank((block.get("text"), block.get("title")))
        return (shorten(body), "heading") if body else None

    if block_type == "formula":
        expression = first_nonblank((block.get("expression"), block.get("formula"), block.get("text")))
        return (f"公式：{shorten(expression, 20)}", "formula") if expression else None

    if block_type == "example":
        body = first_nonblank((block.get("statement"), block.get("text"), block.get("title"), block.get("label")))
        return (f"例题：{shorten(body, 20)}", "example") if body else None

    if block_type == "exercise":
        body = first_nonblank((block.get("stem"), block.get("question"), block.get("text")))
        return (f"练习：{shorten(body, 20)}", "exercise") if body else None

    if block_type == "conclusion":
        body = first_nonblank((block.get("text"), block.get("conclusion")))
        return (f"结论：{shorten(body, 20)}", "conclusion") if body else None

    if block_type == "list":
        items = block.get("items", [])
        body = first_nonblank(items if isinstance(items, list) else ())
        return (f"要点：{shorten(body, 20)}", "list") if body else None

    if block_type == "text":
        body = first_nonblank((block.get("text"),))
        if not body:
            return None
        style = str(block.get("style", "textbook"))
        prefix = TEXT_STYLE_PREFIX.get(style, "知识")
        return f"{prefix}：{shorten(body, 20)}", f"text:{style}"

    return None


def infer_title(page: dict[str, Any]) -> tuple[str, str] | None:
    blocks = page.get("blocks", [])
    if not isinstance(blocks, list):
        return None

    candidates = [candidate for block in blocks if isinstance(block, dict) if (candidate := infer_from_block(block))]
    if not candidates:
        return None

    priority = {
        "heading": 0,
        "example": 1,
        "exercise": 2,
        "formula": 3,
        "text:prompt": 4,
        "text:explanation": 5,
        "text:textbook": 6,
        "conclusion": 7,
        "list": 8,
        "text:history": 9,
        "text:caption": 10,
    }
    candidates.sort(key=lambda item: priority.get(item[1], 99))
    title, evidence = candidates[0]
    if len(normalize(title.split("：", 1)[-1])) < 3:
        return None
    return title, evidence


def refine_titles(course: dict[str, Any]) -> TitleReport:
    report = TitleReport()
    for chapter in course.get("chapters", []):
        for section in chapter.get("sections", []):
            for page in section.get("pages", []):
                report.pages += 1
                old_title = str(page.get("title", "")).strip()
                if not GENERIC_TITLE.fullmatch(old_title):
                    continue
                report.generic_before += 1
                inferred = infer_title(page)
                if inferred is None:
                    report.ambiguous += 1
                    report.pending.append({
                        "pageId": page.get("id"),
                        "sourcePage": page.get("sourcePage"),
                        "title": old_title,
                        "section": section.get("title"),
                    })
                    continue
                new_title, evidence = inferred
                page["title"] = new_title
                page["titleRefinement"] = {
                    "status": "candidate",
                    "source": "page-content",
                    "evidence": evidence,
                    "previousTitle": old_title,
                }
                report.replaced += 1
                report.replacements.append({
                    "pageId": page.get("id"),
                    "sourcePage": page.get("sourcePage"),
                    "oldTitle": old_title,
                    "newTitle": new_title,
                    "evidence": evidence,
                })

    report.generic_after = report.generic_before - report.replaced
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description="为七上课程临时页面标题生成可审计候选")
    parser.add_argument("course", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    course = json.loads(args.course.read_text(encoding="utf-8"))
    report = refine_titles(course)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(asdict(report), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(asdict(report), ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
