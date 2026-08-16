package com.majortomman.school.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.majortomman.school.learning.verification.VerificationSubject
import com.majortomman.school.learning.verification.core.VerificationResult
import com.majortomman.school.learning.verification.core.VerificationRequest
import com.majortomman.school.learning.verification.core.VerificationStatus
import com.majortomman.school.learning.verification.core.VerificationStep
import com.majortomman.school.learning.verification.core.VerificationVisualizationRequest
import com.majortomman.school.learning.verification.core.VerificationVisualizationValue
import com.majortomman.school.learning.verification.math.MathVerificationEngine
import com.majortomman.school.visualization.SchoolVisualization
import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationParameterValue
import com.majortomman.school.visualization.VisualizationParameters
import com.majortomman.school.visualization.VisualizationTexts

@Composable
internal fun VerificationHubScreen() {
    var openedSubjectName by rememberSaveable { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = openedSubjectName,
        transitionSpec = {
            if (targetState != null) {
                (fadeIn(tween(220)) + slideInHorizontally(tween(360)) { it }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { -it / 3 })
            } else {
                (fadeIn(tween(220)) + slideInHorizontally(tween(360)) { -it / 3 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { it })
            }
        },
        label = "verificationSubjectPage",
    ) { subjectName ->
        if (subjectName == null) {
            VerificationSubjectIndex(onOpen = { openedSubjectName = it.name })
        } else {
            val subject = VerificationSubject.valueOf(subjectName)
            when (subject) {
                VerificationSubject.MATHEMATICS -> MathVerificationPage(onBack = { openedSubjectName = null })
                else -> VerificationSubjectPlaceholderPage(subject, onBack = { openedSubjectName = null })
            }
        }
    }
}

@Composable
private fun VerificationSubjectIndex(onOpen: (VerificationSubject) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InteractiveBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("验证", color = InteractiveWhite, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("选择学科，进入对应的本地验证页面。学科共享步骤和结果框架，但各自保留独立的知识与推理规则。", color = InteractiveMuted, fontSize = 15.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(28.dp))

        VerificationSubject.entries.chunked(2).forEach { subjects ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                subjects.forEach { subject ->
                    VerificationSubjectEntry(subject, Modifier.weight(1f)) { onOpen(subject) }
                }
                if (subjects.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun VerificationSubjectEntry(subject: VerificationSubject, modifier: Modifier, onClick: () -> Unit) {
    val available = subject == VerificationSubject.MATHEMATICS
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(subject.label, color = InteractiveWhite, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(if (available) "本地可用" else "待接入", color = if (available) InteractiveGreen else InteractiveMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(subject.subtitle, color = InteractiveMuted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(if (available) 2.dp else 1.dp).background(if (available) InteractiveBlue else InteractiveLine))
    }
}

@Composable
private fun MathVerificationPage(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<VerificationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InteractiveBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("‹ 验证", modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp), color = InteractiveMuted, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Text("数学", color = InteractiveWhite, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("初高中数学 · 本地符号推理", color = InteractiveBlue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("输入表达式、函数或一元方程。本地引擎会识别题型、给出答案和可复核的逐步解析；能可视化的结果会直接调用统一可视化基础设施。", color = InteractiveMuted, fontSize = 14.sp, lineHeight = 22.sp)
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
        Text("当前覆盖数值表达式、代数式展开/合并、一元一次方程、一元二次方程和函数图像；明确不处理极限、导数、积分等高等数学。", color = InteractiveMuted.copy(alpha = 0.82f), fontSize = 11.sp, lineHeight = 18.sp)
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
            Text("本地验证", color = if (enabled) InteractiveBlue else InteractiveMuted.copy(alpha = 0.4f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (enabled) 2.dp else 1.dp).background(if (enabled) InteractiveBlue else InteractiveLine))
        }
        Spacer(Modifier.height(28.dp))

        result?.let { MathVerificationResultView(it) } ?: run {
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
            Spacer(Modifier.height(12.dp))
            Text("答案、逐步解析和可视化会显示在这里", color = InteractiveMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(52.dp))
    }
}

@Composable
private fun MathExample(text: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(text, color = InteractiveWhite.copy(alpha = 0.78f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

@Composable
private fun MathVerificationResultView(result: VerificationResult) {
    when (result.status) {
        VerificationStatus.SUCCESS -> {
            VerificationStatusBlock(
                title = "本地求解完成",
                message = "所有解析、计算和步骤生成均在本地完成。",
                color = InteractiveGreen,
                normalized = result.answer?.display,
                rows = listOf(
                    "题型" to result.problemType.label,
                    "标准化输入" to result.normalizedInput,
                    "步骤" to "${result.steps.size} 步",
                ),
            )
            if (result.steps.isNotEmpty()) {
                Spacer(Modifier.height(30.dp))
                Text("逐步解析", color = InteractiveWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                result.steps.forEachIndexed { index, step ->
                    MathVerificationStep(index + 1, step)
                    if (index != result.steps.lastIndex) Spacer(Modifier.height(18.dp))
                }
            }
            if (result.warnings.isNotEmpty()) {
                Spacer(Modifier.height(26.dp))
                result.warnings.forEach { warning ->
                    Text(warning.message, color = InteractiveYellow, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            result.visualizations.forEach { visualization ->
                Spacer(Modifier.height(30.dp))
                Text("可视化", color = InteractiveWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(330.dp)) {
                    SchoolVisualization(visualization.toVisualizationInvocation(), Modifier.fillMaxSize())
                }
            }
        }
        VerificationStatus.UNSUPPORTED -> VerificationStatusBlock(
            title = "超出当前本地范围",
            message = result.warnings.joinToString("\n") { it.message }.ifBlank { "这个数学输入暂时不在当前初高中本地引擎的支持范围内。" },
            color = InteractiveYellow,
            rows = listOf("识别结果" to result.problemType.label),
        )
        VerificationStatus.INVALID -> VerificationStatusBlock(
            title = "无法识别这个输入",
            message = result.warnings.joinToString("\n") { it.message }.ifBlank { "请检查数学表达式的写法。" },
            color = InteractiveYellow,
            rows = listOf("识别结果" to result.problemType.label),
        )
    }
}

@Composable
private fun MathVerificationStep(number: Int, step: VerificationStep) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(number), color = InteractiveBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(step.title, modifier = Modifier.weight(1f), color = InteractiveWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        step.before?.let { before ->
            Text(before.display, color = InteractiveMuted, fontSize = 13.sp, lineHeight = 20.sp)
            if (step.after != null && step.after.display != before.display) {
                Spacer(Modifier.height(5.dp))
                Text("↓", color = InteractiveBlue, fontSize = 13.sp)
            }
        }
        step.after?.let { after ->
            if (step.before == null || after.display != step.before.display) {
                Spacer(Modifier.height(5.dp))
                Text(after.display, color = InteractiveWhite, fontSize = 17.sp, lineHeight = 24.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(step.explanation, color = InteractiveWhite.copy(alpha = 0.72f), fontSize = 13.sp, lineHeight = 20.sp)
        if (step.conditions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("条件：${step.conditions.joinToString("；")}", color = InteractiveYellow, fontSize = 11.sp, lineHeight = 18.sp)
        }
        if (step.children.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            step.children.forEachIndexed { index, child ->
                MathVerificationStep(index + 1, child)
            }
        }
    }
}

@Composable
private fun VerificationSubjectPlaceholderPage(subject: VerificationSubject, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(InteractiveBlack).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("‹ 验证", modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp), color = InteractiveMuted, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Text(subject.label, color = InteractiveWhite, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(subject.subtitle, color = InteractiveMuted, fontSize = 14.sp, lineHeight = 22.sp)
        }
        Column {
            Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveLine))
            Spacer(Modifier.height(14.dp))
            Text("本地验证能力待接入", color = InteractiveMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("入口与公共 Verification Core 已建立；后续会把该学科现有的确定性内核适配到同一结果、步骤和可视化协议。", color = InteractiveMuted.copy(alpha = 0.72f), fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

private fun VerificationVisualizationRequest.toVisualizationInvocation(): VisualizationInvocation {
    val mapped = linkedMapOf<String, VisualizationParameterValue>()
    for ((name, value) in parameters) {
        mapped[name] = when (value) {
            is VerificationVisualizationValue.NumberValue -> VisualizationParameterValue.NumberValue(value.value)
            is VerificationVisualizationValue.BooleanValue -> VisualizationParameterValue.BooleanValue(value.value)
            is VerificationVisualizationValue.NumberListValue -> VisualizationParameterValue.NumberListValue(value.values)
            is VerificationVisualizationValue.MathExpressionValue -> VisualizationParameterValue.MathExpressionValue.parse(value.expression)
        }
    }
    return VisualizationInvocation(
        renderer = VisualizationKey(renderer),
        parameters = VisualizationParameters.of(mapped),
        texts = VisualizationTexts.of(texts),
    )
}
