package com.majortomman.school.learning.science

import com.majortomman.school.learning.science.math.MathFormulaStatus
import com.majortomman.school.learning.science.math.MathFormulaVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathFormulaVerifierTest {
    @Test
    fun verifiesRadicalAndPiExpressions() {
        val radical = MathFormulaVerifier.verify("√72=6√2")
        val pi = MathFormulaVerifier.verify("2π+3π=5π")

        assertEquals(MathFormulaStatus.TRUE_AT_VALUES, radical.status)
        assertEquals(MathFormulaStatus.TRUE_AT_VALUES, pi.status)
    }

    @Test
    fun verifiesEquationAtCustomValues() {
        val result = MathFormulaVerifier.verify(
            input = "y=2x+1",
            values = mapOf("x" to 3.0, "y" to 7.0),
        )

        assertEquals(MathFormulaStatus.TRUE_AT_VALUES, result.status)
    }

    @Test
    fun sampleCheckFindsCounterexample() {
        val result = MathFormulaVerifier.verify(
            input = "x^2=x",
            sampleRelation = true,
        )

        assertEquals(MathFormulaStatus.SAMPLE_COUNTEREXAMPLE, result.status)
        assertTrue(result.samples.any { !it.matches })
    }

    @Test
    fun sampleMatchDoesNotClaimProof() {
        val result = MathFormulaVerifier.verify(
            input = "(x+1)^2=x^2+2x+1",
            sampleRelation = true,
        )

        assertEquals(MathFormulaStatus.SAMPLE_MATCH, result.status)
        assertTrue(result.message.contains("不是严格恒等证明"))
    }
}
