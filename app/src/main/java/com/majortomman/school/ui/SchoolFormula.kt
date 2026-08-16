package com.majortomman.school.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * School course formulas use pure LaTeX math syntax.
 *
 * Rendering is an APK concern: course packages never name or depend on JLaTeXMath.
 * Long formulas keep their mathematical layout and become horizontally scrollable
 * instead of being squeezed into vertical text.
 */
@Composable
internal fun SchoolFormula(
    latex: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val density = LocalDensity.current
    val defaultColor = MaterialTheme.colorScheme.onBackground
    val defaultSize = MaterialTheme.typography.headlineMedium.fontSize
    val resolvedColor = if (color == Color.Unspecified) {
        if (style.color == Color.Unspecified) defaultColor else style.color
    } else {
        color
    }
    val resolvedSize = if (style.fontSize == TextUnit.Unspecified) defaultSize else style.fontSize
    val drawable = remember(latex, resolvedColor, resolvedSize, density.density, density.fontScale) {
        runCatching {
            JLatexMathDrawable.builder(latex.trim())
                .textSize(with(density) { resolvedSize.toPx() })
                .color(resolvedColor.toArgb())
                .padding(0)
                .align(JLatexMathDrawable.ALIGN_LEFT)
                .build()
        }.getOrNull()
    }

    if (drawable == null) {
        Text(text = latex, modifier = modifier, color = resolvedColor, style = style)
        return
    }

    val scrollState = rememberScrollState()
    with(density) {
        Row(
            modifier = modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.Center,
        ) {
            Canvas(
                modifier = Modifier.size(
                    width = drawable.bounds.width().toDp(),
                    height = drawable.bounds.height().toDp(),
                ),
            ) {
                drawable.draw(drawContext.canvas.nativeCanvas)
            }
        }
    }
}
