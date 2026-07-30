package com.majortomman.school.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.assessment.application.AssessmentCompletion
import com.majortomman.school.learning.assessment.application.AssessmentIntent
import com.majortomman.school.learning.assessment.application.AssessmentQuestionPageState
import com.majortomman.school.learning.assessment.application.AssessmentSessionController
import com.majortomman.school.learning.assessment.application.AssessmentState
import com.majortomman.school.learning.assessment.contract.CourseAssessmentQuestion
import com.majortomman.school.learning.assessment.contract.CourseAssessmentQuestionSet
import com.majortomman.school.learning.assessment.contract.KnowledgePointDefinition
import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.JudgeOutcome
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionCompletionStatus
import com.majortomman.school.learning.assessment.domain.UserAnswer
import com.majortomman.school.learning.assessment.persistence.AssessmentProgressStore
import com.majortomman.school.learning.assessment.persistence.RoomAssessmentSessionGateway
import com.majortomman.school.learning.content.ContentAssetId
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun AssessmentSessionScreen(
    courseId: String,
    contentRevision: String,
    questionSet: CourseAssessmentQuestionSet,
    assetFiles: Map<ContentAssetId, File>,
    knowledgePoints: Map<KnowledgePointId, KnowledgePointDefinition>,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(courseId, contentRevision, questionSet.id) {
        AssessmentSessionController(
            courseId = courseId,
            contentRevision = contentRevision,
            questionSet = questionSet.toDomainDefinition(),
            gateway = RoomAssessmentSessionGateway(AssessmentProgressStore.create(context)),
        )
    }
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    val dispatch: (AssessmentIntent) -> Unit = remember(controller, scope) {
        { intent -> scope.launch { controller.dispatch(intent) } }
    }

    LaunchedEffect(controller) { controller.dispatch(AssessmentIntent.Initialize) }
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InteractiveBlack)
            .systemBarsPadding(),
    ) {
        when (val value = state) {
            AssessmentState.Idle,
            AssessmentState.Loading,
            -> AssessmentLoading()

            is AssessmentState.Question -> AssessmentQuestionPage(
                page = value.page,
                questionSet = questionSet,
                assetFiles = assetFiles,
                onBack = onBack,
                dispatch = dispatch,
            )

            is AssessmentState.FinishConfirmation -> {
                AssessmentQuestionPage(
                    page = value.page,
                    questionSet = questionSet,
                    assetFiles = assetFiles,
                    onBack = onBack,
                    dispatch = dispatch,
                )
                FinishConfirmationDialog(
                    incompleteCount = value.incompleteQuestionIndices.size,
                    busy = value.busy,
                    dispatch = dispatch,
                )
            }

            is AssessmentState.Result -> AssessmentResultPage(
                title = questionSet.title,
                completion = value.completion,
                knowledgePoints = knowledgePoints,
                onFinished = onFinished,
            )

            is AssessmentState.Error -> {
                when (val previous = value.previous) {
                    is AssessmentState.Question -> AssessmentQuestionPage(
                        page = previous.page,
                        questionSet = questionSet,
                        assetFiles = assetFiles,
                        onBack = onBack,
                        dispatch = dispatch,
                    )
                    is AssessmentState.FinishConfirmation -> AssessmentQuestionPage(
                        page = previous.page,
                        questionSet = questionSet,
                        assetFiles = assetFiles,
                        onBack = onBack,
                        dispatch = dispatch,
                    )
                    is AssessmentState.Result -> AssessmentResultPage(
                        title = questionSet.title,
                        completion = previous.completion,
                        knowledgePoints = knowledgePoints,
                        onFinished = onFinished,
                    )
                    AssessmentState.Idle,
                    AssessmentState.Loading,
                    is AssessmentState.Error,
                    -> AssessmentLoading()
                }
                ErrorDialog(value.message) { dispatch(AssessmentIntent.DismissError) }
            }
        }
    }
}

@Composable
private fun AssessmentLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = InteractiveBlue)
    }
}

