from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class RendererSchema:
    required_parameters: frozenset[str] = frozenset()
    optional_parameters: frozenset[str] = frozenset()
    number_list_parameters: frozenset[str] = frozenset()
    boolean_parameters: frozenset[str] = frozenset()
    required_texts: frozenset[str] = frozenset()
    optional_texts: frozenset[str] = frozenset()

    @property
    def parameter_names(self) -> frozenset[str]:
        return self.required_parameters | self.optional_parameters

    @property
    def text_names(self) -> frozenset[str]:
        return self.required_texts | self.optional_texts


NUMBER_LINE_BASE_PARAMETERS = frozenset({"min", "max", "step"})
NUMBER_LINE_BASE_TEXTS = frozenset({"title", "note"})
GEOMETRY_TEXTS = frozenset({"title", "labelA", "labelB", "labelC", "labelD", "note"})
CARTESIAN_RANGES = frozenset({"xMin", "xMax", "yMin", "yMax"})
CARTESIAN_TEXTS = frozenset({"title", "pointLabel", "note"})

RENDERER_SCHEMAS: dict[str, RendererSchema] = {
    "mathematics.context.opposite-quantities": RendererSchema(
        required_parameters=frozenset({"positive", "negative"}),
        required_texts=frozenset({"positiveLabel", "negativeLabel"}),
        optional_texts=frozenset({"title", "baselineLabel", "note"}),
    ),
    "mathematics.classification.rational": RendererSchema(
        required_texts=frozenset({
            "rowPositive", "rowZero", "rowNegative", "columnInteger", "columnFraction",
            "positiveInteger", "positiveFraction", "zero", "negativeInteger", "negativeFraction",
        }),
        optional_texts=frozenset({"title", "note"}),
    ),
    "mathematics.process.integer-to-fraction": RendererSchema(
        required_texts=frozenset({"source0", "target0", "source1", "target1", "source2", "target2"}),
        optional_texts=frozenset({"title", "note"}),
    ),
    "mathematics.process.expression": RendererSchema(
        required_texts=frozenset({"source", "target"}),
        optional_texts=frozenset({"title", "middle", "firstTransition", "secondTransition", "note"}),
    ),
    "mathematics.rule.sign": RendererSchema(
        required_texts=frozenset({"rule0", "rule1", "rule2", "rule3"}),
        optional_texts=frozenset({"title", "note"}),
    ),
    "mathematics.process.power": RendererSchema(
        required_parameters=frozenset({"base", "exponent"}),
        optional_parameters=frozenset({"minBase", "maxBase"}),
        optional_texts=frozenset({"title", "note"}),
    ),
    "mathematics.balance.equation": RendererSchema(
        optional_parameters=frozenset({"tilt"}),
        required_texts=frozenset({"left", "right"}),
        optional_texts=frozenset({"title", "note"}),
    ),
    "mathematics.number-line.basic": RendererSchema(
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"value"}),
        optional_texts=NUMBER_LINE_BASE_TEXTS,
    ),
    "mathematics.number-line.construction": RendererSchema(
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS,
        optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"originLabel", "positiveDirectionLabel", "negativeDirectionLabel", "unitLabel"}),
    ),
    "mathematics.number-line.points": RendererSchema(
        required_parameters=frozenset({"values"}),
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS,
        number_list_parameters=frozenset({"values"}),
        optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({f"label{index}" for index in range(8)}),
    ),
    "mathematics.number-line.opposite": RendererSchema(
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"value"}),
        optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"leftLabel", "rightLabel"}),
    ),
    "mathematics.number-line.absolute-value": RendererSchema(
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"value"}),
        optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"valueLabel", "absoluteLabel"}),
    ),
    "mathematics.number-line.comparison": RendererSchema(
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"left", "right"}),
        optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"leftLabel", "rightLabel"}),
    ),
    "mathematics.number-line.movement": RendererSchema(
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"start", "delta"}),
        optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"startLabel", "endLabel", "movementLabel"}),
    ),
    "mathematics.number-line.root": RendererSchema(
        required_parameters=frozenset({"value"}),
        optional_parameters=NUMBER_LINE_BASE_PARAMETERS,
        required_texts=frozenset({"pointLabel"}),
        optional_texts=NUMBER_LINE_BASE_TEXTS,
    ),
    "mathematics.cartesian.point": RendererSchema(
        required_parameters=frozenset({"x", "y"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS,
    ),
    "mathematics.cartesian.linear": RendererSchema(
        required_parameters=frozenset({"slope", "intercept"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS,
    ),
    "mathematics.cartesian.quadratic": RendererSchema(
        required_parameters=frozenset({"a", "b", "c"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS,
    ),
    "mathematics.cartesian.inverse": RendererSchema(
        required_parameters=frozenset({"k"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS,
    ),
    "mathematics.geometry.triangle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.circle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.angle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.parallel": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.right-triangle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.line-ray-segment": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.projection": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.object-abstraction": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.translation": RendererSchema(
        optional_parameters=frozenset({"dx", "dy"}), optional_texts=frozenset({"title", "originalLabel", "resultLabel", "note"}),
    ),
    "mathematics.geometry.symmetry": RendererSchema(
        optional_parameters=frozenset({"dx", "dy"}), optional_texts=frozenset({"title", "originalLabel", "resultLabel", "note"}),
    ),
    "mathematics.geometry.rotation": RendererSchema(
        optional_parameters=frozenset({"dx", "dy"}), optional_texts=frozenset({"title", "originalLabel", "resultLabel", "note"}),
    ),
    "mathematics.chart.line": RendererSchema(
        required_parameters=frozenset({"values"}), number_list_parameters=frozenset({"values"}),
        optional_texts=frozenset({"title", "note"}) | frozenset({f"label{index}" for index in range(8)}),
    ),
    "mathematics.chart.bar": RendererSchema(
        required_parameters=frozenset({"values"}), number_list_parameters=frozenset({"values"}),
        optional_texts=frozenset({"title", "note"}) | frozenset({f"label{index}" for index in range(8)}),
    ),
    "mathematics.probability.tree": RendererSchema(
        optional_texts=frozenset({"title", "note"}) | frozenset({f"label{index}" for index in range(7)}),
    ),
}


def is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def validate_visualization(renderer: Any, parameters: Any, texts: Any, where: str) -> None:
    if not isinstance(renderer, str) or not renderer.strip():
        raise ValueError(f"{where}.renderer: non-empty string required")
    schema = RENDERER_SCHEMAS.get(renderer)
    if schema is None:
        raise ValueError(f"{where}.renderer: unregistered renderer {renderer!r}")
    if not isinstance(parameters, dict):
        raise ValueError(f"{where}.parameters: object required")
    if not isinstance(texts, dict):
        raise ValueError(f"{where}.texts: object required")

    parameter_names = set(parameters)
    unknown_parameters = parameter_names - schema.parameter_names
    missing_parameters = schema.required_parameters - parameter_names
    if unknown_parameters:
        raise ValueError(f"{where}.parameters: unsupported fields {sorted(unknown_parameters)}")
    if missing_parameters:
        raise ValueError(f"{where}.parameters: missing required fields {sorted(missing_parameters)}")

    for name, value in parameters.items():
        if name in schema.number_list_parameters:
            if not isinstance(value, list) or not all(is_number(item) for item in value):
                raise ValueError(f"{where}.parameters.{name}: number[] required")
        elif name in schema.boolean_parameters:
            if not isinstance(value, bool):
                raise ValueError(f"{where}.parameters.{name}: boolean required")
        elif not is_number(value):
            raise ValueError(f"{where}.parameters.{name}: number required")

    text_names = set(texts)
    unknown_texts = text_names - schema.text_names
    missing_texts = schema.required_texts - text_names
    if unknown_texts:
        raise ValueError(f"{where}.texts: unsupported fields {sorted(unknown_texts)}")
    if missing_texts:
        raise ValueError(f"{where}.texts: missing required fields {sorted(missing_texts)}")
    for name, value in texts.items():
        if not isinstance(value, str):
            raise ValueError(f"{where}.texts.{name}: string required")
        if name in schema.required_texts and not value.strip():
            raise ValueError(f"{where}.texts.{name}: non-empty string required")
