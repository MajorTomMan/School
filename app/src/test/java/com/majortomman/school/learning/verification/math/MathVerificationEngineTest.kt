package com.majortomman.school.learning.verification.math

import com.majortomman.school.learning.verification.core.VerificationRequest
import com.majortomman.school.learning.verification.core.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathVerificationEngineTest {
    @Test
    fun evaluatesExactNumericExpressionWithSteps() {
        val result = MathVerificationEngine.verify(VerificationRequest("2*(3+4)"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("math.numeric-expression", result.problemType.id)
        assertEquals("14", result.answer?.display)
        assertTrue(result.steps.isNotEmpty())
    }

    @Test
    fun keepsRationalArithmeticExact() {
        val result = MathVerificationEngine.verify(VerificationRequest("1/3+1/6"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("1/2", result.answer?.display)
    }

    @Test
    fun evaluatesWhitelistedFunctionWithoutTurningItIntoGraph() {
        val result = MathVerificationEngine.verify(VerificationRequest("sin(0)"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("math.numeric-expression", result.problemType.id)
        assertEquals("0", result.answer?.display)
        assertTrue(result.visualizations.isEmpty())
    }

    @Test
    fun expandsPolynomialAndProducesGraphRequest() {
        val result = MathVerificationEngine.verify(VerificationRequest("(x+2)(x+3)"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("math.polynomial-expression", result.problemType.id)
        assertEquals("x^2 + 5x + 6", result.answer?.display)
        assertTrue(result.steps.any { it.rule.value == "EXPAND_AND_COMBINE" })
        assertEquals("mathematics.function.graph", result.visualizations.single().renderer)
    }

    @Test
    fun solvesLinearEquationWithStructuredRules() {
        val result = MathVerificationEngine.verify(VerificationRequest("2x+3=9"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("math.linear-equation", result.problemType.id)
        assertEquals("x = 3", result.answer?.display)
        assertTrue(result.steps.any { it.rule.value == "TRANSPOSE_TERMS" })
        assertTrue(result.steps.any { it.rule.value == "DIVIDE_BOTH_SIDES" })
    }

    @Test
    fun solvesQuadraticEquationLocally() {
        val result = MathVerificationEngine.verify(VerificationRequest("x^2-5x+6=0"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("math.quadratic-equation", result.problemType.id)
        assertEquals("x = 2，x = 3", result.answer?.display)
        assertTrue(result.steps.any { it.rule.value == "CALCULATE_DISCRIMINANT" })
        assertTrue(result.steps.any { it.rule.value == "APPLY_QUADRATIC_FORMULA" })
    }

    @Test
    fun recognizesGeneralFunctionAndBuildsGraph() {
        val result = MathVerificationEngine.verify(VerificationRequest("sin(x)"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("math.function", result.problemType.id)
        assertEquals("y = sin(x)", result.answer?.display)
        assertEquals("mathematics.function.graph", result.visualizations.single().renderer)
    }

    @Test
    fun fallsBackToSafeFunctionParserForExponentialGraph() {
        val result = MathVerificationEngine.verify(VerificationRequest("e^x"))

        assertEquals(VerificationStatus.SUCCESS, result.status)
        assertEquals("math.function", result.problemType.id)
        assertEquals("y = e^x", result.answer?.display)
        assertEquals("mathematics.function.graph", result.visualizations.single().renderer)
    }

    @Test
    fun rejectsHigherDegreeAndMultipleVariableAlgebraWithinCurrentScope() {
        val cubic = MathVerificationEngine.verify(VerificationRequest("x^3-1=0"))
        val multipleVariables = MathVerificationEngine.verify(VerificationRequest("x+y"))

        assertEquals(VerificationStatus.UNSUPPORTED, cubic.status)
        assertEquals(VerificationStatus.UNSUPPORTED, multipleVariables.status)
    }

    @Test
    fun explicitlyRejectsCalculusScope() {
        val result = MathVerificationEngine.verify(VerificationRequest("∫x"))

        assertEquals(VerificationStatus.UNSUPPORTED, result.status)
        assertTrue(result.warnings.single().message.contains("初高中数学"))
    }
}
