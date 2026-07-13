package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import com.majortomman.school.data.material.InstalledMaterialPack
import com.majortomman.school.data.MasteryStatus
import kotlinx.coroutines.delay

private val SceneBlack = Color(0xFF000000)
private val SceneWhite = Color(0xFFF5F5F7)
private val SceneBlue = Color(0xFF0A84FF)
private val SceneRed = Color(0xFFFF453A)
private val SceneYellow = Color(0xFFFFD60A)

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
        label = "minimalTodayProgress",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SceneBlack),
    ) {
        MinimalAmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 26.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "今天",
                color = SceneWhite.copy(alpha = 0.42f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = lesson.title,
                    color = SceneWhite,
                    fontSize = 54.sp,
                    lineHeight = 57.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = lesson.subtitle,
                    color = SceneWhite.copy(alpha = 0.58f),
                    fontSize = 18.sp,
                    lineHeight = 27.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                MinimalProgress(
                    progress = animatedProgress,
                    leading = "${lessonIndex + 1} / ${lessons.size}",
                    trailing = "${plan.estimatedMinutes} 分钟",
                )
                MinimalAction(
                    label = "继续",
                    color = SceneBlue,
                    onClick = { onStartLesson(lesson.id) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${plan.reviewItems.size} 项复习",
                        color = if (plan.reviewItems.isEmpty()) SceneWhite.copy(alpha = 0.35f) else SceneYellow,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "查看路径",
                        modifier = Modifier.clickable(onClick = onOpenPath),
                        color = SceneWhite.copy(alpha = 0.54f),
                        fontSize = 13.sp,
                    )
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
    val mastered = lessons.count { it.status == MasteryStatus.MASTERED }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SceneBlack),
    ) {
        MinimalAmbientBackground()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 26.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "有理数",
                        color = SceneWhite,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$mastered / ${lessons.size}",
                        color = SceneWhite.copy(alpha = 0.38f),
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.height(52.dp))
            }

            itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                MinimalPathNode(
                    index = index,
                    lesson = lesson,
                    onClick = { onOpenLesson(lesson.id) },
                )
                if (index != lessons.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(start = 9.dp)
                            .width(1.dp)
                            .height(52.dp)
                            .background(
                                if (lesson.status == MasteryStatus.MASTERED) SceneYellow.copy(alpha = 0.65f)
                                else SceneWhite.copy(alpha = 0.12f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalPathNode(
    index: Int,
    lesson: Lesson,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    val active = lesson.status == MasteryStatus.LEARNING
    val pulseTransition = rememberInfiniteTransition(label = "minimalPathPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "minimalPathPulseValue",
    )
    val color = statusColor(lesson.status)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(320),
        label = "minimalPathAppear",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = lesson.title,
            color = if (active) SceneWhite else SceneWhite.copy(alpha = if (lesson.status == MasteryStatus.NOT_STARTED) 0.36f else 0.78f),
            fontSize = if (active) 27.sp else 21.sp,
            lineHeight = 31.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (active) {
            Text("继续", color = SceneBlue, fontSize = 13.sp)
        }
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
    installedMaterial: InstalledMaterialPack?,
    onOpenTextbook: (Int) -> Unit,
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
            installedMaterial = installedMaterial,
            onOpenTextbook = onOpenTextbook,
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
        MinimalAmbientBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    modifier = Modifier.clickable(onClick = onBack),
                    color = SceneWhite,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                )
                SceneDots(current = sceneIndex, total = scenes.size)
            }

            AnimatedContent(
                targetState = scene,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(360)) + slideInHorizontally(tween(460)) { it / 7 }) togetherWith
                        (fadeOut(tween(220)) + slideOutHorizontally(tween(360)) { -it / 8 })
                },
                label = "minimalSceneTransition",
            ) { current ->
                when (current) {
                    PreviewScene.QUESTION -> MinimalQuestionScene()
                    PreviewScene.OBSERVE -> MinimalObserveScene()
                    PreviewScene.INFER -> MinimalInferScene()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (sceneIndex > 0) "上一步" else "",
                    modifier = if (sceneIndex > 0) Modifier.clickable { sceneIndex -= 1 } else Modifier,
                    color = SceneWhite.copy(alpha = 0.42f),
                    fontSize = 15.sp,
                )
                MinimalAction(
                    label = if (scene == PreviewScene.INFER) "练习" else "继续",
                    color = if (scene == PreviewScene.INFER) SceneYellow else SceneBlue,
                    compact = true,
                    onClick = {
                        if (scene == PreviewScene.INFER) legacyMode = true else sceneIndex += 1
                    },
                )
            }
        }
    }
}

