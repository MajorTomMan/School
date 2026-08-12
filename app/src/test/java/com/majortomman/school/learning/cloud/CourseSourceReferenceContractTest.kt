package com.majortomman.school.learning.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CourseSourceReferenceContractTest {
    @Test
    fun sourceReferenceDecodesWithPrintedPageRange() {
        val document = CourseDocumentParser.decode(courseJson(pageEnd = 14))
        val reference = document.chapters.single().sections.single().lessons.single().references.single()
        assertEquals("图1.2-7", reference.label)
        assertEquals(13, reference.pageStart)
        assertEquals(14, reference.pageEnd)
    }

    @Test
    fun sourceReferenceOutsidePdfIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(courseJson(pageEnd = 203)) }
    }

    private fun courseJson(pageEnd: Int): String = """
        {
          "textbook":{"id":"pep-math-7-1","title":"数学七年级上册","publisher":"人民教育出版社","edition":"2024","grade":"七年级","semester":"上册","subject":"数学","pdf":{"path":"assets/textbook.pdf","pageCount":202,"pageIndexOffset":7}},
          "knowledgePoints":[{"id":"absolute-value","name":"绝对值","description":"到原点的距离","prerequisiteIds":[]}],
          "chapters":[{"id":"chapter-01","title":"有理数","sections":[{"id":"section-absolute","title":"绝对值","lessons":[{
            "id":"absolute-value-intro","title":"绝对值是距离","aliases":["绝对值"],"goals":["理解绝对值表示距离"],"knowledgePointIds":["absolute-value"],"prerequisiteLessonIds":[],
            "references":[{"label":"图1.2-7","pageStart":13,"pageEnd":$pageEnd}],
            "steps":[{"type":"sourceLink","referenceIndex":0}],"practice":[],"summary":["绝对值是到原点的距离"]
          }]}]}]
        }
    """.trimIndent()
}
