package com.majortomman.school.learning.evidence.persistence

import android.content.Context
import com.majortomman.school.learning.evidence.domain.LearningEvidence
import com.majortomman.school.learning.runtime.LessonEvidenceGateway

class RoomLessonEvidenceGateway private constructor(
    private val store: LearningEvidenceStore,
) : LessonEvidenceGateway {
    override suspend fun record(evidence: List<LearningEvidence>) {
        store.record(evidence)
    }

    companion object {
        fun create(context: Context): RoomLessonEvidenceGateway =
            RoomLessonEvidenceGateway(LearningEvidenceStore.create(context))
    }
}