@Composable
private fun AssessmentQuestionPage(
    page: AssessmentQuestionPageState,
    questionSet: CourseAssessmentQuestionSet,
    assetFiles: Map<ContentAssetId, File>,
    onBack: () -> Unit,
    dispatch: (AssessmentIntent) -> Unit,
) {
    val richQuestion = questionSet.questions[page.questionIndex]
    Column(Modifier.fillMaxSize()) {
        AssessmentHeader(
            title = questionSet.title,
            page = page,
            onBack = onBack,
            onQuestionSelected = { dispatch(AssessmentIntent.GoToQuestion(it)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = richQuestion.definition.number,
                color = InteractiveBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            AssessmentLearningContentList(
                content = richQuestion.stem,
                assetFiles = assetFiles,
                compact = false,
            )
            AnswerArea(page, richQuestion, assetFiles, dispatch)
            AssessmentAiJudgeSection(page, richQuestion, dispatch)
            JudgeFeedback(page)
            HintAndExplanationArea(page, richQuestion, assetFiles, dispatch)
            Spacer(Modifier.height(28.dp))
        }
        AssessmentBottomActions(page, questionSet, dispatch)
    }
}

@Composable
private fun AssessmentHeader(
    title: String,
    page: AssessmentQuestionPageState,
    onBack: () -> Unit,
    onQuestionSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "返回",
                modifier = Modifier.clickable(enabled = !page.busy, onClick = onBack).padding(vertical = 7.dp),
                color = InteractiveMuted,
                fontSize = 13.sp,
            )
            Text(
                title,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                color = InteractiveWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                "${page.questionIndex + 1} / ${page.questionCount}",
                color = InteractiveMuted,
                fontSize = 13.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            page.allProgress.forEachIndexed { index, progress ->
                val selected = index == page.questionIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 12.dp else 9.dp)
                        .background(progressColor(progress.completionStatus), CircleShape)
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = if (selected) InteractiveWhite else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable(enabled = !page.busy) { onQuestionSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun AnswerArea(
    page: AssessmentQuestionPageState,
    question: CourseAssessmentQuestion,
    assetFiles: Map<ContentAssetId, File>,
    dispatch: (AssessmentIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("你的答案", color = InteractiveWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        when (val input = question.definition.inputSpec) {
            AnswerInputSpec.Integer,
            is AnswerInputSpec.Decimal,
            is AnswerInputSpec.Rational,
            -> {
                val raw = (page.draftAnswer as? UserAnswer.Text)?.raw.orEmpty()
                AssessmentTextField(
                    label = when (input) {
                        AnswerInputSpec.Integer -> "输入整数"
                        is AnswerInputSpec.Decimal -> "输入小数"
                        is AnswerInputSpec.Rational -> "输入分数或等值小数"
                        else -> "输入答案"
                    },
                    value = raw,
                    modifier = Modifier.fillMaxWidth(),
                    page = page,
                    onValueChange = { value ->
                        dispatch(AssessmentIntent.AnswerChanged(value.takeIf(String::isNotBlank)?.let(UserAnswer::Text)))
                    },
                )
            }

            is AnswerInputSpec.SingleChoice -> {
                val selected = (page.draftAnswer as? UserAnswer.Choice)?.optionId
                question.choices.forEach { choice ->
                    val checked = choice.id == selected
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !page.busy && !page.progress.answerLocked) {
                                dispatch(AssessmentIntent.AnswerChanged(UserAnswer.Choice(choice.id)))
                            }
                            .padding(horizontal = 2.dp, vertical = 12.dp),
                    ) {
                        AssessmentLearningContentList(choice.content, assetFiles, compact = true)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .height(if (checked) 2.dp else 1.dp)
                                .background(if (checked) InteractiveBlue else InteractiveLine),
                        )
                    }
                }
            }

            AnswerInputSpec.Coordinate -> {
                val coordinate = page.draftAnswer as? UserAnswer.Coordinate
                val x = coordinate?.rawX.orEmpty()
                val y = coordinate?.rawY.orEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AssessmentTextField("x", x, Modifier.weight(1f), page) { nextX ->
                        dispatch(coordinateAnswer(nextX, y))
                    }
                    AssessmentTextField("y", y, Modifier.weight(1f), page) { nextY ->
                        dispatch(coordinateAnswer(x, nextY))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentTextField(
    label: String,
    value: String,
    modifier: Modifier,
    page: AssessmentQuestionPageState,
    onValueChange: (String) -> Unit,
) {
    val enabled = !page.busy && !page.progress.answerLocked
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(label, color = InteractiveMuted, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(38.dp),
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = if (enabled) InteractiveWhite else InteractiveMuted,
                fontSize = 18.sp,
            ),
            cursorBrush = SolidColor(InteractiveBlue),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (enabled) InteractiveBlue.copy(alpha = 0.66f) else InteractiveLine),
        )
    }
}

private fun coordinateAnswer(x: String, y: String): AssessmentIntent = AssessmentIntent.AnswerChanged(
    if (x.isBlank() && y.isBlank()) null else UserAnswer.Coordinate(x, y),
)

@Composable
private fun JudgeFeedback(page: AssessmentQuestionPageState) {
    val result = page.progress.latestJudgeResult ?: return
    val color = when (result.outcome) {
        JudgeOutcome.CORRECT -> InteractiveGreen
        JudgeOutcome.INCORRECT -> InteractiveRed
        JudgeOutcome.PARTIALLY_CORRECT,
        JudgeOutcome.INVALID_INPUT,
        -> InteractiveYellow
    }
    val message = when (result.outcome) {
        JudgeOutcome.CORRECT -> if (page.progress.wrongAttemptCount > 0) {
            "这次答对了。本题此前答错 ${page.progress.wrongAttemptCount} 次。"
        } else {
            "回答正确。"
        }
        JudgeOutcome.INCORRECT -> "还不正确，本题已答错 ${page.progress.wrongAttemptCount} 次。"
        JudgeOutcome.PARTIALLY_CORRECT -> feedbackMessage(result.feedbackCode) +
            " 本题已记录 ${page.progress.wrongAttemptCount} 次错误尝试。"
        JudgeOutcome.INVALID_INPUT -> feedbackMessage(result.feedbackCode)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color.copy(alpha = 0.72f)))
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            color = color,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun HintAndExplanationArea(
    page: AssessmentQuestionPageState,
    question: CourseAssessmentQuestion,
    assetFiles: Map<ContentAssetId, File>,
    dispatch: (AssessmentIntent) -> Unit,
) {
    if (question.definition.hints.isEmpty() && question.explanation.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.definition.hints.forEachIndexed { index, hint ->
            val viewed = hint.id in page.progress.viewedHintIds
            if (viewed) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveBlue.copy(alpha = 0.54f)))
                    Text(
                        text = "提示 ${index + 1}：${hint.text}",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        color = InteractiveWhite.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                    )
                }
            } else {
                Text(
                    text = "查看提示 ${index + 1}",
                    modifier = Modifier
                        .clickable(enabled = !page.busy) { dispatch(AssessmentIntent.ViewHint(hint.id)) }
                        .padding(vertical = 6.dp),
                    color = InteractiveBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        val canViewExplanation = question.explanation.isNotEmpty() && page.progress.latestJudgeResult != null
        when {
            page.progress.explanationViewed -> {
                Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.58f)))
                Text("参考解析", color = InteractiveYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                AssessmentLearningContentList(question.explanation, assetFiles, compact = true)
            }
            canViewExplanation -> Text(
                text = "查看参考答案与完整解析",
                modifier = Modifier
                    .clickable(enabled = !page.busy) { dispatch(AssessmentIntent.ViewExplanation) }
                    .padding(vertical = 6.dp),
                color = InteractiveYellow,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AssessmentBottomActions(
    page: AssessmentQuestionPageState,
    questionSet: CourseAssessmentQuestionSet,
    dispatch: (AssessmentIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssessmentOutlineAction(
            label = "上一题",
            enabled = page.canGoPrevious,
            modifier = Modifier.weight(1f),
        ) { dispatch(AssessmentIntent.PreviousQuestion) }
        if (questionSet.allowSkip && !page.progress.answerLocked) {
            AssessmentOutlineAction(
                label = "跳过",
                enabled = !page.busy,
                modifier = Modifier.weight(1f),
                color = InteractiveMuted,
            ) { dispatch(AssessmentIntent.SkipQuestion) }
        }
        AssessmentOutlineAction(
            label = if (page.questionIndex == page.questionCount - 1) "检查并结算" else "下一题",
            enabled = page.canGoNext,
            modifier = Modifier.weight(1f),
        ) {
            dispatch(
                if (page.questionIndex == page.questionCount - 1) AssessmentIntent.RequestFinish
                else AssessmentIntent.NextQuestion,
            )
        }
    }
}

@Composable
private fun AssessmentOutlineAction(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    color: Color = InteractiveBlue,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) color else InteractiveMuted.copy(alpha = 0.42f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (enabled) 2.dp else 1.dp)
                .background(if (enabled) color.copy(alpha = 0.76f) else InteractiveLine),
        )
    }
}

@Composable
private fun FinishConfirmationDialog(
    incompleteCount: Int,
    busy: Boolean,
    dispatch: (AssessmentIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) dispatch(AssessmentIntent.CancelFinish) },
        title = { Text("还有 $incompleteCount 道题未完成") },
        text = { Text("可以返回补答，也可以直接结算。跳过题会计入正确率分母，但不会直接降低掌握度。") },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { dispatch(AssessmentIntent.ContinueIncomplete) }) {
                Text("返回补答")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { dispatch(AssessmentIntent.ConfirmFinish) }) {
                Text("直接结算")
            }
        },
    )
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("答题流程未完成") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
    )
}

