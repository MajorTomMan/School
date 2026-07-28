package com.majortomman.school.ui

import com.majortomman.school.learning.assessment.domain.QuestionKey

/** Stable string key used by saveable per-question UI state. */
internal val QuestionKey.value: String
    get() = toString()
