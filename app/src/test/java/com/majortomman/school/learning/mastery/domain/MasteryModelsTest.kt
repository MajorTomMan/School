package com.majortomman.school.learning.mastery.domain

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.KnowledgeBinding
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionCompletionStatus
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionId
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.QuestionResult
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MasteryModelsTest {
    private val knowledgePointId = KnowledgePointId("number-line")
    private val sessionId = SessionId("session-1")
    private val questionKey = QuestionKey(QuestionId("q-1"), revision = 1)

    @Test
    fun `evidence score reflects wrong attempts hints and explanation`() {
        val recovered = result(
            status = QuestionCompletionStatus.RECOVERED_CORRECT,
            wrongAttemptCount = 2,
            hintViewCount = 1,
        )
        val withExplanation = recovered.copy(explanationViewed = true)

        assertEquals(0.51, DefaultMasteryEvidenceScorer.score(recovered)!!, 0.0001)
        assertEquals(0.35, DefaultMasteryEvidenceScorer.score(withExplanation)!!, 0.0001)
        assertNull(
            DefaultMasteryEvidenceScorer.score(
                result(status = QuestionCompletionStatus.SKIPPED),
            ),
        )
    }

    @Test
    fun `factory creates one evidence item for every knowledge binding`() {
        val secondKnowledgePoint = KnowledgePointId("opposite-numbers")
        val question = question(
            bindings = listOf(
                KnowledgeBinding(knowledgePointId, 1.0),
                KnowledgeBinding(secondKnowledgePoint, 0.4),
            ),
        )
        val questionSet = QuestionSetDefinition(
            id = QuestionSetId("set-1"),
            title = "数轴例题",
            questions = listOf(question),
        )
        val summary = SessionSummary(
            sessionId = sessionId,
            questionSetId = questionSet.id,
            questionResults = listOf(
                result(
                    status = QuestionCompletionStatus.RECOVERED_CORRECT,
                    wrongAttemptCount = 1,
                ),
            ),
        )

        val evidence = MasteryEvidenceFactory.create(questionSet, summary)

        assertEquals(2, evidence.size)
        assertEquals(setOf(knowledgePointId, secondKnowledgePoint), evidence.map { it.knowledgePointId }.toSet())
        assertEquals(0.75, evidence.first { it.knowledgePointId == knowledgePointId }.score, 0.0001)
        assertEquals(0.4, evidence.first { it.knowledgePointId == secondKnowledgePoint }.weight, 0.0001)
    }

    @Test
    fun `weighted policy smooths new evidence instead of replacing mastery`() {
        val policy = WeightedMasteryPolicy(version = 1)
        val current = MasteryState(
            knowledgePointId = knowledgePointId,
            score = 0.60,
            accumulatedEvidenceWeight = 4.0,
        )
        val evidence = evidence(score = 1.0, difficulty = Difficulty(0.5))

        val update = policy.update(current, listOf(evidence))

        assertEquals(0.60, update.beforeScore, 0.0001)
        assertEquals(0.68, update.afterScore, 0.0001)
        assertEquals(1.0, update.appliedEvidenceWeight, 0.0001)
        assertEquals(5.0, update.afterEvidenceWeight, 0.0001)
        assertEquals(1, update.policyVersion)
    }

    @Test
    fun `final wrong evidence lowers mastery without resetting it to zero`() {
        val policy = WeightedMasteryPolicy()
        val current = MasteryState(
            knowledgePointId = knowledgePointId,
            score = 0.80,
            accumulatedEvidenceWeight = 4.0,
        )

        val update = policy.update(
            current,
            listOf(evidence(score = 0.0, difficulty = Difficulty(0.5))),
        )

        assertEquals(0.64, update.afterScore, 0.0001)
    }

    private fun question(
        bindings: List<KnowledgeBinding> = listOf(KnowledgeBinding(knowledgePointId, 1.0)),
    ): QuestionDefinition = QuestionDefinition(
        key = questionKey,
        number = "例1",
        inputSpec = AnswerInputSpec.Integer,
        answerRule = AnswerRule.ExactInteger(2L),
        knowledgeBindings = bindings,
        difficulty = Difficulty(0.5),
    )

    private fun result(
        status: QuestionCompletionStatus,
        wrongAttemptCount: Int = 0,
        hintViewCount: Int = 0,
        explanationViewed: Boolean = false,
    ): QuestionResult = QuestionResult(
        questionKey = questionKey,
        status = status,
        totalSubmissionCount = when (status) {
            QuestionCompletionStatus.UNANSWERED,
            QuestionCompletionStatus.SKIPPED,
            -> 0

            else -> wrongAttemptCount + if (status == QuestionCompletionStatus.FINAL_INCORRECT) 0 else 1
        },
        validAttemptCount = when (status) {
            QuestionCompletionStatus.UNANSWERED,
            QuestionCompletionStatus.SKIPPED,
            -> 0

            else -> wrongAttemptCount + if (status == QuestionCompletionStatus.FINAL_INCORRECT) 0 else 1
        },
        invalidSubmissionCount = 0,
        wrongAttemptCount = wrongAttemptCount,
        hintViewCount = hintViewCount,
        explanationViewed = explanationViewed,
        wasEverSkipped = status == QuestionCompletionStatus.SKIPPED,
    )

    private fun evidence(
        score: Double,
        difficulty: Difficulty,
    ): MasteryEvidence = MasteryEvidence(
        knowledgePointId = knowledgePointId,
        questionId = questionKey.id,
        questionRevision = questionKey.revision,
        sessionId = sessionId,
        outcome = if (score == 0.0) {
            MasteryEvidenceOutcome.FINAL_INCORRECT
        } else {
            MasteryEvidenceOutcome.FIRST_TRY_CORRECT
        },
        score = score,
        weight = 1.0,
        difficulty = difficulty,
        wrongAttemptCount = if (score == 0.0) 1 else 0,
        hintViewCount = 0,
        explanationViewed = false,
    )
}
