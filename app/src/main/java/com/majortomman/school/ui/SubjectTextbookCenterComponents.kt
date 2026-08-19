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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CenterBlack = Color.Transparent
private val CenterWhite = Color(0xFFF5F7FA)
private val CenterBlue = Color(0xFF2D7BFF)
private val CenterMuted = CenterWhite.copy(alpha = 0.46f)
private val CenterLine = CenterWhite.copy(alpha = 0.13f)

@Composable
fun NoActiveTextbookScreen(onOpenSubjects: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(CenterBlack).systemBarsPadding().padding(26.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("先选择课程", color = CenterWhite, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Text("课程内容与教材 PDF 直接使用已安装的云端课程。", color = CenterMuted, fontSize = 18.sp, lineHeight = 27.sp)
        Spacer(Modifier.height(30.dp))
        CenterOutlinedButton("前往课程", CenterBlue, onClick = onOpenSubjects)
    }
}

@Composable
internal fun CenterScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 24.dp),
        content = content,
    )
}

@Composable
internal fun CenterOutlinedButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier = modifier.heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = label, color = color, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(color.copy(alpha = 0.76f)))
    }
}

@Composable
internal fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CenterLine))
}
