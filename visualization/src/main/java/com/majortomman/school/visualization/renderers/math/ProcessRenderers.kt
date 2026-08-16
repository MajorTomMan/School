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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

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
            if (title.isNotBlank()) ProcessTitle(title)
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
            if (note.isNotBlank()) ProcessNote(note)
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
        val texts = context.invocation.texts
        val cells = listOf(texts.text("positiveInteger"), texts.text("positiveFraction"), texts.text("zero"), "", texts.text("negativeInteger"), texts.text("negativeFraction"))
        val rows = listOf(texts.text("rowPositive"), texts.text("rowZero"), texts.text("rowNegative"))
        val columns = listOf(texts.text("columnInteger"), texts.text("columnFraction"))
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (texts.text("title").isNotBlank()) ProcessTitle(texts.text("title"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.width(54.dp))
                columns.forEach { column ->
                    Text(text = column, modifier = Modifier.weight(1f), color = context.palette.muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            rows.forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row,
                        modifier = Modifier.width(54.dp),
                        color = if (rowIndex == 2) context.palette.secondary else context.palette.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    repeat(2) { columnIndex ->
                        val index = rowIndex * 2 + columnIndex
                        val label = cells[index]
                        val enabled = label.isNotBlank()
                        val color = if (rowIndex == 2) context.palette.secondary else if (rowIndex == 1) context.palette.foreground else context.palette.primary
                        Box(
                            modifier = Modifier.weight(1f).height(54.dp)
                                .background(if (selected == index) color.copy(alpha = 0.16f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .border(1.dp, color.copy(alpha = if (selected == index) 0.86f else 0.30f), RoundedCornerShape(8.dp))
                                .clickable(enabled = enabled) { selected = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = if (enabled) label else "—", color = if (enabled) color else context.palette.muted.copy(alpha = 0.45f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (texts.text("note").isNotBlank()) ProcessNote(texts.text("note"))
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
        val texts = context.invocation.texts
        Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            if (texts.text("title").isNotBlank()) ProcessTitle(texts.text("title"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = texts.text("source$index"), color = context.palette.foreground, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        Text(text = "↓", color = context.palette.muted, fontSize = 18.sp)
                        Text(text = texts.text("target$index"), color = context.palette.secondary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (texts.text("note").isNotBlank()) ProcessNote(texts.text("note"))
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
        val texts = context.invocation.texts
        val middle = texts.text("middle")
        val entries = if (middle.isBlank()) listOf(texts.text("source"), texts.text("target")) else listOf(texts.text("source"), middle, texts.text("target"))
        Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
            if (texts.text("title").isNotBlank()) ProcessTitle(texts.text("title"))
            entries.forEachIndexed { index, value ->
                Text(text = value, color = if (index == entries.lastIndex) context.palette.secondary else context.palette.foreground, fontSize = 23.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                if (index != entries.lastIndex) {
                    val transition = if (index == 0) texts.text("firstTransition") else texts.text("secondTransition")
                    Text(text = if (transition.isBlank()) "↓" else "↓  $transition", color = context.palette.primary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            if (texts.text("note").isNotBlank()) ProcessNote(texts.text("note"))
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
        val texts = context.invocation.texts
        Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            if (texts.text("title").isNotBlank()) ProcessTitle(texts.text("title"))
            repeat(4) { index ->
                Text(
                    text = texts.text("rule$index"),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (index == 0 || index == 3) context.palette.primary else context.palette.secondary,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (texts.text("note").isNotBlank()) ProcessNote(texts.text("note"))
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
        val parameters = context.invocation.parameters
        var base by remember { mutableDoubleStateOf(parameters.number("base")) }
        var exponent by remember { mutableDoubleStateOf(parameters.number("exponent").coerceIn(1.0, 8.0)) }
        val exponentInt = exponent.toInt().coerceIn(1, 8)
        val result = base.pow(exponentInt)
        val factors = List(exponentInt) { processNumber(base) }.joinToString(" × ")
        Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            if (context.invocation.texts.text("title").isNotBlank()) ProcessTitle(context.invocation.texts.text("title"))
            Slider(value = base.toFloat(), onValueChange = { base = it.toDouble() }, valueRange = parameters.number("minBase", -4.0).toFloat()..parameters.number("maxBase", 4.0).toFloat())
            Slider(value = exponent.toFloat(), onValueChange = { exponent = it.toInt().coerceIn(1, 8).toDouble() }, valueRange = 1f..8f, steps = 6)
            Text(text = "${processNumber(base)}^$exponentInt", modifier = Modifier.fillMaxWidth(), color = context.palette.foreground, fontSize = 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Text(text = factors, modifier = Modifier.fillMaxWidth(), color = context.palette.primary, fontSize = 16.sp, textAlign = TextAlign.Center)
            Text(text = "= ${processNumber(result)}", modifier = Modifier.fillMaxWidth(), color = context.palette.secondary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (context.invocation.texts.text("note").isNotBlank()) ProcessNote(context.invocation.texts.text("note"))
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
        val texts = context.invocation.texts
        Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (texts.text("title").isNotBlank()) ProcessTitle(texts.text("title"))
            ZoomableVisualizationSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                geometry = {
                    val centerX = size.width / 2f
                    val beamY = size.height * 0.38f
                    val delta = size.height * 0.08f * tilt
                    val left = Offset(size.width * 0.18f, beamY + delta)
                    val right = Offset(size.width * 0.82f, beamY - delta)
                    drawLine(context.palette.foreground, left, right, 4f, StrokeCap.Round)
                    drawLine(context.palette.muted, Offset(centerX, beamY), Offset(centerX, size.height * 0.76f), 4f)
                    drawLine(context.palette.primary, Offset(size.width * 0.26f, left.y), Offset(size.width * 0.26f, left.y + 55f), 2f)
                    drawLine(context.palette.secondary, Offset(size.width * 0.74f, right.y), Offset(size.width * 0.74f, right.y + 55f), 2f)
                    val base = Path().apply {
                        moveTo(centerX, size.height * 0.67f)
                        lineTo(centerX - 45f, size.height * 0.84f)
                        lineTo(centerX + 45f, size.height * 0.84f)
                        close()
                    }
                    drawPath(base, context.palette.muted.copy(alpha = 0.65f), style = Stroke(3f))
                },
                annotations = {
                    VisualizationAnnotation(texts.text("left"), 0.26f, 0.58f, context.palette.primary, AnnotationPlacement.BELOW, true)
                    VisualizationAnnotation(texts.text("right"), 0.74f, 0.58f, context.palette.secondary, AnnotationPlacement.BELOW, true)
                },
            )
            if (texts.text("note").isNotBlank()) ProcessNote(texts.text("note"))
        }
    }
}

@Composable
private fun ProcessTitle(text: String) {
    Text(text = text, modifier = Modifier.fillMaxWidth(), color = Color(0xFFF5F5F7), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
}

@Composable
private fun ProcessNote(text: String) {
    Text(text = text, modifier = Modifier.fillMaxWidth(), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
}

private fun processNumber(value: Double): String {
    val integer = value.toLong()
    return if (abs(value - integer) < 0.000001) integer.toString() else "%.3f".format(value).trimEnd('0').trimEnd('.')
}
