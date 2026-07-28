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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

@Composable
internal fun CourseStorageSettingsPage(onCacheChanged: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val downloadState by CourseDownloadCoordinator.state.collectAsState()
    var snapshot by rememberSaveable { mutableStateOf<CourseStorageSnapshot?>(null) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var updateOffer by rememberSaveable { mutableStateOf<CourseUpdateOffer?>(null) }
    var updateStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    var clearStatus by rememberSaveable { mutableStateOf<String?>(null) }

    fun refreshSnapshot() {
        scope.launch { snapshot = CourseStorageManager.inspect(context) }
    }

    LaunchedEffect(Unit) {
        CourseDownloadCoordinator.initialize(context)
        snapshot = CourseStorageManager.inspect(context)
    }
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is CourseDownloadUiState.Success -> {
                updateStatus = if (state.updatedTextbooks > 0) {
                    "课程更新完成，已启用 ${state.updatedTextbooks} 册教材。"
                } else {
                    "课程已经是最新版本。"
                }
                updateOffer = null
                snapshot = CourseStorageManager.inspect(context)
                onCacheChanged()
            }
            is CourseDownloadUiState.Failed -> updateStatus = "下载失败：${state.message}"
            else -> Unit
        }
    }

    val downloadBusy = downloadState is CourseDownloadUiState.Restoring ||
        downloadState is CourseDownloadUiState.Queued ||
        downloadState is CourseDownloadUiState.Running

    Column {
        CourseSettingsSectionTitle("课程更新")
        TextLine(
            if (BuildConfig.COURSE_MANIFEST_URL.isBlank()) {
                "当前 APK 未配置课程源。"
            } else {
                "稳定课程源已配置；检查只读取清单，不会自动下载。"
            },
            if (BuildConfig.COURSE_MANIFEST_URL.isBlank()) CourseSettingsRed else CourseSettingsMuted,
        )
        Spacer(Modifier.height(14.dp))
        snapshot?.let {
            TextLine("上次检查：${formatCheckTime(it.lastCheckedAt)}", CourseSettingsMuted, 12.sp)
        }
        Spacer(Modifier.height(18.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                when {
                    checking -> "正在检查…"
                    downloadBusy -> "下载任务运行中"
                    else -> "检查课程更新"
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
                            CourseUpdateCheckResult.NoUpdate -> updateStatus = "本地课程已经是最新版本。"
                            is CourseUpdateCheckResult.Available -> {
                                updateOffer = result.offer
                                updateStatus = "发现 ${result.offer.textbookCount} 册课程需要更新，预计下载 ${formatBytes(result.offer.estimatedBytes)}。"
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

        AnimatedVisibility(visible = updateStatus != null) {
            CourseSettingsNotice(
                color = if (updateStatus.orEmpty().contains("失败") || updateStatus.orEmpty().contains("未配置")) {
                    CourseSettingsRed
                } else {
                    CourseSettingsBlue
                },
                label = "课程状态",
                body = updateStatus.orEmpty(),
            )
        }

        updateOffer?.let { offer ->
            Spacer(Modifier.height(22.dp))
            offer.textbooks.forEach { CourseUpdateItem(it) }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "下载并安装更新",
                    modifier = Modifier.clickable(enabled = !downloadBusy) {
                        updateStatus = "课程更新已交给后台任务；切换应用不会中断。"
                        CourseDownloadCoordinator.enqueue(context)
                    },
                    color = if (downloadBusy) CourseSettingsMuted else CourseSettingsBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(48.dp))
        CourseSettingsSectionTitle("本地课程缓存")
        snapshot?.let { state ->
            TextLine("已安装 ${state.installedTextbooks} 册 · 共 ${formatBytes(state.totalBytes)}", CourseSettingsWhite.copy(alpha = 0.78f))
            Spacer(Modifier.height(7.dp))
            TextLine(
                "可离线课程 ${formatBytes(state.activeBytes)} · 下载与暂存 ${formatBytes(state.temporaryBytes)}",
                CourseSettingsMuted,
                12.sp,
            )
        } ?: TextLine("正在统计本地课程…", CourseSettingsMuted)
        Spacer(Modifier.height(12.dp))
        TextLine(
            "清理会删除课程包、教材 PDF、图片、下载分片和暂存目录；答题记录、错误次数、复习计划与掌握度不会删除。",
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
                        else -> "清理课程缓存"
                    },
                    modifier = Modifier.clickable(enabled = !clearing && !downloadBusy) {
                        clearStatus = null
                        confirmClear = true
                    },
                    color = if (clearing || downloadBusy) CourseSettingsMuted else CourseSettingsRed,
                )
            } else {
                Column {
                    TextLine("清理后需要重新下载课程才能继续学习，学习记录仍会保留。", CourseSettingsWhite.copy(alpha = 0.72f), 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("取消", modifier = Modifier.clickable { confirmClear = false }, color = CourseSettingsMuted)
                        Text(
                            "确认清理",
                            modifier = Modifier.clickable {
                                confirmClear = false
                                clearing = true
                                scope.launch {
                                    clearStatus = when (val result = CourseStorageManager.clearCache(context)) {
                                        CourseCacheClearResult.Busy -> "后台下载尚未结束，课程缓存没有被修改。"
                                        is CourseCacheClearResult.Cleared -> {
                                            onCacheChanged()
                                            "已清理 ${formatBytes(result.removedBytes)}，移除 ${result.removedTextbooks} 册本地课程；学习记录保持不变。"
                                        }
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
private fun CourseUpdateItem(item: CourseTextbookUpdate) {
    val kind = when (item.kind) {
        CourseUpdateKind.INITIAL -> "新增"
        CourseUpdateKind.FULL -> "完整更新"
        CourseUpdateKind.INCREMENTAL -> "增量更新"
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.id, color = CourseSettingsWhite, fontWeight = FontWeight.Medium)
            Text("$kind · ${formatBytes(item.estimatedBytes)}", color = CourseSettingsBlue, fontSize = 12.sp)
        }
        Text(
            buildString {
                append("变更 ${item.changedFiles} 个文件")
                if (item.deletedFiles > 0) append("，删除 ${item.deletedFiles} 个旧文件")
                if (item.reason.isNotBlank()) append(" · ${item.reason}")
            },
            color = CourseSettingsMuted,
            fontSize = 12.sp,
        )
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
    is CourseDownloadUiState.Failed -> "下载失败"
}

private fun formatCheckTime(timestamp: Long): String {
    if (timestamp <= 0L) return "尚未检查"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
}
