package com.majortomman.school.ui

import android.graphics.Paint
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

/** School 原创的相反数交互：同距镜像与“取相反数”符号过程。 */
@Composable
internal fun OppositeNumberLessonVisual(params: Map<String, String> = emptyMap()) {
    when (params["mode"]) {
        "symbol" -> OppositeSymbolVisual()
        else -> OppositeMirrorVisual(params["initial"]?.toFloatOrNull() ?: 3f)
    }
}

@Composable
private fun OppositeMirrorVisual(initial: Float) {
    var rawDistance by rememberSaveable(initial) {
        mutableFloatStateOf(initial.coerceIn(0f, 5f))
    }
    val distance = round(rawDistance * 2f) / 2f

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "拖动距离，观察两个点如何关于原点对应",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 24f
            val right = size.width - 24f
            val center = (left + right) / 2f
            val y = size.height * 0.55f
            fun xFor(value: Float): Float = left + (value + 6f) / 12f * (right - left)

            drawLine(InteractiveWhite.copy(alpha = 0.76f), Offset(left, y), Offset(right, y), 3f)
            drawLine(InteractiveBlue, Offset(right, y), Offset(right - 13f, y - 8f), 3f)
            drawLine(InteractiveBlue, Offset(right, y), Offset(right - 13f, y + 8f), 3f)
            for (tick in -5..5) {
                val x = xFor(tick.toFloat())
                drawLine(
                    color = if (tick == 0) InteractiveWhite else InteractiveMuted,
                    start = Offset(x, y - 8f),
                    end = Offset(x, y + 8f),
                    strokeWidth = if (tick == 0) 3f else 2f,
                )
                label(numberText(tick.toFloat()), x, y + 36f, InteractiveMuted, 16f)
            }

            val negativeX = xFor(-distance)
            val positiveX = xFor(distance)
            drawLine(
                color = InteractiveYellow,
                start = Offset(center, y - 24f),
                end = Offset(negativeX, y - 24f),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = InteractiveBlue,
                start = Offset(center, y - 24f),
                end = Offset(positiveX, y - 24f),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )

            if (distance == 0f) {
                drawCircle(InteractiveWhite, 10f, Offset(center, y))
                label("0", center, y - 48f, InteractiveWhite, 24f)
            } else {
                drawCircle(InteractiveYellow, 10f, Offset(negativeX, y))
                drawCircle(InteractiveBlue, 10f, Offset(positiveX, y))
                label("−${numberText(distance)}", negativeX, y - 48f, InteractiveYellow, 23f)
                label(numberText(distance), positiveX, y - 48f, InteractiveBlue, 23f)
                label(numberText(distance), (center + negativeX) / 2f, y - 38f, InteractiveYellow, 16f)
                label(numberText(distance), (center + positiveX) / 2f, y - 38f, InteractiveBlue, 16f)
            }
            label("原点", center, y + 63f, InteractiveWhite.copy(alpha = 0.82f), 17f)
        }
        Slider(
            value = distance,
            onValueChange = { rawDistance = round(it * 2f) / 2f },
            valueRange = 0f..5f,
            steps = 9,
        )
        Text(
            text = if (distance == 0f) {
                "0位于原点，它的相反数仍是0。"
            } else {
                "${numberText(distance)}和−${numberText(distance)}到原点的距离都为${numberText(distance)}，只有符号不同。"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.84f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OppositeSymbolVisual() {
    val examples = listOf(5f, -5f, 0f)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val value = examples[selected]
    val opposite = -value

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            examples.forEachIndexed { index, example ->
                Column(
                    modifier = Modifier.weight(1f).clickable { selected = index }.padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "a=${signedText(example)}",
                        color = if (selected == index) InteractiveWhite else InteractiveMuted,
                        fontSize = 13.sp,
                        fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(5.dp))
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
            Text("原数 a", color = InteractiveMuted, fontSize = 12.sp)
            Text(
                text = signedText(value),
                color = signColor(value),
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "↓  取相反数",
                color = InteractiveBlue,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 9.dp),
            )
            Text("−a", color = InteractiveMuted, fontSize = 12.sp)
            Text(
                text = signedText(opposite),
                color = signColor(opposite),
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = when {
                value > 0f -> "−（+${numberText(value)}）=−${numberText(value)}"
                value < 0f -> "−（−${numberText(abs(value))}）=+${numberText(abs(value))}"
                else -> "−0=0"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveYellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "负号表示执行一次“取相反数”，结果要由原数决定。",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.82f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun signColor(value: Float): Color = when {
    value < 0f -> InteractiveYellow
    value > 0f -> InteractiveBlue
    else -> InteractiveWhite
}

private fun signedText(value: Float): String = when {
    value > 0f -> "+${numberText(value)}"
    else -> numberText(value)
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
    textSize: Float,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
