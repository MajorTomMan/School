package com.majortomman.school.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.BuildConfig
import com.majortomman.school.formatBytes
import com.majortomman.school.learning.cloud.CourseCacheClearResult
import com.majortomman.school.learning.cloud.CourseDownloadCoordinator
import com.majortomman.school.learning.cloud.CourseDownloadUiState
import com.majortomman.school.learning.cloud.CourseStorageManager
import com.majortomman.school.learning.cloud.CourseStorageSnapshot
import com.majortomman.school.learning.cloud.CourseTextbookRemovalResult
import com.majortomman.school.learning.cloud.CourseTextbookUpdate
import com.majortomman.school.learning.cloud.CourseUpdateCheckResult
import com.majortomman.school.learning.cloud.CourseUpdateKind
import com.majortomman.school.learning.cloud.CourseUpdateOffer
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private val CourseSettingsWhite = Color(0xFFF5F7FA)
private val CourseSettingsBlue = Color(0xFF2D7BFF)
private val CourseSettingsRed = Color(0xFFFF453A)
private val CourseSettingsYellow = Color(0xFFFFCC00)
private val CourseSettingsMuted = CourseSettingsWhite.copy(alpha = 0.46f)
private val CourseSettingsLine = CourseSettingsWhite.copy(alpha = 0.13f)

private data class CourseBookOption(val id: String, val label: String)

private val CourseBookOptions = listOf(
    CourseBookOption("pep-math-7-1", "数学 · 七年级上册"),
    CourseBookOption("pep-math-7-2", "数学 · 七年级下册"),
    CourseBookOption("pep-math-8-1", "数学 · 八年级上册"),
    CourseBookOption("pep-math-8-2", "数学 · 八年级下册"),
    CourseBookOption("pep-math-9-1", "数学 · 九年级上册"),
    CourseBookOption("pep-math-9-2", "数学 · 九年级下册"),
)

