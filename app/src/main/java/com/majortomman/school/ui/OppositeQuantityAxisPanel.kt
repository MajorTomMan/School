package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * 相对基准类场景的通用布局。
 *
 * 文字全部由 Compose 布局；Canvas 只绘制轴线、方向和当前位置，避免文字缩放后与
 * 基准、数值或其他标签发生重叠。
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuantitySummaryCell(
                label = "基准",
                value = baselineText,
                color = InteractiveWhite,
                modifier = Modifier.weight(1f),
            )
            QuantitySummaryCell(
                label = "当前记录",
                value = displayOppositeSignedQuantity(value, unit),
                color = valueColor,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DirectionMeaningCell(
                text = negativeMeaning,
                color = InteractiveYellow,
                modifier = Modifier.weight(1f),
            )
            DirectionMeaningCell(
                text = positiveMeaning,
                color = InteractiveBlue,
                modifier = Modifier.weight(1f),
            )
        }

        ZoomableMathCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        ) {
            val left = 30.dp.toPx()
            val right = size.width - 30.dp.toPx()
            val centerX = size.width / 2f
            val axisY = size.height / 2f
            val normalized = (value / bound.coerceAtLeast(0.0001f)).coerceIn(-1f, 1f)
            val endX = centerX + normalized * (right - centerX)
            val stroke = 3.dp.toPx()

            drawLine(
                color = InteractiveYellow.copy(alpha = 0.38f),
                start = Offset(left, axisY),
                end = Offset(centerX, axisY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = InteractiveBlue.copy(alpha = 0.38f),
                start = Offset(centerX, axisY),
                end = Offset(right, axisY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = InteractiveWhite,
                start = Offset(centerX, axisY - 20.dp.toPx()),
                end = Offset(centerX, axisY + 20.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )

            if (abs(value) > 0.0001f) {
                drawLine(
                    color = valueColor,
                    start = Offset(centerX, axisY),
                    end = Offset(endX, axisY),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = valueColor,
                    radius = 8.dp.toPx(),
                    center = Offset(endX, axisY),
                )
                val direction = if (value > 0f) 1f else -1f
                drawLine(
                    color = valueColor,
                    start = Offset(endX, axisY),
                    end = Offset(endX - direction * 13.dp.toPx(), axisY - 9.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = valueColor,
                    start = Offset(endX, axisY),
                    end = Offset(endX - direction * 13.dp.toPx(), axisY + 9.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else {
                drawCircle(
                    color = InteractiveWhite,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, axisY),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "负方向",
                color = InteractiveMuted,
                fontSize = 13.sp,
            )
            Text(
                text = "基准 0",
                color = InteractiveWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "正方向",
                color = InteractiveMuted,
                fontSize = 13.sp,
            )
        }

        Text(
            text = if (abs(value) < 0.0001f) {
                "当前记录位于基准，没有向任一方向偏离。"
            } else {
                "距基准 ${displayOppositeQuantity(abs(value))}$unit · 方向：$directionMeaning"
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(valueColor.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            color = InteractiveWhite.copy(alpha = 0.84f),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuantitySummaryCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .background(InteractivePanel.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, color = InteractiveMuted, fontSize = 12.sp)
        Text(
            value,
            color = color,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DirectionMeaningCell(
    text: String,
    color: Color,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
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
