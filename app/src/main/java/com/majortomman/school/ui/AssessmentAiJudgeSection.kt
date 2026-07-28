package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.ai.AnswerEvaluation
import com.majortomman.school.ai.OpenAiCompatibleClient
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.PreferencesRepository
import com.majortomman.school.learning.assessment.application.AssessmentIntent
import com.majortomman.school.learning.assessment.application.AssessmentQuestionPageState
import com.majortomman.school.learning.assessment.contract.CourseAssessmentQuestion
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.assessment.domain.JudgeOutcome
import com.majortomman.school.learning.assessment.domain.UserAnswer
import com.majortomman.school.learning.content.LearningContent
import kotlinx.coroutines.launch

/**
 * AI is deliberately supplementary. The authored deterministic rule remains the source of truth;
 * the model receives that reference and is used for natural-language feedback and explanation.
 */
@Composable
internal fun AssessmentAiJudgeSection(
    page: AssessmentQuestionPageState,
    question: CourseAssessmentQuestion,
    dispatch: (AssessmentIntent) -> Unit,
) {
    val context = LocalContext.current
    val settingsFlow = remember(context) {
        PreferencesRepository(context.applicationContext).aiSettings
    }
    val settings by settingsFlow.collectAsState(initial = AiSettings())
    val scope = rememberCoroutineScope()
    var busy by remember(question.definition.key) { mutableStateOf(false) }
    var evaluation by remember(question.definition.key) { mutableStateOf<AnswerEvaluation?>(null) }
    var error by remember(question.definition.key) { mutableStateOf<String?>(null) }
    val answerText = page.draftAnswer.displayText(question)
    val enabled = answerText.isNotBlank() && !page.busy && !busy

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(
                    width = 1.dp,
                    color = if (enabled) InteractiveYellow.copy(alpha = 0.88f) else InteractiveLine,
                    shape = RoundedCornerShape(12.dp),
                )
                .background(
                    color = if (enabled) InteractiveYellow.copy(alpha = 0.08f) else InteractivePanel,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(enabled = enabled) {
                    if (!page.progress.answerLocked) dispatch(AssessmentIntent.SubmitAnswer)
                    evaluation = null
                    error = null
                    if (settings.endpoint.isBlank() || settings.model.isBlank()) {
                        error = "请先在“设置 → AI”中填写接口地址和模型名称。"
                        return@clickable
                    }
                    scope.launch {
                        busy = true
                        runCatching {
                            OpenAiCompatibleClient(settings).evaluateAnswer(
                                question = question.aiPrompt(),
                                learnerAnswer = answerText,
                            )
                        }.fold(
                            onSuccess = { evaluation = it },
                            onFailure = { failure ->
                                error = "AI 请求失败：${failure.message ?: failure::class.java.simpleName}"
                            },
                        )
                        busy = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = InteractiveYellow,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = "AI 判题与解释",
                    color = if (enabled) InteractiveYellow else InteractiveMuted.copy(alpha = 0.46f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(InteractivePanel, RoundedCornerShape(14.dp))
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                "答案与解释",
                color = InteractiveWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            when {
                evaluation != null -> {
                    val value = requireNotNull(evaluation)
                    Text(
                        value.feedback,
                        color = InteractiveWhite.copy(alpha = 0.88f),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    )
                    value.mistakeType?.let { mistake ->
                        Text(
                            "可能的错误类型：$mistake",
                            color = InteractiveYellow,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }
                error != null -> Text(
                    error.orEmpty(),
                    color = InteractiveRed,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                )
                page.progress.latestJudgeResult != null -> Text(
                    localResultText(page),
                    color = localResultColor(page),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
                else -> Text(
                    "输入答案后可以直接提交本地判题，也可以使用 AI 获得更自然的反馈。提交后，这里会显示参考答案与解析。",
                    color = InteractiveMuted,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                )
            }

            if (page.progress.latestJudgeResult != null || evaluation != null) {
                Text(
                    "参考答案：${question.referenceAnswer()}",
                    color = InteractiveYellow,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val explanation = question.explanationText()
                if (explanation.isNotBlank()) {
                    Text(
                        "参考解释：$explanation",
                        color = InteractiveWhite.copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                    )
                }
            }

            Text(
                "最终对错以课程标准答案的本地确定性判题为准；AI 只负责补充反馈和解释。",
                color = InteractiveMuted.copy(alpha = 0.78f),
                fontSize = 11.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

private fun localResultText(page: AssessmentQuestionPageState): String =
    when (page.progress.latestJudgeResult?.outcome) {
        JudgeOutcome.CORRECT -> "本地判题：回答正确。"
        JudgeOutcome.INCORRECT -> "本地判题：答案还不正确，请检查后重试。"
        JudgeOutcome.PARTIALLY_CORRECT -> "本地判题：答案部分正确。"
        JudgeOutcome.INVALID_INPUT -> "本地判题：当前输入格式无法识别。"
        null -> ""
    }

private fun localResultColor(page: AssessmentQuestionPageState) =
    when (page.progress.latestJudgeResult?.outcome) {
        JudgeOutcome.CORRECT -> InteractiveGreen
        JudgeOutcome.INCORRECT -> InteractiveRed
        JudgeOutcome.PARTIALLY_CORRECT,
        JudgeOutcome.INVALID_INPUT,
        -> InteractiveYellow
        null -> InteractiveMuted
    }

private fun CourseAssessmentQuestion.aiPrompt(): String = buildString {
    appendLine("题目：")
    appendLine(stem.joinToString("\n") { it.plainText() })
    if (choices.isNotEmpty()) {
        appendLine("选项：")
        choices.forEach { choice ->
            appendLine("${choice.id}：${choice.content.joinToString("；") { it.plainText() }}")
        }
    }
    appendLine("课程标准答案：${referenceAnswer()}")
    explanationText().takeIf(String::isNotBlank)?.let {
        appendLine("课程编写者参考解析：$it")
    }
    append("请以标准答案为准，分析学习者答案；错误时指出已做对的部分和下一步，不要把模型自己的判断当成课程标准。")
}

private fun CourseAssessmentQuestion.referenceAnswer(): String = when (val rule = definition.answerRule) {
    is AnswerRule.ExactInteger -> rule.expected.toString()
    is AnswerRule.Decimal -> rule.expected.toString().trimEnd('0').trimEnd('.')
    is AnswerRule.RationalEquivalent -> {
        val value = rule.expected
        if (value.denominator.toString() == "1") value.numerator.toString()
        else "${value.numerator}/${value.denominator}"
    }
    is AnswerRule.SingleChoice -> {
        val choice = choices.firstOrNull { it.id == rule.expectedOptionId }
        val body = choice?.content?.joinToString("；") { it.plainText() }.orEmpty()
        if (body.isBlank()) rule.expectedOptionId else "${rule.expectedOptionId}：$body"
    }
    is AnswerRule.Coordinate -> {
        val x = rule.expectedX
        val y = rule.expectedY
        "(${x.asText()}, ${y.asText()})"
    }
}

private fun com.majortomman.school.learning.assessment.domain.RationalValue.asText(): String =
    if (denominator.toString() == "1") numerator.toString() else "$numerator/$denominator"

private fun CourseAssessmentQuestion.explanationText(): String =
    explanation.joinToString("；") { it.plainText() }.trim()

private fun UserAnswer?.displayText(question: CourseAssessmentQuestion): String = when (this) {
    is UserAnswer.Text -> raw.trim()
    is UserAnswer.Choice -> {
        val choiceText = question.choices.firstOrNull { it.id == optionId }
            ?.content
            ?.joinToString("；") { it.plainText() }
            .orEmpty()
        if (choiceText.isBlank()) optionId else "$optionId：$choiceText"
    }
    is UserAnswer.Coordinate -> "(${rawX.trim()}, ${rawY.trim()})"
    null -> ""
}

private fun LearningContent.plainText(): String = when (this) {
    is LearningContent.Heading -> text
    is LearningContent.Text -> text
    is LearningContent.Formula -> expression
    is LearningContent.ItemList -> items.joinToString("；")
    is LearningContent.Image -> altText
    is LearningContent.Table -> buildString {
        caption?.let { append("$it：") }
        append(columns.joinToString("、"))
        rows.forEach { row -> append("；").append(row.joinToString("、")) }
    }
    is LearningContent.Scene -> "题目中的交互数学图"
}
