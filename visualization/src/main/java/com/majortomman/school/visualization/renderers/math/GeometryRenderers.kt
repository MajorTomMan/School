package com.majortomman.school.visualization.renderers.math

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.visualization.AnnotationPlacement
import com.majortomman.school.visualization.VisualizationAnnotation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationParameterSpec
import com.majortomman.school.visualization.VisualizationParameterType
import com.majortomman.school.visualization.VisualizationRenderContext
import com.majortomman.school.visualization.VisualizationRenderer
import com.majortomman.school.visualization.VisualizationSchema
import com.majortomman.school.visualization.VisualizationSubject
import com.majortomman.school.visualization.VisualizationTextSpec
import com.majortomman.school.visualization.ZoomableVisualizationSurface
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal enum class GeometryVariant {
    TRIANGLE,
    CIRCLE,
    ANGLE,
    PARALLEL,
    RIGHT_TRIANGLE,
    LINE_RAY_SEGMENT,
    PROJECTION,
    OBJECT_ABSTRACTION,
}

internal class GeometryRenderer(
    override val key: VisualizationKey,
    private val variant: GeometryVariant,
) : VisualizationRenderer() {
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        texts = listOf(
            VisualizationTextSpec("title", false, true),
            VisualizationTextSpec("labelA", false, true),
            VisualizationTextSpec("labelB", false, true),
            VisualizationTextSpec("labelC", false, true),
            VisualizationTextSpec("labelD", false, true),
            VisualizationTextSpec("note", false, true),
        ),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val texts = context.invocation.texts
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = texts.text("title")
            if (title.isNotBlank()) GeometryTitle(title, context)
            ZoomableVisualizationSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    when (variant) {
                        GeometryVariant.TRIANGLE, GeometryVariant.RIGHT_TRIANGLE -> drawTriangleGeometry(context, variant == GeometryVariant.RIGHT_TRIANGLE)
                        GeometryVariant.CIRCLE -> drawCircleGeometry(context)
                        GeometryVariant.ANGLE -> drawAngleGeometry(context)
                        GeometryVariant.PARALLEL -> drawParallelGeometry(context)
                        GeometryVariant.LINE_RAY_SEGMENT -> drawLineRaySegmentGeometry(context)
                        GeometryVariant.PROJECTION -> drawProjectionGeometry(context)
                        GeometryVariant.OBJECT_ABSTRACTION -> drawObjectAbstractionGeometry(context)
                    }
                },
                annotations = {
                    val anchors = when (variant) {
                        GeometryVariant.TRIANGLE -> listOf(0.20f to 0.80f, 0.80f to 0.80f, 0.52f to 0.16f)
                        GeometryVariant.RIGHT_TRIANGLE -> listOf(0.20f to 0.80f, 0.80f to 0.80f, 0.20f to 0.20f)
                        GeometryVariant.CIRCLE -> listOf(0.50f to 0.50f)
                        GeometryVariant.ANGLE -> listOf(0.35f to 0.72f, 0.82f to 0.72f, 0.66f to 0.18f)
                        GeometryVariant.PARALLEL -> emptyList()
                        GeometryVariant.LINE_RAY_SEGMENT -> listOf(0.50f to 0.22f, 0.50f to 0.50f, 0.50f to 0.78f)
                        GeometryVariant.PROJECTION -> listOf(0.24f to 0.72f, 0.74f to 0.16f, 0.74f to 0.48f, 0.74f to 0.78f)
                        GeometryVariant.OBJECT_ABSTRACTION -> listOf(0.23f to 0.22f, 0.77f to 0.22f, 0.23f to 0.64f, 0.77f to 0.64f)
                    }
                    listOf("labelA", "labelB", "labelC", "labelD").forEachIndexed { index, textKey ->
                        val label = texts.text(textKey)
                        if (label.isNotBlank() && index < anchors.size) {
                            VisualizationAnnotation(
                                text = label,
                                x = anchors[index].first,
                                y = anchors[index].second,
                                color = if (index % 2 == 0) context.palette.primary else context.palette.secondary,
                                placement = AnnotationPlacement.ABOVE,
                                emphasized = true,
                            )
                        }
                    }
                },
            )
            val note = texts.text("note")
            if (note.isNotBlank()) GeometryNote(note, context)
        }
    }
}

internal enum class TransformVariant { TRANSLATION, SYMMETRY, ROTATION }

