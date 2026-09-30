package com.majortomman.school.learning.assessment.domain

import com.majortomman.school.learning.evidence.domain.LearningEvidence
import com.majortomman.school.learning.evidence.domain.LearningEvidenceOutcome
import com.majortomman.school.learning.evidence.domain.LearningEvidenceSource
import com.majortomman.school.learning.evidence.domain.LearningEvidenceSourceKind

object AssessmentEvidenceFactory {
    fun create(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
        summary: SessionSummary,
    ): List<LearningEvidence> {
        require(courseId.isNotBlank()) { "courseId 不能为空" }
        require(contentRevision.isNotBlank()) { "contentRevision 不能为空" }
        require(summary.questionSetId == questionSet.id) { "summary 与 questionSet 不匹配" }

        val definitions = questionSet.questions.associateBy(QuestionDefinition::key)
        return summary.questionResults.flatMap { result ->
            val definition = definitions[result.questionKey]
                ?: error("summary 包含题组外的问题：${result.questionKey}")
            val evidenceScore = score(result) ?: return@flatMap emptyList()
            val outcome = when (result.status) {
                QuestionCompletionStatus.FIRST_TRY_CORRECT -> LearningEvidenceOutcome.FIRST_TRY_CORRECT
                QuestionCompletionStatus.RECOVERED_CORRECT -> LearningEvidenceOutcome.RECOVERED_CORRECT
                QuestionCompletionStatus.FINAL_INCORRECT -> LearningEvidenceOutcome.FINAL_INCORRECT
                QuestionCompletionStatus.SKIPPED,
                QuestionCompletionStatus.UNANSWERED,
                -> error("跳过或未作答不应生成学习证据")
            }

            definition.knowledgeBindings.map { binding ->
                LearningEvidence(
                    id = buildString {
                        append("assessment:")
                        append(summary.sessionId.value)
                        append(':')
                        append(definition.key.id.value)
                        append(':')
                        append(definition.key.revision)
                        append(':')
                        append(binding.knowledgePointId.value)
                    },
                    courseId = courseId,
                    knowledgePointId = binding.knowledgePointId,
                    source = LearningEvidenceSource(
                        kind = LearningEvidenceSourceKind.ASSESSMENT_QUESTION,
                        contextId = summary.sessionId.value,
                        itemId = definition.key.id.value,
                        itemRevision = definition.key.revision,
                        contentRevision = contentRevision,
                    ),
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

    fun score(result: QuestionResult): Double? {
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
        if (result.hintViewCount > 0) adjusted *= 0.85
        if (result.explanationViewed) adjusted = minOf(adjusted, 0.35)
        return adjusted.coerceIn(0.0, 1.0)
    }
}
