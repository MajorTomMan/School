package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.data.DailyPlan
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.MasteryStatus

@Composable
fun TodayScreen(
    plan: DailyPlan,
    lessons: List<Lesson>,
    courseTitle: String,
    onStartLesson: (String) -> Unit,
    onOpenPath: () -> Unit,
) {
    val lesson = lessons.firstOrNull { it.id == plan.newLessonId } ?: return
    val mastered = lessons.count { it.status == MasteryStatus.MASTERED }
    val progress = if (lessons.isEmpty()) 0f else mastered.toFloat() / lessons.size.toFloat()
    val review = lessons.firstOrNull { it.status == MasteryStatus.NEEDS_REVIEW } ?: lessons.firstOrNull { it.status == MasteryStatus.MASTERED }
    val currentIndex = lessons.indexOfFirst { it.id == lesson.id }
    val next = lessons.getOrNull(currentIndex + 1)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SchoolBrandSlash(modifier = Modifier.height(24.dp).width(7.dp))
            Text("School", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text("初中学习 · 更高效的自己", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        Text("学习", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("继续保持专注，把今天的一小步学扎实。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(30.dp))
        SchoolSectionLabel("继续学习")
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 18.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("数学", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(courseTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(lesson.title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                )
                Text("已完成 $mastered / ${lessons.size} 节", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        SchoolPrimaryAction("继续学习  →", onClick = { onStartLesson(lesson.id) })

        Spacer(Modifier.height(30.dp))
        SchoolSectionLabel("今天建议")
        Spacer(Modifier.height(8.dp))
        review?.let { LearningSuggestionRow("复习", it.title, "回顾概念，巩固基础", "约 10 分钟") { onStartLesson(it.id) } }
        LearningSuggestionRow("练习", lesson.title, "完成当前知识点的应用练习", "约 15 分钟") { onStartLesson(lesson.id) }
        next?.let { LearningSuggestionRow("阅读", it.title, "为下一节内容做准备", "约 10 分钟") { onStartLesson(it.id) } }

        Spacer(Modifier.height(30.dp))
        SchoolSectionLabel("我的课程")
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPath).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(courseTitle, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${lessons.size} 节 · 已完成 $mastered 节", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("查看课程  ›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
    }
}

@Composable
private fun LearningSuggestionRow(kind: String, title: String, description: String, time: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(kind.take(1), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("$kind：$title", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
    SchoolDivider()
}
