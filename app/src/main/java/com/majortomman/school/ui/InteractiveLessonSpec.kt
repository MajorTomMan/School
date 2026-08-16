package com.majortomman.school.ui

import com.majortomman.school.data.Lesson

enum class InteractiveLessonKind {
    CLOUD_COURSE,
}

data class InteractiveLessonSpec(
    val kind: InteractiveLessonKind,
)

object InteractiveLessonCatalog {
    fun resolve(subjectId: String, lesson: Lesson): InteractiveLessonSpec? {
        if (subjectId.isBlank() || lesson.title.isBlank()) return null
        return InteractiveLessonSpec(kind = InteractiveLessonKind.CLOUD_COURSE)
    }
}
