package com.majortomman.school.visualization.renderers.math

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round

internal enum class NumberLineVariant {
    BASIC,
    CONSTRUCTION,
    POINTS,
    OPPOSITE,
    ABSOLUTE_VALUE,
    COMPARISON,
    MOVEMENT,
    ROOT,
}

internal class NumberLineRenderer(
    override val key: VisualizationKey,
    private val variant: NumberLineVariant,
) : VisualizationRenderer() {
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema: VisualizationSchema = numberLineSchema(variant)

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val p = context.invocation.parameters
        val min = p.number("min", -8.0).toFloat()
        val max = p.number("max", 8.0).toFloat()
        require(max > min) { "数轴 max 必须大于 min" }
        val step = p.number("step", 1.0).toFloat().coerceAtLeast(0.01f)
        val title = context.invocation.texts.text("title")
        val note = context.invocation.texts.text("note")

        when (variant) {
            NumberLineVariant.BASIC -> {
                var value by remember(key.value) { mutableDoubleStateOf(p.number("value", 0.0).coerceIn(min.toDouble(), max.toDouble())) }
                NumberLineScaffold(title, note, modifier, slider = {
                    Slider(value.toFloat(), { value = snap(it.toDouble(), step.toDouble()) }, valueRange = min..max)
                }) {
                    NumberLineGraphic(
                        min = min,
                        max = max,
                        step = step,
                        points = listOf(NumberLinePoint(value.toFloat(), context.palette.primary, formatNumber(value), true)),
                        context = context,
                    )
                }
            }
            NumberLineVariant.CONSTRUCTION -> NumberLineScaffold(title, note, modifier) {
                NumberLineGraphic(
                    min = min,
                    max = max,
                    step = step,
                    points = emptyList(),
                    context = context,
                    originLabel = context.invocation.texts.text("originLabel"),
                    positiveDirectionLabel = context.invocation.texts.text("positiveDirectionLabel"),
                    negativeDirectionLabel = context.invocation.texts.text("negativeDirectionLabel"),
                    unitLabel = context.invocation.texts.text("unitLabel"),
                    showUnit = true,
                )
            }
            NumberLineVariant.POINTS -> {
                val values = p.numberList("values").map(Double::toFloat)
                val points = values.take(8).mapIndexed { index, value ->
                    NumberLinePoint(
                        value = value,
                        color = pointColor(value, context),
                        label = context.invocation.texts.text("label$index", formatNumber(value.toDouble())),
                        above = index % 2 == 0,
                    )
                }
                NumberLineScaffold(title, note, modifier) { NumberLineGraphic(min, max, step, points, context) }
            }
            NumberLineVariant.OPPOSITE -> {
                var value by remember(key.value) { mutableDoubleStateOf(abs(p.number("value", 3.0)).coerceIn(0.0, maxOf(abs(min), abs(max)).toDouble())) }
                val safeValue = value.toFloat()
                NumberLineScaffold(title, note, modifier, slider = {
                    Slider(safeValue, { value = snap(it.toDouble(), step.toDouble()) }, valueRange = 0f..maxOf(abs(min), abs(max)))
                }) {
                    NumberLineGraphic(
                        min,
                        max,
                        step,
                        listOf(
                            NumberLinePoint(-safeValue, context.palette.secondary, context.invocation.texts.text("leftLabel", formatNumber(-value)), true),
                            NumberLinePoint(safeValue, context.palette.primary, context.invocation.texts.text("rightLabel", formatNumber(value)), true),
                        ),
                        context,
                        distanceValues = listOf(-safeValue, safeValue),
                    )
                }
            }
            NumberLineVariant.ABSOLUTE_VALUE -> {
                var value by remember(key.value) { mutableDoubleStateOf(p.number("value", -3.0).coerceIn(min.toDouble(), max.toDouble())) }
                val absolute = abs(value).toFloat()
                NumberLineScaffold(title, note, modifier, slider = {
                    Slider(value.toFloat(), { value = snap(it.toDouble(), step.toDouble()) }, valueRange = min..max)
                }) {
                    NumberLineGraphic(
                        min,
                        max,
                        step,
                        listOf(
                            NumberLinePoint(value.toFloat(), context.palette.primary, context.invocation.texts.text("valueLabel", "x=${formatNumber(value)}"), true),
                            NumberLinePoint(absolute, context.palette.secondary, context.invocation.texts.text("absoluteLabel", "|x|=${formatNumber(abs(value))}"), false),
                        ),
                        context,
                        distanceValues = listOf(value.toFloat()),
                    )
                }
            }
            NumberLineVariant.COMPARISON -> {
                var left by remember(key.value + ".left") { mutableDoubleStateOf(p.number("left", -3.0).coerceIn(min.toDouble(), max.toDouble())) }
                var right by remember(key.value + ".right") { mutableDoubleStateOf(p.number("right", 2.0).coerceIn(min.toDouble(), max.toDouble())) }
                NumberLineScaffold(title, note, modifier, slider = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Slider(left.toFloat(), { left = snap(it.toDouble(), step.toDouble()) }, valueRange = min..max)
                        Slider(right.toFloat(), { right = snap(it.toDouble(), step.toDouble()) }, valueRange = min..max)
                    }
                }) {
                    NumberLineGraphic(
                        min,
                        max,
                        step,
                        listOf(
                            NumberLinePoint(left.toFloat(), context.palette.secondary, context.invocation.texts.text("leftLabel", formatNumber(left)), true),
                            NumberLinePoint(right.toFloat(), context.palette.primary, context.invocation.texts.text("rightLabel", formatNumber(right)), false),
                        ),
                        context,
                    )
                }
            }
            NumberLineVariant.MOVEMENT -> {
                var start by remember(key.value + ".start") { mutableDoubleStateOf(p.number("start", -3.0).coerceIn(min.toDouble(), max.toDouble())) }
                var delta by remember(key.value + ".delta") { mutableDoubleStateOf(p.number("delta", 2.0)) }
                val end = (start + delta).coerceIn(min.toDouble(), max.toDouble())
                NumberLineScaffold(title, note, modifier, slider = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Slider(start.toFloat(), { start = snap(it.toDouble(), step.toDouble()) }, valueRange = min..max)
                        Slider(delta.toFloat(), { delta = snap(it.toDouble(), step.toDouble()) }, valueRange = (min - start.toFloat())..(max - start.toFloat()))
                    }
                }) {
                    NumberLineGraphic(
                        min,
                        max,
                        step,
                        listOf(
                            NumberLinePoint(start.toFloat(), context.palette.primary, context.invocation.texts.text("startLabel", formatNumber(start)), false),
                            NumberLinePoint(end.toFloat(), context.palette.secondary, context.invocation.texts.text("endLabel", formatNumber(end)), false),
                        ),
                        context,
                        movement = start.toFloat() to end.toFloat(),
                        movementLabel = context.invocation.texts.text("movementLabel"),
                    )
                }
            }
            NumberLineVariant.ROOT -> {
                val value = p.number("value", 1.41421356237).toFloat().coerceIn(min, max)
                NumberLineScaffold(title, note, modifier) {
                    NumberLineGraphic(
                        min,
                        max,
                        step,
                        listOf(NumberLinePoint(value, context.palette.secondary, context.invocation.texts.text("pointLabel"), true)),
                        context,
                    )
                }
            }
        }
    }
}

