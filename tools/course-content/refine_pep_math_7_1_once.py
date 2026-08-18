#!/usr/bin/env python3

import json
from pathlib import Path

COURSE = Path("courses/pep-math-7-1/course.json")


def merge_unique(*sequences):
    result = []
    for sequence in sequences:
        for item in sequence:
            if item not in result:
                result.append(item)
    return result


def main():
    course = json.loads(COURSE.read_text(encoding="utf-8"))

    def section(chapter_title, section_title):
        for chapter in course["chapters"]:
            if chapter["title"] == chapter_title:
                for item in chapter["sections"]:
                    if item["title"] == section_title:
                        return item
        raise KeyError((chapter_title, section_title))

    def lesson(lesson_id):
        for chapter in course["chapters"]:
            for item in chapter["sections"]:
                for value in item["lessons"]:
                    if value["id"] == lesson_id:
                        return value
        raise KeyError(lesson_id)

    def set_reference(value, start, end):
        label = f"教材第{start}页" if start == end else f"教材第{start}—{end}页"
        value["references"] = [{"label": label, "pageStart": start, "pageEnd": end}]

    for lesson_id, start, end in [
        ("ch01-rational-concept", 7, 7),
        ("ch01-number-line", 8, 10),
        ("ch01-opposite-number", 11, 12),
        ("ch01-absolute-value", 13, 13),
        ("ch01-rational-comparison", 14, 17),
        ("ch02-addition", 25, 27),
        ("ch02-addition-laws", 28, 29),
        ("ch02-subtraction", 30, 31),
        ("ch02-add-sub-mixed", 32, 36),
        ("ch02-multiplication", 38, 40),
        ("ch02-multiplication-laws", 41, 42),
        ("ch02-division", 43, 49),
        ("ch02-power", 51, 52),
        ("ch02-operation-order", 53, 54),
        ("ch02-scientific-notation", 54, 54),
        ("ch02-approximation", 55, 57),
        ("ch03-algebraic-expression", 69, 70),
        ("ch03-writing", 69, 70),
        ("ch03-expression-meaning", 70, 72),
        ("ch03-direct-proportion", 73, 73),
        ("ch03-inverse-proportion", 73, 77),
        ("ch04-monomial", 89, 90),
        ("ch04-polynomial", 91, 94),
        ("ch04-like-terms", 95, 96),
        ("ch04-combine-like-terms", 96, 98),
        ("ch04-remove-parentheses", 98, 100),
        ("ch04-polynomial-add-sub", 101, 103),
    ]:
        set_reference(lesson(lesson_id), start, end)

    sec = section("第五章 一元一次方程", "5.1 方程")
    equation = next(item for item in sec["lessons"] if item["id"] == "ch05-equation")
    linear = next(item for item in sec["lessons"] if item["id"] == "ch05-linear-equation")
    equation["title"] = "5.1.1 从算式到方程"
    equation["aliases"] = merge_unique(equation.get("aliases", []), ["从算式到方程"], linear.get("aliases", []))
    equation["goals"] = merge_unique(equation.get("goals", []), linear.get("goals", []))
    equation["knowledgePointIds"] = merge_unique(equation.get("knowledgePointIds", []), linear.get("knowledgePointIds", []))
    equation["steps"] = equation.get("steps", []) + linear.get("steps", [])
    equation["practice"] = equation.get("practice", []) + linear.get("practice", [])
    equation["summary"] = merge_unique(equation.get("summary", []), linear.get("summary", []))
    set_reference(equation, 111, 114)
    sec["lessons"] = [item for item in sec["lessons"] if item["id"] != "ch05-linear-equation"]

    equality = lesson("ch05-equality-properties")
    equality["title"] = "5.1.2 等式的性质"
    set_reference(equality, 115, 119)

    for chapter in course["chapters"]:
        for item in chapter["sections"]:
            for value in item["lessons"]:
                value["prerequisiteLessonIds"] = [
                    "ch05-equation" if prerequisite == "ch05-linear-equation" else prerequisite
                    for prerequisite in value.get("prerequisiteLessonIds", [])
                ]

    sec = section("第六章 几何图形初步", "6.1 几何图形")
    solid = next(item for item in sec["lessons"] if item["id"] == "ch06-solid-plane")
    views = next(item for item in sec["lessons"] if item["id"] == "ch06-views-unfolding")
    solid["title"] = "6.1.1 立体图形与平面图形"
    solid["aliases"] = merge_unique(solid.get("aliases", []), views.get("aliases", []))
    solid["goals"] = merge_unique(solid.get("goals", []), views.get("goals", []))
    solid["knowledgePointIds"] = merge_unique(solid.get("knowledgePointIds", []), views.get("knowledgePointIds", []))
    solid["steps"] = solid.get("steps", []) + views.get("steps", [])
    solid["practice"] = solid.get("practice", []) + views.get("practice", [])
    solid["summary"] = merge_unique(solid.get("summary", []), views.get("summary", []))
    set_reference(solid, 150, 154)
    sec["lessons"] = [item for item in sec["lessons"] if item["id"] != "ch06-views-unfolding"]

    point_line = lesson("ch06-point-line-surface-body")
    point_line["title"] = "6.1.2 点、线、面、体"
    set_reference(point_line, 155, 159)

    for chapter in course["chapters"]:
        for item in chapter["sections"]:
            for value in item["lessons"]:
                value["prerequisiteLessonIds"] = [
                    "ch06-solid-plane" if prerequisite == "ch06-views-unfolding" else prerequisite
                    for prerequisite in value.get("prerequisiteLessonIds", [])
                ]

    line_ray = lesson("ch06-line-ray-segment")
    line_ray["title"] = "6.2.1 直线、射线、线段"
    set_reference(line_ray, 162, 163)
    for step in line_ray.get("steps", []):
        if step.get("type") == "checkpoint" and "射线AB与射线BA" in step.get("prompt", ""):
            step["expectedAnswer"] = "不是（当A、B是不同点时，它们的端点不同，方向相反）"
            step["explanation"] = "射线的表示先写端点；交换字母会改变端点和延伸方向。"

    segment = lesson("ch06-segment-comparison")
    segment["title"] = "6.2.2 线段的比较与运算"
    set_reference(segment, 164, 167)
    for step in segment.get("steps", []):
        if step.get("type") == "keyIdea" and step.get("title") == "两点之间线段最短":
            step["text"] = "两点的所有连线中，线段最短。连接两点的线段的长度叫作这两点间的距离。"

    sec = section("第六章 几何图形初步", "6.3 角")
    angle = next(item for item in sec["lessons"] if item["id"] == "ch06-angle")
    measure = next(item for item in sec["lessons"] if item["id"] == "ch06-angle-measure")
    angle["title"] = "6.3.1 角的概念"
    angle["aliases"] = merge_unique(angle.get("aliases", []), measure.get("aliases", []))
    angle["goals"] = merge_unique(angle.get("goals", []), measure.get("goals", []))
    angle["knowledgePointIds"] = merge_unique(angle.get("knowledgePointIds", []), measure.get("knowledgePointIds", []))
    angle["steps"] = angle.get("steps", []) + measure.get("steps", [])
    angle["practice"] = angle.get("practice", []) + measure.get("practice", [])
    angle["summary"] = merge_unique(angle.get("summary", []), measure.get("summary", []))
    set_reference(angle, 170, 172)
    sec["lessons"] = [item for item in sec["lessons"] if item["id"] != "ch06-angle-measure"]

    for chapter in course["chapters"]:
        for item in chapter["sections"]:
            for value in item["lessons"]:
                value["prerequisiteLessonIds"] = [
                    "ch06-angle" if prerequisite == "ch06-angle-measure" else prerequisite
                    for prerequisite in value.get("prerequisiteLessonIds", [])
                ]

    operation = next(item for item in sec["lessons"] if item["id"] == "ch06-angle-operation")
    bisector = next(item for item in sec["lessons"] if item["id"] == "ch06-angle-bisector")
    operation["title"] = "6.3.2 角的比较与运算"
    operation["aliases"] = merge_unique(operation.get("aliases", []), bisector.get("aliases", []))
    operation["goals"] = merge_unique(operation.get("goals", []), bisector.get("goals", []))
    operation["knowledgePointIds"] = merge_unique(operation.get("knowledgePointIds", []), bisector.get("knowledgePointIds", []))
    operation["steps"] = operation.get("steps", []) + bisector.get("steps", [])
    operation["practice"] = operation.get("practice", []) + bisector.get("practice", [])
    operation["summary"] = merge_unique(operation.get("summary", []), bisector.get("summary", []))
    set_reference(operation, 173, 175)
    sec["lessons"] = [item for item in sec["lessons"] if item["id"] != "ch06-angle-bisector"]

    for chapter in course["chapters"]:
        for item in chapter["sections"]:
            for value in item["lessons"]:
                value["prerequisiteLessonIds"] = [
                    "ch06-angle-operation" if prerequisite == "ch06-angle-bisector" else prerequisite
                    for prerequisite in value.get("prerequisiteLessonIds", [])
                ]

    complementary = lesson("ch06-complementary-supplementary")
    complementary["title"] = "6.3.3 余角和补角"
    set_reference(complementary, 176, 179)

    COURSE.write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