@Composable
internal fun CourseStorageSettingsPage() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val downloadState by CourseDownloadCoordinator.state.collectAsState()
    var snapshot by remember { mutableStateOf<CourseStorageSnapshot?>(null) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var updateOffer by remember { mutableStateOf<CourseUpdateOffer?>(null) }
    var updateStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    var clearStatus by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        CourseDownloadCoordinator.initialize(context)
        snapshot = CourseStorageManager.inspect(context)
    }
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is CourseDownloadUiState.Success -> {
                updateStatus = if (state.updatedTextbooks > 0) {
                    "教材资源下载完成，已启用 ${state.updatedTextbooks} 册。"
                } else {
                    "所选教材已经是最新版本。"
                }
                updateOffer = null
                snapshot = CourseStorageManager.inspect(context)
            }
            is CourseDownloadUiState.NotPublished -> {
                updateStatus = "新版课程包尚未发布。发布后即可按册下载。"
                updateOffer = null
            }
            is CourseDownloadUiState.Failed -> updateStatus = "下载失败：${state.message}"
            else -> Unit
        }
    }

    val downloadBusy = downloadState is CourseDownloadUiState.Restoring ||
        downloadState is CourseDownloadUiState.Queued ||
        downloadState is CourseDownloadUiState.Running
    val updateById = updateOffer?.textbooks.orEmpty().associateBy(CourseTextbookUpdate::id)

    Column {
        CourseSettingsSectionTitle("教材资源")
        TextLine(
            if (BuildConfig.COURSE_MANIFEST_URL.isBlank()) {
                "当前 APK 未配置课程源。"
            } else {
                "每册教材可独立下载、更新和删除；“全部下载”只是可选操作。"
            },
            if (BuildConfig.COURSE_MANIFEST_URL.isBlank()) CourseSettingsRed else CourseSettingsMuted,
        )
        Spacer(Modifier.height(10.dp))
        snapshot?.let {
            TextLine("已安装 ${it.installedTextbooks} 册 · ${formatBytes(it.activeBytes)}", CourseSettingsWhite.copy(alpha = 0.72f), 12.sp)
            TextLine("上次检查：${formatCheckTime(it.lastCheckedAt)}", CourseSettingsMuted, 12.sp)
        }
        Spacer(Modifier.height(18.dp))

        CourseBookOptions.forEach { book ->
            CourseBookResourceItem(
                book = book,
                installedBytes = snapshot?.textbookBytes?.get(book.id),
                update = updateById[book.id],
                downloadBusy = downloadBusy,
                confirmingDelete = confirmDeleteId == book.id,
                onDownload = {
                    confirmDeleteId = null
                    updateStatus = "${book.label} 已交给后台下载任务。"
                    CourseDownloadCoordinator.enqueue(context, setOf(book.id))
                },
                onBeginDelete = { confirmDeleteId = book.id },
                onCancelDelete = { confirmDeleteId = null },
                onConfirmDelete = {
                    confirmDeleteId = null
                    scope.launch {
                        updateStatus = when (val result = CourseStorageManager.removeTextbook(context, book.id)) {
                            CourseTextbookRemovalResult.Busy -> "后台下载尚未结束，暂时不能删除教材。"
                            CourseTextbookRemovalResult.NotInstalled -> "${book.label} 当前没有本地资源。"
                            is CourseTextbookRemovalResult.Removed -> "已删除 ${book.label}，释放 ${formatBytes(result.removedBytes)}；学习记录保持不变。"
                            is CourseTextbookRemovalResult.Failed -> "删除失败：${result.message}"
                        }
                        snapshot = CourseStorageManager.inspect(context)
                    }
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                when {
                    checking -> "正在检查…"
                    downloadBusy -> "下载任务运行中"
                    else -> "检查全部教材更新"
                },
                modifier = Modifier.clickable(
                    enabled = !checking && !downloadBusy && BuildConfig.COURSE_MANIFEST_URL.isNotBlank(),
                ) {
                    checking = true
                    updateStatus = "正在获取并验证稳定清单…"
                    updateOffer = null
                    scope.launch {
                        val checked = CourseStorageManager.checkForUpdates(context)
                        snapshot = CourseStorageManager.inspect(context)
                        when (val result = checked.result) {
                            CourseUpdateCheckResult.Disabled -> updateStatus = "当前 APK 未配置课程源。"
                            CourseUpdateCheckResult.NotPublished -> updateStatus = "新版课程包尚未发布。发布后即可按册下载。"
                            CourseUpdateCheckResult.NoUpdate -> updateStatus = "已下载教材均为最新版本；未下载教材仍可按册下载。"
                            is CourseUpdateCheckResult.Available -> {
                                updateOffer = result.offer
                                updateStatus = "发现 ${result.offer.textbookCount} 册教材可下载或更新，共 ${formatBytes(result.offer.estimatedBytes)}。"
                            }
                            is CourseUpdateCheckResult.Failed -> updateStatus = "检查失败：${result.message}"
                        }
                        checking = false
                    }
                },
                color = if (checking || downloadBusy) CourseSettingsMuted else CourseSettingsBlue,
                fontWeight = FontWeight.SemiBold,
            )
            downloadState.downloadLabel()?.let { Text(it, color = CourseSettingsMuted, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "全部下载 / 更新",
            modifier = Modifier.clickable(
                enabled = !downloadBusy && BuildConfig.COURSE_MANIFEST_URL.isNotBlank(),
            ) {
                updateStatus = "全部教材已交给后台任务；只会下载缺失或发生变化的文件。"
                CourseDownloadCoordinator.enqueue(context)
            },
            color = if (downloadBusy) CourseSettingsMuted else CourseSettingsWhite.copy(alpha = 0.68f),
            fontSize = 13.sp,
        )

        AnimatedVisibility(visible = updateStatus != null) {
            CourseSettingsNotice(
                color = if (updateStatus.orEmpty().contains("失败") || updateStatus.orEmpty().contains("未配置")) {
                    CourseSettingsRed
                } else {
                    CourseSettingsBlue
                },
                label = "教材状态",
                body = updateStatus.orEmpty(),
            )
        }

        Spacer(Modifier.height(48.dp))
        CourseSettingsSectionTitle("本地课程缓存")
        snapshot?.let { state ->
            TextLine("课程资源共 ${formatBytes(state.totalBytes)}", CourseSettingsWhite.copy(alpha = 0.78f))
            Spacer(Modifier.height(7.dp))
            TextLine(
                "可离线教材 ${formatBytes(state.activeBytes)} · 下载与暂存 ${formatBytes(state.temporaryBytes)}",
                CourseSettingsMuted,
                12.sp,
            )
        } ?: TextLine("正在统计本地课程…", CourseSettingsMuted)
        Spacer(Modifier.height(12.dp))
        TextLine(
            "单册删除和全量清理都只影响课程包、教材 PDF、图片与缓存；答题记录、错误次数、复习计划与掌握度不会删除。",
            CourseSettingsMuted,
            12.sp,
        )
        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState = confirmClear,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "clearCourseCache",
        ) { confirming ->
            if (!confirming) {
                Text(
                    when {
                        clearing -> "正在清理…"
                        downloadBusy -> "下载中不可清理"
                        else -> "清理全部课程缓存"
                    },
                    modifier = Modifier.clickable(enabled = !clearing && !downloadBusy) {
                        clearStatus = null
                        confirmClear = true
                    },
                    color = if (clearing || downloadBusy) CourseSettingsMuted else CourseSettingsRed,
                )
            } else {
                Column {
                    TextLine("这会删除所有已下载教材；学习记录仍会保留。", CourseSettingsWhite.copy(alpha = 0.72f), 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("取消", modifier = Modifier.clickable { confirmClear = false }, color = CourseSettingsMuted)
                        Text(
                            "确认全部清理",
                            modifier = Modifier.clickable {
                                confirmClear = false
                                clearing = true
                                scope.launch {
                                    clearStatus = when (val result = CourseStorageManager.clearCache(context)) {
                                        CourseCacheClearResult.Busy -> "后台下载尚未结束，课程缓存没有被修改。"
                                        is CourseCacheClearResult.Cleared ->
                                            "已清理 ${formatBytes(result.removedBytes)}，移除 ${result.removedTextbooks} 册本地课程；学习记录保持不变。"
                                        is CourseCacheClearResult.Failed -> "清理失败：${result.message}"
                                    }
                                    snapshot = CourseStorageManager.inspect(context)
                                    clearing = false
                                }
                            },
                            color = CourseSettingsRed,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = clearStatus != null) {
            CourseSettingsNotice(
                color = if (clearStatus.orEmpty().contains("失败")) CourseSettingsRed else CourseSettingsYellow,
                label = "清理结果",
                body = clearStatus.orEmpty(),
            )
        }
    }
}

@Composable
private fun CourseBookResourceItem(
    book: CourseBookOption,
    installedBytes: Long?,
    update: CourseTextbookUpdate?,
    downloadBusy: Boolean,
    confirmingDelete: Boolean,
    onDownload: () -> Unit,
    onBeginDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val installed = installedBytes != null
    val stateText = when {
        update != null && update.kind == CourseUpdateKind.INITIAL -> "未下载 · ${formatBytes(update.estimatedBytes)}"
        update != null -> "有更新 · ${formatBytes(update.estimatedBytes)}"
        installed -> "已下载 · ${formatBytes(installedBytes ?: 0L)}"
        else -> "未下载"
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.label, color = CourseSettingsWhite, fontWeight = FontWeight.Medium)
                Text(book.id, color = CourseSettingsMuted, fontSize = 11.sp)
            }
            Text(
                stateText,
                color = if (update != null) CourseSettingsBlue else CourseSettingsMuted,
                fontSize = 12.sp,
            )
        }
        if (confirmingDelete) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("取消", modifier = Modifier.clickable(onClick = onCancelDelete).padding(horizontal = 10.dp, vertical = 4.dp), color = CourseSettingsMuted)
                Text("确认删除", modifier = Modifier.clickable(onClick = onConfirmDelete).padding(horizontal = 10.dp, vertical = 4.dp), color = CourseSettingsRed, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (update != null || !installed) {
                    Text(
                        if (installed) "更新" else "下载",
                        modifier = Modifier.clickable(enabled = !downloadBusy, onClick = onDownload).padding(vertical = 4.dp),
                        color = if (downloadBusy) CourseSettingsMuted else CourseSettingsBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        "删除本册",
                        modifier = Modifier.clickable(enabled = !downloadBusy, onClick = onBeginDelete).padding(vertical = 4.dp),
                        color = if (downloadBusy) CourseSettingsMuted else CourseSettingsRed.copy(alpha = 0.82f),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(CourseSettingsLine))
    }
}

@Composable
private fun CourseSettingsSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(bottom = 18.dp),
        color = CourseSettingsYellow,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun CourseSettingsNotice(color: Color, label: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
        Text(label, color = color, fontWeight = FontWeight.Bold)
        Text(body, color = CourseSettingsWhite.copy(alpha = 0.72f), lineHeight = 23.sp)
    }
}

@Composable
private fun TextLine(text: String, color: Color, size: androidx.compose.ui.unit.TextUnit = 14.sp) {
    Text(text, color = color, fontSize = size, lineHeight = (size.value + 7).sp)
}

private fun CourseDownloadUiState.downloadLabel(): String? = when (this) {
    CourseDownloadUiState.Restoring -> "恢复任务状态"
    CourseDownloadUiState.Idle -> null
    is CourseDownloadUiState.Queued -> "等待网络"
    is CourseDownloadUiState.Running -> if (totalBytes > 0L) {
        "${(downloadedBytes * 100L / totalBytes).coerceIn(0L, 100L)}%"
    } else {
        stage
    }
    is CourseDownloadUiState.Success -> "下载完成"
    is CourseDownloadUiState.NotPublished -> "尚未发布"
    is CourseDownloadUiState.Failed -> "下载失败"
}

private fun formatCheckTime(timestamp: Long): String {
    if (timestamp <= 0L) return "尚未检查"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
}
