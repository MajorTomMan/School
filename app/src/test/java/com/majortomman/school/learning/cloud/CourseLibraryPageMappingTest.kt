package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CoursePdf
import com.majortomman.school.learning.course.CourseTextbook
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseLibraryPageMappingTest {
    @Test
    fun printedPagesMapToPdfIndicesUsingFirstPrintedPageOffset() {
        val course = InstalledCourse(
            rootPath = "",
            document = CourseDocument(
                textbook = CourseTextbook(
                    id = "pep-math-7-1",
                    title = "数学七年级上册",
                    publisher = "人民教育出版社",
                    edition = "2024",
                    grade = "七年级",
                    semester = "上册",
                    subject = "数学",
                    pdf = CoursePdf(path = "assets/textbook.pdf", pageCount = 202, pageIndexOffset = 7),
                ),
                knowledgePoints = emptyList(),
                chapters = emptyList(),
            ),
            contentVersion = 0L,
        )

        assertEquals(7, course.printedPageToPdfIndex(1))
        assertEquals(8, course.printedPageToPdfIndex(2))
        assertEquals(1, course.pdfIndexToPrintedPage(7))
        assertEquals(2, course.pdfIndexToPrintedPage(8))
        listOf(1, 2, 24, 202).forEach { page -> assertEquals(page, course.pdfIndexToPrintedPage(course.printedPageToPdfIndex(page))) }
    }
}
