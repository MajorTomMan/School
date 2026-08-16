package com.majortomman.school.learning.assessment.persistence

import com.majortomman.school.learning.assessment.domain.AssessmentSession
import com.majortomman.school.learning.assessment.domain.AssessmentSessionStatus
import com.majortomman.school.learning.assessment.domain.AttemptId
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.ExplanationViewed
import com.majortomman.school.learning.assessment.domain.HintViewed
import com.majortomman.school.learning.assessment.domain.JudgeOutcome
import com.majortomman.school.learning.assessment.domain.JudgeResult
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionCompletionStatus
import com.majortomman.school.learning.assessment.domain.QuestionId
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionResult
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.assessment.domain.QuestionSkipped
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import com.majortomman.school.learning.assessment.domain.UserAnswer
import com.majortomman.school.learning.mastery.domain.MasteryEvidence
import com.majortomman.school.learning.mastery.domain.MasteryEvidenceOutcome
import com.majortomman.school.learning.mastery.domain.MasteryState
import com.majortomman.school.learning.mastery.domain.MasteryUpdate

data class PersistedAssessmentSession(
    val courseId: String,
    val contentRevision: String,
    val session: AssessmentSession,
    val attempts: List<AttemptRecord>,
    val events: List<PersistedLearningEvent>,
)

data class PersistedLearningEvent(
    val id: String,
    val event: QuestionLearningEvent,
)

data class AssessmentSettlementSnapshot(
    val summary: SessionSummary,
    val evidence: List<MasteryEvidence>,
    val masteryUpdates: List<MasteryUpdate>,
    val settledAtEpochMillis: Long,
    val alreadySettled: Boolean,
)

internal fun AssessmentSession.toEntity(
    courseId: String,
    contentRevision: String,
): AssessmentSessionEntity {
    require(courseId.isNotBlank()) { "courseId 不能为空" }
    require(contentRevision.isNotBlank()) { "contentRevision 不能为空" }
    return AssessmentSessionEntity(
        sessionId = id.value,
        courseId = courseId,
        contentRevision = contentRevision,
        questionSetId = questionSetId.value,
        currentQuestionId = currentQuestionKey.id.value,
        currentQuestionRevision = currentQuestionKey.revision,
        status = status.name,
        startedAtEpochMillis = startedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        settledAtEpochMillis = null,
    )
}

internal fun AssessmentSessionEntity.toDomain(): AssessmentSession = AssessmentSession(
    id = SessionId(sessionId),
    questionSetId = QuestionSetId(questionSetId),
    currentQuestionKey = QuestionKey(QuestionId(currentQuestionId), currentQuestionRevision),
    status = enumValueOrError(status, "assessment session status"),
    startedAtEpochMillis = startedAtEpochMillis,
    completedAtEpochMillis = completedAtEpochMillis,
)

internal fun AttemptRecord.toEntity(): AssessmentAttemptEntity {
    val encoded = when (val value = answer) {
        is UserAnswer.Text -> EncodedAnswer("TEXT", value.raw, null)
        is UserAnswer.Choice -> EncodedAnswer("CHOICE", value.optionId, null)
        is UserAnswer.Coordinate -> EncodedAnswer("COORDINATE", value.rawX, value.rawY)
    }
    return AssessmentAttemptEntity(
        attemptId = id.value,
        sessionId = sessionId.value,
        questionId = questionKey.id.value,
        questionRevision = questionKey.revision,
        submissionSequence = submissionSequence,
        answerKind = encoded.kind,
        answerPrimary = encoded.primary,
        answerSecondary = encoded.secondary,
        workProcess = workProcess,
        judgeOutcome = result.outcome.name,
        normalizedAnswer = result.normalizedAnswer,
        feedbackCode = result.feedbackCode,
        submittedAtEpochMillis = submittedAtEpochMillis,
    )
}

