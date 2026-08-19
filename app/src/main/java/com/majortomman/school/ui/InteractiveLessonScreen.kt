package com.majortomman.school.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.cloud.InstalledCourse
import com.majortomman.school.learning.course.CourseLesson

internal val InteractiveBlack = Color.Transparent
internal val InteractivePanel = Color.Transparent
internal val InteractiveWhite = Color(0xFFF5F7FA)
internal val InteractiveMuted = InteractiveWhite.copy(alpha = 0.52f)
internal val InteractiveLine = InteractiveWhite.copy(alpha = 0.12f)
internal val InteractiveBlue = Color(0xFF58C4DD)
internal val InteractiveYellow = Color(0xFFF4D35E)
internal val InteractiveGreen = Color(0xFF83C167)
internal val InteractiveRed = Color(0xFFFC6255)
internal val InteractivePurple = Color(0xFF9A72AC)

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
    var pageIndex by rememberSaveable(lesson.id, course.contentVersion) { mutableIntStateOf(0) }
    if (pageIndex !in pages.indices) pageIndex = 0
    val textbookReference = lesson.references.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SchoolCompactTopBar(
            title = lesson.title,
            onBack = onBack,
            actionLabel = if (textbookReference != null) "PDF" else null,
            onAction = { textbookReference?.let { onOpenTextbook(it.pageStart) } },
            actionEnabled = textbookReference != null && course.pdfFile.isFile,
        )
        SchoolDivider(color = InteractiveLine)
        AnimatedContent(
            targetState = pageIndex,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (targetState > initialState) {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(260)) { it / 7 }) togetherWith
                        (fadeOut(tween(120)) + slideOutHorizontally(tween(220)) { -it / 8 })
                } else {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(260)) { -it / 7 }) togetherWith
                        (fadeOut(tween(120)) + slideOutHorizontally(tween(220)) { it / 8 })
                }
            },
            label = "lessonPages",
        ) { visibleIndex ->
            val page = pages[visibleIndex]
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = 24.dp),
            ) {
                LessonPresentationPageContent(page, lesson)
                Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
            }
        }
        SchoolDivider(color = InteractiveLine)
        LessonPagerFooter(
            pageIndex = pageIndex,
            pageCount = pages.size,
            hasNextLesson = nextLessonTitle != null,
            onPrevious = { if (pageIndex > 0) pageIndex -= 1 },
            onNext = { if (pageIndex < pages.lastIndex) pageIndex += 1 else onComplete() },
        )
    }
}

@Composable
private fun LessonPresentationPageContent(page: LessonPresentationPage, lesson: CourseLesson) {
    when (page) {
        is LessonPresentationPage.Overview -> {
            SchoolSectionLabel("学习目标", color = InteractiveYellow)
            Spacer(Modifier.height(14.dp))
            Text("这一课先抓住这些目标", color = InteractiveWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            page.goals.forEachIndexed { index, goal ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                    Text("${index + 1}", color = InteractiveYellow, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(goal, modifier = Modifier.weight(1f).padding(start = 12.dp), color = InteractiveWhite.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is LessonPresentationPage.Teaching -> AuthoredTeachingPageContent(page.steps, lesson)
        is LessonPresentationPage.Summary -> {
            SchoolSectionLabel("这一课记住", color = InteractiveYellow)
            Spacer(Modifier.height(14.dp))
            page.items.forEach { item ->
                Text("— $item", color = InteractiveWhite.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
        is LessonPresentationPage.Practice -> AuthoredPracticePage(page.practice, page.number, page.total)
    }
}

@Composable
private fun LessonPagerFooter(
    pageIndex: Int,
    pageCount: Int,
    hasNextLesson: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("${pageIndex + 1} / $pageCount", modifier = Modifier.fillMaxWidth(), color = InteractiveMuted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (pageIndex > 0) {
                    Text("← 上一页", modifier = Modifier.clickable(onClick = onPrevious).padding(vertical = 8.dp), color = InteractiveMuted, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                } else {
                    Text("教材仅作参考", color = InteractiveMuted.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                }
            }
            Text(
                text = if (pageIndex < pageCount - 1) "下一页 →" else if (hasNextLesson) "完成并继续 →" else "完成 →",
                modifier = Modifier.weight(1f).clickable(onClick = onNext).padding(vertical = 8.dp),
                color = InteractiveBlue,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
