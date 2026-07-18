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
import com.majortomman.school.learning.course.PhysicsCourseContentFactory
import com.majortomman.school.learning.science.physics.PhysicsRelationId
import com.majortomman.school.learning.science.physics.PhysicsRelationVerifier
import com.majortomman.school.learning.science.physics.PhysicsVariableValue
import com.majortomman.school.learning.science.physics.PhysicsVerificationStatus
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun PhysicsCourseWorkbench(spec: InteractiveLessonSpec) {
    val category = remember(spec.title) { PhysicsCourseContentFactory.classify(spec.title) }
    val visualization = spec.enrichment.visualization
    val parameterValues = remember(spec.title) {
        mutableStateMapOf<String, String>().apply {
            visualization?.parameters.orEmpty().forEach { put(it.id, it.defaultValue) }
        }
    }
    val relations = remember(category) { PhysicsRelationVerifier.allowedRelations(category) }
    var selectedRelationName by rememberSaveable(spec.title) {
        mutableStateOf(relations.firstOrNull()?.name.orEmpty())
    }
    val relation = relations.firstOrNull { it.name == selectedRelationName } ?: relations.firstOrNull()
    var conditionAccepted by rememberSaveable(spec.title) { mutableStateOf(true) }
    var relationValues by remember(spec.title, relation?.name) {
        mutableStateOf(relation?.variables.orEmpty().associateWith { defaultPhysicsValue(it) })
    }
    var submitted by remember(spec.title, relation?.name) {
        mutableStateOf(defaultPhysicsTarget(relation?.target.orEmpty()))
    }

    SectionTitle(visualization?.title ?: "物理模型可视化", InteractiveBlue)
    Spacer(Modifier.height(12.dp))
    Text(
        visualization?.description ?: "调节教材模型中的物理量，观察状态、图像和结论。",
        color = InteractiveMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
    Spacer(Modifier.height(18.dp))
    visualization?.parameters.orEmpty().forEach { parameter ->
        PhysicsParameterControl(parameter, parameterValues[parameter.id].orEmpty()) { parameterValues[parameter.id] = it }
        Spacer(Modifier.height(10.dp))
    }
    PhysicsVisualization(
        kind = visualization?.kind ?: CourseVisualizationKind.PROCESS,
        values = parameterValues.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
    )

    Spacer(Modifier.height(34.dp))
    SectionTitle("教材物理关系验证", InteractiveYellow)
    Spacer(Modifier.height(12.dp))
    if (relations.isEmpty()) {
        Text(
            "当前主题以方向、现象或实验推理为主，没有配置可直接代数验证的公式。请使用可视化和模型条件检查。",
            color = InteractiveMuted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
        return
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        relations.take(4).forEach { option ->
            Text(
                option.display,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        selectedRelationName = option.name
                        relationValues = option.variables.associateWith { defaultPhysicsValue(it) }
                        submitted = defaultPhysicsTarget(option.target)
                    }
                    .padding(vertical = 10.dp),
                color = if (option == relation) InteractiveYellow else InteractiveMuted,
                fontSize = 12.sp,
                fontWeight = if (option == relation) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
    if (relations.size > 4) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            relations.drop(4).take(4).forEach { option ->
                Text(
                    option.display,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedRelationName = option.name
                            relationValues = option.variables.associateWith { defaultPhysicsValue(it) }
                            submitted = defaultPhysicsTarget(option.target)
                        }
                        .padding(vertical = 10.dp),
                    color = if (option == relation) InteractiveYellow else InteractiveMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
            repeat((4 - relations.drop(4).take(4).size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
    }

    relation?.let { selected ->
        Spacer(Modifier.height(14.dp))
        Text("适用条件：${selected.condition}", color = InteractiveMuted, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            if (conditionAccepted) "已确认模型条件" else "模型条件未确认",
            modifier = Modifier.clickable { conditionAccepted = !conditionAccepted }.padding(vertical = 8.dp),
            color = if (conditionAccepted) InteractiveGreen else InteractiveRed,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        selected.variables.chunked(2).forEach { symbols ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                symbols.forEach { symbol ->
                    PhysicsValueInput(
                        symbol = symbol,
                        current = relationValues.getValue(symbol),
                        modifier = Modifier.weight(1f),
                    ) { updated -> relationValues = relationValues + (symbol to updated) }
                }
                if (symbols.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        PhysicsValueInput(
            symbol = selected.target,
            current = submitted,
            modifier = Modifier.fillMaxWidth(),
            labelPrefix = "你的结果",
        ) { submitted = it }

        val values = relationValues.mapValues { (symbol, value) ->
            PhysicsVariableValue(symbol, value.value.toDoubleOrNull() ?: Double.NaN, value.unit)
        }
        val target = PhysicsVariableValue(
            selected.target,
            submitted.value.toDoubleOrNull() ?: Double.NaN,
            submitted.unit,
        )
        val result = remember(selected, values, target, conditionAccepted) {
            PhysicsRelationVerifier.verify(category, selected, values, target, conditionAccepted)
        }
        Spacer(Modifier.height(18.dp))
        PhysicsVerificationResultBlock(result)
    }
}

private data class EditablePhysicsValue(val value: String, val unit: String)

@Composable
private fun PhysicsValueInput(
    symbol: String,
    current: EditablePhysicsValue,
    modifier: Modifier,
    labelPrefix: String = "已知量",
    onChange: (EditablePhysicsValue) -> Unit,
) {
    Column(modifier) {
        Text("$labelPrefix $symbol", color = InteractiveMuted, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = current.value,
                onValueChange = { text ->
                    if (text.matches(Regex("-?\\d*(\\.\\d*)?"))) onChange(current.copy(value = text.take(18)))
                },
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                textStyle = TextStyle(color = InteractiveWhite, fontSize = 18.sp),
                cursorBrush = SolidColor(InteractiveBlue),
            )
            BasicTextField(
                value = current.unit,
                onValueChange = { onChange(current.copy(unit = it.take(12))) },
                modifier = Modifier.weight(0.55f).padding(vertical = 8.dp),
                textStyle = TextStyle(color = InteractiveYellow, fontSize = 14.sp),
                cursorBrush = SolidColor(InteractiveYellow),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

@Composable
private fun PhysicsParameterControl(parameter: CourseParameterSpec, value: String, onChange: (String) -> Unit) {
    if (parameter.kind == CourseParameterKind.NUMBER || parameter.kind == CourseParameterKind.INTEGER) {
        val min = parameter.minimum ?: -10.0
        val max = parameter.maximum ?: 10.0
        val numeric = value.toDoubleOrNull()?.coerceIn(min, max) ?: min
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(parameter.label, color = InteractiveMuted, fontSize = 13.sp)
            Text("${formatPhysics(numeric)} ${parameter.unit}", color = InteractiveBlue, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = numeric.toFloat(),
            onValueChange = { changed ->
                val step = parameter.step ?: 1.0
                val snapped = kotlin.math.round(changed / step) * step
                onChange(if (parameter.kind == CourseParameterKind.INTEGER) snapped.toInt().toString() else formatPhysics(snapped))
            },
            valueRange = min.toFloat()..max.toFloat(),
        )
    }
}

@Composable
private fun PhysicsVisualization(kind: CourseVisualizationKind, values: Map<String, Double>) {
    Canvas(Modifier.fillMaxWidth().height(310.dp).background(InteractivePanel.copy(alpha = 0.25f))) {
        when (kind) {
            CourseVisualizationKind.MOTION -> drawMotion(values)
            CourseVisualizationKind.FORCE_DIAGRAM,
            CourseVisualizationKind.VECTOR,
            -> drawForce(values)
            CourseVisualizationKind.WAVE -> drawWave(values)
            CourseVisualizationKind.CIRCUIT -> drawCircuit(values)
            CourseVisualizationKind.PARTICLE_MODEL -> drawParticles(values)
            CourseVisualizationKind.DATA_TABLE -> drawMeters(values)
            else -> drawPhysicsProcess(values)
        }
    }
}

private fun DrawScope.drawMotion(values: Map<String, Double>) {
    val v0 = values["v0"] ?: values["v"] ?: 0.0
    val a = values["a"] ?: 0.0
    val t = values["t"] ?: 1.0
    val x = (v0 * t + 0.5 * a * t * t).coerceIn(-25.0, 25.0)
    val baseline = size.height * 0.72f
    drawLine(InteractiveWhite.copy(alpha = 0.6f), Offset(20.dp.toPx(), baseline), Offset(size.width - 20.dp.toPx(), baseline), 2.dp.toPx())
    val position = size.width / 2f + (x / 25.0).toFloat() * size.width * 0.42f
    drawCircle(InteractiveBlue, 18.dp.toPx(), Offset(position, baseline - 18.dp.toPx()))
    val velocity = v0 + a * t
    val end = Offset(position + velocity.toFloat().coerceIn(-15f, 15f) * 7.dp.toPx(), baseline - 45.dp.toPx())
    drawLine(InteractiveYellow, Offset(position, baseline - 45.dp.toPx()), end, 4.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.drawForce(values: Map<String, Double>) {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawRect(InteractivePanel, Offset(center.x - 40.dp.toPx(), center.y - 30.dp.toPx()), androidx.compose.ui.geometry.Size(80.dp.toPx(), 60.dp.toPx()))
    val force = (values["F"] ?: values["field"] ?: 6.0).toFloat().coerceIn(-20f, 20f)
    drawLine(InteractiveRed, center, Offset(center.x + force * 8.dp.toPx(), center.y), 6.dp.toPx(), StrokeCap.Round)
    drawLine(InteractiveGreen, center, Offset(center.x, center.y - 80.dp.toPx()), 5.dp.toPx(), StrokeCap.Round)
    drawLine(InteractiveYellow, center, Offset(center.x, center.y + 80.dp.toPx()), 5.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.drawWave(values: Map<String, Double>) {
    val amplitude = (values["amplitude"] ?: 1.0).toFloat() * 32.dp.toPx()
    val frequency = (values["f"] ?: 2.0).toFloat().coerceIn(0.1f, 10f)
    val mid = size.height / 2f
    var previous = Offset(0f, mid)
    for (index in 1..180) {
        val x = size.width * index / 180f
        val y = mid - amplitude * sin((index / 180f * frequency * 2f * Math.PI).toFloat())
        val point = Offset(x, y)
        drawLine(InteractiveBlue, previous, point, 3.dp.toPx(), StrokeCap.Round)
        previous = point
    }
}

private fun DrawScope.drawCircuit(values: Map<String, Double>) {
    val voltage = values["U"] ?: 6.0
    val resistance = values["R"] ?: 3.0
    val current = if (resistance > 0.0) voltage / resistance else 0.0
    val left = size.width * 0.18f
    val right = size.width * 0.82f
    val top = size.height * 0.27f
    val bottom = size.height * 0.73f
    drawLine(InteractiveWhite, Offset(left, top), Offset(right, top), 4.dp.toPx())
    drawLine(InteractiveWhite, Offset(right, top), Offset(right, bottom), 4.dp.toPx())
    drawLine(InteractiveWhite, Offset(right, bottom), Offset(left, bottom), 4.dp.toPx())
    drawLine(InteractiveWhite, Offset(left, bottom), Offset(left, top), 4.dp.toPx())
    drawLine(InteractiveYellow, Offset(left - 8.dp.toPx(), size.height / 2f), Offset(left + 8.dp.toPx(), size.height / 2f), 8.dp.toPx())
    drawRect(InteractiveBlue, Offset(right - 13.dp.toPx(), size.height / 2f - 30.dp.toPx()), androidx.compose.ui.geometry.Size(26.dp.toPx(), 60.dp.toPx()))
    val paint = Paint().apply { color = InteractiveGreen.toArgb(); textSize = 16.sp.toPx(); textAlign = Paint.Align.CENTER }
    drawContext.canvas.nativeCanvas.drawText("I=${formatPhysics(current)} A", size.width / 2f, size.height * 0.16f, paint)
}

private fun DrawScope.drawParticles(values: Map<String, Double>) {
    val temperature = abs(values["dT"] ?: values["value"] ?: 10.0).coerceAtMost(100.0)
    repeat(32) { index ->
        val column = index % 8
        val row = index / 8
        val jitter = (temperature / 100.0 * 10.dp.toPx()).toFloat()
        val x = size.width * (column + 1f) / 9f + sin(index * 1.7).toFloat() * jitter
        val y = size.height * (row + 1f) / 5f + sin(index * 2.3).toFloat() * jitter
        drawCircle(if (index % 2 == 0) InteractiveRed else InteractiveYellow, 7.dp.toPx(), Offset(x, y))
    }
}

private fun DrawScope.drawMeters(values: Map<String, Double>) {
    val entries = values.entries.take(5)
    entries.forEachIndexed { index, entry ->
        val y = size.height * (index + 1f) / (entries.size + 1f)
        val length = (abs(entry.value) / (entries.maxOfOrNull { abs(it.value) } ?: 1.0).coerceAtLeast(1.0)).toFloat() * size.width * 0.65f
        drawLine(InteractiveLine, Offset(size.width * 0.15f, y), Offset(size.width * 0.85f, y), 10.dp.toPx(), StrokeCap.Round)
        drawLine(InteractiveBlue, Offset(size.width * 0.15f, y), Offset(size.width * 0.15f + length, y), 10.dp.toPx(), StrokeCap.Round)
    }
}

private fun DrawScope.drawPhysicsProcess(values: Map<String, Double>) {
    val count = (values.size + 3).coerceIn(3, 8)
    repeat(count) { index ->
        val x = size.width * (index + 1f) / (count + 1f)
        val y = size.height * (0.5f + 0.14f * sin(index.toDouble()).toFloat())
        if (index > 0) {
            val previousX = size.width * index / (count + 1f)
            val previousY = size.height * (0.5f + 0.14f * sin(index - 1.0).toFloat())
            drawLine(InteractiveLine, Offset(previousX, previousY), Offset(x, y), 3.dp.toPx())
        }
        drawCircle(if (index == count - 1) InteractiveYellow else InteractiveBlue, 11.dp.toPx(), Offset(x, y))
    }
}

@Composable
private fun PhysicsVerificationResultBlock(result: com.majortomman.school.learning.science.physics.PhysicsVerificationResult) {
    val color = when (result.status) {
        PhysicsVerificationStatus.CORRECT -> InteractiveGreen
        PhysicsVerificationStatus.INCORRECT,
        PhysicsVerificationStatus.INVALID_MODEL,
        PhysicsVerificationStatus.NOT_ALLOWED,
        -> InteractiveRed
        PhysicsVerificationStatus.MISSING_VALUES -> InteractiveYellow
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
        result.steps.forEach { Text(it, color = InteractiveMuted, fontSize = 13.sp, lineHeight = 20.sp) }
        Text(result.message, color = color, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

private fun defaultPhysicsValue(symbol: String): EditablePhysicsValue = when (symbol) {
    "s", "h", "u", "v" -> EditablePhysicsValue("2", "m")
    "t", "T" -> EditablePhysicsValue("2", "s")
    "v0" -> EditablePhysicsValue("0", "m/s")
    "a" -> EditablePhysicsValue("2", "m/s²")
    "m" -> EditablePhysicsValue("2", "kg")
    "V" -> EditablePhysicsValue("1", "m³")
    "F" -> EditablePhysicsValue("6", "N")
    "S" -> EditablePhysicsValue("0.5", "m²")
    "W", "Q", "Ek", "Ep" -> EditablePhysicsValue("12", "J")
    "P" -> EditablePhysicsValue("6", "W")
    "g" -> EditablePhysicsValue("9.8", "m/s²")
    "c" -> EditablePhysicsValue("4200", "J/(kg·°C)")
    "dT" -> EditablePhysicsValue("10", "°C")
    "f" -> EditablePhysicsValue("2", "Hz")
    "lambda" -> EditablePhysicsValue("3", "m")
    "U" -> EditablePhysicsValue("6", "V")
    "I" -> EditablePhysicsValue("2", "A")
    "R" -> EditablePhysicsValue("3", "Ω")
    "rho" -> EditablePhysicsValue("2", "kg/m³")
    "p" -> EditablePhysicsValue("12", "Pa")
    else -> EditablePhysicsValue("1", "")
}

private fun defaultPhysicsTarget(symbol: String): EditablePhysicsValue = defaultPhysicsValue(symbol)

private fun formatPhysics(value: Double): String {
    val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
    return if (abs(rounded - rounded.toLong()) < 1e-9) rounded.toLong().toString() else rounded.toString().trimEnd('0').trimEnd('.')
}
