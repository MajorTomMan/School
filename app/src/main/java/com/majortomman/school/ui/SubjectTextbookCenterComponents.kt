package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NoActiveTextbookScreen(onOpenSubjects: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(26.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        SchoolPageTitle("先选择课程")
        Spacer(Modifier.height(14.dp))
        Text("课程内容与教材 PDF 直接使用已安装的云端课程。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        SchoolPrimaryAction("前往课程  →", onClick = onOpenSubjects)
    }
}

@Composable
internal fun CenterScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
        content = content,
    )
}

@Composable
internal fun CenterOutlinedButton(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = label, color = color, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(color.copy(alpha = 0.76f)))
    }
}

@Composable
internal fun ThinDivider() = SchoolDivider()
