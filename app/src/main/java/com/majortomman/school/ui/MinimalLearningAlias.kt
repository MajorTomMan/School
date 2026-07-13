package com.majortomman.school.ui

import androidx.compose.runtime.Composable
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.Lesson

@Composable
fun LearningScreen(
    lesson: Lesson,
    aiSettings: AiSettings,
    progress: LearningProgress,
    onBack: () -> Unit,
    onRecordAttempt: (answer: String, correct: Boolean, feedback: String) -> Unit,
) {
    MinimalLearningScreen(
        lesson = lesson,
        aiSettings = aiSettings,
        progress = progress,
        onBack = onBack,
        onRecordAttempt = onRecordAttempt,
    )
}