@Composable
private fun MinimalQuestionScene() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "数是什么？",
            color = SceneWhite,
            fontSize = 54.sp,
            lineHeight = 58.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("符号", color = SceneBlue, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text("→", color = SceneWhite.copy(alpha = 0.28f), fontSize = 24.sp)
            Text("位置", color = SceneYellow, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MinimalObserveScene() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "看位置。",
            modifier = Modifier.padding(horizontal = 8.dp),
            color = SceneWhite,
            fontSize = 38.sp,
            fontWeight = FontWeight.SemiBold,
        )
        MinimalNumberLine()
        Text(
            text = "左边更小，右边更大",
            modifier = Modifier.fillMaxWidth(),
            color = SceneWhite.copy(alpha = 0.46f),
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun MinimalInferScene() {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(850))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("-3", color = SceneRed, fontSize = 64.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "<",
                color = SceneWhite.copy(alpha = reveal.value),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.graphicsLayer {
                    scaleX = reveal.value
                    scaleY = reveal.value
                },
            )
            Text("2", color = SceneBlue, fontSize = 64.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(30.dp))
        Text(
            text = "因为 2 在右边。",
            color = SceneWhite.copy(alpha = 0.62f),
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun MinimalNumberLine() {
    val axis = remember { Animatable(0f) }
    val points = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        axis.animateTo(1f, animationSpec = tween(900))
        points.animateTo(1f, animationSpec = tween(620))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
    ) {
        val left = 24.dp.toPx()
        val right = size.width - 24.dp.toPx()
        val centerY = size.height * 0.54f
        val unit = (right - left) / 8f
        val axisEnd = left + (right - left) * axis.value

        drawLine(
            color = SceneWhite.copy(alpha = 0.72f),
            start = Offset(left, centerY),
            end = Offset(axisEnd, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val labelPaint = Paint().apply {
            color = SceneWhite.copy(alpha = 0.32f).toArgb()
            textSize = 12.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        for (value in -4..4) {
            val x = left + (value + 4) * unit
            if (x <= axisEnd) {
                drawLine(
                    color = SceneWhite.copy(alpha = if (value == 0) 0.7f else 0.28f),
                    start = Offset(x, centerY - 7.dp.toPx()),
                    end = Offset(x, centerY + 7.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
                if (value == 0) {
                    drawContext.canvas.nativeCanvas.drawText("0", x, centerY + 29.dp.toPx(), labelPaint)
                }
            }
        }

        val minusThreeX = left + unit
        val twoX = left + unit * 6f
        val landingY = centerY - 32.dp.toPx() * (1f - points.value)
        drawCircle(
            color = SceneRed.copy(alpha = points.value),
            radius = 8.dp.toPx() * points.value,
            center = Offset(minusThreeX, landingY),
        )
        drawCircle(
            color = SceneBlue.copy(alpha = points.value),
            radius = 8.dp.toPx() * points.value,
            center = Offset(twoX, landingY),
        )

        val pointPaint = Paint().apply {
            textSize = 18.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        pointPaint.color = SceneRed.copy(alpha = points.value).toArgb()
        drawContext.canvas.nativeCanvas.drawText("-3", minusThreeX, centerY - 20.dp.toPx(), pointPaint)
        pointPaint.color = SceneBlue.copy(alpha = points.value).toArgb()
        drawContext.canvas.nativeCanvas.drawText("2", twoX, centerY - 20.dp.toPx(), pointPaint)
    }
}

@Composable
private fun MinimalAmbientBackground() {
    val transition = rememberInfiniteTransition(label = "minimalAmbient")
    val motion by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "minimalAmbientMotion",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(
            x = size.width * (0.12f + motion * 0.76f),
            y = size.height * 0.34f,
        )
        drawCircle(
            color = SceneBlue.copy(alpha = 0.035f),
            radius = size.minDimension * 0.58f,
            center = center,
        )
        drawLine(
            color = SceneWhite.copy(alpha = 0.035f),
            start = Offset(0f, size.height * 0.78f),
            end = Offset(size.width, size.height * 0.78f),
            strokeWidth = 1f,
        )
    }
}

@Composable
private fun MinimalProgress(
    progress: Float,
    leading: String,
    trailing: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(leading, color = SceneBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(trailing, color = SceneWhite.copy(alpha = 0.36f), fontSize = 13.sp)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
        ) {
            drawLine(
                color = SceneWhite.copy(alpha = 0.12f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = SceneBlue,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SceneDots(
    current: Int,
    total: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == current) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (index == current) SceneBlue else SceneWhite.copy(alpha = 0.22f)),
            )
        }
    }
}

@Composable
private fun MinimalAction(
    label: String,
    color: Color,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = if (compact) 18.sp else 25.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "→",
            color = color,
            fontSize = if (compact) 19.sp else 27.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

private fun statusColor(status: MasteryStatus): Color = when (status) {
    MasteryStatus.MASTERED -> SceneYellow
    MasteryStatus.LEARNING -> SceneBlue
    MasteryStatus.NEEDS_REVIEW -> SceneRed
    MasteryStatus.NOT_STARTED -> SceneWhite.copy(alpha = 0.2f)
}
