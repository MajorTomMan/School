package com.majortomman.school.ui

import com.majortomman.school.learning.course.CourseExample
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseFormula
import com.majortomman.school.learning.course.CourseKeyIdea
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseVisualizationStep
import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonPresentationTest {
    @Test
    fun practiceAlwaysComesAfterTeachingAndSummary() {
        val pages = composeLessonPresentation(
            lesson(
                steps = listOf(CourseQuestion("question", null), CourseExplanation(null, "explanation")),
                practice = listOf(practice("p1"), practice("p2")),
                summary = listOf("summary"),
            ),
        )

        assertTrue(pages.first() is LessonPresentationPage.Overview)
        assertTrue(pages[pages.size - 3] is LessonPresentationPage.Summary)
        assertTrue(pages[pages.size - 2] is LessonPresentationPage.Practice)
        assertTrue(pages.last() is LessonPresentationPage.Practice)
        assertEquals(1, (pages[pages.size - 2] as LessonPresentationPage.Practice).number)
        assertEquals(2, (pages.last() as LessonPresentationPage.Practice).number)
    }

    @Test
    fun semanticStagesBreakWithoutDependingOnScreenHeight() {
        val teaching = composeLessonPresentation(
            lesson(
                steps = listOf(
                    CourseQuestion("question", "hint"),
                    CourseExplanation(null, "explanation"),
                    CourseKeyIdea("key", "content"),
                    CourseExample("example", "prompt", listOf("step"), "answer"),
                    CourseFormula("a+b", null),
                ),
            ),
        ).filterIsInstance<LessonPresentationPage.Teaching>()

        assertEquals(2, teaching.size)
        assertEquals(3, teaching[0].steps.size)
        assertEquals(2, teaching[1].steps.size)
        assertTrue(teaching[0].steps.first() is CourseQuestion)
        assertTrue(teaching[1].steps.first() is CourseExample)
    }

    @Test
    fun visualizationStartsAStableSemanticStage() {
        val visualization = CourseVisualizationStep(VisualizationInvocation(VisualizationKey("mathematics.number-line.basic")))
        val teaching = composeLessonPresentation(
            lesson(steps = listOf(CourseExplanation(null, "before"), visualization, CourseExplanation(null, "after"))),
        ).filterIsInstance<LessonPresentationPage.Teaching>()

        assertEquals(2, teaching.size)
        assertEquals(1, teaching[0].steps.size)
        assertEquals(2, teaching[1].steps.size)
        assertTrue(teaching[1].steps.first() is CourseVisualizationStep)
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
        title = "title",
        aliases = emptyList(),
        goals = listOf("goal"),
        knowledgePointIds = listOf("kp-test"),
        prerequisiteLessonIds = emptyList(),
        references = emptyList(),
        steps = steps,
        practice = practice,
        summary = summary,
    )

    private fun practice(id: String) = CoursePractice(
        id = id,
        prompt = "prompt",
        answer = "answer",
        analysis = listOf("analysis"),
        knowledgePointIds = listOf("kp-test"),
        difficulty = 1,
    )
}
