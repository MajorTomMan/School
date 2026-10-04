package com.majortomman.school.learning.knowledge

import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.mastery.domain.MasteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgePointStateProjectorTest {
    @Test
    fun projectionKeepsCourseFlowOutOfKnowledgeState() {
        val observed = KnowledgePointId("number-line")
        val unseen = KnowledgePointId("opposite-number")
        val states = KnowledgePointStateProjector.project(
            ids = listOf(observed, unseen),
            mastery = mapOf(
                observed to MasteryState(
                    knowledgePointId = observed,
                    score = 0.72,
                    accumulatedEvidenceWeight = 3.0,
                ),
            ),
            evidence = mapOf(
                observed to KnowledgePointEvidenceSummary(
                    evidenceCount = 2,
                    wrongAttemptCount = 1,
                    lastEvidenceAtEpochMillis = 100L,
                ),
            ),
        )

        assertEquals(listOf(observed, unseen), states.map(KnowledgePointState::id))
        assertTrue(states[0].observed)
        assertEquals(0.72, states[0].masteryScore!!, 0.0001)
        assertEquals(2, states[0].evidenceCount)
        assertEquals(1, states[0].wrongAttemptCount)
        assertFalse(states[1].observed)
        assertNull(states[1].masteryScore)
        assertEquals(0.0, states[1].accumulatedEvidenceWeight, 0.0)
    }
}
