package com.majortomman.school.ui

import androidx.compose.runtime.Composable

@Composable
internal fun ScienceCourseWorkbenchHost(spec: InteractiveLessonSpec) {
    when {
        spec.badge.startsWith("化学课程") -> ChemistryCourseWorkbench(spec)
        else -> ChemistryCourseWorkbench(spec)
    }
}
