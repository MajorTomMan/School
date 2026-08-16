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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.course.CourseCheckpoint
import com.majortomman.school.learning.course.CourseExample
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseFormula
import com.majortomman.school.learning.course.CourseKeyIdea
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseSummaryStep
import com.majortomman.school.learning.course.CourseVisualizationStep
import com.majortomman.school.learning.science.math.MathFormulaStatus
import com.majortomman.school.learning.science.math.MathFormulaVerifier
import com.majortomman.school.visualization.SchoolVisualization

@Composable
internal fun AuthoredTeachingPageContent(steps: List<CourseStep>, lesson: CourseLesson) {
    steps.forEachIndexed { index, step ->
        if (index > 0) Spacer(Modifier.height(SchoolUiMetrics.sectionGap))
        AuthoredStep(step)
    }
}

@Composable
internal fun AuthoredPracticePage(practice: CoursePractice, number: Int, total: Int) {
    var workProcess by rememberSaveable(practice.id, "work") { mutableStateOf("") }
    var answerDraft by rememberSaveable(practice.id, "answer") { mutableStateOf("") }
    var checked by rememberSaveable(practice.id, "checked") { mutableStateOf(false) }
    var correct by rememberSaveable(practice.id, "correct") { mutableStateOf(false) }
    var hintRevealed by rememberSaveable(practice.id, "hint") { mutableStateOf(false) }
    var solutionRevealed by rememberSaveable(practice.id, "solution") { mutableStateOf(false) }

    SchoolSectionLabel("练习 $number / $total", color = InteractiveYellow)
    Spacer(Modifier.height(14.dp))
    Text(text = practice.prompt, color = InteractiveWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(24.dp))

    PracticeDraftField(
        label = "做题过程（可选）",
        hint = "把计算、推导或你的判断过程写在这里。过程和最终答案分开保存。",
        value = workProcess,
        minHeight = 112,
        onValueChange = { if (it.length <= 12_000) workProcess = it },
    )
    Spacer(Modifier.height(18.dp))
    PracticeDraftField(
        label = "最终答案",
        hint = "这里只写最后答案",
        value = answerDraft,
        minHeight = 48,
        onValueChange = {
            answerDraft = it.take(1_000)
            checked = false
            solutionRevealed = false
        },
    )

    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "检查答案",
            modifier = Modifier.clickable(enabled = answerDraft.isNotBlank()) {
                correct = practiceAnswerMatches(answerDraft, practice.answer)
                checked = true
            }.padding(vertical = 10.dp),
            color = if (answerDraft.isNotBlank()) InteractiveBlue else InteractiveMuted.copy(alpha = 0.45f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        if (!correct && practice.analysis.isNotEmpty()) {
            Text(
                text = if (hintRevealed) "收起提示" else "查看提示",
                modifier = Modifier.clickable { hintRevealed = !hintRevealed }.padding(vertical = 10.dp),
                color = InteractiveYellow,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (hintRevealed && practice.analysis.isNotEmpty()) {
        Text("提示：${practice.analysis.first()}", color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    }

    if (checked) {
        Spacer(Modifier.height(12.dp))
        val feedbackColor = if (correct) InteractiveGreen else InteractiveRed
        Box(Modifier.fillMaxWidth().height(2.dp).background(feedbackColor.copy(alpha = 0.72f)))
        Spacer(Modifier.height(10.dp))
        Text(if (correct) "回答正确。" else "答案还不正确，可以检查过程或查看提示后再试。", color = feedbackColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        if (!correct) {
            Text(
                text = if (solutionRevealed) "收起参考答案与解析" else "查看参考答案与解析",
                modifier = Modifier.clickable { solutionRevealed = !solutionRevealed }.padding(top = 12.dp, bottom = 8.dp),
                color = InteractiveYellow,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (correct || solutionRevealed) {
        Spacer(Modifier.height(12.dp))
        Text("答案：${practice.answer}", color = InteractiveGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        practice.analysis.forEach { item ->
            Text(text = "— $item", color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun PracticeDraftField(
    label: String,
    hint: String,
    value: String,
    minHeight: Int,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = InteractiveWhite, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp).padding(vertical = 6.dp),
            textStyle = TextStyle(color = InteractiveWhite, fontSize = MaterialTheme.typography.bodyLarge.fontSize, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight),
            cursorBrush = SolidColor(InteractiveBlue),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isBlank()) Text(hint, color = InteractiveMuted.copy(alpha = 0.68f), style = MaterialTheme.typography.bodyMedium)
                    innerTextField()
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

private fun practiceAnswerMatches(actual: String, expected: String): Boolean {
    val normalizedActual = normalizePracticeAnswer(actual)
    val normalizedExpected = normalizePracticeAnswer(expected)
    if (normalizedActual == normalizedExpected) return true
    if (normalizedActual.isBlank() || normalizedExpected.isBlank()) return false
    val verification = MathFormulaVerifier.verify("($actual)=($expected)", sampleRelation = true)
    return verification.status == MathFormulaStatus.TRUE_AT_VALUES || verification.status == MathFormulaStatus.SAMPLE_MATCH
}

private fun normalizePracticeAnswer(value: String): String = value.trim().replace(" ", "").replace('−', '-').replace('×', '*').replace('·', '*').replace('÷', '/').lowercase()

@Composable
private fun AuthoredStep(step: CourseStep) {
    when (step) {
        is CourseExplanation -> {
            step.title?.let {
                OpenSectionTitle(it, InteractiveBlue)
                Spacer(Modifier.height(10.dp))
            }
            Text(step.text, color = InteractiveWhite.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
        }
        is CourseQuestion -> {
            OpenSectionTitle("先想一想", InteractiveYellow)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            step.hint?.takeIf { it.isNotBlank() }?.let {
                Text("提示：$it", color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            }
        }
        is CourseKeyIdea -> {
            Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveBlue.copy(alpha = 0.72f)))
            Spacer(Modifier.height(10.dp))
            step.title?.let { Text(it, color = InteractiveBlue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            Text(step.text, color = InteractiveWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
        }
        is CourseFormula -> {
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
            SchoolFormula(latex = step.expression, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), color = InteractiveYellow, style = MaterialTheme.typography.headlineMedium)
            step.note?.let {
                Text(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
        }
        is CourseExample -> {
            OpenSectionTitle(step.title, InteractiveBlue)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            step.steps.forEachIndexed { index, item ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Text("${index + 1}", color = InteractiveYellow, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text(item, modifier = Modifier.weight(1f), color = InteractiveWhite.copy(alpha = 0.82f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("答案：${step.answer}", color = InteractiveYellow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp))
        }
        is CourseVisualizationStep -> Box(Modifier.fillMaxWidth().height(380.dp)) {
            SchoolVisualization(step.visualization, Modifier.fillMaxWidth())
        }
        is CourseCheckpoint -> {
            OpenSectionTitle("检查一下", InteractiveGreen)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, style = MaterialTheme.typography.titleLarge)
            Text("参考：${step.expectedAnswer}", color = InteractiveGreen, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            Text(step.explanation, color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        is CourseSummaryStep -> Text(step.text, color = InteractiveWhite.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OpenSectionTitle(title: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(2.dp).weight(0.08f).background(color))
        Text(title, modifier = Modifier.weight(0.92f), color = InteractiveWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}
