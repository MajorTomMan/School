package com.majortomman.school.visualization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeMathExpressionTest {
    @Test
    fun evaluatesPolynomialWithoutExecutingCode() {
        val expression = VisualizationParameterValue.MathExpressionValue.parse("x^2-4*x+1")
        assertEquals(setOf("x"), expression.variables)
        assertEquals(-2.0, expression.evaluate(mapOf("x" to 1.0)), 1e-9)
        assertEquals(1.0, expression.evaluate(mapOf("x" to 0.0)), 1e-9)
    }

    @Test
    fun usesMathematicalExponentPrecedence() {
        assertEquals(-4.0, VisualizationParameterValue.MathExpressionValue.parse("-2^2").evaluate(emptyMap()), 1e-9)
        assertEquals(4.0, VisualizationParameterValue.MathExpressionValue.parse("(-2)^2").evaluate(emptyMap()), 1e-9)
        assertEquals(0.25, VisualizationParameterValue.MathExpressionValue.parse("2^-2").evaluate(emptyMap()), 1e-9)
        assertEquals(512.0, VisualizationParameterValue.MathExpressionValue.parse("2^3^2").evaluate(emptyMap()), 1e-9)
    }

    @Test
    fun supportsFixedMathFunctionAllowlist() {
        val expression = VisualizationParameterValue.MathExpressionValue.parse("sqrt(abs(x))+sin(x)")
        val value = expression.evaluate(mapOf("x" to 0.0))
        assertEquals(0.0, value, 1e-9)
    }

    @Test
    fun rejectsCodeLikeSyntaxAndUnknownFunctions() {
        val samples = listOf(
            "java.lang.Runtime.getRuntime()",
            "foo(x)",
            "x[0]",
            "x.__class__",
            "{x}",
            "x;1",
        )
        samples.forEach { source ->
            assertTrue("should reject $source", runCatching { VisualizationParameterValue.MathExpressionValue.parse(source) }.isFailure)
        }
    }

    @Test
    fun functionGraphSchemaAcceptsExpressionAndRejectsExtraVariables() {
        val valid = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.function.graph"),
            parameters = VisualizationParameters.of(
                "expression" to VisualizationParameterValue.MathExpressionValue.parse("x^2-1"),
                "xMin" to VisualizationParameterValue.NumberValue(-5.0),
                "xMax" to VisualizationParameterValue.NumberValue(5.0),
            ),
        )
        assertTrue(SchoolVisualizationCatalog.validate(valid).isEmpty())

        val invalid = valid.copy(
            parameters = VisualizationParameters.of(
                "expression" to VisualizationParameterValue.MathExpressionValue.parse("a*x+1"),
            ),
        )
        assertTrue(SchoolVisualizationCatalog.validate(invalid).any { it.contains("只允许自变量 x") })
    }
}
