package com.majortomman.school.learning.assessment.application

import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.JudgeResult
import com.majortomman.school.learning.assessment.domain.QuestionCompletionStatus
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import com.majortomman.school.learning.assessment.domain.UserAnswer
import com.majortomman.school.learning.mastery.domain.MasteryUpdate

data class AssessmentQuestionProgressState(
    val questionKey: QuestionKey,
    val completionStatus: QuestionCompletionStatus,
    val totalSubmissionCount: Int,
    val wrongAttemptCount: Int,
    val invalidSubmissionCount: Int,
    val viewedHintIds: Set<String>,
    val explanationViewed: Boolean,
    val latestJudgeResult: JudgeResult?,
) {
    val answerLocked: Boolean
        get() = completionStatus == QuestionCompletionStatus.FIRST_TRY_CORRECT ||
            completionStatus == QuestionCompletionStatus.RECOVERED_CORRECT
}

data class AssessmentQuestionPageState(
    val sessionId: SessionId,
    val questionIndex: Int,
    val questionCount: Int,
    val question: QuestionDefinition,
    val draftWorkProcess: String,
    val draftAnswer: UserAnswer?,
    val progress: AssessmentQuestionProgressState,
    val allProgress: List<AssessmentQuestionProgressState>,
    val busy: Boolean = false,
) {
    init {
        require(draftWorkProcess.length <= AttemptRecord.MAX_WORK_PROCESS_LENGTH) { "做题过程过长" }
    }

    val canGoPrevious: Boolean
        get() = !busy && questionIndex > 0

    val canGoNext: Boolean
        get() = !busy

    val canSubmit: Boolean
        get() = !busy && !progress.answerLocked && draftAnswer != null
}

sealed interface AssessmentState {
    data object Idle : AssessmentState
    data object Loading : AssessmentState

    data class Question(
        val page: AssessmentQuestionPageState,
    ) : AssessmentState

    data class FinishConfirmation(
        val page: AssessmentQuestionPageState,
        val preview: SessionSummary,
        val incompleteQuestionIndices: List<Int>,
        val busy: Boolean = false,
    ) : AssessmentState

    data class Result(
        val completion: AssessmentCompletion,
    ) : AssessmentState

    data class Error(
        val previous: AssessmentState,
        val message: String,
    ) : AssessmentState
}

sealed interface AssessmentIntent {
    data object Initialize : AssessmentIntent
    data class WorkProcessChanged(val process: String) : AssessmentIntent
    data class AnswerChanged(val answer: UserAnswer?) : AssessmentIntent
    data object SubmitAnswer : AssessmentIntent
    data object SkipQuestion : AssessmentIntent
    data object PreviousQuestion : AssessmentIntent
    data object NextQuestion : AssessmentIntent
    data class GoToQuestion(val index: Int) : AssessmentIntent
    data class ViewHint(val hintId: String) : AssessmentIntent
    data object ViewExplanation : AssessmentIntent
    data object RequestFinish : AssessmentIntent
    data object ContinueIncomplete : AssessmentIntent
    data object ConfirmFinish : AssessmentIntent
    data object CancelFinish : AssessmentIntent
    data object DismissError : AssessmentIntent
}
