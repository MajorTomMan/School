package com.majortomman.school.learning.knowledge

import android.content.Context
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.persistence.SchoolLearningDatabase

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

/**
 * Read-only projection of learning state.
 *
 * This layer summarizes evidence and mastery. It does not choose lessons, mutate CourseProgress
 * or advance LessonRuntime.
 */
class KnowledgePointStateReader internal constructor(
    private val database: SchoolLearningDatabase,
) {
    suspend fun read(ids: Collection<KnowledgePointId>): List<KnowledgePointState> {
        val orderedIds = ids.distinct()
        if (orderedIds.isEmpty()) return emptyList()

        val dao = database.learningEvidenceDao()
        val rawIds = orderedIds.map(KnowledgePointId::value)
        val mastery = dao.masteryStates(rawIds).associateBy { it.knowledgePointId }
        val stats = dao.evidenceStats(rawIds).associateBy { it.knowledgePointId }

        return orderedIds.map { id ->
            val masteryState = mastery[id.value]
            val evidenceStats = stats[id.value]
            KnowledgePointState(
                id = id,
                masteryScore = masteryState?.score,
                accumulatedEvidenceWeight = masteryState?.accumulatedEvidenceWeight ?: 0.0,
                evidenceCount = evidenceStats?.evidenceCount ?: 0,
                lastEvidenceAtEpochMillis = evidenceStats?.lastEvidenceAtEpochMillis,
            )
        }
    }

    companion object {
        fun create(context: Context): KnowledgePointStateReader =
            KnowledgePointStateReader(SchoolLearningDatabase.get(context))
    }
}
