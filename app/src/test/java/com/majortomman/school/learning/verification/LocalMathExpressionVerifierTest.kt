package com.majortomman.school.learning.verification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMathExpressionVerifierTest {
    @Test
    fun evaluatesConstantExpressionLocally() {
        val result = LocalMathExpressionVerifier.verify("2*(3+4)")
        assertEquals(LocalMathExpressionKind.VALUE, result.kind)
        assertEquals("14", result.displayAnswer)
        assertEquals("2*(3+4)", result.normalizedExpression)
    }

    @Test
    fun acceptsCommonMathSymbols() {
        val pi = LocalMathExpressionVerifier.verify("2π")
        assertEquals(LocalMathExpressionKind.VALUE, pi.kind)
        assertTrue(pi.displayAnswer.startsWith("6.28318"))

        val root = LocalMathExpressionVerifier.verify("√9")
        assertEquals("3", root.displayAnswer)

        val exponential = LocalMathExpressionVerifier.verify("exp(0)")
        assertEquals("1", exponential.displayAnswer)
    }

    @Test
    fun normalizesImplicitMultiplicationAndBuildsFunctionGraph() {
        val result = LocalMathExpressionVerifier.verify("y=2x+1")
        assertEquals(LocalMathExpressionKind.FUNCTION, result.kind)
        assertEquals("2*x+1", result.normalizedExpression)
        assertEquals("y = 2*x+1", result.displayAnswer)
        val graph = assertNotNull(result.graph)
        assertEquals("mathematics.function.graph", graph.renderer.value)
        assertTrue(graph.parameters.number("xMax") > graph.parameters.number("xMin"))
        assertTrue(graph.parameters.number("yMax") > graph.parameters.number("yMin"))
    }

    @Test
    fun supportsGeneralSingleVariableFunctions() {
        val result = LocalMathExpressionVerifier.verify("sin(x)+x^2/4")
        assertEquals(LocalMathExpressionKind.FUNCTION, result.kind)
        assertNotNull(result.graph)
    }

    @Test
    fun rejectsEquationsAndMultipleVariablesForNow() {
        assertTrue(runCatching { LocalMathExpressionVerifier.verify("x+1=2") }.isFailure)
        assertTrue(runCatching { LocalMathExpressionVerifier.verify("x+y") }.isFailure)
    }
}
