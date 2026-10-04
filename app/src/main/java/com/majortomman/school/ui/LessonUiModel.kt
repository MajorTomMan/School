package com.majortomman.school.ui

import com.majortomman.school.learning.progress.LessonProgressStatus

data class LessonUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: LessonProgressStatus,
)