@Composable
private fun AssessmentResultPage(
    title: String,
    completion: AssessmentCompletion,
    knowledgePoints: Map<KnowledgePointId, KnowledgePointDefinition>,
    onFinished: () -> Unit,
) {
    val summary = completion.summary
    val wrongQuestions = summary.questionResults.filter { it.wrongAttemptCount > 0 }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 26.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("题组完成", color = InteractiveBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(title, color = InteractiveWhite, fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveYellow.copy(alpha = 0.64f)))
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${(summary.firstCorrectRate * 100).roundToInt()}%",
                color = InteractiveYellow,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("首次正确率", color = InteractiveMuted, fontSize = 13.sp)
        }
        ResultMetricGrid(completion)

        if (completion.masteryUpdates.isNotEmpty()) {
            Text("掌握度变化", color = InteractiveWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            completion.masteryUpdates.forEach { update ->
                val titleText = knowledgePoints[update.knowledgePointId]?.title ?: update.knowledgePointId.value
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(titleText, modifier = Modifier.weight(1f), color = InteractiveWhite, fontSize = 14.sp)
                        Text(
                            "${(update.beforeScore * 100).roundToInt()} → ${(update.afterScore * 100).roundToInt()}",
                            color = if (update.afterScore >= update.beforeScore) InteractiveGreen else InteractiveRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
                }
            }
        }

        if (wrongQuestions.isNotEmpty()) {
            Text("本次做错过的题", color = InteractiveWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                wrongQuestions.joinToString("、") { result ->
                    val index = summary.questionResults.indexOf(result) + 1
                    "第 $index 题（${result.wrongAttemptCount} 次）"
                },
                color = InteractiveMuted,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        AssessmentOutlineAction(
            label = "完成并继续",
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveBlue,
            onClick = onFinished,
        )
    }
}

@Composable
private fun ResultMetricGrid(completion: AssessmentCompletion) {
    val summary = completion.summary
    val rows = listOf(
        "最终正确率" to "${(summary.finalCorrectRate * 100).roundToInt()}%",
        "完成率" to "${(summary.completionRate * 100).roundToInt()}%",
        "首次答对" to "${summary.firstCorrectCount} 题",
        "修改后答对" to "${summary.recoveredCorrectCount} 题",
        "最终答错" to "${summary.finalWrongCount} 题",
        "跳过" to "${summary.skippedCount} 题",
        "做错过的题目" to "${summary.wrongQuestionCount} 题",
        "错误提交总次数" to "${summary.wrongSubmissionCount} 次",
        "无效输入" to "${summary.invalidSubmissionCount} 次",
        "使用提示" to "${summary.hintViewCount} 次",
    )
    Column {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, color = InteractiveMuted, fontSize = 13.sp)
                Text(value, color = InteractiveWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        }
    }
}

