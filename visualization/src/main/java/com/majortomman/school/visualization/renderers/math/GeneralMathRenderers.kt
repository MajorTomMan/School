package com.majortomman.school.visualization.renderers.math

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

internal class OppositeQuantitiesRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.context.opposite-quantities")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = listOf(
            VisualizationParameterSpec("positive", VisualizationParameterType.NUMBER),
            VisualizationParameterSpec("negative", VisualizationParameterType.NUMBER),
        ),
        texts = listOf(
            VisualizationTextSpec("title", false, true),
            VisualizationTextSpec("positiveLabel"),
            VisualizationTextSpec("negativeLabel"),
            VisualizationTextSpec("baselineLabel", false, true),
            VisualizationTextSpec("note", false, true),
        ),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val positive = abs(context.invocation.parameters.number("positive")).toFloat()
        val negative = -abs(context.invocation.parameters.number("negative")).toFloat()
        val magnitude = max(positive, abs(negative)).coerceAtLeast(1f)
        val title = context.invocation.texts.text("title")
        val note = context.invocation.texts.text("note")
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title.isNotBlank()) VisualTitle(title)
            ZoomableVisualizationSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    val centerX = size.width / 2f
                    val centerY = size.height * 0.56f
                    val half = size.width * 0.40f
                    drawLine(context.palette.foreground.copy(alpha = 0.78f), Offset(centerX - half, centerY), Offset(centerX + half, centerY), 3f, StrokeCap.Round)
                    drawLine(context.palette.grid, Offset(centerX, centerY - size.height * 0.34f), Offset(centerX, centerY + size.height * 0.24f), 2f)
                    val positiveX = centerX + half * positive / magnitude
                    val negativeX = centerX + half * negative / magnitude
                    drawLine(context.palette.primary, Offset(centerX, centerY), Offset(positiveX, centerY), 7f, StrokeCap.Round)
                    drawLine(context.palette.secondary, Offset(centerX, centerY), Offset(negativeX, centerY), 7f, StrokeCap.Round)
                    drawCircle(context.palette.primary, 8f, Offset(positiveX, centerY))
                    drawCircle(context.palette.secondary, 8f, Offset(negativeX, centerY))
                },
                annotations = {
                    VisualizationAnnotation(context.invocation.texts.text("negativeLabel"), 0.10f, 0.56f, context.palette.secondary, AnnotationPlacement.ABOVE, true)
                    VisualizationAnnotation(context.invocation.texts.text("positiveLabel"), 0.90f, 0.56f, context.palette.primary, AnnotationPlacement.ABOVE, true)
                    val baseline = context.invocation.texts.text("baselineLabel")
                    if (baseline.isNotBlank()) VisualizationAnnotation(baseline, 0.50f, 0.56f, context.palette.foreground, AnnotationPlacement.BELOW)
                },
            )
            if (note.isNotBlank()) VisualNote(note)
        }
    }
}

