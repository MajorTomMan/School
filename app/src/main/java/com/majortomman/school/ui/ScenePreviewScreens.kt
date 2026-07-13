package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.DailyPlan
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.MasteryStatus
import kotlinx.coroutines.delay

private val SceneBlack = Color(0xFF050608)
private val SceneWhite = Color(0xFFF5F7FA)
private val SceneBlue = Color(0xFF2D7BFF)
private val SceneRed = Color(0xFFFF3B30)
private val SceneYellow = Color(0xFFFFCC00)

@Composable
fun SceneTodayScreen(
    plan: DailyPlan,
    lessons: List<Lesson>,
    progress: LearningProgress,
    onStartLesson: (String) -> Unit,
    onOpenPath: () -> Unit,
) {
    val lesson = lessons.first { it.id == plan.newLessonId }
    val lessonIndex = lessons.indexOfFirst { it.id == lesson.id }.coerceAtLeast(0)
    val progressTarget = (lessonIndex + 1f) / lessons.size.coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(900),
        label = "sceneTodayProgress",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SceneBlack),
    ) {
        TechGridBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TechLabel("SCHOOL / TODAY", SceneBlue)
                    Text(
                        "继续学习",
                        color = SceneWhite.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        lesson.title,
                        color = SceneWhite,
                        fontSize = 50.sp,
                        lineHeight = 54.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        lesson.subtitle,
                        color = SceneWhite.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${lessonIndex + 1} / ${lessons.size}",
                            color = SceneBlue,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "预计 ${plan.estimatedMinutes} 分钟",
                            color = SceneWhite.copy(alpha = 0.58f),
                        )
                    }
                    ProgressRail(animatedProgress)
                    Text(
                        "理解 → 动画观察 → 推导 → 练习",
                        color = SceneWhite.copy(alpha = 0.48f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                TechAction(
                    label = "进入动态课程",
                    color = SceneBlue,
                    onClick = { onStartLesson(lesson.id) },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TechLabel("TODAY / REVIEW", SceneYellow)
                        Text(
                            "查看全部路径",
                            modifier = Modifier.clickable(onClick = onOpenPath),
                            color = SceneBlue,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    plan.reviewItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) SceneRed else SceneYellow),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = SceneWhite, fontWeight = FontWeight.SemiBold)
                                Text(
                                    item.reason,
                                    color = SceneWhite.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(item.dueLabel, color = SceneWhite.copy(alpha = 0.46f))
                        }
                        ThinDivider()
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("已作答 ${progress.attempts} 次", color = SceneWhite.copy(alpha = 0.42f))
                    Text("正确率 ${progress.accuracyPercent}%", color = SceneYellow)
                }
            }
        }
    }
}

