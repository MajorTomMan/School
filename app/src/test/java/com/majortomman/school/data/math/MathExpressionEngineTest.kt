package com.majortomman.school.data.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExpressionEngineTest {
    @Test
    fun fractionsAndDecimalsAreTreatedAsTheSameRationalValue() {
        val question = question(type = MathQuestionType.NUMERIC_INPUT, spec = MathAnswerSpec.RationalValue("1/2"), canonical = "1/2")
        val evaluation = MathExpressionEngine.evaluate(question, "0.5")

        assertTrue(evaluation.correct)
        assertEquals("1/2", evaluation.normalizedAnswer)
    }

    @Test
    fun equivalentExpressionsAreAccepted() {
        val question = question(type = MathQuestionType.EXPRESSION_INPUT, spec = MathAnswerSpec.EquivalentExpression("2*x+6"), canonical = "2x+6")

        assertTrue(MathExpressionEngine.evaluate(question, "2(x+3)").correct)
        assertTrue(MathExpressionEngine.evaluate(question, "6+2x").correct)
        assertFalse(MathExpressionEngine.evaluate(question, "2x+3").correct)
    }

    @Test
    fun linearEquationChecksTheFinalSolution() {
        val question = question(type = MathQuestionType.NUMERIC_INPUT, spec = MathAnswerSpec.LinearEquation("2*x+3=9", "3"), canonical = "x=3")

        assertTrue(MathExpressionEngine.evaluate(question, "x=3").correct)
        assertFalse(MathExpressionEngine.evaluate(question, "x=-3").correct)
    }

    @Test
    fun stepSequenceRejectsNonEquivalentEquation() {
        val question = question(type = MathQuestionType.STEP_BY_STEP, spec = MathAnswerSpec.StepSequence("2*x+3=9", "3"), canonical = "x=3")
        val valid = MathExpressionEngine.evaluate(question, "2x=6\nx=3")
        val invalid = MathExpressionEngine.evaluate(question, "2x=12\nx=6")

        assertTrue(valid.correct)
        assertFalse(invalid.correct)
    }

    @Test
    fun rationalSetRequiresAllSolutions() {
        val question = question(type = MathQuestionType.EXPRESSION_INPUT, spec = MathAnswerSpec.RationalSet(listOf("6", "-6")), canonical = "6,-6")
        val incomplete = MathExpressionEngine.evaluate(question, "x=6")
        val complete = MathExpressionEngine.evaluate(question, "x=6 或 x=-6")

        assertFalse(incomplete.correct)
        assertTrue(complete.correct)
    }

    private fun question(type: MathQuestionType, spec: MathAnswerSpec, canonical: String): MathQuestion = MathQuestion(
        id = "test",
        templateId = "test",
        textbookKey = "test",
        lessonId = "test",
        knowledgePointId = "test",
        type = type,
        difficulty = MathDifficulty.BASIC,
        source = MathQuestionSource.SYSTEM_TEMPLATE,
        prompt = "input",
        answerSpec = spec,
        canonicalAnswer = canonical,
        hints = emptyList(),
        explanation = "",
    )
}
