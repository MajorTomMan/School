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
import androidx.compose.material3.MaterialTheme
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
import com.majortomman.school.learning.cloud.CourseLibraryRepository
import com.majortomman.school.learning.cloud.CourseResourceUsage
import com.majortomman.school.learning.cloud.CourseStorageManager
import com.majortomman.school.learning.cloud.CourseStorageSnapshot
import com.majortomman.school.learning.cloud.CourseTextbookRemovalResult
import com.majortomman.school.learning.cloud.CourseTextbookUpdate
import com.majortomman.school.learning.cloud.CourseUpdateCheckResult
import com.majortomman.school.learning.cloud.CourseUpdateKind
import com.majortomman.school.learning.cloud.CourseUpdateOffer
import com.majortomman.school.learning.cloud.InstalledCourse
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private data class CourseResourceRow(
    val id: String,
    val course: InstalledCourse?,
    val update: CourseTextbookUpdate?,
)

@Composable
internal fun CourseStorageSettingsPage() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val downloadState by CourseDownloadCoordinator.state.collectAsState()
    val libraryState by CourseLibraryRepository.state.collectAsState()
    var snapshot by remember { mutableStateOf<CourseStorageSnapshot?>(null) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var updateOffer by remember { mutableStateOf<CourseUpdateOffer?>(null) }
    var updateStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    var clearStatus by rememberSaveable { mutableStateOf<String?>(null) }

    suspend fun refreshState() {
        CourseLibraryRepository.refresh(context)
        snapshot = CourseStorageManager.inspect(context)
    }

    LaunchedEffect(Unit) {
        CourseDownloadCoordinator.initialize(context)
        refreshState()
    }
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is CourseDownloadUiState.Success -> {
                updateStatus = if (state.updatedTextbooks > 0) "课程资源下载完成，已更新 ${state.updatedTextbooks} 个课程。" else "所选课程已经是最新版本。"
                updateOffer = null
                refreshState()
            }
            is CourseDownloadUiState.NotPublished -> {
                updateStatus = "新版课程尚未发布。"
                updateOffer = null
            }
            is CourseDownloadUiState.Failed -> updateStatus = "下载失败：${state.message}"
            else -> Unit
        }
    }

    val downloadBusy = downloadState is CourseDownloadUiState.Restoring || downloadState is CourseDownloadUiState.Queued || downloadState is CourseDownloadUiState.Running
    val updates = updateOffer?.textbooks.orEmpty().associateBy(CourseTextbookUpdate::id)
    val local = libraryState.courses.associateBy(InstalledCourse::id)
    val rows = (local.keys + updates.keys).sorted().map { id -> CourseResourceRow(id, local[id], updates[id]) }

    Column {
        CourseSettingsSectionTitle("课程资源")
        TextLine(
            if (BuildConfig.COURSE_MANIFEST_URL.isBlank()) {
                "当前 APK 未配置课程源。"
            } else {
                "课程结构、题库、知识点和教材由远端清单统一管理；除 course.json 外的大资源可独立下载与更新。"
            },
            if (BuildConfig.COURSE_MANIFEST_URL.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        snapshot?.let {
            TextLine("已安装 ${it.installedTextbooks} 个课程 · ${formatBytes(it.activeBytes)}", MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), 12.sp)
            TextLine("上次检查：${formatCheckTime(it.lastCheckedAt)}", MaterialTheme.colorScheme.onSurfaceVariant, 12.sp)
        }
        Spacer(Modifier.height(18.dp))

        if (rows.isEmpty()) {
            TextLine("本地暂无课程。先检查课程源或选择全部下载。", MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        } else {
            rows.forEach { row ->
                CourseResourceItem(
                    row = row,
                    installedBytes = snapshot?.textbookBytes?.get(row.id),
                    resourceUsage = snapshot?.resourceUsage?.get(row.id),
                    downloadBusy = downloadBusy,
                    confirmingDelete = confirmDeleteId == row.id,
                    onDownload = {
                        confirmDeleteId = null
                        updateStatus = "${row.displayTitle()} 已交给后台下载任务。"
                        CourseDownloadCoordinator.enqueue(context, setOf(row.id))
                    },
                    onBeginDelete = { confirmDeleteId = row.id },
                    onCancelDelete = { confirmDeleteId = null },
                    onConfirmDelete = {
                        confirmDeleteId = null
                        scope.launch {
                            updateStatus = when (val result = CourseStorageManager.removeTextbook(context, row.id)) {
                                CourseTextbookRemovalResult.Busy -> "后台下载尚未结束，暂时不能删除课程。"
                                CourseTextbookRemovalResult.NotInstalled -> "${row.displayTitle()} 当前没有本地资源。"
                                is CourseTextbookRemovalResult.Removed -> "已删除 ${row.displayTitle()}，释放 ${formatBytes(result.removedBytes)}；学习记录保持不变。"
                                is CourseTextbookRemovalResult.Failed -> "删除失败：${result.message}"
                            }
                            refreshState()
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                when {
                    checking -> "正在检查…"
                    downloadBusy -> "下载任务运行中"
                    else -> "检查课程更新"
                },
                modifier = Modifier.weight(1f).padding(end = 12.dp).clickable(enabled = !checking && !downloadBusy && BuildConfig.COURSE_MANIFEST_URL.isNotBlank()) {
                    checking = true
                    updateStatus = "正在获取并验证课程清单…"
                    updateOffer = null
                    scope.launch {
                        val result = CourseStorageManager.checkForUpdates(context)
                        refreshState()
                        when (result) {
                            CourseUpdateCheckResult.Disabled -> updateStatus = "当前 APK 未配置课程源。"
                            CourseUpdateCheckResult.NotPublished -> updateStatus = "新版课程尚未发布。"
                            CourseUpdateCheckResult.NoUpdate -> updateStatus = "已安装课程均为最新版本。"
                            is CourseUpdateCheckResult.Available -> {
                                updateOffer = result.offer
                                updateStatus = "发现 ${result.offer.textbookCount} 个课程可下载或更新，共 ${formatBytes(result.offer.estimatedBytes)}。"
                            }
                            is CourseUpdateCheckResult.Failed -> updateStatus = "检查失败：${result.message}"
                        }
                        checking = false
                    }
                },
                color = if (checking || downloadBusy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            downloadState.downloadLabel()?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, softWrap = false) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "全部下载 / 更新",
            modifier = Modifier.clickable(enabled = !downloadBusy && BuildConfig.COURSE_MANIFEST_URL.isNotBlank()) {
                updateStatus = "课程已交给后台任务；只会下载缺失或发生变化的文件。"
                CourseDownloadCoordinator.enqueue(context)
            },
            color = if (downloadBusy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            fontSize = 13.sp,
        )

        AnimatedVisibility(visible = updateStatus != null) {
            CourseSettingsNotice(
                color = if (updateStatus.orEmpty().contains("失败") || updateStatus.orEmpty().contains("未配置")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                label = "课程状态",
                body = updateStatus.orEmpty(),
            )
        }

        Spacer(Modifier.height(48.dp))
        CourseSettingsSectionTitle("本地课程缓存")
        snapshot?.let { state ->
            val structureBytes = state.resourceUsage.values.sumOf(CourseResourceUsage::structureBytes)
            val assessmentBytes = state.resourceUsage.values.sumOf(CourseResourceUsage::assessmentBytes)
            val textbookBytes = state.resourceUsage.values.sumOf(CourseResourceUsage::textbookBytes)
            val otherAssetBytes = state.resourceUsage.values.sumOf(CourseResourceUsage::otherAssetBytes)
            TextLine("课程资源共 ${formatBytes(state.totalBytes)}", MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f))
            Spacer(Modifier.height(7.dp))
            TextLine(
                "结构 ${formatBytes(structureBytes)} · 题库 ${formatBytes(assessmentBytes)} · 教材 ${formatBytes(textbookBytes)}",
                MaterialTheme.colorScheme.onSurfaceVariant,
                12.sp,
            )
            if (otherAssetBytes > 0L) {
                TextLine("题目与其他资源 ${formatBytes(otherAssetBytes)}", MaterialTheme.colorScheme.onSurfaceVariant, 12.sp)
            }
            TextLine("下载与暂存 ${formatBytes(state.temporaryBytes)}", MaterialTheme.colorScheme.onSurfaceVariant, 12.sp)
        } ?: TextLine("正在统计本地课程…", MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        TextLine("清理只删除 course-packs 下的课程、PDF、图片和下载缓存；答题与复习记录单独保存。", MaterialTheme.colorScheme.onSurfaceVariant, 12.sp)
        Spacer(Modifier.height(20.dp))

        AnimatedContent(targetState = confirmClear, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "clearCourseCache") { confirming ->
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
                    color = if (clearing || downloadBusy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            } else {
                Column {
                    TextLine("这会删除所有已下载课程；学习记录仍会保留。", MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("取消", modifier = Modifier.clickable { confirmClear = false }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "确认全部清理",
                            modifier = Modifier.clickable {
                                confirmClear = false
                                clearing = true
                                scope.launch {
                                    clearStatus = when (val result = CourseStorageManager.clearCache(context)) {
                                        CourseCacheClearResult.Busy -> "后台下载尚未结束，课程缓存没有被修改。"
                                        is CourseCacheClearResult.Cleared -> "已清理 ${formatBytes(result.removedBytes)}，移除 ${result.removedTextbooks} 个本地课程；学习记录保持不变。"
                                        is CourseCacheClearResult.Failed -> "清理失败：${result.message}"
                                    }
                                    refreshState()
                                    clearing = false
                                }
                            },
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = clearStatus != null) {
            CourseSettingsNotice(
                color = if (clearStatus.orEmpty().contains("失败")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                label = "清理结果",
                body = clearStatus.orEmpty(),
            )
        }
    }
}

@Composable
private fun CourseResourceItem(
    row: CourseResourceRow,
    installedBytes: Long?,
    resourceUsage: CourseResourceUsage?,
    downloadBusy: Boolean,
    confirmingDelete: Boolean,
    onDownload: () -> Unit,
    onBeginDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val installed = row.course != null
    val update = row.update
    val stateText = when {
        update != null && update.kind == CourseUpdateKind.INITIAL -> "未下载 · ${formatBytes(update.estimatedBytes)}"
        update != null -> "有更新 · ${formatBytes(update.estimatedBytes)}"
        installed -> "已下载 · ${formatBytes(installedBytes ?: 0L)}"
        else -> "未下载"
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.displayTitle(), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                Text(row.displayMetadata(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Text(stateText, color = if (update != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, softWrap = false)
        }
        if (installed && resourceUsage != null) {
            CourseResourceBreakdown(resourceUsage)
        }
        if (confirmingDelete) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("取消", modifier = Modifier.clickable(onClick = onCancelDelete).padding(horizontal = 10.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("确认删除", modifier = Modifier.clickable(onClick = onConfirmDelete).padding(horizontal = 10.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (update != null || !installed) {
                    Text(
                        if (installed) "更新" else "下载",
                        modifier = Modifier.clickable(enabled = !downloadBusy, onClick = onDownload).padding(vertical = 4.dp),
                        color = if (downloadBusy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        "删除本地课程",
                        modifier = Modifier.clickable(enabled = !downloadBusy, onClick = onBeginDelete).padding(vertical = 4.dp),
                        color = if (downloadBusy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.82f),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)))
    }
}

@Composable
private fun CourseResourceBreakdown(usage: CourseResourceUsage) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            CourseResourceMetric("结构", usage.structureBytes, Modifier.weight(1f))
            CourseResourceMetric("题库", usage.assessmentBytes, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            CourseResourceMetric("教材", usage.textbookBytes, Modifier.weight(1f))
            CourseResourceMetric("题目资源", usage.otherAssetBytes, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CourseResourceMetric(label: String, bytes: Long, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(
            if (bytes > 0L) formatBytes(bytes) else "未安装",
            color = if (bytes > 0L) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }
}

private fun CourseResourceRow.displayTitle(): String = course?.title ?: id

private fun CourseResourceRow.displayMetadata(): String {
    val local = course ?: return id
    return listOf(local.subject, local.grade, local.semester).map(String::trim).filter(String::isNotBlank).joinToString(" · ")
}

@Composable
private fun CourseSettingsSectionTitle(text: String) {
    Text(text, modifier = Modifier.padding(bottom = 18.dp), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun CourseSettingsNotice(color: Color, label: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
        Text(label, color = color, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), lineHeight = 23.sp)
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
    is CourseDownloadUiState.Running -> if (totalBytes > 0L) "${(downloadedBytes * 100L / totalBytes).coerceIn(0L, 100L)}%" else stage
    is CourseDownloadUiState.Success -> "下载完成"
    is CourseDownloadUiState.NotPublished -> "尚未发布"
    is CourseDownloadUiState.Failed -> "下载失败"
}

private fun formatCheckTime(timestamp: Long): String {
    if (timestamp <= 0L) return "尚未检查"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
}
