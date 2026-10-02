package com.majortomman.school.learning.progress

import kotlinx.coroutines.flow.Flow

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

interface CourseProgressGateway {
    fun observeCourse(courseId: String): Flow<CourseProgressSnapshot>

    suspend fun startLesson(
        courseId: String,
        lessonId: String,
        atEpochMillis: Long = System.currentTimeMillis(),
    )

    suspend fun finishLessonAndStartNext(
        courseId: String,
        currentLessonId: String,
        nextLessonId: String?,
        atEpochMillis: Long = System.currentTimeMillis(),
    )
}