internal class TransformationRenderer(
    override val key: VisualizationKey,
    private val variant: TransformVariant,
) : VisualizationRenderer() {
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = listOf(
            VisualizationParameterSpec("dx", VisualizationParameterType.NUMBER, false),
            VisualizationParameterSpec("dy", VisualizationParameterType.NUMBER, false),
        ),
        texts = listOf(
            VisualizationTextSpec("title", false, true),
            VisualizationTextSpec("originalLabel", false, true),
            VisualizationTextSpec("resultLabel", false, true),
            VisualizationTextSpec("note", false, true),
        ),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val parameters = context.invocation.parameters
        val texts = context.invocation.texts
        val original = listOf(Offset(0.20f, 0.70f), Offset(0.34f, 0.28f), Offset(0.45f, 0.68f))
        val transformed = original.map { point ->
            when (variant) {
                TransformVariant.TRANSLATION -> Offset(point.x + parameters.number("dx", 0.36).toFloat(), point.y + parameters.number("dy", -0.08).toFloat())
                TransformVariant.SYMMETRY -> Offset(1f - point.x, point.y)
                TransformVariant.ROTATION -> Offset(0.5f + (0.5f - point.y), 0.5f + (point.x - 0.5f))
            }
        }
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = texts.text("title")
            if (title.isNotBlank()) GeometryTitle(title, context)
            ZoomableVisualizationSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    fun toPixels(point: Offset): Offset = Offset(point.x * size.width, point.y * size.height)
                    drawPolygon(original.map(::toPixels), context.palette.primary)
                    drawPolygon(transformed.map(::toPixels), context.palette.secondary)
                    original.zip(transformed).forEach { (from, to) -> drawLine(context.palette.grid, toPixels(from), toPixels(to), 2f) }
                },
                annotations = {
                    val originalLabel = texts.text("originalLabel")
                    val resultLabel = texts.text("resultLabel")
                    if (originalLabel.isNotBlank()) VisualizationAnnotation(originalLabel, original[0].x, original[0].y, context.palette.primary, AnnotationPlacement.BELOW)
                    if (resultLabel.isNotBlank()) VisualizationAnnotation(resultLabel, transformed[0].x, transformed[0].y, context.palette.secondary, AnnotationPlacement.BELOW)
                },
            )
            val note = texts.text("note")
            if (note.isNotBlank()) GeometryNote(note, context)
        }
    }
}

private fun DrawScope.drawTriangleGeometry(context: VisualizationRenderContext, rightTriangle: Boolean) {
    val a = Offset(size.width * 0.20f, size.height * 0.80f)
    val b = Offset(size.width * 0.80f, size.height * 0.80f)
    val c = if (rightTriangle) Offset(size.width * 0.20f, size.height * 0.20f) else Offset(size.width * 0.52f, size.height * 0.16f)
    drawLine(context.palette.primary, a, b, 4f)
    drawLine(context.palette.primary, a, c, 4f)
    drawLine(context.palette.secondary, c, b, 4f)
    if (rightTriangle) {
        drawLine(context.palette.muted, Offset(a.x + 20f, a.y), Offset(a.x + 20f, a.y - 20f), 2f)
        drawLine(context.palette.muted, Offset(a.x, a.y - 20f), Offset(a.x + 20f, a.y - 20f), 2f)
    }
}

private fun DrawScope.drawCircleGeometry(context: VisualizationRenderContext) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.32f
    drawCircle(context.palette.primary, radius, center, style = Stroke(4f))
    drawLine(context.palette.secondary, center, Offset(center.x + radius, center.y), 3f)
}

private fun DrawScope.drawAngleGeometry(context: VisualizationRenderContext) {
    val origin = Offset(size.width * 0.35f, size.height * 0.72f)
    val first = Offset(size.width * 0.82f, size.height * 0.72f)
    val second = Offset(size.width * 0.66f, size.height * 0.18f)
    drawLine(context.palette.primary, origin, first, 5f, StrokeCap.Round)
    drawLine(context.palette.secondary, origin, second, 5f, StrokeCap.Round)
    drawCircle(context.palette.foreground, 7f, origin)
    drawGeometryArrowHead(first, 1f, 0f, context.palette.primary)
    val dx = second.x - origin.x
    val dy = second.y - origin.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    drawGeometryArrowHead(second, dx / length, dy / length, context.palette.secondary)
}

private fun DrawScope.drawParallelGeometry(context: VisualizationRenderContext) {
    drawLine(context.palette.primary, Offset(size.width * 0.10f, size.height * 0.32f), Offset(size.width * 0.90f, size.height * 0.22f), 4f)
    drawLine(context.palette.primary, Offset(size.width * 0.10f, size.height * 0.72f), Offset(size.width * 0.90f, size.height * 0.62f), 4f)
    drawLine(context.palette.secondary, Offset(size.width * 0.35f, size.height * 0.08f), Offset(size.width * 0.62f, size.height * 0.92f), 4f)
}

private fun DrawScope.drawLineRaySegmentGeometry(context: VisualizationRenderContext) {
    val left = size.width * 0.18f
    val right = size.width * 0.82f
    val lineY = size.height * 0.22f
    val rayY = size.height * 0.50f
    val segmentY = size.height * 0.78f

    drawLine(context.palette.primary, Offset(left, lineY), Offset(right, lineY), 4f, StrokeCap.Round)
    drawGeometryArrowHead(Offset(left, lineY), -1f, 0f, context.palette.primary)
    drawGeometryArrowHead(Offset(right, lineY), 1f, 0f, context.palette.primary)

    drawCircle(context.palette.secondary, 7f, Offset(left, rayY))
    drawLine(context.palette.secondary, Offset(left, rayY), Offset(right, rayY), 4f, StrokeCap.Round)
    drawGeometryArrowHead(Offset(right, rayY), 1f, 0f, context.palette.secondary)

    drawCircle(context.palette.foreground, 7f, Offset(left, segmentY))
    drawCircle(context.palette.foreground, 7f, Offset(right, segmentY))
    drawLine(context.palette.foreground, Offset(left, segmentY), Offset(right, segmentY), 4f, StrokeCap.Round)
}

