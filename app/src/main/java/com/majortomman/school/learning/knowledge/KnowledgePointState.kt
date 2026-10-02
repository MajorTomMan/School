package com.majortomman.school.learning.knowledge

import android.content.Context
import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.mastery.domain.MasteryState
import com.majortomman.school.learning.persistence.SchoolLearningDatabase

data class KnowledgePointEvidenceSummary(
    val evidenceCount: Int,
    val lastEvidenceAtEpochMillis: Long?,
) {
    init {
        require(evidenceCount >= 0) { "evidenceCount 不能小于 0" }
        require(lastEvidenceAtEpochMillis == null || lastEvidenceAtEpochMillis >= 0L) {
            "lastEvidenceAtEpochMillis 不能小于 0"
        }
    }
}

data class KnowledgePointState(
    val id: KnowledgePointId,
    val masteryScore: Double?,
    val accumulatedEvidenceWeight: Double,
    val evidenceCount: Int,
    val lastEvidenceAtEpochMillis: Long?,
) {
    val observed: Boolean
        get() = evidenceCount > 0
}

object KnowledgePointStateProjector {
    fun project(
        ids: Collection<KnowledgePointId>,
        mastery: Map<KnowledgePointId, MasteryState>,
        evidence: Map<KnowledgePointId, KnowledgePointEvidenceSummary>,
    ): List<KnowledgePointState> = ids.distinct().map { id ->
        val masteryState = mastery[id]
        val summary = evidence[id]
        KnowledgePointState(
            id = id,
            masteryScore = masteryState?.score,
            accumulatedEvidenceWeight = masteryState?.accumulatedEvidenceWeight ?: 0.0,
            evidenceCount = summary?.evidenceCount ?: 0,
            lastEvidenceAtEpochMillis = summary?.lastEvidenceAtEpochMillis,
        )
    }
}

/**
 * Read-only projection of learning state.
 *
 * It summarizes evidence and mastery only. It does not choose lessons, mutate CourseProgress,
 * emit navigation decisions or advance LessonRuntime.
 */
class KnowledgePointStateReader internal constructor(
    private val database: SchoolLearningDatabase,
) {
    suspend fun read(courseId: String, ids: Collection<KnowledgePointId>): List<KnowledgePointState> {
        require(courseId.isNotBlank()) { "courseId 不能为空" }
        val orderedIds = ids.distinct()
        if (orderedIds.isEmpty()) return emptyList()

        val dao = database.learningEvidenceDao()
        val rawIds = orderedIds.map(KnowledgePointId::value)
        val mastery = dao.masteryStates(courseId, rawIds)
            .associate { row ->
                val id = KnowledgePointId(row.knowledgePointId)
                id to MasteryState(id, row.score, row.accumulatedEvidenceWeight)
            }
        val evidence = dao.evidenceStats(courseId, rawIds)
            .associate { row ->
                KnowledgePointId(row.knowledgePointId) to KnowledgePointEvidenceSummary(
                    evidenceCount = row.evidenceCount,
                    lastEvidenceAtEpochMillis = row.lastEvidenceAtEpochMillis,
                )
            }
        return KnowledgePointStateProjector.project(orderedIds, mastery, evidence)
    }

    companion object {
        fun create(context: Context): KnowledgePointStateReader =
            KnowledgePointStateReader(SchoolLearningDatabase.get(context))
    }
}
