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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.Lesson
import com.majortomman.school.data.material.InstalledMaterialPack
import com.majortomman.school.learning.cloud.CloudCourseRepository

internal val InteractiveBlack = Color.Transparent
internal val InteractivePanel = Color.Transparent
internal val InteractiveWhite = Color(0xFFF5F7FA)
internal val InteractiveMuted = InteractiveWhite.copy(alpha = 0.52f)
internal val InteractiveLine = InteractiveWhite.copy(alpha = 0.12f)
internal val InteractiveBlue = Color(0xFF58C4DD)
internal val InteractiveYellow = Color(0xFFF4D35E)
internal val InteractiveGreen = Color(0xFF83C167)
internal val InteractiveRed = Color(0xFFFC6255)
internal val InteractivePurple = Color(0xFF9A72AC)

/** Entry point for the authored lesson-centric course runtime. */
@Composable
fun InteractiveLessonScreen(
    lesson: Lesson,
    spec: InteractiveLessonSpec,
    installedMaterial: InstalledMaterialPack,
    nextLessonTitle: String?,
    onOpenTextbook: (Int) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    require(spec.kind == InteractiveLessonKind.CLOUD_COURSE)
    val revision by CloudCourseRepository.revision.collectAsState()
    val authoredLesson = remember(lesson.id, lesson.title, revision) { CloudCourseRepository.lessonFor(lesson.id, lesson.title) }
    if (authoredLesson == null) {
        AuthoredCourseUnavailable(lesson.title, onBack)
        return
    }

    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("返回", modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp), color = InteractiveMuted, fontSize = 14.sp)
            Text(authoredLesson.title, color = InteractiveWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Text(authoredLesson.title, color = InteractiveWhite, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            authoredLesson.goals.forEach { goal -> Text("— $goal", color = InteractiveMuted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 3.dp)) }
            Spacer(Modifier.height(28.dp))
            AuthoredLessonContent(authoredLesson, installedMaterial.pdfFile.isFile, onOpenTextbook)
            Spacer(Modifier.height(36.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("教材仅作参考", color = InteractiveMuted.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(
                nextLessonTitle?.let { "完成并继续 →" } ?: "完成 →",
                modifier = Modifier.clickable(onClick = onComplete).padding(vertical = 8.dp),
                color = InteractiveBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AuthoredCourseUnavailable(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("返回", modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp), color = InteractiveMuted, fontSize = 14.sp)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = InteractiveWhite, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Text("新版课程内容尚未安装", color = InteractiveBlue, fontSize = 15.sp)
        }
        Text("旧版按教材页生成的课程包不会再被兼容，请下载新版课程。", color = InteractiveMuted, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
internal fun SectionTitle(title: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(3.dp).weight(0.12f).background(color))
        Text(title, modifier = Modifier.weight(0.88f), color = InteractiveWhite, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun InteractiveAction(label: String, color: Color, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier = modifier.height(48.dp).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) color else InteractiveMuted.copy(alpha = 0.45f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (enabled) 2.dp else 1.dp).background(if (enabled) color.copy(alpha = 0.78f) else InteractiveLine))
    }
}
