#!/usr/bin/env python3

import json
import re
import sys
from pathlib import Path

EXPECTED_CHAPTERS = [
    ("第一章 有理数", [
        "章引言",
        "1.1 正数和负数",
        "阅读与思考：用正负数表示允许偏差",
        "1.2 有理数及其大小比较",
        "图说数学史：漫漫长路识负数",
        "数学活动",
        "小结",
    ]),
    ("第二章 有理数的运算", [
        "章引言",
        "2.1 有理数的加法与减法",
        "阅读与思考：我国古代的正负数加减运算法则——正负术",
        "2.2 有理数的乘法与除法",
        "探究与发现：从数系扩充看有理数乘法法则",
        "2.3 有理数的乘方",
        "数学活动",
        "小结",
        "综合与实践：进位制的认识与探究",
    ]),
    ("第三章 代数式", [
        "章引言",
        "3.1 列代数式表示数量关系",
        "阅读与思考：数字1与字母X的对话",
        "3.2 代数式的值",
        "数学活动",
        "小结",
    ]),
    ("第四章 整式的加减", [
        "章引言",
        "4.1 整式",
        "4.2 整式的加法与减法",
        "信息技术应用：用电子表格进行数据计算",
        "数学活动",
        "小结",
    ]),
    ("第五章 一元一次方程", [
        "章引言",
        "5.1 方程",
        "5.2 解一元一次方程",
        "探究与发现：无限循环小数化分数",
        "5.3 实际问题与一元一次方程",
        "阅读与思考：初步认识数学模型",
        "数学活动",
        "小结",
    ]),
    ("第六章 几何图形初步", [
        "章引言",
        "6.1 几何图形",
        "图说数学史：几何的起源",
        "6.2 直线、射线、线段",
        "阅读与思考：长度的测量",
        "6.3 角",
        "阅读与思考：角的度量",
        "数学活动",
        "小结",
        "综合与实践：设计学校田径运动会比赛场地",
    ]),
]

EXPECTED_TEXTBOOK = {
    "id": "pep-math-7-1",
    "title": "义务教育教科书·数学七年级上册",
    "publisher": "人民教育出版社",
    "edition": "2024",
    "grade": "七年级",
    "semester": "上册",
    "subject": "数学",
}

EXPECTED_LESSON_LAYOUT = {
    "ch01-rational-concept": ("1.2.1 有理数的概念", 7, 7),
    "ch01-number-line": ("1.2.2 数轴", 8, 10),
    "ch01-opposite-number": ("1.2.3 相反数", 11, 12),
    "ch01-absolute-value": ("1.2.4 绝对值", 13, 13),
    "ch01-rational-comparison": ("1.2.5 有理数的大小比较", 14, 17),
    "ch02-addition": ("2.1.1 有理数的加法", 25, 27),
    "ch02-addition-laws": ("加法运算律", 28, 29),
    "ch02-subtraction": ("2.1.2 有理数的减法", 30, 31),
    "ch02-add-sub-mixed": ("有理数的加减混合运算", 32, 36),
    "ch02-multiplication": ("2.2.1 有理数的乘法", 38, 40),
    "ch02-multiplication-laws": ("乘法运算律", 41, 42),
    "ch02-division": ("2.2.2 有理数的除法", 43, 49),
    "ch02-power": ("2.3.1 乘方", 51, 52),
    "ch02-operation-order": ("有理数的混合运算", 53, 54),
    "ch02-scientific-notation": ("2.3.2 科学记数法", 54, 54),
    "ch02-approximation": ("2.3.3 近似数", 55, 57),
    "ch03-algebraic-expression": ("用代数式表示数量关系", 69, 70),
    "ch03-writing": ("代数式的书写", 69, 70),
    "ch03-expression-meaning": ("代数式表示的意义", 70, 72),
    "ch03-direct-proportion": ("正比例关系", 73, 73),
    "ch03-inverse-proportion": ("反比例关系", 73, 77),
    "ch04-monomial": ("单项式", 89, 90),
    "ch04-polynomial": ("多项式与整式", 91, 94),
    "ch04-like-terms": ("同类项", 95, 96),
    "ch04-combine-like-terms": ("合并同类项", 96, 98),
    "ch04-remove-parentheses": ("去括号", 98, 100),
    "ch04-polynomial-add-sub": ("整式的加减", 101, 103),
    "ch05-equation": ("5.1.1 从算式到方程", 111, 114),
    "ch05-equality-properties": ("5.1.2 等式的性质", 115, 119),
    "ch06-solid-plane": ("6.1.1 立体图形与平面图形", 150, 154),
    "ch06-point-line-surface-body": ("6.1.2 点、线、面、体", 155, 159),
    "ch06-line-ray-segment": ("6.2.1 直线、射线、线段", 162, 163),
    "ch06-segment-comparison": ("6.2.2 线段的比较与运算", 164, 167),
    "ch06-angle": ("6.3.1 角的概念", 170, 172),
    "ch06-angle-operation": ("6.3.2 角的比较与运算", 173, 175),
    "ch06-complementary-supplementary": ("6.3.3 余角和补角", 176, 179),
}

