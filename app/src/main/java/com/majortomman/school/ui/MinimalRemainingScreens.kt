@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.majortomman.school.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.ai.OpenAiCompatibleClient
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.AttemptRecord
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.ReviewItem
import com.majortomman.school.data.ScheduledReview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MinimalBlack = Color(0xFF050608)
private val MinimalWhite = Color(0xFFF5F7FA)
private val MinimalBlue = Color(0xFF2D7BFF)
private val MinimalRed = Color(0xFFFF3B30)
private val MinimalYellow = Color(0xFFFFCC00)
private val MinimalMuted = MinimalWhite.copy(alpha = 0.46f)
private val MinimalLine = MinimalWhite.copy(alpha = 0.13f)

private enum class MinimalLessonStep(val title: String) {
    INTUITION("核心直觉"),
    PITFALL("容易踩坑"),
    TEXTBOOK("教材定位"),
    PRACTICE("独立练习"),
    SUMMARY("完成"),
}

@Composable
fun MinimalLearningScreen(
    lesson: Lesson,
    aiSettings: AiSettings,
    progress: LearningProgress,
    onBack: () -> Unit,
    onRecordAttempt: (answer: String, correct: Boolean, feedback: String) -> Unit,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var helpVisible by rememberSaveable { mutableStateOf(false) }
    val steps = MinimalLessonStep.entries
    val step = steps[stepIndex]

    Scaffold(
        containerColor = MinimalBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MinimalBlack)
                    .systemBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${stepIndex + 1} / ${steps.size}",
                        color = MinimalMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                MinimalProgress(current = stepIndex, total = steps.size)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MinimalBlack)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedVisibility(
                    visible = helpVisible,
                    enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(220)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(MinimalYellow))
                        Text(
                            minimalHelp(step),
                            color = MinimalWhite.copy(alpha = 0.72f),
                            lineHeight = 23.sp,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinimalOutlinedAction(
                        label = "返回",
                        color = MinimalWhite.copy(alpha = 0.72f),
                        modifier = Modifier.weight(1f),
                        onClick = onBack,
                    )
                    MinimalOutlinedAction(
                        label = "我没看懂",
                        color = MinimalYellow,
                        modifier = Modifier.weight(1f),
                    ) {
                        helpVisible = !helpVisible
                    }
                    MinimalOutlinedAction(
                        label = "继续",
                        color = MinimalBlue,
                        modifier = Modifier.weight(1f),
                    ) {
                        helpVisible = false
                        if (step == MinimalLessonStep.SUMMARY) onBack()
                        else stepIndex = (stepIndex + 1).coerceAtMost(steps.lastIndex)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                step.title,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                color = MinimalWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInHorizontally(tween(340)) { it / 7 }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { -it / 8 })
                },
                label = "minimalLessonStep",
            ) { current ->
                when (current) {
                    MinimalLessonStep.INTUITION -> MinimalIntuition(lesson)
                    MinimalLessonStep.PITFALL -> MinimalPitfall(lesson)
                    MinimalLessonStep.TEXTBOOK -> MinimalTextbook(lesson)
                    MinimalLessonStep.PRACTICE -> MinimalPractice(
                        lesson = lesson,
                        settings = aiSettings,
                        progress = progress,
                        onRecordAttempt = onRecordAttempt,
                    )
                    MinimalLessonStep.SUMMARY -> MinimalSummary(lesson, progress)
                }
            }
        }
    }
}

@Composable
private fun MinimalIntuition(lesson: Lesson) = MinimalScroll {
    Spacer(Modifier.height(34.dp))
    Text(
        lesson.explanation,
        color = MinimalWhite,
        fontSize = 25.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(42.dp))
    lesson.objectives.forEachIndexed { index, objective ->
        LaunchedObjective(index = index, objective = objective)
        if (index != lesson.objectives.lastIndex) MinimalDivider()
    }
}

@Composable
private fun LaunchedObjective(index: Int, objective: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 130L + 120L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + expandVertically(tween(320)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                "%02d".format(index + 1),
                color = MinimalBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                objective,
                modifier = Modifier.weight(1f),
                color = MinimalWhite.copy(alpha = 0.78f),
                fontSize = 18.sp,
                lineHeight = 27.sp,
            )
        }
    }
}

@Composable
private fun MinimalPitfall(lesson: Lesson) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(900))
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "先停一下。",
            color = MinimalRed.copy(alpha = 0.35f + reveal.value * 0.65f),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(reveal.value)
                .height(2.dp)
                .background(MinimalRed),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            lesson.commonMistake,
            color = MinimalWhite,
            fontSize = 23.sp,
            lineHeight = 34.sp,
        )
        Spacer(Modifier.height(30.dp))
        Text(
            "红色只在错误出现时出现。\n看清错误后，它就应该退出视线。",
            color = MinimalMuted,
            lineHeight = 23.sp,
        )
    }
}

