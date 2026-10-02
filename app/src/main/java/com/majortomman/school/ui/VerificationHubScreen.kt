package com.majortomman.school.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.verification.core.VerificationResult
import com.majortomman.school.learning.verification.core.VerificationRequest
import com.majortomman.school.learning.verification.core.VerificationStatus
import com.majortomman.school.learning.verification.core.VerificationStep
import com.majortomman.school.learning.verification.math.MathVerificationEngine
import com.majortomman.school.visualization.SchoolVisualization

@Composable
internal fun VerificationHubScreen() {
    MathVerificationPage()
}

@Composable
private fun MathVerificationPage() {
    var input by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<VerificationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("数学", color = MaterialTheme.colorScheme.onBackground, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("初高中数学 · 本地符号推理", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("输入表达式、函数或一元方程。本地引擎会识别题型、给出答案和可复核的逐步解析；能可视化的结果会直接调用统一可视化基础设施。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))

        VerificationTextInput(
            label = "数学输入",
            value = input,
            onValueChange = {
                input = it
                result = null
            },
            hint = "例如：2x+3=9 或 x^2-5x+6=0",
            maxLength = 512,
        )
        Spacer(Modifier.height(14.dp))
        Text("当前覆盖数值表达式、代数式展开/合并、一元一次方程、一元二次方程和函数图像；明确不处理极限、导数、积分等高等数学。", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f), fontSize = 11.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(16.dp))

        listOf("2*(3+4)", "(x+2)(x+3)", "2x+3=9", "x^2-5x+6=0", "sin(x)").chunked(3).forEach { examples ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                examples.forEach { example ->
                    MathExample(example, Modifier.weight(1f)) {
                        input = example
                        result = null
                    }
                }
                repeat(3 - examples.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(14.dp))

        val enabled = input.isNotBlank()
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp).clickable(enabled = enabled) {
                result = MathVerificationEngine.verify(VerificationRequest(input))
            },
            contentAlignment = Alignment.Center,
        ) {
            Text("本地验证", color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (enabled) 2.dp else 1.dp).background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
        }
        Spacer(Modifier.height(28.dp))

        result?.let { MathVerificationResultView(it) } ?: run {
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
            Spacer(Modifier.height(12.dp))
            Text("答案、逐步解析和可视化会显示在这里", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Spacer(Modifier.height(52.dp))
    }
}

@Composable
private fun MathExample(text: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(text, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
    }
}

@Composable
private fun MathVerificationResultView(result: VerificationResult) {
    when (result.status) {
        VerificationStatus.SUCCESS -> {
            VerificationStatusBlock(
                title = "本地求解完成",
                message = "所有解析、计算和步骤生成均在本地完成。",
                color = MaterialTheme.colorScheme.tertiary,
                normalized = result.answer?.display,
                rows = listOf(
                    "题型" to result.problemType.label,
                    "标准化输入" to result.normalizedInput,
                    "步骤" to "${result.steps.size} 步",
                ),
            )
            if (result.steps.isNotEmpty()) {
                Spacer(Modifier.height(30.dp))
                Text("逐步解析", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                result.steps.forEachIndexed { index, step ->
                    MathVerificationStep(index + 1, step)
                    if (index != result.steps.lastIndex) Spacer(Modifier.height(18.dp))
                }
            }
            if (result.warnings.isNotEmpty()) {
                Spacer(Modifier.height(26.dp))
                result.warnings.forEach { warning ->
                    Text(warning.message, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            result.visualizations.forEach { visualization ->
                Spacer(Modifier.height(30.dp))
                Text("可视化", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(330.dp)) {
                    SchoolVisualization(visualization, Modifier.fillMaxSize())
                }
            }
        }
        VerificationStatus.UNSUPPORTED -> VerificationStatusBlock(
            title = "超出当前本地范围",
            message = result.warnings.joinToString("\n") { it.message }.ifBlank { "这个数学输入暂时不在当前初高中本地引擎的支持范围内。" },
            color = MaterialTheme.colorScheme.secondary,
            rows = listOf("识别结果" to result.problemType.label),
        )
        VerificationStatus.INVALID -> VerificationStatusBlock(
            title = "无法识别这个输入",
            message = result.warnings.joinToString("\n") { it.message }.ifBlank { "请检查数学表达式的写法。" },
            color = MaterialTheme.colorScheme.secondary,
            rows = listOf("识别结果" to result.problemType.label),
        )
    }
}

@Composable
private fun MathVerificationStep(number: Int, step: VerificationStep) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(number), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(step.title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        step.before?.let { before ->
            Text(before.display, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
            if (step.after != null && step.after.display != before.display) {
                Spacer(Modifier.height(5.dp))
                Text("↓", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
        step.after?.let { after ->
            if (step.before == null || after.display != step.before.display) {
                Spacer(Modifier.height(5.dp))
                Text(after.display, color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, lineHeight = 24.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(step.explanation, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), fontSize = 13.sp, lineHeight = 20.sp)
        if (step.conditions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("条件：${step.conditions.joinToString("；")}", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, lineHeight = 18.sp)
        }
        if (step.children.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            step.children.forEachIndexed { index, child ->
                MathVerificationStep(index + 1, child)
            }
        }
    }
}
