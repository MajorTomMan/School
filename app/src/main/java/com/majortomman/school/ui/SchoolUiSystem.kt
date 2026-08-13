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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Shared layout tokens for all School top-level and lesson screens. */
internal object SchoolUiMetrics {
    val pageHorizontal = 24.dp
    val pageTop = 28.dp
    val pageBottom = 36.dp
    val sectionGap = 28.dp
    val itemGap = 14.dp
    val compactGap = 8.dp
    val minTouchHeight = 48.dp
    val settingsRowMinHeight = 52.dp
    val textInputMinHeight = 56.dp
    val tabMinWidth = 58.dp
}

@Composable
internal fun SchoolPageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun SchoolSectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun SchoolDivider(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.13f)) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/**
 * Tabs never squeeze text into multiple lines. Large text turns the row into a horizontal scroller
 * instead of changing labels into vertical text or clipping neighboring tabs.
 */
@Composable
internal fun SchoolScrollableTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.onBackground,
    mutedColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f),
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
                modifier = Modifier
                    .widthIn(min = SchoolUiMetrics.tabMinWidth)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = label,
                    color = if (selected) selectedColor else mutedColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.58f)
                        .height(if (selected) 3.dp else 1.dp)
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
    valueColor: Color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SchoolUiMetrics.settingsRowMinHeight)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) 1f else 0.76f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
        )
    }
    SchoolDivider()
}

@Composable
internal fun SchoolCompactTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SchoolUiMetrics.minTouchHeight)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "返回",
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}
