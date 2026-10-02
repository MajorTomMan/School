package com.majortomman.school.learning.runtime

import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.CoreTextAnswerActivityHandler
import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.activity.SubmitActivity
import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.activity.TextAnswerActivityState
import com.majortomman.school.learning.activity.TextChanged
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.assessment.domain.InlineAssessmentRule
import com.majortomman.school.learning.assessment.domain.InlineAssessmentSpec
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseStepRole
import com.majortomman.school.learning.evidence.domain.LearningEvidence
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LessonSessionControllerTest {
    @Before
    fun installActivityHandlers() {
        SchoolActivityRuntimeCatalog.install(CoreTextAnswerActivityHandler)
    }

    @Test
    fun incorrectAnswerDoesNotAdvanceAndCorrectAnswerPersistsBeforeAdvance() = runBlocking {
        val recorded = mutableListOf<LearningEvidence>()
        val controller = LessonSessionController(
            courseId = "course-1",
            contentRevision = "rev-1",
            lesson = lesson(),
            evidenceGateway = LessonEvidenceGateway { recorded += it },
        )

        controller.dispatch(LessonSessionIntent.ActivityEventDispatched("practice", TextChanged("wrong")))
        controller.dispatch(LessonSessionIntent.ActivityEventDispatched("practice", SubmitActivity))
        assertEquals(0, controller.state.value.runtime.currentStepIndex)
        assertEquals(InlineAssessmentOutcome.INCORRECT, controller.state.value.assessmentFeedback?.outcome)

        controller.dispatch(LessonSessionIntent.ActivityEventDispatched("practice", TextChanged("-3")))
        controller.dispatch(LessonSessionIntent.ActivityEventDispatched("practice", SubmitActivity))
        assertTrue(recorded.isNotEmpty())
        assertTrue(controller.state.value.runtime.finished)
        assertEquals(InlineAssessmentOutcome.CORRECT, controller.state.value.assessmentFeedback?.outcome)
    }

    @Test
    fun activitySemanticStateIsOwnedByController() = runBlocking {
        val controller = LessonSessionController(
            courseId = "course-1",
            contentRevision = "rev-1",
            lesson = lesson(),
            evidenceGateway = LessonEvidenceGateway { },
        )

        val initial = controller.state.value.activityState as TextAnswerActivityState
        assertEquals("", initial.draft)

        controller.dispatch(LessonSessionIntent.ActivityEventDispatched("practice", TextChanged(" -3 ")))

        val edited = controller.state.value.activityState as TextAnswerActivityState
        assertEquals(" -3 ", edited.draft)
        assertFalse(controller.state.value.runtime.finished)
    }

    @Test
    fun persistenceFailureNeverAdvancesLesson() = runBlocking {
        val controller = LessonSessionController(
            courseId = "course-1",
            contentRevision = "rev-1",
            lesson = lesson(),
            evidenceGateway = LessonEvidenceGateway { error("disk unavailable") },
        )

        controller.dispatch(LessonSessionIntent.ActivityEventDispatched("practice", TextChanged("-3")))
        controller.dispatch(LessonSessionIntent.ActivityEventDispatched("practice", SubmitActivity))

        assertFalse(controller.state.value.runtime.finished)
        assertEquals(0, controller.state.value.runtime.currentStepIndex)
        assertTrue(controller.state.value.errorMessage?.contains("disk unavailable") == true)
    }

    private fun lesson(): CourseLesson = CourseLesson(
        id = "lesson-1",
        title = "数轴",
        aliases = emptyList(),
        goals = listOf("理解数轴"),
        knowledgePointIds = listOf("number-line"),
        prerequisiteLessonIds = emptyList(),
        references = emptyList(),
        steps = listOf(
            CourseStep(
                id = "practice",
                role = CourseStepRole.PRACTICE,
                title = null,
                content = listOf(LearningContent.Text("把点移动到 -3")),
                activity = TextAnswerActivitySpec(ActivityId("activity-answer")),
                assessment = InlineAssessmentSpec(
                    rule = InlineAssessmentRule.ExactText("-3"),
                    explanation = listOf(LearningContent.Text("正确")),
                    knowledgePointIds = listOf("number-line"),
                    difficulty = Difficulty(0.3),
                ),
            ),
        ),
    )
}
