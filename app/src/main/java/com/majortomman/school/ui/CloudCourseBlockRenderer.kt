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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.majortomman.school.learning.activity.ActivityResult
import com.majortomman.school.learning.activity.ActivityRuntime
import com.majortomman.school.learning.activity.PlaceOnNumberLineActivitySpec
import com.majortomman.school.learning.activity.SubmitActivity
import com.majortomman.school.learning.activity.TextAnswerActivityState
import com.majortomman.school.learning.activity.TextChanged
import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.activity.math.NumberLinePositionState
import com.majortomman.school.learning.activity.math.PositionSelected
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseStepRole
import com.majortomman.school.visualization.SchoolVisualization
import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationParameterValue
import com.majortomman.school.visualization.VisualizationParameters

@Composable
internal fun AuthoredTeachingContent(
    steps: List<CourseStep>,
    activeStepId: String,
    assessmentOutcome: InlineAssessmentOutcome?,
    activityEnabled: Boolean = true,
    onActivityResult: (CourseStep, ActivityResult) -> Unit,
) {
    steps.forEachIndexed { index, step ->
        if (index > 0) {
            Spacer(Modifier.height(SchoolUiMetrics.sectionGap))
            SchoolDivider()
            Spacer(Modifier.height(18.dp))
        }
        AuthoredStep(
            step = step,
            active = step.id == activeStepId,
            assessmentOutcome = if (step.id == activeStepId) assessmentOutcome else null,
            activityEnabled = activityEnabled,
            onActivityResult = onActivityResult,
        )
    }
}

@Composable
private fun AuthoredStep(
    step: CourseStep,
    active: Boolean,
    assessmentOutcome: InlineAssessmentOutcome?,
    activityEnabled: Boolean,
    onActivityResult: (CourseStep, ActivityResult) -> Unit,
) {
    val title = step.title ?: defaultTitle(step.role)
    if (title != null) {
        SchoolSectionLabel(title, color = roleColor(step.role))
        Spacer(Modifier.height(14.dp))
    }
    LearningContentList(step.content)
    if (active) {
        step.activity?.let {
            Spacer(Modifier.height(18.dp))
            when (it) {
                is TextAnswerActivitySpec -> TextAnswerActivity(
                    spec = it,
                    assessmentOutcome = assessmentOutcome,
                    explanation = step.assessment?.explanation.orEmpty(),
                    enabled = activityEnabled,
                    onResult = { result -> onActivityResult(step, result) },
                )
                is PlaceOnNumberLineActivitySpec -> PlaceOnNumberLineActivity(
                    spec = it,
                    assessmentOutcome = assessmentOutcome,
                    explanation = step.assessment?.explanation.orEmpty(),
                    enabled = activityEnabled,
                    onResult = { result -> onActivityResult(step, result) },
                )
            }
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
private fun TextAnswerActivity(
    spec: TextAnswerActivitySpec,
    assessmentOutcome: InlineAssessmentOutcome?,
    explanation: List<LearningContent>,
    enabled: Boolean,
    onResult: (ActivityResult) -> Unit,
) {
    val runtime = remember(spec) { ActivityRuntime(spec) }
    var state by remember(spec.id.value) { mutableStateOf(runtime.state as TextAnswerActivityState) }

    BasicTextField(
        value = state.draft,
        enabled = enabled,
        onValueChange = { value ->
            state = runtime.dispatch(TextChanged(value)).state as TextAnswerActivityState
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
                if (state.draft.isBlank()) {
                    Text(spec.placeholder ?: "输入答案", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
                inner()
            }
        },
    )
    SchoolDivider()
    Spacer(Modifier.height(12.dp))
    SchoolPrimaryAction(
        label = "提交",
        enabled = enabled && state.draft.isNotBlank(),
        onClick = {
            val transition = runtime.dispatch(SubmitActivity)
            state = transition.state as TextAnswerActivityState
            transition.result?.let(onResult)
        },
    )

    ActivityAssessmentFeedback(assessmentOutcome, explanation)
}

@Composable
private fun PlaceOnNumberLineActivity(
    spec: PlaceOnNumberLineActivitySpec,
    assessmentOutcome: InlineAssessmentOutcome?,
    explanation: List<LearningContent>,
    enabled: Boolean,
    onResult: (ActivityResult) -> Unit,
) {
    val runtime = remember(spec) { ActivityRuntime(spec) }
    var state by remember(spec.id.value) { mutableStateOf(runtime.state as NumberLinePositionState) }
    val visualization = VisualizationInvocation(
        renderer = VisualizationKey("mathematics.number-line.basic"),
        parameters = VisualizationParameters.of(
            "min" to VisualizationParameterValue.NumberValue(spec.min),
            "max" to VisualizationParameterValue.NumberValue(spec.max),
            "step" to VisualizationParameterValue.NumberValue(spec.step),
            "value" to VisualizationParameterValue.NumberValue(state.selectedValue),
        ),
    )

    Box(Modifier.fillMaxWidth().height(260.dp)) {
        SchoolVisualization(visualization, Modifier.fillMaxWidth())
    }
    Text(
        text = "当前位置：${formatActivityNumber(state.selectedValue)}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Slider(
        value = state.selectedValue.toFloat(),
        enabled = enabled,
        onValueChange = { raw ->
            state = runtime.dispatch(PositionSelected(raw.toDouble())).state as NumberLinePositionState
        },
        valueRange = spec.min.toFloat()..spec.max.toFloat(),
    )
    Spacer(Modifier.height(12.dp))
    SchoolPrimaryAction(
        label = "提交",
        enabled = enabled,
        onClick = {
            val transition = runtime.dispatch(SubmitActivity)
            state = transition.state as NumberLinePositionState
            transition.result?.let(onResult)
        },
    )
    ActivityAssessmentFeedback(assessmentOutcome, explanation)
}

@Composable
private fun ActivityAssessmentFeedback(
    assessmentOutcome: InlineAssessmentOutcome?,
    explanation: List<LearningContent>,
) {
    assessmentOutcome?.let { outcome ->
        Spacer(Modifier.height(12.dp))
        val color = when (outcome) {
            InlineAssessmentOutcome.CORRECT -> MaterialTheme.colorScheme.tertiary
            InlineAssessmentOutcome.INCORRECT -> MaterialTheme.colorScheme.error
            InlineAssessmentOutcome.INVALID -> MaterialTheme.colorScheme.secondary
        }
        val message = when (outcome) {
            InlineAssessmentOutcome.CORRECT -> "✓ 回答正确。"
            InlineAssessmentOutcome.INCORRECT -> "答案还不正确，可以检查后再试。"
            InlineAssessmentOutcome.INVALID -> "当前结果无法判定，请重新提交。"
        }
        Text(message, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        if (outcome == InlineAssessmentOutcome.CORRECT && explanation.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LearningContentList(explanation)
        }
    }
}

private fun formatActivityNumber(value: Double): String {
    val integer = value.toLong()
    return if (kotlin.math.abs(value - integer) < 1e-9) integer.toString()
    else "%.3f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
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
