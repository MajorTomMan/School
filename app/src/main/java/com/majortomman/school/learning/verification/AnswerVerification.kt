package com.majortomman.school.learning.verification

import com.majortomman.school.learning.science.math.MathFormulaStatus
import com.majortomman.school.learning.science.math.MathFormulaVerifier

enum class AnswerVerificationVerdict {
    CORRECT,
    INCORRECT,
    PARTIALLY_CORRECT,
    AMBIGUOUS,
    UNSUPPORTED,
}

enum class AnswerVerificationMethod {
    DETERMINISTIC,
    AI,
}

data class AnswerVerificationRequest(
    val subjectHint: VerificationSubject?,
    val question: String,
    val workProcess: String,
    val answer: String,
    val referenceAnswer: String?,
) {
    init {
        require(question.length <= 8_000) { "题目文本不能超过 8000 个字符" }
        require(workProcess.length <= 12_000) { "做题过程不能超过 12000 个字符" }
        require(answer.length <= 8_000) { "答案文本不能超过 8000 个字符" }
        require(referenceAnswer == null || referenceAnswer.length <= 8_000) { "参考答案不能超过 8000 个字符" }
    }
}

data class AnswerVerificationResult(
    val verdict: AnswerVerificationVerdict,
    val method: AnswerVerificationMethod,
    val subject: String,
    val feedback: String,
    val referenceAnswer: String?,
    val explanation: String,
    val limitation: String?,
)

object DeterministicAnswerVerifier {
    fun verify(request: AnswerVerificationRequest): AnswerVerificationResult? {
        if (request.subjectHint != null && request.subjectHint != VerificationSubject.MATHEMATICS) return null
        if (request.answer.isBlank()) return null

        val reference = request.referenceAnswer?.trim().orEmpty()
        if (reference.isNotBlank()) {
            val comparison = MathFormulaVerifier.verify("(${request.answer})=($reference)", sampleRelation = true)
            return comparison.toVerificationResult(reference)
        }

        if (!containsMathRelation(request.answer)) return null
        val relation = MathFormulaVerifier.verify(request.answer, sampleRelation = true)
        return relation.toVerificationResult(null)
    }

    private fun containsMathRelation(value: String): Boolean =
        value.any { it == '=' || it == '<' || it == '>' || it == '≤' || it == '≥' }

    private fun com.majortomman.school.learning.science.math.MathFormulaVerificationResult.toVerificationResult(reference: String?): AnswerVerificationResult? = when (status) {
        MathFormulaStatus.TRUE_AT_VALUES,
        MathFormulaStatus.SAMPLE_MATCH,
        -> AnswerVerificationResult(
            verdict = AnswerVerificationVerdict.CORRECT,
            method = AnswerVerificationMethod.DETERMINISTIC,
            subject = "数学",
            feedback = message,
            referenceAnswer = reference,
            explanation = normalizedLeft?.let { left -> normalizedRight?.let { right -> "$left ${relation?.symbol.orEmpty()} $right" } ?: left }.orEmpty(),
            limitation = if (status == MathFormulaStatus.SAMPLE_MATCH) "多组安全样本未发现反例，但这不是形式化恒等证明。" else null,
        )
        MathFormulaStatus.FALSE_AT_VALUES,
        MathFormulaStatus.SAMPLE_COUNTEREXAMPLE,
        -> AnswerVerificationResult(
            verdict = AnswerVerificationVerdict.INCORRECT,
            method = AnswerVerificationMethod.DETERMINISTIC,
            subject = "数学",
            feedback = message,
            referenceAnswer = reference,
            explanation = samples.firstOrNull { !it.matches }?.let { "发现反例：${it.variables.entries.joinToString { entry -> "${entry.key}=${entry.value}" }}" }.orEmpty(),
            limitation = null,
        )
        MathFormulaStatus.UNSUPPORTED -> null
        MathFormulaStatus.NEEDS_VARIABLE_VALUES,
        MathFormulaStatus.VALID_EXPRESSION,
        -> null
    }
}
