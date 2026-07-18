package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.course.CourseParameterKind
import com.majortomman.school.learning.course.CourseParameterSpec
import com.majortomman.school.learning.course.CourseVisualizationKind
import com.majortomman.school.learning.science.expression.ComplexApprox
import com.majortomman.school.learning.science.math.MathFormulaStatus
import com.majortomman.school.learning.science.math.MathFormulaVerifier
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun MathCourseWorkbench(spec: InteractiveLessonSpec) {
    val visualization = spec.enrichment.visualization
    val verification = spec.enrichment.verification
    val parameterValues = remember(spec.title) {
        mutableStateMapOf<String, String>().apply {
            visualization?.parameters.orEmpty().forEach { put(it.id, it.defaultValue) }
        }
    }
    var formula by rememberSaveable(spec.title) {
        mutableStateOf(verification?.examples?.firstOrNull() ?: spec.formula)
    }
    var sampleMode by rememberSaveable(spec.title) { mutableStateOf(false) }
    var variableValues by remember(spec.title) { mutableStateOf<Map<String, String>>(emptyMap()) }
    val numericVariables = variableValues.mapValues { it.value.toDoubleOrNull() }.filterValues { it != null }.mapValues { it.value!! }
    val result = remember(formula, numericVariables, sampleMode) {
        MathFormulaVerifier.verify(formula, numericVariables, sampleRelation = sampleMode)
    }
    if (result.variables.toSet() != variableValues.keys) {
        variableValues = result.variables.associateWith { variableValues[it] ?: "1" }
    }

    SectionTitle(visualization?.title ?: "自定义参数与可视化", InteractiveBlue)
    Spacer(Modifier.height(12.dp))
    Text(
        visualization?.description ?: "调整参数，观察数学对象和结果同步变化。",
        color = InteractiveMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
    Spacer(Modifier.height(18.dp))
    visualization?.parameters.orEmpty().forEach { parameter ->
        MathParameterControl(parameter, parameterValues[parameter.id].orEmpty()) { parameterValues[parameter.id] = it }
        Spacer(Modifier.height(12.dp))
    }
    ParameterizedMathCanvas(
        kind = visualization?.kind ?: CourseVisualizationKind.PROCESS,
        values = parameterValues.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
    )

    Spacer(Modifier.height(34.dp))
    SectionTitle("自定义数学公式验证", InteractiveYellow)
    Spacer(Modifier.height(12.dp))
    Text(
        "支持分数、根号、π、e、i、整数幂、隐式乘法、等式和不等式。多组样本未发现反例不等于严格证明。",
        color = InteractiveMuted,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    )
    Spacer(Modifier.height(14.dp))
    BasicTextField(
        value = formula,
        onValueChange = { formula = it.take(256) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = InteractiveWhite, fontSize = 22.sp, lineHeight = 30.sp),
        cursorBrush = SolidColor(InteractiveYellow),
    )
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))

    if (result.variables.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        result.variables.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { name ->
                    Column(Modifier.weight(1f)) {
                        Text(name, color = InteractiveMuted, fontSize = 12.sp)
                        BasicTextField(
                            value = variableValues[name].orEmpty(),
                            onValueChange = { newValue ->
                                if (newValue.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                                    variableValues = variableValues + (name to newValue.take(16))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            textStyle = TextStyle(color = InteractiveWhite, fontSize = 18.sp),
                            cursorBrush = SolidColor(InteractiveBlue),
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("当前取值验证", color = if (!sampleMode) InteractiveBlue else InteractiveMuted)
            Text(
                "多组样本检查",
                modifier = Modifier.clickable { sampleMode = !sampleMode },
                color = if (sampleMode) InteractiveYellow else InteractiveMuted,
                fontWeight = if (sampleMode) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }

    Spacer(Modifier.height(18.dp))
    FormulaResult(result)
}

@Composable
private fun MathParameterControl(parameter: CourseParameterSpec, value: String, onValueChange: (String) -> Unit) {
    when (parameter.kind) {
        CourseParameterKind.NUMBER,
        CourseParameterKind.INTEGER,
        -> {
            val minimum = parameter.minimum ?: -10.0
            val maximum = parameter.maximum ?: 10.0
            val numeric = value.toDoubleOrNull()?.coerceIn(minimum, maximum) ?: minimum
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(parameter.label, color = InteractiveMuted, fontSize = 13.sp)
                Text("${formatMathNumber(numeric)}${parameter.unit}", color = InteractiveBlue, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = numeric.toFloat(),
                onValueChange = { changed ->
                    val step = parameter.step ?: 1.0
                    val snapped = kotlin.math.round(changed / step) * step
                    onValueChange(
                        if (parameter.kind == CourseParameterKind.INTEGER) snapped.toInt().toString()
                        else formatMathNumber(snapped),
                    )
                },
                valueRange = minimum.toFloat()..maximum.toFloat(),
            )
        }
        CourseParameterKind.BOOLEAN -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(parameter.label, color = InteractiveMuted)
            Text(
                if (value == "true") "开启" else "关闭",
                modifier = Modifier.clickable { onValueChange((value != "true").toString()) },
                color = InteractiveBlue,
            )
        }
        CourseParameterKind.CHOICE -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            parameter.choices.forEach { choice ->
                Text(
                    choice,
                    modifier = Modifier.clickable { onValueChange(choice) }.weight(1f),
                    color = if (choice == value) InteractiveBlue else InteractiveMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        CourseParameterKind.TEXT -> BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(80)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = InteractiveWhite, fontSize = 17.sp),
            cursorBrush = SolidColor(InteractiveBlue),
        )
    }
}

@Composable
private fun ParameterizedMathCanvas(kind: CourseVisualizationKind, values: Map<String, Double>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(300.dp).background(InteractivePanel.copy(alpha = 0.25f))) {
        when (kind) {
            CourseVisualizationKind.NUMBER_LINE -> drawNumberLine(values["x"] ?: 2.0)
            CourseVisualizationKind.GEOMETRY_2D -> drawGeometry(values["length"] ?: 4.0, values["angle"] ?: 60.0)
            CourseVisualizationKind.VECTOR -> drawVectors(values)
            CourseVisualizationKind.CARTESIAN_GRAPH -> drawFunction(values)
            CourseVisualizationKind.DATA_TABLE -> drawDataBars(values)
            else -> drawProcess(values)
        }
    }
}

private fun DrawScope.drawAxes() {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawLine(InteractiveWhite.copy(alpha = 0.6f), Offset(18.dp.toPx(), center.y), Offset(size.width - 18.dp.toPx(), center.y), 2.dp.toPx())
    drawLine(InteractiveWhite.copy(alpha = 0.6f), Offset(center.x, 18.dp.toPx()), Offset(center.x, size.height - 18.dp.toPx()), 2.dp.toPx())
    repeat(9) { index ->
        val x = 18.dp.toPx() + index * (size.width - 36.dp.toPx()) / 8f
        val y = 18.dp.toPx() + index * (size.height - 36.dp.toPx()) / 8f
        drawLine(InteractiveLine, Offset(x, 18.dp.toPx()), Offset(x, size.height - 18.dp.toPx()), 1.dp.toPx())
        drawLine(InteractiveLine, Offset(18.dp.toPx(), y), Offset(size.width - 18.dp.toPx(), y), 1.dp.toPx())
    }
}

private fun DrawScope.drawFunction(values: Map<String, Double>) {
    drawAxes()
    val a = values["a"] ?: 0.0
    val b = values["b"] ?: values["k"] ?: 1.0
    val c = values["c"] ?: values["b"] ?: 0.0
    val left = 18.dp.toPx()
    val right = size.width - 18.dp.toPx()
    val top = 18.dp.toPx()
    val bottom = size.height - 18.dp.toPx()
    fun sx(x: Double) = left + ((x + 5.0) / 10.0).toFloat() * (right - left)
    fun sy(y: Double) = bottom - ((y + 10.0) / 20.0).toFloat() * (bottom - top)
    var previous: Offset? = null
    for (index in 0..160) {
        val x = -5.0 + index * 10.0 / 160.0
        val y = if (values.containsKey("a")) a * x * x + b * x + c else b * x + c
        val point = Offset(sx(x), sy(y))
        if (point.y in top..bottom) previous?.let { drawLine(InteractiveBlue, it, point, 3.dp.toPx(), StrokeCap.Round) }
        previous = if (point.y in top..bottom) point else null
    }
}

private fun DrawScope.drawNumberLine(value: Double) {
    val centerY = size.height * 0.56f
    val left = 24.dp.toPx()
    val right = size.width - 24.dp.toPx()
    drawLine(InteractiveWhite.copy(alpha = 0.7f), Offset(left, centerY), Offset(right, centerY), 3.dp.toPx(), StrokeCap.Round)
    for (number in -10..10) {
        val x = left + (number + 10) / 20f * (right - left)
        drawLine(InteractiveLine.copy(alpha = 0.9f), Offset(x, centerY - 8.dp.toPx()), Offset(x, centerY + 8.dp.toPx()), 1.dp.toPx())
    }
    val x = left + ((value.coerceIn(-10.0, 10.0) + 10.0) / 20.0).toFloat() * (right - left)
    drawCircle(InteractiveYellow, 9.dp.toPx(), Offset(x, centerY))
    val paint = Paint().apply { color = InteractiveYellow.toArgb(); textSize = 18.sp.toPx(); textAlign = Paint.Align.CENTER }
    drawContext.canvas.nativeCanvas.drawText(formatMathNumber(value), x, centerY - 24.dp.toPx(), paint)
}

private fun DrawScope.drawGeometry(length: Double, angleDegrees: Double) {
    val origin = Offset(size.width * 0.22f, size.height * 0.74f)
    val scale = minOf(size.width, size.height) / 12f
    val base = Offset(origin.x + length.toFloat() * scale, origin.y)
    val radians = Math.toRadians(angleDegrees)
    val end = Offset(origin.x + (length * cos(radians)).toFloat() * scale, origin.y - (length * sin(radians)).toFloat() * scale)
    drawLine(InteractiveBlue, origin, base, 4.dp.toPx(), StrokeCap.Round)
    drawLine(InteractiveYellow, origin, end, 4.dp.toPx(), StrokeCap.Round)
    drawLine(InteractivePurple, base, end, 3.dp.toPx(), StrokeCap.Round)
    drawCircle(InteractiveWhite, 5.dp.toPx(), origin)
    drawCircle(InteractiveWhite, 5.dp.toPx(), base)
    drawCircle(InteractiveWhite, 5.dp.toPx(), end)
}

private fun DrawScope.drawVectors(values: Map<String, Double>) {
    drawAxes()
    val center = Offset(size.width / 2f, size.height / 2f)
    val scale = minOf(size.width, size.height) / 12f
    val a = Offset(center.x + (values["ax"] ?: 3.0).toFloat() * scale, center.y - (values["ay"] ?: 2.0).toFloat() * scale)
    val b = Offset(center.x + (values["bx"] ?: 1.0).toFloat() * scale, center.y - (values["by"] ?: 4.0).toFloat() * scale)
    drawLine(InteractiveBlue, center, a, 5.dp.toPx(), StrokeCap.Round)
    drawLine(InteractiveYellow, center, b, 5.dp.toPx(), StrokeCap.Round)
    drawCircle(InteractiveBlue, 6.dp.toPx(), a)
    drawCircle(InteractiveYellow, 6.dp.toPx(), b)
}

private fun DrawScope.drawDataBars(values: Map<String, Double>) {
    val entries = values.entries.take(6).ifEmpty { listOf("value" to 5.0) }
    val maximum = entries.maxOf { abs(it.value) }.coerceAtLeast(1.0)
    entries.forEachIndexed { index, entry ->
        val width = size.width / entries.size
        val height = (abs(entry.value) / maximum).toFloat() * size.height * 0.65f
        drawRect(
            color = if (index % 2 == 0) InteractiveBlue else InteractiveYellow,
            topLeft = Offset(index * width + width * 0.18f, size.height * 0.82f - height),
            size = androidx.compose.ui.geometry.Size(width * 0.64f, height),
        )
    }
}

private fun DrawScope.drawProcess(values: Map<String, Double>) {
    val count = maxOf(3, values.size + 2)
    repeat(count) { index ->
        val x = size.width * (index + 1f) / (count + 1f)
        val y = size.height * (0.38f + 0.18f * sin(index.toDouble()).toFloat())
        if (index > 0) {
            val previousX = size.width * index / (count + 1f)
            val previousY = size.height * (0.38f + 0.18f * sin(index - 1.0).toFloat())
            drawLine(InteractiveLine.copy(alpha = 0.9f), Offset(previousX, previousY), Offset(x, y), 3.dp.toPx())
        }
        drawCircle(if (index == count - 1) InteractiveYellow else InteractiveBlue, 10.dp.toPx(), Offset(x, y))
    }
}

@Composable
private fun FormulaResult(result: com.majortomman.school.learning.science.math.MathFormulaVerificationResult) {
    val color = when (result.status) {
        MathFormulaStatus.TRUE_AT_VALUES,
        MathFormulaStatus.SAMPLE_MATCH,
        MathFormulaStatus.VALID_EXPRESSION,
        -> InteractiveGreen
        MathFormulaStatus.FALSE_AT_VALUES,
        MathFormulaStatus.SAMPLE_COUNTEREXAMPLE,
        -> InteractiveRed
        else -> InteractiveYellow
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
        result.normalizedLeft?.let { left ->
            Text(
                result.normalizedRight?.let { right -> "$left ${result.relation?.symbol.orEmpty()} $right" } ?: left,
                color = InteractiveWhite,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            )
        }
        if (result.leftValue != null) {
            Text(
                buildString {
                    append("左侧：").append(formatComplex(result.leftValue))
                    result.rightValue?.let { append("    右侧：").append(formatComplex(it)) }
                },
                color = InteractiveMuted,
                fontSize = 13.sp,
            )
        }
        Text(result.message, color = color, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

private fun formatComplex(value: ComplexApprox): String = when {
    abs(value.imaginary) < 1e-9 -> formatMathNumber(value.real)
    abs(value.real) < 1e-9 -> "${formatMathNumber(value.imaginary)}i"
    value.imaginary > 0 -> "${formatMathNumber(value.real)}+${formatMathNumber(value.imaginary)}i"
    else -> "${formatMathNumber(value.real)}${formatMathNumber(value.imaginary)}i"
}

private fun formatMathNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
    return if (abs(rounded - rounded.toLong()) < 1e-9) rounded.toLong().toString()
    else rounded.toString().trimEnd('0').trimEnd('.')
}
