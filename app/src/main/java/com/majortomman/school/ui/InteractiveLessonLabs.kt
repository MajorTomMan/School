package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@Composable
internal fun LinearFunctionLab(lessonId: String) {
    var k by rememberSaveable(lessonId, "linear-k") { mutableFloatStateOf(1.5f) }
    var b by rememberSaveable(lessonId, "linear-b") { mutableFloatStateOf(1f) }
    var xText by rememberSaveable(lessonId, "linear-x") { mutableStateOf("2") }
    val xValue = xText.toFloatOrNull() ?: 0f
    val animatedK by animateFloatAsState(k, tween(320), label = "linearK")
    val animatedB by animateFloatAsState(b, tween(320), label = "linearB")
    val animatedX by animateFloatAsState(xValue, tween(280), label = "linearX")
    val yValue = animatedK * animatedX + animatedB

    SectionTitle("动态函数实验", InteractiveBlue)
    Spacer(Modifier.height(12.dp))
    Text(
        "拖动 k 和 b，直线会连续旋转和平移；输入 x 后，数据点、公式和结果同步更新。",
        color = InteractiveWhite.copy(alpha = 0.75f),
        fontSize = 16.sp,
        lineHeight = 25.sp,
    )
    Spacer(Modifier.height(18.dp))

    LinearFunctionGraph(k = animatedK, b = animatedB, x = animatedX)
    Spacer(Modifier.height(18.dp))

    ValueReadout(
        expression = "y = ${formatNumber(animatedK)} × ${formatNumber(animatedX)} ${signedNumber(animatedB)}",
        result = "y = ${formatNumber(yValue)}",
    )
    Spacer(Modifier.height(20.dp))

    ParameterSlider(
        label = "斜率 k",
        value = k,
        valueRange = -5f..5f,
        color = InteractiveBlue,
        onValueChange = { k = snapTenth(it) },
    )
    Spacer(Modifier.height(14.dp))
    ParameterSlider(
        label = "截距 b",
        value = b,
        valueRange = -5f..5f,
        color = InteractiveYellow,
        onValueChange = { b = snapTenth(it) },
    )
    Spacer(Modifier.height(14.dp))
    NumericInput(
        label = "输入真实 x 值",
        value = xText,
        onValueChange = { next ->
            if (next.length <= 9 && next.matches(Regex("-?\\d*(\\.\\d*)?"))) xText = next
        },
    )

    Spacer(Modifier.height(18.dp))
    val interpretation = when {
        abs(k) < 0.001f -> "k=0：图像是水平直线，y 不随 x 改变。"
        k > 0f -> "k>0：x 增大时 y 增大，直线从左向右上升。"
        else -> "k<0：x 增大时 y 减小，直线从左向右下降。"
    }
    Text(interpretation, color = InteractiveGreen, fontSize = 15.sp, lineHeight = 23.sp)
}

