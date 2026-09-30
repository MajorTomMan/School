package com.majortomman.school.learning.evidence.domain

import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.knowledge.KnowledgePointId

enum class LearningEvidenceSourceKind {
    ASSESSMENT_QUESTION,
    LESSON_ACTIVITY,
}

data class LearningEvidenceSource(
    val kind: LearningEvidenceSourceKind,
    val contextId: String,
    val itemId: String,
    val itemRevision: Int = 1,
    val contentRevision: String,
) {
    init {
        require(contextId.isNotBlank()) { "evidence source contextId 不能为空" }
        require(itemId.isNotBlank()) { "evidence source itemId 不能为空" }
        require(itemRevision > 0) { "evidence source itemRevision 必须大于 0" }
        require(contentRevision.isNotBlank()) { "evidence source contentRevision 不能为空" }
    }
}

enum class LearningEvidenceOutcome {
    FIRST_TRY_CORRECT,
    RECOVERED_CORRECT,
    FINAL_INCORRECT,
}

data class LearningEvidence(
    val id: String,
    val courseId: String,
    val knowledgePointId: KnowledgePointId,
    val source: LearningEvidenceSource,
    val outcome: LearningEvidenceOutcome,
    val score: Double,
    val weight: Double,
    val difficulty: Difficulty,
    val wrongAttemptCount: Int = 0,
    val hintViewCount: Int = 0,
    val explanationViewed: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "evidence id 不能为空" }
        require(courseId.isNotBlank()) { "courseId 不能为空" }
        require(score.isFinite() && score in 0.0..1.0) { "evidence score 必须位于 0.0 到 1.0" }
        require(weight.isFinite() && weight > 0.0) { "evidence weight 必须大于 0" }
        require(wrongAttemptCount >= 0) { "wrongAttemptCount 不能小于 0" }
        require(hintViewCount >= 0) { "hintViewCount 不能小于 0" }
    }
}
