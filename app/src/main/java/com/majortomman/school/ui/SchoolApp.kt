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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.AttemptRecord
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.PreferencesRepository
import com.majortomman.school.data.SampleContent
import com.majortomman.school.data.ScheduledReview
import com.majortomman.school.data.recordAttempt
import kotlinx.coroutines.launch

private val NavigationBlack = Color(0xFF050608)
private val NavigationWhite = Color(0xFFF5F7FA)
private val NavigationBlue = Color(0xFF2D7BFF)

private enum class MainTab(
    val label: String,
    val symbol: String,
) {
    TODAY("今天", "01"),
    PATH("路径", "02"),
    REVIEW("复习", "03"),
    SETTINGS("设置", "04"),
}

@Composable
fun SchoolApp(repository: PreferencesRepository) {
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.TODAY.name) }
    var openedLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val progress by repository.learningProgress.collectAsState(initial = LearningProgress())
    val aiSettings by repository.aiSettings.collectAsState(initial = AiSettings())
    val recentAttempts by repository.recentAttempts.collectAsState(initial = emptyList<AttemptRecord>())
    val reviewQueue by repository.reviewQueue.collectAsState(initial = emptyList<ScheduledReview>())

    val lessons = SampleContent.lessons.map { lesson ->
        lesson.copy(status = progress.lessonStatuses[lesson.id] ?: lesson.status)
    }
    val selectedTab = MainTab.valueOf(selectedTabName)
    val openedLesson = lessons.firstOrNull { it.id == openedLessonId }

    AnimatedContent(
        targetState = openedLesson,
        transitionSpec = {
            if (targetState != null) {
                (fadeIn(tween(240)) + slideInHorizontally(tween(320)) { it / 5 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { -it / 8 })
            } else {
                (fadeIn(tween(220)) + slideInHorizontally(tween(300)) { -it / 6 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { it / 8 })
            }
        },
        label = "appNavigation",
    ) { lesson ->
        if (lesson != null) {
            SceneLearningScreen(
                lesson = lesson,
                aiSettings = aiSettings,
                progress = progress,
                onBack = { openedLessonId = null },
                onRecordAttempt = { answer, correct, feedback ->
                    scope.launch {
                        repository.recordAttempt(lesson.id, answer, correct, feedback)
                    }
                },
            )
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    SceneBottomBar(
                        selected = selectedTab,
                        onSelect = { selectedTabName = it.name },
                    )
                },
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(210)) + slideInHorizontally(tween(260)) { it / 12 }) togetherWith
                                (fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { -it / 12 })
                        },
                        label = "mainTabs",
                    ) { tab ->
                        when (tab) {
                            MainTab.TODAY -> SceneTodayScreen(
                                plan = SampleContent.dailyPlan,
                                lessons = lessons,
                                progress = progress,
                                onStartLesson = { openedLessonId = it },
                                onOpenPath = { selectedTabName = MainTab.PATH.name },
                            )

                            MainTab.PATH -> SceneCoursePathScreen(
                                lessons = lessons,
                                onOpenLesson = { openedLessonId = it },
                            )

                            MainTab.REVIEW -> RoomReviewScreen(
                                fallbackItems = SampleContent.reviews,
                                progress = progress,
                                scheduledReviews = reviewQueue,
                                recentAttempts = recentAttempts,
                                onOpenLesson = { openedLessonId = it },
                            )

                            MainTab.SETTINGS -> SettingsScreen(
                                settings = aiSettings,
                                onSave = { updated -> scope.launch { repository.saveAiSettings(updated) } },
                                onClearProgress = { scope.launch { repository.clearLearningProgress() } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavigationBlack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NavigationWhite.copy(alpha = 0.12f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        tab.symbol,
                        color = if (isSelected) NavigationBlue else NavigationWhite.copy(alpha = 0.32f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        tab.label,
                        color = if (isSelected) NavigationWhite else NavigationWhite.copy(alpha = 0.46f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth(0.42f)
                            .background(if (isSelected) NavigationBlue else Color.Transparent),
                    )
                }
            }
        }
    }
}
