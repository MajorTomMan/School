package com.majortomman.school.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_schedules",
    indices = [Index(value = ["dueAt"])],
)
data class ReviewScheduleEntity(
    @PrimaryKey
    val lessonId: String,
    val dueAt: Long,
    val intervalDays: Int,
    val repetitions: Int,
    val easeFactor: Double,
    val lastReviewedAt: Long,
    val lastCorrect: Boolean,
)
