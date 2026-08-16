package com.majortomman.school.learning.course

import com.majortomman.school.visualization.VisualizationInvocation

/**
 * Authored learning course contract.
 *
 * The textbook is a reference source, not the course body. Course packages contain original
 * teaching lessons, structured practice and optional visualization invocations. Visualization
 * input is deliberately limited to renderer + typed parameters + text and is validated before
 * the course becomes active.
 */
data class CourseDocument(
    val textbook: CourseTextbook,
    val knowledgePoints: List<CourseKnowledgePoint>,
    val chapters: List<CourseChapter>,
)

data class CourseTextbook(
    val id: String,
    val title: String,
    val publisher: String,
    val edition: String,
    val grade: String,
    val semester: String,
    val subject: String,
    val pdf: CoursePdf,
)

data class CoursePdf(
    val path: String,
    val pageCount: Int,
    val pageIndexOffset: Int,
)

data class CourseKnowledgePoint(
    val id: String,
    val name: String,
    val description: String,
    val prerequisiteIds: List<String>,
)

data class CourseChapter(
    val id: String,
    val title: String,
    val sections: List<CourseSection>,
)

data class CourseSection(
    val id: String,
    val title: String,
    val lessons: List<CourseLesson>,
)

data class CourseLesson(
    val id: String,
    val title: String,
    val aliases: List<String>,
    val goals: List<String>,
    val knowledgePointIds: List<String>,
    val prerequisiteLessonIds: List<String>,
    val references: List<CourseSourceReference>,
    val steps: List<CourseStep>,
    val practice: List<CoursePractice>,
    val summary: List<String>,
)

data class CourseSourceReference(
    val label: String,
    val pageStart: Int,
    val pageEnd: Int,
)

sealed interface CourseStep

data class CourseExplanation(val title: String?, val text: String) : CourseStep

data class CourseQuestion(val prompt: String, val hint: String?) : CourseStep

data class CourseKeyIdea(val title: String?, val text: String) : CourseStep

data class CourseFormula(val expression: String, val note: String?) : CourseStep

data class CourseExample(
    val title: String,
    val prompt: String,
    val steps: List<String>,
    val answer: String,
) : CourseStep

data class CourseVisualizationStep(val visualization: VisualizationInvocation) : CourseStep

data class CourseCheckpoint(
    val prompt: String,
    val expectedAnswer: String,
    val explanation: String,
) : CourseStep

data class CourseSummaryStep(val text: String) : CourseStep

data class CoursePractice(
    val id: String,
    val prompt: String,
    val answer: String,
    val analysis: List<String>,
    val knowledgePointIds: List<String>,
    val difficulty: Int,
)
