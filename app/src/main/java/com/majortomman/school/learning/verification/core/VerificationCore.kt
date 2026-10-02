package com.majortomman.school.learning.verification.core

import com.majortomman.school.visualization.VisualizationInvocation

enum class VerificationStatus {
    SUCCESS,
    UNSUPPORTED,
    INVALID,
}

data class VerificationRequest(
    val input: String,
) {
    init {
        require(input.length <= 4_000) { "验证输入不能超过 4000 个字符。" }
    }
}

@JvmInline
value class VerificationRuleKey(val value: String) {
    init {
        require(RULE_PATTERN.matches(value)) { "验证规则 key 无效：$value" }
    }

    companion object {
        private val RULE_PATTERN = Regex("[A-Z][A-Z0-9_]*")
    }
}

data class VerificationProblemType(
    val id: String,
    val label: String,
) {
    init {
        require(ID_PATTERN.matches(id)) { "验证题型 id 无效：$id" }
        require(label.isNotBlank()) { "验证题型名称不能为空。" }
    }

    companion object {
        private val ID_PATTERN = Regex("[a-z][a-z0-9._-]*")
    }
}

sealed interface VerificationArtifact {
    val display: String

    data class MathExpression(override val display: String) : VerificationArtifact
    data class MathEquation(override val display: String) : VerificationArtifact
    data class MathFunction(override val display: String) : VerificationArtifact
    data class MathSolution(override val display: String) : VerificationArtifact
}

data class VerificationStep(
    val rule: VerificationRuleKey,
    val title: String,
    val before: VerificationArtifact? = null,
    val after: VerificationArtifact? = null,
    val explanation: String,
    val conditions: List<String> = emptyList(),
    val children: List<VerificationStep> = emptyList(),
) {
    init {
        require(title.isNotBlank()) { "验证步骤标题不能为空。" }
        require(explanation.isNotBlank()) { "验证步骤解释不能为空。" }
        require(before != null || after != null) { "验证步骤至少需要 before 或 after。" }
    }
}

data class VerificationWarning(
    val code: String,
    val message: String,
) {
    init {
        require(code.matches(Regex("[A-Z][A-Z0-9_]*"))) { "验证警告 code 无效：$code" }
        require(message.isNotBlank()) { "验证警告内容不能为空。" }
    }
}

data class VerificationResult(
    val status: VerificationStatus,
    val problemType: VerificationProblemType,
    val normalizedInput: String,
    val answer: VerificationArtifact? = null,
    val steps: List<VerificationStep> = emptyList(),
    val warnings: List<VerificationWarning> = emptyList(),
    val visualizations: List<VisualizationInvocation> = emptyList(),
) {
    init {
        if (status == VerificationStatus.SUCCESS) require(answer != null) { "成功的验证结果必须包含答案。" }
    }
}
