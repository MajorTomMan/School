#!/usr/bin/env python3

from __future__ import annotations

import unittest

from build_course_release import public_release_url
from normalize_course_contract import normalize_course


class CourseDistributionTest(unittest.TestCase):
    def test_legacy_course_becomes_strict_business_contract(self) -> None:
        legacy = {
            "schemaVersion": 1,
            "textbook": {
                "id": "pep-math-7-1",
                "title": "数学七年级上册",
                "publisher": "人民教育出版社",
                "edition": "人教版",
                "grade": "七年级",
                "semester": "上册",
                "subject": "数学",
                "pdf": {
                    "path": "assets/textbook.pdf",
                    "url": "https://example.invalid/textbook.pdf",
                    "size": 123,
                    "sha256": "a" * 64,
                    "pageCount": 202,
                    "pageIndexOffset": 7,
                },
            },
            "chapters": [
                {
                    "id": "chapter-01",
                    "number": "第一章",
                    "title": "有理数",
                    "aliases": [],
                    "sections": [
                        {
                            "id": "section-01",
                            "number": "1.1",
                            "title": "正数和负数",
                            "aliases": [],
                            "manualReview": {"printedPages": [2]},
                            "pages": [
                                {
                                    "id": "page-01",
                                    "type": "lesson",
                                    "title": "为什么需要负数",
                                    "sourcePage": 2,
                                    "sourceAnchors": [{"page": 2, "text": "负数"}],
                                    "blocks": [
                                        {"type": "textbook_text", "text": "教材正文"},
                                        {"type": "historical_note", "text": "历史说明"},
                                        {"type": "summary", "items": ["结论一"]},
                                        {
                                            "type": "visualization",
                                            "renderer": "number_line_lesson",
                                            "params": {"mode": "value", "signed": "true", "initial": "3.5"},
                                        },
                                    ],
                                }
                            ],
                        }
                    ],
                }
            ],
        }

        normalized = normalize_course(legacy)
        self.assertEqual({"textbook", "chapters"}, set(normalized))
        self.assertEqual(
            {"path", "pageCount", "pageIndexOffset"},
            set(normalized["textbook"]["pdf"]),
        )
        page = normalized["chapters"][0]["sections"][0]["pages"][0]
        self.assertNotIn("sourceAnchors", page)
        self.assertNotIn("type", page)
        self.assertEqual("text", page["blocks"][0]["type"])
        self.assertEqual("history", page["blocks"][1]["style"])
        self.assertEqual("list", page["blocks"][2]["type"])
        self.assertEqual("number_line", page["blocks"][3]["template"])
        self.assertIs(page["blocks"][3]["data"]["signed"], True)
        self.assertEqual(3.5, page["blocks"][3]["data"]["initial"])

    def test_duplicate_generic_ids_receive_stable_namespace(self) -> None:
        course = {
            "textbook": {
                "id": "book",
                "title": "Book",
                "publisher": "Publisher",
                "edition": "Edition",
                "grade": "七年级",
                "semester": "上册",
                "subject": "数学",
                "pdf": {"path": "assets/textbook.pdf", "pageCount": 20, "pageIndexOffset": 0},
            },
            "chapters": [],
        }
        for chapter_number in (1, 2):
            course["chapters"].append(
                {
                    "id": f"chapter-{chapter_number}",
                    "number": str(chapter_number),
                    "title": f"Chapter {chapter_number}",
                    "aliases": [],
                    "sections": [
                        {
                            "id": "section-01",
                            "number": "",
                            "title": "Section",
                            "aliases": [],
                            "pages": [
                                {
                                    "id": "page-01",
                                    "title": "Page",
                                    "sourcePage": chapter_number,
                                    "blocks": [{"type": "conclusion", "text": "Done"}],
                                }
                            ],
                        }
                    ],
                }
            )

        normalized = normalize_course(course)
        first = normalized["chapters"][0]["sections"][0]
        second = normalized["chapters"][1]["sections"][0]
        self.assertEqual("section-01", first["id"])
        self.assertEqual("chapter-2-section-01", second["id"])

    def test_public_release_url_is_immutable_release_path(self) -> None:
        self.assertEqual(
            "https://course.example/cloud/course/public/releases/r-1/book/course.zip",
            public_release_url(
                "https://course.example/cloud/course/public",
                "r-1",
                "book",
                "course.zip",
            ),
        )


if __name__ == "__main__":
    unittest.main()
