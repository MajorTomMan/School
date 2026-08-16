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
import com.majortomman.school.learning.verification.LocalMathExpressionKind
import com.majortomman.school.learning.verification.LocalMathExpressionResult
import com.majortomman.school.learning.verification.LocalMathExpressionVerifier
import com.majortomman.school.learning.verification.VerificationSubject
import com.majortomman.school.visualization.SchoolVisualization

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
                VerificationSubject.MATHEMATICS -> MathExpressionVerificationPage(onBack = { openedSubjectName = null })
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
        Text("选择学科，进入对应的本地验证页面。每个学科拥有独立的解析与可视化能力。", color = InteractiveMuted, fontSize = 15.sp, lineHeight = 23.sp)
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
private fun MathExpressionVerificationPage(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<LocalMathExpressionResult?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

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
        Text("任意数学表达式", color = InteractiveBlue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("完全本地解析。常量表达式直接计算结果；只含 x 的表达式自动识别为函数，并使用统一坐标系绘制图像。", color = InteractiveMuted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))

        VerificationTextInput(
            label = "表达式",
            value = input,
            onValueChange = {
                input = it
                result = null
                error = null
            },
            hint = "例如：2*(3+4) 或 y=x^2-4",
            maxLength = 256,
        )
        Spacer(Modifier.height(14.dp))
        Text("支持 +  -  *  /  ^、括号、π、√、abs、sqrt、sin、cos、tan、ln、log、exp，以及 y=f(x) 写法。", color = InteractiveMuted.copy(alpha = 0.82f), fontSize = 11.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            MathExample("2*(3+4)", Modifier.weight(1f)) { input = "2*(3+4)"; result = null; error = null }
            MathExample("x^2-4", Modifier.weight(1f)) { input = "x^2-4"; result = null; error = null }
            MathExample("sin(x)", Modifier.weight(1f)) { input = "sin(x)"; result = null; error = null }
        }
        Spacer(Modifier.height(22.dp))

        val enabled = input.isNotBlank()
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp).clickable(enabled = enabled) {
                runCatching { LocalMathExpressionVerifier.verify(input) }.fold(
                    onSuccess = {
                        result = it
                        error = null
                    },
                    onFailure = {
                        result = null
                        error = it.message ?: "无法解析这个表达式。"
                    },
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            Text("本地验证", color = if (enabled) InteractiveBlue else InteractiveMuted.copy(alpha = 0.4f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (enabled) 2.dp else 1.dp).background(if (enabled) InteractiveBlue else InteractiveLine))
        }
        Spacer(Modifier.height(28.dp))

        when {
            error != null -> VerificationStatusBlock(
                title = "暂时无法验证",
                message = error.orEmpty(),
                color = InteractiveYellow,
            )
            result != null -> MathExpressionResultView(requireNotNull(result))
            else -> {
                Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
                Spacer(Modifier.height(12.dp))
                Text("结果会显示在这里", color = InteractiveMuted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(52.dp))
    }
}

@Composable
private fun MathExample(text: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(text, color = InteractiveWhite.copy(alpha = 0.78f), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

@Composable
private fun MathExpressionResultView(result: LocalMathExpressionResult) {
    when (result.kind) {
        LocalMathExpressionKind.VALUE -> VerificationStatusBlock(
            title = "计算完成",
            message = "表达式已由本地数学引擎解析并计算，不涉及网络或 AI。",
            color = InteractiveGreen,
            normalized = result.displayAnswer,
            rows = listOf(
                "表达式" to result.normalizedExpression,
                "类型" to "数值表达式",
            ),
        )
        LocalMathExpressionKind.FUNCTION -> {
            val graph = requireNotNull(result.graph)
            VerificationStatusBlock(
                title = "函数已识别",
                message = "检测到唯一自变量 x，已自动生成函数图像。",
                color = InteractiveGreen,
                normalized = result.displayAnswer,
                rows = listOf(
                    "类型" to "单变量函数",
                    "横轴" to "${axisNumber(graph.parameters.number("xMin"))} ～ ${axisNumber(graph.parameters.number("xMax"))}",
                    "纵轴" to "${axisNumber(graph.parameters.number("yMin"))} ～ ${axisNumber(graph.parameters.number("yMax"))}",
                ),
            )
            Spacer(Modifier.height(26.dp))
            Text("函数图像", color = InteractiveWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(330.dp)) {
                SchoolVisualization(graph, Modifier.fillMaxSize())
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
            Text("入口和翻页层级已经建立，后续会按学科分别接入独立的本地解析器。", color = InteractiveMuted.copy(alpha = 0.72f), fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

private fun axisNumber(value: Double): String {
    val integer = value.toInt()
    return if (value == integer.toDouble()) integer.toString() else "%.2f".format(value)
}
