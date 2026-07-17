package com.majortomman.school.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.science.expression.ScienceExpressionEvaluator
import com.majortomman.school.learning.science.expression.ScienceExpressionParser
import com.majortomman.school.learning.science.expression.ScienceExpressionRenderer
import com.majortomman.school.learning.science.expression.ScienceExpressionSimplifier
import com.majortomman.school.learning.science.quantity.QuantityParser
import com.majortomman.school.learning.science.quantity.UnitCatalog

private enum class ScienceCoreSection(val label: String) {
    EXPRESSION("精确表达式"),
    QUANTITY("单位与量纲"),
}

@Composable
internal fun ScienceCoreLabSample() {
    var selectedName by rememberSaveable { mutableStateOf(ScienceCoreSection.EXPRESSION.name) }
    val selected = ScienceCoreSection.valueOf(selectedName)

    SectionTitle("科学计算内核", InteractiveBlue)
    Spacer(Modifier.height(12.dp))
    Text(
        "这里验证数学、物理和化学共用的确定性基础。精确值不会过早转换为 Double，单位计算会先检查量纲。",
        color = InteractiveMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
    Spacer(Modifier.height(18.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        ScienceCoreSection.entries.forEach { item ->
            Column(
                modifier = Modifier
                    .clickable { selectedName = item.name }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    item.label,
                    color = if (selected == item) InteractiveBlue else InteractiveMuted,
                    fontSize = 14.sp,
                    fontWeight = if (selected == item) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (selected == item) 2.dp else 1.dp)
                        .background(if (selected == item) InteractiveBlue else InteractiveLine),
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))

    when (selected) {
        ScienceCoreSection.EXPRESSION -> ExactExpressionSample()
        ScienceCoreSection.QUANTITY -> QuantitySample()
    }
}

@Composable
private fun ExactExpressionSample() {
    var input by rememberSaveable { mutableStateOf("√8 + 3π + 2π") }
    val result = runCatching {
        val parsed = ScienceExpressionParser.parse(input)
        val simplified = ScienceExpressionSimplifier.simplify(parsed)
        Triple(
            ScienceExpressionRenderer.render(parsed),
            ScienceExpressionRenderer.render(simplified),
            ScienceExpressionEvaluator.evaluate(simplified),
        )
    }

    Text("根号、π、分数和隐式乘法", color = InteractivePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    CoreTextInput(input) { input = it.take(120) }
    Spacer(Modifier.height(18.dp))
    result.fold(
        onSuccess = { (parsed, simplified, approximate) ->
            CoreResultLine("解析", parsed)
            CoreResultLine("精确化简", simplified)
            CoreResultLine(
                "近似值",
                if (kotlin.math.abs(approximate.imaginary) < 1e-10) {
                    "%.6f".format(approximate.real)
                } else {
                    "%.6f %+.6fi".format(approximate.real, approximate.imaginary)
                },
            )
        },
        onFailure = { CoreError(it.message ?: "无法解析表达式。") },
    )
    Spacer(Modifier.height(14.dp))
    Text(
        "可试：1/3 + 1/6、√72、2π + 3π、2√(x+1)。含变量的式子保持符号形式。",
        color = InteractiveMuted,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun QuantitySample() {
    var input by rememberSaveable { mutableStateOf("72 km/h") }
    var targetSymbol by rememberSaveable { mutableStateOf("m/s") }
    val result = runCatching {
        val source = QuantityParser.parse(input)
        source to source.convertTo(UnitCatalog.bySymbol(targetSymbol))
    }

    Text("精确单位换算与量纲检查", color = InteractiveGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    CoreTextInput(input) { input = it.take(80) }
    Spacer(Modifier.height(14.dp))
    Text("目标单位", color = InteractiveMuted, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf("m/s", "km/h", "J", "kWh", "K", "°C").forEach { symbol ->
            Text(
                symbol,
                modifier = Modifier.clickable { targetSymbol = symbol }.padding(vertical = 8.dp),
                color = if (targetSymbol == symbol) InteractiveGreen else InteractiveMuted,
                fontSize = 14.sp,
                fontWeight = if (targetSymbol == symbol) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    result.fold(
        onSuccess = { (source, converted) ->
            CoreResultLine("原值", "${source.value} ${source.unit.symbol}")
            CoreResultLine("目标值", "${converted.value} ${converted.unit.symbol}")
            CoreResultLine("量纲", source.dimension.symbol())
        },
        onFailure = { CoreError(it.message ?: "无法换算单位。") },
    )
    Spacer(Modifier.height(14.dp))
    Text(
        "可试：1 kWh → J、20 °C → K、1000 g → kg。不同量纲之间会明确拒绝换算。",
        color = InteractiveMuted,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun CoreTextInput(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = InteractiveWhite, fontSize = 22.sp, lineHeight = 30.sp),
        cursorBrush = SolidColor(InteractiveBlue),
    )
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
}

@Composable
private fun CoreResultLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = InteractiveMuted, fontSize = 13.sp)
        Text(value, color = InteractiveWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CoreError(message: String) {
    Text(message, color = InteractiveRed, fontSize = 14.sp, lineHeight = 21.sp)
}
