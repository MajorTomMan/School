package com.majortomman.school.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.ActivityEvent
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.ActivityState
import com.majortomman.school.learning.activity.MatchActivitySpec
import com.majortomman.school.learning.activity.MatchActivityState
import com.majortomman.school.learning.activity.MoveDirection
import com.majortomman.school.learning.activity.MoveOrderItem
import com.majortomman.school.learning.activity.OrderActivitySpec
import com.majortomman.school.learning.activity.OrderActivityState
import com.majortomman.school.learning.activity.SelectMatchLeft
import com.majortomman.school.learning.activity.SelectMatchRight
import com.majortomman.school.learning.activity.SelectOneActivitySpec
import com.majortomman.school.learning.activity.SelectOneActivityState
import com.majortomman.school.learning.activity.SelectOption
import com.majortomman.school.learning.activity.SubmitActivity
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.capability.CapabilityKey
import com.majortomman.school.learning.content.LearningContent

internal object SelectOneActivityUiHost : ActivityUiHost {
    override val capability: CapabilityKey = ActivityCapabilityKeys.SELECT_ONE
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
        require(spec is SelectOneActivitySpec) { "core.select-one UI host 收到错误 spec" }
        require(state is SelectOneActivityState) { "core.select-one UI host 收到错误 state" }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            spec.options.forEach { option ->
                val selected = state.selectedId == option.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onEvent(SelectOption(option.id)) }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (selected) "●" else "○",
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        option.label,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                SchoolDivider()
            }
        }
        Spacer(Modifier.height(12.dp))
        SchoolPrimaryAction(
            label = "提交",
            enabled = enabled && state.selectedId != null,
            onClick = { onEvent(SubmitActivity) },
        )
        CollectionActivityAssessmentFeedback(assessmentOutcome, explanation)
    }
}

internal object OrderActivityUiHost : ActivityUiHost {
    override val capability: CapabilityKey = ActivityCapabilityKeys.ORDER
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
        require(spec is OrderActivitySpec) { "core.order UI host 收到错误 spec" }
        require(state is OrderActivityState) { "core.order UI host 收到错误 state" }
        val items = spec.items.associateBy { it.id }

        state.orderedIds.forEachIndexed { index, id ->
            val item = items.getValue(id)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "%02d".format(index + 1),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    item.label,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "↑",
                    modifier = Modifier.clickable(enabled = enabled && index > 0) {
                        onEvent(MoveOrderItem(id, MoveDirection.UP))
                    }.padding(8.dp),
                    color = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "↓",
                    modifier = Modifier.clickable(enabled = enabled && index < state.orderedIds.lastIndex) {
                        onEvent(MoveOrderItem(id, MoveDirection.DOWN))
                    }.padding(8.dp),
                    color = if (index < state.orderedIds.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            SchoolDivider()
        }
        Spacer(Modifier.height(12.dp))
        SchoolPrimaryAction(
            label = "提交顺序",
            enabled = enabled,
            onClick = { onEvent(SubmitActivity) },
        )
        CollectionActivityAssessmentFeedback(assessmentOutcome, explanation)
    }
}

internal object MatchActivityUiHost : ActivityUiHost {
    override val capability: CapabilityKey = ActivityCapabilityKeys.MATCH
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
        require(spec is MatchActivitySpec) { "core.match UI host 收到错误 spec" }
        require(state is MatchActivityState) { "core.match UI host 收到错误 state" }
        val rightById = spec.rightItems.associateBy { it.id }

        Text(
            "先选择左侧项目，再选择与它对应的右侧项目。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        spec.leftItems.forEach { item ->
            val selected = state.selectedLeftId == item.id
            val matchedLabel = state.matches[item.id]?.let { rightById[it]?.label }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onEvent(SelectMatchLeft(item.id)) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (selected) "●" else "○",
                    color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
                    if (matchedLabel != null) {
                        Text("→ $matchedLabel", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            SchoolDivider()
        }

        Spacer(Modifier.height(16.dp))
        Text("可匹配项目", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        spec.rightItems.forEach { item ->
            val used = item.id in state.matches.values
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled && state.selectedLeftId != null) { onEvent(SelectMatchRight(item.id)) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
                if (used) {
                    Text("已匹配", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
                }
            }
            SchoolDivider()
        }

        Spacer(Modifier.height(12.dp))
        SchoolPrimaryAction(
            label = "提交匹配",
            enabled = enabled && state.matches.size == spec.leftItems.size,
            onClick = { onEvent(SubmitActivity) },
        )
        CollectionActivityAssessmentFeedback(assessmentOutcome, explanation)
    }
}

@Composable
private fun CollectionActivityAssessmentFeedback(
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
            InlineAssessmentOutcome.INCORRECT -> "结果还不正确，可以调整后再试。"
            InlineAssessmentOutcome.INVALID -> "当前结果无法判定，请重新提交。"
        }
        Text(message, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        if (outcome == InlineAssessmentOutcome.CORRECT && explanation.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LearningContentList(explanation)
        }
    }
}
