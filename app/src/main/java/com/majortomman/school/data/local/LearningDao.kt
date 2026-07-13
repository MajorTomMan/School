package com.majortomman.school.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Insert
    suspend fun insertAttempt(attempt: PracticeAttemptEntity): Long

    @Query("SELECT * FROM practice_attempts ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentAttempts(limit: Int = 20): Flow<List<PracticeAttemptEntity>>

    @Query(
        """
        SELECT COUNT(*) AS attempts,
               COALESCE(SUM(CASE WHEN correct = 1 THEN 1 ELSE 0 END), 0) AS correctAttempts
        FROM practice_attempts
        """,
    )
    fun observeAttemptStats(): Flow<AttemptStatsRow>

    @Query("SELECT * FROM review_schedules ORDER BY dueAt ASC")
    fun observeReviewSchedules(): Flow<List<ReviewScheduleEntity>>

    @Query("SELECT * FROM review_schedules WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getReviewSchedule(lessonId: String): ReviewScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReviewSchedule(schedule: ReviewScheduleEntity)

    @Query("DELETE FROM practice_attempts")
    suspend fun clearAttempts()

    @Query("DELETE FROM review_schedules")
    suspend fun clearReviewSchedules()
}

data class AttemptStatsRow(
    val attempts: Int,
    val correctAttempts: Int,
)