CRITICAL_PRACTICE_ANSWERS = {
    "ch02-p01": "-0.8",
    "ch02-p02": "10",
    "ch02-p03": "12",
    "ch02-p04": "-6",
    "ch02-p05": "2",
    "ch02-p06": "-40",
    "ch02-p07": "2/3",
    "ch02-p08": "1，-1",
    "ch02-p09": "3",
    "ch02-p10": "3×10^8",
    "ch02-p11": "1300",
    "ch02-p12": "17",
    "ch02-p13": "13",
    "ch03-p04": "8",
    "ch03-p05": "0.9a-5；a=100时为85元",
    "ch04-p04": "-3a+4b",
    "ch04-p06": "5a-6b",
    "ch04-p07": "-3x+6",
    "ch05-p02": "x=-5",
    "ch05-p03": "x=4",
    "ch05-p04": "x=3",
    "ch05-p05": "x=-11",
    "ch05-p06": "7/9",
    "ch05-p08": "3.5 h",
    "ch05-p09": "x=3",
    "ch06-p03": "8 cm",
    "ch06-p05": "45.3°",
    "ch06-p06": "64°15′",
    "ch06-p07": "45°",
    "ch06-p08": "AM=6 cm，∠AOC=45°",
    "ch06-p09": "40 m",
    "ch06-p11": "这个角62°，补角118°",
}

FORBIDDEN_EDITORIAL_FRAGMENTS = (
    "TODO",
    "FIXME",
    "待补",
    "待完善",
    "重新核对",
    "这里先",
    "临时",
)

GENERATED_TITLE_PATTERN = re.compile(r"^教材第\s*\d+\s*页(?:（\d+）|\(\d+\))?$")
NUMBERED_LESSON_PATTERN = re.compile(r"^\d+\.\d+\.\d+\s")


def fail(message: str) -> None:
    raise SystemExit(message)


def walk_strings(value, path="$", output=None):
    if output is None:
        output = []
    if isinstance(value, str):
        output.append((path, value))
    elif isinstance(value, list):
        for index, item in enumerate(value):
            walk_strings(item, f"{path}[{index}]", output)
    elif isinstance(value, dict):
        for key, item in value.items():
            walk_strings(item, f"{path}.{key}", output)
    return output


