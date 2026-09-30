package com.majortomman.school.learning.assessment.persistence

import com.majortomman.school.learning.assessment.domain.AssessmentEvidenceFactory
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import com.majortomman.school.learning.assessment.domain.SessionSummaryCalculator
import com.majortomman.school.learning.evidence.domain.LearningEvidence

data class AssessmentSettlementPlan(
    val summary: SessionSummary,
    val evidence: List<LearningEvidence>,
)

class AssessmentSettlementPlanner {
    fun plan(
        courseId: String,
        contentRevision: String,
        sessionId: SessionId,
        questionSet: QuestionSetDefinition,
        attempts: List<AttemptRecord>,
        events: List<QuestionLearningEvent>,
    ): AssessmentSettlementPlan {
        val summary = SessionSummaryCalculator.summarize(
            sessionId = sessionId,
            questionSet = questionSet,
            attempts = attempts,
            events = events,
        )
        return AssessmentSettlementPlan(
            summary = summary,
            evidence = AssessmentEvidenceFactory.create(
                courseId = courseId,
                contentRevision = contentRevision,
                questionSet = questionSet,
                summary = summary,
            ),
        )
    }
}
