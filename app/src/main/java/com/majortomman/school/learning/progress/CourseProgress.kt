package com.majortomman.school.learning.progress

enum class LessonProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
}

data class CourseProgressSnapshot(
    val courseId: String,
    val lessonStatuses: Map<String, LessonProgressStatus> = emptyMap(),
    val lastLessonId: String? = null,
) {
    fun status(lessonId: String): LessonProgressStatus =
        lessonStatuses[lessonId] ?: LessonProgressStatus.NOT_STARTED
}