@Composable
private fun MinimalTextbook(lesson: Lesson) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("教材", color = MinimalMuted, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "${lesson.textbookPages.first}—${lesson.textbookPages.last}",
            color = MinimalYellow,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("页", color = MinimalWhite, fontSize = 25.sp)
        Spacer(Modifier.height(40.dp))
        MinimalDivider()
        Spacer(Modifier.height(24.dp))
        Text(
            "真实教材导入后，这里直接打开对应页。\n不再让你从整本书里寻找上下文。",
            color = MinimalWhite.copy(alpha = 0.67f),
            fontSize = 18.sp,
            lineHeight = 28.sp,
        )
    }
}

@Composable
private fun MinimalPractice(
    lesson: Lesson,
    settings: AiSettings,
    progress: LearningProgress,
    onRecordAttempt: (answer: String, correct: Boolean, feedback: String) -> Unit,
) {
    var answer by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var mistakeType by rememberSaveable { mutableStateOf<String?>(null) }
    var hint by rememberSaveable { mutableIntStateOf(0) }
    var isEvaluating by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val question = "在数轴上，-3 与 2 哪个数更大？请简单说明理由。"
    val hints = listOf(
        "先看左右，不要先计算。",
        "-3 在原点左边，2 在原点右边。",
        "数轴右边的数更大。",
    )

    LaunchedEffect(progress.lastLessonId, progress.lastAnswer) {
        if (progress.lastLessonId == lesson.id && answer.isBlank()) {
            answer = progress.lastAnswer
            result = progress.lastFeedback.takeIf { it.isNotBlank() }
        }
    }

    MinimalScroll {
        Spacer(Modifier.height(20.dp))
        Text(
            question,
            color = MinimalWhite,
            fontSize = 27.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(40.dp))
        MinimalInput(
            label = "你的答案",
            value = answer,
            onValueChange = { answer = it; result = null; mistakeType = null },
            minHeight = 120.dp,
        )
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MinimalOutlinedAction(
                label = "提示",
                color = MinimalYellow,
                modifier = Modifier.weight(1f),
            ) {
                hint = (hint + 1).coerceAtMost(hints.size)
            }
            MinimalOutlinedAction(
                label = "本地检查",
                color = MinimalBlue,
                enabled = answer.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                val compact = answer.replace(" ", "")
                val correct = compact.contains("2") && (compact.contains("大") || compact.contains(">"))
                val feedback = if (correct) {
                    "判断正确。2 位于 -3 的右侧。"
                } else {
                    "再检查两个数在数轴上的左右位置。"
                }
                result = feedback
                mistakeType = if (correct) null else "位置关系"
                onRecordAttempt(answer, correct, feedback)
            }
            MinimalOutlinedAction(
                label = if (isEvaluating) "批改中…" else "AI 批改",
                color = MinimalWhite.copy(alpha = 0.82f),
                enabled = answer.isNotBlank() && !isEvaluating && settings.endpoint.isNotBlank() && settings.model.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                isEvaluating = true
                result = "正在理解你的答案…"
                scope.launch {
                    val evaluation = runCatching {
                        OpenAiCompatibleClient(settings).evaluateAnswer(question, answer)
                    }.getOrElse { error ->
                        result = "AI 批改失败：${error.message ?: error::class.java.simpleName}"
                        mistakeType = "连接失败"
                        isEvaluating = false
                        return@launch
                    }
                    result = evaluation.feedback
                    mistakeType = evaluation.mistakeType
                    onRecordAttempt(answer, evaluation.correct, evaluation.feedback)
                    isEvaluating = false
                }
            }
        }
        AnimatedVisibility(
            visible = hint > 0,
            enter = fadeIn(tween(220)) + expandVertically(tween(260)),
            exit = fadeOut() + shrinkVertically(),
        ) {
            MinimalInlineNotice(
                color = MinimalYellow,
                label = "提示 $hint",
                body = hints[(hint - 1).coerceAtLeast(0)],
            )
        }
        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn(tween(240)) + expandVertically(tween(300)),
            exit = fadeOut() + shrinkVertically(),
        ) {
            MinimalInlineNotice(
                color = if (mistakeType == null) MinimalBlue else MinimalRed,
                label = mistakeType?.let { "需要检查 · $it" } ?: "反馈",
                body = result.orEmpty(),
            )
        }
    }
}

