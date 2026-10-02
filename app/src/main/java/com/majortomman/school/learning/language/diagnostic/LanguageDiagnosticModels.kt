package com.majortomman.school.learning.language.diagnostic

enum class DiagnosticStatus {
    INPUT_IN_PROGRESS,
    CORRECT,
    INCORRECT,
    UNSUPPORTED,
}

enum class DiagnosticErrorType {
    SPELLING,
    CAPITALIZATION,
    PUNCTUATION,
    WORD_FORM,
    WORD_ORDER,
    SENTENCE_STRUCTURE,
    PARTICLE,
    CONJUGATION,
    SPEECH_REGISTER,
}

data class DiagnosticStep(
    val title: String,
    val expression: String,
    val correct: Boolean? = null,
)

data class DiagnosticResult(
    val status: DiagnosticStatus,
    val normalizedAnswer: String? = null,
    val steps: List<DiagnosticStep> = emptyList(),
    val errorType: DiagnosticErrorType? = null,
    val message: String,
)
