package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 相对基准类场景的连续画布布局。
 *
 * 遵循项目的极简技术风格：不用卡片承载信息，只使用留白、细线与数学语义色建立层级。
 * Canvas 仅绘制轴线、刻度、方向和当前位置；所有文字由 Compose 独立排版，因此字号变化
 * 不会与点位、基准或其他标签重叠。
 */
@Composable
internal fun OppositeQuantityAxisPanel(
    baselineText: String,
    value: Float,
    bound: Float,
    negativeMeaning: String,
    positiveMeaning: String,
    unit: String,
) {
    val valueColor = oppositeQuantityValueColor(value)
    val directionMeaning = when {
        value < -0.0001f -> negativeMeaning
        value > 0.0001f -> positiveMeaning
        else -> "位于基准"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            InlineQuantitySummary(
                label = "基准",
                value = baselineText,
                color = InteractiveWhite,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "→",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                color = InteractiveMuted.copy(alpha = 0.55f),
                fontSize = 18.sp,
            )
            InlineQuantitySummary(
                label = "当前记录",
                value = displayOppositeSignedQuantity(value, unit),
                color = valueColor,
                modifier = Modifier.weight(1f),
                endAligned = true,
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = negativeMeaning,
                modifier = Modifier.weight(1f),
                color = InteractiveYellow,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                maxLines = 2,
            )
            Text(
                text = "0",
                modifier = Modifier.padding(horizontal = 14.dp),
                color = InteractiveWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = positiveMeaning,
                modifier = Modifier.weight(1f),
                color = InteractiveBlue,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 2,
            )
        }

        ZoomableMathCanvas(
            modifier = Modifier.fillMaxWidth().height(154.dp),
        ) {
            val left = 22.dp.toPx()
            val right = size.width - 22.dp.toPx()
            val centerX = size.width / 2f
            val axisY = size.height / 2f
            val halfWidth = right - centerX
            val normalized = (value / bound.coerceAtLeast(0.0001f)).coerceIn(-1f, 1f)
            val endX = centerX + normalized * halfWidth

            drawLine(
                color = InteractiveYellow.copy(alpha = 0.34f),
                start = Offset(left, axisY),
                end = Offset(centerX, axisY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = InteractiveBlue.copy(alpha = 0.34f),
                start = Offset(centerX, axisY),
                end = Offset(right, axisY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )

            for (tick in -4..4) {
                if (tick == 0) continue
                val x = centerX + tick / 4f * halfWidth
                drawLine(
                    color = InteractiveWhite.copy(alpha = 0.16f),
                    start = Offset(x, axisY - 5.dp.toPx()),
                    end = Offset(x, axisY + 5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            drawLine(
                color = InteractiveWhite.copy(alpha = 0.92f),
                start = Offset(centerX, axisY - 21.dp.toPx()),
                end = Offset(centerX, axisY + 21.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )

            if (abs(value) > 0.0001f) {
                drawLine(
                    color = valueColor,
                    start = Offset(centerX, axisY),
                    end = Offset(endX, axisY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = valueColor,
                    radius = 7.dp.toPx(),
                    center = Offset(endX, axisY),
                )

                val direction = if (value > 0f) 1f else -1f
                val arrowLength = 12.dp.toPx()
                val arrowHeight = 8.dp.toPx()
                drawLine(
                    color = valueColor,
                    start = Offset(endX, axisY),
                    end = Offset(endX - direction * arrowLength, axisY - arrowHeight),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = valueColor,
                    start = Offset(endX, axisY),
                    end = Offset(endX - direction * arrowLength, axisY + arrowHeight),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else {
                drawCircle(
                    color = InteractiveWhite,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, axisY),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "−${displayOppositeQuantity(bound)}$unit",
                color = InteractiveMuted.copy(alpha = 0.68f),
                fontSize = 11.sp,
            )
            Text(
                text = "0",
                color = InteractiveWhite.copy(alpha = 0.78f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "+${displayOppositeQuantity(bound)}$unit",
                color = InteractiveMuted.copy(alpha = 0.68f),
                fontSize = 11.sp,
            )
        }

        Text(
            text = if (abs(value) < 0.0001f) {
                "当前记录位于基准，没有向任一方向偏离。"
            } else {
                "距基准 ${displayOppositeQuantity(abs(value))}$unit   ·   $directionMeaning"
            },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            color = valueColor.copy(alpha = 0.92f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InlineQuantitySummary(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier,
    endAligned: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (endAligned) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            color = InteractiveMuted,
            fontSize = 11.sp,
        )
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (endAligned) TextAlign.End else TextAlign.Start,
            maxLines = 2,
        )
    }
}

private fun oppositeQuantityValueColor(value: Float): Color = when {
    value > 0.0001f -> InteractiveBlue
    value < -0.0001f -> InteractiveYellow
    else -> InteractiveWhite
}

private fun displayOppositeQuantity(value: Float): String {
    val rounded = value.roundToInt()
    return if (abs(value - rounded) < 0.0001f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

private fun displayOppositeSignedQuantity(value: Float, unit: String): String = when {
    value > 0.0001f -> "+${displayOppositeQuantity(value)}$unit"
    value < -0.0001f -> "${displayOppositeQuantity(value)}$unit"
    else -> "0$unit"
}
