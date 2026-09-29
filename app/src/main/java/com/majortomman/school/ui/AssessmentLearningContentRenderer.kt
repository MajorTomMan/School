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

@Composable
internal fun AssessmentLearningContentList(
    content: List<LearningContent>,
    assetFiles: Map<ContentAssetId, File>,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content.forEach { item -> AssessmentLearningContentItem(item, assetFiles, compact) }
    }
}

@Composable
private fun AssessmentLearningContentItem(item: LearningContent, assetFiles: Map<ContentAssetId, File>, compact: Boolean) {
    when (item) {
        is LearningContent.Heading -> Text(text = item.text, color = MaterialTheme.colorScheme.onBackground, fontSize = if (compact) 19.sp else 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        is LearningContent.Text -> AssessmentText(item, compact)
        is LearningContent.Formula -> AssessmentFormula(item, compact)
        is LearningContent.ItemList -> AssessmentList(item, compact)
        is LearningContent.Image -> AssessmentImage(item, assetFiles[item.assetId])
        is LearningContent.Table -> AssessmentTable(item)
        is LearningContent.Visualization -> SchoolVisualization(item.visualization, Modifier.fillMaxWidth().height(if (compact) 240.dp else 280.dp))
    }
}

@Composable
private fun AssessmentText(item: LearningContent.Text, compact: Boolean) {
    val color = when (item.style) {
        LearningTextStyle.PROMPT -> MaterialTheme.colorScheme.primary
        LearningTextStyle.CAPTION -> MaterialTheme.colorScheme.onSurfaceVariant
        LearningTextStyle.EXPLANATION -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
        LearningTextStyle.BODY -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
    }
    Text(text = item.text, color = color, fontSize = if (compact) 15.sp else 16.sp, lineHeight = if (compact) 24.sp else 27.sp, fontStyle = if (item.style == LearningTextStyle.CAPTION) FontStyle.Italic else FontStyle.Normal)
}

@Composable
private fun AssessmentFormula(item: LearningContent.Formula, compact: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)))
        Text(text = item.expression, modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), color = MaterialTheme.colorScheme.secondary, fontSize = if (compact) 20.sp else 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (item.conditions.isNotEmpty()) {
            Text(text = item.conditions.joinToString("，"), modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)))
    }
}

@Composable
private fun AssessmentList(item: LearningContent.ItemList, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.items.forEach { text ->
            Row(verticalAlignment = Alignment.Top) {
                Text("—", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(text = text, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f), fontSize = if (compact) 15.sp else 16.sp, lineHeight = 25.sp)
            }
        }
    }
}

@Composable
private fun AssessmentImage(item: LearningContent.Image, file: File?) {
    val bitmap = remember(file?.absolutePath, file?.lastModified()) { file?.takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.absolutePath) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = item.altText, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        } else {
            Column(modifier = Modifier.fillMaxWidth().height(160.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
                Spacer(Modifier.height(55.dp))
                Text("图片暂时不可用", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(55.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
            }
        }
        Text(text = item.caption ?: item.altText, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AssessmentTable(item: LearningContent.Table) {
    val horizontal = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.caption?.let { caption -> Text(text = caption, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center) }
        Column(modifier = Modifier.fillMaxWidth().horizontalScroll(horizontal)) {
            AssessmentTableRow(item.columns, header = true)
            item.rows.forEach { row -> AssessmentTableRow(row, header = false) }
        }
    }
}

@Composable
private fun AssessmentTableRow(cells: List<String>, header: Boolean) {
    Column {
        Row {
            cells.forEach { value ->
                Box(modifier = Modifier.width(132.dp).padding(horizontal = 10.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
                    Text(text = value, color = if (header) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f), fontSize = 13.sp, lineHeight = 19.sp, fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(if (header) 2.dp else 1.dp).background(if (header) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
    }
}
