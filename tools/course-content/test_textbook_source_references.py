#!/usr/bin/env python3

import json
from pathlib import Path
import tempfile
import unittest

import normalize_course_contract
import postprocess_math_courses


class TextbookSourceReferenceTest(unittest.TestCase):
    def test_postprocess_attaches_reviewed_figure_page(self):
        course = {
            "textbook": {"id": "pep-math-7-1"},
            "chapters": [
                {
                    "sections": [
                        {
                            "pages": [
                                {
                                    "id": "page-1",
                                    "blocks": [
                                        {"type": "textbook_text", "text": "教材图１．２－７表示点到原点的距离。"}
                                    ],
                                }
                            ]
                        }
                    ]
                }
            ],
        }

        attached = postprocess_math_courses.attach_source_references(course)

        self.assertEqual(1, attached)
        self.assertEqual(
            [{"label": "图1.2-7", "sourcePage": 13}],
            course["chapters"][0]["sections"][0]["pages"][0]["sourceReferences"],
        )

    def test_manual_section_with_chapter_id_does_not_cross_chapters(self):
        course = self._course_with_duplicate_section_ids()
        self._apply_temp_manual_section(
            course,
            {
                "chapterId": "chapter-01",
                "id": "section-01",
                "title": "第一章引言",
                "aliases": [],
                "pages": [self._page("manual-ch1", 1)],
            },
        )
        self.assertEqual("manual-ch1", course["chapters"][0]["sections"][0]["pages"][0]["id"])
        self.assertEqual("generated-ch6", course["chapters"][1]["sections"][0]["pages"][0]["id"])

    def test_ambiguous_manual_section_is_resolved_by_reviewed_page_range(self):
        course = self._course_with_duplicate_section_ids()
        self._apply_temp_manual_section(
            course,
            {
                "id": "section-01",
                "title": "第一章引言",
                "aliases": [],
                "pages": [self._page("manual-by-page", 1)],
            },
        )
        self.assertEqual("manual-by-page", course["chapters"][0]["sections"][0]["pages"][0]["id"])
        self.assertEqual("generated-ch6", course["chapters"][1]["sections"][0]["pages"][0]["id"])

    def test_normalizer_preserves_source_references(self):
        payload = {
            "textbook": {
                "id": "pep-math-7-1",
                "title": "七年级上册",
                "publisher": "人民教育出版社",
                "edition": "人教版",
                "grade": "七年级",
                "semester": "上册",
                "subject": "数学",
                "pdf": {"path": "assets/textbook.pdf", "pageCount": 202, "pageIndexOffset": 7},
            },
            "chapters": [
                {
                    "id": "chapter-01",
                    "number": "第一章",
                    "title": "有理数",
                    "aliases": [],
                    "sections": [
                        {
                            "id": "1.2.4",
                            "number": "1.2.4",
                            "title": "绝对值",
                            "aliases": [],
                            "pages": [
                                {
                                    "id": "absolute-value-definition",
                                    "title": "绝对值的定义",
                                    "sourcePage": 13,
                                    "sourceReferences": [{"label": "图1.2-7", "sourcePage": 13}],
                                    "blocks": [{"type": "textbook_text", "text": "数轴上表示数a的点与原点的距离叫作绝对值。"}],
                                }
                            ],
                        }
                    ],
                }
            ],
        }

        normalized = normalize_course_contract.normalize_course(payload)
        page = normalized["chapters"][0]["sections"][0]["pages"][0]
        self.assertEqual([{"label": "图1.2-7", "sourcePage": 13}], page["sourceReferences"])

    def test_normalizer_rejects_reference_outside_pdf(self):
        with self.assertRaises(ValueError):
            normalize_course_contract.normalize_source_references(
                [{"label": "图1.2-7", "sourcePage": 203}],
                202,
                "page.sourceReferences",
            )

    def _apply_temp_manual_section(self, course, override):
        old_root = postprocess_math_courses.MANUAL_ROOT
        try:
            with tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                directory = root / "pep-math-7-1"
                directory.mkdir(parents=True)
                (directory / "section-01.json").write_text(json.dumps(override, ensure_ascii=False), encoding="utf-8")
                postprocess_math_courses.MANUAL_ROOT = root
                self.assertEqual(1, postprocess_math_courses.apply_manual_sections(course))
        finally:
            postprocess_math_courses.MANUAL_ROOT = old_root

    def _course_with_duplicate_section_ids(self):
        return {
            "textbook": {"id": "pep-math-7-1"},
            "chapters": [
                {
                    "id": "chapter-01",
                    "sections": [
                        {
                            "id": "section-01",
                            "pages": [self._page("generated-ch1", 1)],
                        }
                    ],
                },
                {
                    "id": "chapter-06",
                    "sections": [
                        {
                            "id": "section-01",
                            "pages": [self._page("generated-ch6", 149)],
                        }
                    ],
                },
            ],
        }

    def _page(self, page_id, source_page):
        return {
            "id": page_id,
            "title": page_id,
            "sourcePage": source_page,
            "sourceAnchors": [{"page": source_page, "text": "足够长度的教材锚点文本"}],
            "blocks": [{"type": "textbook_text", "text": "正文"}],
        }


if __name__ == "__main__":
    unittest.main()
