package com.majortomman.school.learning.assessment.persistence

import android.content.Context
import androidx.room.withTransaction
import com.majortomman.school.learning.assessment.domain.AssessmentSession
import com.majortomman.school.learning.assessment.domain.AssessmentSessionStatus
import com.majortomman.school.learning.assessment.domain.AttemptRecord
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.mastery.domain.MasteryState

/**
 * Assessment 有界上下文的持久化入口。
 *
 * 旧的 SchoolDatabase 继续承载历史练习与课程目录；新答题系统只通过本仓库读写事实、
 * 结算和掌握度快照，避免界面或其他模块直接跨数据库拼装统计。
 */
class AssessmentProgressStore internal constructor(
    private val database: LearningProgressDatabase,
    private val settlementPlanner: AssessmentSettlementPlanner = AssessmentSettlementPlanner(),
) {
    private val dao: AssessmentProgressDao
        get() = database.assessmentProgressDao()

    suspend fun startSession(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
        session: AssessmentSession,
    ) {
        require(session.status == AssessmentSessionStatus.IN_PROGRESS) {
            "新建会话必须处于 IN_PROGRESS"
        }
        require(session.questionSetId == questionSet.id) { "session 与 questionSet 不匹配" }
        require(session.currentQuestionKey in questionSet.questionKeys()) {
            "当前题目不属于即将开始的题组"
        }
        database.withTransaction {
            require(
                dao.findInProgressSession(
                    courseId = courseId,
                    contentRevision = contentRevision,
                    questionSetId = questionSet.id.value,
                ) == null,
            ) { "同一课程修订和题组已经存在未完成会话，应先恢复原会话" }
            dao.insertSession(session.toEntity(courseId, contentRevision))
        }
    }

    suspend fun findResumableSession(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
    ): PersistedAssessmentSession? = database.withTransaction {
        val entity = dao.findInProgressSession(
            courseId = courseId,
            contentRevision = contentRevision,
            questionSetId = questionSet.id.value,
        ) ?: return@withTransaction null
        require(entity.currentQuestionKey() in questionSet.questionKeys()) {
            "未完成会话的当前题目不属于对应课程修订"
        }
        loadPersistedSession(entity)
    }

    suspend fun moveToQuestion(
        sessionId: SessionId,
        questionSet: QuestionSetDefinition,
        questionKey: QuestionKey,
    ) {
        require(questionKey in questionSet.questionKeys()) { "目标题目不属于当前题组" }
        database.withTransaction {
            val session = requireActiveSession(sessionId)
            requireSessionMatches(session, questionSet)
            check(
                dao.updateCurrentQuestion(
                    sessionId = sessionId.value,
                    questionId = questionKey.id.value,
                    questionRevision = questionKey.revision,
                ) == 1,
            ) { "更新当前题目失败，会话可能已结束" }
        }
    }

    suspend fun recordAttempt(
        questionSet: QuestionSetDefinition,
        attempt: AttemptRecord,
    ) {
        require(attempt.questionKey in questionSet.questionKeys()) { "提交记录引用了题组外的问题" }
        database.withTransaction {
            val session = requireActiveSession(attempt.sessionId)
            requireSessionMatches(session, questionSet)
            require(
                dao.correctAttemptCount(
                    sessionId = attempt.sessionId.value,
                    questionId = attempt.questionKey.id.value,
                    questionRevision = attempt.questionKey.revision,
                ) == 0,
            ) { "题目答对后不能继续追加提交" }
            val expectedSequence = dao.maxSubmissionSequence(
                sessionId = attempt.sessionId.value,
                questionId = attempt.questionKey.id.value,
                questionRevision = attempt.questionKey.revision,
            ) + 1
            require(attempt.submissionSequence == expectedSequence) {
                "submissionSequence 必须连续，期望 $expectedSequence，实际 ${attempt.submissionSequence}"
            }
            dao.insertAttempt(attempt.toEntity())
        }
    }

    suspend fun recordEvent(
        questionSet: QuestionSetDefinition,
        persistedEvent: PersistedLearningEvent,
    ) {
        val event = persistedEvent.event
        require(event.questionKey in questionSet.questionKeys()) { "学习事件引用了题组外的问题" }
        database.withTransaction {
            val session = requireActiveSession(event.sessionId)
            requireSessionMatches(session, questionSet)
            dao.insertEvent(persistedEvent.toEntity())
        }
    }

    suspend fun restoreSession(sessionId: SessionId): PersistedAssessmentSession? =
        database.withTransaction {
            dao.findSession(sessionId.value)?.let { loadPersistedSession(it) }
        }

    suspend fun abandonSession(sessionId: SessionId) {
        database.withTransaction {
            val existing = dao.findSession(sessionId.value) ?: error("答题会话不存在：$sessionId")
            if (existing.status == AssessmentSessionStatus.ABANDONED.name) return@withTransaction
            require(existing.status == AssessmentSessionStatus.IN_PROGRESS.name) {
                "只有进行中的会话可以放弃"
            }
            check(dao.abandonSession(sessionId.value) == 1) { "放弃答题会话失败" }
        }
    }

    suspend fun settle(
        sessionId: SessionId,
        expectedContentRevision: String,
        questionSet: QuestionSetDefinition,
        completedAtEpochMillis: Long,
        settledAtEpochMillis: Long = completedAtEpochMillis,
    ): AssessmentSettlementSnapshot = database.withTransaction {
        dao.findSettlement(sessionId.value)?.let {
            return@withTransaction loadSettlement(it, alreadySettled = true)
        }

        val session = requireActiveSession(sessionId)
        requireSessionMatches(session, questionSet)
        require(session.contentRevision == expectedContentRevision) {
            "课程内容修订已变化，不能用新题目定义结算旧会话"
        }
        require(completedAtEpochMillis >= session.startedAtEpochMillis) {
            "completedAtEpochMillis 不能早于会话开始时间"
        }
        require(settledAtEpochMillis >= completedAtEpochMillis) {
            "settledAtEpochMillis 不能早于完成时间"
        }

        val attempts = dao.attemptsForSession(sessionId.value).map(AssessmentAttemptEntity::toDomain)
        val events = dao.eventsForSession(sessionId.value).map(AssessmentEventEntity::toDomain)
        val knowledgePointIds = questionSet.questions
            .flatMap(QuestionDefinition::knowledgeBindings)
            .map { it.knowledgePointId }
            .distinct()
        val currentMastery = buildMap {
            knowledgePointIds.forEach { id ->
                dao.findMasteryState(id.value)?.let { put(id, it.toDomain()) }
            }
        }
        val plan = settlementPlanner.plan(
            sessionId = sessionId,
            questionSet = questionSet,
            attempts = attempts,
            events = events.map(PersistedLearningEvent::event),
            currentMastery = currentMastery,
        )

        dao.insertQuestionResults(plan.summary.questionResults.map { it.toEntity(sessionId) })
        if (plan.evidence.isNotEmpty()) {
            dao.insertMasteryEvidence(plan.evidence.map(MasteryEvidenceEntity.Companion::fromDomain))
        }
        plan.masteryUpdates.forEach { update ->
            dao.upsertMasteryState(update.toStateEntity(settledAtEpochMillis))
            dao.insertMasterySnapshot(update.toSnapshotEntity(sessionId, settledAtEpochMillis))
        }
        dao.insertSettlement(
            plan.summary.toSettlementEntity(
                policyVersion = plan.masteryPolicyVersion,
                settledAtEpochMillis = settledAtEpochMillis,
            ),
        )
        check(
            dao.completeSession(
                sessionId = sessionId.value,
                completedAtEpochMillis = completedAtEpochMillis,
                settledAtEpochMillis = settledAtEpochMillis,
            ) == 1,
        ) { "结算时更新会话状态失败" }

        AssessmentSettlementSnapshot(
            summary = plan.summary,
            evidence = plan.evidence,
            masteryUpdates = plan.masteryUpdates,
            settledAtEpochMillis = settledAtEpochMillis,
            alreadySettled = false,
        )
    }

    suspend fun loadSettlement(sessionId: SessionId): AssessmentSettlementSnapshot? =
        database.withTransaction {
            dao.findSettlement(sessionId.value)?.let { loadSettlement(it, alreadySettled = true) }
        }

    suspend fun masteryState(knowledgePointId: KnowledgePointId): MasteryState? =
        dao.findMasteryState(knowledgePointId.value)?.toDomain()

    suspend fun masteryHistory(knowledgePointId: KnowledgePointId): List<MasteryHistoryPoint> =
        dao.masterySnapshotsForKnowledgePoint(knowledgePointId.value).map(
            MasterySnapshotEntity::toHistoryPoint,
        )

    private suspend fun loadPersistedSession(
        entity: AssessmentSessionEntity,
    ): PersistedAssessmentSession = PersistedAssessmentSession(
        courseId = entity.courseId,
        contentRevision = entity.contentRevision,
        session = entity.toDomain(),
        attempts = dao.attemptsForSession(entity.sessionId).map(AssessmentAttemptEntity::toDomain),
        events = dao.eventsForSession(entity.sessionId).map(AssessmentEventEntity::toDomain),
    )

    private suspend fun loadSettlement(
        settlement: AssessmentSettlementEntity,
        alreadySettled: Boolean,
    ): AssessmentSettlementSnapshot {
        val results = dao.questionResultsForSession(settlement.sessionId).map(
            AssessmentQuestionResultEntity::toDomain,
        )
        val summary = com.majortomman.school.learning.assessment.domain.SessionSummary(
            sessionId = SessionId(settlement.sessionId),
            questionSetId = com.majortomman.school.learning.assessment.domain.QuestionSetId(
                settlement.questionSetId,
            ),
            questionResults = results,
        )
        settlement.verifyAgainst(summary)
        return AssessmentSettlementSnapshot(
            summary = summary,
            evidence = dao.masteryEvidenceForSession(settlement.sessionId).map(
                MasteryEvidenceEntity::toDomain,
            ),
            masteryUpdates = dao.masterySnapshotsForSession(settlement.sessionId).map(
                MasterySnapshotEntity::toDomain,
            ),
            settledAtEpochMillis = settlement.settledAtEpochMillis,
            alreadySettled = alreadySettled,
        )
    }

    private suspend fun requireActiveSession(sessionId: SessionId): AssessmentSessionEntity {
        val session = dao.findSession(sessionId.value) ?: error("答题会话不存在：$sessionId")
        require(session.status == AssessmentSessionStatus.IN_PROGRESS.name) {
            "答题会话已经结束：${session.status}"
        }
        return session
    }

    private fun requireSessionMatches(
        session: AssessmentSessionEntity,
        questionSet: QuestionSetDefinition,
    ) {
        require(session.questionSetId == questionSet.id.value) {
            "持久化会话与 questionSet 不匹配"
        }
    }

    private fun AssessmentSessionEntity.currentQuestionKey(): QuestionKey = QuestionKey(
        id = com.majortomman.school.learning.assessment.domain.QuestionId(currentQuestionId),
        revision = currentQuestionRevision,
    )

    private fun QuestionSetDefinition.questionKeys(): Set<QuestionKey> =
        questions.map(QuestionDefinition::key).toSet()

    companion object {
        fun create(context: Context): AssessmentProgressStore = AssessmentProgressStore(
            database = LearningProgressDatabase.get(context),
        )
    }
}

private fun MasteryEvidenceEntity.Companion.fromDomain(
    value: com.majortomman.school.learning.mastery.domain.MasteryEvidence,
): MasteryEvidenceEntity = value.toEntity()
