package com.majortomman.school.learning.runtime

import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivityRuntime
import com.majortomman.school.learning.activity.CoreTextAnswerActivityHandler
import com.majortomman.school.learning.activity.PlaceOnNumberLineActivitySpec
import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.activity.SubmitActivity
import com.majortomman.school.learning.activity.TextAnswerActivityResult
import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.activity.TextChanged
import com.majortomman.school.learning.activity.math.NumberLinePositionState
import com.majortomman.school.learning.activity.math.NumberPositionResult
import com.majortomman.school.learning.activity.math.PlaceOnNumberLineActivityHandler
import com.majortomman.school.learning.activity.math.PositionSelected
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
import org.junit.Before
import org.junit.Test

class RuntimeBoundaryTest {
    @Before
    fun installActivityHandlers() {
        SchoolActivityRuntimeCatalog.install(CoreTextAnswerActivityHandler)
        SchoolActivityRuntimeCatalog.install(PlaceOnNumberLineActivityHandler)
    }

    @Test
    fun textActivityProducesSemanticResultWithoutOwningLessonFlow() {
        val runtime = ActivityRuntime(TextAnswerActivitySpec(ActivityId("activity.answer")))
        runtime.dispatch(TextChanged(" -3 "))
        val transition = runtime.dispatch(SubmitActivity)

        val result = transition.result as TextAnswerActivityResult
        assertEquals("-3", result.value)
    }

    @Test
    fun numberLineActivityConvertsRawSelectionToSemanticPosition() {
        val spec = PlaceOnNumberLineActivitySpec(
            id = ActivityId("activity.position"),
            min = -8.0,
            max = 8.0,
            step = 1.0,
            initialValue = 0.0,
        )
        val runtime = ActivityRuntime(spec)
        val state = runtime.dispatch(PositionSelected(-2.7)).state as NumberLinePositionState
        assertEquals(-3.0, state.selectedValue, 0.0)

        val result = runtime.dispatch(SubmitActivity).result as NumberPositionResult
        assertEquals(-3.0, result.value, 0.0)

        val assessment = InlineAssessmentSpec(
            rule = InlineAssessmentRule.ExactNumber(expected = -3.0, tolerance = 1e-9),
            explanation = listOf(LearningContent.Text("正确")),
            knowledgePointIds = listOf("kp"),
            difficulty = Difficulty(0.2),
        )
        assertEquals(
            InlineAssessmentOutcome.CORRECT,
            InlineAssessmentEvaluator.evaluate(assessment, result).outcome,
        )
    }

    @Test
    fun assessmentEvaluatesActivityResultWithoutAdvancingLesson() {
        val spec = InlineAssessmentSpec(
            rule = InlineAssessmentRule.ExactText("-3"),
            explanation = listOf(LearningContent.Text("正确")),
            knowledgePointIds = listOf("kp"),
            difficulty = Difficulty(0.2),
        )
        val result = InlineAssessmentEvaluator.evaluate(
            spec,
            TextAnswerActivityResult(ActivityId("activity.answer"), "-3"),
        )

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
