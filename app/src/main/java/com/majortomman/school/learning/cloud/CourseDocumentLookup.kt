package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.course.CourseChapter
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CoursePage
import com.majortomman.school.learning.course.CourseSection

/** Resolves curriculum lesson titles to pages in an installed, APK-validated course document. */
internal fun CourseDocument.pagesFor(title: String): List<CoursePage> {
    val requested = normalizeCourseTitle(title)
    if (requested.isBlank()) return emptyList()

    chapters.forEach { chapter ->
        if (chapter.names().any { normalizeCourseTitle(it) == requested }) {
            return chapter.sections.flatMap(CourseSection::pages) + chapter.review?.pages.orEmpty()
        }

        chapter.sections.forEach { section ->
            if (section.names().any { normalizeCourseTitle(it) == requested }) return section.pages
            section.pages.firstOrNull { page ->
                page.names().any { normalizeCourseTitle(it) == requested }
            }?.let { return listOf(it) }
        }

        chapter.review?.let { review ->
            if (review.names().any { normalizeCourseTitle(it) == requested }) return review.pages
            review.pages.firstOrNull { page ->
                page.names().any { normalizeCourseTitle(it) == requested }
            }?.let { return listOf(it) }
        }
    }

    return emptyList()
}

private fun CourseChapter.names(): List<String> = buildList {
    add(title)
    if (number.isNotBlank()) add(number + title)
    addAll(aliases)
}

private fun CourseSection.names(): List<String> = buildList {
    add(title)
    if (number.isNotBlank()) add(number + title)
    addAll(aliases)
}

private fun CoursePage.names(): List<String> = listOf(title) + aliases

private fun normalizeCourseTitle(value: String): String = value
    .replace(" ", "")
    .replace("　", "")
    .replace("（", "(")
    .replace("）", ")")
    .trim()
