#!/usr/bin/env python3
"""Convert generated/legacy course JSON into the strict APK business-data contract."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import shutil
from typing import Any

IDENTIFIER = re.compile(r"^[A-Za-z0-9._-]+$")
TEXT_STYLES = {"textbook", "explanation", "history", "prompt", "caption"}
SCENE_TEMPLATES = {
    "opposite_quantities", "rational_classification", "integer_to_fraction", "number_line",
    "opposite_numbers", "absolute_value", "number_comparison", "addition_process",
    "subtraction_transform", "multiplication_sign", "division_transform", "power_process",
    "algebra_process", "equation_balance", "root_number_line", "cartesian_plane",
    "function_graph", "geometry", "transformation", "right_triangle", "data_chart",
    "probability", "projection", "diagram",
}
SCENE_ALIASES = {"number_line_lesson": "number_line", "declarative_diagram": "diagram"}


def require_text(value: Any, location: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{location} must be a non-empty string")
    return value.strip()


def identifier(value: Any, location: str) -> str:
    result = require_text(value, location)
    if not IDENTIFIER.fullmatch(result):
        raise ValueError(f"{location} is not a valid identifier: {result}")
    return result


def unique_identifier(value: Any, location: str, seen_ids: set[str], namespace: str) -> str:
    preferred = identifier(value, location)
    if preferred not in seen_ids:
        seen_ids.add(preferred)
        return preferred
    candidate = f"{namespace}-{preferred}"
    suffix = 2
    while candidate in seen_ids:
        candidate = f"{namespace}-{preferred}-{suffix}"
        suffix += 1
    if not IDENTIFIER.fullmatch(candidate):
        raise ValueError(f"cannot create unique id for {location}: {candidate}")
    seen_ids.add(candidate)
    return candidate


def string_list(value: Any, location: str) -> list[str]:
    if value is None:
        return []
    if not isinstance(value, list):
        raise ValueError(f"{location} must be an array")
    result = [require_text(item, f"{location}[]") for item in value]
    if len(set(result)) != len(result):
        raise ValueError(f"{location} contains duplicates")
    return result


def normalize_source_references(value: Any, page_count: int, location: str) -> list[dict[str, Any]]:
    if value is None:
        return []
    if not isinstance(value, list):
        raise ValueError(f"{location} must be an array")
    result: list[dict[str, Any]] = []
    seen: set[tuple[str, int]] = set()
    for index, raw in enumerate(value):
        item_location = f"{location}[{index}]"
        if not isinstance(raw, dict) or set(raw) != {"label", "sourcePage"}:
            raise ValueError(f"{item_location} must contain only label and sourcePage")
        label = require_text(raw.get("label"), f"{item_location}.label")
        source_page = raw.get("sourcePage")
        if not isinstance(source_page, int) or source_page <= 0 or source_page > page_count:
            raise ValueError(f"{item_location}.sourcePage is invalid")
        key = (label, source_page)
        if key in seen:
            raise ValueError(f"{location} contains duplicate reference: {label}")
        seen.add(key)
        result.append({"label": label, "sourcePage": source_page})
    return result


def number_from_string(value: str) -> int | float | str:
    text = value.strip()
    if re.fullmatch(r"[-+]?\d+", text):
        return int(text)
    if re.fullmatch(r"[-+]?(?:\d+\.\d*|\d*\.\d+)", text):
        return float(text)
    return value


def normalize_scene_data(template: str, raw: Any) -> dict[str, Any]:
    if raw is None:
        return {}
    if not isinstance(raw, dict):
        raise ValueError("scene data must be an object")
    data: dict[str, Any] = {}
    for key, value in raw.items():
        if template == "number_line" and key == "signed" and isinstance(value, str):
            lowered = value.strip().lower()
            if lowered not in {"true", "false"}:
                raise ValueError("number_line.signed must be a boolean")
            data[key] = lowered == "true"
        elif template == "number_line" and key == "initial" and isinstance(value, str):
            converted = number_from_string(value)
            if isinstance(converted, str):
                raise ValueError("number_line.initial must be numeric")
            data[key] = converted
        elif template == "opposite_quantities" and key == "scenes" and isinstance(value, str):
            data[key] = [item.strip() for item in value.split(",") if item.strip()]
        else:
            data[key] = value
    return data


def normalize_block(raw: Any, location: str) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise ValueError(f"{location} must be an object")
    kind = require_text(raw.get("type"), f"{location}.type")
    if kind == "heading":
        return {"type": "heading", "text": require_text(raw.get("text"), f"{location}.text")}

    style_by_legacy_type = {
        "textbook_text": "textbook", "explanation": "explanation", "historical_note": "history",
        "history": "history", "prompt": "prompt", "caption": "caption",
    }
    if kind in style_by_legacy_type:
        return {"type": "text", "style": style_by_legacy_type[kind], "text": require_text(raw.get("text"), f"{location}.text")}
    if kind == "text":
        style = require_text(raw.get("style"), f"{location}.style").lower()
        if style not in TEXT_STYLES:
            raise ValueError(f"{location}.style is unsupported: {style}")
        return {"type": "text", "style": style, "text": require_text(raw.get("text"), f"{location}.text")}
    if kind == "formula":
        result: dict[str, Any] = {"type": "formula", "expression": require_text(raw.get("expression"), f"{location}.expression")}
        conditions = string_list(raw.get("conditions"), f"{location}.conditions")
        if conditions:
            result["conditions"] = conditions
        return result
    if kind in {"summary", "list"}:
        items = string_list(raw.get("items"), f"{location}.items")
        if not items:
            raise ValueError(f"{location}.items cannot be empty")
        return {"type": "list", "items": items}
    if kind in {"worked_example", "example"}:
        result = {"type": "example", "statement": require_text(raw.get("statement"), f"{location}.statement")}
        label = raw.get("label")
        if isinstance(label, str) and label.strip():
            result["label"] = label.strip()
        steps = string_list(raw.get("steps"), f"{location}.steps")
        if steps:
            result["steps"] = steps
        value = raw.get("result")
        if isinstance(value, str) and value.strip():
            result["result"] = value.strip()
        return result
    if kind == "exercise":
        result = {"type": "exercise", "stem": require_text(raw.get("stem"), f"{location}.stem")}
        number = raw.get("number")
        if isinstance(number, str) and number.strip():
            result["number"] = number.strip()
        choices = string_list(raw.get("choices"), f"{location}.choices")
        hints = string_list(raw.get("hints"), f"{location}.hints")
        if choices:
            result["choices"] = choices
        if hints:
            result["hints"] = hints
        return result
    if kind == "conclusion":
        return {"type": "conclusion", "text": require_text(raw.get("text"), f"{location}.text")}
    if kind in {"visualization", "scene"}:
        raw_template = raw.get("renderer") if kind == "visualization" else raw.get("template")
        requested = require_text(raw_template, f"{location}.template")
        template = SCENE_ALIASES.get(requested, requested)
        if template not in SCENE_TEMPLATES:
            raise ValueError(f"{location} uses unsupported scene template: {template}")
        raw_data = raw.get("params") if kind == "visualization" else raw.get("data")
        return {"type": "scene", "template": template, "data": normalize_scene_data(template, raw_data)}
    if kind == "source_excerpt":
        fallback = raw.get("fallbackText") or raw.get("altText")
        return {"type": "text", "style": "caption", "text": require_text(fallback, f"{location}.fallbackText")}
    raise ValueError(f"{location} uses unsupported block type: {kind}")


def normalize_page(raw: Any, location: str, page_count: int, seen_ids: set[str], namespace: str) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise ValueError(f"{location} must be an object")
    page_id = unique_identifier(raw.get("id"), f"{location}.id", seen_ids, namespace)
    source_page = raw.get("sourcePage")
    if not isinstance(source_page, int) or source_page <= 0 or source_page > page_count:
        raise ValueError(f"{location}.sourcePage is invalid")
    source_page_end = raw.get("sourcePageEnd", source_page)
    if not isinstance(source_page_end, int) or not source_page <= source_page_end <= page_count:
        raise ValueError(f"{location}.sourcePageEnd is invalid")
    blocks_raw = raw.get("blocks")
    if not isinstance(blocks_raw, list) or not blocks_raw:
        raise ValueError(f"{location}.blocks cannot be empty")
    result: dict[str, Any] = {
        "id": page_id,
        "title": require_text(raw.get("title"), f"{location}.title"),
        "aliases": string_list(raw.get("aliases"), f"{location}.aliases"),
        "sourcePage": source_page,
        "blocks": [normalize_block(item, f"{location}.blocks[{index}]") for index, item in enumerate(blocks_raw)],
    }
    source_references = normalize_source_references(raw.get("sourceReferences"), page_count, f"{location}.sourceReferences")
    if source_references:
        result["sourceReferences"] = source_references
    if source_page_end != source_page:
        result["sourcePageEnd"] = source_page_end
    return result


def normalize_section(raw: Any, location: str, page_count: int, seen_ids: set[str], namespace: str) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise ValueError(f"{location} must be an object")
    section_id = unique_identifier(raw.get("id"), f"{location}.id", seen_ids, namespace)
    pages = raw.get("pages")
    if not isinstance(pages, list) or not pages:
        raise ValueError(f"{location}.pages cannot be empty")
    return {
        "id": section_id,
        "number": str(raw.get("number") or "").strip(),
        "title": require_text(raw.get("title"), f"{location}.title"),
        "aliases": string_list(raw.get("aliases"), f"{location}.aliases"),
        "pages": [normalize_page(item, f"{location}.pages[{index}]", page_count, seen_ids, section_id) for index, item in enumerate(pages)],
    }


def normalize_course(payload: Any) -> dict[str, Any]:
    if not isinstance(payload, dict):
        raise ValueError("course root must be an object")
    textbook_raw = payload.get("textbook")
    chapters_raw = payload.get("chapters")
    if not isinstance(textbook_raw, dict) or not isinstance(chapters_raw, list) or not chapters_raw:
        raise ValueError("course must contain textbook and non-empty chapters")
    pdf_raw = textbook_raw.get("pdf")
    if not isinstance(pdf_raw, dict):
        raise ValueError("textbook.pdf must be an object")
    page_count = pdf_raw.get("pageCount")
    if not isinstance(page_count, int) or page_count <= 0:
        raise ValueError("textbook.pdf.pageCount must be positive")
    offset = pdf_raw.get("pageIndexOffset", 0)
    if not isinstance(offset, int) or not -10_000 <= offset <= 10_000:
        raise ValueError("textbook.pdf.pageIndexOffset is invalid")
    textbook = {
        "id": identifier(textbook_raw.get("id"), "textbook.id"),
        "title": require_text(textbook_raw.get("title"), "textbook.title"),
        "publisher": require_text(textbook_raw.get("publisher"), "textbook.publisher"),
        "edition": require_text(textbook_raw.get("edition"), "textbook.edition"),
        "grade": require_text(textbook_raw.get("grade"), "textbook.grade"),
        "semester": require_text(textbook_raw.get("semester"), "textbook.semester"),
        "subject": require_text(textbook_raw.get("subject"), "textbook.subject"),
        "pdf": {
            "path": require_text(pdf_raw.get("path"), "textbook.pdf.path").replace("\\", "/"),
            "pageCount": page_count,
            "pageIndexOffset": offset,
        },
    }
    if not textbook["pdf"]["path"].lower().endswith(".pdf"):
        raise ValueError("textbook.pdf.path must point to a PDF")
    seen_ids: set[str] = set()
    chapters: list[dict[str, Any]] = []
    for chapter_index, raw_chapter in enumerate(chapters_raw):
        location = f"chapters[{chapter_index}]"
        if not isinstance(raw_chapter, dict):
            raise ValueError(f"{location} must be an object")
        chapter_id = unique_identifier(raw_chapter.get("id"), f"{location}.id", seen_ids, "chapter")
        sections_raw = raw_chapter.get("sections")
        if not isinstance(sections_raw, list) or not sections_raw:
            raise ValueError(f"{location}.sections cannot be empty")
        chapter: dict[str, Any] = {
            "id": chapter_id,
            "number": str(raw_chapter.get("number") or "").strip(),
            "title": require_text(raw_chapter.get("title"), f"{location}.title"),
            "aliases": string_list(raw_chapter.get("aliases"), f"{location}.aliases"),
            "sections": [normalize_section(item, f"{location}.sections[{index}]", page_count, seen_ids, chapter_id) for index, item in enumerate(sections_raw)],
        }
        if raw_chapter.get("review") is not None:
            chapter["review"] = normalize_section(raw_chapter["review"], f"{location}.review", page_count, seen_ids, chapter_id)
        chapters.append(chapter)
    return {"textbook": textbook, "chapters": chapters}


def convert_file(source: Path, target: Path) -> None:
    payload = json.loads(source.read_text(encoding="utf-8"))
    normalized = normalize_course(payload)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(normalized, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--source", type=Path)
    group.add_argument("--source-root", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--output-root", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.source is not None:
        if args.output is None:
            raise SystemExit("--output is required with --source")
        convert_file(args.source.resolve(), args.output.resolve())
        print(args.output.resolve())
        return 0
    if args.output_root is None:
        raise SystemExit("--output-root is required with --source-root")
    source_root = args.source_root.resolve()
    output_root = args.output_root.resolve()
    if output_root.exists():
        shutil.rmtree(output_root)
    count = 0
    for source in sorted(source_root.glob("*/course.json")):
        convert_file(source, output_root / source.parent.name / "course.json")
        count += 1
    if count == 0:
        raise SystemExit(f"no course.json files found under {source_root}")
    print(f"normalized {count} course files into {output_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