@Composable
private fun LinearFunctionGraph(k: Float, b: Float, x: Float) {
    val xMin = -6f
    val xMax = 6f
    val yMin = -6f
    val yMax = 6f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .background(InteractivePanel, RoundedCornerShape(18.dp))
            .border(1.dp, InteractiveLine, RoundedCornerShape(18.dp))
            .padding(10.dp),
    ) {
        val left = 30.dp.toPx()
        val right = size.width - 18.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        fun sx(worldX: Float): Float = left + (worldX - xMin) / (xMax - xMin) * (right - left)
        fun sy(worldY: Float): Float = bottom - (worldY - yMin) / (yMax - yMin) * (bottom - top)

        for (grid in -6..6) {
            val gx = sx(grid.toFloat())
            val gy = sy(grid.toFloat())
            drawLine(InteractiveLine.copy(alpha = 0.65f), Offset(gx, top), Offset(gx, bottom), 1.dp.toPx())
            drawLine(InteractiveLine.copy(alpha = 0.65f), Offset(left, gy), Offset(right, gy), 1.dp.toPx())
        }

        drawLine(InteractiveWhite.copy(alpha = 0.66f), Offset(left, sy(0f)), Offset(right, sy(0f)), 2.dp.toPx(), StrokeCap.Round)
        drawLine(InteractiveWhite.copy(alpha = 0.66f), Offset(sx(0f), top), Offset(sx(0f), bottom), 2.dp.toPx(), StrokeCap.Round)

        val yAtMin = k * xMin + b
        val yAtMax = k * xMax + b
        drawLine(
            color = InteractiveBlue,
            start = Offset(sx(xMin), sy(yAtMin)),
            end = Offset(sx(xMax), sy(yAtMax)),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val pointY = k * x + b
        if (x in xMin..xMax && pointY in yMin..yMax) {
            drawLine(InteractiveYellow.copy(alpha = 0.48f), Offset(sx(x), sy(0f)), Offset(sx(x), sy(pointY)), 2.dp.toPx())
            drawLine(InteractiveYellow.copy(alpha = 0.48f), Offset(sx(0f), sy(pointY)), Offset(sx(x), sy(pointY)), 2.dp.toPx())
            drawCircle(InteractiveYellow, 7.dp.toPx(), Offset(sx(x), sy(pointY)))
        }

        val triangleX0 = -0.5f
        val triangleX1 = 0.5f
        val triangleY0 = k * triangleX0 + b
        val triangleY1 = k * triangleX1 + b
        if (triangleY0 in yMin..yMax && triangleY1 in yMin..yMax) {
            val a = Offset(sx(triangleX0), sy(triangleY0))
            val c = Offset(sx(triangleX1), sy(triangleY0))
            val d = Offset(sx(triangleX1), sy(triangleY1))
            drawLine(InteractiveGreen, a, c, 3.dp.toPx(), StrokeCap.Round)
            drawLine(InteractiveRed, c, d, 3.dp.toPx(), StrokeCap.Round)
        }

        val labelPaint = Paint().apply {
            color = InteractiveMuted.toArgb()
            textSize = 12.sp.toPx()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("x", right - 3.dp.toPx(), sy(0f) - 8.dp.toPx(), labelPaint)
        drawContext.canvas.nativeCanvas.drawText("y", sx(0f) + 10.dp.toPx(), top + 8.dp.toPx(), labelPaint)
    }
}

@Composable
internal fun NewtonFirstLawLab(lessonId: String) {
    var initialVelocity by rememberSaveable(lessonId, "newton-v") { mutableFloatStateOf(6f) }
    var friction by rememberSaveable(lessonId, "newton-mu") { mutableFloatStateOf(0.18f) }
    var elapsed by rememberSaveable(lessonId, "newton-t") { mutableFloatStateOf(0f) }
    var running by rememberSaveable(lessonId, "newton-running") { mutableStateOf(false) }

    val accelerationMagnitude = friction * 9.8f
    val stopTime = if (accelerationMagnitude > 0.0001f) initialVelocity / accelerationMagnitude else Float.POSITIVE_INFINITY
    val effectiveTime = min(elapsed, stopTime)
    val velocity = max(0f, initialVelocity - accelerationMagnitude * elapsed)
    val position = initialVelocity * effectiveTime - 0.5f * accelerationMagnitude * effectiveTime * effectiveTime

    LaunchedEffect(running, initialVelocity, friction) {
        if (!running) return@LaunchedEffect
        val offset = elapsed
        val start = withFrameNanos { it }
        while (isActive && running) {
            withFrameNanos { now -> elapsed = offset + (now - start) / 1_000_000_000f }
            val reachedStop = accelerationMagnitude > 0.0001f && elapsed >= stopTime + 0.35f
            if (reachedStop || elapsed >= 8f) running = false
        }
    }

    fun reset() {
        running = false
        elapsed = 0f
    }

    SectionTitle("伽利略式思想实验", InteractiveBlue)
    Spacer(Modifier.height(12.dp))
    Text(
        "保持初速度不变，只改变摩擦系数。观察摩擦逐渐减小时，物体为什么能运动得更久、更远。",
        color = InteractiveWhite.copy(alpha = 0.75f),
        fontSize = 16.sp,
        lineHeight = 25.sp,
    )
    Spacer(Modifier.height(18.dp))

    NewtonMotionCanvas(
        initialVelocity = initialVelocity,
        friction = friction,
        elapsed = elapsed,
        position = position,
        velocity = velocity,
    )
    Spacer(Modifier.height(14.dp))
    VelocityTimeGraph(
        initialVelocity = initialVelocity,
        friction = friction,
        elapsed = elapsed,
        currentVelocity = velocity,
    )
    Spacer(Modifier.height(18.dp))

    ValueReadout(
        expression = if (friction < 0.001f) "ΣF = 0，a = 0" else "a = -μg = -${formatNumber(accelerationMagnitude)} m/s²",
        result = "t=${formatNumber(elapsed)} s  ·  v=${formatNumber(velocity)} m/s  ·  x=${formatNumber(position)} m",
    )
    Spacer(Modifier.height(20.dp))

    ParameterSlider(
        label = "初速度 v₀",
        value = initialVelocity,
        valueRange = 2f..12f,
        color = InteractiveBlue,
        onValueChange = { initialVelocity = snapTenth(it); reset() },
    )
    Spacer(Modifier.height(14.dp))
    ParameterSlider(
        label = "摩擦系数 μ",
        value = friction,
        valueRange = 0f..0.6f,
        color = InteractiveRed,
        onValueChange = { friction = snapHundredth(it); reset() },
    )
    Spacer(Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InteractiveAction(
            label = if (running) "暂停" else if (elapsed > 0f) "继续" else "播放",
            color = InteractiveGreen,
            modifier = Modifier.weight(1f),
        ) { running = !running }
        InteractiveAction(
            label = "重置",
            color = InteractiveMuted,
            modifier = Modifier.weight(1f),
            onClick = ::reset,
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        if (friction < 0.001f) {
            "摩擦为 0：速度—时间图保持水平。物体不需要持续受力来维持匀速运动。"
        } else {
            "物体最终停下是因为摩擦力持续改变速度；把摩擦调小，停止时间和滑行距离都会增加。"
        },
        color = if (friction < 0.001f) InteractiveGreen else InteractiveYellow,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
}

@Composable
private fun NewtonMotionCanvas(
    initialVelocity: Float,
    friction: Float,
    elapsed: Float,
    position: Float,
    velocity: Float,
) {
    val stopDistance = if (friction > 0.0001f) initialVelocity * initialVelocity / (2f * friction * 9.8f) else initialVelocity * 8f
    val worldMax = max(12f, stopDistance * 1.18f)
    val fraction = (position / worldMax).coerceIn(0f, 1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(InteractivePanel, RoundedCornerShape(18.dp))
            .border(1.dp, InteractiveLine, RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        val left = 28.dp.toPx()
        val right = size.width - 28.dp.toPx()
        val roadY = size.height * 0.68f
        drawLine(InteractiveWhite.copy(alpha = 0.45f), Offset(left, roadY), Offset(right, roadY), 3.dp.toPx(), StrokeCap.Round)

        for (index in 0..8) {
            val x = left + (right - left) * index / 8f
            drawLine(InteractiveLine, Offset(x, roadY + 8.dp.toPx()), Offset(x, roadY + 18.dp.toPx()), 1.dp.toPx())
        }

        val ballX = left + fraction * (right - left)
        val ballCenter = Offset(ballX, roadY - 18.dp.toPx())
        drawCircle(InteractiveBlue.copy(alpha = 0.16f), 25.dp.toPx(), ballCenter)
        drawCircle(InteractiveBlue, 16.dp.toPx(), ballCenter)

        val velocityArrow = (velocity / 12f).coerceIn(0f, 1f) * 100.dp.toPx()
        if (velocityArrow > 2.dp.toPx()) {
            drawLine(
                InteractiveGreen,
                Offset(ballX, roadY - 54.dp.toPx()),
                Offset(ballX + velocityArrow, roadY - 54.dp.toPx()),
                4.dp.toPx(),
                StrokeCap.Round,
            )
        }
        if (friction > 0.001f && velocity > 0.01f) {
            val frictionArrow = max(20.dp.toPx(), friction / 0.6f * 75.dp.toPx())
            drawLine(
                InteractiveRed,
                Offset(ballX, roadY - 82.dp.toPx()),
                Offset(ballX - frictionArrow, roadY - 82.dp.toPx()),
                4.dp.toPx(),
                StrokeCap.Round,
            )
        }

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 13.sp.toPx()
            color = InteractiveMuted.toArgb()
        }
        drawContext.canvas.nativeCanvas.drawText("速度", left, 30.dp.toPx(), paint)
        paint.color = InteractiveGreen.toArgb()
        drawContext.canvas.nativeCanvas.drawText("v", ballX + 6.dp.toPx(), roadY - 62.dp.toPx(), paint)
        if (friction > 0.001f) {
            paint.color = InteractiveRed.toArgb()
            drawContext.canvas.nativeCanvas.drawText("摩擦力", max(left, ballX - 74.dp.toPx()), roadY - 91.dp.toPx(), paint)
        }
        paint.color = InteractiveMuted.toArgb()
        drawContext.canvas.nativeCanvas.drawText("${formatNumber(elapsed)} s", left, size.height - 8.dp.toPx(), paint)
    }
}

@Composable
private fun VelocityTimeGraph(
    initialVelocity: Float,
    friction: Float,
    elapsed: Float,
    currentVelocity: Float,
) {
    val a = friction * 9.8f
    val stopTime = if (a > 0.0001f) initialVelocity / a else 8f
    val timeMax = max(4f, min(8f, ceil(stopTime + 1f)))

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(InteractivePanel, RoundedCornerShape(18.dp))
            .border(1.dp, InteractiveLine, RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        val left = 34.dp.toPx()
        val right = size.width - 18.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        fun sx(t: Float): Float = left + (t / timeMax).coerceIn(0f, 1f) * (right - left)
        fun sy(v: Float): Float = bottom - (v / 12f).coerceIn(0f, 1f) * (bottom - top)

        drawLine(InteractiveWhite.copy(alpha = 0.58f), Offset(left, bottom), Offset(right, bottom), 2.dp.toPx())
        drawLine(InteractiveWhite.copy(alpha = 0.58f), Offset(left, top), Offset(left, bottom), 2.dp.toPx())

        val endTime = min(timeMax, stopTime)
        val endVelocity = if (friction < 0.001f) initialVelocity else 0f
        drawLine(
            InteractiveBlue,
            Offset(sx(0f), sy(initialVelocity)),
            Offset(sx(endTime), sy(endVelocity)),
            4.dp.toPx(),
            StrokeCap.Round,
        )
        if (friction > 0.001f && stopTime < timeMax) {
            drawLine(InteractiveBlue, Offset(sx(stopTime), bottom), Offset(right, bottom), 4.dp.toPx(), StrokeCap.Round)
        }

        val pointTime = min(elapsed, timeMax)
        drawCircle(InteractiveYellow, 6.dp.toPx(), Offset(sx(pointTime), sy(currentVelocity)))
        drawLine(InteractiveYellow.copy(alpha = 0.35f), Offset(sx(pointTime), bottom), Offset(sx(pointTime), sy(currentVelocity)), 2.dp.toPx())

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 12.sp.toPx()
            color = InteractiveMuted.toArgb()
        }
        drawContext.canvas.nativeCanvas.drawText("v / (m·s⁻¹)", left, top - 3.dp.toPx(), paint)
        drawContext.canvas.nativeCanvas.drawText("t / s", right - 28.dp.toPx(), bottom + 23.dp.toPx(), paint)
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InteractivePanel, RoundedCornerShape(14.dp))
            .border(1.dp, InteractiveLine, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = InteractiveMuted, fontSize = 14.sp)
            Text(formatNumber(value), color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun NumericInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InteractivePanel, RoundedCornerShape(14.dp))
            .border(1.dp, InteractiveLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(label, color = InteractiveMuted, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = InteractiveWhite, fontSize = 26.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(InteractiveBlue),
            singleLine = true,
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text("0", color = InteractiveMuted, fontSize = 26.sp)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun ValueReadout(expression: String, result: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InteractivePanel, RoundedCornerShape(14.dp))
            .border(1.dp, InteractiveLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(expression, color = InteractiveMuted, fontSize = 15.sp)
        Text(result, color = InteractiveYellow, fontSize = 23.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatNumber(value: Float): String {
    if (!value.isFinite()) return "∞"
    val rounded = round(value * 100f) / 100f
    return if (abs(rounded - rounded.toInt()) < 0.001f) rounded.toInt().toString() else rounded.toString().trimEnd('0').trimEnd('.')
}

private fun signedNumber(value: Float): String = if (value >= 0f) "+ ${formatNumber(value)}" else "- ${formatNumber(abs(value))}"

private fun snapTenth(value: Float): Float = round(value * 10f) / 10f

private fun snapHundredth(value: Float): Float = round(value * 100f) / 100f
