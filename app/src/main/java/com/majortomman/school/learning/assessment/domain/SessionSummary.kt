package com.majortomman.school.learning.assessment.domain

enum class QuestionCompletionStatus {
    UNANSWERED,
    SKIPPED,
    FIRST_TRY_CORRECT,
    RECOVERED_CORRECT,
    FINAL_INCORRECT,
}

data class QuestionResult(
    val questionKey: QuestionKey,
    val status: QuestionCompletionStatus,
    val totalSubmissionCount: Int,
    val validAttemptCount: Int,
    val invalidSubmissionCount: Int,
    val wrongAttemptCount: Int,
    val hintViewCount: Int,
    val explanationViewed: Boolean,
    val wasEverSkipped: Boolean,
) {
    val firstAttemptCorrect: Boolean
        get() = status == QuestionCompletionStatus.FIRST_TRY_CORRECT

    val finalCorrect: Boolean
        get() = status == QuestionCompletionStatus.FIRST_TRY_CORRECT ||
            status == QuestionCompletionStatus.RECOVERED_CORRECT

    val recoveredAfterError: Boolean
        get() = status == QuestionCompletionStatus.RECOVERED_CORRECT

    val finalWrong: Boolean
        get() = status == QuestionCompletionStatus.FINAL_INCORRECT

    val currentlySkipped: Boolean
        get() = status == QuestionCompletionStatus.SKIPPED

    val attempted: Boolean
        get() = validAttemptCount > 0
}

data class SessionSummary(
    val sessionId: SessionId,
    val questionSetId: QuestionSetId,
    val questionResults: List<QuestionResult>,
) {
    val totalQuestionCount: Int = questionResults.size
    val attemptedQuestionCount: Int = questionResults.count(QuestionResult::attempted)
    val firstCorrectCount: Int = questionResults.count(QuestionResult::firstAttemptCorrect)
    val recoveredCorrectCount: Int = questionResults.count(QuestionResult::recoveredAfterError)
    val finalCorrectCount: Int = questionResults.count(QuestionResult::finalCorrect)
    val finalWrongCount: Int = questionResults.count(QuestionResult::finalWrong)
    val skippedCount: Int = questionResults.count(QuestionResult::currentlySkipped)
    val unansweredCount: Int = questionResults.count {
        it.status == QuestionCompletionStatus.UNANSWERED
    }
    val wrongQuestionCount: Int = questionResults.count { it.wrongAttemptCount > 0 }
    val wrongSubmissionCount: Int = questionResults.sumOf(QuestionResult::wrongAttemptCount)
    val invalidSubmissionCount: Int = questionResults.sumOf(QuestionResult::invalidSubmissionCount)
    val hintViewCount: Int = questionResults.sumOf(QuestionResult::hintViewCount)
    val hintedQuestionCount: Int = questionResults.count { it.hintViewCount > 0 }
    val explanationViewedQuestionCount: Int = questionResults.count(QuestionResult::explanationViewed)

    val firstCorrectRate: Double = ratio(firstCorrectCount, totalQuestionCount)
    val finalCorrectRate: Double = ratio(finalCorrectCount, totalQuestionCount)
    val completionRate: Double = ratio(attemptedQuestionCount, totalQuestionCount)
    val firstAttemptAccuracyAmongAttempted: Double = ratio(firstCorrectCount, attemptedQuestionCount)

    init {
        require(questionResults.isNotEmpty()) { "SessionSummary 至少需要一道题" }
        require(questionResults.map(QuestionResult::questionKey).distinct().size == questionResults.size) {
            "SessionSummary 不能包含重复题目"
        }
    }

    companion object {
        private fun ratio(numerator: Int, denominator: Int): Double =
            if (denominator == 0) 0.0 else numerator.toDouble() / denominator.toDouble()
    }
}

