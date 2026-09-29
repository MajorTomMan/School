package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.learning.cloud.CourseLibraryState
import com.majortomman.school.learning.cloud.InstalledCourse

@Composable
fun SubjectTextbookCenterScreen(
    libraryState: CourseLibraryState,
    onEnterCourse: (InstalledCourse) -> Unit,
    onOpenTextbook: (InstalledCourse, Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        SchoolPageTitle("课程", eyebrow = "SCHOOL / COURSES")
        Text("教材决定顺序，School 负责把每一节课讲清楚。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        if (libraryState.courses.isEmpty()) {
            Text("暂无已安装课程", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("前往“我的”中的课程设置下载课程包。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        libraryState.courses.groupBy(InstalledCourse::subject).forEach { (subject, courses) ->
            SchoolSectionLabel(subject)
            Spacer(Modifier.height(8.dp))
            courses.forEach { course ->
                CourseRow(course, onEnterCourse, onOpenTextbook)
                SchoolDivider()
            }
            Spacer(Modifier.height(28.dp))
        }
        Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
    }
}

@Composable
private fun CourseRow(course: InstalledCourse, onEnterCourse: (InstalledCourse) -> Unit, onOpenTextbook: (InstalledCourse, Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(course.title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            listOf(course.grade, course.semester, course.document.textbook.publisher, course.document.textbook.edition)
                .map(String::trim).filter(String::isNotBlank).joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("进入课程  →", modifier = Modifier.clickable { onEnterCourse(course) }.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("教材 PDF", modifier = Modifier.clickable { onOpenTextbook(course, 1) }.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
    }
}
