package com.majortomman.school.learning.assessment.domain

import java.math.BigInteger

@JvmInline
value class QuestionSetId(val value: String) {
    init {
        require(value.isNotBlank()) { "questionSetId 不能为空" }
    }

    override fun toString(): String = value
}

@JvmInline
value class QuestionId(val value: String) {
    init {
        require(value.isNotBlank()) { "questionId 不能为空" }
    }

    override fun toString(): String = value
}

@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "sessionId 不能为空" }
    }

    override fun toString(): String = value
}

@JvmInline
value class AttemptId(val value: String) {
    init {
        require(value.isNotBlank()) { "attemptId 不能为空" }
    }

    override fun toString(): String = value
}

@JvmInline
value class KnowledgePointId(val value: String) {
    init {
        require(value.isNotBlank()) { "knowledgePointId 不能为空" }
    }

    override fun toString(): String = value
}

@JvmInline
value class Difficulty(val value: Double) {
    init {
        require(value.isFinite() && value in 0.0..1.0) {
            "difficulty 必须位于 0.0 到 1.0"
        }
    }
}

data class QuestionKey(
    val id: QuestionId,
    val revision: Int,
) {
    init {
        require(revision > 0) { "question revision 必须大于 0" }
    }
}

data class KnowledgeBinding(
    val knowledgePointId: KnowledgePointId,
    val weight: Double,
) {
    init {
        require(weight.isFinite() && weight > 0.0) { "knowledge weight 必须大于 0" }
    }
}

data class QuestionHint(
    val id: String,
    val text: String,
) {
    init {
        require(id.isNotBlank()) { "hint id 不能为空" }
        require(text.isNotBlank()) { "hint text 不能为空" }
    }
}

sealed interface AnswerInputSpec {
    data object Integer : AnswerInputSpec

    data class Decimal(
        val allowFraction: Boolean = false,
    ) : AnswerInputSpec

    data class Rational(
        val allowDecimal: Boolean = true,
    ) : AnswerInputSpec

    data class SingleChoice(
        val optionIds: List<String>,
    ) : AnswerInputSpec {
        init {
            require(optionIds.size >= 2) { "单选题至少需要两个选项" }
            require(optionIds.all(String::isNotBlank)) { "选项 ID 不能为空" }
            require(optionIds.distinct().size == optionIds.size) { "选项 ID 不能重复" }
        }
    }

    data object Coordinate : AnswerInputSpec
}

data class RationalValue private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger,
) {
    companion object {
        fun of(numerator: Long, denominator: Long): RationalValue =
            of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

        fun of(numerator: BigInteger, denominator: BigInteger): RationalValue {
            require(denominator != BigInteger.ZERO) { "分母不能为 0" }
            if (numerator == BigInteger.ZERO) {
                return RationalValue(BigInteger.ZERO, BigInteger.ONE)
            }

            val sign = if (denominator.signum() < 0) BigInteger.valueOf(-1L) else BigInteger.ONE
            val signedNumerator = numerator * sign
            val positiveDenominator = denominator.abs()
            val divisor = signedNumerator.abs().gcd(positiveDenominator)
            return RationalValue(
                numerator = signedNumerator / divisor,
                denominator = positiveDenominator / divisor,
            )
        }
    }
}

sealed interface AnswerRule {
    data class ExactInteger(
        val expected: BigInteger,
    ) : AnswerRule {
        constructor(expected: Long) : this(BigInteger.valueOf(expected))
    }

    data class Decimal(
        val expected: Double,
        val tolerance: Double = 0.0,
    ) : AnswerRule {
        init {
            require(expected.isFinite()) { "expected 必须是有限小数" }
            require(tolerance.isFinite() && tolerance >= 0.0) { "tolerance 不能小于 0" }
        }
    }

    data class RationalEquivalent(
        val expected: RationalValue,
    ) : AnswerRule

    data class SingleChoice(
        val expectedOptionId: String,
    ) : AnswerRule {
        init {
            require(expectedOptionId.isNotBlank()) { "expectedOptionId 不能为空" }
        }
    }

    data class Coordinate(
        val expectedX: RationalValue,
        val expectedY: RationalValue,
    ) : AnswerRule
}

sealed interface UserAnswer {
    data class Text(val raw: String) : UserAnswer

    data class Choice(val optionId: String) : UserAnswer

    data class Coordinate(
        val rawX: String,
        val rawY: String,
    ) : UserAnswer
}

data class QuestionDefinition(
    val key: QuestionKey,
    val number: String,
    val inputSpec: AnswerInputSpec,
    val answerRule: AnswerRule,
    val knowledgeBindings: List<KnowledgeBinding>,
    val difficulty: Difficulty,
    val hints: List<QuestionHint> = emptyList(),
) {
    init {
        require(number.isNotBlank()) { "题号不能为空" }
        require(knowledgeBindings.isNotEmpty()) { "题目至少需要绑定一个知识点" }
        require(
            knowledgeBindings.map(KnowledgeBinding::knowledgePointId).distinct().size ==
                knowledgeBindings.size,
        ) { "同一题目不能重复绑定知识点" }
        require(hints.map(QuestionHint::id).distinct().size == hints.size) {
            "同一题目的提示 ID 不能重复"
        }

        when {
            inputSpec is AnswerInputSpec.Integer && answerRule !is AnswerRule.ExactInteger ->
                error("整数输入必须使用 ExactInteger 规则")

            inputSpec is AnswerInputSpec.Decimal && answerRule !is AnswerRule.Decimal ->
                error("小数输入必须使用 Decimal 规则")

            inputSpec is AnswerInputSpec.Rational && answerRule !is AnswerRule.RationalEquivalent ->
                error("分数输入必须使用 RationalEquivalent 规则")

            inputSpec is AnswerInputSpec.SingleChoice && answerRule !is AnswerRule.SingleChoice ->
                error("单选输入必须使用 SingleChoice 规则")

            inputSpec is AnswerInputSpec.Coordinate && answerRule !is AnswerRule.Coordinate ->
                error("坐标输入必须使用 Coordinate 规则")
        }

        if (inputSpec is AnswerInputSpec.SingleChoice && answerRule is AnswerRule.SingleChoice) {
            require(answerRule.expectedOptionId in inputSpec.optionIds) {
                "正确选项必须存在于 optionIds 中"
            }
        }
    }
}

data class QuestionSetDefinition(
    val id: QuestionSetId,
    val title: String,
    val questions: List<QuestionDefinition>,
    val allowSkip: Boolean = true,
    val allowReviewBeforeFinish: Boolean = true,
) {
    init {
        require(title.isNotBlank()) { "题组标题不能为空" }
        require(questions.isNotEmpty()) { "题组至少需要一道题" }
        require(questions.map(QuestionDefinition::key).distinct().size == questions.size) {
            "题组内 question key 不能重复"
        }
    }
}
