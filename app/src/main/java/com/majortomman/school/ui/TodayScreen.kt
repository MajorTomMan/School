package com.majortomman.school.ui

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.data.DailyPlan
import com.majortomman.school.data.Lesson

@Composable
fun TodayScreen(
    plan: DailyPlan,
    lessons: List<Lesson>,
    onStartLesson: (String) -> Unit,
    onOpenPath: () -> Unit,
) {
    val lesson = lessons.firstOrNull { it.id == plan.newLessonId } ?: return
    val lessonIndex = lessons.indexOfFirst { it.id == lesson.id }.coerceAtLeast(0)
    val progress = (lessonIndex + 1f) / lessons.size.coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("今天", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.labelLarge)

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(lesson.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text(lesson.subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), style = MaterialTheme.typography.bodyLarge)
        }

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${lessonIndex + 1} / ${lessons.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text("${plan.estimatedMinutes} 分钟", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                text = "继续 →",
                modifier = Modifier.clickable { onStartLesson(lesson.id) }.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${plan.reviewItems.size} 项复习", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "查看路径",
                    modifier = Modifier.clickable(onClick = onOpenPath).padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
