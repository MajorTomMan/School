package com.majortomman.school.learning.assessment.persistence

import com.majortomman.school.learning.assessment.domain.SessionId
import com.majortomman.school.learning.mastery.domain.MasteryUpdate

data class MasteryHistoryPoint(
    val sessionId: SessionId,
    val update: MasteryUpdate,
    val createdAtEpochMillis: Long,
)

internal fun MasterySnapshotEntity.toHistoryPoint(): MasteryHistoryPoint = MasteryHistoryPoint(
    sessionId = SessionId(sessionId),
    update = toDomain(),
    createdAtEpochMillis = createdAtEpochMillis,
)
