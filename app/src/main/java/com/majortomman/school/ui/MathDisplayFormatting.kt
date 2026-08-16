package com.majortomman.school.ui

import kotlin.math.abs

internal fun formatEquationNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
    return if (abs(rounded - rounded.toLong()) < 1e-9) {
        rounded.toLong().toString()
    } else {
        rounded.toString().trimEnd('0').trimEnd('.')
    }
}
