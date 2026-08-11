package com.majortomman.school.learning.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CourseSourceReferenceContractTest {
    @Test
    fun sourceReferenceDecodesWithPrintedPage() {
        val document = CourseDocumentParser.decode(courseJson(referencePage = 13))
        val reference = document.chapters.single().sections.single().pages.single().sourceReferences.single()

        assertEquals("图1.2-7", reference.label)
        assertEquals(13, reference.sourcePage)
    }

    @Test
    fun sourceReferenceOutsidePdfIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CourseDocumentParser.decode(courseJson(referencePage = 203))
        }
    }

    private fun courseJson(referencePage: Int): String = """
        {
          "textbook": {
            "id": "pep-math-7-1",
            "title": "义务教育教科书·数学七年级上册",
            "publisher": "人民教育出版社",
            "edition": "人教版",
            "grade": "七年级",
            "semester": "上册",
            "subject": "数学",
            "pdf": {
              "path": "assets/textbook.pdf",
              "pageCount": 202,
              "pageIndexOffset": 7
            }
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
                      "aliases": [],
                      "sourcePage": 13,
                      "sourceReferences": [
                        {
                          "label": "图1.2-7",
                          "sourcePage": $referencePage
                        }
                      ],
                      "blocks": [
                        {
                          "type": "text",
                          "style": "textbook",
                          "text": "数轴上表示数a的点与原点的距离叫作绝对值。"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()
}
