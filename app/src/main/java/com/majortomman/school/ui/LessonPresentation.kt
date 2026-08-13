package com.majortomman.school.ui

import com.majortomman.school.learning.course.CourseCheckpoint
import com.majortomman.school.learning.course.CourseExample
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseFormula
import com.majortomman.school.learning.course.CourseKeyIdea
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseSceneStep
import com.majortomman.school.learning.course.CourseSourceLink
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseSummaryStep

/** Stable semantic pages. Layout size never decides where a lesson page breaks. */
internal sealed interface LessonPresentationPage {
    val label: String

    data class Overview(val goals: List<String>) : LessonPresentationPage {
        override val label: String = "学习目标"
    }

    data class Teaching(override val label: String, val steps: List<CourseStep>) : LessonPresentationPage

    data class Summary(val items: List<String>) : LessonPresentationPage {
        override val label: String = "这一课记住"
    }

    data class Practice(val number: Int, val total: Int, val practice: CoursePractice) : LessonPresentationPage {
        override val label: String = "练习 $number / $total"
    }
}

internal fun composeLessonPresentation(lesson: CourseLesson): List<LessonPresentationPage> {
    val pages = mutableListOf<LessonPresentationPage>()
    if (lesson.goals.isNotEmpty()) pages += LessonPresentationPage.Overview(lesson.goals)
    pages += composeTeachingPages(lesson.steps)
    if (lesson.summary.isNotEmpty()) pages += LessonPresentationPage.Summary(lesson.summary)
    lesson.practice.forEachIndexed { index, practice ->
        pages += LessonPresentationPage.Practice(index + 1, lesson.practice.size, practice)
    }
    return pages.ifEmpty { listOf(LessonPresentationPage.Summary(listOf("完成本课学习。"))) }
}

private fun composeTeachingPages(steps: List<CourseStep>): List<LessonPresentationPage.Teaching> {
    val result = mutableListOf<LessonPresentationPage.Teaching>()
    val current = mutableListOf<CourseStep>()

    fun flush() {
        if (current.isEmpty()) return
        result += LessonPresentationPage.Teaching(labelFor(current.first()), current.toList())
        current.clear()
    }

    steps.forEach { step ->
        val boundary = current.isNotEmpty() && (startsSemanticStage(step) || current.size >= MAX_STEPS_PER_PAGE)
        if (boundary) flush()
        current += step
    }
    flush()
    return result
}

private fun startsSemanticStage(step: CourseStep): Boolean = when (step) {
    is CourseQuestion,
    is CourseExample,
    is CourseSceneStep,
    is CourseCheckpoint,
    -> true
    is CourseExplanation -> step.title != null
    else -> false
}

private fun labelFor(step: CourseStep): String = when (step) {
    is CourseQuestion -> "先想一想"
    is CourseExplanation -> step.title ?: "建立概念"
    is CourseKeyIdea -> step.title ?: "关键理解"
    is CourseFormula -> "公式与规则"
    is CourseExample -> step.title
    is CourseSceneStep -> "看一看"
    is CourseCheckpoint -> "检查一下"
    is CourseSourceLink -> "教材参考"
    is CourseSummaryStep -> "阶段小结"
}

private const val MAX_STEPS_PER_PAGE = 4
