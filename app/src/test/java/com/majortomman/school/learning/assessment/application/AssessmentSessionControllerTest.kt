package com.majortomman.school.learning.assessment.application

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.assessment.domain.AssessmentSession
import com.majortomman.school.learning.assessment.domain.AssessmentSessionStatus
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.HintViewed
import com.majortomman.school.learning.assessment.domain.KnowledgeBinding
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionCompletionStatus
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionHint
import com.majortomman.school.learning.assessment.domain.QuestionId
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummaryCalculator
import com.majortomman.school.learning.assessment.domain.UserAnswer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentSessionControllerTest {
    @Test
    fun recordsTwoWrongAnswersThenLocksRecoveredCorrectQuestion() = runBlocking {
        val gateway = FakeGateway()
        val controller = controller(gateway, questionSet(questionCount = 1))

        controller.dispatch(AssessmentIntent.Initialize)
        submitText(controller, "1")
        submitText(controller, "2")
        submitText(controller, "3")

        val page = (controller.state.value as AssessmentState.Question).page
        assertEquals(3, page.progress.totalSubmissionCount)
        assertEquals(2, page.progress.wrongAttemptCount)
        assertEquals(QuestionCompletionStatus.RECOVERED_CORRECT, page.progress.completionStatus)
        assertTrue(page.progress.answerLocked)
        assertFalse(page.canSubmit)
        assertEquals(3, gateway.requireFacts().attempts.size)
    }

    @Test
    fun invalidInputIsTrackedWithoutIncreasingWrongCount() = runBlocking {
        val controller = controller(FakeGateway(), questionSet(questionCount = 1))

        controller.dispatch(AssessmentIntent.Initialize)
        submitText(controller, "-")

        val progress = (controller.state.value as AssessmentState.Question).page.progress
        assertEquals(1, progress.invalidSubmissionCount)
        assertEquals(0, progress.wrongAttemptCount)
        assertEquals(QuestionCompletionStatus.UNANSWERED, progress.completionStatus)
    }

    @Test
    fun skippedQuestionsAreConfirmedBeforeSettlement() = runBlocking {
        val gateway = FakeGateway()
        val controller = controller(gateway, questionSet(questionCount = 2))

        controller.dispatch(AssessmentIntent.Initialize)
        controller.dispatch(AssessmentIntent.SkipQuestion)
        assertEquals(1, (controller.state.value as AssessmentState.Question).page.questionIndex)

        controller.dispatch(AssessmentIntent.SkipQuestion)
        val confirmation = controller.state.value as AssessmentState.FinishConfirmation
        assertEquals(listOf(0, 1), confirmation.incompleteQuestionIndices)
        assertEquals(2, confirmation.preview.skippedCount)

        controller.dispatch(AssessmentIntent.ConfirmFinish)
        val result = controller.state.value as AssessmentState.Result
        assertEquals(2, result.completion.summary.skippedCount)
        assertEquals(1, gateway.completeCallCount)

        controller.dispatch(AssessmentIntent.ConfirmFinish)
        assertEquals(1, gateway.completeCallCount)
    }

    @Test
    fun continueIncompleteReturnsToFirstSkippedQuestion() = runBlocking {
        val controller = controller(FakeGateway(), questionSet(questionCount = 2))

        controller.dispatch(AssessmentIntent.Initialize)
        controller.dispatch(AssessmentIntent.SkipQuestion)
        submitText(controller, "3")
        controller.dispatch(AssessmentIntent.NextQuestion)

        val confirmation = controller.state.value as AssessmentState.FinishConfirmation
        assertEquals(listOf(0), confirmation.incompleteQuestionIndices)

        controller.dispatch(AssessmentIntent.ContinueIncomplete)
        assertEquals(0, (controller.state.value as AssessmentState.Question).page.questionIndex)
    }

    @Test
    fun restoresCurrentQuestionAndLatestSubmittedDraft() = runBlocking {
        val set = questionSet(questionCount = 2)
        val session = AssessmentSession(
            id = SessionId("session-existing"),
            questionSetId = set.id,
            currentQuestionKey = set.questions[1].key,
            status = AssessmentSessionStatus.IN_PROGRESS,
            startedAtEpochMillis = 10,
        )
        val gateway = FakeGateway(
            initial = AssessmentSessionFacts(
                courseId = "course-1",
                contentRevision = "revision-1",
                session = session,
                attempts = listOf(
                    attempt(
                        sessionId = session.id,
                        question = set.questions[1],
                        answer = UserAnswer.Text("2"),
                        sequence = 1,
                    ),
                ),
                events = emptyList(),
            ),
        )
        val controller = controller(gateway, set)

        controller.dispatch(AssessmentIntent.Initialize)

        val page = (controller.state.value as AssessmentState.Question).page
        assertEquals(1, page.questionIndex)
        assertEquals(UserAnswer.Text("2"), page.draftAnswer)
        assertEquals(1, page.progress.wrongAttemptCount)
        assertEquals(0, gateway.startCallCount)
    }

    @Test
    fun hintViewIsPersistedOnlyOncePerHint() = runBlocking {
        val gateway = FakeGateway()
        val controller = controller(gateway, questionSet(questionCount = 1))

        controller.dispatch(AssessmentIntent.Initialize)
        controller.dispatch(AssessmentIntent.ViewHint("hint-1"))
        controller.dispatch(AssessmentIntent.ViewHint("hint-1"))

        val page = (controller.state.value as AssessmentState.Question).page
        assertEquals(setOf("hint-1"), page.progress.viewedHintIds)
        assertEquals(1, gateway.requireFacts().events.filterIsInstance<HintViewed>().size)
    }

    @Test
    fun navigationPersistsCurrentQuestion() = runBlocking {
        val gateway = FakeGateway()
        val controller = controller(gateway, questionSet(questionCount = 3))

        controller.dispatch(AssessmentIntent.Initialize)
        controller.dispatch(AssessmentIntent.GoToQuestion(2))

        assertEquals(2, (controller.state.value as AssessmentState.Question).page.questionIndex)
        assertEquals(2, gateway.lastMovedIndex)
        assertEquals(gateway.requireFacts().session.currentQuestionKey, questionSet(3).questions[2].key)
    }

    private suspend fun submitText(controller: AssessmentSessionController, value: String) {
        controller.dispatch(AssessmentIntent.AnswerChanged(UserAnswer.Text(value)))
        controller.dispatch(AssessmentIntent.SubmitAnswer)
    }

    private fun controller(
        gateway: FakeGateway,
        set: QuestionSetDefinition,
    ): AssessmentSessionController {
        var now = 100L
        var id = 0
        return AssessmentSessionController(
            courseId = "course-1",
            contentRevision = "revision-1",
            questionSet = set,
            gateway = gateway,
            clock = AssessmentClock { now++ },
            idFactory = AssessmentIdFactory { prefix -> "$prefix-${++id}" },
        )
    }

    private fun questionSet(questionCount: Int): QuestionSetDefinition = QuestionSetDefinition(
        id = QuestionSetId("set-1"),
        title = "有理数检测",
        questions = (1..questionCount).map { index ->
            QuestionDefinition(
                key = QuestionKey(QuestionId("question-$index"), 1),
                number = index.toString(),
                inputSpec = AnswerInputSpec.Integer,
                answerRule = AnswerRule.ExactInteger(3),
                knowledgeBindings = listOf(
                    KnowledgeBinding(KnowledgePointId("knowledge-$index"), 1.0),
                ),
                difficulty = Difficulty(0.4),
                hints = listOf(QuestionHint("hint-1", "先观察符号")),
            )
        },
    )

    private fun attempt(
        sessionId: SessionId,
        question: QuestionDefinition,
        answer: UserAnswer,
        sequence: Int,
    ): AttemptRecord = AttemptRecord(
        id = com.majortomman.school.learning.assessment.domain.AttemptId("restored-$sequence"),
        sessionId = sessionId,
        questionKey = question.key,
        submissionSequence = sequence,
        answer = answer,
        result = com.majortomman.school.learning.assessment.judge.DefaultAssessmentAnswerJudge.judge(
            question,
            answer,
        ),
        submittedAtEpochMillis = sequence.toLong(),
    )

    private class FakeGateway(
        initial: AssessmentSessionFacts? = null,
    ) : AssessmentSessionGateway {
        private var facts: AssessmentSessionFacts? = initial
        private var cachedCompletion: AssessmentCompletion? = null
        var startCallCount: Int = 0
        var completeCallCount: Int = 0
        var lastMovedIndex: Int? = null

        override suspend fun findResumable(
            courseId: String,
            contentRevision: String,
            questionSet: QuestionSetDefinition,
        ): AssessmentSessionFacts? = facts

        override suspend fun start(
            courseId: String,
            contentRevision: String,
            questionSet: QuestionSetDefinition,
            session: AssessmentSession,
        ) {
            startCallCount++
            facts = AssessmentSessionFacts(
                courseId = courseId,
                contentRevision = contentRevision,
                session = session,
                attempts = emptyList(),
                events = emptyList(),
            )
        }

        override suspend fun moveToQuestion(
            sessionId: SessionId,
            questionSet: QuestionSetDefinition,
            questionIndex: Int,
        ) {
            lastMovedIndex = questionIndex
            val current = requireFacts()
            facts = current.copy(
                session = current.session.copy(
                    currentQuestionKey = questionSet.questions[questionIndex].key,
                ),
            )
        }

        override suspend fun appendAttempt(
            questionSet: QuestionSetDefinition,
            attempt: AttemptRecord,
        ) {
            val current = requireFacts()
            facts = current.copy(attempts = current.attempts + attempt)
        }

        override suspend fun appendEvent(
            eventId: String,
            questionSet: QuestionSetDefinition,
            event: QuestionLearningEvent,
        ) {
            val current = requireFacts()
            facts = current.copy(events = current.events + event)
        }

        override suspend fun complete(
            sessionId: SessionId,
            contentRevision: String,
            questionSet: QuestionSetDefinition,
            completedAtEpochMillis: Long,
        ): AssessmentCompletion {
            cachedCompletion?.let { return it.copy(alreadySettled = true) }
            completeCallCount++
            val current = requireFacts()
            val summary = SessionSummaryCalculator.summarize(
                sessionId = sessionId,
                questionSet = questionSet,
                attempts = current.attempts,
                events = current.events,
            )
            return AssessmentCompletion(
                summary = summary,
                evidence = emptyList(),
                masteryUpdates = emptyList(),
                settledAtEpochMillis = completedAtEpochMillis,
                alreadySettled = false,
            ).also { cachedCompletion = it }
        }

        fun requireFacts(): AssessmentSessionFacts = checkNotNull(facts)
    }
}
