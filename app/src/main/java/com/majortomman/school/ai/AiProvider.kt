package com.majortomman.school.ai

data class AiMessage(
    val role: String,
    val content: String,
)

/**
 * Strict result returned by the AI grading protocol.
 * A result may only be accepted after [completed] and [answerCorrect] are both true.
 */
data class AnswerEvaluation(
    val completed: Boolean,
    val answerCorrect: Boolean,
    val feedback: String,
    val explanation: String,
    val mistakeType: String? = null,
) {
    /** Backward-compatible read-only alias for older non-assessment AI screens. */
    val correct: Boolean
        get() = answerCorrect
}

interface AiProvider {
    suspend fun explain(concept: String, learnerQuestion: String): String

    suspend fun giveHint(question: String, learnerAnswer: String, level: Int): String

    suspend fun evaluateAnswer(question: String, learnerAnswer: String): AnswerEvaluation
}

class OfflinePlaceholderAiProvider : AiProvider {
    override suspend fun explain(concept: String, learnerQuestion: String): String =
        "AI 服务尚未配置。当前仍可使用教材讲解与本地练习。"

    override suspend fun giveHint(question: String, learnerAnswer: String, level: Int): String =
        "先在数轴上找到两个数的位置，再判断谁更靠右。"

    override suspend fun evaluateAnswer(
        question: String,
        learnerAnswer: String,
    ): AnswerEvaluation = AnswerEvaluation(
        completed = false,
        answerCorrect = false,
        feedback = "AI 服务尚未配置。",
        explanation = "请先在设置中配置可用的 OpenAI 兼容接口和模型。",
    )
}
