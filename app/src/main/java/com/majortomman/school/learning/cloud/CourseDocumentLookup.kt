package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CourseLesson

internal fun CourseDocument.lessons(): List<CourseLesson> = chapters.flatMap { chapter ->
    chapter.sections.flatMap { section -> section.lessons }
}

internal fun CourseDocument.lessonFor(id: String, title: String): CourseLesson? {
    val requested = normalizeCourseTitle(title)
    return lessons().firstOrNull { lesson ->
        lesson.id == id || normalizeCourseTitle(lesson.title) == requested || lesson.aliases.any { normalizeCourseTitle(it) == requested }
    }
}

private fun normalizeCourseTitle(value: String): String = value.replace(" ", "").replace("　", "").replace("（", "(").replace("）", ")").trim()
