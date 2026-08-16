#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VISUALIZATION_SOURCE = ROOT / "visualization" / "src" / "main" / "java"
PYTHON_CONTRACT = ROOT / "tools" / "course-content" / "visualization_contract.py"
KEY_PATTERN = re.compile(r'VisualizationKey\("([a-z0-9][a-z0-9._-]*)"\)')


def load_python_keys() -> set[str]:
    spec = importlib.util.spec_from_file_location("course_visualization_contract", PYTHON_CONTRACT)
    if spec is None or spec.loader is None:
        raise SystemExit(f"cannot import {PYTHON_CONTRACT}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return set(module.RENDERER_SCHEMAS)


def load_kotlin_keys() -> set[str]:
    keys: set[str] = set()
    for path in VISUALIZATION_SOURCE.rglob("*.kt"):
        keys.update(KEY_PATTERN.findall(path.read_text(encoding="utf-8")))
    return keys


def main() -> int:
    kotlin_keys = load_kotlin_keys()
    python_keys = load_python_keys()
    if kotlin_keys != python_keys:
        only_kotlin = sorted(kotlin_keys - python_keys)
        only_python = sorted(python_keys - kotlin_keys)
        details = []
        if only_kotlin:
            details.append(f"missing from Python contract: {only_kotlin}")
        if only_python:
            details.append(f"missing from Kotlin registry/source: {only_python}")
        raise SystemExit("visualization renderer contract is out of sync:\n" + "\n".join(details))
    print(f"visualization contract sync passed: {len(kotlin_keys)} renderer keys")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
