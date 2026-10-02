package com.majortomman.school.learning.advisor

import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.knowledge.KnowledgePointState

data class ReviewAdvice(
    val knowledgePointId: KnowledgePointId,
    val masteryScore: Double,
    val evidenceCount: Int,
    val lastEvidenceAtEpochMillis: Long?,
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
    ): LearningAdvice {
        require(reviewLimit >= 0) { "reviewLimit 不能小于 0" }
        val reviews = states.asSequence()
            .filter { state ->
                state.observed &&
                    state.masteryScore != null &&
                    state.masteryScore < reviewThreshold
            }
            .sortedWith(
                compareBy<KnowledgePointState> { it.masteryScore }
                    .thenBy { it.lastEvidenceAtEpochMillis ?: Long.MIN_VALUE }
                    .thenBy { it.id.value },
            )
            .take(reviewLimit)
            .map { state ->
                ReviewAdvice(
                    knowledgePointId = state.id,
                    masteryScore = requireNotNull(state.masteryScore),
                    evidenceCount = state.evidenceCount,
                    lastEvidenceAtEpochMillis = state.lastEvidenceAtEpochMillis,
                )
            }
            .toList()
        return LearningAdvice(reviews)
    }

    companion object {
        const val DEFAULT_REVIEW_THRESHOLD = 0.60
        const val DEFAULT_REVIEW_LIMIT = 3
    }
}
