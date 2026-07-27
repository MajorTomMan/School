package com.majortomman.school.learning.assessment.persistence

import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import com.majortomman.school.learning.assessment.domain.SessionSummaryCalculator
import com.majortomman.school.learning.mastery.domain.MasteryEvidence
import com.majortomman.school.learning.mastery.domain.MasteryEvidenceFactory
import com.majortomman.school.learning.mastery.domain.MasteryPolicy
import com.majortomman.school.learning.mastery.domain.MasteryState
import com.majortomman.school.learning.mastery.domain.MasteryUpdate
import com.majortomman.school.learning.mastery.domain.WeightedMasteryPolicy

data class MasteryPrior(
    val score: Double = 0.5,
    val evidenceWeight: Double = 1.0,
) {
    init {
        require(score.isFinite() && score in 0.0..1.0) { "初始掌握度必须位于 0.0 到 1.0" }
        require(evidenceWeight.isFinite() && evidenceWeight > 0.0) {
            "初始证据权重必须大于 0，以避免一次练习把掌握度推到边界"
        }
    }

    fun stateFor(id: KnowledgePointId): MasteryState = MasteryState(
        knowledgePointId = id,
        score = score,
        accumulatedEvidenceWeight = evidenceWeight,
    )
}

data class AssessmentSettlementPlan(
    val summary: SessionSummary,
    val evidence: List<MasteryEvidence>,
    val masteryUpdates: List<MasteryUpdate>,
    val masteryPolicyVersion: Int,
)

class AssessmentSettlementPlanner(
    private val masteryPolicy: MasteryPolicy = WeightedMasteryPolicy(),
    private val masteryPrior: MasteryPrior = MasteryPrior(),
) {
    fun plan(
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
        val evidence = MasteryEvidenceFactory.create(questionSet, summary)
        val updates = evidence
            .groupBy(MasteryEvidence::knowledgePointId)
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
