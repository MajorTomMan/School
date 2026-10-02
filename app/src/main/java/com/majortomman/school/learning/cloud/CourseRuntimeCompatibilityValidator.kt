package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.visualization.SchoolVisualizationCatalog

internal object CourseRuntimeCompatibilityValidator {
    fun validate(course: CourseDocument) {
        course.chapters
            .flatMap { it.sections }
            .flatMap { it.lessons }
            .flatMap { it.steps }
            .forEach { step ->
                step.activity?.let(SchoolActivityRuntimeCatalog::requireHandler)
                step.content.filterIsInstance<LearningContent.Visualization>().forEach { content ->
                    SchoolVisualizationCatalog.requireValid(content.visualization)
                }
            }
    }
}
