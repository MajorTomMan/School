package com.majortomman.school.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_attempts",
    indices = [
        Index(value = ["lessonId"]),
        Index(value = ["createdAt"]),
    ],
)
data class PracticeAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonId: String,
    val questionId: String,
    val questionText: String,
    val answer: String,
    val correct: Boolean,
    val feedback: String,
    val mistakeType: String?,
    val createdAt: Long,
)
