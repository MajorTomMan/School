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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.course.BiologyCourseCategory
import com.majortomman.school.learning.course.BiologyCourseContentFactory
import com.majortomman.school.learning.course.CourseParameterKind
import com.majortomman.school.learning.course.CourseVisualizationKind
import com.majortomman.school.learning.science.biology.BiologyRelationId
import com.majortomman.school.learning.science.biology.BiologyRelationVerifier
import com.majortomman.school.learning.science.biology.BiologyVerificationStatus
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun BiologyCourseWorkbench(spec: InteractiveLessonSpec) {
    val category = remember(spec.title) { BiologyCourseContentFactory.classify(spec.title) }
    val visualization = spec.enrichment.visualization
    val parameterValues = remember(spec.title) {
        mutableStateMapOf<String, String>().apply {
            visualization?.parameters.orEmpty().forEach { put(it.id, it.defaultValue) }
        }
    }

    SectionTitle(visualization?.title ?: "生物结构与过程", InteractiveBlue)
    Spacer(Modifier.height(12.dp))
    Text(
        visualization?.description ?: "调节教材模型中的条件，观察结构、过程和数量关系。",
        color = InteractiveMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
    Spacer(Modifier.height(18.dp))
    visualization?.parameters.orEmpty().forEach { parameter ->
        when (parameter.kind) {
            CourseParameterKind.NUMBER,
            CourseParameterKind.INTEGER,
            -> {
                val min = parameter.minimum ?: 0.0
                val max = parameter.maximum ?: 100.0
                val numeric = parameterValues[parameter.id]?.toDoubleOrNull()?.coerceIn(min, max) ?: min
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(parameter.label, color = InteractiveMuted, fontSize = 13.sp)
                    Text("${formatBiology(numeric)} ${parameter.unit}", color = InteractiveBlue, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = numeric.toFloat(),
                    onValueChange = { changed ->
                        val step = parameter.step ?: 1.0
                        val snapped = kotlin.math.round(changed / step) * step
                        parameterValues[parameter.id] = if (parameter.kind == CourseParameterKind.INTEGER) snapped.toInt().toString() else formatBiology(snapped)
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                )
            }
            CourseParameterKind.CHOICE -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                parameter.choices.forEach { choice ->
                    Text(
                        choice,
                        modifier = Modifier.weight(1f).clickable { parameterValues[parameter.id] = choice }.padding(vertical = 10.dp),
                        color = if (parameterValues[parameter.id] == choice) InteractiveBlue else InteractiveMuted,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                    )
                }
            }
            else -> Unit
        }
        Spacer(Modifier.height(10.dp))
    }
    BiologyVisualization(
        category = category,
        kind = visualization?.kind ?: CourseVisualizationKind.BIOLOGICAL_PROCESS,
        values = parameterValues.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
    )

    Spacer(Modifier.height(34.dp))
    SectionTitle("生物数量关系验证", InteractiveYellow)
    Spacer(Modifier.height(12.dp))
    val relations = remember(category) { BiologyRelationVerifier.allowedRelations(category) }
    if (relations.isEmpty()) {
        Text(
            "当前主题主要验证结构、分类或过程顺序，没有配置可直接代数计算的关系。可视化仍会显示结构与条件变化。",
            color = InteractiveMuted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
        return
    }

    var selectedName by rememberSaveable(spec.title) { mutableStateOf(relations.first().name) }
    val relation = relations.firstOrNull { it.name == selectedName } ?: relations.first()
    var conditionsAccepted by rememberSaveable(spec.title) { mutableStateOf(true) }
    var values by remember(spec.title, relation.name) {
        mutableStateOf(relation.variables.associateWith(::defaultBiologyValue))
    }
    var submitted by remember(spec.title, relation.name) { mutableStateOf(defaultBiologyTarget(relation)) }
    var unit by remember(spec.title, relation.name) { mutableStateOf(defaultBiologyUnit(relation)) }

    relations.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { option ->
                Text(
                    option.display.substringBefore('='),
                    modifier = Modifier.weight(1f).clickable {
                        selectedName = option.name
                        values = option.variables.associateWith(::defaultBiologyValue)
                        submitted = defaultBiologyTarget(option)
                        unit = defaultBiologyUnit(option)
                    }.padding(vertical = 10.dp),
                    color = if (option == relation) InteractiveYellow else InteractiveMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("适用条件：${relation.condition}", color = InteractiveMuted, fontSize = 13.sp, lineHeight = 20.sp)
    Text(
        if (conditionsAccepted) "已确认统计口径和条件" else "条件尚未确认",
        modifier = Modifier.clickable { conditionsAccepted = !conditionsAccepted }.padding(vertical = 10.dp),
        color = if (conditionsAccepted) InteractiveGreen else InteractiveRed,
        fontWeight = FontWeight.Bold,
    )

    relation.variables.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            row.forEach { symbol ->
                BiologyNumberInput(symbol, values.getValue(symbol), Modifier.weight(1f)) { updated ->
                    values = values + (symbol to updated)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        BiologyNumberInput("你的结果 ${relation.target}", submitted, Modifier.weight(1f)) { submitted = it }
        Column(Modifier.weight(0.65f)) {
            Text("单位", color = InteractiveMuted, fontSize = 12.sp)
            BasicTextField(
                value = unit,
                onValueChange = { unit = it.take(18) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textStyle = TextStyle(color = InteractiveYellow, fontSize = 16.sp),
                cursorBrush = SolidColor(InteractiveYellow),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        }
    }

    val numericValues = values.mapValues { it.value.toDoubleOrNull() ?: Double.NaN }
    val numericSubmitted = submitted.toDoubleOrNull()
    val result = remember(relation, numericValues, numericSubmitted, unit, conditionsAccepted) {
        BiologyRelationVerifier.verify(category, relation, numericValues, numericSubmitted, unit, conditionsAccepted)
    }
    Spacer(Modifier.height(18.dp))
    BiologyResultBlock(result)
}

@Composable
private fun BiologyNumberInput(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    Column(modifier) {
        Text(label, color = InteractiveMuted, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = { text -> if (text.matches(Regex("-?\\d*(\\.\\d*)?"))) onChange(text.take(18)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textStyle = TextStyle(color = InteractiveWhite, fontSize = 18.sp),
            cursorBrush = SolidColor(InteractiveBlue),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

@Composable
private fun BiologyResultBlock(result: com.majortomman.school.learning.science.biology.BiologyVerificationResult) {
    val color = when (result.status) {
        BiologyVerificationStatus.CORRECT -> InteractiveGreen
        BiologyVerificationStatus.MISSING_VALUES -> InteractiveYellow
        else -> InteractiveRed
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
        result.steps.forEach { Text(it, color = InteractiveMuted, fontSize = 13.sp, lineHeight = 20.sp) }
        Text(result.message, color = color, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun BiologyVisualization(
    category: BiologyCourseCategory,
    kind: CourseVisualizationKind,
    values: Map<String, Double>,
) {
    Canvas(Modifier.fillMaxWidth().height(320.dp).background(InteractivePanel.copy(alpha = 0.25f))) {
        when (category) {
            BiologyCourseCategory.CELL -> drawBiologyCell(values)
            BiologyCourseCategory.GENETICS -> drawPunnett(values)
            BiologyCourseCategory.ECOLOGY -> drawEcology(values)
            BiologyCourseCategory.HUMAN -> drawHumanSystems(values)
            BiologyCourseCategory.CLASSIFICATION -> drawClassification(values)
            else -> if (kind == CourseVisualizationKind.DATA_TABLE) drawBiologyBars(values) else drawBiologyProcess(values)
        }
    }
}

private fun DrawScope.drawBiologyCell(values: Map<String, Double>) {
    val scale = ((values["magnification"] ?: 100.0) / 1000.0).toFloat().coerceIn(0.15f, 1f)
    val width = size.width * (0.55f + 0.25f * scale)
    val height = size.height * (0.55f + 0.20f * scale)
    val left = (size.width - width) / 2f
    val top = (size.height - height) / 2f
    drawRoundRect(InteractiveGreen.copy(alpha = 0.18f), Offset(left, top), Size(width, height), CornerRadius(28.dp.toPx()))
    drawRoundRect(InteractiveGreen, Offset(left, top), Size(width, height), CornerRadius(28.dp.toPx()), style = Stroke(7.dp.toPx()))
    drawRoundRect(InteractiveWhite.copy(alpha = 0.7f), Offset(left + 10.dp.toPx(), top + 10.dp.toPx()), Size(width - 20.dp.toPx(), height - 20.dp.toPx()), CornerRadius(22.dp.toPx()), style = Stroke(2.dp.toPx()))
    val nucleus = Offset(left + width * 0.35f, top + height * 0.45f)
    drawCircle(InteractiveYellow, minOf(width, height) * 0.11f, nucleus)
    drawCircle(InteractiveRed, minOf(width, height) * 0.04f, nucleus)
    repeat(5) { index ->
        drawOval(InteractiveGreen, Offset(left + width * (0.25f + index * 0.12f), top + height * 0.72f), Size(28.dp.toPx(), 13.dp.toPx()))
    }
}

private fun DrawScope.drawPunnett(values: Map<String, Double>) {
    val left = size.width * 0.22f
    val top = size.height * 0.18f
    val cell = minOf(size.width * 0.22f, size.height * 0.25f)
    repeat(3) { index ->
        drawLine(InteractiveWhite.copy(alpha = 0.7f), Offset(left, top + index * cell), Offset(left + 2 * cell, top + index * cell), 2.dp.toPx())
        drawLine(InteractiveWhite.copy(alpha = 0.7f), Offset(left + index * cell, top), Offset(left + index * cell, top + 2 * cell), 2.dp.toPx())
    }
    val labels = listOf("AA", "Aa", "Aa", "aa")
    val paint = Paint().apply { color = InteractiveYellow.toArgb(); textSize = 19.sp.toPx(); textAlign = Paint.Align.CENTER }
    labels.forEachIndexed { index, label ->
        val column = index % 2
        val row = index / 2
        drawContext.canvas.nativeCanvas.drawText(label, left + (column + 0.5f) * cell, top + (row + 0.58f) * cell, paint)
    }
}

private fun DrawScope.drawEcology(values: Map<String, Double>) {
    val levels = listOf(0.82f to InteractiveGreen, 0.62f to InteractiveYellow, 0.42f to InteractiveBlue, 0.24f to InteractiveRed)
    levels.forEachIndexed { index, (ratio, color) ->
        val width = size.width * ratio
        val y = size.height * (0.75f - index * 0.17f)
        drawRect(color.copy(alpha = 0.78f), Offset((size.width - width) / 2f, y), Size(width, 32.dp.toPx()))
    }
    val paint = Paint().apply { color = InteractiveMuted.toArgb(); textSize = 12.sp.toPx(); textAlign = Paint.Align.CENTER }
    drawContext.canvas.nativeCanvas.drawText("能量沿营养级递减", size.width / 2f, size.height * 0.92f, paint)
}

private fun DrawScope.drawHumanSystems(values: Map<String, Double>) {
    val center = Offset(size.width / 2f, size.height * 0.28f)
    drawCircle(InteractiveWhite.copy(alpha = 0.65f), 30.dp.toPx(), center, style = Stroke(3.dp.toPx()))
    drawLine(InteractiveWhite.copy(alpha = 0.65f), Offset(center.x, center.y + 30.dp.toPx()), Offset(center.x, size.height * 0.78f), 5.dp.toPx(), StrokeCap.Round)
    drawLine(InteractiveWhite.copy(alpha = 0.65f), Offset(center.x, size.height * 0.42f), Offset(center.x - 70.dp.toPx(), size.height * 0.58f), 4.dp.toPx())
    drawLine(InteractiveWhite.copy(alpha = 0.65f), Offset(center.x, size.height * 0.42f), Offset(center.x + 70.dp.toPx(), size.height * 0.58f), 4.dp.toPx())
    drawCircle(InteractiveRed, 17.dp.toPx(), Offset(center.x - 18.dp.toPx(), size.height * 0.48f))
    drawOval(InteractiveBlue.copy(alpha = 0.8f), Offset(center.x + 4.dp.toPx(), size.height * 0.39f), Size(42.dp.toPx(), 70.dp.toPx()))
    repeat(5) { index ->
        drawLine(InteractiveRed.copy(alpha = 0.55f), Offset(center.x - 18.dp.toPx(), size.height * 0.48f), Offset(size.width * (0.2f + index * 0.15f), size.height * 0.72f), 2.dp.toPx())
    }
}

private fun DrawScope.drawClassification(values: Map<String, Double>) {
    val root = Offset(size.width / 2f, size.height * 0.16f)
    drawCircle(InteractiveYellow, 12.dp.toPx(), root)
    repeat(3) { branch ->
        val mid = Offset(size.width * (0.22f + branch * 0.28f), size.height * 0.48f)
        drawLine(InteractiveLine, root, mid, 3.dp.toPx())
        drawCircle(InteractiveBlue, 10.dp.toPx(), mid)
        repeat(2) { leaf ->
            val end = Offset(mid.x + (leaf * 2 - 1) * 38.dp.toPx(), size.height * 0.78f)
            drawLine(InteractiveLine, mid, end, 2.dp.toPx())
            drawCircle(InteractiveGreen, 8.dp.toPx(), end)
        }
    }
}

private fun DrawScope.drawBiologyBars(values: Map<String, Double>) {
    val entries = values.entries.take(6).ifEmpty { mapOf("sample" to 1.0).entries.toList() }
    val max = entries.maxOf { abs(it.value) }.coerceAtLeast(1.0)
    entries.forEachIndexed { index, entry ->
        val width = size.width / entries.size
        val height = (abs(entry.value) / max).toFloat() * size.height * 0.68f
        drawRect(if (index % 2 == 0) InteractiveGreen else InteractiveBlue, Offset(index * width + width * 0.18f, size.height * 0.82f - height), Size(width * 0.64f, height))
    }
}

private fun DrawScope.drawBiologyProcess(values: Map<String, Double>) {
    val count = (values.size + 4).coerceIn(4, 9)
    repeat(count) { index ->
        val x = size.width * (index + 1f) / (count + 1f)
        val y = size.height * (0.5f + 0.14f * sin(index.toDouble()).toFloat())
        if (index > 0) {
            val px = size.width * index / (count + 1f)
            val py = size.height * (0.5f + 0.14f * sin(index - 1.0).toFloat())
            drawLine(InteractiveLine, Offset(px, py), Offset(x, y), 3.dp.toPx())
        }
        drawCircle(if (index == count - 1) InteractiveYellow else InteractiveGreen, 11.dp.toPx(), Offset(x, y))
    }
}

private fun defaultBiologyValue(symbol: String): String = when (symbol) {
    "image" -> "5"
    "actual" -> "0.05"
    "count" -> "120"
    "area" -> "20"
    "final" -> "120"
    "initial" -> "100"
    "next" -> "100"
    "previous" -> "1000"
    "heartRate" -> "75"
    "strokeVolume" -> "70"
    "gas", "product" -> "20"
    "time" -> "10"
    "favorable" -> "3"
    "total" -> "4"
    "survived" -> "80"
    else -> "1"
}

private fun defaultBiologyTarget(relation: BiologyRelationId): String = when (relation) {
    BiologyRelationId.MAGNIFICATION -> "100"
    BiologyRelationId.POPULATION_DENSITY -> "6"
    BiologyRelationId.GROWTH_RATE -> "20"
    BiologyRelationId.ENERGY_EFFICIENCY -> "10"
    BiologyRelationId.CARDIAC_OUTPUT -> "5250"
    BiologyRelationId.RESPIRATION_RATE,
    BiologyRelationId.PHOTOSYNTHESIS_RATE,
    -> "2"
    BiologyRelationId.GENETIC_PROBABILITY -> "0.75"
    BiologyRelationId.SURVIVAL_RATE -> "80"
}

private fun defaultBiologyUnit(relation: BiologyRelationId): String = when (relation) {
    BiologyRelationId.MAGNIFICATION -> "×"
    BiologyRelationId.POPULATION_DENSITY -> "个/m²"
    BiologyRelationId.GROWTH_RATE,
    BiologyRelationId.ENERGY_EFFICIENCY,
    BiologyRelationId.SURVIVAL_RATE,
    -> "%"
    BiologyRelationId.CARDIAC_OUTPUT -> "mL/min"
    BiologyRelationId.RESPIRATION_RATE,
    BiologyRelationId.PHOTOSYNTHESIS_RATE,
    -> "单位量/min"
    BiologyRelationId.GENETIC_PROBABILITY -> ""
}

private fun formatBiology(value: Double): String {
    val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
    return if (abs(rounded - rounded.toLong()) < 1e-9) rounded.toLong().toString() else rounded.toString().trimEnd('0').trimEnd('.')
}
