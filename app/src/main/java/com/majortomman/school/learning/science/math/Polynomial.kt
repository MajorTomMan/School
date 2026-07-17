package com.majortomman.school.learning.science.math

import com.majortomman.school.learning.science.expression.BigRational

class Polynomial private constructor(
    private val terms: Map<Int, BigRational>,
) {
    val degree: Int get() = terms.keys.maxOrNull() ?: -1
    val isZero: Boolean get() = terms.isEmpty()

    fun coefficient(exponent: Int): BigRational = terms[exponent] ?: BigRational.ZERO

    operator fun plus(other: Polynomial): Polynomial {
        val result = terms.toMutableMap()
        other.terms.forEach { (exponent, coefficient) ->
            result[exponent] = (result[exponent] ?: BigRational.ZERO) + coefficient
        }
        return of(result)
    }

    operator fun minus(other: Polynomial): Polynomial = this + (-other)

    operator fun unaryMinus(): Polynomial = of(terms.mapValues { -it.value })

    operator fun times(other: Polynomial): Polynomial {
        val result = mutableMapOf<Int, BigRational>()
        terms.forEach { (leftExponent, leftCoefficient) ->
            other.terms.forEach { (rightExponent, rightCoefficient) ->
                val exponent = leftExponent + rightExponent
                result[exponent] = (result[exponent] ?: BigRational.ZERO) + leftCoefficient * rightCoefficient
            }
        }
        return of(result)
    }

    fun scale(value: BigRational): Polynomial = of(terms.mapValues { it.value * value })

    fun derivative(): Polynomial = of(
        terms.filterKeys { it > 0 }.map { (exponent, coefficient) ->
            exponent - 1 to coefficient * BigRational.of(exponent.toLong())
        }.toMap(),
    )

    fun evaluate(value: BigRational): BigRational {
        if (isZero) return BigRational.ZERO
        var result = BigRational.ZERO
        for (exponent in degree downTo 0) {
            result = result * value + coefficient(exponent)
        }
        return result
    }

    fun divideByLinearRoot(root: BigRational): PolynomialDivision {
        require(degree >= 1) { "多项式次数不足，不能做线性因式除法。" }
        val quotient = mutableMapOf<Int, BigRational>()
        var carry = coefficient(degree)
        quotient[degree - 1] = carry
        for (exponent in degree - 1 downTo 1) {
            carry = coefficient(exponent) + carry * root
            quotient[exponent - 1] = carry
        }
        val remainder = coefficient(0) + carry * root
        return PolynomialDivision(of(quotient), remainder)
    }

    fun render(variable: String = "x"): String {
        if (terms.isEmpty()) return "0"
        val entries = terms.entries.sortedByDescending { it.key }
        return entries.mapIndexed { index, (exponent, coefficient) ->
            val negative = coefficient.signum < 0
            val magnitude = coefficient.abs()
            val coefficientText = when {
                exponent > 0 && magnitude == BigRational.ONE -> ""
                else -> magnitude.toString()
            }
            val variableText = when (exponent) {
                0 -> ""
                1 -> variable
                else -> "$variable^$exponent"
            }
            val body = coefficientText + variableText
            when {
                index == 0 && negative -> "-$body"
                index == 0 -> body
                negative -> "- $body"
                else -> "+ $body"
            }
        }.joinToString(" ")
    }

    override fun equals(other: Any?): Boolean = other is Polynomial && terms == other.terms

    override fun hashCode(): Int = terms.hashCode()

    override fun toString(): String = render()

    companion object {
        val ZERO: Polynomial = Polynomial(emptyMap())
        val ONE: Polynomial = Polynomial(mapOf(0 to BigRational.ONE))

        fun constant(value: BigRational): Polynomial = of(mapOf(0 to value))

        fun monomial(coefficient: BigRational, exponent: Int): Polynomial = of(mapOf(exponent to coefficient))

        fun of(vararg coefficientsByExponent: Pair<Int, BigRational>): Polynomial =
            of(coefficientsByExponent.toMap())

        fun of(coefficientsByExponent: Map<Int, BigRational>): Polynomial {
            require(coefficientsByExponent.keys.all { it >= 0 }) { "多项式指数不能为负数。" }
            return Polynomial(coefficientsByExponent.filterValues { !it.isZero }.toSortedMap())
        }
    }
}

data class PolynomialDivision(
    val quotient: Polynomial,
    val remainder: BigRational,
)
