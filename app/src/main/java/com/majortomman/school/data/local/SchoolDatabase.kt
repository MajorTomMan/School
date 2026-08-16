package com.majortomman.school.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PracticeAttemptEntity::class,
        ReviewScheduleEntity::class,
        MathPracticeAttemptEntity::class,
        MathKnowledgeMasteryEntity::class,
        MathMistakeEntity::class,
        SubjectEntity::class,
        LearningLevelSystemEntity::class,
        LearningLevelEntity::class,
        CurriculumEntity::class,
        CurriculumNodeEntity::class,
        KnowledgePointEntity::class,
        KnowledgeRelationEntity::class,
        NodeKnowledgeRefEntity::class,
        LearningResourceEntity::class,
        ResourceBindingEntity::class,
        KnowledgeMasteryEntity::class,
        CurriculumNodeProgressEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class SchoolDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
    abstract fun curriculumDao(): CurriculumDao

    companion object {
        @Volatile
        private var instance: SchoolDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `math_practice_attempts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `textbookKey` TEXT NOT NULL,
                        `lessonId` TEXT,
                        `questionId` TEXT NOT NULL,
                        `templateId` TEXT NOT NULL,
                        `knowledgePointId` TEXT NOT NULL,
                        `questionJson` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `canonicalAnswer` TEXT NOT NULL,
                        `correct` INTEGER NOT NULL,
                        `mistakeType` TEXT,
                        `usedHint` INTEGER NOT NULL,
                        `durationMillis` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_practice_attempts_textbookKey` ON `math_practice_attempts` (`textbookKey`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_practice_attempts_knowledgePointId` ON `math_practice_attempts` (`knowledgePointId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_practice_attempts_questionId` ON `math_practice_attempts` (`questionId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_practice_attempts_createdAt` ON `math_practice_attempts` (`createdAt`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `math_knowledge_mastery` (
                        `textbookKey` TEXT NOT NULL,
                        `knowledgePointId` TEXT NOT NULL,
                        `score` REAL NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `correctStreak` INTEGER NOT NULL,
                        `wrongStreak` INTEGER NOT NULL,
                        `lastCorrect` INTEGER NOT NULL,
                        `dueAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`textbookKey`, `knowledgePointId`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_knowledge_mastery_dueAt` ON `math_knowledge_mastery` (`dueAt`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `math_mistakes` (
                        `mistakeKey` TEXT NOT NULL,
                        `textbookKey` TEXT NOT NULL,
                        `lessonId` TEXT,
                        `questionId` TEXT NOT NULL,
                        `templateId` TEXT NOT NULL,
                        `knowledgePointId` TEXT NOT NULL,
                        `questionJson` TEXT NOT NULL,
                        `wrongCount` INTEGER NOT NULL,
                        `resolvedStreak` INTEGER NOT NULL,
                        `lastAnswer` TEXT NOT NULL,
                        `mistakeType` TEXT NOT NULL,
                        `dueAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mistakeKey`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_mistakes_textbookKey` ON `math_mistakes` (`textbookKey`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_mistakes_knowledgePointId` ON `math_mistakes` (`knowledgePointId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_math_mistakes_dueAt` ON `math_mistakes` (`dueAt`)")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `capabilityIds` TEXT NOT NULL,
                        `stageIds` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_subjects_orderIndex` ON `subjects` (`orderIndex`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `learning_level_systems` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `learning_levels` (
                        `id` TEXT NOT NULL,
                        `systemId` TEXT NOT NULL,
                        `parentId` TEXT,
                        `title` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `legacyGrade` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_levels_systemId` ON `learning_levels` (`systemId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_levels_parentId` ON `learning_levels` (`parentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_levels_legacyGrade` ON `learning_levels` (`legacyGrade`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `curricula` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `levelSystemId` TEXT,
                        `standard` TEXT,
                        `region` TEXT,
                        `version` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curricula_subjectId` ON `curricula` (`subjectId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curricula_source` ON `curricula` (`source`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curricula_orderIndex` ON `curricula` (`orderIndex`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `curriculum_nodes` (
                        `id` TEXT NOT NULL,
                        `curriculumId` TEXT NOT NULL,
                        `parentId` TEXT,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `legacyLessonId` TEXT,
                        `metadataJson` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_curriculumId` ON `curriculum_nodes` (`curriculumId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_parentId` ON `curriculum_nodes` (`parentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_legacyLessonId` ON `curriculum_nodes` (`legacyLessonId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_curriculumId_parentId_orderIndex` ON `curriculum_nodes` (`curriculumId`, `parentId`, `orderIndex`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `knowledge_points` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `evaluatorId` TEXT,
                        `metadataJson` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_points_subjectId` ON `knowledge_points` (`subjectId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_points_kind` ON `knowledge_points` (`kind`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `knowledge_relations` (
                        `fromKnowledgeId` TEXT NOT NULL,
                        `toKnowledgeId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `weight` REAL NOT NULL,
                        `origin` TEXT NOT NULL,
                        PRIMARY KEY(`fromKnowledgeId`, `toKnowledgeId`, `type`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_relations_toKnowledgeId` ON `knowledge_relations` (`toKnowledgeId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_relations_origin` ON `knowledge_relations` (`origin`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `node_knowledge_refs` (
                        `nodeId` TEXT NOT NULL,
                        `knowledgePointId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`nodeId`, `knowledgePointId`, `role`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_node_knowledge_refs_knowledgePointId` ON `node_knowledge_refs` (`knowledgePointId`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `learning_resources` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `uri` TEXT,
                        `publisher` TEXT,
                        `edition` TEXT,
                        `legacyTextbookKey` TEXT,
                        `metadataJson` TEXT NOT NULL,
                        `origin` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_resources_subjectId` ON `learning_resources` (`subjectId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_resources_type` ON `learning_resources` (`type`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_resources_legacyTextbookKey` ON `learning_resources` (`legacyTextbookKey`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_resources_origin` ON `learning_resources` (`origin`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `resource_bindings` (
                        `id` TEXT NOT NULL,
                        `resourceId` TEXT NOT NULL,
                        `nodeId` TEXT,
                        `knowledgePointId` TEXT,
                        `role` TEXT NOT NULL,
                        `pageStart` INTEGER,
                        `pageEnd` INTEGER,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_resource_bindings_resourceId` ON `resource_bindings` (`resourceId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_resource_bindings_nodeId` ON `resource_bindings` (`nodeId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_resource_bindings_knowledgePointId` ON `resource_bindings` (`knowledgePointId`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `knowledge_mastery` (
                        `subjectId` TEXT NOT NULL,
                        `knowledgePointId` TEXT NOT NULL,
                        `score` REAL NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `correctStreak` INTEGER NOT NULL,
                        `wrongStreak` INTEGER NOT NULL,
                        `lastCorrect` INTEGER NOT NULL,
                        `dueAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`subjectId`, `knowledgePointId`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_mastery_dueAt` ON `knowledge_mastery` (`dueAt`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_mastery_updatedAt` ON `knowledge_mastery` (`updatedAt`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `curriculum_node_progress` (
                        `nodeId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastVisitedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        PRIMARY KEY(`nodeId`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_node_progress_status` ON `curriculum_node_progress` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_node_progress_lastVisitedAt` ON `curriculum_node_progress` (`lastVisitedAt`)")

                database.execSQL(
                    """
                    INSERT OR REPLACE INTO `knowledge_mastery` (
                        `subjectId`, `knowledgePointId`, `score`, `attempts`, `correctStreak`,
                        `wrongStreak`, `lastCorrect`, `dueAt`, `updatedAt`
                    )
                    SELECT 'math', `knowledgePointId`, MAX(`score`), SUM(`attempts`),
                           MAX(`correctStreak`), MAX(`wrongStreak`), MAX(`lastCorrect`),
                           MIN(`dueAt`), MAX(`updatedAt`)
                    FROM `math_knowledge_mastery`
                    GROUP BY `knowledgePointId`
                    """.trimIndent(),
                )
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE `learning_levels_new` (
                        `id` TEXT NOT NULL,
                        `systemId` TEXT NOT NULL,
                        `parentId` TEXT,
                        `title` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `grade` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO `learning_levels_new` (`id`, `systemId`, `parentId`, `title`, `orderIndex`, `grade`)
                    SELECT `id`, `systemId`, `parentId`, `title`, `orderIndex`, `legacyGrade` FROM `learning_levels`
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE `learning_levels`")
                database.execSQL("ALTER TABLE `learning_levels_new` RENAME TO `learning_levels`")
                database.execSQL("CREATE INDEX `index_learning_levels_systemId` ON `learning_levels` (`systemId`)")
                database.execSQL("CREATE INDEX `index_learning_levels_parentId` ON `learning_levels` (`parentId`)")
                database.execSQL("CREATE INDEX `index_learning_levels_grade` ON `learning_levels` (`grade`)")

                database.execSQL(
                    """
                    CREATE TABLE `curriculum_nodes_new` (
                        `id` TEXT NOT NULL,
                        `curriculumId` TEXT NOT NULL,
                        `parentId` TEXT,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `lessonId` TEXT,
                        `metadataJson` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO `curriculum_nodes_new` (`id`, `curriculumId`, `parentId`, `type`, `title`, `orderIndex`, `lessonId`, `metadataJson`)
                    SELECT `id`, `curriculumId`, `parentId`, `type`, `title`, `orderIndex`, `legacyLessonId`, `metadataJson` FROM `curriculum_nodes`
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE `curriculum_nodes`")
                database.execSQL("ALTER TABLE `curriculum_nodes_new` RENAME TO `curriculum_nodes`")
                database.execSQL("CREATE INDEX `index_curriculum_nodes_curriculumId` ON `curriculum_nodes` (`curriculumId`)")
                database.execSQL("CREATE INDEX `index_curriculum_nodes_parentId` ON `curriculum_nodes` (`parentId`)")
                database.execSQL("CREATE INDEX `index_curriculum_nodes_lessonId` ON `curriculum_nodes` (`lessonId`)")
                database.execSQL("CREATE INDEX `index_curriculum_nodes_curriculumId_parentId_orderIndex` ON `curriculum_nodes` (`curriculumId`, `parentId`, `orderIndex`)")

                database.execSQL(
                    """
                    CREATE TABLE `learning_resources_new` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `uri` TEXT,
                        `publisher` TEXT,
                        `edition` TEXT,
                        `textbookKey` TEXT,
                        `metadataJson` TEXT NOT NULL,
                        `origin` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO `learning_resources_new` (`id`, `subjectId`, `type`, `title`, `uri`, `publisher`, `edition`, `textbookKey`, `metadataJson`, `origin`)
                    SELECT `id`, `subjectId`, `type`, `title`, `uri`, `publisher`, `edition`, `legacyTextbookKey`, `metadataJson`, `origin` FROM `learning_resources`
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE `learning_resources`")
                database.execSQL("ALTER TABLE `learning_resources_new` RENAME TO `learning_resources`")
                database.execSQL("CREATE INDEX `index_learning_resources_subjectId` ON `learning_resources` (`subjectId`)")
                database.execSQL("CREATE INDEX `index_learning_resources_type` ON `learning_resources` (`type`)")
                database.execSQL("CREATE INDEX `index_learning_resources_textbookKey` ON `learning_resources` (`textbookKey`)")
                database.execSQL("CREATE INDEX `index_learning_resources_origin` ON `learning_resources` (`origin`)")
            }
        }

        fun getInstance(context: Context): SchoolDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SchoolDatabase::class.java,
                "school.db",
            )
                .addMigrations(migration1To2, migration2To3, migration3To4)
                .build()
                .also { instance = it }
        }
    }
}
