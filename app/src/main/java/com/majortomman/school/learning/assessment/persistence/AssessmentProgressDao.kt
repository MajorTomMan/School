package com.majortomman.school.learning.assessment.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface AssessmentProgressDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(entity: AssessmentSessionEntity)

    @Query("SELECT * FROM assessment_session WHERE sessionId = :sessionId")
    suspend fun findSession(sessionId: String): AssessmentSessionEntity?

    @Query(
        """
        SELECT * FROM assessment_session
        WHERE courseId = :courseId
          AND contentRevision = :contentRevision
          AND questionSetId = :questionSetId
          AND status = 'IN_PROGRESS'
        ORDER BY startedAtEpochMillis DESC, sessionId DESC
        LIMIT 1
        """,
    )
    suspend fun findInProgressSession(
        courseId: String,
        contentRevision: String,
        questionSetId: String,
    ): AssessmentSessionEntity?

    @Query(
        """
        UPDATE assessment_session
        SET currentQuestionId = :questionId,
            currentQuestionRevision = :questionRevision
        WHERE sessionId = :sessionId AND status = 'IN_PROGRESS'
        """,
    )
    suspend fun updateCurrentQuestion(
        sessionId: String,
        questionId: String,
        questionRevision: Int,
    ): Int

    @Query(
        """
        UPDATE assessment_session
        SET status = 'ABANDONED'
        WHERE sessionId = :sessionId AND status = 'IN_PROGRESS'
        """,
    )
    suspend fun abandonSession(sessionId: String): Int

    @Query(
        """
        UPDATE assessment_session
        SET status = 'COMPLETED',
            completedAtEpochMillis = :completedAtEpochMillis,
            settledAtEpochMillis = :settledAtEpochMillis
        WHERE sessionId = :sessionId AND status = 'IN_PROGRESS'
        """,
    )
    suspend fun completeSession(
        sessionId: String,
        completedAtEpochMillis: Long,
        settledAtEpochMillis: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttempt(entity: AssessmentAttemptEntity)

    @Query(
        """
        SELECT COALESCE(MAX(submissionSequence), 0)
        FROM assessment_attempt
        WHERE sessionId = :sessionId
          AND questionId = :questionId
          AND questionRevision = :questionRevision
        """,
    )
    suspend fun maxSubmissionSequence(
        sessionId: String,
        questionId: String,
        questionRevision: Int,
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM assessment_attempt
        WHERE sessionId = :sessionId
          AND questionId = :questionId
          AND questionRevision = :questionRevision
          AND judgeOutcome = 'CORRECT'
        """,
    )
    suspend fun correctAttemptCount(
        sessionId: String,
        questionId: String,
        questionRevision: Int,
    ): Int

    @Query(
        """
        SELECT * FROM assessment_attempt
        WHERE sessionId = :sessionId
        ORDER BY submittedAtEpochMillis ASC, submissionSequence ASC, attemptId ASC
        """,
    )
    suspend fun attemptsForSession(sessionId: String): List<AssessmentAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(entity: AssessmentEventEntity)

    @Query(
        """
        SELECT * FROM assessment_event
        WHERE sessionId = :sessionId
        ORDER BY occurredAtEpochMillis ASC, eventId ASC
        """,
    )
    suspend fun eventsForSession(sessionId: String): List<AssessmentEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestionResults(entities: List<AssessmentQuestionResultEntity>)

    @Query(
        """
        SELECT * FROM assessment_question_result
        WHERE sessionId = :sessionId
        ORDER BY questionId ASC, questionRevision ASC
        """,
    )
    suspend fun questionResultsForSession(sessionId: String): List<AssessmentQuestionResultEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSettlement(entity: AssessmentSettlementEntity)

    @Query("SELECT * FROM assessment_settlement WHERE sessionId = :sessionId")
    suspend fun findSettlement(sessionId: String): AssessmentSettlementEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMasteryEvidence(entities: List<MasteryEvidenceEntity>)

    @Query(
        """
        SELECT * FROM mastery_evidence
        WHERE sessionId = :sessionId
        ORDER BY knowledgePointId ASC, questionId ASC, questionRevision ASC
        """,
    )
    suspend fun masteryEvidenceForSession(sessionId: String): List<MasteryEvidenceEntity>

    @Query("SELECT * FROM mastery_state WHERE knowledgePointId = :knowledgePointId")
    suspend fun findMasteryState(knowledgePointId: String): MasteryStateEntity?

    @Upsert
    suspend fun upsertMasteryState(entity: MasteryStateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMasterySnapshot(entity: MasterySnapshotEntity)

    @Query(
        """
        SELECT * FROM mastery_snapshot
        WHERE sessionId = :sessionId
        ORDER BY knowledgePointId ASC
        """,
    )
    suspend fun masterySnapshotsForSession(sessionId: String): List<MasterySnapshotEntity>

    @Query(
        """
        SELECT * FROM mastery_snapshot
        WHERE knowledgePointId = :knowledgePointId
        ORDER BY createdAtEpochMillis ASC, sessionId ASC
        """,
    )
    suspend fun masterySnapshotsForKnowledgePoint(
        knowledgePointId: String,
    ): List<MasterySnapshotEntity>
}
