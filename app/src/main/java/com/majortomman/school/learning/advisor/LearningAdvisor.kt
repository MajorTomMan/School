package com.majortomman.school.learning.advisor

import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.knowledge.KnowledgePointState
import kotlin.math.min

data class ReviewAdvice(
    val knowledgePointId: KnowledgePointId,
    val masteryScore: Double,
    val evidenceCount: Int,
    val wrongAttemptCount: Int,
    val lastEvidenceAtEpochMillis: Long?,
    val priorityScore: Double,
)

data class LearningAdvice(
    val reviews: List<ReviewAdvice>,
)

class LearningAdvisor(
    private val reviewThreshold: Double = DEFAULT_REVIEW_THRESHOLD,
) {
    init {
        require(reviewThreshold.isFinite() && reviewThreshold in 0.0..1.0) {
            "reviewThreshold 必须位于 0.0 到 1.0"
        }
    }

    fun advise(
        states: Collection<KnowledgePointState>,
        reviewLimit: Int = DEFAULT_REVIEW_LIMIT,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): LearningAdvice {
        require(reviewLimit >= 0) { "reviewLimit 不能小于 0" }
        require(nowEpochMillis >= 0L) { "nowEpochMillis 不能小于 0" }

        val reviews = states.asSequence()
            .filter { state ->
                state.observed &&
                    state.masteryScore != null &&
                    state.masteryScore < reviewThreshold
            }
            .map { state ->
                ReviewAdvice(
                    knowledgePointId = state.id,
                    masteryScore = requireNotNull(state.masteryScore),
                    evidenceCount = state.evidenceCount,
                    wrongAttemptCount = state.wrongAttemptCount,
                    lastEvidenceAtEpochMillis = state.lastEvidenceAtEpochMillis,
                    priorityScore = priorityScore(state, nowEpochMillis),
                )
            }
            .sortedWith(
                compareByDescending<ReviewAdvice> { it.priorityScore }
                    .thenBy { it.masteryScore }
                    .thenByDescending { it.wrongAttemptCount }
                    .thenBy { it.lastEvidenceAtEpochMillis ?: Long.MIN_VALUE }
                    .thenBy { it.knowledgePointId.value },
            )
            .take(reviewLimit)
            .toList()
        return LearningAdvice(reviews)
    }

    private fun priorityScore(state: KnowledgePointState, nowEpochMillis: Long): Double {
        val mastery = requireNotNull(state.masteryScore)
        val weakness = ((reviewThreshold - mastery) / reviewThreshold).coerceIn(0.0, 1.0)
        val wrongAttemptPressure = min(state.wrongAttemptCount / 5.0, 1.0)
        val ageMillis = state.lastEvidenceAtEpochMillis?.let { (nowEpochMillis - it).coerceAtLeast(0L) } ?: 0L
        val staleness = min(ageMillis.toDouble() / REVIEW_STALE_WINDOW_MILLIS.toDouble(), 1.0)
        return weakness * 0.65 + wrongAttemptPressure * 0.20 + staleness * 0.15
    }

    companion object {
        const val DEFAULT_REVIEW_THRESHOLD = 0.60
        const val DEFAULT_REVIEW_LIMIT = 3
        private const val REVIEW_STALE_WINDOW_MILLIS = 30L * 24L * 60L * 60L * 1_000L
    }
}
