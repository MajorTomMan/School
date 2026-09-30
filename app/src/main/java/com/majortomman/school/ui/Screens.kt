package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.data.Lesson
import com.majortomman.school.learning.progress.LessonProgressStatus

@Composable
fun CoursePathScreen(
    courseTitle: String,
    lessons: List<Lesson>,
    onOpenLesson: (String) -> Unit,
    onChooseCourse: () -> Unit,
) {
    val masteredCount = lessons.count { it.status == LessonProgressStatus.COMPLETED }
    val current = lessons.firstOrNull { it.status == LessonProgressStatus.IN_PROGRESS }
        ?: lessons.firstOrNull { it.status == MasteryStatus.NEEDS_REVIEW }
        ?: lessons.firstOrNull { it.status == LessonProgressStatus.NOT_STARTED }
    val progress = if (lessons.isEmpty()) 0f else masteredCount.toFloat() / lessons.size.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding(),
        contentPadding = PaddingValues(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        item {
            SchoolPageTitle(courseTitle, eyebrow = "SCHOOL / COURSE")
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("学习进度  $masteredCount / ${lessons.size} 节", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
            )
            current?.let {
                Spacer(Modifier.height(18.dp))
                SchoolPrimaryAction("继续学习  ${it.title}  →", onClick = { onOpenLesson(it.id) })
            }
            Spacer(Modifier.height(10.dp))
            Text("切换课程", modifier = Modifier.clickable(onClick = onChooseCourse).padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(24.dp))
            SchoolSectionLabel("课程目录")
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
            CourseLessonRow(index + 1, lesson, onClick = { onOpenLesson(lesson.id) })
        }
        item { Spacer(Modifier.height(SchoolUiMetrics.pageBottom)) }
    }
}

@Composable
private fun CourseLessonRow(number: Int, lesson: Lesson, onClick: () -> Unit) {
    val marker = when (lesson.status) {
        LessonProgressStatus.COMPLETED -> "✓"
        LessonProgressStatus.IN_PROGRESS -> "●"
        LessonProgressStatus.NOT_STARTED -> "○"
    }
    val markerColor = when (lesson.status) {
        LessonProgressStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        LessonProgressStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        LessonProgressStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(marker, color = markerColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("$number", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(lesson.title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = if (lesson.status == LessonProgressStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Medium)
            lesson.subtitle.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
        Text(
            when (lesson.status) {
                LessonProgressStatus.COMPLETED -> "已完成"
                LessonProgressStatus.IN_PROGRESS -> "当前  ›"
                        LessonProgressStatus.NOT_STARTED -> "›"
            },
            color = markerColor,
            style = MaterialTheme.typography.labelMedium,
        )
    }
    SchoolDivider()
}
