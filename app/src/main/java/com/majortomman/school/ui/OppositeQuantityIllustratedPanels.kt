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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 场景化的相反意义量可视化。
 *
 * 图形仍由可缩放 Canvas 实时绘制，但所有说明文字均在 Canvas 外由 Compose 排版，既保留
 * 温度计、山脉海平面和天平的直观性，也避免全局字号变化后发生文字重叠。
 */
@Composable
internal fun TemperatureQuantityPanel(
    baselineText: String,
    value: Float,
    bound: Float,
    unit: String,
) {
    val valueColor = illustratedValueColor(value)
    IllustratedQuantitySummary(
        baselineText = baselineText,
        signedValue = illustratedSignedQuantity(value, unit),
        valueColor = valueColor,
    )
    DirectionLegend(
        negativeMeaning = "零下",
        centerMeaning = "0 ℃ 基准",
        positiveMeaning = "零上",
    )

    ZoomableMathCanvas(
        modifier = Modifier.fillMaxWidth().height(164.dp),
        maxScale = 3f,
    ) {
        val top = 14.dp.toPx()
        val bottom = size.height - 27.dp.toPx()
        val centerX = size.width * 0.5f
        val tubeHalfWidth = 12.dp.toPx()
        val bulbRadius = 18.dp.toPx()
        val normalized = ((value + bound) / (bound * 2f)).coerceIn(0f, 1f)
        val mercuryTop = bottom - normalized * (bottom - top)
        val zeroY = bottom - 0.5f * (bottom - top)

        drawRoundRect(
            color = InteractiveWhite.copy(alpha = 0.46f),
            topLeft = Offset(centerX - tubeHalfWidth, top),
            size = Size(tubeHalfWidth * 2f, bottom - top),
            cornerRadius = CornerRadius(tubeHalfWidth, tubeHalfWidth),
            style = Stroke(width = 2.dp.toPx()),
        )
        drawRoundRect(
            color = InteractiveWhite.copy(alpha = 0.035f),
            topLeft = Offset(centerX - tubeHalfWidth + 3.dp.toPx(), top + 3.dp.toPx()),
            size = Size(tubeHalfWidth * 2f - 6.dp.toPx(), bottom - top - 4.dp.toPx()),
            cornerRadius = CornerRadius(tubeHalfWidth, tubeHalfWidth),
        )
        drawLine(
            color = valueColor.copy(alpha = 0.22f),
            start = Offset(centerX, bottom),
            end = Offset(centerX, mercuryTop),
            strokeWidth = 15.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = valueColor,
            start = Offset(centerX, bottom),
            end = Offset(centerX, mercuryTop),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = valueColor.copy(alpha = 0.16f),
            radius = bulbRadius + 7.dp.toPx(),
            center = Offset(centerX, bottom + 8.dp.toPx()),
        )
        drawCircle(
            color = valueColor,
            radius = bulbRadius,
            center = Offset(centerX, bottom + 8.dp.toPx()),
        )
        drawCircle(
            color = InteractiveWhite.copy(alpha = 0.35f),
            radius = 4.dp.toPx(),
            center = Offset(centerX - 5.dp.toPx(), bottom + 3.dp.toPx()),
        )

        (-2..2).forEach { index ->
            val y = bottom - (index + 2f) / 4f * (bottom - top)
            val strong = index == 0
            drawLine(
                color = if (strong) InteractiveWhite.copy(alpha = 0.86f) else InteractiveWhite.copy(alpha = 0.28f),
                start = Offset(centerX + 18.dp.toPx(), y),
                end = Offset(centerX + if (strong) 50.dp.toPx() else 39.dp.toPx(), y),
                strokeWidth = if (strong) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = InteractiveWhite.copy(alpha = 0.42f),
            start = Offset(centerX - 58.dp.toPx(), zeroY),
            end = Offset(centerX + 52.dp.toPx(), zeroY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx())),
        )
    }

    QuantityScaleLabels(
        negative = "−${illustratedQuantity(bound)}$unit",
        center = "0$unit",
        positive = "+${illustratedQuantity(bound)}$unit",
    )
    IllustratedStatusLine(
        text = when {
            abs(value) < 0.0001f -> "当前温度恰好位于 0 ℃ 基准。"
            value > 0f -> "当前 ${illustratedSignedQuantity(value, unit)}，表示零上 ${illustratedQuantity(value)} ℃。"
            else -> "当前 ${illustratedSignedQuantity(value, unit)}，表示零下 ${illustratedQuantity(abs(value))} ℃。"
        },
        color = valueColor,
    )
}