@Composable
fun SceneCoursePathScreen(
    lessons: List<Lesson>,
    onOpenLesson: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SceneBlack),
    ) {
        TechGridBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            TechLabel("KNOWLEDGE / ROUTE", SceneBlue)
            Spacer(Modifier.height(8.dp))
            Text(
                "有理数",
                color = SceneWhite,
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "节点不是菜单，而是理解的中转站。",
                color = SceneWhite.copy(alpha = 0.58f),
            )
            Spacer(Modifier.height(34.dp))

            lessons.forEachIndexed { index, lesson ->
                AnimatedPathNode(
                    index = index,
                    lesson = lesson,
                    onClick = { onOpenLesson(lesson.id) },
                )
                if (index != lessons.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(start = 25.dp)
                            .width(2.dp)
                            .height(54.dp)
                            .background(
                                if (lesson.status == MasteryStatus.MASTERED) SceneYellow.copy(alpha = 0.7f)
                                else SceneWhite.copy(alpha = 0.15f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedPathNode(
    index: Int,
    lesson: Lesson,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 90L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(360)) + slideInVertically(tween(420)) { it / 3 },
    ) {
        PathNode(lesson = lesson, onClick = onClick)
    }
}

@Composable
private fun PathNode(
    lesson: Lesson,
    onClick: () -> Unit,
) {
    val active = lesson.status == MasteryStatus.LEARNING
    val infinite = rememberInfiniteTransition(label = "scenePathPulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scenePathPulseValue",
    )
    val color = when (lesson.status) {
        MasteryStatus.MASTERED -> SceneYellow
        MasteryStatus.LEARNING -> SceneBlue
        MasteryStatus.NEEDS_REVIEW -> SceneRed
        MasteryStatus.NOT_STARTED -> SceneWhite.copy(alpha = 0.34f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .drawBehind {
                    drawCircle(color = color.copy(alpha = 0.16f), radius = size.minDimension * 0.64f)
                }
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when (lesson.status) {
                    MasteryStatus.MASTERED -> "✓"
                    MasteryStatus.LEARNING -> "→"
                    MasteryStatus.NEEDS_REVIEW -> "!"
                    MasteryStatus.NOT_STARTED -> "·"
                },
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                lesson.title,
                color = SceneWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                lesson.subtitle,
                color = SceneWhite.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            lesson.status.label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private enum class PreviewScene {
    QUESTION,
    OBSERVE,
    INFER,
}

@Composable
fun SceneLearningScreen(
    lesson: Lesson,
    aiSettings: AiSettings,
    progress: LearningProgress,
    onBack: () -> Unit,
    onRecordAttempt: (answer: String, correct: Boolean, feedback: String) -> Unit,
) {
    var legacyMode by rememberSaveable { mutableStateOf(false) }
    var sceneIndex by rememberSaveable { mutableIntStateOf(0) }

    if (legacyMode) {
        LearningScreen(
            lesson = lesson,
            aiSettings = aiSettings,
            progress = progress,
            onBack = onBack,
            onRecordAttempt = onRecordAttempt,
        )
        return
    }

    val scenes = PreviewScene.entries
    val scene = scenes[sceneIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SceneBlack)
            .systemBarsPadding(),
    ) {
        TechGridBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "← 返回",
                    modifier = Modifier.clickable(onClick = onBack),
                    color = SceneWhite.copy(alpha = 0.72f),
                )
                TechLabel("SCENE ${sceneIndex + 1} / ${scenes.size}", SceneBlue)
            }
            ProgressRail((sceneIndex + 1f) / scenes.size)
            AnimatedContent(
                targetState = scene,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(420)) { it / 4 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(300)) { -it / 5 })
                },
                label = "sceneLessonTransition",
            ) { current ->
                when (current) {
                    PreviewScene.QUESTION -> QuestionScene(lesson)
                    PreviewScene.OBSERVE -> ObserveScene()
                    PreviewScene.INFER -> InferScene()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sceneIndex > 0) {
                    TechAction(
                        label = "上一步",
                        color = SceneWhite.copy(alpha = 0.64f),
                        modifier = Modifier.weight(1f),
                        onClick = { sceneIndex -= 1 },
                    )
                }
                TechAction(
                    label = if (scene == PreviewScene.INFER) "进入现有练习" else "继续",
                    color = if (scene == PreviewScene.INFER) SceneYellow else SceneBlue,
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        if (scene == PreviewScene.INFER) legacyMode = true else sceneIndex += 1
                    },
                )
            }
        }
    }
}

