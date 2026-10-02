package com.majortomman.school.learning.persistence

import com.majortomman.school.learning.application.LearningDataMaintenance
import com.majortomman.school.learning.assessment.persistence.AssessmentProgressStore
import com.majortomman.school.learning.progress.persistence.CourseProgressStore

class RoomLearningDataMaintenance(
    private val courseProgressStore: CourseProgressStore,
    private val assessmentProgressStore: AssessmentProgressStore,
) : LearningDataMaintenance {
    override suspend fun clearAll() {
        courseProgressStore.clearAll()
        assessmentProgressStore.clearAll()
    }
}
