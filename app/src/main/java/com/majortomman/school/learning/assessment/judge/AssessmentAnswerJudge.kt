package com.majortomman.school.learning.assessment.judge

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.assessment.domain.JudgeOutcome
import com.majortomman.school.learning.assessment.domain.JudgeResult
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.RationalValue
import com.majortomman.school.learning.assessment.domain.UserAnswer
import java.math.BigDecimal
import java.math.BigInteger

fun interface AssessmentAnswerJudge {
    fun judge(question: QuestionDefinition, answer: UserAnswer): JudgeResult
}

/**
 * 首批题型的确定性判题器。
 *
 * 它只处理声明式答案规则，不执行课程包中的脚本，也不会把格式不完整的输入计为答错。
 */
object DefaultAssessmentAnswerJudge : AssessmentAnswerJudge {
    override fun judge(question: QuestionDefinition, answer: UserAnswer): JudgeResult = when (
        val input = question.inputSpec
    ) {
        AnswerInputSpec.Integer -> judgeInteger(answer, question.answerRule as AnswerRule.ExactInteger)
        is AnswerInputSpec.Decimal -> judgeDecimal(answer, input, question.answerRule as AnswerRule.Decimal)
        is AnswerInputSpec.Rational -> judgeRational(
            answer,
            input,
            question.answerRule as AnswerRule.RationalEquivalent,
        )

        is AnswerInputSpec.SingleChoice -> judgeChoice(
            answer,
            input,
            question.answerRule as AnswerRule.SingleChoice,
        )

        AnswerInputSpec.Coordinate -> judgeCoordinate(
            answer,
            question.answerRule as AnswerRule.Coordinate,
        )
    }

    private fun judgeInteger(
        answer: UserAnswer,
        rule: AnswerRule.ExactInteger,
    ): JudgeResult {
        val raw = (answer as? UserAnswer.Text)?.raw
            ?: return invalid("answer_type_mismatch")
        val normalized = normalizeMathText(raw)
        if (!INTEGER_PATTERN.matches(normalized)) return invalid("invalid_integer")
        val value = normalized.toBigIntegerOrNull() ?: return invalid("integer_out_of_range")
        return compared(value == rule.expected, value.toString())
    }

    private fun judgeDecimal(
        answer: UserAnswer,
        input: AnswerInputSpec.Decimal,
        rule: AnswerRule.Decimal,
    ): JudgeResult {
        val raw = (answer as? UserAnswer.Text)?.raw
            ?: return invalid("answer_type_mismatch")
        val normalized = normalizeMathText(raw)
        val parsed = if (input.allowFraction && '/' in normalized) {
            parseRational(normalized, allowDecimal = false)?.toBigDecimal()
        } else {
            parseDecimal(normalized)
        } ?: return invalid("invalid_decimal")

        val expected = BigDecimal.valueOf(rule.expected)
        val difference = parsed.subtract(expected).abs()
        val correct = difference <= BigDecimal.valueOf(rule.tolerance)
        return compared(correct, parsed.normalizedText())
    }

    private fun judgeRational(
        answer: UserAnswer,
        input: AnswerInputSpec.Rational,
        rule: AnswerRule.RationalEquivalent,
    ): JudgeResult {
        val raw = (answer as? UserAnswer.Text)?.raw
            ?: return invalid("answer_type_mismatch")
        val value = parseRational(normalizeMathText(raw), allowDecimal = input.allowDecimal)
            ?: return invalid("invalid_rational")
        return compared(value == rule.expected, value.normalizedText())
    }

    private fun judgeChoice(
        answer: UserAnswer,
        input: AnswerInputSpec.SingleChoice,
        rule: AnswerRule.SingleChoice,
    ): JudgeResult {
        val optionId = (answer as? UserAnswer.Choice)?.optionId?.trim()
            ?: return invalid("answer_type_mismatch")
        if (optionId.isBlank() || optionId !in input.optionIds) return invalid("unknown_option")
        return compared(optionId == rule.expectedOptionId, optionId)
    }

