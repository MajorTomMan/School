package com.majortomman.school.learning.assessment.persistence

import com.majortomman.school.learning.assessment.domain.AssessmentEvidenceFactory
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import com.majortomman.school.learning.assessment.domain.SessionSummaryCalculator
import com.majortomman.school.learning.evidence.domain.LearningEvidence
import com.majortomman.school.learning.mastery.domain.MasteryPolicy
import com.majortomman.school.learning.mastery.domain.MasteryPrior
import com.majortomman.school.learning.mastery.domain.MasteryState
import com.majortomman.school.learning.mastery.domain.MasteryUpdate
import com.majortomman.school.learning.mastery.domain.WeightedMasteryPolicy

data class AssessmentSettlementPlan(
    val summary: SessionSummary,
    val evidence: List<LearningEvidence>,
    val masteryUpdates: List<MasteryUpdate>,
    val masteryPolicyVersion: Int,
)

class AssessmentSettlementPlanner(
    private val masteryPolicy: MasteryPolicy = WeightedMasteryPolicy(),
    private val masteryPrior: MasteryPrior = MasteryPrior(),
) {
    fun plan(
        courseId: String,
        contentRevision: String,
        sessionId: SessionId,
        questionSet: QuestionSetDefinition,
        attempts: List<AttemptRecord>,
        events: List<QuestionLearningEvent>,
        currentMastery: Map<KnowledgePointId, MasteryState>,
    ): AssessmentSettlementPlan {
        require(currentMastery.all { (id, state) -> id == state.knowledgePointId }) {
            "currentMastery 的键与状态知识点不一致"
        }
        val summary = SessionSummaryCalculator.summarize(
            sessionId = sessionId,
            questionSet = questionSet,
            attempts = attempts,
            events = events,
        )
        val evidence = AssessmentEvidenceFactory.create(
            courseId = courseId,
            contentRevision = contentRevision,
            questionSet = questionSet,
            summary = summary,
        )
        val updates = evidence
            .groupBy(LearningEvidence::knowledgePointId)
            .toSortedMap(compareBy(KnowledgePointId::value))
            .map { (knowledgePointId, items) ->
                val current = currentMastery[knowledgePointId] ?: masteryPrior.stateFor(knowledgePointId)
                masteryPolicy.update(current, items)
            }
        return AssessmentSettlementPlan(
            summary = summary,
            evidence = evidence,
            masteryUpdates = updates,
            masteryPolicyVersion = masteryPolicy.version,
        )
    }
}
