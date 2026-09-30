package com.majortomman.school.learning.runtime

import com.majortomman.school.learning.activity.ActivityEvent
import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivityResult
import com.majortomman.school.learning.activity.ActivityRuntime
import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.InlineAssessmentEvaluator
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.assessment.domain.InlineAssessmentRule
import com.majortomman.school.learning.assessment.domain.InlineAssessmentSpec
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseStepRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeBoundaryTest {
    @Test
    fun activityProducesSemanticResultWithoutOwningLessonFlow() {
        val runtime = ActivityRuntime(TextAnswerActivitySpec(ActivityId("activity.answer")))
        runtime.dispatch(ActivityEvent.TextChanged(" -3 "))
        val transition = runtime.dispatch(ActivityEvent.Submit)

        val result = transition.result as ActivityResult.TextAnswer
        assertEquals("-3", result.value)
    }

    @Test
    fun assessmentEvaluatesActivityResultWithoutAdvancingLesson() {
        val spec = InlineAssessmentSpec(
            rule = InlineAssessmentRule.ExactText("-3"),
            explanation = listOf(LearningContent.Text("正确")),
            knowledgePointIds = listOf("kp"),
            difficulty = Difficulty(0.2),
        )
        val result = InlineAssessmentEvaluator.evaluate(spec, ActivityResult.TextAnswer(ActivityId("activity.answer"), "-3"))

        assertEquals(InlineAssessmentOutcome.CORRECT, result.outcome)
    }

    @Test
    fun onlyLessonRuntimeAdvancesCurrentStep() {
        val lesson = lesson()
        val runtime = LessonRuntime(lesson)

        assertEquals("explain", runtime.currentStep.id)
        runtime.dispatch(LessonRuntimeEvent.ActivityCompleted("practice"))
        assertEquals("explain", runtime.currentStep.id)

        runtime.dispatch(LessonRuntimeEvent.ContinueRequested)
        assertEquals("practice", runtime.currentStep.id)

        runtime.dispatch(LessonRuntimeEvent.AssessmentEvaluated("practice", correct = false))
        assertTrue(runtime.state.needsRetry)
        assertEquals("practice", runtime.currentStep.id)

        runtime.dispatch(LessonRuntimeEvent.AssessmentEvaluated("practice", correct = true))
        assertTrue(runtime.state.finished)
        assertFalse(runtime.state.needsRetry)
        assertEquals(setOf("explain", "practice"), runtime.state.completedStepIds)
    }

    private fun lesson(): CourseLesson {
        val assessment = InlineAssessmentSpec(
            rule = InlineAssessmentRule.ExactText("-3"),
            explanation = listOf(LearningContent.Text("正确")),
            knowledgePointIds = listOf("kp"),
            difficulty = Difficulty(0.2),
        )
        return CourseLesson(
            id = "lesson",
            title = "lesson",
            aliases = emptyList(),
            goals = listOf("goal"),
            knowledgePointIds = listOf("kp"),
            prerequisiteLessonIds = emptyList(),
            references = emptyList(),
            steps = listOf(
                CourseStep(
                    id = "explain",
                    role = CourseStepRole.EXPLANATION,
                    title = null,
                    content = listOf(LearningContent.Text("explanation")),
                ),
                CourseStep(
                    id = "practice",
                    role = CourseStepRole.PRACTICE,
                    title = null,
                    content = listOf(LearningContent.Text("prompt")),
                    activity = TextAnswerActivitySpec(ActivityId("activity.answer")),
                    assessment = assessment,
                ),
            ),
        )
    }
}
