package com.majortomman.school.learning.course

import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.assessment.domain.InlineAssessmentSpec
import com.majortomman.school.learning.content.LearningContent

/**
 * Authored learning course contract.
 *
 * Course packages describe teaching structure and declarative content only. Runtime state,
 * layout, rendering and activity execution remain APK-owned.
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
)

data class CourseSourceReference(
    val label: String,
    val pageStart: Int,
    val pageEnd: Int,
)

enum class CourseStepRole {
    EXPLANATION,
    INQUIRY,
    EXAMPLE,
    KEY_IDEA,
    PRACTICE,
    CHECKPOINT,
    SUMMARY,
}

data class CourseStep(
    val id: String,
    val role: CourseStepRole,
    val title: String?,
    val content: List<LearningContent>,
    val activity: ActivitySpec? = null,
    val assessment: InlineAssessmentSpec? = null,
) {
    init {
        require(id.isNotBlank()) { "step id 不能为空" }
        require(title == null || title.isNotBlank()) { "step title 不能为空字符串" }
        require(content.isNotEmpty() || activity != null) { "step " + id + " 必须包含内容或活动" }
        require(assessment == null || activity != null) { "step " + id + " 声明 assessment 时必须同时声明 activity" }
        require(activity != null || role !in setOf(CourseStepRole.PRACTICE, CourseStepRole.CHECKPOINT)) {
            "step " + id + " 的 " + role.name.lowercase() + " 必须声明 activity"
        }
    }
}
