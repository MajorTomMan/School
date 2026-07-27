package com.majortomman.school.learning.mastery.domain

import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionCompletionStatus
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionId
import com.majortomman.school.learning.assessment.domain.QuestionResult
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary

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

enum class MasteryEvidenceOutcome {
    FIRST_TRY_CORRECT,
    RECOVERED_CORRECT,
    FINAL_INCORRECT,
}

data class MasteryEvidence(
    val knowledgePointId: KnowledgePointId,
    val questionId: QuestionId,
    val questionRevision: Int,
    val sessionId: SessionId,
    val outcome: MasteryEvidenceOutcome,
    val score: Double,
    val weight: Double,
    val difficulty: Difficulty,
    val wrongAttemptCount: Int,
    val hintViewCount: Int,
    val explanationViewed: Boolean,
) {
    init {
        require(questionRevision > 0) { "questionRevision 必须大于 0" }
        require(score.isFinite() && score in 0.0..1.0) { "evidence score 必须位于 0.0 到 1.0" }
        require(weight.isFinite() && weight > 0.0) { "evidence weight 必须大于 0" }
        require(wrongAttemptCount >= 0) { "wrongAttemptCount 不能小于 0" }
        require(hintViewCount >= 0) { "hintViewCount 不能小于 0" }
    }
}

interface MasteryEvidenceScorer {
    fun score(result: QuestionResult): Double?
}

/**
 * 第一版可解释评分：首次答对证据最强，错误次数、提示和完整解析会降低证据强度。
 * 跳过与未作答不产生正负证据；最终答错产生 0 分证据。
 */
object DefaultMasteryEvidenceScorer : MasteryEvidenceScorer {
    override fun score(result: QuestionResult): Double? {
        val base = when (result.status) {
            QuestionCompletionStatus.FIRST_TRY_CORRECT -> 1.0
            QuestionCompletionStatus.RECOVERED_CORRECT -> when (result.wrongAttemptCount) {
                0 -> 1.0
                1 -> 0.75
                2 -> 0.60
                else -> 0.45
            }

            QuestionCompletionStatus.FINAL_INCORRECT -> 0.0
            QuestionCompletionStatus.SKIPPED,
            QuestionCompletionStatus.UNANSWERED,
            -> return null
        }

        var adjusted = base
        if (result.hintViewCount > 0) {
            adjusted *= 0.85
        }
        if (result.explanationViewed) {
            adjusted = minOf(adjusted, 0.35)
        }
        return adjusted.coerceIn(0.0, 1.0)
    }
}

object MasteryEvidenceFactory {
    fun create(
        questionSet: QuestionSetDefinition,
        summary: SessionSummary,
        scorer: MasteryEvidenceScorer = DefaultMasteryEvidenceScorer,
    ): List<MasteryEvidence> {
        require(summary.questionSetId == questionSet.id) { "summary 与 questionSet 不匹配" }

        val definitions = questionSet.questions.associateBy(QuestionDefinition::key)
        return summary.questionResults.flatMap { result ->
            val definition = definitions[result.questionKey]
                ?: error("summary 包含题组外的问题：${result.questionKey}")
            val evidenceScore = scorer.score(result) ?: return@flatMap emptyList()
            val outcome = when (result.status) {
                QuestionCompletionStatus.FIRST_TRY_CORRECT -> MasteryEvidenceOutcome.FIRST_TRY_CORRECT
                QuestionCompletionStatus.RECOVERED_CORRECT -> MasteryEvidenceOutcome.RECOVERED_CORRECT
                QuestionCompletionStatus.FINAL_INCORRECT -> MasteryEvidenceOutcome.FINAL_INCORRECT
                QuestionCompletionStatus.SKIPPED,
                QuestionCompletionStatus.UNANSWERED,
                -> error("跳过或未作答不应生成掌握度证据")
            }

            definition.knowledgeBindings.map { binding ->
                MasteryEvidence(
                    knowledgePointId = binding.knowledgePointId,
                    questionId = definition.key.id,
                    questionRevision = definition.key.revision,
                    sessionId = summary.sessionId,
                    outcome = outcome,
                    score = evidenceScore,
                    weight = binding.weight,
                    difficulty = definition.difficulty,
                    wrongAttemptCount = result.wrongAttemptCount,
                    hintViewCount = result.hintViewCount,
                    explanationViewed = result.explanationViewed,
                )
            }
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

interface MasteryPolicy {
    val version: Int

    fun update(
        current: MasteryState,
        evidence: List<MasteryEvidence>,
    ): MasteryUpdate
}

/**
 * 使用累计证据权重进行平滑更新，避免一次练习把掌握度直接推到 0 或 100。
 * 难题提供略强证据，简单题提供略弱证据，但差异被限制在 0.75 到 1.25 倍。
 */
class WeightedMasteryPolicy(
    override val version: Int = 1,
) : MasteryPolicy {
    init {
        require(version > 0) { "policy version 必须大于 0" }
    }

    override fun update(
        current: MasteryState,
        evidence: List<MasteryEvidence>,
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
