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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.activity.ActivityResult
import com.majortomman.school.learning.assessment.domain.InlineAssessmentEvidenceFactory
import com.majortomman.school.learning.assessment.domain.InlineAssessmentEvaluator
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.cloud.InstalledCourse
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.evidence.persistence.LearningEvidenceStore
import com.majortomman.school.learning.runtime.LessonRuntime
import com.majortomman.school.learning.runtime.LessonRuntimeEvent
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
    val runtime = remember(lesson) { LessonRuntime(lesson) }
    val evidenceStore = remember(context) { LearningEvidenceStore.create(context) }
    val wrongAttempts = remember(lesson) { mutableStateMapOf<String, Int>() }
    var runtimeState by remember(lesson) { mutableStateOf(runtime.state) }
    var assessmentFeedback by remember(lesson) { mutableStateOf<Pair<String, InlineAssessmentOutcome>?>(null) }
    var evidenceInFlight by remember(lesson) { mutableStateOf(false) }
    var persistenceError by remember(lesson) { mutableStateOf<String?>(null) }
    val textbookReference = lesson.references.firstOrNull()
    val currentStep = lesson.steps[runtimeState.currentStepIndex]
    val visibleSteps = lesson.steps.take(runtimeState.currentStepIndex + 1)

    fun applyTransition() {
        runtimeState = runtime.state
        if (runtimeState.finished) onComplete()
    }

    fun handleActivityResult(step: CourseStep, result: ActivityResult) {
        persistenceError = null
        val assessment = step.assessment
        if (assessment == null) {
            runtime.dispatch(LessonRuntimeEvent.ActivityCompleted(step.id))
            assessmentFeedback = null
            applyTransition()
            return
        }

        val evaluated = InlineAssessmentEvaluator.evaluate(assessment, result)
        when (evaluated.outcome) {
            InlineAssessmentOutcome.INVALID -> {
                assessmentFeedback = step.id to InlineAssessmentOutcome.INVALID
            }

            InlineAssessmentOutcome.INCORRECT -> {
                wrongAttempts[step.id] = (wrongAttempts[step.id] ?: 0) + 1
                assessmentFeedback = step.id to InlineAssessmentOutcome.INCORRECT
                runtime.dispatch(
                    LessonRuntimeEvent.AssessmentEvaluated(
                        stepId = step.id,
                        correct = false,
                    ),
                )
                applyTransition()
            }

            InlineAssessmentOutcome.CORRECT -> {
                val evidence = InlineAssessmentEvidenceFactory.createForCompletedActivity(
                    courseId = course.id,
                    contentRevision = course.contentVersion.toString(),
                    lessonId = lesson.id,
                    stepId = step.id,
                    activityId = result.activityId,
                    assessment = assessment,
                    wrongAttemptCount = wrongAttempts[step.id] ?: 0,
                )
                evidenceInFlight = true
                scope.launch {
                    runCatching { evidenceStore.record(evidence) }
                        .onSuccess {
                            wrongAttempts.remove(step.id)
                            assessmentFeedback = step.id to InlineAssessmentOutcome.CORRECT
                            runtime.dispatch(
                                LessonRuntimeEvent.AssessmentEvaluated(
                                    stepId = step.id,
                                    correct = true,
                                ),
                            )
                            applyTransition()
                        }
                        .onFailure {
                            persistenceError = "学习记录保存失败，请重新提交。"
                        }
                    evidenceInFlight = false
                }
            }
        }
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
                assessmentOutcome = assessmentFeedback?.takeIf { it.first == currentStep.id }?.second,
                activityEnabled = !evidenceInFlight,
                onActivityResult = ::handleActivityResult,
            )
            persistenceError?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.padding(top = SchoolUiMetrics.pageBottom))
        }

        if (!runtimeState.finished && currentStep.activity == null) {
            SchoolDivider()
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SchoolPrimaryAction(
                    label = if (runtimeState.currentStepIndex == lesson.steps.lastIndex) "完成  →" else "继续  →",
                    onClick = {
                        assessmentFeedback = null
                        runtime.dispatch(LessonRuntimeEvent.ContinueRequested)
                        applyTransition()
                    },
                )
            }
        }
    }
}
