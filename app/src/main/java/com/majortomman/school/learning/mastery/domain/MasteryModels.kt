package com.majortomman.school.learning.mastery.domain

import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.evidence.domain.LearningEvidence

data class MasteryState(
    val knowledgePointId: KnowledgePointId,
    val score: Double,
    val accumulatedEvidenceWeight: Double,
) {
    init {
        require(score.isFinite() && score in 0.0..1.0) { "mastery score 必须位于 0.0 到 1.0" }
        require(accumulatedEvidenceWeight.isFinite() && accumulatedEvidenceWeight >= 0.0) {
            "accumulatedEvidenceWeight 不能小于 0"
        }
    }
}

data class MasteryUpdate(
    val knowledgePointId: KnowledgePointId,
    val beforeScore: Double,
    val afterScore: Double,
    val beforeEvidenceWeight: Double,
    val appliedEvidenceWeight: Double,
    val afterEvidenceWeight: Double,
    val policyVersion: Int,
) {
    init {
        require(beforeScore in 0.0..1.0 && afterScore in 0.0..1.0) {
            "mastery update score 必须位于 0.0 到 1.0"
        }
        require(beforeEvidenceWeight >= 0.0) { "beforeEvidenceWeight 不能小于 0" }
        require(appliedEvidenceWeight >= 0.0) { "appliedEvidenceWeight 不能小于 0" }
        require(afterEvidenceWeight >= beforeEvidenceWeight) {
            "afterEvidenceWeight 不能小于 beforeEvidenceWeight"
        }
        require(policyVersion > 0) { "policyVersion 必须大于 0" }
    }
}

data class MasteryPrior(
    val score: Double = 0.5,
    val evidenceWeight: Double = 1.0,
) {
    init {
        require(score.isFinite() && score in 0.0..1.0) { "初始掌握度必须位于 0.0 到 1.0" }
        require(evidenceWeight.isFinite() && evidenceWeight > 0.0) {
            "初始证据权重必须大于 0"
        }
    }

    fun stateFor(id: KnowledgePointId): MasteryState = MasteryState(
        knowledgePointId = id,
        score = score,
        accumulatedEvidenceWeight = evidenceWeight,
    )
}

interface MasteryPolicy {
    val version: Int

    fun update(
        current: MasteryState,
        evidence: List<LearningEvidence>,
    ): MasteryUpdate
}

/**
 * 使用累计证据权重进行平滑更新。
 *
 * Policy 只消费语义 Evidence，不知道 Evidence 来自独立 Assessment 还是 Lesson Activity。
 */
class WeightedMasteryPolicy(
    override val version: Int = 1,
) : MasteryPolicy {
    init {
        require(version > 0) { "policy version 必须大于 0" }
    }

    override fun update(
        current: MasteryState,
        evidence: List<LearningEvidence>,
    ): MasteryUpdate {
        require(evidence.all { it.knowledgePointId == current.knowledgePointId }) {
            "一次 update 只能处理同一个知识点"
        }

        val weightedEvidence = evidence.map { item ->
            val difficultyMultiplier = 0.75 + item.difficulty.value * 0.5
            val effectiveWeight = item.weight * difficultyMultiplier
            item.score to effectiveWeight
        }
        val appliedWeight = weightedEvidence.sumOf { it.second }
        if (appliedWeight == 0.0) {
            return MasteryUpdate(
                knowledgePointId = current.knowledgePointId,
                beforeScore = current.score,
                afterScore = current.score,
                beforeEvidenceWeight = current.accumulatedEvidenceWeight,
                appliedEvidenceWeight = 0.0,
                afterEvidenceWeight = current.accumulatedEvidenceWeight,
                policyVersion = version,
            )
        }

        val beforeWeightedScore = current.score * current.accumulatedEvidenceWeight
        val addedWeightedScore = weightedEvidence.sumOf { (score, weight) -> score * weight }
        val afterWeight = current.accumulatedEvidenceWeight + appliedWeight
        val afterScore = (beforeWeightedScore + addedWeightedScore) / afterWeight

        return MasteryUpdate(
            knowledgePointId = current.knowledgePointId,
            beforeScore = current.score,
            afterScore = afterScore.coerceIn(0.0, 1.0),
            beforeEvidenceWeight = current.accumulatedEvidenceWeight,
            appliedEvidenceWeight = appliedWeight,
            afterEvidenceWeight = afterWeight,
            policyVersion = version,
        )
    }
}
