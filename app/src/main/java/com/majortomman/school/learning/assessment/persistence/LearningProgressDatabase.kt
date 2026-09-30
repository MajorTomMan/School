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