internal fun AssessmentAttemptEntity.toDomain(): AttemptRecord = AttemptRecord(
    id = AttemptId(attemptId),
    sessionId = SessionId(sessionId),
    questionKey = QuestionKey(QuestionId(questionId), questionRevision),
    submissionSequence = submissionSequence,
    answer = when (answerKind) {
        "TEXT" -> UserAnswer.Text(answerPrimary)
        "CHOICE" -> UserAnswer.Choice(answerPrimary)
        "COORDINATE" -> UserAnswer.Coordinate(
            rawX = answerPrimary,
            rawY = answerSecondary ?: error("坐标答案缺少 Y 值"),
        )
        else -> error("不支持的持久化答案类型：$answerKind")
    },
    workProcess = workProcess,
    result = JudgeResult(
        outcome = enumValueOrError(judgeOutcome, "judge outcome"),
        normalizedAnswer = normalizedAnswer,
        feedbackCode = feedbackCode,
    ),
    submittedAtEpochMillis = submittedAtEpochMillis,
)

internal fun PersistedLearningEvent.toEntity(): AssessmentEventEntity {
    require(id.isNotBlank()) { "event id 不能为空" }
    val encoded = when (val value = event) {
        is QuestionSkipped -> EncodedEvent("SKIPPED", null)
        is HintViewed -> EncodedEvent("HINT_VIEWED", value.hintId)
        is ExplanationViewed -> EncodedEvent("EXPLANATION_VIEWED", null)
    }
    return AssessmentEventEntity(
        eventId = id,
        sessionId = event.sessionId.value,
        questionId = event.questionKey.id.value,
        questionRevision = event.questionKey.revision,
        eventType = encoded.type,
        hintId = encoded.hintId,
        occurredAtEpochMillis = event.occurredAtEpochMillis,
    )
}

internal fun AssessmentEventEntity.toDomain(): PersistedLearningEvent {
    val session = SessionId(sessionId)
    val key = QuestionKey(QuestionId(questionId), questionRevision)
    val event = when (eventType) {
        "SKIPPED" -> QuestionSkipped(session, key, occurredAtEpochMillis)
        "HINT_VIEWED" -> HintViewed(
            sessionId = session,
            questionKey = key,
            hintId = hintId ?: error("提示事件缺少 hintId"),
            occurredAtEpochMillis = occurredAtEpochMillis,
        )
        "EXPLANATION_VIEWED" -> ExplanationViewed(session, key, occurredAtEpochMillis)
        else -> error("不支持的持久化学习事件：$eventType")
    }
    return PersistedLearningEvent(eventId, event)
}

internal fun QuestionResult.toEntity(sessionId: SessionId): AssessmentQuestionResultEntity =
    AssessmentQuestionResultEntity(
        sessionId = sessionId.value,
        questionId = questionKey.id.value,
        questionRevision = questionKey.revision,
        completionStatus = status.name,
        totalSubmissionCount = totalSubmissionCount,
        validAttemptCount = validAttemptCount,
        invalidSubmissionCount = invalidSubmissionCount,
        wrongAttemptCount = wrongAttemptCount,
        hintViewCount = hintViewCount,
        explanationViewed = explanationViewed,
        wasEverSkipped = wasEverSkipped,
    )

internal fun AssessmentQuestionResultEntity.toDomain(): QuestionResult = QuestionResult(
    questionKey = QuestionKey(QuestionId(questionId), questionRevision),
    status = enumValueOrError(completionStatus, "question completion status"),
    totalSubmissionCount = totalSubmissionCount,
    validAttemptCount = validAttemptCount,
    invalidSubmissionCount = invalidSubmissionCount,
    wrongAttemptCount = wrongAttemptCount,
    hintViewCount = hintViewCount,
    explanationViewed = explanationViewed,
    wasEverSkipped = wasEverSkipped,
)

internal fun SessionSummary.toSettlementEntity(
    policyVersion: Int,
    settledAtEpochMillis: Long,
): AssessmentSettlementEntity = AssessmentSettlementEntity(
    sessionId = sessionId.value,
    questionSetId = questionSetId.value,
    totalQuestionCount = totalQuestionCount,
    attemptedQuestionCount = attemptedQuestionCount,
    firstCorrectCount = firstCorrectCount,
    recoveredCorrectCount = recoveredCorrectCount,
    finalCorrectCount = finalCorrectCount,
    finalWrongCount = finalWrongCount,
    skippedCount = skippedCount,
    unansweredCount = unansweredCount,
    wrongQuestionCount = wrongQuestionCount,
    wrongSubmissionCount = wrongSubmissionCount,
    invalidSubmissionCount = invalidSubmissionCount,
    hintViewCount = hintViewCount,
    explanationViewedQuestionCount = explanationViewedQuestionCount,
    masteryPolicyVersion = policyVersion,
    settledAtEpochMillis = settledAtEpochMillis,
)

