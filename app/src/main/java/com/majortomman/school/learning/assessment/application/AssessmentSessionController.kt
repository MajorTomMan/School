package com.majortomman.school.learning.assessment.application

import com.majortomman.school.learning.assessment.domain.AssessmentSession
import com.majortomman.school.learning.assessment.domain.AssessmentSessionStatus
import com.majortomman.school.learning.assessment.domain.AttemptId
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.ExplanationViewed
import com.majortomman.school.learning.assessment.domain.HintViewed
import com.majortomman.school.learning.assessment.domain.QuestionCompletionStatus
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.QuestionLearningEvent
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.QuestionSkipped
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.assessment.domain.SessionSummary
import com.majortomman.school.learning.assessment.domain.SessionSummaryCalculator
import com.majortomman.school.learning.assessment.domain.UserAnswer
import com.majortomman.school.learning.assessment.judge.AssessmentAnswerJudge
import com.majortomman.school.learning.assessment.judge.DefaultAssessmentAnswerJudge
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 一次题组会话的应用层状态机。
 *
 * 页面只向它发送 [AssessmentIntent] 并订阅 [state]；判题、跳题、导航、恢复和最终结算
 * 均由本控制器串行处理，避免 Compose 页面直接维护业务状态。
 */
class AssessmentSessionController(
    private val courseId: String,
    private val contentRevision: String,
    private val questionSet: QuestionSetDefinition,
    private val gateway: AssessmentSessionGateway,
    private val answerJudge: AssessmentAnswerJudge = DefaultAssessmentAnswerJudge,
    private val clock: AssessmentClock = AssessmentClock(System::currentTimeMillis),
    private val idFactory: AssessmentIdFactory = AssessmentIdFactory { prefix ->
        "$prefix-${UUID.randomUUID()}"
    },
) {
    init {
        require(courseId.isNotBlank()) { "courseId 不能为空" }
        require(contentRevision.isNotBlank()) { "contentRevision 不能为空" }
    }

    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<AssessmentState>(AssessmentState.Idle)
    private var facts: AssessmentSessionFacts? = null
    private val drafts = linkedMapOf<QuestionKey, UserAnswer?>()
    private val workDrafts = linkedMapOf<QuestionKey, String>()

    val state: StateFlow<AssessmentState> = mutableState.asStateFlow()

    suspend fun dispatch(intent: AssessmentIntent) {
        mutex.withLock {
            val stableState = mutableState.value.unwrapError()
            runCatching { handle(intent) }
                .onFailure { error ->
                    mutableState.value = AssessmentState.Error(
                        previous = stableState,
                        message = error.message ?: "答题流程发生未知错误",
                    )
                }
        }
    }

    private suspend fun handle(intent: AssessmentIntent) {
        when (intent) {
            AssessmentIntent.Initialize -> initialize()
            is AssessmentIntent.WorkProcessChanged -> changeWorkProcess(intent.process)
            is AssessmentIntent.AnswerChanged -> changeAnswer(intent.answer)
            AssessmentIntent.SubmitAnswer -> submitAnswer()
            AssessmentIntent.SkipQuestion -> skipQuestion()
            AssessmentIntent.PreviousQuestion -> navigateRelative(-1)
            AssessmentIntent.NextQuestion -> navigateRelative(1)
            is AssessmentIntent.GoToQuestion -> navigateTo(intent.index)
            is AssessmentIntent.ViewHint -> viewHint(intent.hintId)
            AssessmentIntent.ViewExplanation -> viewExplanation()
            AssessmentIntent.RequestFinish -> requestFinish()
            AssessmentIntent.ContinueIncomplete -> continueIncomplete()
            AssessmentIntent.ConfirmFinish -> completeAssessment()
            AssessmentIntent.CancelFinish -> cancelFinish()
            AssessmentIntent.DismissError -> dismissError()
        }
    }

    private suspend fun initialize() {
        if (facts != null) {
            renderCurrentQuestion()
            return
        }
        mutableState.value = AssessmentState.Loading

        val restored = gateway.findResumable(courseId, contentRevision, questionSet)
        facts = if (restored != null) {
            validateRestored(restored)
            restored
        } else {
            val now = clock.nowEpochMillis()
            val first = questionSet.questions.first()
            val session = AssessmentSession(
                id = SessionId(idFactory.createId("session")),
                questionSetId = questionSet.id,
                currentQuestionKey = first.key,
                status = AssessmentSessionStatus.IN_PROGRESS,
                startedAtEpochMillis = now,
            )
            gateway.start(courseId, contentRevision, questionSet, session)
            AssessmentSessionFacts(
                courseId = courseId,
                contentRevision = contentRevision,
                session = session,
                attempts = emptyList(),
                events = emptyList(),
            )
        }

        restoreDrafts(requireFacts())
        renderCurrentQuestion()
    }

    private fun changeWorkProcess(process: String) {
        val page = mutableState.value.currentPage() ?: return
        if (page.progress.answerLocked || page.busy) return
        require(process.length <= AttemptRecord.MAX_WORK_PROCESS_LENGTH) { "做题过程过长" }
        workDrafts[page.question.key] = process
        renderCurrentQuestion()
    }

    private fun changeAnswer(answer: UserAnswer?) {
        val page = mutableState.value.currentPage() ?: return
        if (page.progress.answerLocked || page.busy) return
        drafts[page.question.key] = answer
        renderCurrentQuestion()
    }

    private suspend fun submitAnswer() {
        val page = mutableState.value.currentPage() ?: return
        if (page.progress.answerLocked || page.busy) return
        val answer = page.draftAnswer ?: return
        val currentFacts = requireFacts()
        val now = clock.nowEpochMillis()
        val sequence = currentFacts.attempts
            .filter { it.questionKey == page.question.key }
            .maxOfOrNull(AttemptRecord::submissionSequence)
            ?.plus(1)
            ?: 1
        val attempt = AttemptRecord(
            id = AttemptId(idFactory.createId("attempt")),
            sessionId = currentFacts.session.id,
            questionKey = page.question.key,
            submissionSequence = sequence,
            answer = answer,
            workProcess = page.draftWorkProcess.trim(),
            result = answerJudge.judge(page.question, answer),
            submittedAtEpochMillis = now,
        )

        setBusy(true)
        gateway.appendAttempt(questionSet, attempt)
        facts = currentFacts.copy(attempts = currentFacts.attempts + attempt)
        renderCurrentQuestion()
    }

    private suspend fun skipQuestion() {
        require(questionSet.allowSkip) { "当前题组不允许跳过题目" }
        val page = mutableState.value.currentPage() ?: return
        if (page.busy) return
        val currentFacts = requireFacts()
        val alreadySkipped = currentFacts.events.any {
            it is QuestionSkipped && it.questionKey == page.question.key
        }
        if (!alreadySkipped) {
            val event = QuestionSkipped(
                sessionId = currentFacts.session.id,
                questionKey = page.question.key,
                occurredAtEpochMillis = clock.nowEpochMillis(),
            )
            setBusy(true)
            gateway.appendEvent(
                eventId = idFactory.createId("event"),
                questionSet = questionSet,
                event = event,
            )
            facts = currentFacts.copy(events = currentFacts.events + event)
        }

        if (page.questionIndex == questionSet.questions.lastIndex) {
            requestFinish()
        } else {
            navigateTo(page.questionIndex + 1)
        }
    }

    private suspend fun navigateRelative(offset: Int) {
        val page = mutableState.value.currentPage() ?: return
        val target = page.questionIndex + offset
        if (target !in questionSet.questions.indices) {
            if (offset > 0 && page.questionIndex == questionSet.questions.lastIndex) {
                requestFinish()
            }
            return
        }
        navigateTo(target)
    }

    private suspend fun navigateTo(index: Int) {
        require(index in questionSet.questions.indices) { "题目索引越界：$index" }
        val currentFacts = requireFacts()
        val target = questionSet.questions[index]
        if (currentFacts.session.currentQuestionKey != target.key) {
            setBusy(true)
            gateway.moveToQuestion(currentFacts.session.id, questionSet, index)
            facts = currentFacts.copy(
                session = currentFacts.session.copy(currentQuestionKey = target.key),
            )
        }
        renderCurrentQuestion()
    }

    private suspend fun viewHint(hintId: String) {
        val page = mutableState.value.currentPage() ?: return
        require(page.question.hints.any { it.id == hintId }) { "提示不属于当前题目：$hintId" }
        if (hintId in page.progress.viewedHintIds) return
        val currentFacts = requireFacts()
        val event = HintViewed(
            sessionId = currentFacts.session.id,
            questionKey = page.question.key,
            hintId = hintId,
            occurredAtEpochMillis = clock.nowEpochMillis(),
        )
        setBusy(true)
        gateway.appendEvent(idFactory.createId("event"), questionSet, event)
        facts = currentFacts.copy(events = currentFacts.events + event)
        renderCurrentQuestion()
    }

    private suspend fun viewExplanation() {
        val page = mutableState.value.currentPage() ?: return
        if (page.progress.explanationViewed) return
        val currentFacts = requireFacts()
        val event = ExplanationViewed(
            sessionId = currentFacts.session.id,
            questionKey = page.question.key,
            occurredAtEpochMillis = clock.nowEpochMillis(),
        )
        setBusy(true)
        gateway.appendEvent(idFactory.createId("event"), questionSet, event)
        facts = currentFacts.copy(events = currentFacts.events + event)
        renderCurrentQuestion()
    }

    private suspend fun requestFinish() {
        val page = mutableState.value.currentPage()
            ?: (mutableState.value as? AssessmentState.FinishConfirmation)?.page
            ?: return
        val preview = summarize()
        val incomplete = preview.questionResults.mapIndexedNotNull { index, result ->
            index.takeIf {
                result.status == QuestionCompletionStatus.UNANSWERED ||
                    result.status == QuestionCompletionStatus.SKIPPED
            }
        }
        if (incomplete.isEmpty()) {
            completeAssessment()
        } else {
            mutableState.value = AssessmentState.FinishConfirmation(
                page = page.copy(busy = false),
                preview = preview,
                incompleteQuestionIndices = incomplete,
            )
        }
    }

    private suspend fun continueIncomplete() {
        val confirmation = mutableState.value as? AssessmentState.FinishConfirmation ?: return
        val first = confirmation.incompleteQuestionIndices.firstOrNull() ?: run {
            completeAssessment()
            return
        }
        navigateTo(first)
    }

    private suspend fun completeAssessment() {
        val currentFacts = requireFacts()
        val now = clock.nowEpochMillis()
        setBusy(true)
        val completion = gateway.complete(
            sessionId = currentFacts.session.id,
            contentRevision = contentRevision,
            questionSet = questionSet,
            completedAtEpochMillis = now,
        )
        facts = currentFacts.copy(
            session = currentFacts.session.copy(
                status = AssessmentSessionStatus.COMPLETED,
                completedAtEpochMillis = now,
            ),
        )
        mutableState.value = AssessmentState.Result(completion)
    }

    private fun cancelFinish() {
        val confirmation = mutableState.value as? AssessmentState.FinishConfirmation ?: return
        mutableState.value = AssessmentState.Question(confirmation.page.copy(busy = false))
    }

    private fun dismissError() {
        val error = mutableState.value as? AssessmentState.Error ?: return
        mutableState.value = error.previous
    }

    private fun validateRestored(restored: AssessmentSessionFacts) {
        require(restored.courseId == courseId) { "恢复会话的 courseId 不匹配" }
        require(restored.contentRevision == contentRevision) { "恢复会话的内容修订不匹配" }
        require(restored.session.questionSetId == questionSet.id) { "恢复会话的题组不匹配" }
        require(restored.session.status == AssessmentSessionStatus.IN_PROGRESS) {
            "只能恢复进行中的答题会话"
        }
        require(questionSet.questions.any { it.key == restored.session.currentQuestionKey }) {
            "恢复会话的当前题目不属于课程题组"
        }
    }

    private fun restoreDrafts(restored: AssessmentSessionFacts) {
        drafts.clear()
        workDrafts.clear()
        restored.attempts
            .groupBy(AttemptRecord::questionKey)
            .forEach { (key, attempts) ->
                val latest = attempts.maxWithOrNull(
                    compareBy<AttemptRecord>(AttemptRecord::submittedAtEpochMillis)
                        .thenBy(AttemptRecord::submissionSequence),
                )
                drafts[key] = latest?.answer
                workDrafts[key] = latest?.workProcess.orEmpty()
            }
    }

    private fun renderCurrentQuestion() {
        val currentFacts = requireFacts()
        val index = questionSet.questions.indexOfFirst {
            it.key == currentFacts.session.currentQuestionKey
        }
        check(index >= 0) { "当前题目不属于题组" }
        val allProgress = progressStates(currentFacts)
        val questionKey = questionSet.questions[index].key
        mutableState.value = AssessmentState.Question(
            AssessmentQuestionPageState(
                sessionId = currentFacts.session.id,
                questionIndex = index,
                questionCount = questionSet.questions.size,
                question = questionSet.questions[index],
                draftWorkProcess = workDrafts[questionKey].orEmpty(),
                draftAnswer = drafts[questionKey],
                progress = allProgress[index],
                allProgress = allProgress,
            ),
        )
    }

    private fun progressStates(currentFacts: AssessmentSessionFacts): List<AssessmentQuestionProgressState> {
        val summary = SessionSummaryCalculator.summarize(
            sessionId = currentFacts.session.id,
            questionSet = questionSet,
            attempts = currentFacts.attempts,
            events = currentFacts.events,
        )
        val attemptsByQuestion = currentFacts.attempts.groupBy(AttemptRecord::questionKey)
        return summary.questionResults.map { result ->
            val latest = attemptsByQuestion[result.questionKey]
                .orEmpty()
                .maxWithOrNull(
                    compareBy<AttemptRecord>(AttemptRecord::submittedAtEpochMillis)
                        .thenBy(AttemptRecord::submissionSequence),
                )
            val questionEvents = currentFacts.events.filter { it.questionKey == result.questionKey }
            AssessmentQuestionProgressState(
                questionKey = result.questionKey,
                completionStatus = result.status,
                totalSubmissionCount = result.totalSubmissionCount,
                wrongAttemptCount = result.wrongAttemptCount,
                invalidSubmissionCount = result.invalidSubmissionCount,
                viewedHintIds = questionEvents.filterIsInstance<HintViewed>().map(HintViewed::hintId).toSet(),
                explanationViewed = questionEvents.any { it is ExplanationViewed },
                latestJudgeResult = latest?.result,
            )
        }
    }

    private fun summarize(): SessionSummary {
        val currentFacts = requireFacts()
        return SessionSummaryCalculator.summarize(
            sessionId = currentFacts.session.id,
            questionSet = questionSet,
            attempts = currentFacts.attempts,
            events = currentFacts.events,
        )
    }

    private fun setBusy(busy: Boolean) {
        mutableState.value = when (val current = mutableState.value) {
            is AssessmentState.Question -> current.copy(page = current.page.copy(busy = busy))
            is AssessmentState.FinishConfirmation -> current.copy(
                page = current.page.copy(busy = busy),
                busy = busy,
            )

            else -> current
        }
    }

    private fun requireFacts(): AssessmentSessionFacts =
        facts ?: error("答题状态机尚未初始化")

    private fun AssessmentState.currentPage(): AssessmentQuestionPageState? = when (this) {
        is AssessmentState.Question -> page
        is AssessmentState.FinishConfirmation -> page
        is AssessmentState.Error -> previous.currentPage()
        else -> null
    }

    private fun AssessmentState.unwrapError(): AssessmentState =
        if (this is AssessmentState.Error) previous.unwrapError() else this
}
