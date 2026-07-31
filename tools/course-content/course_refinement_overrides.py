#!/usr/bin/env python3
"""Apply explicit, auditable course-content overrides before packaging.

Overrides live beside a textbook's ``course.json`` and are keyed by stable page id.
They are content data, not APK behavior. Every changed page remains pending until a
human PDF review records a matching digest.
"""
from __future__ import annotations

import copy
import json
from pathlib import Path
from typing import Any


def _page_index(course: dict[str, Any]) -> dict[str, dict[str, Any]]:
    pages: dict[str, dict[str, Any]] = {}
    for chapter in course.get("chapters", []):
        for section in chapter.get("sections", []):
            for page in section.get("pages", []):
                if not isinstance(page, dict):
                    continue
                page_id = str(page.get("id", "")).strip()
                if not page_id:
                    raise ValueError("course page is missing id")
                if page_id in pages:
                    raise ValueError(f"duplicate course page id: {page_id}")
                pages[page_id] = page
    return pages


def apply_refinement_overrides(course: dict[str, Any], textbook_root: Path) -> dict[str, Any]:
    """Return a deep-copied course with optional page overrides applied."""
    override_path = textbook_root / "refinement-overrides.json"
    if not override_path.is_file():
        return course

    payload = json.loads(override_path.read_text(encoding="utf-8"))
    if payload.get("textbookId") != course.get("textbook", {}).get("id"):
        raise ValueError(f"refinement override textbook mismatch: {override_path}")
    entries = payload.get("pages", {})
    if not isinstance(entries, dict):
        raise ValueError("refinement-overrides.json pages must be an object")

    result = copy.deepcopy(course)
    pages = _page_index(result)
    for page_id, patch in entries.items():
        if page_id not in pages:
            raise ValueError(f"refinement override references unknown page: {page_id}")
        if not isinstance(patch, dict):
            raise ValueError(f"refinement override for {page_id} must be an object")
        forbidden = {"id", "sourcePage"}.intersection(patch)
        if forbidden:
            raise ValueError(f"refinement override cannot replace stable fields for {page_id}: {sorted(forbidden)}")
        pages[page_id].update(copy.deepcopy(patch))
        pages[page_id]["reviewStatus"] = "pending"
        refinement = pages[page_id].setdefault("refinement", {})
        refinement.update({
            "source": "refinement-overrides.json",
            "verifiedAgainstPdf": False,
        })
    return result
