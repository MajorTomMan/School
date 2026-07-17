package com.majortomman.school.learning.science

import com.majortomman.school.learning.science.expression.BigRational
import com.majortomman.school.learning.science.expression.ScienceExpression
import com.majortomman.school.learning.science.expression.ScienceExpressionParser
import com.majortomman.school.learning.science.expression.ScienceNumberDomain
import com.majortomman.school.learning.science.math.AlgebraSolver
import com.majortomman.school.learning.science.math.Comparison
import com.majortomman.school.learning.science.math.DomainConstraint
import com.majortomman.school.learning.science.math.EquationSolution
import com.majortomman.school.learning.science.math.FunctionDefinition
import com.majortomman.school.learning.science.math.InequalitySolver
import com.majortomman.school.learning.science.math.Line2
import com.majortomman.school.learning.science.math.Point2
import com.majortomman.school.learning.science.math.Polynomial
import com.majortomman.school.learning.science.math.ProofStep
import com.majortomman.school.learning.science.math.ProofStepKind
import com.majortomman.school.learning.science.math.ProofStructureValidator
import com.majortomman.school.learning.science.math.Vector2
import com.majortomman.school.learning.science.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MathFoundationTest {
    @Test
    fun polynomialOperationsStayExact() {
        val xMinusTwo = Polynomial.of(1 to BigRational.ONE, 0 to BigRational.of(-2))
        val xMinusThree = Polynomial.of(1 to BigRational.ONE, 0 to BigRational.of(-3))
        val product = xMinusTwo * xMinusThree

        assertEquals("x^2 - 5x + 6", product.render())
        assertEquals(BigRational.ZERO, product.evaluate(BigRational.of(2)))
        assertEquals("2x - 5", product.derivative().render())
    }

    @Test
    fun quadraticSolverReturnsExactOrderedRoots() {
        val result = AlgebraSolver.solveQuadratic(
            a = BigRational.ONE,
            b = BigRational.of(-5),
            c = BigRational.of(6),
        )
        val roots = (result.solution as EquationSolution.Roots).values

        assertEquals(listOf("2", "3"), roots.map { it.text })
        assertTrue(result.steps.any { it.title == "判别式" })
    }

    @Test
    fun quadraticSolverCanEnterComplexDomain() {
        val real = AlgebraSolver.solveQuadratic(BigRational.ONE, BigRational.ZERO, BigRational.ONE)
        val complex = AlgebraSolver.solveQuadratic(
            BigRational.ONE,
            BigRational.ZERO,
            BigRational.ONE,
            ScienceNumberDomain.COMPLEX,
        )

        assertTrue(real.solution is EquationSolution.NoSolution)
        val roots = (complex.solution as EquationSolution.Roots).values
        assertEquals(2, roots.size)
        assertTrue(roots.all { kotlin.math.abs(kotlin.math.abs(it.approximate.imaginary) - 1.0) < 1e-9 })
    }

    @Test
    fun dividingInequalityByNegativeReversesDirection() {
        val result = InequalitySolver.solveLinear(
            a = BigRational.of(-2),
            b = BigRational.of(3),
            comparison = Comparison.LESS,
            c = BigRational.of(7),
        )

        assertEquals("x > -2", result.intervals.single().render())
        assertTrue(result.steps.any { it.reason.contains("反转") })
    }

    @Test
    fun functionDomainBlocksDivisionByZero() {
        val x = ScienceExpression.Variable("x")
        val function = FunctionDefinition(
            name = "f",
            variable = "x",
            expression = ScienceExpressionParser.parse("1/x"),
            domain = listOf(DomainConstraint.NonZero(x)),
        )

        assertEquals(0.5, function.evaluate(2.0), 1e-9)
        assertThrows(IllegalArgumentException::class.java) { function.evaluate(0.0) }
    }

    @Test
    fun vectorsLinesAndProjectionsUseGeometricRelations() {
        val projection = Vector2(3.0, 4.0).projectionOnto(Vector2(1.0, 0.0))
        val intersection = Line2(Point2(0.0, 0.0), Vector2(1.0, 1.0))
            .intersection(Line2(Point2(0.0, 2.0), Vector2(1.0, -1.0)))
        val cross = Vector3(1.0, 0.0, 0.0).cross(Vector3(0.0, 1.0, 0.0))

        assertEquals(Vector2(3.0, 0.0), projection)
        assertEquals(Point2(1.0, 1.0), intersection)
        assertEquals(Vector3(0.0, 0.0, 1.0), cross)
    }

    @Test
    fun proofStructureFindsForwardReference() {
        val validation = ProofStructureValidator.validate(
            listOf(
                ProofStep(
                    kind = ProofStepKind.INFERENCE,
                    statement = "AB = AC",
                    reason = "由前一步",
                    dependencies = setOf("triangle_isosceles"),
                    producedFactId = "equal_sides",
                ),
            ),
        )

        assertFalse(validation.valid)
        assertTrue("triangle_isosceles" in validation.missingDependencies)
    }
}
