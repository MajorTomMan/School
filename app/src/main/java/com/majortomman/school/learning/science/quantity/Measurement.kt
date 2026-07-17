package com.majortomman.school.learning.science.quantity

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class MeasuredValue(
    val value: BigDecimal,
    val significantFigures: Int,
    val absoluteUncertainty: BigDecimal? = null,
) {
    init {
        require(significantFigures > 0) { "Significant figures must be positive." }
        require(absoluteUncertainty == null || absoluteUncertainty.signum() >= 0) {
            "Uncertainty cannot be negative."
        }
    }

    fun rounded(): BigDecimal = value.round(MathContext(significantFigures, RoundingMode.HALF_UP))

    fun relativeUncertainty(): BigDecimal? {
        val uncertainty = absoluteUncertainty ?: return null
        if (value.signum() == 0) return null
        return uncertainty.divide(value.abs(), MathContext.DECIMAL64)
    }

    fun percentageUncertainty(): BigDecimal? =
        relativeUncertainty()?.multiply(BigDecimal(100), MathContext.DECIMAL64)
}

object SignificantFigures {
    fun count(text: String): Int {
        val normalized = text.trim().lowercase().substringBefore('e').replace("+", "").replace("-", "")
        require(normalized.any(Char::isDigit)) { "没有可识别的数字。" }
        val digits = normalized.filter(Char::isDigit)
        if ('.' in normalized) {
            val withoutLeadingZeros = digits.dropWhile { it == '0' }
            return withoutLeadingZeros.length.coerceAtLeast(1)
        }
        val withoutLeading = digits.dropWhile { it == '0' }
        val withoutTrailing = withoutLeading.dropLastWhile { it == '0' }
        return withoutTrailing.length.coerceAtLeast(1)
    }

    fun round(value: BigDecimal, figures: Int): BigDecimal {
        require(figures > 0) { "有效数字必须大于 0。" }
        return value.round(MathContext(figures, RoundingMode.HALF_UP))
    }
}