@Composable
private fun QuestionScene(lesson: Lesson) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        TechLabel("QUESTION", SceneRed)
        Spacer(Modifier.height(18.dp))
        Text(
            "数，究竟是什么？",
            color = SceneWhite,
            fontSize = 48.sp,
            lineHeight = 54.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "在这一节里，数不再只是符号。\n它会变成空间中的位置。",
            color = SceneWhite.copy(alpha = 0.68f),
            fontSize = 20.sp,
            lineHeight = 30.sp,
        )
        Spacer(Modifier.height(34.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LargeToken("数", SceneBlue)
            LargeToken("→", SceneWhite.copy(alpha = 0.42f))
            LargeToken("位置", SceneYellow)
        }
        Spacer(Modifier.height(30.dp))
        Text(
            lesson.explanation,
            color = SceneWhite.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ObserveScene() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        TechLabel("OBSERVE", SceneBlue)
        Spacer(Modifier.height(10.dp))
        Text(
            "看位置，不要先算。",
            color = SceneWhite,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "-3 与 2 会依次落到数轴上。",
            color = SceneWhite.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(22.dp))
        AnimatedNumberLine()
        Text(
            "方向：左  ←  0  →  右",
            modifier = Modifier.fillMaxWidth(),
            color = SceneYellow,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InferScene() {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(900))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TechLabel("INFER", SceneYellow)
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("-3", color = SceneRed, fontSize = 58.sp, fontWeight = FontWeight.Black)
            Text(
                "<",
                color = SceneYellow.copy(alpha = reveal.value),
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer { scaleX = reveal.value; scaleY = reveal.value },
            )
            Text("2", color = SceneBlue, fontSize = 58.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(28.dp))
        ThinDivider(color = SceneWhite.copy(alpha = 0.22f))
        Spacer(Modifier.height(24.dp))
        Text(
            "右边的数更大。",
            color = SceneWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "结论不是弹出来的，\n而是从位置关系中自然生成。",
            color = SceneWhite.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            lineHeight = 25.sp,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "这是一半预览：后续练习、复习和设置暂时沿用现有界面。",
            color = SceneWhite.copy(alpha = 0.36f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AnimatedNumberLine() {
    val axis = remember { Animatable(0f) }
    val points = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        axis.animateTo(1f, animationSpec = tween(850))
        points.animateTo(1f, animationSpec = tween(700))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        val left = 28.dp.toPx()
        val right = size.width - 28.dp.toPx()
        val centerY = size.height * 0.52f
        val unit = (right - left) / 8f
        val axisEnd = left + (right - left) * axis.value

        for (row in 1..4) {
            val y = size.height * row / 5f
            drawLine(
                color = SceneWhite.copy(alpha = 0.045f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
        drawLine(
            color = SceneWhite.copy(alpha = 0.78f),
            start = Offset(left, centerY),
            end = Offset(axisEnd, centerY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val textPaint = Paint().apply {
            color = SceneWhite.copy(alpha = 0.56f).toArgb()
            textSize = 13.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        for (value in -4..4) {
            val x = left + (value + 4) * unit
            if (x <= axisEnd) {
                drawLine(
                    color = SceneWhite.copy(alpha = 0.5f),
                    start = Offset(x, centerY - 8.dp.toPx()),
                    end = Offset(x, centerY + 8.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    value.toString(),
                    x,
                    centerY + 31.dp.toPx(),
                    textPaint,
                )
            }
        }

        val minusThreeX = left + unit
        val twoX = left + unit * 6f
        val pointY = centerY - 28.dp.toPx() * (1f - points.value)
        drawCircle(
            color = SceneRed,
            radius = 10.dp.toPx() * points.value,
            center = Offset(minusThreeX, pointY),
        )
        drawCircle(
            color = SceneBlue,
            radius = 10.dp.toPx() * points.value,
            center = Offset(twoX, pointY),
        )

        val pointPaint = Paint().apply {
            textSize = 17.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        pointPaint.color = SceneRed.toArgb()
        drawContext.canvas.nativeCanvas.drawText("-3", minusThreeX, centerY - 22.dp.toPx(), pointPaint)
        pointPaint.color = SceneBlue.toArgb()
        drawContext.canvas.nativeCanvas.drawText("2", twoX, centerY - 22.dp.toPx(), pointPaint)
    }
}

@Composable
private fun TechGridBackground() {
    val transition = rememberInfiniteTransition(label = "techScan")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4200)),
        label = "techScanProgress",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grid = 32.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = SceneWhite.copy(alpha = 0.035f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            x += grid
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = SceneWhite.copy(alpha = 0.035f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += grid
        }
        val scanX = size.width * scan
        drawLine(
            color = SceneBlue.copy(alpha = 0.12f),
            start = Offset(scanX, 0f),
            end = Offset(scanX, size.height),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
private fun ProgressRail(progress: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        drawLine(
            color = SceneWhite.copy(alpha = 0.15f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = SceneBlue,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TechLabel(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 12.sp,
        letterSpacing = 1.8.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun LargeToken(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun ThinDivider(color: Color = SceneWhite.copy(alpha = 0.12f)) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
private fun TechAction(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .border(1.dp, color)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
    }
}
