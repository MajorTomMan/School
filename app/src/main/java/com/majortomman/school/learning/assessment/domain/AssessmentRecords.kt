package com.majortomman.school.learning.assessment.domain

enum class JudgeOutcome {
    INVALID_INPUT,
    INCORRECT,
    PARTIALLY_CORRECT,
    CORRECT,
}

data class JudgeResult(
    val outcome: JudgeOutcome,
    val normalizedAnswer: String? = null,
    val feedbackCode: String? = null,
) {
    init {
        require(normalizedAnswer == null || normalizedAnswer.isNotBlank()) {
            "normalizedAnswer 不能是空字符串"
        }
        require(feedbackCode == null || feedbackCode.isNotBlank()) {
            "feedbackCode 不能是空字符串"
        }
    }

    val countsAsValidAttempt: Boolean
        get() = outcome != JudgeOutcome.INVALID_INPUT

    val countsAsWrongAttempt: Boolean
        get() = outcome == JudgeOutcome.INCORRECT || outcome == JudgeOutcome.PARTIALLY_CORRECT
}

data class AttemptRecord(
    val id: AttemptId,
    val sessionId: SessionId,
    val questionKey: QuestionKey,
    val submissionSequence: Int,
    val answer: UserAnswer,
    val workProcess: String,
    val result: JudgeResult,
    val submittedAtEpochMillis: Long,
) {
    init {
        require(submissionSequence > 0) { "submissionSequence 必须大于 0" }
        require(workProcess.length <= MAX_WORK_PROCESS_LENGTH) { "做题过程不能超过 $MAX_WORK_PROCESS_LENGTH 个字符" }
        require(submittedAtEpochMillis >= 0L) { "submittedAtEpochMillis 不能小于 0" }
    }

    companion object {
        const val MAX_WORK_PROCESS_LENGTH = 12_000
    }
}

sealed interface QuestionLearningEvent {
    val sessionId: SessionId
    val questionKey: QuestionKey
    val occurredAtEpochMillis: Long
}

data class QuestionSkipped(
    override val sessionId: SessionId,
    override val questionKey: QuestionKey,
    override val occurredAtEpochMillis: Long,
) : QuestionLearningEvent {
    init {
        require(occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis 不能小于 0" }
    }
}

data class HintViewed(
    override val sessionId: SessionId,
    override val questionKey: QuestionKey,
    val hintId: String,
    override val occurredAtEpochMillis: Long,
) : QuestionLearningEvent {
    init {
        require(hintId.isNotBlank()) { "hintId 不能为空" }
        require(occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis 不能小于 0" }
    }
}

data class ExplanationViewed(
    override val sessionId: SessionId,
    override val questionKey: QuestionKey,
    override val occurredAtEpochMillis: Long,
) : QuestionLearningEvent {
    init {
        require(occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis 不能小于 0" }
    }
}

enum class AssessmentSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}

data class AssessmentSession(
    val id: SessionId,
    val questionSetId: QuestionSetId,
    val currentQuestionKey: QuestionKey,
    val status: AssessmentSessionStatus,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
) {
    init {
        require(startedAtEpochMillis >= 0L) { "startedAtEpochMillis 不能小于 0" }
        require(completedAtEpochMillis == null || completedAtEpochMillis >= startedAtEpochMillis) {
            "completedAtEpochMillis 不能早于 startedAtEpochMillis"
        }
        require(
            (status == AssessmentSessionStatus.COMPLETED) == (completedAtEpochMillis != null),
        ) { "只有 COMPLETED 会话必须设置 completedAtEpochMillis" }
    }
}
