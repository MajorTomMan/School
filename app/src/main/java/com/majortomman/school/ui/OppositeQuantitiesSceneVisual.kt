package com.majortomman.school.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.course.CourseSceneData
import com.majortomman.school.ui.visualization.SchoolVisualizationCatalog
import com.majortomman.school.ui.visualization.core.VisualizationArguments
import com.majortomman.school.ui.visualization.subjects.math.AccountTrendVisualizationRenderer
import com.majortomman.school.ui.visualization.subjects.math.GrowthRateTrendVisualizationRenderer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

private data class OppositeQuantityScene(
    val id: String,
    val label: String,
    val unit: String,
    val positiveMeaning: String,
    val negativeMeaning: String,
    val bound: Float,
    val step: Float,
    val initialValue: Float,
)

private val oppositeQuantityScenes = listOf(
    OppositeQuantityScene("temperature", "温度", "℃", "零上", "零下", 10f, 1f, 3f),
    OppositeQuantityScene("account", "收支", "万元", "盈利", "亏损", 50f, 10f, 50f),
    OppositeQuantityScene("change", "变化", "%", "增长", "减少", 10f, 0.1f, -0.7f),
    OppositeQuantityScene("deviation", "质量偏差", "g", "超过标准", "低于标准", 100f, 5f, -30f),
    OppositeQuantityScene("elevation", "海拔", "m", "高于海平面", "低于海平面", 300f, 10f, 60f),
    OppositeQuantityScene("tolerance", "允许偏差", "mm", "偏大", "偏小", 0.08f, 0.01f, 0.03f),
)

/**
 * “相反意义的量”统一交互入口。
 *
 * 温度、海拔和质量偏差使用专用具象场景；收支与增长率由跨学科可视化目录提供折线趋势图；
 * 允许偏差继续使用通用基准轴。所有说明文字均由 Compose 测量，Canvas 只绘制图形。
 */
@Composable
internal fun OppositeQuantitiesSceneVisual(data: CourseSceneData) {
    val requestedScene = oppositeQuantityScenes.firstOrNull { it.id == data.string("scene") }
        ?: oppositeQuantityScenes.first()
    val allowedIds = data.strings("scenes")
    val availableScenes = allowedIds
        .mapNotNull { id -> oppositeQuantityScenes.firstOrNull { it.id == id } }
        .distinctBy { it.id }
        .ifEmpty { listOf(requestedScene) }

    var selectedId by rememberSaveable(data.string("scene"), allowedIds.joinToString("|")) {
        mutableStateOf(requestedScene.id)
    }
    val selectedScene = availableScenes.firstOrNull { it.id == selectedId }
        ?: availableScenes.first()
    var value by rememberSaveable(selectedScene.id) {
        mutableFloatStateOf(selectedScene.initialValue)
    }
    val animatedValue by animateFloatAsState(
        targetValue = value,
        label = "opposite-quantity-value",
    )
    val accent = when {
        animatedValue > 0.0001f -> InteractiveBlue
        animatedValue < -0.0001f -> InteractiveYellow
        else -> InteractiveWhite
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (availableScenes.size > 1) {
            Row(modifier = Modifier.fillMaxWidth()) {
                availableScenes.forEach { option ->
                    val selected = option.id == selectedScene.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedId = option.id
                                value = option.initialValue
                            }
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = option.label,
                            color = if (selected) InteractiveWhite else InteractiveMuted,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(if (selected) 2.dp else 1.dp)
                                .background(if (selected) InteractiveBlue else InteractiveLine.copy(alpha = 0.45f)),
                        )
                    }
                }
            }
        }

        when (selectedScene.id) {
            "temperature" -> TemperatureQuantityPanel(
                baselineText = baselineText(selectedScene),
                value = animatedValue,
                bound = selectedScene.bound,
                unit = selectedScene.unit,
            )
            "elevation" -> ElevationQuantityPanel(
                baselineText = baselineText(selectedScene),
                value = animatedValue,
                bound = selectedScene.bound,
                unit = selectedScene.unit,
            )
            "deviation" -> MassDeviationQuantityPanel(
                baselineText = baselineText(selectedScene),
                value = animatedValue,
                bound = selectedScene.bound,
                unit = selectedScene.unit,
            )
            "account" -> SchoolVisualizationCatalog.Render(
                key = AccountTrendVisualizationRenderer.key,
                arguments = trendArguments(selectedScene, animatedValue),
                modifier = Modifier.fillMaxWidth(),
            )
            "change" -> SchoolVisualizationCatalog.Render(
                key = GrowthRateTrendVisualizationRenderer.key,
                arguments = trendArguments(selectedScene, animatedValue),
                modifier = Modifier.fillMaxWidth(),
            )
            else -> OppositeQuantityAxisPanel(
                baselineText = baselineText(selectedScene),
                value = animatedValue,
                bound = selectedScene.bound,
                negativeMeaning = selectedScene.negativeMeaning,
                positiveMeaning = selectedScene.positiveMeaning,
                unit = selectedScene.unit,
            )
        }

        Slider(
            value = value.coerceIn(-selectedScene.bound, selectedScene.bound),
            onValueChange = { raw -> value = snapToStep(raw, selectedScene.step) },
            modifier = Modifier.fillMaxWidth().height(34.dp),
            valueRange = -selectedScene.bound..selectedScene.bound,
            steps = ((selectedScene.bound * 2f / selectedScene.step).roundToInt() - 1)
                .coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent.copy(alpha = 0.72f),
                inactiveTrackColor = InteractiveLine,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )

        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        Text(
            text = observationText(selectedScene, animatedValue),
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.84f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Start,
            maxLines = 2,
        )
    }
}

private fun trendArguments(scene: OppositeQuantityScene, value: Float): VisualizationArguments =
    VisualizationArguments.of(
        "value" to value,
        "bound" to scene.bound,
        "unit" to scene.unit,
    )

private fun baselineText(scene: OppositeQuantityScene): String = when (scene.id) {
    "temperature" -> "0 ℃"
    "account" -> "收支平衡 0 万元"
    "change" -> "变化率 0%"
    "deviation" -> "标准质量 2.5 kg · 偏差 0 g"
    "elevation" -> "海平面 0 m"
    "tolerance" -> "标准直径 40.00 mm"
    else -> "以 0 为基准"
}

private fun observationText(scene: OppositeQuantityScene, value: Float): String = when {
    scene.id == "tolerance" && abs(value) <= 0.0501f ->
        "实际直径是 ${displayQuantity(40f + value)} mm，位于 39.95～40.05 mm 的合格范围内。"
    scene.id == "tolerance" ->
        "实际直径是 ${displayQuantity(40f + value)} mm，已经超出 ±0.05 mm 的允许偏差。"
    abs(value) < 0.0001f ->
        "0 是两个相反方向共同的基准，不属于任何一个方向。"
    value > 0f ->
        "+ 表示相对基准向“${scene.positiveMeaning}”方向变化。"
    else ->
        "− 表示相对基准向“${scene.negativeMeaning}”方向变化。"
}

private fun snapToStep(value: Float, step: Float): Float = round(value / step) * step

private fun displayQuantity(value: Float): String {
    val rounded = value.roundToInt()
    return if (abs(value - rounded) < 0.0001f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}
