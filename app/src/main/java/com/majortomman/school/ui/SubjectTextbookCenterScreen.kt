package com.majortomman.school.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.material.IMPORT_TUTORIAL_VERSION
import com.majortomman.school.data.material.InstalledTextbook
import com.majortomman.school.data.material.MaterialLibraryState
import com.majortomman.school.data.material.SubjectTemplate
import com.majortomman.school.data.material.SubjectTemplates
import com.majortomman.school.data.material.TextbookProcessingState
import com.majortomman.school.data.material.TextbookProcessingStatus
import com.majortomman.school.data.material.TextbookSlot
import com.majortomman.school.data.material.TextbookVolume
import com.majortomman.school.data.material.gradeLabel

private val CenterBlack = Color(0xFF050608)
private val CenterWhite = Color(0xFFF5F7FA)
private val CenterBlue = Color(0xFF2D7BFF)
private val CenterRed = Color(0xFFFF453A)
private val CenterYellow = Color(0xFFFFCC00)
private val CenterMuted = CenterWhite.copy(alpha = 0.46f)
private val CenterLine = CenterWhite.copy(alpha = 0.13f)

private enum class CenterPage {
    SUBJECTS,
    GRADES,
    SLOT,
    TUTORIAL,
}

@Composable
fun SubjectTextbookCenterScreen(
    libraryState: MaterialLibraryState,
    completedTutorials: Set<String>,
    onTutorialCompleted: (String, Int) -> Unit,
    onImport: (TextbookSlot, Uri) -> Unit,
    onCancelProcessing: (TextbookSlot) -> Unit,
    onRemove: (TextbookSlot) -> Unit,
    onEnterCourse: (InstalledTextbook) -> Unit,
    onOpenTextbook: (InstalledTextbook, Int) -> Unit,
) {
    var pageName by rememberSaveable { mutableStateOf(CenterPage.SUBJECTS.name) }
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSlotKey by rememberSaveable { mutableStateOf<String?>(null) }
    var tutorialPage by rememberSaveable { mutableIntStateOf(0) }
    var pendingImportSlotKey by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val slot = pendingImportSlotKey?.let(TextbookSlot::fromKey)
        if (uri != null && slot != null) onImport(slot, uri)
        pendingImportSlotKey = null
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        documentLauncher.launch(materialMimeTypes())
    }

    fun launchImport(slot: TextbookSlot) {
        pendingImportSlotKey = slot.key
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            documentLauncher.launch(materialMimeTypes())
        }
    }

    val selectedSubject = selectedSubjectId?.let(SubjectTemplates::find)
    val selectedSlot = selectedSlotKey?.let(TextbookSlot::fromKey)
    val page = CenterPage.valueOf(pageName)

    AnimatedContent(
        targetState = page,
        modifier = Modifier.fillMaxSize().background(CenterBlack),
        transitionSpec = {
            (fadeIn(tween(260)) + slideInHorizontally(tween(360)) { it / 8 }) togetherWith
                (fadeOut(tween(150)) + slideOutHorizontally(tween(260)) { -it / 10 })
        },
        label = "subjectCenterNavigation",
    ) { currentPage ->
        when (currentPage) {
            CenterPage.SUBJECTS -> SubjectListPage(
                libraryState = libraryState,
                onSelect = { subject ->
                    selectedSubjectId = subject.id
                    selectedSlotKey = null
                    pageName = CenterPage.GRADES.name
                },
            )

            CenterPage.GRADES -> {
                if (selectedSubject == null) {
                    pageName = CenterPage.SUBJECTS.name
                } else {
                    GradeListPage(
                        subject = selectedSubject,
                        libraryState = libraryState,
                        onBack = { pageName = CenterPage.SUBJECTS.name },
                        onSelect = { slot ->
                            selectedSlotKey = slot.key
                            confirmRemove = false
                            val installed = libraryState.installed(slot)
                            val processing = libraryState.processing(slot)
                            pageName = if (installed == null && processing == null) {
                                tutorialPage = 0
                                CenterPage.TUTORIAL.name
                            } else {
                                CenterPage.SLOT.name
                            }
                        },
                    )
                }
            }

            CenterPage.SLOT -> {
                if (selectedSlot == null) {
                    pageName = CenterPage.GRADES.name
                } else {
                    SlotPage(
                        slot = selectedSlot,
                        installed = libraryState.installed(selectedSlot),
                        processing = libraryState.processing(selectedSlot),
                        confirmRemove = confirmRemove,
                        onBack = { pageName = CenterPage.GRADES.name },
                        onImport = {
                            tutorialPage = 0
                            pageName = CenterPage.TUTORIAL.name
                        },
                        onCancel = { onCancelProcessing(selectedSlot) },
                        onRemove = {
                            if (confirmRemove) {
                                onRemove(selectedSlot)
                                confirmRemove = false
                            } else {
                                confirmRemove = true
                            }
                        },
                        onEnterCourse = onEnterCourse,
                        onOpenTextbook = onOpenTextbook,
                    )
                }
            }

            CenterPage.TUTORIAL -> {
                if (selectedSlot == null || selectedSubject == null) {
                    pageName = CenterPage.GRADES.name
                } else {
                    val token = "${selectedSubject.id}:$IMPORT_TUTORIAL_VERSION"
                    ImportTutorialPage(
                        slot = selectedSlot,
                        pageIndex = tutorialPage,
                        canSkip = token in completedTutorials,
                        onBack = {
                            if (tutorialPage > 0) tutorialPage -= 1
                            else pageName = if (libraryState.installed(selectedSlot) == null) {
                                CenterPage.GRADES.name
                            } else {
                                CenterPage.SLOT.name
                            }
                        },
                        onSkip = {
                            onTutorialCompleted(selectedSubject.id, IMPORT_TUTORIAL_VERSION)
                            pageName = CenterPage.SLOT.name
                            launchImport(selectedSlot)
                        },
                        onContinue = {
                            if (tutorialPage < tutorialPages.lastIndex) {
                                tutorialPage += 1
                            } else {
                                onTutorialCompleted(selectedSubject.id, IMPORT_TUTORIAL_VERSION)
                                pageName = CenterPage.SLOT.name
                                launchImport(selectedSlot)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectListPage(
    libraryState: MaterialLibraryState,
    onSelect: (SubjectTemplate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 28.dp),
    ) {
        Text("学科", color = CenterWhite, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("教材、课程和学习记录按学科与年级分别管理。", color = CenterMuted, lineHeight = 23.sp)
        Spacer(Modifier.height(42.dp))
        SubjectTemplates.all.forEachIndexed { index, subject ->
            val installedCount = libraryState.installedTextbooks.count { it.slot.subjectId == subject.id }
            val processingCount = libraryState.processing.values.count { it.slot.subjectId == subject.id }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(subject) }
                    .padding(vertical = 19.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(subject.title, color = CenterWhite, fontSize = 28.sp, fontWeight = FontWeight.Medium)
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
            if (index != SubjectTemplates.all.lastIndex) ThinDivider()
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun GradeListPage(
    subject: SubjectTemplate,
    libraryState: MaterialLibraryState,
    onBack: () -> Unit,
    onSelect: (TextbookSlot) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
    ) {
        CenterBack("学科", onBack)
        Spacer(Modifier.height(30.dp))
        Text(subject.title, color = CenterWhite, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("选择年级和册次", color = CenterMuted)
        Spacer(Modifier.height(38.dp))
        subject.grades.forEach { grade ->
            Text(gradeLabel(grade), color = CenterWhite, fontSize = 23.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextbookVolume.entries.forEach { volume ->
                    val slot = TextbookSlot(subject.id, subject.title, grade, volume)
                    val installed = libraryState.installed(slot)
                    val job = libraryState.processing(slot)
                    SlotButton(
                        label = volume.label,
                        status = when {
                            job?.status == TextbookProcessingStatus.RUNNING -> "${job.progress}%"
                            job?.status == TextbookProcessingStatus.QUEUED -> "等待中"
                            job?.status == TextbookProcessingStatus.FAILED -> "未完成"
                            installed != null -> "已导入"
                            else -> "未导入"
                        },
                        color = when {
                            job?.status == TextbookProcessingStatus.FAILED -> CenterRed
                            job != null -> CenterYellow
                            installed != null -> CenterBlue
                            else -> CenterWhite.copy(alpha = 0.42f)
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(slot) },
                    )
                }
            }
            Spacer(Modifier.height(27.dp))
            if (grade != subject.grades.last) ThinDivider()
            Spacer(Modifier.height(27.dp))
        }
    }
}

@Composable
private fun SlotPage(
    slot: TextbookSlot,
    installed: InstalledTextbook?,
    processing: TextbookProcessingState?,
    confirmRemove: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onEnterCourse: (InstalledTextbook) -> Unit,
    onOpenTextbook: (InstalledTextbook, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
    ) {
        CenterBack(slot.subjectTitle, onBack)
        Spacer(Modifier.height(46.dp))
        Text(slot.displayTitle, color = CenterWhite, fontSize = 42.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))

        if (processing != null) {
            ProcessingSection(processing)
            Spacer(Modifier.height(32.dp))
            when (processing.status) {
                TextbookProcessingStatus.FAILED -> {
                    Text("已有处理结果会保留。重新选择教材后可以再次处理。", color = CenterMuted, lineHeight = 23.sp)
                    Spacer(Modifier.height(24.dp))
                    CenterOutlinedButton("重新导入", CenterBlue, onClick = onImport)
                }
                TextbookProcessingStatus.QUEUED, TextbookProcessingStatus.RUNNING -> {
                    Text("你可以离开此页面，后台任务会继续运行。", color = CenterMuted, lineHeight = 23.sp)
                    Spacer(Modifier.height(24.dp))
                    CenterOutlinedButton("取消处理", CenterRed, onClick = onCancel)
                }
            }
        }

        if (installed != null) {
            if (processing != null) Spacer(Modifier.height(44.dp))
            Text(installed.pack.manifest.title, color = CenterWhite, fontSize = 25.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "${installed.lessons.size} 个课程 · ${installed.pageCount.takeIf { it > 0 } ?: "—"} 页 · ${installed.pack.manifest.version}",
                color = CenterMuted,
            )
            Spacer(Modifier.height(34.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CenterOutlinedButton(
                    label = "导入教科书",
                    color = CenterYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onImport,
                )
                CenterOutlinedButton(
                    label = "进入课程",
                    color = CenterBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onEnterCourse(installed) },
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "查看教材",
                    modifier = Modifier.clickable {
                        onOpenTextbook(installed, installed.lessons.firstOrNull()?.pageStart ?: 1)
                    },
                    color = CenterWhite.copy(alpha = 0.68f),
                )
                Text(
                    if (confirmRemove) "再次点击确认移除" else "移除教材",
                    modifier = Modifier.clickable(onClick = onRemove),
                    color = CenterRed,
                )
            }
        } else if (processing == null) {
            Text("尚未导入教材", color = CenterMuted, fontSize = 18.sp)
            Spacer(Modifier.height(30.dp))
            CenterOutlinedButton("开始导入", CenterBlue, onClick = onImport)
        }
    }
}

@Composable
private fun ProcessingSection(state: TextbookProcessingState) {
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
private fun ImportTutorialPage(
    slot: TextbookSlot,
    pageIndex: Int,
    canSkip: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val tutorial = tutorialPages[pageIndex]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 26.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("返回", modifier = Modifier.clickable(onClick = onBack), color = CenterWhite.copy(alpha = 0.72f))
            if (canSkip) {
                Text("跳过", modifier = Modifier.clickable(onClick = onSkip), color = CenterBlue)
            } else {
                Text("首次导入 · 必须阅读", color = CenterYellow, fontSize = 12.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                "%02d".format(pageIndex + 1),
                color = tutorial.color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                tutorial.title,
                color = CenterWhite,
                fontSize = 46.sp,
                lineHeight = 51.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(tutorial.body(slot), color = CenterWhite.copy(alpha = 0.68f), fontSize = 19.sp, lineHeight = 30.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ProgressLine(((pageIndex + 1) * 100) / tutorialPages.size, tutorial.color)
            CenterOutlinedButton(
                label = if (pageIndex == tutorialPages.lastIndex) "选择教材" else "继续",
                color = tutorial.color,
                onClick = onContinue,
            )
        }
    }
}

@Composable
fun NoActiveTextbookScreen(onOpenSubjects: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(CenterBlack).systemBarsPadding().padding(26.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("先选择教材", color = CenterWhite, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Text("课程路径会根据已处理完成的教材生成。", color = CenterMuted, fontSize = 18.sp, lineHeight = 27.sp)
        Spacer(Modifier.height(30.dp))
        CenterOutlinedButton("前往学科", CenterBlue, onClick = onOpenSubjects)
    }
}

@Composable
private fun CenterBack(label: String, onClick: () -> Unit) {
    Text(
        "‹  $label",
        modifier = Modifier.clickable(onClick = onClick),
        color = CenterWhite.copy(alpha = 0.72f),
        fontSize = 15.sp,
    )
}

@Composable
private fun SlotButton(
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
        Text(label, color = CenterWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(status, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun CenterOutlinedButton(
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
private fun ProgressLine(progress: Int, color: Color) {
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
private fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CenterLine))
}

private data class TutorialContent(
    val title: String,
    val body: (TextbookSlot) -> String,
    val color: Color,
)

private val tutorialPages = listOf(
    TutorialContent(
        title = "选择正确的教材",
        body = { slot ->
            "当前正在为${slot.displayTitle}导入教材。系统会检查学科、年级与册次，错误教材不会覆盖其他年级。"
        },
        color = CenterBlue,
    ),
    TutorialContent(
        title = "处理会在后台继续",
        body = {
            "教材会依次完成导入、完整性校验、页面索引和课程生成。离开页面或锁屏后，任务仍由系统继续调度。"
        },
        color = CenterYellow,
    ),
    TutorialContent(
        title = "课程来自教材",
        body = {
            "课程名称、页码范围和学习路径由教材目录与处理结果生成。每个课程都保留返回教材原页的入口。"
        },
        color = CenterWhite,
    ),
    TutorialContent(
        title = "完成后提醒你",
        body = {
            "允许通知后，处理完成或失败时会发送一条本地提醒。不会用于广告或无关消息。"
        },
        color = CenterRed,
    ),
)

private fun materialMimeTypes(): Array<String> = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
)
