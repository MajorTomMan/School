package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal object SchoolUiMetrics {
    val pageHorizontal = 24.dp
    val pageTop = 24.dp
    val pageBottom = 36.dp
    val sectionGap = 30.dp
    val itemGap = 14.dp
    val compactGap = 8.dp
    val minTouchHeight = 48.dp
    val settingsRowMinHeight = 52.dp
    val textInputMinHeight = 56.dp
    val tabMinWidth = 58.dp
}

@Composable
internal fun SchoolBrandSlash(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    Box(modifier = modifier.width(8.dp).height(28.dp).rotate(18f).background(color, RoundedCornerShape(1.dp)))
}

@Composable
internal fun SchoolPageTitle(text: String, modifier: Modifier = Modifier, eyebrow: String? = null) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        eyebrow?.let {
            Text(it.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SchoolBrandSlash()
            Text(text, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun SchoolSectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SchoolBrandSlash(color = color, modifier = Modifier.height(24.dp).width(7.dp))
        Text(text, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Box(Modifier.weight(1f).height(1.dp).background(color.copy(alpha = 0.72f)))
    }
}

@Composable
internal fun SchoolPrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(10.dp))
            .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SchoolDivider(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
internal fun SchoolScrollableTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.onBackground,
    mutedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier.widthIn(min = SchoolUiMetrics.tabMinWidth).clickable { onSelect(index) }.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    label,
                    color = if (selected) selectedColor else mutedColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    Modifier.padding(top = 8.dp).fillMaxWidth(0.58f).height(if (selected) 3.dp else 1.dp)
                        .background(if (selected) indicatorColor else Color.Transparent),
                )
            }
        }
    }
}

@Composable
internal fun SchoolSettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    valueColor: Color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = SchoolUiMetrics.settingsRowMinHeight).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false, textAlign = TextAlign.End)
    }
    SchoolDivider()
}

@Composable
internal fun SchoolCompactTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = SchoolUiMetrics.minTouchHeight).padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("‹", modifier = Modifier.clickable(onClick = onBack).padding(vertical = 6.dp, horizontal = 2.dp), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall)
        Text(title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                modifier = Modifier.clickable(enabled = actionEnabled, onClick = onAction).padding(vertical = 8.dp),
                color = if (actionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
