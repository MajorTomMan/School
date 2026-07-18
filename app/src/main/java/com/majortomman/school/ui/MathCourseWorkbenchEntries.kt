package com.majortomman.school.ui

/** Normalizes the mixed fallback list used by the compact data-bar renderer. */
internal val Any.value: Double
    get() = when (this) {
        is Map.Entry<*, *> -> (this.value as? Number)?.toDouble() ?: 0.0
        is Pair<*, *> -> (second as? Number)?.toDouble() ?: 0.0
        else -> 0.0
    }
