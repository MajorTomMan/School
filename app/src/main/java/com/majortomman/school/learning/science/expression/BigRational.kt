package com.majortomman.school.learning.science.expression

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

/** Exact rational number used by the science engines. */
data class BigRational private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger,
) : Comparable<BigRational> {
    init {
        require(denominator.signum() > 0) { "Denominator must be positive." }
        require(numerator.gcd(denominator) == BigInteger.ONE) { "Rational must be normalized." }
    }

    operator fun plus(other: BigRational): BigRational = of(
        numerator * other.denominator + other.numerator * denominator,
        denominator * other.denominator,
    )

    operator fun minus(other: BigRational): BigRational = of(
        numerator * other.denominator - other.numerator * denominator,
        denominator * other.denominator,
    )

    operator fun times(other: BigRational): BigRational = of(
        numerator * other.numerator,
        denominator * other.denominator,
    )

    operator fun div(other: BigRational): BigRational {
        require(!other.isZero) { "Cannot divide by zero." }
        return of(numerator * other.denominator, denominator * other.numerator)
    }

    operator fun unaryMinus(): BigRational = of(-numerator, denominator)

    fun reciprocal(): BigRational {
        require(!isZero) { "Zero has no reciprocal." }
        return of(denominator, numerator)
    }

    fun abs(): BigRational = if (numerator.signum() < 0) -this else this

    fun pow(exponent: Int): BigRational = when {
        exponent == 0 -> ONE
        exponent > 0 -> of(numerator.pow(exponent), denominator.pow(exponent))
        else -> reciprocal().pow(-exponent)
    }

    val isZero: Boolean get() = numerator.signum() == 0
    val isInteger: Boolean get() = denominator == BigInteger.ONE
    val signum: Int get() = numerator.signum()

    fun toBigDecimal(mathContext: MathContext = MathContext.DECIMAL128): BigDecimal =
        BigDecimal(numerator).divide(BigDecimal(denominator), mathContext)

    fun toDouble(): Double = numerator.toDouble() / denominator.toDouble()

    override fun compareTo(other: BigRational): Int =
        (numerator * other.denominator).compareTo(other.numerator * denominator)

    override fun toString(): String = if (isInteger) numerator.toString() else "$numerator/$denominator"

    companion object {
        val ZERO: BigRational = of(0)
        val ONE: BigRational = of(1)
        val TWO: BigRational = of(2)

        fun of(value: Long): BigRational = of(BigInteger.valueOf(value), BigInteger.ONE)

        fun of(numerator: Long, denominator: Long): BigRational =
            of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

        fun of(numerator: BigInteger, denominator: BigInteger = BigInteger.ONE): BigRational {
            require(denominator.signum() != 0) { "Denominator cannot be zero." }
            if (numerator.signum() == 0) return BigRational(BigInteger.ZERO, BigInteger.ONE)
            val sign = if (denominator.signum() < 0) BigInteger.valueOf(-1) else BigInteger.ONE
            val signedNumerator = numerator * sign
            val positiveDenominator = denominator * sign
            val gcd = signedNumerator.gcd(positiveDenominator)
            return BigRational(signedNumerator / gcd, positiveDenominator / gcd)
        }

        /** Parses integers, fractions and finite decimals without converting through Double. */
        fun parse(text: String): BigRational {
            val normalized = text.trim().replace('−', '-')
            require(normalized.isNotEmpty()) { "Number is empty." }
            if ('/' in normalized) {
                val parts = normalized.split('/')
                require(parts.size == 2) { "Invalid fraction: $text" }
                return of(parts[0].trim().toBigInteger(), parts[1].trim().toBigInteger())
            }
            val decimal = normalized.toBigDecimal()
            val scale = decimal.scale().coerceAtLeast(0)
            val denominator = BigInteger.TEN.pow(scale)
            return of(decimal.movePointRight(scale).toBigIntegerExact(), denominator)
        }
    }
}
