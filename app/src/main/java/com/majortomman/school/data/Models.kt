package com.majortomman.school.data

enum class MasteryStatus(val label: String) {
    MASTERED("已掌握"),
    LEARNING("学习中"),
    NOT_STARTED("未开始"),
    NEEDS_REVIEW("需要复习"),
}

data class Lesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val estimatedMinutes: Int,
    val textbookPages: IntRange,
    val status: MasteryStatus,
    val objectives: List<String>,
    val explanation: String,
    val commonMistake: String,
)

data class ReviewItem(
    val id: String,
    val title: String,
    val reason: String,
    val dueLabel: String,
)

data class DailyPlan(
    val newLessonId: String,
    val reviewItems: List<ReviewItem>,
    val estimatedMinutes: Int,
)
