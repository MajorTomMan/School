package com.majortomman.school.learning.assessment.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AssessmentSessionEntity::class,
        AssessmentAttemptEntity::class,
        AssessmentEventEntity::class,
        AssessmentQuestionResultEntity::class,
        AssessmentSettlementEntity::class,
        MasteryEvidenceEntity::class,
        MasteryStateEntity::class,
        MasterySnapshotEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class LearningProgressDatabase : RoomDatabase() {
    abstract fun assessmentProgressDao(): AssessmentProgressDao

    companion object {
        private const val DATABASE_NAME = "school-learning-progress.db"

        @Volatile
        private var instance: LearningProgressDatabase? = null

        fun get(context: Context): LearningProgressDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LearningProgressDatabase::class.java,
                DATABASE_NAME,
            ).build().also { instance = it }
        }
    }
}
