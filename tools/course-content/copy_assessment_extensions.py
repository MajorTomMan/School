#!/usr/bin/env python3
"""Copy already validated assessment extensions and assets beside normalized course.json files."""
from __future__ import annotations

import argparse
from pathlib import Path
import shutil

from prepare_assessment_packages import ASSESSMENTS, KNOWLEDGE, validate_package


def copy_extensions(source: Path, target: Path) -> bool:
    assessment = source / ASSESSMENTS
    knowledge = source / KNOWLEDGE
    if assessment.is_file() != knowledge.is_file():
        raise ValueError(f"{source}: assessment extension files must appear together")
    if not assessment.is_file():
        return False
    shutil.copyfile(assessment, target / ASSESSMENTS)
    shutil.copyfile(knowledge, target / KNOWLEDGE)
    source_assets = source / "assets"
    if source_assets.is_dir():
        shutil.copytree(source_assets, target / "assets", dirs_exist_ok=True)
    validate_package(target)
    return True


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    args = parser.parse_args()
    count = 0
    for source in sorted(path for path in args.source_root.iterdir() if path.is_dir()):
        target = args.output_root / source.name
        if not (target / "course.json").is_file():
            continue
        if copy_extensions(source, target):
            count += 1
    print(f"copied assessment extensions for {count} courses")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
