package com.majortomman.school.data

suspend fun PreferencesRepository.recordAttempt(
    lessonId: String,
    answer: String,
    correct: Boolean,
    feedback: String,
) {
    recordAttempt(
        lessonId = lessonId,
        draft = AttemptDraft(
            questionId = "number-line-compare",
            questionText = "在数轴上，-3 与 2 哪个数更大？请简单说明理由。",
            answer = answer,
            correct = correct,
            feedback = feedback,
            mistakeType = if (correct) null else "待进一步诊断",
        ),
    )
}
