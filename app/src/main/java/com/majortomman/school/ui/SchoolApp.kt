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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.AttemptRecord
import com.majortomman.school.data.BackgroundMode
import com.majortomman.school.data.DailyPlan
import com.majortomman.school.data.DisplayPreferences
import com.majortomman.school.data.DisplaySettings
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.MasteryStatus
import com.majortomman.school.data.PreferencesRepository
import com.majortomman.school.data.ScheduledReview
import com.majortomman.school.data.math.MathQuestionBankRepository
import com.majortomman.school.learning.cloud.CourseLibraryRepository
import com.majortomman.school.learning.cloud.InstalledCourse
import com.majortomman.school.learning.course.CourseLesson
import kotlinx.coroutines.launch

private val NavigationBlack = Color.Transparent
private val NavigationWhite = Color(0xFFF5F5F7)
private val NavigationBlue = Color(0xFF0A84FF)

private enum class MainTab(val label: String) {
    SUBJECTS("课程"),
    TODAY("今天"),
    PATH("路径"),
    BANK("题库"),
    REVIEW("复习"),
    LAB("验证"),
    SETTINGS("设置"),
}

@Composable
fun SchoolApp(
    repository: PreferencesRepository,
    mathQuestionRepository: MathQuestionBankRepository,
    initialCourseId: String? = null,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.SUBJECTS.name) }
    var activeCourseId by rememberSaveable { mutableStateOf(initialCourseId) }
    var openedLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedTextbookPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var readingRangeStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var readingRangeEnd by rememberSaveable { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val progress by repository.learningProgress.collectAsState(initial = LearningProgress())
    val aiSettings by repository.aiSettings.collectAsState(initial = AiSettings())
    val recentAttempts by repository.recentAttempts.collectAsState(initial = emptyList<AttemptRecord>())
    val reviewQueue by repository.reviewQueue.collectAsState(initial = emptyList<ScheduledReview>())
    val displaySettings by DisplayPreferences.state.collectAsState(initial = DisplaySettings())
    val libraryState by CourseLibraryRepository.state.collectAsState()
    val bottomBarBackground = when (displaySettings.backgroundMode) {
        BackgroundMode.PRESET -> Color(displaySettings.backgroundPreset.argb)
        BackgroundMode.CUSTOM -> Color.Black.copy(alpha = 0.18f)
    }

    val activeCourse = libraryState.course(activeCourseId)
    val lessons = activeCourse?.lessons.orEmpty().mapIndexed { index, lesson ->
        lesson.toUiLesson(progress.lessonStatuses[lesson.id] ?: if (index == 0) MasteryStatus.LEARNING else MasteryStatus.NOT_STARTED)
    }
    val currentLesson = lessons.firstOrNull { it.status == MasteryStatus.LEARNING }
        ?: lessons.firstOrNull { it.status == MasteryStatus.NEEDS_REVIEW }
        ?: lessons.firstOrNull { it.status == MasteryStatus.NOT_STARTED }
        ?: lessons.firstOrNull()
    val dailyPlan = currentLesson?.let { DailyPlan(it.id, emptyList(), it.estimatedMinutes) }
    val selectedTab = MainTab.valueOf(selectedTabName)
    val openedCourseLesson = activeCourse?.lessons?.firstOrNull { it.id == openedLessonId }
    val openedLessonIndex = activeCourse?.lessons?.indexOfFirst { it.id == openedLessonId } ?: -1
    val nextCourseLesson = activeCourse?.lessons?.getOrNull(openedLessonIndex + 1).takeIf { openedLessonIndex >= 0 }
    val openedTextbook = libraryState.course(openedCourseId)
    val readingRange = if (readingRangeStart != null && readingRangeEnd != null) readingRangeStart!!..readingRangeEnd!! else null

    LaunchedEffect(libraryState.courses.map { it.id }) {
        if (activeCourseId != null && activeCourse == null) {
            activeCourseId = null
            openedLessonId = null
        }
        if (openedCourseId != null && openedTextbook == null) closeTextbook(
            onCourse = { openedCourseId = it },
            onPage = { openedTextbookPage = it },
            onRangeStart = { readingRangeStart = it },
            onRangeEnd = { readingRangeEnd = it },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = openedCourseLesson,
            transitionSpec = {
                if (targetState != null) {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(420)) { it / 7 }) togetherWith
                        (fadeOut(tween(170)) + slideOutHorizontally(tween(280)) { -it / 9 })
                } else {
                    (fadeIn(tween(280)) + slideInHorizontally(tween(400)) { -it / 8 }) togetherWith
                        (fadeOut(tween(170)) + slideOutHorizontally(tween(280)) { it / 9 })
                }
            },
            label = "appNavigation",
        ) { lesson ->
            if (lesson != null && activeCourse != null) {
                InteractiveLessonScreen(
                    course = activeCourse,
                    lesson = lesson,
                    nextLessonTitle = nextCourseLesson?.title,
                    onOpenTextbook = { printedPage ->
                        openedCourseId = activeCourse.id
                        openedTextbookPage = printedPage
                        val range = activeCourse.readingRange(lesson)
                        readingRangeStart = range?.first
                        readingRangeEnd = range?.last
                    },
                    onBack = { openedLessonId = null },
                    onComplete = {
                        val nextId = nextCourseLesson?.id
                        scope.launch { repository.finishLessonAndStartNext(lesson.id, nextId) }
                        if (nextCourseLesson != null) {
                            openedLessonId = nextCourseLesson.id
                        } else {
                            openedLessonId = null
                            selectedTabName = MainTab.PATH.name
                        }
                    },
                )
            } else {
                Scaffold(
                    containerColor = NavigationBlack,
                    bottomBar = {
                        MinimalBottomBar(selectedTab, bottomBarBackground) { selectedTabName = it.name }
                    },
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (fadeIn(tween(260)) + slideInHorizontally(tween(360)) { it / 14 }) togetherWith
                                    (fadeOut(tween(150)) + slideOutHorizontally(tween(260)) { -it / 14 })
                            },
                            label = "mainTabs",
                        ) { tab ->
                            when (tab) {
                                MainTab.SUBJECTS -> SubjectTextbookCenterScreen(
                                    libraryState = libraryState,
                                    onEnterCourse = { course ->
                                        activeCourseId = course.id
                                        openedLessonId = null
                                        selectedTabName = MainTab.TODAY.name
                                    },
                                    onOpenTextbook = { course, page ->
                                        openedCourseId = course.id
                                        openedTextbookPage = page
                                        readingRangeStart = null
                                        readingRangeEnd = null
                                    },
                                )
                                MainTab.TODAY -> {
                                    if (activeCourse == null || dailyPlan == null || lessons.isEmpty()) {
                                        NoActiveTextbookScreen { selectedTabName = MainTab.SUBJECTS.name }
                                    } else {
                                        TodayScreen(plan = dailyPlan, lessons = lessons, onStartLesson = { openedLessonId = it }, onOpenPath = { selectedTabName = MainTab.PATH.name })
                                    }
                                }
                                MainTab.PATH -> {
                                    if (activeCourse == null || lessons.isEmpty()) {
                                        NoActiveTextbookScreen { selectedTabName = MainTab.SUBJECTS.name }
                                    } else {
                                        CoursePathScreen(lessons = lessons, onOpenLesson = { openedLessonId = it })
                                    }
                                }
                                MainTab.BANK -> MathQuestionBankScreen(
                                    repository = mathQuestionRepository,
                                    textbook = activeCourse,
                                    onOpenSubjects = { selectedTabName = MainTab.SUBJECTS.name },
                                    onOpenTextbook = { page ->
                                        activeCourse?.let { course ->
                                            openedCourseId = course.id
                                            openedTextbookPage = page
                                            readingRangeStart = null
                                            readingRangeEnd = null
                                        }
                                    },
                                )
                                MainTab.REVIEW -> MinimalRoomReviewScreen(
                                    fallbackItems = emptyList(),
                                    progress = progress,
                                    scheduledReviews = reviewQueue,
                                    recentAttempts = recentAttempts,
                                    onOpenLesson = { lessonId -> if (activeCourse?.lessons?.any { it.id == lessonId } == true) openedLessonId = lessonId },
                                )
                                MainTab.LAB -> VerificationHubScreen()
                                MainTab.SETTINGS -> MaterialSettingsScreen(
                                    settings = aiSettings,
                                    onSave = { updated -> scope.launch { repository.saveAiSettings(updated) } },
                                    onOpenSubjects = { selectedTabName = MainTab.SUBJECTS.name },
                                    onClearProgress = { scope.launch { repository.clearLearningProgress() } },
                                )
                            }
                        }
                    }
                }
            }
        }

        val textbookPage = openedTextbookPage
        if (textbookPage != null && openedTextbook != null) {
            PdfTextbookScreen(
                course = openedTextbook,
                initialPrintedPage = textbookPage,
                readingRange = readingRange,
                onBack = {
                    openedCourseId = null
                    openedTextbookPage = null
                    readingRangeStart = null
                    readingRangeEnd = null
                },
            )
        }
    }
}

private fun CourseLesson.toUiLesson(status: MasteryStatus): Lesson {
    val start = references.minOfOrNull { it.pageStart } ?: 1
    val end = references.maxOfOrNull { it.pageEnd } ?: start
    return Lesson(
        id = id,
        title = title,
        subtitle = goals.firstOrNull().orEmpty(),
        estimatedMinutes = 18,
        textbookPages = start..end,
        status = status,
        objectives = goals,
        explanation = "",
        commonMistake = "",
    )
}

private fun closeTextbook(
    onCourse: (String?) -> Unit,
    onPage: (Int?) -> Unit,
    onRangeStart: (Int?) -> Unit,
    onRangeEnd: (Int?) -> Unit,
) {
    onCourse(null)
    onPage(null)
    onRangeStart(null)
    onRangeEnd(null)
}

@Composable
private fun MinimalBottomBar(selected: MainTab, backgroundColor: Color, onSelect: (MainTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(horizontal = 7.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(tab) }.padding(horizontal = 1.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) NavigationWhite else NavigationWhite.copy(alpha = 0.32f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                )
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) NavigationBlue else Color.Transparent))
            }
        }
    }
}
