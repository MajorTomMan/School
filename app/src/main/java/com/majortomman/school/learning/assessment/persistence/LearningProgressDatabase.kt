package com.majortomman.school.learning.assessment.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 只保存新版 Assessment 的追加事实与结算快照。
 *
 * 它有意与承载旧练习、课程目录和兼容数据的 SchoolDatabase 分离，使答题结算能够独立演进；
 * 业务层必须通过 AssessmentProgressStore 访问，不能在界面层跨库拼装掌握度。
 */
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
