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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.advisor.ReviewAdvice
import com.majortomman.school.learning.assessment.persistence.AssessmentPracticeState
import com.majortomman.school.learning.assessment.persistence.AssessmentPracticeStatus
import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.knowledge.KnowledgePointState
import kotlin.math.roundToInt

@Composable
fun LearningRecordScreen(
    courseTitle: String,
    completedLessonCount: Int,
    totalLessonCount: Int,
    recentLessonTitle: String?,
    practiceStatuses: Collection<AssessmentPracticeStatus>,
    knowledgeStates: List<KnowledgePointState>,
    reviewQueue: List<ReviewAdvice>,
    knowledgePointNames: Map<KnowledgePointId, String>,
    onBack: () -> Unit,
) {
    val lessonProgress = if (totalLessonCount == 0) 0f else completedLessonCount.toFloat() / totalLessonCount
    val completedPractice = practiceStatuses.count { it.state == AssessmentPracticeState.COMPLETED }
    val inProgressPractice = practiceStatuses.count { it.state == AssessmentPracticeState.IN_PROGRESS }
    val observedKnowledge = knowledgeStates.filter { it.observed && it.masteryScore != null }
    val averageMastery = observedKnowledge.mapNotNull { it.masteryScore }.average().takeIf { !it.isNaN() }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        Text(
            "‹ 我的",
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        SchoolPageTitle("学习记录", eyebrow = "SCHOOL / RECORD")
        Text(courseTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(28.dp))
        SchoolSectionLabel("课程进度")
        Spacer(Modifier.height(14.dp))
        RecordMetricRow("已完成课程", "$completedLessonCount / $totalLessonCount 节")
        LinearProgressIndicator(
            progress = { lessonProgress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
        )
        RecordMetricRow("最近学习", recentLessonTitle ?: "暂无记录")

        Spacer(Modifier.height(28.dp))
        SchoolSectionLabel("练习")
        Spacer(Modifier.height(10.dp))
        RecordMetricRow("已完成题组", completedPractice.toString())
        RecordMetricRow("进行中题组", inProgressPractice.toString())
        val latestCompleted = practiceStatuses
            .filter { it.state == AssessmentPracticeState.COMPLETED && it.completedAtEpochMillis != null }
            .maxByOrNull { it.completedAtEpochMillis ?: Long.MIN_VALUE }
        if (latestCompleted?.finalCorrectCount != null && latestCompleted.totalQuestionCount != null) {
            RecordMetricRow(
                "最近一次结果",
                "${latestCompleted.finalCorrectCount} / ${latestCompleted.totalQuestionCount}",
            )
        }

        Spacer(Modifier.height(28.dp))
        SchoolSectionLabel("知识掌握")
        Spacer(Modifier.height(10.dp))
        RecordMetricRow("已观察知识点", observedKnowledge.size.toString())
        RecordMetricRow(
            "平均掌握度",
            averageMastery?.let { "${(it * 100).roundToInt()}%" } ?: "暂无数据",
        )

        if (reviewQueue.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SchoolSectionLabel("建议复习")
            Spacer(Modifier.height(8.dp))
            reviewQueue.forEachIndexed { index, advice ->
                val name = knowledgePointNames[advice.knowledgePointId] ?: advice.knowledgePointId.value
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "%02d".format(index + 1),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "掌握 ${(advice.masteryScore * 100).roundToInt()}% · 错误尝试 ${advice.wrongAttemptCount} 次 · 证据 ${advice.evidenceCount} 条",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                SchoolDivider()
            }
        }

        Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
    }
}

@Composable
private fun RecordMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