private fun progressColor(status: QuestionCompletionStatus): Color = when (status) {
    QuestionCompletionStatus.FIRST_TRY_CORRECT -> InteractiveGreen
    QuestionCompletionStatus.RECOVERED_CORRECT -> InteractiveBlue
    QuestionCompletionStatus.FINAL_INCORRECT -> InteractiveRed
    QuestionCompletionStatus.SKIPPED -> InteractiveMuted
    QuestionCompletionStatus.UNANSWERED -> InteractiveLine
}

private fun feedbackMessage(code: String?): String = when (code) {
    "invalid_integer" -> "请输入完整整数。"
    "integer_out_of_range" -> "这个整数超出了可处理范围。"
    "invalid_decimal" -> "请输入有效的小数。"
    "invalid_rational" -> "请输入有效分数，且分母不能为 0。"
    "unknown_option" -> "请选择题目提供的选项。"
    "invalid_coordinate_x" -> "请输入完整的横坐标。"
    "invalid_coordinate_y" -> "请输入完整的纵坐标。"
    "coordinate_x_incorrect" -> "纵坐标正确，横坐标还不正确。"
    "coordinate_y_incorrect" -> "横坐标正确，纵坐标还不正确。"
    "answer_type_mismatch" -> "当前答案格式与题型不匹配。"
    else -> "答案还不正确，请再检查一下。"
}
