package com.majortomman.school.learning.assessment.persistence

import com.majortomman.school.learning.assessment.application.AssessmentCompletion
import com.majortomman.school.learning.assessment.application.AssessmentSessionFacts
import com.majortomman.school.learning.assessment.application.AssessmentSessionGateway
import com.majortomman.school.learning.assessment.domain.AssessmentSession
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.SessionId

class RoomAssessmentSessionGateway(
    private val store: AssessmentProgressStore,
) : AssessmentSessionGateway {
    override suspend fun findResumable(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
    ): AssessmentSessionFacts? = store.findResumableSession(
        courseId = courseId,
        contentRevision = contentRevision,
        questionSet = questionSet,
    )?.let { persisted ->
        AssessmentSessionFacts(
            courseId = persisted.courseId,
            contentRevision = persisted.contentRevision,
            session = persisted.session,
            attempts = persisted.attempts,
            events = persisted.events.map(PersistedLearningEvent::event),
        )
    }

    override suspend fun start(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
        session: AssessmentSession,
    ) {
        store.startSession(courseId, contentRevision, questionSet, session)
    }

    override suspend fun moveToQuestion(
        sessionId: SessionId,
        questionSet: QuestionSetDefinition,
        questionIndex: Int,
    ) {
        require(questionIndex in questionSet.questions.indices) { "题目索引越界：$questionIndex" }
        store.moveToQuestion(
            sessionId = sessionId,
            questionSet = questionSet,
            questionKey = questionSet.questions[questionIndex].key,
        )
    }

    override suspend fun appendAttempt(
        questionSet: QuestionSetDefinition,
        attempt: AttemptRecord,
    ) {
        store.recordAttempt(questionSet, attempt)
    }

    override suspend fun appendEvent(
        eventId: String,
        questionSet: QuestionSetDefinition,
        event: QuestionLearningEvent,
    ) {
        store.recordEvent(
            questionSet = questionSet,
            persistedEvent = PersistedLearningEvent(eventId, event),
        )
    }

    override suspend fun complete(
        sessionId: SessionId,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
        completedAtEpochMillis: Long,
    ): AssessmentCompletion = store.settle(
        sessionId = sessionId,
        expectedContentRevision = contentRevision,
        questionSet = questionSet,
        completedAtEpochMillis = completedAtEpochMillis,
    ).let { snapshot ->
        AssessmentCompletion(
            summary = snapshot.summary,
            evidence = snapshot.evidence,
            masteryUpdates = snapshot.masteryUpdates,
            settledAtEpochMillis = snapshot.settledAtEpochMillis,
            alreadySettled = snapshot.alreadySettled,
        )
    }
}
