package com.majortomman.school.data.material

import org.junit.Assert.assertEquals
import org.junit.Test

class TextbookCatalogParserTest {
    @Test
    fun parsesCurrentFlatLessonCatalog() {
        val slot = TextbookSlot(
            subjectId = "math",
            subjectTitle = "数学",
            grade = 7,
            volume = TextbookVolume.FIRST,
        )
        val manifest = MaterialPackManifest(
            schemaVersion = MATERIAL_PACK_SCHEMA_VERSION,
            packId = "math-grade7-volume1",
            version = "1.0.0",
            title = "七年级数学上册",
            subject = "数学",
            catalogPath = "catalog.json",
            pdf = MaterialPdfAsset("books/textbook.pdf", "a".repeat(64), 0),
        )
        val catalog = TextbookCatalogParser.parse(
            json = """
                {
                  "schemaVersion":1,
                  "book":{"id":"math-grade7-volume1","title":"七年级数学上册","subject":"数学","grade":7,"volume":1},
                  "lessons":[
                    {"id":"number-line","title":"数轴","pageStart":15,"pageEnd":20,"orderIndex":0}
                  ]
                }
            """.trimIndent(),
            manifest = manifest,
            selectedSlot = slot,
        )

        val lesson = catalog.lessons.single()
        assertEquals("number-line", lesson.id)
        assertEquals(15, lesson.pageStart)
        assertEquals(20, lesson.pageEnd)
        assertEquals("math-grade7-volume1", catalog.book.id)
    }
}
