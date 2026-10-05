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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.data.DisplayPreferences
import com.majortomman.school.data.DisplaySettings

@Composable
fun MyScreen(
    currentCourseTitle: String?,
    recentLessonTitle: String?,
    onOpenLearningRecord: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenCourseSettings: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val displaySettings by DisplayPreferences.state.collectAsState(initial = DisplaySettings())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        SchoolPageTitle("我的", eyebrow = "SCHOOL / PROFILE")
        Spacer(Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("S", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("同学", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("在知识的世界里，遇见更好的自己。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(26.dp))
        SchoolDivider()
        MyRow("◷", "学习记录", recentLessonTitle?.let { "最近学习：$it" } ?: "还没有学习记录", onOpenLearningRecord)
        MyRow("▤", "教材管理", currentCourseTitle?.let { "当前教材：$it" } ?: "尚未选择课程", onOpenCourses)
        MyRow("↓", "下载与存储", "课程 · 题库 · 教材", onOpenCourseSettings)
        MyRow("◐", "显示模式", displaySettings.themeMode.label, onOpenDisplaySettings)
        MyRow("⚙", "设置", "", onOpenSettings)
        MyRow("ⓘ", "关于 School", "", onOpenSettings)

        Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
    }
}

@Composable
private fun MyRow(symbol: String, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (value.isNotBlank()) {
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
    }
    SchoolDivider()
}
