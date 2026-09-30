package com.majortomman.school.learning.evidence.persistence

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.evidence.domain.LearningEvidence
import com.majortomman.school.learning.evidence.domain.LearningEvidenceOutcome
import com.majortomman.school.learning.evidence.domain.LearningEvidenceSource
import com.majortomman.school.learning.evidence.domain.LearningEvidenceSourceKind
import com.majortomman.school.learning.mastery.domain.MasteryPolicy
import com.majortomman.school.learning.mastery.domain.MasteryPrior
import com.majortomman.school.learning.mastery.domain.MasteryState
import com.majortomman.school.learning.mastery.domain.MasteryUpdate
import com.majortomman.school.learning.mastery.domain.WeightedMasteryPolicy
import com.majortomman.school.learning.persistence.SchoolLearningDatabase

@Entity(
    tableName = "learning_evidence",
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["knowledgePointId", "recordedAtEpochMillis"]),
        Index(value = ["sourceKind", "sourceContextId"]),
    ],
)
internal data class LearningEvidenceEntity(
    @androidx.room.PrimaryKey
    val evidenceId: String,
    val courseId: String,
    val knowledgePointId: String,
    val sourceKind: String,
    val sourceContextId: String,
    val sourceItemId: String,
    val sourceItemRevision: Int,
    val contentRevision: String,
    val outcome: String,
    val score: Double,
    val weight: Double,
    val difficulty: Double,
    val wrongAttemptCount: Int,
    val hintViewCount: Int,
    val explanationViewed: Boolean,
    val recordedAtEpochMillis: Long,
)

@Entity(tableName = "mastery_state")
internal data class MasteryStateEntity(
    @androidx.room.PrimaryKey
    val knowledgePointId: String,
    val score: Double,
    val accumulatedEvidenceWeight: Double,
    val lastPolicyVersion: Int,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "mastery_update_snapshot",
    indices = [
        Index(value = ["sourceContextId"]),
        Index(value = ["knowledgePointId", "createdAtEpochMillis"]),
    ],
)
internal data class MasteryUpdateSnapshotEntity(
    @androidx.room.PrimaryKey
    val snapshotId: String,
    val sourceContextId: String,
    val knowledgePointId: String,
    val beforeScore: Double,
    val afterScore: Double,
    val beforeEvidenceWeight: Double,
    val appliedEvidenceWeight: Double,
    val afterEvidenceWeight: Double,
    val policyVersion: Int,
    val createdAtEpochMillis: Long,
)

@Dao
internal interface LearningEvidenceDao {
    @Query("SELECT * FROM learning_evidence WHERE evidenceId = :evidenceId")
    suspend fun findEvidence(evidenceId: String): LearningEvidenceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvidence(entities: List<LearningEvidenceEntity>)

    @Query(
        """
        SELECT * FROM learning_evidence
        WHERE sourceContextId = :contextId
        ORDER BY recordedAtEpochMillis ASC, evidenceId ASC
        """,
    )
    suspend fun evidenceForContext(contextId: String): List<LearningEvidenceEntity>

    @Query(
        """
        SELECT knowledgePointId,
               COUNT(*) AS evidenceCount,
               MAX(recordedAtEpochMillis) AS lastEvidenceAtEpochMillis
        FROM learning_evidence
        WHERE knowledgePointId IN (:knowledgePointIds)
        GROUP BY knowledgePointId
        """,
    )
    suspend fun evidenceStats(knowledgePointIds: List<String>): List<KnowledgePointEvidenceStats>

    @Query("SELECT * FROM mastery_state WHERE knowledgePointId = :knowledgePointId")
    suspend fun findMasteryState(knowledgePointId: String): MasteryStateEntity?

    @Query("SELECT * FROM mastery_state WHERE knowledgePointId IN (:knowledgePointIds)")
    suspend fun masteryStates(knowledgePointIds: List<String>): List<MasteryStateEntity>

