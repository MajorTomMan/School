package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.cloud.InstalledCourse
import com.majortomman.school.learning.course.CourseLesson

@Composable
fun InteractiveLessonScreen(
    course: InstalledCourse,
    lesson: CourseLesson,
    nextLessonTitle: String?,
    onOpenTextbook: (Int) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val pages = remember(lesson) { composeLessonPresentation(lesson) }
    val textbookReference = lesson.references.firstOrNull()

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
            Text(
                "MATHEMATICS / JUNIOR HIGH / SCHOOL",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.padding(top = 5.dp))
            Text(
                lesson.title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                course.title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall,
            )
            lesson.goals.firstOrNull()?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.padding(top = 7.dp))
                Text(it, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.padding(top = 14.dp))
            pages.forEachIndexed { index, page ->
                if (index > 0) {
                    Spacer(Modifier.padding(top = 14.dp))
                    SchoolDivider()
                    Spacer(Modifier.padding(top = 14.dp))
                }
                LessonPresentationPageContent(page, lesson)
            }
            Spacer(Modifier.padding(top = SchoolUiMetrics.pageBottom))
        }

        SchoolDivider()
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SchoolPrimaryAction(
                label = if (nextLessonTitle != null) "完成并继续  →" else "完成  →",
                onClick = onComplete,
            )
        }
    }
}

@Composable
private fun LessonPresentationPageContent(page: LessonPresentationPage, lesson: CourseLesson) {
    when (page) {
        is LessonPresentationPage.Overview -> {
            SchoolSectionLabel("学习目标")
            Spacer(Modifier.padding(top = 6.dp))
            page.goals.forEachIndexed { index, goal ->
                Text(
                    "${index + 1}.  $goal",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        is LessonPresentationPage.Teaching -> AuthoredTeachingPageContent(page.steps, lesson)

        is LessonPresentationPage.Summary -> {
            SchoolSectionLabel("小结")
            Spacer(Modifier.padding(top = 6.dp))
            page.items.forEachIndexed { index, item ->
                Text(
                    "${index + 1}.  $item",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        is LessonPresentationPage.Practice -> AuthoredPracticePage(page.practice, page.number, page.total)
    }
}