internal class RationalClassificationRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.classification.rational")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        texts = listOf(
            VisualizationTextSpec("title", false, true),
            VisualizationTextSpec("rowPositive"),
            VisualizationTextSpec("rowZero"),
            VisualizationTextSpec("rowNegative"),
            VisualizationTextSpec("columnInteger"),
            VisualizationTextSpec("columnFraction"),
            VisualizationTextSpec("positiveInteger"),
            VisualizationTextSpec("positiveFraction"),
            VisualizationTextSpec("zero"),
            VisualizationTextSpec("negativeInteger"),
            VisualizationTextSpec("negativeFraction"),
            VisualizationTextSpec("note", false, true),
        ),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        var selected by remember { mutableIntStateOf(4) }
        val t = context.invocation.texts
        val cells = listOf(t.text("positiveInteger"), t.text("positiveFraction"), t.text("zero"), "", t.text("negativeInteger"), t.text("negativeFraction"))
        val rows = listOf(t.text("rowPositive"), t.text("rowZero"), t.text("rowNegative"))
        val columns = listOf(t.text("columnInteger"), t.text("columnFraction"))
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.width(54.dp))
                columns.forEach { Text(it, Modifier.weight(1f), context.palette.muted, 11.sp, textAlign = TextAlign.Center) }
            }
            rows.forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row, Modifier.width(54.dp), if (rowIndex == 2) context.palette.secondary else context.palette.primary, 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    repeat(2) { columnIndex ->
                        val index = rowIndex * 2 + columnIndex
                        val text = cells[index]
                        val enabled = text.isNotBlank()
                        val color = if (rowIndex == 2) context.palette.secondary else if (rowIndex == 1) context.palette.foreground else context.palette.primary
                        Box(
                            Modifier.weight(1f).height(54.dp).background(if (selected == index) color.copy(alpha = 0.16f) else Color.Transparent, RoundedCornerShape(8.dp)).border(1.dp, color.copy(alpha = if (selected == index) 0.86f else 0.30f), RoundedCornerShape(8.dp)).clickable(enabled) { selected = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (enabled) text else "—", color = if (enabled) color else context.palette.muted.copy(alpha = 0.45f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal class IntegerToFractionRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.process.integer-to-fraction")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(texts = buildList {
        add(VisualizationTextSpec("title", false, true))
        repeat(3) { index ->
            add(VisualizationTextSpec("source$index"))
            add(VisualizationTextSpec("target$index"))
        }
        add(VisualizationTextSpec("note", false, true))
    })

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val t = context.invocation.texts
        Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t.text("source$index"), color = context.palette.foreground, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        Text("↓", color = context.palette.muted, fontSize = 18.sp)
                        Text(t.text("target$index"), color = context.palette.secondary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal class ExpressionProcessRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.process.expression")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(texts = listOf(
        VisualizationTextSpec("title", false, true),
        VisualizationTextSpec("source"),
        VisualizationTextSpec("middle", false, true),
        VisualizationTextSpec("target"),
        VisualizationTextSpec("firstTransition", false, true),
        VisualizationTextSpec("secondTransition", false, true),
        VisualizationTextSpec("note", false, true),
    ))

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val t = context.invocation.texts
        val middle = t.text("middle")
        val entries = if (middle.isBlank()) listOf(t.text("source"), t.text("target")) else listOf(t.text("source"), middle, t.text("target"))
        Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            entries.forEachIndexed { index, text ->
                Text(text, color = if (index == entries.lastIndex) context.palette.secondary else context.palette.foreground, fontSize = 23.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                if (index != entries.lastIndex) {
                    val transition = if (index == 0) t.text("firstTransition") else t.text("secondTransition")
                    Text(if (transition.isBlank()) "↓" else "↓  $transition", color = context.palette.primary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal class SignRuleRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.rule.sign")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(texts = listOf(
        VisualizationTextSpec("title", false, true),
        VisualizationTextSpec("rule0"),
        VisualizationTextSpec("rule1"),
        VisualizationTextSpec("rule2"),
        VisualizationTextSpec("rule3"),
        VisualizationTextSpec("note", false, true),
    ))

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val t = context.invocation.texts
        Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            repeat(4) { index -> Text(t.text("rule$index"), Modifier.fillMaxWidth(), if (index == 0 || index == 3) context.palette.primary else context.palette.secondary, 20.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium) }
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal class PowerProcessRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.process.power")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = listOf(
            VisualizationParameterSpec("base", VisualizationParameterType.NUMBER),
            VisualizationParameterSpec("exponent", VisualizationParameterType.NUMBER),
            VisualizationParameterSpec("minBase", VisualizationParameterType.NUMBER, false),
            VisualizationParameterSpec("maxBase", VisualizationParameterType.NUMBER, false),
        ),
        texts = listOf(VisualizationTextSpec("title", false, true), VisualizationTextSpec("note", false, true)),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val p = context.invocation.parameters
        var base by remember { mutableDoubleStateOf(p.number("base")) }
        var exponent by remember { mutableDoubleStateOf(p.number("exponent").coerceIn(1.0, 8.0)) }
        val exponentInt = exponent.toInt().coerceIn(1, 8)
        val result = base.pow(exponentInt)
        val factors = List(exponentInt) { compactNumber(base) }.joinToString(" × ")
        Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            if (context.invocation.texts.text("title").isNotBlank()) VisualTitle(context.invocation.texts.text("title"))
            Slider(base.toFloat(), { base = it.toDouble() }, valueRange = p.number("minBase", -4.0).toFloat()..p.number("maxBase", 4.0).toFloat())
            Slider(exponent.toFloat(), { exponent = it.toInt().coerceIn(1, 8).toDouble() }, valueRange = 1f..8f, steps = 6)
            Text("${compactNumber(base)}^$exponentInt", Modifier.fillMaxWidth(), context.palette.foreground, 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Text(factors, Modifier.fillMaxWidth(), context.palette.primary, 16.sp, textAlign = TextAlign.Center)
            Text("= ${compactNumber(result)}", Modifier.fillMaxWidth(), context.palette.secondary, 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (context.invocation.texts.text("note").isNotBlank()) VisualNote(context.invocation.texts.text("note"))
        }
    }
}

internal class EquationBalanceRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.balance.equation")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = listOf(VisualizationParameterSpec("tilt", VisualizationParameterType.NUMBER, false)),
        texts = listOf(
            VisualizationTextSpec("title", false, true),
            VisualizationTextSpec("left"),
            VisualizationTextSpec("right"),
            VisualizationTextSpec("note", false, true),
        ),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val tilt = context.invocation.parameters.number("tilt", 0.0).toFloat().coerceIn(-1f, 1f)
        val t = context.invocation.texts
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            ZoomableVisualizationSurface(
                Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    val cx = size.width / 2f
                    val beamY = size.height * 0.38f
                    val delta = size.height * 0.08f * tilt
                    val left = Offset(size.width * 0.18f, beamY + delta)
                    val right = Offset(size.width * 0.82f, beamY - delta)
                    drawLine(context.palette.foreground, left, right, 4f, StrokeCap.Round)
                    drawLine(context.palette.muted, Offset(cx, beamY), Offset(cx, size.height * 0.76f), 4f)
                    drawLine(context.palette.primary, Offset(size.width * 0.26f, left.y), Offset(size.width * 0.26f, left.y + 55f), 2f)
                    drawLine(context.palette.secondary, Offset(size.width * 0.74f, right.y), Offset(size.width * 0.74f, right.y + 55f), 2f)
                    val base = Path().apply {
                        moveTo(cx, size.height * 0.67f)
                        lineTo(cx - 45f, size.height * 0.84f)
                        lineTo(cx + 45f, size.height * 0.84f)
                        close()
                    }
                    drawPath(base, context.palette.muted.copy(alpha = 0.65f), style = Stroke(3f))
                },
                annotations = {
                    VisualizationAnnotation(t.text("left"), 0.26f, 0.58f, context.palette.primary, AnnotationPlacement.BELOW, true)
                    VisualizationAnnotation(t.text("right"), 0.74f, 0.58f, context.palette.secondary, AnnotationPlacement.BELOW, true)
                },
            )
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal enum class CartesianVariant { POINT, LINEAR, QUADRATIC, INVERSE }

