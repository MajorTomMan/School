package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.course.CourseDocument

internal object CourseRuntimeCompatibilityValidator {
    fun validate(course: CourseDocument) {
        course.chapters
            .flatMap { it.sections }
            .flatMap { it.lessons }
            .flatMap { it.steps }
            .forEach { step ->
                step.activity?.let(SchoolActivityRuntimeCatalog::requireHandler)
                LearningContentRuntimeCompatibilityValidator.validate(step.content)
            }
    }
}
