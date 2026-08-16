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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.abs

internal enum class ChartVariant { LINE, BAR }

internal class BasicChartRenderer(
    override val key: VisualizationKey,
    private val variant: ChartVariant,
) : VisualizationRenderer() {
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = listOf(VisualizationParameterSpec("values", VisualizationParameterType.NUMBER_LIST)),
        texts = buildList {
            add(VisualizationTextSpec("title", false, true))
            repeat(8) { add(VisualizationTextSpec("label$it", false, true)) }
            add(VisualizationTextSpec("note", false, true))
        },
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val values = context.invocation.parameters.numberList("values").take(8).map(Double::toFloat)
        require(values.isNotEmpty()) { "图表 values 不能为空" }
        val texts = context.invocation.texts
        val minimum = minOf(0f, values.minOrNull() ?: 0f)
        val maximumRaw = maxOf(0f, values.maxOrNull() ?: 0f)
        val maximum = if (maximumRaw == minimum) maximumRaw + 1f else maximumRaw
        fun yRatio(value: Float): Float = 0.86f - (value - minimum) / (maximum - minimum) * 0.70f

        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = texts.text("title")
            if (title.isNotBlank()) {
                Text(text = title, modifier = Modifier.fillMaxWidth(), color = context.palette.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
            ZoomableVisualizationSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    val left = size.width * 0.08f
                    val right = size.width * 0.94f
                    val baselineY = size.height * yRatio(0f)
                    drawLine(context.palette.foreground.copy(alpha = 0.68f), Offset(left, baselineY), Offset(right, baselineY), 2f)
                    val slotCount = values.size
                    if (variant == ChartVariant.LINE) {
                        val path = Path()
                        values.forEachIndexed { index, value ->
                            val x = left + (right - left) * (index + 0.5f) / slotCount
                            val y = size.height * yRatio(value)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(context.palette.secondary, 6f, Offset(x, y))
                        }
                        drawPath(path, context.palette.primary, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    } else {
                        values.forEachIndexed { index, value ->
                            val slotWidth = (right - left) / slotCount
                            val x = left + slotWidth * index + slotWidth * 0.22f
                            val y = size.height * yRatio(value)
                            val top = minOf(y, baselineY)
                            drawRect(context.palette.primary.copy(alpha = 0.82f), Offset(x, top), Size(slotWidth * 0.56f, abs(baselineY - y)))
                        }
                    }
                },
                annotations = {
                    values.forEachIndexed { index, _ ->
                        val label = texts.text("label$index")
                        if (label.isNotBlank()) {
                            VisualizationAnnotation(
                                text = label,
                                x = 0.08f + 0.86f * (index + 0.5f) / values.size,
                                y = 0.91f,
                                color = context.palette.muted,
                                placement = AnnotationPlacement.BELOW,
                            )
                        }
                    }
                },
            )
            val note = texts.text("note")
            if (note.isNotBlank()) {
                Text(text = note, modifier = Modifier.fillMaxWidth(), color = context.palette.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

internal class ProbabilityTreeRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.probability.tree")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(texts = buildList {
        add(VisualizationTextSpec("title", false, true))
        repeat(7) { add(VisualizationTextSpec("label$it", false, true)) }
        add(VisualizationTextSpec("note", false, true))
    })

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val texts = context.invocation.texts
        val nodes = listOf(
            Offset(0.12f, 0.50f),
            Offset(0.46f, 0.28f),
            Offset(0.46f, 0.72f),
            Offset(0.86f, 0.14f),
            Offset(0.86f, 0.38f),
            Offset(0.86f, 0.62f),
            Offset(0.86f, 0.86f),
        )
        val edges = listOf(0 to 1, 0 to 2, 1 to 3, 1 to 4, 2 to 5, 2 to 6)

        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = texts.text("title")
            if (title.isNotBlank()) {
                Text(text = title, modifier = Modifier.fillMaxWidth(), color = context.palette.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
            ZoomableVisualizationSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    fun toPixels(point: Offset): Offset = Offset(point.x * size.width, point.y * size.height)
                    edges.forEachIndexed { index, (from, to) ->
                        drawLine(if (index % 2 == 0) context.palette.primary else context.palette.secondary, toPixels(nodes[from]), toPixels(nodes[to]), 3f)
                    }
                    nodes.forEach { drawCircle(context.palette.foreground, 6f, toPixels(it)) }
                },
                annotations = {
                    nodes.forEachIndexed { index, node ->
                        val label = texts.text("label$index")
                        if (label.isNotBlank()) {
                            VisualizationAnnotation(label, node.x, node.y, context.palette.foreground, AnnotationPlacement.ABOVE)
                        }
                    }
                },
            )
            val note = texts.text("note")
            if (note.isNotBlank()) {
                Text(text = note, modifier = Modifier.fillMaxWidth(), color = context.palette.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
