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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS mastery_evidence")
                db.execSQL("DROP TABLE IF EXISTS mastery_snapshot")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS learning_evidence (
                        evidenceId TEXT NOT NULL,
                        courseId TEXT NOT NULL,
                        knowledgePointId TEXT NOT NULL,
                        sourceKind TEXT NOT NULL,
                        sourceContextId TEXT NOT NULL,
                        sourceItemId TEXT NOT NULL,
                        sourceItemRevision INTEGER NOT NULL,
                        contentRevision TEXT NOT NULL,
                        outcome TEXT NOT NULL,
                        score REAL NOT NULL,
                        weight REAL NOT NULL,
                        difficulty REAL NOT NULL,
                        wrongAttemptCount INTEGER NOT NULL,
                        hintViewCount INTEGER NOT NULL,
                        explanationViewed INTEGER NOT NULL,
                        recordedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(evidenceId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_learning_evidence_courseId ON learning_evidence(courseId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_learning_evidence_knowledgePointId_recordedAtEpochMillis ON learning_evidence(knowledgePointId, recordedAtEpochMillis)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_learning_evidence_sourceKind_sourceContextId ON learning_evidence(sourceKind, sourceContextId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mastery_update_snapshot (
                        snapshotId TEXT NOT NULL,
                        sourceContextId TEXT NOT NULL,
                        knowledgePointId TEXT NOT NULL,
                        beforeScore REAL NOT NULL,
                        afterScore REAL NOT NULL,
                        beforeEvidenceWeight REAL NOT NULL,
                        appliedEvidenceWeight REAL NOT NULL,
                        afterEvidenceWeight REAL NOT NULL,
                        policyVersion INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(snapshotId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mastery_update_snapshot_sourceContextId ON mastery_update_snapshot(sourceContextId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mastery_update_snapshot_knowledgePointId_createdAtEpochMillis ON mastery_update_snapshot(knowledgePointId, createdAtEpochMillis)",
                )
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS mastery_state")
                db.execSQL("DROP TABLE IF EXISTS mastery_update_snapshot")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mastery_state (
                        courseId TEXT NOT NULL,
                        knowledgePointId TEXT NOT NULL,
                        score REAL NOT NULL,
                        accumulatedEvidenceWeight REAL NOT NULL,
                        lastPolicyVersion INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(courseId, knowledgePointId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mastery_state_courseId ON mastery_state(courseId)",
                )
                db.execSQL(
                    """
                    INSERT INTO mastery_state (
                        courseId,
                        knowledgePointId,
                        score,
                        accumulatedEvidenceWeight,
                        lastPolicyVersion,
                        updatedAtEpochMillis
                    )
                    SELECT
                        courseId,
                        knowledgePointId,
                        (
                            0.5 + SUM(score * weight * (0.75 + difficulty * 0.5))
                        ) / (
                            1.0 + SUM(weight * (0.75 + difficulty * 0.5))
                        ),
                        1.0 + SUM(weight * (0.75 + difficulty * 0.5)),
                        1,
                        MAX(recordedAtEpochMillis)
                    FROM learning_evidence
                    GROUP BY courseId, knowledgePointId
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mastery_update_snapshot (
                        snapshotId TEXT NOT NULL,
                        courseId TEXT NOT NULL,
                        sourceContextId TEXT NOT NULL,
                        knowledgePointId TEXT NOT NULL,
                        beforeScore REAL NOT NULL,
                        afterScore REAL NOT NULL,
                        beforeEvidenceWeight REAL NOT NULL,
                        appliedEvidenceWeight REAL NOT NULL,
                        afterEvidenceWeight REAL NOT NULL,
                        policyVersion INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(snapshotId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mastery_update_snapshot_courseId_sourceContextId ON mastery_update_snapshot(courseId, sourceContextId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mastery_update_snapshot_courseId_knowledgePointId_createdAtEpochMillis ON mastery_update_snapshot(courseId, knowledgePointId, createdAtEpochMillis)",
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
        }
    }
}
