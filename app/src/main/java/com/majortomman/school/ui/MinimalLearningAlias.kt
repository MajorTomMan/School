package com.majortomman.school.ui

import androidx.compose.runtime.Composable
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.material.InstalledMaterialPack

@Composable
fun LearningScreen(
    lesson: Lesson,
    aiSettings: AiSettings,
    progress: LearningProgress,
    installedMaterial: InstalledMaterialPack?,
    onOpenTextbook: (Int) -> Unit,
    onBack: () -> Unit,
    onRecordAttempt: (answer: String, correct: Boolean, feedback: String) -> Unit,
) {
    MinimalLearningScreenV2(
        lesson = lesson,
        aiSettings = aiSettings,
        progress = progress,
        installedMaterial = installedMaterial,
        onOpenTextbook = onOpenTextbook,
        onBack = onBack,
        onRecordAttempt = onRecordAttempt,
    )
}
