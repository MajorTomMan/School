package com.majortomman.school.learning.assessment.application

import com.majortomman.school.learning.assessment.domain.AssessmentSession
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import com.majortomman.school.learning.mastery.domain.MasteryEvidence
import com.majortomman.school.learning.mastery.domain.MasteryUpdate

data class AssessmentSessionFacts(
    val courseId: String,
    val contentRevision: String,
    val session: AssessmentSession,
    val attempts: List<AttemptRecord>,
    val events: List<QuestionLearningEvent>,
)

data class AssessmentCompletion(
    val summary: SessionSummary,
    val evidence: List<MasteryEvidence>,
    val masteryUpdates: List<MasteryUpdate>,
    val settledAtEpochMillis: Long,
    val alreadySettled: Boolean,
)

interface AssessmentSessionGateway {
    suspend fun findResumable(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
    ): AssessmentSessionFacts?

    suspend fun start(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
        session: AssessmentSession,
    )

    suspend fun moveToQuestion(
        sessionId: SessionId,
        questionSet: QuestionSetDefinition,
        questionIndex: Int,
    )

    suspend fun appendAttempt(
        questionSet: QuestionSetDefinition,
        attempt: AttemptRecord,
    )

    suspend fun appendEvent(
        eventId: String,
        questionSet: QuestionSetDefinition,
        event: QuestionLearningEvent,
    )

    suspend fun complete(
        sessionId: SessionId,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
        completedAtEpochMillis: Long,
    ): AssessmentCompletion
}

fun interface AssessmentClock {
    fun nowEpochMillis(): Long
}

fun interface AssessmentIdFactory {
    fun createId(prefix: String): String
}
