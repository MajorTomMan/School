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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.AppSettingsRepository
import com.majortomman.school.learning.advisor.LearningAdvisor
import com.majortomman.school.learning.advisor.ReviewAdvice
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.assessment.persistence.AssessmentPracticeStatus
import com.majortomman.school.learning.assessment.persistence.AssessmentProgressStore
import com.majortomman.school.learning.cloud.CourseLibraryRepository
import com.majortomman.school.learning.cloud.InstalledCourse
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.knowledge.KnowledgePointId
import com.majortomman.school.learning.knowledge.KnowledgePointState
import com.majortomman.school.learning.knowledge.KnowledgePointStateReader
import com.majortomman.school.learning.progress.CourseProgressSnapshot
import com.majortomman.school.learning.progress.LessonProgressStatus
import com.majortomman.school.learning.persistence.RoomLearningDataMaintenance
import com.majortomman.school.learning.progress.persistence.CourseProgressStore
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private enum class MainTab(val label: String, val symbol: String) {
    LEARN("学习", "⌂"),
    COURSES("课程", "▤"),
    PRACTICE("练习", "✎"),
    MINE("我的", "●"),
}

private enum class MinePage {
    HOME,
    RECORD,
    SETTINGS,
}

@Composable
fun SchoolApp(
    settingsRepository: AppSettingsRepository,
    courseProgressStore: CourseProgressStore,
    initialCourseId: String? = null,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.LEARN.name) }
    var minePageName by rememberSaveable { mutableStateOf(MinePage.HOME.name) }
    var activeCourseId by rememberSaveable { mutableStateOf(initialCourseId) }
    var openedLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedTextbookPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var readingRangeStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var readingRangeEnd by rememberSaveable { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val assessmentProgressStore = remember(context) { AssessmentProgressStore.create(context) }
    val learningDataMaintenance = remember(courseProgressStore, assessmentProgressStore) {
        RoomLearningDataMaintenance(courseProgressStore, assessmentProgressStore)
    }
    val knowledgeStateReader = remember(context) { KnowledgePointStateReader.create(context) }
    val learningAdvisor = remember { LearningAdvisor() }
    val aiSettings by settingsRepository.aiSettings.collectAsState(initial = AiSettings())
    val libraryState by CourseLibraryRepository.state.collectAsState()

    val activeCourse = libraryState.course(activeCourseId)
    var knowledgeStates by remember(activeCourse?.id) { mutableStateOf<List<KnowledgePointState>>(emptyList()) }
    var reviewQueue by remember(activeCourse?.id) { mutableStateOf<List<ReviewAdvice>>(emptyList()) }
    var practiceStatuses by remember(activeCourse?.id) {
        mutableStateOf<Map<QuestionSetId, AssessmentPracticeStatus>>(emptyMap())
    }
    val progressFlow = remember(activeCourse?.id) {
        activeCourse?.id?.let(courseProgressStore::observeCourse)
            ?: flowOf(CourseProgressSnapshot(courseId = ""))
    }
    val progress by progressFlow.collectAsState(initial = CourseProgressSnapshot(activeCourse?.id.orEmpty()))

    val lessons = activeCourse?.lessons.orEmpty().mapIndexed { index, lesson ->
        val defaultStatus = if (index == 0 && progress.lessonStatuses.isEmpty()) {
            LessonProgressStatus.IN_PROGRESS
        } else {
            LessonProgressStatus.NOT_STARTED
        }
        lesson.toUiLesson(progress.lessonStatuses[lesson.id] ?: defaultStatus)
    }
    val currentLesson = lessons.firstOrNull { it.status == LessonProgressStatus.IN_PROGRESS }
        ?: lessons.firstOrNull { it.status == LessonProgressStatus.NOT_STARTED }
        ?: lessons.lastOrNull()
    val selectedTab = MainTab.valueOf(selectedTabName)
    val reviewSuggestion = activeCourse?.let { course ->
        reviewQueue.firstOrNull()?.let { advice ->
            val knowledgePoint = course.document.knowledgePoints.firstOrNull {
                it.id == advice.knowledgePointId.value
            }
            val reviewLesson = course.lessons.firstOrNull { courseLesson ->
                val progressStatus = lessons.firstOrNull { it.id == courseLesson.id }?.status
                advice.knowledgePointId.value in courseLesson.knowledgePointIds &&
                    progressStatus != null &&
                    progressStatus != LessonProgressStatus.NOT_STARTED
            }
            if (knowledgePoint != null && reviewLesson != null) {
                LearningReviewSuggestion(
                    knowledgePointName = knowledgePoint.name,
                    lessonId = reviewLesson.id,
                )
            } else {
                null
            }
        }
    }
    val openedCourseLesson = activeCourse?.lessons?.firstOrNull { it.id == openedLessonId }
    val openedLessonIndex = activeCourse?.lessons?.indexOfFirst { it.id == openedLessonId } ?: -1
    val nextCourseLesson = activeCourse?.lessons?.getOrNull(openedLessonIndex + 1).takeIf { openedLessonIndex >= 0 }
    val openedTextbook = libraryState.course(openedCourseId)
    val readingRange = if (readingRangeStart != null && readingRangeEnd != null) {
        readingRangeStart!!..readingRangeEnd!!
    } else {
        null
    }

    fun openLesson(course: InstalledCourse, lessonId: String) {
        openedLessonId = lessonId
        val uiLesson = lessons.firstOrNull { it.id == lessonId }
        if (uiLesson?.status == LessonProgressStatus.NOT_STARTED) {
            scope.launch { courseProgressStore.startLesson(course.id, lessonId) }
        }
    }

    LaunchedEffect(activeCourse?.id, activeCourse?.contentVersion, selectedTab, openedLessonId) {
        val course = activeCourse
        if (course == null) {
            knowledgeStates = emptyList()
            reviewQueue = emptyList()
            practiceStatuses = emptyMap()
        } else if (openedLessonId == null && (selectedTab == MainTab.LEARN || selectedTab == MainTab.MINE)) {
            val ids = course.document.knowledgePoints.map { KnowledgePointId(it.id) }
            knowledgeStates = runCatching { knowledgeStateReader.read(course.id, ids) }.getOrDefault(emptyList())
            reviewQueue = learningAdvisor.advise(
                states = knowledgeStates,
                reviewLimit = 3,
            ).reviews
            val assessmentDocument = course.assessments
            practiceStatuses = if (assessmentDocument == null) {
                emptyMap()
            } else {
                runCatching {
                    assessmentProgressStore.practiceStatuses(
                        courseId = course.id,
                        contentRevision = course.contentVersion.toString(),
                        questionSetIds = assessmentDocument.questionSets.map { it.id },
                    )
                }.getOrDefault(emptyMap())
            }
        }
    }

    LaunchedEffect(libraryState.courses.map { it.id }) {
        if (activeCourseId == null && libraryState.courses.size == 1) {
            activeCourseId = libraryState.courses.first().id
        }
        if (activeCourseId != null && activeCourse == null) {
            activeCourseId = null
            openedLessonId = null
        }
        if (openedCourseId != null && openedTextbook == null) {
            closeTextbook(
                onCourse = { openedCourseId = it },
                onPage = { openedTextbookPage = it },
                onRangeStart = { readingRangeStart = it },
                onRangeEnd = { readingRangeEnd = it },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = openedCourseLesson,
            transitionSpec = {
                if (targetState != null) {
                    (fadeIn(tween(260)) + slideInHorizontally(tween(340)) { it / 8 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { -it / 10 })
                } else {
                    (fadeIn(tween(240)) + slideInHorizontally(tween(320)) { -it / 9 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { it / 10 })
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
                        scope.launch {
                            courseProgressStore.finishLessonAndStartNext(
                                courseId = activeCourse.id,
                                currentLessonId = lesson.id,
                                nextLessonId = nextId,
                            )
                        }
                        if (nextCourseLesson != null) {
                            openedLessonId = nextCourseLesson.id
                        } else {
                            openedLessonId = null
                            selectedTabName = MainTab.COURSES.name
                        }
                    },
                )
            } else {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        SchoolBottomBar(selectedTab) {
                            if (it != MainTab.MINE) minePageName = MinePage.HOME.name
                            selectedTabName = it.name
                        }
                    },
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (fadeIn(tween(220)) + slideInHorizontally(tween(300)) { it / 16 }) togetherWith
                                    (fadeOut(tween(130)) + slideOutHorizontally(tween(220)) { -it / 16 })
                            },
                            label = "mainTabs",
                        ) { tab ->
                            when (tab) {
                                MainTab.LEARN -> {
                                    if (activeCourse == null || currentLesson == null || lessons.isEmpty()) {
                                        NoActiveTextbookScreen { selectedTabName = MainTab.COURSES.name }
                                    } else {
                                        TodayScreen(
                                            currentLessonId = currentLesson.id,
                                            lessons = lessons,
                                            courseTitle = activeCourse.title,
                                            courseSubject = activeCourse.subject,
                                            reviewSuggestion = reviewSuggestion,
                                            onStartLesson = { openLesson(activeCourse, it) },
                                            onOpenPractice = { selectedTabName = MainTab.PRACTICE.name },
                                            onOpenPath = { selectedTabName = MainTab.COURSES.name },
                                        )
                                    }
                                }

                                MainTab.COURSES -> {
                                    if (activeCourse == null || lessons.isEmpty()) {
                                        SubjectTextbookCenterScreen(
                                            libraryState = libraryState,
                                            onEnterCourse = { course ->
                                                activeCourseId = course.id
                                                openedLessonId = null
                                            },
                                            onOpenTextbook = { course, page ->
                                                openedCourseId = course.id
                                                openedTextbookPage = page
                                                readingRangeStart = null
                                                readingRangeEnd = null
                                            },
                                        )
                                    } else {
                                        CoursePathScreen(
                                            courseTitle = activeCourse.title,
                                            lessons = lessons,
                                            onOpenLesson = { openLesson(activeCourse, it) },
                                            onChooseCourse = { activeCourseId = null },
                                        )
                                    }
                                }

                                MainTab.PRACTICE -> PracticeScreen(
                                    course = activeCourse,
                                    onOpenCourses = { selectedTabName = MainTab.COURSES.name },
                                )

                                MainTab.MINE -> {
                                    when (MinePage.valueOf(minePageName)) {
                                        MinePage.HOME -> MyScreen(
                                            currentCourseTitle = activeCourse?.title,
                                            recentLessonTitle = progress.lastLessonId
                                                ?.let { lastId -> lessons.firstOrNull { it.id == lastId }?.title },
                                            onOpenLearningRecord = {
                                                if (activeCourse == null) {
                                                    selectedTabName = MainTab.COURSES.name
                                                } else {
                                                    minePageName = MinePage.RECORD.name
                                                }
                                            },
                                            onOpenCourses = { selectedTabName = MainTab.COURSES.name },
                                            onOpenSettings = { minePageName = MinePage.SETTINGS.name },
                                        )

                                        MinePage.RECORD -> {
                                            val course = activeCourse
                                            if (course == null) {
                                                NoActiveTextbookScreen { selectedTabName = MainTab.COURSES.name }
                                            } else {
                                                LearningRecordScreen(
                                                    courseTitle = course.title,
                                                    completedLessonCount = lessons.count { it.status == LessonProgressStatus.COMPLETED },
                                                    totalLessonCount = lessons.size,
                                                    recentLessonTitle = progress.lastLessonId
                                                        ?.let { lastId -> lessons.firstOrNull { it.id == lastId }?.title },
                                                    practiceStatuses = practiceStatuses.values,
                                                    knowledgeStates = knowledgeStates,
                                                    reviewQueue = reviewQueue,
                                                    knowledgePointNames = course.document.knowledgePoints.associate {
                                                        KnowledgePointId(it.id) to it.name
                                                    },
                                                    onBack = { minePageName = MinePage.HOME.name },
                                                )
                                            }
                                        }

                                        MinePage.SETTINGS -> SettingsScreen(
                                            settings = aiSettings,
                                            onSave = { updated ->
                                                scope.launch { settingsRepository.saveAiSettings(updated) }
                                            },
                                            onOpenSubjects = {
                                                minePageName = MinePage.HOME.name
                                                selectedTabName = MainTab.COURSES.name
                                            },
                                            onClearProgress = {
                                                scope.launch {
                                                    learningDataMaintenance.clearAll()
                                                    knowledgeStates = emptyList()
                                                    reviewQueue = emptyList()
                                                    practiceStatuses = emptyMap()
                                                }
                                            },
                                            onBack = { minePageName = MinePage.HOME.name },
                                        )
                                    }
                                }
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

private fun CourseLesson.toUiLesson(status: LessonProgressStatus): LessonUiModel =
    LessonUiModel(
        id = id,
        title = title,
        subtitle = goals.firstOrNull().orEmpty(),
        status = status,
    )

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
private fun SchoolBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    tab.symbol,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
                Text(
                    tab.label,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier.size(4.dp).clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background),
                )
            }
        }
    }
    SchoolDivider()
}
