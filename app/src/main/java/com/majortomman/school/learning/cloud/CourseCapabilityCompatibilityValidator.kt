package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.capability.CapabilityKey
import com.majortomman.school.learning.capability.CapabilityKind
import com.majortomman.school.learning.capability.SchoolCapabilityCatalog
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseDocument

internal object CourseCapabilityCompatibilityValidator {
    fun validate(course: CourseDocument) {
        course.chapters
            .flatMap { it.sections }
            .flatMap { it.lessons }
            .flatMap { it.steps }
            .forEach { step ->
                step.activity?.let { activity ->
                    SchoolCapabilityCatalog.requireSupported(
                        key = activity.capability,
                        kind = CapabilityKind.ACTIVITY,
                        schemaVersion = activity.schemaVersion,
                    )
                }
                step.content.filterIsInstance<LearningContent.Visualization>().forEach { content ->
                    SchoolCapabilityCatalog.requireSupported(
                        key = CapabilityKey(content.visualization.renderer.value),
                        kind = CapabilityKind.VISUALIZATION,
                        schemaVersion = content.visualization.schemaVersion,
                    )
                }
            }
    }
}
