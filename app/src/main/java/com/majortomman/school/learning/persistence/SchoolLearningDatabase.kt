package com.majortomman.school.learning.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.majortomman.school.learning.assessment.persistence.AssessmentAttemptEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentEventEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentProgressDao
import com.majortomman.school.learning.assessment.persistence.AssessmentQuestionResultEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentSessionEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentSettlementEntity
import com.majortomman.school.learning.evidence.persistence.LearningEvidenceDao
import com.majortomman.school.learning.evidence.persistence.LearningEvidenceEntity
import com.majortomman.school.learning.evidence.persistence.MasteryStateEntity
import com.majortomman.school.learning.evidence.persistence.MasteryUpdateSnapshotEntity
import com.majortomman.school.learning.progress.persistence.CourseLessonProgressEntity
import com.majortomman.school.learning.progress.persistence.CourseProgressDao

/**
 * School 的统一学习数据数据库。
 *
 * 课程进度、Assessment 事实、通用 LearningEvidence 和 Mastery projection 位于同一个
 * 持久化边界。各业务域只能通过自己的 Store 访问数据，UI 不直接访问 Room。
 */
@Database(
    entities = [
        AssessmentSessionEntity::class,
        AssessmentAttemptEntity::class,
        AssessmentEventEntity::class,
        AssessmentQuestionResultEntity::class,
        AssessmentSettlementEntity::class,
        LearningEvidenceEntity::class,
        MasteryStateEntity::class,
        MasteryUpdateSnapshotEntity::class,
        CourseLessonProgressEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
internal abstract class SchoolLearningDatabase : RoomDatabase() {
    abstract fun assessmentProgressDao(): AssessmentProgressDao
    abstract fun learningEvidenceDao(): LearningEvidenceDao
    abstract fun courseProgressDao(): CourseProgressDao

    companion object {
        private const val DATABASE_NAME = "school-learning-progress.db"

        @Volatile
        private var instance: SchoolLearningDatabase? = null

        fun get(context: Context): SchoolLearningDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SchoolLearningDatabase::class.java,
                DATABASE_NAME,
            ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
        }
    }
}
