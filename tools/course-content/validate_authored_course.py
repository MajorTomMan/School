#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

ID = re.compile(r"^[A-Za-z0-9._:-]+$")
STEP_TYPES = {"explanation", "question", "keyIdea", "formula", "example", "scene", "checkpoint", "sourceLink", "summary"}
SCENES = {
    "opposite_quantities", "rational_classification", "integer_to_fraction", "number_line", "opposite_numbers",
    "absolute_value", "number_comparison", "addition_process", "subtraction_transform", "multiplication_sign",
    "division_transform", "power_process", "algebra_process", "equation_balance", "root_number_line",
    "cartesian_plane", "function_graph", "geometry", "transformation", "right_triangle", "data_chart",
    "probability", "projection", "diagram",
}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def exact(obj: dict[str, Any], keys: set[str], where: str) -> None:
    require(set(obj) == keys, f"{where}: fields expected={sorted(keys)} actual={sorted(obj)}")


def text(obj: dict[str, Any], key: str, where: str) -> str:
    value = obj.get(key)
    require(isinstance(value, str) and value.strip(), f"{where}.{key}: non-empty string required")
    return value.strip()


def identifier(obj: dict[str, Any], key: str, where: str) -> str:
    value = text(obj, key, where)
    require(bool(ID.fullmatch(value)), f"{where}.{key}: invalid id {value!r}")
    return value


def strings(value: Any, where: str, allow_empty: bool = True) -> list[str]:
    require(isinstance(value, list) and all(isinstance(item, str) and item.strip() for item in value), f"{where}: string array required")
    if not allow_empty:
        require(bool(value), f"{where}: must not be empty")
    return [item.strip() for item in value]


