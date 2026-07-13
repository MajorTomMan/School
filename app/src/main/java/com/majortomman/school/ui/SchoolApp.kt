package com.majortomman.school.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.majortomman.school.data.SampleContent

private enum class MainTab(
    val label: String,
    val emoji: String,
) {
    TODAY("今日", "☀️"),
    PATH("课程", "🧭"),
    REVIEW("复习", "📝"),
    SETTINGS("设置", "⚙️"),
}

@Composable
fun SchoolApp() {
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.TODAY.name) }
    var openedLessonId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedTab = MainTab.valueOf(selectedTabName)
    val openedLesson = SampleContent.lessons.firstOrNull { it.id == openedLessonId }

    if (openedLesson != null) {
        LearningScreen(
            lesson = openedLesson,
            onBack = { openedLessonId = null },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTabName = tab.name },
                        icon = { Text(tab.emoji) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                MainTab.TODAY -> TodayScreen(
                    plan = SampleContent.dailyPlan,
                    lessons = SampleContent.lessons,
                    onStartLesson = { openedLessonId = it },
                    onOpenPath = { selectedTabName = MainTab.PATH.name },
                )

                MainTab.PATH -> CoursePathScreen(
                    lessons = SampleContent.lessons,
                    onOpenLesson = { openedLessonId = it },
                )

                MainTab.REVIEW -> ReviewScreen(items = SampleContent.reviews)
                MainTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
