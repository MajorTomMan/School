package com.majortomman.school.learning.assessment.persistence

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.assessment.domain.AttemptId
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.HintViewed
import com.majortomman.school.learning.assessment.domain.JudgeOutcome
import com.majortomman.school.learning.assessment.domain.JudgeResult
import com.majortomman.school.learning.assessment.domain.KnowledgeBinding
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionId
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.assessment.domain.QuestionSkipped
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.UserAnswer
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentSettlementPlannerTest {
    private val sessionId = SessionId("session-1")
    private val knowledgePointId = KnowledgePointId("number-line")
    private val questionOne = question("q-1", "1")
    private val questionTwo = question("q-2", "2")
    private val questionSet = QuestionSetDefinition(
        id = QuestionSetId("set-1"),
        title = "数轴练习",
        questions = listOf(questionOne, questionTwo),
    )

    @Test
    fun plansWrongCountsEvidenceAndSmoothedMasteryFromFacts() {
        val attempts = listOf(
            attempt("a-1", questionOne.key, 1, JudgeOutcome.INCORRECT, 10L),
            attempt("a-2", questionOne.key, 2, JudgeOutcome.INCORRECT, 20L),
            attempt("a-3", questionOne.key, 3, JudgeOutcome.CORRECT, 30L),
        )
        val events = listOf(
            HintViewed(sessionId, questionOne.key, "hint-1", 15L),
            QuestionSkipped(sessionId, questionTwo.key, 40L),
        )

        val plan = AssessmentSettlementPlanner().plan(
            sessionId = sessionId,
            questionSet = questionSet,
            attempts = attempts,
            events = events,
            currentMastery = emptyMap(),
        )

        assertEquals(2, plan.summary.totalQuestionCount)
        assertEquals(1, plan.summary.recoveredCorrectCount)
        assertEquals(1, plan.summary.skippedCount)
        assertEquals(1, plan.summary.wrongQuestionCount)
        assertEquals(2, plan.summary.wrongSubmissionCount)
        assertEquals(1, plan.summary.hintViewCount)
        assertEquals(1, plan.evidence.size)
        assertClose(0.51, plan.evidence.single().score)

        val update = plan.masteryUpdates.single()
        assertClose(0.5, update.beforeScore)
        assertClose(1.0, update.beforeEvidenceWeight)
        assertClose(0.505, update.afterScore)
        assertTrue(update.afterScore > 0.0)
        assertTrue(update.afterScore < 1.0)
    }

    @Test
    fun skippedAndUnansweredQuestionsDoNotCreateMasteryEvidence() {
        val plan = AssessmentSettlementPlanner().plan(
            sessionId = sessionId,
            questionSet = questionSet,
            attempts = emptyList(),
            events = listOf(QuestionSkipped(sessionId, questionOne.key, 10L)),
            currentMastery = emptyMap(),
        )

        assertEquals(1, plan.summary.skippedCount)
        assertEquals(1, plan.summary.unansweredCount)
        assertTrue(plan.evidence.isEmpty())
        assertTrue(plan.masteryUpdates.isEmpty())
    }

    @Test
    fun attemptAndEventEntitiesRoundTripWithoutLosingRawInput() {
        val coordinateAttempt = AttemptRecord(
            id = AttemptId("coordinate-attempt"),
            sessionId = sessionId,
            questionKey = questionOne.key,
            submissionSequence = 1,
            answer = UserAnswer.Coordinate(rawX = " -1/2 ", rawY = ""),
            result = JudgeResult(JudgeOutcome.INVALID_INPUT, feedbackCode = "missing_y"),
            submittedAtEpochMillis = 100L,
        )
        val restoredAttempt = coordinateAttempt.toEntity().toDomain()
        assertEquals(coordinateAttempt, restoredAttempt)

        val persistedEvent = PersistedLearningEvent(
            id = "event-1",
            event = HintViewed(sessionId, questionOne.key, "hint-1", 101L),
        )
        assertEquals(persistedEvent, persistedEvent.toEntity().toDomain())
    }

    @Test
    fun settlementAggregateVerificationDetectsCorruption() {
        val plan = AssessmentSettlementPlanner().plan(
            sessionId = sessionId,
            questionSet = questionSet,
            attempts = listOf(attempt("a-1", questionOne.key, 1, JudgeOutcome.CORRECT, 10L)),
            events = emptyList(),
            currentMastery = emptyMap(),
        )
        val valid = plan.summary.toSettlementEntity(plan.masteryPolicyVersion, 20L)
        valid.verifyAgainst(plan.summary)

        val corrupted = valid.copy(wrongSubmissionCount = valid.wrongSubmissionCount + 1)
        val error = runCatching { corrupted.verifyAgainst(plan.summary) }.exceptionOrNull()
        assertFalse(error == null)
        assertTrue(error?.message.orEmpty().contains("不一致"))
    }

    private fun question(id: String, number: String): QuestionDefinition = QuestionDefinition(
        key = QuestionKey(QuestionId(id), revision = 1),
        number = number,
        inputSpec = AnswerInputSpec.Integer,
        answerRule = AnswerRule.ExactInteger(2L),
        knowledgeBindings = listOf(KnowledgeBinding(knowledgePointId, weight = 1.0)),
        difficulty = Difficulty(0.5),
    )

    private fun attempt(
        id: String,
        questionKey: QuestionKey,
        sequence: Int,
        outcome: JudgeOutcome,
        submittedAt: Long,
    ): AttemptRecord = AttemptRecord(
        id = AttemptId(id),
        sessionId = sessionId,
        questionKey = questionKey,
        submissionSequence = sequence,
        answer = UserAnswer.Text("2"),
        result = JudgeResult(outcome, normalizedAnswer = "2"),
        submittedAtEpochMillis = submittedAt,
    )

    private fun assertClose(expected: Double, actual: Double) {
        assertTrue("expected=$expected actual=$actual", abs(expected - actual) < 1e-9)
    }
}
