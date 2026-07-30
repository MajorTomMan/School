import importlib.util
import sys
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("refine_page_titles.py")
SPEC = importlib.util.spec_from_file_location("refine_page_titles", MODULE_PATH)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


def page(title, blocks, page_id="p1", source_page=1):
    return {
        "id": page_id,
        "title": title,
        "sourcePage": source_page,
        "blocks": blocks,
    }


def course_with(*pages):
    return {
        "chapters": [
            {
                "sections": [
                    {
                        "title": "测试小节",
                        "pages": list(pages),
                    }
                ]
            }
        ]
    }


def test_example_has_priority_over_plain_text():
    value = page(
        "教材第25页（2）",
        [
            {"type": "text", "style": "textbook", "text": "先介绍有理数加法。"},
            {"type": "example", "statement": "计算（−3）+（−5）"},
        ],
    )
    report = MODULE.refine_titles(course_with(value))
    assert value["title"] == "例题：计算（−3）+（−5）"
    assert value["titleRefinement"]["status"] == "candidate"
    assert report.replaced == 1
    assert report.generic_after == 0


def test_formula_title_preserves_mathematical_expression():
    value = page(
        "教材第58页",
        [{"type": "formula", "expression": "(−2)^3=−8"}],
    )
    report = MODULE.refine_titles(course_with(value))
    assert value["title"] == "公式：(−2)^3=−8"
    assert report.replacements[0]["evidence"] == "formula"


def test_existing_manual_title_is_untouched():
    value = page(
        "负数乘方与括号",
        [{"type": "text", "style": "textbook", "text": "区分负号与底数。"}],
    )
    report = MODULE.refine_titles(course_with(value))
    assert value["title"] == "负数乘方与括号"
    assert report.generic_before == 0
    assert "titleRefinement" not in value


def test_ambiguous_page_remains_pending():
    value = page("教材第194页", [{"type": "scene", "template": "geometry", "data": {}}])
    report = MODULE.refine_titles(course_with(value))
    assert value["title"] == "教材第194页"
    assert report.ambiguous == 1
    assert report.pending[0]["sourcePage"] == 1


def test_long_body_is_shortened_without_destroying_prefix():
    value = page(
        "教材第132页（7）",
        [{"type": "exercise", "stem": "某工程队计划在若干天内完成一项非常复杂并且需要分阶段计算的工程任务"}],
    )
    MODULE.refine_titles(course_with(value))
    assert value["title"].startswith("练习：")
    assert value["title"].endswith("…")
    assert len(value["title"]) <= 24
