package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

private data class RoadObject(
    val position: Float,
    val letter: String,
    val name: String,
    val type: RoadObjectType,
    val upperRow: Boolean,
)

private enum class RoadObjectType { TREE, STATION, POLE }

private val roadObjects = listOf(
    RoadObject(-4.8f, "E", "电线杆", RoadObjectType.POLE, true),
    RoadObject(-3f, "D", "槐树", RoadObjectType.TREE, false),
    RoadObject(0f, "O", "汽车站牌", RoadObjectType.STATION, true),
    RoadObject(3f, "B", "柳树", RoadObjectType.TREE, false),
    RoadObject(7.5f, "C", "标志杆", RoadObjectType.POLE, true),
)

/**
 * School 原创数轴交互。Canvas 内所有文字、线宽和触点都使用 dp/sp 换算，
 * 不再把裸像素当字号，因此在高密度手机上仍保持可读。
 */
@Composable
internal fun NumberLineLessonVisual(data: CourseSceneData) {
    when (data.string("mode")) {
        "road" -> RoadScene(signed = data.boolean("signed"))
        "construction" -> NumberLineConstruction()
        "value" -> NumberLineValue(initial = data.number("initial", 6.5).toFloat())
        "example" -> FixedPointsScene(readingExercise = false)
        "read_points" -> FixedPointsScene(readingExercise = true)
        "opposite" -> OppositeMirrorScene(initial = data.number("initial", 3.0).toFloat())
        "opposite_symbol" -> OppositeSymbolScene()
        else -> AdjustableNumberLine(NumberLineMode.VALUE)
    }
}

