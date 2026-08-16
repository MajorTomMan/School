package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.course.CourseCheckpoint
import com.majortomman.school.learning.course.CourseExample
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseFormula
import com.majortomman.school.learning.course.CourseKeyIdea
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseSummaryStep
import com.majortomman.school.learning.course.CourseVisualizationStep
import com.majortomman.school.visualization.SchoolVisualization

@Composable
internal fun AuthoredTeachingPageContent(steps: List<CourseStep>, lesson: CourseLesson) {
    steps.forEachIndexed { index, step ->
        if (index > 0) Spacer(Modifier.height(SchoolUiMetrics.sectionGap))
        AuthoredStep(step)
    }
}

@Composable
internal fun AuthoredPracticePage(practice: CoursePractice, number: Int, total: Int) {
    var revealed by rememberSaveable(practice.id) { mutableStateOf(false) }
    SchoolSectionLabel("练习 $number / $total", color = InteractiveYellow)
    Spacer(Modifier.height(14.dp))
    Text(
        text = practice.prompt,
        color = InteractiveWhite,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = if (revealed) "收起答案与解析" else "查看答案与解析",
        modifier = Modifier.clickable { revealed = !revealed }.padding(vertical = 10.dp),
        color = InteractiveBlue,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
    )
    if (revealed) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveGreen.copy(alpha = 0.72f)))
        Spacer(Modifier.height(12.dp))
        Text("答案：${practice.answer}", color = InteractiveGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        practice.analysis.forEach { item ->
            Text(
                text = "— $item",
                color = InteractiveMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AuthoredStep(step: CourseStep) {
    when (step) {
        is CourseExplanation -> {
            step.title?.let {
                OpenSectionTitle(it, InteractiveBlue)
                Spacer(Modifier.height(10.dp))
            }
            Text(step.text, color = InteractiveWhite.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
        }
        is CourseQuestion -> {
            OpenSectionTitle("先想一想", InteractiveYellow)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            step.hint?.takeIf { it.isNotBlank() }?.let {
                Text("提示：$it", color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            }
        }
        is CourseKeyIdea -> {
            Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveBlue.copy(alpha = 0.72f)))
            Spacer(Modifier.height(10.dp))
            step.title?.let { Text(it, color = InteractiveBlue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            Text(step.text, color = InteractiveWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
        }
        is CourseFormula -> {
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
            SchoolFormula(
                latex = step.expression,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                color = InteractiveYellow,
                style = MaterialTheme.typography.headlineMedium,
            )
            step.note?.let {
                Text(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
        }
        is CourseExample -> {
            OpenSectionTitle(step.title, InteractiveBlue)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            step.steps.forEachIndexed { index, item ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Text("${index + 1}", color = InteractiveYellow, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text(item, modifier = Modifier.weight(1f), color = InteractiveWhite.copy(alpha = 0.82f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("答案：${step.answer}", color = InteractiveYellow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp))
        }
        is CourseVisualizationStep -> Box(Modifier.fillMaxWidth().height(380.dp)) {
            SchoolVisualization(step.visualization, Modifier.fillMaxWidth())
        }
        is CourseCheckpoint -> {
            OpenSectionTitle("检查一下", InteractiveGreen)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, style = MaterialTheme.typography.titleLarge)
            Text("参考：${step.expectedAnswer}", color = InteractiveGreen, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            Text(step.explanation, color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        is CourseSummaryStep -> Text(step.text, color = InteractiveWhite.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OpenSectionTitle(title: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(2.dp).weight(0.08f).background(color))
        Text(title, modifier = Modifier.weight(0.92f), color = InteractiveWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}
