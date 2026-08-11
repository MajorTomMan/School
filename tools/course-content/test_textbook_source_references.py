#!/usr/bin/env python3

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


if __name__ == "__main__":
    unittest.main()
