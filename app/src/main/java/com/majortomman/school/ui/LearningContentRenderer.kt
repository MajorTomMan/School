package com.majortomman.school.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.content.ContentAssetId
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.content.LearningTextStyle
import com.majortomman.school.visualization.SchoolVisualization
import java.io.File

internal enum class LearningContentSurface {
    COURSE,
    ASSESSMENT,
}

@Composable
internal fun LearningContentList(
    content: List<LearningContent>,
    assetFiles: Map<ContentAssetId, File> = emptyMap(),
    surface: LearningContentSurface = LearningContentSurface.COURSE,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content.forEach { item -> LearningContentItem(item, assetFiles, surface, compact) }
    }
}

@Composable
private fun LearningContentItem(
    item: LearningContent,
    assetFiles: Map<ContentAssetId, File>,
    surface: LearningContentSurface,
    compact: Boolean,
) {
    when (item) {
        is LearningContent.Heading -> LearningHeading(item, surface, compact)
        is LearningContent.Text -> LearningText(item, surface, compact)
        is LearningContent.Formula -> LearningFormula(item, surface, compact)
        is LearningContent.ItemList -> LearningList(item, surface, compact)
        is LearningContent.Image -> LearningImage(item, assetFiles[item.assetId])
        is LearningContent.Table -> LearningTable(item, surface)
        is LearningContent.Visualization -> SchoolVisualization(
            item.visualization,
            Modifier.fillMaxWidth().height(
                when {
                    surface == LearningContentSurface.COURSE -> 360.dp
                    compact -> 240.dp
                    else -> 280.dp
                },
            ),
        )
    }
}

@Composable
private fun LearningHeading(item: LearningContent.Heading, surface: LearningContentSurface, compact: Boolean) {
    if (surface == LearningContentSurface.COURSE) {
        Text(
            text = item.text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        return
    }
    Text(
        text = item.text,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = if (compact) 19.sp else 21.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LearningText(item: LearningContent.Text, surface: LearningContentSurface, compact: Boolean) {
    if (surface == LearningContentSurface.COURSE) {
        val prompt = item.style == LearningTextStyle.PROMPT
        Text(
            text = item.text,
            color = when (item.style) {
                LearningTextStyle.PROMPT, LearningTextStyle.BODY -> MaterialTheme.colorScheme.onBackground
                LearningTextStyle.CAPTION, LearningTextStyle.EXPLANATION -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = if (prompt) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (prompt) FontWeight.Medium else FontWeight.Normal,
        )
        return
    }

    val color = when (item.style) {
        LearningTextStyle.PROMPT -> MaterialTheme.colorScheme.primary
        LearningTextStyle.CAPTION -> MaterialTheme.colorScheme.onSurfaceVariant
        LearningTextStyle.EXPLANATION -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
        LearningTextStyle.BODY -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
    }
    Text(
        text = item.text,
        color = color,
        fontSize = if (compact) 15.sp else 16.sp,
        lineHeight = if (compact) 24.sp else 27.sp,
        fontStyle = if (item.style == LearningTextStyle.CAPTION) FontStyle.Italic else FontStyle.Normal,
    )
}

@Composable
private fun LearningFormula(item: LearningContent.Formula, surface: LearningContentSurface, compact: Boolean) {
    if (surface == LearningContentSurface.COURSE) {
        SchoolFormula(
            latex = item.expression,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.headlineMedium,
        )
        item.conditions.forEach { condition ->
            Text(
                condition,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)))
        SchoolFormula(
            latex = item.expression,
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            color = MaterialTheme.colorScheme.secondary,
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
        )
        if (item.conditions.isNotEmpty()) {
            Text(
                text = item.conditions.joinToString("，"),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)))
    }
}

@Composable
private fun LearningList(item: LearningContent.ItemList, surface: LearningContentSurface, compact: Boolean) {
    if (surface == LearningContentSurface.COURSE) {
        item.items.forEach { value ->
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    value,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.items.forEach { value ->
            Row(verticalAlignment = Alignment.Top) {
                Text("—", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                    fontSize = if (compact) 15.sp else 16.sp,
                    lineHeight = 25.sp,
                )
            }
        }
    }
}

@Composable
private fun LearningImage(item: LearningContent.Image, file: File?) {
    val bitmap = remember(file?.absolutePath, file?.lastModified()) {
        file?.takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = item.altText,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
                Spacer(Modifier.height(55.dp))
                Text("图片暂时不可用", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(55.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
            }
        }
        Text(
            text = item.caption ?: item.altText,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LearningTable(item: LearningContent.Table, surface: LearningContentSurface) {
    if (surface == LearningContentSurface.COURSE) {
        item.caption?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            item.columns.joinToString("   "),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        item.rows.forEach { row ->
            Text(
                row.joinToString("   "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    val horizontal = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.caption?.let { caption ->
            Text(
                text = caption,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        Column(modifier = Modifier.fillMaxWidth().horizontalScroll(horizontal)) {
            LearningTableRow(item.columns, header = true)
            item.rows.forEach { row -> LearningTableRow(row, header = false) }
        }
    }
}

@Composable
private fun LearningTableRow(cells: List<String>, header: Boolean) {
    Column {
        Row {
            cells.forEach { value ->
                Box(
                    modifier = Modifier.width(132.dp).padding(horizontal = 10.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value,
                        color = if (header) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(if (header) 2.dp else 1.dp).background(
                if (header) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            ),
        )
    }
}