def validate(path: Path) -> None:
    course = json.loads(path.read_text(encoding="utf-8"))
    textbook = course["textbook"]

    for key, expected in EXPECTED_TEXTBOOK.items():
        if textbook.get(key) != expected:
            fail(f"{path}: textbook.{key} must be {expected!r}, got {textbook.get(key)!r}")

    pdf = textbook.get("pdf", {})
    expected_pdf = {"path": "assets/textbook.pdf", "pageCount": 202, "pageIndexOffset": 7}
    if pdf != expected_pdf:
        fail(f"{path}: textbook.pdf must be exactly {expected_pdf!r}, got {pdf!r}")

    chapters = course["chapters"]
    actual_chapter_titles = [chapter["title"] for chapter in chapters]
    expected_chapter_titles = [chapter_title for chapter_title, _ in EXPECTED_CHAPTERS]
    if actual_chapter_titles != expected_chapter_titles:
        fail(f"{path}: chapter order differs from the 2024 textbook: {actual_chapter_titles!r}")

    covered_pages = set()
    lessons_by_id = {}
    practices_by_id = {}

    for chapter, (expected_chapter_title, expected_sections) in zip(chapters, EXPECTED_CHAPTERS):
        if chapter["title"] != expected_chapter_title:
            fail(f"{path}: unexpected chapter title {chapter['title']!r}")

        sections = chapter["sections"]
        actual_sections = [section["title"] for section in sections]
        if actual_sections != expected_sections:
            fail(
                f"{path}: section order for {expected_chapter_title!r} differs from textbook. "
                f"expected={expected_sections!r}, actual={actual_sections!r}"
            )

        for section in sections:
            if not section["lessons"]:
                fail(f"{path}: section {section['title']!r} must contain at least one lesson")

            for lesson in section["lessons"]:
                lesson_id = lesson["id"]
                if lesson_id in lessons_by_id:
                    fail(f"{path}: duplicate lesson id {lesson_id!r}")
                lessons_by_id[lesson_id] = lesson

                if GENERATED_TITLE_PATTERN.fullmatch(lesson["title"].strip()):
                    fail(f"{path}: generated page title is retired: {lesson['title']!r}")

                if NUMBERED_LESSON_PATTERN.match(lesson["title"]) and lesson_id not in EXPECTED_LESSON_LAYOUT:
                    fail(f"{path}: unrecognized numbered textbook lesson {lesson_id!r}: {lesson['title']!r}")

                references = lesson["references"]
                if not references:
                    fail(f"{path}: {lesson_id} must reference its printed textbook page range")
                for reference in references:
                    start = reference["pageStart"]
                    end = reference["pageEnd"]
                    if start < 1 or end > 195 or start > end:
                        fail(
                            f"{path}: {lesson_id} uses invalid printed page range {start}-{end}; "
                            "this textbook's printed teaching pages are 1-195"
                        )
                    covered_pages.update(range(start, end + 1))

                for practice in lesson["practice"]:
                    practice_id = practice["id"]
                    if practice_id in practices_by_id:
                        fail(f"{path}: duplicate practice id {practice_id!r}")
                    practices_by_id[practice_id] = practice

    for lesson_id, (expected_title, start, end) in EXPECTED_LESSON_LAYOUT.items():
        lesson = lessons_by_id.get(lesson_id)
        if lesson is None:
            fail(f"{path}: missing textbook-aligned lesson {lesson_id!r}")
        if lesson["title"] != expected_title:
            fail(f"{path}: {lesson_id} title must be {expected_title!r}, got {lesson['title']!r}")
        actual_ranges = [(ref["pageStart"], ref["pageEnd"]) for ref in lesson["references"]]
        if actual_ranges != [(start, end)]:
            fail(f"{path}: {lesson_id} must use printed pages {start}-{end}, got {actual_ranges!r}")

    for practice_id, expected_answer in CRITICAL_PRACTICE_ANSWERS.items():
        practice = practices_by_id.get(practice_id)
        if practice is None:
            fail(f"{path}: missing critical checked practice {practice_id!r}")
        if practice["answer"] != expected_answer:
            fail(
                f"{path}: checked answer drift for {practice_id}: "
                f"expected {expected_answer!r}, got {practice['answer']!r}"
            )

    for lesson_id, lesson in lessons_by_id.items():
        for prerequisite in lesson.get("prerequisiteLessonIds", []):
            if prerequisite not in lessons_by_id:
                fail(f"{path}: {lesson_id} references missing prerequisite lesson {prerequisite!r}")

    missing_pages = sorted(set(range(1, 196)) - covered_pages)
    if missing_pages:
        fail(f"{path}: printed textbook pages not covered by lesson references: {missing_pages}")

    for json_path, text in walk_strings(course):
        for fragment in FORBIDDEN_EDITORIAL_FRAGMENTS:
            if fragment in text:
                fail(f"{path}: editorial residue {fragment!r} found at {json_path}: {text!r}")

    print(
        f"{path}: textbook editorial structure valid "
        f"({len(chapters)} chapters, {len(lessons_by_id)} lessons, "
        f"{len(practices_by_id)} practices, pages 1-195 covered)"
    )


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: validate_pep_math_7_1_editorial.py <course.json>")
    validate(Path(sys.argv[1]))


if __name__ == "__main__":
    main()
