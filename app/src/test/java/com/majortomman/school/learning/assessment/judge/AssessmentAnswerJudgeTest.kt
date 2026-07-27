package com.majortomman.school.learning.assessment.judge

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.JudgeOutcome
import com.majortomman.school.learning.assessment.domain.KnowledgeBinding
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionId
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.RationalValue
import com.majortomman.school.learning.assessment.domain.UserAnswer
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class AssessmentAnswerJudgeTest {
    @Test
    fun acceptsFullWidthIntegerAndNormalizesSign() {
        val result = DefaultAssessmentAnswerJudge.judge(
            question(AnswerInputSpec.Integer, AnswerRule.ExactInteger(-12)),
            UserAnswer.Text(" －１２ "),
        )

        assertEquals(JudgeOutcome.CORRECT, result.outcome)
        assertEquals("-12", result.normalizedAnswer)
    }

    @Test
    fun treatsEquivalentFractionAndDecimalAsSameRational() {
        val definition = question(
            AnswerInputSpec.Rational(allowDecimal = true),
            AnswerRule.RationalEquivalent(RationalValue.of(1, 2)),
        )

        assertEquals(
            JudgeOutcome.CORRECT,
            DefaultAssessmentAnswerJudge.judge(definition, UserAnswer.Text("2/4")).outcome,
        )
        assertEquals(
            JudgeOutcome.CORRECT,
            DefaultAssessmentAnswerJudge.judge(definition, UserAnswer.Text("0.5")).outcome,
        )
    }

    @Test
    fun appliesDecimalTolerance() {
        val definition = question(
            AnswerInputSpec.Decimal(),
            AnswerRule.Decimal(expected = 3.14, tolerance = 0.01),
        )

        assertEquals(
            JudgeOutcome.CORRECT,
            DefaultAssessmentAnswerJudge.judge(definition, UserAnswer.Text("3.149")).outcome,
        )
        assertEquals(
            JudgeOutcome.INCORRECT,
            DefaultAssessmentAnswerJudge.judge(definition, UserAnswer.Text("3.16")).outcome,
        )
    }

    @Test
    fun reportsPartiallyCorrectCoordinate() {
        val definition = question(
            AnswerInputSpec.Coordinate,
            AnswerRule.Coordinate(
                expectedX = RationalValue.of(-2, 1),
                expectedY = RationalValue.of(3, 1),
            ),
        )

        val result = DefaultAssessmentAnswerJudge.judge(
            definition,
            UserAnswer.Coordinate(rawX = "-2", rawY = "4"),
        )

        assertEquals(JudgeOutcome.PARTIALLY_CORRECT, result.outcome)
        assertEquals("coordinate_y_incorrect", result.feedbackCode)
    }

    @Test
    fun invalidInputDoesNotBecomeWrongAnswer() {
        val result = DefaultAssessmentAnswerJudge.judge(
            question(AnswerInputSpec.Integer, AnswerRule.ExactInteger(2)),
            UserAnswer.Text("-"),
        )

        assertEquals(JudgeOutcome.INVALID_INPUT, result.outcome)
        assertEquals(false, result.countsAsWrongAttempt)
    }

    @Test
    fun rejectsUnknownChoiceAsInvalidInput() {
        val result = DefaultAssessmentAnswerJudge.judge(
            question(
                AnswerInputSpec.SingleChoice(listOf("a", "b")),
                AnswerRule.SingleChoice("a"),
            ),
            UserAnswer.Choice("c"),
        )

        assertEquals(JudgeOutcome.INVALID_INPUT, result.outcome)
    }

    private fun question(
        input: AnswerInputSpec,
        rule: AnswerRule,
    ): QuestionDefinition = QuestionDefinition(
        key = QuestionKey(QuestionId("question-1"), 1),
        number = "1",
        inputSpec = input,
        answerRule = rule,
        knowledgeBindings = listOf(KnowledgeBinding(KnowledgePointId("knowledge-1"), 1.0)),
        difficulty = Difficulty(0.5),
    )
}
