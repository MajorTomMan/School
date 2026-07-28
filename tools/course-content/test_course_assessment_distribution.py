#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
import shutil
import struct
import tempfile
import unittest
import zipfile

from copy_assessment_extensions import copy_extensions
from course_release_bundle import collect_bundled_files, write_deterministic_zip
from postprocess_math_courses import manual_section_paths
from prepare_assessment_packages import validate_package


class CourseAssessmentDistributionTest(unittest.TestCase):
    def manual_root(self) -> Path:
        return Path(__file__).resolve().parent / "manual" / "pep-math-7-1"

    def minimal_course(self) -> dict:
        return {
            "textbook": {"id": "pep-math-7-1"},
            "chapters": [{
                "sections": [
                    {"id": "1.1"}, {"id": "1.2.1"}, {"id": "1.2.2"}, {"id": "1.2.3"},
                ],
            }],
        }

    def write_fake_declared_png(self, target: Path, width: int, height: int) -> None:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(
            b"\x89PNG\r\n\x1a\n" + b"\x00\x00\x00\rIHDR" + struct.pack(">II", width, height)
        )

    def test_checked_in_grade7_documents_match_distribution_contract(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            root.joinpath("course.json").write_text(json.dumps(self.minimal_course()), encoding="utf-8")
            for name in ("assessments.json", "knowledge-points.json"):
                shutil.copyfile(self.manual_root() / name, root / name)
            assessments = json.loads((root / "assessments.json").read_text(encoding="utf-8"))
            for asset in assessments["assets"]:
                self.write_fake_declared_png(root / asset["path"], asset["width"], asset["height"])
            result = validate_package(root)
            self.assertEqual(
                {"questionSets": 4, "questions": 20, "knowledgePoints": 11, "assets": 0},
                result,
            )

    def test_manual_section_overlay_ignores_package_extension_json(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in (
                "1.1.json",
                "1.2.1.json",
                "assessments.json",
                "knowledge-points.json",
                "asset-crops.json",
                "review-decisions.json",
            ):
                (root / name).write_text("{}", encoding="utf-8")
            self.assertEqual(
                ["1.1.json", "1.2.1.json"],
                [path.name for path in manual_section_paths(root)],
            )

    def test_deterministic_zip_contains_extensions_and_assets_in_sorted_order(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for relative in (
                "course.json", "knowledge-points.json", "assessments.json",
                "assets/figures/b.png", "assets/figures/a.png",
            ):
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(relative, encoding="utf-8")
            files = collect_bundled_files(root)
            archive = root / "course.zip"
            write_deterministic_zip(files, archive)
            with zipfile.ZipFile(archive) as bundle:
                self.assertEqual(sorted(files), bundle.namelist())

    def test_extension_pair_is_required(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "source"
            target = Path(directory) / "target"
            source.mkdir()
            (source / "assessments.json").write_text("{}", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "must be provided together"):
                copy_extensions(source, target)


if __name__ == "__main__":
    unittest.main()
