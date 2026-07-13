package com.majortomman.school.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.majortomman.school.data.AttemptRecord
import com.majortomman.school.data.LearningProgress
import com.majortomman.school.data.ReviewItem
import com.majortomman.school.data.ScheduledReview

@Composable
fun RoomReviewScreen(
    fallbackItems: List<ReviewItem>,
    progress: LearningProgress,
    scheduledReviews: List<ScheduledReview>,
    recentAttempts: List<AttemptRecord>,
    onOpenLesson: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeading(
                eyebrow = "Review",
                title = "复习开始有记忆了",
                subtitle = "每次作答都会留下记录，并自动决定这个知识点什么时候再出现。",
            )
        }
        item {
            AnimatedCardItem(index = 0) {
                MotionCard(tone = CardTone.SOFT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("学习数据库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Room 已记录 ${recentAttempts.size} 条最近作答", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LabelPill("${progress.accuracyPercent}%")
                    }
                    LinearProgressIndicator(
                        progress = { progress.accuracyPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MetricRow {
                        MetricTile(progress.attempts.toString(), "累计作答")
                        MetricTile(progress.correctAttempts.toString(), "正确")
                        MetricTile(scheduledReviews.size.toString(), "复习队列")
                    }
                }
            }
        }

        item { SectionTitle("复习队列") }
        if (scheduledReviews.isEmpty()) {
            itemsIndexed(fallbackItems, key = { _, item -> item.id }) { index, item ->
                AnimatedCardItem(index = index + 1) {
                    MotionCard(tone = CardTone.SURFACE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBubble("↻", background = MaterialTheme.colorScheme.primaryContainer)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LabelPill(item.dueLabel)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(scheduledReviews, key = { _, item -> item.lessonId }) { index, item ->
                AnimatedCardItem(index = index + 1) {
                    ScheduledReviewCard(item = item, onClick = { onOpenLesson(item.lessonId) })
                }
            }
        }

        item { SectionTitle("最近作答") }
        if (recentAttempts.isEmpty()) {
            item {
                MotionCard(tone = CardTone.SOFT) {
                    Text("还没有详细记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("完成下一道练习后，这里会显示你的答案、反馈、错误类型和作答时间。")
                }
            }
        } else {
            itemsIndexed(recentAttempts, key = { _, item -> item.id }) { index, item ->
                AnimatedCardItem(index = scheduledReviews.size + index + 2) {
                    AttemptHistoryCard(item)
                }
            }
        }
    }
}

@Composable
private fun ScheduledReviewCard(
    item: ScheduledReview,
    onClick: () -> Unit,
) {
    MotionCard(
        tone = if (item.lastCorrect) CardTone.SUCCESS else CardTone.WARNING,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(if (item.lastCorrect) "✓" else "!")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.lessonTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (item.lastCorrect) "上次答对，间隔已拉长" else "上次未通过，安排短间隔巩固",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LabelPill(item.dueLabel)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelPill("间隔 ${item.intervalDays} 天")
            LabelPill("连续通过 ${item.repetitions} 次")
        }
        Text("点击打开这个知识点", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AttemptHistoryCard(item: AttemptRecord) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    MotionCard(
        tone = if (item.correct) CardTone.SUCCESS else CardTone.WARNING,
        onClick = { expanded = !expanded },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(if (item.correct) "✓" else "×")
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.lessonTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(item.createdLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LabelPill(if (item.correct) "正确" else "需要复习")
        }
        Text(item.feedback, maxLines = if (expanded) Int.MAX_VALUE else 2)
        item.mistakeType?.let { LabelPill("错误类型 · $it") }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("题目", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(item.questionText)
                Text("你的答案", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(item.answer.ifBlank { "未填写" })
            }
        }
        Text(
            if (expanded) "点击收起" else "点击查看当时的答案",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
