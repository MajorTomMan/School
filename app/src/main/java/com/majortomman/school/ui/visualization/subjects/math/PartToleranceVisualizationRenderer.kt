package com.majortomman.school.ui.visualization.subjects.math

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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.ui.ZoomableVisualizationCanvas
import com.majortomman.school.ui.visualization.core.VisualizationFieldSpec
import com.majortomman.school.ui.visualization.core.VisualizationKey
import com.majortomman.school.ui.visualization.core.VisualizationRenderContext
import com.majortomman.school.ui.visualization.core.VisualizationSchema
import com.majortomman.school.ui.visualization.core.VisualizationValueType
import com.majortomman.school.ui.visualization.core.plotArea
import com.majortomman.school.ui.visualization.subjects.MathematicsVisualizationRenderer
import java.util.Locale
import kotlin.math.abs

enum class PartToleranceState {
    STANDARD,
    WITHIN_NEGATIVE,
    WITHIN_POSITIVE,
    UNDER_LIMIT,
    OVER_LIMIT,
}

fun evaluatePartTolerance(
    deviation: Float,
    tolerance: Float,
    epsilon: Float = 0.0001f,
): PartToleranceState {
    require(tolerance > 0f) { "允许偏差必须大于 0" }
    return when {
        abs(deviation) <= epsilon -> PartToleranceState.STANDARD
        deviation < -tolerance - epsilon -> PartToleranceState.UNDER_LIMIT
        deviation > tolerance + epsilon -> PartToleranceState.OVER_LIMIT
        deviation < 0f -> PartToleranceState.WITHIN_NEGATIVE
        else -> PartToleranceState.WITHIN_POSITIVE
    }
}

object PartToleranceVisualizationRenderer : MathematicsVisualizationRenderer() {
    override val key: VisualizationKey = VisualizationKey("mathematics.measurement.part-tolerance-overlay")
    override val schema: VisualizationSchema = VisualizationSchema(
        fields = listOf(
            VisualizationFieldSpec("value", VisualizationValueType.NUMBER, description = "相对标准值的偏差"),
            VisualizationFieldSpec("bound", VisualizationValueType.NUMBER, description = "交互范围的绝对边界"),
            VisualizationFieldSpec("unit", VisualizationValueType.STRING, description = "测量单位"),
            VisualizationFieldSpec("standard", VisualizationValueType.NUMBER, description = "标准尺寸"),
            VisualizationFieldSpec("tolerance", VisualizationValueType.NUMBER, description = "允许正负偏差"),
        ),
    )

    @Composable
    override fun RenderContent(context: VisualizationRenderContext, modifier: Modifier) {
        val deviation = context.arguments.float("value")
        val bound = context.arguments.float("bound", 0.08f).coerceAtLeast(0.001f)
        val unit = context.arguments.string("unit", "mm")
        val standard = context.arguments.float("standard", 40f)
        val tolerance = context.arguments.float("tolerance", 0.05f).coerceAtLeast(0.001f)
        PartToleranceOverlay(
            standard = standard,
            deviation = deviation,
            tolerance = tolerance,
            bound = bound,
            unit = unit,
            context = context,
            modifier = modifier,
        )
    }
}

