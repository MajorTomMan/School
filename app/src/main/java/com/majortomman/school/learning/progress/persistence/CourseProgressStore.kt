package com.majortomman.school.learning.progress.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import com.majortomman.school.learning.persistence.SchoolLearningDatabase
import com.majortomman.school.learning.progress.CourseProgressGateway
import com.majortomman.school.learning.progress.CourseProgressSnapshot
import com.majortomman.school.learning.progress.LessonProgressStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(
    tableName = "course_lesson_progress",
    primaryKeys = ["courseId", "lessonId"],
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["updatedAtEpochMillis"]),
    ],
)
internal data class CourseLessonProgressEntity(
    val courseId: String,
    val lessonId: String,
    val status: String,
    val updatedAtEpochMillis: Long,
)

@Dao
internal interface CourseProgressDao {
    @Query(
        """
        SELECT * FROM course_lesson_progress
        WHERE courseId = :courseId
        ORDER BY updatedAtEpochMillis ASC, lessonId ASC
        """,
    )
    fun observeCourse(courseId: String): Flow<List<CourseLessonProgressEntity>>

    @Query(
        """
        SELECT * FROM course_lesson_progress
        WHERE courseId = :courseId AND lessonId = :lessonId
        LIMIT 1
        """,
    )
    suspend fun find(courseId: String, lessonId: String): CourseLessonProgressEntity?

    @Upsert
    suspend fun upsert(entity: CourseLessonProgressEntity)

    @Query("DELETE FROM course_lesson_progress")
    suspend fun clearAll()
}

class CourseProgressStore internal constructor(
    private val database: SchoolLearningDatabase,
) : CourseProgressGateway {
    private val dao: CourseProgressDao
        get() = database.courseProgressDao()

    override fun observeCourse(courseId: String): Flow<CourseProgressSnapshot> =
        dao.observeCourse(courseId).map { rows ->
            CourseProgressSnapshot(
                courseId = courseId,
                lessonStatuses = rows.associate { row ->
                    row.lessonId to LessonProgressStatus.valueOf(row.status)
                },
                lastLessonId = rows.maxByOrNull(CourseLessonProgressEntity::updatedAtEpochMillis)?.lessonId,
            )
        }

    override suspend fun startLesson(courseId: String, lessonId: String, atEpochMillis: Long) {
        require(courseId.isNotBlank()) { "courseId 不能为空" }
        require(lessonId.isNotBlank()) { "lessonId 不能为空" }
        dao.upsert(
            CourseLessonProgressEntity(
                courseId = courseId,
                lessonId = lessonId,
                status = LessonProgressStatus.IN_PROGRESS.name,
                updatedAtEpochMillis = atEpochMillis,
            ),
        )
    }

    override suspend fun finishLessonAndStartNext(
        courseId: String,
        currentLessonId: String,
        nextLessonId: String?,
        atEpochMillis: Long,
    ) {
        database.withTransaction {
            dao.upsert(
                CourseLessonProgressEntity(
                    courseId = courseId,
                    lessonId = currentLessonId,
                    status = LessonProgressStatus.COMPLETED.name,
                    updatedAtEpochMillis = atEpochMillis,
                ),
            )
            if (nextLessonId != null) {
                val next = dao.find(courseId, nextLessonId)
                if (next?.status != LessonProgressStatus.COMPLETED.name) {
                    dao.upsert(
                        CourseLessonProgressEntity(
                            courseId = courseId,
                            lessonId = nextLessonId,
                            status = LessonProgressStatus.IN_PROGRESS.name,
                            updatedAtEpochMillis = atEpochMillis + 1,
                        ),
                    )
                }
            }
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    companion object {
        fun create(context: android.content.Context): CourseProgressStore =
            CourseProgressStore(SchoolLearningDatabase.get(context))
    }
}
