package com.majortomman.school.data

import com.majortomman.school.learning.progress.LessonProgressStatus

data class Lesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val estimatedMinutes: Int,
    val textbookPages: IntRange,
    val status: LessonProgressStatus,
    val objectives: List<String>,
)
