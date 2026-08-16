package com.majortomman.school.learning.assessment.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "assessment_session",
    indices = [
        Index(value = ["courseId", "questionSetId", "status"]),
        Index(value = ["contentRevision"]),
    ],
)
data class AssessmentSessionEntity(
    @androidx.room.PrimaryKey
    val sessionId: String,
    val courseId: String,
    val contentRevision: String,
    val questionSetId: String,
    val currentQuestionId: String,
    val currentQuestionRevision: Int,
    val status: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val settledAtEpochMillis: Long?,
)

@Entity(
    tableName = "assessment_attempt",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(
            value = ["sessionId", "questionId", "questionRevision", "submissionSequence"],
            unique = true,
        ),
    ],
)
data class AssessmentAttemptEntity(
    @androidx.room.PrimaryKey
    val attemptId: String,
    val sessionId: String,
    val questionId: String,
    val questionRevision: Int,
    val submissionSequence: Int,
    val answerKind: String,
    val answerPrimary: String,
    val answerSecondary: String?,
    val workProcess: String,
    val judgeOutcome: String,
    val normalizedAnswer: String?,
    val feedbackCode: String?,
    val submittedAtEpochMillis: Long,
)

@Entity(
    tableName = "assessment_event",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "questionId", "questionRevision", "occurredAtEpochMillis"]),
    ],
)
data class AssessmentEventEntity(
    @androidx.room.PrimaryKey
    val eventId: String,
    val sessionId: String,
    val questionId: String,
    val questionRevision: Int,
    val eventType: String,
    val hintId: String?,
    val occurredAtEpochMillis: Long,
)

@Entity(
    tableName = "assessment_question_result",
    primaryKeys = ["sessionId", "questionId", "questionRevision"],
    foreignKeys = [
        ForeignKey(
            entity = AssessmentSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"])],
)
data class AssessmentQuestionResultEntity(
    val sessionId: String,
    val questionId: String,
    val questionRevision: Int,
    val completionStatus: String,
    val totalSubmissionCount: Int,
    val validAttemptCount: Int,
    val invalidSubmissionCount: Int,
    val wrongAttemptCount: Int,
    val hintViewCount: Int,
    val explanationViewed: Boolean,
    val wasEverSkipped: Boolean,
)

@Entity(
    tableName = "assessment_settlement",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questionSetId", "settledAtEpochMillis"])],
)
data class AssessmentSettlementEntity(
    @androidx.room.PrimaryKey
    val sessionId: String,
    val questionSetId: String,
    val totalQuestionCount: Int,
    val attemptedQuestionCount: Int,
    val firstCorrectCount: Int,
    val recoveredCorrectCount: Int,
    val finalCorrectCount: Int,
    val finalWrongCount: Int,
    val skippedCount: Int,
    val unansweredCount: Int,
    val wrongQuestionCount: Int,
    val wrongSubmissionCount: Int,
    val invalidSubmissionCount: Int,
    val hintViewCount: Int,
    val explanationViewedQuestionCount: Int,
    val masteryPolicyVersion: Int,
    val settledAtEpochMillis: Long,
)

@Entity(
    tableName = "mastery_evidence",
    primaryKeys = ["sessionId", "knowledgePointId", "questionId", "questionRevision"],
    foreignKeys = [
        ForeignKey(
            entity = AssessmentSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["knowledgePointId", "sessionId"]),
    ],
)
data class MasteryEvidenceEntity(
    val sessionId: String,
    val knowledgePointId: String,
    val questionId: String,
    val questionRevision: Int,
    val outcome: String,
    val score: Double,
    val weight: Double,
    val difficulty: Double,
    val wrongAttemptCount: Int,
    val hintViewCount: Int,
    val explanationViewed: Boolean,
)

@Entity(tableName = "mastery_state")
data class MasteryStateEntity(
    @androidx.room.PrimaryKey
    val knowledgePointId: String,
    val score: Double,
    val accumulatedEvidenceWeight: Double,
    val lastPolicyVersion: Int,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "mastery_snapshot",
    primaryKeys = ["sessionId", "knowledgePointId"],
    foreignKeys = [
        ForeignKey(
            entity = AssessmentSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["knowledgePointId", "createdAtEpochMillis"]),
    ],
)
data class MasterySnapshotEntity(
    val sessionId: String,
    val knowledgePointId: String,
    val beforeScore: Double,
    val afterScore: Double,
    val beforeEvidenceWeight: Double,
    val appliedEvidenceWeight: Double,
    val afterEvidenceWeight: Double,
    val policyVersion: Int,
    val createdAtEpochMillis: Long,
)
