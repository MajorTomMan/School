package com.majortomman.school.learning.runtime

import com.majortomman.school.learning.activity.ActivityResult
import com.majortomman.school.learning.assessment.domain.InlineAssessmentEvidenceFactory
import com.majortomman.school.learning.assessment.domain.InlineAssessmentEvaluator
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.evidence.domain.LearningEvidence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface LessonEvidenceGateway {
    suspend fun record(evidence: List<LearningEvidence>)
}

data class LessonAssessmentFeedback(
    val stepId: String,
    val outcome: InlineAssessmentOutcome,
)

data class LessonSessionState(
    val runtime: LessonRuntimeState,
    val assessmentFeedback: LessonAssessmentFeedback? = null,
    val busy: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LessonSessionIntent {
    data object ContinueRequested : LessonSessionIntent

    data class ActivityResultSubmitted(
        val stepId: String,
        val result: ActivityResult,
    ) : LessonSessionIntent
}

/**
 * Lesson 应用层控制器。
 *
 * UI 只发送语义 Intent 并订阅 state；Inline Assessment、Evidence 持久化和 Step 推进
 * 均在这里串行执行。只有 Evidence 成功写入后，带 assessment 的 Activity 才允许推进。
 */
class LessonSessionController(
    private val courseId: String,
    private val contentRevision: String,
    private val lesson: CourseLesson,
    private val evidenceGateway: LessonEvidenceGateway,
) {
    init {
        require(courseId.isNotBlank()) { "courseId 不能为空" }
        require(contentRevision.isNotBlank()) { "contentRevision 不能为空" }
    }

    private val mutex = Mutex()
    private val runtime = LessonRuntime(lesson)
    private val wrongAttempts = linkedMapOf<String, Int>()
    private val mutableState = MutableStateFlow(LessonSessionState(runtime.state))

    val state: StateFlow<LessonSessionState> = mutableState.asStateFlow()

    suspend fun dispatch(intent: LessonSessionIntent) {
        mutex.withLock {
            runCatching {
                when (intent) {
                    LessonSessionIntent.ContinueRequested -> continueLesson()
                    is LessonSessionIntent.ActivityResultSubmitted -> submitActivityResult(intent)
                }
            }.onFailure {
                publish(
                    assessmentFeedback = mutableState.value.assessmentFeedback,
                    busy = false,
                    errorMessage = it.message ?: "学习记录处理失败，请重试。",
                )
            }
        }
    }

    private fun continueLesson() {
        if (mutableState.value.busy) return
        runtime.dispatch(LessonRuntimeEvent.ContinueRequested)
        publish()
    }

    private suspend fun submitActivityResult(intent: LessonSessionIntent.ActivityResultSubmitted) {
        if (mutableState.value.busy || runtime.state.finished) return
        val step = runtime.currentStep
        if (intent.stepId != step.id || step.activity == null) return

        val assessment = step.assessment
        if (assessment == null) {
            runtime.dispatch(LessonRuntimeEvent.ActivityCompleted(step.id))
            publish()
            return
        }

        val evaluated = InlineAssessmentEvaluator.evaluate(assessment, intent.result)
        when (evaluated.outcome) {
            InlineAssessmentOutcome.INVALID -> {
                publish(
                    assessmentFeedback = LessonAssessmentFeedback(step.id, InlineAssessmentOutcome.INVALID),
                )
            }

            InlineAssessmentOutcome.INCORRECT -> {
                wrongAttempts[step.id] = (wrongAttempts[step.id] ?: 0) + 1
                runtime.dispatch(LessonRuntimeEvent.AssessmentEvaluated(step.id, correct = false))
                publish(
                    assessmentFeedback = LessonAssessmentFeedback(step.id, InlineAssessmentOutcome.INCORRECT),
                )
            }

            InlineAssessmentOutcome.CORRECT -> {
                val evidence = InlineAssessmentEvidenceFactory.createForCompletedActivity(
                    courseId = courseId,
                    contentRevision = contentRevision,
                    lessonId = lesson.id,
                    stepId = step.id,
                    activityId = intent.result.activityId,
                    assessment = assessment,
                    wrongAttemptCount = wrongAttempts[step.id] ?: 0,
                )
                publish(busy = true)
                evidenceGateway.record(evidence)
                wrongAttempts.remove(step.id)
                runtime.dispatch(LessonRuntimeEvent.AssessmentEvaluated(step.id, correct = true))
                publish(
                    assessmentFeedback = LessonAssessmentFeedback(step.id, InlineAssessmentOutcome.CORRECT),
                )
            }
        }
    }

    private fun publish(
        assessmentFeedback: LessonAssessmentFeedback? = null,
        busy: Boolean = false,
        errorMessage: String? = null,
    ) {
        mutableState.value = LessonSessionState(
            runtime = runtime.state,
            assessmentFeedback = assessmentFeedback,
            busy = busy,
            errorMessage = errorMessage,
        )
    }
}
