package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.cloud.InstalledCourse
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.evidence.persistence.RoomLessonEvidenceGateway
import com.majortomman.school.learning.runtime.LessonSessionController
import com.majortomman.school.learning.runtime.LessonSessionIntent
import kotlinx.coroutines.launch

@Composable
fun InteractiveLessonScreen(
    course: InstalledCourse,
    lesson: CourseLesson,
    nextLessonTitle: String?,
    onOpenTextbook: (Int) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val evidenceGateway = remember(context) { RoomLessonEvidenceGateway.create(context) }
    val controller = remember(course.id, course.contentVersion, lesson.id, evidenceGateway) {
        LessonSessionController(
            courseId = course.id,
            contentRevision = course.contentVersion.toString(),
            lesson = lesson,
            evidenceGateway = evidenceGateway,
        )
    }
    val state by controller.state.collectAsState()
    val currentStep = lesson.steps[state.runtime.currentStepIndex.coerceAtMost(lesson.steps.lastIndex)]
    val visibleSteps = lesson.steps.take(state.runtime.currentStepIndex + 1)
    val textbookReference = lesson.references.firstOrNull()

    LaunchedEffect(state.runtime.finished) {
        if (state.runtime.finished) onComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
    ) {
        SchoolCompactTopBar(
            title = lesson.title,
            onBack = onBack,
            actionLabel = if (textbookReference != null) "教材" else null,
            onAction = { textbookReference?.let { onOpenTextbook(it.pageStart) } },
            actionEnabled = textbookReference != null && course.pdfFile.isFile,
        )
        SchoolDivider()

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = 22.dp),
        ) {
            Text("MATHEMATICS / JUNIOR HIGH / SCHOOL", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.padding(top = 5.dp))
            Text(lesson.title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(course.title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)

            Spacer(Modifier.padding(top = 18.dp))
            SchoolSectionLabel("学习目标")
            Spacer(Modifier.padding(top = 6.dp))
            lesson.goals.forEachIndexed { index, goal ->
                Text(
                    "${index + 1}.  $goal",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(28.dp))
            SchoolDivider()
            Spacer(Modifier.height(22.dp))
            AuthoredTeachingContent(
                steps = visibleSteps,
                activeStepId = currentStep.id,
                assessmentOutcome = state.assessmentFeedback
                    ?.takeIf { it.stepId == currentStep.id }
                    ?.outcome,
                activityState = state.activityState,
                activityEnabled = !state.busy,
                onActivityEvent = { step, event ->
                    scope.launch {
                        controller.dispatch(
                            LessonSessionIntent.ActivityEventDispatched(
                                stepId = step.id,
                                event = event,
                            ),
                        )
                    }
                },
            )
            state.errorMessage?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.padding(top = SchoolUiMetrics.pageBottom))
        }

        if (!state.runtime.finished && currentStep.activity == null) {
            SchoolDivider()
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SchoolPrimaryAction(
                    label = if (state.runtime.currentStepIndex == lesson.steps.lastIndex) "完成  →" else "继续  →",
                    enabled = !state.busy,
                    onClick = {
                        scope.launch {
                            controller.dispatch(LessonSessionIntent.ContinueRequested)
                        }
                    },
                )
            }
        }
    }
}
