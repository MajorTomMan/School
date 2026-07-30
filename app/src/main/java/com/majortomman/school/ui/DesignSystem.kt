package com.majortomman.school.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

internal enum class CardTone {
    SURFACE,
    ACCENT,
    SOFT,
    SUCCESS,
    WARNING,
}

@Composable
internal fun PageHeading(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 连续页面中的重点区域。名称保留以兼容现有页面，但不再创建封闭卡片。
 */
@Composable
internal fun FocusSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.72f else 1f,
        animationSpec = tween(120),
        label = "focusSurfaceAlpha",
    )
    val clickModifier = if (onClick == null) Modifier else Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )

    Column(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .then(clickModifier)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/**
 * 旧名称 MotionCard 现在表示一个开放的信息段落：没有背景、圆角或阴影。
 */
@Composable
internal fun MotionCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.SURFACE,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.68f else 1f,
        animationSpec = tween(120),
        label = "sectionPressAlpha",
    )
    val accent = toneAccent(tone)
    val clickModifier = if (onClick == null) Modifier else Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )

    Column(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .then(clickModifier)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (tone == CardTone.SURFACE || tone == CardTone.SOFT) 1.dp else 2.dp)
                .background(accent),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content,
        )
    }
}

@Composable
internal fun AnimatedCardItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(8) * 45L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(280)) + slideInVertically(tween(320)) { it / 6 },
    ) {
        content()
    }
}

@Composable
internal fun SectionTitle(
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        action?.invoke()
    }
}

/** 标签使用文字与短线，不再使用胶囊背景。 */
@Composable
internal fun LabelPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    foreground: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier.padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            Modifier
                .width(28.dp)
                .height(1.dp)
                .background(background.copy(alpha = 0.82f)),
        )
    }
}

/** 图标保持为符号，不再放入圆形气泡。 */
@Composable
internal fun IconBubble(
    symbol: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = symbol,
        modifier = modifier.padding(vertical = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = background,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(totalSteps) { index ->
            val target = if (index <= currentStep) 1f else 0f
            val fill by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(260),
                label = "stepProgress$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
internal fun PathConnector(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(2.dp)
            .height(46.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
            ),
    )
}

@Composable
internal fun MetricRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}

@Composable
internal fun RowScope.MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier.weight(1f),
) {
    Column(
        modifier = modifier.padding(vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        )
    }
}

@Composable
private fun toneAccent(tone: CardTone): Color = when (tone) {
    CardTone.SURFACE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    CardTone.ACCENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    CardTone.SOFT -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    CardTone.SUCCESS -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
    CardTone.WARNING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
}
