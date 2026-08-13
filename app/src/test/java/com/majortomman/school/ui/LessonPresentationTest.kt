package com.majortomman.school.ui

import com.majortomman.school.learning.course.CourseExample
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseFormula
import com.majortomman.school.learning.course.CourseKeyIdea
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonPresentationTest {
    @Test
    fun practiceAlwaysComesAfterTeachingAndSummary() {
        val lesson = lesson(
            steps = listOf(CourseQuestion("为什么需要负数？", null), CourseExplanation(null, "相反意义的量需要共同基准。")),
            practice = listOf(practice("p1"), practice("p2")),
            summary = listOf("正数和负数可以表示相反意义的量。"),
        )

        val pages = composeLessonPresentation(lesson)
        assertTrue(pages.first() is LessonPresentationPage.Overview)
        assertTrue(pages[pages.size - 3] is LessonPresentationPage.Summary)
        assertTrue(pages[pages.size - 2] is LessonPresentationPage.Practice)
        assertTrue(pages.last() is LessonPresentationPage.Practice)
        assertEquals(1, (pages[pages.size - 2] as LessonPresentationPage.Practice).number)
        assertEquals(2, (pages.last() as LessonPresentationPage.Practice).number)
    }

    @Test
    fun semanticStagesBreakWithoutDependingOnScreenHeight() {
        val lesson = lesson(
            steps = listOf(
                CourseQuestion("先想一想", "提示"),
                CourseExplanation(null, "解释"),
                CourseKeyIdea("关键理解", "结论"),
                CourseExample("例题", "题目", listOf("步骤"), "答案"),
                CourseFormula("a+b", "公式说明"),
            ),
        )

        val teaching = composeLessonPresentation(lesson).filterIsInstance<LessonPresentationPage.Teaching>()
        assertEquals(2, teaching.size)
        assertEquals(3, teaching[0].steps.size)
        assertEquals(2, teaching[1].steps.size)
        assertEquals("先想一想", teaching[0].label)
        assertEquals("例题", teaching[1].label)
    }

    @Test
    fun noTeachingPageContainsMoreThanFourSteps() {
        val steps = (1..9).map { CourseFormula("x+$it", null) }
        val teaching = composeLessonPresentation(lesson(steps = steps)).filterIsInstance<LessonPresentationPage.Teaching>()
        assertTrue(teaching.isNotEmpty())
        assertTrue(teaching.all { it.steps.size <= 4 })
    }

    private fun lesson(
        steps: List<com.majortomman.school.learning.course.CourseStep> = emptyList(),
        practice: List<CoursePractice> = emptyList(),
        summary: List<String> = emptyList(),
    ) = CourseLesson(
        id = "lesson-test",
        title = "测试课程",
        aliases = emptyList(),
        goals = listOf("理解测试目标"),
        knowledgePointIds = listOf("kp-test"),
        prerequisiteLessonIds = emptyList(),
        references = emptyList(),
        steps = steps,
        practice = practice,
        summary = summary,
    )

    private fun practice(id: String) = CoursePractice(
        id = id,
        prompt = "练习题",
        answer = "答案",
        analysis = listOf("解析"),
        knowledgePointIds = listOf("kp-test"),
        difficulty = 1,
    )
}
