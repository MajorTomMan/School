package com.majortomman.school.learning.assessment.domain

import com.majortomman.school.learning.activity.ActivityResult
import com.majortomman.school.learning.activity.TextualActivityResult
import com.majortomman.school.learning.activity.NumericActivityResult
import com.majortomman.school.learning.content.LearningContent
import kotlin.math.abs

sealed interface InlineAssessmentRule {
    data class ExactText(
        val expected: String,
        val ignoreCase: Boolean = false,
    ) : InlineAssessmentRule {
        init {
            require(expected.isNotBlank()) { "expected text 不能为空" }
        }
    }

    data class ExactNumber(
        val expected: Double,
        val tolerance: Double = 1e-9,
    ) : InlineAssessmentRule {
        init {
            require(expected.isFinite()) { "expected number 必须是有限数" }
            require(tolerance.isFinite() && tolerance >= 0.0) { "number tolerance 必须是非负有限数" }
        }
    }
}

data class InlineAssessmentSpec(
    val rule: InlineAssessmentRule,
    val explanation: List<LearningContent>,
    val knowledgePointIds: List<String>,
    val difficulty: Difficulty,
) {
    init {
        require(explanation.isNotEmpty()) { "inline assessment explanation 不能为空" }
        require(knowledgePointIds.isNotEmpty()) { "inline assessment knowledgePointIds 不能为空" }
        require(knowledgePointIds.all(String::isNotBlank)) { "inline assessment knowledgePointIds 不能包含空字符串" }
        require(knowledgePointIds.distinct().size == knowledgePointIds.size) { "inline assessment knowledgePointIds 不能重复" }
    }
}

enum class InlineAssessmentOutcome {
    CORRECT,
    INCORRECT,
    INVALID,
}

data class InlineAssessmentResult(
    val outcome: InlineAssessmentOutcome,
)

object InlineAssessmentEvaluator {
    fun evaluate(spec: InlineAssessmentSpec, result: ActivityResult): InlineAssessmentResult = when (val rule = spec.rule) {
        is InlineAssessmentRule.ExactText -> {
            val answer = (result as? TextualActivityResult)?.value
                ?: return InlineAssessmentResult(InlineAssessmentOutcome.INVALID)
            val expected = rule.expected.trim()
            val actual = answer.trim()
            val correct = if (rule.ignoreCase) actual.equals(expected, ignoreCase = true) else actual == expected
            InlineAssessmentResult(if (correct) InlineAssessmentOutcome.CORRECT else InlineAssessmentOutcome.INCORRECT)
        }

        is InlineAssessmentRule.ExactNumber -> {
            val answer = (result as? NumericActivityResult)?.value
                ?: return InlineAssessmentResult(InlineAssessmentOutcome.INVALID)
            val correct = abs(answer - rule.expected) <= rule.tolerance
            InlineAssessmentResult(if (correct) InlineAssessmentOutcome.CORRECT else InlineAssessmentOutcome.INCORRECT)
        }
    }
}