@Composable
private fun MinimalSummary(lesson: Lesson, progress: LearningProgress) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("完成", color = MinimalBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(
            lesson.title,
            color = MinimalWhite,
            fontSize = 46.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(34.dp))
        MinimalDivider()
        Spacer(Modifier.height(26.dp))
        lesson.objectives.forEach { objective ->
            Text(
                "—  $objective",
                modifier = Modifier.padding(vertical = 7.dp),
                color = MinimalWhite.copy(alpha = 0.7f),
                fontSize = 17.sp,
                lineHeight = 25.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "累计 ${progress.attempts} 次作答 · 正确率 ${progress.accuracyPercent}%",
            color = MinimalYellow,
        )
    }
}

@Composable
fun MinimalSettingsScreen(
    settings: AiSettings,
    onSave: (AiSettings) -> Unit,
    onClearProgress: () -> Unit,
) {
    var endpoint by rememberSaveable { mutableStateOf(settings.endpoint) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var apiKey by rememberSaveable { mutableStateOf(settings.apiKey) }
    var connectionStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var isTesting by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings) {
        endpoint = settings.endpoint
        model = settings.model
        apiKey = settings.apiKey
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MinimalBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 30.dp),
    ) {
        Text("设置", color = MinimalWhite, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(42.dp))
        MinimalSectionTitle("AI")
        MinimalInput("接口地址", endpoint, { endpoint = it; connectionStatus = null }, keyboardType = KeyboardType.Uri)
        Spacer(Modifier.height(22.dp))
        MinimalInput("模型", model, { model = it; connectionStatus = null })
        Spacer(Modifier.height(22.dp))
        MinimalInput(
            "API Key",
            apiKey,
            { apiKey = it },
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "测试连接",
                modifier = Modifier.clickable(enabled = !isTesting && endpoint.isNotBlank()) {
                    isTesting = true
                    connectionStatus = "正在连接…"
                    val updated = AiSettings(endpoint.trim(), model.trim(), apiKey.trim())
                    scope.launch {
                        connectionStatus = OpenAiCompatibleClient(updated).testConnection().fold(
                            onSuccess = { it },
                            onFailure = { "连接失败：${it.message ?: it::class.java.simpleName}" },
                        )
                        isTesting = false
                    }
                },
                color = MinimalMuted,
            )
            Text(
                "保存",
                modifier = Modifier.clickable(enabled = endpoint.isNotBlank() && model.isNotBlank()) {
                    onSave(AiSettings(endpoint.trim(), model.trim(), apiKey.trim()))
                    connectionStatus = "已保存"
                },
                color = MinimalBlue,
                fontWeight = FontWeight.SemiBold,
            )
        }
        AnimatedVisibility(connectionStatus != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            MinimalInlineNotice(
                color = if (connectionStatus.orEmpty().startsWith("连接失败")) MinimalRed else MinimalBlue,
                label = "状态",
                body = connectionStatus.orEmpty(),
            )
        }
        Spacer(Modifier.height(48.dp))
        MinimalSectionTitle("教材")
        Text("七年级数学上册", color = MinimalWhite, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("真实教材导入将在下一阶段接入。", color = MinimalMuted)
        Spacer(Modifier.height(48.dp))
        MinimalSectionTitle("学习数据")
        Text("答案、反馈和掌握状态保存在本机。", color = MinimalWhite.copy(alpha = 0.72f))
        Spacer(Modifier.height(18.dp))
        AnimatedContent(
            targetState = confirmClear,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "minimalClearData",
        ) { confirming ->
            if (!confirming) {
                Text(
                    "清空学习记录",
                    modifier = Modifier.clickable { confirmClear = true },
                    color = MinimalRed,
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("取消", modifier = Modifier.clickable { confirmClear = false }, color = MinimalMuted)
                    Text(
                        "确认清空",
                        modifier = Modifier.clickable { onClearProgress(); confirmClear = false },
                        color = MinimalRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun MinimalRoomReviewScreen(
    fallbackItems: List<ReviewItem>,
    progress: LearningProgress,
    scheduledReviews: List<ScheduledReview>,
    recentAttempts: List<AttemptRecord>,
    onOpenLesson: (String) -> Unit,
) {
    val current = scheduledReviews.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MinimalBlack),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 30.dp),
    ) {
        item {
            Text("复习", color = MinimalWhite, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(34.dp))
        }
        item {
            if (current != null) {
                Text(current.dueLabel, color = MinimalYellow, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    current.lessonTitle,
                    color = MinimalWhite,
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (current.lastCorrect) "快速确认一次。" else "重新看清错误发生的位置。",
                    color = MinimalMuted,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(28.dp))
                MinimalTextAction("开始复习", MinimalBlue) { onOpenLesson(current.lessonId) }
            } else if (fallbackItems.isNotEmpty()) {
                Text("今天没有必须完成的复习。", color = MinimalWhite, fontSize = 25.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Text(fallbackItems.first().title, color = MinimalMuted)
            } else {
                Text("今天没有复习任务。", color = MinimalWhite, fontSize = 25.sp)
            }
            Spacer(Modifier.height(36.dp))
            MinimalDivider()
            Spacer(Modifier.height(30.dp))
        }
        if (scheduledReviews.size > 1) {
            item { MinimalSectionTitle("接下来") }
            itemsIndexed(scheduledReviews.drop(1), key = { _, item -> item.lessonId }) { index, item ->
                MinimalReviewRow(
                    index = index,
                    title = item.lessonTitle,
                    trailing = item.dueLabel,
                    color = if (item.lastCorrect) MinimalBlue else MinimalRed,
                    onClick = { onOpenLesson(item.lessonId) },
                )
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MinimalSectionTitle("最近作答")
                Text("${progress.accuracyPercent}%", color = MinimalYellow, fontWeight = FontWeight.Bold)
            }
        }
        if (recentAttempts.isEmpty()) {
            item {
                Text("完成下一道练习后，这里会出现记录。", color = MinimalMuted)
            }
        } else {
            itemsIndexed(recentAttempts, key = { _, item -> item.id }) { index, item ->
                MinimalAttemptRow(index, item)
            }
        }
    }
}

@Composable
private fun MinimalReviewRow(
    index: Int,
    title: String,
    trailing: String,
    color: Color,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn() + expandVertically()) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.size(14.dp))
                Text(title, modifier = Modifier.weight(1f), color = MinimalWhite, fontSize = 18.sp)
                Text(trailing, color = MinimalMuted)
            }
            Spacer(Modifier.height(16.dp))
            MinimalDivider()
        }
    }
}

