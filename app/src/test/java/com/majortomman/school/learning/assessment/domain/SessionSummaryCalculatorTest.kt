package com.majortomman.school.learning.assessment.domain

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSummaryCalculatorTest {
    private val sessionId = SessionId("session-1")
    private val knowledgePointId = KnowledgePointId("number-line")

    @Test
    fun `summarizes first correct recovered wrong skipped and wrong submissions`() {
        val questions = (1..5).map(::question)
        val questionSet = QuestionSetDefinition(
            id = QuestionSetId("set-1"),
            title = "数轴例题",
            questions = questions,
        )
        val attempts = listOf(
            attempt("q1-a1", questions[0], 1, JudgeOutcome.CORRECT, 100L),
            attempt("q2-a1", questions[1], 1, JudgeOutcome.INCORRECT, 200L),
            attempt("q2-a2", questions[1], 2, JudgeOutcome.INCORRECT, 201L),
            attempt("q2-a3", questions[1], 3, JudgeOutcome.CORRECT, 202L),
            attempt("q3-a1", questions[2], 1, JudgeOutcome.INCORRECT, 300L),
            attempt("q3-a2", questions[2], 2, JudgeOutcome.CORRECT, 301L),
            attempt("q4-a1", questions[3], 1, JudgeOutcome.INCORRECT, 400L),
        )
        val events = listOf(
            QuestionSkipped(sessionId, questions[4].key, 500L),
        )

        val summary = SessionSummaryCalculator.summarize(
            sessionId = sessionId,
            questionSet = questionSet,
            attempts = attempts,
            events = events,
        )

        assertEquals(5, summary.totalQuestionCount)
        assertEquals(4, summary.attemptedQuestionCount)
        assertEquals(1, summary.firstCorrectCount)
        assertEquals(2, summary.recoveredCorrectCount)
        assertEquals(3, summary.finalCorrectCount)
        assertEquals(1, summary.finalWrongCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(0, summary.unansweredCount)
        assertEquals(3, summary.wrongQuestionCount)
        assertEquals(4, summary.wrongSubmissionCount)
        assertEquals(0.20, summary.firstCorrectRate, 0.0001)
        assertEquals(0.60, summary.finalCorrectRate, 0.0001)
        assertEquals(0.80, summary.completionRate, 0.0001)
        assertEquals(0.25, summary.firstAttemptAccuracyAmongAttempted, 0.0001)
    }

    @Test
    fun `invalid input is recorded but does not count as a wrong or valid attempt`() {
        val question = question(1)
        val questionSet = questionSet(question)
        val attempts = listOf(
            attempt("a1", question, 1, JudgeOutcome.INVALID_INPUT, 100L),
            attempt("a2", question, 2, JudgeOutcome.CORRECT, 101L),
        )

        val result = SessionSummaryCalculator.summarize(
            sessionId,
            questionSet,
            attempts,
            emptyList(),
        ).questionResults.single()

        assertEquals(QuestionCompletionStatus.FIRST_TRY_CORRECT, result.status)
        assertEquals(2, result.totalSubmissionCount)
        assertEquals(1, result.validAttemptCount)
        assertEquals(1, result.invalidSubmissionCount)
        assertEquals(0, result.wrongAttemptCount)
        assertTrue(result.firstAttemptCorrect)
    }

    @Test
    fun `answering a previously skipped question removes it from current skipped count`() {
        val question = question(1)
        val questionSet = questionSet(question)
        val attempts = listOf(
            attempt("a1", question, 1, JudgeOutcome.CORRECT, 200L),
        )
        val events = listOf(
            QuestionSkipped(sessionId, question.key, 100L),
        )

        val result = SessionSummaryCalculator.summarize(
            sessionId,
            questionSet,
            attempts,
            events,
        ).questionResults.single()

        assertFalse(result.currentlySkipped)
        assertTrue(result.wasEverSkipped)
        assertEquals(QuestionCompletionStatus.FIRST_TRY_CORRECT, result.status)
    }

    @Test
    fun `submissions after the first correct result cannot pollute the locked result`() {
        val question = question(1)
        val questionSet = questionSet(question)
        val attempts = listOf(
            attempt("a1", question, 1, JudgeOutcome.CORRECT, 100L),
            attempt("a2", question, 2, JudgeOutcome.INCORRECT, 101L),
        )

        val result = SessionSummaryCalculator.summarize(
            sessionId,
            questionSet,
            attempts,
            emptyList(),
        ).questionResults.single()

        assertEquals(QuestionCompletionStatus.FIRST_TRY_CORRECT, result.status)
        assertEquals(1, result.totalSubmissionCount)
        assertEquals(0, result.wrongAttemptCount)
    }

    private fun question(index: Int): QuestionDefinition = QuestionDefinition(
        key = QuestionKey(QuestionId("q-$index"), revision = 1),
        number = "例$index",
        inputSpec = AnswerInputSpec.Integer,
        answerRule = AnswerRule.ExactInteger(BigInteger.valueOf(index.toLong())),
        knowledgeBindings = listOf(KnowledgeBinding(knowledgePointId, weight = 1.0)),
        difficulty = Difficulty(0.4),
    )

    private fun questionSet(question: QuestionDefinition): QuestionSetDefinition =
        QuestionSetDefinition(
            id = QuestionSetId("set-1"),
            title = "测试题组",
            questions = listOf(question),
        )

    private fun attempt(
        id: String,
        question: QuestionDefinition,
        sequence: Int,
        outcome: JudgeOutcome,
        timestamp: Long,
    ): AttemptRecord = AttemptRecord(
        id = AttemptId(id),
        sessionId = sessionId,
        questionKey = question.key,
        submissionSequence = sequence,
        answer = UserAnswer.Text(sequence.toString()),
        result = JudgeResult(outcome = outcome),
        submittedAtEpochMillis = timestamp,
    )
}
