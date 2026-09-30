package com.majortomman.school.learning.assessment.domain

import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.evidence.domain.LearningEvidence
import com.majortomman.school.learning.evidence.domain.LearningEvidenceOutcome
import com.majortomman.school.learning.evidence.domain.LearningEvidenceSource
import com.majortomman.school.learning.evidence.domain.LearningEvidenceSourceKind

object InlineAssessmentEvidenceFactory {
    fun createForCompletedActivity(
        courseId: String,
        contentRevision: String,
        lessonId: String,
        stepId: String,
        activityId: ActivityId,
        assessment: InlineAssessmentSpec,
        wrongAttemptCount: Int,
    ): List<LearningEvidence> {
        require(wrongAttemptCount >= 0) { "wrongAttemptCount 不能小于 0" }
        val score = when (wrongAttemptCount) {
            0 -> 1.0
            1 -> 0.75
            2 -> 0.60
            else -> 0.45
        }
        val outcome = if (wrongAttemptCount == 0) {
            LearningEvidenceOutcome.FIRST_TRY_CORRECT
        } else {
            LearningEvidenceOutcome.RECOVERED_CORRECT
        }
        val contextId = "lesson:$courseId:$lessonId:$stepId"
        return assessment.knowledgePointIds.map { knowledgePointId ->
            LearningEvidence(
                id = "$contextId:${activityId.value}:$knowledgePointId",
                courseId = courseId,
                knowledgePointId = KnowledgePointId(knowledgePointId),
                source = LearningEvidenceSource(
                    kind = LearningEvidenceSourceKind.LESSON_ACTIVITY,
                    contextId = contextId,
                    itemId = activityId.value,
                    itemRevision = 1,
                    contentRevision = contentRevision,
                ),
                outcome = outcome,
                score = score,
                weight = 1.0,
                difficulty = assessment.difficulty,
                wrongAttemptCount = wrongAttemptCount,
            )
        }
    }
}