private data class NumberLinePoint(val value: Float, val color: Color, val label: String, val above: Boolean)

@Composable
private fun NumberLineScaffold(
    title: String,
    note: String,
    modifier: Modifier,
    slider: (@Composable () -> Unit)? = null,
    graphic: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (title.isNotBlank()) Text(title, modifier = Modifier.fillMaxWidth(), color = Color(0xFFF5F5F7), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { graphic() }
        slider?.invoke()
        if (note.isNotBlank()) Text(note, modifier = Modifier.fillMaxWidth(), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NumberLineGraphic(
    min: Float,
    max: Float,
    step: Float,
    points: List<NumberLinePoint>,
    context: VisualizationRenderContext,
    originLabel: String = "",
    positiveDirectionLabel: String = "",
    negativeDirectionLabel: String = "",
    unitLabel: String = "",
    showUnit: Boolean = false,
    distanceValues: List<Float> = emptyList(),
    movement: Pair<Float, Float>? = null,
    movementLabel: String = "",
) {
    val range = max - min
    fun ratio(value: Float): Float = ((value - min) / range).coerceIn(0f, 1f)
    val axisY = 0.56f
    val tickCount = ceil(range / step).toInt().coerceIn(1, 80)

    ZoomableVisualizationSurface(
        modifier = Modifier.fillMaxWidth().height(210.dp),
        geometry = {
            val left = size.width * 0.06f
            val right = size.width * 0.94f
            val y = size.height * axisY
            fun xFor(value: Float): Float = left + ratio(value) * (right - left)
            val zeroVisible = 0f in min..max
            if (zeroVisible) {
                val zeroX = xFor(0f)
                drawLine(context.palette.secondary.copy(alpha = 0.14f), Offset(left, y), Offset(zeroX, y), 8f, StrokeCap.Round)
                drawLine(context.palette.primary.copy(alpha = 0.14f), Offset(zeroX, y), Offset(right, y), 8f, StrokeCap.Round)
            }
            drawLine(context.palette.foreground.copy(alpha = 0.82f), Offset(left, y), Offset(right, y), 2.5f, StrokeCap.Round)
            drawArrowHead(Offset(left, y), -1f, context.palette.secondary)
            drawArrowHead(Offset(right, y), 1f, context.palette.primary)

            for (index in 0..tickCount) {
                val value = (min + index * step).coerceAtMost(max)
                val x = xFor(value)
                val origin = abs(value) < step * 0.25f
                drawLine(
                    if (origin) context.palette.foreground else context.palette.muted.copy(alpha = 0.66f),
                    Offset(x, y - if (origin) 10f else 6f),
                    Offset(x, y + if (origin) 10f else 6f),
                    if (origin) 2.5f else 1.5f,
                )
            }

            distanceValues.forEachIndexed { index, value ->
                if (0f in min..max) {
                    val lineY = y + 22f + index * 11f
                    drawLine(pointColor(value, context).copy(alpha = 0.72f), Offset(xFor(0f), lineY), Offset(xFor(value), lineY), 4f, StrokeCap.Round)
                }
            }

            movement?.let { (start, end) ->
                val moveY = y - 27f
                drawLine(context.palette.secondary, Offset(xFor(start), moveY), Offset(xFor(end), moveY), 4f, StrokeCap.Round)
                val direction = if (end >= start) 1f else -1f
                drawArrowHead(Offset(xFor(end), moveY), direction, context.palette.secondary)
            }

            if (showUnit && 0f in min..max && 0f + step <= max) {
                val unitY = y + 47f
                drawLine(context.palette.secondary, Offset(xFor(0f), unitY), Offset(xFor(step), unitY), 3f, StrokeCap.Round)
                drawLine(context.palette.secondary, Offset(xFor(0f), unitY - 5f), Offset(xFor(0f), unitY + 5f), 2f)
                drawLine(context.palette.secondary, Offset(xFor(step), unitY - 5f), Offset(xFor(step), unitY + 5f), 2f)
            }

            points.forEach { point -> drawCircle(point.color, 6.5f, Offset(xFor(point.value), y)) }
        },
        annotations = {
            val labelEvery = when {
                tickCount <= 10 -> 1
                tickCount <= 20 -> 2
                else -> 4
            }
            for (index in 0..tickCount) {
                if (index % labelEvery != 0 && index != tickCount) continue
                val value = (min + index * step).coerceAtMost(max)
                VisualizationAnnotation(formatNumber(value.toDouble()), 0.06f + ratio(value) * 0.88f, axisY + 0.11f, context.palette.muted, AnnotationPlacement.BELOW)
            }
            points.forEach { point ->
                if (point.label.isNotBlank()) VisualizationAnnotation(point.label, 0.06f + ratio(point.value) * 0.88f, axisY, point.color, if (point.above) AnnotationPlacement.ABOVE else AnnotationPlacement.BELOW, true)
            }
            if (originLabel.isNotBlank() && 0f in min..max) VisualizationAnnotation(originLabel, 0.06f + ratio(0f) * 0.88f, axisY - 0.16f, context.palette.foreground, AnnotationPlacement.ABOVE)
            if (negativeDirectionLabel.isNotBlank()) VisualizationAnnotation(negativeDirectionLabel, 0.20f, axisY - 0.20f, context.palette.secondary, AnnotationPlacement.ABOVE)
            if (positiveDirectionLabel.isNotBlank()) VisualizationAnnotation(positiveDirectionLabel, 0.80f, axisY - 0.20f, context.palette.primary, AnnotationPlacement.ABOVE)
            if (unitLabel.isNotBlank() && showUnit && 0f in min..max) VisualizationAnnotation(unitLabel, 0.06f + ratio(step / 2f) * 0.88f, axisY + 0.29f, context.palette.secondary, AnnotationPlacement.BELOW)
            if (movementLabel.isNotBlank() && movement != null) {
                val midpoint = (movement.first + movement.second) / 2f
                VisualizationAnnotation(movementLabel, 0.06f + ratio(midpoint) * 0.88f, axisY - 0.15f, context.palette.secondary, AnnotationPlacement.ABOVE)
            }
        },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(end: Offset, direction: Float, color: Color) {
    val length = 11f
    drawLine(color, end, Offset(end.x - direction * length, end.y - length * 0.62f), 2.5f, StrokeCap.Round)
    drawLine(color, end, Offset(end.x - direction * length, end.y + length * 0.62f), 2.5f, StrokeCap.Round)
}

private fun pointColor(value: Float, context: VisualizationRenderContext): Color = when {
    value < 0f -> context.palette.secondary
    value > 0f -> context.palette.primary
    else -> context.palette.foreground
}

private fun snap(value: Double, step: Double): Double = round(value / step) * step

private fun formatNumber(value: Double): String {
    val integer = value.toLong()
    val raw = if (abs(value - integer) < 0.000001) integer.toString() else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
    return raw.replace('-', '−')
}

private fun numberLineSchema(variant: NumberLineVariant): VisualizationSchema {
    val baseParameters = mutableListOf(
        VisualizationParameterSpec("min", VisualizationParameterType.NUMBER, false),
        VisualizationParameterSpec("max", VisualizationParameterType.NUMBER, false),
        VisualizationParameterSpec("step", VisualizationParameterType.NUMBER, false),
    )
    val texts = mutableListOf(
        VisualizationTextSpec("title", false, true),
        VisualizationTextSpec("note", false, true),
    )
    when (variant) {
        NumberLineVariant.BASIC -> baseParameters += VisualizationParameterSpec("value", VisualizationParameterType.NUMBER, false)
        NumberLineVariant.CONSTRUCTION -> {
            texts += VisualizationTextSpec("originLabel", false, true)
            texts += VisualizationTextSpec("positiveDirectionLabel", false, true)
            texts += VisualizationTextSpec("negativeDirectionLabel", false, true)
            texts += VisualizationTextSpec("unitLabel", false, true)
        }
        NumberLineVariant.POINTS -> {
            baseParameters += VisualizationParameterSpec("values", VisualizationParameterType.NUMBER_LIST)
            repeat(8) { texts += VisualizationTextSpec("label$it", false, true) }
        }
        NumberLineVariant.OPPOSITE -> {
            baseParameters += VisualizationParameterSpec("value", VisualizationParameterType.NUMBER, false)
            texts += VisualizationTextSpec("leftLabel", false, true)
            texts += VisualizationTextSpec("rightLabel", false, true)
        }
        NumberLineVariant.ABSOLUTE_VALUE -> {
            baseParameters += VisualizationParameterSpec("value", VisualizationParameterType.NUMBER, false)
            texts += VisualizationTextSpec("valueLabel", false, true)
            texts += VisualizationTextSpec("absoluteLabel", false, true)
        }
        NumberLineVariant.COMPARISON -> {
            baseParameters += VisualizationParameterSpec("left", VisualizationParameterType.NUMBER, false)
            baseParameters += VisualizationParameterSpec("right", VisualizationParameterType.NUMBER, false)
            texts += VisualizationTextSpec("leftLabel", false, true)
            texts += VisualizationTextSpec("rightLabel", false, true)
        }
        NumberLineVariant.MOVEMENT -> {
            baseParameters += VisualizationParameterSpec("start", VisualizationParameterType.NUMBER, false)
            baseParameters += VisualizationParameterSpec("delta", VisualizationParameterType.NUMBER, false)
            texts += VisualizationTextSpec("startLabel", false, true)
            texts += VisualizationTextSpec("endLabel", false, true)
            texts += VisualizationTextSpec("movementLabel", false, true)
        }
        NumberLineVariant.ROOT -> {
            baseParameters += VisualizationParameterSpec("value", VisualizationParameterType.NUMBER)
            texts += VisualizationTextSpec("pointLabel")
        }
    }
    return VisualizationSchema(baseParameters, texts)
}