internal class CartesianRenderer(override val key: VisualizationKey, private val variant: CartesianVariant) : VisualizationRenderer() {
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = when (variant) {
            CartesianVariant.POINT -> listOf(VisualizationParameterSpec("x", VisualizationParameterType.NUMBER), VisualizationParameterSpec("y", VisualizationParameterType.NUMBER))
            CartesianVariant.LINEAR -> listOf(VisualizationParameterSpec("slope", VisualizationParameterType.NUMBER), VisualizationParameterSpec("intercept", VisualizationParameterType.NUMBER))
            CartesianVariant.QUADRATIC -> listOf(VisualizationParameterSpec("a", VisualizationParameterType.NUMBER), VisualizationParameterSpec("b", VisualizationParameterType.NUMBER), VisualizationParameterSpec("c", VisualizationParameterType.NUMBER))
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
        val p = context.invocation.parameters
        val t = context.invocation.texts
        val xMin = p.number("xMin", -5.0).toFloat()
        val xMax = p.number("xMax", 5.0).toFloat()
        val yMin = p.number("yMin", -4.0).toFloat()
        val yMax = p.number("yMax", 4.0).toFloat()
        require(xMax > xMin && yMax > yMin) { "坐标范围无效" }
        fun xRatio(x: Float) = ((x - xMin) / (xMax - xMin)).coerceIn(0f, 1f)
        fun yRatio(y: Float) = (1f - (y - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            ZoomableVisualizationSurface(
                Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    val left = size.width * 0.08f
                    val top = size.height * 0.08f
                    val right = size.width * 0.95f
                    val bottom = size.height * 0.92f
                    fun xFor(x: Float) = left + xRatio(x) * (right - left)
                    fun yFor(y: Float) = top + yRatio(y) * (bottom - top)
                    if (0f in yMin..yMax) drawLine(context.palette.foreground.copy(alpha = 0.72f), Offset(left, yFor(0f)), Offset(right, yFor(0f)), 2f)
                    if (0f in xMin..xMax) drawLine(context.palette.foreground.copy(alpha = 0.72f), Offset(xFor(0f), bottom), Offset(xFor(0f), top), 2f)
                    for (x in kotlin.math.ceil(xMin.toDouble()).toInt()..kotlin.math.floor(xMax.toDouble()).toInt()) drawLine(context.palette.grid.copy(alpha = 0.42f), Offset(xFor(x.toFloat()), top), Offset(xFor(x.toFloat()), bottom), 1f)
                    for (y in kotlin.math.ceil(yMin.toDouble()).toInt()..kotlin.math.floor(yMax.toDouble()).toInt()) drawLine(context.palette.grid.copy(alpha = 0.42f), Offset(left, yFor(y.toFloat())), Offset(right, yFor(y.toFloat())), 1f)
                    if (variant == CartesianVariant.POINT) {
                        val x = p.number("x").toFloat()
                        val y = p.number("y").toFloat()
                        drawCircle(context.palette.secondary, 7f, Offset(xFor(x), yFor(y)))
                    } else {
                        val path = Path()
                        var started = false
                        val samples = 240
                        for (index in 0..samples) {
                            val x = xMin + (xMax - xMin) * index / samples.toFloat()
                            val y = when (variant) {
                                CartesianVariant.LINEAR -> (p.number("slope") * x + p.number("intercept")).toFloat()
                                CartesianVariant.QUADRATIC -> (p.number("a") * x * x + p.number("b") * x + p.number("c")).toFloat()
                                CartesianVariant.INVERSE -> if (abs(x) < 0.001f) Float.NaN else (p.number("k") / x).toFloat()
                                CartesianVariant.POINT -> 0f
                            }
                            if (!y.isFinite() || y !in yMin..yMax) {
                                started = false
                            } else if (!started) {
                                path.moveTo(xFor(x), yFor(y))
                                started = true
                            } else path.lineTo(xFor(x), yFor(y))
                        }
                        drawPath(path, context.palette.primary, style = Stroke(4f, cap = StrokeCap.Round))
                    }
                },
                annotations = {
                    if (variant == CartesianVariant.POINT && t.text("pointLabel").isNotBlank()) {
                        VisualizationAnnotation(t.text("pointLabel"), 0.08f + xRatio(p.number("x").toFloat()) * 0.87f, 0.08f + yRatio(p.number("y").toFloat()) * 0.84f, context.palette.secondary, AnnotationPlacement.ABOVE, true)
                    }
                },
            )
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal enum class GeometryVariant { TRIANGLE, CIRCLE, ANGLE, PARALLEL, RIGHT_TRIANGLE, LINE_RAY_SEGMENT, PROJECTION, OBJECT_ABSTRACTION }

internal class GeometryRenderer(override val key: VisualizationKey, private val variant: GeometryVariant) : VisualizationRenderer() {
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
        val t = context.invocation.texts
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            ZoomableVisualizationSurface(
                Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    when (variant) {
                        GeometryVariant.TRIANGLE, GeometryVariant.RIGHT_TRIANGLE -> {
                            val a = Offset(size.width * 0.20f, size.height * 0.80f)
                            val b = Offset(size.width * 0.80f, size.height * 0.80f)
                            val c = if (variant == GeometryVariant.RIGHT_TRIANGLE) Offset(size.width * 0.20f, size.height * 0.20f) else Offset(size.width * 0.52f, size.height * 0.16f)
                            drawLine(context.palette.primary, a, b, 4f)
                            drawLine(context.palette.primary, a, c, 4f)
                            drawLine(context.palette.secondary, c, b, 4f)
                            if (variant == GeometryVariant.RIGHT_TRIANGLE) {
                                drawLine(context.palette.muted, Offset(a.x + 20f, a.y), Offset(a.x + 20f, a.y - 20f), 2f)
                                drawLine(context.palette.muted, Offset(a.x, a.y - 20f), Offset(a.x + 20f, a.y - 20f), 2f)
                            }
                        }
                        GeometryVariant.CIRCLE -> {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val r = minOf(size.width, size.height) * 0.32f
                            drawCircle(context.palette.primary, r, c, style = Stroke(4f))
                            drawLine(context.palette.secondary, c, Offset(c.x + r, c.y), 3f)
                        }
                        GeometryVariant.ANGLE -> {
                            val o = Offset(size.width * 0.35f, size.height * 0.72f)
                            val a = Offset(size.width * 0.82f, size.height * 0.72f)
                            val b = Offset(size.width * 0.66f, size.height * 0.18f)
                            drawLine(context.palette.primary, o, a, 5f, StrokeCap.Round)
                            drawLine(context.palette.secondary, o, b, 5f, StrokeCap.Round)
                            drawCircle(context.palette.foreground, 7f, o)
                        }
                        GeometryVariant.PARALLEL -> {
                            drawLine(context.palette.primary, Offset(size.width * 0.10f, size.height * 0.32f), Offset(size.width * 0.90f, size.height * 0.22f), 4f)
                            drawLine(context.palette.primary, Offset(size.width * 0.10f, size.height * 0.72f), Offset(size.width * 0.90f, size.height * 0.62f), 4f)
                            drawLine(context.palette.secondary, Offset(size.width * 0.35f, size.height * 0.08f), Offset(size.width * 0.62f, size.height * 0.92f), 4f)
                        }
                        GeometryVariant.LINE_RAY_SEGMENT -> {
                            val left = size.width * 0.18f
                            val right = size.width * 0.82f
                            listOf(0.22f, 0.50f, 0.78f).forEachIndexed { index, ratio ->
                                val y = size.height * ratio
                                drawLine(if (index == 1) context.palette.secondary else context.palette.primary, Offset(left, y), Offset(right, y), 4f, StrokeCap.Round)
                                if (index == 1) drawCircle(context.palette.secondary, 7f, Offset(left, y))
                                if (index == 2) {
                                    drawCircle(context.palette.foreground, 7f, Offset(left, y))
                                    drawCircle(context.palette.foreground, 7f, Offset(right, y))
                                }
                            }
                        }
                        GeometryVariant.PROJECTION -> {
                            val x = size.width * 0.10f
                            val y = size.height * 0.26f
                            val w = size.width * 0.28f
                            val h = size.height * 0.36f
                            val d = size.width * 0.08f
                            val points = listOf(Offset(x, y), Offset(x + w, y), Offset(x + w, y + h), Offset(x, y + h), Offset(x + d, y - d), Offset(x + w + d, y - d), Offset(x + w + d, y + h - d), Offset(x + d, y + h - d))
                            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7).forEach { (from, to) -> drawLine(context.palette.primary, points[from], points[to], 3f) }
                            val vx = size.width * 0.62f
                            drawRect(context.palette.secondary, Offset(vx, size.height * 0.18f), Size(size.width * 0.24f, size.height * 0.16f), style = Stroke(3f))
                            drawRect(context.palette.secondary, Offset(vx + size.width * 0.07f, size.height * 0.50f), Size(size.width * 0.10f, size.height * 0.16f), style = Stroke(3f))
                            drawRect(context.palette.secondary, Offset(vx, size.height * 0.80f), Size(size.width * 0.24f, size.height * 0.07f), style = Stroke(3f))
                        }
                        GeometryVariant.OBJECT_ABSTRACTION -> {
                            drawRect(context.palette.primary, Offset(size.width * 0.12f, size.height * 0.22f), Size(size.width * 0.22f, size.height * 0.22f), style = Stroke(4f))
                            drawRect(context.palette.secondary, Offset(size.width * 0.66f, size.height * 0.22f), Size(size.width * 0.22f, size.height * 0.22f), style = Stroke(4f))
                            drawArrow(context, Offset(size.width * 0.38f, size.height * 0.33f), Offset(size.width * 0.62f, size.height * 0.33f))
                            drawOval(context.palette.primary, Offset(size.width * 0.15f, size.height * 0.64f), Size(size.width * 0.16f, size.height * 0.08f), style = Stroke(4f))
                            drawOval(context.palette.secondary, Offset(size.width * 0.69f, size.height * 0.64f), Size(size.width * 0.16f, size.height * 0.08f), style = Stroke(4f))
                            drawArrow(context, Offset(size.width * 0.38f, size.height * 0.68f), Offset(size.width * 0.62f, size.height * 0.68f))
                        }
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
                    listOf("labelA", "labelB", "labelC", "labelD").forEachIndexed { index, key ->
                        val label = t.text(key)
                        if (label.isNotBlank() && index < anchors.size) VisualizationAnnotation(label, anchors[index].first, anchors[index].second, if (index % 2 == 0) context.palette.primary else context.palette.secondary, AnnotationPlacement.ABOVE, true)
                    }
                },
            )
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal enum class TransformVariant { TRANSLATION, SYMMETRY, ROTATION }

internal class TransformationRenderer(override val key: VisualizationKey, private val variant: TransformVariant) : VisualizationRenderer() {
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(
        parameters = listOf(
            VisualizationParameterSpec("dx", VisualizationParameterType.NUMBER, false),
            VisualizationParameterSpec("dy", VisualizationParameterType.NUMBER, false),
        ),
        texts = listOf(VisualizationTextSpec("title", false, true), VisualizationTextSpec("originalLabel", false, true), VisualizationTextSpec("resultLabel", false, true), VisualizationTextSpec("note", false, true)),
    )

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val p = context.invocation.parameters
        val t = context.invocation.texts
        val original = listOf(Offset(0.20f, 0.70f), Offset(0.34f, 0.28f), Offset(0.45f, 0.68f))
        val result = original.map { point ->
            when (variant) {
                TransformVariant.TRANSLATION -> Offset(point.x + p.number("dx", 0.36).toFloat(), point.y + p.number("dy", -0.08).toFloat())
                TransformVariant.SYMMETRY -> Offset(1f - point.x, point.y)
                TransformVariant.ROTATION -> Offset(0.5f + (0.5f - point.y), 0.5f + (point.x - 0.5f))
            }
        }
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            ZoomableVisualizationSurface(
                Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    fun px(point: Offset) = Offset(point.x * size.width, point.y * size.height)
                    drawPolygon(original.map(::px), context.palette.primary)
                    drawPolygon(result.map(::px), context.palette.secondary)
                    original.zip(result).forEach { (from, to) -> drawLine(context.palette.grid, px(from), px(to), 2f) }
                },
                annotations = {
                    if (t.text("originalLabel").isNotBlank()) VisualizationAnnotation(t.text("originalLabel"), original[0].x, original[0].y, context.palette.primary, AnnotationPlacement.BELOW)
                    if (t.text("resultLabel").isNotBlank()) VisualizationAnnotation(t.text("resultLabel"), result[0].x, result[0].y, context.palette.secondary, AnnotationPlacement.BELOW)
                },
            )
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

internal enum class ChartVariant { LINE, BAR }

internal class BasicChartRenderer(override val key: VisualizationKey, private val variant: ChartVariant) : VisualizationRenderer() {
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
        val t = context.invocation.texts
        val minValue = minOf(0f, values.min())
        val maxValue = maxOf(0f, values.max()).let { if (it == minValue) it + 1f else it }
        fun yRatio(value: Float) = 0.86f - (value - minValue) / (maxValue - minValue) * 0.70f
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            ZoomableVisualizationSurface(
                Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    val left = size.width * 0.08f
                    val right = size.width * 0.94f
                    val baselineY = size.height * yRatio(0f)
                    drawLine(context.palette.foreground.copy(alpha = 0.68f), Offset(left, baselineY), Offset(right, baselineY), 2f)
                    val slots = values.size.coerceAtLeast(1)
                    if (variant == ChartVariant.LINE) {
                        val path = Path()
                        values.forEachIndexed { index, value ->
                            val x = left + (right - left) * (index + 0.5f) / slots
                            val y = size.height * yRatio(value)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(context.palette.secondary, 6f, Offset(x, y))
                        }
                        drawPath(path, context.palette.primary, style = Stroke(4f, cap = StrokeCap.Round))
                    } else {
                        values.forEachIndexed { index, value ->
                            val slotWidth = (right - left) / slots
                            val x = left + slotWidth * index + slotWidth * 0.22f
                            val y = size.height * yRatio(value)
                            val top = minOf(y, baselineY)
                            drawRect(context.palette.primary.copy(alpha = 0.82f), Offset(x, top), Size(slotWidth * 0.56f, abs(baselineY - y)))
                        }
                    }
                },
                annotations = {
                    values.forEachIndexed { index, _ ->
                        val label = t.text("label$index")
                        if (label.isNotBlank()) VisualizationAnnotation(label, 0.08f + 0.86f * (index + 0.5f) / values.size, 0.91f, context.palette.muted, AnnotationPlacement.BELOW)
                    }
                },
            )
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
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
        val t = context.invocation.texts
        val nodes = listOf(Offset(0.12f, 0.50f), Offset(0.46f, 0.28f), Offset(0.46f, 0.72f), Offset(0.86f, 0.14f), Offset(0.86f, 0.38f), Offset(0.86f, 0.62f), Offset(0.86f, 0.86f))
        val edges = listOf(0 to 1, 0 to 2, 1 to 3, 1 to 4, 2 to 5, 2 to 6)
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.text("title").isNotBlank()) VisualTitle(t.text("title"))
            ZoomableVisualizationSurface(
                Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    fun px(point: Offset) = Offset(point.x * size.width, point.y * size.height)
                    edges.forEachIndexed { index, (from, to) -> drawLine(if (index % 2 == 0) context.palette.primary else context.palette.secondary, px(nodes[from]), px(nodes[to]), 3f) }
                    nodes.forEach { drawCircle(context.palette.foreground, 6f, px(it)) }
                },
                annotations = {
                    nodes.forEachIndexed { index, node -> if (t.text("label$index").isNotBlank()) VisualizationAnnotation(t.text("label$index"), node.x, node.y, context.palette.foreground, AnnotationPlacement.ABOVE) }
                },
            )
            if (t.text("note").isNotBlank()) VisualNote(t.text("note"))
        }
    }
}

