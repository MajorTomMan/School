package com.majortomman.school.ai

import org.json.JSONObject

/**
 * Contract used by AI grading. The model is not allowed to return Markdown or prose outside JSON.
 */
internal object StrictAiGradingProtocol {
    val systemPrompt: String = """
        你是一名严谨、耐心的中国中学老师，负责批改中学阶段的题目。
        只能使用中学课程范围内的概念、方法、符号和语言解释；不得把大学数学、微积分、
        高等代数、抽象代数或其他超出中学阶段的知识作为判断依据或必要解释。
        课程提供的标准答案和参考解析是本次批改的依据，不得擅自修改标准答案。

        只返回一个 JSON 对象，不要使用 Markdown、代码围栏、前言或结尾。格式必须严格为：
        {
          "completed": true,
          "answer_correct": true或false,
          "feedback": "对学习者答案的简洁评价",
          "explanation": "只使用中学知识给出的解释",
          "mistake_type": "概念错误/步骤错误/计算错误/表达不完整/无"
        }

        completed 只有在你已经完成题目、学习者答案和标准答案的核对，并完整生成反馈与解释后才能为 true。
        answer_correct 只有在学习者答案符合题目和标准答案时才能为 true；否则必须为 false。
    """.trimIndent()

    fun parse(raw: String): AnswerEvaluation {
        val text = raw.trim()
        require(text.isNotEmpty()) { "AI 返回内容为空" }
        require(text.startsWith('{') && text.endsWith('}')) {
            "AI 必须只返回 JSON 对象，不能包含 Markdown 或额外文字"
        }

        val json = runCatching { JSONObject(text) }
            .getOrElse { throw IllegalArgumentException("AI 返回的 JSON 无法解析", it) }
        val allowedKeys = setOf("completed", "answer_correct", "feedback", "explanation", "mistake_type")
        val actualKeys = buildSet { json.keys().forEachRemaining(::add) }
        val unknown = actualKeys - allowedKeys
        require(unknown.isEmpty()) { "AI JSON 包含未知字段：${unknown.sorted().joinToString()}" }
        require("completed" in actualKeys) { "AI JSON 缺少 completed" }
        require("answer_correct" in actualKeys) { "AI JSON 缺少 answer_correct" }
        require("feedback" in actualKeys) { "AI JSON 缺少 feedback" }
        require("explanation" in actualKeys) { "AI JSON 缺少 explanation" }

        val completedRaw = json.get("completed")
        val correctRaw = json.get("answer_correct")
        require(completedRaw is Boolean) { "AI JSON 的 completed 必须是布尔值" }
        require(correctRaw is Boolean) { "AI JSON 的 answer_correct 必须是布尔值" }

        val feedback = json.get("feedback") as? String
            ?: throw IllegalArgumentException("AI JSON 的 feedback 必须是字符串")
        val explanation = json.get("explanation") as? String
            ?: throw IllegalArgumentException("AI JSON 的 explanation 必须是字符串")
        require(feedback.isNotBlank()) { "AI JSON 的 feedback 不能为空" }
        require(explanation.isNotBlank()) { "AI JSON 的 explanation 不能为空" }
        require(feedback.length <= MAX_TEXT_LENGTH) { "AI feedback 过长" }
        require(explanation.length <= MAX_TEXT_LENGTH) { "AI explanation 过长" }

        val mistakeType = when {
            !json.has("mistake_type") || json.isNull("mistake_type") -> null
            json.get("mistake_type") is String -> json.getString("mistake_type")
                .trim()
                .takeIf { it.isNotEmpty() && it != "无" }
            else -> throw IllegalArgumentException("AI JSON 的 mistake_type 必须是字符串或 null")
        }

        return AnswerEvaluation(
            completed = completedRaw,
            answerCorrect = correctRaw,
            feedback = feedback.trim(),
            explanation = explanation.trim(),
            mistakeType = mistakeType,
        )
    }

    private const val MAX_TEXT_LENGTH = 8_000
}
