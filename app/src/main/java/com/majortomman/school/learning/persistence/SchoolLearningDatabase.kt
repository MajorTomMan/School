package com.majortomman.school.learning.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.majortomman.school.learning.assessment.persistence.AssessmentAttemptEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentEventEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentProgressDao
import com.majortomman.school.learning.assessment.persistence.AssessmentQuestionResultEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentSessionEntity
import com.majortomman.school.learning.assessment.persistence.AssessmentSettlementEntity
import com.majortomman.school.learning.assessment.persistence.MasteryEvidenceEntity
import com.majortomman.school.learning.assessment.persistence.MasterySnapshotEntity
import com.majortomman.school.learning.assessment.persistence.MasteryStateEntity
import com.majortomman.school.learning.progress.persistence.CourseLessonProgressEntity
import com.majortomman.school.learning.progress.persistence.CourseProgressDao

/**
 * School 的统一学习数据数据库。
 *
 * 课程进度、Assessment 事实、结算快照和知识掌握状态都落在这一持久化边界中。
 * 各业务域只能通过自己的 Store 访问数据，UI 不直接访问 Room。
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
        CourseLessonProgressEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
internal abstract class SchoolLearningDatabase : RoomDatabase() {
    abstract fun assessmentProgressDao(): AssessmentProgressDao
    abstract fun courseProgressDao(): CourseProgressDao

    companion object {
        private const val DATABASE_NAME = "school-learning-progress.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assessment_attempt ADD COLUMN workProcess TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS course_lesson_progress (
                        courseId TEXT NOT NULL,
                        lessonId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(courseId, lessonId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_course_lesson_progress_courseId ON course_lesson_progress(courseId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_course_lesson_progress_updatedAtEpochMillis ON course_lesson_progress(updatedAtEpochMillis)",
                )
            }
        }

        @Volatile
        private var instance: SchoolLearningDatabase? = null

        fun get(context: Context): SchoolLearningDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SchoolLearningDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