    private fun judgeCoordinate(
        answer: UserAnswer,
        rule: AnswerRule.Coordinate,
    ): JudgeResult {
        val coordinate = answer as? UserAnswer.Coordinate
            ?: return invalid("answer_type_mismatch")
        val x = parseRational(normalizeMathText(coordinate.rawX), allowDecimal = true)
            ?: return invalid("invalid_coordinate_x")
        val y = parseRational(normalizeMathText(coordinate.rawY), allowDecimal = true)
            ?: return invalid("invalid_coordinate_y")

        val xCorrect = x == rule.expectedX
        val yCorrect = y == rule.expectedY
        val normalized = "(${x.normalizedText()},${y.normalizedText()})"
        return when {
            xCorrect && yCorrect -> JudgeResult(JudgeOutcome.CORRECT, normalized)
            xCorrect || yCorrect -> JudgeResult(
                outcome = JudgeOutcome.PARTIALLY_CORRECT,
                normalizedAnswer = normalized,
                feedbackCode = if (xCorrect) "coordinate_y_incorrect" else "coordinate_x_incorrect",
            )

            else -> JudgeResult(
                outcome = JudgeOutcome.INCORRECT,
                normalizedAnswer = normalized,
                feedbackCode = "answer_incorrect",
            )
        }
    }

    private fun compared(correct: Boolean, normalized: String): JudgeResult = JudgeResult(
        outcome = if (correct) JudgeOutcome.CORRECT else JudgeOutcome.INCORRECT,
        normalizedAnswer = normalized,
        feedbackCode = if (correct) null else "answer_incorrect",
    )

    private fun invalid(code: String): JudgeResult = JudgeResult(
        outcome = JudgeOutcome.INVALID_INPUT,
        feedbackCode = code,
    )

    private fun parseDecimal(value: String): BigDecimal? {
        if (!DECIMAL_PATTERN.matches(value)) return null
        return runCatching { BigDecimal(value) }.getOrNull()
    }

    private fun parseRational(value: String, allowDecimal: Boolean): RationalValue? {
        if ('/' in value) {
            val match = FRACTION_PATTERN.matchEntire(value) ?: return null
            val numerator = match.groupValues[1].toBigIntegerOrNull() ?: return null
            val denominator = match.groupValues[2].toBigIntegerOrNull() ?: return null
            if (denominator == BigInteger.ZERO) return null
            return RationalValue.of(numerator, denominator)
        }
        if (!allowDecimal) {
            if (!INTEGER_PATTERN.matches(value)) return null
            return value.toBigIntegerOrNull()?.let { RationalValue.of(it, BigInteger.ONE) }
        }
        val decimal = parseDecimal(value) ?: return null
        val normalized = decimal.stripTrailingZeros()
        val scale = normalized.scale()
        return if (scale <= 0) {
            RationalValue.of(normalized.toBigIntegerExact(), BigInteger.ONE)
        } else {
            RationalValue.of(normalized.unscaledValue(), BigInteger.TEN.pow(scale))
        }
    }

    private fun RationalValue.toBigDecimal(): BigDecimal =
        BigDecimal(numerator).divide(BigDecimal(denominator), DECIMAL_CONTEXT)

    private fun RationalValue.normalizedText(): String =
        if (denominator == BigInteger.ONE) numerator.toString() else "$numerator/$denominator"

    private fun BigDecimal.normalizedText(): String = stripTrailingZeros().toPlainString()

    private fun normalizeMathText(raw: String): String = buildString(raw.length) {
        raw.trim().forEach { character ->
            append(
                when (character) {
                    in '０'..'９' -> ('0'.code + character.code - '０'.code).toChar()
                    '＋' -> '+'
                    '－', '−', '﹣' -> '-'
                    '．' -> '.'
                    '／' -> '/'
                    else -> character
                },
            )
        }
    }.filterNot(Char::isWhitespace)

    private val INTEGER_PATTERN = Regex("[+-]?\\d+")
    private val DECIMAL_PATTERN = Regex("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)")
    private val FRACTION_PATTERN = Regex("([+-]?\\d+)/([+-]?\\d+)")
    private val DECIMAL_CONTEXT = java.math.MathContext.DECIMAL128
}