    @Upsert
    suspend fun upsertMasteryState(entity: MasteryStateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMasterySnapshot(entity: MasteryUpdateSnapshotEntity)

    @Query(
        """
        SELECT * FROM mastery_update_snapshot
        WHERE sourceContextId = :contextId
        ORDER BY knowledgePointId ASC
        """,
    )
    suspend fun masterySnapshotsForContext(contextId: String): List<MasteryUpdateSnapshotEntity>

    @Query(
        """
        SELECT * FROM mastery_update_snapshot
        WHERE knowledgePointId = :knowledgePointId
        ORDER BY createdAtEpochMillis ASC, snapshotId ASC
        """,
    )
    suspend fun masterySnapshotsForKnowledgePoint(knowledgePointId: String): List<MasteryUpdateSnapshotEntity>

    @Query("DELETE FROM learning_evidence")
    suspend fun clearEvidence()

    @Query("DELETE FROM mastery_update_snapshot")
    suspend fun clearMasterySnapshots()

    @Query("DELETE FROM mastery_state")
    suspend fun clearMasteryStates()
}

data class LearningEvidenceApplyResult(
    val evidence: List<LearningEvidence>,
    val masteryUpdates: List<MasteryUpdate>,
    val masteryPolicyVersion: Int,
)

data class KnowledgePointEvidenceStats(
    val knowledgePointId: String,
    val evidenceCount: Int,
    val lastEvidenceAtEpochMillis: Long?,
)

data class MasteryHistoryPoint(
    val sourceContextId: String,
    val update: MasteryUpdate,
    val createdAtEpochMillis: Long,
)

class LearningEvidenceStore internal constructor(
    private val database: SchoolLearningDatabase,
    private val masteryPolicy: MasteryPolicy = WeightedMasteryPolicy(),
    private val masteryPrior: MasteryPrior = MasteryPrior(),
) {
    private val dao: LearningEvidenceDao
        get() = database.learningEvidenceDao()

    suspend fun record(
        evidence: List<LearningEvidence>,
        recordedAtEpochMillis: Long = System.currentTimeMillis(),
    ): LearningEvidenceApplyResult = database.withTransaction {
        recordInsideTransaction(evidence, recordedAtEpochMillis)
    }

    internal suspend fun recordInsideTransaction(
        evidence: List<LearningEvidence>,
        recordedAtEpochMillis: Long,
    ): LearningEvidenceApplyResult {
        if (evidence.isEmpty()) {
            return LearningEvidenceApplyResult(emptyList(), emptyList(), masteryPolicy.version)
        }
        require(recordedAtEpochMillis >= 0L) { "recordedAtEpochMillis 不能小于 0" }
        val contexts = evidence.map { it.source.contextId }.distinct()
        require(contexts.size == 1) { "一次 evidence record 必须来自同一个 context" }

        val newEvidence = evidence.filter { dao.findEvidence(it.id) == null }
        if (newEvidence.isEmpty()) {
            return LearningEvidenceApplyResult(emptyList(), emptyList(), masteryPolicy.version)
        }

        dao.insertEvidence(newEvidence.map { it.toEntity(recordedAtEpochMillis) })
        val updates = newEvidence
            .groupBy(LearningEvidence::knowledgePointId)
            .toSortedMap(compareBy(KnowledgePointId::value))
            .map { (knowledgePointId, items) ->
                val current = dao.findMasteryState(knowledgePointId.value)?.toDomain()
                    ?: masteryPrior.stateFor(knowledgePointId)
                masteryPolicy.update(current, items)
            }

        val contextId = contexts.single()
        updates.forEach { update ->
            dao.upsertMasteryState(update.toStateEntity(recordedAtEpochMillis))
            dao.insertMasterySnapshot(
                update.toSnapshotEntity(
                    sourceContextId = contextId,
                    createdAtEpochMillis = recordedAtEpochMillis,
                ),
            )
        }
        return LearningEvidenceApplyResult(newEvidence, updates, masteryPolicy.version)
    }

    suspend fun masteryState(knowledgePointId: KnowledgePointId): MasteryState? =
        dao.findMasteryState(knowledgePointId.value)?.toDomain()

    suspend fun masteryStates(knowledgePointIds: Collection<KnowledgePointId>): Map<KnowledgePointId, MasteryState> {
        if (knowledgePointIds.isEmpty()) return emptyMap()
        return dao.masteryStates(knowledgePointIds.map(KnowledgePointId::value))
            .map(MasteryStateEntity::toDomain)
            .associateBy(MasteryState::knowledgePointId)
    }

    internal suspend fun evidenceForContext(contextId: String): List<LearningEvidence> =
        dao.evidenceForContext(contextId).map(LearningEvidenceEntity::toDomain)

    internal suspend fun masteryUpdatesForContext(contextId: String): List<MasteryUpdate> =
        dao.masterySnapshotsForContext(contextId).map(MasteryUpdateSnapshotEntity::toDomain)

    suspend fun masteryHistory(knowledgePointId: KnowledgePointId): List<MasteryHistoryPoint> =
        dao.masterySnapshotsForKnowledgePoint(knowledgePointId.value).map {
            MasteryHistoryPoint(it.sourceContextId, it.toDomain(), it.createdAtEpochMillis)
        }

    suspend fun clearAll() {
        database.withTransaction {
            clearInsideTransaction()
        }
    }

    internal suspend fun clearInsideTransaction() {
        dao.clearEvidence()
        dao.clearMasterySnapshots()
        dao.clearMasteryStates()
    }

    companion object {
        fun create(context: Context): LearningEvidenceStore =
            LearningEvidenceStore(SchoolLearningDatabase.get(context))
    }
}

private fun LearningEvidence.toEntity(recordedAtEpochMillis: Long): LearningEvidenceEntity =
    LearningEvidenceEntity(
        evidenceId = id,
        courseId = courseId,
        knowledgePointId = knowledgePointId.value,
        sourceKind = source.kind.name,
        sourceContextId = source.contextId,
        sourceItemId = source.itemId,
        sourceItemRevision = source.itemRevision,
        contentRevision = source.contentRevision,
        outcome = outcome.name,
        score = score,
        weight = weight,
        difficulty = difficulty.value,
        wrongAttemptCount = wrongAttemptCount,
        hintViewCount = hintViewCount,
        explanationViewed = explanationViewed,
        recordedAtEpochMillis = recordedAtEpochMillis,
    )

private fun LearningEvidenceEntity.toDomain(): LearningEvidence =
    LearningEvidence(
        id = evidenceId,
        courseId = courseId,
        knowledgePointId = KnowledgePointId(knowledgePointId),
        source = LearningEvidenceSource(
            kind = enumValueOf<LearningEvidenceSourceKind>(sourceKind),
            contextId = sourceContextId,
            itemId = sourceItemId,
            itemRevision = sourceItemRevision,
            contentRevision = contentRevision,
        ),
        outcome = enumValueOf<LearningEvidenceOutcome>(outcome),
        score = score,
        weight = weight,
        difficulty = Difficulty(difficulty),
        wrongAttemptCount = wrongAttemptCount,
        hintViewCount = hintViewCount,
        explanationViewed = explanationViewed,
    )

private fun MasteryStateEntity.toDomain(): MasteryState =
    MasteryState(
        knowledgePointId = KnowledgePointId(knowledgePointId),
        score = score,
        accumulatedEvidenceWeight = accumulatedEvidenceWeight,
    )

private fun MasteryUpdate.toStateEntity(updatedAtEpochMillis: Long): MasteryStateEntity =
    MasteryStateEntity(
        knowledgePointId = knowledgePointId.value,
        score = afterScore,
        accumulatedEvidenceWeight = afterEvidenceWeight,
        lastPolicyVersion = policyVersion,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

private fun MasteryUpdate.toSnapshotEntity(
    sourceContextId: String,
    createdAtEpochMillis: Long,
): MasteryUpdateSnapshotEntity =
    MasteryUpdateSnapshotEntity(
        snapshotId = "$sourceContextId:${knowledgePointId.value}:$createdAtEpochMillis",
        sourceContextId = sourceContextId,
        knowledgePointId = knowledgePointId.value,
        beforeScore = beforeScore,
        afterScore = afterScore,
        beforeEvidenceWeight = beforeEvidenceWeight,
        appliedEvidenceWeight = appliedEvidenceWeight,
        afterEvidenceWeight = afterEvidenceWeight,
        policyVersion = policyVersion,
        createdAtEpochMillis = createdAtEpochMillis,
    )

private fun MasteryUpdateSnapshotEntity.toDomain(): MasteryUpdate =
    MasteryUpdate(
        knowledgePointId = KnowledgePointId(knowledgePointId),
        beforeScore = beforeScore,
        afterScore = afterScore,
        beforeEvidenceWeight = beforeEvidenceWeight,
        appliedEvidenceWeight = appliedEvidenceWeight,
        afterEvidenceWeight = afterEvidenceWeight,
        policyVersion = policyVersion,
    )
