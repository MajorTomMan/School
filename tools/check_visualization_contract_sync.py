#!/usr/bin/env python3
from __future__ import annotations

import re
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VISUALIZATION_SOURCE = ROOT / "visualization" / "src" / "main" / "java"
PYTHON_CONTRACT = ROOT / "tools" / "course-content" / "visualization_contract.py"
SNAPSHOT = ROOT / "visualization" / "src" / "test" / "resources" / "visualization-contract.snapshot"
KEY_PATTERN = re.compile(r'VisualizationKey\("([a-z0-9][a-z0-9._-]*)"\)')


def load_python_contract() -> tuple[set[str], str]:
    namespace = runpy.run_path(str(PYTHON_CONTRACT))
    schemas = namespace["RENDERER_SCHEMAS"]
    lines: list[str] = []
    for renderer, schema in sorted(schemas.items()):
        parameters: list[str] = []
        for name in sorted(schema.parameter_names):
            if name in schema.number_list_parameters:
                parameter_type = "NUMBER_LIST"
            elif name in schema.boolean_parameters:
                parameter_type = "BOOLEAN"
            else:
                parameter_type = "NUMBER"
            requirement = "required" if name in schema.required_parameters else "optional"
            parameters.append(f"{name}:{parameter_type}:{requirement}")
        texts: list[str] = []
        for name in sorted(schema.text_names):
            requirement = "required" if name in schema.required_texts else "optional"
            allow_blank = "false" if name in schema.required_texts else "true"
            texts.append(f"{name}:{requirement}:{allow_blank}")
        lines.append(f"{renderer}|{','.join(parameters)}|{','.join(texts)}")
    return set(schemas), "\n".join(lines) + "\n"


def load_kotlin_keys() -> set[str]:
    keys: set[str] = set()
    for path in VISUALIZATION_SOURCE.rglob("*.kt"):
        keys.update(KEY_PATTERN.findall(path.read_text(encoding="utf-8")))
    return keys


def main() -> int:
    python_keys, expected_snapshot = load_python_contract()
    kotlin_keys = load_kotlin_keys()
    if kotlin_keys != python_keys:
        only_kotlin = sorted(kotlin_keys - python_keys)
        only_python = sorted(python_keys - kotlin_keys)
        details = []
        if only_kotlin:
            details.append(f"missing from Python contract: {only_kotlin}")
        if only_python:
            details.append(f"missing from Kotlin registry/source: {only_python}")
        raise SystemExit("visualization renderer contract is out of sync:\n" + "\n".join(details))

    if not SNAPSHOT.is_file():
        raise SystemExit(f"missing visualization contract snapshot: {SNAPSHOT.relative_to(ROOT)}")
    actual_snapshot = SNAPSHOT.read_text(encoding="utf-8")
    if actual_snapshot != expected_snapshot:
        raise SystemExit(
            "visualization Python schema and checked-in runtime snapshot differ; "
            "update both contracts together and regenerate visualization-contract.snapshot"
        )

    print(f"visualization contract sync passed: {len(kotlin_keys)} renderer keys and exact structural snapshot")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
