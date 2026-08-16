package com.majortomman.school.ai

import com.majortomman.school.learning.verification.AnswerVerificationMethod
import com.majortomman.school.learning.verification.AnswerVerificationResult
import com.majortomman.school.learning.verification.AnswerVerificationVerdict
import org.json.JSONObject

internal object GenericAnswerVerificationProtocol {
    val systemPrompt: String = """
        你是 School 学习应用中的通用答案验证器。输入可能包含题目文字、做题过程、最终答案、可选参考答案，以及题目或答案图片。
        你的任务是验证学习者答案，不是聊天。

        规则：
        1. 先识别学科和题目要求，再独立求解或依据明确参考答案核对。
        2. 做题过程与最终答案必须分开判断；如果最终答案正确但过程有明确错误，verdict 使用 partially_correct。
        3. 图片只能作为题目或学习者作答证据，不得根据模糊内容臆测；无法可靠读取时使用 unsupported 或 ambiguous。
        4. 只使用中小学课程范围内必要知识；不确定时明确说明限制。
        5. 如果给出了参考答案，优先依据参考答案和题目条件核对，但若参考答案本身明显与题目冲突，使用 ambiguous 并说明。
        6. completed 只有在你已经完整检查了本次输入时才为 true。
        7. 只返回一个 JSON 对象，不要 Markdown，不要额外文本。

        JSON 必须严格包含：
        {
          "completed": true或false,
          "verdict": "correct|incorrect|partially_correct|ambiguous|unsupported",
          "subject": "识别到的学科",
          "feedback": "对学习者最有用的简短反馈",
          "reference_answer": "你确认的参考答案；无法确认时为空字符串",
          "explanation": "核对依据和必要过程",
          "limitation": "能力边界或不确定性；没有则为空字符串"
        }
    """.trimIndent()

    fun parse(raw: String): AnswerVerificationResult {
        val json = JSONObject(raw)
        val expected = setOf("completed", "verdict", "subject", "feedback", "reference_answer", "explanation", "limitation")
        require(json.keys().asSequence().toSet() == expected) { "AI 验证结果字段不符合协议" }
        require(json.getBoolean("completed")) { "AI 未完成本次验证" }
        val verdict = when (json.getString("verdict")) {
            "correct" -> AnswerVerificationVerdict.CORRECT
            "incorrect" -> AnswerVerificationVerdict.INCORRECT
            "partially_correct" -> AnswerVerificationVerdict.PARTIALLY_CORRECT
            "ambiguous" -> AnswerVerificationVerdict.AMBIGUOUS
            "unsupported" -> AnswerVerificationVerdict.UNSUPPORTED
            else -> error("AI 返回了不支持的验证结论")
        }
        val subject = json.getString("subject").trim()
        val feedback = json.getString("feedback").trim()
        val reference = json.getString("reference_answer").trim().ifBlank { null }
        val explanation = json.getString("explanation").trim()
        val limitation = json.getString("limitation").trim().ifBlank { null }
        require(subject.isNotBlank()) { "AI 验证结果缺少学科" }
        require(feedback.isNotBlank()) { "AI 验证结果缺少反馈" }
        require(explanation.isNotBlank()) { "AI 验证结果缺少解释" }
        return AnswerVerificationResult(
            verdict = verdict,
            method = AnswerVerificationMethod.AI,
            subject = subject,
            feedback = feedback,
            referenceAnswer = reference,
            explanation = explanation,
            limitation = limitation,
        )
    }
}
