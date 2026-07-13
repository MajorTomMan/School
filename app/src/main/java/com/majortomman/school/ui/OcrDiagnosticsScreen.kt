package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.material.InstalledTextbook
import com.majortomman.school.data.material.OcrDiagnosticRecord
import com.majortomman.school.data.material.TextbookOcrStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LogBlack = Color(0xFF050608)
private val LogWhite = Color(0xFFF5F7FA)
private val LogBlue = Color(0xFF2D7BFF)
private val LogYellow = Color(0xFFFFCC00)
private val LogRed = Color(0xFFFF453A)
private val LogMuted = LogWhite.copy(alpha = 0.46f)
private val LogLine = LogWhite.copy(alpha = 0.13f)

@Composable
internal fun OcrDiagnosticsScreen(
    textbook: InstalledTextbook,
    onBack: () -> Unit,
) {
    var records by remember { mutableStateOf<List<OcrDiagnosticRecord>>(emptyList()) }
    var selectedPage by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(textbook.pack.rootPath) {
        records = withContext(Dispatchers.IO) {
            TextbookOcrStore.readDiagnostics(File(textbook.pack.rootPath))
        }
    }

    val selected = selectedPage?.let { page -> records.firstOrNull { it.printedPage == page } }
    if (selected != null) {
        OcrPageDetail(record = selected, onBack = { selectedPage = null })
        return
    }

    val suspiciousCount = records.sumOf { it.diagnostics.suspiciousTokens.size }
    val ignoredCount = records.sumOf { it.diagnostics.ignoredLineCount }
    val keptCount = records.sumOf { it.diagnostics.keptLineCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LogBlack)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(
                "‹  ${textbook.slot.displayTitle}",
                modifier = Modifier.clickable(onClick = onBack),
                color = LogWhite.copy(alpha = 0.72f),
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(28.dp))
            Text("OCR 日志", color = LogWhite, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "设备端保存原始文字、清洗后文字、被过滤行和可疑数字。Logcat 标签为 SchoolTextbookOCR。",
                color = LogMuted,
                fontSize = 15.sp,
                lineHeight = 23.sp,
            )
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DiagnosticMetric("页面", records.size.toString(), LogBlue)
                DiagnosticMetric("保留行", keptCount.toString(), LogWhite)
                DiagnosticMetric("过滤行", ignoredCount.toString(), LogYellow)
                DiagnosticMetric("可疑", suspiciousCount.toString(), if (suspiciousCount > 0) LogRed else LogMuted)
            }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(LogLine))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = 44.dp,
            ),
        ) {
            if (records.isEmpty()) {
                item {
                    Spacer(Modifier.height(72.dp))
                    Text("还没有新的 OCR 日志", color = LogWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "0.17.0 会使旧 OCR 缓存失效。重新扫描教材页面后，这里会出现逐页诊断。",
                        color = LogMuted,
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                    )
                }
            }
            items(records, key = { it.printedPage }) { record ->
                OcrRecordRow(record = record, onClick = { selectedPage = record.printedPage })
            }
        }
    }
}

@Composable
private fun DiagnosticMetric(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(label, color = LogMuted, fontSize = 11.sp)
    }
}

@Composable
private fun OcrRecordRow(
    record: OcrDiagnosticRecord,
    onClick: () -> Unit,
) {
    val diagnostic = record.diagnostics
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 17.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("教材第 ${record.printedPage} 页", color = LogWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                if (diagnostic.suspiciousTokens.isEmpty()) "正常" else "发现 ${diagnostic.suspiciousTokens.size} 项可疑文字",
                color = if (diagnostic.suspiciousTokens.isEmpty()) LogBlue else LogRed,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "原始 ${diagnostic.rawLineCount} 行 · 保留 ${diagnostic.keptLineCount} 行 · 过滤 ${diagnostic.ignoredLineCount} 行 · ${diagnostic.durationMs} ms",
            color = LogMuted,
            fontSize = 12.sp,
        )
        if (diagnostic.suspiciousTokens.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Text(
                "可疑：${diagnostic.suspiciousTokens.joinToString("、")}",
                color = LogRed,
                fontSize = 12.sp,
                lineHeight = 19.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            record.cleanedPreview.replace('\n', ' ').take(150).ifBlank { "清洗后没有可用文字" },
            color = LogWhite.copy(alpha = 0.54f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(LogLine))
}

@Composable
private fun OcrPageDetail(
    record: OcrDiagnosticRecord,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LogBlack).systemBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    ) {
        item {
            Text(
                "‹  OCR 日志",
                modifier = Modifier.clickable(onClick = onBack),
                color = LogWhite.copy(alpha = 0.72f),
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(28.dp))
            Text("第 ${record.printedPage} 页", color = LogWhite, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "PDF 索引 ${record.pdfIndex} · ${record.diagnostics.durationMs} ms",
                color = LogMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(30.dp))
            LogSection("清洗后文字", record.cleanedPreview.ifBlank { "没有可用文字" }, LogBlue)
            Spacer(Modifier.height(32.dp))
            LogSection("原始 OCR", record.rawPreview.ifBlank { "没有原始文字" }, LogYellow)
            Spacer(Modifier.height(32.dp))
            LogSection(
                "被过滤的行",
                record.ignoredLines.joinToString("\n").ifBlank { "没有过滤行" },
                LogRed,
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun LogSection(
    title: String,
    text: String,
    color: Color,
) {
    Text(title, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Text(
        text,
        color = LogWhite.copy(alpha = 0.74f),
        fontSize = 13.sp,
        lineHeight = 21.sp,
        fontFamily = FontFamily.Monospace,
    )
}
