package com.majortomman.school.ui

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.sp

/**
 * Shared typography floor for mathematical diagrams.
 * Legacy Canvas values were raw pixels; this maps their relative weight to readable sp values,
 * so both system font scaling and the app text-size preference are respected.
 */
internal object VisualizationTypography {
    const val MINIMUM_LABEL_SP = 13f
    const val TICK_LABEL_SP = 14f
    const val REGULAR_LABEL_SP = 16f
    const val EMPHASIS_LABEL_SP = 18f
    const val HEADING_LABEL_SP = 20f
}

internal fun DrawScope.visualTextSizePx(
    legacySize: Float,
    minimumSp: Float = VisualizationTypography.MINIMUM_LABEL_SP,
): Float {
    val requestedSp = when {
        legacySize >= 38f -> VisualizationTypography.HEADING_LABEL_SP
        legacySize >= 30f -> VisualizationTypography.EMPHASIS_LABEL_SP
        legacySize >= 24f -> VisualizationTypography.REGULAR_LABEL_SP
        legacySize >= 18f -> VisualizationTypography.TICK_LABEL_SP
        else -> minimumSp
    }
    return maxOf(requestedSp, minimumSp).sp.toPx()
}
