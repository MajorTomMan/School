#!/usr/bin/env python3
"""Attach manually reviewed assessments, knowledge points and approved PDF crops to generated courses."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import struct
from typing import Any

import fitz

ASSESSMENTS = "assessments.json"
KNOWLEDGE = "knowledge-points.json"
CROPS = "asset-crops.json"
ID = re.compile(r"^[a-z0-9][a-z0-9._-]{0,95}$")
ASSET_ID = re.compile(r"^[a-z][a-z0-9_-]{0,63}$")
CONTENT_TYPES = {"heading", "text", "formula", "list", "image", "table", "scene"}
TEXT_STYLES = {"body", "prompt", "caption", "explanation"}
INPUT_ANSWER = {
    "integer": "exact_integer",
    "decimal": "decimal",
    "rational": "rational_equivalent",
    "single_choice": "single_choice",
    "coordinate": "coordinate",
}


def exact(obj: dict[str, Any], required: set[str], optional: set[str], where: str) -> None:
    missing = required - obj.keys()
    unknown = obj.keys() - required - optional
    if missing or unknown:
        raise ValueError(f"{where}: missing={sorted(missing)}, unknown={sorted(unknown)}")


def text(value: Any, where: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{where} must be a non-empty string")
    return value.strip()


def identifier(value: Any, where: str, pattern: re.Pattern[str] = ID) -> str:
    result = text(value, where)
    if not pattern.fullmatch(result):
        raise ValueError(f"{where} has invalid identifier: {result}")
    return result


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def safe_asset_path(value: Any, where: str) -> str:
    raw = text(value, where)
    path = PurePosixPath(raw)
    if path.is_absolute() or not raw.startswith("assets/") or "\\" in raw:
        raise ValueError(f"{where} must be a safe assets/ path")
    if any(part in {"", ".", ".."} for part in path.parts):
        raise ValueError(f"{where} contains unsafe segments")
    return raw


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
        if mode is not None and mode not in {"road", "construction", "value", "example", "read_points"}:
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
        asset = identifier(value["assetId"], f"{where}.assetId", ASSET_ID)
        referenced_assets.add(asset)
        text(value["altText"], f"{where}.altText")
        if "caption" in value:
            text(value["caption"], f"{where}.caption")
    elif kind == "table":
        exact(value, {"type", "columns", "rows"}, {"caption", "sourceAssetId"}, where)
        columns = string_array(value["columns"], f"{where}.columns", allow_empty=False)
        rows = value["rows"]
        if not isinstance(rows, list) or not rows:
            raise ValueError(f"{where}.rows must be non-empty")
        for index, row in enumerate(rows):
            cells = string_array(row, f"{where}.rows[{index}]", allow_empty=False)
            if len(cells) != len(columns):
                raise ValueError(f"{where}.rows[{index}] width mismatch")
        if "caption" in value:
            text(value["caption"], f"{where}.caption")
        if "sourceAssetId" in value:
            referenced_assets.add(identifier(value["sourceAssetId"], f"{where}.sourceAssetId", ASSET_ID))
    else:
        validate_scene(value, where)


def validate_content_array(value: Any, where: str, referenced_assets: set[str], allow_empty: bool) -> None:
    if not isinstance(value, list) or (not allow_empty and not value):
        raise ValueError(f"{where} must be {'non-empty ' if not allow_empty else ''}array")
    for index, item in enumerate(value):
        validate_content(item, f"{where}[{index}]", referenced_assets)


def validate_question(question: Any, where: str, knowledge_ids: set[str], referenced_assets: set[str]) -> str:
    if not isinstance(question, dict):
        raise ValueError(f"{where} must be an object")
    exact(question, {"id", "revision", "number", "stem", "input", "answer", "knowledgeBindings",
                     "difficulty", "hints", "choices", "explanation"}, set(), where)
    question_id = identifier(question["id"], f"{where}.id")
    positive_int(question["revision"], f"{where}.revision")
    text(question["number"], f"{where}.number")
    validate_content_array(question["stem"], f"{where}.stem", referenced_assets, False)
    validate_content_array(question["explanation"], f"{where}.explanation", referenced_assets, True)

    input_spec, answer = question["input"], question["answer"]
    if not isinstance(input_spec, dict) or not isinstance(answer, dict):
        raise ValueError(f"{where}.input and answer must be objects")
    input_type = text(input_spec.get("type"), f"{where}.input.type")
    answer_type = text(answer.get("type"), f"{where}.answer.type")
    if INPUT_ANSWER.get(input_type) != answer_type:
        raise ValueError(f"{where}: input/answer mismatch")
    if input_type == "integer":
        exact(input_spec, {"type"}, set(), f"{where}.input")
        exact(answer, {"type", "expected"}, set(), f"{where}.answer")
        if isinstance(answer["expected"], bool) or not isinstance(answer["expected"], int):
            raise ValueError(f"{where}.answer.expected must be integer")
    elif input_type == "decimal":
        exact(input_spec, {"type", "allowFraction"}, set(), f"{where}.input")
        if not isinstance(input_spec["allowFraction"], bool):
            raise ValueError(f"{where}.input.allowFraction must be boolean")
        exact(answer, {"type", "expected", "tolerance"}, set(), f"{where}.answer")
        finite_number(answer["expected"], f"{where}.answer.expected")
        if finite_number(answer["tolerance"], f"{where}.answer.tolerance") < 0:
            raise ValueError(f"{where}.answer.tolerance cannot be negative")
    elif input_type == "rational":
        exact(input_spec, {"type", "allowDecimal"}, set(), f"{where}.input")
        if not isinstance(input_spec["allowDecimal"], bool):
            raise ValueError(f"{where}.input.allowDecimal must be boolean")
        exact(answer, {"type", "expected"}, set(), f"{where}.answer")
        validate_rational(answer["expected"], f"{where}.answer.expected")
    elif input_type == "single_choice":
        exact(input_spec, {"type", "optionIds"}, set(), f"{where}.input")
        option_ids = string_array(input_spec["optionIds"], f"{where}.input.optionIds", allow_empty=False)
        if len(option_ids) < 2:
            raise ValueError(f"{where} needs at least two choices")
        exact(answer, {"type", "expectedOptionId"}, set(), f"{where}.answer")
        expected = text(answer["expectedOptionId"], f"{where}.answer.expectedOptionId")
        if expected not in option_ids:
            raise ValueError(f"{where} expected option is missing")
    elif input_type == "coordinate":
        exact(input_spec, {"type"}, set(), f"{where}.input")
        exact(answer, {"type", "expected"}, set(), f"{where}.answer")
        expected = answer["expected"]
        if not isinstance(expected, dict):
            raise ValueError(f"{where}.answer.expected must be object")
        exact(expected, {"x", "y"}, set(), f"{where}.answer.expected")
        validate_rational(expected["x"], f"{where}.answer.expected.x")
        validate_rational(expected["y"], f"{where}.answer.expected.y")
    else:
        raise ValueError(f"{where}.input.type unsupported")

    bindings = question["knowledgeBindings"]
    if not isinstance(bindings, list) or not bindings:
        raise ValueError(f"{where}.knowledgeBindings must be non-empty")
    bound: list[str] = []
    for index, binding in enumerate(bindings):
        location = f"{where}.knowledgeBindings[{index}]"
        if not isinstance(binding, dict):
            raise ValueError(f"{location} must be object")
        exact(binding, {"knowledgePointId", "weight"}, set(), location)
        knowledge_id = identifier(binding["knowledgePointId"], f"{location}.knowledgePointId")
        if knowledge_id not in knowledge_ids:
            raise ValueError(f"{location} references unknown knowledge point")
        if finite_number(binding["weight"], f"{location}.weight") <= 0:
            raise ValueError(f"{location}.weight must be positive")
        bound.append(knowledge_id)
    if len(bound) != len(set(bound)):
        raise ValueError(f"{where}.knowledgeBindings contains duplicates")

    difficulty = finite_number(question["difficulty"], f"{where}.difficulty")
    if not 0 <= difficulty <= 1:
        raise ValueError(f"{where}.difficulty outside 0..1")
    hints = question["hints"]
    if not isinstance(hints, list):
        raise ValueError(f"{where}.hints must be array")
    hint_ids: list[str] = []
    for index, hint in enumerate(hints):
        location = f"{where}.hints[{index}]"
        if not isinstance(hint, dict):
            raise ValueError(f"{location} must be object")
        exact(hint, {"id", "text"}, set(), location)
        hint_ids.append(identifier(hint["id"], f"{location}.id"))
        text(hint["text"], f"{location}.text")
    if len(hint_ids) != len(set(hint_ids)):
        raise ValueError(f"{where}.hints contains duplicates")

    choices = question["choices"]
    if not isinstance(choices, list):
        raise ValueError(f"{where}.choices must be array")
    choice_ids: list[str] = []
    for index, choice in enumerate(choices):
        location = f"{where}.choices[{index}]"
        if not isinstance(choice, dict):
            raise ValueError(f"{location} must be object")
        exact(choice, {"id", "content"}, set(), location)
        choice_ids.append(identifier(choice["id"], f"{location}.id"))
        validate_content_array(choice["content"], f"{location}.content", referenced_assets, False)
    if len(choice_ids) != len(set(choice_ids)):
        raise ValueError(f"{where}.choices contains duplicates")
    if input_type == "single_choice":
        if choice_ids != input_spec["optionIds"]:
            raise ValueError(f"{where}.choices must match optionIds in order")
    elif choices:
        raise ValueError(f"{where}: only single_choice may declare choices")
    return question_id


def png_dimensions(path: Path) -> tuple[int, int]:
    with path.open("rb") as stream:
        header = stream.read(24)
    if len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError(f"not a PNG: {path}")
    return struct.unpack(">II", header[16:24])


def validate_package(root: Path) -> dict[str, int]:
    course = json.loads((root / "course.json").read_text(encoding="utf-8"))
    assessments = json.loads((root / ASSESSMENTS).read_text(encoding="utf-8"))
    knowledge = json.loads((root / KNOWLEDGE).read_text(encoding="utf-8"))
    exact(assessments, {"courseId", "assets", "questionSets", "placements"}, set(), ASSESSMENTS)
    exact(knowledge, {"courseId", "knowledgePoints"}, set(), KNOWLEDGE)
    course_id = text(course["textbook"]["id"], "course.textbook.id")
    if assessments["courseId"] != course_id or knowledge["courseId"] != course_id:
        raise ValueError("extension courseId does not match course.json")
    sections = {
        section["id"]
        for chapter in course["chapters"]
        for section in chapter.get("sections", []) + ([chapter["review"]] if chapter.get("review") else [])
    }

    knowledge_ids: set[str] = set()
    prerequisites: dict[str, list[str]] = {}
    for index, point in enumerate(knowledge["knowledgePoints"]):
        where = f"{KNOWLEDGE}.knowledgePoints[{index}]"
        if not isinstance(point, dict):
            raise ValueError(f"{where} must be object")
        exact(point, {"id", "title", "description", "prerequisiteIds", "sectionIds"}, set(), where)
        point_id = identifier(point["id"], f"{where}.id")
        if point_id in knowledge_ids:
            raise ValueError(f"duplicate knowledge point: {point_id}")
        knowledge_ids.add(point_id)
        text(point["title"], f"{where}.title")
        text(point["description"], f"{where}.description")
        prerequisites[point_id] = string_array(point["prerequisiteIds"], f"{where}.prerequisiteIds")
        point_sections = string_array(point["sectionIds"], f"{where}.sectionIds", allow_empty=False)
        if set(point_sections) - sections:
            raise ValueError(f"{where} references unknown sections")

    for point_id, required in prerequisites.items():
        if set(required) - knowledge_ids or point_id in required:
            raise ValueError(f"invalid prerequisites for {point_id}")
    visiting: set[str] = set()
    visited: set[str] = set()
    def visit(point_id: str) -> None:
        if point_id in visited:
            return
        if point_id in visiting:
            raise ValueError("knowledge prerequisite cycle")
        visiting.add(point_id)
        for item in prerequisites[point_id]:
            visit(item)
        visiting.remove(point_id)
        visited.add(point_id)
    for point_id in knowledge_ids:
        visit(point_id)

    declared_assets: set[str] = set()
    for index, asset in enumerate(assessments["assets"]):
        where = f"{ASSESSMENTS}.assets[{index}]"
        if not isinstance(asset, dict):
            raise ValueError(f"{where} must be object")
        exact(asset, {"id", "path", "mediaType", "width", "height"}, set(), where)
        asset_id = identifier(asset["id"], f"{where}.id", ASSET_ID)
        if asset_id in declared_assets:
            raise ValueError(f"duplicate asset: {asset_id}")
        declared_assets.add(asset_id)
        path = safe_asset_path(asset["path"], f"{where}.path")
        if asset["mediaType"] != "image/png" or not path.lower().endswith(".png"):
            raise ValueError(f"{where} only supports approved PNG in this workflow")
        width = positive_int(asset["width"], f"{where}.width")
        height = positive_int(asset["height"], f"{where}.height")
        if png_dimensions(root / path) != (width, height):
            raise ValueError(f"{where} dimensions do not match generated PNG")

    referenced_assets: set[str] = set()
    set_ids: list[str] = []
    question_ids: set[tuple[str, int]] = set()
    for set_index, question_set in enumerate(assessments["questionSets"]):
        where = f"{ASSESSMENTS}.questionSets[{set_index}]"
        if not isinstance(question_set, dict):
            raise ValueError(f"{where} must be object")
        exact(question_set, {"id", "title", "allowSkip", "allowReviewBeforeFinish", "questions"}, set(), where)
        set_id = identifier(question_set["id"], f"{where}.id")
        set_ids.append(set_id)
        text(question_set["title"], f"{where}.title")
        if not isinstance(question_set["allowSkip"], bool) or not isinstance(question_set["allowReviewBeforeFinish"], bool):
            raise ValueError(f"{where} flags must be boolean")
        questions = question_set["questions"]
        if not isinstance(questions, list) or not questions:
            raise ValueError(f"{where}.questions must be non-empty")
        for question_index, question in enumerate(questions):
            question_id = validate_question(
                question, f"{where}.questions[{question_index}]", knowledge_ids, referenced_assets
            )
            key = (question_id, question["revision"])
            if key in question_ids:
                raise ValueError(f"duplicate question key: {key}")
            question_ids.add(key)
    if len(set_ids) != len(set(set_ids)):
        raise ValueError("duplicate question set ids")

    placed: list[str] = []
    placement_sections: set[str] = set()
    for index, placement in enumerate(assessments["placements"]):
        where = f"{ASSESSMENTS}.placements[{index}]"
        if not isinstance(placement, dict):
            raise ValueError(f"{where} must be object")
        exact(placement, {"sectionId", "questionSetIds"}, set(), where)
        section_id = identifier(placement["sectionId"], f"{where}.sectionId")
        if section_id not in sections or section_id in placement_sections:
            raise ValueError(f"{where} has invalid or duplicate section")
        placement_sections.add(section_id)
        ids = string_array(placement["questionSetIds"], f"{where}.questionSetIds", allow_empty=False)
        if set(ids) - set(set_ids):
            raise ValueError(f"{where} references unknown question set")
        placed.extend(ids)
    if len(placed) != len(set(placed)) or set(placed) != set(set_ids):
        raise ValueError("each question set must be placed exactly once")
    if referenced_assets != declared_assets:
        raise ValueError(f"asset references mismatch: declared={sorted(declared_assets)}, used={sorted(referenced_assets)}")
    return {"questionSets": len(set_ids), "questions": len(question_ids), "knowledgePoints": len(knowledge_ids),
            "assets": len(declared_assets)}


def prepare_one(manual: Path, generated: Path, pdf: Path) -> dict[str, int]:
    for name in (ASSESSMENTS, KNOWLEDGE, CROPS):
        if not (manual / name).is_file():
            raise ValueError(f"{manual}: missing {name}")
    crop_document = json.loads((manual / CROPS).read_text(encoding="utf-8"))
    exact(crop_document, {"courseId", "pdfSha256", "pageIndexOffset", "crops"}, set(), CROPS)
    if sha256(pdf) != crop_document["pdfSha256"]:
        raise ValueError(f"{pdf.name}: SHA-256 does not match approved crop source")
    assessments = json.loads((manual / ASSESSMENTS).read_text(encoding="utf-8"))
    if assessments["courseId"] != crop_document["courseId"]:
        raise ValueError("asset crop courseId mismatch")
    declared = {item["id"]: item for item in assessments["assets"]}
    document = fitz.open(pdf)
    generated.mkdir(parents=True, exist_ok=True)
    for crop in crop_document["crops"]:
        exact(crop, {"candidateId", "assetId", "path", "printedPage", "clip", "renderScale", "mediaType",
                     "width", "height", "reviewStatus", "reviewNotes"}, set(), "asset-crops.crops[]")
        if crop["reviewStatus"] != "approved" or crop["mediaType"] != "image/png":
            raise ValueError(f"crop is not approved PNG: {crop.get('candidateId')}")
        asset_id = identifier(crop["assetId"], "crop.assetId", ASSET_ID)
        if asset_id not in declared:
            raise ValueError(f"crop asset is not declared: {asset_id}")
        relative = safe_asset_path(crop["path"], "crop.path")
        target = generated / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        printed_page = positive_int(crop["printedPage"], "crop.printedPage")
        page_index = printed_page + int(crop_document["pageIndexOffset"]) - 1
        if page_index not in range(document.page_count):
            raise ValueError(f"crop page outside PDF: {printed_page}")
        clip = crop["clip"]
        if not isinstance(clip, list) or len(clip) != 4:
            raise ValueError("crop.clip must contain four numbers")
        rect = fitz.Rect(*[finite_number(item, "crop.clip[]") for item in clip])
        if rect.is_empty or not document[page_index].rect.contains(rect):
            raise ValueError("crop.clip outside PDF page")
        scale = finite_number(crop["renderScale"], "crop.renderScale")
        if scale <= 0 or scale > 8:
            raise ValueError("crop.renderScale outside range")
        pixmap = document[page_index].get_pixmap(matrix=fitz.Matrix(scale, scale), clip=rect, alpha=False)
        pixmap.save(target)
        expected = (positive_int(crop["width"], "crop.width"), positive_int(crop["height"], "crop.height"))
        if (pixmap.width, pixmap.height) != expected:
            raise ValueError(f"crop dimensions changed: {(pixmap.width, pixmap.height)} != {expected}")
        declaration = declared[asset_id]
        if declaration["path"] != relative or (declaration["width"], declaration["height"]) != expected:
            raise ValueError(f"assessment asset declaration does not match crop: {asset_id}")
    shutil.copyfile(manual / ASSESSMENTS, generated / ASSESSMENTS)
    shutil.copyfile(manual / KNOWLEDGE, generated / KNOWLEDGE)
    return validate_package(generated)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--pdf-root", type=Path, required=True)
    parser.add_argument("--manual-root", type=Path, default=Path(__file__).resolve().parent / "manual")
    args = parser.parse_args()
    prepared = 0
    for manual in sorted(path for path in args.manual_root.iterdir() if path.is_dir()):
        if not (manual / ASSESSMENTS).is_file():
            continue
        generated = args.source_root / manual.name
        course = json.loads((generated / "course.json").read_text(encoding="utf-8"))
        candidates = sorted(args.pdf_root.glob("*.pdf"))
        matching = [
            path for path in candidates
            if course["textbook"]["grade"] in path.name and course["textbook"]["semester"] in path.name
        ]
        if len(matching) != 1:
            raise ValueError(f"cannot resolve exactly one textbook PDF for {manual.name}: {matching}")
        result = prepare_one(manual, generated, matching[0])
        print(f"{manual.name}: {result}")
        prepared += 1
    print(f"prepared assessment extensions for {prepared} courses")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
