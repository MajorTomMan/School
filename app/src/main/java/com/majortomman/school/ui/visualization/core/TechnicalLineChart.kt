package com.majortomman.school.ui.visualization.core

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.ui.ZoomableVisualizationCanvas
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class VisualizationPoint(
    val label: String,
    val value: Float,
)

data class TechnicalLineChartModel(
    val points: List<VisualizationPoint>,
    val minimum: Float,
    val maximum: Float,
    val baseline: Float = 0f,
    val unit: String,
    val positiveMeaning: String,
    val negativeMeaning: String,
    val baselineLabel: String,
    val decimals: Int = 0,
) {
    init {
        require(points.size >= 2) { "折线图至少需要两个数据点" }
        require(maximum > minimum) { "折线图最大值必须大于最小值" }
        require(baseline in minimum..maximum) { "基准值必须位于图表范围内" }
        require(decimals in 0..4) { "小数位数必须位于 0 到 4 之间" }
    }
}

/**
 * Reusable 3Blue1Brown-inspired line chart for ordered observations.
 * Canvas draws only geometry; all labels remain Compose text so font scaling never causes chart-label collisions.
 */
@Composable
fun TechnicalLineChart(
    model: TechnicalLineChartModel,
    palette: VisualizationPalette,
    modifier: Modifier = Modifier,
) {
    val latest = model.points.last().value
    val latestColor = when {
        latest > model.baseline -> palette.positive
        latest < model.baseline -> palette.negative
        else -> palette.foreground
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(model.positiveMeaning, color = palette.positive, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    formatSignedChartValue(model.maximum, model.unit, model.decimals),
                    color = palette.muted,
                    fontSize = 10.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("当前", color = palette.muted, fontSize = 10.sp)
                Text(
                    formatSignedChartValue(latest, model.unit, model.decimals),
                    color = latestColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        ZoomableVisualizationCanvas(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            maxScale = 3f,
        ) {
            val area = plotArea()

            fun xAt(index: Int): Float = area.xAt(index / model.points.lastIndex.toFloat())
            fun yAt(value: Float): Float = area.yFor(value, model.minimum, model.maximum)

            drawTechnicalGrid(
                area = area,
                rows = 4,
                columns = model.points.lastIndex.coerceAtLeast(1),
                color = palette.grid.copy(alpha = 0.38f),
            )

            val baselineY = yAt(model.baseline)
            drawReferenceLine(
                area = area,
                y = baselineY,
                color = palette.foreground.copy(alpha = 0.64f),
            )

            val linePath = Path().apply {
                model.points.forEachIndexed { index, point ->
                    val x = xAt(index)
                    val y = yAt(point.value)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val areaPath = Path().apply {
                moveTo(xAt(0), baselineY)
                model.points.forEachIndexed { index, point -> lineTo(xAt(index), yAt(point.value)) }
                lineTo(xAt(model.points.lastIndex), baselineY)
                close()
            }

            drawPath(areaPath, palette.primary.copy(alpha = 0.10f))
            drawPath(
                path = linePath,
                color = palette.primary,
                style = Stroke(width = 4f, cap = StrokeCap.Round),
            )

            model.points.forEachIndexed { index, point ->
                val color = when {
                    point.value > model.baseline -> palette.positive
                    point.value < model.baseline -> palette.negative
                    else -> palette.foreground
                }
                drawDataMarker(
                    center = Offset(xAt(index), yAt(point.value)),
                    color = color,
                    emphasized = index == model.points.lastIndex,
                )
            }

            val latestCenter = Offset(xAt(model.points.lastIndex), yAt(latest))
            drawLine(
                color = latestColor.copy(alpha = 0.44f),
                start = latestCenter,
                end = Offset(latestCenter.x, baselineY),
                strokeWidth = 2f,
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            model.points.forEach { point ->
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    color = palette.muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.grid))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                model.negativeMeaning,
                color = palette.negative,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(model.baselineLabel, color = palette.foreground.copy(alpha = 0.76f), fontSize = 11.sp)
            Text(
                formatSignedChartValue(model.minimum, model.unit, model.decimals),
                color = palette.muted,
                fontSize = 10.sp,
            )
        }
    }
}

fun formatSignedChartValue(value: Float, unit: String, decimals: Int): String {
    val normalized = if (abs(value) < 0.0001f) 0f else value
    val number = if (decimals <= 0) {
        normalized.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.$decimals" + "f", normalized).trimEnd('0').trimEnd('.')
    }
    return when {
        normalized > 0f -> "+$number$unit"
        else -> "$number$unit"
    }
}