def validate(path: Path) -> dict[str, int]:
    root = json.loads(path.read_text(encoding="utf-8"))
    require(isinstance(root, dict), f"{path}: root must be object")
    exact(root, {"textbook", "knowledgePoints", "chapters"}, "root")

    textbook = root["textbook"]
    require(isinstance(textbook, dict), "textbook must be object")
    exact(textbook, {"id", "title", "publisher", "edition", "grade", "semester", "subject", "pdf"}, "textbook")
    textbook_id = identifier(textbook, "id", "textbook")
    for key in ("title", "publisher", "edition", "grade", "semester", "subject"):
        text(textbook, key, "textbook")
    pdf = textbook["pdf"]
    require(isinstance(pdf, dict), "textbook.pdf must be object")
    exact(pdf, {"path", "pageCount", "pageIndexOffset"}, "textbook.pdf")
    pdf_path = text(pdf, "path", "textbook.pdf")
    require(pdf_path.lower().endswith(".pdf") and not pdf_path.startswith("/") and ".." not in Path(pdf_path).parts, "invalid PDF path")
    require(isinstance(pdf["pageCount"], int) and pdf["pageCount"] > 0, "pageCount must be positive int")
    require(isinstance(pdf["pageIndexOffset"], int), "pageIndexOffset must be int")

    points = root["knowledgePoints"]
    require(isinstance(points, list) and points, "knowledgePoints must be non-empty array")
    point_ids: set[str] = set()
    prerequisites: dict[str, list[str]] = {}
    for index, point in enumerate(points):
        where = f"knowledgePoints[{index}]"
        require(isinstance(point, dict), f"{where}: object required")
        exact(point, {"id", "name", "description", "prerequisiteIds"}, where)
        point_id = identifier(point, "id", where)
        require(point_id not in point_ids, f"duplicate knowledge point {point_id}")
        point_ids.add(point_id)
        text(point, "name", where)
        text(point, "description", where)
        prerequisites[point_id] = strings(point["prerequisiteIds"], f"{where}.prerequisiteIds")
    for point_id, deps in prerequisites.items():
        require(set(deps) <= point_ids, f"{point_id}: unknown prerequisite {sorted(set(deps) - point_ids)}")
    visiting: set[str] = set()
    visited: set[str] = set()
    def visit(point_id: str) -> None:
        if point_id in visited:
            return
        require(point_id not in visiting, f"knowledge graph cycle at {point_id}")
        visiting.add(point_id)
        for dependency in prerequisites[point_id]:
            visit(dependency)
        visiting.remove(point_id)
        visited.add(point_id)
    for point_id in point_ids:
        visit(point_id)

    chapters = root["chapters"]
    require(isinstance(chapters, list) and chapters, "chapters must be non-empty array")
    lesson_ids: set[str] = set()
    pending_lesson_deps: list[tuple[str, str]] = []
    practice_ids: set[str] = set()
    lesson_count = 0
    step_count = 0
    practice_count = 0
    for chapter_index, chapter in enumerate(chapters):
        cw = f"chapters[{chapter_index}]"
        require(isinstance(chapter, dict), f"{cw}: object required")
        exact(chapter, {"id", "title", "sections"}, cw)
        identifier(chapter, "id", cw); text(chapter, "title", cw)
        sections = chapter["sections"]
        require(isinstance(sections, list) and sections, f"{cw}.sections must be non-empty")
        for section_index, section in enumerate(sections):
            sw = f"{cw}.sections[{section_index}]"
            require(isinstance(section, dict), f"{sw}: object required")
            exact(section, {"id", "title", "lessons"}, sw)
            identifier(section, "id", sw); text(section, "title", sw)
            lessons = section["lessons"]
            require(isinstance(lessons, list) and lessons, f"{sw}.lessons must be non-empty")
            for lesson_index, lesson in enumerate(lessons):
                lw = f"{sw}.lessons[{lesson_index}]"
                require(isinstance(lesson, dict), f"{lw}: object required")
                exact(lesson, {"id", "title", "aliases", "goals", "knowledgePointIds", "prerequisiteLessonIds", "references", "steps", "practice", "summary"}, lw)
                lesson_id = identifier(lesson, "id", lw)
                require(lesson_id not in lesson_ids, f"duplicate lesson {lesson_id}")
                lesson_ids.add(lesson_id); lesson_count += 1
                text(lesson, "title", lw)
                strings(lesson["aliases"], f"{lw}.aliases")
                strings(lesson["goals"], f"{lw}.goals", allow_empty=False)
                bound_points = strings(lesson["knowledgePointIds"], f"{lw}.knowledgePointIds", allow_empty=False)
                require(set(bound_points) <= point_ids, f"{lw}: unknown knowledge point")
                for dependency in strings(lesson["prerequisiteLessonIds"], f"{lw}.prerequisiteLessonIds"):
                    pending_lesson_deps.append((lesson_id, dependency))
                references = lesson["references"]
                require(isinstance(references, list), f"{lw}.references must be array")
                for ref_index, reference in enumerate(references):
                    rw = f"{lw}.references[{ref_index}]"
                    require(isinstance(reference, dict), f"{rw}: object required")
                    exact(reference, {"label", "pageStart", "pageEnd"}, rw)
                    text(reference, "label", rw)
                    start, end = reference["pageStart"], reference["pageEnd"]
                    require(isinstance(start, int) and isinstance(end, int) and 1 <= start <= end <= pdf["pageCount"], f"{rw}: invalid page range")
                steps = lesson["steps"]
                require(isinstance(steps, list) and steps, f"{lw}.steps must be non-empty")
                for step_index, step in enumerate(steps):
                    stw = f"{lw}.steps[{step_index}]"
                    require(isinstance(step, dict), f"{stw}: object required")
                    step_type = text(step, "type", stw)
                    require(step_type in STEP_TYPES, f"{stw}: unsupported type {step_type}")
                    if step_type == "sourceLink":
                        ref_index = step.get("referenceIndex")
                        require(isinstance(ref_index, int) and 0 <= ref_index < len(references), f"{stw}: referenceIndex out of range")
                    if step_type == "scene":
                        require(text(step, "template", stw) in SCENES, f"{stw}: unsupported scene")
                        require(isinstance(step.get("data"), dict), f"{stw}.data must be object")
                    step_count += 1
                practice = lesson["practice"]
                require(isinstance(practice, list), f"{lw}.practice must be array")
                for practice_index, item in enumerate(practice):
                    pw = f"{lw}.practice[{practice_index}]"
                    require(isinstance(item, dict), f"{pw}: object required")
                    exact(item, {"id", "prompt", "answer", "analysis", "knowledgePointIds", "difficulty"}, pw)
                    practice_id = identifier(item, "id", pw)
                    require(practice_id not in practice_ids, f"duplicate practice {practice_id}")
                    practice_ids.add(practice_id)
                    text(item, "prompt", pw); text(item, "answer", pw)
                    strings(item["analysis"], f"{pw}.analysis", allow_empty=False)
                    item_points = strings(item["knowledgePointIds"], f"{pw}.knowledgePointIds", allow_empty=False)
                    require(set(item_points) <= point_ids, f"{pw}: unknown knowledge point")
                    require(isinstance(item["difficulty"], int) and 1 <= item["difficulty"] <= 5, f"{pw}.difficulty must be 1..5")
                    practice_count += 1
                strings(lesson["summary"], f"{lw}.summary", allow_empty=False)
    for lesson_id, dependency in pending_lesson_deps:
        require(dependency in lesson_ids, f"{lesson_id}: unknown prerequisite lesson {dependency}")
    print(f"validated {path}: textbook={textbook_id}, knowledge={len(point_ids)}, lessons={lesson_count}, steps={step_count}, practice={practice_count}")
    return {"knowledgePoints": len(point_ids), "lessons": lesson_count, "steps": step_count, "practice": practice_count}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+", type=Path)
    args = parser.parse_args()
    for path in args.paths:
        validate(path)


if __name__ == "__main__":
    main()
