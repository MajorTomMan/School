package com.majortomman.school.learning.advisor

import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.knowledge.KnowledgePointState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningAdvisorTest {
    private val advisor = LearningAdvisor(reviewThreshold = 0.60)

    @Test
    fun onlyObservedWeakKnowledgePointsBecomeReviewAdvice() {
        val advice = advisor.advise(
            states = listOf(
                state("weak", mastery = 0.40, evidenceCount = 3, lastEvidenceAt = 200L),
                state("strong", mastery = 0.80, evidenceCount = 4, lastEvidenceAt = 300L),
                state("unseen", mastery = null, evidenceCount = 0, lastEvidenceAt = null),
                state("boundary", mastery = 0.60, evidenceCount = 2, lastEvidenceAt = 100L),
            ),
        )

        assertEquals(listOf("weak"), advice.reviews.map { it.knowledgePointId.value })
    }

    @Test
    fun reviewsAreDeterministicAndWeakestFirst() {
        val advice = advisor.advise(
            states = listOf(
                state("recent", mastery = 0.30, evidenceCount = 2, lastEvidenceAt = 300L),
                state("older", mastery = 0.30, evidenceCount = 2, lastEvidenceAt = 100L),
                state("weakest", mastery = 0.10, evidenceCount = 1, lastEvidenceAt = 400L),
            ),
            reviewLimit = 2,
            nowEpochMillis = 500L,
        )

        assertEquals(listOf("weakest", "older"), advice.reviews.map { it.knowledgePointId.value })
    }

    @Test
    fun wrongAttemptsRaiseReviewPriorityForEqualMastery() {
        val advice = advisor.advise(
            states = listOf(
                state("clean", mastery = 0.40, evidenceCount = 3, lastEvidenceAt = 200L),
                state("struggling", mastery = 0.40, evidenceCount = 3, lastEvidenceAt = 300L, wrongAttemptCount = 4),
            ),
            nowEpochMillis = 500L,
        )

        assertEquals("struggling", advice.reviews.first().knowledgePointId.value)
        assertTrue(advice.reviews.first().priorityScore > advice.reviews.last().priorityScore)
    }

    @Test
    fun advisorNeverInventsAdviceWithoutEvidence() {
        val advice = advisor.advise(
            states = listOf(state("unseen", mastery = null, evidenceCount = 0, lastEvidenceAt = null)),
        )

        assertTrue(advice.reviews.isEmpty())
    }

    private fun state(
        id: String,
        mastery: Double?,
        evidenceCount: Int,
        lastEvidenceAt: Long?,
        wrongAttemptCount: Int = 0,
    ): KnowledgePointState = KnowledgePointState(
        id = KnowledgePointId(id),
        masteryScore = mastery,
        accumulatedEvidenceWeight = if (mastery == null) 0.0 else evidenceCount.toDouble(),
        evidenceCount = evidenceCount,
        wrongAttemptCount = wrongAttemptCount,
        lastEvidenceAtEpochMillis = lastEvidenceAt,
    )
}
