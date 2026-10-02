package com.majortomman.school.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.activity.ActivityEvent
import com.majortomman.school.learning.activity.ActivityState
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseStepRole

@Composable
internal fun AuthoredTeachingContent(
    steps: List<CourseStep>,
    activeStepId: String,
    assessmentOutcome: InlineAssessmentOutcome?,
    activityState: ActivityState?,
    activityEnabled: Boolean = true,
    onActivityEvent: (CourseStep, ActivityEvent) -> Unit,
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
            activityState = if (step.id == activeStepId) activityState else null,
            activityEnabled = activityEnabled,
            onActivityEvent = onActivityEvent,
        )
    }
}

@Composable
private fun AuthoredStep(
    step: CourseStep,
    active: Boolean,
    assessmentOutcome: InlineAssessmentOutcome?,
    activityState: ActivityState?,
    activityEnabled: Boolean,
    onActivityEvent: (CourseStep, ActivityEvent) -> Unit,
) {
    val title = step.title ?: defaultTitle(step.role)
    if (title != null) {
        SchoolSectionLabel(title, color = roleColor(step.role))
        Spacer(Modifier.height(14.dp))
    }
    LearningContentList(step.content)

    val spec = step.activity
    if (active && spec != null) {
        Spacer(Modifier.height(18.dp))
        val state = requireNotNull(activityState) { "当前 Activity 缺少 runtime state：" + spec.id }
        require(state.spec == spec) { "Activity UI state 与 spec 不一致：" + spec.id }
        SchoolActivityUiCatalog.Render(
            spec = spec,
            state = state,
            assessmentOutcome = assessmentOutcome,
            explanation = step.assessment?.explanation.orEmpty(),
            enabled = activityEnabled,
            onEvent = { event -> onActivityEvent(step, event) },
        )
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
