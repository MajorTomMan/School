package com.majortomman.school.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.material.InternalPdfBrowserStore
import com.majortomman.school.data.material.PdfLibraryDirectory
import com.majortomman.school.data.material.PdfLibraryEntry
import com.majortomman.school.data.material.PdfLibraryScanResult
import com.majortomman.school.data.material.TextbookSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BrowserBlack = Color(0xFF050608)
private val BrowserWhite = Color(0xFFF5F7FA)
private val BrowserBlue = Color(0xFF2D7BFF)
private val BrowserYellow = Color(0xFFFFCC00)
private val BrowserRed = Color(0xFFFF453A)
private val BrowserMuted = BrowserWhite.copy(alpha = 0.46f)
private val BrowserLine = BrowserWhite.copy(alpha = 0.13f)

@Composable
internal fun InternalPdfBrowserScreen(
    slot: TextbookSlot,
    installedTitles: Set<String>,
    onSelect: (Uri) -> Unit,
    onOtherLocation: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var scanResult by remember {
        mutableStateOf(PdfLibraryScanResult(emptyList(), emptyList(), emptyList()))
    }
    var confirmRemoveUri by rememberSaveable { mutableStateOf<String?>(null) }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            InternalPdfBrowserStore.addDirectory(context, uri)
            refreshToken += 1
        }
    }

    LaunchedEffect(slot.key, refreshToken) {
        loading = true
        scanResult = withContext(Dispatchers.IO) {
            InternalPdfBrowserStore.scan(context, slot)
        }
        loading = false
    }

    val normalizedQuery = query.trim().lowercase()
    val visibleFiles = scanResult.files.filter { file ->
        normalizedQuery.isBlank() ||
            normalizedQuery in file.name.lowercase() ||
            normalizedQuery in file.relativePath.lowercase() ||
            normalizedQuery in file.inferredLabel().lowercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowserBlack)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹  ${slot.displayTitle}",
                    modifier = Modifier.clickable(onClick = onBack),
                    color = BrowserWhite.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                )
                Text(
                    "其他位置",
                    modifier = Modifier.clickable(onClick = onOtherLocation),
                    color = BrowserBlue,
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.height(28.dp))
            Text("教材文件", color = BrowserWhite, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "授权一次目录后，以后直接在这里搜索和选择 PDF。",
                color = BrowserMuted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(24.dp))
            BrowserSearchField(query, onValueChange = { query = it })
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrowserAction(
                    label = "添加教材目录",
                    color = BrowserBlue,
                    modifier = Modifier.weight(1f),
                ) { treeLauncher.launch(null) }
                BrowserAction(
                    label = if (loading) "扫描中" else "刷新",
                    color = BrowserYellow,
                    modifier = Modifier.weight(1f),
                    enabled = !loading,
                ) { refreshToken += 1 }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = 44.dp,
            ),
        ) {
            if (scanResult.directories.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("已授权目录", color = BrowserMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
                items(scanResult.directories, key = { it.uri }) { directory ->
                    DirectoryRow(
                        directory = directory,
                        confirming = confirmRemoveUri == directory.uri,
                        onRemove = {
                            if (confirmRemoveUri == directory.uri) {
                                InternalPdfBrowserStore.removeDirectory(context, directory.uri)
                                confirmRemoveUri = null
                                refreshToken += 1
                            } else {
                                confirmRemoveUri = directory.uri
                            }
                        },
                    )
                }
                item {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        if (visibleFiles.isEmpty() && !loading) "没有找到 PDF" else "PDF · ${visibleFiles.size}",
                        color = BrowserMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (scanResult.directories.isEmpty() && !loading) {
                item {
                    EmptyBrowserState(
                        onAddDirectory = { treeLauncher.launch(null) },
                        onOtherLocation = onOtherLocation,
                    )
                }
            } else if (loading && visibleFiles.isEmpty()) {
                item {
                    Spacer(Modifier.height(70.dp))
                    Text("正在扫描授权目录中的 PDF……", color = BrowserYellow, fontSize = 18.sp)
                }
            }

            items(visibleFiles, key = { it.uri }) { file ->
                PdfFileRow(
                    file = file,
                    slot = slot,
                    installed = installedTitles.any { installed ->
                        installed.equals(file.name.removeSuffix(".pdf"), ignoreCase = true) ||
                            file.name.contains(installed, ignoreCase = true)
                    },
                    onSelect = { onSelect(Uri.parse(file.uri)) },
                )
            }

            if (scanResult.errors.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("部分目录无法读取", color = BrowserRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    scanResult.errors.forEach { error ->
                        Text(error, color = BrowserWhite.copy(alpha = 0.58f), fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserSearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrowserLine, RoundedCornerShape(9.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
    ) {
        if (value.isBlank()) {
            Text("搜索文件名、学科或年级", color = BrowserMuted, fontSize = 15.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = BrowserWhite, fontSize = 15.sp),
            cursorBrush = SolidColor(BrowserBlue),
        )
    }
}

@Composable
private fun DirectoryRow(
    directory: PdfLibraryDirectory,
    confirming: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(directory.name, color = BrowserWhite, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text("包含全部子目录", color = BrowserMuted, fontSize = 11.sp)
        }
        Text(
            if (confirming) "确认移除" else "移除",
            modifier = Modifier.clickable(onClick = onRemove).padding(start = 14.dp, vertical = 8.dp),
            color = BrowserRed,
            fontSize = 12.sp,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BrowserLine))
}

@Composable
private fun PdfFileRow(
    file: PdfLibraryEntry,
    slot: TextbookSlot,
    installed: Boolean,
    onSelect: () -> Unit,
) {
    val score = file.matchScore(slot)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 17.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                file.name,
                modifier = Modifier.weight(1f),
                color = BrowserWhite,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    installed -> "已导入"
                    score >= 70 -> "高度匹配"
                    score >= 40 -> "可能匹配"
                    else -> "PDF"
                },
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                color = when {
                    installed -> BrowserBlue
                    score >= 70 -> BrowserYellow
                    else -> BrowserMuted
                },
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(file.inferredLabel(), color = if (score >= 70) BrowserYellow else BrowserMuted, fontSize = 12.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            "${file.sizeLabel()} · ${file.modifiedLabel()}\n${file.relativePath}",
            color = BrowserWhite.copy(alpha = 0.34f),
            fontSize = 11.sp,
            lineHeight = 17.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BrowserLine))
}

@Composable
private fun EmptyBrowserState(
    onAddDirectory: () -> Unit,
    onOtherLocation: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 70.dp)) {
        Text("还没有教材目录", color = BrowserWhite, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Text(
            "建议先建立 Documents/教材，然后只通过系统界面授权这一次。之后所有 PDF 都在内置浏览器中选择。",
            color = BrowserMuted,
            fontSize = 16.sp,
            lineHeight = 25.sp,
        )
        Spacer(Modifier.height(28.dp))
        BrowserAction("添加教材目录", BrowserBlue, onClick = onAddDirectory)
        Spacer(Modifier.height(12.dp))
        BrowserAction("仅这一次使用其他位置", BrowserWhite.copy(alpha = 0.64f), onClick = onOtherLocation)
    }
}

@Composable
private fun BrowserAction(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        label,
        modifier = modifier
            .height(48.dp)
            .border(1.dp, color.copy(alpha = if (enabled) 1f else 0.35f), RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        color = color.copy(alpha = if (enabled) 1f else 0.35f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
