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
import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.assessment.domain.InlineAssessmentRule
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseStepRole
import com.majortomman.school.visualization.SchoolVisualization

@Composable
internal fun AuthoredTeachingContent(steps: List<CourseStep>) {
    steps.forEachIndexed { index, step ->
        if (index > 0) {
            Spacer(Modifier.height(SchoolUiMetrics.sectionGap))
            SchoolDivider()
            Spacer(Modifier.height(18.dp))
        }
        AuthoredStep(step)
    }
}

@Composable
private fun AuthoredStep(step: CourseStep) {
    val title = step.title ?: defaultTitle(step.role)
    if (title != null) {
        SchoolSectionLabel(title, color = roleColor(step.role))
        Spacer(Modifier.height(14.dp))
    }
    LearningContentList(step.content)
    step.activity?.let {
        Spacer(Modifier.height(18.dp))
        when (it) {
            is TextAnswerActivitySpec -> TextAnswerActivity(step, it)
        }
    }
}

@Composable
private fun LearningContentList(content: List<LearningContent>) {
    content.forEachIndexed { index, item ->
        if (index > 0) Spacer(Modifier.height(12.dp))
        when (item) {
            is LearningContent.Heading -> Text(item.text, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            is LearningContent.Text -> Text(
                item.text,
                color = when (item.style) {
                    com.majortomman.school.learning.content.LearningTextStyle.PROMPT -> MaterialTheme.colorScheme.onBackground
                    com.majortomman.school.learning.content.LearningTextStyle.CAPTION -> MaterialTheme.colorScheme.onSurfaceVariant
                    com.majortomman.school.learning.content.LearningTextStyle.EXPLANATION -> MaterialTheme.colorScheme.onSurfaceVariant
                    com.majortomman.school.learning.content.LearningTextStyle.BODY -> MaterialTheme.colorScheme.onBackground
                },
                style = if (item.style == com.majortomman.school.learning.content.LearningTextStyle.PROMPT) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.style == com.majortomman.school.learning.content.LearningTextStyle.PROMPT) FontWeight.Medium else FontWeight.Normal,
            )
            is LearningContent.Formula -> {
                SchoolFormula(
                    latex = item.expression,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.headlineMedium,
                )
                item.conditions.forEach { condition ->
                    Text(condition, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
            is LearningContent.ItemList -> item.items.forEach { value ->
                Row(modifier = Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                    Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
                }
            }
            is LearningContent.Table -> {
                item.caption?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge) }
                Text(item.columns.joinToString("   "), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                item.rows.forEach { row -> Text(row.joinToString("   "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            }
            is LearningContent.Visualization -> Box(Modifier.fillMaxWidth().height(360.dp)) {
                SchoolVisualization(item.visualization, Modifier.fillMaxWidth())
            }
            is LearningContent.Image -> error("Course image content must be rejected by CourseDocumentParser")
        }
    }
}

@Composable
private fun TextAnswerActivity(step: CourseStep, spec: TextAnswerActivitySpec) {
    var answer by rememberSaveable(spec.id.value) { mutableStateOf("") }
    var checked by rememberSaveable(spec.id.value, "checked") { mutableStateOf(false) }
    val assessment = step.assessment
    val correct = if (!checked || assessment == null) null else when (val rule = assessment.rule) {
        is InlineAssessmentRule.ExactText -> {
            val actual = answer.trim()
            val expected = rule.expected.trim()
            if (rule.ignoreCase) actual.equals(expected, ignoreCase = true) else actual == expected
        }
    }

    BasicTextField(
        value = answer,
        onValueChange = {
            answer = it.take(2_000)
            checked = false
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = SchoolUiMetrics.textInputMinHeight).padding(vertical = 8.dp),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                if (answer.isBlank()) {
                    Text(spec.placeholder ?: "输入答案", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
                inner()
            }
        },
    )
    SchoolDivider()
    Spacer(Modifier.height(12.dp))
    SchoolPrimaryAction(
        label = if (assessment == null) "完成" else "提交",
        enabled = answer.isNotBlank(),
        onClick = { checked = true },
    )

    if (checked && assessment != null) {
        Spacer(Modifier.height(12.dp))
        val color = if (correct == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        Text(
            if (correct == true) "✓ 回答正确。" else "答案还不正确，可以检查后再试。",
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (correct == true) {
            Spacer(Modifier.height(8.dp))
            LearningContentList(assessment.explanation)
        }
    }
}

@Composable
private fun roleColor(role: CourseStepRole) = when (role) {
    CourseStepRole.EXPLANATION, CourseStepRole.EXAMPLE -> MaterialTheme.colorScheme.primary
    CourseStepRole.INQUIRY, CourseStepRole.KEY_IDEA -> MaterialTheme.colorScheme.secondary
    CourseStepRole.PRACTICE -> MaterialTheme.colorScheme.primary
    CourseStepRole.CHECKPOINT -> MaterialTheme.colorScheme.tertiary
    CourseStepRole.SUMMARY -> MaterialTheme.colorScheme.secondary
}

private fun defaultTitle(role: CourseStepRole): String? = when (role) {
    CourseStepRole.EXPLANATION -> null
    CourseStepRole.INQUIRY -> "先想一想"
    CourseStepRole.EXAMPLE -> "例题"
    CourseStepRole.KEY_IDEA -> "关键理解"
    CourseStepRole.PRACTICE -> "动手试一试"
    CourseStepRole.CHECKPOINT -> "检查一下"
    CourseStepRole.SUMMARY -> "小结"
}