internal fun MasteryEvidence.toEntity(): MasteryEvidenceEntity = MasteryEvidenceEntity(
    sessionId = sessionId.value,
    knowledgePointId = knowledgePointId.value,
    questionId = questionId.value,
    questionRevision = questionRevision,
    outcome = outcome.name,
    score = score,
    weight = weight,
    difficulty = difficulty.value,
    wrongAttemptCount = wrongAttemptCount,
    hintViewCount = hintViewCount,
    explanationViewed = explanationViewed,
)

internal fun MasteryEvidenceEntity.toDomain(): MasteryEvidence = MasteryEvidence(
    knowledgePointId = KnowledgePointId(knowledgePointId),
    questionId = QuestionId(questionId),
    questionRevision = questionRevision,
    sessionId = SessionId(sessionId),
    outcome = enumValueOrError(outcome, "mastery evidence outcome"),
    score = score,
    weight = weight,
    difficulty = Difficulty(difficulty),
    wrongAttemptCount = wrongAttemptCount,
    hintViewCount = hintViewCount,
    explanationViewed = explanationViewed,
)

internal fun MasteryStateEntity.toDomain(): MasteryState = MasteryState(
    knowledgePointId = KnowledgePointId(knowledgePointId),
    score = score,
    accumulatedEvidenceWeight = accumulatedEvidenceWeight,
)

internal fun MasteryUpdate.toStateEntity(updatedAtEpochMillis: Long): MasteryStateEntity =
    MasteryStateEntity(
        knowledgePointId = knowledgePointId.value,
        score = afterScore,
        accumulatedEvidenceWeight = afterEvidenceWeight,
        lastPolicyVersion = policyVersion,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

internal fun MasteryUpdate.toSnapshotEntity(
    sessionId: SessionId,
    createdAtEpochMillis: Long,
): MasterySnapshotEntity = MasterySnapshotEntity(
    sessionId = sessionId.value,
    knowledgePointId = knowledgePointId.value,
    beforeScore = beforeScore,
    afterScore = afterScore,
    beforeEvidenceWeight = beforeEvidenceWeight,
    appliedEvidenceWeight = appliedEvidenceWeight,
    afterEvidenceWeight = afterEvidenceWeight,
    policyVersion = policyVersion,
    createdAtEpochMillis = createdAtEpochMillis,
)

internal fun MasterySnapshotEntity.toDomain(): MasteryUpdate = MasteryUpdate(
    knowledgePointId = KnowledgePointId(knowledgePointId),
    beforeScore = beforeScore,
    afterScore = afterScore,
    beforeEvidenceWeight = beforeEvidenceWeight,
    appliedEvidenceWeight = appliedEvidenceWeight,
    afterEvidenceWeight = afterEvidenceWeight,
    policyVersion = policyVersion,
)

internal fun AssessmentSettlementEntity.verifyAgainst(summary: SessionSummary) {
    require(sessionId == summary.sessionId.value && questionSetId == summary.questionSetId.value) {
        "结算快照与题组汇总标识不一致"
    }
    require(
        totalQuestionCount == summary.totalQuestionCount &&
            attemptedQuestionCount == summary.attemptedQuestionCount &&
            firstCorrectCount == summary.firstCorrectCount &&
            recoveredCorrectCount == summary.recoveredCorrectCount &&
            finalCorrectCount == summary.finalCorrectCount &&
            finalWrongCount == summary.finalWrongCount &&
            skippedCount == summary.skippedCount &&
            unansweredCount == summary.unansweredCount &&
            wrongQuestionCount == summary.wrongQuestionCount &&
            wrongSubmissionCount == summary.wrongSubmissionCount &&
            invalidSubmissionCount == summary.invalidSubmissionCount &&
            hintViewCount == summary.hintViewCount &&
            explanationViewedQuestionCount == summary.explanationViewedQuestionCount
    ) { "结算聚合字段与题目结果不一致" }
}

private data class EncodedAnswer(
    val kind: String,
    val primary: String,
    val secondary: String?,
)

private data class EncodedEvent(
    val type: String,
    val hintId: String?,
)

private inline fun <reified T : Enum<T>> enumValueOrError(value: String, label: String): T =
    enumValues<T>().firstOrNull { it.name == value }
        ?: error("不支持的 $label：$value")
