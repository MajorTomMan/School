package com.majortomman.school.ai

data class AiMessage(
    val role: String,
    val content: String,
)

data class AnswerEvaluation(
    val correct: Boolean,
    val feedback: String,
    val mistakeType: String? = null,
)

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
        correct = false,
        feedback = "AI 批改将在接入 llama.cpp 或 OpenAI 兼容接口后启用。",
    )
}