@Composable
internal fun ElevationQuantityPanel(
    baselineText: String,
    value: Float,
    bound: Float,
    unit: String,
) {
    val valueColor = illustratedValueColor(value)
    IllustratedQuantitySummary(
        baselineText = baselineText,
        signedValue = illustratedSignedQuantity(value, unit),
        valueColor = valueColor,
    )
    DirectionLegend(
        negativeMeaning = "低于海平面",
        centerMeaning = "海平面 0 m",
        positiveMeaning = "高于海平面",
    )

    ZoomableMathCanvas(
        modifier = Modifier.fillMaxWidth().height(176.dp),
        maxScale = 3f,
    ) {
        val left = 10.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val seaY = size.height * 0.58f
        val normalized = (value / bound.coerceAtLeast(0.0001f)).coerceIn(-1f, 1f)
        val positiveRange = seaY - 18.dp.toPx()
        val negativeRange = size.height - seaY - 18.dp.toPx()
        val pointY = if (normalized >= 0f) {
            seaY - normalized * positiveRange
        } else {
            seaY + abs(normalized) * negativeRange
        }
        val pointX = if (normalized >= 0f) size.width * 0.70f else size.width * 0.30f

        drawRect(
            color = InteractiveBlue.copy(alpha = 0.045f),
            topLeft = Offset(left, seaY),
            size = Size(right - left, size.height - seaY),
        )

        val distantMountain = Path().apply {
            moveTo(left, seaY)
            lineTo(size.width * 0.18f, seaY - 34.dp.toPx())
            lineTo(size.width * 0.34f, seaY - 10.dp.toPx())
            lineTo(size.width * 0.48f, seaY - 46.dp.toPx())
            lineTo(size.width * 0.62f, seaY)
            close()
        }
        drawPath(distantMountain, InteractiveBlue.copy(alpha = 0.055f))
        drawPath(
            distantMountain,
            InteractiveBlue.copy(alpha = 0.24f),
            style = Stroke(width = 1.dp.toPx()),
        )

        val foregroundMountain = Path().apply {
            moveTo(size.width * 0.43f, seaY)
            lineTo(size.width * 0.58f, seaY - 48.dp.toPx())
            lineTo(size.width * 0.69f, seaY - 96.dp.toPx())
            lineTo(size.width * 0.78f, seaY - 42.dp.toPx())
            lineTo(size.width * 0.88f, seaY - 70.dp.toPx())
            lineTo(right, seaY)
            close()
        }
        drawPath(foregroundMountain, InteractiveBlue.copy(alpha = 0.10f))
        drawPath(
            foregroundMountain,
            InteractiveBlue.copy(alpha = 0.62f),
            style = Stroke(width = 2.dp.toPx()),
        )

        val underwaterBasin = Path().apply {
            moveTo(left, seaY)
            lineTo(size.width * 0.16f, seaY + 34.dp.toPx())
            lineTo(size.width * 0.28f, seaY + 68.dp.toPx())
            lineTo(size.width * 0.39f, seaY + 28.dp.toPx())
            lineTo(size.width * 0.49f, seaY)
            close()
        }
        drawPath(underwaterBasin, InteractiveYellow.copy(alpha = 0.055f))
        drawPath(
            underwaterBasin,
            InteractiveYellow.copy(alpha = 0.38f),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        repeat(3) { index ->
            val y = seaY + (index + 1) * 12.dp.toPx()
            drawLine(
                color = InteractiveBlue.copy(alpha = 0.16f - index * 0.03f),
                start = Offset(size.width * 0.50f, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawLine(
            color = InteractiveBlue.copy(alpha = 0.88f),
            start = Offset(left, seaY),
            end = Offset(right, seaY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        drawLine(
            color = valueColor.copy(alpha = 0.55f),
            start = Offset(pointX, seaY),
            end = Offset(pointX, pointY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
        )
        drawCircle(
            color = valueColor.copy(alpha = 0.16f),
            radius = 13.dp.toPx(),
            center = Offset(pointX, pointY),
        )
        drawCircle(
            color = valueColor,
            radius = 6.5.dp.toPx(),
            center = Offset(pointX, pointY),
        )
        drawCircle(
            color = InteractiveWhite,
            radius = 2.dp.toPx(),
            center = Offset(pointX, pointY),
        )
    }

    QuantityScaleLabels(
        negative = "海平面以下",
        center = "0 m",
        positive = "海平面以上",
    )
    IllustratedStatusLine(
        text = when {
            abs(value) < 0.0001f -> "当前位置恰好在海平面上，记作 0 m。"
            value > 0f -> "当前位置比海平面高 ${illustratedQuantity(value)} m，记作 ${illustratedSignedQuantity(value, unit)}。"
            else -> "当前位置比海平面低 ${illustratedQuantity(abs(value))} m，记作 ${illustratedSignedQuantity(value, unit)}。"
        },
        color = valueColor,
    )
}

@Composable
internal fun MassDeviationQuantityPanel(
    baselineText: String,
    value: Float,
    bound: Float,
    unit: String,
) {
    val valueColor = illustratedValueColor(value)
    IllustratedQuantitySummary(
        baselineText = baselineText,
        signedValue = illustratedSignedQuantity(value, unit),
        valueColor = valueColor,
    )
    DirectionLegend(
        negativeMeaning = "低于标准",
        centerMeaning = "偏差 0 g",
        positiveMeaning = "超过标准",
    )

    ZoomableMathCanvas(
        modifier = Modifier.fillMaxWidth().height(172.dp),
        maxScale = 3f,
    ) {
        val centerX = size.width / 2f
        val beamY = 42.dp.toPx()
        val beamHalf = size.width * 0.25f
        val normalized = (value / bound.coerceAtLeast(0.0001f)).coerceIn(-1f, 1f)
        val tilt = normalized * 8.dp.toPx()
        val leftBeam = Offset(centerX - beamHalf, beamY - tilt)
        val rightBeam = Offset(centerX + beamHalf, beamY + tilt)

        drawLine(
            color = InteractiveWhite.copy(alpha = 0.52f),
            start = leftBeam,
            end = rightBeam,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = InteractiveBlue.copy(alpha = 0.14f),
            radius = 9.dp.toPx(),
            center = Offset(centerX, beamY),
        )
        drawCircle(
            color = InteractiveWhite.copy(alpha = 0.78f),
            radius = 3.5.dp.toPx(),
            center = Offset(centerX, beamY),
        )
        drawLine(
            color = InteractiveWhite.copy(alpha = 0.42f),
            start = Offset(centerX, beamY + 4.dp.toPx()),
            end = Offset(centerX, beamY + 47.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = InteractiveWhite.copy(alpha = 0.42f),
            start = Offset(centerX - 26.dp.toPx(), beamY + 47.dp.toPx()),
            end = Offset(centerX + 26.dp.toPx(), beamY + 47.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        fun drawPan(anchor: Offset, panColor: Color) {
            val ropeLength = 25.dp.toPx()
            val panY = anchor.y + ropeLength
            drawLine(
                color = InteractiveWhite.copy(alpha = 0.34f),
                start = anchor,
                end = Offset(anchor.x - 19.dp.toPx(), panY),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = InteractiveWhite.copy(alpha = 0.34f),
                start = anchor,
                end = Offset(anchor.x + 19.dp.toPx(), panY),
                strokeWidth = 1.dp.toPx(),
            )
            drawArc(
                color = panColor.copy(alpha = 0.64f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(anchor.x - 25.dp.toPx(), panY - 7.dp.toPx()),
                size = Size(50.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        drawPan(leftBeam, InteractiveWhite)
        drawPan(rightBeam, valueColor)

        val axisY = size.height - 29.dp.toPx()
        val left = 17.dp.toPx()
        val right = size.width - 17.dp.toPx()
        val halfWidth = right - centerX
        val currentX = centerX + normalized * halfWidth
        drawLine(
            color = InteractiveYellow.copy(alpha = 0.42f),
            start = Offset(left, axisY),
            end = Offset(centerX, axisY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = InteractiveBlue.copy(alpha = 0.42f),
            start = Offset(centerX, axisY),
            end = Offset(right, axisY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = InteractiveWhite.copy(alpha = 0.88f),
            start = Offset(centerX, axisY - 10.dp.toPx()),
            end = Offset(centerX, axisY + 10.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = valueColor.copy(alpha = 0.55f),
            start = Offset(currentX, axisY - 34.dp.toPx()),
            end = Offset(currentX, axisY - 7.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
        drawCircle(
            color = valueColor.copy(alpha = 0.18f),
            radius = 11.dp.toPx(),
            center = Offset(currentX, axisY),
        )
        drawCircle(
            color = valueColor,
            radius = 6.dp.toPx(),
            center = Offset(currentX, axisY),
        )
    }

    QuantityScaleLabels(
        negative = "−${illustratedQuantity(bound)}$unit",
        center = "0$unit",
        positive = "+${illustratedQuantity(bound)}$unit",
    )
    IllustratedStatusLine(
        text = when {
            abs(value) < 0.0001f -> "当前质量与 2.5 kg 标准质量一致，偏差为 0 g。"
            value > 0f -> "当前质量比标准质量多 ${illustratedQuantity(value)} g。"
            else -> "当前质量比标准质量少 ${illustratedQuantity(abs(value))} g。"
        },
        color = valueColor,
    )
}

@Composable
private fun IllustratedQuantitySummary(
    baselineText: String,
    signedValue: String,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("基准", color = InteractiveMuted, fontSize = 11.sp)
            Text(
                baselineText,
                color = InteractiveWhite,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }
        Column(
            modifier = Modifier.weight(0.72f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("当前记录", color = InteractiveMuted, fontSize = 11.sp)
            Text(
                signedValue,
                color = valueColor,
                fontSize = 21.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 7.dp, bottom = 7.dp)
            .height(1.dp)
            .background(InteractiveLine),
    )
}

@Composable
private fun DirectionLegend(
    negativeMeaning: String,
    centerMeaning: String,
    positiveMeaning: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            negativeMeaning,
            modifier = Modifier.weight(1f),
            color = InteractiveYellow,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            maxLines = 2,
        )
        Text(
            centerMeaning,
            modifier = Modifier.weight(1f),
            color = InteractiveWhite.copy(alpha = 0.82f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            positiveMeaning,
            modifier = Modifier.weight(1f),
            color = InteractiveBlue,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
        )
    }
}

@Composable
private fun QuantityScaleLabels(
    negative: String,
    center: String,
    positive: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(negative, color = InteractiveYellow.copy(alpha = 0.76f), fontSize = 11.sp)
        Text(center, color = InteractiveWhite.copy(alpha = 0.76f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(positive, color = InteractiveBlue.copy(alpha = 0.76f), fontSize = 11.sp)
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun IllustratedStatusLine(text: String, color: Color) {
    // The shared explanation below the interactive control is the single source of narrative feedback.
}

private fun illustratedValueColor(value: Float): Color = when {
    value > 0.0001f -> InteractiveBlue
    value < -0.0001f -> InteractiveYellow
    else -> InteractiveWhite
}

private fun illustratedQuantity(value: Float): String {
    val rounded = value.roundToInt()
    return if (abs(value - rounded) < 0.0001f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

private fun illustratedSignedQuantity(value: Float, unit: String): String = when {
    value > 0.0001f -> "+${illustratedQuantity(value)}$unit"
    value < -0.0001f -> "${illustratedQuantity(value)}$unit"
    else -> "0$unit"
}
