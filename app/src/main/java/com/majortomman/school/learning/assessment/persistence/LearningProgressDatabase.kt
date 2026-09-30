package com.majortomman.school.learning.assessment.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.majortomman.school.learning.progress.persistence.CourseLessonProgressEntity
import com.majortomman.school.learning.progress.persistence.CourseProgressDao

/**
 * School 的统一学习数据数据库。
 *
 * 课程进度、Assessment 事实、结算快照和知识掌握状态在同一持久化边界内演进；
 * UI 只能通过对应 Store 访问，不能直接拼装数据库状态。
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
internal abstract class LearningProgressDatabase : RoomDatabase() {
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
        private var instance: LearningProgressDatabase? = null

        fun get(context: Context): LearningProgressDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LearningProgressDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
