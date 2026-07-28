package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.course.CourseChapter
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CoursePdf
import com.majortomman.school.learning.course.CourseSection
import com.majortomman.school.learning.course.CourseTextbook
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class Grade7SampleAssessmentContractTest {
    @Test
    fun manuallyReviewedSampleUsesExactApkContract() {
        val manual = repositoryFile("tools/course-content/manual/pep-math-7-1")
        val assessments = AssessmentDocumentParser.decode(
            File(manual, "assessments.json").readText(Charsets.UTF_8),
        )
        val knowledge = KnowledgePointDocumentParser.decode(
            File(manual, "knowledge-points.json").readText(Charsets.UTF_8),
        )
        val course = CourseDocument(
            textbook = CourseTextbook(
                id = "pep-math-7-1",
                title = "义务教育教科书·数学七年级上册",
                publisher = "人民教育出版社",
                edition = "人教版",
                grade = "七年级",
                semester = "上册",
                subject = "数学",
                pdf = CoursePdf("assets/textbook.pdf", 202, 7),
            ),
            chapters = listOf(
                CourseChapter(
                    id = "chapter-01",
                    number = "第一章",
                    title = "有理数",
                    aliases = emptyList(),
                    sections = listOf(
                        section("1.1"),
                        section("1.2.1"),
                        section("1.2.2"),
                        section("1.2.3"),
                    ),
                ),
            ),
        )

        AssessmentPackageContract.validate(course, assessments, knowledge)

        assertEquals(4, assessments.questionSets.size)
        assertEquals(20, assessments.questionSets.sumOf { it.questions.size })
        assertEquals(11, knowledge.knowledgePoints.size)
        assertEquals(
            setOf("integer", "decimal", "rational", "single_choice", "coordinate"),
            assessments.questionSets
                .flatMap { it.questions }
                .map { question ->
                    when (question.definition.inputSpec) {
                        com.majortomman.school.learning.assessment.domain.AnswerInputSpec.Integer -> "integer"
                        is com.majortomman.school.learning.assessment.domain.AnswerInputSpec.Decimal -> "decimal"
                        is com.majortomman.school.learning.assessment.domain.AnswerInputSpec.Rational -> "rational"
                        is com.majortomman.school.learning.assessment.domain.AnswerInputSpec.SingleChoice -> "single_choice"
                        com.majortomman.school.learning.assessment.domain.AnswerInputSpec.Coordinate -> "coordinate"
                    }
                }
                .toSet(),
        )
    }

    private fun section(id: String): CourseSection = CourseSection(
        id = id,
        number = id,
        title = id,
        aliases = emptyList(),
        pages = emptyList(),
    )

    private fun repositoryFile(relative: String): File {
        val candidates = listOf(
            File(relative),
            File("..", relative),
            File("../..", relative),
        )
        return candidates.firstOrNull(File::exists)
            ?: error("找不到仓库文件：$relative；工作目录=${File(".").canonicalPath}")
    }
}
