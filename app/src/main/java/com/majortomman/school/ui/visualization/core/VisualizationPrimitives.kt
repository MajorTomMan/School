package com.majortomman.school.ui.visualization.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class VisualizationPlotArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun xAt(ratio: Float): Float = left + width * ratio.coerceIn(0f, 1f)

    fun yAt(ratio: Float): Float = bottom - height * ratio.coerceIn(0f, 1f)

    fun yFor(value: Float, minimum: Float, maximum: Float): Float {
        require(maximum > minimum) { "坐标范围无效" }
        return yAt((value - minimum) / (maximum - minimum))
    }
}

fun DrawScope.plotArea(
    horizontalPadding: Float = 18f,
    verticalPadding: Float = 12f,
): VisualizationPlotArea = VisualizationPlotArea(
    left = horizontalPadding,
    top = verticalPadding,
    right = size.width - horizontalPadding,
    bottom = size.height - verticalPadding,
)

fun DrawScope.drawTechnicalGrid(
    area: VisualizationPlotArea,
    rows: Int = 4,
    columns: Int = 6,
    color: Color,
) {
    require(rows > 0 && columns > 0) { "网格行列数必须为正数" }
    repeat(rows + 1) { index ->
        val y = area.top + area.height * index / rows.toFloat()
        drawLine(
            color = color,
            start = Offset(area.left, y),
            end = Offset(area.right, y),
            strokeWidth = 1f,
        )
    }
    repeat(columns + 1) { index ->
        val x = area.left + area.width * index / columns.toFloat()
        drawLine(
            color = color.copy(alpha = color.alpha * 0.72f),
            start = Offset(x, area.top),
            end = Offset(x, area.bottom),
            strokeWidth = 1f,
        )
    }
}

fun DrawScope.drawReferenceLine(
    area: VisualizationPlotArea,
    y: Float,
    color: Color,
    strokeWidth: Float = 2f,
) {
    drawLine(
        color = color,
        start = Offset(area.left, y),
        end = Offset(area.right, y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

fun DrawScope.drawDataMarker(
    center: Offset,
    color: Color,
    emphasized: Boolean = false,
) {
    drawCircle(
        color = color.copy(alpha = 0.20f),
        radius = if (emphasized) 13f else 9f,
        center = center,
    )
    drawCircle(
        color = color,
        radius = if (emphasized) 6.5f else 4.5f,
        center = center,
    )
}

/** Reusable vector/force/flow arrow for mathematics, physics, chemistry and biology scenes. */
fun DrawScope.drawVectorArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float = 3f,
    headLength: Float = 13f,
) {
    drawLine(color, start, end, strokeWidth, StrokeCap.Round)
    val angle = atan2(end.y - start.y, end.x - start.x)
    listOf(-PI.toFloat() / 6f, PI.toFloat() / 6f).forEach { delta ->
        drawLine(
            color = color,
            start = end,
            end = Offset(
                x = end.x - headLength * cos((angle + delta).toDouble()).toFloat(),
                y = end.y - headLength * sin((angle + delta).toDouble()).toFloat(),
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
