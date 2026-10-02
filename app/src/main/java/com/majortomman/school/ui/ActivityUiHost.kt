package com.majortomman.school.ui

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.ActivityEvent
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.ActivityState
import com.majortomman.school.learning.activity.PlaceOnNumberLineActivitySpec
import com.majortomman.school.learning.activity.SchoolActivityHostCapabilityCatalog
import com.majortomman.school.learning.activity.SubmitActivity
import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.activity.TextAnswerActivityState
import com.majortomman.school.learning.activity.TextChanged
import com.majortomman.school.learning.activity.math.NumberLinePositionState
import com.majortomman.school.learning.activity.math.PositionSelected
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.capability.CapabilityKey
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.visualization.SchoolVisualization
import com.majortomman.school.visualization.SchoolVisualizationCatalog
import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationParameterValue
import com.majortomman.school.visualization.VisualizationParameters

internal interface ActivityUiHost {
    val capability: CapabilityKey
    val schemaVersion: Int
    val requiredVisualizations: Map<VisualizationKey, Int>
        get() = emptyMap()

    @Composable
    fun Render(
        spec: ActivitySpec,
        state: ActivityState,
        assessmentOutcome: InlineAssessmentOutcome?,
        explanation: List<LearningContent>,
        enabled: Boolean,
        onEvent: (ActivityEvent) -> Unit,
    )
}

internal object SchoolActivityUiCatalog {
    private val hosts = linkedMapOf<CapabilityKey, ActivityUiHost>()

    fun install(host: ActivityUiHost) {
        require(host.schemaVersion > 0) { "Activity UI host schemaVersion 必须大于 0" }
        host.requiredVisualizations.forEach { (key, version) ->
            val installed = SchoolVisualizationCatalog.registeredCapabilities()[key]
            require(installed == version) {
                "Activity UI host ${host.capability} 依赖 visualization ${key.value}@$version，当前=$installed"
            }
        }
        synchronized(this) {
            val existing = hosts[host.capability]
            if (existing != null) {
                require(existing.schemaVersion == host.schemaVersion) {
                    "Activity UI host ${host.capability} 已注册 schemaVersion=${existing.schemaVersion}"
                }
                return
            }
            hosts[host.capability] = host
            SchoolActivityHostCapabilityCatalog.install(host.capability, host.schemaVersion)
        }
    }

    @Composable
    fun Render(
        spec: ActivitySpec,
        state: ActivityState,
        assessmentOutcome: InlineAssessmentOutcome?,
        explanation: List<LearningContent>,
        enabled: Boolean,
        onEvent: (ActivityEvent) -> Unit,
    ) {
        val host = synchronized(this) { hosts[spec.capability] }
            ?: error("未安装 Activity UI host：${spec.capability}")
        require(host.schemaVersion == spec.schemaVersion) {
            "Activity UI host ${spec.capability} schemaVersion 不兼容：course=${spec.schemaVersion}, app=${host.schemaVersion}"
        }
        host.Render(spec, state, assessmentOutcome, explanation, enabled, onEvent)
    }
}

internal object CoreTextAnswerActivityUiHost : ActivityUiHost {
    override val capability = ActivityCapabilityKeys.TEXT_ANSWER
    override val schemaVersion: Int = 1

    @Composable
    override fun Render(
        spec: ActivitySpec,
        state: ActivityState,
        assessmentOutcome: InlineAssessmentOutcome?,
        explanation: List<LearningContent>,
        enabled: Boolean,
        onEvent: (ActivityEvent) -> Unit,
    ) {
        require(spec is TextAnswerActivitySpec) { "core.text-answer UI host 收到错误 spec" }
        require(state is TextAnswerActivityState) { "core.text-answer UI host 收到错误 state" }
        BasicTextField(
            value = state.draft,
            enabled = enabled,
            onValueChange = { value -> onEvent(TextChanged(value)) },
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
            onClick = { onEvent(SubmitActivity) },
        )
        ActivityAssessmentFeedback(assessmentOutcome, explanation)
    }
}

internal object PlaceOnNumberLineActivityUiHost : ActivityUiHost {
    private val visualizationKey = VisualizationKey("mathematics.number-line.basic")

    override val capability = ActivityCapabilityKeys.PLACE_ON_NUMBER_LINE
    override val schemaVersion: Int = 1
    override val requiredVisualizations: Map<VisualizationKey, Int> = mapOf(visualizationKey to 1)

    @Composable
    override fun Render(
        spec: ActivitySpec,
        state: ActivityState,
        assessmentOutcome: InlineAssessmentOutcome?,
        explanation: List<LearningContent>,
        enabled: Boolean,
        onEvent: (ActivityEvent) -> Unit,
    ) {
        require(spec is PlaceOnNumberLineActivitySpec) { "place-on-number-line UI host 收到错误 spec" }
        require(state is NumberLinePositionState) { "place-on-number-line UI host 收到错误 state" }
        val visualization = VisualizationInvocation(
            renderer = visualizationKey,
            schemaVersion = 1,
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
            onValueChange = { raw -> onEvent(PositionSelected(raw.toDouble())) },
            valueRange = spec.min.toFloat()..spec.max.toFloat(),
        )
        Spacer(Modifier.height(12.dp))
        SchoolPrimaryAction(
            label = "提交",
            enabled = enabled,
            onClick = { onEvent(SubmitActivity) },
        )
        ActivityAssessmentFeedback(assessmentOutcome, explanation)
    }
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
