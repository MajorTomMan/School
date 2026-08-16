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
import kotlin.math.ceil
import kotlin.math.floor

internal enum class CartesianVariant { POINT, LINEAR, QUADRATIC, INVERSE }

internal class CartesianRenderer(
    override val key: VisualizationKey,
    private val variant: CartesianVariant,
) : VisualizationRenderer() {
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = when (variant) {
            CartesianVariant.POINT -> listOf(
                VisualizationParameterSpec("x", VisualizationParameterType.NUMBER),
                VisualizationParameterSpec("y", VisualizationParameterType.NUMBER),
            )
            CartesianVariant.LINEAR -> listOf(
                VisualizationParameterSpec("slope", VisualizationParameterType.NUMBER),
                VisualizationParameterSpec("intercept", VisualizationParameterType.NUMBER),
            )
            CartesianVariant.QUADRATIC -> listOf(
                VisualizationParameterSpec("a", VisualizationParameterType.NUMBER),
                VisualizationParameterSpec("b", VisualizationParameterType.NUMBER),
                VisualizationParameterSpec("c", VisualizationParameterType.NUMBER),
            )
            CartesianVariant.INVERSE -> listOf(VisualizationParameterSpec("k", VisualizationParameterType.NUMBER))
        } + listOf(
            VisualizationParameterSpec("xMin", VisualizationParameterType.NUMBER, false),
            VisualizationParameterSpec("xMax", VisualizationParameterType.NUMBER, false),
            VisualizationParameterSpec("yMin", VisualizationParameterType.NUMBER, false),
            VisualizationParameterSpec("yMax", VisualizationParameterType.NUMBER, false),
        ),
        texts = listOf(
            VisualizationTextSpec("title", false, true),
            VisualizationTextSpec("pointLabel", false, true),
            VisualizationTextSpec("note", false, true),
        ),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val parameters = context.invocation.parameters
        val texts = context.invocation.texts
        val xMin = parameters.number("xMin", -5.0).toFloat()
        val xMax = parameters.number("xMax", 5.0).toFloat()
        val yMin = parameters.number("yMin", -4.0).toFloat()
        val yMax = parameters.number("yMax", 4.0).toFloat()
        require(xMax > xMin && yMax > yMin) { "坐标范围无效" }

        fun xRatio(value: Float): Float = ((value - xMin) / (xMax - xMin)).coerceIn(0f, 1f)
        fun yRatio(value: Float): Float = (1f - (value - yMin) / (yMax - yMin)).coerceIn(0f, 1f)

        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = texts.text("title")
            if (title.isNotBlank()) {
                Text(text = title, modifier = Modifier.fillMaxWidth(), color = context.palette.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
            ZoomableVisualizationSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    val left = size.width * 0.08f
                    val top = size.height * 0.08f
                    val right = size.width * 0.95f
                    val bottom = size.height * 0.92f
                    fun xFor(value: Float): Float = left + xRatio(value) * (right - left)
                    fun yFor(value: Float): Float = top + yRatio(value) * (bottom - top)

                    for (x in ceil(xMin.toDouble()).toInt()..floor(xMax.toDouble()).toInt()) {
                        val px = xFor(x.toFloat())
                        drawLine(context.palette.grid.copy(alpha = 0.42f), Offset(px, top), Offset(px, bottom), 1f)
                    }
                    for (y in ceil(yMin.toDouble()).toInt()..floor(yMax.toDouble()).toInt()) {
                        val py = yFor(y.toFloat())
                        drawLine(context.palette.grid.copy(alpha = 0.42f), Offset(left, py), Offset(right, py), 1f)
                    }
                    if (0f in yMin..yMax) drawLine(context.palette.foreground.copy(alpha = 0.72f), Offset(left, yFor(0f)), Offset(right, yFor(0f)), 2f)
                    if (0f in xMin..xMax) drawLine(context.palette.foreground.copy(alpha = 0.72f), Offset(xFor(0f), bottom), Offset(xFor(0f), top), 2f)

                    if (variant == CartesianVariant.POINT) {
                        val x = parameters.number("x").toFloat()
                        val y = parameters.number("y").toFloat()
                        drawCircle(context.palette.secondary, 7f, Offset(xFor(x), yFor(y)))
                    } else {
                        val path = Path()
                        var started = false
                        val sampleCount = 240
                        for (index in 0..sampleCount) {
                            val x = xMin + (xMax - xMin) * index / sampleCount.toFloat()
                            val y = when (variant) {
                                CartesianVariant.LINEAR -> (parameters.number("slope") * x + parameters.number("intercept")).toFloat()
                                CartesianVariant.QUADRATIC -> (parameters.number("a") * x * x + parameters.number("b") * x + parameters.number("c")).toFloat()
                                CartesianVariant.INVERSE -> if (abs(x) < 0.001f) Float.NaN else (parameters.number("k") / x).toFloat()
                                CartesianVariant.POINT -> Float.NaN
                            }
                            if (!y.isFinite() || y !in yMin..yMax) {
                                started = false
                            } else if (!started) {
                                path.moveTo(xFor(x), yFor(y))
                                started = true
                            } else {
                                path.lineTo(xFor(x), yFor(y))
                            }
                        }
                        drawPath(path, context.palette.primary, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    }
                },
                annotations = {
                    val pointLabel = texts.text("pointLabel")
                    if (variant == CartesianVariant.POINT && pointLabel.isNotBlank()) {
                        VisualizationAnnotation(
                            text = pointLabel,
                            x = 0.08f + xRatio(parameters.number("x").toFloat()) * 0.87f,
                            y = 0.08f + yRatio(parameters.number("y").toFloat()) * 0.84f,
                            color = context.palette.secondary,
                            placement = AnnotationPlacement.ABOVE,
                            emphasized = true,
                        )
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
