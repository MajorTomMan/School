package com.majortomman.school.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.material.EducationStage
import com.majortomman.school.data.material.InstalledTextbook
import com.majortomman.school.data.material.MaterialLibraryState
import com.majortomman.school.data.material.SubjectTemplate
import com.majortomman.school.data.material.SubjectTemplates
import com.majortomman.school.data.material.TextbookProcessingState
import com.majortomman.school.data.material.TextbookProcessingStatus
import com.majortomman.school.data.material.TextbookSlot
import com.majortomman.school.data.material.TextbookVolume
import com.majortomman.school.data.material.gradeLabel

private val SelectionWhite = Color(0xFFF5F7FA)
private val SelectionBlue = Color(0xFF2D7BFF)
private val SelectionRed = Color(0xFFFF453A)
private val SelectionYellow = Color(0xFFFFCC00)
private val SelectionMuted = SelectionWhite.copy(alpha = 0.46f)

@Composable
internal fun StageListPage(
    libraryState: MaterialLibraryState,
    onSelect: (EducationStage) -> Unit,
) {
    CenterScrollPage {
        Text("学习阶段", color = SelectionWhite, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("教材、课程、题库和学习记录按教育阶段分别管理。", color = SelectionMuted, lineHeight = 23.sp)
        Spacer(Modifier.height(42.dp))
        EducationStage.entries.forEachIndexed { index, stage ->
            val installedCount = libraryState.installedTextbooks.count { it.slot.stage == stage }
            val processingCount = libraryState.processing.values.count { it.slot.stage == stage }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(stage) }.padding(vertical = 21.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stage.label, color = SelectionWhite, fontSize = 31.sp, fontWeight = FontWeight.Medium)
                StatusText(installedCount, processingCount)
            }
            if (index != EducationStage.entries.lastIndex) ThinDivider()
        }
    }
}