@Composable
private fun PartToleranceOverlay(
    standard: Float,
    deviation: Float,
    tolerance: Float,
    bound: Float,
    unit: String,
    context: VisualizationRenderContext,
    modifier: Modifier,
) {
    val state = evaluatePartTolerance(deviation, tolerance)
    val current = standard + deviation
    val stateColor = when (state) {
        PartToleranceState.STANDARD -> context.palette.foreground
        PartToleranceState.WITHIN_NEGATIVE -> context.palette.negative
        PartToleranceState.WITHIN_POSITIVE -> context.palette.positive
        PartToleranceState.UNDER_LIMIT,
        PartToleranceState.OVER_LIMIT,
        -> context.palette.danger
    }
    val stateLabel = when (state) {
        PartToleranceState.STANDARD -> "标准"
        PartToleranceState.WITHIN_NEGATIVE -> "偏小但合格"
        PartToleranceState.WITHIN_POSITIVE -> "偏大但合格"
        PartToleranceState.UNDER_LIMIT -> "偏小超差"
        PartToleranceState.OVER_LIMIT -> "偏大超差"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("标准尺寸", color = context.palette.muted, fontSize = 11.sp)
                Text(
                    "${formatMeasurement(standard)} $unit",
                    color = context.palette.foreground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("当前零件", color = context.palette.muted, fontSize = 11.sp)
                Text(
                    "${formatMeasurement(current)} $unit",
                    color = stateColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(context.palette.grid))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("虚线：标准件", color = context.palette.muted, fontSize = 11.sp)
            Text("实线：当前件", color = stateColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(stateLabel, color = stateColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        ZoomableVisualizationCanvas(
            modifier = Modifier.fillMaxWidth().height(184.dp),
            maxScale = 3f,
        ) {
            val center = Offset(size.width * 0.5f, size.height * 0.50f)
            val standardRadius = 53.dp.toPx()
            val toleranceVisualRadius = 8.dp.toPx()
            val visualDelta = (deviation / tolerance)
                .coerceIn(-1.8f, 1.8f) * toleranceVisualRadius
            val currentRadius = (standardRadius + visualDelta).coerceAtLeast(30.dp.toPx())
            val innerRadius = 19.dp.toPx()
            val boltRadius = 4.dp.toPx()
            val boltDistance = 34.dp.toPx()

            // The translucent annulus is the acceptable outer-diameter range.
            drawCircle(
                color = context.palette.success.copy(alpha = 0.08f),
                radius = standardRadius + toleranceVisualRadius,
                center = center,
            )
            drawCircle(
                color = Color.Transparent,
                radius = standardRadius - toleranceVisualRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = context.palette.grid.copy(alpha = 0.42f),
                radius = standardRadius + toleranceVisualRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = context.palette.grid.copy(alpha = 0.42f),
                radius = standardRadius - toleranceVisualRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            // Current part: low-opacity body plus a strong outline.
            drawCircle(
                color = stateColor.copy(alpha = 0.10f),
                radius = currentRadius,
                center = center,
            )
            drawCircle(
                color = stateColor,
                radius = currentRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )

            // Standard outer diameter stays visible as a dashed reference overlay.
            drawCircle(
                color = context.palette.foreground.copy(alpha = 0.78f),
                radius = standardRadius,
                center = center,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(7.dp.toPx(), 5.dp.toPx()),
                    ),
                ),
            )

            // Shared inner geometry makes the silhouette read as a machined flange, not an abstract circle.
            drawCircle(
                color = context.palette.foreground.copy(alpha = 0.18f),
                radius = innerRadius,
                center = center,
            )
            drawCircle(
                color = context.palette.foreground.copy(alpha = 0.70f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            listOf(
                Offset(0f, -boltDistance),
                Offset(boltDistance, 0f),
                Offset(0f, boltDistance),
                Offset(-boltDistance, 0f),
            ).forEach { delta ->
                drawCircle(
                    color = context.palette.foreground.copy(alpha = 0.16f),
                    radius = boltRadius,
                    center = center + delta,
                )
                drawCircle(
                    color = context.palette.foreground.copy(alpha = 0.55f),
                    radius = boltRadius,
                    center = center + delta,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            // Diameter dimension line for the current part.
            val dimensionY = center.y + currentRadius + 13.dp.toPx()
            val startX = center.x - currentRadius
            val endX = center.x + currentRadius
            drawLine(
                color = stateColor.copy(alpha = 0.72f),
                start = Offset(startX, dimensionY),
                end = Offset(endX, dimensionY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            listOf(startX, endX).forEach { x ->
                drawLine(
                    color = stateColor.copy(alpha = 0.72f),
                    start = Offset(x, dimensionY - 6.dp.toPx()),
                    end = Offset(x, dimensionY + 6.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }

        Text(
            text = "轮廓差异按教学比例放大；判定仍使用真实测量值。",
            modifier = Modifier.fillMaxWidth(),
            color = context.palette.muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )

        ToleranceBand(
            deviation = deviation,
            bound = bound,
            tolerance = tolerance,
            standard = standard,
            unit = unit,
            stateColor = stateColor,
            context = context,
        )
    }
}

@Composable
private fun ToleranceBand(
    deviation: Float,
    bound: Float,
    tolerance: Float,
    standard: Float,
    unit: String,
    stateColor: Color,
    context: VisualizationRenderContext,
) {
    ZoomableVisualizationCanvas(
        modifier = Modifier.fillMaxWidth().height(58.dp),
        maxScale = 2.4f,
    ) {
        val area = plotArea(horizontalPadding = 15.dp.toPx(), verticalPadding = 15.dp.toPx())
        val axisY = size.height * 0.52f
        fun xFor(value: Float): Float = area.xAt(((value + bound) / (bound * 2f)).coerceIn(0f, 1f))

        drawLine(
            color = context.palette.grid,
            start = Offset(area.left, axisY),
            end = Offset(area.right, axisY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawRoundRect(
            color = context.palette.success.copy(alpha = 0.22f),
            topLeft = Offset(xFor(-tolerance), axisY - 7.dp.toPx()),
            size = Size(xFor(tolerance) - xFor(-tolerance), 14.dp.toPx()),
            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
        )
        listOf(-tolerance, 0f, tolerance).forEach { marker ->
            val x = xFor(marker)
            drawLine(
                color = if (marker == 0f) context.palette.foreground else context.palette.success,
                start = Offset(x, axisY - 10.dp.toPx()),
                end = Offset(x, axisY + 10.dp.toPx()),
                strokeWidth = if (marker == 0f) 2.dp.toPx() else 1.5.dp.toPx(),
            )
        }
        val currentX = xFor(deviation)
        drawLine(
            color = stateColor.copy(alpha = 0.70f),
            start = Offset(currentX, axisY - 21.dp.toPx()),
            end = Offset(currentX, axisY - 7.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
        )
        drawCircle(
            color = stateColor.copy(alpha = 0.18f),
            radius = 10.dp.toPx(),
            center = Offset(currentX, axisY),
        )
        drawCircle(
            color = stateColor,
            radius = 5.dp.toPx(),
            center = Offset(currentX, axisY),
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${formatMeasurement(standard - tolerance)} $unit",
            modifier = Modifier.weight(1f),
            color = context.palette.muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Start,
        )
        Text(
            "${formatMeasurement(standard)} $unit",
            modifier = Modifier.weight(1f),
            color = context.palette.foreground.copy(alpha = 0.80f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            "${formatMeasurement(standard + tolerance)} $unit",
            modifier = Modifier.weight(1f),
            color = context.palette.muted,
            fontSize = 10.sp,
            textAlign = TextAlign.End,
        )
    }
    Text(
        text = "允许范围 ${formatMeasurement(standard - tolerance)}～${formatMeasurement(standard + tolerance)} $unit · 偏差 ${formatSignedDeviation(deviation, unit)}",
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        color = stateColor,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}

fun formatMeasurement(value: Float): String = String.format(Locale.US, "%.2f", value)

fun formatSignedDeviation(value: Float, unit: String): String = when {
    abs(value) <= 0.0001f -> "0 $unit"
    value > 0f -> "+${formatMeasurement(value)} $unit"
    else -> "${formatMeasurement(value)} $unit"
}
