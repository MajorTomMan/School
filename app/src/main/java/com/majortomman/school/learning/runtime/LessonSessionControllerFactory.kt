package com.majortomman.school.learning.runtime

import com.majortomman.school.learning.course.CourseLesson

class LessonSessionControllerFactory(
    private val evidenceGateway: LessonEvidenceGateway,
) {
    fun create(
        courseId: String,
        contentRevision: String,
        lesson: CourseLesson,
    ): LessonSessionController = LessonSessionController(
        courseId = courseId,
        contentRevision = contentRevision,
        lesson = lesson,
        evidenceGateway = evidenceGateway,
    )
}
