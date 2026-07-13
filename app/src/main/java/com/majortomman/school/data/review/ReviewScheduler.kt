package com.majortomman.school.data.review

import com.majortomman.school.data.local.ReviewScheduleEntity
import kotlin.math.max
import kotlin.math.roundToInt

object ReviewScheduler {
    private const val dayMillis = 24L * 60L * 60L * 1_000L
    private const val defaultEase = 2.5
    private const val minimumEase = 1.3

    fun next(
        lessonId: String,
        previous: ReviewScheduleEntity?,
        correct: Boolean,
        now: Long = System.currentTimeMillis(),
    ): ReviewScheduleEntity {
        val currentEase = previous?.easeFactor ?: defaultEase

        val repetitions: Int
        val intervalDays: Int
        val easeFactor: Double

        if (!correct) {
            repetitions = 0
            intervalDays = 1
            easeFactor = max(minimumEase, currentEase - 0.2)
        } else {
            repetitions = (previous?.repetitions ?: 0) + 1
            intervalDays = when (repetitions) {
                1 -> 1
                2 -> 6
                else -> max(1, ((previous?.intervalDays ?: 1) * currentEase).roundToInt())
            }
            easeFactor = max(minimumEase, currentEase + 0.1)
        }

        return ReviewScheduleEntity(
            lessonId = lessonId,
            dueAt = now + intervalDays * dayMillis,
            intervalDays = intervalDays,
            repetitions = repetitions,
            easeFactor = easeFactor,
            lastReviewedAt = now,
            lastCorrect = correct,
        )
    }
}
