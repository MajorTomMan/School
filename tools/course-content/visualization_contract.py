from __future__ import annotations

import ast
from dataclasses import dataclass
import math
import struct
from typing import Any


@dataclass(frozen=True)
class RendererSchema:
    required_parameters: frozenset[str] = frozenset()
    optional_parameters: frozenset[str] = frozenset()
    number_list_parameters: frozenset[str] = frozenset()
    boolean_parameters: frozenset[str] = frozenset()
    math_expression_parameters: frozenset[str] = frozenset()
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
FLOAT32_MAX = 3.4028234663852886e38

RENDERER_SCHEMAS: dict[str, RendererSchema] = {
    "mathematics.context.opposite-quantities": RendererSchema(required_parameters=frozenset({"positive", "negative"}), required_texts=frozenset({"positiveLabel", "negativeLabel"}), optional_texts=frozenset({"title", "baselineLabel", "note"})),
    "mathematics.classification.rational": RendererSchema(required_texts=frozenset({"rowPositive", "rowZero", "rowNegative", "columnInteger", "columnFraction", "positiveInteger", "positiveFraction", "zero", "negativeInteger", "negativeFraction"}), optional_texts=frozenset({"title", "note"})),
    "mathematics.process.integer-to-fraction": RendererSchema(required_texts=frozenset({"source0", "target0", "source1", "target1", "source2", "target2"}), optional_texts=frozenset({"title", "note"})),
    "mathematics.process.expression": RendererSchema(required_texts=frozenset({"source", "target"}), optional_texts=frozenset({"title", "middle", "firstTransition", "secondTransition", "note"})),
    "mathematics.rule.sign": RendererSchema(required_texts=frozenset({"rule0", "rule1", "rule2", "rule3"}), optional_texts=frozenset({"title", "note"})),
    "mathematics.process.power": RendererSchema(required_parameters=frozenset({"base", "exponent"}), optional_parameters=frozenset({"minBase", "maxBase"}), optional_texts=frozenset({"title", "note"})),
    "mathematics.balance.equation": RendererSchema(optional_parameters=frozenset({"tilt"}), required_texts=frozenset({"left", "right"}), optional_texts=frozenset({"title", "note"})),
    "mathematics.number-line.basic": RendererSchema(optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"value"}), optional_texts=NUMBER_LINE_BASE_TEXTS),
    "mathematics.number-line.construction": RendererSchema(optional_parameters=NUMBER_LINE_BASE_PARAMETERS, optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"originLabel", "positiveDirectionLabel", "negativeDirectionLabel", "unitLabel"})),
    "mathematics.number-line.points": RendererSchema(required_parameters=frozenset({"values"}), optional_parameters=NUMBER_LINE_BASE_PARAMETERS, number_list_parameters=frozenset({"values"}), optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({f"label{index}" for index in range(8)})),
    "mathematics.number-line.opposite": RendererSchema(optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"value"}), optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"leftLabel", "rightLabel"})),
    "mathematics.number-line.absolute-value": RendererSchema(optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"value"}), optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"valueLabel", "absoluteLabel"})),
    "mathematics.number-line.comparison": RendererSchema(optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"left", "right"}), optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"leftLabel", "rightLabel"})),
    "mathematics.number-line.movement": RendererSchema(optional_parameters=NUMBER_LINE_BASE_PARAMETERS | frozenset({"start", "delta"}), optional_texts=NUMBER_LINE_BASE_TEXTS | frozenset({"startLabel", "endLabel", "movementLabel"})),
    "mathematics.number-line.root": RendererSchema(required_parameters=frozenset({"value"}), optional_parameters=NUMBER_LINE_BASE_PARAMETERS, required_texts=frozenset({"pointLabel"}), optional_texts=NUMBER_LINE_BASE_TEXTS),
    "mathematics.cartesian.point": RendererSchema(required_parameters=frozenset({"x", "y"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS),
    "mathematics.cartesian.linear": RendererSchema(required_parameters=frozenset({"slope", "intercept"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS),
    "mathematics.cartesian.quadratic": RendererSchema(required_parameters=frozenset({"a", "b", "c"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS),
    "mathematics.cartesian.inverse": RendererSchema(required_parameters=frozenset({"k"}), optional_parameters=CARTESIAN_RANGES, optional_texts=CARTESIAN_TEXTS),
    "mathematics.function.graph": RendererSchema(required_parameters=frozenset({"expression"}), optional_parameters=CARTESIAN_RANGES, math_expression_parameters=frozenset({"expression"}), optional_texts=CARTESIAN_TEXTS),
    "mathematics.geometry.triangle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.circle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.angle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.parallel": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.right-triangle": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.line-ray-segment": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.projection": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.object-abstraction": RendererSchema(optional_texts=GEOMETRY_TEXTS),
    "mathematics.geometry.translation": RendererSchema(optional_parameters=frozenset({"dx", "dy"}), optional_texts=frozenset({"title", "originalLabel", "resultLabel", "note"})),
    "mathematics.geometry.symmetry": RendererSchema(optional_parameters=frozenset({"dx", "dy"}), optional_texts=frozenset({"title", "originalLabel", "resultLabel", "note"})),
    "mathematics.geometry.rotation": RendererSchema(optional_parameters=frozenset({"dx", "dy"}), optional_texts=frozenset({"title", "originalLabel", "resultLabel", "note"})),
    "mathematics.chart.line": RendererSchema(required_parameters=frozenset({"values"}), number_list_parameters=frozenset({"values"}), optional_texts=frozenset({"title", "note"}) | frozenset({f"label{index}" for index in range(8)})),
    "mathematics.chart.bar": RendererSchema(required_parameters=frozenset({"values"}), number_list_parameters=frozenset({"values"}), optional_texts=frozenset({"title", "note"}) | frozenset({f"label{index}" for index in range(8)})),
    "mathematics.probability.tree": RendererSchema(optional_texts=frozenset({"title", "note"}) | frozenset({f"label{index}" for index in range(7)})),
}


def float32(value: float) -> float:
    try:
        return struct.unpack("!f", struct.pack("!f", float(value)))[0]
    except OverflowError:
        return math.copysign(math.inf, value)


def is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(float(value)) and abs(float(value)) <= FLOAT32_MAX


def validate_math_expression(value: Any, where: str, allowed_variables: set[str] | None = None) -> set[str]:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{where}: non-empty safe math expression required")
    source = value.strip()
    if len(source) > 256:
        raise ValueError(f"{where}: math expression must not exceed 256 characters")
    translated = source.replace("^", "**")
    try:
        tree = ast.parse(translated, mode="eval")
    except SyntaxError as error:
        raise ValueError(f"{where}: invalid math expression") from error
    functions = {"abs", "sqrt", "sin", "cos", "tan", "ln", "log", "exp"}
    constants = {"pi", "e"}
    variables: set[str] = set()
    node_count = 0

    def visit(node: ast.AST, depth: int) -> None:
        nonlocal node_count
        node_count += 1
        if node_count > 160 or depth > 32:
            raise ValueError(f"{where}: math expression is too complex")
        if isinstance(node, ast.Expression):
            visit(node.body, depth + 1)
        elif isinstance(node, ast.BinOp):
            if not isinstance(node.op, (ast.Add, ast.Sub, ast.Mult, ast.Div, ast.Pow)):
                raise ValueError(f"{where}: unsupported math operator")
            visit(node.left, depth + 1)
            visit(node.right, depth + 1)
        elif isinstance(node, ast.UnaryOp):
            if not isinstance(node.op, (ast.UAdd, ast.USub)):
                raise ValueError(f"{where}: unsupported unary operator")
            visit(node.operand, depth + 1)
        elif isinstance(node, ast.Constant):
            if not is_number(node.value):
                raise ValueError(f"{where}: only finite numeric constants are allowed")
        elif isinstance(node, ast.Name):
            name = node.id.lower()
            if name not in constants:
                if not name.isidentifier() or len(name) > 16 or not name[0].isalpha() or not all(ch.islower() or ch.isdigit() or ch == "_" for ch in name):
                    raise ValueError(f"{where}: invalid variable {node.id!r}")
                variables.add(name)
        elif isinstance(node, ast.Call):
            if not isinstance(node.func, ast.Name) or node.func.id.lower() not in functions or len(node.args) != 1 or node.keywords:
                raise ValueError(f"{where}: only allowlisted one-argument math functions are allowed")
            visit(node.args[0], depth + 1)
        else:
            raise ValueError(f"{where}: code-like or unsupported expression syntax is forbidden")

    visit(tree, 0)
    if allowed_variables is not None and variables - allowed_variables:
        raise ValueError(f"{where}: unsupported variables {sorted(variables - allowed_variables)}")
    return variables


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
                raise ValueError(f"{where}.parameters.{name}: float-safe finite number[] required")
        elif name in schema.boolean_parameters:
            if not isinstance(value, bool):
                raise ValueError(f"{where}.parameters.{name}: boolean required")
        elif name in schema.math_expression_parameters:
            validate_math_expression(value, f"{where}.parameters.{name}")
        elif not is_number(value):
            raise ValueError(f"{where}.parameters.{name}: float-safe finite number required")
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
    validate_visualization_semantics(renderer, parameters, where)


def validate_visualization_semantics(renderer: str, parameters: dict[str, Any], where: str) -> None:
    if renderer.startswith("mathematics.number-line."):
        minimum = float(parameters.get("min", -8.0)); maximum = float(parameters.get("max", 8.0)); step = float(parameters.get("step", 1.0))
        minimum_f = float32(minimum); maximum_f = float32(maximum); step_f = float32(step); span_f = float32(maximum_f - minimum_f)
        if maximum <= minimum: raise ValueError(f"{where}.parameters: max must be greater than min")
        if step <= 0: raise ValueError(f"{where}.parameters.step: must be greater than 0")
        if 0 < step < 0.01: raise ValueError(f"{where}.parameters.step: must not be smaller than 0.01")
        if not maximum_f > minimum_f or not math.isfinite(span_f): raise ValueError(f"{where}.parameters: number-line range exceeds Float rendering precision")
        if step > 0 and not step_f > 0: raise ValueError(f"{where}.parameters.step: exceeds Float rendering precision")
        if (maximum - minimum) / step > 80.0 + 1e-9: raise ValueError(f"{where}.parameters: number-line tick count must not exceed 80")
        if step_f > 0 and span_f / step_f > 80.01: raise ValueError(f"{where}.parameters: number-line tick count exceeds 80 at Float rendering precision")
        def in_range(value: float) -> bool: return minimum <= value <= maximum
        if renderer == "mathematics.number-line.basic" and "value" in parameters and not in_range(float(parameters["value"])): raise ValueError(f"{where}.parameters.value: must be inside number-line range")
        if renderer == "mathematics.number-line.construction" and not in_range(0.0): raise ValueError(f"{where}.parameters: construction range must contain 0")
        if renderer == "mathematics.number-line.points":
            values = [float(item) for item in parameters["values"]]
            if not values: raise ValueError(f"{where}.parameters.values: must not be empty")
            if len(values) > 8: raise ValueError(f"{where}.parameters.values: at most 8 points are supported")
            if any(not in_range(value) for value in values): raise ValueError(f"{where}.parameters.values: all points must be inside number-line range")
        if renderer == "mathematics.number-line.opposite":
            value = abs(float(parameters.get("value", 3.0)))
            if not in_range(0.0): raise ValueError(f"{where}.parameters: opposite range must contain 0")
            if value > min(abs(minimum), abs(maximum)): raise ValueError(f"{where}.parameters.value: both opposite points must fit inside number-line range")
        if renderer == "mathematics.number-line.absolute-value":
            value = float(parameters.get("value", -3.0))
            if not in_range(value): raise ValueError(f"{where}.parameters.value: must be inside number-line range")
            if not in_range(0.0) or abs(value) > maximum: raise ValueError(f"{where}.parameters.value: value and absolute value must both fit inside number-line range")
        if renderer == "mathematics.number-line.comparison":
            for name in ("left", "right"):
                if name in parameters and not in_range(float(parameters[name])): raise ValueError(f"{where}.parameters.{name}: must be inside number-line range")
        if renderer == "mathematics.number-line.movement":
            start = float(parameters.get("start", -3.0)); delta = float(parameters.get("delta", 2.0))
            if not in_range(start): raise ValueError(f"{where}.parameters.start: must be inside number-line range")
            if not in_range(start + delta): raise ValueError(f"{where}.parameters: start + delta must be inside number-line range")
        if renderer == "mathematics.number-line.root" and not in_range(float(parameters["value"])): raise ValueError(f"{where}.parameters.value: must be inside number-line range")

    if renderer.startswith("mathematics.cartesian.") or renderer == "mathematics.function.graph":
        x_min = float(parameters.get("xMin", -5.0)); x_max = float(parameters.get("xMax", 5.0)); y_min = float(parameters.get("yMin", -4.0)); y_max = float(parameters.get("yMax", 4.0))
        if x_max <= x_min: raise ValueError(f"{where}.parameters: xMax must be greater than xMin")
        if y_max <= y_min: raise ValueError(f"{where}.parameters: yMax must be greater than yMin")
        if x_max - x_min > 100.0: raise ValueError(f"{where}.parameters: x range must not exceed 100 units")
        if y_max - y_min > 100.0: raise ValueError(f"{where}.parameters: y range must not exceed 100 units")
        if x_max > x_min and not float32(x_max) > float32(x_min): raise ValueError(f"{where}.parameters: x range exceeds Float rendering precision")
        if y_max > y_min and not float32(y_max) > float32(y_min): raise ValueError(f"{where}.parameters: y range exceeds Float rendering precision")
        if renderer == "mathematics.cartesian.point":
            x = float(parameters["x"]); y = float(parameters["y"])
            if not x_min <= x <= x_max: raise ValueError(f"{where}.parameters.x: must be inside x range")
            if not y_min <= y <= y_max: raise ValueError(f"{where}.parameters.y: must be inside y range")
        if renderer == "mathematics.function.graph":
            validate_math_expression(parameters["expression"], f"{where}.parameters.expression", {"x"})

    if renderer in {"mathematics.chart.line", "mathematics.chart.bar"}:
        values = parameters["values"]
        if not values: raise ValueError(f"{where}.parameters.values: must not be empty")
        if len(values) > 8: raise ValueError(f"{where}.parameters.values: at most 8 values are supported")
        float_values = [float32(float(value)) for value in values]; minimum_f = min(0.0, min(float_values)); maximum_f = max(0.0, max(float_values))
        if not math.isfinite(float32(maximum_f - minimum_f)): raise ValueError(f"{where}.parameters.values: chart span exceeds Float rendering precision")

    if renderer == "mathematics.process.power":
        exponent = float(parameters["exponent"]); minimum = float(parameters.get("minBase", -4.0)); maximum = float(parameters.get("maxBase", 4.0)); base = float(parameters["base"])
        exponent_valid = abs(exponent - round(exponent)) <= 1e-9 and 1.0 <= exponent <= 8.0
        if not exponent_valid: raise ValueError(f"{where}.parameters.exponent: integer from 1 to 8 required")
        if maximum <= minimum: raise ValueError(f"{where}.parameters: maxBase must be greater than minBase")
        if not minimum <= base <= maximum: raise ValueError(f"{where}.parameters.base: must be inside minBase..maxBase")
        if maximum > minimum:
            minimum_f = float32(minimum); maximum_f = float32(maximum)
            if not maximum_f > minimum_f or not math.isfinite(float32(maximum_f - minimum_f)): raise ValueError(f"{where}.parameters: power slider range exceeds Float rendering precision")
        exponent_int = int(round(exponent))
        if not math.isfinite(abs(minimum) ** exponent_int) or not math.isfinite(abs(maximum) ** exponent_int): raise ValueError(f"{where}.parameters: power slider endpoint produces non-finite result")

    if renderer == "mathematics.balance.equation" and "tilt" in parameters and not -1.0 <= float(parameters["tilt"]) <= 1.0:
        raise ValueError(f"{where}.parameters.tilt: must be inside -1..1")
