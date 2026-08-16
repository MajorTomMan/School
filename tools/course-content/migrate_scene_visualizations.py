#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Any


def visualization(renderer: str, parameters: dict[str, Any] | None = None, texts: dict[str, str] | None = None) -> dict[str, Any]:
    return {
        "type": "visualization",
        "renderer": renderer,
        "parameters": parameters or {},
        "texts": {key: value for key, value in (texts or {}).items() if isinstance(value, str)},
    }


def scene_text(data: dict[str, Any], key: str, default: str = "") -> str:
    value = data.get(key)
    return value.strip() if isinstance(value, str) and value.strip() else default


def scene_number(data: dict[str, Any], key: str, default: float) -> float:
    value = data.get(key)
    return float(value) if isinstance(value, (int, float)) and not isinstance(value, bool) else default


def formula_fallback(steps: list[dict[str, Any]]) -> str:
    for step in steps:
        if step.get("type") == "formula":
            value = step.get("expression")
            if isinstance(value, str) and value.strip():
                return value.strip()
    return "原式"


def migrate_opposite_quantities(data: dict[str, Any]) -> dict[str, Any]:
    scene = scene_text(data, "scene", "temperature")
    specs = {
        "temperature": (3.0, 3.0, "零上 3 ℃", "零下 3 ℃", "0 ℃ 是共同基准"),
        "account": (50.0, 10.0, "盈利 50 万元", "亏损 10 万元", "收支平衡记作 0"),
        "change": (7.8, 0.7, "增长 7.8%", "减少 0.7%", "变化率 0% 是共同基准"),
        "deviation": (30.0, 30.0, "高于标准", "低于标准", "标准值对应偏差 0"),
        "elevation": (60.0, 60.0, "高于海平面", "低于海平面", "海平面记作 0 m"),
        "tolerance": (0.03, 0.03, "正偏差", "负偏差", "标准尺寸对应偏差 0"),
    }
    positive, negative, positive_label, negative_label, baseline = specs.get(scene, specs["temperature"])
    return visualization(
        "mathematics.context.opposite-quantities",
        {"positive": positive, "negative": negative},
        {
            "title": scene_text(data, "title", "相反意义的量"),
            "positiveLabel": positive_label,
            "negativeLabel": negative_label,
            "baselineLabel": baseline,
            "note": "正负号表示相对同一基准的两个相反方向。",
        },
    )


def migrate_number_line(data: dict[str, Any]) -> dict[str, Any]:
    mode = scene_text(data, "mode", "value")
    title = scene_text(data, "title", "在同一条数轴上观察数的位置")
    note = scene_text(data, "note", "原点、正方向和单位长度共同确定一条数轴。")
    common = {"min": -8, "max": 8, "step": 1}
    if mode == "construction":
        return visualization(
            "mathematics.number-line.construction",
            common,
            {
                "title": title,
                "originLabel": "原点",
                "positiveDirectionLabel": "正方向",
                "negativeDirectionLabel": "负方向",
                "unitLabel": "1 个单位长度",
                "note": note,
            },
        )
    if mode == "road":
        return visualization(
            "mathematics.number-line.points",
            {**common, "values": [-4.8, -3, 0, 3, 7.5]},
            {
                "title": title,
                "label0": "电线杆 −4.8",
                "label1": "槐树 −3",
                "label2": "站牌 0",
                "label3": "柳树 +3",
                "label4": "标志杆 +7.5",
                "note": note,
            },
        )
    if mode == "example":
        return visualization(
            "mathematics.number-line.points",
            {**common, "values": [-4, -2.5, -1, 0, 0.5, 3, 4]},
            {"title": title, "note": note},
        )
    if mode == "read_points":
        return visualization(
            "mathematics.number-line.points",
            {**common, "values": [-3, -2, 0, 1, 2.5]},
            {"title": title, "label0": "E", "label1": "B", "label2": "A", "label3": "C", "label4": "D", "note": note},
        )
    if mode in {"opposite", "opposite_symbol"}:
        initial = abs(scene_number(data, "initial", 3.0))
        return visualization(
            "mathematics.number-line.opposite",
            {**common, "value": initial},
            {"title": title, "note": "相反数位于 0 的两侧，到 0 的距离相等。"},
        )
    return visualization(
        "mathematics.number-line.basic",
        {**common, "value": scene_number(data, "initial", 3.0)},
        {"title": title, "note": note},
    )


