package com.majortomman.school.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.material.InstalledMaterialPack
import com.majortomman.school.learning.cloud.CloudAssessmentRepository
import com.majortomman.school.learning.cloud.CloudCourseRepository

/**
 * Runs a lesson's explanatory pages first, then every question set placed in the matched course section.
 * A learner can leave the assessment and return to the lesson without marking the lesson complete.
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
    val assessments = remember(
        lesson.title,
        lesson.textbookPages.first,
        lesson.textbookPages.last,
        repositoryRevision,
    ) {
        CloudAssessmentRepository.forLesson(context, lesson.title, lesson.textbookPages)
    }
    var activeQuestionSetIndex by rememberSaveable(
        lesson.id,
        assessments?.contentRevision,
    ) { mutableStateOf<Int?>(null) }

    val activeIndex = activeQuestionSetIndex
    if (activeIndex != null && assessments != null) {
        val questionSet = assessments.questionSets.getOrNull(activeIndex)
        if (questionSet != null) {
            AssessmentSessionScreen(
                courseId = assessments.courseId,
                contentRevision = assessments.contentRevision,
                questionSet = questionSet,
                assetFiles = assessments.assetFiles,
                knowledgePoints = assessments.knowledgePoints,
                onBack = { activeQuestionSetIndex = null },
                onFinished = {
                    if (activeIndex < assessments.questionSets.lastIndex) {
                        activeQuestionSetIndex = activeIndex + 1
                    } else {
                        activeQuestionSetIndex = null
                        onComplete()
                    }
                },
            )
            return
        }
        activeQuestionSetIndex = null
    }

    CloudCourseLessonScreen(
        lesson = lesson,
        installedMaterial = installedMaterial,
        nextLessonTitle = nextLessonTitle,
        onOpenTextbook = onOpenTextbook,
        onBack = onBack,
        onComplete = {
            if (assessments?.questionSets.isNullOrEmpty()) onComplete()
            else activeQuestionSetIndex = 0
        },
    )
}
