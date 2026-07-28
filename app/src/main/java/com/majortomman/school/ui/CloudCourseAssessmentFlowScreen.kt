package com.majortomman.school.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.material.InstalledMaterialPack
import com.majortomman.school.learning.assessment.contract.CourseAssessmentQuestionSet
import com.majortomman.school.learning.cloud.CloudAssessmentRepository
import com.majortomman.school.learning.cloud.CloudCourseRepository
import com.majortomman.school.learning.cloud.InstalledLessonAssessments
import com.majortomman.school.learning.course.CourseConclusion
import com.majortomman.school.learning.course.CourseExercise
import com.majortomman.school.learning.course.CoursePage

private data class SectionLearningStage(
    val label: String,
    val pages: List<CoursePage>,
    val questionSets: List<CourseAssessmentQuestionSet>,
)

/**
 * Presents one section at a time. A section's authored explanation is immediately followed by its
 * placed assessment, so static exercise paragraphs never compete with the real one-question UI.
 */
@Composable
fun CloudCourseAssessmentFlowScreen(
    lesson: Lesson,
    installedMaterial: InstalledMaterialPack,
    nextLessonTitle: String?,
    onOpenTextbook: (Int) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val repositoryRevision by CloudCourseRepository.revision.collectAsState()
    val pages = remember(
        lesson.title,
        lesson.textbookPages.first,
        lesson.textbookPages.last,
        repositoryRevision,
    ) {
        CloudCourseRepository.pagesFor(lesson.title, lesson.textbookPages)
    }
    val assessments = remember(
        lesson.title,
        lesson.textbookPages.first,
        lesson.textbookPages.last,
        repositoryRevision,
    ) {
        CloudAssessmentRepository.forLesson(context, lesson.title, lesson.textbookPages)
    }
    val stages = remember(pages, assessments?.contentRevision) {
        buildSectionStages(pages, assessments)
    }

    if (stages.isEmpty()) {
        CloudCourseLessonScreen(
            lesson = lesson,
            installedMaterial = installedMaterial,
            nextLessonTitle = nextLessonTitle,
            onOpenTextbook = onOpenTextbook,
            onBack = onBack,
            onComplete = onComplete,
        )
        return
    }

    var sectionIndex by rememberSaveable(lesson.id, assessments?.contentRevision) {
        mutableIntStateOf(0)
    }
    var activeQuestionSetIndex by rememberSaveable(
        lesson.id,
        assessments?.contentRevision,
        sectionIndex,
    ) {
        mutableStateOf<Int?>(null)
    }

    val boundedSectionIndex = sectionIndex.coerceIn(0, stages.lastIndex)
    if (boundedSectionIndex != sectionIndex) sectionIndex = boundedSectionIndex
    val stage = stages[boundedSectionIndex]
    val questionIndex = activeQuestionSetIndex
        ?: 0.takeIf { stage.pages.isEmpty() && stage.questionSets.isNotEmpty() }
    val questionSet = questionIndex?.let(stage.questionSets::getOrNull)

    fun advanceSection() {
        activeQuestionSetIndex = null
        if (boundedSectionIndex < stages.lastIndex) {
            sectionIndex = boundedSectionIndex + 1
        } else {
            onComplete()
        }
    }

    if (questionIndex != null && questionSet != null && assessments != null) {
        AssessmentSessionScreen(
            courseId = assessments.courseId,
            contentRevision = assessments.contentRevision,
            questionSet = questionSet,
            assetFiles = assessments.assetFiles,
            knowledgePoints = assessments.knowledgePoints,
            onBack = {
                if (stage.pages.isEmpty()) onBack() else activeQuestionSetIndex = null
            },
            onFinished = {
                if (questionIndex < stage.questionSets.lastIndex) {
                    activeQuestionSetIndex = questionIndex + 1
                } else {
                    advanceSection()
                }
            },
        )
        return
    }

    key(stage.label) {
        CloudCourseLessonScreen(
            lesson = lesson,
            installedMaterial = installedMaterial,
            nextLessonTitle = when {
                stage.questionSets.isNotEmpty() -> "节末练习"
                boundedSectionIndex < stages.lastIndex -> stages[boundedSectionIndex + 1].label
                else -> nextLessonTitle
            },
            pagesOverride = stage.pages,
            onOpenTextbook = onOpenTextbook,
            onBack = onBack,
            onComplete = {
                if (stage.questionSets.isNotEmpty()) {
                    activeQuestionSetIndex = 0
                } else {
                    advanceSection()
                }
            },
        )
    }
}

private fun buildSectionStages(
    pages: List<CoursePage>,
    assessments: InstalledLessonAssessments?,
): List<SectionLearningStage> {
    val grouped = linkedMapOf<String, MutableList<CoursePage>>()
    pages.forEach { page -> grouped.getOrPut(page.section) { mutableListOf() }.add(page) }

    return grouped.mapNotNull { (label, originalPages) ->
        val questionSets = assessments?.questionSetsFor(label).orEmpty()
        val teachingPages = if (questionSets.isEmpty()) {
            originalPages
        } else {
            originalPages.mapNotNull(::withoutStaticExercises)
        }
        when {
            teachingPages.isNotEmpty() -> SectionLearningStage(label, teachingPages, questionSets)
            questionSets.isNotEmpty() -> SectionLearningStage(label, emptyList(), questionSets)
            else -> null
        }
    }
}

private fun withoutStaticExercises(page: CoursePage): CoursePage? {
    val filtered = page.blocks.filterNot { it is CourseExercise }
    val hasTeachingContent = filtered.any { block -> block !is CourseConclusion }
    return if (hasTeachingContent) page.copy(blocks = filtered) else null
}
