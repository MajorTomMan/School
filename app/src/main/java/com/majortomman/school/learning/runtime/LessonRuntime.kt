package com.majortomman.school.learning.runtime

import com.majortomman.school.learning.course.CourseLesson

data class LessonRuntimeState(
    val lessonId: String,
    val currentStepIndex: Int,
    val completedStepIds: Set<String> = emptySet(),
    val needsRetry: Boolean = false,
    val finished: Boolean = false,
) {
    init {
        require(currentStepIndex >= 0) { "currentStepIndex 不能小于 0" }
    }
}

sealed interface LessonRuntimeEvent {
    data object ContinueRequested : LessonRuntimeEvent
    data class ActivityCompleted(val stepId: String) : LessonRuntimeEvent
    data class AssessmentEvaluated(val stepId: String, val correct: Boolean) : LessonRuntimeEvent
}

enum class StepOutcome {
    COMPLETED,
    NEEDS_RETRY,
}

data class LessonTransition(
    val state: LessonRuntimeState,
    val outcome: StepOutcome? = null,
)

class LessonRuntime(private val lesson: CourseLesson) {
    init {
        require(lesson.steps.isNotEmpty()) { "LessonRuntime 不能运行空 lesson" }
    }

    var state: LessonRuntimeState = LessonRuntimeState(lesson.id, currentStepIndex = 0)
        private set

    val currentStep get() = lesson.steps[state.currentStepIndex.coerceAtMost(lesson.steps.lastIndex)]

    fun dispatch(event: LessonRuntimeEvent): LessonTransition {
        if (state.finished) return LessonTransition(state)
        val step = currentStep
        val transition = when (event) {
            LessonRuntimeEvent.ContinueRequested -> {
                if (step.activity == null) completeCurrentStep() else LessonTransition(state)
            }
            is LessonRuntimeEvent.ActivityCompleted -> {
                if (event.stepId == step.id && step.activity != null && step.assessment == null) completeCurrentStep()
                else LessonTransition(state)
            }
            is LessonRuntimeEvent.AssessmentEvaluated -> {
                if (event.stepId != step.id || step.assessment == null) LessonTransition(state)
                else if (event.correct) completeCurrentStep()
                else {
                    state = state.copy(needsRetry = true)
                    LessonTransition(state, StepOutcome.NEEDS_RETRY)
                }
            }
        }
        state = transition.state
        return transition
    }

    private fun completeCurrentStep(): LessonTransition {
        val step = currentStep
        val completed = state.completedStepIds + step.id
        return if (state.currentStepIndex == lesson.steps.lastIndex) {
            LessonTransition(
                state.copy(completedStepIds = completed, needsRetry = false, finished = true),
                StepOutcome.COMPLETED,
            )
        } else {
            LessonTransition(
                state.copy(currentStepIndex = state.currentStepIndex + 1, completedStepIds = completed, needsRetry = false),
                StepOutcome.COMPLETED,
            )
        }
    }
}
