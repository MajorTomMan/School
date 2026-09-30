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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.assessment.contract.CourseAssessmentQuestionSet
import com.majortomman.school.learning.cloud.InstalledCourse

@Composable
fun PracticeScreen(
    course: InstalledCourse?,
    onOpenCourses: () -> Unit,
) {
    var openedQuestionSetId by rememberSaveable(course?.id) { mutableStateOf<String?>(null) }
    val assessmentDocument = course?.assessments
    val knowledgeDocument = course?.assessmentKnowledgePoints
    val openedQuestionSet = assessmentDocument?.questionSets?.firstOrNull {
        it.id.value == openedQuestionSetId
    }

    if (course != null && assessmentDocument != null && knowledgeDocument != null && openedQuestionSet != null) {
        AssessmentSessionScreen(
            courseId = course.id,
            contentRevision = course.contentVersion.toString(),
            questionSet = openedQuestionSet,
            assetFiles = course.assessmentAssetFiles(),
            knowledgePoints = knowledgeDocument.knowledgePoints.associateBy { it.id },
            onBack = { openedQuestionSetId = null },
            onFinished = { openedQuestionSetId = null },
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
            "练习来自课程包中的正式题组；作答事实、结算和知识掌握由统一 Assessment Runtime 记录。",
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
                SchoolSectionLabel("课程练习")
                Spacer(Modifier.height(8.dp))
                assessmentDocument.questionSets.forEach { questionSet ->
                    val placement = assessmentDocument.placements.firstOrNull { placement ->
                        questionSet.id in placement.questionSetIds
                    }
                    PracticeSetRow(
                        questionSet = questionSet,
                        sectionLabel = placement?.sectionId?.let { course.sectionTitle(it) },
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "✎",
            color = MaterialTheme.colorScheme.primary,
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
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("开始  ›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

private fun InstalledCourse.sectionTitle(sectionId: String): String? =
    document.chapters.asSequence()
        .flatMap { it.sections.asSequence() }
        .firstOrNull { it.id == sectionId }
        ?.title