def migrate_scene(template: str, data: dict[str, Any], lesson_steps: list[dict[str, Any]]) -> dict[str, Any]:
    if template == "opposite_quantities":
        return migrate_opposite_quantities(data)
    if template == "rational_classification":
        return visualization(
            "mathematics.classification.rational",
            texts={
                "title": scene_text(data, "title", "有理数的双维分类"),
                "rowPositive": "正",
                "rowZero": "0",
                "rowNegative": "负",
                "columnInteger": "整数",
                "columnFraction": "分数",
                "positiveInteger": "正整数",
                "positiveFraction": "正分数",
                "zero": "0",
                "negativeInteger": "负整数",
                "negativeFraction": "负分数",
                "note": "同一个有理数可以从符号和表示形式两个维度分类。",
            },
        )
    if template == "integer_to_fraction":
        return visualization(
            "mathematics.process.integer-to-fraction",
            texts={
                "title": scene_text(data, "title", "整数写成分数形式"),
                "source0": "2", "target0": "2/1",
                "source1": "−3", "target1": "−3/1",
                "source2": "0", "target2": "0/1",
                "note": "给整数补上分母 1，数值不变。",
            },
        )
    if template == "number_line":
        return migrate_number_line(data)
    if template == "opposite_numbers":
        return visualization("mathematics.number-line.opposite", {"min": -8, "max": 8, "step": 0.5, "value": abs(scene_number(data, "initial", 3.0))}, {"title": scene_text(data, "title", "相反数"), "note": "两个相反数关于 0 对称。"})
    if template == "absolute_value":
        return visualization("mathematics.number-line.absolute-value", {"min": -8, "max": 8, "step": 0.5, "value": scene_number(data, "initial", -3.0)}, {"title": scene_text(data, "title", "绝对值表示到 0 的距离"), "note": "距离没有正负，因此绝对值不小于 0。"})
    if template == "number_comparison":
        return visualization("mathematics.number-line.comparison", {"min": -8, "max": 8, "step": 1, "left": scene_number(data, "left", -3.0), "right": scene_number(data, "right", 2.0)}, {"title": scene_text(data, "title", "用数轴比较大小"), "note": "数轴上越靠右的数越大。"})
    if template == "addition_process":
        return visualization("mathematics.number-line.movement", {"min": -10, "max": 10, "step": 1, "start": scene_number(data, "start", -3.0), "delta": scene_number(data, "delta", -2.0)}, {"title": scene_text(data, "title", "有理数加法对应数轴上的移动"), "movementLabel": "按加数的符号和大小移动", "note": "起点加上位移得到终点。"})
    if template in {"subtraction_transform", "division_transform"}:
        source = scene_text(data, "left", formula_fallback(lesson_steps))
        operation = "减法转化为加相反数" if template == "subtraction_transform" else "除法转化为乘倒数"
        target = scene_text(data, "right", operation)
        return visualization("mathematics.process.expression", texts={"title": scene_text(data, "title", operation), "source": source, "target": target, "firstTransition": operation, "note": "先完成等价转化，再按已有运算规则处理。"})
    if template == "multiplication_sign":
        return visualization("mathematics.rule.sign", texts={"title": scene_text(data, "title", "乘法符号法则"), "rule0": "＋ × ＋ = ＋", "rule1": "＋ × − = −", "rule2": "− × ＋ = −", "rule3": "− × − = ＋", "note": "同号得正，异号得负。"})
    if template == "power_process":
        return visualization("mathematics.process.power", {"base": scene_number(data, "base", -2.0), "exponent": scene_number(data, "exponent", 3.0), "minBase": -4, "maxBase": 4}, {"title": scene_text(data, "title", "乘方表示相同因数的连乘"), "note": "指数表示相同因数出现的次数。"})
    if template == "algebra_process":
        return visualization("mathematics.process.expression", texts={"title": scene_text(data, "title", "代数式的等价变形"), "source": scene_text(data, "left", formula_fallback(lesson_steps)), "target": scene_text(data, "right", "等价化简结果"), "firstTransition": "合并或变形", "note": scene_text(data, "note", "每一步都保持表达式的等价关系。")})
    if template == "equation_balance":
        return visualization("mathematics.balance.equation", {"tilt": scene_number(data, "tilt", 0.0)}, {"title": scene_text(data, "title", "等式两边保持平衡"), "left": scene_text(data, "left", "x + 3"), "right": scene_text(data, "right", "7"), "note": scene_text(data, "note", "等式两边进行相同的运算，等式仍成立。")})
    if template == "root_number_line":
        return visualization("mathematics.number-line.root", {"min": 0, "max": 4, "step": 1, "value": math.sqrt(2)}, {"title": scene_text(data, "title", "在数轴上定位 √2"), "pointLabel": "√2", "note": "无理数也对应数轴上的确定位置。"})
    if template == "cartesian_plane":
        return visualization("mathematics.cartesian.point", {"x": scene_number(data, "x", 2.0), "y": scene_number(data, "y", 2.0), "xMin": -5, "xMax": 5, "yMin": -4, "yMax": 4}, {"title": scene_text(data, "title", "平面直角坐标系"), "pointLabel": scene_text(data, "pointLabel", "P(2, 2)"), "note": scene_text(data, "note", "横坐标和纵坐标共同确定点的位置。")})
    if template == "function_graph":
        function = scene_text(data, "function", "linear").lower()
        ranges = {"xMin": -5, "xMax": 5, "yMin": -4, "yMax": 4}
        if "quadratic" in function:
            return visualization("mathematics.cartesian.quadratic", {**ranges, "a": 0.38, "b": 0, "c": -1.2}, {"title": scene_text(data, "title", "二次函数图像"), "note": scene_text(data, "note", "自变量变化时，函数值对应坐标系中的纵坐标。")})
        if "inverse" in function:
            return visualization("mathematics.cartesian.inverse", {**ranges, "k": 1.7}, {"title": scene_text(data, "title", "反比例函数图像"), "note": scene_text(data, "note", "图像反映自变量与函数值的对应关系。")})
        return visualization("mathematics.cartesian.linear", {**ranges, "slope": 0.72, "intercept": 0.5}, {"title": scene_text(data, "title", "一次函数图像"), "note": scene_text(data, "note", "图像反映自变量与函数值的对应关系。")})
    if template == "geometry":
        shape = (scene_text(data, "shape") or scene_text(data, "title")).lower()
        if "角" in shape:
            renderer, labels = "mathematics.geometry.angle", {"labelA": "O", "labelB": "A", "labelC": "B"}
        elif "circle" in shape or "圆" in shape:
            renderer, labels = "mathematics.geometry.circle", {"labelA": "O"}
        elif "parallel" in shape or "平行" in shape:
            renderer, labels = "mathematics.geometry.parallel", {}
        elif "物体" in shape or "几何对象" in shape or "抽象" in shape:
            renderer, labels = "mathematics.geometry.object-abstraction", {"labelA": "实物", "labelB": "几何图形", "labelC": "实物", "labelD": "几何图形"}
        else:
            renderer, labels = "mathematics.geometry.triangle", {"labelA": "A", "labelB": "B", "labelC": "C"}
        return visualization(renderer, texts={"title": scene_text(data, "title", "几何图形"), **labels, "note": scene_text(data, "note", "用基本图形和关系描述几何对象。")})
    if template == "transformation":
        mode = scene_text(data, "mode", "translation").lower()
        renderer = "mathematics.geometry.rotation" if "rotation" in mode else "mathematics.geometry.symmetry" if "symmetry" in mode else "mathematics.geometry.translation"
        parameters = {"dx": scene_number(data, "dx", 0.36), "dy": scene_number(data, "dy", -0.08)} if renderer.endswith("translation") else {}
        return visualization(renderer, parameters, {"title": scene_text(data, "title", "图形变换"), "originalLabel": "原图形", "resultLabel": "变换后", "note": scene_text(data, "note", "比较对应点的位置变化。")})
    if template == "right_triangle":
        return visualization("mathematics.geometry.right-triangle", texts={"title": scene_text(data, "title", "直角三角形"), "labelA": "A", "labelB": "B", "labelC": "C", "note": scene_text(data, "note", scene_text(data, "formula", "直角边与斜边之间存在确定关系。"))})
    if template == "data_chart":
        renderer = "mathematics.chart.line" if "line" in scene_text(data, "mode", "bar").lower() else "mathematics.chart.bar"
        return visualization(renderer, {"values": [0.38, 0.72, 0.52, 0.88, 0.64]}, {"title": scene_text(data, "title", "数据图表"), "label0": "1", "label1": "2", "label2": "3", "label3": "4", "label4": "5", "note": scene_text(data, "note", "图表把数据的大小和变化转成可比较的几何位置。")})
    if template == "probability":
        return visualization("mathematics.probability.tree", texts={"title": scene_text(data, "title", "概率树"), "note": scene_text(data, "note", "按阶段列出所有可能路径，避免遗漏。")})
    if template == "projection":
        return visualization("mathematics.geometry.projection", texts={"title": scene_text(data, "title", "从不同方向观察立体图形"), "labelA": "立体图形", "labelB": "正面", "labelC": "侧面", "labelD": "上面", "note": scene_text(data, "note", "不同观察方向对应不同平面图形。")})
    if template == "diagram":
        title = scene_text(data, "title", "几何示意图")
        if any(token in title for token in ("直线", "射线", "线段", "端点")):
            return visualization("mathematics.geometry.line-ray-segment", texts={"title": title, "labelA": "直线", "labelB": "射线", "labelC": "线段", "note": scene_text(data, "note", "用端点数量和延伸方向区分三种基本图形。")})
        return visualization("mathematics.geometry.triangle", texts={"title": title, "labelA": "A", "labelB": "B", "labelC": "C", "note": scene_text(data, "note", "示意图只保留与当前问题有关的几何关系。")})
    raise ValueError(f"unsupported legacy scene template: {template}")


def migrate(document: dict[str, Any]) -> tuple[dict[str, Any], int]:
    migrated = 0
    for chapter in document.get("chapters", []):
        for section in chapter.get("sections", []):
            for lesson in section.get("lessons", []):
                steps = lesson.get("steps")
                if not isinstance(steps, list):
                    continue
                original_steps = [step for step in steps if isinstance(step, dict)]
                replacement: list[dict[str, Any]] = []
                for step in original_steps:
                    if step.get("type") != "scene":
                        replacement.append(step)
                        continue
                    if set(step) != {"type", "template", "data"}:
                        raise ValueError(f"legacy scene has unexpected fields: {sorted(step)}")
                    template = step.get("template")
                    data = step.get("data")
                    if not isinstance(template, str) or not isinstance(data, dict):
                        raise ValueError("legacy scene must contain string template and object data")
                    replacement.append(migrate_scene(template, data, original_steps))
                    migrated += 1
                lesson["steps"] = replacement
    return document, migrated


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    document = json.loads(args.source.read_text(encoding="utf-8"))
    migrated_document, count = migrate(document)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(migrated_document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"migrated {count} legacy scene steps: {args.source} -> {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
