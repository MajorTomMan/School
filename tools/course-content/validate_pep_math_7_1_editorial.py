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
    lesson_ids = set()
    practice_ids = set()

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
                if lesson_id in lesson_ids:
                    fail(f"{path}: duplicate lesson id {lesson_id!r}")
                lesson_ids.add(lesson_id)

                if GENERATED_TITLE_PATTERN.fullmatch(lesson["title"].strip()):
                    fail(f"{path}: generated page title is retired: {lesson['title']!r}")

                references = lesson["references"]
                if not references:
                    fail(f"{path}: {lesson_id} must reference its printed textbook page range")
                for reference in references:
                    start = reference["pageStart"]
                    end = reference["pageEnd"]
                    if start < 1 or end > 195:
                        fail(
                            f"{path}: {lesson_id} uses printed page range {start}-{end}; "
                            "this textbook's printed teaching pages are 1-195"
                        )
                    covered_pages.update(range(start, end + 1))

                for practice in lesson["practice"]:
                    practice_id = practice["id"]
                    if practice_id in practice_ids:
                        fail(f"{path}: duplicate practice id {practice_id!r}")
                    practice_ids.add(practice_id)

    expected_pages = set(range(1, 196))
    missing_pages = sorted(expected_pages - covered_pages)
    if missing_pages:
        fail(f"{path}: printed textbook pages not covered by lesson references: {missing_pages}")

    for json_path, text in walk_strings(course):
        for fragment in FORBIDDEN_EDITORIAL_FRAGMENTS:
            if fragment in text:
                fail(f"{path}: editorial residue {fragment!r} found at {json_path}: {text!r}")

    print(
        f"{path}: textbook editorial structure valid "
        f"({len(chapters)} chapters, {len(lesson_ids)} lessons, {len(practice_ids)} practices, pages 1-195 covered)"
    )


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: validate_pep_math_7_1_editorial.py <course.json>")
    validate(Path(sys.argv[1]))


if __name__ == "__main__":
    main()
