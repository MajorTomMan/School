package com.majortomman.school.visualization.renderers.math

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationRenderContext
import com.majortomman.school.visualization.VisualizationRenderer
import com.majortomman.school.visualization.VisualizationSchema
import com.majortomman.school.visualization.VisualizationSubject
import com.majortomman.school.visualization.VisualizationTextSpec

/** Shows how practical needs led to successive extensions of the familiar number system. */
internal class NumberDevelopmentRenderer : VisualizationRenderer() {
    override val key = VisualizationKey("mathematics.context.number-development")
    override val subject = VisualizationSubject.MATHEMATICS
    override val schema = VisualizationSchema(texts = listOf(
        VisualizationTextSpec("title", false, true),
        VisualizationTextSpec("countingOrigin"),
        VisualizationTextSpec("countingNeed"),
        VisualizationTextSpec("countingNumbers"),
        VisualizationTextSpec("zeroOrigin"),
        VisualizationTextSpec("zeroNeed"),
        VisualizationTextSpec("zeroNumber"),
        VisualizationTextSpec("fractionOrigin"),
        VisualizationTextSpec("fractionNeed"),
        VisualizationTextSpec("fractionNumbers"),
        VisualizationTextSpec("note", false, true),
    ))

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        val texts = context.invocation.texts
        Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val title = texts.text("title")
            if (title.isNotBlank()) Text(title, modifier = Modifier.fillMaxWidth(), color = context.palette.foreground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            DevelopmentRow(texts.text("countingOrigin"), texts.text("countingNeed"), texts.text("countingNumbers"), context.palette.primary, context.palette.foreground, context.palette.grid)
            DevelopmentRow(texts.text("zeroOrigin"), texts.text("zeroNeed"), texts.text("zeroNumber"), context.palette.secondary, context.palette.foreground, context.palette.grid)
            DevelopmentRow(texts.text("fractionOrigin"), texts.text("fractionNeed"), texts.text("fractionNumbers"), context.palette.primary, context.palette.foreground, context.palette.grid)
            val note = texts.text("note")
            if (note.isNotBlank()) Text(note, modifier = Modifier.fillMaxWidth(), color = context.palette.muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DevelopmentRow(origin: String, need: String, numbers: String, accent: Color, foreground: Color, border: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        DevelopmentCell(origin, foreground, border, Modifier.weight(0.9f))
        Text("→", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        DevelopmentCell(need, foreground, border, Modifier.weight(1.35f))
        Text("→", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.weight(1.0f).border(1.dp, accent.copy(alpha = 0.58f), RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(numbers, color = accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DevelopmentCell(text: String, foreground: Color, border: Color, modifier: Modifier) {
    Box(modifier = modifier.border(1.dp, border, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = foreground, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
    }
}
