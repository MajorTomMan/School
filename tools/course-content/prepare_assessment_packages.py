#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path
import shutil
from typing import Any

import fitz

ASSESSMENTS = "assessments.json"
KNOWLEDGE = "knowledge-points.json"
ASSET_CROPS = "asset-crops.json"
ASSET_ROOT = "assets"
CONTENT_TYPES = {"heading", "text", "formula", "list", "image", "table", "scene"}
TEXT_STYLES = {"body", "prompt", "caption", "explanation"}
INPUT_TYPES = {"integer", "decimal", "rational", "single_choice", "coordinate"}
ANSWER_TYPES = {"exact_integer", "decimal", "rational_equivalent", "single_choice", "coordinate"}


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def exact(value: dict[str, Any], required: set[str], optional: set[str], where: str) -> None:
    keys = set(value)
    missing = required - keys
    unknown = keys - required - optional
    if missing:
        raise ValueError(f"{where} missing keys: {sorted(missing)}")
    if unknown:
        raise ValueError(f"{where} has unknown keys: {sorted(unknown)}")


def text(value: Any, where: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{where} must be non-empty text")
    return value


def identifier(value: Any, where: str) -> str:
    result = text(value, where)
    if not result[0].islower() or not all(character.islower() or character.isdigit() or character in "_-" for character in result):
        raise ValueError(f"{where} has invalid identifier format: {result}")
    if len(result) > 64:
        raise ValueError(f"{where} is longer than 64 characters")
    return result


def positive_int(value: Any, where: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise ValueError(f"{where} must be a positive integer")
    return value


def finite_number(value: Any, where: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{where} must be numeric")
    result = float(value)
    if result != result or result in (float("inf"), float("-inf")):
        raise ValueError(f"{where} must be finite")
    return result


def string_array(value: Any, where: str, allow_empty: bool = True) -> list[str]:
    if not isinstance(value, list) or (not allow_empty and not value):
        raise ValueError(f"{where} must be {'non-empty ' if not allow_empty else ''}array")
    result = [text(item, f"{where}[]") for item in value]
    if len(result) != len(set(result)):
        raise ValueError(f"{where} contains duplicates")
    return result


def validate_rational(value: Any, where: str) -> None:
    if not isinstance(value, dict):
        raise ValueError(f"{where} must be an object")
    exact(value, {"numerator", "denominator"}, set(), where)
    for key in ("numerator", "denominator"):
        if isinstance(value[key], bool) or not isinstance(value[key], int):
            raise ValueError(f"{where}.{key} must be an integer")
    if value["denominator"] == 0:
        raise ValueError(f"{where}.denominator cannot be zero")


def validate_scene(value: dict[str, Any], where: str) -> None:
    exact(value, {"type", "template", "data"}, set(), where)
    template = text(value["template"], f"{where}.template")
    if not isinstance(value["data"], dict):
        raise ValueError(f"{where}.data must be an object")
    if template == "number_line":
        allowed = {"title", "mode", "signed", "initial"}
        unknown = value["data"].keys() - allowed
        if unknown:
            raise ValueError(f"{where}.data has unknown keys: {sorted(unknown)}")
        mode = value["data"].get("mode")
        if mode is not None and mode not in {
            "road",
            "construction",
            "value",
            "example",
            "read_points",
            "opposite",
            "opposite_symbol",
        }:
            raise ValueError(f"{where}.data.mode is unsupported")


def validate_content(value: Any, where: str, referenced_assets: set[str]) -> None:
    if not isinstance(value, dict):
        raise ValueError(f"{where} must be an object")
    kind = text(value.get("type"), f"{where}.type")
    if kind not in CONTENT_TYPES:
        raise ValueError(f"{where}.type is unsupported: {kind}")
    if kind == "heading":
        exact(value, {"type", "text"}, set(), where)
        text(value["text"], f"{where}.text")
    elif kind == "text":
        exact(value, {"type", "text", "style"}, set(), where)
        text(value["text"], f"{where}.text")
        if value["style"] not in TEXT_STYLES:
            raise ValueError(f"{where}.style is unsupported")
    elif kind == "formula":
        exact(value, {"type", "expression", "conditions"}, set(), where)
        text(value["expression"], f"{where}.expression")
        string_array(value["conditions"], f"{where}.conditions")
    elif kind == "list":
        exact(value, {"type", "items"}, set(), where)
        string_array(value["items"], f"{where}.items", allow_empty=False)
    elif kind == "image":
        exact(value, {"type", "assetId", "altText"}, {"caption"}, where)
        referenced_assets.add(identifier(value["assetId"], f"{where}.assetId"))
        text(value["altText"], f"{where}.altText")
        if "caption" in value:
            text(value["caption"], f"{where}.caption")
    elif kind == "table":
        exact(value, {"type", "columns", "rows"}, {"caption", "sourceAssetId"}, where)
        columns = string_array(value["columns"], f"{where}.columns", allow_empty=False)
        if not isinstance(value["rows"], list) or not value["rows"]:
            raise ValueError(f"{where}.rows must be a non-empty array")
        for index, row in enumerate(value["rows"]):
            cells = string_array(row, f"{where}.rows[{index}]", allow_empty=False)
            if len(cells) != len(columns):
                raise ValueError(f"{where}.rows[{index}] column count mismatch")
        if "caption" in value:
            text(value["caption"], f"{where}.caption")
        if "sourceAssetId" in value:
            referenced_assets.add(identifier(value["sourceAssetId"], f"{where}.sourceAssetId"))
    elif kind == "scene":
        validate_scene(value, where)


def validate_content_array(value: Any, where: str, referenced_assets: set[str], allow_empty: bool) -> None:
    if not isinstance(value, list) or (not allow_empty and not value):
        raise ValueError(f"{where} must be {'non-empty ' if not allow_empty else ''}array")
    for index, item in enumerate(value):
        validate_content(item, f"{where}[{index}]", referenced_assets)


def validate_question(
    question: Any,
    where: str,
    knowledge_ids: set[str],
    asset_ids: set[str],
    referenced_assets: set[str],
) -> str:
    if not isinstance(question, dict):
        raise ValueError(f"{where} must be an object")
    exact(
        question,
        {
            "id", "revision", "number", "stem", "input", "answer", "knowledgeBindings",
            "difficulty", "hints", "choices", "explanation",
        },
        set(),
        where,
    )
    question_id = identifier(question["id"], f"{where}.id")
    positive_int(question["revision"], f"{where}.revision")
    text(question["number"], f"{where}.number")
    validate_content_array(question["stem"], f"{where}.stem", referenced_assets, False)

    input_value = question["input"]
    if not isinstance(input_value, dict):
        raise ValueError(f"{where}.input must be an object")
    input_type = text(input_value.get("type"), f"{where}.input.type")
    if input_type not in INPUT_TYPES:
        raise ValueError(f"{where}.input.type is unsupported")
    if input_type == "integer":
        exact(input_value, {"type"}, set(), f"{where}.input")
    elif input_type == "decimal":
        exact(input_value, {"type", "allowFraction"}, set(), f"{where}.input")
        if not isinstance(input_value["allowFraction"], bool):
            raise ValueError(f"{where}.input.allowFraction must be boolean")
    elif input_type == "rational":
        exact(input_value, {"type", "allowDecimal"}, set(), f"{where}.input")
        if not isinstance(input_value["allowDecimal"], bool):
            raise ValueError(f"{where}.input.allowDecimal must be boolean")
    elif input_type == "single_choice":
        exact(input_value, {"type", "optionIds"}, set(), f"{where}.input")
        option_ids = string_array(input_value["optionIds"], f"{where}.input.optionIds", allow_empty=False)
        for option_id in option_ids:
            identifier(option_id, f"{where}.input.optionIds[]")
    elif input_type == "coordinate":
        exact(input_value, {"type"}, set(), f"{where}.input")

    answer = question["answer"]
    if not isinstance(answer, dict):
        raise ValueError(f"{where}.answer must be an object")
    answer_type = text(answer.get("type"), f"{where}.answer.type")
    if answer_type not in ANSWER_TYPES:
        raise ValueError(f"{where}.answer.type is unsupported")
    if answer_type == "exact_integer":
        exact(answer, {"type", "expected"}, set(), f"{where}.answer")
        if isinstance(answer["expected"], bool) or not isinstance(answer["expected"], int):
            raise ValueError(f"{where}.answer.expected must be integer")
    elif answer_type == "decimal":
        exact(answer, {"type", "expected", "tolerance"}, set(), f"{where}.answer")
        finite_number(answer["expected"], f"{where}.answer.expected")
        if finite_number(answer["tolerance"], f"{where}.answer.tolerance") < 0:
            raise ValueError(f"{where}.answer.tolerance cannot be negative")
    elif answer_type == "rational_equivalent":
        exact(answer, {"type", "expected"}, set(), f"{where}.answer")
        validate_rational(answer["expected"], f"{where}.answer.expected")
    elif answer_type == "single_choice":
        exact(answer, {"type", "expectedOptionId"}, set(), f"{where}.answer")
        identifier(answer["expectedOptionId"], f"{where}.answer.expectedOptionId")
    elif answer_type == "coordinate":
        exact(answer, {"type", "expectedX", "expectedY"}, set(), f"{where}.answer")
        validate_rational(answer["expectedX"], f"{where}.answer.expectedX")
        validate_rational(answer["expectedY"], f"{where}.answer.expectedY")

    if not isinstance(question["knowledgeBindings"], list) or not question["knowledgeBindings"]:
        raise ValueError(f"{where}.knowledgeBindings must be a non-empty array")
    total_weight = 0.0
    bound_ids: set[str] = set()
    for index, binding in enumerate(question["knowledgeBindings"]):
        binding_where = f"{where}.knowledgeBindings[{index}]"
        if not isinstance(binding, dict):
            raise ValueError(f"{binding_where} must be an object")
        exact(binding, {"knowledgePointId", "weight"}, set(), binding_where)
        knowledge_id = identifier(binding["knowledgePointId"], f"{binding_where}.knowledgePointId")
        if knowledge_id not in knowledge_ids:
            raise ValueError(f"{binding_where} references unknown knowledge point")
        if knowledge_id in bound_ids:
            raise ValueError(f"{where}.knowledgeBindings contains duplicate knowledge point")
        bound_ids.add(knowledge_id)
        weight = finite_number(binding["weight"], f"{binding_where}.weight")
        if weight <= 0:
            raise ValueError(f"{binding_where}.weight must be positive")
        total_weight += weight
    if not math.isclose(total_weight, 1.0, rel_tol=1e-9, abs_tol=1e-9):
        raise ValueError(f"{where}.knowledgeBindings weights must total 1.0")

    difficulty = finite_number(question["difficulty"], f"{where}.difficulty")
    if not 0.0 <= difficulty <= 1.0:
        raise ValueError(f"{where}.difficulty must be between 0 and 1")

    if not isinstance(question["hints"], list):
        raise ValueError(f"{where}.hints must be an array")
    hint_ids: set[str] = set()
    for index, hint in enumerate(question["hints"]):
        hint_where = f"{where}.hints[{index}]"
        if not isinstance(hint, dict):
            raise ValueError(f"{hint_where} must be an object")
        exact(hint, {"id", "text"}, set(), hint_where)
        hint_id = identifier(hint["id"], f"{hint_where}.id")
        if hint_id in hint_ids:
            raise ValueError(f"{where}.hints contains duplicate id")
        hint_ids.add(hint_id)
        text(hint["text"], f"{hint_where}.text")

    if not isinstance(question["choices"], list):
        raise ValueError(f"{where}.choices must be an array")
    choice_ids: set[str] = set()
    for index, choice in enumerate(question["choices"]):
        choice_where = f"{where}.choices[{index}]"
        if not isinstance(choice, dict):
            raise ValueError(f"{choice_where} must be an object")
        exact(choice, {"id", "content"}, set(), choice_where)
        choice_id = identifier(choice["id"], f"{choice_where}.id")
        if choice_id in choice_ids:
            raise ValueError(f"{where}.choices contains duplicate id")
        choice_ids.add(choice_id)
        validate_content_array(choice["content"], f"{choice_where}.content", referenced_assets, False)
    if input_type == "single_choice":
        declared = set(input_value["optionIds"])
        if declared != choice_ids:
            raise ValueError(f"{where}.input.optionIds must match choices")
        if answer["expectedOptionId"] not in choice_ids:
            raise ValueError(f"{where}.answer.expectedOptionId must reference a choice")
    elif question["choices"]:
        raise ValueError(f"{where}.choices must be empty for non-choice input")

    validate_content_array(question["explanation"], f"{where}.explanation", referenced_assets, True)
    unknown_references = referenced_assets - asset_ids
    if unknown_references:
        raise ValueError(f"{where} references unknown assets: {sorted(unknown_references)}")
    return question_id


def validate_package(root: Path) -> dict[str, int]:
    assessments_path = root / ASSESSMENTS
    knowledge_path = root / KNOWLEDGE
    if assessments_path.exists() != knowledge_path.exists():
        raise ValueError(f"{root}: assessment extension files must appear together")
    if not assessments_path.exists():
        return {"questionSets": 0, "questions": 0, "knowledgePoints": 0, "assets": 0}

    course = load_json(root / "course.json")
    section_ids = {
        section["id"]
        for chapter in course.get("chapters", [])
        for section in chapter.get("sections", []) + ([chapter["review"]] if chapter.get("review") else [])
    }
    assessments = load_json(assessments_path)
    knowledge = load_json(knowledge_path)

    if not isinstance(knowledge, dict):
        raise ValueError(f"{KNOWLEDGE} must be an object")
    exact(knowledge, {"courseId", "knowledgePoints"}, set(), KNOWLEDGE)
    if knowledge["courseId"] != course["textbook"]["id"]:
        raise ValueError(f"{KNOWLEDGE}.courseId does not match course")
    if not isinstance(knowledge["knowledgePoints"], list) or not knowledge["knowledgePoints"]:
        raise ValueError(f"{KNOWLEDGE}.knowledgePoints must be a non-empty array")
    knowledge_ids: set[str] = set()
    prerequisite_edges: dict[str, list[str]] = {}
    for index, item in enumerate(knowledge["knowledgePoints"]):
        where = f"{KNOWLEDGE}.knowledgePoints[{index}]"
        if not isinstance(item, dict):
            raise ValueError(f"{where} must be an object")
        exact(item, {"id", "title", "description", "prerequisiteIds", "sectionIds"}, set(), where)
        item_id = identifier(item["id"], f"{where}.id")
        if item_id in knowledge_ids:
            raise ValueError(f"{KNOWLEDGE} contains duplicate knowledge point id")
        knowledge_ids.add(item_id)
        text(item["title"], f"{where}.title")
        text(item["description"], f"{where}.description")
        prerequisite_edges[item_id] = string_array(item["prerequisiteIds"], f"{where}.prerequisiteIds")
        referenced_sections = set(string_array(item["sectionIds"], f"{where}.sectionIds", allow_empty=False))
        if not referenced_sections <= section_ids:
            raise ValueError(f"{where} references unknown sections")

    for item_id, prerequisites in prerequisite_edges.items():
        if item_id in prerequisites:
            raise ValueError(f"{KNOWLEDGE}: {item_id} cannot depend on itself")
        unknown = set(prerequisites) - knowledge_ids
        if unknown:
            raise ValueError(f"{KNOWLEDGE}: {item_id} has unknown prerequisites: {sorted(unknown)}")
    visit_state: dict[str, int] = {}

    def visit(item_id: str) -> None:
        state = visit_state.get(item_id, 0)
        if state == 1:
            raise ValueError(f"{KNOWLEDGE}: prerequisite graph contains cycle at {item_id}")
        if state == 2:
            return
        visit_state[item_id] = 1
        for prerequisite in prerequisite_edges[item_id]:
            visit(prerequisite)
        visit_state[item_id] = 2

    for item_id in knowledge_ids:
        visit(item_id)

    if not isinstance(assessments, dict):
        raise ValueError(f"{ASSESSMENTS} must be an object")
    exact(assessments, {"courseId", "assets", "questionSets", "placements"}, set(), ASSESSMENTS)
    if assessments["courseId"] != course["textbook"]["id"]:
        raise ValueError(f"{ASSESSMENTS}.courseId does not match course")
    if not isinstance(assessments["assets"], list):
        raise ValueError(f"{ASSESSMENTS}.assets must be an array")
    asset_ids: set[str] = set()
    asset_paths: set[str] = set()
    for index, asset in enumerate(assessments["assets"]):
        where = f"{ASSESSMENTS}.assets[{index}]"
        if not isinstance(asset, dict):
            raise ValueError(f"{where} must be an object")
        exact(asset, {"id", "path", "mediaType", "sha256", "width", "height"}, set(), where)
        asset_id = identifier(asset["id"], f"{where}.id")
        if asset_id in asset_ids:
            raise ValueError(f"{ASSESSMENTS} contains duplicate asset id")
        asset_ids.add(asset_id)
        asset_path = text(asset["path"], f"{where}.path")
        target = (root / asset_path).resolve()
        if root.resolve() not in target.parents:
            raise ValueError(f"{where}.path escapes course root")
        if asset_path in asset_paths:
            raise ValueError(f"{ASSESSMENTS} contains duplicate asset path")
        asset_paths.add(asset_path)
        if not target.is_file():
            raise ValueError(f"{where}.path is missing")
        if asset["mediaType"] != "image/png":
            raise ValueError(f"{where}.mediaType must be image/png")
        digest = hashlib.sha256(target.read_bytes()).hexdigest()
        if digest != asset["sha256"]:
            raise ValueError(f"{where}.sha256 mismatch")
        width, height = png_size(target)
        if width != positive_int(asset["width"], f"{where}.width") or height != positive_int(asset["height"], f"{where}.height"):
            raise ValueError(f"{where} dimensions mismatch")

    if not isinstance(assessments["questionSets"], list) or not assessments["questionSets"]:
        raise ValueError(f"{ASSESSMENTS}.questionSets must be a non-empty array")
    question_set_ids: set[str] = set()
    question_ids: set[str] = set()
    referenced_assets: set[str] = set()
    question_count = 0
    for set_index, question_set in enumerate(assessments["questionSets"]):
        set_where = f"{ASSESSMENTS}.questionSets[{set_index}]"
        if not isinstance(question_set, dict):
            raise ValueError(f"{set_where} must be an object")
        exact(question_set, {"id", "title", "allowSkip", "allowReviewBeforeFinish", "questions"}, set(), set_where)
        set_id = identifier(question_set["id"], f"{set_where}.id")
        if set_id in question_set_ids:
            raise ValueError(f"{ASSESSMENTS} contains duplicate question set id")
        question_set_ids.add(set_id)
        text(question_set["title"], f"{set_where}.title")
        if not isinstance(question_set["allowSkip"], bool) or not isinstance(question_set["allowReviewBeforeFinish"], bool):
            raise ValueError(f"{set_where} flags must be boolean")
        if not isinstance(question_set["questions"], list) or not question_set["questions"]:
            raise ValueError(f"{set_where}.questions must be a non-empty array")
        for question_index, question in enumerate(question_set["questions"]):
            question_id = validate_question(
                question,
                f"{set_where}.questions[{question_index}]",
                knowledge_ids,
                asset_ids,
                referenced_assets,
            )
            if question_id in question_ids:
                raise ValueError(f"{ASSESSMENTS} contains duplicate question id")
            question_ids.add(question_id)
            question_count += 1

    if referenced_assets != asset_ids:
        unused = asset_ids - referenced_assets
        raise ValueError(f"{ASSESSMENTS} contains unused assets: {sorted(unused)}")

    if not isinstance(assessments["placements"], list) or not assessments["placements"]:
        raise ValueError(f"{ASSESSMENTS}.placements must be a non-empty array")
    placed_sets: set[str] = set()
    placed_sections: set[str] = set()
    for index, placement in enumerate(assessments["placements"]):
        where = f"{ASSESSMENTS}.placements[{index}]"
        if not isinstance(placement, dict):
            raise ValueError(f"{where} must be an object")
        exact(placement, {"sectionId", "questionSetIds"}, set(), where)
        section_id = text(placement["sectionId"], f"{where}.sectionId")
        if section_id not in section_ids:
            raise ValueError(f"{where}.sectionId is unknown")
        if section_id in placed_sections:
            raise ValueError(f"{ASSESSMENTS}.placements contains duplicate section")
        placed_sections.add(section_id)
        ids = string_array(placement["questionSetIds"], f"{where}.questionSetIds", allow_empty=False)
        unknown = set(ids) - question_set_ids
        if unknown:
            raise ValueError(f"{where} references unknown question sets: {sorted(unknown)}")
        overlap = set(ids) & placed_sets
        if overlap:
            raise ValueError(f"{ASSESSMENTS} question sets placed more than once: {sorted(overlap)}")
        placed_sets.update(ids)
    if placed_sets != question_set_ids:
        raise ValueError(f"{ASSESSMENTS} contains unplaced question sets: {sorted(question_set_ids - placed_sets)}")

    return {
        "questionSets": len(question_set_ids),
        "questions": question_count,
        "knowledgePoints": len(knowledge_ids),
        "assets": len(asset_ids),
    }


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError(f"{path}: invalid PNG header")
    return int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big")


def materialize_approved_crops(course_root: Path, pdf_root: Path) -> int:
    crop_file = course_root / ASSET_CROPS
    if not crop_file.is_file():
        return 0
    config = load_json(crop_file)
    exact(config, {"courseId", "pdfSha256", "pageIndexOffset", "crops"}, set(), str(crop_file))
    if config["courseId"] != course_root.name:
        raise ValueError(f"{crop_file}: courseId does not match directory")
    offset = config["pageIndexOffset"]
    if not isinstance(offset, int):
        raise ValueError(f"{crop_file}: pageIndexOffset must be an integer")
    pdfs = sorted(pdf_root.glob("*.pdf"))
    matching = [path for path in pdfs if hashlib.sha256(path.read_bytes()).hexdigest() == config["pdfSha256"]]
    if len(matching) != 1:
        raise ValueError(f"{crop_file}: expected exactly one matching PDF")
    pdf_path = matching[0]
    count = 0
    with fitz.open(pdf_path) as document:
        for index, crop in enumerate(config["crops"]):
            where = f"{crop_file}.crops[{index}]"
            exact(
                crop,
                {"candidateId", "assetId", "path", "printedPage", "clip", "renderScale", "mediaType", "width", "height", "reviewStatus", "reviewNotes"},
                set(),
                where,
            )
            if crop["reviewStatus"] != "approved":
                raise ValueError(f"{where}: only approved crops may be materialized")
            identifier(crop["assetId"], f"{where}.assetId")
            if crop["mediaType"] != "image/png":
                raise ValueError(f"{where}.mediaType must be image/png")
            clip_values = crop["clip"]
            if not isinstance(clip_values, list) or len(clip_values) != 4:
                raise ValueError(f"{where}.clip must contain four numbers")
            clip = fitz.Rect(*(finite_number(value, f"{where}.clip[]") for value in clip_values))
            page_index = positive_int(crop["printedPage"], f"{where}.printedPage") + offset - 1
            if page_index < 0 or page_index >= document.page_count:
                raise ValueError(f"{where}.printedPage is outside the PDF")
            scale = finite_number(crop["renderScale"], f"{where}.renderScale")
            if scale <= 0:
                raise ValueError(f"{where}.renderScale must be positive")
            pixmap = document[page_index].get_pixmap(matrix=fitz.Matrix(scale, scale), clip=clip, alpha=False)
            output = (course_root / text(crop["path"], f"{where}.path")).resolve()
            if course_root.resolve() not in output.parents:
                raise ValueError(f"{where}.path escapes course root")
            output.parent.mkdir(parents=True, exist_ok=True)
            pixmap.save(output)
            width = positive_int(crop["width"], f"{where}.width")
            height = positive_int(crop["height"], f"{where}.height")
            if (pixmap.width, pixmap.height) != (width, height):
                raise ValueError(
                    f"{where}: rendered dimensions {(pixmap.width, pixmap.height)} do not match {(width, height)}"
                )
            count += 1
    return count


def prepare(source_root: Path, pdf_root: Path) -> dict[str, dict[str, int]]:
    results: dict[str, dict[str, int]] = {}
    for course_root in sorted(path for path in source_root.iterdir() if path.is_dir()):
        has_assessments = (course_root / ASSESSMENTS).is_file()
        has_knowledge = (course_root / KNOWLEDGE).is_file()
        if not has_assessments and not has_knowledge:
            continue
        materialize_approved_crops(course_root, pdf_root)
        results[course_root.name] = validate_package(course_root)
    return results


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate and materialize optional course assessment extensions.")
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--pdf-root", required=True, type=Path)
    args = parser.parse_args()
    results = prepare(args.source_root, args.pdf_root)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
