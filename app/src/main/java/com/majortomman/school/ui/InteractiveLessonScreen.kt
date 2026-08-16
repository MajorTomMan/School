package com.majortomman.school.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.material.InstalledMaterialPack
import com.majortomman.school.learning.cloud.CloudCourseRepository

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

/** Entry point for the authored lesson-centric course runtime. */
@Composable
fun InteractiveLessonScreen(
    lesson: Lesson,
    spec: InteractiveLessonSpec,
    installedMaterial: InstalledMaterialPack,
    nextLessonTitle: String?,
    onOpenTextbook: (Int) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    require(spec.kind == InteractiveLessonKind.CLOUD_COURSE)
    val revision by CloudCourseRepository.revision.collectAsState()
    val authoredLesson = remember(lesson.id, lesson.title, revision) { CloudCourseRepository.lessonFor(lesson.id, lesson.title) }
    if (authoredLesson == null) {
        AuthoredCourseUnavailable(lesson.title, onBack)
        return
    }

    val pages = remember(authoredLesson, revision) { composeLessonPresentation(authoredLesson) }
    var pageIndex by rememberSaveable(authoredLesson.id, revision) { mutableIntStateOf(0) }
    if (pageIndex !in pages.indices) pageIndex = 0
    val textbookReference = authoredLesson.references.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SchoolCompactTopBar(
            title = authoredLesson.title,
            onBack = onBack,
            actionLabel = if (textbookReference != null) "PDF" else null,
            onAction = { textbookReference?.let { onOpenTextbook(it.pageStart) } },
            actionEnabled = textbookReference != null && installedMaterial.pdfFile.isFile,
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = 24.dp),
            ) {
                LessonPresentationPageContent(page = page, lesson = authoredLesson)
                Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
            }
        }

        SchoolDivider(color = InteractiveLine)
        LessonPagerFooter(
            pageIndex = pageIndex,
            pageCount = pages.size,
            hasNextLesson = nextLessonTitle != null,
            onPrevious = { if (pageIndex > 0) pageIndex -= 1 },
            onNext = {
                if (pageIndex < pages.lastIndex) pageIndex += 1 else onComplete()
            },
        )
    }
}

@Composable
private fun LessonPresentationPageContent(
    page: LessonPresentationPage,
    lesson: com.majortomman.school.learning.course.CourseLesson,
) {
    when (page) {
        is LessonPresentationPage.Overview -> {
            SchoolSectionLabel("学习目标", color = InteractiveYellow)
            Spacer(Modifier.height(14.dp))
            Text("这一课先抓住这些目标", color = InteractiveWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            page.goals.forEachIndexed { index, goal ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                    Text("${index + 1}", color = InteractiveYellow, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = goal,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        color = InteractiveWhite.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        is LessonPresentationPage.Teaching -> AuthoredTeachingPageContent(page.steps, lesson)
        is LessonPresentationPage.Summary -> {
            SchoolSectionLabel("这一课记住", color = InteractiveYellow)
            Spacer(Modifier.height(14.dp))
            page.items.forEach { item ->
                Text(
                    text = "— $item",
                    color = InteractiveWhite.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${pageIndex + 1} / $pageCount",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (pageIndex > 0) {
                    Text(
                        text = "← 上一页",
                        modifier = Modifier.clickable(onClick = onPrevious).padding(vertical = 8.dp),
                        color = InteractiveMuted,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                    )
                } else {
                    Text(
                        text = "教材仅作参考",
                        color = InteractiveMuted.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                    )
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

@Composable
private fun AuthoredCourseUnavailable(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        SchoolCompactTopBar(title = title, onBack = onBack, modifier = Modifier.fillMaxWidth())
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("新版课程内容尚未安装", color = InteractiveBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("请在设置的课程页重新下载当前教材。", color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Text("旧版按教材页生成的课程包不会再被兼容。", color = InteractiveMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun SectionTitle(title: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(3.dp).weight(0.12f).background(color))
        Text(title, modifier = Modifier.weight(0.88f), color = InteractiveWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun InteractiveAction(label: String, color: Color, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier = modifier.heightIn(min = SchoolUiMetrics.minTouchHeight).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) color else InteractiveMuted.copy(alpha = 0.45f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (enabled) 2.dp else 1.dp).background(if (enabled) color.copy(alpha = 0.78f) else InteractiveLine))
    }
}