object SessionSummaryCalculator {
    fun summarize(
        sessionId: SessionId,
        questionSet: QuestionSetDefinition,
        attempts: List<AttemptRecord>,
        events: List<QuestionLearningEvent>,
    ): SessionSummary {
        validateFacts(sessionId, questionSet, attempts, events)

        val attemptsByQuestion = attempts.groupBy(AttemptRecord::questionKey)
        val eventsByQuestion = events.groupBy(QuestionLearningEvent::questionKey)
        val results = questionSet.questions.map { definition ->
            summarizeQuestion(
                definition = definition,
                attempts = attemptsByQuestion[definition.key].orEmpty(),
                events = eventsByQuestion[definition.key].orEmpty(),
            )
        }
        return SessionSummary(
            sessionId = sessionId,
            questionSetId = questionSet.id,
            questionResults = results,
        )
    }

    private fun validateFacts(
        sessionId: SessionId,
        questionSet: QuestionSetDefinition,
        attempts: List<AttemptRecord>,
        events: List<QuestionLearningEvent>,
    ) {
        require(attempts.map(AttemptRecord::id).distinct().size == attempts.size) {
            "attempt id 不能重复"
        }
        require(attempts.all { it.sessionId == sessionId }) {
            "存在不属于当前 session 的提交记录"
        }
        require(events.all { it.sessionId == sessionId }) {
            "存在不属于当前 session 的学习事件"
        }

        val questionKeys = questionSet.questions.map(QuestionDefinition::key).toSet()
        require(attempts.all { it.questionKey in questionKeys }) {
            "提交记录引用了题组外的问题"
        }
        require(events.all { it.questionKey in questionKeys }) {
            "学习事件引用了题组外的问题"
        }

        attempts.groupBy(AttemptRecord::questionKey).forEach { (_, records) ->
            require(records.map(AttemptRecord::submissionSequence).distinct().size == records.size) {
                "同一题目的 submissionSequence 不能重复"
            }
        }
    }

    private fun summarizeQuestion(
        definition: QuestionDefinition,
        attempts: List<AttemptRecord>,
        events: List<QuestionLearningEvent>,
    ): QuestionResult {
        val orderedAttempts = attempts.sortedWith(
            compareBy<AttemptRecord>(AttemptRecord::submittedAtEpochMillis)
                .thenBy(AttemptRecord::submissionSequence),
        )

        // 一旦首次答对，本次题目结果即锁定；异常产生的后续记录不能污染统计。
        val firstCorrectIndex = orderedAttempts.indexOfFirst {
            it.result.outcome == JudgeOutcome.CORRECT
        }
        val effectiveAttempts = if (firstCorrectIndex >= 0) {
            orderedAttempts.take(firstCorrectIndex + 1)
        } else {
            orderedAttempts
        }

        val validAttempts = effectiveAttempts.filter { it.result.countsAsValidAttempt }
        val wrongAttemptCount = validAttempts.count { it.result.countsAsWrongAttempt }
        val firstValidAttempt = validAttempts.firstOrNull()
        val finalCorrect = validAttempts.any { it.result.outcome == JudgeOutcome.CORRECT }
        val wasEverSkipped = events.any { it is QuestionSkipped }
        val hintViewCount = events.count { it is HintViewed }
        val explanationViewed = events.any { it is ExplanationViewed }

        val status = when {
            finalCorrect && firstValidAttempt?.result?.outcome == JudgeOutcome.CORRECT ->
                QuestionCompletionStatus.FIRST_TRY_CORRECT

            finalCorrect -> QuestionCompletionStatus.RECOVERED_CORRECT
            validAttempts.isNotEmpty() -> QuestionCompletionStatus.FINAL_INCORRECT
            wasEverSkipped -> QuestionCompletionStatus.SKIPPED
            else -> QuestionCompletionStatus.UNANSWERED
        }

        return QuestionResult(
            questionKey = definition.key,
            status = status,
            totalSubmissionCount = effectiveAttempts.size,
            validAttemptCount = validAttempts.size,
            invalidSubmissionCount = effectiveAttempts.count {
                it.result.outcome == JudgeOutcome.INVALID_INPUT
            },
            wrongAttemptCount = wrongAttemptCount,
            hintViewCount = hintViewCount,
            explanationViewed = explanationViewed,
            wasEverSkipped = wasEverSkipped,
        )
    }
}