@Composable
private fun VisualTitle(text: String) {
    Text(text, Modifier.fillMaxWidth(), Color(0xFFF5F5F7), 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
}

@Composable
private fun VisualNote(text: String) {
    Text(text, Modifier.fillMaxWidth(), Color(0xFF8E8E93), 12.sp, textAlign = TextAlign.Center)
}

private fun compactNumber(value: Double): String {
    val integer = value.toLong()
    return if (abs(value - integer) < 0.000001) integer.toString() else "%.3f".format(value).trimEnd('0').trimEnd('.')
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(context: VisualizationRenderContext, start: Offset, end: Offset) {
    drawLine(context.palette.secondary, start, end, 4f, StrokeCap.Round)
    val angle = atan2(end.y - start.y, end.x - start.x)
    val length = 13f
    drawLine(context.palette.secondary, end, Offset(end.x - length * cos((angle - PI.toFloat() / 6).toDouble()).toFloat(), end.y - length * sin((angle - PI.toFloat() / 6).toDouble()).toFloat()), 4f)
    drawLine(context.palette.secondary, end, Offset(end.x - length * cos((angle + PI.toFloat() / 6).toDouble()).toFloat(), end.y - length * sin((angle + PI.toFloat() / 6).toDouble()).toFloat()), 4f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolygon(points: List<Offset>, color: Color) {
    points.zip(points.drop(1) + points.first()).forEach { (from, to) -> drawLine(color, from, to, 4f) }
}
