package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.assessment.contract.CourseAssessmentQuestionSet
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.assessment.persistence.AssessmentPracticeState
import com.majortomman.school.learning.assessment.persistence.AssessmentPracticeStatus
import com.majortomman.school.learning.assessment.persistence.AssessmentProgressStore
import com.majortomman.school.learning.cloud.InstalledCourse

@Composable
fun PracticeScreen(
    course: InstalledCourse?,
    onOpenCourses: () -> Unit,
) {
    var openedQuestionSetId by rememberSaveable(course?.id) { mutableStateOf<String?>(null) }
    var refreshVersion by rememberSaveable(course?.id) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val progressStore = remember(context) { AssessmentProgressStore.create(context) }
    val assessmentDocument = course?.assessments
    val knowledgeDocument = course?.assessmentKnowledgePoints
    var statuses by remember(course?.id, course?.contentVersion) {
        mutableStateOf<Map<QuestionSetId, AssessmentPracticeStatus>>(emptyMap())
    }
    val openedQuestionSet = assessmentDocument?.questionSets?.firstOrNull {
        it.id.value == openedQuestionSetId
    }

    LaunchedEffect(course?.id, course?.contentVersion, assessmentDocument, refreshVersion) {
        statuses = if (course == null || assessmentDocument == null) {
            emptyMap()
        } else {
            progressStore.practiceStatuses(
                courseId = course.id,
                contentRevision = course.contentVersion.toString(),
                questionSetIds = assessmentDocument.questionSets.map { it.id },
            )
        }
    }

    if (course != null && assessmentDocument != null && knowledgeDocument != null && openedQuestionSet != null) {
        AssessmentSessionScreen(
            courseId = course.id,
            contentRevision = course.contentVersion.toString(),
            questionSet = openedQuestionSet,
            assetFiles = course.assessmentAssetFiles(),
            knowledgePoints = knowledgeDocument.knowledgePoints.associateBy { it.id },
            onBack = {
                openedQuestionSetId = null
                refreshVersion++
            },
            onFinished = {
                openedQuestionSetId = null
                refreshVersion++
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        SchoolPageTitle("练习", eyebrow = "SCHOOL / PRACTICE")
        Text(
            "练习来自当前课程的正式题组。未完成会话会自动恢复，完成后的结果继续用于知识掌握与复习建议。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(28.dp))

        when {
            course == null -> {
                SchoolSectionLabel("还没有当前课程")
                Spacer(Modifier.height(12.dp))
                Text(
                    "先选择一本课程，再开始与课程内容对应的练习。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(18.dp))
                SchoolPrimaryAction("前往课程  →", onClick = onOpenCourses)
            }

            assessmentDocument == null || knowledgeDocument == null -> {
                SchoolSectionLabel("当前课程暂无练习包")
                Spacer(Modifier.height(12.dp))
                Text(
                    "这本课程没有安装 assessments.json / knowledge-points.json。School 不会用 APK 内置题库补齐课程内容。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> {
                val inProgressCount = statuses.values.count { it.state == AssessmentPracticeState.IN_PROGRESS }
                val completedCount = statuses.values.count { it.state == AssessmentPracticeState.COMPLETED }
                SchoolSectionLabel("课程练习")
                Spacer(Modifier.height(8.dp))
                if (statuses.isNotEmpty()) {
                    Text(
                        "进行中 $inProgressCount · 已完成 $completedCount / ${assessmentDocument.questionSets.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                assessmentDocument.questionSets.forEach { questionSet ->
                    val placement = assessmentDocument.placements.firstOrNull { placement ->
                        questionSet.id in placement.questionSetIds
                    }
                    PracticeSetRow(
                        questionSet = questionSet,
                        sectionLabel = placement?.sectionId?.let { course.sectionTitle(it) },
                        status = statuses[questionSet.id],
                        onClick = { openedQuestionSetId = questionSet.id.value },
                    )
                    SchoolDivider()
                }
            }
        }

        Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
    }
}

@Composable
private fun PracticeSetRow(
    questionSet: CourseAssessmentQuestionSet,
    sectionLabel: String?,
    status: AssessmentPracticeStatus?,
    onClick: () -> Unit,
) {
    val state = status?.state ?: AssessmentPracticeState.NOT_STARTED
    val action = when (state) {
        AssessmentPracticeState.NOT_STARTED -> "开始  ›"
        AssessmentPracticeState.IN_PROGRESS -> "继续  ›"
        AssessmentPracticeState.COMPLETED -> "再练  ›"
    }
    val resultText = if (
        state == AssessmentPracticeState.COMPLETED &&
        status?.finalCorrectCount != null &&
        status.totalQuestionCount != null
    ) {
        " · 上次 ${status.finalCorrectCount}/${status.totalQuestionCount}"
    } else {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when (state) {
                AssessmentPracticeState.NOT_STARTED -> "✎"
                AssessmentPracticeState.IN_PROGRESS -> "●"
                AssessmentPracticeState.COMPLETED -> "✓"
            },
            color = when (state) {
                AssessmentPracticeState.NOT_STARTED -> MaterialTheme.colorScheme.primary
                AssessmentPracticeState.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
                AssessmentPracticeState.COMPLETED -> MaterialTheme.colorScheme.tertiary
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                questionSet.title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append("${questionSet.questions.size} 题")
                    sectionLabel?.let { append(" · ").append(it) }
                    append(resultText)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            action,
            color = if (state == AssessmentPracticeState.IN_PROGRESS) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun InstalledCourse.sectionTitle(sectionId: String): String? =
    document.chapters.asSequence()
        .flatMap { it.sections.asSequence() }
        .firstOrNull { it.id == sectionId }
        ?.title