private fun DrawScope.drawProjectionGeometry(context: VisualizationRenderContext) {
    val x = size.width * 0.10f
    val y = size.height * 0.26f
    val width = size.width * 0.28f
    val height = size.height * 0.36f
    val depth = size.width * 0.08f
    val points = listOf(
        Offset(x, y), Offset(x + width, y), Offset(x + width, y + height), Offset(x, y + height),
        Offset(x + depth, y - depth), Offset(x + width + depth, y - depth),
        Offset(x + width + depth, y + height - depth), Offset(x + depth, y + height - depth),
    )
    val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
    edges.forEach { (from, to) -> drawLine(context.palette.primary, points[from], points[to], 3f) }

    val viewX = size.width * 0.62f
    drawRect(context.palette.secondary, Offset(viewX, size.height * 0.18f), Size(size.width * 0.24f, size.height * 0.16f), style = Stroke(3f))
    drawRect(context.palette.secondary, Offset(viewX + size.width * 0.07f, size.height * 0.50f), Size(size.width * 0.10f, size.height * 0.16f), style = Stroke(3f))
    drawRect(context.palette.secondary, Offset(viewX, size.height * 0.80f), Size(size.width * 0.24f, size.height * 0.07f), style = Stroke(3f))
}

private fun DrawScope.drawObjectAbstractionGeometry(context: VisualizationRenderContext) {
    drawRect(context.palette.primary, Offset(size.width * 0.12f, size.height * 0.22f), Size(size.width * 0.22f, size.height * 0.22f), style = Stroke(4f))
    drawRect(context.palette.secondary, Offset(size.width * 0.66f, size.height * 0.22f), Size(size.width * 0.22f, size.height * 0.22f), style = Stroke(4f))
    drawGeometryArrow(context.palette.secondary, Offset(size.width * 0.38f, size.height * 0.33f), Offset(size.width * 0.62f, size.height * 0.33f))

    drawRect(context.palette.primary, Offset(size.width * 0.15f, size.height * 0.58f), Size(size.width * 0.16f, size.height * 0.20f), style = Stroke(4f))
    drawOval(context.palette.primary, Offset(size.width * 0.15f, size.height * 0.56f), Size(size.width * 0.16f, size.height * 0.05f), style = Stroke(3f))
    drawRect(context.palette.secondary, Offset(size.width * 0.69f, size.height * 0.58f), Size(size.width * 0.16f, size.height * 0.20f), style = Stroke(4f))
    drawOval(context.palette.secondary, Offset(size.width * 0.69f, size.height * 0.56f), Size(size.width * 0.16f, size.height * 0.05f), style = Stroke(3f))
    drawGeometryArrow(context.palette.secondary, Offset(size.width * 0.38f, size.height * 0.68f), Offset(size.width * 0.62f, size.height * 0.68f))
}

private fun DrawScope.drawGeometryArrow(color: Color, start: Offset, end: Offset) {
    drawLine(color, start, end, 4f, StrokeCap.Round)
    val angle = atan2(end.y - start.y, end.x - start.x)
    val length = 13f
    drawLine(color, end, Offset(end.x - length * cos((angle - PI.toFloat() / 6f).toDouble()).toFloat(), end.y - length * sin((angle - PI.toFloat() / 6f).toDouble()).toFloat()), 4f)
    drawLine(color, end, Offset(end.x - length * cos((angle + PI.toFloat() / 6f).toDouble()).toFloat(), end.y - length * sin((angle + PI.toFloat() / 6f).toDouble()).toFloat()), 4f)
}

private fun DrawScope.drawGeometryArrowHead(end: Offset, dx: Float, dy: Float, color: Color) {
    val length = 16f
    val wing = 9f
    val perpendicularX = -dy
    val perpendicularY = dx
    drawLine(color, end, Offset(end.x - dx * length + perpendicularX * wing, end.y - dy * length + perpendicularY * wing), 3f, StrokeCap.Round)
    drawLine(color, end, Offset(end.x - dx * length - perpendicularX * wing, end.y - dy * length - perpendicularY * wing), 3f, StrokeCap.Round)
}

private fun DrawScope.drawPolygon(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    points.zip(points.drop(1) + points.first()).forEach { (from, to) -> drawLine(color, from, to, 4f) }
}

@Composable
private fun GeometryTitle(text: String, context: VisualizationRenderContext) {
    Text(text = text, modifier = Modifier.fillMaxWidth(), color = context.palette.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
}

@Composable
private fun GeometryNote(text: String, context: VisualizationRenderContext) {
    Text(text = text, modifier = Modifier.fillMaxWidth(), color = context.palette.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
}
