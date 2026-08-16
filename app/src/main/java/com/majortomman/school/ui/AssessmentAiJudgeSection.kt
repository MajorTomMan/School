package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.net.SocketTimeoutException
import kotlinx.coroutines.launch

/**
 * Local judging and AI judging remain independent. AI never submits the local answer before a
 * complete, strictly parsed response says that the learner's answer is correct.
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
    var aiBusy by remember(question.definition.key) { mutableStateOf(false) }
    var evaluation by remember(question.definition.key) { mutableStateOf<AnswerEvaluation?>(null) }
    var error by remember(question.definition.key) { mutableStateOf<String?>(null) }
    var aiFailureCount by rememberSaveable(question.definition.key.value) { mutableIntStateOf(0) }
    var aiRejectedCount by rememberSaveable(question.definition.key.value) { mutableIntStateOf(0) }

    val answerText = page.draftAnswer.displayText(question)
    val localEnabled = page.canSubmit
    val aiEnabled = answerText.isNotBlank() && !aiBusy && !page.progress.answerLocked

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            JudgeAction(
                label = if (page.progress.answerLocked) "本题已完成" else "本地判题",
                enabled = localEnabled,
                color = InteractiveBlue,
                modifier = Modifier.weight(1f),
            ) {
                dispatch(AssessmentIntent.SubmitAnswer)
            }
            JudgeAction(
                label = if (aiBusy) "AI 正在判题" else "AI 判题",
                enabled = aiEnabled,
                color = InteractiveYellow,
                modifier = Modifier.weight(1f),
                busy = aiBusy,
            ) {
                evaluation = null
                error = null

                val configurationError = validateAiSettings(settings)
                if (configurationError != null) {
                    aiFailureCount += 1
                    error = "$configurationError（AI 失败 $aiFailureCount 次）"
                    return@JudgeAction
                }

                scope.launch {
                    aiBusy = true
                    runCatching {
                        OpenAiCompatibleClient(settings).evaluateAnswer(
                            question = question.aiPrompt(),
                            learnerAnswer = answerText,
                        )
                    }.fold(
                        onSuccess = { result ->
                            if (!result.completed) {
                                aiFailureCount += 1
                                error = "AI 返回 completed=false，本次结果无效；题目仍未通过。（AI 失败 $aiFailureCount 次）"
                            } else {
                                evaluation = result
                                if (result.answerCorrect) {
                                    // The strict AI flag gates this path. The deterministic local rule
                                    // records the attempt and prevents an inconsistent model response
                                    // from overriding the authored course answer.
                                    dispatch(AssessmentIntent.SubmitAnswer)
                                } else {
                                    aiRejectedCount += 1
                                }
                            }
                        },
                        onFailure = { failure ->
                            aiFailureCount += 1
                            error = "${aiFailureMessage(failure)}（AI 失败 $aiFailureCount 次）"
                        },
                    )
                    aiBusy = false
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(InteractiveBlue.copy(alpha = 0.58f)),
            )
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
                        if (value.answerCorrect) "AI 已完成核对：答案符合标准答案。" else "AI 已完成核对：答案暂不符合标准答案。",
                        color = if (value.answerCorrect) InteractiveGreen else InteractiveRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        value.feedback,
                        color = InteractiveWhite.copy(alpha = 0.88f),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    )
                    Text(
                        "AI 解释：${value.explanation}",
                        color = InteractiveWhite.copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                    )
                    value.mistakeType?.let { mistake ->
                        Text(
                            "错误类型：$mistake",
                            color = InteractiveYellow,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                        )
                    }
                    if (!value.answerCorrect) {
                        Text(
                            "AI 已判为不通过 $aiRejectedCount 次。可以修改答案后重试，或随时使用本地判题。",
                            color = InteractiveRed.copy(alpha = 0.88f),
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
                    "输入答案后可选择本地判题或 AI 判题。本地判题始终可用；AI 未配置、超时、网络失败或 JSON 不完整时，本题不会被认定为通过。",
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
                        "课程解析：$explanation",
                        color = InteractiveWhite.copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                    )
                }
            }

            if (aiFailureCount > 0) {
                Text(
                    "AI 技术或协议失败累计：$aiFailureCount 次。失败不会锁定答案，也不会影响本地判题。",
                    color = InteractiveMuted.copy(alpha = 0.86f),
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                )
            }
            Text(
                "AI 只有在 completed=true 且 answer_correct=true 时才会进入通过流程；课程本地规则仍会做最终一致性确认。",
                color = InteractiveMuted.copy(alpha = 0.78f),
                fontSize = 11.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun JudgeAction(
    label: String,
    enabled: Boolean,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.height(21.dp),
                color = color,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                label,
                color = if (enabled) color else InteractiveMuted.copy(alpha = 0.46f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (enabled || busy) 2.dp else 1.dp)
                .background(if (enabled || busy) color.copy(alpha = 0.76f) else InteractiveLine),
        )
    }
}

private fun validateAiSettings(settings: AiSettings): String? {
    val endpoint = settings.endpoint.trim()
    return when {
        endpoint.isBlank() -> "AI 接口地址未配置，本次未发起请求，题目仍为未解答"
        !endpoint.startsWith("http://") && !endpoint.startsWith("https://") ->
            "AI 接口地址格式无效，本次未发起请求，题目仍为未解答"
        settings.model.isBlank() -> "AI 模型名称未配置，本次未发起请求，题目仍为未解答"
        else -> null
    }
}

private fun aiFailureMessage(error: Throwable): String = when {
    error is SocketTimeoutException || error.message.orEmpty().contains("timed out", ignoreCase = true) ->
        "AI 请求超时，本次按未解答处理；可以重试或直接使用本地判题"
    error is IllegalArgumentException ->
        "AI 协议校验失败：${error.message ?: "返回数据不完整"}；本次按未解答处理"
    else -> "AI 请求失败：${error.message ?: error::class.java.simpleName}；本次按未解答处理"
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
    appendLine("教学阶段：中国中学课程。")
    append("只能使用中学阶段知识核对和解释，不得引入超出中学课程范围的知识。")
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
    is LearningContent.Visualization -> visualization.texts.keys.sorted().map { visualization.texts.text(it) }.filter(String::isNotBlank).joinToString("；").ifBlank { "题目中的可视化图" }
}
