package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.course.CourseSceneData
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

private const val WHOLE_AXIS_MIN = -8f
private const val WHOLE_AXIS_MAX = 8f

private data class WholeAxisPoint(val value: Float, val label: String, val color: Color, val above: Boolean)

@Composable
internal fun WholeNumberLineVisual(data: CourseSceneData) {
    val mode = data.string("mode")
    var value by rememberSaveable(mode) { mutableFloatStateOf(initialWholeAxisValue(mode, data)) }
    val snapped = round(value * 2f) / 2f
    val showSlider = mode == "value" || mode == "opposite" || mode == "opposite_symbol" || mode.isBlank()
    val points = wholeAxisPoints(mode, snapped)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = wholeAxisTitle(mode),
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = wholeAxisHint(mode),
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        ZoomableMathCanvas(Modifier.fillMaxWidth().weight(1f)) {
            val axis = drawWholeAxis()
            drawWholeAxisOverlay(mode, snapped, axis)
            points.forEach { point -> drawWholeAxisPoint(axis, point) }
        }
        if (showSlider) {
            Slider(
                value = snapped,
                onValueChange = { value = round(it * 2f) / 2f },
                valueRange = if (mode == "opposite") 0f..5f else -7f..7f,
                steps = if (mode == "opposite") 9 else 27,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = wholeAxisConclusion(mode, snapped),
                color = InteractiveWhite.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class WholeAxisGeometry(val left: Float, val right: Float, val y: Float) {
    fun xFor(value: Float): Float = left + (value.coerceIn(WHOLE_AXIS_MIN, WHOLE_AXIS_MAX) - WHOLE_AXIS_MIN) / (WHOLE_AXIS_MAX - WHOLE_AXIS_MIN) * (right - left)
}

private fun DrawScope.drawWholeAxis(): WholeAxisGeometry {
    val left = 18.dp.toPx()
    val right = size.width - 18.dp.toPx()
    val y = size.height * 0.56f
    val axis = WholeAxisGeometry(left, right, y)
    val zeroX = axis.xFor(0f)

    drawLine(InteractiveYellow.copy(alpha = 0.18f), Offset(left, y), Offset(zeroX, y), 9.dp.toPx(), StrokeCap.Round)
    drawLine(InteractiveBlue.copy(alpha = 0.18f), Offset(zeroX, y), Offset(right, y), 9.dp.toPx(), StrokeCap.Round)
    drawLine(InteractiveWhite.copy(alpha = 0.82f), Offset(left, y), Offset(right, y), 2.dp.toPx(), StrokeCap.Round)
    drawWholeAxisArrow(left, y, -1f, InteractiveYellow)
    drawWholeAxisArrow(right, y, 1f, InteractiveBlue)

    for (tick in WHOLE_AXIS_MIN.toInt()..WHOLE_AXIS_MAX.toInt()) {
        val x = axis.xFor(tick.toFloat())
        val isOrigin = tick == 0
        drawLine(
            if (isOrigin) InteractiveWhite else InteractiveMuted.copy(alpha = 0.72f),
            Offset(x, y - if (isOrigin) 9.dp.toPx() else 6.dp.toPx()),
            Offset(x, y + if (isOrigin) 9.dp.toPx() else 6.dp.toPx()),
            if (isOrigin) 2.dp.toPx() else 1.dp.toPx(),
        )
        if (tick % 2 == 0) wholeAxisLabel(tick.toString(), x, y + 29.dp.toPx(), if (isOrigin) InteractiveWhite else InteractiveMuted, 12.sp)
    }

    wholeAxisLabel("负方向", axis.xFor(-5.4f), y - 54.dp.toPx(), InteractiveYellow, 14.sp)
    wholeAxisLabel("0", zeroX, y - 25.dp.toPx(), InteractiveWhite, 16.sp)
    wholeAxisLabel("正方向", axis.xFor(5.4f), y - 54.dp.toPx(), InteractiveBlue, 14.sp)
    return axis
}

private fun DrawScope.drawWholeAxisOverlay(mode: String, value: Float, axis: WholeAxisGeometry) {
    val zeroX = axis.xFor(0f)
    when (mode) {
        "construction" -> {
            val oneX = axis.xFor(1f)
            drawLine(InteractiveYellow, Offset(zeroX, axis.y + 45.dp.toPx()), Offset(oneX, axis.y + 45.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
            drawLine(InteractiveYellow, Offset(zeroX, axis.y + 39.dp.toPx()), Offset(zeroX, axis.y + 51.dp.toPx()), 2.dp.toPx())
            drawLine(InteractiveYellow, Offset(oneX, axis.y + 39.dp.toPx()), Offset(oneX, axis.y + 51.dp.toPx()), 2.dp.toPx())
            wholeAxisLabel("1 个单位长度", (zeroX + oneX) / 2f, axis.y + 67.dp.toPx(), InteractiveYellow, 12.sp)
            wholeAxisLabel("原点", zeroX, axis.y - 55.dp.toPx(), InteractiveWhite, 13.sp)
        }
        "value" -> drawDistanceFromZero(axis, value, wholeAxisSignColor(value))
        "opposite", "opposite_symbol" -> {
            val distance = abs(value)
            if (distance > 0f) {
                drawDistanceFromZero(axis, -distance, InteractiveYellow)
                drawDistanceFromZero(axis, distance, InteractiveBlue)
                wholeAxisLabel("到 0 的距离相等", zeroX, axis.y + 67.dp.toPx(), InteractiveWhite.copy(alpha = 0.86f), 12.sp)
            }
        }
        "road" -> wholeAxisLabel("同一个基准、同一个方向、同一个单位", size.width / 2f, axis.y + 67.dp.toPx(), InteractiveMuted, 12.sp)
    }
}

private fun DrawScope.drawDistanceFromZero(axis: WholeAxisGeometry, value: Float, color: Color) {
    val zeroX = axis.xFor(0f)
    val valueX = axis.xFor(value)
    drawLine(color.copy(alpha = 0.72f), Offset(zeroX, axis.y + 18.dp.toPx()), Offset(valueX, axis.y + 18.dp.toPx()), 4.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.drawWholeAxisPoint(axis: WholeAxisGeometry, point: WholeAxisPoint) {
    val x = axis.xFor(point.value)
    drawCircle(point.color, 6.dp.toPx(), Offset(x, axis.y))
    wholeAxisLabel(point.label, x, axis.y + if (point.above) -20.dp.toPx() else 49.dp.toPx(), point.color, 12.sp)
}

private fun DrawScope.drawWholeAxisArrow(x: Float, y: Float, direction: Float, color: Color) {
    val length = 9.dp.toPx()
    drawLine(color, Offset(x, y), Offset(x - direction * length, y - length * 0.62f), 2.dp.toPx(), StrokeCap.Round)
    drawLine(color, Offset(x, y), Offset(x - direction * length, y + length * 0.62f), 2.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.wholeAxisLabel(text: String, x: Float, y: Float, color: Color, fontSize: TextUnit) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = fontSize.toPx()
        textAlign = Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun wholeAxisPoints(mode: String, value: Float): List<WholeAxisPoint> = when (mode) {
    "road" -> listOf(
        WholeAxisPoint(-4.8f, "电线杆 −4.8", InteractiveYellow, true),
        WholeAxisPoint(-3f, "槐树 −3", InteractiveYellow, false),
        WholeAxisPoint(0f, "站牌 0", InteractiveWhite, true),
        WholeAxisPoint(3f, "柳树 +3", InteractiveBlue, false),
        WholeAxisPoint(7.5f, "标志杆 +7.5", InteractiveBlue, true),
    )
    "construction" -> emptyList()
    "example" -> listOf(
        WholeAxisPoint(-4f, "−4", InteractiveYellow, true),
        WholeAxisPoint(-2.5f, "−5/2", InteractiveYellow, false),
        WholeAxisPoint(-1f, "−1", InteractiveYellow, true),
        WholeAxisPoint(0f, "0", InteractiveWhite, false),
        WholeAxisPoint(0.5f, "0.5", InteractiveBlue, true),
        WholeAxisPoint(3f, "3", InteractiveBlue, false),
        WholeAxisPoint(4f, "4", InteractiveBlue, true),
    )
    "read_points" -> listOf(
        WholeAxisPoint(-3f, "E", InteractiveYellow, true),
        WholeAxisPoint(-2f, "B", InteractiveYellow, false),
        WholeAxisPoint(0f, "A", InteractiveWhite, true),
        WholeAxisPoint(1f, "C", InteractiveBlue, false),
        WholeAxisPoint(2.5f, "D", InteractiveBlue, true),
    )
    "opposite" -> {
        val distance = abs(value)
        if (distance == 0f) listOf(WholeAxisPoint(0f, "0", InteractiveWhite, true)) else listOf(
            WholeAxisPoint(-distance, "−${wholeNumberText(distance)}", InteractiveYellow, true),
            WholeAxisPoint(distance, wholeNumberText(distance), InteractiveBlue, true),
        )
    }
    "opposite_symbol" -> listOf(
        WholeAxisPoint(value, "a=${wholeSignedNumber(value)}", wholeAxisSignColor(value), true),
        WholeAxisPoint(-value, "−a=${wholeSignedNumber(-value)}", wholeAxisSignColor(-value), false),
    )
    else -> listOf(WholeAxisPoint(value, wholeSignedNumber(value), wholeAxisSignColor(value), true))
}

private fun initialWholeAxisValue(mode: String, data: CourseSceneData): Float = when (mode) {
    "opposite" -> abs(data.number("initial", 3.0).toFloat()).coerceIn(0f, 5f)
    "opposite_symbol" -> data.number("initial", 5.0).toFloat().coerceIn(-7f, 7f)
    else -> data.number("initial", 3.0).toFloat().coerceIn(-7f, 7f)
}

private fun wholeAxisTitle(mode: String): String = when (mode) {
    "construction" -> "一条完整数轴由原点、正方向和单位长度确定"
    "road" -> "把相对位置统一放到一条数轴上"
    "example" -> "不同形式的有理数都能落在同一条数轴上"
    "read_points" -> "沿着同一条数轴读取各点的位置"
    "opposite", "opposite_symbol" -> "相反数在同一条数轴上关于 0 对称"
    else -> "在完整数轴上确定一个数的位置"
}

private fun wholeAxisHint(mode: String): String = when (mode) {
    "construction" -> "负半轴、原点和正半轴始终同时保留，只突出数轴的三个基本要素。"
    "road" -> "左边表示负方向，右边表示正方向，0 是共同基准。"
    "read_points" -> "先找 0，再看点在左边还是右边，最后按单位长度读数。"
    "opposite", "opposite_symbol" -> "左右两边使用同一刻度，离 0 同样远的两个点方向相反。"
    else -> "数越大位置越靠右；0 把负数和正数分在两侧。"
}

private fun wholeAxisConclusion(mode: String, value: Float): String = when (mode) {
    "construction" -> "原点表示 0，向右规定为正方向，相邻刻度保持相同的单位长度。"
    "road" -> "方向由正负号表示，离 0 的远近由数的绝对大小表示。"
    "example" -> "整数、分数和小数都共享这一条连续的数轴。"
    "read_points" -> "同一条数轴上的位置一旦确定，每个点表示的数也随之确定。"
    "opposite" -> if (value == 0f) "0 的相反数仍是 0。" else "${wholeNumberText(value)} 和 −${wholeNumberText(value)} 到 0 的距离相等、方向相反。"
    "opposite_symbol" -> "a 与 −a 关于 0 对称；取相反数就是到数轴另一侧的对称位置。"
    else -> if (value == 0f) "0 位于原点。" else "${wholeSignedNumber(value)} 位于${if (value > 0f) "正" else "负"}半轴上。"
}

private fun wholeAxisSignColor(value: Float): Color = when {
    value < 0f -> InteractiveYellow
    value > 0f -> InteractiveBlue
    else -> InteractiveWhite
}

private fun wholeSignedNumber(value: Float): String = when {
    value > 0f -> "+${wholeNumberText(value)}"
    else -> wholeNumberText(value)
}

private fun wholeNumberText(value: Float): String {
    val integer = value.roundToInt()
    val text = if (abs(value - integer) < 0.0001f) integer.toString() else String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    return text.replace('-', '−')
}
