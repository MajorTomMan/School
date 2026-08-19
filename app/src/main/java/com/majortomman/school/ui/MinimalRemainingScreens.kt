package com.majortomman.school.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.AttemptRecord
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.ReviewItem
import com.majortomman.school.data.ScheduledReview
import kotlinx.coroutines.delay

private val MinimalBlack = Color.Transparent
private val MinimalWhite = Color(0xFFF5F7FA)
private val MinimalBlue = Color(0xFF2D7BFF)
private val MinimalRed = Color(0xFFFF3B30)
private val MinimalYellow = Color(0xFFFFCC00)
private val MinimalMuted = MinimalWhite.copy(alpha = 0.46f)
private val MinimalLine = MinimalWhite.copy(alpha = 0.13f)

@Composable
fun MinimalRoomReviewScreen(
    fallbackItems: List<ReviewItem>,
    progress: LearningProgress,
    scheduledReviews: List<ScheduledReview>,
    recentAttempts: List<AttemptRecord>,
    onOpenLesson: (String) -> Unit,
) {
    val current = scheduledReviews.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MinimalBlack),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 30.dp),
    ) {
        item {
            Text("复习", color = MinimalWhite, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(34.dp))
        }
        item {
            if (current != null) {
                Text(current.dueLabel, color = MinimalYellow, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    current.lessonTitle,
                    color = MinimalWhite,
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (current.lastCorrect) "快速确认一次。" else "重新看清错误发生的位置。",
                    color = MinimalMuted,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(28.dp))
                MinimalTextAction("开始复习", MinimalBlue) { onOpenLesson(current.lessonId) }
            } else if (fallbackItems.isNotEmpty()) {
                Text("今天没有必须完成的复习。", color = MinimalWhite, fontSize = 25.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Text(fallbackItems.first().title, color = MinimalMuted)
            } else {
                Text("今天没有复习任务。", color = MinimalWhite, fontSize = 25.sp)
            }
            Spacer(Modifier.height(36.dp))
            MinimalDivider()
            Spacer(Modifier.height(30.dp))
        }
        if (scheduledReviews.size > 1) {
            item { MinimalSectionTitle("接下来") }
            itemsIndexed(scheduledReviews.drop(1), key = { _, item -> item.lessonId }) { index, item ->
                MinimalReviewRow(
                    index = index,
                    title = item.lessonTitle,
                    trailing = item.dueLabel,
                    color = if (item.lastCorrect) MinimalBlue else MinimalRed,
                    onClick = { onOpenLesson(item.lessonId) },
                )
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MinimalSectionTitle("最近作答")
                Text("${progress.accuracyPercent}%", color = MinimalYellow, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
        if (recentAttempts.isEmpty()) {
            item { Text("完成下一道练习后，这里会出现记录。", color = MinimalMuted) }
        } else {
            itemsIndexed(recentAttempts, key = { _, item -> item.id }) { index, item -> MinimalAttemptRow(index, item) }
        }
    }
}

@Composable
private fun MinimalReviewRow(
    index: Int,
    title: String,
    trailing: String,
    color: Color,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn() + expandVertically()) {
        Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.size(14.dp))
                Text(title, modifier = Modifier.weight(1f), color = MinimalWhite, fontSize = 18.sp)
                Text(trailing, color = MinimalMuted, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(16.dp))
            MinimalDivider()
        }
    }
}

@Composable
private fun MinimalAttemptRow(index: Int, item: AttemptRecord) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val color = if (item.correct) MinimalBlue else MinimalRed
    Column(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(index + 1), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.lessonTitle, color = MinimalWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(item.createdLabel, color = MinimalMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(if (item.correct) "正确" else "复习", color = color, maxLines = 1, softWrap = false)
        }
        AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(modifier = Modifier.padding(start = 34.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.feedback, color = MinimalWhite.copy(alpha = 0.72f), lineHeight = 22.sp)
                item.mistakeType?.let { Text("错误类型 · $it", color = MinimalRed) }
                Text("你的答案", color = MinimalMuted, style = MaterialTheme.typography.labelMedium)
                Text(item.answer.ifBlank { "未填写" }, color = MinimalWhite)
            }
        }
        Spacer(Modifier.height(16.dp))
        MinimalDivider()
    }
}

@Composable
private fun MinimalTextAction(label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, color = color, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
    }
}

@Composable
private fun MinimalSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(bottom = 18.dp),
        color = MinimalBlue,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun MinimalDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MinimalLine))
}