@Composable
private fun RoadScene(signed: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (signed) "一个数同时记录方向和距离" else "以汽车站牌为基准观察相对位置",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 18.dp.toPx()
            val right = size.width - 18.dp.toPx()
            val roadY = size.height * 0.58f
            val minimum = -5.5f
            val maximum = 8f
            fun xFor(value: Float): Float =
                left + (value - minimum) / (maximum - minimum) * (right - left)

            drawLine(
                color = InteractiveWhite.copy(alpha = 0.72f),
                start = Offset(left, roadY),
                end = Offset(right, roadY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawArrowHead(right, roadY, InteractiveBlue)
            label("西", left, roadY + 48.dp.toPx(), InteractiveYellow, 13.sp, Paint.Align.LEFT)
            label("东", right, roadY + 48.dp.toPx(), InteractiveBlue, 13.sp, Paint.Align.RIGHT)

            roadObjects.forEach { item ->
                val x = xFor(item.position)
                val accent = signColor(item.position)
                drawLine(accent, Offset(x, roadY - 7.dp.toPx()), Offset(x, roadY + 7.dp.toPx()), 2.dp.toPx())
                drawRoadObject(item.type, x, roadY, accent)
                val titleY = roadY - if (item.upperRow) 70.dp.toPx() else 54.dp.toPx()
                label(item.name, x, titleY, InteractiveWhite, 11.sp)
                label(item.letter, x, roadY - 13.dp.toPx(), accent, 12.sp)
                val positionText = when {
                    item.position == 0f -> if (signed) "0" else "基准"
                    signed -> signedNumber(item.position)
                    else -> "${numberText(abs(item.position))} m"
                }
                label(positionText, x, roadY + 27.dp.toPx(), accent, 12.sp)
            }

            val originX = xFor(0f)
            val unitX = xFor(1f)
            drawLine(InteractiveMuted, Offset(unitX, roadY - 6.dp.toPx()), Offset(unitX, roadY + 6.dp.toPx()), 1.dp.toPx())
            label("A", unitX, roadY - 13.dp.toPx(), InteractiveMuted, 11.sp)
            if (signed) {
                label("+1", unitX, roadY + 27.dp.toPx(), InteractiveMuted, 11.sp)
            } else {
                val unitY = roadY + 44.dp.toPx()
                drawLine(InteractiveYellow, Offset(originX, unitY), Offset(unitX, unitY), 2.dp.toPx(), StrokeCap.Round)
                drawLine(InteractiveYellow, Offset(originX, unitY - 4.dp.toPx()), Offset(originX, unitY + 4.dp.toPx()), 1.dp.toPx())
                drawLine(InteractiveYellow, Offset(unitX, unitY - 4.dp.toPx()), Offset(unitX, unitY + 4.dp.toPx()), 1.dp.toPx())
                label("OA = 1 m", (originX + unitX) / 2f, unitY + 17.dp.toPx(), InteractiveYellow, 11.sp)
            }
        }
        Text(
            text = if (signed) {
                "符号说明位于站牌哪一侧，数值说明离站牌有多远。"
            } else {
                "线段 OA 表示 1 m；先确定共同基准，再观察方向和距离。"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.86f),
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun DrawScope.drawRoadObject(type: RoadObjectType, x: Float, roadY: Float, color: Color) {
    when (type) {
        RoadObjectType.TREE -> {
            drawLine(color, Offset(x, roadY - 9.dp.toPx()), Offset(x, roadY - 34.dp.toPx()), 3.dp.toPx())
            drawCircle(color.copy(alpha = 0.82f), 10.dp.toPx(), Offset(x, roadY - 41.dp.toPx()))
        }
        RoadObjectType.STATION -> {
            drawLine(color, Offset(x, roadY - 9.dp.toPx()), Offset(x, roadY - 44.dp.toPx()), 3.dp.toPx())
            drawRect(InteractivePanel, Offset(x - 12.dp.toPx(), roadY - 61.dp.toPx()), Size(24.dp.toPx(), 16.dp.toPx()))
            drawRect(
                color = InteractiveWhite.copy(alpha = 0.8f),
                topLeft = Offset(x - 12.dp.toPx(), roadY - 61.dp.toPx()),
                size = Size(24.dp.toPx(), 16.dp.toPx()),
                style = Stroke(2.dp.toPx()),
            )
        }
        RoadObjectType.POLE -> {
            drawLine(color, Offset(x, roadY - 9.dp.toPx()), Offset(x, roadY - 44.dp.toPx()), 3.dp.toPx())
            drawCircle(color, 5.dp.toPx(), Offset(x, roadY - 49.dp.toPx()))
        }
    }
}

@Composable
private fun NumberLineConstruction() {
    val stages = listOf("原点", "正方向", "单位长度")
    var stage by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            stages.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier.weight(1f).clickable { stage = index }.padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title,
                        color = if (stage == index) InteractiveWhite else InteractiveMuted,
                        fontSize = 15.sp,
                        fontWeight = if (stage == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(2.dp)
                            .background(if (stage == index) InteractiveBlue else Color.Transparent),
                    )
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 20.dp.toPx()
            val right = size.width - 20.dp.toPx()
            val center = size.width / 2f
            val y = size.height * 0.52f
            val unit = (right - left) / 8f
            drawLine(InteractiveWhite.copy(alpha = 0.76f), Offset(left, y), Offset(right, y), 2.dp.toPx())
            drawLine(InteractiveWhite, Offset(center, y - 9.dp.toPx()), Offset(center, y + 9.dp.toPx()), 2.dp.toPx())
            drawCircle(InteractiveWhite, 4.dp.toPx(), Offset(center, y))
            label("O", center, y - 18.dp.toPx(), InteractiveWhite, 15.sp)
            label("0", center, y + 26.dp.toPx(), InteractiveWhite, 13.sp)
            if (stage >= 1) {
                drawArrowHead(right, y, InteractiveBlue)
                label("正方向", right, y - 20.dp.toPx(), InteractiveBlue, 13.sp, Paint.Align.RIGHT)
                label("负方向", left, y - 20.dp.toPx(), InteractiveYellow, 13.sp, Paint.Align.LEFT)
            }
            if (stage >= 2) {
                for (value in -4..4) {
                    val x = center + value * unit
                    drawLine(
                        if (value == 0) InteractiveWhite else InteractiveMuted,
                        Offset(x, y - 6.dp.toPx()),
                        Offset(x, y + 6.dp.toPx()),
                        if (value == 0) 2.dp.toPx() else 1.dp.toPx(),
                    )
                    label(numberText(value.toFloat()), x, y + 26.dp.toPx(), InteractiveWhite.copy(alpha = 0.84f), 12.sp)
                }
                drawLine(
                    InteractiveYellow,
                    Offset(center, y + 43.dp.toPx()),
                    Offset(center + unit, y + 43.dp.toPx()),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                label("1 个单位长度", center + unit / 2f, y + 61.dp.toPx(), InteractiveYellow, 12.sp)
            }
        }
        Text(
            text = when (stage) {
                0 -> "原点确定基准，用它表示 0。"
                1 -> "正方向规定数增大的方向。"
                else -> "单位长度决定相邻刻度代表多少。"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.86f),
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NumberLineValue(initial: Float) {
    var rawValue by rememberSaveable(initial) { mutableFloatStateOf(initial.coerceIn(-7f, 7f)) }
    val snapped = round(rawValue * 2f) / 2f
    val animatedValue by animateFloatAsState(snapped, label = "number-line-point")
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = signedNumber(animatedValue),
            modifier = Modifier.fillMaxWidth(),
            color = signColor(animatedValue),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 18.dp.toPx()
            val right = size.width - 18.dp.toPx()
            val center = (left + right) / 2f
            val y = size.height * 0.55f
            fun xFor(number: Float): Float = left + (number + 7f) / 14f * (right - left)
            val pointX = xFor(animatedValue)
            val accent = signColor(animatedValue)

            drawLine(InteractiveYellow.copy(alpha = 0.24f), Offset(left, y), Offset(center, y), 7.dp.toPx())
            drawLine(InteractiveBlue.copy(alpha = 0.24f), Offset(center, y), Offset(right, y), 7.dp.toPx())
            drawLine(InteractiveWhite.copy(alpha = 0.78f), Offset(left, y), Offset(right, y), 2.dp.toPx())
            drawArrowHead(right, y, InteractiveBlue)
            for (tick in -7..7) {
                val x = xFor(tick.toFloat())
                drawLine(
                    if (tick == 0) InteractiveWhite else InteractiveMuted,
                    Offset(x, y - 6.dp.toPx()),
                    Offset(x, y + 6.dp.toPx()),
                    if (tick == 0) 2.dp.toPx() else 1.dp.toPx(),
                )
                if (tick % 2 != 0 || tick == 0) {
                    label(numberText(tick.toFloat()), x, y + 25.dp.toPx(), InteractiveMuted, 12.sp)
                }
            }
            drawLine(accent, Offset(center, y), Offset(pointX, y), 5.dp.toPx(), StrokeCap.Round)
            drawCircle(accent, 7.dp.toPx(), Offset(pointX, y))
            label(numberText(animatedValue), pointX, y - 20.dp.toPx(), accent, 18.sp)
            label("负半轴", left, y - 34.dp.toPx(), InteractiveYellow, 13.sp, Paint.Align.LEFT)
            label("正半轴", right, y - 34.dp.toPx(), InteractiveBlue, 13.sp, Paint.Align.RIGHT)
        }
        Slider(
            value = snapped,
            onValueChange = { rawValue = round(it * 2f) / 2f },
            valueRange = -7f..7f,
            steps = 27,
        )
        Text(
            text = when {
                animatedValue > 0f -> "${numberText(animatedValue)} 在正半轴上，与原点的距离是 ${numberText(animatedValue)}。"
                animatedValue < 0f -> "${numberText(animatedValue)} 在负半轴上，与原点的距离是 ${numberText(abs(animatedValue))}。"
                else -> "0 由原点表示，是正半轴与负半轴的分界。"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.86f),
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FixedPointsScene(readingExercise: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (readingExercise) "先根据刻度读出每个点表示的数" else "不同形式的有理数使用同一条数轴",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 18.dp.toPx()
            val right = size.width - 18.dp.toPx()
            val y = size.height * 0.52f
            val min = if (readingExercise) -3.5f else -5f
            val max = if (readingExercise) 3.5f else 5f
            fun xFor(number: Float): Float = left + (number - min) / (max - min) * (right - left)

            drawLine(InteractiveWhite.copy(alpha = 0.78f), Offset(left, y), Offset(right, y), 2.dp.toPx())
            drawArrowHead(right, y, InteractiveBlue)
            val integerTicks = if (readingExercise) -3..3 else -5..5
            integerTicks.forEach { tick ->
                val x = xFor(tick.toFloat())
                drawLine(InteractiveMuted, Offset(x, y - 6.dp.toPx()), Offset(x, y + 6.dp.toPx()), 1.dp.toPx())
                label(numberText(tick.toFloat()), x, y + 25.dp.toPx(), InteractiveMuted, 12.sp)
            }
            if (readingExercise) {
                listOf(
                    Triple("E", -3f, InteractiveYellow),
                    Triple("B", -2f, InteractiveYellow),
                    Triple("A", 0f, InteractiveWhite),
                    Triple("C", 1f, InteractiveBlue),
                    Triple("D", 2.5f, InteractiveBlue),
                ).forEach { (name, value, color) ->
                    val x = xFor(value)
                    drawCircle(color, 6.dp.toPx(), Offset(x, y))
                    label(name, x, y - 20.dp.toPx(), color, 17.sp)
                }
                label("D 位于 2 与 3 的正中间", size.width / 2f, y + 55.dp.toPx(), InteractiveMuted, 13.sp)
            } else {
                listOf(
                    -4f to "−4",
                    -2.5f to "−5/2",
                    -1f to "−1",
                    0f to "0",
                    0.5f to "0.5",
                    3f to "3",
                    4f to "4",
                ).forEachIndexed { index, (value, text) ->
                    val x = xFor(value)
                    val color = signColor(value)
                    drawCircle(color, 6.dp.toPx(), Offset(x, y))
                    label(text, x, y - if (index % 2 == 0) 20.dp.toPx() else 38.dp.toPx(), color, 13.sp)
                }
            }
        }
    }
}

@Composable
private fun OppositeMirrorScene(initial: Float) {
    var rawDistance by rememberSaveable(initial) { mutableFloatStateOf(abs(initial).coerceIn(0f, 5f)) }
    val distance = round(rawDistance * 2f) / 2f
    val animatedDistance by animateFloatAsState(distance, label = "opposite-distance")
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "拖动共同距离，观察关于原点左右对应的一对点",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 18.dp.toPx()
            val right = size.width - 18.dp.toPx()
            val center = (left + right) / 2f
            val y = size.height * 0.56f
            fun xFor(value: Float): Float = left + (value + 6f) / 12f * (right - left)

            drawLine(InteractiveWhite.copy(alpha = 0.8f), Offset(left, y), Offset(right, y), 2.dp.toPx())
            drawArrowHead(right, y, InteractiveBlue)
            for (tick in -5..5) {
                val x = xFor(tick.toFloat())
                drawLine(
                    if (tick == 0) InteractiveWhite else InteractiveMuted,
                    Offset(x, y - 6.dp.toPx()),
                    Offset(x, y + 6.dp.toPx()),
                    if (tick == 0) 2.dp.toPx() else 1.dp.toPx(),
                )
                label(numberText(tick.toFloat()), x, y + 25.dp.toPx(), InteractiveMuted, 12.sp)
            }
            val negativeX = xFor(-animatedDistance)
            val positiveX = xFor(animatedDistance)
            if (animatedDistance == 0f) {
                drawCircle(InteractiveWhite, 7.dp.toPx(), Offset(center, y))
                label("0", center, y - 22.dp.toPx(), InteractiveWhite, 19.sp)
            } else {
                drawLine(
                    InteractiveYellow,
                    Offset(center, y - 17.dp.toPx()),
                    Offset(negativeX, y - 17.dp.toPx()),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    InteractiveBlue,
                    Offset(center, y - 17.dp.toPx()),
                    Offset(positiveX, y - 17.dp.toPx()),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawCircle(InteractiveYellow, 7.dp.toPx(), Offset(negativeX, y))
                drawCircle(InteractiveBlue, 7.dp.toPx(), Offset(positiveX, y))
                label("−${numberText(animatedDistance)}", negativeX, y - 25.dp.toPx(), InteractiveYellow, 18.sp)
                label(numberText(animatedDistance), positiveX, y - 25.dp.toPx(), InteractiveBlue, 18.sp)
                label(
                    "同距 ${numberText(animatedDistance)}",
                    size.width / 2f,
                    y + 54.dp.toPx(),
                    InteractiveWhite.copy(alpha = 0.82f),
                    13.sp,
                )
            }
        }
        Slider(
            value = distance,
            onValueChange = { rawDistance = round(it * 2f) / 2f },
            valueRange = 0f..5f,
            steps = 9,
        )
        Text(
            text = if (distance == 0f) {
                "0 位于原点，它的相反数仍是 0。"
            } else {
                "${numberText(distance)} 与 −${numberText(distance)} 到原点距离相等、方向相反，二者互为相反数。"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.88f),
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OppositeSymbolScene() {
    val examples = listOf(5f, -5f, 0f)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val value = examples[selected]
    val opposite = -value
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            examples.forEachIndexed { index, example ->
                Column(
                    modifier = Modifier.weight(1f).clickable { selected = index }.padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "a = ${signedNumber(example)}",
                        color = if (selected == index) InteractiveWhite else InteractiveMuted,
                        fontSize = 14.sp,
                        fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(2.dp)
                            .background(if (selected == index) InteractiveBlue else Color.Transparent),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("原数 a", color = InteractiveMuted, fontSize = 14.sp)
            Text(signedNumber(value), color = signColor(value), fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text("↓  取一次相反数", color = InteractiveBlue, fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp))
            Text("−a", color = InteractiveMuted, fontSize = 14.sp)
            Text(signedNumber(opposite), color = signColor(opposite), fontSize = 38.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = when {
                value > 0f -> "−（+${numberText(value)}）= −${numberText(value)}"
                value < 0f -> "−（−${numberText(abs(value))}）= +${numberText(abs(value))}"
                else -> "−0 = 0"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveYellow,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            "式子前的“−”表示执行一次取相反数，结果不能只凭最外面的符号判断。",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.86f),
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun DrawScope.drawArrowHead(x: Float, y: Float, color: Color) {
    val length = 9.dp.toPx()
    drawLine(color, Offset(x, y), Offset(x - length, y - length * 0.62f), 2.dp.toPx(), StrokeCap.Round)
    drawLine(color, Offset(x, y), Offset(x - length, y + length * 0.62f), 2.dp.toPx(), StrokeCap.Round)
}

private fun signColor(value: Float): Color = when {
    value < 0f -> InteractiveYellow
    value > 0f -> InteractiveBlue
    else -> InteractiveWhite
}

private fun signedNumber(value: Float): String = when {
    value > 0f -> "+${numberText(value)}"
    value < 0f -> numberText(value)
    else -> "0"
}

private fun numberText(value: Float): String {
    val integer = value.roundToInt()
    val text = if (abs(value - integer) < 0.0001f) {
        integer.toString()
    } else {
        String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    }
    return text.replace('-', '−')
}

private fun DrawScope.label(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    fontSize: TextUnit,
    align: Paint.Align = Paint.Align.CENTER,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = fontSize.toPx()
        textAlign = align
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