@Composable
internal fun SubjectListPage(
    stage: EducationStage,
    libraryState: MaterialLibraryState,
    onBack: () -> Unit,
    onSelect: (SubjectTemplate) -> Unit,
) {
    CenterScrollPage {
        CenterBack("学习阶段", onBack)
        Spacer(Modifier.height(30.dp))
        Text(stage.label, color = SelectionWhite, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("选择学科", color = SelectionMuted)
        Spacer(Modifier.height(38.dp))
        val subjects = SubjectTemplates.forStage(stage)
        subjects.forEachIndexed { index, subject ->
            val installedCount = libraryState.installedTextbooks.count {
                it.slot.stage == stage && it.slot.subjectId == subject.id
            }
            val processingCount = libraryState.processing.values.count {
                it.slot.stage == stage && it.slot.subjectId == subject.id
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(subject) }.padding(vertical = 19.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(subject.title, color = SelectionWhite, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                StatusText(installedCount, processingCount)
            }
            if (index != subjects.lastIndex) ThinDivider()
        }
    }
}

@Composable
internal fun GradeListPage(
    stage: EducationStage,
    subject: SubjectTemplate,
    libraryState: MaterialLibraryState,
    onBack: () -> Unit,
    onSelect: (TextbookSlot) -> Unit,
) {
    val specialistSeniorSubject = stage == EducationStage.SENIOR_HIGH && subject.id in setOf(
        "chinese", "english", "japanese", "physics", "chemistry",
    )
    val grades = if (specialistSeniorSubject) listOf(10) else subject.gradesFor(stage).toList()
    val volumes = TextbookVolume.optionsFor(stage, subject.id)
    CenterScrollPage {
        CenterBack(stage.label, onBack)
        Spacer(Modifier.height(30.dp))
        Text(subject.title, color = SelectionWhite, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(if (specialistSeniorSubject) "选择教材卷册" else "选择年级和册次", color = SelectionMuted)
        Spacer(Modifier.height(38.dp))
        grades.forEachIndexed { gradeIndex, grade ->
            Text(
                if (specialistSeniorSubject) "高中教材" else gradeLabel(grade),
                color = SelectionWhite,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(14.dp))
            volumes.chunked(2).forEach { rowVolumes ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowVolumes.forEach { volume ->
                        val slot = TextbookSlot(subject.id, subject.title, grade, volume, stage)
                        val installed = libraryState.installed(slot)
                        val job = libraryState.processing(slot)
                        SlotButton(
                            label = volume.labelFor(stage),
                            status = when {
                                job?.status == TextbookProcessingStatus.RUNNING -> "${job.progress}%"
                                job?.status == TextbookProcessingStatus.QUEUED -> "等待中"
                                job?.status == TextbookProcessingStatus.FAILED -> "未完成"
                                installed != null -> if (installed.pack.pdfFile.isFile) "已绑定" else "课程已就绪"
                                else -> "未导入"
                            },
                            color = when {
                                job?.status == TextbookProcessingStatus.FAILED -> SelectionRed
                                job != null -> SelectionYellow
                                installed != null -> SelectionBlue
                                else -> SelectionWhite.copy(alpha = 0.42f)
                            },
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(slot) },
                        )
                    }
                    if (rowVolumes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(17.dp))
            if (gradeIndex != grades.lastIndex) ThinDivider()
            Spacer(Modifier.height(17.dp))
        }
    }
}

@Composable
internal fun SlotPage(
    slot: TextbookSlot,
    installed: InstalledTextbook?,
    processing: TextbookProcessingState?,
    confirmRemove: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onOpenOcrLog: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onEnterCourse: (InstalledTextbook) -> Unit,
    onOpenTextbook: (InstalledTextbook, Int) -> Unit,
) {
    CenterScrollPage {
        CenterBack(slot.subjectTitle, onBack)
        Spacer(Modifier.height(46.dp))
        Text(
            installed?.pack?.manifest?.title ?: slot.displayTitle,
            color = SelectionWhite,
            fontSize = 42.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text("${slot.stage.label} · ${slot.volumeLabel}", color = SelectionMuted, fontSize = 13.sp)
        Spacer(Modifier.height(26.dp))

        if (processing != null) {
            ProcessingSection(processing)
            Spacer(Modifier.height(32.dp))
            if (processing.status == TextbookProcessingStatus.FAILED) {
                CenterOutlinedButton("重新选择 PDF", SelectionBlue, onClick = onImport)
            } else {
                CenterOutlinedButton("取消处理", SelectionRed, onClick = onCancel)
            }
            Spacer(Modifier.height(36.dp))
        }

        if (installed != null) {
            val bound = installed.pack.pdfFile.isFile
            Text(
                if (bound) "教材原书已绑定" else "预制课程已就绪，原书未绑定",
                color = if (bound) SelectionBlue else SelectionYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text("${installed.lessons.size} 个课程 · ${installed.pageCount} 页目录", color = SelectionMuted)
            Spacer(Modifier.height(30.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CenterOutlinedButton(
                    label = if (bound) "替换 PDF" else "绑定 PDF",
                    color = SelectionYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onImport,
                )
                CenterOutlinedButton(
                    label = "进入课程",
                    color = SelectionBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onEnterCourse(installed) },
                )
            }
            Spacer(Modifier.height(26.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (bound) {
                    Text(
                        "查看教材",
                        modifier = Modifier.clickable {
                            onOpenTextbook(installed, installed.lessons.firstOrNull()?.pageStart ?: 1)
                        },
                        color = SelectionWhite.copy(alpha = 0.68f),
                    )
                    Text("OCR 日志", modifier = Modifier.clickable(onClick = onOpenOcrLog), color = SelectionYellow)
                } else {
                    Text("绑定后可查看教材原页", color = SelectionMuted)
                }
                Text(
                    if (confirmRemove) "再次点击确认移除" else "移除教材",
                    modifier = Modifier.clickable(onClick = onRemove),
                    color = SelectionRed,
                )
            }
        } else if (processing == null) {
            Text("尚未安装课程或绑定教材 PDF", color = SelectionMuted, fontSize = 18.sp)
            Spacer(Modifier.height(30.dp))
            CenterOutlinedButton("选择教材 PDF", SelectionBlue, onClick = onImport)
        }
    }
}