@Composable
private fun MinimalAttemptRow(index: Int, item: AttemptRecord) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val color = if (item.correct) MinimalBlue else MinimalRed
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(index + 1), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.lessonTitle, color = MinimalWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(item.createdLabel, color = MinimalMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(if (item.correct) "正确" else "复习", color = color)
        }
        AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(modifier = Modifier.padding(start = 34.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.feedback, color = MinimalWhite.copy(alpha = 0.72f), lineHeight = 22.sp)
                item.mistakeType?.let { Text("错误类型 · $it", color = MinimalRed) }
                Text("你的答案", color = MinimalMuted, style = MaterialTheme.typography.labelMedium)
                Text(item.answer.ifBlank { "未填写" }, color = MinimalWhite)
            }
        }
        Spacer(Modifier.height(16.dp))
        MinimalDivider()
    }
}

@Composable
private fun MinimalInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minHeight: androidx.compose.ui.unit.Dp = 52.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, color = MinimalMuted, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(minHeight),
            textStyle = TextStyle(color = MinimalWhite, fontSize = 18.sp, lineHeight = 26.sp),
            cursorBrush = SolidColor(MinimalBlue),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.TopStart) {
                    if (value.isEmpty()) Text("输入…", color = MinimalWhite.copy(alpha = 0.2f), fontSize = 18.sp)
                    inner()
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(MinimalLine))
    }
}

@Composable
private fun MinimalInlineNotice(color: Color, label: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
        Text(label, color = color, fontWeight = FontWeight.Bold)
        Text(body, color = MinimalWhite.copy(alpha = 0.74f), lineHeight = 23.sp)
    }
}

@Composable
private fun MinimalOutlinedAction(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) color else MinimalMuted.copy(alpha = 0.52f)
    val borderColor = if (enabled) color.copy(alpha = 0.82f) else MinimalLine
    Box(
        modifier = modifier
            .height(48.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun MinimalTextAction(label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, color = color, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
    }
}

@Composable
private fun MinimalSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(bottom = 18.dp),
        color = MinimalBlue,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun MinimalProgress(current: Int, total: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(if (index <= current) MinimalBlue else MinimalLine),
            )
        }
    }
}

@Composable
private fun MinimalDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MinimalLine))
}

@Composable
private fun MinimalScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        content = content,
    )
}

private fun minimalHelp(step: MinimalLessonStep): String = when (step) {
    MinimalLessonStep.INTUITION -> "先不要背定义。只抓住：数轴右边更大，左边更小。"
    MinimalLessonStep.PITFALL -> "看到负号时，先回到位置关系，不要只比较数字表面大小。"
    MinimalLessonStep.TEXTBOOK -> "只看对应页的定义和例题，不必一次读完整章。"
    MinimalLessonStep.PRACTICE -> "先写结论，再补一句理由。理由不完整也比空白更容易诊断。"
    MinimalLessonStep.SUMMARY -> "能说出这节课最重要的一句话，就已经完成了一次有效学习。"
}
