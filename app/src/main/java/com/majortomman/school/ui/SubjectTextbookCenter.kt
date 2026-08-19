package com.majortomman.school.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.cloud.CourseLibraryState
import com.majortomman.school.learning.cloud.InstalledCourse

private val CenterWhite = Color(0xFFF5F7FA)
private val CenterBlue = Color(0xFF2D7BFF)
private val CenterMuted = CenterWhite.copy(alpha = 0.46f)

@Composable
fun SubjectTextbookCenterScreen(
    libraryState: CourseLibraryState,
    onEnterCourse: (InstalledCourse) -> Unit,
    onOpenTextbook: (InstalledCourse, Int) -> Unit,
) {
    CenterScrollPage {
        Text("课程", color = CenterWhite, fontSize = 46.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text("已安装课程直接来自云端 course.json，不再维护 APK 内置教材目录。", color = CenterMuted, fontSize = 15.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(30.dp))

        if (libraryState.courses.isEmpty()) {
            Text("暂无已安装课程", color = CenterMuted, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Text("请在设置的课程页下载需要的课程。", color = CenterMuted, fontSize = 14.sp)
            return@CenterScrollPage
        }

        libraryState.courses.groupBy(InstalledCourse::subject).forEach { (subject, courses) ->
            Text(subject, color = CenterBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            courses.forEach { course ->
                CourseRow(course, onEnterCourse, onOpenTextbook)
                ThinDivider()
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CourseRow(
    course: InstalledCourse,
    onEnterCourse: (InstalledCourse) -> Unit,
    onOpenTextbook: (InstalledCourse, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(course.title, color = CenterWhite, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        Text(
            listOf(course.grade, course.semester, course.document.textbook.publisher, course.document.textbook.edition)
                .map(String::trim).filter(String::isNotBlank).joinToString(" · "),
            color = CenterMuted,
            fontSize = 13.sp,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            CenterOutlinedButton("进入课程", CenterBlue, modifier = Modifier.weight(1f)) { onEnterCourse(course) }
            CenterOutlinedButton("打开教材", CenterWhite.copy(alpha = 0.72f), modifier = Modifier.weight(1f)) {
                onOpenTextbook(course, course.document.textbook.pdf.pageIndexOffset.coerceAtLeast(1))
            }
        }
    }
}
