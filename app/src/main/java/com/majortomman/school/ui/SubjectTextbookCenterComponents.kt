package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.material.TextbookProcessingState
import com.majortomman.school.data.material.TextbookProcessingStatus

private val CenterBlack = Color(0xFF050608)
private val CenterWhite = Color(0xFFF5F7FA)
private val CenterBlue = Color(0xFF2D7BFF)
private val CenterRed = Color(0xFFFF453A)
private val CenterYellow = Color(0xFFFFCC00)
private val CenterMuted = CenterWhite.copy(alpha = 0.46f)
private val CenterLine = CenterWhite.copy(alpha = 0.13f)

@Composable
internal fun ProcessingSection(state: TextbookProcessingState) {
    val color = if (state.status == TextbookProcessingStatus.FAILED) CenterRed else CenterYellow
    Text(
        when (state.status) {
            TextbookProcessingStatus.QUEUED -> "等待处理"
            TextbookProcessingStatus.RUNNING -> state.stage.label
            TextbookProcessingStatus.FAILED -> "处理未完成"
        },
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(13.dp))
    Text(state.message, color = CenterWhite, fontSize = 21.sp, lineHeight = 29.sp)
    Spacer(Modifier.height(22.dp))
    ProgressLine(state.progress, color)
    Spacer(Modifier.height(9.dp))
    Text("${state.progress}%", color = CenterMuted, fontSize = 13.sp)
}

@Composable
fun NoActiveTextbookScreen(onOpenSubjects: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(CenterBlack).systemBarsPadding().padding(26.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("先选择教材", color = CenterWhite, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Text("课程路径会根据预制课程或已绑定教材生成。", color = CenterMuted, fontSize = 18.sp, lineHeight = 27.sp)
        Spacer(Modifier.height(30.dp))
        CenterOutlinedButton("前往学科", CenterBlue, onClick = onOpenSubjects)
    }
}

@Composable
internal fun CenterScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
        content = content,
    )
}

@Composable
internal fun StatusText(installedCount: Int, processingCount: Int) {
    Text(
        when {
            processingCount > 0 -> "$processingCount 本处理中"
            installedCount > 0 -> "$installedCount 本教材"
            else -> "尚未导入"
        },
        color = when {
            processingCount > 0 -> CenterYellow
            installedCount > 0 -> CenterBlue
            else -> CenterMuted
        },
        fontSize = 13.sp,
    )
}

@Composable
internal fun CenterBack(label: String, onClick: () -> Unit) {
    Text(
        text = "‹  $label",
        modifier = Modifier.clickable(onClick = onClick),
        color = CenterWhite.copy(alpha = 0.72f),
        fontSize = 15.sp,
    )
}

@Composable
internal fun SlotButton(
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(82.dp)
            .border(1.dp, color.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = CenterWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
        Text(status, color = color, fontSize = 12.sp)
    }
}

@Composable
internal fun CenterOutlinedButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = modifier
            .height(48.dp)
            .border(1.dp, color, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun ProgressLine(progress: Int, color: Color) {
    Box(Modifier.fillMaxWidth().height(2.dp).background(CenterLine)) {
        Box(
            Modifier
                .fillMaxWidth((progress.coerceIn(0, 100) / 100f).coerceAtLeast(0.01f))
                .height(2.dp)
                .background(color),
        )
    }
}

@Composable
internal fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CenterLine))
}
